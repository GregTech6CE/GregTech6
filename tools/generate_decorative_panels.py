"""Reproduce original six-screw panel recipes over available native plank identities.

GT6 Loader_MultiTileEntities:2043-2082, Gregorius Techneticies, LGPL-3.0-or-later.
Writes only shared resources; native tests compare every row with PanelCatalog.
"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "core/src/main/java/com/gregtech/gregtech"
RES = ROOT / "core/src/main/resources"
LEGACY = {"panel_wood", "panel_concrete", "panel_cfoam", "panel_asphalt", "panel_colored_gray", "panel_colored_black"}


def specs():
    result = [dict(id="panel_wood", kind="wood", input="minecraft:oak_planks", texture="minecraft:block/oak_planks", color="")]
    for suffix, kind, color in (("concrete", "concrete", "light_gray"), ("cfoam", "cfoam", "white"),
                                ("asphalt", "asphalt", "gray"), ("colored_gray", "concrete", "gray"), ("colored_black", "concrete", "black")):
        result.append(dict(id="panel_" + suffix, kind=kind, input="gt6ce:" + kind,
                           texture="gt6ce:block/iconsets/" + ("cfoam_hardened" if kind == "cfoam" else kind), color=color, legacy=True))
    dyes = re.findall(r'new Dye\(\d+,"([a-z_]+)",0x[0-9A-F]+\)', (JAVA / "content/tool/PaintingRules.java").read_text(encoding="utf-8"))
    assert len(dyes) == 16
    for color in dyes:
        for kind in ("concrete", "cfoam", "asphalt"):
            result.append(dict(id=f"panel_{kind}_{color}", kind=kind, color=color, input="gt6ce:" + kind,
                               texture="gt6ce:block/iconsets/" + ("cfoam_hardened" if kind == "cfoam" else kind)))
    variants = (JAVA / "content/storage/BottleCrateVariants.java").read_text(encoding="utf-8")
    lists = re.findall(r'List\.of\((.*?)\)', variants, re.S)
    vanilla, other = [re.findall(r'"([a-z_]+)"', text) for text in lists]
    species = re.findall(r'^\s+[A-Z_]+\("([a-z_]+)",', (JAVA / "block/wood/WoodSpecies.java").read_text(encoding="utf-8"), re.M)
    for wood in vanilla[1:] + species + other + ["treated"]:
        plank = f"minecraft:{wood}_planks" if wood in vanilla else f"gt6ce:planks_{wood}"
        texture = "minecraft:block/" + wood + "_planks" if wood in vanilla else "gt6ce:block/iconsets/planks_" + ("bluemahoe" if wood == "blue_mahoe" else wood)
        texture = {"pine": "minecraft:block/spruce_planks", "ebony": "minecraft:block/dark_oak_planks", "white_mahoe": "minecraft:block/birch_planks"}.get(wood, texture)
        result.append(dict(id="panel_wood_" + wood, kind="wood", color="", input=plank, texture=texture))
    assert len(result) == 83 and len({s["id"] for s in result}) == 83
    return result


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main():
    rows = specs()
    files = []
    for spec in rows:
        faces = {side: {"uv": [0, 0, 16, 16], "texture": "#surface", "tintindex": 0}
                 for side in ("down", "up", "north", "south", "west", "east")}
        write(RES / f"assets/gt6ce/models/item/{spec['id']}.json", {
            "parent": "minecraft:block/block", "textures": {"surface": spec["texture"], "particle": spec["texture"]},
            "elements": [{"from": [0, 0, 7], "to": [16, 16, 9], "faces": faces}]})
        if spec.get("legacy"):
            continue
        recipe = {"type": "gt6ce:tool_shaped", "allow_mirror": True,
                  "pattern": ["TsT", "TPT", "TdT"],
                  "key": {"T": {"tag": "gt6ce:screws/any_iron_or_steel"}, "P": {"item": spec["input"]},
                          "s": {"item": "gt6ce:tool_saw"}, "d": {"item": "gt6ce:tool_screwdriver"}},
                  "result": {"item": "gt6ce:" + spec["id"], "count": 6}}
        if spec["color"]:
            recipe["construction_color"] = spec["color"]
        name = f"decorative_panels/{spec['id']}.json"
        files.append(name)
        write(RES / f"data/gt6ce/recipes/{name}", recipe)
    catalog = JAVA / "content/recipe/EquipmentCraftingCatalog.java"
    text = catalog.read_text(encoding="utf-8")
    text = re.sub(r'\s*"decorative_panels/[^"\n]+",?', "", text)
    marker = '            "wood/slab_bluespruce.json"'
    assert marker in text
    text = text.replace(marker + ",", marker)
    text = text.replace(marker, marker + ",\n" + ",\n".join('            "' + name + '"' for name in files))
    catalog.write_text(text, encoding="utf-8")
    print(f"Shared panel resources: {len(rows)} models, {len(files)} recipes")


if __name__ == "__main__":
    main()
