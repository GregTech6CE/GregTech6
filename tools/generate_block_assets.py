#!/usr/bin/env python3
"""Generate blockstates/models/lang for material blocks and GT stone blocks."""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = native_path(ROOT / "src/main/resources/assets/gregtech")
BLOCKSTATES = ASSETS / "blockstates"
MODELS = ASSETS / "models/block"
ITEM_MODELS = ASSETS / "models/item"
LANG_EN = ASSETS / "lang/en_us.json"
LANG_ZH = ASSETS / "lang/zh_cn.json"
JAVA_MATERIALS = ROOT / "tools/generated/materials-source.java.txt"

BLOCK_PREFIXES = [
    ("blockRaw", "Block of Ore", "矿块", "oreRaw"),
    ("blockGem", "Block of Gems", "宝石块", "gem"),
    ("blockDust", "Block of Dust", "粉块", "dust"),
    ("blockIngot", "Block of Ingots", "锭块", "ingot"),
    ("blockPlate", "Block of Plates", "板块", "plate"),
    ("blockPlateGem", "Block of Gem Plates", "宝石板块", "plateGem"),
    ("blockSolid", "Block of Cast Metal", "固体块", "ingot"),
    ("casingMachine", "Machine Casing", "机器外壳", "parts"),
    ("casingMachineDouble", "Robust Machine Casing", "坚固机器外壳", "parts"),
    ("casingMachineQuadruple", "Reinforced Machine Casing", "强化机器外壳", "parts"),
    ("casingMachineDense", "Dense Machine Casing", "致密机器外壳", "parts"),
    ("crateGtRaw", "Partial Crate of Ore", "矿石小板条箱", "oreRaw"),
    ("crateGtGem", "Partial Crate of Gems", "宝石小板条箱", "gem"),
    ("crateGtDust", "Partial Crate of Dust", "粉末小板条箱", "dust"),
    ("crateGtIngot", "Partial Crate of Ingots", "锭小板条箱", "ingot"),
    ("crateGtPlate", "Partial Crate of Plates", "板小板条箱", "plate"),
    ("crateGtPlateGem", "Partial Crate of Gem Plates", "宝石板小板条箱", "plateGem"),
    ("crateGt64Raw", "Crate of Ore", "粗矿板条箱", "oreRaw"),
    ("crateGt64Gem", "Crate of Gems", "宝石板条箱", "gem"),
    ("crateGt64Dust", "Crate of Dust", "粉板条箱", "dust"),
    ("crateGt64Ingot", "Crate of Ingots", "锭板条箱", "ingot"),
    ("crateGt64Plate", "Crate of Plates", "板板条箱", "plate"),
    ("crateGt64PlateGem", "Crate of Gem Plates", "宝石板板条箱", "plateGem"),
]

# Runtime shared models use OP texture() stubs (without "64"), not prefix_id.lower().
CRATE_TEXTURE_STUB = {
    "crateGtRaw": "crategtraw",
    "crateGtGem": "crategtgem",
    "crateGtDust": "crategtdust",
    "crateGtIngot": "crategtingot",
    "crateGtPlate": "crategtplate",
    "crateGtPlateGem": "crategtplategem",
    "crateGt64Raw": "crategtraw",
    "crateGt64Gem": "crategtgem",
    "crateGt64Dust": "crategtdust",
    "crateGt64Ingot": "crategtingot",
    "crateGt64Plate": "crategtplate",
    "crateGt64PlateGem": "crategtplategem",
}
CRATE_BOTTOM_TEXTURE = "gregtech:block/iconsets/crate"

STONE_TYPES = [
    "granite_black", "granite_red", "basalt", "marble", "limestone", "granite", "diorite", "andesite",
    "komatiite", "greenschist", "blueschist", "kimberlite", "quartzite", "prismarine_light",
    "prismarine_dark", "slate", "shale",
    "chalk", "dolomite", "gabbro", "gneiss", "gypsum", "oilshale", "rhyolite", "salt", "sylvite", "talc",
]
STONE_VARIANTS = [
    "stone", "cobble", "cobble_mossy", "smooth", "bricks", "bricks_mossy", "bricks_cracked",
    "bricks_chiseled", "bricks_redstone", "bricks_reinforced", "small_bricks", "square_bricks",
    "tiles", "small_tiles", "windmill_tiles_a", "windmill_tiles_b",
]
STONE_TEXTURE_FOLDERS = {
    "granite_black": "gt.stone.granite.black",
    "granite_red": "gt.stone.granite.red",
    "basalt": "gt.stone.basalt",
    "marble": "gt.stone.marble",
    "limestone": "gt.stone.limestone",
    "granite": "gt.stone.granite",
    "diorite": "gt.stone.diorite",
    "andesite": "gt.stone.andesite",
    "komatiite": "gt.stone.komatiite",
    "greenschist": "gt.stone.greenschist",
    "blueschist": "gt.stone.blueschist",
    "kimberlite": "gt.stone.kimberlite",
    "quartzite": "gt.stone.quartzite",
    "prismarine_light": "gt.stone.prismarine.light",
    "prismarine_dark": "gt.stone.prismarine.dark",
    "slate": "gt.stone.slate",
    "shale": "gt.stone.shale",
    "chalk": "gt.stone.chalk",
    "dolomite": "gt.stone.dolomite",
    "gabbro": "gt.stone.gabbro",
    "gneiss": "gt.stone.gneiss",
    "gypsum": "gt.stone.gypsum",
    "oilshale": "gt.stone.oilshale",
    "rhyolite": "gt.stone.rhyolite",
    "salt": "gt.stone.salt",
    "sylvite": "gt.stone.sylvite",
    "talc": "gt.stone.talc",
}
VARIANT_TEXTURE = {
    "stone": "stone",
    "cobble": "cobble",
    "cobble_mossy": "cobble_mossy",
    "smooth": "smooth",
    "bricks": "bricks",
    "bricks_mossy": "bricks_mossy",
    "bricks_cracked": "bricks_cracked",
    "bricks_chiseled": "bricks_chiseled",
    "bricks_redstone": "bricks_redstone",
    "bricks_reinforced": "bricks_reinforced",
    "small_bricks": "small_bricks",
    "square_bricks": "square_bricks",
    "tiles": "tiles",
    "small_tiles": "small_tiles",
    "windmill_tiles_a": "windmill_tiles_a",
    "windmill_tiles_b": "windmill_tiles_b",
}

MATERIAL_LINE_RE = re.compile(
    r"(\w+)\s*=\s*(?:\w+\.)?(?:create|metal|element|alloy|ore|gem|dust|gas|wood|stone|elec|cent|clay)\(\s*[^,]+,\s*\"((?:\\.|[^\"\\])*)\"")


def camel_to_snake(name: str) -> str:
    out: list[str] = []
    for i, c in enumerate(name):
        if c.isupper():
            if i > 0:
                out.append("_")
            out.append(c.lower())
        else:
            out.append(c)
    return "".join(out)


def sanitize(name: str) -> str:
    return name.replace(" ", "").replace("-", "").replace("'", "")


def parse_material_blocks(text: str) -> list[str]:
    blocks: list[str] = []
    lines = text.splitlines()
    i = 0
    while i < len(lines):
        if MATERIAL_LINE_RE.search(lines[i]):
            block = lines[i]
            i += 1
            while i < len(lines) and lines[i].strip().startswith("."):
                block += lines[i]
                i += 1
            blocks.append(block)
        else:
            i += 1
    return blocks


def parse_materials(text: str) -> list[tuple[str, str, str]]:
    entries: list[tuple[str, str, str]] = []
    for block in parse_material_blocks(text):
        match = MATERIAL_LINE_RE.search(block)
        if not match:
            continue
        field = match.group(1)
        factory = match.group(2)
        entries.append((field, sanitize(factory), block))
    return entries


def classify(block: str) -> set[str]:
    kinds: set[str] = set()
    lower = block.lower()
    if ".sethidden()" in lower or "materialproperty.hidden" in lower:
        return kinds
    if "gem(" in lower or "materialproperty.gem" in lower or ".setgem" in lower:
        kinds.add("gem")
    if any(x in lower for x in ("metal(", "element(", "alloy(", "ingot", "materialproperty.metal")):
        kinds.add("ingot")
    if "dust(" in lower or "materialproperty.dust" in lower or ".setdust" in lower:
        kinds.add("dust")
    if "ore(" in lower or "materialproperty.ore" in lower or "generate_ore" in lower:
        kinds.add("oreRaw")
    if ".settoolstats" in lower or "tool_head" in lower or "generate_parts" in lower:
        kinds.add("parts")
    if "plate" in lower or "wood(" in lower or "stone(" in lower:
        kinds.add("plate")
    if "gem(" in lower and "plate" in lower:
        kinds.add("plateGem")
    if "stick" in lower or "wood(" in lower:
        kinds.add("plate")
    if "ingot" in kinds or "plate" in kinds:
        kinds.add("plate")
    if "gem(" in lower:
        kinds.add("plateGem")
    if factory_name := re.search(r'\"([^\"]+)\"', block):
        name = factory_name.group(1)
        if name in ("Sand", "RedSand", "Stone"):
            kinds.discard("dust")
    return kinds


def valid(prefix_rule: str, kinds: set[str], registry: str) -> bool:
    if prefix_rule == "dust" and registry in ("Sand", "RedSand", "Stone"):
        return False
    if prefix_rule == "blockSolid":
        return "ingot" in kinds
    if prefix_rule == "parts":
        return "parts" in kinds and "ingot" in kinds
    if prefix_rule == "plateGem":
        return "gem" in kinds
    return prefix_rule in kinds


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    content = json.dumps(data, indent=2) + "\n"
    if not path.exists() or path.read_text(encoding="utf-8") != content:
        path.write_text(content, encoding="utf-8")


def write_json_lang(path: Path, data: dict) -> None:
    content = json.dumps(dict(sorted(data.items())), indent=2, ensure_ascii=False) + "\n"
    if not path.exists() or path.read_text(encoding="utf-8") != content:
        path.write_text(content, encoding="utf-8")


def fill_partial_crate_assets() -> int:
    """Mirror every existing 64-piece crate asset for GT6's hidden 16-piece crate.

    The generated material-source heuristic misses some runtime materials; the older 64-piece
    crate asset inventory is broader. Mirroring its three JSON files keeps both sizes equally
    renderable, including those materials the heuristic cannot infer.
    """
    written = 0
    for form in ("Raw", "Gem", "Dust", "Ingot", "Plate", "PlateGem"):
        full_prefix = camel_to_snake(f"crateGt64{form}")
        partial_prefix = camel_to_snake(f"crateGt{form}")
        for full_state in BLOCKSTATES.glob(f"{full_prefix}_*.json"):
            full_id = full_state.stem
            partial_id = partial_prefix + full_id[len(full_prefix):]
            for folder in (BLOCKSTATES, MODELS / "blocks", ITEM_MODELS):
                source = folder / f"{full_id}.json"
                if not source.exists():
                    raise FileNotFoundError(f"missing paired crate asset: {source}")
                target = folder / f"{partial_id}.json"
                content = source.read_text(encoding="utf-8").replace(full_id, partial_id)
                if not target.exists() or target.read_text(encoding="utf-8") != content:
                    target.write_text(content, encoding="utf-8")
                    written += 1
    return written


def write_crate_shared_model(path: Path, layer0: str, hull: str) -> None:
    layer1 = f"{layer0}_overlay"
    write_json(path, {
        "loader": "forge:composite",
        "children": {
            "layer0": {
                "parent": "minecraft:block/cube_all",
                "textures": {"all": hull},
                "render_type": "minecraft:solid",
            },
            "layer1": {
                "parent": "minecraft:block/cube_all",
                "textures": {"all": layer0},
                "render_type": "minecraft:solid",
            },
            "layer2": {
                "parent": "minecraft:block/cube_all",
                "textures": {"all": layer1},
                "render_type": "minecraft:cutout",
            },
        },
    })


def write_slab_templates() -> None:
    down = MODELS / "stones" / "slab" / "down.json"
    side = MODELS / "stones" / "slab" / "side.json"
    item_display = MODELS / "stones" / "slab" / "item_display.json"
    if not down.exists():
        write_json(down, {
            "textures": {"particle": "#all"},
            "elements": [{
                "from": [0, 0, 0], "to": [16, 8, 16],
                "faces": {
                    "down": {"texture": "#all", "cullface": "down"},
                    "up": {"texture": "#all"},
                    "north": {"texture": "#all"},
                    "south": {"texture": "#all"},
                    "west": {"texture": "#all"},
                    "east": {"texture": "#all"},
                },
            }],
        })
    if not side.exists():
        write_json(side, {
            "textures": {"particle": "#all"},
            "elements": [{
                "from": [0, 0, 0], "to": [16, 16, 8],
                "faces": {
                    "down": {"texture": "#all"},
                    "up": {"texture": "#all"},
                    "north": {"texture": "#all", "cullface": "north"},
                    "south": {"texture": "#all"},
                    "west": {"texture": "#all"},
                    "east": {"texture": "#all"},
                },
            }],
        })
    write_json(item_display, {
        "textures": {"particle": "#all"},
        "elements": [{
            "from": [0, 0, 0], "to": [16, 8, 16],
            "faces": {
                "down": {"texture": "#all"},
                "up": {"texture": "#all"},
                "north": {"texture": "#all"},
                "south": {"texture": "#all"},
                "west": {"texture": "#all"},
                "east": {"texture": "#all"},
            },
        }],
    })


def write_stone_slab_assets(stone: str, variant: str, folder: str, tex: str) -> None:
    block_id = f"stone_{stone}_{variant}_slab"
    texture = f"gregtech:block/stones/{folder}/{tex}"
    write_json(MODELS / "stones" / f"{block_id}.json", {
        "parent": "gregtech:block/stones/slab/down",
        "textures": {"all": texture},
    })
    write_json(MODELS / "stones" / f"{block_id}_side.json", {
        "parent": "gregtech:block/stones/slab/side",
        "textures": {"all": texture},
    })
    variants = {
        "facing=down": {"model": f"gregtech:block/stones/{block_id}"},
        "facing=up": {"model": f"gregtech:block/stones/{block_id}", "x": 180},
        "facing=north": {"model": f"gregtech:block/stones/{block_id}_side"},
        "facing=south": {"model": f"gregtech:block/stones/{block_id}_side", "y": 180},
        "facing=east": {"model": f"gregtech:block/stones/{block_id}_side", "y": 90},
        "facing=west": {"model": f"gregtech:block/stones/{block_id}_side", "y": 270},
    }
    write_json(BLOCKSTATES / f"{block_id}.json", {"variants": variants})
    write_json(ITEM_MODELS / f"{block_id}.json", {
        "parent": "minecraft:block/slab",
        "textures": {
            "bottom": texture,
            "top": texture,
            "side": texture,
        },
    })


def main() -> None:
    text = JAVA_MATERIALS.read_text(encoding="utf-8")
    materials = parse_materials(text)

    shared_models = 0
    blockstates = 0
    item_models = 0
    for prefix_id, en, zh, rule in BLOCK_PREFIXES:
        snake = camel_to_snake(prefix_id)
        tex_stub = CRATE_TEXTURE_STUB.get(prefix_id, prefix_id.lower())
        is_crate = prefix_id in CRATE_TEXTURE_STUB
        for tex_set in [
            "metallic", "dull", "shiny", "fine", "sand", "stone", "wood", "diamond", "emerald",
            "quartz", "ruby", "redstone", "rough", "powder", "copper", "cube", "cube_shiny",
            "fiery", "flint", "fluid", "food", "gas", "gem_horizontal", "gem_vertical", "glass",
            "hex", "lapis", "leaf", "lignite", "magnetic", "netherstar", "none", "opal", "paper",
            "plasma", "prismarine", "rad", "rubber", "shards", "space", "brick",
        ]:
            model_path = MODELS / "material" / tex_set / f"{tex_stub}.json"
            if is_crate or not model_path.exists():
                layer0 = f"gregtech:block/material_icons/{tex_set}/{tex_stub}"
                if is_crate:
                    write_crate_shared_model(model_path, layer0, CRATE_BOTTOM_TEXTURE)
                else:
                    write_json(model_path, {
                        "loader": "forge:composite",
                        "children": {
                            "layer0": {
                                "parent": "minecraft:block/cube_all",
                                "textures": {"all": layer0},
                                "render_type": "minecraft:solid",
                            },
                            "layer1": {
                                "parent": "minecraft:block/cube_all",
                                "textures": {"all": f"{layer0}_overlay"},
                                "render_type": "minecraft:cutout",
                            },
                        },
                    })
                shared_models += 1

        for _field, registry, block in materials:
            if not valid(rule if prefix_id != "blockSolid" else "ingot", classify(block), registry):
                continue
            block_id = f"{snake}_{registry.lower()}"
            write_json(BLOCKSTATES / f"{block_id}.json", {"variants": {"": {"model": f"gregtech:block/blocks/{block_id}"}}})
            write_json(MODELS / "blocks" / f"{block_id}.json", {
                "parent": "minecraft:block/cube_all",
                "textures": {"all": "minecraft:block/iron_block"},
            })
            write_json(ITEM_MODELS / f"{block_id}.json", {
                "parent": f"gregtech:block/blocks/{block_id}",
            })
            blockstates += 1
            item_models += 1

    partial_assets = fill_partial_crate_assets()

    stone_count = 0
    slab_count = 0
    write_slab_templates()
    for stone in STONE_TYPES:
        folder = STONE_TEXTURE_FOLDERS[stone]
        for variant in STONE_VARIANTS:
            block_id = f"stone_{stone}_{variant}"
            tex = VARIANT_TEXTURE[variant]
            write_json(BLOCKSTATES / f"{block_id}.json", {"variants": {"": {"model": f"gregtech:block/stones/{block_id}"}}})
            write_json(MODELS / "stones" / f"{block_id}.json", {
                "parent": "minecraft:block/cube_all",
                "textures": {"all": f"gregtech:block/stones/{folder}/{tex}"},
            })
            write_json(ITEM_MODELS / f"{block_id}.json", {
                "parent": f"gregtech:block/stones/{block_id}",
            })
            stone_count += 1
            item_models += 1
            write_stone_slab_assets(stone, variant, folder, tex)
            slab_count += 1
            item_models += 1

    en_lang = json.loads(LANG_EN.read_text(encoding="utf-8")) if LANG_EN.exists() else {}
    zh_lang = json.loads(LANG_ZH.read_text(encoding="utf-8")) if LANG_ZH.exists() else {}
    for prefix_id, en, zh, _rule in BLOCK_PREFIXES:
        snake = camel_to_snake(prefix_id)
        en_lang[f"block.gregtech.{snake}"] = f"%s {en}"
        zh_lang[f"block.gregtech.{snake}"] = f"%s{zh}"
        en_lang[f"itemGroup.gregtech.{snake}"] = f"GregTech {en}s"
        zh_lang[f"itemGroup.gregtech.{snake}"] = f"格雷科技·{zh}"
    en_lang["block.gregtech.stone"] = "%s %s"
    zh_lang["block.gregtech.stone"] = "%s%s"
    en_lang["block.gregtech.stone_slab"] = "%s %s Slab"
    zh_lang["block.gregtech.stone_slab"] = "%s%s半砖"
    en_lang["itemGroup.gregtech.stones"] = "GregTech Stones"
    zh_lang["itemGroup.gregtech.stones"] = "格雷科技·石头"
    en_lang["itemGroup.gregtech.tools"] = "GregTech Tools"
    zh_lang["itemGroup.gregtech.tools"] = "格雷科技·工具"
    write_json_lang(LANG_EN, en_lang)
    write_json_lang(LANG_ZH, zh_lang)

    print(f"Shared material block models: {shared_models}, blockstates: {blockstates}, partial crate assets updated: {partial_assets}, stone blocks: {stone_count}, stone slabs: {slab_count}, item models: {item_models}")

    tags_root = native_path(ROOT / "src/main/resources/data")
    stone_ids = [f"gregtech:stone_{stone}_{variant}" for stone in STONE_TYPES for variant in STONE_VARIANTS]
    slab_ids = [f"gregtech:stone_{stone}_{variant}_slab" for stone in STONE_TYPES for variant in STONE_VARIANTS]
    write_json(tags_root / "gregtech/tags/blocks/stone_blocks.json", {"values": stone_ids + slab_ids})
    write_json(tags_root / "minecraft/tags/blocks/mineable/pickaxe.json", {
        "replace": False,
        "values": [{"id": "#gregtech:stone_blocks", "required": False}],
    })
    diamond_stones = [
        f"gregtech:stone_{stone}_{variant}" + suffix
        for stone in ("granite_black", "granite_red")
        for variant in STONE_VARIANTS
        for suffix in ("", "_slab")
    ]
    iron_stones = [
        f"gregtech:stone_{stone}_{variant}" + suffix
        for stone in ("basalt", "komatiite", "kimberlite")
        for variant in STONE_VARIANTS
        for suffix in ("", "_slab")
    ]
    stone_tool_stones = [
        f"gregtech:stone_{stone}_{variant}" + suffix
        for stone in ("granite", "slate", "prismarine_dark")
        for variant in STONE_VARIANTS
        for suffix in ("", "_slab")
    ]
    for path, ids in (
        ("minecraft/tags/blocks/needs_diamond_tool.json", diamond_stones),
        ("minecraft/tags/blocks/needs_iron_tool.json", iron_stones),
        ("minecraft/tags/blocks/needs_stone_tool.json", stone_tool_stones),
    ):
        write_json(tags_root / path, {"replace": False, "values": ids})
    print(f"Stone block tags: {len(stone_ids)} entries")


if __name__ == "__main__":
    main()
