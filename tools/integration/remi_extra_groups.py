"""Explicit viewer families for component variants and catalog-only identities.

No recipe tags or registrations are changed. Typed fluid matching deliberately
excludes item stacks with identical registry paths and other mods' fluids.
"""
from remi_food_groups import UNFIRED_CLAY, resources as food_resources


def resources(root):
    java = root / "core/src/main/java/com/gregtech/gregtech"
    # Colours are stack components/NBT on these blocks, not suffixed registry IDs.
    for stem in ("asphalt", "cfoam_fresh", "cfoam", "cfoam_slab", "concrete",
                 "concrete_reinforced", "glass_clear", "glass_glow", "glass_glow_slab", "glass_clear_slab"):
        yield "decor_" + stem, {"type": "remi:group", "name": "block.gt6ce." + stem,
                "contents": ["item:gt6ce:" + item for item in
                             {"cfoam": ("cfoam", "cfoam_hardened", "cfoam_hardened_owned"),
                              "cfoam_fresh": ("cfoam_fresh", "cfoam_fresh_owned")}.get(stem, (stem,))],
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
