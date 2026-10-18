"""Register GT6's book shelf: block + block entity + assets + language keys."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import io
import json
import os

DECOR = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "registry", "GTDecorBlocks.java")
BE = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "registry", "GTBlockEntities.java")
ASSETS = os.path.join(str(native_path("src")), "main", "resources", "assets", "gregtech")
LANG = os.path.join(ASSETS, "lang")

FIELD = """    // N12: Misc placeables
    public static RegistryObject<Block> LOOT_CRATE;"""
NEW_FIELD = """    // N12: Misc placeables
    public static RegistryObject<Block> LOOT_CRATE;
    /** GT6's book shelf (MultiTileEntityBookShelf, LoaderBookList). */
    public static RegistryObject<com.gregtech.gregtech.block.BookShelfBlock> BOOKSHELF;"""

REGISTER_ANCHOR = """        LOOT_CRATE = reg("loot_crate", () -> new com.gregtech.gregtech.block.LootCrateBlock(
                props(MapColor.WOOD, 4f).sound(SoundType.WOOD)));"""
NEW_REGISTER = REGISTER_ANCHOR + """
        BOOKSHELF = reg("bookshelf", () -> new com.gregtech.gregtech.block.BookShelfBlock(
                props(MapColor.WOOD, 1.5f).sound(SoundType.WOOD)));"""

BE_ANCHOR = """    /** GT6's drying construction foam ({@code MultiTileEntityCFoam}). */"""
NEW_BE = """    /** GT6's book shelf (MultiTileEntityBookShelf): eight slots of books. */
    public static final RegistryObject<BlockEntityType<com.gregtech.gregtech.blockentity.inventory.BookShelfBlockEntity>> BOOKSHELF =
            BLOCK_ENTITY_TYPES.register("bookshelf", () ->
                    BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.inventory.BookShelfBlockEntity::new,
                            GTDecorBlocks.BOOKSHELF.get()).build(null));

""" + BE_ANCHOR

written = []


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(obj, handle, indent=2)
        handle.write("\n")
    written.append(os.path.relpath(path, ASSETS).replace("\\", "/"))


decor = io.open(DECOR, encoding="utf-8").read()
if "BOOKSHELF" in decor:
    print("GTDecorBlocks: already wired")
else:
    assert decor.count(FIELD) == 1, "decor field anchor"
    decor = decor.replace(FIELD, NEW_FIELD)
    assert decor.count(REGISTER_ANCHOR) == 1, "decor register anchor"
    decor = decor.replace(REGISTER_ANCHOR, NEW_REGISTER)
    io.open(DECOR, "w", encoding="utf-8", newline="\n").write(decor)
    print("GTDecorBlocks: bookshelf registered")

be = io.open(BE, encoding="utf-8").read()
if "BOOKSHELF" in be:
    print("GTBlockEntities: already wired")
else:
    assert be.count(BE_ANCHOR) == 1, "be anchor"
    be = be.replace(BE_ANCHOR, NEW_BE)
    io.open(BE, "w", encoding="utf-8", newline="\n").write(be)
    print("GTBlockEntities: bookshelf registered")

# Keep the historical wiring entry point from restoring obsolete cube assets.
from generate_bookshelf_assets import generate
generate()

for name, label in (("en_us.json", "Book Shelf"), ("zh_cn.json", "书架")):
    file = os.path.join(LANG, name)
    lines = io.open(file, encoding="utf-8").readlines()
    anchor = next((i for i, line in enumerate(lines) if '"block.gregtech.loot_crate"' in line), None)
    if anchor is None:
        raise SystemExit("%s: no anchor" % name)
    key = '"block.gregtech.bookshelf"'
    if any(key in line for line in lines):
        print("%s: key present" % name)
        continue
    indent = lines[anchor][:len(lines[anchor]) - len(lines[anchor].lstrip())]
    lines[anchor:anchor] = ['%s%s: "%s",\n' % (indent, key, label)]
    io.open(file, "w", encoding="utf-8", newline="\n").writelines(lines)
    print("%s: bookshelf key" % name)

print("bookshelf wiring checked and current assets generated")
