"""Wire GT6's coltan field: feature registration plus the worldgen data files."""

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


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(obj, handle, indent=2)
        handle.write("\n")
    print("  " + path.replace("\\", "/"))


text = io.open(FEATURES, encoding="utf-8").read()
if "COLTAN" not in text:
    anchor = "    /** GT6's fluid springs at bedrock (WorldgenFluidSpring, Loader_Worldgen:782-797). */"
    new = """    /** GT6's coltan field (WorldgenColtan, Loader_Worldgen:779). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> COLTAN =
            FEATURES.register("gt_coltan", GTColtanFeature::new);

""" + anchor
    assert text.count(anchor) == 1, "features anchor"
    io.open(FEATURES, "w", encoding="utf-8", newline="\n").write(text.replace(anchor, new))
    print("GTFeatures: gt_coltan registered")
else:
    print("GTFeatures: already wired")

write_json(os.path.join(DATA, "worldgen", "configured_feature", "gt_coltan.json"),
           {"type": "gt6ce:gt_coltan", "config": {}})
write_json(os.path.join(DATA, "worldgen", "placed_feature", "gt_coltan.json"),
           {"feature": "gt6ce:gt_coltan", "placement": []})
write_json(os.path.join(DATA, "forge", "biome_modifier", "gt_coltan.json"),
           {"type": "forge:add_features", "biomes": "#minecraft:is_overworld",
            "features": ["gt6ce:gt_coltan"], "step": "underground_ores"})
