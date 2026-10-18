"""Which GT6 materials does the port not declare at all?

Two sources of truth: ``docs/gt6-form-flags.json`` (GT6's declarations, extracted with their ids by
``tools/extract_gt6_form_flags.py``) and the port's own material declarations (scanned across
``src/main/java``, including ``data/ImportedMaterialData`` and the supplemental catalogs). A GT6
material counts as missing when neither its id nor its normalised name appears in the port — matching
by name too avoids reporting the port's own id scheme as a gap.

Usage:  python tools/report_missing_materials.py [--limit N]
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = native_path(ROOT / "src/main/java")
REPORT = ROOT / "docs/gt6-form-flags.json"

PORT_DECL = re.compile(r'(?:GTMaterial|MaterialDefinition)\s+(\w+)\s*=\s*[\w.]+\s*\(\s*(\d{1,5})\s*,\s*"([^"]+)"')
GT6_DECL = re.compile(r"^\s*(\w+)\s*=\s*\w+\(\s*(\d+)\s*,\s*\"([^\"]+)\"", re.M)
GT6_MT = pathlib.Path(
    r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java\gregapi\data\MT.java")
PORT_NAME = re.compile(r'"([A-Za-z][A-Za-z0-9 _\-\']{2,})"')


def normalize(name: str) -> str:
    return "".join(c for c in name.lower() if c.isalnum())


def main() -> None:
    limit = 40
    if "--limit" in sys.argv:
        limit = int(sys.argv[sys.argv.index("--limit") + 1])

    materials = json.loads(REPORT.read_text(encoding="utf-8"))["materials"]
    display = {field: name for field, _id, name in GT6_DECL.findall(
        GT6_MT.read_text(encoding="utf-8", errors="replace"))}
    gt6 = {}
    for field, entry in materials.items():
        for material_id in entry.get("ids", []):
            gt6.setdefault(int(material_id), (field, display.get(field, field), entry["flags"]))

    port_ids: set[int] = set()
    port_names: set[str] = set()
    for path in JAVA.rglob("*.java"):
        text = path.read_text(encoding="utf-8", errors="replace")
        for _field, material_id, display in PORT_DECL.findall(text):
            port_ids.add(int(material_id))
            port_names.add(normalize(display))
        if "material" in path.name.lower():
            for name in PORT_NAME.findall(text):
                port_names.add(normalize(name))

    missing = [(material_id, field, name, flags)
               for material_id, (field, name, flags) in sorted(gt6.items())
               if material_id not in port_ids
               and normalize(name) not in port_names
               and normalize(field) not in port_names]
    print(f"GT6 materials with an id: {len(gt6)}; port ids: {len(port_ids)}; port names: {len(port_names)}")
    print(f"GT6 materials the port does not declare (no id, no name match): {len(missing)}")
    for material_id, field, name, flags in missing[:limit]:
        print(f"  id {material_id:5} {field:24} {name:26} {','.join(flags)[:50]}")


if __name__ == "__main__":
    main()
