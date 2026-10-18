"""Generate the crafting recipes of GT6's ropes and explosives.

Source: the pattern arguments of GT6's registrations, read straight off
``Loader_MultiTileEntities:2086-2091`` (ropes) and ``:2235`` (boomstick); every row is
``CR.DEF_REV_NCC``, i.e. it wears the tools and does **not** mirror (``CR.java:161-163``).

  rope_silk   " S"  / "SS"  / "Sq"    S = string (GT6 Items.string),          q = scissors
  rope_grass  " GG" / "GGG" / "GGq"   G = dry grass (GT6 OD.itemGrassDry),    q = scissors
  rope_vine   " V"  / "VV"  / "Vb"    V = vine (GT6 Blocks.vine),             b = blade (GT6 swords)
  rope_steel  " P"  / "PP"  / "Px"    P = steel fine wire (GT6 OP.wireFine),  x = wire cutter
  boomstick   " S " / "PGP" / "DGD"   S = string, P = paper, G = gunpowder dust,
                                      D = SiO2 dust

Two of GT6's rope rows are deliberately *not* generated, with the original's own reason:
  * "rope" itself needs the ore-dictionary entry ``cropHemp``, which GT6 only fills from the HBM and
    Immersive Engineering compat loaders (``LoaderItemData:840-841``) - without those mods the
    original's own recipe is dead as well.
  * "rope_plastic" needs a fine wire of Plastic; the port's ``wireFine`` condition
    (``MaterialPrefix``: GENERATE_WIRE or PARTS+INGOT or the WIRES form flag) does not hold for
    Plastic, so the item does not exist yet (a material-form gap, not a recipe gap).

The dynamite itself comes from the Press, not from this file: GT6's rows for it are already
transpiled (``GTOtherGen``) and light up as soon as the block items exist.

Usage:  python tools/generate_rope_dynamite_recipes.py
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = native_path(ROOT / "src/main/resources/data/gregtech/recipes/tool_blocks")


def item(id: str) -> dict:
    return {"item": id}


def recipe(comment: str, pattern: list[str], key: dict, result: str) -> dict:
    return {
        "type": "gregtech:tool_shaped",
        "_comment": f"GT6 {comment}",
        "pattern": pattern,
        "key": key,
        # GT6's DEF_REV_NCC does not set MIR; 1.20.1 would mirror by default (see §18.2)
        "allow_mirror": False,
        "result": {"item": f"gregtech:{result}"},
    }


RECIPES = {
    "rope_silk": recipe("Loader_MultiTileEntities:2087", [" S", "SS", "Sq"],
                        {"S": item("minecraft:string"),
                         "q": item("gregtech:tool_scissors")}, "rope_silk"),
    "rope_grass": recipe("Loader_MultiTileEntities:2088", [" GG", "GGG", "GGq"],
                         {"G": item("gregtech:dry_grass"),
                          "q": item("gregtech:tool_scissors")}, "rope_grass"),
    "rope_vine": recipe("Loader_MultiTileEntities:2089", [" V", "VV", "Vb"],
                        {"V": item("minecraft:vine"),
                         "b": item("gregtech:tool_sword")}, "rope_vine"),
    "rope_steel": recipe("Loader_MultiTileEntities:2091", [" P", "PP", "Px"],
                         {"P": item("gregtech:wire_fine_steel"),
                          "x": item("gregtech:tool_wire_cutter")}, "rope_steel"),
    "boomstick": recipe("Loader_MultiTileEntities:2235", [" S ", "PGP", "DGD"],
                        {"S": item("minecraft:string"), "P": item("minecraft:paper"),
                         # GT6 dust.mat(MT.Gunpowder) / dust.mat(ANY.SiO2): the port registers the
                         # silicon dioxide dust under its own material id, dust_silicondioxide
                         "G": item("gregtech:dust_gunpowder"),
                         "D": item("gregtech:dust_silicondioxide")},
                        "boomstick"),
}


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, data in RECIPES.items():
        path = OUT / f"{name}.json"
        text = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
        if not path.exists() or path.read_text(encoding="utf-8") != text:
            path.write_text(text, encoding="utf-8")
    print(f"wrote {len(RECIPES)} tool block recipes into {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
