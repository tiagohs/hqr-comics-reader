"""Rebuilds docs/rtm.md from docs/srs.md and docs/use-cases.md, keeping filled downstream columns."""
import re
from pathlib import Path

DOCS = Path(__file__).resolve().parents[2] / "docs"


def expand(trace: str) -> list[str]:
    ids = []
    for part in trace.rstrip(".").split(", "):
        match = re.match(r"(RN?F)-(\d+)(?: a RN?F-(\d+))?", part.strip())
        if not match:
            continue
        start, end = int(match.group(2)), int(match.group(3) or match.group(2))
        ids += [f"{match.group(1)}-{i:02d}" for i in range(start, end + 1)]
    return ids


def main() -> None:
    srs = (DOCS / "srs.md").read_text()
    use_cases = (DOCS / "use-cases.md").read_text()
    rtm_path = DOCS / "rtm.md"
    previous = rtm_path.read_text() if rtm_path.exists() else ""
    kept = {m.group(1): m.group(0) for m in re.finditer(r"^\| (RN?F-\d+) \|.*$", previous, re.M)}

    trace = {}
    for block in re.split(r"\n### ", use_cases)[1:]:
        uc_id = block.split()[0]
        found = re.search(r"Rastreia:\*\* (.*)", block)
        for req in expand(found.group(1)) if found else []:
            trace.setdefault(req, set()).add(uc_id)

    screens = {}
    ux = (DOCS / "ux-foundations.md").read_text()
    for scr, reqs in re.findall(r"^\| (SCR-[A-Z]+-\d+) \| [^|]+ \| ([^|]+) \|", ux, re.M):
        for req in expand(reqs):
            screens.setdefault(req, []).append(scr)

    arch = (DOCS / "architecture.md").read_text()
    design = {}
    for comp, reqs in re.findall(r"^\| `([^`]+)` \| [^|]+ \| [^|]+ \| ([^|]+) \|$", arch, re.M):
        for req in expand(reqs):
            design.setdefault(req, []).append(comp)
    for adr, body in re.findall(r"### (ADR-\d+)[^\n]*\n(.*?)(?=\n### |\Z)", arch, re.S):
        found_req = re.search(r"Requisitos: (.*)", body)
        for req in expand(found_req.group(1)) if found_req else []:
            design.setdefault(req, []).append(adr)

    plan_path = DOCS / "implementation-plan.md"
    feats = {}
    if plan_path.exists():
        plan = plan_path.read_text()
        for feat, reqs in re.findall(r"^\| (FEAT-\d+) \| [^|]+ \| ([^|]+) \|", plan, re.M):
            for req in re.findall(r"RF-\d+", reqs):
                feats.setdefault(req, []).append(feat)
        found = re.search(r"Estados de tela \((RF-\d+)\).*?\n.*?Estado que sobrevive \((RF-\d+)", plan, re.S)
        for req in re.findall(r"RF-\d+", found.group(0)) if found else []:
            feats.setdefault(req, []).append("Fundações")

    # Tests are named after what they cover (RF38_…, RF22_RF23_…): the column lists the test classes.
    tests = {}
    root = DOCS.parent
    for path in root.glob("**/src/*Test/**/*.kt"):
        if "/build/" in str(path):
            continue
        text = path.read_text()
        for name in re.findall(r"fun `?((?:RN?F\d+_)+)", text):
            for num in re.findall(r"(RN?F)(\d+)_", name):
                tests.setdefault(f"{num[0]}-{int(num[1]):02d}", set()).add(path.stem)

    def tested(req: str, fallback: str) -> str:
        found = sorted(tests.get(req, []))
        return ", ".join(found) if found else fallback

    def ucs(req: str) -> str:
        return ", ".join(sorted(trace.get(req, []))) or "—"

    def downstream(req: str, cols: int) -> list[str]:
        cells = [c.strip() for c in kept.get(req, "").strip("|").split("|")]
        tail = cells[-cols:] if len(cells) > cols else []
        return tail if tail else ["—"] * cols

    lines = [
        "# Matriz de rastreabilidade (RTM): HQR", "",
        "> Gerada por `scripts/docs/build_rtm.py` · Colunas: UC (`docs/use-cases.md`) · Componente e ADR (`docs/architecture.md`) · Tela (`docs/ux-foundations.md`) · FEAT (`docs/implementation-plan.md`) · Teste.", "",
        "## Requisitos funcionais", "",
        "| ID | Prior. | UC | Componente / ADR | Tela | FEAT | Teste |", "| :-- | :-- | :-- | :-- | :-- | :-- | :-- |",
    ]
    for req, prio in re.findall(r"^\| (RF-\d+) \| .*? \| (Must|Should|Could) \|", srs, re.M):
        tail = downstream(req, 4)
        comp = ", ".join(dict.fromkeys(design.get(req, []))) or "—"
        scr = ", ".join(dict.fromkeys(screens.get(req, []))) or "—"
        feat = ", ".join(dict.fromkeys(feats.get(req, []))) or tail[2]
        lines.append(f"| {req} | {prio} | {ucs(req)} | {comp} | {scr} | {feat} | {tested(req, tail[3])} |")
    lines += ["", "## Requisitos não funcionais", "", "| ID | UC | ADR | Verificação | FEAT |", "| :-- | :-- | :-- | :-- | :-- |"]
    for req in re.findall(r"^\| (RNF-\d+) \|", srs, re.M):
        tail = downstream(req, 3)
        adrs = ", ".join(x for x in dict.fromkeys(design.get(req, [])) if x.startswith("ADR")) or "—"
        lines.append(f"| {req} | {ucs(req) if req in trace else 'transversal'} | {adrs} | {tail[1]} | {tail[2]} |")
    rtm_path.write_text("\n".join(lines) + "\n")
    missing = [r for r in re.findall(r"^\| (RF-\d+) \|", srs, re.M) if r not in trace]
    print("RF without use case:", missing or "none")
    no_screen = [r for r in re.findall(r"^\| (RF-\d+) \|", srs, re.M) if r not in screens]
    print("RF without screen:", no_screen or "none")
    print("RF with tests:", sum(1 for r in re.findall(r"^\| (RF-\d+) \|", srs, re.M) if r in tests))


if __name__ == "__main__":
    main()
