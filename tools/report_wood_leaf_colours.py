"""Print the mean colour of the GT6 leaf textures so the wood-species tint colours can be
checked against the convention the existing entries follow."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

import os
from PIL import Image

ROOT = os.path.join(str(native_path("src")), "main", "resources", "assets", "gregtech", "textures", "block", "iconsets")
SPECIES = {
    "rubber": 0xC4A46B, "maple": 0xE0A860, "willow": 0x8DBD60, "blue_mahoe": 0x9EB9D4,
    "rainbowood": 0x88CC88, "pine": 0xBC9862, "ebony": 0x3C2A1E, "white_mahoe": 0xE8DCC8,
    "hazel": None, "cinnamon": None, "coconut": None, "bluespruce": None,
}


def mean_colour(path):
    img = Image.open(path).convert("RGBA")
    px = list(img.getdata())
    opaque = [p for p in px if p[3] > 8]
    if not opaque:
        return None
    n = len(opaque)
    return tuple(sum(p[i] for p in opaque) // n for i in range(3))


for name, expected in SPECIES.items():
    path = os.path.join(ROOT, "leaves_%s.png" % name)
    if not os.path.exists(path):
        print("%-12s (no texture)" % name)
        continue
    got = mean_colour(path)
    hexed = "0x%02X%02X%02X" % got if got else "-"
    print("%-12s mean=%s  enum=%s" % (name, hexed, ("0x%06X" % expected) if expected else "-"))
