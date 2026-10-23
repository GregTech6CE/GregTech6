"""Explicit viewer families for component variants and catalog-only identities.

No recipe tags or registrations are changed. Typed fluid matching deliberately
excludes item stacks with identical registry paths and other mods' fluids.
"""
from remi_food_groups import UNFIRED_CLAY, resources as food_resources


def resources(root):
    java = root / "core/src/main/java/com/gregtech/gregtech"
    tiers = ("ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "xv")
    for stem, label in (("crystal_charger", "itemGroup.gt6ce.crystal_chargers"),
                        ("large_crystal_charger", "emi.group.gt6ce.large_crystal_chargers")):
        yield "functional_" + stem + "s", {
            "type": "remi:group", "name": label,
            "contents": ["item:gt6ce:" + stem + "_" + tier for tier in tiers], "priority": 135}
    yield "functional_energy_crystals", {
        "type": "remi:group", "name": "emi.group.gt6ce.energy_crystals", "priority": 135,
        "contents": ["item:gt6ce:" + form + "_" + material
                     for material in ("energiumred", "energiumcyan")
                     for form in ("gem_chipped", "gem_flawed", "gem", "gem_flawless", "gem_exquisite", "gem_legendary")]}
    import re
    barrels = (java / "content/storage/ItemBarrelCatalog.java").read_text(encoding="utf-8")
    yield "functional_item_barrels", {
        "type": "remi:group", "name": "emi.group.gt6ce.item_barrels", "priority": 135,
        "contents": ["item:gt6ce:" + item for item in re.findall(r'new Spec\("([^"]+)"', barrels)]}
    from remi_catalog_groups import wooden_bucket_variants
    for content, members in wooden_bucket_variants(root).items():
        yield "functional_wooden_bucket_" + content, {
            "type": "remi:group", "name": "emi.group.gt6ce.wooden_bucket_" + content,
            "contents": ["item:gt6ce:" + item for item in members], "priority": 125}
    yield "functional_power_cells", {
        "type": "remi:group", "name": "itemGroup.gt6ce.portable_power_cells",
        "contents": ["item:gt6ce:" + name for name in (
            "power_cell_empty", "power_cell_hydrogen",
            "aneutronic_fusion_power_cell_empty", "aneutronic_fusion_power_cell_helium_3")],
        "priority": 110}
    # Sixteen registered color identities; unsuffixed palette IDs are load compatibility only.
    for stem in ("asphalt", "cfoam_fresh", "cfoam", "cfoam_slab", "concrete",
                 "concrete_reinforced", "glass_clear", "glass_glow", "glass_glow_slab", "glass_clear_slab"):
        yield "decor_" + stem, {"type": "remi:group", "name": "block.gt6ce." + stem,
                "contents": ["item:gt6ce:" + item for item in
                             {"cfoam": ("cfoam", "cfoam_hardened", "cfoam_hardened_owned"),
                              "cfoam_fresh": ("cfoam_fresh", "cfoam_fresh_owned")}.get(stem, (stem,))]
                    + ["item:gt6ce:" + stem + "_" + color for color in
                       ("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")],
                "priority": 120}
    for stem, name in (("extruder_shape", "item.gt6ce.extruder_shape_bottle"),
                       ("low_heat_extruder_shape", "item.gt6ce.low_heat_extruder_shape_bottle"),
                       ("loot_chest", "block.gt6ce.loot_chest_dungeon")):
        yield "functional_" + stem, {"type": "remi:group", "name": name,
                "regex": "^item:gt6ce:" + stem + "_[a-z0-9_]+$", "priority": 120}
    yield from food_resources(java)
    # Raw clay molds have unique IDs; the fired mold uses shape data on one ID.
    yield "functional_clay_molds", {"type": "remi:group", "name": "item.gt6ce.clay_mold",
            "regex": "^item:gt6ce:" + UNFIRED_CLAY + "$", "priority": 120}
    yield "glass_panes", {"type": "remi:group", "name": "block.minecraft.glass_pane",
            "regex": "^item:minecraft:(?:[a-z_]+_stained_)?glass_pane$", "priority": 120}
    yield "paper", {"type": "remi:group", "name": "item.minecraft.paper",
            "contents": ["item:gt6ce:plate_paper"], "priority": 120}
    yield "real_fluids", {"type": "remi:group", "name": "itemGroup.gt6ce.fluids",
            "regex": "^fluid:gt6ce:(?!fluid_item_)[a-z0-9_./-]+$", "priority": 120}
