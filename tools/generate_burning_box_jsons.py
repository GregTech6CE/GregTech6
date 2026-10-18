#!/usr/bin/env python3
"""Generate blockstate + model JSONs for liquid/gas/fluidized-bed burning boxes."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

import json, os, shutil

BASE = str(native_path("src/main/resources/assets/gt6ce"))
BS_DIR = os.path.join(BASE, "blockstates")
MODEL_DIR = os.path.join(BASE, "models/block/machine")

# Texture set name -> directory name
TEXTURE_SETS = {
    "burning_liquid": "burning_liquid",
    "burning_gas": "burning_gas",
    "burning_fluidbed": "burning_fluidbed",
}

# Material suffixes used in GTMachines registrations
MATERIALS = [
    "lead", "bismuth", "bronze", "arsenic_copper", "arsenic_bronze",
    "invar", "steel", "chromium", "titanium", "netherite",
    "tungsten", "tungsten_steel", "ta4hfc5", "ultimet",
]

# Direction -> texture mappings (north/south/east/west are world directions)
# "front/back/left/right" are the named faces in the texture set
DIR_TEX = {
    "north": {"north": "front", "south": "back", "west": "left", "east": "right"},
    "south": {"north": "back", "south": "front", "west": "right", "east": "left"},
    "west":  {"north": "right", "south": "left", "west": "back", "east": "front"},
    "east":  {"north": "left", "south": "right", "west": "front", "east": "back"},
}

TEX_BASE = "gt6ce:block/machines/generators"


def make_direction_model(tex_set, facing, lit):
    """Create a forge:composite model for one direction + lit state."""
    overlay = "overlay_active" if lit else "overlay"
    mapping = DIR_TEX[facing]

    def layer(tex_dir, tinted):
        tex = {}
        tex["down"] = f"{TEX_BASE}/{tex_set}/{tex_dir}/bottom"
        tex["up"] = f"{TEX_BASE}/{tex_set}/{tex_dir}/top"
        for world_dir, named_face in mapping.items():
            tex[world_dir] = f"{TEX_BASE}/{tex_set}/{tex_dir}/{named_face}"

        faces = {}
        for face in ["down", "up", "north", "south", "west", "east"]:
            fd = {"texture": f"#{face}", "cullface": face}
            if tinted:
                fd["tintindex"] = 0
            faces[face] = fd

        return {
            "parent": "minecraft:block/block",
            "textures": tex,
            "elements": [{
                "from": [0, 0, 0],
                "to": [16, 16, 16],
                "faces": faces,
            }],
            "render_type": "minecraft:solid" if tinted else "minecraft:cutout",
        }

    return {
        "parent": "minecraft:block/block",
        "textures": {"particle": f"{TEX_BASE}/{tex_set}/colored/bottom"},
        "loader": "forge:composite",
        "children": {
            "layer0": layer("colored", True),
            "layer1": layer(overlay, False),
        },
    }


def make_model_dir(tex_set):
    """Create the model subdirectory for a texture set."""
    d = os.path.join(MODEL_DIR, tex_set)
    os.makedirs(d, exist_ok=True)
    for facing in ["north", "south", "west", "east"]:
        for lit, suffix in [(False, "off"), (True, "on")]:
            path = os.path.join(d, f"{facing}_{suffix}.json")
            model = make_direction_model(tex_set, facing, lit)
            with open(path, "w", encoding="utf-8") as f:
                json.dump(model, f, indent=2)


def make_model_id(tex_set):
    """Generate the base model ID for use in blockstates."""
    return f"gt6ce:block/machine/{tex_set}"


def make_blockstate(tex_set):
    """Generate a blockstate JSON."""
    model_id = make_model_id(tex_set)
    variants = {}
    for facing in ["north", "south", "west", "east"]:
        variants[f"facing={facing},lit=false"] = {"model": f"{model_id}/{facing}_off"}
        variants[f"facing={facing},lit=true"] = {"model": f"{model_id}/{facing}_on"}
    return {"variants": variants}


def main():
    # Step 1: Create model files for each texture set
    print("Creating model files...")
    for tex_set in TEXTURE_SETS:
        make_model_dir(tex_set)
        print(f"  Models: {tex_set} (8 files)")

    # Step 2: Create blockstate JSONs for each block
    print("\nCreating blockstate files...")
    blockstates = 0
    for tex_key, tex_set in TEXTURE_SETS.items():
        # Normal variants
        for mat in MATERIALS:
            block_id = f"burning_box_{tex_key[8:]}_{mat}"  # burning_box_liquid_lead etc
            bs = make_blockstate(tex_set)
            path = os.path.join(BS_DIR, f"{block_id}.json")
            with open(path, "w", encoding="utf-8") as f:
                json.dump(bs, f, indent=2)
            blockstates += 1
        # Dense variants
        for mat in MATERIALS:
            block_id = f"burning_box_{tex_key[8:]}_dense_{mat}"
            bs = make_blockstate(tex_set)
            path = os.path.join(BS_DIR, f"{block_id}.json")
            with open(path, "w", encoding="utf-8") as f:
                json.dump(bs, f, indent=2)
            blockstates += 1

    print(f"  Blockstates: {blockstates} files")
    print(f"\nDone! Total: {blockstates} blockstates + {len(TEXTURE_SETS) * 8} models")

    # Step 3: Copy textures from burning_solid as placeholder
    src_tex = os.path.join(BASE, "textures/block/machines/generators/burning_solid")
    if os.path.isdir(src_tex):
        for tex_set in TEXTURE_SETS.values():
            dst_tex = os.path.join(BASE, f"textures/block/machines/generators/{tex_set}")
            if not os.path.isdir(dst_tex):
                print(f"\nCopying textures: burning_solid -> {tex_set}")
                shutil.copytree(src_tex, dst_tex)
            else:
                print(f"\nTexture dir already exists: {tex_set}")
    else:
        print(f"\nWARNING: Source textures not found at {src_tex}")
        print("You need to create textures for:")
        for tex_set in TEXTURE_SETS.values():
            print(f"  - textures/block/machines/generators/{tex_set}/colored/*.png")
            print(f"  - textures/block/machines/generators/{tex_set}/overlay/*.png")
            print(f"  - textures/block/machines/generators/{tex_set}/overlay_active/*.png")


if __name__ == "__main__":
    main()
