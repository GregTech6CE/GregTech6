#!/usr/bin/env python3
"""Rock/twig model variants: 3 pebble shapes x 10 ground textures, coordinate-random
via blockstate variant arrays; twigs get 3 shapes x 4 rotations."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import json
import os

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, str(native_path("src")), "main", "resources", "assets", "gt6ce")
BLOCKSTATES = os.path.join(ASSETS, "blockstates")
BLOCK_MODELS = os.path.join(ASSETS, "models", "block")

GROUNDS = {
    "stone": "minecraft:block/stone",
    "sand": "minecraft:block/sand",
    "red_sand": "minecraft:block/red_sand",
    "gravel": "minecraft:block/gravel",
    "deepslate": "minecraft:block/deepslate",
    "granite": "minecraft:block/granite",
    "diorite": "minecraft:block/diorite",
    "andesite": "minecraft:block/andesite",
    "netherrack": "minecraft:block/netherrack",
    "end_stone": "minecraft:block/end_stone",
}

# Pebble shapes: (elements as (from, to) box lists). With 4 rotations each this
# yields shapes*4 visual variants per ground.
SHAPES = [
    [((4, 0, 5), (10, 4, 11)), ((8, 0, 8), (12, 3, 12)), ((6, 0, 3), (9, 2, 6))],
    [((5, 0, 4), (11, 3, 10)), ((3, 0, 8), (7, 2, 12)), ((9, 0, 10), (13, 4, 14))],
    [((6, 0, 6), (11, 5, 11)), ((4, 0, 9), (7, 2, 12))],
    [((3, 0, 3), (8, 3, 8)), ((7, 0, 7), (10, 2, 10)), ((10, 0, 4), (13, 2, 7)), ((5, 0, 10), (8, 1, 13))],
    [((6, 0, 4), (12, 2, 9)), ((5, 0, 8), (9, 4, 12))],
    [((7, 0, 7), (10, 3, 10)), ((4, 0, 5), (7, 1, 8)), ((10, 0, 9), (12, 1, 11)), ((8, 0, 3), (10, 1, 5)), ((3, 0, 10), (5, 1, 12))],
]

# Twig layouts: (boxes with optional y-rotation per element).
# Element rotations must be multiples of 22.5 within +-45 (vanilla model rule).
TWIG_SHAPES = [
    [((1, 0, 3), (15, 2, 5), 22.5), ((3, 0, 8), (14, 2, 10), -22.5), ((6, 0, 11), (13, 2, 13), 0)],
    [((2, 0, 2), (14, 2, 4), -45), ((4, 0, 6), (15, 2, 8), 22.5), ((1, 0, 11), (10, 2, 13), 45), ((11, 0, 10), (14, 2, 12), 0)],
    [((3, 0, 7), (15, 2, 9), 22.5), ((5, 0, 4), (12, 2, 6), -22.5)],
    [((1, 0, 5), (9, 2, 7), 45), ((7, 0, 8), (15, 2, 10), -22.5), ((4, 0, 12), (11, 2, 14), 22.5), ((10, 0, 2), (14, 2, 4), -45)],
]


def face(uv):
    return {"texture": "#rock", "uv": uv}


def element(box):
    (x0, y0, z0), (x1, y1, z1) = box
    return {
        "from": [x0, y0, z0],
        "to": [x1, y1, z1],
        "faces": {
            "down":  face([x0, z0, x1, z1]),
            "up":    face([x0, z0, x1, z1]),
            "north": face([x0, 16 - y1, x1, 16.0]),
            "south": face([x0, 16 - y1, x1, 16.0]),
            "west":  face([z0, 16 - y1, z1, 16.0]),
            "east":  face([z0, 16 - y1, z1, 16.0]),
        },
    }


def write(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def main():
    variants = {}
    for ground, tex in GROUNDS.items():
        models = []
        for i, shape in enumerate(SHAPES):
            name = f"rock_{ground}_{i}"
            write(os.path.join(BLOCK_MODELS, name + ".json"), {
                "textures": {"particle": tex, "rock": tex},
                "elements": [element(b) for b in shape],
            })
            for rot in (0, 90, 180, 270):
                entry = {"model": "gt6ce:block/" + name}
                if rot:
                    entry["y"] = rot
                models.append(entry)
        variants[f"ground={ground}"] = models
    write(os.path.join(BLOCKSTATES, "rock.json"), {"variants": variants})
    # drop the old single model
    old = os.path.join(BLOCK_MODELS, "rock.json")
    if os.path.exists(old):
        os.remove(old)

    # Twigs: several stick layouts, each with 4 rotations.
    twig_variants = []
    for i, shape in enumerate(TWIG_SHAPES):
        name = f"twigs_{i}"
        elements = []
        for (x0, y0, z0), (x1, y1, z1), angle in [((a, b, c), (d, e, f), g)
                                                  for (a, b, c), (d, e, f), g in shape]:
            el = {
                "from": [x0, y0, z0],
                "to": [x1, y1, z1],
                "faces": {
                    "down":  {"texture": "#twig", "uv": [x0, z0, x1, z1]},
                    "up":    {"texture": "#twig", "uv": [x0, z0, x1, z1]},
                    "north": {"texture": "#twig", "uv": [x0, 14, x1, 16]},
                    "south": {"texture": "#twig", "uv": [x0, 14, x1, 16]},
                    "west":  {"texture": "#twig", "uv": [z0, 14, z1, 16]},
                    "east":  {"texture": "#twig", "uv": [z0, 14, z1, 16]},
                },
            }
            if angle:
                cx, cz = (x0 + x1) / 2, (z0 + z1) / 2
                el["rotation"] = {"origin": [cx, 1, cz], "axis": "y", "angle": angle}
            elements.append(el)
        write(os.path.join(BLOCK_MODELS, name + ".json"), {
            "render_type": "minecraft:cutout",
            "textures": {"particle": "minecraft:block/oak_log", "twig": "minecraft:block/oak_log"},
            "elements": elements,
        })
        for rot in (0, 90, 180, 270):
            entry = {"model": "gt6ce:block/" + name}
            if rot:
                entry["y"] = rot
            twig_variants.append(entry)
    write(os.path.join(BLOCKSTATES, "twigs.json"), {"variants": {"": twig_variants}})
    old_twigs = os.path.join(BLOCK_MODELS, "twigs.json")
    if os.path.exists(old_twigs):
        os.remove(old_twigs)
    print(f"rock models: {len(GROUNDS) * len(SHAPES)}, twig models: {len(TWIG_SHAPES)} x4 rotations")


if __name__ == "__main__":
    main()
