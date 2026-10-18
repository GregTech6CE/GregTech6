#!/usr/bin/env python3
"""Generate shared material model JSONs and lang entries for all GT6 item prefixes."""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODELS = native_path(ROOT / "src/main/resources/assets/gregtech/models/item/material")
LANG_EN = native_path(ROOT / "src/main/resources/assets/gregtech/lang/en_us.json")
LANG_ZH = native_path(ROOT / "src/main/resources/assets/gregtech/lang/zh_cn.json")

# (id, display_en, display_zh, texture_file) — texture defaults to id.lower() with special cases below
PREFIXES: list[tuple[str, str, str, str | None]] = [
    ("dust", "Dust", "粉", None),
    ("dustSmall", "Small Dust", "小堆粉", None),
    ("dustTiny", "Tiny Dust", "小撮粉", None),
    ("dustDiv72", "1/72 Dust", "1/72粉", None),
    ("dustImpure", "Impure Dust", "含杂质粉", "dust"),
    ("crushed", "Crushed Ore", "粉碎矿石", None),
    ("crushedTiny", "Tiny Crushed Ore", "小堆粉碎矿石", None),
    ("crushedPurified", "Purified Crushed Ore", "纯净粉碎矿石", None),
    ("crushedPurifiedTiny", "Tiny Purified Crushed Ore", "小堆纯净粉碎矿石", None),
    ("crushedCentrifuged", "Refined Crushed Ore", "精炼粉碎矿石", None),
    ("crushedCentrifugedTiny", "Tiny Refined Crushed Ore", "小堆精炼粉碎矿石", None),
    ("gemChipped", "Chipped Gem", "缺角宝石", None),
    ("gemFlawed", "Flawed Gem", "瑕疵宝石", None),
    ("gem", "Gem", "宝石", None),
    ("gemFlawless", "Flawless Gem", "完美宝石", None),
    ("gemExquisite", "Exquisite Gem", "精致宝石", None),
    ("gemLegendary", "Legendary Gem", "传说宝石", None),
    ("bouleGt", "Boule", "晶锭", None),
    ("nugget", "Nugget", "粒", None),
    ("chunkGt", "Chunk", "块", None),
    ("billet", "Billet", "短锭", None),
    ("ingot", "Ingot", "锭", None),
    ("ingotHot", "Hot Ingot", "热锭", None),
    ("ingotDouble", "Double Ingot", "二重锭", None),
    ("ingotTriple", "Triple Ingot", "三重锭", None),
    ("ingotQuadruple", "Quadruple Ingot", "四重锭", None),
    ("ingotQuintuple", "Quintuple Ingot", "五重锭", None),
    ("plateGemTiny", "Tiny Gem Plate", "小型宝石板", None),
    ("plateGem", "Gem Plate", "宝石板", None),
    ("plateTiny", "Tiny Plate", "小型板", None),
    ("plate", "Plate", "板", None),
    ("plateDouble", "Double Plate", "二重板", None),
    ("plateTriple", "Triple Plate", "三重板", None),
    ("plateQuadruple", "Quadruple Plate", "四重板", None),
    ("plateQuintuple", "Quintuple Plate", "五重板", None),
    ("plateDense", "Dense Plate", "致密板", None),
    ("plateCurved", "Curved Plate", "曲面板", None),
    ("scrapGt", "Scrap", "废料", None),
    ("rockGt", "Rock", "岩石", None),
    ("oreRaw", "Raw Ore", "原矿", "oreraw"),
    ("gearGtSmall", "Small Gear", "小型齿轮", None),
    ("gearGt", "Gear", "齿轮", None),
    ("rotor", "Rotor", "转子", None),
    ("stick", "Rod", "杆", None),
    ("stickLong", "Long Rod", "长杆", None),
    ("springSmall", "Small Spring", "小弹簧", None),
    ("spring", "Spring", "弹簧", None),
    ("lens", "Lens", "透镜", None),
    ("round", "Round", "圆料", None),
    ("bolt", "Bolt", "螺栓", None),
    ("screw", "Screw", "螺丝", None),
    ("ring", "Ring", "环", None),
    ("chain", "Chain", "链", None),
    ("foil", "Foil", "箔", None),
    ("casingSmall", "Small Casing", "小型外壳", None),
    ("wireFine", "Fine Wire", "细线", None),
    ("minecartWheels", "Minecart Wheels", "矿车车轮", None),
    ("railGt", "Rail", "轨道", None),
    ("plantGtBerry", "Berry", "浆果", None),
    ("plantGtBlossom", "Blossom", "花", None),
    ("plantGtFiber", "Fiber", "纤维", None),
    ("plantGtTwig", "Twig", "细枝", None),
    ("plantGtWart", "Wart", "疣", None),
    ("chemtube", "Chem Tube", "化学试管", None),
    ("toolHeadRawSword", "Raw Sword Blade", "粗剑胚", None),
    ("toolHeadSword", "Sword Blade", "剑胚", None),
    ("toolHeadRawPickaxe", "Raw Pickaxe Head", "粗镐胚", None),
    ("toolHeadPickaxe", "Pickaxe Head", "镐胚", None),
    ("toolHeadPickaxeGem", "Gem Pickaxe Head", "宝石镐胚", None),
    ("toolHeadConstructionPickaxe", "Construction Pickaxe Head", "建筑镐胚", None),
    ("toolHeadBuilderwand", "Builder Wand Head", "建筑魔杖头", None),
    ("toolHeadRawShovel", "Raw Shovel Head", "粗铲胚", None),
    ("toolHeadShovel", "Shovel Head", "铲胚", None),
    ("toolHeadRawSpade", "Raw Spade Head", "粗锹胚", None),
    ("toolHeadSpade", "Spade Head", "锹胚", None),
    ("toolHeadRawAxe", "Raw Axe Head", "粗斧胚", None),
    ("toolHeadAxe", "Axe Head", "斧胚", None),
    ("toolHeadRawAxeDouble", "Raw Double Axe Head", "粗双头斧胚", None),
    ("toolHeadAxeDouble", "Double Axe Head", "双头斧胚", None),
    ("toolHeadRawHoe", "Raw Hoe Head", "粗锄胚", None),
    ("toolHeadHoe", "Hoe Head", "锄胚", None),
    ("toolHeadHammer", "Hammer Head", "锤头", None),
    ("toolHeadFile", "File Head", "锉刀头", None),
    ("toolHeadRawChisel", "Raw Chisel Head", "粗凿胚", None),
    ("toolHeadChisel", "Chisel Head", "凿胚", None),
    ("toolHeadRawSaw", "Raw Saw Blade", "粗锯胚", None),
    ("toolHeadSaw", "Saw Blade", "锯胚", None),
    ("toolHeadDrill", "Drill Head", "钻头", None),
    ("toolHeadChainsaw", "Chainsaw Head", "链锯头", None),
    ("toolHeadWrench", "Wrench Head", "扳手头", None),
    ("toolHeadScrewdriver", "Screwdriver Head", "螺丝刀头", None),
    ("toolHeadRawUniversalSpade", "Raw Universal Spade Head", "粗通用锹胚", None),
    ("toolHeadUniversalSpade", "Universal Spade Head", "通用锹胚", None),
    ("toolHeadRawSense", "Raw Sense Tool Head", "粗感应工具头", None),
    ("toolHeadSense", "Sense Tool Head", "感应工具头", None),
    ("toolHeadRawPlow", "Raw Plow Head", "粗犁胚", None),
    ("toolHeadPlow", "Plow Head", "犁胚", None),
    ("toolHeadBuzzSaw", "Buzz Saw Blade", "圆锯片", None),
    ("toolHeadRawArrow", "Raw Arrow Head", "粗箭头", None),
    ("toolHeadArrow", "Arrow Head", "箭头", None),
    ("arrowGtWood", "Wood Arrow", "木箭", None),
    ("arrowGtPlastic", "Plastic Arrow", "塑料箭", None),
    ("bulletGtSmall", "Small Bullet", "小子弹", None),
    ("bulletGtMedium", "Medium Bullet", "中子弹", None),
    ("bulletGtLarge", "Large Bullet", "大子弹", None),
    # The coin's icon is not a material icon at all: GT6 draws its coins with the block icon
    # Textures.BlockIcons.COIN (gregapi/old/Textures.java:40, used by MultiTileEntityCoin.java:430-435),
    # so tools/add_coin_assets.py copies that icon into every texture set and writes a transparent
    # coin_overlay.png next to it. The model itself uses the shared minted relief.
    ("coin", "Coin", "硬币", "coin"),
]

TEXTURE_SETS = [
    "metallic", "dull", "shiny", "fine", "sand", "stone", "wood", "diamond", "emerald",
    "quartz", "ruby", "redstone", "rough", "powder", "copper", "cube", "cube_shiny",
    "fiery", "flint", "fluid", "food", "gas", "gem_horizontal", "gem_vertical", "glass",
    "hex", "lapis", "leaf", "lignite", "magnetic", "netherstar", "none", "opal", "paper",
    "plasma", "prismarine", "rad", "rubber", "shards", "space", "brick",
]


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


def texture_name(prefix_id: str, override: str | None) -> str:
    if override:
        return override
    if prefix_id in ("ore", "oreRaw"):
        return "oreraw"
    return prefix_id.lower()


def model_json(tex_set: str, tex_file: str) -> str:
    # Coins use the original minted relief, shared by every material texture set.
    if tex_file == "coin":
        return '{"parent": "gregtech:item/coin_minted"}\n'
    layer0 = f"gregtech:item/material_icons/{tex_set}/{tex_file}"
    layer1 = f"{layer0}_overlay"
    return json.dumps(
        {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": layer0, "layer1": layer1},
        },
        indent=2,
    ) + "\n"


def main() -> None:
    created = 0
    for tex_set in TEXTURE_SETS:
        out_dir = MODELS / tex_set
        out_dir.mkdir(parents=True, exist_ok=True)
        for prefix_id, _, _, tex_override in PREFIXES:
            tex_file = texture_name(prefix_id, tex_override)
            path = out_dir / f"{tex_file}.json"
            path.write_text(model_json(tex_set, tex_file), encoding="utf-8")
            created += 1

    en: dict[str, str] = {}
    zh: dict[str, str] = {}
    for prefix_id, display_en, display_zh, _ in PREFIXES:
        key = camel_to_snake(prefix_id)
        en[f"item.gregtech.{key}"] = f"%s {display_en}"
        en[f"itemGroup.gregtech.{key}"] = f"GregTech {display_en}s"
        zh[f"item.gregtech.{key}"] = f"%s{display_zh}"
        zh[f"itemGroup.gregtech.{key}"] = f"格雷科技·{display_zh}"

    for path, data in ((LANG_EN, en), (LANG_ZH, zh)):
        existing = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
        # These files also hold registered items and curated GregTech.lang names.
        # Prefix generation supplies missing defaults; it must not delete or rename them.
        for key, value in data.items():
            existing.setdefault(key, value)
        path.write_text(json.dumps(dict(sorted(existing.items())), indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    print(f"Wrote {created} model JSONs and {len(PREFIXES) * 2} lang keys per locale")


if __name__ == "__main__":
    main()
