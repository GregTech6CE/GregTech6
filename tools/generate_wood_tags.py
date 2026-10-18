"""Write the vanilla block tags for the port's GT6 wood so vanilla mechanics (and other mods) see
them: ``minecraft:logs`` (leaf distance propagation, axe stripping, "is this a log" checks) and
``minecraft:leaves``.

Without these tags vanilla's LeavesBlock never finds a log near a GT6 leaf, so its distance stays at
7 and the leaf decays — the bug that made naturally grown GT6 trees lose their leaves.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import json
import os

DATA = os.path.join(str(native_path("src")), "main", "resources", "data", "minecraft", "tags", "blocks")
SPECIES = ["rubber", "maple", "rainbowood", "willow", "blue_mahoe",
           "hazel", "cinnamon", "coconut", "bluespruce",
           "pine", "ebony", "white_mahoe"]

written = []


def write_json(name, values):
    path = os.path.join(DATA, name)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        json.dump({"replace": False, "values": values}, handle, indent=2)
        handle.write("\n")
    written.append(name)


write_json("logs.json", sorted("gregtech:log_%s" % species for species in SPECIES))
write_json("leaves.json", sorted("gregtech:leaves_%s" % species for species in SPECIES))

print("wrote %d tag files" % len(written))
for name in written:
    print("  data/minecraft/tags/blocks/" + name)
