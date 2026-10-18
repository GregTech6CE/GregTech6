"""Wire GT6's ocean / river / swamp water passes (Loader_Worldgen:576-578)."""

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
DATA = os.path.join(str(native_path("src")), "main", "resources", "data", "gregtech")
FEATURE_ID = "gt_water_bodies"
FIELD = "WATER_BODIES"
CLASS = "GTWaterBodyFeature"
ANCHOR = "    /** GT6's river black sands (WorldgenBlackSand, Loader_Worldgen:581). */"


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(obj, handle, indent=2)
        handle.write("\n")
    print("  " + path.replace("\\", "/"))


text = io.open(FEATURES, encoding="utf-8").read()
if FIELD in text:
    print("GTFeatures: already wired")
else:
    new = """    /** GT6's ocean/river/swamp water (WorldgenOcean/River/Swamp, Loader_Worldgen:576-578). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> %s =
            FEATURES.register("%s", %s::new);

""" % (FIELD, FEATURE_ID, CLASS) + ANCHOR
    assert text.count(ANCHOR) == 1, "features anchor"
    io.open(FEATURES, "w", encoding="utf-8", newline="\n").write(text.replace(ANCHOR, new))
    print("GTFeatures: %s registered" % FEATURE_ID)

write_json(os.path.join(DATA, "worldgen", "configured_feature", FEATURE_ID + ".json"),
           {"type": "gregtech:" + FEATURE_ID, "config": {}})
write_json(os.path.join(DATA, "worldgen", "placed_feature", FEATURE_ID + ".json"),
           {"feature": "gregtech:" + FEATURE_ID, "placement": []})
# Last step of the chunk pipeline: every other feature still sees vanilla water, like before this
# batch, and the conversion happens once the terrain and its decoration are complete.
write_json(os.path.join(DATA, "forge", "biome_modifier", FEATURE_ID + ".json"),
           {"type": "forge:add_features", "biomes": "#minecraft:is_overworld",
            "features": ["gregtech:" + FEATURE_ID], "step": "top_layer_modification"})
