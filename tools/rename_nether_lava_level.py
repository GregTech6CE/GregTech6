"""Rename GTNetherDepositFeature's private LAVA_LEVEL to the public NETHER_WATER_LEVEL."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import io

PATH = str(native_path("src/main/java/com/gregtech/gregtech/worldgen/GTNetherDepositFeature.java"))
OLD = "    /** Nether lava sea level. */\n    private static final int LAVA_LEVEL = 31;"
NEW = ("    /** GT6's nether water level ({@code WD.waterLevel} in a no-sky dimension) - the lava sea. */\n"
       "    public static final int NETHER_WATER_LEVEL = 31;")

text = io.open(PATH, encoding="utf-8").read()
if "NETHER_WATER_LEVEL" in text:
    print("already renamed")
else:
    assert text.count(OLD) == 1, "anchors: %d" % text.count(OLD)
    text = text.replace(OLD, NEW)
    count = text.count("LAVA_LEVEL")
    text = text.replace("LAVA_LEVEL", "NETHER_WATER_LEVEL")
    io.open(PATH, "w", encoding="utf-8", newline="\n").write(text)
    print("renamed %d usages" % count)
