"""GT6 BottleCrate passes 0–8; dungeon variant 8762 is treated planks (wood index 62).

Geometry/source assets: GregTech 6, LGPL-3.0-or-later, GregTech-6 Team.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = native_path(ROOT / "src/main/resources/assets/gt6ce")
ORIGINAL = ROOT.parent / "gregtech6-master/gregtech6-master/src/main/resources/assets/gregtech"


def generate(assets=ASSETS):
    # North/south frame. East/west rotate it by 90 degrees; the bottle grid stays in world X/Z.
    bounds = [([1, 0, 1], [15, 1, 15]),
              ([1, 1, 5], [15, 5, 6]), ([1, 1, 10], [15, 5, 11]),
              ([5, 1, 1], [6, 4, 15]), ([10, 1, 1], [11, 4, 15]),
              ([0, 0, 0], [1, 8, 16]), ([15, 0, 0], [16, 8, 16]),
              ([1, 5, 0], [15, 7, 1]), ([1, 5, 15], [15, 7, 16])]
    def write(name, data):
        path = assets / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    faces = {side: {"texture": "#wood"} for side in ("up", "down", "north", "south", "east", "west")}
    write("models/block/inventory/bottle_crate.json", {
        "parent": "minecraft:block/block", "ambientocclusion": False,
        "textures": {"wood": "gt6ce:block/iconsets/planks_treated", "particle": "#wood"},
        "elements": [{"from": a, "to": b, "faces": faces} for a, b in bounds]})
    write("models/item/bottle_crate.json", {"parent": "gt6ce:block/inventory/bottle_crate"})
    write("blockstates/bottle_crate.json", {"variants": {
        "facing="+face: {"model": "gt6ce:block/inventory/bottle_crate", "y": angle}
        for face, angle in (("north", 0), ("south", 0), ("east", 90), ("west", 90))}})
    target = assets / "textures/block/iconsets"
    target.mkdir(parents=True, exist_ok=True)
    for name in ("BOTTLECRATE_BOTTLE_TOP", "BOTTLECRATE_BOTTLE_SIDES", "BOTTLECRATE_BOTTLE_CAP", "PLANKS_TREATED"):
        shutil.copyfile(ORIGINAL / f"textures/blocks/iconsets/{name}.png", target / f"{name.lower()}.png")


if __name__ == "__main__":
    generate()
