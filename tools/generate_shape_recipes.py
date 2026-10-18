"""Write the shape-item crafting recipes the port was missing (food molds and slicer blades).

GT6 registers these items with their recipe in `MultiItemTechnological.java`:
  * food grade molds: `:336` and `:344-348`
  * slicer blades:    `:364` and `:374-380`
Both families exist in the port as items but had no recipe at all (see
`docs/items-without-recipes.json`). Keys follow the original: `h` hard hammer, `f` file, `s` saw,
`x` wire cutter, `P`/`B`/`R` stainless steel plateDouble/plateTiny/stick.

Usage: python tools/generate_shape_recipes.py [--check]
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import argparse
import json
import os

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
OUT_DIR = os.path.join(ROOT, str(native_path("src")), "main", "resources", "data", "gt6ce", "recipes", "extruder_shapes")

HAMMER = {"item": "gt6ce:tool_hammer"}
FILE = {"item": "gt6ce:tool_file"}
SAW = {"item": "gt6ce:tool_saw"}
WIRE_CUTTER = {"item": "gt6ce:tool_wire_cutter"}
# OP.plateDouble / plateTiny / stick / ring of MT.StainlessSteel
PLATE_DOUBLE = {"item": "gt6ce:plate_double_stainlesssteel"}
PLATE_TINY = {"item": "gt6ce:plate_tiny_stainlesssteel"}
STICK = {"tag": "forge:rods/stainless_steel"}
RING = {"tag": "forge:rings/stainless_steel"}

# MultiItemTechnological:336 - "hf" / "xP"
FOODMOLD_EMPTY = (["hf", "xP"], {"h": HAMMER, "f": FILE, "x": WIRE_CUTTER, "P": PLATE_DOUBLE},
                  "foodmold_shape_empty")
# MultiItemTechnological:344-348 - one hammer mark around the empty mold
FOODMOLDS = {
    "foodmold_shape_bun": ["h  ", " P ", "   "],
    "foodmold_shape_bread": [" h ", " P ", "   "],
    "foodmold_shape_baguette": ["  h", " P ", "   "],
    "foodmold_shape_cylinder": ["   ", " Ph", "   "],
    "foodmold_shape_toast": ["   ", " P ", "  h"],
}
# MultiItemTechnological:364 - " R " / "RhR" / " R "
SLICER_EMPTY = ([" R ", "RhR", " R "], {"R": STICK, "h": HAMMER}, "slicer_shape_empty")
# MultiItemTechnological:374-380, keys: O = the empty frame, B = plateTiny, R = ring
SLICERS = {
    "slicer_shape_flat": (["B f", "BO ", "B s"], False),
    "slicer_shape_grid": ([" Bf", "BOB", " Bs"], False),
    "slicer_shape_eights": (["B B", "s f", "BOB"], False),
    "slicer_shape_eights_hollow": (["B B", "sRf", "BOB"], True),
    "slicer_shape_split": ([" Of", "BBB", "  s"], False),
    "slicer_shape_quaters": (["fB ", "B s", " O "], False),
    "slicer_shape_quaters_hollow": (["fB ", "BRs", " O "], True),
}


def shaped(pattern: list[str], key: dict, result: str) -> dict:
    # allow_mirror stays off, exactly like the port's 64 extruder shapes: GT6's CR.DEF_REV adds a
    # mirrored variant, which makes some of these patterns (food mold bun vs baguette) ambiguous, and
    # ManufacturingTests requires every mold to have exactly one orientation.
    return {"type": "gt6ce:tool_shaped", "allow_mirror": False, "pattern": pattern, "key": key,
            "result": {"item": f"gt6ce:{result}", "count": 1}}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    recipes: dict[str, dict] = {}
    pattern, key, result = FOODMOLD_EMPTY
    recipes[result] = shaped(pattern, key, result)
    for result, pattern in FOODMOLDS.items():
        recipes[result] = shaped(pattern, {"h": HAMMER, "P": {"item": "gt6ce:foodmold_shape_empty"}}, result)
    pattern, key, result = SLICER_EMPTY
    recipes[result] = shaped(pattern, key, result)
    for result, (pattern, hollow) in SLICERS.items():
        key = {"O": {"item": "gt6ce:slicer_shape_empty"}, "B": PLATE_TINY, "f": FILE, "s": SAW}
        if hollow:
            key["R"] = RING
        recipes[result] = shaped(pattern, key, result)

    written, skipped = [], []
    for name, recipe in recipes.items():
        path = os.path.join(OUT_DIR, name + ".json")
        if os.path.exists(path):
            skipped.append(name)
            continue
        if not args.check:
            with open(path, "w", encoding="utf-8", newline="\n") as handle:
                json.dump(recipe, handle, indent=2)
                handle.write("\n")
        written.append(name)
    print(f"{'would write' if args.check else 'wrote'} {len(written)} shape recipes: {', '.join(written)}")
    if skipped:
        print(f"already present ({len(skipped)}): {', '.join(skipped)}")


if __name__ == "__main__":
    main()
