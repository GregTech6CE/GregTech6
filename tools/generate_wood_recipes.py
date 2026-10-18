"""Generate the port's wood-dictionary crafting rows from GT6's own wood table.

GT6 drives its wood recipes from two sources:

* ``gregapi/load/LoaderWoodDictionary.java`` (the task's ``gregapi/loaders/...``) builds the
  ``gregapi/wooddict`` table: ``new WoodEntry(log, new BeamEntry(beam, new PlankEntry(planks, slab,
  material, index), charcoal, creosote), charcoal, creosote)`` chains, with
  ``MT.WoodRubber``/``MT.WOODS.*`` as the plank material, plus the vanilla and other-mod entries.
* ``gregtech/loaders/c/Loader_Recipes_Woods.java`` (the task's ``gregtech/loaders/a/...``) iterates
  that table and registers the recipe rows: 128 statements in total, of which **71 are
  ``RM.<map>.addRecipe*(...)`` machine rows** (the "71 rows" of
  ``docs/PORTING_REMAINING_2026-09-14.md`` 搂2/搂14 - that count is the RM half; the other 57 are
  ``CR.shaped``/``CR.shapeless``/``CR.remove`` crafting rows).

This tool reads both files (plus the port's own wood sources) and writes
``src/main/java/com/gregtech/gregtech/content/recipe/GTWoodRecipes.java`` containing only the rows
the port can express, together with the unmappable rows and their GT6 line numbers.

Usage: python tools/generate_wood_recipes.py [--check]
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import argparse
import os
import re
import string
import sys

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
GT6 = os.path.normpath(os.path.join(ROOT, "..", "gregtech6-master", "gregtech6-master", str(native_path("src")), "main", "java"))

DICT_FILE = os.path.join(GT6, "gregapi", "load", "LoaderWoodDictionary.java")
ROWS_FILE = os.path.join(GT6, "gregtech", "loaders", "c", "Loader_Recipes_Woods.java")
MT_FILE = os.path.join(GT6, "gregapi", "data", "MT.java")
TD_FILE = os.path.join(GT6, "gregapi", "data", "TD.java")
CS_FILE = os.path.join(GT6, "gregapi", "data", "CS.java")
WOOD_ENTRY_FILE = os.path.join(GT6, "gregapi", "wooddict", "WoodEntry.java")
BEAM_ENTRY_FILE = os.path.join(GT6, "gregapi", "wooddict", "BeamEntry.java")
PLANK_ENTRY_FILE = os.path.join(GT6, "gregapi", "wooddict", "PlankEntry.java")

PORT = os.path.join(ROOT, str(native_path("src")), "main", "java", "com", "gregtech", "gregtech")
CORE = os.path.join(ROOT, "core", str(native_path("src")), "main", "java", "com", "gregtech", "gregtech")
SPECIES_FILE = os.path.join(CORE, "block", "wood", "WoodSpecies.java")
WOODS_FILE = os.path.join(PORT, "registry", "GTWoods.java")
MATERIALS_DIR = os.path.join(CORE, "content", "material", "generated")
MATERIAL_FORMS_FILE = os.path.join(CORE, "data", "generated", "MaterialForms.java")
OUT_FILE = os.path.join(CORE, "content", "recipe", "GTWoodRecipes.java")

# GT6's own trees, as the port's WoodSpecies enum spells them. The dictionary row
# (LoaderWoodDictionary) is verified against the material field below, so a GT6 rename fails loudly.
SPECIES = [
    # (GT6 log block, meta, port species id, expected GT6 plank material field)
    ("LogA", 0, "rubber", "WoodRubber"),
    ("LogA", 1, "maple", "Maple"),
    ("LogA", 2, "willow", "Willow"),
    ("LogA", 3, "blue_mahoe", "BlueMahoe"),
    ("LogB", 0, "hazel", "Hazel"),
    ("LogB", 1, "cinnamon", "Cinnamon"),
    ("LogB", 2, "coconut", "Coconut"),
    ("LogB", 3, "rainbowood", "Rainbowood"),
    ("LogC", 0, "bluespruce", "BlueSpruce"),
]

# Port species GT6's dictionary has no block of its own for: the port registers them because
# `MT.WOODS` knows the material, but GT6 only ever bound it to another mod's blocks (Forestry's pine,
# MineFantasy's ebony, ...) or never registered a dictionary row at all.
PORT_ONLY_SPECIES = [
    ("pine", "Pine"),
    ("ebony", "Ebony"),
    ("white_mahoe", "Mahoe"),
]

# The eight crafting rows of the dictionary loops, with the GT6 statement they come from. Each is
# verified to still be that statement before anything is written.
ROW_SPECS = [
    # (row id, GT6 line, signature that has to appear on that line, copy)
    ("log_to_sticks", 183, r'CR\.shaped\s*\(ST\.validMeta\(NERFED_WOOD\?aEntry\.mStickCountSaw',
     "saw + log + file -> long rods"),
    ("log_to_planks_saw", 184, r'CR\.shaped\s*\(ST\.validMeta\(NERFED_WOOD\?aEntry\.mPlankCountSaw',
     "saw + log -> planks"),
    ("log_to_planks_hand", 185, r'CR\.shapeless\s*\(ST\.validMeta\(NERFED_WOOD\?aEntry\.mPlankCountHand',
     "log -> planks by hand"),
    ("beam_to_sticks", 203, r'CR\.shaped\s*\(ST\.validMeta\(NERFED_WOOD\?aEntry\.mStickCountSaw',
     "saw + beam + file -> long rods"),
    ("beam_to_planks_saw", 204, r'CR\.shaped\s*\(ST\.validMeta\(NERFED_WOOD\?aEntry\.mPlankCountSaw',
     "saw + beam -> planks"),
    ("beam_to_planks_hand", 205, r'CR\.shapeless\s*\(ST\.validMeta\(NERFED_WOOD\?aEntry\.mPlankCountHand',
     "beam -> planks by hand"),
    ("planks_to_sticks_hand", 243, r'CR\.shaped\s*\(ST\.validMeta_\(\(NERFED_WOOD\?aEntry\.mStickCountHand',
     "2 planks -> 2 rods by hand"),
    ("planks_to_sticks_saw", 244, r'CR\.shaped\s*\(ST\.validMeta_\(\s*NERFED_WOOD\?aEntry\.mStickCountSaw',
     "saw + plank -> rods"),
]

# The rows whose result is a rod of the species material (`OP.stick`/`OP.stickLong`), which needs the
# port's STICKS form flag on that material - verified against the imported GT6 form flags below.
ROD_ROWS = ("log_to_sticks", "beam_to_sticks", "planks_to_sticks_hand", "planks_to_sticks_saw")

# The spec kinds `Loader_WoodCraftingRecipes.resolve` implements; a row with any other kind would be
# skipped at runtime, so the generator refuses to write it.
SPEC_KINDS = ("log", "planks", "beam", "rod", "rodlong", "tool")

# What each GT6 statement registers, straight from `Loader_Recipes_Woods`: the "sLf"/"sBf" rows of the
# wood and beam loops write `aEntry.mStick` of a WoodEntry/BeamEntry (= OP.stickLong), the "P"/"P" and
# "s"/"P" rows of the plank loop write the PlankEntry's OP.stick, and the other four produce planks.
# A row whose result kind disagrees with the line it cites is a wrong result mapping.
RESULT_KIND_BY_LINE = {
    183: "rodlong", 184: "planks", 185: "planks",
    203: "rodlong", 204: "planks", 205: "planks",
    243: "rod", 244: "rod",
}

# Reasons for the GT6 statements this batch does not turn into port rows. Every entry names the GT6
# class/line and the concrete port difference. The first pattern is matched against the statement text
# *and* against the loop/if header that contains it (GT6 gates a whole block on e.g. MD.FR.mLoaded).
# ``{button}`` is filled in after the port's imported form flags for MT.Wood have been read.
SKIP_REASONS = [
    (r'MD\.FR', r'(RM\.|CR\.)', "Forestry wood: the mod does not exist for 1.20.1"),
    (r'IL\.RC_|IL\.RC_|IL\.IE_|IL\.Treated|RC_Tie_Wood|RC_Creosote', r'(CR\.|RM\.)',
     "Railcraft/Immersive Engineering treated wood: neither mod exists for 1.20.1"),
    (r'BlocksGT\.\w*FireProof|tLog1|tPlank1|tStair1|tSlab1',
     r'RM\.(Laminator|Bath)\s*\.addRecipe',
     "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the "
     "port, so these rows have no output to produce"),
    (r'RM\.Squeezer', r'RM\.Squeezer',
     "Squeezer row (RM half): the latex/resin/sap chain is a machine row, not part of this crafting "
     "batch - the port has no Latex/Resin_Spruce/Sap_Maple/Sap_Rainbow recipe at all today"),
    (r'IL\.Crate|IL\.Crate_Fireproof', r'RM\.(unbox|box|boxunbox)',
     "crates: IL.Crate is GT6's crate item, which the port does not register"),
    (r'OD\.buttonWood', r'CR\.shaped', "{button}"),
    (r'OD\.plankWood|OD\.plankAnyWood', r'CR\.shaped',
     "OD.plankWood bowl: vanilla ships minecraft:bowl from minecraft:planks, which does not contain "
     "the port's plank blocks; GT6's own row targets its ore dictionary name instead"),
    (r'ANY\.WoodDefault|stickAnyWood', r'CR\.shaped',
     "OD.stickAnyWood: the port publishes per-material rod tags only (data/MaterialTagPack.java), so "
     "there is no umbrella wood-rod tag to key this row on"),
    (r'tMetaTool|ToolsGT\.CLUB', r'CR\.shaped',
     "club (ToolsGT.CLUB): the port registers no club tool"),
    (r'RM\.Injector|RM\.CNC', r'RM\.(Injector|CNC)',
     "RM-half machine row (Injector petrified wood / CNC gear): not part of this crafting batch"),
    (r'gearGt|casingSmall|plateTiny|round|ring|bolt|toolHead|ROLLING_PIN', r'CR\.shaped',
     "per-plank forms (gear/casing/tiny plate/round/ring/tool heads/rolling pin): both GT6's "
     "ONLY_IF_HAS_RESULT flag and the port skip them - MT.Wood carries no PARTS/TOOL_HEAD form"),
    (r'aEntry\.mStair', r'CR\.shaped',
     "stairs: the port registers no stairs block for its own wood species"),
    (r'aEntry\.mSlab', r'CR\.shaped',
     "slabs: the port registers no slab block for its own wood species"),
    (r'blockPlate|crateGtPlate|crateGt64Plate', r'RM\.',
     "crate/packed-plate rows: the port has no per-wood crate or packed plank block"),
    (r'IL\.Beam', r'RM\.generify',
     "generify to IL.Beam: the port has no generic beam item (its beams are per-species blocks)"),
    (r'aEntry\.mLog|aEntry\.mBeam|aEntry\.mPlank', r'RM\.',
     "RM-half machine row (pressure washer / pulverizer / sawing / lathing / coke oven): the port "
     "registers these for its own species in content/recipe/RegisteredWoodSurvivalRecipes.java"),
    (r'CR\.remove', r'CR\.remove',
     "CR.remove: removes the recipes competing with the rows above; the port has no competing recipe "
     "for its log/plank/beam blocks, so there is nothing to remove"),
]

FALLBACK_REASON = "not expressed by this batch (no port equivalent of this GT6 statement)"

FLAG_TEMPLATE = """\
package com.gregtech.gregtech.content.recipe;

/**
 * GENERATED by {@code tools/generate_wood_recipes.py} - do not edit by hand, rerun the tool.
 *
 * <p>The crafting-table half of GT6's wood dictionary recipes
 * ({@code gregtech/loaders/c/Loader_Recipes_Woods.java}, driven by
 * {@code gregapi/load/LoaderWoodDictionary.java} and {@code gregapi/wooddict/*}). GT6's file holds
 * @@row_count@@ recipe statements: <b>@@rm_count@@ {@code RM.*} machine rows</b> (the "71 rows" of
 * {@code docs/PORTING_REMAINING_2026-09-14.md} sections 2 and 14 count the {@code RM.*} statements) and
 * @@cr_count@@ {@code CR.shaped}/{@code CR.shapeless}/{@code CR.remove} crafting rows. The machine half is
 * already registered for the port's species by {@code content/recipe/RegisteredWoodSurvivalRecipes.java}
 * and {@code content/recipe/VanillaWoodProcessingRecipes.java}; the crafting half was missing and is
 * what this table carries.</p>
 *
 * <p>The rows iterate GT6's {@code WoodDictionary} entries. Only the entries backed by GT6's own nine
 * trees ({@code BlocksGT.LogA/LogB/LogC} metas 0-3/0-3/0) can be expressed: the port registers exactly
 * those species as {@code gt6ce:log_*} / {@code gt6ce:planks_*} / {@code gt6ce:beam_*} blocks
 * ({@code registry/GTWoods.java}, {@code block/wood/WoodSpecies.java}). Every row below is registered
 * with the numbers GT6's own table carries for that species, with {@code CS.NERFED_WOOD = true}
 * ({@code gregapi/data/CS.java:866} in the original), i.e. the hand/saw counts rather than the
 * buzz-saw/lathe ones.</p>
 *
 * <p>Port differences, all of them deliberate:</p>
 * <ul>
 *   <li>No fireproof wood: GT6's {@code BlocksGT.Log*FireProof}/{@code PlanksFireProof}/{@code Beam*FireProof}
 *       family (Laminator/Bath rows at {@code Loader_Recipes_Woods.java:46-119}) has no port block.</li>
 *   <li>No slabs or stairs for the port's species: GT6's {@code LIST_STAIRS}/{@code LIST_SLABS} rows
 *       ({@code :266-270}, {@code :288}, {@code :305}) are therefore skipped.</li>
 *   <li>Vanilla woods ({@code LoaderWoodDictionary.java:51-56}) keep vanilla's own recipes; the port's
 *       per-species log rows would only duplicate {@code minecraft:oak_planks} and friends.</li>
 *   <li>Other mods' wood (IC2/MFR/Atum/Fossil/BoP/TC/TF/BTL/Aether/Botania/Witchery/AbyssalCraft/
 *       Steamcraft/EBXL/Bamboo/Caveworld/TCFM/Forestry) is absent from 1.20.1.</li>
 *   <li>{@code MT.WOODS.Cinnamon} is the registered Cinnamonwood material (9317), separate from
 *       Cinnamon spice (9785). All eight dictionary rows are emitted for this species.</li>
 *   <li>The four fallen Log1 entries are registered separately by {@code NaturalWoodCraftingRows};
 *       this generated tree-species table does not duplicate those rows.</li>
 * </ul>
 *
 * <p>{@code CRAFT_ROWS} entries are {@code path|shape|mirror|pattern|keys|result|gt6}: pattern rows are
 * {@code /}-separated, keys are {@code symbol=spec} pairs separated by {@code ;}, and the result is
 * {@code spec*count}. Specs are {@code log:<species>}, {@code planks:<species>}, {@code beam:<species>},
 * {@code rod:<material>} ({@code OP.stick}), {@code rodlong:<material>} ({@code OP.stickLong}) and
 * {@code tool:<id>} (a {@code GTToolType} id). {@code Loader_WoodCraftingRecipes} resolves them and
 * registers the recipes; {@code gametest/WoodRecipeTests.java} pins the row count.</p>
 */
public final class GTWoodRecipes {
    private GTWoodRecipes() {}

    /** Rows GT6's dictionary produces for the port's species: {@code id|log|planks|beam|material|gt6}. */
    public static final String[] SPECIES = {
@@species@@
    };

    /** The generated crafting rows, in GT6's own order per species. */
    public static final String[] CRAFT_ROWS = {
@@rows@@
    };

    /** GT6 statements of {@code Loader_Recipes_Woods} this batch does not turn into port rows. */
    public static final String[] SKIPPED_ROWS = {
@@skipped@@
    };

    /**
     * Rows the eight dictionary statements would produce but this port cannot express, with the GT6
     * line and the reason: {@code <species>/<row>|<GT6 reference>|<reason>}. The row count plus the
     * length of this array is the full {@code 9 x 8} the dictionary has for the mapped species.
     */
    public static final String[] SKIPPED_CRAFT_ROWS = {
@@skipped_rows@@
    };

    /** GT6 wood-dictionary entries with no port species, with the GT6 line and the reason. */
    public static final String[] UNMAPPED_ENTRIES = {
@@unmapped@@
    };

    /** Number of crafting rows this table carries; the gametest pins this number. */
    public static int craftRowCount() { return CRAFT_ROWS.length; }

    /** Number of GT6 recipe statements the source file holds, for reports. */
    public static int gt6StatementCount() { return @@row_count@@; }

    /** Number of {@code RM.*} machine rows in GT6's file (the "71 rows"). */
    public static int gt6MachineRowCount() { return @@rm_count@@; }

    /** Number of {@code CR.*} crafting statements in GT6's file. */
    public static int gt6CraftingStatementCount() { return @@cr_count@@; }
}
"""


# 鈹€鈹€ Java source scanning 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

def read(path: str) -> str:
    with open(path, encoding="utf-8", errors="replace") as handle:
        return handle.read()


def strip_comments(text: str) -> str:
    """Blank out comments and literal bodies while keeping line structure (for statement scanning)."""
    out = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c == "/" and i + 1 < n and text[i + 1] == "/":
            while i < n and text[i] != "\n":
                out.append(" ")
                i += 1
            continue
        if c == "/" and i + 1 < n and text[i + 1] == "*":
            while i < n and not (text[i] == "*" and i + 1 < n and text[i + 1] == "/"):
                out.append("\n" if text[i] == "\n" else " ")
                i += 1
            out.append("  ")
            i += 2
            continue
        if c in "\"'":
            quote = c
            out.append(c)
            i += 1
            while i < n and text[i] != quote:
                if text[i] == "\\" and i + 1 < n:
                    out.append("  ")
                    i += 2
                    continue
                out.append("\n" if text[i] == "\n" else " ")
                i += 1
            if i < n:
                out.append(quote)
                i += 1
            continue
        out.append(c)
        i += 1
    return "".join(out)


def statements(text: str):
    """Yield (statement_text, first_non_blank_line) for every ``;``-terminated statement.

    Only parentheses nest for statement purposes: the wood table registers its entries inside
    ``if (...)`` blocks, so braces must not hide the terminating ``;``.
    """
    clean = strip_comments(text)
    line_of = [0] * (len(clean) + 1)
    line = 1
    for i, c in enumerate(clean):
        line_of[i] = line
        if c == "\n":
            line += 1
    line_of[len(clean)] = line

    depth = 0
    start = 0
    out = []
    for i, c in enumerate(clean):
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
        elif c == ";" and depth <= 0:
            out.append((clean[start:i + 1], start))
            start = i + 1
    tail = clean[start:]
    if tail.strip():
        out.append((tail, start))

    for body, offset in out:
        stripped = body.lstrip()
        first = offset + (len(body) - len(stripped))
        yield body, line_of[first]


def split_args(arglist: str):
    """Split a constructor argument list at top-level commas."""
    args = []
    depth = 0
    current = []
    for c in arglist:
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        if c == "," and depth == 0:
            args.append("".join(current).strip())
            current = []
            continue
        current.append(c)
    if "".join(current).strip():
        args.append("".join(current).strip())
    return args


STACK_RE = re.compile(r"ST\.make\(\s*(?P<block>.+?)\s*,\s*(?P<count>[^,]+?)\s*,\s*(?P<meta>[^,()]+?)\s*\)\s*$")


def stack_spec(expr: str):
    """``ST.make(<block>, <count>, <meta>)`` -> (block expr, meta) or None."""
    expr = expr.strip()
    match = STACK_RE.match(expr)
    if not match:
        return None
    block = match.group("block").strip()
    meta = match.group("meta").strip()
    if meta == "W":
        meta = "W"
    return block, meta


def block_key(block: str, meta: str):
    """Normalise a GT6 block expression to ``(qualified name, meta)``.

    ``BlocksGT.Planks`` (and the ``BlockMetaType.mSlabs`` accessor) stay distinguishable from
    ``Blocks.planks`` and from another mod's ``MD.*``/``IL.*`` items, which is what the mapping and the
    skip report need.
    """
    block = block.strip()
    slab = ".mSlabs" in block
    for name in ("LogAFireProof", "LogBFireProof", "LogCFireProof", "LogDFireProof",
                 "LogA", "LogB", "LogC", "LogD", "Log1",
                 "BeamAFireProof", "BeamBFireProof", "BeamCFireProof", "BeamDFireProof",
                 "Beam1FireProof", "Beam2FireProof", "Beam3FireProof",
                 "BeamA", "BeamB", "BeamC", "BeamD", "Beam1", "Beam2", "Beam3",
                 "PlanksFireProof", "Planks2FireProof", "Planks", "Planks2"):
        if re.search(r"\bBlocksGT\." + name + r"\b", block):
            return ("BlocksGT." + name + ("_slab" if slab else ""), meta)
    if re.search(r"\bBlocks\.planks\b", block):
        return ("Blocks.planks", meta)
    if re.search(r"\bBlocks\.log2\b", block):
        return ("Blocks.log2", meta)
    if re.search(r"\bBlocks\.log\b", block):
        return ("Blocks.log", meta)
    if re.search(r"\bBlocks\.wooden_slab\b", block):
        return ("Blocks.wooden_slab", meta)
    other = re.search(r"\b(MD|IL)\.([A-Za-z0-9_]+)", block)
    if other:
        return ("%s.%s" % (other.group(1), other.group(2)), meta)
    return (re.sub(r"\s+", "", block), meta)


def material_field(expr: str):
    """``MT.WoodRubber`` -> ``("any", "WoodRubber")``; ``MT.WOODS.Maple`` -> ``("woods", "Maple")``."""
    expr = expr.strip()
    if expr in ("null", "NI"):
        return None
    match = re.fullmatch(r"MT\.(WOODS\.)?([A-Za-z_][A-Za-z0-9_]*)", expr)
    if not match:
        return None
    return ("woods" if match.group(1) else "any", match.group(2))


# 鈹€鈹€ GT6 wood dictionary 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

class Entry:
    def __init__(self, kind, line):
        self.kind = kind
        self.line = line
        self.stack = None            # (block, meta)
        self.material = None         # (scope, GT6 material field) of the *plank* material
        self.bark_material = None    # (scope, field) of the bark material, if any
        self.ints = []
        self.beam = None             # BeamEntry (resolved)
        self.plank = None            # PlankEntry (resolved)
        self.beam_ref = None         # unresolved ``WoodDictionary.BEAMS.get(...)`` reference
        self.plank_ref = None
        self.raw = ""

    def __repr__(self):
        return "Entry(%s, line=%s, stack=%s, material=%s)" % (
            self.kind, self.line, self.stack, self.material)


CTOR_RE = re.compile(r"new (WoodEntry|BeamEntry|PlankEntry)\s*\(")


def parse_ctor(text):
    """Recursively parse one ``new (Wood|Beam|Plank)Entry(...)`` expression into an Entry.

    The material that matters for the port is the *plank* material the table carries
    (``MT.WoodRubber``, ``MT.WOODS.Maple``...); a wood entry's trailing material arguments are the
    bark material (``WoodEntry.java:47``), which is why they are collected separately.
    """
    match = CTOR_RE.match(text.strip())
    if not match:
        return None
    inside = text.strip()[match.end():]
    if inside.endswith(")"):
        inside = inside[:inside.rfind(")")]
    args = split_args(inside)
    entry = Entry(match.group(1), 0)
    entry.raw = re.sub(r"\s+", " ", text.strip())
    direct = []
    if args:
        spec = stack_spec(args[0])
        if spec:
            entry.stack = block_key(spec[0], spec[1])
    for arg in args[1:]:
        if CTOR_RE.match(arg.strip()):
            inner = parse_ctor(arg)
            if inner.kind == "PlankEntry":
                entry.plank = inner
            elif inner.kind == "BeamEntry":
                entry.beam = inner
            continue
        if "WoodDictionary.PLANKS.get" in arg:
            entry.plank_ref = arg
        elif "WoodDictionary.BEAMS.get" in arg:
            entry.beam_ref = arg
        elif re.fullmatch(r"\d+", arg):
            entry.ints.append(int(arg))
        else:
            field = material_field(arg)
            if field:
                if entry.kind == "PlankEntry":
                    entry.material = field
                else:
                    direct.append(field)
    if entry.material is None:
        source = entry.plank if entry.plank is not None else entry.beam
        if source is not None:
            entry.material = source.material
    if entry.material is None and direct and entry.kind == "BeamEntry" and entry.plank_ref is None:
        entry.material = direct[0]
    if entry.kind == "WoodEntry" and direct:
        entry.bark_material = direct[-1]
    return entry


def parse_dictionary():
    """Parse LoaderWoodDictionary.java into plank/beam/wood entries.

    GT6 nests the table (a wood entry owns a beam entry which owns a plank entry), so the parser
    walks the constructor calls recursively; ``WoodDictionary.PLANKS/BEAMS.get(block, meta)``
    references are resolved in a second pass once every inline entry is known.
    """
    text = read(DICT_FILE)
    entries = []
    planks_by_stack = {}
    beams_by_stack = {}

    def collect(entry):
        if entry is None:
            return
        if entry.kind == "PlankEntry" and entry.stack:
            planks_by_stack[entry.stack] = entry
        if entry.kind == "BeamEntry" and entry.stack:
            beams_by_stack[entry.stack] = entry
        collect(entry.plank)
        collect(entry.beam)

    pending = []
    for statement, line in statements(text):
        stripped = statement.strip()
        if not CTOR_RE.match(stripped) or stripped.startswith("new SaplingEntry"):
            continue
        entry = parse_ctor(stripped)
        if entry is None:
            continue
        entry.line = line
        for child in (entry.plank, entry.beam):
            if child is not None:
                child.line = line
        entries.append(entry)
        collect(entry)
        if entry.beam_ref or entry.plank_ref:
            pending.append(entry)

    # Second pass: resolve the references to other entries of the same table.
    for entry in pending:
        for attr, table in (("beam_ref", beams_by_stack), ("plank_ref", planks_by_stack)):
            raw = getattr(entry, attr)
            if not raw:
                continue
            key = re.search(r"WoodDictionary\.(?:PLANKS|BEAMS)\.get\(\s*(?P<block>.+?)\s*(?:,\s*"
                            r"(?P<meta>[^,()]+?)\s*)?\)", raw)
            if not key:
                continue
            spec = stack_spec("ST.make(%s, 1, %s)" % (key.group("block"), key.group("meta") or "0"))
            stack = block_key(spec[0], spec[1]) if spec else None
            resolved = table.get(stack) if stack else None
            setattr(entry, attr.replace("_ref", ""), resolved)
            if resolved is not None and entry.material is None:
                entry.material = resolved.material
    return entries


def parse_material_ids():
    """GT6 material ids: ``(all fields, the ``MT.WOODS`` class fields)``.

    ``MT.java`` declares the spice ``Cinnamon`` and the wood ``WOODS.Cinnamon``
    (``Cinnamonwood``, id 9317) under fields of different names, so the two namespaces have to stay
    apart: the wood table refers to ``MT.WOODS.*`` with a qualified name.
    """
    text = strip_comments(read(MT_FILE))
    all_ids = {}
    for match in re.finditer(r"\b([A-Z][A-Za-z0-9_]*)\s*=\s*[a-zA-Z_][a-zA-Z0-9_]*\s*\(\s*(\d{3,5})\s*,\s*\"",
                             text):
        all_ids.setdefault(match.group(1), int(match.group(2)))

    wood_ids = {}
    start = text.find("class WOODS")
    if start < 0:
        sys.exit("MT.java has no WOODS class - the generator needs updating")
    open_brace = text.find("{", start)
    depth = 0
    end = open_brace
    for i in range(open_brace, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                end = i
                break
    for match in re.finditer(r"\b([A-Z][A-Za-z0-9_]*)\s*=\s*[a-zA-Z_][a-zA-Z0-9_]*\s*\(\s*(\d{3,5})\s*,\s*\"",
                             text[open_brace:end]):
        wood_ids.setdefault(match.group(1), int(match.group(2)))
    return all_ids, wood_ids


def parse_wood_counts():
    """The table's own count defaults and the NERFED_WOOD switch."""
    wood = read(WOOD_ENTRY_FILE)
    beam = read(BEAM_ENTRY_FILE)
    plank = read(PLANK_ENTRY_FILE)
    cs = read(CS_FILE)

    def find(pattern, text, what):
        """Last match wins: the count-forwarding constructor sits after the count-less one."""
        matches = list(re.finditer(pattern, text))
        if not matches:
            sys.exit("cannot read %s from the GT6 wood table - the generator needs updating" % what)
        return matches[-1]

    w = find(r"this\(aLog, aBeam, aPlank, aCharcoalCount, aCreosoteAmount, (\d+), (\d+), (\d+)\);",
             wood, "WoodEntry plank counts")
    ws = find(r"aPlankCountBuzz, (?:[^;]*?, )?aStick, (\d+), (\d+)\);", wood, "WoodEntry stick counts")
    b = find(r"this\(aBeam, aPlank, aCharcoalCount, aCreosoteAmount, (\d+), (\d+), (\d+)\);",
             beam, "BeamEntry plank counts")
    bs = find(r"aPlankCountBuzz, (?:[^;]*?, )?aStick, (\d+), (\d+)\);", beam, "BeamEntry stick counts")
    p = find(r"aPlankIndex, aStick, (\d+), (\d+), (\d+)\);", plank, "PlankEntry stick counts")
    nerfed = find(r"NERFED_WOOD = ([TF])", cs, "CS.NERFED_WOOD").group(1) == "T"
    return {
        "wood": {"hand": int(w.group(1)), "saw": int(w.group(2)), "buzz": int(w.group(3)),
                 "stick_saw": int(ws.group(1)), "stick_lathe": int(ws.group(2))},
        "beam": {"hand": int(b.group(1)), "saw": int(b.group(2)), "buzz": int(b.group(3)),
                 "stick_saw": int(bs.group(1)), "stick_lathe": int(bs.group(2))},
        "plank": {"hand": int(p.group(1)), "saw": int(p.group(2)), "lathe": int(p.group(3))},
        "nerfed": nerfed,
    }


# 鈹€鈹€ GT6 recipe rows 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

def parse_rows():
    """Every recipe statement of Loader_Recipes_Woods.java, with its line and the enclosing loop."""
    text = read(ROWS_FILE)
    clean = strip_comments(text)
    lines = clean.splitlines()
    raw_lines = text.splitlines()
    rows = []
    loop = None
    for_loop = None
    for index, line in enumerate(lines):
        stripped = line.strip()
        # The enclosing `for` decides which WoodDictionary list a row iterates (the result family);
        # a later `if (IL.…)`/`if (MD.…)` must not hide it, so both are tracked separately.
        if stripped.startswith("for ("):
            for_loop = (index + 1, stripped)
        if stripped.startswith("for (") or stripped.startswith("if (MD.") or stripped.startswith("if (IL."):
            loop = (index + 1, stripped)
        match = re.match(r"^(RM\.\w+\s*\.addRecipe\w*|CR\.(?:shaped|shapeless|remove|cycling))\s*\(", stripped)
        if not match:
            continue
        # statement text may run over several lines: collect until the parens balance
        depth = 0
        chunk = []
        for offset in range(index, len(lines)):
            chunk.append(raw_lines[offset].strip())
            depth += lines[offset].count("(") - lines[offset].count(")")
            if depth <= 0:
                break
        statement = re.sub(r"\s+", " ", " ".join(chunk))
        rows.append({"line": index + 1, "kind": "RM" if statement.startswith("RM.") else "CR",
                     "statement": statement, "loop": loop, "for_loop": for_loop})
    return rows


def statement_result_family(loop_text: str, statement: str):
    """Which form GT6's statement registers, read from its loop and its *output* expression.

    ``Loader_Recipes_Woods`` writes the result as the first argument of its ``CR.shaped``/``CR.shapeless``
    call: the wood and beam loops pass ``aEntry.mStick`` (a ``WoodEntry``/``BeamEntry`` carries
    ``OP.stickLong``), the plank loop passes the ``PlankEntry``'s ``aEntry.mStick`` (``OP.stick``) and the
    plank rows of the wood/beam loops pass ``aEntry.mPlankEntry.mPlank`` (planks).
    """
    if not loop_text:
        return None
    loop = re.search(r"WoodDictionary\.(LIST_WOODS|LIST_BEAMS|LIST_PLANKS)", loop_text)
    if not loop:
        return None
    start = statement.index("(") + 1
    args = split_args(statement[start:statement.rindex(")")])
    if not args:
        return None
    output = args[0]
    if "aEntry.mStick" in output:
        return "rod" if loop.group(1) == "LIST_PLANKS" else "rodlong"
    if "aEntry.mPlank" in output:
        return "planks"
    return None


def mirror_flag(statement: str) -> str:
    """GT6's ``CR.MIR`` bit of a pattern row (``CR.java:161-168``): mirror or not."""
    return "mirror" if re.search(r"\bMIR\b", statement) else "nomirror"


# 鈹€鈹€ Port sources 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

def parse_port_species():
    text = read(SPECIES_FILE)
    return set(re.findall(r'^\s+[A-Z_0-9]+\(\s*"([a-z_0-9]+)"', text, re.M))


def parse_port_wood_blocks():
    text = read(WOODS_FILE)
    needed = set()
    for prefix in ("log_", "planks_", "beam_"):
        if re.search(r'reg\("%s" \+ sp\.id\(\)' % prefix, text):
            needed.add(prefix)
    return needed


def parse_port_materials():
    """Port material id -> registry name, from the generated material tables."""
    materials = {}
    for name in sorted(os.listdir(MATERIALS_DIR)):
        if not name.endswith(".java"):
            continue
        text = read(os.path.join(MATERIALS_DIR, name))
        for match in re.finditer(r'=\s*[a-zA-Z_][a-zA-Z0-9_]*\s*\(\s*(\d+)\s*,\s*"([^"]+)"', text):
            materials[int(match.group(1))] = match.group(2)
    return materials


def material_form_flags(material_id):
    text = read(MATERIAL_FORMS_FILE)
    match = re.search(r'"%d\|([A-Z_,]*)"' % material_id, text)
    return set(match.group(1).split(",")) if match else set()


# 鈹€鈹€ row generation 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="report without writing the table")
    args = parser.parse_args()

    dictionary = parse_dictionary()
    rows = parse_rows()
    all_material_ids, wood_material_ids = parse_material_ids()
    counts = parse_wood_counts()
    port_species = parse_port_species()
    port_blocks = parse_port_wood_blocks()
    port_materials = parse_port_materials()

    rm_count = sum(1 for row in rows if row["kind"] == "RM")
    cr_count = sum(1 for row in rows if row["kind"] == "CR")
    print("GT6 Loader_Recipes_Woods.java: %d recipe statements (%d RM.*, %d CR.*)"
          % (len(rows), rm_count, cr_count))
    print("GT6 LoaderWoodDictionary.java: %d table entries" % len(dictionary))
    print("counts: %s (NERFED_WOOD=%s)" % (counts, counts["nerfed"]))

    # Verify the eight generated statements are where they are expected to be.
    source_lines = read(ROWS_FILE).splitlines()
    for row_id, line, signature, _copy in ROW_SPECS:
        if line > len(source_lines) or not re.search(signature, source_lines[line - 1]):
            sys.exit("Loader_Recipes_Woods.java:%d is no longer the %s row - the generator needs updating"
                     % (line, row_id))

    # Index the dictionary by (block family, meta), and the statements by line.
    by_stack = {}
    for entry in dictionary:
        if entry.stack:
            by_stack.setdefault(entry.stack, entry)
    statements_by_line = {row["line"]: row["statement"] for row in rows}
    loops_by_line = {row["line"]: (row["for_loop"][1] if row["for_loop"] else "") for row in rows}

    species_rows = []
    craft_rows = []
    skipped_rows = []
    unmapped = []
    missing_materials = {}

    for family, meta, species, expected_material in SPECIES:
        if species not in port_species:
            sys.exit("the port has no WoodSpecies '%s' - the generator needs updating" % species)
        for prefix in ("log_", "planks_", "beam_"):
            if prefix not in port_blocks:
                sys.exit("GTWoods registers no '%s<species>' block - the generator needs updating" % prefix)
        log_entry = by_stack.get(("BlocksGT." + family, str(meta)))
        if log_entry is None or log_entry.kind != "WoodEntry":
            sys.exit("LoaderWoodDictionary has no %s meta %d wood entry - the generator needs updating"
                     % (family, meta))
        if log_entry.material is None or log_entry.material[1] != expected_material:
            sys.exit("LoaderWoodDictionary.java:%d carries plank material %s, the generator expects %s"
                     % (log_entry.line, log_entry.material, expected_material))
        scope, field = log_entry.material
        material_id = (wood_material_ids if scope == "woods" else all_material_ids).get(field)
        if material_id is None:
            sys.exit("MT.java has no id for %s.%s - the generator needs updating" % (scope, field))
        port_material = port_materials.get(material_id)
        plank_entry = log_entry.plank if log_entry.plank is not None else (
            log_entry.beam.plank if log_entry.beam is not None else None)
        plank_ref = ("%s/%s" % plank_entry.stack) if plank_entry is not None and plank_entry.stack else "-"
        species_rows.append("%s|log_%s|planks_%s|beam_%s|%s|LoaderWoodDictionary.java:%d|%s"
                            % (species, species, species, species, port_material or "-",
                               log_entry.line, plank_ref))

        def add(row_id, shape, pattern, keys, result, line):
            # GT6's CR.MIR bit decides mirroring (CR.java:161-168); the eight rows all use CR.DEF_NCC.
            craft_rows.append("%s/%s|%s|%s|%s|%s|%s|Loader_Recipes_Woods.java:%d"
                              % (species, row_id, shape, mirror_flag(statements_by_line[line]),
                                 pattern, keys, result, line))

        def drop(row_id, reason, line):
            """A row GT6 registers but this port cannot express; the table has to say so."""
            skipped_rows.append("%s/%s|Loader_Recipes_Woods.java:%d|%s"
                                % (species, row_id, line, reason))
            missing_materials.setdefault(field, []).append("%s/%s" % (species, row_id))

        # The rod rows need the port's STICKS form on the plank material (OP.stick/stickLong); without
        # it GTItems.getStack returns nothing and the loader would have to skip the row at runtime.
        material_flags = material_form_flags(material_id)

        # Loader_Recipes_Woods.java:183-185 (the LIST_WOODS loop)
        if port_material is None:
            drop("log_to_sticks",
                 "the port has no MT.WOODS.%s (GT6 material %d), so GT6's OP.stickLong of the plank "
                 "material cannot be built" % (field, material_id), 183)
        elif "STICKS" not in material_flags:
            drop("log_to_sticks",
                 "the port's imported form flags for %s (%d) carry no STICKS, so it has no long rod"
                 % (port_material, material_id), 183)
        else:
            add("log_to_sticks", "shaped", "sLf",
                "s=tool:saw;L=log:%s;f=tool:file" % species,
                "rodlong:%s*%d" % (port_material, counts["wood"]["stick_saw"]), 183)
        add("log_to_planks_saw", "shaped", "s/L",
            "s=tool:saw;L=log:%s" % species,
            "planks:%s*%d" % (species, counts["wood"]["saw"]), 184)
        add("log_to_planks_hand", "shapeless", "",
            "L=log:%s" % species,
            "planks:%s*%d" % (species, counts["wood"]["hand"]), 185)

        # Loader_Recipes_Woods.java:203-205 (the LIST_BEAMS loop); beams need their own counts
        if log_entry.beam is None:
            sys.exit("LoaderWoodDictionary.java:%d has no beam entry - the generator needs updating"
                     % log_entry.line)
        if port_material is None:
            drop("beam_to_sticks",
                 "the port has no MT.WOODS.%s (GT6 material %d), so GT6's OP.stickLong of the beam "
                 "material cannot be built" % (field, material_id), 203)
        elif "STICKS" not in material_flags:
            drop("beam_to_sticks",
                 "the port's imported form flags for %s (%d) carry no STICKS, so it has no long rod"
                 % (port_material, material_id), 203)
        else:
            add("beam_to_sticks", "shaped", "sBf",
                "s=tool:saw;B=beam:%s;f=tool:file" % species,
                "rodlong:%s*%d" % (port_material, counts["beam"]["stick_saw"]), 203)
        add("beam_to_planks_saw", "shaped", "s/B",
            "s=tool:saw;B=beam:%s" % species,
            "planks:%s*%d" % (species, counts["beam"]["saw"]), 204)
        add("beam_to_planks_hand", "shapeless", "",
            "B=beam:%s" % species,
            "planks:%s*%d" % (species, counts["beam"]["hand"]), 205)

        # Loader_Recipes_Woods.java:243-244 (the LIST_PLANKS loop): OP.stick of the plank material
        if port_material is None:
            for row_id in ("planks_to_sticks_hand", "planks_to_sticks_saw"):
                drop(row_id,
                     "the port has no MT.WOODS.%s (GT6 material %d), so GT6's OP.stick of the plank "
                     "material cannot be built" % (field, material_id),
                     243 if row_id.endswith("hand") else 244)
        elif "STICKS" not in material_flags:
            for row_id in ("planks_to_sticks_hand", "planks_to_sticks_saw"):
                drop(row_id,
                     "the port's imported form flags for %s (%d) carry no STICKS, so it has no rod"
                     % (port_material, material_id), 243 if row_id.endswith("hand") else 244)
        else:
            add("planks_to_sticks_hand", "shaped", "P/P",
                "P=planks:%s" % species,
                "rod:%s*%d" % (port_material, counts["plank"]["hand"] * 2), 243)
            add("planks_to_sticks_saw", "shaped", "s/P",
                "s=tool:saw;P=planks:%s" % species,
                "rod:%s*%d" % (port_material, counts["plank"]["saw"]), 244)

    # Skipped statements: everything that is not one of the eight generated lines. GT6 gates whole
    # blocks on mod loaders, so the containing loop/if header counts as context for the reason.
    wood_flags = material_form_flags(8221)
    if "PARTS" in wood_flags or "TOOL_HEAD" in wood_flags:
        button_reason = ("GT6's wooden-button rows: the port has no OD.buttonWood tag; the rows' outputs "
                         "are MT.Wood forms the port does generate, so a tag would have to be invented")
    else:
        button_reason = ("GT6's wooden-button rows: the output form (OP.gearGt/casingSmall/toolHead*) is "
                         "not generated for MT.Wood - MT.java:312 G_WOOD (TD.java:604) carries no "
                         "PARTS/TOOL_HEAD, so the original's CR.shaped finds no output itself")
    generated_lines = {line for _id, line, _sig, _copy in ROW_SPECS}
    skipped = []
    for row in rows:
        if row["line"] in generated_lines:
            continue
        context = row["statement"] + " " + (row["loop"][1] if row["loop"] else "")
        reason = FALLBACK_REASON
        for pattern, kind, text in SKIP_REASONS:
            if re.search(kind, row["statement"]) and re.search(pattern, context):
                reason = text.replace("{button}", button_reason)
                break
        skipped.append("Loader_Recipes_Woods.java:%d|%s|%s|%s"
                       % (row["line"], row["kind"], reason, row["statement"][:150]))

    # Validate every generated row against the rules `Loader_WoodCraftingRecipes` applies, so a
    # malformed table can never be written: 7 fields, single-character key symbols of the form
    # <symbol>=<kind>:<argument>, spec kinds the loader implements, and every pattern symbol defined.
    # (An earlier revision wrote keys the loader rejected, which skipped every single row at runtime.)
    for row in craft_rows:
        parts = row.split("|")
        path = parts[0]
        if len(parts) != 7:
            sys.exit("generated row %s has %d fields, expected 7: %s" % (path, len(parts), row))
        shape = parts[1]
        symbols = set()
        for entry in filter(None, parts[4].split(";")):
            if len(entry) < 3 or entry[1] != "=":
                sys.exit("generated row %s has a malformed key %r" % (path, entry))
            symbols.add(entry[0])
            kind = entry[2:].split(":", 1)[0]
            if kind not in SPEC_KINDS:
                sys.exit("generated row %s uses spec kind %r, which Loader_WoodCraftingRecipes does "
                         "not implement (it knows: %s)" % (path, kind, ", ".join(SPEC_KINDS)))
        if shape == "shapeless":
            if parts[3]:
                sys.exit("generated shapeless row %s carries a pattern %r" % (path, parts[3]))
        else:
            for pattern_row in parts[3].split("/"):
                for symbol in pattern_row:
                    if symbol != " " and symbol not in symbols:
                        sys.exit("generated row %s uses pattern symbol %r without a key" % (path, symbol))
        result_kind = parts[5].split("*", 1)[0].split(":", 1)[0]
        if result_kind not in SPEC_KINDS:
            sys.exit("generated row %s has result kind %r, which Loader_WoodCraftingRecipes does not "
                     "implement" % (path, result_kind))
        # The row has to produce what the GT6 statement it cites registers, and that expectation has to
        # agree with what the statement's own loop and output expression say.
        line = int(parts[6][parts[6].index(":") + 1:])
        expected = RESULT_KIND_BY_LINE.get(line)
        if expected is None:
            sys.exit("generated row %s cites %s, which is not one of the eight dictionary statements"
                     % (path, parts[6]))
        if result_kind != expected:
            sys.exit("generated row %s cites %s, which registers a %s, but the row produces a %s"
                     % (path, parts[6], expected, result_kind))
        derived = statement_result_family(loops_by_line.get(line, ""), statements_by_line.get(line, ""))
        if derived is None:
            sys.exit("cannot read the result family of %s from its GT6 statement - the generator needs "
                     "updating" % parts[6])
        if derived != expected:
            sys.exit("Loader_Recipes_Woods.java:%d registers a %s (its loop and output expression), but "
                     "the generator expects a %s" % (line, derived, expected))

    # Dictionary entries the port has no blocks for, grouped by block family.
    mapped_families = {"BlocksGT." + family for family, _meta, _species, _material in SPECIES}
    families = {}
    for entry in dictionary:
        if not entry.stack:
            continue
        families.setdefault(entry.stack[0], []).append(entry.line)
    for family in sorted(families):
        lines = families[family]
        if family in mapped_families:
            continue
        if family in ("BlocksGT.Planks", "BlocksGT.Planks2", "BlocksGT.PlanksFireProof",
                      "BlocksGT.Planks2FireProof"):
            reason = ("the extra plank blocks of this family: metas 0-7 are the mapped species' planks, "
                      "the rest are GT6's compressed/treated/dead/rotten/fireproof planks, which the port "
                      "does not register")
        elif family.startswith("BlocksGT."):
            reason = ("GT6-only wood block family: the port registers none of these (dead/rotten/mossy/"
                      "frozen planks, the fireproof twins and GT6's own beam blocks)")
        elif family.startswith("Blocks."):
            reason = ("vanilla wood: the port keeps vanilla's own log/plank recipes, so this dictionary "
                      "row is not translated")
        elif family.startswith("MD.") or family.startswith("IL."):
            reason = "another mod's block (IC2/MFR/BoP/TC/TF/EBXL/RH/EtFu/...), not present in 1.20.1"
        else:
            reason = "a local variable of a mod-specific row of the table (other mod)"
        unmapped.append("%s|LoaderWoodDictionary.java:%d-%d|%d entries|%s"
                        % (family, min(lines), max(lines), len(lines), reason))
    for family, meta, species, _material in SPECIES:
        entry = by_stack.get(("BlocksGT." + family, str(meta)))
        if entry is not None:
            unmapped.append("%s meta %d|LoaderWoodDictionary.java:%d|mapped to the port's %s blocks"
                            % (family, meta, entry.line, species))
    # The port's species GT6's dictionary cannot express at all: they are mapped 1:0, not 1:1, and the
    # report has to say so with the line the material does appear on (if any).
    dictionary_lines = read(DICT_FILE).splitlines()
    for species, material in PORT_ONLY_SPECIES:
        if species not in port_species:
            sys.exit("the port has no WoodSpecies '%s' - the generator needs updating" % species)
        if material not in wood_material_ids:
            sys.exit("MT.WOODS has no %s field - the generator needs updating" % material)
        found = [number for number, line in enumerate(dictionary_lines, 1)
                 if re.search(r"\bMT\.WOODS\." + material + r"\b", line)]
        if found:
            unmapped.append("%s (%s)|LoaderWoodDictionary.java:%d|mapped 1:0 - the only %s dictionary row "
                            "is another mod's block|the port's own %s log/planks/beam have no GT6 source "
                            "row" % (species, material, found[0], material, species))
        else:
            unmapped.append("%s (%s)|LoaderWoodDictionary.java (no row)|mapped 1:0 - GT6 never registered "
                            "a dictionary row for %s|the port's own %s log/planks/beam have no GT6 source "
                            "row" % (species, material, material, species))

    # Deterministic order, no duplicates.
    unmapped = sorted(set(unmapped))

    total = len(SPECIES) * len(ROW_SPECS)
    if len(craft_rows) + len(skipped_rows) != total:
        sys.exit("the table accounts for %d of the %d dictionary rows (9 species x 8) - every row has to "
                 "be either generated or listed in SKIPPED_CRAFT_ROWS"
                 % (len(craft_rows) + len(skipped_rows), total))

    print("generated crafting rows: %d of %d (%d species x %d rows), %d rows cannot be expressed"
          % (len(craft_rows), total, len(SPECIES), len(ROW_SPECS), len(skipped_rows)))
    for name, rows_missing in sorted(missing_materials.items()):
        print("  material %s missing in the port -> skipped: %s" % (name, ", ".join(rows_missing)))
    print("skipped GT6 statements: %d" % len(skipped))
    print("unmapped dictionary families: %d" % len(unmapped))

    def java_array(values, indent="            "):
        return "\n".join('%s"%s",' % (indent, value.replace('\\', '\\\\').replace('"', '\\"'))
                         for value in values)

    text = FLAG_TEMPLATE
    for token, value in (("@@species@@", java_array(species_rows)),
                         ("@@rows@@", java_array(craft_rows)),
                         ("@@skipped@@", java_array(skipped)),
                         ("@@skipped_rows@@", java_array(skipped_rows)),
                         ("@@unmapped@@", java_array(unmapped)),
                         ("@@row_count@@", str(len(rows))),
                         ("@@rm_count@@", str(rm_count)),
                         ("@@cr_count@@", str(cr_count))):
        text = text.replace(token, value)
    if "@@" in text:
        sys.exit("unreplaced placeholder in the generated table: %s"
                 % text[text.index("@@"):text.index("@@") + 30])

    if args.check:
        print("--check: not written")
        return
    with open(OUT_FILE, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(text)
    print("wrote %s (%d rows, %d documented row skips, %d skipped statements, %d unmapped families)"
          % (os.path.relpath(OUT_FILE, ROOT), len(craft_rows), len(skipped_rows), len(skipped),
             len(unmapped)))


if __name__ == "__main__":
    main()
