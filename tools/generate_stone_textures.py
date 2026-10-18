#!/usr/bin/env python3
"""Generate texture sets for GT stones that have no dedicated art.

GT6's worldgen config references more stone layers (chalk, dolomite, gabbro, ...)
than it ships textures for — in 1.7.10 those came from cross-mod texture packs or
material-tinted rendering. Here we synthesize each set offline: the limestone set
(the most neutral of the shipped sets) is converted to greyscale and multiplied by
the stone material's color from GT6Materials.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import os

from PIL import Image, ImageOps

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
STONES = os.path.join(ROOT, str(native_path("src")), "main", "resources", "assets", "gt6ce", "textures", "block", "stones")
BASE = os.path.join(STONES, "gt.stone.limestone")

# stone id -> (texture folder, material color from GT6Materials)
NEW_STONES = {
    "chalk":    ("gt.stone.chalk",    0xFAFAFA),
    "dolomite": ("gt.stone.dolomite", 0xE1CDCD),
    "gabbro":   ("gt.stone.gabbro",   0x413C3C),
    "gneiss":   ("gt.stone.gneiss",   0xFFC986),
    "gypsum":   ("gt.stone.gypsum",   0xF0F0F0),
    "oilshale": ("gt.stone.oilshale", 0x32323C),
    "rhyolite": ("gt.stone.rhyolite", 0x797979),
    "salt":     ("gt.stone.salt",     0xFAFAFA),
    "sylvite":  ("gt.stone.sylvite",  0xF0C8C8),
    "talc":     ("gt.stone.talc",     0x5F915F),
}


def tint(img: Image.Image, color: int) -> Image.Image:
    rgba = img.convert("RGBA")
    grey = ImageOps.grayscale(rgba)
    r, g, b = (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF
    # Normalize the base around its mean brightness so light colors stay light.
    out = Image.new("RGBA", rgba.size)
    grey_px = grey.load()
    src_px = rgba.load()
    out_px = out.load()
    for y in range(rgba.size[1]):
        for x in range(rgba.size[0]):
            v = grey_px[x, y] / 160.0  # limestone mean is bright; 160 keeps tones natural
            out_px[x, y] = (
                min(255, int(r * v)),
                min(255, int(g * v)),
                min(255, int(b * v)),
                src_px[x, y][3],
            )
    return out


def main() -> None:
    files = [f for f in os.listdir(BASE) if f.endswith(".png")]
    for stone, (folder, color) in NEW_STONES.items():
        out_dir = os.path.join(STONES, folder)
        os.makedirs(out_dir, exist_ok=True)
        for fname in files:
            img = Image.open(os.path.join(BASE, fname))
            tint(img, color).save(os.path.join(out_dir, fname))
        print(f"{stone}: {len(files)} textures -> {folder}")


if __name__ == "__main__":
    main()
