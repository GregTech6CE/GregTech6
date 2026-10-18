#!/usr/bin/env python3
"""Add the Dusty Guide Book (GT6's ``IL.Book_Loot_Guide``) to the port.

GT6 registers it as a MultiItemBooks entry (``MultiItemBooks.java:67``,
``addItem(32765, "Dusty Guide Book", "Loot: Some random Manual or so", …,
new Behavior_Drop_Loot("gt.books"))``): a loot item that a chest hands out and that the player
right-clicks to receive a random manual from the ``gt.books`` table.

The port registers it in ``GTMultiItems`` (a hand-written registration, see that file) because it is
the only MultiItemBooks entry the loot chain needs, so this tool only adds the assets:

  * ``textures/item/books/dusty_guide_book.png`` — GT6's texture 32765
  * ``models/item/dusty_guide_book.json``        — the usual generated item model
  * lang keys (en_us + zh_cn): name, GT6 tooltip, and the loot-bag use hint
    (``Behavior_Drop_Loot.getAdditionalToolTips``)

Usage:  python tools/generate_guide_book_assets.py
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
    r"\textures\items\gt.multiitem.books\32765.png")
ASSETS = native_path(ROOT / "src" / "main" / "resources" / "assets" / "gregtech")
ITEM_ID = "dusty_guide_book"
TEXTURE = ASSETS / "textures" / "item" / "books" / f"{ITEM_ID}.png"
MODEL = ASSETS / "models" / "item" / f"{ITEM_ID}.json"

EN = {
    f"item.gregtech.{ITEM_ID}": "Dusty Guide Book",
    f"item.gregtech.{ITEM_ID}.tooltip": "Loot: Some random Manual or so",
    "gregtech.tooltip.loot_bag": "Rightclick this on a Block to loot",
}
ZH = {
    f"item.gregtech.{ITEM_ID}": "布满灰尘的指南书",
    f"item.gregtech.{ITEM_ID}.tooltip": "战利品：随机一本手册",
    "gregtech.tooltip.loot_bag": "对着一方块右键即可开启",
}


def add_lang(path: pathlib.Path, keys: dict[str, str]) -> int:
    text = path.read_text(encoding="utf-8")
    data = json.loads(text)
    added = 0
    for key, value in keys.items():
        if key not in data:
            data[key] = value
            added += 1
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return added


def main() -> None:
    if not GT6_TEX.exists():
        raise SystemExit(f"GT6 texture missing: {GT6_TEX}")
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(GT6_TEX, TEXTURE)
    MODEL.write_text(json.dumps(
        {"parent": "minecraft:item/generated",
         "textures": {"layer0": f"gregtech:item/books/{ITEM_ID}"}}, indent=2) + "\n", encoding="utf-8")

    lang = ASSETS / "lang"
    print(f"en_us: {add_lang(lang / 'en_us.json', EN)} keys added")
    print(f"zh_cn: {add_lang(lang / 'zh_cn.json', ZH)} keys added")
    print("wrote", TEXTURE.relative_to(ROOT), "and", MODEL.relative_to(ROOT))


if __name__ == "__main__":
    main()
