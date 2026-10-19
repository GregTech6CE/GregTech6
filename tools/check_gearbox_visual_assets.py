"""Check that the GT6 custom gearbox sprites are stitched by real block models."""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSETS = native_path(ROOT / "src/main/resources/assets/gt6ce")
SOURCE = ROOT.parent / "gregtech6-master/gregtech6-master/src/main/resources/assets/gregtech/textures/blocks/iconsets"
NAMES = (
    "gearbox",
    "gearbox_axle",
    "gear",
    "gear_clockwise",
    "gear_counterclockwise",
)


def check() -> None:
    registered = (native_path(ROOT / "src/main/java/com/gregtech/gregtech/registry/GTIconSetBlocks.java")).read_text(encoding="utf-8")
    loader = (native_path(ROOT / "src/main/java/com/gregtech/gregtech/bootstrap/content/BlockBootstrap.java")).read_text(encoding="utf-8")
    model = (native_path(ROOT / "src/main/java/com/gregtech/gregtech/client/GearboxBakedModel.java")).read_text(encoding="utf-8")
    assert "GTIconSetBlocks.registerAll()" in loader, "icon set blocks are not registered"
    for name in NAMES:
        assert f'"{name}"' in registered, f"{name} is not a registered icon set block"
        assert f'"block/iconsets/{name}"' in model, f"dynamic gearbox model does not use {name}"
        state = json.loads((ASSETS / f"blockstates/{name}.json").read_text(encoding="utf-8"))
        assert state["variants"][""]["model"] == f"gt6ce:block/iconsets/{name}"
        block_model = json.loads((ASSETS / f"models/block/iconsets/{name}.json").read_text(encoding="utf-8"))
        assert block_model["textures"]["all"] == f"gt6ce:block/iconsets/{name}"
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
