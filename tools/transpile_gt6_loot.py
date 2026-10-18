"""Transpile GT6's ``Loader_Loot`` rows into the port's item-spec format.

GT6 fills its chests from loot tables it defines with ``addLoot(table, weight, min, max, stack)``
(``gregtech/loaders/c/Loader_Loot.java``, 575 rows). Two kinds of table appear there:

  * ``addLoot("gt.<name>", …)`` — GT's own tables (``gt.misc``, ``gt.gems``, ``gt.flawless``,
    ``gt.seeds``, ``gt.saplings``, ``gt.bottles``, ``gt.books``, ``gt.matdicts``). These are
    reachable in GT6 through the loot-bag items (``Behavior_Drop_Loot``: Bagged Sapling, Seed Pouch,
    Gem Pouch, Loot Pouch, Dusty Guide Book) and through placeable GT loot chests — *not* through
    world chests.
  * ``addLoot(ChestGenHooks.<CONST>, …)`` — the rows GT6 adds to the **vanilla** chest tables, i.e.
    exactly what a player finds in a dungeon/mineshaft/stronghold/pyramid/village chest (the GT
    chest a worldgen chest is replaced with carries ``gt.dungeonloot = <vanilla category>`` and
    rolls that category: ``ChestGenHooksChestReplacer:122``, ``MultiTileEntityChest:262``,
    ``ST.generateLoot``). Those rows are emitted with the table name ``van.<CONST>``.

Specs:

  * ``OP.<prefix>.mat(MT.<Material>, n)``  -> ``i:<prefix>:<Material>:n``   (port material forms)
  * ``OP.<prefix>.dat(MT.<Material>, n)``  -> ``i:<prefix>:<Material>:n``
  * ``IL.<Item>.get(n)``                   -> ``tech:<item>:n``            (technological items)
  * ``ST.make(Items.<x>, n, <meta>)``      -> ``v:<x>:n``                  (meta is dropped: see below)
  * ``ST.make(Blocks.<x>, n, <meta>)``     -> ``v:<x>:n``
  * ``ST.book("<mapping>")``               -> ``book:<mapping>``           (content/book/GTBooks)

Rows for other mods' items (``ST.make(MD.<mod>, …)``), fluid-filled bottles (``FL.<fluid>.fill(…)``)
and stack arrays are recorded as skipped with their reason: the port registers neither those mods nor
book items, and a vanilla meta value (wool colour, sapling type, …) does not survive into a 1.20.1
item id, so guessing one would put the wrong item in a chest.

Usage:  python tools/transpile_gt6_loot.py
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import json
import pathlib
import re

GT6 = pathlib.Path(r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java")
SOURCE = GT6 / "gregtech/loaders/c/Loader_Loot.java"
OUT_JAVA = native_path(pathlib.Path(
    "src/main/java/com/gregtech/gregtech/loaders/c/GTLootGen.java"))
OUT_JSON = pathlib.Path("docs/gt6-loot-rows.json")

ROW = re.compile(
    r"addLoot\(\s*\"([^\"]+)\"\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(.*?)\)\s*;", re.S)
# ``addLoot(ChestGenHooks.VILLAGE_BLACKSMITH, w, min, max, stack)`` — the rows GT6 adds to the
# *vanilla* chest tables, which is the loot a player actually finds in world chests: the GT chest a
# worldgen chest is replaced with carries ``gt.dungeonloot = <vanilla category>`` and rolls that
# category (ChestGenHooksChestReplacer:122, MultiTileEntityChest:262, ST.generateLoot).
VANILLA_ROW = re.compile(
    r"addLoot\(\s*ChestGenHooks\.(\w+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(.*?)\)\s*;", re.S)
# ``ChestGenHooks.getInfo("gt.books").setMin( 8)`` / ``.setMax(24)`` — how many items a roll hands out.
COUNT = re.compile(
    r'getInfo\(\s*"(gt\.\w+)"\s*\)\s*\.set(Min|Max)\(\s*(\d+)\s*\)')

MAT = re.compile(
    r"OP\.(\w+)\s*\.\s*(?:mat|dat)\(\s*(?:ANY\.|MT\.STONES\.|MT\.OREMATS\.|MT\.)(\w+)")
ITEM = re.compile(r"IL\.(\w+)\s*\.get\(\s*(\d+)\s*\)")
VANILLA = re.compile(r"ST\.make\(\s*(Items|Blocks)\.(\w+)\s*,\s*(\d+)\s*,\s*(\d+)")
BOOK = re.compile(r'ST\.book\(\s*"([A-Za-z0-9_]+)"\s*\)')

# 1.7.10 vanilla names that 1.20.1 spells differently (the port's own table lives in
# tools/generate_vanilla_compositions.py); the loot rows hit the music discs.
VANILLA_RENAMES = {
    "record_13": "music_disc_13", "record_cat": "music_disc_cat",
    "record_blocks": "music_disc_blocks", "record_chirp": "music_disc_chirp",
    "record_far": "music_disc_far", "record_mall": "music_disc_mall",
    "record_mellohi": "music_disc_mellohi", "record_stal": "music_disc_stal",
    "record_strad": "music_disc_strad", "record_ward": "music_disc_ward",
    "record_11": "music_disc_11", "record_wait": "music_disc_wait",
    "brick_block": "bricks", "nether_brick": "nether_bricks",
    "hardened_clay": "terracotta", "noteblock": "note_block",
    "stonebrick": "stone_bricks", "waterlily": "lily_pad", "deadbush": "dead_bush",
    "snow_layer": "snow", "snow": "snow_block", "speckled_melon": "glistering_melon_slice",
    "fireworks": "firework_rocket", "firework_charge": "firework_star",
    "reeds": "sugar_cane",
}

# 1.7.10 blocks whose loot rows carry a *variant* meta; 1.20.1 splits them into separate items, so
# the meta identifies the item (dropping it would turn six different plants into one item, and the
# GT6 sapling bag into six copies of it).
VANILLA_META = {
    # Blocks.tallgrass: 0 = shrub (dead bush), 1 = grass, 2 = fern
    ("tallgrass", 0): "dead_bush",
    ("tallgrass", 1): "grass",
    ("tallgrass", 2): "large_fern",
    # Blocks.double_plant: 0 sunflower, 1 syringa, 2 grass, 3 fern, 4 rose, 5 paeonia
    ("double_plant", 0): "sunflower", ("double_plant", 1): "lilac",
    ("double_plant", 2): "tall_grass", ("double_plant", 3): "large_fern",
    ("double_plant", 4): "rose_bush", ("double_plant", 5): "peony",
    # Items.dye meta 3 = cocoa beans
    ("dye", 3): "cocoa_beans",
}

# GT6 items whose port id is spelled differently, or that the port does not register at all.
# The loot bags and bottles are GT6 MultiItem entries, so the port names them after their GT6 display
# names ("Bagged Sapling" -> bagged_sapling, "Green Slime Bottle" -> green_slime_bottle, ...).
ALIASES = {
    "porcelain_cup": "modeled_porcelain_cup",
    "bag_loot_sapling": "bagged_sapling",
    "bag_loot_seeds": "seed_pouch",
    "bag_loot_gems": "gem_pouch",
    "bag_loot_misc": "loot_pouch",
    "book_loot_guide": "dusty_guide_book",
    "book_loot_matdict": "dusty_material_dictionary",
    "bottle_slime_green": "green_slime_bottle",
    "bottle_lubricant": "lubricant_bottle",
    "bottle_holy_water": "holy_water",
    "bottle_purple_drink": "purple_drink",
    # §25: the Loot Bottle is ported, so its gt.bottles rows matter; the port's bottle ids follow the
    # GT6 display names ("Milk" -> milk, "Bottle o'Blood" -> bottle_oblood, …).
    "bottle_milk": "milk",
    "bottle_milk_soy": "soy_milk",
    "bottle_beer": "beer",
    "bottle_blood": "bottle_oblood",
    "bottle_glue": "glue_bottle",
    "bottle_ink": "ink_bottle",
    "bottle_slime_pink": "pink_slime_bottle",
    "bottle_slime_blue": "blue_slime_bottle",
    "bottle_loot": "loot_bottle",
}

# GT6 MultiItem entries whose port id differs but that are *not* items of their own: GT6 hands them
# out through other mods' items too, so they map onto 1.20.1 vanilla instead.
TECH_VANILLA = {
    "dye_cocoa": "cocoa_beans",  # IL.Dye_Cocoa -> minecraft:cocoa_beans
}

# Tables the port cannot deliver yet (their delivery item is not ported). Empty since §25 ported GT6's
# Loot Bottle (IL.Bottle_Loot), which is what rolls gt.bottles.
SKIPPED_TABLES: dict[str, str] = {}

UNMAPPED = {
    "food_can_": "canned food (GT6 passes a variant meta the port names per food)",
    "tool_matchbox": "matchbox (the port names its lighters differently)",
    "tool_lighter": "lighter variants (the port names its lighters differently)",
    "pill_": "pills (the port registers blue_pill/red_pill)",
    "chemtube:": "chem tube (the port registers no chemtube items)",
    "paper_magic_research": "another mod's item (Thaumcraft research notes)",
    "etfu_": "another mod's item (Extra Food Utilities)",
    "gasu_": "another mod's item (Galacticraft/GaSu plants)",
    "bop_": "another mod's item (Biomes O' Plenty)",
}

# Material prefixes GT6 loots that the port does not register as items (crates are blocks there).
UNMAPPED_PREFIXES = {
    "crategt": "crate blocks (the port registers crates as blocks, not as material items)",
}


def spec_for(expression: str) -> tuple[str | None, str]:
    """Port spec for a GT6 stack expression, or (None, reason) when it cannot be carried over."""
    text = " ".join(expression.split())
    if "ST.make(MD." in text or "MD." in text and "ST.make(MD." in text:
        return None, "another mod's item"
    book = BOOK.search(text)
    if book:
        # GTBooks resolves literal and material-generated books by the same mapping.
        return f"book:{book.group(1)}", ""
    if "ST.book(" in text:
        return None, "book built from a variable mapping"
    if ".fill(" in text:
        return None, "fluid-filled bottle"
    if "ST.array(" in text or "," in text.split("(")[0]:
        return None, "stack array"
    match = MAT.search(text)
    if match:
        material = match.group(2)
        if match.group(1) in ("chemtube",):
            return None, UNMAPPED["chemtube:"]
        for prefix, reason in UNMAPPED_PREFIXES.items():
            if match.group(1).lower().startswith(prefix):
                return None, reason
        # ANY.X / MT.X spell the same material; the port resolves both through its registry.
        # A bare variable (GT6 loops over every material with a flag, e.g.
        # ``OP.gem.mat(tMaterial, 1)`` for the RANDOM_SMALL_GEM_ORE family) cannot be carried over
        # without expanding the loop against GT6's material flags, so those rows are skipped.
        return f"i:{match.group(1)}:{material}:1", ""
    if re.search(r"OP\.\w+\s*\.\s*(?:mat|dat)\(\s*\w+\s*,", text):
        return None, "per-material loop (GT6 expands it over a material flag)"
    match = ITEM.search(text)
    if match:
        name = match.group(1).lower()
        for prefix, reason in UNMAPPED.items():
            if name.startswith(prefix.replace(":", "")):
                return None, reason
        if name in TECH_VANILLA:
            return f"v:{TECH_VANILLA[name]}:1", ""
        if name in ALIASES:
            name = ALIASES[name]
        return f"tech:{name}:{match.group(2)}", ""
    if "chemtube:" in text:
        return None, UNMAPPED["chemtube:"]
    if "MultiTileEntityCoin" in text or "COIN_MAP" in text:
        return None, "GT coins (the port registers no coin items)"
    match = VANILLA.search(text)
    if match and "ST.make(MD." not in text:
        block, meta = match.group(2).lower(), int(match.group(4))
        if (block, meta) in VANILLA_META:
            return f"v:{VANILLA_META[(block, meta)]}:1", ""
        name = VANILLA_RENAMES.get(block, block)
        return f"v:{name.lower()}:1", ""
    return None, "unmapped expression"


def main() -> None:
    text = SOURCE.read_text(encoding="utf-8", errors="replace")
    rows = []
    skipped = []
    found = [(table, weight, low, high, stack) for table, weight, low, high, stack in ROW.findall(text)]
    found += [(f"van.{const}", weight, low, high, stack)
              for const, weight, low, high, stack in VANILLA_ROW.findall(text)]
    for table, weight, low, high, stack in found:
        if table in SKIPPED_TABLES:
            skipped.append({"table": table, "stack": " ".join(stack.split())[:90],
                            "reason": SKIPPED_TABLES[table]})
            continue
        spec, reason = spec_for(stack)
        if spec is None:
            skipped.append({"table": table, "stack": " ".join(stack.split())[:90], "reason": reason})
            continue
        rows.append({"table": table, "weight": int(weight), "min": int(low), "max": int(high),
                     "spec": spec})

    tables: dict[str, list[dict]] = {}
    for row in rows:
        tables.setdefault(row["table"], []).append(row)

    # Roll counts: ChestGenHooks.getInfo("<table>").setMin(N) / .setMax(M) per table.
    bounds: dict[str, dict[str, int]] = {}
    for name, kind, value in COUNT.findall(text):
        bounds.setdefault(name, {})[kind] = int(value)
    counts: dict[str, tuple[int, int]] = {
        name: (values["Min"], values["Max"])
        for name, values in bounds.items() if "Min" in values and "Max" in values}

    lines = [
        "package com.gregtech.gregtech.loaders.c;",
        "",
        "/**",
        " * GT6's loot rows, transpiled from {@code Loader_Loot} by",
        " * {@code tools/transpile_gt6_loot.py}: {@code table|weight|min|max|spec}. The specs use the",
        " * same grammar as the chemistry loaders ({@code i:<prefix>:<material>:<count>},",
        " * {@code tech:<item>:<count>}, {@code v:<vanilla item>:<count>}), resolved at load time by",
        " * {@code GTGeneratedChem.resolveSpec}.",
        " */",
        "final class GTLootGen {".replace("final class", "public final class"),
        "    private GTLootGen() {}",
        "",
        "    /** The transpiled rows, as {@code table|weight|min|max|spec}. */",
        "    public static String[] rows() { return ROWS; }",
        "",
        "    /**",
        "     * How many items a roll of a GT6 table hands out, as {@code table|min|max}",
        "     * ({@code ChestGenHooks.getInfo(\"gt.books\").setMin(8).setMax(24)}, {@code Loader_Loot:337-339});",
        "     * GT6's configurable loot chest fills its slots with one roll per drawn item.",
        "     */",
        "    public static String[] tableCounts() { return TABLE_COUNTS; }",
        "",
        "    static final String[] TABLE_COUNTS = {",
    ]
    for table, (low, high) in sorted(counts.items()):
        lines.append(f'        "{table}|{low}|{high}",')
    lines += [
        "    };",
        "",
        "    static final String[] ROWS = {",
    ]
    for row in rows:
        lines.append(f'        "{row["table"]}|{row["weight"]}|{row["min"]}|{row["max"]}|{row["spec"]}",')
    lines += ["    };", "}", ""]
    OUT_JAVA.write_text("\n".join(lines), encoding="utf-8")

    OUT_JSON.write_text(json.dumps(
        {"rows": len(rows), "skipped": len(skipped), "perTable": {k: len(v) for k, v in tables.items()},
         "skippedRows": skipped,
         "vanillaRows": [r for r in rows if r["table"].startswith("van.")]},
        indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"{len(rows)} rows transpiled, {len(skipped)} skipped")
    for table, table_rows in sorted(tables.items()):
        print(f"  {table:14} {len(table_rows):4} rows")
    print("wrote", OUT_JAVA, "and", OUT_JSON)


if __name__ == "__main__":
    main()
