#!/usr/bin/env python3
"""Generate smeltery companion block assets (models, blockstates, lang)."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = native_path(ROOT / "src/main/resources/assets/gregtech")
BOWL_DIR = ASSETS / "models/block/machine/crucible_bowl"
EN_LANG = ASSETS / "lang/en_us.json"

# GT6 MultiTileEntityBasin — 1px walls, same inner/outer face pattern as crucible bowl (2px)
BASIN_BOXES = [
    {
        "from": [0, 0, 0],
        "to": [1, 16, 16],
        "faces": ["west", "east", "north", "south", "up"],
        "cull": ("west", "north", "south"),
    },
    {
        "from": [15, 0, 0],
        "to": [16, 16, 16],
        "faces": ["east", "west", "north", "south", "up"],
        "cull": ("east", "north", "south"),
    },
    {
        "from": [1, 0, 0],
        "to": [15, 16, 1],
        "faces": ["north", "south", "west", "east", "up"],
        "cull": ("north", "west", "east"),
    },
    {
        "from": [1, 0, 15],
        "to": [15, 16, 16],
        "faces": ["south", "north", "west", "east", "up"],
        "cull": ("south", "west", "east"),
    },
    {
        "from": [0, 0, 0],
        "to": [16, 1, 16],
        "faces": ["down", "up"],
        "cull": ("down",),
    },
]

# Crossing — user 16×16 patterns (y=1–2 floor, y=2–6 mid section)
CROSSING_BOXES = [
    {"from": [6, 1, 0], "to": [10, 2, 6]},
    {"from": [0, 1, 6], "to": [16, 2, 10]},
    {"from": [6, 1, 10], "to": [10, 2, 16]},
    {"from": [5, 2, 0], "to": [6, 6, 5]},
    {"from": [10, 2, 0], "to": [11, 6, 5]},
    {"from": [0, 2, 5], "to": [6, 6, 6]},
    {"from": [10, 2, 5], "to": [16, 6, 6]},
    {"from": [0, 2, 10], "to": [6, 6, 11]},
    {"from": [10, 2, 10], "to": [16, 6, 11]},
    {"from": [5, 2, 11], "to": [6, 6, 16]},
    {"from": [10, 2, 11], "to": [11, 6, 16]},
]

# GT6 faucet — hollow channel x/z 6–10 (GT6 width); skirt + tab
FAUCET_BOXES_BY_FACING = {
    "south": [
        {"from": [6, 1, 12], "to": [10, 2, 16], "cull": ("south",)},
        {"from": [5, 2, 12], "to": [6, 6, 16], "cull": ("south",)},
        {"from": [10, 2, 12], "to": [11, 6, 16], "cull": ("south",)},
    ],
    "north": [
        {"from": [6, 1, 0], "to": [10, 2, 4], "cull": ("north",)},
        {"from": [5, 2, 0], "to": [6, 6, 4], "cull": ("north",)},
        {"from": [10, 2, 0], "to": [11, 6, 4], "cull": ("north",)},
    ],
    "west": [
        {"from": [0, 1, 6], "to": [4, 2, 10], "cull": ("west",)},
        {"from": [0, 2, 5], "to": [4, 6, 6], "cull": ("west",)},
        {"from": [0, 2, 10], "to": [4, 6, 11], "cull": ("west",)},
    ],
    "east": [
        {"from": [12, 1, 6], "to": [16, 2, 10], "cull": ("east",)},
        {"from": [12, 2, 5], "to": [16, 6, 6], "cull": ("east",)},
        {"from": [12, 2, 10], "to": [16, 6, 11], "cull": ("east",)},
    ],
}

# GT6 MultiTileEntityMold hull (passes 1–18, empty shell — no molten cavity faces)
MOLD_BOXES = [
    {"from": [0, 0, 0], "to": [16, 1, 16], "cull": ("down", "north", "south", "west", "east")},
    {"from": [14, 0, 0], "to": [16, 4, 16], "cull": ("east",)},
    {"from": [0, 0, 14], "to": [16, 4, 16], "cull": ("south",)},
    {"from": [0, 0, 0], "to": [2, 4, 16], "cull": ("west",)},
    {"from": [0, 0, 0], "to": [16, 4, 2], "cull": ("north",)},
    {"from": [6, 4, 0], "to": [7, 6, 2], "cull": ("north",)},
    {"from": [9, 4, 0], "to": [10, 6, 2], "cull": ("north",)},
    {"from": [6, 6, 0], "to": [10, 7, 2], "cull": ("north",)},
    {"from": [6, 4, 14], "to": [7, 6, 16], "cull": ("south",)},
    {"from": [9, 4, 14], "to": [10, 6, 16], "cull": ("south",)},
    {"from": [6, 6, 14], "to": [10, 7, 16], "cull": ("south",)},
    {"from": [0, 4, 6], "to": [2, 6, 7], "cull": ("west",)},
    {"from": [0, 4, 9], "to": [2, 6, 10], "cull": ("west",)},
    {"from": [0, 6, 6], "to": [2, 7, 10], "cull": ("west",)},
    {"from": [14, 4, 6], "to": [16, 6, 7], "cull": ("east",)},
    {"from": [14, 4, 9], "to": [16, 6, 10], "cull": ("east",)},
    {"from": [14, 6, 6], "to": [16, 7, 10], "cull": ("east",)},
]

ALL_FACES = ("down", "up", "north", "south", "west", "east")
CULL_BY_FACE = {
    "down": "down",
    "up": "up",
    "north": "north",
    "south": "south",
    "west": "west",
    "east": "east",
}


def build_elements(boxes, texture_ref: str, tinted: bool) -> list:
    out = []
    for box in boxes:
        faces = box.get("faces", ALL_FACES)
        cull_faces = set(box.get("cull", ()))
        face_map = {}
        for face in faces:
            data = {"texture": texture_ref}
            if tinted:
                data["tintindex"] = 0
            if face in cull_faces:
                data["cullface"] = CULL_BY_FACE[face]
            face_map[face] = data
        out.append({"from": box["from"], "to": box["to"], "faces": face_map})
    return out


def composite_model(base: str, overlay: str, elements_tinted: list, elements_overlay: list) -> dict:
    return {
        "loader": "forge:composite",
        "children": {
            "layer0": {
                "parent": "minecraft:block/block",
                "textures": {"particle": base, "wall": base},
                "elements": elements_tinted,
                "render_type": "minecraft:solid",
            },
            "layer1": {
                "parent": "minecraft:block/block",
                "textures": {"wall": overlay},
                "elements": elements_overlay,
                "render_type": "minecraft:cutout",
            },
        },
    }


def write_shared_models(texture_set: str) -> None:
    base = f"gregtech:block/material_icons/{texture_set}/blocksolid"
    overlay = f"gregtech:block/material_icons/{texture_set}/blocksolid_overlay"

    simple_models = {
        "mold_basin": BASIN_BOXES,
        "crucible_mold": MOLD_BOXES,
        "crucible_crossing": CROSSING_BOXES,
    }
    for folder, boxes in simple_models.items():
        out_dir = ASSETS / f"models/block/machine/{folder}/{texture_set}"
        out_dir.mkdir(parents=True, exist_ok=True)
        model = composite_model(
            base,
            overlay,
            build_elements(boxes, "#wall", True),
            build_elements(boxes, "#wall", False),
        )
        (out_dir / "blocksolid.json").write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")

    faucet_dir = ASSETS / f"models/block/machine/crucible_faucet/{texture_set}"
    faucet_dir.mkdir(parents=True, exist_ok=True)
    for facing, boxes in FAUCET_BOXES_BY_FACING.items():
        model = composite_model(
            base,
            overlay,
            build_elements(boxes, "#wall", True),
            build_elements(boxes, "#wall", False),
        )
        (faucet_dir / f"blocksolid_{facing}.json").write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
    south_path = faucet_dir / "blocksolid_south.json"
    if south_path.exists():
        (faucet_dir / "blocksolid.json").write_text(south_path.read_text(encoding="utf-8"), encoding="utf-8")


def crucible_suffixes() -> list[str]:
    return sorted(
        path.stem[len("smelting_crucible_") :]
        for path in (ASSETS / "blockstates").glob("smelting_crucible_*.json")
    )


def write_blockstates(suffixes: list[str]) -> None:
    for suffix in suffixes:
        for prefix in ("mold", "mold_basin", "crucible_crossing"):
            data = {"variants": {"": {"model": f"gregtech:block/blocks/{prefix}_{suffix}"}}}
            (ASSETS / "blockstates" / f"{prefix}_{suffix}.json").write_text(
                json.dumps(data, indent=2) + "\n", encoding="utf-8")
        faucet = {
            "variants": {
                f"facing={facing}": {"model": f"gregtech:block/blocks/crucible_faucet_{suffix}"}
                for facing in ("north", "south", "east", "west")
            }
        }
        (ASSETS / "blockstates" / f"crucible_faucet_{suffix}.json").write_text(
            json.dumps(faucet, indent=2) + "\n", encoding="utf-8")


def write_item_models(suffixes: list[str]) -> None:
    for suffix in suffixes:
        for prefix in ("mold", "mold_basin", "crucible_crossing", "crucible_faucet"):
            block_id = f"{prefix}_{suffix}"
            data = {"parent": f"gregtech:block/blocks/{block_id}"}
            (ASSETS / "models/item" / f"{block_id}.json").write_text(
                json.dumps(data, indent=2) + "\n", encoding="utf-8")


def update_lang(suffixes: list[str]) -> None:
    en = json.loads(EN_LANG.read_text(encoding="utf-8-sig"))
    en["itemGroup.gregtech.smelting_crucibles"] = "Crucibles / Molds"
    labels = {
        "mold": "Mold",
        "mold_basin": "Mold Basin",
        "crucible_faucet": "Crucible Faucet",
        "crucible_crossing": "Crucible Crossing",
    }
    for suffix in suffixes:
        crucible_en = en.get(f"block.gregtech.smelting_crucible_{suffix}", "")
        mat_match = re.search(r"\((.+)\)\s*$", crucible_en)
        mat_name = mat_match.group(1) if mat_match else suffix.replace("_", " ").title()
        for prefix, label in labels.items():
            en[f"block.gregtech.{prefix}_{suffix}"] = f"{label} ({mat_name})"
    EN_LANG.write_text(json.dumps(dict(sorted(en.items())), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    texture_sets = sorted(p.name for p in BOWL_DIR.iterdir() if p.is_dir())
    for ts in texture_sets:
        write_shared_models(ts)
    suffixes = crucible_suffixes()
    write_blockstates(suffixes)
    write_item_models(suffixes)
    update_lang(suffixes)
    print(
        f"Generated assets for {len(suffixes)} material tiers x 4 block types, "
        f"{len(texture_sets)} texture sets"
    )


if __name__ == "__main__":
    main()
