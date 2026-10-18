#!/usr/bin/env python3
"""Rename iconset block assets: drop the `iconset_` id prefix.

Registry ids change from `iconset_<name>` to `<name>`, except `ore_*` names which
become `special_ore_*` (plain `ore_<x>` would collide with the per-material
OreBlock ids). Renames blockstates, block models and item models and rewrites the
model references inside them. Texture paths (block/iconsets/<name>) are untouched.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import json
import os
import sys

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, str(native_path("src")), "main", "resources", "assets", "gregtech")
BLOCKSTATES = os.path.join(ASSETS, "blockstates")
BLOCK_MODELS = os.path.join(ASSETS, "models", "block", "iconsets")
ITEM_MODELS = os.path.join(ASSETS, "models", "item")


def new_id(icon_name: str) -> str:
    if icon_name.startswith("ore_"):
        return "special_" + icon_name
    return icon_name


def main() -> None:
    renamed = 0
    for fname in sorted(os.listdir(BLOCKSTATES)):
        if not fname.startswith("iconset_") or not fname.endswith(".json"):
            continue
        icon = fname[len("iconset_"):-len(".json")]
        nid = new_id(icon)

        # blockstate: rename + point at the renamed block model
        src = os.path.join(BLOCKSTATES, fname)
        with open(src, encoding="utf-8") as f:
            data = json.load(f)
        for variant in data.get("variants", {}).values():
            if isinstance(variant, dict) and variant.get("model", "").endswith("iconset_" + icon):
                variant["model"] = "gregtech:block/iconsets/" + nid
        with open(os.path.join(BLOCKSTATES, nid + ".json"), "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)
            f.write("\n")
        os.remove(src)

        # block model: rename (content references textures by icon name — unchanged)
        bsrc = os.path.join(BLOCK_MODELS, "iconset_" + icon + ".json")
        if os.path.exists(bsrc):
            os.replace(bsrc, os.path.join(BLOCK_MODELS, nid + ".json"))

        # item model: rename + re-point parent
        isrc = os.path.join(ITEM_MODELS, "iconset_" + icon + ".json")
        if os.path.exists(isrc):
            with open(isrc, encoding="utf-8") as f:
                idata = json.load(f)
            if idata.get("parent", "").endswith("iconset_" + icon):
                idata["parent"] = "gregtech:block/iconsets/" + nid
            with open(os.path.join(ITEM_MODELS, nid + ".json"), "w", encoding="utf-8") as f:
                json.dump(idata, f, indent=2)
                f.write("\n")
            os.remove(isrc)

        renamed += 1
    print(f"renamed {renamed} iconset asset sets")


if __name__ == "__main__":
    main()
