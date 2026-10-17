#!/usr/bin/env python3
"""Generate forge:composite model JSONs and update blockstate JSONs for Wave 38 blocks."""
import json, os

BASE = "platform-shared/src/main/resources/assets/gregtech"
MODEL_DIR = f"{BASE}/models/block/machine"
BLOCKSTATE_DIR = f"{BASE}/blockstates"

DISPLAY = {
    "gui": {"rotation": [30, 45, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]}
}

def cube_element():
    return [{"from": [0, 0, 0], "to": [16, 16, 16],
        "faces": {
            "down":  {"texture": "#down",  "cullface": "down",  "tintindex": 0},
            "up":    {"texture": "#up",    "cullface": "up",    "tintindex": 0},
            "north": {"texture": "#north", "cullface": "north", "tintindex": 0},
            "south": {"texture": "#south", "cullface": "south", "tintindex": 0},
            "west":  {"texture": "#west",  "cullface": "west",  "tintindex": 0},
            "east":  {"texture": "#east",  "cullface": "east",  "tintindex": 0}
        }}]

def cube_element_overlay():
    return [{"from": [0, 0, 0], "to": [16, 16, 16],
        "faces": {
            "down":  {"texture": "#down",  "cullface": "down"},
            "up":    {"texture": "#up",    "cullface": "up"},
            "north": {"texture": "#north", "cullface": "north"},
            "south": {"texture": "#south", "cullface": "south"},
            "west":  {"texture": "#west",  "cullface": "west"},
            "east":  {"texture": "#east",  "cullface": "east"}
        }}]

def make_composite(texture_path, faces):
    """faces: dict of face->suffix like {'down':'bottom','up':'top','north':'front','south':'back','west':'side','east':'side'}"""
    return {
        "loader": "forge:composite",
        "display": DISPLAY,
        "children": {
            "layer0": {
                "parent": "minecraft:block/block",
                "textures": {d: f"gregtech:block/machines/{texture_path}/colored/{s}" for d, s in faces.items()},
                "elements": cube_element(),
                "render_type": "minecraft:solid"
            },
            "layer1": {
                "parent": "minecraft:block/block",
                "textures": {d: f"gregtech:block/machines/{texture_path}/overlay/{s}" for d, s in faces.items()},
                "elements": cube_element_overlay(),
                "render_type": "minecraft:cutout"
            }
        }
    }

# --- 1. Gearbox: all 6 faces same "side" ---
os.makedirs(f"{MODEL_DIR}/gearbox", exist_ok=True)
faces_all_side = {d: "side" for d in ["down","up","north","south","west","east"]}
with open(f"{MODEL_DIR}/gearbox/gearbox.json", "w") as f:
    json.dump(make_composite("gearbox", faces_all_side), f, indent=2)

# --- 2. Pump: directional (front=output, back=input) ---
os.makedirs(f"{MODEL_DIR}/pump", exist_ok=True)
faces_pump = {"down": "bottom", "up": "top", "north": "front", "south": "back", "west": "side", "east": "side"}
with open(f"{MODEL_DIR}/pump/rotational_pump.json", "w") as f:
    json.dump(make_composite("pump", faces_pump), f, indent=2)

# --- 3. Rotation Transformer: directional ---
os.makedirs(f"{MODEL_DIR}/transformer", exist_ok=True)
with open(f"{MODEL_DIR}/transformer/rotation_transformer.json", "w") as f:
    json.dump(make_composite("transformer", faces_pump), f, indent=2)

# --- Update blockstate JSONs ---
# Gearboxes: FACING variants -> gearbox model
materials = ["wood", "bronze", "steel", "titanium", "tungstensteel"]
for mat in materials:
    bs = {
        "variants": {
            "facing=down":  {"model": "gregtech:block/machine/gearbox/gearbox", "x": 90},
            "facing=up":    {"model": "gregtech:block/machine/gearbox/gearbox", "x": 270},
            "facing=north": {"model": "gregtech:block/machine/gearbox/gearbox"},
            "facing=south": {"model": "gregtech:block/machine/gearbox/gearbox", "y": 180},
            "facing=west":  {"model": "gregtech:block/machine/gearbox/gearbox", "y": 270},
            "facing=east":  {"model": "gregtech:block/machine/gearbox/gearbox", "y": 90}
        }
    }
    with open(f"{BLOCKSTATE_DIR}/gearbox_{mat}.json", "w") as f:
        json.dump(bs, f, indent=2)

# Pumps: FACING variants -> pump model
pump_mats = ["bronze", "steel", "titanium", "tungstensteel"]
for mat in pump_mats:
    bs = {
        "variants": {
            "facing=down":  {"model": "gregtech:block/machine/pump/rotational_pump", "x": 90},
            "facing=up":    {"model": "gregtech:block/machine/pump/rotational_pump", "x": 270},
            "facing=north": {"model": "gregtech:block/machine/pump/rotational_pump"},
            "facing=south": {"model": "gregtech:block/machine/pump/rotational_pump", "y": 180},
            "facing=west":  {"model": "gregtech:block/machine/pump/rotational_pump", "y": 270},
            "facing=east":  {"model": "gregtech:block/machine/pump/rotational_pump", "y": 90}
        }
    }
    with open(f"{BLOCKSTATE_DIR}/rotational_pump_{mat}.json", "w") as f:
        json.dump(bs, f, indent=2)

# Rotation Transformers: FACING variants -> transformer model
trans_mats = ["bronze", "steel", "titanium", "tungstensteel"]
for mat in trans_mats:
    bs = {
        "variants": {
            "facing=down":  {"model": "gregtech:block/machine/transformer/rotation_transformer", "x": 90},
            "facing=up":    {"model": "gregtech:block/machine/transformer/rotation_transformer", "x": 270},
            "facing=north": {"model": "gregtech:block/machine/transformer/rotation_transformer"},
            "facing=south": {"model": "gregtech:block/machine/transformer/rotation_transformer", "y": 180},
            "facing=west":  {"model": "gregtech:block/machine/transformer/rotation_transformer", "y": 270},
            "facing=east":  {"model": "gregtech:block/machine/transformer/rotation_transformer", "y": 90}
        }
    }
    with open(f"{BLOCKSTATE_DIR}/rotation_transformer_{mat}.json", "w") as f:
        json.dump(bs, f, indent=2)

# --- Update item models to point to same composite models ---
ITEM_DIR = f"{BASE}/models/item"
for mat in materials:
    im = {"model": f"gregtech:block/machine/gearbox/gearbox"}
    with open(f"{ITEM_DIR}/gearbox_{mat}.json", "w") as f:
        json.dump(im, f, indent=2)

for mat in pump_mats:
    im = {"model": "gregtech:block/machine/pump/rotational_pump"}
    with open(f"{ITEM_DIR}/rotational_pump_{mat}.json", "w") as f:
        json.dump(im, f, indent=2)

for mat in trans_mats:
    im = {"model": "gregtech:block/machine/transformer/rotation_transformer"}
    with open(f"{ITEM_DIR}/rotation_transformer_{mat}.json", "w") as f:
        json.dump(im, f, indent=2)

# --- Axle item models: keep simple rod model but reference proper texture ---
# The in-world axles use PipeWireBakedModel; items use the static JSON
# Update axle item models to reference the existing axle icon texture
for mat in materials:
    for size in range(1, 5):
        name = f"axle_{mat}_{size}"
        im = {"parent": "item/generated", "textures": {"layer0": f"gregtech:block/iconsets/axle"}}
        with open(f"{ITEM_DIR}/{name}.json", "w") as f:
            json.dump(im, f, indent=2)

# Axle blockstates stay as-is (single variant — PipeWireBakedModel handles dynamic rendering)

print("All files updated successfully.")
print(f"  Gearbox models: {len(materials)} blockstates + 1 model")
print(f"  Pump models: {len(pump_mats)} blockstates + 1 model")
print(f"  Transformer models: {len(trans_mats)} blockstates + 1 model")
print(f"  Axle item models: {len(materials) * 4} items")
