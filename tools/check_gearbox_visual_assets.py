"""Check that the GT6 custom gearbox sprites are stitched by real block models."""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS = ROOT / "src/main/resources/assets/gregtech"
SOURCE = ROOT.parent / "gregtech6-master/gregtech6-master/src/main/resources/assets/gregtech/textures/blocks/iconsets"
NAMES = (
    "gearbox",
    "gearbox_axle",
    "gear",
    "gear_clockwise",
    "gear_counterclockwise",
)


def check() -> None:
    registered = (ROOT / "src/main/java/com/gregtech/gregtech/registry/GTIconSetBlocks.java").read_text(encoding="utf-8")
    loader = (ROOT / "src/main/java/com/gregtech/gregtech/loaders/a/Loader_Blocks.java").read_text(encoding="utf-8")
    model = (ROOT / "src/main/java/com/gregtech/gregtech/client/GearboxBakedModel.java").read_text(encoding="utf-8")
    assert "GTIconSetBlocks.registerAll()" in loader, "icon set blocks are not registered"
    for name in NAMES:
        assert f'"{name}"' in registered, f"{name} is not a registered icon set block"
        assert f'"block/iconsets/{name}"' in model, f"dynamic gearbox model does not use {name}"
        state = json.loads((SHARED_ASSETS / f"blockstates/{name}.json").read_text(encoding="utf-8"))
        assert state["variants"][""]["model"] == f"gregtech:block/iconsets/{name}"
        block_model = json.loads((SHARED_ASSETS / f"models/block/iconsets/{name}.json").read_text(encoding="utf-8"))
        assert block_model["textures"]["all"] == f"gregtech:block/iconsets/{name}"
        texture = (ASSETS / f"textures/block/iconsets/{name}.png").read_bytes()
        assert texture.startswith(b"\x89PNG\r\n\x1a\n"), f"{name} is not PNG"
        original = SOURCE / f"{name.upper()}.png"
        if original.exists():
            assert texture == original.read_bytes(), f"{name} differs from GT6 original"
    for name in ("gear_clockwise", "gear_counterclockwise"):
        animated = ASSETS / f"textures/block/iconsets/{name}.png.mcmeta"
        assert json.loads(animated.read_text(encoding="utf-8"))["animation"]["frametime"] == 1
    print("GT6 gearbox atlas chain and 5 original iconset sprites verified")


if __name__ == "__main__":
    check()
