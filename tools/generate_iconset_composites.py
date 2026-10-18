#!/usr/bin/env python3
"""Composite iconset blocks: assets + lang.

GT6 logs/beams/grass/bales were single blocks using side+top texture pairs; the
port had registered one block per texture. This script:
  1. deletes the per-texture assets of merged component textures,
  2. writes blockstates/models/item models for the composite blocks
     (pillars via cube_column, grass-style via cube_bottom_top),
  3. regenerates lang entries (en_us + zh_cn) for ALL iconset blocks.

Keep COLUMN/BOTTOM_TOP in sync with GTIconSetBlocks.java.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import json
import os
import re

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, str(native_path("src")), "main", "resources", "assets", "gt6ce")
BLOCKSTATES = os.path.join(ASSETS, "blockstates")
BLOCK_MODELS = os.path.join(ASSETS, "models", "block", "iconsets")
ITEM_MODELS = os.path.join(ASSETS, "models", "item")
LANG_EN = os.path.join(ASSETS, "lang", "en_us.json")
LANG_ZH = os.path.join(ASSETS, "lang", "zh_cn.json")
JAVA = os.path.join(ROOT, str(native_path("src")), "main", "java", "com", "gregtech", "gregtech",
                    "registry", "GTIconSetBlocks.java")

WOODS = ["bluemahoe", "bluespruce", "cinnamon", "coconut", "dry", "frozen", "hazel",
         "maple", "mossy", "rainbowood", "rotten", "rubber", "willow"]
BEAMS = ["acacia", "birch", "bluemahoe", "bluespruce", "cinnamon", "coconut", "darkoak",
         "darkwood", "greatwood", "hazel", "jungle", "maple", "oak", "rainbowood",
         "rubber", "rubberwood", "silverwood", "skyroot", "spruce", "willow", "wood"]
CROPS = ["barley", "oat", "rice", "rye"]

COLUMN = (
    [(f"log_{w}", f"log_side_{w}", f"log_top_{w}") for w in WOODS]
    + [("log_hole_maple", "log_hole_maple", "log_top_maple"),
       ("log_hole_rainbowood", "log_hole_rainbowood", "log_top_rainbowood"),
       ("log_hole_rubber", "log_hole_rubber", "log_top_rubber"),
       ("log_sap_maple", "log_sap_maple", "log_top_maple"),
       ("log_sap_rainbowood", "log_sap_rainbowood", "log_top_rainbowood"),
       ("log_resin_rubber", "log_resin_rubber", "log_top_rubber")]
    + [(f"beam_{w}", f"beam_side_{w}", f"beam_top_{w}") for w in BEAMS]
    + [(f"bale_{c}", f"{c}_side", f"{c}_top") for c in CROPS]
    + [("coins", "coin_side", "coin_top")]
)

BOTTOM_TOP = (
    [("grass", "grass_side", "grass_top", "minecraft:block/dirt"),
     ("grass_dry", "grass_side_dry", "grass_top_dry", "minecraft:block/dirt"),
     ("grass_moldy", "grass_side_moldy", "grass_top_moldy", "minecraft:block/dirt"),
     ("grass_rotten", "grass_side_rotten", "grass_top_rotten", "minecraft:block/dirt")]
    + [(f"grassblock_{c}", f"grassblock_side_{c}", f"grassblock_top_{c}", "minecraft:block/dirt")
       for c in ["brown", "dark", "light", "medium", "normal", "yellow"]]
    + [("path", "path_side", "path_top", "minecraft:block/dirt"),
       ("zpm", "zpm_sides", "zpm_top", "zpm_bottom"),
       ("aneutronic_fusion", "aneutronic_fusion_sides", "aneutronic_fusion_top", "aneutronic_fusion_bottom"),
       ("bottlecrate", "bottlecrate_bottle_sides", "bottlecrate_bottle_cap", "bottlecrate_bottle_top"),
       ("powercell", "powercell_sides", "powercell_top", "powercell_top")]
)

# Component textures whose standalone block assets must be deleted (plus overlays).
REMOVED = set()
for _, side, top in COLUMN:
    REMOVED.update([side, top])
for _, side, top, bottom in BOTTOM_TOP:
    REMOVED.update([side, top])
    if not bottom.startswith("minecraft:"):
        REMOVED.add(bottom)
REMOVED.update(["coin_bottom", "fiber_wire_overlay", "logistics_wire_overlay",
                "powercell_sides_overlay", "powercell_top_overlay",
                "axle_clockwise", "axle_counterclockwise", "axle_down", "axle_horizontal",
                "axle_left", "axle_right", "axle_up", "axle_vertical"])
# Composite ids identical to former simple-block ids keep their files (overwritten below).
KEEP_IDS = {bid for bid, *_ in COLUMN} | {bid for bid, *_ in BOTTOM_TOP}
REMOVED -= KEEP_IDS


def tex(name: str) -> str:
    return name if name.startswith("minecraft:") else "gt6ce:block/iconsets/" + name


def write(path: str, data: dict) -> None:
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def delete_component_assets() -> int:
    n = 0
    for name in sorted(REMOVED):
        for path in (os.path.join(BLOCKSTATES, name + ".json"),
                     os.path.join(BLOCK_MODELS, name + ".json"),
                     os.path.join(ITEM_MODELS, name + ".json")):
            if os.path.exists(path):
                os.remove(path)
                n += 1
    return n


def write_composites() -> None:
    for bid, side, top in COLUMN:
        model = "gt6ce:block/iconsets/" + bid
        write(os.path.join(BLOCK_MODELS, bid + ".json"), {
            "parent": "minecraft:block/cube_column",
            "textures": {"end": tex(top), "side": tex(side)},
        })
        write(os.path.join(BLOCK_MODELS, bid + "_horizontal.json"), {
            "parent": "minecraft:block/cube_column_horizontal",
            "textures": {"end": tex(top), "side": tex(side)},
        })
        write(os.path.join(BLOCKSTATES, bid + ".json"), {
            "variants": {
                "axis=y": {"model": model},
                "axis=z": {"model": model + "_horizontal", "x": 90},
                "axis=x": {"model": model + "_horizontal", "x": 90, "y": 90},
            }
        })
        write(os.path.join(ITEM_MODELS, bid + ".json"), {"parent": model})

    for bid, side, top, bottom in BOTTOM_TOP:
        model = "gt6ce:block/iconsets/" + bid
        write(os.path.join(BLOCK_MODELS, bid + ".json"), {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {"top": tex(top), "side": tex(side), "bottom": tex(bottom)},
        })
        write(os.path.join(BLOCKSTATES, bid + ".json"), {
            "variants": {"": {"model": model}}
        })
        write(os.path.join(ITEM_MODELS, bid + ".json"), {"parent": model})


# ---- lang generation -------------------------------------------------------

ZH_WORDS = {
    "log": "原木", "beam": "横梁", "planks": "木板", "leaves": "树叶", "opaque": "不透明",
    "sapling": "树苗", "large": "大型", "small": "小型", "grass": "草", "grassblock": "草方块",
    "bale": "草捆", "coins": "硬币堆", "coin": "硬币", "path": "小径", "slab": "台阶",
    "turf": "草皮", "mud": "泥", "asphalt": "沥青", "concrete": "混凝土", "reinforced": "强化",
    "crate": "板条箱", "bottlecrate": "瓶箱", "powercell": "能量单元", "zpm": "零点模块",
    "machine": "机器", "hatch": "舱口", "gear": "齿轮", "gearbox": "齿轮箱", "axle": "轮轴",
    "plate": "板", "atom": "原子", "void": "虚空", "rail": "铁轨", "booster": "加速",
    "detector": "侦测", "straight": "直", "turned": "弯", "active": "激活", "road": "道路",
    "reflector": "反光", "stripe": "条纹", "glass": "玻璃", "clear": "纯净", "clay": "黏土",
    "sand": "沙", "magnetite": "磁铁矿", "flower": "花", "glowtus": "光莲", "insulation": "绝缘层",
    "wire": "导线", "fiber": "光纤", "logistics": "物流", "pipe": "管道", "restrictor": "限流",
    "piston": "活塞", "idle": "待机", "moving": "运行", "duct": "胶布", "tape": "胶带",
    "ore": "矿石", "crystal": "晶体", "block": "方块", "long": "长", "dist": "距离",
    "fluid": "流体", "item": "物品", "spring": "泉", "maple": "枫木", "coconut": "椰子木",
    "willow": "柳木", "rubber": "橡胶木", "rubberwood": "橡胶木", "cinnamon": "肉桂木",
    "hazel": "榛木", "bluemahoe": "蓝槿木", "bluespruce": "蓝云杉", "rainbowood": "彩虹木",
    "dry": "干枯", "frozen": "冰冻", "mossy": "苔藓", "rotten": "腐朽", "moldy": "发霉",
    "acacia": "金合欢", "birch": "白桦", "darkoak": "深色橡木", "darkwood": "黑暗木",
    "greatwood": "宏伟木", "jungle": "丛林", "oak": "橡木", "silverwood": "银树木",
    "skyroot": "天根木", "spruce": "云杉", "wood": "木", "barley": "大麦", "oat": "燕麦",
    "rice": "稻米", "rye": "黑麦", "hole": "孔洞", "sap": "树液", "resin": "树脂",
    "treated": "处理过的", "compressed": "压缩", "xmas": "圣诞", "brown": "棕色",
    "dark": "深色", "light": "浅色", "medium": "中等", "normal": "普通", "yellow": "黄色",
    "aneutronic": "无中子", "fusion": "聚变", "cfoam": "建筑泡沫", "fresh": "未凝固",
    "hardened": "硬化", "owned": "私有", "greg": "格雷", "lantern": "灯笼",
    "rendering": "渲染", "error": "错误",
}


def en_name(bid: str) -> str:
    words = bid.replace("greg_o_lantern", "greg-o-lantern").split("_")
    return " ".join(w.capitalize() for w in words if w)


def zh_name(bid: str) -> str:
    words = [w for w in bid.split("_") if w]
    out = []
    for w in words:
        out.append(ZH_WORDS.get(w, w.upper() if len(w) <= 3 else w.capitalize()))
    return "".join(out) if all(w in ZH_WORDS for w in words) else " ".join(out)


def block_id(icon: str) -> str:
    return "block_" + icon if icon.startswith("ore_") else icon


def collect_block_ids() -> list:
    text = open(JAVA, encoding="utf-8").read()
    array = text.split("buildIconNames() {")[1].split("};")[0]
    names = re.findall(r'"([a-z0-9_]+)"', array)
    ids = [block_id(n) for n in names]
    ids += [bid for bid, *_ in COLUMN]
    ids += [bid for bid, *_ in BOTTOM_TOP]
    return ids


def update_lang() -> int:
    ids = collect_block_ids()
    for path, namer in ((LANG_EN, en_name), (LANG_ZH, zh_name)):
        with open(path, encoding="utf-8-sig") as f:
            lang = json.load(f)
        for bid in ids:
            lang["block.gt6ce." + bid] = namer(bid)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(lang, f, indent=2, ensure_ascii=False, sort_keys=True)
            f.write("\n")
    return len(ids)


def main() -> None:
    removed = delete_component_assets()
    write_composites()
    n = update_lang()
    print(f"removed {removed} component asset files; "
          f"{len(COLUMN)} pillar + {len(BOTTOM_TOP)} bottom-top composites written; "
          f"{n} lang entries per language")


if __name__ == "__main__":
    main()
