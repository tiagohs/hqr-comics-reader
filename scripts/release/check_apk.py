#!/usr/bin/env python3
"""RNF08_apk_has_compose_resources: an APK without the shared screens' resources crashes at the first string.

Usage: python3 scripts/release/check_apk.py <apk> [<apk> ...]
"""
import sys
import zipfile

REQUIRED = [
    "composeResources/com.tiagohs.hqr.ui.resources/values/strings.commonMain.cvr",
    "composeResources/com.tiagohs.hqr.ui.resources/values-pt/strings.commonMain.cvr",
    "composeResources/com.tiagohs.hqr.ui.resources/values-es/strings.commonMain.cvr",
    "classes.dex",
]


def missing(apk: str) -> list[str]:
    with zipfile.ZipFile(apk) as z:
        names = set(z.namelist())
    # Compose resources are packed as assets or as Java resources, depending on the plugin version.
    return [r for r in REQUIRED if r not in names and f"assets/{r}" not in names]


def main(apks: list[str]) -> int:
    if not apks:
        print(__doc__)
        return 2
    failed = False
    for apk in apks:
        gone = missing(apk)
        if gone:
            failed = True
            print(f"{apk}: missing {', '.join(gone)}")
        else:
            print(f"{apk}: ok")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
