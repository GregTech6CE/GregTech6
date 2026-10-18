"""Audit GT6's bedrock-ore registrations against the port's ore blocks.

GT6 places bedrock ores through one meta-carrying block (`BlocksGT.oreBedrock`), so ANY material
can be a bedrock ore. The port models a bedrock ore as `ore_<material>` with the background stone
set to bedrock, which only exists for materials carrying the ORE form flag -- this prints which
GT6 bedrock ores therefore cannot be placed yet.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import glob
import io
import os
import re

GT6 = r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java\gregtech\loaders\b\Loader_Worldgen.java"
MATERIALS = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "content", "material")

# GT6 name -> port material field name (the port keeps GT6's internal names for ores).
ALIASES = {
    "Au": "Gold", "Cu": "Copper", "Fe2O3": "Iron(III) Oxide", "V2O5": "Vanadium Pentoxide",
    "KCl": "Sylvite", "NaCl": "Salt", "KIO3": "Potassium Iodate", "Niter": "Niter",
    "Nq": "Naquadah", "Ke": "Trinium", "Mn": "Manganese", "MnCO3": "Magnesium Carbonate",
    "MgCO3": "Magnesium Carbonate", "Ti": "Titanium",
}


def port_ore_materials():
    """Material field names that the port gives an `ore_*` block (ORE form flag)."""
    ores = set()
    for path in glob.glob(os.path.join(MATERIALS, "**", "*.java"), recursive=True):
        text = io.open(path, encoding="utf-8", errors="replace").read()
        for match in re.finditer(
                r'(?:public static final GTMaterial|static final GTMaterial)\s+(\w+)\s*=\s*'
                r'(ore|gem|cent|dust|metal|ingot|compound|element|stone)?\(', text):
            name, factory = match.group(1), match.group(2)
            if factory == "ore":
                ores.add(name)
        # materials that opt in through MaterialProperty.ORE explicitly
        for match in re.finditer(
                r'(?:public static final GTMaterial|static final GTMaterial)\s+(\w+)\s*=\s*[^;]*'
                r'MaterialProperty\.ORE', text):
            ores.add(match.group(1))
    return ores


def bedrock_registrations():
    text = io.open(GT6, encoding="utf-8", errors="replace").read()
    flat = re.sub(r"\s+", " ", text)
    out = []
    for match in re.finditer(r"new WorldgenOresBedrock\((.*?)\)\s*;", flat):
        args = [a.strip() for a in match.group(1).split(",")]
        if len(args) < 5:
            continue
        material = args[4].replace("MT.OREMATS.", "").replace("MT.", "").strip()
        dims = [a for a in args[5:] if a.startswith("GEN_")]
        chance = args[3]
        out.append((args[0].strip('"'), material, chance, dims))
    return out


def main():
    ores = port_ore_materials()
    print("port materials with an ore block: %d" % len(ores))
    ok, missing = [], []
    for name, material, chance, dims in bedrock_registrations():
        field = material.split(".")[-1]
        field = ALIASES.get(field, field)
        if field in ores or material in ores:
            ok.append((name, material, dims))
        else:
            missing.append((name, material, chance, dims))
    print("\nplaceable via ore_<material>: %d" % len(ok))
    print("NOT placeable (no ore block): %d" % len(missing))
    for name, material, chance, dims in missing:
        print("   %-28s %-26s chance=%-7s %s" % (name, material, chance, ",".join(dims)))


if __name__ == "__main__":
    main()
