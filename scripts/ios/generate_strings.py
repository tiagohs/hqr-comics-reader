#!/usr/bin/env python3
"""App texts for iOS: the shared screens' strings.xml (Compose resources) -> ios/HQR/Resources/Localizable.xcstrings.

The strings.xml of shared/ui are the only source (ADR-017: one text per idea in the three languages). Keys are the Android names;
`%1$s` becomes `%1$@` and `%1$d` becomes `%1$lld`; plurals become plural variations. `--check` fails when the catalog
is not up to date.
"""

import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "shared/ui/src/commonMain/composeResources"
OUT = ROOT / "ios/HQR/Resources/Localizable.xcstrings"
LANGS = {"en": "values", "pt-BR": "values-pt", "es": "values-es"}


def text_of(node):
    raw = "".join(node.itertext())
    if raw.startswith('"') and raw.endswith('"'):
        raw = raw[1:-1]
    raw = raw.replace("\\'", "'").replace('\\"', '"').replace("\\n", "\n").replace("\\@", "@").replace("\\?", "?")
    raw = re.sub(r"%(\d+)\$s", r"%\1$@", raw)
    raw = re.sub(r"%(\d+)\$d", r"%\1$lld", raw)
    raw = raw.replace("%s", "%@").replace("%d", "%lld")
    return raw


def read(folder):
    tree = ET.parse(RES / folder / "strings.xml").getroot()
    strings, plurals, fixed = {}, {}, set()
    for node in tree:
        name = node.get("name")
        if node.tag == "string":
            strings[name] = text_of(node)
            if node.get("translatable") == "false":
                fixed.add(name)
        elif node.tag == "plurals":
            plurals[name] = {item.get("quantity"): text_of(item) for item in node.findall("item")}
    return strings, plurals, fixed


def unit(value):
    return {"stringUnit": {"state": "translated", "value": value}}


def build():
    data = {lang: read(folder) for lang, folder in LANGS.items()}
    base_strings, base_plurals, fixed = data["en"]
    out = {}
    for name in sorted(base_strings):
        entry = {"extractionState": "manual", "localizations": {}}
        if name in fixed:
            entry["shouldTranslate"] = False
            entry["localizations"]["en"] = unit(base_strings[name])
        else:
            for lang, (strings, _, _) in data.items():
                if name in strings:
                    entry["localizations"][lang] = unit(strings[name])
        out[name] = entry
    for name in sorted(base_plurals):
        entry = {"extractionState": "manual", "localizations": {}}
        for lang, (_, plurals, _) in data.items():
            forms = plurals.get(name)
            if forms:
                entry["localizations"][lang] = {
                    "variations": {"plural": {q: unit(v) for q, v in forms.items()}}
                }
        out[name] = entry
    catalog = {"sourceLanguage": "en", "strings": out, "version": "1.0"}
    return json.dumps(catalog, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def main():
    text = build()
    if "--check" in sys.argv:
        if not OUT.exists() or OUT.read_text(encoding="utf-8") != text:
            print("Localizable.xcstrings is out of date: python3 scripts/ios/generate_strings.py")
            sys.exit(1)
        print("Localizable.xcstrings up to date")
        return
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(text, encoding="utf-8")
    print(f"{OUT.relative_to(ROOT)}: {len(json.loads(text)['strings'])} keys")


if __name__ == "__main__":
    main()
