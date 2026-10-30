"""Prepares the GitHub release of a version (RF-84, ADR-015) as a draft; publishes it only with --publish.

The APK goes first (the 0.0.5 updater downloads the first file of releases/latest), then `<apk>.sha256`, read by the
new updater. The APK comes from the CI draft release `ci-<branch>` (or --apk) and must be signed with the app key:
a "-test-signed" build is refused. Notes: scripts/release/notes/v<version>.md.

    python3 scripts/release/prepare_release.py                 # draft v<versionName> from ci-rewrite-v1
    python3 scripts/release/prepare_release.py --publish       # makes the draft the latest release (public!)
"""
import argparse, hashlib, json, pathlib, re, subprocess, sys, tempfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
REPO = "tiagohs/hqr-comics-reader"


def gh(*args, data=None):
    cmd = ["gh", "api", *args]
    out = subprocess.run(cmd, input=data, capture_output=True, check=True)
    return json.loads(out.stdout) if out.stdout.strip() else None


def version_name():
    text = (ROOT / "android/build.gradle.kts").read_text()
    return re.search(r'versionName\s*=\s*"([^"]+)"', text).group(1)


def release_by_tag(tag):
    return next((r for r in gh(f"repos/{REPO}/releases?per_page=100") if r["tag_name"] == tag), None)


def ci_apk(branch, folder):
    ci = release_by_tag("ci-" + branch.replace("/", "-"))
    if ci is None:
        sys.exit(f"No CI release for {branch}: push and let CI finish first.")
    asset = next((a for a in ci["assets"] if a["name"].endswith(".apk") and "-release" in a["name"]), None)
    if asset is None:
        sys.exit("The CI release has no release APK.")
    path = folder / asset["name"]
    data = subprocess.run(
        ["gh", "api", "-H", "Accept: application/octet-stream", f"repos/{REPO}/releases/assets/{asset['id']}"],
        capture_output=True, check=True,
    ).stdout
    path.write_bytes(data)
    return path, ci["target_commitish"]


def upload(release_id, path, content_type):
    url = f"https://uploads.github.com/repos/{REPO}/releases/{release_id}/assets?name={path.name}"
    subprocess.run(
        ["gh", "api", "-X", "POST", url, "-H", f"Content-Type: {content_type}", "--input", str(path)],
        capture_output=True, check=True,
    )


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", default=version_name())
    parser.add_argument("--branch", default="rewrite/v1")
    parser.add_argument("--apk", type=pathlib.Path, help="a release APK instead of the CI one")
    parser.add_argument("--target", help="commit of the tag (default: the CI build's)")
    parser.add_argument("--publish", action="store_true", help="make the draft public and the latest release")
    parser.add_argument("--allow-test-signed", action="store_true", help="dry runs only: never publish such a build")
    args = parser.parse_args()
    tag = "v" + args.version
    existing = release_by_tag(tag)

    if args.publish:
        if existing is None or not existing["draft"]:
            sys.exit(f"{tag}: no draft to publish.")
        names = [a["name"] for a in existing["assets"]]
        if not names or not names[0].endswith(".apk") or "-test-signed" in names[0]:
            sys.exit(f"{tag}: the first file must be the app-signed APK, found {names}.")
        gh(f"repos/{REPO}/releases/{existing['id']}", "-X", "PATCH", "-F", "draft=false", "-f", "make_latest=true")
        print(f"{tag} published: {existing['html_url'].replace('/untagged-', '/tag/')}")
        return

    notes = (ROOT / f"scripts/release/notes/{tag}.md").read_text()
    with tempfile.TemporaryDirectory() as tmp:
        folder = pathlib.Path(tmp)
        apk, commit = (args.apk, args.target) if args.apk else ci_apk(args.branch, folder)
        if "-test-signed" in apk.name and not args.allow_test_signed:
            sys.exit(f"{apk.name} is signed with a test key: add the app key to CI (HQR_KEYSTORE_*) first.")
        final = folder / f"hqr-{tag}.apk"
        final.write_bytes(apk.read_bytes())
        digest = hashlib.sha256(final.read_bytes()).hexdigest()
        sha = folder / (final.name + ".sha256")
        sha.write_text(f"{digest}  {final.name}\n")
        if existing is not None:
            if not existing["draft"]:
                sys.exit(f"{tag} is already public: nothing changed.")
            gh(f"repos/{REPO}/releases/{existing['id']}", "-X", "DELETE")
        body = {"tag_name": tag, "name": f"HQR {args.version}", "body": notes, "draft": True, "prerelease": False}
        if commit or args.target:
            body["target_commitish"] = args.target or commit
        release = gh(f"repos/{REPO}/releases", "-X", "POST", "--input", "-", data=json.dumps(body).encode())
        upload(release["id"], final, "application/vnd.android.package-archive")
        upload(release["id"], sha, "text/plain")
        print(f"Draft {tag} ready ({final.name}, sha256 {digest[:12]}…): {release['html_url']}")
        print("Check it on GitHub, then: python3 scripts/release/prepare_release.py --publish")


if __name__ == "__main__":
    main()
