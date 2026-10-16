#!/usr/bin/env python3
"""Generate per-material pipe models using forge:composite with textures baked in.

Textures are baked into each model file from the hardcoded material→texture mapping,
so blockstate multipart variants don't need texture overrides.
"""

from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS = ROOT / "src/main/resources/assets/gregtech"
BLOCKSTATES = SHARED_ASSETS / "blockstates"
MODELS_MACHINE = SHARED_ASSETS / "models/block/machine"
MODELS_BLOCKS = SHARED_ASSETS / "models/block/blocks"
MODELS_ITEM = SHARED_ASSETS / "models/item"

# ── Material → texture set folder mapping (extracted from original blockstates) ──
TEX_FOLDER: dict[str, str] = {
    "wood": "wood",
    "treated_wood": "wood",
    "plastic": "dull",
    "rubber": "rubber",
    "copper": "copper",
    "aluminium": "copper",
    "tin_alloy": "metallic",
    "bronze": "metallic",
    "invar": "metallic",
    "steel": "metallic",
    "galvanized_steel": "metallic",
    "hsla": "metallic",
    "gold": "shiny",
    "chrome": "shiny",
    "stainless_steel": "metallic",
    "vanadium_steel": "metallic",
    "desh": "dull",
    "tungsten_alloy": "metallic",
    "tungsten_steel": "metallic",
    "tungsten_carbide": "metallic",
    "desh_alloy": "metallic",
    "palladium": "shiny",
    "carbon": "fine",
    "tantalum_hafnium_carbide": "metallic",
    "titanium": "metallic",
    "tungsten": "metallic",
    "efrine": "metallic",
    "netherite": "metallic",
    "iridium": "dull",
    "ironwood": "wood",
    "thaumium": "metallic",
    "manasteel": "metallic",
    "void_metal": "metallic",
    "terrasteel": "metallic",
    "gaia_spirit": "metallic",
    "bedrock_hsla": "brick",
    "adamantium": "shiny",
    "draconium": "metallic",
    "awakened_draconium": "metallic",
    "infinity": "metallic",
}

# ── Geometry ──────────────────────────────────────────────────────────────

CORE_GEO = {
    "tiny":  [6, 6, 6,  10, 10, 10],
    "small": [5, 5, 5,  11, 11, 11],
    "medium":[4, 4, 4,  12, 12, 12],
    "large": [2, 2, 2,  14, 14, 14],
    "huge":  [0, 0, 0,  16, 16, 16],
    "quadruple": [0, 0, 0, 16, 16, 16],
    "nonuple":   [0, 0, 0, 16, 16, 16],
}

SIDE_GEO = {
    "tiny":  [6, 6, 0,  10, 10, 6],
    "small": [5, 5, 0,  11, 11, 5],
    "medium":[4, 4, 0,  12, 12, 4],
    "large": [2, 2, 0,  14, 14, 2],
    "huge":  [0, 0, 0,  16, 16, 0],
    "quadruple": [0, 0, 0, 16, 16, 0],
    "nonuple":   [0, 0, 0, 16, 16, 0],
}

SIDE_END = {
    "tiny": "pipetiny", "small": "pipesmall", "medium": "pipemedium",
    "large": "pipelarge", "huge": "pipehuge",
    "quadruple": "pipequadruple", "nonuple": "pipenonuple",
}

CORE_FACE = {
    "tiny": "pipeside", "small": "pipeside", "medium": "pipeside",
    "large": "pipeside", "huge": "pipehuge",
    "quadruple": "pipequadruple", "nonuple": "pipenonuple",
}


def _g(g, i): return [g[i], g[i+1], g[i+2]]


# ── Model builders (forge:composite with baked textures) ──────────────────

def build_core(tex: str, size: str) -> dict:
    face = CORE_FACE[size]
    geo = CORE_GEO[size]
    t = f"gregtech:block/material_icons/{tex}"
    return {
        "loader": "forge:composite",
        "children": {
            "layer0": {
                "parent": "minecraft:block/block",
                "textures": {
                    "down": f"{t}/{face}", "up": f"{t}/{face}",
                    "north": f"{t}/{face}", "south": f"{t}/{face}",
                    "west": f"{t}/{face}", "east": f"{t}/{face}",
                },
                "elements": [{
                    "from": _g(geo, 0), "to": _g(geo, 3),
                    "faces": {
                        "down": {"texture": "#down", "tintindex": 0},
                        "up": {"texture": "#up", "tintindex": 0},
                        "north": {"texture": "#north", "tintindex": 0},
                        "south": {"texture": "#south", "tintindex": 0},
                        "west": {"texture": "#west", "tintindex": 0},
                        "east": {"texture": "#east", "tintindex": 0},
                    },
                }],
                "render_type": "minecraft:solid",
            },
            "layer1": {
                "parent": "minecraft:block/block",
                "textures": {
                    "down": f"{t}/{face}_overlay", "up": f"{t}/{face}_overlay",
                    "north": f"{t}/{face}_overlay", "south": f"{t}/{face}_overlay",
                    "west": f"{t}/{face}_overlay", "east": f"{t}/{face}_overlay",
                },
                "elements": [{
                    "from": _g(geo, 0), "to": _g(geo, 3),
                    "faces": {
                        "down": {"texture": "#down"}, "up": {"texture": "#up"},
                        "north": {"texture": "#north"}, "south": {"texture": "#south"},
                        "west": {"texture": "#west"}, "east": {"texture": "#east"},
                    },
                }],
                "render_type": "minecraft:cutout",
            },
        },
    }


def build_side(tex: str, size: str) -> dict:
    end = SIDE_END[size]
    side = "pipeside"
    geo = SIDE_GEO[size]
    t = f"gregtech:block/material_icons/{tex}"
    return {
        "loader": "forge:composite",
        "children": {
            "layer0": {
                "parent": "minecraft:block/block",
                "textures": {
                    "side": f"{t}/{side}", "end": f"{t}/{end}",
                    "up": f"{t}/{side}", "down": f"{t}/{side}",
                },
                "elements": [{
                    "from": _g(geo, 0), "to": _g(geo, 3),
                    "faces": {
                        "down": {"texture": "#down", "tintindex": 0},
                        "up": {"texture": "#up", "tintindex": 0},
                        "north": {"texture": "#end", "cullface": "north", "tintindex": 0},
                        "south": {"texture": "#side", "tintindex": 0},
                        "west": {"texture": "#side", "tintindex": 0},
                        "east": {"texture": "#side", "tintindex": 0},
                    },
                }],
                "render_type": "minecraft:solid",
            },
            "layer1": {
                "parent": "minecraft:block/block",
                "textures": {
                    "side_ov": f"{t}/{side}_overlay", "end_ov": f"{t}/{end}_overlay",
                    "up_ov": f"{t}/{side}_overlay", "down_ov": f"{t}/{side}_overlay",
                },
                "elements": [{
                    "from": _g(geo, 0), "to": _g(geo, 3),
                    "faces": {
                        "down": {"texture": "#down_ov"}, "up": {"texture": "#up_ov"},
                        "north": {"texture": "#end_ov", "cullface": "north"},
                        "south": {"texture": "#side_ov"},
                        "west": {"texture": "#side_ov"}, "east": {"texture": "#side_ov"},
                    },
                }],
                "render_type": "minecraft:cutout",
            },
        },
    }


def build_item_block(tex: str, size: str) -> dict:
    """Simple north-south bar model (Z=0→16), matching shared pipe_item_{size}.json."""
    end = SIDE_END[size]
    side = "pipeside"
    t = f"gregtech:block/material_icons/{tex}"
    x1 = CORE_GEO[size][0]
    y1 = CORE_GEO[size][1]
    x2 = CORE_GEO[size][3]
    y2 = CORE_GEO[size][4]
    return {
        "parent": "minecraft:block/block",
        "textures": {
            "side": f"{t}/{side}",
            "end": f"{t}/{end}",
        },
        "elements": [{
            "from": [x1, y1, 0], "to": [x2, y2, 16],
            "faces": {
                "down":  {"texture": "#side", "tintindex": 0},
                "up":    {"texture": "#side", "tintindex": 0},
                "east":  {"texture": "#side", "tintindex": 0},
                "west":  {"texture": "#side", "tintindex": 0},
                "north": {"texture": "#end",  "tintindex": 0},
                "south": {"texture": "#end",  "tintindex": 0},
            },
        }],
    }


# ── Parsing ───────────────────────────────────────────────────────────────

def parse_pipe(name: str) -> tuple[str, str] | None:
    m = re.match(r"(?:item_)?pipe_(tiny|small|medium|large|huge|quadruple|nonuple)_(.+)\.json", name)
    if not m:
        return None
    return (m.group(1), m.group(2))


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


# ── Main ──────────────────────────────────────────────────────────────────

def main() -> None:
    gen = 0
    bs_count = 0
    item_count = 0

    # ── Fluid pipes ────────────────────────────────────────────────────
    for bs_file in sorted(BLOCKSTATES.glob("pipe_*_*.json")):
        if bs_file.name.startswith("item_pipe_"):
            continue
        parsed = parse_pipe(bs_file.name)
        if not parsed:
            continue
        size, mat = parsed

        tex = TEX_FOLDER.get(mat, "metallic")

        for suffix, builder in [("core", build_core), ("side", build_side)]:
            write_json(MODELS_MACHINE / f"pipe_{size}_{mat}_{suffix}.json", builder(tex, size))
            gen += 1

        core_ref = f"gregtech:block/machine/pipe_{size}_{mat}_core"
        side_ref = f"gregtech:block/machine/pipe_{size}_{mat}_side"
        write_json(bs_file, {
            "multipart": [
                {"apply": {"model": core_ref}},
                {"when": {"north": "true"}, "apply": {"model": side_ref, "y": 0}},
                {"when": {"south": "true"}, "apply": {"model": side_ref, "y": 180}},
                {"when": {"east": "true"},  "apply": {"model": side_ref, "y": 90}},
                {"when": {"west": "true"},  "apply": {"model": side_ref, "y": 270}},
                {"when": {"up": "true"},    "apply": {"model": side_ref, "y": 0, "x": 270}},
                {"when": {"down": "true"},  "apply": {"model": side_ref, "y": 0, "x": 90}},
            ]
        })
        bs_count += 1

        write_json(MODELS_BLOCKS / f"pipe_item_{size}_{mat}.json", build_item_block(tex, size))
        gen += 1

        item_model = MODELS_ITEM / f"pipe_{size}_{mat}.json"
        if item_model.exists():
            write_json(item_model, {"parent": f"gregtech:block/blocks/pipe_item_{size}_{mat}"})
            item_count += 1

    # ── Item pipes (transport items) ───────────────────────────────────
    for bs_file in sorted(BLOCKSTATES.glob("item_pipe_*_*.json")):
        parsed = parse_pipe(bs_file.name)
        if not parsed:
            continue
        size, mat = parsed

        tex = TEX_FOLDER.get(mat, "metallic")

        for suffix, builder in [("core", build_core), ("side", build_side)]:
            path = MODELS_MACHINE / f"pipe_{size}_{mat}_{suffix}.json"
            if not path.exists():
                write_json(path, builder(tex, size))
                gen += 1

        core_ref = f"gregtech:block/machine/pipe_{size}_{mat}_core"
        side_ref = f"gregtech:block/machine/pipe_{size}_{mat}_side"
        write_json(bs_file, {
            "multipart": [
                {"apply": {"model": core_ref}},
                {"when": {"north": "true"}, "apply": {"model": side_ref, "y": 0}},
                {"when": {"south": "true"}, "apply": {"model": side_ref, "y": 180}},
                {"when": {"east": "true"},  "apply": {"model": side_ref, "y": 90}},
                {"when": {"west": "true"},  "apply": {"model": side_ref, "y": 270}},
                {"when": {"up": "true"},    "apply": {"model": side_ref, "y": 0, "x": 270}},
                {"when": {"down": "true"},  "apply": {"model": side_ref, "y": 0, "x": 90}},
            ]
        })
        bs_count += 1

        item_block = MODELS_BLOCKS / f"pipe_item_{size}_{mat}.json"
        if not item_block.exists():
            write_json(item_block, build_item_block(tex, size))
            gen += 1

        item_model = MODELS_ITEM / f"item_pipe_{size}_{mat}.json"
        if item_model.exists():
            write_json(item_model, {"parent": f"gregtech:block/blocks/pipe_item_{size}_{mat}"})
            item_count += 1

    print(f"Generated {gen} model files")
    print(f"Updated {bs_count} blockstate files")
    print(f"Updated {item_count} item model references")


if __name__ == "__main__":
    main()
