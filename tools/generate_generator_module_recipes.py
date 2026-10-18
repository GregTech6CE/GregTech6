"""Generate the three generator-module recipes (GT6 MultiItemRandomTools:421-426).

GT6's generator modules are recipe *keys*: the Extruder rows for stone, basalt and blackstone name
them as the special slot item (Loader_Recipes_Extruder:44-151), so the modules have to exist and be
craftable before that whole machine family is reachable in survival. The items themselves were
already registered (GTMultiItemsGen "randomtools"), but they had no recipe.

  stone      "CPC" / "LMW" / "COC"   MultiItemRandomTools:424
             M machine casing (SteelGalvanized), O extruder shape (block), C OD_CIRCUITS[4],
             P any vanilla piston (GT6 OD.craftingPiston), L a 1000 mB lava container,
             W a 1000 mB water container
  basalt     "S" / "M" / "I"         MultiItemRandomTools:425   (soul sand / stone module / packed ice)
  blackstone "S" / "M" / "O"         MultiItemRandomTools:426   (soul sand / stone module / obsidian)

Narrowings, both recorded here rather than silently dropped:
  * GT6's OD.craftingPiston covers the vanilla pistons plus modded ones; the port lists the two
    vanilla pistons.
Finite container ingredients accept and drain 1000 mB from buckets, cells and capsules.

Run: python tools/generate_generator_module_recipes.py
"""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
# recipe_keys/, not components/ or extruder_shapes/: ElectronicsTests counts the component crafting
# recipes (84) and ManufacturingTests owns extruder_shapes/ for the 78 molds, so the generator modules
# — which are recipe keys rather than components — get their own directory.
OUT = ROOT / "core/src/main/resources/data/gt6ce/recipes/recipe_keys"

# GT6 OD_CIRCUITS[4]: the cumulative gt:circuitN chain (LoaderOreDictReRegistrations:375-383).
CIRCUITS = {"tag": "gt6ce:circuits_tier_4_plus"}


def recipe(result: str, source: str, pattern: list, key: dict) -> dict:
    return {
        "type": "gt6ce:tool_shaped",
        "allow_mirror": False,
        "_comment": f"GT6 {source}",
        "pattern": pattern,
        "key": key,
        "result": {"item": f"gt6ce:{result}"},
    }


RECIPES = {
    "stone_generator_module": recipe(
        "stone_generator_module", "MultiItemRandomTools:424", ["CPC", "LMW", "COC"],
        {"C": CIRCUITS,
         "P": [{"item": "minecraft:piston"}, {"item": "minecraft:sticky_piston"}],
         "M": {"item": "gt6ce:casing_machine_steelgalvanized"},
         "O": {"item": "gt6ce:extruder_shape_block"},
         "L": {"type": "gt6ce:finite_fluid_container_1000", "fluid": "Lava"},
         "W": {"type": "gt6ce:finite_fluid_container_1000", "fluid": "Water"}}),
    "basalt_generator_module": recipe(
        "basalt_generator_module", "MultiItemRandomTools:425", ["S", "M", "I"],
        {"S": {"item": "minecraft:soul_sand"},
         "M": {"item": "gt6ce:stone_generator_module"},
         "I": {"item": "minecraft:packed_ice"}}),
    "blackstone_generator_module": recipe(
        "blackstone_generator_module", "MultiItemRandomTools:426", ["S", "M", "O"],
        {"S": {"item": "minecraft:soul_sand"},
         "M": {"item": "gt6ce:stone_generator_module"},
         "O": {"item": "minecraft:obsidian"}}),
}


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, data in RECIPES.items():
        path = OUT / f"{name}.json"
        text = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
        if not path.exists() or path.read_text(encoding="utf-8") != text:
            path.write_text(text, encoding="utf-8")
    print(f"wrote {len(RECIPES)} generator module recipes into {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
