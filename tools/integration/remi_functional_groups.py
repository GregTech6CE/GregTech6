"""Viewer-only functional families; exact members, not recipe tag changes.

Native block/item language identities provide the enumerated variants. Generic
material-bush IDs are generated at registration, so that one family uses regex.
Longest stems win: reinforced engines, dense boxes and basins never leak into
their normal counterpart. Recipe-viewer groups preserve all native items.
"""
import collections
import json
import re

# Functional boundaries mirror the native registries/catalogs. Do not merge the
# whole creative category (e.g. every engine, every mold-related item) into a group.
STEMS = """locker chest reinforced_wood_chest advanced_crafting_table drawer_quad
mass_storage logistics_mass_storage hopper queue_hopper scaffold
smelting_crucible crucible_faucet crucible_crossing mold mold_basin
steam_boiler strong_steam_boiler gearbox rotation_transformer rotational_pump
engine_electric engine_flux engine_rotation engine_diesel engine_steam engine_steam_strong
burning_box_solid burning_box_solid_dense burning_box_gas burning_box_gas_dense
burning_box_fluidbed burning_box_fluidbed_dense
electric_motor electric_dynamo electric_generator electric_heater electric_cooler
electric_magnet flux_motor flux_dynamo flux_heater flux_cooler flux_magnet
electric_laser flux_laser steam_turbine gas_turbine
magnet magnet_electromagnetic battery_box solar_panel
drum wood_barrel fluid_cell fluid_capsule fluid_measure
tap fluid_funnel cap_nozzle nozzle anvil bush bumble_hive
track track_booster track_detector
panel_wood panel_concrete panel_cfoam panel_asphalt
compact_electric_pump compact_electric_conveyor compact_robot_arm
burning_box_liquid burning_box_liquid_dense long_dist_wire long_dist_transformer
electromagnet energy_storage transformer
integrated_circuit sensor compact_sensor fluid_spring bookshelf bars bottle_crate beam
auto_igniter auto_hammer charging_crafting_table co2_laser laser_absorber
canvas single_use book large_book glowtus""".split()
STEMS += """quantum_energizer compact_electric_motor compact_electric_piston compact_force_field_emitter compact_signal_emitter
spray_paint leaves sapling planks log_hole grassblock robot_arm compass rope bale mortar
circuit_board circuit_part circuit_plate circuit_wire circuit_wiring crystal_circuit
magic_research_paper""".split()

def catalog(root):
    java = root / "core/src/main/java/com/gregtech/gregtech"
    en = json.loads((root / "core/src/main/resources/assets/gt6ce/lang/en_us.json").read_text(encoding="utf-8"))
    # Only actual name keys, never .tooltip entries or %s material templates.
    ids = {k.split('.')[2]: k for k, v in en.items()
           if re.fullmatch(r"(?:item|block)\.gt6ce\.[a-z0-9_]+", k) and "%" not in v}
    machine_source = (java / "content/machine/BasicMachineCatalog.java").read_text(encoding="utf-8")
    machines = set(re.findall(r'new MachineDef\("([^"]+)"', machine_source))
    materials = {k.removeprefix("material.gt6ce.").replace("_", "") for k in en if k.startswith("material.gt6ce.")}
    materials.update(v.replace("_", "") for v in re.findall(r'MAT_SHORT.put\(Materials.\w+, "([^"]+)"', machine_source))
    # Restrict basic-machine groups to exact tier/material registry identities;
    # machine parts and extruder shape items are not machines.
    stems = sorted(set(STEMS) | machines, key=lambda s: (-len(s), s))
    groups = collections.defaultdict(list)
    for item, key in ids.items():
        for stem in stems:
            if item != stem and not item.startswith(stem + "_"):
                continue
            suffix = re.sub(r"_t\d+$", "", item.removeprefix(stem + "_")).replace("_", "")
            if stem in machines and (not key.startswith("block.") or suffix not in materials):
                continue
            if stem in {"anvil"} and not key.startswith("block."):
                continue
            groups[stem].append(item)
            break
    groups["bumble_hive"].append("clay_comb")
    # Use the actual native ore definitions, not display-name or broad ore substring guesses.
    ores = (java / "block/SpecialOreDefinitions.java").read_text(encoding="utf-8")
    groups["special_ores"] = ["block_" + icon for icon in re.findall(r'new VanillaSpec\("([^"]+)"', ores)]
    groups["rock_ores"] = ["block_" + icon for icon in re.findall(r'Map.entry\("([^"]+)", new RockSpec', ores)]
    groups["crystal_ores"] = sorted(set(re.findall(r'"(crystal_ore_[a-z_]+)"', ores)))
    assert tuple(len(groups[g]) for g in ("special_ores", "rock_ores", "crystal_ores")) == (16, 9, 12)
    assert all(item in ids for g in ("special_ores", "rock_ores", "crystal_ores") for item in groups[g])

    # Axle thickness is an independent specification, just as pipe/wire size is.
    for item in ids:
        match = re.fullmatch(r"axle_.+_([1-4])", item)
        if match:
            groups["axle_" + match[1]].append(item)
    bee_source = (java / "content/bumble/GTBumbleSpecies.java").read_text(encoding="utf-8")
    species = re.findall(r'new Species\(\d+,\s*"([^"]+)"', bee_source)
    assert len(species) == 80, "Review new bumble species/state coverage"
    for state in ("dead", "drone", "princess", "queen", "scanned_dead", "scanned_drone", "scanned_princess", "scanned_queen"):
        for prefix in species:
            item = prefix + "_" + state
            assert item in ids, item
            groups["bumblebee_" + state].append(item)
    for item in ids:
        can = re.fullmatch(r"(tiny|small|tall|wide|large|huge)_food_can_.+", item)
        if can:
            groups[can[1] + "_food_can"].append(item)
    rods = (java / "content/nuclear/ReactorRodCatalog.java").read_text(encoding="utf-8")
    for item, kind in re.findall(r'new Rod\(\d+,"([^"]+)","[^"]*","[^"]*",Kind\.(\w+)', rods):
        if item in ids:
            groups["reactor_rod_" + kind.lower()].append(item)
    tech = (java / "content/recipe/TechnologicalItemDefinitions.java").read_text(encoding="utf-8")
    covers = tech.split("// ===== Covers =====", 1)[1].split("// The pump", 1)[0]
    groups["covers"] = re.findall(r'"([a-z0-9_]+)"', covers)
    logistics = (java / "content/logistics/LogisticsCoverDefinition.java").read_text(encoding="utf-8")
    groups["logistics_covers"] = re.findall(r'\("([^"]+)", Role\.', logistics)
    # Enumerate the same type x form product as both native Loader_Blocks.
    # Full blocks and slabs stay separate; rock identity never creates a group.
    stones = (java / "block/stone/StoneType.java").read_text(encoding="utf-8")
    variants = (java / "block/stone/StoneVariant.java").read_text(encoding="utf-8")
    rocks = re.findall(r'^    [A-Z_]+\("([^"]+)"', stones, re.M)
    forms = re.findall(r'^    [A-Z_]+\(\d+, "([^"]+)"', variants, re.M)
    assert len(rocks) == 27 and len(forms) == 16, "Review stone registration coverage"
    for form in forms:
        for shape in ("", "_slab"):
            items = ["stone_" + rock + "_" + form + shape for rock in rocks]
            assert all(item in ids for item in items), items
            groups["stone_" + form + shape] = items
    try:
        from .remi_catalog_groups import extend
    except ImportError:
        from remi_catalog_groups import extend
    extend(root, groups, ids)
    return {g: sorted(set(items)) for g, items in sorted(groups.items()) if len(set(items)) > 1}, ids

def resources(root):
    groups, ids = catalog(root)
    from remi_group_titles import headings
    language = json.loads((root / "core/src/main/resources/assets/gt6ce/lang/en_us.json").read_text(encoding="utf-8"))
    titles = headings(root, language)
    for group, items in groups.items():
        chosen = next((i for i in items if i.endswith("_bronze")), items[0])
        if group.startswith("stone_"):
            chosen = "stone_basalt_" + group.removeprefix("stone_")
        # Reuse an exact existing label, including its source-bound translation.
        # The representative's material never limits membership of the group.
        title_key = "emi.group.gt6ce.functional_" + group
        yield "functional_" + group, {"type": "remi:group", "name": title_key if title_key in titles else ids[chosen],
                "contents": ["item:gt6ce:" + item for item in items], "priority": 110}
    yield "functional_material_bush", {"type": "remi:group", "name": "block.gt6ce.bush",
            "regex": "^gt6ce:bush_plant_gt_berry_[a-z0-9_]+$", "priority": 110}
    yield "fluid_item", {"type": "remi:group", "name": "itemGroup.gt6ce.fluids",
            "regex": "^gt6ce:fluid_item_[a-z0-9_]+$", "priority": 110}
    from remi_extra_groups import resources as extra_resources
    yield from extra_resources(root)
