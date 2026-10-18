"""Wire GT6's river black sand and swamp turf worldgen objects (Loader_Worldgen:581-582)."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import io
import json
import os

FEATURES = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "worldgen", "GTFeatures.java")
DATA = os.path.join(str(native_path("src")), "main", "resources", "data", "gt6ce")

REGISTRATIONS = [
    ("BLACK_SAND", "gt_black_sand", "GTBlackSandFeature",
     "GT6's river black sands (WorldgenBlackSand, Loader_Worldgen:581).",
     "    /** GT6's large sand and clay pits (WorldgenPit, Loader_Worldgen:592-597). */",
     "top_layer_modification"),
    ("TURF", "gt_turf", "GTTurfFeature",
     "GT6's swamp turf bogs (WorldgenTurf, Loader_Worldgen:582).",
     "    /** GT6's large sand and clay pits (WorldgenPit, Loader_Worldgen:592-597). */",
     "top_layer_modification"),
]


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(obj, handle, indent=2)
        handle.write("\n")
    print("  " + path.replace("\\", "/"))


text = io.open(FEATURES, encoding="utf-8").read()
for field, feature_id, feature_class, comment, anchor, step in REGISTRATIONS:
    if field in text:
        print("GTFeatures: %s already wired" % field)
    else:
        new = """    /** %s */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> %s =
            FEATURES.register("%s", %s::new);

""" % (comment, field, feature_id, feature_class) + anchor
        assert text.count(anchor) == 1, "features anchor"
        text = text.replace(anchor, new)
        print("GTFeatures: %s registered" % feature_id)
io.open(FEATURES, "w", encoding="utf-8", newline="\n").write(text)

for field, feature_id, feature_class, comment, anchor, step in REGISTRATIONS:
    write_json(os.path.join(DATA, "worldgen", "configured_feature", feature_id + ".json"),
               {"type": "gt6ce:" + feature_id, "config": {}})
    write_json(os.path.join(DATA, "worldgen", "placed_feature", feature_id + ".json"),
               {"feature": "gt6ce:" + feature_id, "placement": []})
    write_json(os.path.join(DATA, "forge", "biome_modifier", feature_id + ".json"),
               {"type": "forge:add_features", "biomes": "#minecraft:is_overworld",
                "features": ["gt6ce:" + feature_id], "step": step})
