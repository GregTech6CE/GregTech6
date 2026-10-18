"""Print the port's vein-table order and the GT6 registration order, for insertion parity."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import io
import json
import re

PORT = str(native_path("src/main/java/com/gregtech/gregtech/worldgen/GTOreVeins.java"))
text = io.open(PORT, encoding="utf-8").read()


def vein_names(table):
    start = text.find(table + " = List.of(")
    if start < 0:
        return []
    end = text.find("    );", start)
    return re.findall(r'new OreVein\("([a-z0-9_]+)"', text[start:end])


for table in ["OVERWORLD_VEINS", "NETHER_VEINS", "END_VEINS"]:
    names = vein_names(table)
    print("%-16s (%d): %s" % (table, len(names), " ".join(names)))

data = json.load(open("docs/gt6-large-ores.json", encoding="utf-8"))
for dim in ["GEN_OVERWORLD", "GEN_END"]:
    names = [r["name"] for r in data["registrations"] if dim in r["dims"] and r["default"].strip() == "T"]
    print("\nGT6 %s (%d): %s" % (dim, len(names), " ".join(names)))
