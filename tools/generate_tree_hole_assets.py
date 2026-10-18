"""Generate the assets of GT6's tree holes (resin hole / tapped maple / tapped rainbowood) and add
their language keys.

The three blocks carry a horizontal facing plus GT6's "has resin" flag, so each one needs
two models (empty hole and filled hole) whose drilled face uses GT6's ``log_hole_*`` /
``log_resin_*`` / ``log_sap_*`` texture while the other faces stay the species' log. The blockstate
rotates the model for the four horizontal facings.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import json
import os

ASSETS = os.path.join(str(native_path("src")), "main", "resources", "assets", "gt6ce")
LANG = os.path.join(ASSETS, "lang")

# (block id, species, drilled texture, filled texture)
HOLES = [
    ("resin_hole_rubber", "rubber", "log_hole_rubber", "log_resin_rubber"),
    ("tapped_maple", "maple", "log_hole_maple", "log_sap_maple"),
    ("tapped_rainbowood", "rainbowood", "log_hole_rainbowood", "log_sap_rainbowood"),
]

LANG_KEYS = [
    ("resin_hole_rubber", "Rubber Resin Hole", "橡胶树脂孔"),
    ("tapped_maple", "Tapped Maple", "采脂枫木"),
    ("tapped_rainbowood", "Tapped Rainbowood", "采脂彩虹木"),
]

written = []


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(obj, handle, indent=2)
        handle.write("\n")
    written.append(os.path.relpath(path, ASSETS).replace("\\", "/"))


def model(species, drilled):
    return {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": "gt6ce:block/iconsets/log_side_%s" % species,
            "drilled": "gt6ce:block/iconsets/%s" % drilled,
            "side": "gt6ce:block/iconsets/log_side_%s" % species,
            "top": "gt6ce:block/iconsets/log_top_%s" % species,
        },
        "elements": [{
            "from": [0, 0, 0],
            "to": [16, 16, 16],
            "faces": {
                "north": {"texture": "#drilled", "cullface": "north"},
                "south": {"texture": "#side", "cullface": "south"},
                "east": {"texture": "#side", "cullface": "east"},
                "west": {"texture": "#side", "cullface": "west"},
                "up": {"texture": "#top", "cullface": "up"},
                "down": {"texture": "#top", "cullface": "down"},
            },
        }],
    }


def blockstate(block_id):
    variants = {}
    for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for resin, suffix in ((False, "hole"), (True, "resin")):
            entry = {"model": "gt6ce:block/wood/%s_%s" % (block_id, suffix)}
            if rotation:
                entry["y"] = rotation
            variants["facing=%s,resin=%s" % (facing, "true" if resin else "false")] = entry
    return {"variants": variants}


for block_id, species, empty, filled in HOLES:
    write_json(os.path.join(ASSETS, "blockstates", "%s.json" % block_id), blockstate(block_id))
    write_json(os.path.join(ASSETS, "models", "block", "wood", "%s_hole.json" % block_id), model(species, empty))
    write_json(os.path.join(ASSETS, "models", "block", "wood", "%s_resin.json" % block_id), model(species, filled))

# Language keys: textual insertion next to the twig block so the files keep their formatting.
for name, index in (("en_us.json", 1), ("zh_cn.json", 2)):
    path = os.path.join(LANG, name)
    with open(path, encoding="utf-8") as handle:
        lines = handle.readlines()
    if any('"block.gt6ce.%s"' % LANG_KEYS[0][0] in line for line in lines):
        print("%s: tree-hole keys already present" % name)
        continue
    anchor = next((i for i, line in enumerate(lines) if '"block.gt6ce.rock"' in line), None)
    if anchor is None:
        raise SystemExit("%s: no anchor line for the tree-hole keys" % name)
    indent = lines[anchor][:len(lines[anchor]) - len(lines[anchor].lstrip())]
    block = ["%s\"block.gt6ce.%s\": \"%s\",\n" % (indent, key, values[index - 1]) for key, *values in LANG_KEYS]
    lines[anchor:anchor] = block
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.writelines(lines)
    print("%s: inserted %d tree-hole keys" % (name, len(block)))

print("holes: %d, wrote %d asset files" % (len(HOLES), len(written)))
for rel in written:
    print("  " + rel)
