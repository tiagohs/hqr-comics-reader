#!/usr/bin/env python3
"""Every SwiftUI view of the iOS app has light and dark previews in its own file (like Android's @ThemePreviews).

    python3 scripts/ios/check_previews.py           # report
    python3 scripts/ios/check_previews.py --check   # exit 1 listing the views without both previews

A view is any non-private `struct NAME: View` (or `struct NAME<...>: View`); its file needs
`#Preview("NAME · Light"` and `#Preview("NAME · Dark"`.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
SOURCES = ROOT / "ios/HQR"
VIEW = re.compile(r"^(?P<access>(?:public |internal |private |fileprivate )?)struct (?P<name>\w+)(?:<[^>]*>)?\s*:\s*(?:[\w.]+\s*,\s*)*View\b", re.M)
SKIP = {"Previews.swift"}


def missing():
    found = []
    for file in sorted(SOURCES.rglob("*.swift")):
        if file.name in SKIP:
            continue
        text = file.read_text(encoding="utf-8")
        for match in VIEW.finditer(text):
            if match.group("access").strip() in ("private", "fileprivate"):
                continue
            name = match.group("name")
            lacks = [mode for mode in ("Light", "Dark") if f'#Preview("{name} · {mode}"' not in text]
            if lacks:
                found.append((file.relative_to(ROOT), name, lacks))
    return found


def main():
    gaps = missing()
    for file, name, lacks in gaps:
        print(f"{file}: {name} without {' and '.join(lacks)} preview")
    if not gaps:
        print("Every iOS view has light and dark previews")
    if "--check" in sys.argv and gaps:
        sys.exit(1)


if __name__ == "__main__":
    main()
