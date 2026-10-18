#!/usr/bin/env python3
"""Wave 16 iconset cleanup: delete wrongly-added blocks' assets, glowtus -> lily pad."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import json
import os

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, str(native_path("src")), "main", "resources", "assets", "gregtech")
BLOCKSTATES = os.path.join(ASSETS, "blockstates")
BLOCK_MODELS = os.path.join(ASSETS, "models", "block", "iconsets")
ITEM_MODELS = os.path.join(ASSETS, "models", "item")
TEXTURES = os.path.join(ASSETS, "textures", "block", "iconsets")

REMOVED = [
    "atom", "axle", "path_slab", "pipe_restrictor", "plate", "void",
    "fiber_wire", "duct_tape", "coin",
    "insulation_bundled", "insulation_full", "insulation_huge", "insulation_large",
    "insulation_medium", "insulation_private", "insulation_small", "insulation_tiny",
    # removed composites
    "bottlecrate", "powercell", "path", "zpm", "aneutronic_fusion", "coins",
]

GLOWTUS = [f"glowtus_{c}" for c in
           ["black", "blue", "brown", "cyan", "gray", "green", "light_blue", "light_gray",
            "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow"]]


def write(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def main():
    deleted = 0
    for name in REMOVED:
        for base, suffix in ((BLOCKSTATES, ".json"), (BLOCK_MODELS, ".json"),
                             (BLOCK_MODELS, "_horizontal.json"), (ITEM_MODELS, ".json")):
            path = os.path.join(base, name + suffix)
            if os.path.exists(path):
                os.remove(path)
                deleted += 1

    for name in GLOWTUS:
        tex = "gregtech:block/iconsets/" + name
        write(os.path.join(BLOCK_MODELS, name + ".json"), {
            "parent": "minecraft:block/lily_pad",
            "render_type": "minecraft:cutout",
            "textures": {"particle": tex, "texture": tex},
        })
        write(os.path.join(ITEM_MODELS, name + ".json"), {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": tex},
        })

    # xmas animation sanity check
    for name in ["leaves_bluespruce_xmas", "leaves_opaque_bluespruce_xmas"]:
        meta = os.path.join(TEXTURES, name + ".png.mcmeta")
        status = "OK" if os.path.exists(meta) else "MISSING"
        content = open(meta, encoding="utf-8").read().strip() if os.path.exists(meta) else ""
        print(f"xmas mcmeta {name}: {status} {content}")

    print(f"deleted {deleted} asset files; {len(GLOWTUS)} glowtus converted to lily pads")


if __name__ == "__main__":
    main()
