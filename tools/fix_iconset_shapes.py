#!/usr/bin/env python3
"""Iconset block shape + render-type fixes.

- Plants (flowers/glowtus/saplings/fluid_spring) -> cross models, cutout.
- flower_hexalily -> lily-pad model (flat, water-placed).
- rail_* textures belong to the actual material tracks; do not generate iconset blocks.
- Any iconset texture with transparent pixels -> render_type cutout on its block
  model (textures rendered on the solid layer show white where alpha < 1).
- Renames the UPPERCASE xmas .png.mcmeta files to match the lowercase .png names
  (animation metadata was being ignored).
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import json
import os

from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, str(native_path("src")), "main", "resources", "assets", "gregtech")
BLOCKSTATES = os.path.join(ASSETS, "blockstates")
BLOCK_MODELS = os.path.join(ASSETS, "models", "block", "iconsets")
ITEM_MODELS = os.path.join(ASSETS, "models", "item")
TEXTURES = os.path.join(ASSETS, "textures", "block", "iconsets")


def write(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def block_id(icon):
    return "block_" + icon if icon.startswith("ore_") else icon


def is_plant(n):
    return ((n.startswith("flower_") and n != "flower_hexalily")
            or n.startswith("glowtus_") or n.startswith("sapling_") or n == "fluid_spring")


def has_transparency(png_path):
    try:
        img = Image.open(png_path).convert("RGBA")
    except Exception:
        return False
    alpha = img.getchannel("A")
    lo, hi = alpha.getextrema()
    return lo < 255


def main():
    # 1. Fix the xmas animation metadata file names.
    renamed_meta = 0
    for fname in os.listdir(TEXTURES):
        if fname.endswith(".png.mcmeta"):
            lower = fname.lower()
            if lower != fname:
                target = os.path.join(TEXTURES, lower)
                if os.path.exists(target):
                    os.remove(os.path.join(TEXTURES, fname))
                else:
                    os.replace(os.path.join(TEXTURES, fname), target)
                renamed_meta += 1

    plants = 0
    cutouts = 0

    for fname in sorted(os.listdir(TEXTURES)):
        if not fname.endswith(".png"):
            continue
        icon = fname[:-4]
        if icon.startswith("rail_"):
            continue  # texture input for generate_track_assets.py, never a standalone block
        bid = block_id(icon)
        model_path = os.path.join(BLOCK_MODELS, bid + ".json")
        if not os.path.exists(model_path):
            continue  # composite/component textures without a standalone block
        tex = "gregtech:block/iconsets/" + icon

        if icon == "flower_hexalily":
            write(model_path, {
                "parent": "minecraft:block/lily_pad",
                "render_type": "minecraft:cutout",
                "textures": {"particle": tex, "texture": tex},
            })
            write(os.path.join(ITEM_MODELS, bid + ".json"), {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": tex},
            })
            plants += 1
            continue

        if is_plant(icon):
            write(model_path, {
                "parent": "minecraft:block/cross",
                "render_type": "minecraft:cutout",
                "textures": {"cross": tex},
            })
            write(os.path.join(ITEM_MODELS, bid + ".json"), {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": tex},
            })
            plants += 1
            continue

        # Generic: transparent textures need the cutout render type.
        if has_transparency(os.path.join(TEXTURES, fname)):
            with open(model_path, encoding="utf-8") as f:
                model = json.load(f)
            if model.get("render_type") != "minecraft:cutout":
                model["render_type"] = "minecraft:cutout"
                write(model_path, model)
                cutouts += 1

    print(f"plants/lily: {plants}, extra cutouts: {cutouts}, mcmeta renamed: {renamed_meta}")


if __name__ == "__main__":
    main()
