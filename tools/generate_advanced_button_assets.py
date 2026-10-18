"""The default GT6 advanced-button design, with off/on layered textures."""

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
ORIGINAL = ROOT.parent / "gregtech6-master/gregtech6-master/src/main/resources/assets/gregtech/textures/blocks/machines/redstone/buttons/advanced/0/0"
TARGET = ASSETS / "textures/block/machines/redstone/buttons/advanced/0/0"


def write(path, data):
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def model(suffix):
    children = {}
    for layer in ("colored", "overlay", "glowing", "glowverlay"):
        texture = f"gt6ce:block/machines/redstone/buttons/advanced/0/0/{layer}_{suffix}"
        faces = {side: {"texture": "#face"} for side in
                 ("north", "south", "east", "west", "up", "down")}
        if layer in ("colored", "glowing"):
            for face in faces.values():
                face["tintindex"] = 0
        children[layer] = {
            "textures": {"face": texture},
            "elements": [{"from": [4, 4, 0], "to": [12, 12, 2], "faces": faces}],
            "render_type": "minecraft:cutout",
        }
    return {
        "parent": "minecraft:block/block",
        "loader": "forge:composite",
        "textures": {"particle": "gt6ce:block/machines/redstone/buttons/advanced/0/0/colored_off"},
        "children": children,
    }


def main():
    TARGET.mkdir(parents=True, exist_ok=True)
    for suffix in ("off", "on"):
        for layer in ("colored", "overlay", "glowing", "glowverlay"):
            shutil.copy2(ORIGINAL / f"{layer}_{suffix}.png", TARGET / f"{layer}_{suffix}.png")
        write(ASSETS / f"models/block/tool/advanced_button_{suffix}.json", model(suffix))

    variants = {}
    rotations = {
        "south": (0, 0), "north": (0, 180), "east": (0, 270), "west": (0, 90),
        "up": (90, 0), "down": (270, 0),
    }
    for facing, (x, y) in rotations.items():
        for active in ("false", "true"):
            for lit in ("false", "true"):
                variants[f"active={active},facing={facing},lit={lit}"] = {
                    "model": f"gt6ce:block/tool/advanced_button_{'on' if lit == 'true' else 'off'}",
                    "x": x, "y": y,
                }
    write(ASSETS / "blockstates/advanced_button.json", {"variants": variants})
    write(ASSETS / "models/item/advanced_button.json",
          {"parent": "gt6ce:block/tool/advanced_button_off"})


if __name__ == "__main__":
    main()
