#!/usr/bin/env python3
"""Add GT6's Loot Bottle (``IL.Bottle_Loot``) to the port (§25).

``MultiItemBottles.java:367`` registers it as ``addItem(32761, "Clouded Bottle", "Loot: A random
Bottle", …, new Behavior_Drop_Loot("gt.bottles"))`` — the item that opens GT's ``gt.bottles`` table,
the last table the port had deferred. It is registered by hand in ``GTMultiItems`` (a MultiItemBottles
entry, like the Dusty Guide Book), so this tool adds the assets:

  * ``textures/item/bottles/loot_bottle.png`` — GT6's texture 32761
  * ``models/item/loot_bottle.json``          — the usual generated item model
  * lang keys (en_us + zh_cn): the GT6 display name and tooltip

Usage:  python tools/generate_loot_bottle_assets.py
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
    r"\textures\items\gt.multiitem.bottles\32761.png")
ASSETS = native_path(ROOT / "src" / "main" / "resources" / "assets" / "gregtech")
ITEM_ID = "loot_bottle"
TEXTURE = ASSETS / "textures" / "item" / "bottles" / f"{ITEM_ID}.png"
MODEL = ASSETS / "models" / "item" / f"{ITEM_ID}.json"

EN = {
    f"item.gregtech.{ITEM_ID}": "Clouded Bottle",
    f"item.gregtech.{ITEM_ID}.tooltip": "Loot: A random Bottle",
}
ZH = {
    f"item.gregtech.{ITEM_ID}": "浑浊的瓶子",
    f"item.gregtech.{ITEM_ID}.tooltip": "战利品：随机一瓶东西",
}


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
    if not GT6_TEX.exists():
        raise SystemExit(f"GT6 texture missing: {GT6_TEX}")
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(GT6_TEX, TEXTURE)
    MODEL.write_text(json.dumps(
        {"parent": "minecraft:item/generated",
         "textures": {"layer0": f"gregtech:item/bottles/{ITEM_ID}"}}, indent=2) + "\n", encoding="utf-8")

    lang = ASSETS / "lang"
    print(f"en_us: {add_lang(lang / 'en_us.json', EN)} keys added")
    print(f"zh_cn: {add_lang(lang / 'zh_cn.json', ZH)} keys added")
    print("wrote", TEXTURE.relative_to(ROOT), "and", MODEL.relative_to(ROOT))


if __name__ == "__main__":
    main()
