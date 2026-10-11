#!/usr/bin/env python3
"""Generate MultiblockCraftingRecipes.java from GT6's Multiblock Machines tab and Logistics Core.

The port registers the GregTech 6 multiblock machines either as single blocks (the legacy
`large*` machine family) or as controller blocks (`*_main`). Neither had a crafting recipe:
the Basic Machines pack skips them and no resource-pack recipe exists. This generator maps
each GT6 multiblock registration onto the port block that stands in for it.

A target is emitted only when every original key resolves to a known 1.20.1 counterpart;
anything else is reported instead of being invented.

Inputs : tools/gt6_multiblock_recipes.json
Output : src/main/java/com/gregtech/gregtech/data/MultiblockCraftingRecipes.java
"""

from __future__ import annotations

import json
import re
from pathlib import Path

JAVA = Path(__file__).resolve().parents[1] / "src/main/java/com/gregtech/gregtech"
SOURCE_JSON = Path(__file__).with_name("gt6_multiblock_recipes.json")
LOGISTICS_SOURCE_JSON = Path(__file__).with_name("gt6_logistics_core_recipe.json")
LOGISTICS_WIRE_JSON = Path(__file__).with_name("gt6_logistics_wire_recipe.json")
OUT = JAVA / "data/MultiblockCraftingRecipes.java"

# GT6 part id -> port block id, for the parts this port already provides.
PART_BLOCKS = {
    "24900": "laser_fiber_wire",         # GT6 logistics wire uses the laser-fiber connector
    "18000": "coke_oven_wall",              # GT6 "Fire Bricks"
    "18002": "tank_wall",                   # GT6 "Stainless Steel Wall"
    "18003": "tungstensteel_wall",
    "18006": "titanium_wall",
    "18007": "invar_wall",
    "18008": "galvanized_steel_wall",
    "18009": "steel_wall",
    "18022": "tank_wall_dense",             # GT6 "Dense Stainless Steel Wall"
    "18023": "implosion_compressor_wall",
    "18024": "dense_tungsten_wall",
    "18025": "dense_adamantium_wall",
    "18026": "dense_titanium_wall",
    "18031": "dense_lead_wall",
    "18100": "centrifuge_part",
    "18101": "heat_transmitter",
    "18102": "distillation_tower_part",
    "18105": "electrolyzer_part",
}

# GT6 machine name -> (port block ids, structural block for the "M" key).
# The legacy `large*` block ids are resolved from BasicMachineDefinitions at generation time
# (their casing material differs per machine: see legacy_block_ids()).
TARGETS = {
    # BasicMachineDefinitions excludes the obsolete single-block variants of these machines;
    # each exists only as its *_main controller in this port.
    "Distillation Tower": (["distillation_tower_main"], "distillation_tower_part"),
    "Cryo Distillation Tower": (["cryo_distillation_main"], "cryo_distillation_wall"),
    "Fusion Reactor": (["fusion_reactor_main"], "fusion_reactor_wall"),
    "Large Centrifuge": ([], "centrifuge_part"),
    "Large Electrolyzer": ([], "electrolyzer_part"),
    "Large Coagulator Array": ([], "tank_wall"),
    "Large Autoclave": ([], "tank_wall_dense"),
    "Large Bathing Vat": ([], "tank_wall"),
    "Large Batch Mixer": ([], "tank_wall"),
    "Large Fermenter": ([], "tank_wall"),
    "Large Electric Oven": ([], "invar_wall"),
    "Large Sluice": ([], "titanium_wall"),
    "Large Crusher": ([], "tungstensteel_wall"),
    "Large Shredder": ([], "tungstensteel_wall"),
    "Large Squeezer": ([], "steel_wall"),
    "Large Matter Fabricator": ([], "dense_lead_wall"),
    "Large Heat Exchanger": (["heat_exchanger_main"], "dense_tungsten_wall"),
    "Bedrock Mining Drill Controller": (["bedrock_drill_main"], "bedrock_drill_wall"),
    "Lightning Rod Electric Output": (["lightning_rod_main"], "lightning_rod_wall"),
    "Coke Oven": (["coke_oven_main"], "coke_oven_wall"),
    "Implosion Compressor": (["implosion_compressor_main"], "implosion_compressor_wall"),
    # Boiler barometers and dynamo housings come in one GT6 variant per material; the port
    # registers one boiler and four dynamo grades, so only the matching variants are mapped.
    "Stainless Steel Boiler Main Barometer": (["large_boiler_main"], "boiler_wall"),
    "Stainless Steel Dynamo Main Housing": (["large_dynamo_main"], "large_dynamo_wall"),
    "Titanium Dynamo Main Housing": (["large_dynamo_titanium"], "large_dynamo_wall"),
    "Tungstensteel Dynamo Main Housing": (["large_dynamo_tungstensteel"], "large_dynamo_wall"),
    "Adamantium Dynamo Main Housing": (["large_dynamo_adamantium"], "large_dynamo_wall"),
    # The twelve old axial *_main blocks are conversion-only aliases. Their direct recipes
    # used generic walls and tier-1 components, bypassing the original 172xx progression.
    "Large Stainless Steel Crucible": (["large_crucible_main"], "large_crucible_wall"),
}

# Legacy axial controllers must first be crafted as their GT6 172xx originals. In particular
# the four original gas turbine recipes consume the matching 17211-17214 steam turbine.
AXIAL_LEGACY_CONVERSIONS = {
    "large_turbine_main": ("Magnalium Steam Turbine Main Housing", 17211),
    "large_steam_turbine_trinitanium": ("Trinitanium Steam Turbine Main Housing", 17212),
    "large_steam_turbine_graphene": ("Graphene Steam Turbine Main Housing", 17213),
    "large_steam_turbine_vibramantium": ("Vibramantium Steam Turbine Main Housing", 17214),
    "large_dynamo_main": ("Stainless Steel Dynamo Main Housing", 17221),
    "large_dynamo_titanium": ("Titanium Dynamo Main Housing", 17222),
    "large_dynamo_tungstensteel": ("Tungstensteel Dynamo Main Housing", 17223),
    "large_dynamo_adamantium": ("Adamantium Dynamo Main Housing", 17224),
    "large_gas_turbine_main": ("Magnalium Gas Turbine Main Housing", 17231),
    "large_gas_turbine_trinitanium": ("Trinitanium Gas Turbine Main Housing", 17232),
    "large_gas_turbine_graphene": ("Graphene Gas Turbine Main Housing", 17233),
    "large_gas_turbine_vibramantium": ("Vibramantium Gas Turbine Main Housing", 17234),
}

# GT6 machine name -> port machine type, for the legacy `large*` family whose block id carries
# the casing material (resolved from BasicMachineDefinitions).
LEGACY_MACHINE_TYPES = {
    "Large Centrifuge": "largecentrifuge",
    "Large Electrolyzer": "largeelectrolyzer",
    "Large Coagulator Array": "largecoagulator",
    "Large Autoclave": "largeautoclave",
    "Large Bathing Vat": "largebath",
    "Large Batch Mixer": "largemixer",
    "Large Fermenter": "largefermenter",
    "Large Electric Oven": "largeoven",
    "Large Sluice": "largesluice",
    "Large Crusher": "largecrusher",
    "Large Shredder": "largeshredder",
    "Large Squeezer": "largesqueezer",
    "Large Matter Fabricator": "largemassfab",
}

# Multiblock parts this port also registers as blocks of their own, mapped 1:1.
PART_TARGETS = {
    # Wall prerequisites for the 170xx tank valves. Wood uses a dedicated JSON because
    # GT6's WoodTreated plate is a plank BlockItem, not a MaterialItem indexed by the pack.
    "Tungsten Wall": "tungsten_wall",
    "Adamantium Wall": "adamantium_wall",
    "Centrifuge Part": "centrifuge_part",
    "Electrolyzer Part": "electrolyzer_part",
    "Heat Transmitter": "heat_transmitter",
    "Distillation Tower Part": "distillation_tower_part",
    "Sluice Part": "sluice_part",
    "Crusher Wheels": "crusher_wheels",
    "Shredder Blades": "shredder_blades",
    "Large Niobium-Titanium Coil": "large_niobium_titanium_coil",
    "Large Nichrome Coil": "large_nichrome_coil",
    "Large Carborundum Coil": "large_carborundum_coil",
    "Large Osmium Coil": "large_osmium_coil",
    "Large Iridium Coil": "large_iridium_coil",
    "Versatile Quadcore Processor Unit": "versatile_processor_unit",
    "Logic Quadcore Processor Unit": "logic_processor_unit",
    "Control Quadcore Processor Unit": "control_processor_unit",
    "Conversion Quadcore Processor Unit": "conversion_processor_unit",
    "Ventilation Unit": "fusion_ventilation_unit",
}

# ── key translation (mirrors generate_machine_recipe_table.py) ─────────────────

MAT_SPECS = {
    "OP.casingMachine.dat(aMat)": "block:casingMachine",
    "OP.casingMachineDouble.dat(aMat)": "block:casingMachineDouble",
    "OP.casingMachineQuadruple.dat(aMat)": "block:casingMachineQuadruple",
    "OP.casingMachineDense.dat(aMat)": "block:casingMachineDense",
    "OP.plate.dat(aMat)": "mat:plate",
    "OP.plateDouble.dat(aMat)": "mat:plateDouble",
    "OP.plateTriple.dat(aMat)": "mat:plateTriple",
    "OP.plateQuadruple.dat(aMat)": "mat:plateQuadruple",
    "OP.plateQuintuple.dat(aMat)": "mat:plateQuintuple",
    "OP.plateDense.dat(aMat)": "mat:plateDense",
    "OP.blockPlate.dat(aMat)": "block:blockPlate",
    "OP.gearGt.dat(aMat)": "mat:gearGt",
    "OP.gearGtSmall.dat(aMat)": "mat:gearGtSmall",
    "OP.stick.dat(aMat)": "mat:stick",
    "OP.stickLong.dat(aMat)": "mat:stickLong",
    "OP.screw.dat(aMat)": "mat:screw",
    "OP.pipeTiny.dat(aMat)": "pipe:tiny",
    "OP.pipeSmall.dat(aMat)": "pipe:small",
    "OP.pipeMedium.dat(aMat)": "pipe:medium",
    "OP.pipeLarge.dat(aMat)": "pipe:large",
    "OP.pipeHuge.dat(aMat)": "pipe:huge",
    "OP.pipeNonuple.dat(aMat)": "pipe:nonuple",
    "OP.wireGt01.dat(aMat)": "wire:1",
    "OP.wireGt02.dat(aMat)": "wire:2",
    "OP.wireGt04.dat(aMat)": "wire:4",
    "OP.wireGt16.dat(aMat)": "wire:16",
    "OP.gem.dat(ANY.Diamond)": "mat:gem@Diamond",
    "OP.plateGem.dat(ANY.Diamond)": "mat:plateGem@Diamond",
    "OP.toolHeadDrill.dat(MT.TungstenSteel)": "mat:toolHeadDrill@Tungstensteel",
}

FIXED_MATERIAL = {
    "ANY.Plastic": "Plastic",
    "ANY.Emerald": "Emerald",
    "MT.Os": "OsmiumElemental",
    "ANY.Cu": "Copper",
    "ANY.Steel": "Steel",
    "ANY.Iron": "Iron",
    "ANY.W": "Tungsten",
    "MT.StainlessSteel": "StainlessSteel",
    "MT.SteelGalvanized": "SteelGalvanized",
    "MT.Invar": "Invar",
    "MT.TungstenSteel": "Tungstensteel",
    "MT.TungstenCarbide": "TungstenCarbide",
    "MT.W": "Tungsten",
    "MT.Pb": "Lead",
    "MT.Ceramic": "Ceramic",
    "MT.Ad": "Adamantium",
    "MT.AnnealedCopper": "AnnealedCopper",
    "MT.Nichrome": "Nichrome",
    "MT.NiobiumTitanium": "NiobiumTitanium",
    "MT.Ir": "Iridium",
    "MT.Ti": "Titanium",
    "MT.Pt": "Platinum",
    "MT.SiC": "Carborundum",
    "MT.Graphene": "Graphene",
    "MT.Magnalium": "Magnalium",
    "MT.Trinitanium": "Trinitanium",
    "MT.Vibramantium": "Vibramantium",
}

IL_ITEMS = {
    "IL.Processor_Crystal_Ruby": "gregtech:crystal_processor_ruby",
    "IL.Processor_Crystal_Sapphire": "gregtech:crystal_processor_sapphire",
    "IL.Processor_Crystal_Diamond": "gregtech:crystal_processor_diamond",
    "IL.Processor_Crystal_Emerald": "gregtech:crystal_processor_emerald",
    "IL.Cover_Vent": "gregtech:air_vent",
}

VANILLA_GEMS = {
    "OP.gem.dat(MT.NetherStar)": "item:minecraft:nether_star",
    "OP.gem.dat(MT.EnderEye)": "item:minecraft:ender_eye",
}

IL_COMPONENTS = {
    "MOTORS": "compact_electric_motor_",
    "PUMPS": "compact_electric_pump_",
    "PISTONS": "compact_electric_piston_",
    "EMITTERS": "compact_signal_emitter_",
    "CONVEYERS": "compact_electric_conveyor_",
    "SENSORS": "compact_sensor_",
    "ROBOT_ARMS": "compact_robot_arm_",
    "FIELD_GENERATORS": "compact_force_field_emitter_",
}

OD_ITEMS = {
    "Blocks.brick_block": "item:minecraft:bricks",
}

TOOL_CHARS = {
    "d": "gregtech:tool_screwdriver",
    "h": "gregtech:tool_hammer",
    "r": "gregtech:tool_soft_hammer",
    "s": "gregtech:tool_saw",
    "w": "gregtech:tool_wrench",
    "x": "gregtech:tool_wire_cutter",
    "y": "gregtech:tool_chisel",
}

UNMAPPED: dict[str, int] = {}


def norm(expr: str) -> str:
    e = expr.strip()
    e = re.sub(r"\s+\.", ".", e)
    e = re.sub(r"\.\s*dat", ".dat", e)
    e = re.sub(r"\(\s*", "(", e)
    e = re.sub(r"\s*\)", ")", e)
    e = re.sub(r"\s+", " ", e)
    return e


def translate(expr: str, default_material: str | None = None) -> str | None:
    """Translate one GT6 key value; {@code default_material} replaces {@code aMat}."""
    e = norm(expr)
    if e in MAT_SPECS:
        spec = MAT_SPECS[e]
        if default_material and "@" not in spec and spec.split(":")[0] in ("mat", "block", "pipe", "wire"):
            return spec + "@" + default_material
        return spec
    if e in IL_ITEMS:
        return "item:" + IL_ITEMS[e]
    if e in VANILLA_GEMS:
        return VANILLA_GEMS[e]
    if e in OD_ITEMS:
        return OD_ITEMS[e]

    m = re.fullmatch(r"OP\.(\w+)\.dat\((aMat|[^)]+)\)", e)
    if m:
        prefix, mat_expr = m.group(1), m.group(2).strip()
        material = default_material if mat_expr == "aMat" else FIXED_MATERIAL.get(mat_expr)
        if material is None:
            return None
        if prefix.startswith("pipe"):
            return "pipe:%s@%s" % (prefix[4:].lower(), material)
        if prefix.startswith("wireGt") or prefix.startswith("cableGt"):
            size = int(re.sub(r"\D", "", prefix))
            kind = "cable" if prefix.startswith("cableGt") else "wire"
            return "%s:%d@%s" % (kind, size, material)
        if prefix == "blockPlate":
            return "block:blockPlate@%s" % material
        return "mat:%s@%s" % (prefix, material)

    m = re.fullmatch(r"IL\.(\w+)\[(\d+)\]", e)
    if m and m.group(1) in IL_COMPONENTS:
        return "il:%s" % m.group(1)

    m = re.fullmatch(r"OD_CIRCUITS\[(\d+)\]", e)
    if m:
        return "circuit:%d" % int(m.group(1))

    m = re.fullmatch(r"aRegistry\.getItem\((\d+)\)", e)
    if m:
        block = PART_BLOCKS.get(m.group(1))
        return "item:gregtech:" + block if block else None

    m = re.fullmatch(r'"?(gt:re-battery\d+)"?', e)
    if m:
        return "oredict:" + m.group(1)

    UNMAPPED[e] = UNMAPPED.get(e, 0) + 1
    return None


def legacy_block_ids() -> dict[str, list[str]]:
    """Port block id of every legacy `large*` machine, read from BasicMachineDefinitions."""
    src = (JAVA / "content/machine/BasicMachineDefinitions.java").read_text(encoding="utf-8")
    short = {m.group(1): m.group(2) for m in re.finditer(
        r'MAT_SHORT\.put\(Materials\.(\w+),\s*"([a-z_0-9]+)"\)', src)}
    result: dict[str, list[str]] = {}
    for m in re.finditer(r'new MachineDef\("([a-z_0-9]+)",\s*"\w+",\s*([^,]+?),\s*m ->', src):
        name, tier_expr = m.group(1), m.group(2).strip()
        if name not in LEGACY_MACHINE_TYPES.values():
            continue
        if tier_expr == "SS":
            suffix = "stainless_steel"
        else:
            material = re.search(r"Materials\.(\w+)", tier_expr)
            if not material:
                continue
            material_name = material.group(1)
            # MAT_SHORT covers the tier-array materials; anything else falls back to the
            # lowercased material name, exactly like BasicMachineDefinitions does.
            suffix = short.get(material_name, material_name.lower())
        result.setdefault(name, []).append("%s_%s" % (name, suffix))
    return result


def original_part_ids() -> dict[str, str]:
    """Read numeric GT6 ids at the registry boundary, avoiding a second block-name table."""
    src = (JAVA / "content/multiblock/LargeMachineParts.java").read_text(encoding="utf-8")
    return {original_id: block_id for original_id, block_id in re.findall(
        r'new Part\(\s*(\d+)\s*,\s*"([a-z_0-9]+)"', src)}


def main() -> int:
    entries = {e["machine"]: e for e in json.loads(SOURCE_JSON.read_text(encoding="utf-8"))["entries"]}
    entries.update({e["machine"]: e for e in json.loads(LOGISTICS_SOURCE_JSON.read_text(encoding="utf-8"))["entries"]})
    entries.update({e["machine"]: e for e in json.loads(LOGISTICS_WIRE_JSON.read_text(encoding="utf-8"))["entries"]})
    original_parts = original_part_ids()
    for original_id, block_id in original_parts.items():
        # GT6 valve recipes refer to their wall (180xx) or prior valve (170xx) by number.
        if 17001 <= int(original_id) <= 17067 or 18001 <= int(original_id) <= 18027:
            PART_BLOCKS.setdefault(original_id, block_id)
    legacy = legacy_block_ids()
    rows: list[str] = []
    skipped: list[str] = []

    def build(gt6_name: str, block_ids: list[str], structural: str, allow_m_override: bool,
              own_material: str | None) -> None:
        entry = entries.get(gt6_name)
        if entry is None:
            skipped.append("%s (no GT6 registration)" % gt6_name)
            return
        # GT6 keys resolve against the machine's own casing material; for the structural parts
        # this port keeps them as their own blocks, so the original "aMat" is spelled out.
        material = FIXED_MATERIAL.get(entry["tier"].strip()) or own_material
        keys: dict[str, str] = {}
        # Original17104 explicitly references IL.ROBOT_ARMS[2] (MV), independent of the one-tier hull.
        fixed_robot_arm = gt6_name == "Large Bathing Vat"
        for symbol, value in entry["keys"].items():
            if fixed_robot_arm and norm(value) == "IL.ROBOT_ARMS[2]":
                keys[symbol] = "item:gregtech:compact_robot_arm_mv"
                continue
            if allow_m_override and symbol == "M":
                keys[symbol] = "item:gregtech:" + structural
                continue
            spec = translate(value, material)
            if spec is None:
                skipped.append("%s (key %s = %s)" % (gt6_name, symbol, value))
                return
            keys[symbol] = spec
        for symbol in set("".join(entry["pattern"])) - {" "}:
            if symbol in keys:
                continue
            tool = TOOL_CHARS.get(symbol)
            if tool is None:
                skipped.append("%s (pattern symbol %r)" % (gt6_name, symbol))
                return
            keys[symbol] = "item:" + tool
        pattern = "|".join(entry["pattern"])
        key_src = ", ".join("'%s', \"%s\"" % (s, v) for s, v in sorted(keys.items()))
        for block_id in block_ids:
            rows.append(
                '            new Entry("%s", "%s", "%s", Map.of(%s))'
                % (block_id, gt6_name.replace('"', '\\"'), pattern, key_src)
            )

    for gt6_name, (blocks, structural) in TARGETS.items():
        # The original 17221 dynamo main was formerly the source for the old alias, too.
        # It now has its own recipe, and the alias is handled by the conversion loop below.
        blocks = [block for block in blocks if block not in AXIAL_LEGACY_CONVERSIONS]
        if not blocks and gt6_name not in LEGACY_MACHINE_TYPES:
            continue
        machine_type = LEGACY_MACHINE_TYPES.get(gt6_name)
        if machine_type:
            resolved = legacy.get(machine_type)
            if not resolved:
                skipped.append("%s (legacy block id not found in BasicMachineDefinitions)" % gt6_name)
                continue
            blocks = resolved
        build(gt6_name, blocks, structural, True, None)
    # Every registered GT6 170xx valve is craftable. Source M keys resolve to the exact
    # material wall for small valves and to the previous small valve for large valves.
    for original_id, block_id in original_parts.items():
        number = int(original_id)
        if not (17001 <= number <= 17007 or 17022 <= number <= 17027
                or 17042 <= number <= 17047 or 17062 <= number <= 17067):
            continue
        source = next((entry for entry in entries.values()
                       if entry.get("registryId") == original_id), None)
        if source is None:
            skipped.append("GT6 tank valve %s (no GT6 registration)" % original_id)
            continue
        build(source["machine"], [block_id], "", False, None)
    # 17996's M is a galvanized machine casing, not a multiblock wall.
    build("Von da Graagg Generator", [original_parts["17996"]], "", False, None)
    # Compatibility blocks are reachable only after crafting the original 172xx controller.
    for legacy_id, (original_name, original_id) in AXIAL_LEGACY_CONVERSIONS.items():
        original_block = original_parts[str(original_id)]
        rows.append('            new Entry("%s", "%s (legacy conversion)", "M", '
                    'Map.of(\'M\', "item:gregtech:%s"))'
                    % (legacy_id, original_name, original_block))
    # The original numeric controllers are separate craftable blocks. Their M ingredient is
    # the machine casing from Loader_MultiTileEntities, not the replacement *_main wall.
    build("Lightning Rod Electric Output", ["lightning_rod_electric_output"], "", False, None)
    build("Bedrock Mining Drill Controller", ["bedrock_mining_drill_controller"], "", False, None)
    for gt6_name, block_id in PART_TARGETS.items():
        build(gt6_name, [block_id], "", False, None)
    # GT6 registers this controller under the Logistics tab; its M is a galvanized
    # machine casing, not a multiblock wall. Keep that key rather than overriding it.
    build("Logistics Core", ["logistics_core"], "", False, None)
    build("Logistics Wire", ["logistics_wire"], "", False, None)

    header = '''package com.gregtech.gregtech.data;

import java.util.List;
import java.util.Map;

/**
 * Crafting recipes for the multiblock machines and their structural parts, copied from the
 * original GregTech 6 {@code Loader_MultiTileEntities} Multiblock Machines and Logistics registrations.
 * <p>
 * GENERATED by {@code tools/generate_multiblock_recipe_table.py} - do not edit by hand.
 * Keys use the spec grammar of {@link BasicMachineCraftingRecipes} and are resolved by
 * {@link MachineRecipeIngredients}. Original 170xx/17996 controllers retain their GT6
 * ingredients; old axial aliases use one-item conversions from their 172xx controllers.
 * </p>
 */
public final class MultiblockCraftingRecipes {
    /** One craftable block: port id, GT6 name or legacy conversion, pattern and keys. */
    public record Entry(String blockId, String originalName, String pattern, Map<Character, String> keys) {
        public String[] rows() { return pattern.split("\\\\|", -1); }
    }

    public static final List<Entry> ENTRIES = List.of(
'''
    footer = '''    );

    /** Recipe entries for one port block id (a block can have at most one). */
    public static Entry find(String blockId) {
        for (Entry entry : ENTRIES) {
            if (entry.blockId().equals(blockId)) return entry;
        }
        return null;
    }

    /** Original GT6 registration names covered by this table. */
    public static java.util.Set<String> coveredMachines() {
        java.util.Set<String> names = new java.util.TreeSet<>();
        for (Entry entry : ENTRIES) names.add(entry.originalName());
        return java.util.Collections.unmodifiableSet(names);
    }

    private MultiblockCraftingRecipes() {}
}
'''
    OUT.write_text(header + ",\n".join(rows) + "\n" + footer, encoding="utf-8", newline="\n")
    print("wrote %s (%d entries)" % (OUT, len(rows)))
    if UNMAPPED:
        print("NOTE ingredient expressions seen but not translatable:")
        for expr, n in sorted(UNMAPPED.items(), key=lambda kv: -kv[1]):
            print("   %-46s %d" % (expr, n))
    if skipped:
        print("SKIPPED targets (kept without a recipe rather than inventing ingredients):")
        for name in sorted(set(skipped)):
            print("   ", name)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
