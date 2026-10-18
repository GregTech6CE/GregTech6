#!/usr/bin/env python3
"""Register the GT6 MultiItem categories (random tools, bottles, food, bumblebees).

Parses the ID -> name registrations from the GT6 sources, copies the numbered
textures into name-based files (technological-style), and generates
GTMultiItemsGen.java + item models + lang entries.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import json
import os
import re
import shutil

GT6 = r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main"
ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, str(native_path("src")), "main", "resources", "assets", "gregtech")
ITEM_TEX = os.path.join(ASSETS, "textures", "item")
ITEM_MODELS = os.path.join(ASSETS, "models", "item")
OUT_JAVA = os.path.join(ROOT, str(native_path("src")), "main", "java", "com", "gregtech", "gregtech",
                        "registry", "GTMultiItemsGen.java")

SRC_TEX = os.path.join(GT6, "resources", "assets", "gregtech", "textures", "items")

CATEGORIES = {
    "randomtools": ("gregtech/items/MultiItemRandomTools.java", "gt.multiitem.randomtools"),
    "bottles": ("gregtech/items/MultiItemBottles.java", "gt.multiitem.bottles"),
    "food": ("gregtech/items/MultiItemFood.java", "gt.multiitem.food"),
    "cans": ("gregtech/items/MultiItemCans.java", "gt.multiitem.cans"),
}

# Ids the port registers in Java instead of from this table (see GTMultiItems.registerAll). Emitting
# a row for them would register the same id twice, which crashes the item registry.
HAND_REGISTERED = {
    "dusty_guide_book",  # MultiItemBooks:67, registered as a LootBagItem (gt.books)
}

ADD_ITEM = re.compile(r'addItem\(\s*(\d+)\s*,\s*"((?:\\.|[^"\\])*)"(?:\s*,\s*"((?:\\.|[^"\\])*)")?')
BEE_SPECIES = re.compile(r'\bmake\(\s*(\d+)\s*,\s*"((?:\\.|[^"\\])*)"')


def snake(name: str) -> str:
    s = name.lower()
    s = re.sub(r"[''.()&,!?]", "", s)
    s = re.sub(r"[^a-z0-9]+", "_", s).strip("_")
    return s


def parse_add_items(java_rel: str):
    path = os.path.join(GT6, "java", java_rel)
    text = open(path, encoding="utf-8", errors="replace").read()
    text = "\n".join(line.split("//")[0] for line in text.splitlines())
    out = {}
    for m in ADD_ITEM.finditer(text):
        tooltip = m.group(3) or ""
        if tooltip.startswith("gt.") or tooltip.startswith("tile.") or tooltip.startswith("item."):
            tooltip = ""  # lang keys, not display text
        out.setdefault(int(m.group(1)), (m.group(2), tooltip))
    return out


def copy_texture(src_dir: str, tex_id: int, dst_dir: str, dst_name: str) -> bool:
    src = os.path.join(src_dir, f"{tex_id}.png")
    if not os.path.exists(src):
        return False
    os.makedirs(dst_dir, exist_ok=True)
    shutil.copy(src, os.path.join(dst_dir, dst_name + ".png"))
    meta = src + ".mcmeta"
    if os.path.exists(meta):
        shutil.copy(meta, os.path.join(dst_dir, dst_name + ".png.mcmeta"))
    return True


def write_model(item_id: str, layers):
    textures = {f"layer{i}": tex for i, tex in enumerate(layers)}
    with open(os.path.join(ITEM_MODELS, item_id + ".json"), "w", encoding="utf-8") as f:
        json.dump({"parent": "minecraft:item/generated", "textures": textures}, f, indent=2)
        f.write("\n")


def cleanup_previous():
    """Remove artifacts of a previous run (old GTMultiItemsGen ids)."""
    out_java = OUT_JAVA
    if not os.path.exists(out_java):
        return set()
    old_ids = set(re.findall(r'^\s*"([a-z0-9_]+)", "', open(out_java, encoding="utf-8").read(), re.M))
    removed = 0
    for item_id in old_ids:
        model = os.path.join(ITEM_MODELS, item_id + ".json")
        if os.path.exists(model):
            os.remove(model)
            removed += 1
    for category in list(CATEGORIES) + ["bumblebee"]:
        folder = os.path.join(ITEM_TEX, category)
        if os.path.isdir(folder):
            shutil.rmtree(folder)
    for lang_path in (os.path.join(ASSETS, "lang", "en_us.json"),
                      os.path.join(ASSETS, "lang", "zh_cn.json")):
        with open(lang_path, encoding="utf-8-sig") as f:
            lang = json.load(f)
        for item_id in old_ids:
            lang.pop("item.gregtech." + item_id, None)
        with open(lang_path, "w", encoding="utf-8") as f:
            json.dump(lang, f, indent=2, ensure_ascii=False, sort_keys=True)
            f.write("\n")
    print(f"cleanup: removed {removed} old models, texture folders, lang keys")
    return old_ids


def unique_id(base: str, used: set) -> str:
    if base not in used:
        return base
    n = 2
    while f"{base}_{n}" in used:
        n += 1
    return f"{base}_{n}"


def existing_ids_from_sources() -> set:
    """Authoritative taken-id set from the Java registries (the models directory
    alone is not reliable — an earlier run may have clobbered block-item models)."""
    java = os.path.join(ROOT, str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "registry")
    used = set()
    icon = open(os.path.join(java, "GTIconSetBlocks.java"), encoding="utf-8").read()
    used |= set(re.findall(r'"([a-z0-9_]+)"', icon.split("buildIconNames() {")[1].split("};")[0]))
    # composite block ids (log_/beam_/bale_/grass etc.)
    for m in re.finditer(r'list\.add\(new String\[\]\{"([a-z0-9_]+)"', icon):
        used.add(m.group(1))
    for prefix, names in (("log_", "bluemahoe bluespruce cinnamon coconut dry frozen hazel maple mossy rainbowood rotten rubber willow"),
                          ("beam_", "acacia birch bluemahoe bluespruce cinnamon coconut darkoak darkwood greatwood hazel jungle maple oak rainbowood rubber rubberwood silverwood skyroot spruce willow wood"),
                          ("bale_", "barley oat rice rye"),
                          ("grass_", "dry moldy rotten"),
                          ("grassblock_", "brown dark light medium normal yellow")):
        for n in names.split():
            used.add(prefix + n)
    used.update(["grass", "rock", "twigs"])
    tech = open(os.path.join(java, "GTTechnological.java"), encoding="utf-8").read()
    used |= set(re.findall(r'"([a-z0-9_]+)"', tech.split("String[] IDS = {")[1].split("};")[0]))
    used |= {f"integrated_circuit_{i}" for i in range(25)}
    return used


def main():
    cleanup_previous()

    # Taken ids = every existing item model file + the authoritative Java registries.
    used = {os.path.splitext(f)[0] for f in os.listdir(ITEM_MODELS) if f.endswith(".json")}
    used |= existing_ids_from_sources()
    registered = []  # (id, display name)

    for category, (java_rel, tex_folder) in CATEGORIES.items():
        items = parse_add_items(java_rel)
        src_dir = os.path.join(SRC_TEX, tex_folder)
        dst_dir = os.path.join(ITEM_TEX, category)
        count = 0
        for tex_id, (display, tooltip) in sorted(items.items()):
            base = snake(display)
            if not base:
                continue
            item_id = unique_id(base, used)
            if item_id in HAND_REGISTERED or base in HAND_REGISTERED:
                continue
            if not copy_texture(src_dir, tex_id, dst_dir, item_id):
                continue
            used.add(item_id)
            write_model(item_id, [f"gregtech:item/{category}/{item_id}"])
            registered.append((item_id, display, category, tooltip))
            count += 1
        print(f"{category}: {count} items")

    # Bumblebees: species drones + overlay-layered princess/queen/dead variants.
    bee_src = open(os.path.join(GT6, "java", "gregtech/items/MultiItemBumbles.java"),
                   encoding="utf-8", errors="replace").read()
    bee_src = "\n".join(line.split("//")[0] for line in bee_src.splitlines())
    bee_dir = os.path.join(SRC_TEX, "gt.multiitem.bumblebee")
    dst_dir = os.path.join(ITEM_TEX, "bumblebee")
    os.makedirs(dst_dir, exist_ok=True)
    for overlay in ("overlay_princess", "overlay_queen", "overlay_dead"):
        src = os.path.join(bee_dir, overlay + ".png")
        if os.path.exists(src):
            shutil.copy(src, os.path.join(dst_dir, overlay + ".png"))
    bee_count = 0
    for m in BEE_SPECIES.finditer(bee_src):
        species_id, display = int(m.group(1)), m.group(2)
        base = snake(display)
        if not base:
            continue
        base = unique_id(base, used)
        if not copy_texture(bee_dir, species_id, dst_dir, base):
            continue
        used.add(base)
        base_tex = f"gregtech:item/bumblebee/{base}"
        for suffix, overlay, label in (
                ("_drone", None, " Drone"),
                ("_princess", "overlay_princess", " Princess"),
                ("_queen", "overlay_queen", " Queen"),
                ("_dead", "overlay_dead", " (Dead)")):
            item_id = unique_id(base + suffix, used)
            used.add(item_id)
            layers = [base_tex] + ([f"gregtech:item/bumblebee/{overlay}"] if overlay else [])
            write_model(item_id, layers)
            registered.append((item_id, display + label, "bumblebee", ""))
            bee_count += 1
    print(f"bumblebee: {bee_count} items")

    # Generated Java registry data.
    def esc(s):
        return s.replace("\\", "\\\\").replace('"', '\\"')
    entries = "\n".join(
        f'        "{item_id}", "{esc(display)}", "{category}", "{"T" if tooltip else ""}",'
        for item_id, display, category, tooltip in registered)
    content = f"""package com.gregtech.gregtech.registry;

/**
 * GT6 MultiItem registrations (random tools, bottles, food, bumblebees) —
 * {len(registered)} items as (id, display, category, tooltip-flag) tuples.
 *
 * <p>GENERATED by tools/transpile_gt6_multiitems.py — do not edit by hand.</p>
 */
final class GTMultiItemsGen {{
    private GTMultiItemsGen() {{}}

    static final String[] ENTRIES = {{
{entries}
    }};
}}
"""
    with open(OUT_JAVA, "w", encoding="utf-8") as f:
        f.write(content)

    # Lang entries (en; zh falls back to the English display name for now).
    for lang_path in (os.path.join(ASSETS, "lang", "en_us.json"),
                      os.path.join(ASSETS, "lang", "zh_cn.json")):
        with open(lang_path, encoding="utf-8-sig") as f:
            lang = json.load(f)
        for item_id, display, category, tooltip in registered:
            lang.setdefault("item.gregtech." + item_id, display)
            if tooltip:
                lang.setdefault("item.gregtech." + item_id + ".tooltip", tooltip)
        with open(lang_path, "w", encoding="utf-8") as f:
            json.dump(lang, f, indent=2, ensure_ascii=False, sort_keys=True)
            f.write("\n")
    print(f"total registered: {len(registered)}")


if __name__ == "__main__":
    main()

    from sync_standard_chinese import sync
    sync()
