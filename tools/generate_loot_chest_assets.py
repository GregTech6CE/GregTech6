#!/usr/bin/env python3
"""Assets for GT6's configurable loot chests (§25).

GT6 has one loot-chest block with a `gt.dungeonloot` name; the port registers one block per table
(`GTLootChests`), so this writes the shared block model, the 17 blockstates/item models and the lang
keys, and copies GT6's own chest texture:

  * `textures/block/machines/lootchest/colored.png`  ← GT6 `model/gt.multitileentity/lootchest.colored.png`
  * `models/block/machine/storage/loot_chest.json`   — the metal-chest model with the loot-chest texture
  * `blockstates/loot_chest_<suffix>.json`           — facing variants → the shared model
  * `models/item/loot_chest_<suffix>.json`           — parent `gregtech:item/metal_chest`
  * lang (en_us + zh_cn): `block.gregtech.loot_chest_<suffix>`

The English labels are GT6's own (`LH.java` `loot.*`: "+Random Books+", "+Library+", …).

Usage:  python tools/generate_loot_chest_assets.py
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
import shutil

ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = native_path(ROOT / "src" / "main" / "resources" / "assets" / "gregtech")
GT6 = pathlib.Path(r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\resources\assets\gregtech")
CHEST_MODEL = ASSETS / "models" / "block" / "machine" / "storage" / "metal_chest.json"
LOOT_MODEL = ASSETS / "models" / "block" / "machine" / "storage" / "loot_chest.json"
LOOT_TEXTURE = ASSETS / "textures" / "block" / "machines" / "lootchest" / "colored.png"

# suffix -> (GT6 table, English label from LH.java, Chinese label)
TABLES = {
    "dungeon": ("dungeonChest", "Loot Chest (Dungeon)", "战利品箱（地牢）"),
    "mineshaft": ("mineshaftCorridor", "Loot Chest (Mineshaft)", "战利品箱（废弃矿井）"),
    "library": ("strongholdLibrary", "Loot Chest (Library)", "战利品箱（图书馆）"),
    "stronghold_storage": ("strongholdCrossing", "Loot Chest (Stronghold Storage)", "战利品箱（要塞仓库）"),
    "stronghold_corridor": ("strongholdCorridor", "Loot Chest (Stronghold Corridor)", "战利品箱（要塞走廊）"),
    "desert_pyramid": ("pyramidDesertyChest", "Loot Chest (Desert Pyramid)", "战利品箱（沙漠神殿）"),
    "jungle_temple": ("pyramidJungleChest", "Loot Chest (Jungle Temple)", "战利品箱（丛林神庙）"),
    "jungle_dispenser": ("pyramidJungleDispenser", "Loot Chest (Jungle Dispenser)", "战利品箱（丛林发射器）"),
    "blacksmith": ("villageBlacksmith", "Loot Chest (Blacksmith)", "战利品箱（铁匠铺）"),
    "bonus": ("bonusChest", "Loot Chest (Bonus)", "战利品箱（奖励箱）"),
    "flawless": ("gt.flawless", "Loot Chest (Flawless Gems)", "战利品箱（无瑕宝石）"),
    "gems": ("gt.gems", "Loot Chest (Gems)", "战利品箱（宝石）"),
    "misc": ("gt.misc", "Loot Chest (Miscellaneous)", "战利品箱（杂项）"),
    "seeds": ("gt.seeds", "Loot Chest (Seeds)", "战利品箱（种子）"),
    "saplings": ("gt.saplings", "Loot Chest (Saplings)", "战利品箱（树苗）"),
    "books": ("gt.books", "Loot Chest (Books)", "战利品箱（书籍）"),
    "bottles": ("gt.bottles", "Loot Chest (Bottles)", "战利品箱（瓶子）"),
    # GT6's own label is LH.java's "loot.gt.matdicts" = "-Random Material Dictionaries-"; the table is
    # built from the registered materials since §31 (material dictionary books).
    "matdicts": ("gt.matdicts", "Loot Chest (Material Dictionaries)", "战利品箱（材料词典）"),
}

FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}


def write_json(path: pathlib.Path, data: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def add_lang(path: pathlib.Path, keys: dict[str, str]) -> int:
    data = json.loads(path.read_text(encoding="utf-8"))
    added = 0
    for key, value in keys.items():
        if key not in data:
            data[key] = value
            added += 1
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return added


def main() -> None:
    source = GT6 / "textures" / "model" / "gt.multitileentity" / "lootchest.colored.png"
    if not source.exists():
        raise SystemExit(f"GT6 texture missing: {source}")
    LOOT_TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, LOOT_TEXTURE)

    model = json.loads(CHEST_MODEL.read_text(encoding="utf-8"))
    model["children"]["layer0"]["textures"] = {
        "all": "gregtech:block/machines/lootchest/colored",
        "particle": "gregtech:block/machines/lootchest/colored",
    }
    write_json(LOOT_MODEL, model)

    for suffix in TABLES:
        write_json(ASSETS / "blockstates" / f"loot_chest_{suffix}.json", {
            "variants": {
                f"facing={facing}": {"model": "gregtech:block/machine/storage/loot_chest", **({"y": y} if y else {})}
                for facing, y in FACINGS.items()
            },
        })
        write_json(ASSETS / "models" / "item" / f"loot_chest_{suffix}.json",
                   {"parent": "gregtech:item/metal_chest"})

    lang = ASSETS / "lang"
    print(f"en_us: {add_lang(lang / 'en_us.json', {f'block.gregtech.loot_chest_{s}': e[1] for s, e in TABLES.items()})} keys added")
    print(f"zh_cn: {add_lang(lang / 'zh_cn.json', {f'block.gregtech.loot_chest_{s}': e[2] for s, e in TABLES.items()})} keys added")
    print(f"wrote {len(TABLES)} blockstates + item models, the shared model and the texture")


if __name__ == "__main__":
    main()
