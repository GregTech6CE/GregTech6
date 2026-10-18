"""GT6 MultiTileEntityBookShelf frame (passes 1-6) and original book textures."""

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
BOOKS = ("book_vanilla", "book_enchanted", "book_matdict", "book_dusty", "folder", "folder_red", "frame")


def generate(assets=ASSETS):
    def write(relative, data):
        path = assets / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")

    # MultiTileEntityBookShelf.setBlockBounds2/getTexture2, passes 0-6, facing north.
    # Pass 0 is the *outer* full-block shell on the two sides and top/bottom. The
    # six inset pieces draw only their exposed faces. Giving every piece six faces
    # makes coplanar faces fight with the shell and closes surfaces GT6 leaves open.
    boxes = [
        ([0, 0, 0], [16, 16, 16], ("up", "down", "west", "east")),
        ([0, 0, 0], [16, 1, 16], ("up", "north", "south")),
        ([0, 15, 0], [16, 16, 16], ("down", "north", "south")),
        ([0, 1, 0], [1, 15, 16], ("up", "down", "north", "south", "east")),
        ([15, 1, 0], [16, 15, 16], ("up", "down", "north", "south", "west")),
        ([1, 1, 7], [15, 15, 9], ("north", "south")),
        ([1, 7, 1], [15, 9, 15], ("up", "down", "north", "south")),
    ]
    elements = []
    for start, end, directions in boxes:
        faces = {direction: {"texture": "#wood"} for direction in directions}
        elements.append({"from": start, "to": end, "faces": faces})
    write("models/block/bookshelf.json", {
        "parent": "minecraft:block/block", "textures": {
            # GT6 PlankData.PLANK_ICONS[0] starts as Blocks.planks:0 (oak).
            "wood": "minecraft:block/oak_planks", "particle": "#wood"},
        "elements": elements})
    write("models/item/bookshelf.json", {"parent": "gt6ce:block/bookshelf"})
    write("blockstates/bookshelf.json", {"variants": {
        "facing=" + facing: {"model": "gt6ce:block/bookshelf", "y": angle}
        for facing, angle in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    target = assets / "textures/block/books"
    target.mkdir(parents=True, exist_ok=True)
    for book in BOOKS:
        for face in ("back", "side"):
            name = book + "_" + face
            shutil.copyfile(ORIGINAL / "textures/blocks/books" / (name.upper() + ".png"), target / (name + ".png"))


if __name__ == "__main__":
    generate()
