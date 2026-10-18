"""Debug: reconcile the material-id sets of GT6 and the port."""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import pathlib
import re

import importlib.util

spec = importlib.util.spec_from_file_location(
    "mm", pathlib.Path("tools/report_missing_materials.py"))
mod = importlib.util.module_from_spec(spec)

GT6_MT = pathlib.Path(
    r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java\gregapi\data\MT.java")
PORT_MATERIALS = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/content/material"))
FORMS = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/data/generated/MaterialForms.java"))
GT6_DECL = re.compile(r"^\s*(\w+)\s*=\s*\w+\(\s*(\d+)\s*,\s*\"([^\"]+)\"", re.M)
PORT_DECL = re.compile(r"GTMaterial\s+(\w+)\s*=\s*(\w+)\(\s*(\d+)\s*,\s*\"([^\"]+)\"", re.M)
ROW = re.compile(r'^\s+"(\d+)\|([A-Z_,]*)"', re.M)

gt6 = {}
for field, gt_id, display in GT6_DECL.findall(GT6_MT.read_text(encoding="utf-8", errors="replace")):
    gt6.setdefault(int(gt_id), (field, display))
with_flags = {int(m.group(1)): m.group(2) for m in ROW.finditer(FORMS.read_text(encoding="utf-8"))}

port = {}
for path in PORT_MATERIALS.rglob("*.java"):
    for name, factory, port_id, display in PORT_DECL.findall(path.read_text(encoding="utf-8", errors="replace")):
        port.setdefault(int(port_id), (name, factory, display))

print("gt6 declarations:", len(gt6), "with flags:", len(with_flags), "port ids:", len(port))
print("port has 8421:", port.get(8421))
inter = set(with_flags) & set(port)
print("intersection:", len(inter), "gt6-only:", len(set(with_flags) - set(port)))
only = sorted(set(with_flags) - set(port))
print("gt6 ids the port lacks:", len(only))
for material_id in only[:30]:
    field, display = gt6.get(material_id, ("?", "?"))
    print(f"  {material_id:5} {field:22} {display}")
