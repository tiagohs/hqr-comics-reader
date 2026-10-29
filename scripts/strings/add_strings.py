"""Adds or replaces keys in the three languages. Input: JSON {key: [en, pt, es]} on stdin."""
import html, json, pathlib, re, sys
root = pathlib.Path(__file__).resolve().parents[2] / "shared/ui/src/commonMain/composeResources"
data = json.load(sys.stdin)
for folder, i in (("values", 0), ("values-pt", 1), ("values-es", 2)):
    path = root / folder / "strings.xml"
    text = path.read_text()
    for key, values in data.items():
        value = html.escape(values[i], quote=False).replace("'", "\\'")
        line = f'    <string name="{key}">{value}</string>'
        pattern = re.compile(rf'^    <string name="{re.escape(key)}">.*</string>$', re.M)
        text = pattern.sub(line, text) if pattern.search(text) else text.replace("</resources>", line + "\n</resources>")
    path.write_text(text)
print(f"{len(data)} keys")
