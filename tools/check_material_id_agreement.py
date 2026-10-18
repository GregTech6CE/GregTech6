"""Check that the port's material ids really mean the same material as GT6's.

``MaterialForms`` is looked up by GT6 material id first, so a port material carrying an id that GT6
gives to a *different* material would silently inherit the wrong forms. This joins both sides by id
and prints the pairs whose names do not look alike, for manual review.

Usage:  python tools/check_material_id_agreement.py
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import pathlib
import re

GT6_MT = pathlib.Path(
    r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java\gregapi\data\MT.java")
PORT_MATERIALS = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/content/material"))

GT6_DECL = re.compile(r"^\s*(\w+)\s*=\s*\w+\(\s*(\d+)\s*,\s*\"([^\"]+)\"", re.M)
PORT_DECL = re.compile(r"GTMaterial\s+(\w+)\s*=\s*\w+\(\s*(\d+)\s*,\s*\"([^\"]+)\"", re.M)


def normalize(name: str) -> str:
    return "".join(c for c in name.lower() if c.isalnum())


def alike(a: str, b: str) -> bool:
    return a == b or a.startswith(b) or b.startswith(a)


def main() -> None:
    gt6 = {int(i): (f, d) for f, i, d in GT6_DECL.findall(
        GT6_MT.read_text(encoding="utf-8", errors="replace"))}
    port = {}
    for path in sorted(PORT_MATERIALS.rglob("*.java")):
        for name, port_id, display in PORT_DECL.findall(path.read_text(encoding="utf-8", errors="replace")):
            port.setdefault(int(port_id), (name, display, path.name))

    joined = mismatched = 0
    for gt_id, (port_name, display, path_name) in sorted(port.items()):
        declaration = gt6.get(gt_id)
        if declaration is None:
            continue
        joined += 1
        field, gt6_display = declaration
        if alike(normalize(display), normalize(gt6_display)) or alike(normalize(port_name), normalize(gt6_display)) \
                or alike(normalize(display), normalize(field)):
            continue
        mismatched += 1
        print(f"  id {gt_id:5}  port {port_name:24} {display:24} GT6 {field:14} {gt6_display}  [{path_name}]")
    print(f"joined by id: {joined}; names that do not look alike: {mismatched}")


if __name__ == "__main__":
    main()
