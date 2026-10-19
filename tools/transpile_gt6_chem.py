#!/usr/bin/env python3
"""Transpile GT6 Loader_Recipes_* RM.*.addRecipe* calls into port data.

Usage: python tools/transpile_gt6_chem.py [set]

Each set generates one class under content/recipe/generated/: one r(...) call per translatable
recipe with string ingredient specs resolved at runtime by GTGeneratedChem
(missing materials or fluids skip the recipe defensively). Untranslatable
recipes (cross-mod items, loop variables, ST.make blocks) are skipped and
counted.

Sets: chem (default), other, potions, food, extruder.

Spec grammar:
  items:  omd:<Mat>:<mU>     OM.dust(material, mU/1000 * U)
          i:<prefix>:<Mat>:<count>
          tag:<N>            selector tag (catalyst)
  fluids: m:<Mat>:<mB>       material gas/liquid/molten fluid
          f:<FLKey>:<mB>     named FL fluid
          w:<mB>             water
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import os
import re
import sys

GT6 = r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java"
ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

# set name -> (GT6 source file, generated class, doc description)
SOURCES = {
    "chem": ("Loader_Recipes_Chem.java", "GTChemGen",
             "GT6 chemistry recipes transpiled from {@code Loader_Recipes_Chem}"),
    "other": ("Loader_Recipes_Other.java", "GTOtherGen",
              "GT6 machine recipes transpiled from {@code Loader_Recipes_Other}"),
    "potions": ("Loader_Recipes_Potions.java", "GTPotionGen",
                "GT6 potion recipes transpiled from {@code Loader_Recipes_Potions}"),
    "food": ("Loader_Recipes_Food.java", "GTFoodGen",
             "GT6 food recipes transpiled from {@code Loader_Recipes_Food}"),
    "extruder": ("Loader_Recipes_Extruder.java", "GTExtruderGen",
                 "GT6 extruder recipes transpiled from {@code Loader_Recipes_Extruder}"),
    "vanilla": ("Loader_Recipes_Vanilla.java", "GTVanillaGen",
                "GT6 machine recipes for vanilla items transpiled from {@code Loader_Recipes_Vanilla}"
                        + " (the file's crafting-table {@code CR.*} calls stay with the hand-written"
                        + " vanilla recipe sets)"),
    "temporary": ("Loader_Recipes_Temporary.java", "GTTemporaryGen",
                  "GT6 recipes transpiled from {@code Loader_Recipes_Temporary}"),
    "ores": ("Loader_Recipes_Ores.java", "GTOresGen",
             "GT6 ore processing recipes transpiled from {@code Loader_Recipes_Ores} (the port's own"
                     + " ore chains are registered earlier and win every collision)"),
    # Not transpilable as-is: Loader_Recipes_OreDict / _Crops register every recipe through GT6's
    # ore-dictionary event callback (aEvent.mStack), and Loader_Recipes_Woods is driven by GT6's
    # BlocksGT log blocks plus an entry table. They need the port to gain an equivalent event layer
    # first, so they are deliberately not generated here.
}

SET = sys.argv[1] if len(sys.argv) > 1 else "chem"
if SET not in SOURCES:
    raise SystemExit("unknown set %r (choose from %s)" % (SET, ", ".join(SOURCES)))
SOURCE_FILE, CLASS_NAME, DOC = SOURCES[SET]
SRC = os.path.join(GT6, "gregtech", "loaders", "c", SOURCE_FILE)
OUT = os.path.join(str(_NativePath(__file__).resolve().parents[1] / "core" / "src"), "main", "java", "com", "gregtech", "gregtech", "content", "recipe", "generated",
                   CLASS_NAME + ".java")

# RM map fields available in the port (others are skipped).
PORT_MAPS = {
    "Anvil", "AnvilBendBig", "AnvilBendSmall", "Assembler", "Autoclave", "Autocrafter",
    "Bath", "BlastFurnace", "Boxinator", "Bumblelyzer", "BurnMixer", "CNC", "Calciner",
    "Canner", "CatalyticCracking", "Centrifuge", "Chisel", "ClusterMill", "Coagulator",
    "CokeOven", "Compressor", "Cooking", "Crusher", "CryoDistillationTower", "CryoMixer",
    "CrystallisationCrucible", "Cutter", "DistillationTower", "Distillery", "Drying",
    "Electrolyzer", "Extruder", "Fermenter", "Freezer", "Fusion", "Generifier", "Hammer",
    "ImplosionCompressor", "Injector", "Juicer", "Laminator", "LaserEngraver", "Lathe",
    "Lightning", "Loom", "MagneticSeparator", "Massfab", "Melter", "Microwave", "Mixer",
    "Mortar", "Nanofab", "Plantalyzer", "Polarizer", "Press", "PressureWasher", "Printer",
    "Replicator", "Roasting", "RollBender", "RollFormer", "RollingMill", "ScannerMolecular",
    "ScannerVisuals", "Sharpening", "Shredder", "Sifting", "Slicer", "Sluice", "Smelter",
    "Squeezer", "SteamCracking", "Unboxinator", "VacuumFreezer", "Welder", "Wiremill",
    "Furnace", "HeatMixer",
}
# Maps the port fills from a dedicated, value-vetted loader instead: transpiling GT6's other
# call sites for them would register a second, slightly different copy of the same reaction
# (the fusion reactions are asserted value-by-value in FusionRegressionTests).
PORT_MAPS -= {"Fusion"}

def _port_prefixes():
    """Item prefixes the port actually registers, read from MaterialPrefix.java.

    GT6 recipe calls name prefixes such as chunkGt / bouleGt / plateCurved / toolHeadRawSword;
    only the ones this port has an item form for can be translated, so the list is derived from
    the port instead of being maintained by hand.
    """
    path = os.path.join(ROOT, str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "data",
                        "MaterialPrefix.java")
    try:
        text = open(path, encoding="utf-8").read()
    except OSError:
        return set()
    return set(re.findall(r"public static final MaterialPrefix (\w+);", text))


KNOWN_PREFIXES = _port_prefixes() | {
    "dust", "dustSmall", "dustTiny", "dustDiv72", "dustImpure", "gem", "gemChipped",
    "gemFlawed", "gemFlawless", "gemExquisite", "gemLegendary", "ingot", "nugget", "plate",
    "plateTiny", "plateCurved", "plateDouble", "plateTriple", "plateDense", "stick", "stickLong",
    "screw", "gearGt", "gearGtSmall", "ring", "foil", "oreRaw", "crushed", "crushedTiny",
    "crushedPurified", "crushedPurifiedTiny", "crushedCentrifuged", "crushedCentrifugedTiny",
    "rockGt", "itemCasing", "rotor", "spring", "plateGem", "plateGemTiny", "wireFine", "round",
    "bolt", "unit", "chunkGt", "bouleGt", "billet",
    # GT6 block-sized prefixes this port has no item for; the spec is still emitted so the runtime
    # reports it as accepted-missing content instead of silently dropping the recipe.
    "blockDust", "blockIngot", "blockGem", "rawOreChunk"}

# GT6 item names (IL.*) that the port registers under a known id. Compound_<Material> is GT6's
# unmelted alloy mixture, with one ninth of a material unit per item. Shape items retain
# GT6's `Plate_Curved`/`Gear_Small` word order normalised to the port's `curvedplate`/`smallgear`.
IL_ITEM_PREFIXES = {
    "Compound_": "tech:",
}

SHAPE_WORDS = {
    "plate_curved": "curvedplate",
    "rod_long": "longrod",
    "gear_small": "smallgear",
    "tiny_plate": "tinyplate",
    "shovel": "shovelhead",
    "sword": "swordblade",
    "pickaxe": "pickaxehead",
    "axe": "axehead",
    "hoe": "hoehead",
    "hammer": "hammerhead",
    "file": "filehead",
    "saw": "sawblade",
    "tinypipe": "tinypipe",
    "smallpipe": "smallpipe",
    "mediumpipe": "mediumpipe",
    "largepipe": "largepipe",
    "hugepipe": "hugepipe",
    "capsulecell": "capsulecellcontainer",
}


def _shape_id(gt_prefix, port_prefix, name):
    """IL.Shape_Extruder_Plate_Curved -> extruder_shape_curvedplate."""
    words = name[len(gt_prefix):].lower().replace("_", "")
    for gt_name, port_name in SHAPE_WORDS.items():
        if words == gt_name.replace("_", ""):
            return port_prefix + port_name
    return port_prefix + words


IL_SHAPE_PREFIXES = {
    "Shape_Extruder_": "extruder_shape_",
    "Shape_SimpleEx_": "low_heat_extruder_shape_",
    "Shape_Slicer_": "slicer_shape_",
    "Shape_Press_": "shape_press_",
}


def il_item_spec(arg):
    """Translate an IL.<Name>.get(<count>) reference into a port item spec, or None."""
    m = re.match(r"IL\.(\w+)\s*\.\s*get\s*\(\s*(\d+)\s*\)", arg)
    if not m:
        return None
    name, count = m.group(1), int(m.group(2))
    for prefix, spec in IL_ITEM_PREFIXES.items():
        if name.startswith(prefix):
            material = re.sub(r'(?!^)([A-Z])', r'_\1', name[len(prefix):]).lower()
            return [f"tech:{material}_compound:{max(1, count)}"]
    for gt_prefix, port_prefix in IL_SHAPE_PREFIXES.items():
        if name.startswith(gt_prefix):
            return [f"tech:{_shape_id(gt_prefix, port_prefix, name)}:{max(1, count)}"]
    # Port-registered technological items keep their registry id in lower case.
    return [f"tech:{name.lower()}:{max(1, count)}"]


def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return "\n".join(line.split("//")[0] for line in text.splitlines())


def balanced(text, start):
    i = text.index("(", start)
    depth = 0
    for j in range(i, len(text)):
        if text[j] == "(":
            depth += 1
        elif text[j] == ")":
            depth -= 1
            if depth == 0:
                return text[i + 1:j]
    raise ValueError


def split_args(body):
    out, depth, cur = [], 0, []
    for ch in body:
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            depth -= 1
        if ch == "," and depth == 0:
            out.append("".join(cur).strip())
            cur = []
        else:
            cur.append(ch)
    if cur:
        out.append("".join(cur).strip())
    return [a for a in out if a]


U_TOKEN = re.compile(r"\bU(\d+)?\b")

# GT6 balance constants referenced inside recipe expressions (gregapi/data/CS.java).
GT_CONSTANTS = {"STEAM_PER_WATER": 160, "STEAM_PER_EU": 2, "EU_PER_LAVA": 80}


def _substitute_constants(expr: str) -> str:
    for name, value in GT_CONSTANTS.items():
        expr = re.sub(r"\b" + name + r"\b", str(value), expr)
    return expr


def eval_u(expr: str):
    """Evaluate a U-amount expression into mU (U=1000)."""
    expr = _substitute_constants(expr.strip())
    if not expr:
        return None
    def repl(m):
        n = m.group(1)
        return str(1000 if not n else max(1, round(1000 / int(n))))
    expr2 = U_TOKEN.sub(repl, expr)
    if not re.fullmatch(r"[0-9+\-*/() ]+", expr2):
        return None
    try:
        return int(eval(expr2))
    except Exception:
        return None


# GT6 CS.java:129 — L is the fluid amount of one material unit (144 mB), so fluid amounts are
# written as L, L/2, L*3 in the original loaders.
MB_CONSTANTS = {"L": 144}


def eval_mb(expr: str):
    """Evaluate a fluid amount expression in mB (GT6 L = 144)."""
    expr = expr.strip()
    if not expr:
        return None
    for name, value in MB_CONSTANTS.items():
        expr = re.sub(r"\b" + name + r"\b", str(value), expr)
    if not re.fullmatch(r"[0-9+\-*/() ]+", expr):
        return None
    try:
        value = int(eval(expr))
    except Exception:
        return None
    return value if value > 0 else None


MT_NAME = re.compile(r"MT\.(?:OREMATS\.|STONES\.|WOODS\.)?(\w+)$")


def mt(expr):
    m = MT_NAME.match(expr.strip())
    return m.group(1) if m else None


# ── 1.7.10 vanilla names ───────────────────────────────────────────────────
# GT6 writes 1.7.10 registry names; several were renamed in 1.20.1. Items and Blocks need separate
# tables because the same word is an item and a block with different modern ids
# (Items.netherbrick -> nether_brick, Blocks.nether_brick -> nether_bricks).
ITEM_RENAMES = {
    "speckled_melon": "glistering_melon_slice",
    "fish": "cod",
    "reeds": "sugar_cane",
    "netherbrick": "nether_brick",
    "skull": "skeleton_skull",
    "dye_powder": "white_dye",
}
BLOCK_RENAMES = {
    "stonebrick": "stone_bricks",
    "nether_brick": "nether_bricks",
    "netherbrick": "nether_bricks",
    "brick_block": "bricks",
    "grass": "grass_block",
    "waterlily": "lily_pad",
    "double_plant": "sunflower",
    "red_flower": "poppy",
    "yellow_flower": "dandelion",
    "hardened_clay": "terracotta",
    "stained_hardened_clay": "white_terracotta",
    "melon_block": "melon",
    "web": "cobweb",
    "wooden_slab": "oak_slab",
}

# Variant-carrying names: 1.7.10 encodes the colour/wood in the damage value. A numeric meta maps
# to one modern id; GT6's W (32767, "any variant") fans the whole recipe out over every variant,
# which is what the original accepts.
DYE_META = ["black", "red", "green", "brown", "blue", "purple", "cyan", "light_gray", "gray",
            "pink", "lime", "yellow", "light_blue", "magenta", "orange", "white"]
BLOCK_META = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
              "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]
WOOD_META = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak"]
FLOWER_META = ["poppy", "blue_orchid", "allium", "houstonia", "red_tulip", "orange_tulip",
               "white_tulip", "pink_tulip", "oxeye_daisy"]
PLANT2_META = ["sunflower", "syringa", "tall_grass", "large_fern", "rose_bush", "peony"]
# 1.7.10 stone-slab damage order: stone, sandstone, wooden(oak), cobblestone, brick, stone brick,
# nether brick, quartz. A double slab is the full block of the same material.
STONE_SLAB_META = ["stone_slab", "sandstone_slab", "oak_slab", "cobblestone_slab",
                   "brick_slab", "stone_brick_slab", "nether_brick_slab", "quartz_slab"]
DOUBLE_SLAB_META = ["smooth_stone", "sandstone", "oak_planks", "cobblestone",
                    "bricks", "stone_bricks", "nether_bricks", "quartz_block"]
STONE_META = ["stone", "granite", "polished_granite", "diorite", "polished_diorite", "andesite",
              "polished_andesite"]
FISH_META = ["cod", "salmon", "tropical_fish", "pufferfish"]

VANILLA_VARIANTS = {
    ("items", "dye"): [c + "_dye" for c in DYE_META],
    ("items", "coal"): ["coal", "charcoal"],
    ("items", "fish"): FISH_META,
    ("blocks", "wool"): [c + "_wool" for c in BLOCK_META],
    ("blocks", "carpet"): [c + "_carpet" for c in BLOCK_META],
    ("blocks", "stained_glass"): [c + "_stained_glass" for c in BLOCK_META],
    ("blocks", "stained_glass_pane"): [c + "_stained_glass_pane" for c in BLOCK_META],
    ("blocks", "stained_hardened_clay"): [c + "_terracotta" for c in BLOCK_META],
    ("blocks", "planks"): [w + "_planks" for w in WOOD_META],
    ("blocks", "log"): [w + "_log" for w in WOOD_META],
    ("blocks", "log2"): [WOOD_META[4] + "_log", WOOD_META[5] + "_log"],
    ("blocks", "sapling"): [w + "_sapling" for w in WOOD_META],
    ("blocks", "leaves"): [w + "_leaves" for w in WOOD_META],
    # log2/leaves2 only carry the two "new" wood types in 1.7.10.
    ("blocks", "leaves2"): [WOOD_META[4] + "_leaves", WOOD_META[5] + "_leaves"],
    ("blocks", "wooden_slab"): [w + "_slab" for w in WOOD_META],
    ("blocks", "red_flower"): FLOWER_META,
    ("blocks", "double_plant"): PLANT2_META,
    ("blocks", "stone_slab"): STONE_SLAB_META,
    ("blocks", "double_stone_slab"): DOUBLE_SLAB_META,
    ("blocks", "stone"): STONE_META,
}

# GT6 W: "any damage value" (CS.W). Recipes using it are fanned out over all variants below.
WILDCARD_META = 32767
# Fan-out ceiling: a recipe touching a wildcard variant of several names at once could explode.
MAX_VARIANT_FANOUT = 32


def vanilla_specs(kind, name, count, meta):
    """Modern item spec(s) for a 1.7.10 vanilla reference.

    Returns a list with one spec for a concrete item or an ``any:`` marker when GT6's wildcard
    damage value selects every variant; the caller fans the whole recipe out per variant.
    """
    variants = VANILLA_VARIANTS.get((kind, name.lower()))
    if variants:
        if meta == WILDCARD_META:
            return [f"any:{kind}:{name.lower()}:{count}"]
        index = meta if kind == "items" and name.lower() == "dye" else meta
        if 0 <= index < len(variants):
            return [f"v:{variants[index]}:{count}"]
        return None
    renamed = (ITEM_RENAMES if kind == "items" else BLOCK_RENAMES).get(name.lower(), name.lower())
    return [f"v:{renamed}:{count}"]


def expand_variants(call):
    """Fan a parsed call out over every ``any:`` wildcard variant it contains.

    GT6's W damage value means "any variant" (any wool colour, any dye, any plank wood …). A
    machine recipe has one slot per input, so instead of one recipe accepting sixteen dyes this
    emits one recipe per variant — the same set of accepted inputs the original had.
    """
    any_specs = [(i, spec) for i, spec in enumerate(call[4]) if spec.startswith("any:")]
    if not any_specs:
        return [call]
    choices = []
    for _, spec in any_specs:
        _, kind, name, _count = spec.split(":")
        variants = VANILLA_VARIANTS.get((kind, name))
        if not variants:
            return [call]
        choices.append(variants)
    total = 1
    for variants in choices:
        total *= len(variants)
    if total > MAX_VARIANT_FANOUT:
        # Too many combinations to be useful: keep the first variant of each instead of exploding.
        choices = [variants[:1] for variants in choices]
    import itertools
    expanded = []
    for combo in itertools.product(*choices):
        item_in = list(call[4])
        for (index, spec), variant in zip(any_specs, combo):
            _, _, _, count = spec.split(":")
            item_in[index] = f"v:{variant}:{count}"
        expanded.append(call[:4] + (item_in,) + call[5:])
    return expanded


def parse_item(arg):
    arg = arg.strip()
    if arg in ("ZL_IS", "NI"):
        return []
    # GT6 catalyst arrays: ST.array(a, b, c) is several inputs, ST.amount(n, x) repeats x n times
    # (a count of 0 marks GT6's "catalyst" inputs, which is why the 0 is preserved).
    if arg.startswith("ST.array"):
        out = []
        for sub in split_args(balanced(arg, 0)):
            r = parse_item(sub)
            if r is None:
                return None
            out.extend(r)
        return out
    m = re.match(r"ST\.amount\s*\(\s*(\d+)\s*,\s*(.+)\)$", arg)
    if m:
        inner = parse_item(m.group(2))
        if inner is None:
            return None
        count = int(m.group(1))
        if count == 0:
            # Catalyst input: keep the marker visible for GTGeneratedChem's withCatalystInputs.
            return [re.sub(r":(\d+)$", ":0", spec) for spec in inner]
        out = []
        for spec in inner:
            if spec.startswith("i:"):
                parts = spec.split(":")
                parts[3] = str(int(parts[3]) * count)
                out.append(":".join(parts))
            else:
                out.append(spec)
        return out
    # IL.<Name>.get(<count>) — GT6 technological items.
    il = il_item_spec(arg)
    if il is not None:
        return il
    # GT6 aligns its calls, so "OP.dust .mat(MT.X, 1)" and "MT.KOH , 6" are common.
    m = re.match(r"OM\.dust(?:OrIngot)?\s*\(", arg)
    if m:
        inner = split_args(balanced(arg, 0))
        name = mt(inner[0])
        if name is None:
            return None
        mu = 1000 if len(inner) == 1 else eval_u(inner[1])
        if mu is None or mu <= 0:
            return None
        return [f"omd:{name}:{mu}"]
    m = re.match(r"(?:OP\.)?(\w+)\s*\.\s*mat\s*\(", arg)
    if m:
        prefix = m.group(1)
        if prefix not in KNOWN_PREFIXES:
            return None
        inner = split_args(balanced(arg, 0))
        name = mt(inner[0])
        if name is None:
            return None
        try:
            count = int(inner[1])
        except Exception:
            return None
        return [f"i:{prefix}:{name}:{count}"]
    m = re.match(r"ST\.tag\s*\(\s*(\d+)\s*\)$", arg)
    if m:
        return [f"tag:{m.group(1)}"]
    # Vanilla items/blocks: ST.make(Items.X, count, meta) — 1.7.10 field names are translated to
    # modern registry paths, including the colour/wood variants hidden in the damage value.
    m = re.match(r"ST\.make\s*\(\s*(Items|Blocks)\.(\w+)\s*,\s*(\d+)\s*,\s*([W0-9]+)\s*\)$", arg)
    if m:
        kind = "items" if m.group(1) == "Items" else "blocks"
        meta = WILDCARD_META if m.group(4) == "W" else int(m.group(4))
        return vanilla_specs(kind, m.group(2), int(m.group(3)), meta)
    return None



# ── Family-loop unrolling ──────────────────────────────────────────────────
# GT6 registers many recipes inside loops over fluid/material families
# (FL.waters, FluidsGT.OXYGEN/AIR, ANY.X.mToThis). We substitute the loop
# variable with one canonical representative so the body parses normally.

FLUIDS_GT = {"OXYGEN": "Oxygen", "AIR": "Air"}


def _block_span(text, pos):
    """Span of the loop body starting at pos (skips an optional if(...) guard)."""
    i = pos
    while True:
        while i < len(text) and text[i] in " \t\r\n":
            i += 1
        if text.startswith("if", i):
            i = text.index("(", i)
            depth = 0
            while i < len(text):
                if text[i] == "(":
                    depth += 1
                elif text[i] == ")":
                    depth -= 1
                    if depth == 0:
                        i += 1
                        break
                i += 1
            continue
        break
    if i < len(text) and text[i] == "{":
        depth = 0
        for j in range(i, len(text)):
            if text[j] == "{":
                depth += 1
            elif text[j] == "}":
                depth -= 1
                if depth == 0:
                    return i + 1, j
        return None
    # braceless: a single statement up to the first ';' at paren depth 0
    depth = 0
    for j in range(i, len(text)):
        if text[j] in "([":
            depth += 1
        elif text[j] in ")]":
            depth -= 1
        elif text[j] == ";" and depth == 0:
            return i, j + 1
    return None


LOOP_RE = re.compile(
    r"for\s*\(\s*(?:FluidStack|String|OreDictMaterial|ItemStack)\s+(\w+)\s*:\s*")


def _for_iterable(text, start):
    """Span of the iterable expression after `for (Type var : `.

    The iterable can contain parentheses (`ST.array(OP.dust.mat(MT.X, 1), …)`), so the end is
    found by balancing brackets rather than with a regex.
    """
    depth = 0
    i = start
    while i < len(text):
        ch = text[i]
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            if ch == ")" and depth == 0:
                return start, i
            depth -= 1
        i += 1
    return None


def _inline_locals(body):
    """Inline `ItemStack tName = OM.dust(...);` locals within a loop body."""
    for m in list(re.finditer(r"ItemStack\s+(\w+)\s*=\s*(OM\.dust\w*\([^;]+?\))\s*;", body)):
        name, expr = m.group(1), m.group(2)
        body = body[:m.start()] + " " * (m.end() - m.start()) + body[m.end():]
        body = re.sub(r"\b" + re.escape(name) + r"\b", expr, body)
    return body


def unroll_loops(text):
    changed = True
    while changed:
        changed = False
        for m in reversed(list(LOOP_RE.finditer(text))):
            var = m.group(1)
            iterable_span = _for_iterable(text, m.end())
            if iterable_span is None:
                continue
            iterable = text[iterable_span[0]:iterable_span[1]].strip()
            span = _block_span(text, iterable_span[1] + 1)
            if span is None:
                continue
            b0, b1 = span
            body = text[b0:b1]
            wm = re.match(r"FL\.waters\s*\(\s*(\d+)\s*\)", iterable)
            am = re.match(r"FluidsGT\.(\w+)", iterable)
            mm = re.match(r"ANY\.(\w+)\.mToThis", iterable)
            sm = re.match(r"ST\.array\s*\(", iterable)
            if sm:
                # for (ItemStack tStack : ST.array(a, b, c)) { … } — one recipe per array element,
                # with the loop variable substituted by that element.
                elements = split_args(balanced(iterable, 0))
                if not elements:
                    continue
                body = " ".join(re.sub(r"\b" + re.escape(var) + r"\b", lambda _m, el=el: el, body)
                                for el in elements)
            elif wm:
                n = int(wm.group(1))
                body = re.sub(r"FL\.mul\s*\(\s*" + var + r"\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*\w+\s*\)",
                              lambda x: f"FL.Water.make({max(1, n * int(x.group(1)) // int(x.group(2)))})", body)
                body = re.sub(r"FL\.mul\s*\(\s*" + var + r"\s*,\s*(\d+)\s*\)",
                              lambda x: f"FL.Water.make({n * int(x.group(1))})", body)
                body = re.sub(r"\b" + var + r"\b", f"FL.Water.make({n})", body)
            elif am and am.group(1) in FLUIDS_GT:
                key = FLUIDS_GT[am.group(1)]
                body = re.sub(r"FL\.make\s*\(\s*" + var + r"\s*,", f"FL.{key}.make(", body)
                body = re.sub(r"\b" + var + r"\b", f'"{key.lower()}"', body)
            elif mm:
                body = re.sub(r"\b" + var + r"\b", "MT." + mm.group(1), body)
            else:
                continue
            body = _inline_locals(body)
            new_text = text[:m.start()] + " " * (b0 - m.start()) + body + text[b1:]
            if new_text != text:
                text = new_text
                changed = True
    return text

def parse_fluid(arg):
    arg = arg.strip()
    if arg in ("NF", "ZL_FS"):
        return []
    if arg.startswith("FL.array"):
        out = []
        for sub in split_args(balanced(arg, 0)):
            r = parse_fluid(sub)
            if r is None:
                return None
            out.extend(r)
        return out
    m = re.match(r"MT\.((?:OREMATS\.|STONES\.)?\w+)\s*\.\s*(gas|liquid|fluid)\s*\(", arg)
    if m:
        name = m.group(1).split(".")[-1]
        inner = split_args(balanced(arg, 0))
        mu = eval_u(inner[0])
        if mu is None or mu <= 0:
            return None
        return [f"m:{name}:{mu}"]
    m = re.match(r"FL\.(\w+)\s*\.\s*make\s*\(\s*([0-9*+\- /L]+)\s*\)", arg)
    if m:
        key = m.group(1)
        mb = eval_mb(m.group(2))
        if mb is None:
            return None
        if key == "Water":
            # "w:" resolves to plain water; every other water variant keeps its own identity.
            return [f"w:{mb}"]
        return [f"f:{key}:{mb}"]
    m = re.match(r"FL\.mul\s*\(\s*tWater\s*,\s*(\d+)(?:\s*,\s*(\d+)\s*,\s*\w)?\s*\)", arg)
    if m:
        a = int(m.group(1))
        b = int(m.group(2)) if m.group(2) else 1
        return [f"w:{max(1, 250 * a // b)}"]
    if arg == "tWater":
        return ["w:250"]
    # String-named fluids: FL.make("registryname", n) — resolved by registry name at runtime.
    m = re.match(r"FL\.\s*make\s*\(\s*\"([\w. ]+)\"\s*,\s*([0-9*+\- ]+)\s*(?:,|\))", arg)
    if m:
        try:
            mb = int(eval(m.group(2)))
        except Exception:
            return None
        return [f"fr:{m.group(1)}:{mb}"]
    return None


def is_fluid_expr(arg):
    return (arg in ("NF", "ZL_FS", "tWater") or arg.startswith("FL.") or
            re.match(r"MT\.(?:OREMATS\.|STONES\.)?\w+\s*\.\s*(gas|liquid|fluid)\s*\(", arg) is not None)


def parse_call(map_name, body, item_inputs=1):
    """Parse one RM call.

    ``item_inputs`` is the GT6 overload's item-input arity (addRecipe1 = 1, addRecipe2 = 2, …):
    everything else that is an item is an output. Recipes that mix fluids and items always list
    the item inputs before the first fluid, so any item after a fluid is an output too.
    """
    args = split_args(body)
    if not args or args[0] not in ("T", "F"):
        return None
    idx = 1
    # GT6's raw overloads pass up to four more flags before the numbers:
    # addRecipeX(aOptimize, aCheckForCollisions, aFakeRecipe, aHidden, aLogErrors, EU/t, ticks, …).
    while idx < len(args) and args[idx] in ("T", "F"):
        idx += 1
    nums = []
    while idx < len(args) and re.fullmatch(r"[0-9*+\-/ ]+", args[idx]):
        try:
            nums.append(int(eval(args[idx])))
        except Exception:
            return None
        idx += 1
    if len(nums) != 2:
        return None
    eut, dur = nums
    chances = None
    if idx < len(args) and args[idx].startswith("new long[]"):
        ch = re.findall(r"\d+", args[idx])
        chances = [int(x) for x in ch]
        idx += 1

    item_in, fluid_groups, item_out = [], [], []
    seen_fluid = False
    item_args = 0
    for arg in args[idx:]:
        if is_fluid_expr(arg):
            fl = parse_fluid(arg)
            if fl is None:
                return None
            fluid_groups.append(fl)
            seen_fluid = True
        else:
            it = parse_item(arg)
            if it is None:
                return None
            item_args += 1
            if seen_fluid or item_args > item_inputs:
                item_out.extend(it)
            else:
                item_in.extend(it)
    if len(fluid_groups) > 2:
        # GT6 allows several output fluid arrays; the port spec keeps one input array and one
        # output array, so additional groups after the first are merged into the outputs.
        first_out = fluid_groups[1] if len(fluid_groups) > 1 else None
        merged = []
        for group in fluid_groups[1:]:
            merged.extend(group)
        fluid_in = fluid_groups[0] if fluid_groups else []
        fluid_out = merged
    else:
        fluid_in = fluid_groups[0] if fluid_groups else []
        fluid_out = fluid_groups[1] if len(fluid_groups) > 1 else []
    if not item_in and not fluid_in:
        return None
    if not item_out and not fluid_out:
        return None
    return (map_name, eut, dur, chances, item_in, fluid_in, fluid_out, item_out)


def main():
    text = unroll_loops(strip_comments(open(SRC, encoding="utf-8").read()))
    total, parsed_calls, ok = 0, 0, []
    for m in re.finditer(r"RM\.(\w+)\s*\.addRecipe([0-9X])\s*\(", text):
        total += 1
        map_name = m.group(1)
        if map_name not in PORT_MAPS:
            continue
        try:
            body = balanced(text, m.end() - 1)
        except ValueError:
            continue
        arity = 1 if m.group(2) == "X" else int(m.group(2))
        parsed = parse_call(map_name, body, arity)
        if parsed:
            parsed_calls += 1
            ok.extend(expand_variants(parsed))

    lines = []
    for map_name, eut, dur, chances, item_in, fluid_in, fluid_out, item_out in ok:
        ch = "null" if not chances else "ch(" + ", ".join(str(c) for c in chances) + ")"
        def arr(specs):
            return "sp(" + ", ".join(f'"{s}"' for s in specs) + ")" if specs else "NONE"
        lines.append(f'        r("{map_name}", {eut}, {dur}, {ch}, '
                     f'{arr(item_in)}, {arr(fluid_in)}, {arr(fluid_out)}, {arr(item_out)});')

    content = f"""package com.gregtech.gregtech.content.recipe.generated;

/**
 * {DOC}
 * ({parsed_calls} of {total} RM calls translated into {len(ok)} recipes; the rest
 * reference cross-mod content or untranslatable expressions).
 *
 * <p>GENERATED by tools/transpile_gt6_chem.py {SET} — do not edit by hand.
 * Spec resolution happens in {{@link GTGeneratedChem}}.</p>
 */
final class {CLASS_NAME} {{
    private static final String[] NONE = new String[0];

    private {CLASS_NAME}() {{}}

    private static String[] sp(String... specs) {{ return specs; }}

    private static long[] ch(long... chances) {{ return chances; }}

    private static void r(String map, long eut, long dur, long[] chances,
                          String[] itemIn, String[] fluidIn, String[] fluidOut, String[] itemOut) {{
        GeneratedRecipeSink.register(map, eut, dur, chances, itemIn, fluidIn, fluidOut, itemOut);
    }}

    static void load() {{
{chr(10).join(lines)}
    }}
}}
"""
    with open(OUT, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"[{SET}] translated {parsed_calls} / {total} RM calls -> {len(ok)} recipes "
          f"({os.path.basename(OUT)})")


if __name__ == "__main__":
    main()
