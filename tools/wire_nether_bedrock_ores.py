"""Add the Nether biome modifier for GT6's bedrock ores (the feature now switches tables by dimension)."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import io
import json
import os

DATA = os.path.join(str(native_path("src")), "main", "resources", "data", "gt6ce", "forge", "biome_modifier")
PATH = os.path.join(DATA, "gt_nether_bedrock_ores.json")

payload = {
    "type": "forge:add_features",
    "biomes": "#minecraft:is_nether",
    "features": ["gt6ce:gt_bedrock_ores"],
    "step": "underground_ores",
}

os.makedirs(DATA, exist_ok=True)
if os.path.exists(PATH):
    print("already written: %s" % PATH.replace("\\", "/"))
else:
    with open(PATH, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2)
        handle.write("\n")
    print("wrote %s" % PATH.replace("\\", "/"))
