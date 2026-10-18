"""Write GT6's wooden-pipe crafting recipes for the port's wood pipes.

GT6's only pipe recipes that live in the crafting table are the five wooden ones
(`Loader_MultiTileEntities:1887-1891`): a saw and a soft hammer shape a slab, planks or a beam into a
pipe of the matching size. Metal pipes come from the Welder (`Loader_Recipes_Handlers:324-343`, ported
in `content/recipe/PipeRecipes.java`) — the original has no hand recipe for them.

GT6 tool keys (CR.java:195-216): `s` = saw, `r` = soft hammer.

Usage: python tools/generate_wooden_pipe_recipes.py [--check]
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
OUT_DIR = os.path.join(ROOT, str(native_path("src")), "main", "resources", "data", "gt6ce", "recipes", "pipes")

SAW = {"item": "gt6ce:tool_saw"}
SOFT_HAMMER = {"item": "gt6ce:tool_soft_hammer"}
# tags MaterialTagPack publishes: vanilla planks/slabs plus the port's own wood species
PLANKS = {"tag": "gt6ce:wooden_planks"}
WOODEN_SLABS = {"tag": "gt6ce:wooden_slabs"}
BEAM = {"tag": "gt6ce:wooden_beams"}

# Loader_MultiTileEntities:1887-1891 - pattern, wood form, result pipe size
RECIPES = {
    "pipe_tiny_wood": (["  s", " W ", "r  "], WOODEN_SLABS),
    "pipe_small_wood": (["  s", " W ", "r  "], PLANKS),
    "pipe_medium_wood": (["  s", "WWW", "r  "], PLANKS),
    "pipe_large_wood": (["WWs", "W W", "rWW"], PLANKS),
    "pipe_huge_wood": (["  s", "W W", "r  "], BEAM),
}


def recipe(pattern: list[str], wood: dict, result: str) -> dict:
    return {"type": "gt6ce:tool_shaped", "pattern": pattern,
            "key": {"s": SAW, "r": SOFT_HAMMER, "W": wood},
            "result": {"item": f"gt6ce:{result}", "count": 1}}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    written, skipped = [], []
    for result, (pattern, wood) in RECIPES.items():
        path = os.path.join(OUT_DIR, result + ".json")
        if os.path.exists(path):
            skipped.append(result)
            continue
        if not args.check:
            os.makedirs(OUT_DIR, exist_ok=True)
            with open(path, "w", encoding="utf-8", newline="\n") as handle:
                json.dump(recipe(pattern, wood, result), handle, indent=2)
                handle.write("\n")
        written.append(result)
    print(f"{'would write' if args.check else 'wrote'} {len(written)} wooden pipe recipes: "
          f"{', '.join(written)}")
    if skipped:
        print(f"already present ({len(skipped)}): {', '.join(skipped)}")
    print("NOTE: metal pipes are Welder-only in GT6 too (Loader_Recipes_Handlers:324-343).")


if __name__ == "__main__":
    main()
