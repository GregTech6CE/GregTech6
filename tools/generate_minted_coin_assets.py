"""GT6 default minted item geometry. Pattern: GregTech-6 Team, LGPL-3.0-or-later."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS = ROOT / "src/main/resources/assets/gregtech"


def generate(assets=ASSETS):
    shape = json.loads((ASSETS / "geometry/coin_shape.json").read_text())
    def depth(x, z):
        return ((shape[0][x] >> z) & 1) + 2 * ((shape[1][x] >> z) & 1)

    elements = []
    for x in range(16):
        for z in range(16):
            pixel_depth = depth(x, z)
            if pixel_depth == 3:
                continue
            faces = {d: {"texture": "#top", "tintindex": 0} for d in ("up", "down")}
            # GT6's getTexture suppresses an internal side when its neighbour is the same height or
            # taller. Omit those faces from the baked item too: the relief stays exact, while the
            # inventory model has hundreds fewer quads to bake and draw.
            for side, nx, nz in (("north", x, z - 1), ("south", x, z + 1),
                                 ("west", x - 1, z), ("east", x + 1, z)):
                if nx < 0 or nx >= 16 or nz < 0 or nz >= 16 or pixel_depth < depth(nx, nz):
                    faces[side] = {"texture": "#side", "tintindex": 0}
            elements.append({"from": [x, 7, z], "to": [x + 1, 9 - pixel_depth / 4, z + 1], "faces": faces})
    model = {"parent": "minecraft:block/block", "textures": {
        "top": "gregtech:block/iconsets/coin", "side": "gregtech:block/iconsets/coin_side", "particle": "#top"},
        "elements": elements}
    root = assets / "models/item/coin_minted.json"
    root.parent.mkdir(parents=True, exist_ok=True)
    root.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
    for path in (SHARED_ASSETS / "models/item/material").glob("*/coin.json"):
        target = assets / path.relative_to(ASSETS)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text('{"parent": "gregtech:item/coin_minted"}\n', encoding="utf-8")


if __name__ == "__main__":
    generate()
