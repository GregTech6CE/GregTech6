"""Baked variants for GT6's four structural scaffold designs (north-facing mesh)."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import json
from pathlib import Path


ROOT = native_path(Path(__file__).resolve().parents[1] / "src/main/resources/assets/gregtech")


def cube(bounds, texture="plate"):
    return {
        "from": list(bounds[:3]),
        "to": list(bounds[3:]),
        "faces": {side: {"texture": "#" + texture, "tintindex": 0}
                  for side in ("down", "up", "north", "south", "west", "east")},
    }


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def main():
    base = ROOT / "models/block/tool"
    posts_lower = [(0, 0, 0, 2, 10, 2), (0, 0, 14, 2, 10, 16),
                   (14, 0, 0, 16, 10, 2), (14, 0, 14, 16, 10, 16)]
    posts_full = [(x, 0, z, x + 2, 16, z + 2) for x in (0, 14) for z in (0, 14)]
    rails = [(2, 4, 0, 14, 5, 4), (2, 12, 0, 14, 13, 4)]
    variants = {
        0: [(0, 14, 0, 16, 16, 16), (2, 12, 0, 4, 14, 16),
            (6, 10, 0, 10, 14, 16), (12, 12, 0, 14, 14, 16)],
        1: [(0, 10, 0, 16, 16, 16), *posts_lower, *rails],
        2: [*posts_full, *rails],
        3: [(0, 0, 0, 16, 16, 16)],
    }
    # GT6 Scaffold:117-128: posts use the material's solid-block texture, while
    # platforms and rails use PLATE. Design 1 adds HATCH on their vertical faces.
    for design, bounds in variants.items():
        rods = {0: {1, 2, 3}, 1: {1, 2, 3, 4}, 2: {0, 1, 2, 3}, 3: set()}[design]
        textures = {"particle": "gregtech:block/iconsets/plate",
                    "plate": "gregtech:block/iconsets/plate",
                    "rod": "gregtech:block/material_icons/metallic/blocksolid"}
        base_model = {"textures": textures, "elements": [
            cube(box, "rod" if i in rods else "plate") for i, box in enumerate(bounds)]}
        model = {"parent": "minecraft:block/block", **base_model}
        if design == 1:
            overlays = [cube(bounds[i], "hatch") for i in (0, 5, 6)]
            for element in overlays:
                element["faces"] = {side: face for side, face in element["faces"].items()
                                    if side in ("up", "down")}
            model = {"parent": "minecraft:block/block", "loader": "forge:composite",
                     "textures": {"particle": textures["particle"]},
                     "children": {"base": base_model, "hatch": {
                         "textures": {"hatch": "gregtech:block/iconsets/hatch"},
                         "elements": overlays, "render_type": "minecraft:cutout"}}}
        write(base / f"scaffold{'_' + str(design) if design else ''}.json", model)

    blockstates = {}
    for design in range(4):
        for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            blockstates[f"design={design},facing={facing}"] = {
                "model": f"gregtech:block/tool/scaffold{f'_{design}' if design else ''}",
                "y": rotation,
            }
    write(ROOT / "blockstates/scaffold.json", {"variants": blockstates})
    write(ROOT / "models/item/scaffold.json", {"parent": "gregtech:block/tool/scaffold_1"})


if __name__ == "__main__":
    main()
