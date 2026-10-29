"""RNF-14: every key exists in en, pt and es (shared/ui composeResources)."""
import re, sys, pathlib
root = pathlib.Path(__file__).resolve().parents[2] / "shared/ui/src/commonMain/composeResources"
keys = {d.name: set(re.findall(r'<string name="([^"]+)"', (d / "strings.xml").read_text())) for d in root.glob("values*") if (d / "strings.xml").exists()}
base = keys["values"]
bad = {name: sorted(base ^ k) for name, k in keys.items() if k != base}
if bad:
    print("Missing or extra keys:", bad)
    sys.exit(1)
print(f"{len(base)} keys in {len(keys)} languages")
