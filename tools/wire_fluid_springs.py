"""Wire the fluid spring: GT6's spring block gets its block entity type, and the worldgen data files
of {@code WorldgenFluidSpring} are added."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import io
import json
import os
import re

ROOT = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech")
written = []


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(obj, handle, indent=2)
        handle.write("\n")
    written.append(path)


# 1. fluid_spring becomes a block entity block.
decor = os.path.join(ROOT, "registry", "GTDecorBlocks.java")
text = io.open(decor, encoding="utf-8").read()
if "FluidSpringBlock" not in text:
    old = 'FLUID_SPRING = reg("fluid_spring", () -> new Block(props(MapColor.WATER, 3f).sound(SoundType.STONE)));'
    new = ('FLUID_SPRING = reg("fluid_spring", () -> new com.gregtech.gregtech.block.FluidSpringBlock(\n'
           '                props(MapColor.WATER, 3f).sound(SoundType.STONE)));')
    assert text.count(old) == 1, "fluid_spring anchor"
    io.open(decor, "w", encoding="utf-8", newline="\n").write(text.replace(old, new))
    print("GTDecorBlocks: fluid_spring is a FluidSpringBlock")

# 2. Block entity type.
be = os.path.join(ROOT, "registry", "GTBlockEntities.java")
text = io.open(be, encoding="utf-8").read()
if "FLUID_SPRING" not in text:
    old = """    /** GT6's berry bush (WorldgenBushes places it, players set its berry type). */"""
    new = """    /** GT6's fluid springs at bedrock (WorldgenFluidSpring). */
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.FluidSpringBlockEntity>> FLUID_SPRING =
            BLOCK_ENTITY_TYPES.register("fluid_spring", () ->
                    BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.FluidSpringBlockEntity::new,
                            GTDecorBlocks.FLUID_SPRING.get()).build(null));

""" + old
    assert text.count(old) == 1, "block entity anchor"
    io.open(be, "w", encoding="utf-8", newline="\n").write(text.replace(old, new))
    print("GTBlockEntities: fluid_spring type registered")

# 3. Feature registration + data files.
features = os.path.join(ROOT, "worldgen", "GTFeatures.java")
text = io.open(features, encoding="utf-8").read()
if "FLUID_SPRINGS" not in text:
    old = """    /** GT6's loose items lying around in the Nether (WorldgenRacks, Loader_Worldgen:619). */"""
    new = """    /** GT6's fluid springs at bedrock (WorldgenFluidSpring, Loader_Worldgen:782-797). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> FLUID_SPRINGS =
            FEATURES.register("gt_fluid_springs", GTFluidSpringsFeature::new);

""" + old
    assert text.count(old) == 1, "features anchor"
    io.open(features, "w", encoding="utf-8", newline="\n").write(text.replace(old, new))
    print("GTFeatures: gt_fluid_springs registered")

data = os.path.join(str(native_path("src")), "main", "resources", "data", "gregtech")
write_json(os.path.join(data, "worldgen", "configured_feature", "gt_fluid_springs.json"),
           {"type": "gregtech:gt_fluid_springs", "config": {}})
write_json(os.path.join(data, "worldgen", "placed_feature", "gt_fluid_springs.json"),
           {"feature": "gregtech:gt_fluid_springs", "placement": []})
write_json(os.path.join(data, "forge", "biome_modifier", "gt_fluid_springs.json"),
           {"type": "forge:add_features", "biomes": "#minecraft:is_overworld",
            "features": ["gregtech:gt_fluid_springs"], "step": "underground_decoration"})

print("wrote %d data files" % len(written))
for path in written:
    print("  " + path.replace("\\", "/"))
