"""Generate the missing GT6-species tree assets and fix the property-variant blockstates of the
GT6 wood blocks.

Two jobs:

1. ``sapling_<species>`` for the four GT6 tree species that only existed as decorative icon-set
   blocks (hazel, cinnamon, coconut, blue spruce): blockstate, block model (cross) and item model,
   all reusing the ``sapling_small_<species>`` texture GT6 ships.
2. Re-emit the blockstates of the GT6 wood blocks with the property variants their block classes
   actually declare — ``LeavesBlock`` has ``distance``/``persistent`` and ``SaplingBlock`` has
   ``stage``, so the single ``""`` variant the icon-set generator produced never matched a real
   block state (missing model). ``log_*``/``beam_*`` (axis) and ``planks_*`` were already correct.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import json
import os

ASSETS = os.path.join(str(native_path("src")), "main", "resources", "assets", "gregtech")
SPECIES = ["rubber", "maple", "rainbowood", "willow", "blue_mahoe",
           "hazel", "cinnamon", "coconut", "bluespruce",
           "pine", "ebony", "white_mahoe"]
NEW_SPECIES = ["hazel", "cinnamon", "coconut", "bluespruce"]

written = []


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(obj, handle, indent=2)
        handle.write("\n")
    written.append(os.path.relpath(path, ASSETS).replace("\\", "/"))


def leaves_blockstate(species):
    variants = {}
    for distance in range(1, 8):
        for persistent in ("false", "true"):
            variants["distance=%d,persistent=%s" % (distance, persistent)] = {
                "model": "gregtech:block/iconsets/leaves_%s" % species}
    return {"variants": variants}


def sapling_blockstate(species):
    return {"variants": {"stage=%d" % stage: {"model": "gregtech:block/wood/sapling_%s" % species}
                         for stage in (0, 1)}}


for species in SPECIES:
    write_json(os.path.join(ASSETS, "blockstates", "leaves_%s.json" % species), leaves_blockstate(species))
    write_json(os.path.join(ASSETS, "blockstates", "sapling_%s.json" % species), sapling_blockstate(species))
    write_json(os.path.join(ASSETS, "models", "block", "wood", "sapling_%s.json" % species),
               {"parent": "minecraft:block/cross",
                "textures": {"cross": "gregtech:block/iconsets/sapling_small_%s" % species}})
    write_json(os.path.join(ASSETS, "models", "item", "sapling_%s.json" % species),
               {"parent": "minecraft:item/generated",
                "textures": {"layer0": "gregtech:block/iconsets/sapling_small_%s" % species}})

print("species: %d (new: %s)" % (len(SPECIES), ", ".join(NEW_SPECIES)))
print("wrote %d files" % len(written))
for rel in written[:6]:
    print("  " + rel)
print("  ...")
