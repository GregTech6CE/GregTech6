"""Remove the twig placement that GTSurfaceDepositFeature still carried.

§35 moved GT6's WorldgenSticks into GTSurfaceFloraFeature (GT6 has exactly one WorldgenSticks
registration), so the older copy in the deposit feature placed twigs a second time. This drops it.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import io
import os

PATH = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "worldgen",
                    "GTSurfaceDepositFeature.java")

with io.open(PATH, encoding="utf-8") as handle:
    text = handle.read()

replacements = [
    ("        // Twigs (GT6 WorldgenSticks, 2 attempts/chunk): sticks lying under trees.\n"
     "        boolean twigsPlaced = placeTwigs(level, random, origin);\n\n"
     "        // GT6-ish rarity: roughly one deposit per ~6 chunks.\n"
     "        if (random.nextInt(6) != 0) return twigsPlaced;",
     "        // GT6-ish rarity: roughly one deposit per ~6 chunks. (GT6's twigs live in\n"
     "        // GTSurfaceFloraFeature — WorldgenSticks has exactly one registration.)\n"
     "        if (random.nextInt(6) != 0) return false;"),
    ("            if (waterNeighbours == 0) return twigsPlaced;", "            if (waterNeighbours == 0) return false;"),
    ("            if (deposit == null) return twigsPlaced;\n"
     "            return placeClaySeam(level, random, x, z, deposit, radius, depth, underWater) || twigsPlaced;",
     "            if (deposit == null) return false;\n"
     "            return placeClaySeam(level, random, x, z, deposit, radius, depth, underWater);"),
    ("        } else {\n            return twigsPlaced;\n        }\n        if (deposit == null) return twigsPlaced;",
     "        } else {\n            return false;\n        }\n        if (deposit == null) return false;"),
    ("        return placedAny || twigsPlaced;", "        return placedAny;"),
]

for old, new in replacements:
    if text.count(old) != 1:
        raise SystemExit("anchor not found exactly once (%d): %r" % (text.count(old), old[:60]))
    text = text.replace(old, new)

start = text.index("    /** Two attempts per chunk; twigs go on grass below leaves (forest floors). */")
end = text.index("    /** Number of water blocks in the 5x5x2 neighbourhood")
text = text[:start] + text[end:]

text = text.replace(" * pits near water (GT6 {@code BlockSands} / {@code WorldgenPit}). Small flat blobs\n"
                    " * replacing the top sediment layers.",
                    " * pits near water (GT6 {@code BlockSands} seeds and small clay seams; the big\n"
                    " * {@code WorldgenPit} pits are {@link GTPitFeature}). Small flat blobs replacing the top\n"
                    " * sediment layers.")

io.open(PATH, "w", encoding="utf-8", newline="\n").write(text)
print("twig placement removed from GTSurfaceDepositFeature")
