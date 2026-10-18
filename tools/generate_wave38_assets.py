#!/usr/bin/env python3
"""Generate blockstate + model JSONs for Wave 38: axles, gearboxes, rotation transformers, pumps."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import json, os

ASSETS = str(native_path("src/main/resources/assets/gt6ce"))
BLOCKSTATES = f"{ASSETS}/blockstates"
MODELS_BLOCK = f"{ASSETS}/models/block"
MODELS_ITEM = f"{ASSETS}/models/item"

os.makedirs(BLOCKSTATES, exist_ok=True)
os.makedirs(MODELS_BLOCK, exist_ok=True)
os.makedirs(MODELS_ITEM, exist_ok=True)

def write_json(path, data):
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2)
    print(f"  {path}")

# ── Axles ──────────────────────────────────────────────────────────────────
AXLE_MATERIALS = ["wood", "bronze", "steel", "titanium", "tungstensteel"]
AXLE_SIZES = [1, 2, 3, 4]

for mat in AXLE_MATERIALS:
    for size in AXLE_SIZES:
        id_str = f"axle_{mat}_{size}"
        tex = "block/iconsets/axle" if mat == "wood" else "block/material_icons/metallic/gear"
        tint = 0 if mat == "wood" else 1

        # Blockstate: simple variant fallback (dynamic model replaces at runtime)
        bs = {"variants": {"": {"model": f"gt6ce:block/{id_str}"}}}
        write_json(f"{BLOCKSTATES}/{id_str}.json", bs)

        # Block model: thin rod core
        model = {
            "parent": "block/block",
            "textures": {"rod": tex},
            "elements": [{
                "from": [7, 7, 7], "to": [9, 9, 9],
                "faces": {
                    "down": {"texture": "#rod", "tintindex": tint},
                    "up": {"texture": "#rod", "tintindex": tint},
                    "north": {"texture": "#rod", "tintindex": tint},
                    "south": {"texture": "#rod", "tintindex": tint},
                    "west": {"texture": "#rod", "tintindex": tint},
                    "east": {"texture": "#rod", "tintindex": tint}
                }
            }]
        }
        write_json(f"{MODELS_BLOCK}/{id_str}.json", model)

        # Item model
        item = {"parent": f"gt6ce:block/{id_str}"}
        write_json(f"{MODELS_ITEM}/{id_str}.json", item)

# ── Gearboxes ──────────────────────────────────────────────────────────────
for mat in AXLE_MATERIALS:
    id_str = f"gearbox_{mat}"
    tex = "block/iconsets/gearbox"

    # Blockstate with FACING
    bs = {"variants": {
        "facing=down":  {"model": f"gt6ce:block/{id_str}", "x": 90},
        "facing=up":    {"model": f"gt6ce:block/{id_str}", "x": 270},
        "facing=north": {"model": f"gt6ce:block/{id_str}"},
        "facing=south": {"model": f"gt6ce:block/{id_str}", "y": 180},
        "facing=west":  {"model": f"gt6ce:block/{id_str}", "y": 270},
        "facing=east":  {"model": f"gt6ce:block/{id_str}", "y": 90},
    }}
    write_json(f"{BLOCKSTATES}/{id_str}.json", bs)

    model = {
        "parent": "block/cube",
        "textures": {
            "particle": tex,
            "down": tex,
            "up": tex,
            "north": tex,
            "south": tex,
            "west": tex,
            "east": tex
        }
    }
    write_json(f"{MODELS_BLOCK}/{id_str}.json", model)
    item = {"parent": f"gt6ce:block/{id_str}"}
    write_json(f"{MODELS_ITEM}/{id_str}.json", item)

# ── Rotation Transformers ──────────────────────────────────────────────────
RT_MATS = ["bronze", "steel", "titanium", "tungstensteel"]

for mat in RT_MATS:
    id_str = f"rotation_transformer_{mat}"
    tex = "block/material_icons/metallic/machine_bottom"

    bs = {"variants": {
        "facing=down":  {"model": f"gt6ce:block/{id_str}", "x": 90},
        "facing=up":    {"model": f"gt6ce:block/{id_str}", "x": 270},
        "facing=north": {"model": f"gt6ce:block/{id_str}"},
        "facing=south": {"model": f"gt6ce:block/{id_str}", "y": 180},
        "facing=west":  {"model": f"gt6ce:block/{id_str}", "y": 270},
        "facing=east":  {"model": f"gt6ce:block/{id_str}", "y": 90},
    }}
    write_json(f"{BLOCKSTATES}/{id_str}.json", bs)

    model = {
        "parent": "block/cube",
        "textures": {
            "particle": tex,
            "down": tex, "up": tex,
            "north": tex, "south": tex,
            "west": tex, "east": tex
        }
    }
    write_json(f"{MODELS_BLOCK}/{id_str}.json", model)
    item = {"parent": f"gt6ce:block/{id_str}"}
    write_json(f"{MODELS_ITEM}/{id_str}.json", item)

# ── Pumps ──────────────────────────────────────────────────────────────────
PUMP_MATS = ["bronze", "steel", "titanium", "tungstensteel"]

for mat in PUMP_MATS:
    id_str = f"rotational_pump_{mat}"
    tex = "block/material_icons/metallic/machine_bottom"

    bs = {"variants": {
        "facing=down":  {"model": f"gt6ce:block/{id_str}", "x": 90},
        "facing=up":    {"model": f"gt6ce:block/{id_str}", "x": 270},
        "facing=north": {"model": f"gt6ce:block/{id_str}"},
        "facing=south": {"model": f"gt6ce:block/{id_str}", "y": 180},
        "facing=west":  {"model": f"gt6ce:block/{id_str}", "y": 270},
        "facing=east":  {"model": f"gt6ce:block/{id_str}", "y": 90},
    }}
    write_json(f"{BLOCKSTATES}/{id_str}.json", bs)

    model = {
        "parent": "block/cube",
        "textures": {
            "particle": tex,
            "down": tex, "up": tex,
            "north": tex, "south": tex,
            "west": tex, "east": tex
        }
    }
    write_json(f"{MODELS_BLOCK}/{id_str}.json", model)
    item = {"parent": f"gt6ce:block/{id_str}"}
    write_json(f"{MODELS_ITEM}/{id_str}.json", item)

print("\nDone! Generated all Wave 38 asset files.")
