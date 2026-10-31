#!/usr/bin/env python3
"""The iOS asset catalog from the Android icon: android/src/main/res/drawable/ic_launcher_*.xml -> ios/HQR/Assets.xcassets.

The app icon (1024 px, light, dark and tinted) is the same speech balloon as the Android adaptive icon, drawn from its
vector paths; the launch background and the accent come from docs/tokens.json. Needs `pip install cairosvg` to draw.
`--check` fails when the catalog's JSON files or the icon are missing or stale.
"""

import hashlib
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "android/src/main/res/drawable"
OUT = ROOT / "ios/HQR/Assets.xcassets"
TOKENS = ROOT / "docs/tokens.json"
NS = "{http://schemas.android.com/apk/res/android}"
SIZE = 1024
# The adaptive icon's safe zone is the middle 72 of 108: iOS shows the whole square, so the art is scaled to fill it.
ZOOM = 108 / 80


def paths(name, fill=None):
    out = []
    for p in ET.parse(RES / f"{name}.xml").getroot().iter("path"):
        color = fill or p.get(NS + "fillColor")
        alpha = p.get(NS + "fillAlpha", "1")
        out.append(f'<path fill="{color}" fill-opacity="{alpha}" d="{p.get(NS + "pathData")}"/>')
    return "".join(out)


def svg(background, foreground_fill=None):
    scale = SIZE / 108 * ZOOM
    shift = -(108 * ZOOM - 108) / 2
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{SIZE}" height="{SIZE}" viewBox="0 0 {SIZE} {SIZE}">'
        f'<rect width="{SIZE}" height="{SIZE}" fill="{background}"/>'
        f'<g transform="scale({scale}) translate({shift},{shift})">{paths("ic_launcher_foreground", foreground_fill)}</g></svg>'
    )


def color_json(hex_light, hex_dark=None):
    def comp(h):
        h = h.lstrip("#")
        return {"color": {"color-space": "srgb", "components": {
            "red": f"0x{h[0:2]}", "green": f"0x{h[2:4]}", "blue": f"0x{h[4:6]}", "alpha": "1.000"}}, "idiom": "universal"}
    colors = [comp(hex_light)]
    if hex_dark:
        dark = comp(hex_dark)
        dark["appearances"] = [{"appearance": "luminosity", "value": "dark"}]
        colors.append(dark)
    return {"colors": colors, "info": {"author": "xcode", "version": 1}}


def files():
    tokens = json.loads(TOKENS.read_text(encoding="utf-8"))
    ink = tokens["color"]["illustration"]["ink"]["$value"]
    primary_light = tokens["color"]["light"]["primary"]["$value"]
    primary_dark = tokens["color"]["dark"]["primary"]["$value"]
    icon = {
        "images": [
            {"filename": "icon.png", "idiom": "universal", "platform": "ios", "size": "1024x1024"},
            {"appearances": [{"appearance": "luminosity", "value": "dark"}], "filename": "icon-dark.png",
             "idiom": "universal", "platform": "ios", "size": "1024x1024"},
            {"appearances": [{"appearance": "luminosity", "value": "tinted"}], "filename": "icon-tinted.png",
             "idiom": "universal", "platform": "ios", "size": "1024x1024"},
        ],
        "info": {"author": "xcode", "version": 1},
    }
    dump = lambda d: json.dumps(d, indent=2, sort_keys=True) + "\n"
    return {
        OUT / "Contents.json": dump({"info": {"author": "xcode", "version": 1}}),
        OUT / "AppIcon.appiconset/Contents.json": dump(icon),
        OUT / "AccentColor.colorset/Contents.json": dump(color_json(primary_light, primary_dark)),
        OUT / "LaunchBackground.colorset/Contents.json": dump(color_json(ink)),
    }, {
        OUT / "AppIcon.appiconset/icon.png": svg(ink),
        OUT / "AppIcon.appiconset/icon-dark.png": svg("#000000"),
        OUT / "AppIcon.appiconset/icon-tinted.png": svg("#000000", "#FFFFFF"),
    }


def stamp(svgs):
    return hashlib.sha256("".join(svgs[k] for k in sorted(svgs)).encode()).hexdigest()


def main():
    texts, svgs = files()
    marker = OUT / "AppIcon.appiconset/.source-sha256"
    if "--check" in sys.argv:
        stale = [str(p.relative_to(ROOT)) for p, t in texts.items() if not p.exists() or p.read_text() != t]
        stale += [str(p.relative_to(ROOT)) for p in svgs if not p.exists()]
        if not marker.exists() or marker.read_text().strip() != stamp(svgs):
            stale.append(str(marker.relative_to(ROOT)))
        if stale:
            print("Asset catalog out of date: python3 scripts/ios/generate_assets.py", *stale)
            sys.exit(1)
        print("Asset catalog up to date")
        return
    import cairosvg

    for path, text in texts.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)
    for path, drawing in svgs.items():
        cairosvg.svg2png(bytestring=drawing.encode(), write_to=str(path), output_width=SIZE, output_height=SIZE)
    marker.write_text(stamp(svgs) + "\n")
    print(f"{OUT.relative_to(ROOT)} written")


if __name__ == "__main__":
    main()
