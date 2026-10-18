"""Sanity-check the regenerated MaterialForms table (id rows, name rows, sample materials).

Usage:  python tools/check_material_forms.py
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import pathlib
import re

FORMS = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/data/generated/MaterialForms.java"))
ROW = re.compile(r'^\s+"([^"|]+)\|([A-Z_,]*)"', re.M)


def main() -> None:
    text = FORMS.read_text(encoding="utf-8")
    rows = ROW.findall(text)
    ids = {int(k): v for k, v in rows if k.isdigit()}
    names = {k: v for k, v in rows if not k.isdigit()}
    print(f"rows {len(rows)}: id keys {len(ids)}, name keys {len(names)}")
    for material_id in (1260, 1740, 790, 9369, 9311, 9079):
        flags = ids.get(material_id, "")
        print(f"  id {material_id:5} {flags}")
    print("--- sample name keys ---")
    for key in ("trinium", "naquadah", "gold", "co", "clay", "metal", "goldwood", "mossy"):
        print(f"  {key:10} {names.get(key, '(absent)')}")


if __name__ == "__main__":
    main()
