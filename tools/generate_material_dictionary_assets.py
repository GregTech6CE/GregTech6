#!/usr/bin/env python3
"""Add the Dusty Material Dictionary (GT6's ``IL.Book_Loot_MatDict``) to the port.

GT6 registers it as a MultiItemBooks entry (``MultiItemBooks.java:68``,
``addItem(32766, "Dusty Material Dictionary", "Loot: Book about a random Material", …)`` with
``Behavior_Drop_Loot("gt.matdicts")``): the loot item that rolls GT6's material dictionary table, one
row per material (``Loader_Loot:359-364``).  The port registers the item in ``GTMultiItems`` and builds
the table in ``GTLootTables`` from the registered materials, so this tool only adds the assets:

  * ``textures/item/books/dusty_material_dictionary.png`` — GT6's texture 32766
  * ``models/item/dusty_material_dictionary.json``        — the usual generated item model
  * lang keys (en_us + zh_cn): name, GT6 tooltip and the loot-bag use hint

Usage:  python tools/generate_material_dictionary_assets.py
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
GT6_TEX = pathlib.Path(
    r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\resources\assets\gregtech"
    r"\textures\items\gt.multiitem.books\32766.png")
ASSETS = native_path(ROOT / "src" / "main" / "resources" / "assets" / "gt6ce")
ITEM_ID = "dusty_material_dictionary"
TEXTURE = ASSETS / "textures" / "item" / "books" / f"{ITEM_ID}.png"
MODEL = ASSETS / "models" / "item" / f"{ITEM_ID}.json"

EN = {
    f"item.gt6ce.{ITEM_ID}": "Dusty Material Dictionary",
    f"item.gt6ce.{ITEM_ID}.tooltip": "Loot: Book about a random Material",
    "gt6ce.tooltip.loot_bag": "Rightclick this on a Block to loot",
}
ZH = {
    f"item.gt6ce.{ITEM_ID}": "沾满灰尘的材料词典",
    f"item.gt6ce.{ITEM_ID}.tooltip": "战利品：随机一本材料词典",
    "gt6ce.tooltip.loot_bag": "对着一方块右键即可开启",
}


def main() -> int:
    if not GT6_TEX.exists():
        raise SystemExit(f"GT6 texture missing: {GT6_TEX}")
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(GT6_TEX, TEXTURE)
    MODEL.parent.mkdir(parents=True, exist_ok=True)
    MODEL.write_text(json.dumps(
        {"parent": "minecraft:item/generated", "textures": {"layer0": f"gt6ce:item/books/{ITEM_ID}"}},
        indent=2) + "\n", encoding="utf-8")
    for name, keys in (("en_us.json", EN), ("zh_cn.json", ZH)):
        path = ASSETS / "lang" / name
        data = json.loads(path.read_text(encoding="utf-8"))
        data.update(keys)
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False, sort_keys=True) + "\n", encoding="utf-8")
    print(f"wrote {TEXTURE.relative_to(ROOT)}")
    print(f"wrote {MODEL.relative_to(ROOT)}")
    print(f"lang keys: {len(EN)} en, {len(ZH)} zh")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
