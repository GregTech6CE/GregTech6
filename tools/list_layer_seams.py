"""List the seam materials in the port's generated stone-layer table, and the GT6 layer defs.

Answers whether coal/lignite/bauxite/salt already exist as whole-block seams in the port (the
reason the port's vein table cites for omitting those veins).
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import io
import re
from collections import Counter

PORT = str(native_path("src/main/java/com/gregtech/gregtech/worldgen/GTStoneLayersGen.java"))
text = io.open(PORT, encoding="utf-8", errors="replace").read()
start = text.find("static void load()")
body = text[start:]
materials = re.findall(r'"([A-Za-z][A-Za-z ]*)"', body)
counts = Counter(materials)
print("port stone-layer seam materials (%d distinct, %d defs):" % (len(counts), sum(counts.values())))
for name, count in counts.most_common(40):
    print("   %-24s %d" % (name, count))

for want in ["Lignite", "Coal", "Bauxite", "RockSalt", "Salt", "Iodine", "Kaolinite", "Bentonite"]:
    print("%-12s in port stone layers: %s" % (want, counts.get(want, 0)))
