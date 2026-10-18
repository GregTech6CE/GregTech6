"""Print the GT6 form flags of the materials whose flags the port currently cannot reach.

Companion to ``tools/report_material_form_lookup.py``: that tool finds the materials (joined by GT6
id), this one prints what they would gain, so the effect of keying ``MaterialForms`` by id can be
judged before switching it on.

Usage:  python tools/report_material_form_lookup.py > /dev/null   # find them
        python tools/report_material_form_flags.py
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import collections
import pathlib
import re

GT6_MT = pathlib.Path(
    r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java\gregapi\data\MT.java")
PORT_MATERIALS = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/content/material"))
FORMS = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/data/generated/MaterialForms.java"))

GT6_DECL = re.compile(r"^\s*(\w+)\s*=\s*\w+\(\s*(\d+)\s*,\s*\"([^\"]+)\"", re.M)
PORT_DECL = re.compile(r"GTMaterial\s+(\w+)\s*=\s*\w+\(\s*(\d+)\s*,\s*\"([^\"]+)\"", re.M)


def normalize(name: str) -> str:
    return "".join(c for c in name.lower() if c.isalnum())


def main() -> None:
    gt6 = {int(i): (f, d) for f, i, d in GT6_DECL.findall(
        GT6_MT.read_text(encoding="utf-8", errors="replace"))}
    port = {}
    for path in PORT_MATERIALS.rglob("*.java"):
        for name, port_id, display in PORT_DECL.findall(path.read_text(encoding="utf-8", errors="replace")):
            port.setdefault(int(port_id), (name, display))

    table = {}
    for line in re.findall(r'^\s*"([a-z0-9]+)\|([A-Z_,]*)"', FORMS.read_text(encoding="utf-8"), re.M):
        table[line[0]] = set(line[1].split(",")) if line[1] else set()

    gained: collections.Counter[str] = collections.Counter()
    rows = []
    for gt_id, (field, gt6_display) in gt6.items():
        entry = port.get(gt_id)
        if entry is None:
            continue
        port_name, display = entry
        if normalize(display) in table or normalize(port_name) in table:
            continue
        flags = table.get(field.lower(), set()) or table.get(normalize(gt6_display), set())
        if not flags:
            continue
        rows.append((gt_id, port_name, field, sorted(flags)))
        for flag in flags:
            gained[flag] += 1

    print(f"materials whose flags the port cannot reach: {len(rows)}")
    print("--- flags they would gain (material count per flag) ---")
    for flag, count in gained.most_common():
        print(f"  {flag:20} {count}")
    print("--- per material ---")
    for gt_id, port_name, field, flags in sorted(rows):
        print(f"  id {gt_id:5}  {port_name:24} field {field:12} {','.join(flags)}")


if __name__ == "__main__":
    main()
