"""Non-prefix families. Structural IDs come from the original part catalog.

Only viewer grouping is affected; forms, grades and recipe ingredients keep
their native identity. In particular machine parts are never basic machines.
"""
import re


def extend(root, groups, ids):
    java = root / "core/src/main/java/com/gregtech/gregtech"
    # A translated sprite name is not a registered gearbox. Match the same 13 tiers
    # used by both native registries; never include the obsolete bare "gearbox" ID.
    gearboxes = (java / "content/energy/GearboxCatalog.java").read_text(encoding="utf-8")
    groups["gearbox"] = ["gearbox_" + suffix for suffix in re.findall(r'new Tier\("([^"]+)"', gearboxes)]
    assert len(groups["gearbox"]) == 13 and all(item in ids for item in groups["gearbox"])
    def select(group, pattern):
        groups[group] = [i for i in ids if re.fullmatch(pattern, i)]

    for group, pattern in {
        "long_dist_pipe_fluid": r"long_dist_pipe_(?:fluid|adamantium|draconium|tungsten)",
        "laser_emitters": r"(?:laser_emitter|laser_emitter_co2|co2_laser)(?:_.+)?",
        "batteries": r"(?:battery_(?!box).+|rechargeable_battery_.+)",
        "filters": r"filter_(?:items|fluids|items_fluids|oredict)",
        "wall_spikes": r"spike_.+",
        "logs": r"log_(?!hole_|sap_|resin_).+",
        "log_hole": r"(?:log_(?:hole|sap|resin)_.+|tapped_.+)",
        "crystal_processors": r"crystal_processor_.+",
        "loot_items": r"(?:loot_bottle|clouded_bottle|gem_pouch|seed_pouch|loot_pouch|bagged_sapling|dusty_guide_book|dusty_material_dictionary)",
        "wood_slabs": r"slab_.+",
        "food_shapes": r"(?:slicer_shape|foodmold_shape)_.+",
        "indicator_flowers": r"flower_.+",
        "circuit": r"circuit_(?:basic|good|advanced|elite|master|ultimate|signalum|enderium|magic)",
        "circuit_wire": r"circuit_(?:wire|wiring)_.+",
        "keys": r"(?:key_(?:brass|bronze|copper|gold|iron|lead|plastic|platinum|silver|tin)|(?:brass|bronze|copper|gold|iron|lead|plastic|platinum|silver|tin)_key)",
        "clay_blocks": r"clay_(?:blue|brown|red|white|yellow)",
        "clay_balls": r"(?:blue|brown|red|white|yellow)_clay",
        "bridges": r"(?:inventory|inventory_tank|tank|universal)_bridge",
        "usb_sticks": r"usb[1-4]_stick",
        "usb_cables": r"usb[1-4]_cable",
        "usb_hdd": r"usb[1-4]_hdd",
        "repeaters": r"(?:extender_(?:basic|advanced|elite|wireless)|(?:inventory|inventory_tank|tank|universal)_extender)",
        "dynamite": r"(?:boomstick|(?:strong_)?dynamite(?:_.+)?)",
        "fluid_measure": r"(?:fluid_measure|fluid_measuring_pot)(?:_.+)?",
        "barometer_bottle": r"fluid_barometer_gas_cylinder(?:_.+)?",
    }.items():
        select(group, pattern)
    # Merge the two spellings of etched wiring, not the motherboards/parts.
    groups.pop("circuit_wiring", None)
    parts = (java / "content/multiblock/OriginalMultiblockPartData.java").read_text(encoding="utf-8")
    for number, path in re.findall(r'new Part\((\d+), "([^"]+)"', parts):
        n = int(number)
        family = ("walls" if 18001 <= n <= 18012 else "dense_walls" if 18022 <= n <= 18032
                  else "coils" if 18040 <= n <= 18045 else None)
        if family and path in ids:
            groups[family].append(path)
    controllers = (java / "content/multiblock/SharedLargeMachineParts.java").read_text(encoding="utf-8")
    for number, path in re.findall(r'new Part\((\d+),"([^"]+)"', controllers):
        n = int(number)
        family = ("multiblock_tanks" if 17001 <= n <= 17007 or 17042 <= n <= 17047
                  else "multiblock_dense_tanks" if 17022 <= n <= 17027 or 17062 <= n <= 17067
                  else "large_crucibles" if 17302 <= n <= 17312
                  else "gas_turbine_cores" if 17231 <= n <= 17234
                  else "steam_turbine_cores" if 17211 <= n <= 17214
                  else "generator_cores" if 17221 <= n <= 17224 else None)
        if family and path in ids:
            groups[family].append(path)
    for group, pattern in {
        "generator_cores": r"large_dynamo_(?!wall$).+",
        "gas_turbine_cores": r"large_gas_turbine_(?!wall$).+",
        "steam_turbine_cores": r"large_(?:steam_turbine_(?!wall$).+|turbine_main)",
        "large_crucibles": r"large_crucible_main",
        "multiblock_tanks": r"tank_3x3",
        "multiblock_dense_tanks": r"tank_5x5",
    }.items():
        groups[group].extend(i for i in ids if re.fullmatch(pattern, i))
