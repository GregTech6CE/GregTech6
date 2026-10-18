"""Wire GT6's surface rock litter (WorldgenRocks) and deep-ocean prismarine pylons (WorldgenDeepOcean)."""

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
ANCHOR = "    /** GT6's ocean/river/swamp water (WorldgenOcean/River/Swamp, Loader_Worldgen:576-578). */"

REGISTRATIONS = [
    ("ROCKS", "gt_rocks", "GTRocksFeature",
     "GT6's surface rock litter (WorldgenRocks, Loader_Worldgen:618).", "vegetal_decoration"),
    ("DEEP_OCEAN", "gt_deep_ocean", "GTDeepOceanFeature",
     "GT6's deep ocean prismarine pylons (WorldgenDeepOcean, Loader_Worldgen:580).",
     "top_layer_modification"),
]


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(obj, handle, indent=2)
        handle.write("\n")
    print("  " + path.replace("\\", "/"))


text = io.open(FEATURES, encoding="utf-8").read()
for field, feature_id, feature_class, comment, step in REGISTRATIONS:
    if field in text:
        print("GTFeatures: %s already wired" % field)
        continue
    new = """    /** %s */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> %s =
            FEATURES.register("%s", %s::new);

""" % (comment, field, feature_id, feature_class) + ANCHOR
    assert text.count(ANCHOR) == 1, "features anchor"
    text = text.replace(ANCHOR, new)
    print("GTFeatures: %s registered" % feature_id)
io.open(FEATURES, "w", encoding="utf-8", newline="\n").write(text)

for field, feature_id, feature_class, comment, step in REGISTRATIONS:
    write_json(os.path.join(DATA, "worldgen", "configured_feature", feature_id + ".json"),
               {"type": "gt6ce:" + feature_id, "config": {}})
    write_json(os.path.join(DATA, "worldgen", "placed_feature", feature_id + ".json"),
               {"feature": "gt6ce:" + feature_id, "placement": []})
    write_json(os.path.join(DATA, "forge", "biome_modifier", feature_id + ".json"),
               {"type": "forge:add_features", "biomes": "#minecraft:is_overworld",
                "features": ["gt6ce:" + feature_id], "step": step})
