"""Generate the currently obtainable GT6 bookshelf family from one checked-in manifest.

`--refresh-manifest` reads GT6's Loader_MultiTileEntities metalset table and the
wood-dictionary plank IDs that have a corresponding block in this 1.20.1 port.
Normal regeneration needs only the checked-in manifest, not the original checkout.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = native_path(ROOT / "src/main/resources/assets/gt6ce")
MANIFEST = native_path(ROOT / "src/main/resources/data/gt6ce/bookshelf_variants.json")
RECIPES = native_path(ROOT / "src/main/resources/data/gt6ce/recipes/bookshelves")
ORIGINAL_LOADER = (ROOT.parent / "gregtech6-master/gregtech6-master/src/main/java"
                   / "gregtech/loaders/b/Loader_MultiTileEntities.java")
ALIASES = native_path(ROOT / "src/main/java/com/gregtech/gregtech/data/generated/MaterialRegistryExtras.java")
MATERIALS = native_path(ROOT / "src/main/java/com/gregtech/gregtech/content/material/generated")

# Index is PlankData.PLANKS[] in GT6 LoaderWoodDictionary. The GT6-owned trees
# have shipped plank textures here. Third-party pine/ebony plank models currently
# point at missing textures, so they cannot be represented as faithful GT6 variants.
WOODS = (
    (0, "oak", "Oak", "minecraft:block/oak_planks"),
    (1, "spruce", "Spruce", "minecraft:block/spruce_planks"),
    (2, "birch", "Birch", "minecraft:block/birch_planks"),
    (3, "jungle", "Junglewood", "minecraft:block/jungle_planks"),
    (4, "acacia", "Acacia", "minecraft:block/acacia_planks"),
    (5, "dark_oak", "DarkOak", "minecraft:block/dark_oak_planks"),
    (6, "rubber", "WoodRubber", None),
    (7, "maple", "Maple", None),
    (37, "willow", "Willow", None),
    (38, "blue_mahoe", "BlueMahoe", None),
    (39, "hazel", "Hazel", None),
    (97, "cinnamon", "Cinnamon", None),
    (98, "coconut", "Coconutwood", None),
    (99, "rainbowood", "Rainbowood", None),
    (239, "bluespruce", "BlueSpruce", None),
)


def original_wood_id(index):
    if index < 100:
        return 7000 + index
    if index < 200:
        return 7900 + index - 100
    return 7800 + index - 200


def snake(name):
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).replace("__", "_").lower()


def refresh_manifest():
    alias_source = ALIASES.read_text(encoding="utf-8")
    aliases = dict(re.findall(r'GTMaterialRegistry\.registerAlias\("([^"]+)", "([^"]+)"\)', alias_source))
    texture_sets = {}
    for file in MATERIALS.glob("*.java"):
        for line in file.read_text(encoding="utf-8").splitlines():
            match = re.search(r'public static final GTMaterial (\w+) = .*?\.setTextureSet\(MaterialTextureSet\.(\w+)\)', line)
            if match:
                texture_sets[match[1]] = match[2].lower()

    variants = []
    for index, species, display, texture in WOODS:
        if texture is None:
            model_path = ASSETS / f"models/block/wood/planks_{species}.json"
            if not model_path.is_file():
                model_path = ASSETS / f"models/block/iconsets/planks_{species}.json"
            model = json.loads(model_path.read_text(encoding="utf-8"))
            texture = model["textures"]["all"]
        variants.append(dict(path="bookshelf" if index == 0 else f"bookshelf_{species}",
                             gt6_id=original_wood_id(index), kind="wood", material="Wood",
                             display=display, texture=texture, hardness=2.0, resistance=2.0,
                             flammability=150))

    source = ORIGINAL_LOADER.read_text(encoding="utf-8")
    rows = re.findall(
        r'^\s*metalset\(aRegistry, aMetal, aUtilMetal, aMachine, aWooden, '
        r'(?:MT|ANY)\.([A-Za-z_]+)\s*,\s*(\d+),\s*([\d.]+)F,\s*([\d.]+)F',
        source, re.MULTILINE)
    if len(rows) != 60:
        raise AssertionError(f"GT6 metalset table changed: {len(rows)} rows")
    for alias, original_id, hardness, resistance in rows:
        material = aliases.get(alias, alias)
        material_field = re.sub(r"[^A-Za-z0-9]", "", material)
        texture_set = texture_sets.get(material_field)
        if texture_set is None:
            raise AssertionError(f"Missing texture set for GT material {alias} -> {material_field}")
        texture = f"gt6ce:block/material_icons/{texture_set}/casingmachine"
        texture_file = ASSETS / "textures" / (texture.split(":", 1)[1] + ".png")
        if not texture_file.is_file():
            raise AssertionError(f"Missing bookshelf texture: {texture_file}")
        variants.append(dict(path="bookshelf_metal_" + snake(material_field),
                             gt6_id=7100 + int(original_id), kind="metal", material=material_field,
                             display=material_field, texture=texture, hardness=float(hardness),
                             resistance=float(resistance), flammability=0))

    paths = [v["path"] for v in variants]
    ids = [v["gt6_id"] for v in variants]
    if len(set(paths)) != len(paths) or len(set(ids)) != len(ids):
        raise AssertionError("Duplicate bookshelf path or GT6 multi-tile ID")
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text(json.dumps({"variants": variants}, indent=2) + "\n", encoding="utf-8")
    return variants


def generate(assets=ASSETS, variants=None):
    if variants is None:
        variants = json.loads(MANIFEST.read_text(encoding="utf-8"))["variants"]
    frame = json.loads((ASSETS / "models/block/bookshelf.json").read_text(encoding="utf-8"))
    for variant in variants:
        if variant["path"] == "bookshelf":
            continue  # The legacy oak ID is owned by generate_bookshelf_assets.py.
        path = variant["path"]
        model = json.loads(json.dumps(frame))
        model["textures"]["wood"] = variant["texture"]
        if variant["kind"] == "metal":
            for element in model["elements"]:
                for face in element["faces"].values():
                    face["tintindex"] = 0
        blockstate = {"variants": {f"facing={direction}": {"model": f"gt6ce:block/{path}", "y": angle}
                                   for direction, angle in (("north", 0), ("east", 90),
                                                            ("south", 180), ("west", 270))}}
        files = {
            f"models/block/{path}.json": model,
            f"models/item/{path}.json": {"parent": f"gt6ce:block/{path}"},
            f"blockstates/{path}.json": blockstate,
        }
        for relative, data in files.items():
            target = assets / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            rendered = json.dumps(data, indent=2) + "\n"
            if not target.exists() or target.read_text(encoding="utf-8") != rendered:
                target.write_text(rendered, encoding="utf-8")
    return len(variants)


def recipe_for(variant):
    """GT6 Loader_MultiTileEntities:143 / :181-183, via CR.DEF_REV_NCC.

    The source's lowercase tool slots are ingredients that survive crafting. They
    must use our tool-aware serializer; vanilla crafting would consume the tools.
    GT6 does not set CR.MIR for these registrations.
    """
    if variant["kind"] == "wood":
        plank_by_id = {
            original_wood_id(index): (
                f"minecraft:{species}_planks" if texture is not None
                else f"gt6ce:planks_{species}"
            ) for index, species, _display, texture in WOODS
        }
        plank = plank_by_id.get(variant["gt6_id"])
        if plank is None:
            raise ValueError(f"GT6 plank-index {variant['gt6_id']} is not mapped")
        pattern = ["PPP", "sfr", "PPP"]
        keys = {
            "P": {"item": plank},
            "s": {"item": "gt6ce:tool_saw"},
            "f": {"item": "gt6ce:tool_file"},
            "r": {"item": "gt6ce:tool_soft_hammer"},
        }
        source = "Loader_MultiTileEntities:181-183"
    elif variant["kind"] == "metal":
        # MaterialEquivalence.materialName: split only lowercase/digit -> uppercase,
        # then lowercase. These Forge tags include the port's material forms and
        # matching forms supplied by another mod.
        material = re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", variant["material"]).lower()
        pattern = ["PTP", "sdh", "PTP"]
        keys = {
            "P": {"tag": f"forge:plates/{material}"},
            "T": {"tag": f"forge:screws/{material}"},
            "s": {"item": "gt6ce:tool_saw"},
            "d": {"item": "gt6ce:tool_screwdriver"},
            "h": {"item": "gt6ce:tool_hammer"},
        }
        source = "Loader_MultiTileEntities:143"
    else:
        raise ValueError(f"Unknown bookshelf kind: {variant['kind']}")
    return {
        "type": "gt6ce:tool_shaped",
        "_comment": f"GT6 {source}, multi-tile {variant['gt6_id']}",
        "pattern": pattern,
        "key": keys,
        "allow_mirror": False,
        "result": {"item": f"gt6ce:{variant['path']}", "count": 1},
    }


def generate_recipes(directory=RECIPES, variants=None, *, check=False):
    if variants is None:
        variants = json.loads(MANIFEST.read_text(encoding="utf-8"))["variants"]
    expected = set()
    for variant in variants:
        target = directory / f"{variant['path']}.json"
        expected.add(target.name)
        rendered = json.dumps(recipe_for(variant), indent=2) + "\n"
        if check:
            if not target.is_file() or target.read_text(encoding="utf-8") != rendered:
                raise AssertionError(f"Stale or missing bookshelf recipe: {target}")
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            if not target.exists() or target.read_text(encoding="utf-8") != rendered:
                target.write_text(rendered, encoding="utf-8")
    if directory.exists():
        unexpected = {file.name for file in directory.glob("*.json")} - expected
        if unexpected:
            raise AssertionError(f"Unexpected bookshelf recipes: {sorted(unexpected)}")
    return len(expected)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--refresh-manifest", action="store_true")
    parser.add_argument("--check", action="store_true", help="verify generated recipes without writing")
    args = parser.parse_args()
    if args.check and args.refresh_manifest:
        parser.error("--check cannot refresh the manifest")
    variants = refresh_manifest() if args.refresh_manifest else None
    if not args.check:
        print(f"{generate(variants=variants)} GT6 bookshelf variants represented")
    print(f"{generate_recipes(variants=variants, check=args.check)} GT6 bookshelf crafting recipes "
          f"{'verified' if args.check else 'generated'}")
