"""Wire the drying construction foam: the fresh block gets its block entity type and the hardened
block stays a plain block."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import io
import os

ROOT = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech")

# 1. cfoam_fresh becomes the drying block.
decor = os.path.join(ROOT, "registry", "GTDecorBlocks.java")
text = io.open(decor, encoding="utf-8").read()
old = 'CFOAM_FRESH = reg("cfoam_fresh", () -> new CFoamBlock(props(MapColor.WOOL, 0.5f).sound(SoundType.WOOL)));'
if "new CFoamBlock(true," not in text:
    assert text.count(old) == 1, "cfoam_fresh anchor"
    new = 'CFOAM_FRESH = reg("cfoam_fresh", () -> new CFoamBlock(true, props(MapColor.WOOL, 0.5f).sound(SoundType.WOOL)));'
    io.open(decor, "w", encoding="utf-8", newline="\n").write(text.replace(old, new))
    print("GTDecorBlocks: cfoam_fresh dries")
else:
    print("GTDecorBlocks: already wired")

# 2. Block entity type for the wet foam.
be = os.path.join(ROOT, "registry", "GTBlockEntities.java")
text = io.open(be, encoding="utf-8").read()
if 'register("cfoam"' not in text:
    old = """    /** GT6's fluid springs at bedrock (WorldgenFluidSpring). */"""
    new = """    /** GT6's drying construction foam ({@code MultiTileEntityCFoam}). */
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.CFoamBlockEntity>> CFOAM =
            BLOCK_ENTITY_TYPES.register("cfoam", () ->
                    BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.CFoamBlockEntity::new,
                            GTDecorBlocks.CFOAM_FRESH.get()).build(null));

""" + old
    assert text.count(old) == 1, "block entity anchor"
    io.open(be, "w", encoding="utf-8", newline="\n").write(text.replace(old, new))
    print("GTBlockEntities: cfoam type registered")
else:
    print("GTBlockEntities: already wired")
