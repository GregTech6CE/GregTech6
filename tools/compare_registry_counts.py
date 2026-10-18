#!/usr/bin/env python3
"""Compare GT6 vs port registry table sizes."""
from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import re
from pathlib import Path

GT6 = Path(r"f:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java\gregapi\data")
PORT = native_path(Path(__file__).resolve().parents[1] / "src/main/java/com/gregtech/gregtech/data")


def count_any(path: Path) -> int:
    return len(re.findall(r"=\s*any\(", path.read_text(encoding="utf-8", errors="ignore")))


def count_moddata(path: Path) -> int:
    return len(re.findall(r"new ModData\(", path.read_text(encoding="utf-8", errors="ignore")))


def count_am(path: Path) -> int:
    text = path.read_text(encoding="utf-8", errors="ignore")
    return len(
        re.findall(
            r"=\s*(?:create|element|metal|diatomic|noblegas|alkali|alkaline|lanthanide|actinide|transmetal|posttrans|metalloid|nonmetal|polyatomic)\(",
            text,
        )
    )


def count_material_lines(path: Path) -> int:
    text = path.read_text(encoding="utf-8", errors="ignore")
    return len(
        re.findall(
            r"=\s*(?:\w+\.)?(?:metal|element|alloy|ore|gem|dust|gas|wood|stone|create|elec|cent|clay)\(",
            text,
        )
    )


def skipped_gt6_materials() -> int:
    src = GT6 / "MT.java"
    skip = {"tier", "unused", "deprecated", "invalid", "unknown", "gem_aa", "handle", "brick",
            "mix", "mixdust", "dcmp", "cent", "elec", "hexorium", "clay"}
    count = 0
    for line in src.read_text(encoding="utf-8", errors="ignore").splitlines():
        m = re.search(r"=\s*(\w+)\s*\(", line)
        if m and m.group(1) in skip:
            count += 1
    return count


def main() -> None:
    gen = Path(__file__).resolve().parent / "generated/materials-source.java.txt"
    print("=== GT6 vs Port registry counts ===")
    print(f"ANY (unification):  GT6={count_any(GT6 / 'ANY.java'):3d}  Port={count_any(PORT / 'MaterialGroups.java'):3d}")
    print(f"MD (mod IDs):       GT6={count_moddata(GT6 / 'MD.java'):3d}  Port={count_moddata(PORT / 'ModReferences.java'):3d}")
    print(f"AM (anti-matter):   GT6={count_am(GT6 / 'AM.java'):3d}  Port={count_am(PORT / 'AntimatterMaterials.java'):3d}")
    print(f"MT materials:       GT6={count_material_lines(GT6 / 'MT.java'):3d}  Port_MT={count_material_lines(PORT / 'ImportedMaterialData.java'):3d}")
    print(f"GT6Materials.java:  Port_generated={count_material_lines(gen):3d}")
    print(f"GT6 MT skipped by transpiler (cent/elec/clay/...): ~{skipped_gt6_materials()}")


if __name__ == "__main__":
    main()
