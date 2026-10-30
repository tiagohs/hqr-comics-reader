"""RNF-14: every key exists in en, pt and es, and the screens show no text written in the code."""
import re, sys, pathlib

repo = pathlib.Path(__file__).resolve().parents[2]
root = repo / "shared/ui/src/commonMain/composeResources"
keys = {
    d.name: set(re.findall(r'<(?:string|plurals) name="([^"]+)"', (d / "strings.xml").read_text()))
    for d in root.glob("values*")
    if (d / "strings.xml").exists()
}
base = keys["values"]

# Compose resources show "\'" as written (a backslash on screen): apostrophes go plain.
escaped = [f"{d.name}: {line.strip()}" for d in root.glob("values*") for line in (d / "strings.xml").read_text().splitlines() if "\\'" in line]
if escaped:
    print("Escaped apostrophes (show a backslash):", *escaped, sep="\n  ")
    sys.exit(1)
bad = {name: sorted(base ^ k) for name, k in keys.items() if k != base}
if bad:
    print("Missing or extra keys:", bad)
    sys.exit(1)

# Words shown to people come from strings.xml: Text("Word…"), label = "Word…" or "Word " + … in screen code.
literal = re.compile(r'Text\(\s*"[A-Za-zÀ-ú]|label\s*=\s*"[A-Za-zÀ-ú]|title\s*=\s*"[A-Za-zÀ-ú]|"[A-ZÀ-Ú][a-zà-ú]+\.? "\s*\+')
found = []
for path in sorted((repo / "shared/ui/src/commonMain/kotlin").rglob("*.kt")) + sorted((repo / "shared/presentation/src/commonMain/kotlin").rglob("*.kt")):
    for n, line in enumerate(path.read_text().splitlines(), 1):
        code = line.split("//")[0]
        if literal.search(code) and "Exception(" not in code:
            found.append(f"{path.relative_to(repo)}:{n}: {line.strip()}")
if found:
    print("Text written in the code (use strings.xml):", *found, sep="\n  ")
    sys.exit(1)
print(f"{len(base)} keys in {len(keys)} languages; no text in the code")
