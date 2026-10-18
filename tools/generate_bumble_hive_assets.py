"""Bake the sixteen bumblebee hive colour variants and their models.

GT6's hive is a `TileEntityBase07Paintable`: it renders two layers per face
(`MultiTileEntityBumbleHive:78`)

    BlockTextureMulti.get(BlockTextureDefault.get(sColoreds[face], mRGBa),
                          BlockTextureDefault.get(sOverlays[face]))

so the `colored/*` icon is multiplied by the hive's NBT colour and the `overlay/*` icon is drawn on
top of it. The port has no on-the-fly tinting (the colour is a block state), so this tool bakes the
combination: for every one of the sixteen dye colours and every face it writes

    assets/gt6ce/textures/block/nature/bumblehive/<colour>/<face>.png

plus the blockstate, the sixteen block models, the item model and the two lang entries.
GT6's colour ints are CS.java:403-418 (`DYE_*` -> `DYE_INT_*`), and its own overlays in the resource
pack are fully transparent, so compositing them is a no-op today - it is kept so a real overlay would
just work.

Usage: python tools/generate_bumble_hive_assets.py [--check]
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

import argparse
import json
import os
import sys

from PIL import Image, ImageChops

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, str(native_path("src")), "main", "resources", "assets", "gt6ce")
SOURCE = os.path.join(ASSETS, "textures", "block", "nature", "bumblehive")
FACES = ("bottom", "top", "side")

# CS.java:403-418: DYE_<name> as packed RGB (`UT.Code.getRGBInt(DYE_*)`). These are the values GT6
# multiplies the `colored` icon with, and the ones `NBT_COLOR` carries.
DYE_COLORS = {
    "white": 0xFFFFFF,
    "orange": 0xFF8000,
    "magenta": 0xFF00FF,
    "light_blue": 0x8080FF,
    "yellow": 0xFFFF00,
    "lime": 0x80FF80,
    "pink": 0xFFC0C0,
    "gray": 0x808080,
    "light_gray": 0xC0C0C0,
    "cyan": 0x00FFFF,
    "purple": 0x800080,
    "blue": 0x0000FF,
    "brown": 0x604000,
    "green": 0x00FF00,
    "red": 0xFF0000,
    "black": 0x202020,
}

# The block state's default, i.e. `BumbleHiveBlock`'s registered default colour.
DEFAULT_COLOR = "light_gray"


def tint(source: Image.Image, rgb: int) -> Image.Image:
    """GT6's `BlockTextureDefault.get(icon, mRGBa)`: multiply every channel by the colour."""
    layer = Image.new("RGBA", source.size, ((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255))
    return ImageChops.multiply(source.convert("RGBA"), layer)


def composite(colored: Image.Image, overlay: Image.Image) -> Image.Image:
    """GT6 draws the overlay on top of the tinted base layer."""
    base = colored.convert("RGBA")
    base.alpha_composite(overlay.convert("RGBA"))
    return base


def load_sources():
    out = {}
    for face in FACES:
        colored = Image.open(os.path.join(SOURCE, "colored", face + ".png")).convert("RGBA")
        overlay_path = os.path.join(SOURCE, "overlay", face + ".png")
        overlay = Image.open(overlay_path).convert("RGBA") if os.path.exists(overlay_path) else None
        if overlay is None:
            overlay = Image.new("RGBA", colored.size, (255, 255, 255, 0))
        out[face] = (colored, overlay)
    return out


def write_if_changed(path: str, data, check: bool, written: list) -> None:
    payload = data if isinstance(data, bytes) else json.dumps(data, indent=2).encode("utf-8") + b"\n"
    if os.path.exists(path) and open(path, "rb").read() == payload:
        return
    written.append(os.path.relpath(path, ROOT).replace("\\", "/"))
    if not check:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as handle:
            handle.write(payload)


def blockstate() -> dict:
    variants = {}
    for color in DYE_COLORS:
        variants["color=" + color] = {"model": "gt6ce:block/bumble_hive_" + color}
    return {"variants": variants}


def block_model(color: str) -> dict:
    base = "gt6ce:block/nature/bumblehive/" + color + "/"
    return {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {
            "bottom": base + "bottom",
            "top": base + "top",
            "side": base + "side",
        },
    }


def item_model() -> dict:
    return {"parent": "gt6ce:block/bumble_hive_" + DEFAULT_COLOR}


def lang_entry(path: str, value: str, check: bool, written: list) -> None:
    """Insert `block.gt6ce.bumble_hive` in sorted position (the port's lang files are sorted)."""
    with open(path, "r", encoding="utf-8") as handle:
        lines = handle.read().split("\n")
    key = "block.gt6ce.bumble_hive"
    if any(('"' + key + '"') in line for line in lines):
        return
    entry = '  "%s": %s,' % (key, json.dumps(value, ensure_ascii=False))
    anchor = None
    for index, line in enumerate(lines):
        if line.startswith('  "block.gt6ce.'):
            current = line.split('"')[1]
            if current > key and anchor is None:
                anchor = index
                break
    if anchor is None:
        raise SystemExit("no block.gt6ce.* anchor found in " + path)
    lines.insert(anchor, entry)
    written.append(os.path.relpath(path, ROOT).replace("\\", "/"))
    if not check:
        with open(path, "w", encoding="utf-8") as handle:
            handle.write("\n".join(lines))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="report differences without writing")
    args = parser.parse_args()

    sources = load_sources()
    written = []
    for color, rgb in DYE_COLORS.items():
        for face in FACES:
            colored, overlay = sources[face]
            image = composite(tint(colored, rgb), overlay)
            target = os.path.join(SOURCE, color, face + ".png")
            if os.path.exists(target):
                with open(target, "rb") as handle:
                    existing = handle.read()
            else:
                existing = None
            buffer = os.path.join(ROOT, "build", "_hive_tmp.png")
            os.makedirs(os.path.dirname(buffer), exist_ok=True)
            image.save(buffer, "PNG")
            with open(buffer, "rb") as handle:
                payload = handle.read()
            os.remove(buffer)
            if existing != payload:
                written.append(os.path.relpath(target, ROOT).replace("\\", "/"))
                if not args.check:
                    os.makedirs(os.path.dirname(target), exist_ok=True)
                    with open(target, "wb") as handle:
                        handle.write(payload)

    write_if_changed(os.path.join(ASSETS, "blockstates", "bumble_hive.json"), blockstate(), args.check, written)
    for color in DYE_COLORS:
        write_if_changed(os.path.join(ASSETS, "models", "block", "bumble_hive_" + color + ".json"),
                         block_model(color), args.check, written)
    write_if_changed(os.path.join(ASSETS, "models", "item", "bumble_hive.json"), item_model(), args.check, written)
    lang_entry(os.path.join(ASSETS, "lang", "en_us.json"), "Bumblebee Hive", args.check, written)
    lang_entry(os.path.join(ASSETS, "lang", "zh_cn.json"), "\u871c\u8702\u5de2", args.check, written)

    print(("would write " if args.check else "wrote ") + str(len(written)) + " files")
    for path in written:
        print("  " + path)
    return 0


if __name__ == "__main__":
    sys.exit(main())
