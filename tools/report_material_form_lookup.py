"""Measure which port materials can reach GT6's item-generator flags.

``MaterialPrefix`` consults ``MaterialForms`` (GT6's per-material form sets, imported from
``TD.java``/``MT.java``) through ``MaterialForms.has(material, flag)``. The table is keyed by GT6
material id and, for unambiguous names, by the normalised declaration name; the runtime lookup tries
the material's id first and its name second. This tool joins GT6's declarations (field name + numeric
id + display name) with the port's material declarations (id + display name) and prints:

  * how many port materials resolve their flags at all,
  * how many would be lost without the id key (name lookup only),
  * the materials in that set, with the GT6 field names that differ from the port's naming.

Usage:  python tools/report_material_form_lookup.py [--limit N]
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import pathlib
import re
import sys

GT6_MT = pathlib.Path(
    r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java\gregapi\data\MT.java")
PORT_MATERIALS = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/content/material"))
FORMS = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/data/generated/MaterialForms.java"))

GT6_DECL = re.compile(r"^\s*(\w+)\s*=\s*\w+\(\s*(\d+)\s*,\s*\"([^\"]+)\"", re.M)
PORT_DECL = re.compile(r"GTMaterial\s+(\w+)\s*=\s*\w+\(\s*(\d+)\s*,\s*\"([^\"]+)\"", re.M)
ROW = re.compile(r'^\s+"([^"|]+)\|([A-Z_,]*)"', re.M)


def normalize(name: str) -> str:
    return "".join(c for c in name.lower() if c.isalnum())


def main() -> None:
    limit = 40
    if "--limit" in sys.argv:
        limit = int(sys.argv[sys.argv.index("--limit") + 1])

    gt6 = {}
    for field, gt_id, display in GT6_DECL.findall(GT6_MT.read_text(encoding="utf-8", errors="replace")):
        gt6.setdefault(int(gt_id), (field, display))

    port = {}
    for path in PORT_MATERIALS.rglob("*.java"):
        for name, port_id, display in PORT_DECL.findall(path.read_text(encoding="utf-8", errors="replace")):
            port.setdefault(int(port_id), (name, display, path.name))

    rows = ROW.findall(FORMS.read_text(encoding="utf-8"))
    id_keys = {key for key, _flags in rows if key.isdigit()}
    name_keys = {key for key, _flags in rows if not key.isdigit()}

    joined = by_id = by_name = lost = 0
    lost_rows = []
    for gt_id, (port_name, display, _file) in port.items():
        declaration = gt6.get(gt_id)
        if declaration is None:
            continue
        joined += 1
        field, _gt6_display = declaration
        if str(gt_id) in id_keys:
            by_id += 1
        if normalize(display) in name_keys or normalize(port_name) in name_keys:
            by_name += 1
        if str(gt_id) not in id_keys and normalize(display) not in name_keys \
                and normalize(port_name) not in name_keys and field.lower() in name_keys:
            lost += 1
            lost_rows.append((gt_id, port_name, display, field))

    print(f"GT6 declarations joined: {joined} of {len(port)} port materials")
    print(f"flags reachable through the GT6 id: {by_id}")
    print(f"flags reachable through the material name: {by_name}")
    print(f"still unreachable (GT6 declares them under another field name): {lost}")
    for gt_id, port_name, display, field in sorted(lost_rows)[:limit]:
        print(f"  id {gt_id:5}  port {port_name:24} {display:26} GT6 field {field}")


if __name__ == "__main__":
    main()
