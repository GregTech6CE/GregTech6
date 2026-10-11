#!/usr/bin/env python3
"""Generate BasicMachineCraftingRecipes.java from the original GT6 registrations.

Input : tools/gt6_basic_machine_recipes.json  (extracted from
        gregtech6-master/.../Loader_MultiTileEntities.java, tab "Basic Machines")
        core/src/main/java/.../content/machine/BasicMachineCatalog.java (tier counts)
Output: core/src/main/java/com/gregtech/gregtech/data/BasicMachineCraftingRecipes.java

The generated table carries the ORIGINAL GT6 pattern rows and key symbols for every
port machine variant. Ingredient resolution (material forms, tier components,
fallbacks) happens at runtime in MachineRecipeIngredients.java.
"""

from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "core/src/main/java/com/gregtech/gregtech"
SOURCE_JSON = Path(__file__).with_name("gt6_basic_machine_recipes.json")
OUT = JAVA / "data/BasicMachineCraftingRecipes.java"

# ── port machine name -> original GT6 machine name (Basic Machines tab) ─────────
SAME_NAME = {
    "oven": "Oven",
    "roaster": "Roasting Oven",
    "distillery": "Distillery",
    "smelter": "Smelter",
    "crystallisationcrucible": "Crystallisation Crucible",
    "dryer": "Dryer",
    "laminator": "Laminator",
    "catalyticcracker": "Catalytic Cracker",
    "steamcracker": "Steam Cracker",
    "shredder": "Shredder",
    "lathe": "Lathe",
    "buzzsaw": "Buzzsaw",
    "centrifuge": "Centrifuge",
    "rollingmill": "Rolling Mill",
    "rollbender": "Roll Bender",
    "rollformer": "Roll Former",
    "clustermill": "Cluster Mill",
    "wiremill": "Wiremill",
    "mixer": "Mixer",
    "loom": "Loom",
    "sluice": "Sluice",
    "sander": "Sanding Machine",
    "burnmixer": "Burner Mixer",
    "debarker": "Pressure Washer",
    "crusher": "Crusher",
    "sifter": "Sifter",
    "squeezer": "Squeezer",
    "compressor": "Compressor",
    "press": "Press",
    "electrolyzer": "Electrolyzer",
    "canner": "Canning Machine",
    "injector": "Injector",
    "printer": "Printer",
    "scannervisuals": "Scanner (Visuals,",
    "autocrafter": "Autocrafter",
    "electricmixer": "Electric Mixer",
    "electricloom": "Electric Loom",
    "electricsifter": "Electric Sifter",
    "slicer": "Slicer",
    "nanofab": "Nanoscale Fabricator",
    "plantalyzer": "Plantalyzer",
    "bumblelyzer": "Bumblelyzer",
    "boxinator": "Boxinator",
    "unboxinator": "Unboxinator",
    "polarizer": "Polarizer",
    "magneticseparator": "Magnetic Separator",
    "autoclave": "Autoclave",
    "bath": "Bath",
    "generifier": "Generifier",
    "coagulator": "Coagulator",
    "fermenter": "Fermenter",
    "melter": "Melter",
    "lightning": "Lightning Processor",
}

# port machine -> ("<original name with %d>", tier count) ; each port tier is its own GT6 machine
PER_TIER_NAME = {
    "laserengraver": "Laser Engraver (T%d)",
    "laserwelder": "Laser Welder (T%d)",
    "freezer": "Freezer (T%d)",
    "cryomixer": "Cryo Mixer (T%d)",
    "massfab": "Matter Fabricator (T%d)",
    "scannermolecular": "Molecular Scanner (T%d)",
    "replicator": "Matter Replicator (T%d)",
}

# port machine -> explicit per-port-tier original machine name
EXPLICIT_TIERS = {
    # GT6 registers the low tier as its own machine; the port merged it into one type.
    "extruder": ["Low Heat Extruder", "Extruder", "Extruder", "Extruder"],
}

# The port registers extra variants beyond the tier list in BasicMachineDefinitions
# (see the massfab_osmiridium_t2..t5 loop at the end of specifications()).
EXTRA_TIERS = {
    "massfab": [1, 2, 3, 4, 5],
    "replicator": [1, 2, 3, 4, 5],
}

# Port tier numbers for machines whose GT6 variants are not T1..Tn: GT6 registers the Molecular
# Scanner as T3 only (Loader_MultiTileEntities:1549-1553 — T1/T2/T4/T5 are commented out), so the
# port's single scanner is tier 3 (see tools/gt6_machine_map.py for the same table).
TIER_NUMBERS = {
    "scannermolecular": [3],
}

# Port machines registered as single blocks although GT6 has them in the
# "Multiblock Machines" tab. Only the ones whose parts this port already provides are
# listed; the rest stay out of the pack (see BasicMachineRecipePack.MULTIBLOCK_CONTROLLERS).
MULTIBLOCK_BACKED = {
    "cokeoven": "Coke Oven",
    "implosioncompressor": "Implosion Compressor",
}

# Machines registered as basic machines in the port, but as GT6 multiblocks (no
# "Basic Machines" registration) — handled by the multiblock recipe table instead.
SKIP = {
    "cokeoven", "implosioncompressor", "fusionreactor",
    "cryodistillationtower", "distillationtower",
    "largecentrifuge", "largeelectrolyzer", "largecoagulator", "largeautoclave",
    "largebath", "largemixer", "largefermenter", "largeoven", "largesluice",
    "largecrusher", "largeshredder", "largesqueezer", "largemassfab",
    "laserengraver_alias",
}


def norm(expr: str) -> str:
    e = expr.strip()
    e = re.sub(r"\s+\.", ".", e)
    e = re.sub(r"\.\s*dat", ".dat", e)
    e = re.sub(r"\(\s*", "(", e)
    e = re.sub(r"\s*\)", ")", e)
    e = re.sub(r"\s+", " ", e)
    return e


# ── ingredient expression -> runtime spec ──────────────────────────────────────
# Material-parameterised forms use the machine's own casing material ("aMat").
MAT_SPECS = {
    "OP.casingMachine.dat(aMat)": "block:casingMachine",
    "OP.casingMachineDouble.dat(aMat)": "block:casingMachineDouble",
    "OP.casingMachineQuadruple.dat(aMat)": "block:casingMachineQuadruple",
    "OP.casingSmall.dat(aMat)": "mat:itemCasing",
    "OP.gearGt.dat(aMat)": "mat:gearGt",
    "OP.gearGtSmall.dat(aMat)": "mat:gearGtSmall",
    "OP.stick.dat(aMat)": "mat:stick",
    "OP.stickLong.dat(aMat)": "mat:stickLong",
    "OP.screw.dat(aMat)": "mat:screw",
    "OP.spring.dat(aMat)": "mat:spring",
    "OP.rotor.dat(aMat)": "mat:rotor",
    "OP.plate.dat(aMat)": "mat:plate",
    "OP.plateDouble.dat(aMat)": "mat:plateDouble",
    "OP.plateTriple.dat(aMat)": "mat:plateTriple",
    "OP.plateQuadruple.dat(aMat)": "mat:plateQuadruple",
    "OP.plateQuintuple.dat(aMat)": "mat:plateQuintuple",
    "OP.plateDense.dat(aMat)": "mat:plateDense",
    "OP.wireFine.dat(aMat)": "mat:wireFine",
    "OP.pipeTiny.dat(aMat)": "pipe:tiny",
    "OP.pipeSmall.dat(aMat)": "pipe:small",
    "OP.pipeMedium.dat(aMat)": "pipe:medium",
    "OP.pipeLarge.dat(aMat)": "pipe:large",
    "OP.pipeHuge.dat(aMat)": "pipe:huge",
    "OP.pipeQuadruple.dat(aMat)": "pipe:quadruple",
}

# Prefix name used by the port's Loader_Items for each GT6 OP item prefix.
PREFIX_NAME = {
    "plate": "plate",
    "plateDouble": "plateDouble",
    "plateTriple": "plateTriple",
    "plateQuadruple": "plateQuadruple",
    "plateQuintuple": "plateQuintuple",
    "plateDense": "plateDense",
    "plateGem": "plateGem",
    "plateGemTiny": "plateGemTiny",
    "gearGt": "gearGt",
    "gearGtSmall": "gearGtSmall",
    "stick": "stick",
    "stickLong": "stickLong",
    "screw": "screw",
    "spring": "spring",
    "rotor": "rotor",
    "wireFine": "wireFine",
    "dust": "dust",
    "gem": "gem",
    "itemCasing": "itemCasing",
    "toolHeadBuzzSaw": "toolHeadBuzzSaw",
    "blockPlate": "blockPlate",
    "cableGt01": "cable",
    "wireGt01": "wire",
}
for size in (1, 2, 4, 8, 12, 16):
    PREFIX_NAME[f"wireGt{size:02d}"] = "wire"
    PREFIX_NAME[f"cableGt{size:02d}"] = "cable"

# Fixed (non-aMat) material references used by the original recipes.
FIXED_MATERIAL = {
    "ANY.Cu": "Copper",
    "ANY.Steel": "Steel",
    "ANY.Iron": "Iron",
    "ANY.Diamond": "Diamond",
    "MT.StainlessSteel": "StainlessSteel",
    "MT.Lumium": "Lumium",
    "MT.Invar": "Invar",
    "MT.Si": "Silicon",
    "MT.TungstenCarbide": "TungstenCarbide",
    "MT.Pt": "Platinum",
    "MT.Constantan": "Constantan",
    "MT.Kanthal": "Kanthal",
    "MT.Nichrome": "Nichrome",
    "MT.SiC": "Carborundum",
    "MT.CobaltBrass": "CobaltBrass",
    "MT.OREMATS.Zeolite": "Zeolite",
    "MT.AnnealedCopper": "AnnealedCopper",
    "MT.NetherStar": "NetherStar",
    "MT.EnderEye": "EnderEye",
    "MT.W": "Tungsten",
    "MT.Ad": "Adamantium",
    "MT.Graphene": "Graphene",
    "MT.Magnalium": "Magnalium",
    "MT.Trinitanium": "Trinitanium",
    "MT.Vibramantium": "Vibramantium",
}

# IL.* component arrays -> port compact-component id prefix (tier suffix appended)
IL_COMPONENT = {
    "MOTORS": "compact_electric_motor_",
    "PUMPS": "compact_electric_pump_",
    "PISTONS": "compact_electric_piston_",
    "EMITTERS": "compact_signal_emitter_",
    "CONVEYERS": "compact_electric_conveyor_",
    "SENSORS": "compact_sensor_",
    "ROBOT_ARMS": "compact_robot_arm_",
    "FIELD_GENERATORS": "compact_force_field_emitter_",
}

# IL.* single items -> port item id
IL_ITEM = {
    "IL.Processor_Crystal_Diamond": "gregtech:crystal_processor_diamond",
    "IL.Processor_Crystal_Sapphire": "gregtech:crystal_processor_sapphire",
    "IL.Processor_Crystal_Ruby": "gregtech:crystal_processor_ruby",
    "IL.Processor_Crystal_Emerald": "gregtech:crystal_processor_emerald",
    "IL.Comp_Laser_Gas_Ar": "gregtech:laser_emitter_argon",
    "IL.Comp_Laser_Gas_Kr": "gregtech:laser_emitter_krypton",
    "IL.Comp_Laser_Gas_Xe": "gregtech:laser_emitter_xenon",
    "IL.Comp_Laser_Gas_He": "gregtech:laser_emitter_helium",
    "IL.Comp_Laser_Gas_Ne": "gregtech:laser_emitter_neon",
    "IL.Comp_Laser_Gas_HeNe": "gregtech:laser_emitter_heliumneon",
    "IL.Comp_Laser_Gas_CO": "gregtech:laser_emitter_carbonmonoxide",
    "IL.Comp_Laser_Gas_CO2": "gregtech:laser_emitter_carbondioxide",
    "IL.Comp_Laser_Gas_Empty": "gregtech:laser_emitter_emptygas",
}

# Vanilla / ore-dictionary references.
OD_ITEM = {
    "Blocks.brick_block": "item:minecraft:bricks",
    "OD.craftingHardenedClay": "item:minecraft:terracotta",
    "OD.blockGlassColorless": "item:minecraft:glass",
    "OD.sandstone": "tag:forge:sandstone",
    "OD.container1000honey": "item:minecraft:honey_bottle",
    "OD.itemRedstone": "item:minecraft:redstone",
    "OP.treeSapling": "tag:gregtech:saplings",
    "DYE_OREDICTS_LENS[DYE_INDEX_Yellow]": "tag:gregtech:lens",
    "DYE_OREDICTS_LENS[DYE_INDEX_White]": "tag:gregtech:lens",
    "DYE_OREDICTS_LENS[DYE_INDEX_Red]": "tag:gregtech:lens",
    "DYE_OREDICTS_LENS[DYE_INDEX_Green]": "tag:gregtech:lens",
    "DYE_OREDICTS_LENS[DYE_INDEX_Blue]": "tag:gregtech:lens",
}

# aRegistry.getItem(<id>) -> GT6 smelting crucible of a specific material.
CRUCIBLE_IDS = {
    "1005": "gregtech:smelting_crucible_ceramic",
    "1018": "gregtech:smelting_crucible_quartz",
    "1019": "gregtech:smelting_crucible_carbon",
    "1024": "gregtech:smelting_crucible_tungsten",
    "1039": "gregtech:smelting_crucible_iridium",
    "1043": "gregtech:smelting_crucible_ta4hfc5",
}

# GT6 shaped recipes let tool characters stand in for a tool without declaring a key
# (gregapi/recipes/CR.java: b/c/d/h/k/p/r/s/w/x/y). The port needs the key spelled out.
TOOL_CHARS = {
    "b": "gregtech:tool_saw",           # GT6 "blade"
    "c": "gregtech:tool_crowbar",
    "d": "gregtech:tool_screwdriver",
    "h": "gregtech:tool_hammer",
    "k": "gregtech:tool_knife",
    "p": "gregtech:tool_file",          # GT6 "drawplate": nearest port tool
    "r": "gregtech:tool_soft_hammer",
    "s": "gregtech:tool_saw",
    "w": "gregtech:tool_wrench",
    "x": "gregtech:tool_wire_cutter",
    "y": "gregtech:tool_chisel",
}

UNMAPPED: dict[str, int] = {}
UNMAPPED_KEY_SYMBOLS: dict[str, int] = {}


def translate(expr: str) -> str:
    """Translate one original key value into a runtime ingredient spec."""
    e = norm(expr)

    if e in MAT_SPECS:
        return MAT_SPECS[e]
    if e in IL_ITEM:
        return "item:" + IL_ITEM[e]
    if e in OD_ITEM:
        return OD_ITEM[e]
    if e == "OP.treeSapling":
        return OD_ITEM[e]

    m = re.fullmatch(r"OP\.(\w+)\.dat\(([^)]+)\)", e)
    if m:
        prefix, mat_expr = m.group(1), m.group(2).strip()
        if mat_expr == "aMat":
            if prefix.startswith("pipe"):
                return "pipe:" + prefix[4:].lower()
            return "mat:" + prefix
        material = FIXED_MATERIAL.get(mat_expr, mat_expr.split(".")[-1])
        if prefix.startswith("pipe"):
            return "pipe:%s@%s" % (prefix[4:].lower(), material)
        if prefix.startswith("wireGt") or prefix.startswith("cableGt"):
            size = int(re.sub(r"\D", "", prefix))
            kind = "cable" if prefix.startswith("cableGt") else "wire"
            family = {"ANY.Iron": "any_iron_or_steel", "ANY.Cu": "any_copper"}.get(mat_expr)
            if family:
                return "tag:gregtech:%s_%02d/%s" % (kind, size, family)
            return "%s:%d@%s" % (kind, size, material)
        return "mat:%s@%s" % (prefix, material)

    m = re.fullmatch(r"IL\.(\w+)\[(\d+)\]", e)
    if m and m.group(1) in IL_COMPONENT:
        return "il:%s" % m.group(1)

    m = re.fullmatch(r"MT\.DATA\.(CABLES_01|WIRES_04)\[(\d+)\]", e)
    if m:
        kind = "cabletier" if m.group(1) == "CABLES_01" else "wiretier"
        return "%s:%d" % (kind, int(m.group(2)))

    m = re.fullmatch(r"OD_CIRCUITS\[(\d+)\]", e)
    if m:
        return "circuit:%d" % int(m.group(1))

    m = re.fullmatch(r"aRegistry\.getItem\((\d+)\)", e)
    if m and m.group(1) in CRUCIBLE_IDS:
        return "item:" + CRUCIBLE_IDS[m.group(1)]
    if m:
        # GT6 multiblock part / casing registered with a numeric id.
        return "part:" + m.group(1)

    m = re.fullmatch(r"\"(gt:[a-z0-9_]+)\"", e)
    if m:
        return "oredict:" + m.group(1)

    UNMAPPED[e] = UNMAPPED.get(e, 0) + 1
    return "unmapped"


def port_tier_counts() -> dict[str, int]:
    src = (JAVA / "content/machine/BasicMachineCatalog.java").read_text(encoding="utf-8")
    known = {
        "HU_TIERS": 4,
        "RU_KU_TIERS": 4,
        "EU_MU_LU_CU_TIERS": 5,
        "SS": 1,
    }
    counts: dict[str, int] = {}
    for m in re.finditer(
        r'new MachineDef\("([a-z_0-9]+)",\s*"(\w+)",\s*(\w+|new GTMaterial\[\]\{[^}]+\})(?:,\s*\d+)?\)',
        src,
    ):
        name, tier_expr = m.group(1), m.group(3).strip()
        if tier_expr in known:
            counts[name] = known[tier_expr]
        elif "GTMaterial[]{" in tier_expr:
            counts[name] = tier_expr.count("Materials.")
        else:
            raise SystemExit("cannot determine tier count for %s (%r)" % (name, tier_expr))
    if not counts:
        raise SystemExit("No shared MachineDef rows found; refusing to generate empty machine tables")
    return counts


def main() -> int:
    data = json.loads(SOURCE_JSON.read_text(encoding="utf-8"))
    by_machine: dict[str, dict[int, dict]] = {}
    for e in data["entries"]:
        m = re.search(r"\[(\d+)\]", e["tier"])
        tier = int(m.group(1)) if m else 1
        by_machine.setdefault(e["machine"], {})[tier] = e

    counts = port_tier_counts()
    rows: list[str] = []
    missing: list[str] = []

    def emit(port_name: str, tier: int, entry: dict) -> None:
        keys = {k: translate(v) for k, v in entry["keys"].items()}
        if port_name == "melter":
            # aMat is ANY.Iron here, including the source's steel pipe/casing forms.
            keys["M"] = "tag:gregtech:casing_machine/any_iron_or_steel"
            keys["P"] = "tag:gregtech:pipe_medium/any_iron_or_steel"
        # Tool characters are implicit in GT6's shaped recipes; make them explicit here.
        for symbol in set("".join(entry["pattern"])) - {" "}:
            if symbol in keys:
                continue
            tool = TOOL_CHARS.get(symbol)
            if tool is None:
                UNMAPPED_KEY_SYMBOLS[symbol] = UNMAPPED_KEY_SYMBOLS.get(symbol, 0) + 1
                continue
            keys[symbol] = "item:" + tool
        pattern = "|".join(entry["pattern"])
        key_src = ", ".join(
            "'%s', \"%s\"" % (sym, spec) for sym, spec in sorted(keys.items())
        )
        rows.append(
            '            new Entry("%s", %d, "%s", Map.of(%s))'
            % (port_name, tier, pattern, key_src)
        )

    for port_name, tier_count in counts.items():
        if port_name in SKIP:
            continue
        tiers = TIER_NUMBERS.get(port_name, EXTRA_TIERS.get(port_name, list(range(1, tier_count + 1))))
        for tier in tiers:
            if port_name in EXPLICIT_TIERS:
                orig = EXPLICIT_TIERS[port_name][tier - 1]
            elif port_name in PER_TIER_NAME:
                orig = PER_TIER_NAME[port_name] % tier
            elif port_name in SAME_NAME:
                orig = SAME_NAME[port_name]
            else:
                missing.append(port_name)
                continue
            entry = by_machine.get(orig, {}).get(tier)
            if entry is None:
                # Per-tier machines such as "Feeder (T4)" carry no [n] in their tier
                # expression because they name a fixed material; they have one entry.
                single = by_machine.get(orig, {})
                if len(single) == 1:
                    entry = next(iter(single.values()))
            if entry is None:
                missing.append("%s (tier %d -> %s)" % (port_name, tier, orig))
                continue
            emit(port_name, tier, entry)

    # Machines GT6 registers in the "Multiblock Machines" tab.
    if MULTIBLOCK_BACKED:
        multi = json.loads(Path(__file__).with_name("gt6_multiblock_recipes.json").read_text(encoding="utf-8"))
        multi_index = {e["machine"]: e for e in multi["entries"]}
        for port_name, original in MULTIBLOCK_BACKED.items():
            entry = multi_index.get(original)
            if entry is None:
                missing.append("%s -> %s (multiblock tab)" % (port_name, original))
                continue
            emit(port_name, 1, entry)

    header = '''package com.gregtech.gregtech.data;

import java.util.List;
import java.util.Map;

/**
 * Crafting-recipe table for the single-block machines, copied from the original
 * GregTech 6 {@code Loader_MultiTileEntities} "Basic Machines" registrations.
 * <p>
 * GENERATED by {@code tools/generate_machine_recipe_table.py} - do not edit by hand.
 * Each entry keeps the original pattern rows and key symbols verbatim; ingredient
 * translation to 1.20.1 registry objects happens in {@link MachineRecipeIngredients}.
 * </p>
 *
 * @see MachineRecipeIngredients
 */
public final class BasicMachineCraftingRecipes {
    /** One machine variant: port machine type, port tier, pattern rows joined by '|' and key symbols. */
    public record Entry(String machine, int tier, String pattern, Map<Character, String> keys) {
        public String[] rows() { return pattern.split("\\\\|", -1); }
    }

    public static final List<Entry> ENTRIES = List.of(
'''
    footer = '''    );

    /** The original recipe for one port machine type and tier, or {@code null} if GT6 has none. */
    public static Entry find(String machine, int tier) {
        for (Entry entry : ENTRIES) {
            if (entry.machine().equals(machine) && entry.tier() == tier) return entry;
        }
        return null;
    }

    /** Machine types covered by this table. */
    public static java.util.Set<String> machines() {
        java.util.Set<String> names = new java.util.TreeSet<>();
        for (Entry entry : ENTRIES) names.add(entry.machine());
        return java.util.Collections.unmodifiableSet(names);
    }

    private BasicMachineCraftingRecipes() {}
}
'''
    (OUT).write_text(header + ",\n".join(rows) + "\n" + footer, encoding="utf-8", newline="\n")
    print("wrote %s (%d entries)" % (OUT, len(rows)))
    if UNMAPPED:
        print("WARNING unmapped ingredient expressions:")
        for expr, n in sorted(UNMAPPED.items(), key=lambda kv: -kv[1]):
            print("   %-50s %d" % (expr, n))
    if UNMAPPED_KEY_SYMBOLS:
        print("WARNING pattern symbols with no key and no tool mapping:")
        for symbol, n in sorted(UNMAPPED_KEY_SYMBOLS.items(), key=lambda kv: -kv[1]):
            print("   %-4s %d" % (symbol, n))
    if missing:
        print("WARNING machines without an original entry:")
        for name in sorted(set(missing)):
            print("   ", name)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
