"""Explicit viewer families for component variants and catalog-only identities.

No recipe tags or registrations are changed. Typed fluid matching deliberately
excludes item stacks with identical registry paths and other mods' fluids.
"""
import re

UNFIRED_CLAY = r"clay_(?:(?:[a-z0-9_]+_)?mold|tap|funnel|jug|measuring_pot|juicer|bowl|basin|crossing|crucible|faucet)"


def resources(root):
    java = root / "core/src/main/java/com/gregtech/gregtech"
    # Colours are stack components/NBT on these blocks, not suffixed registry IDs.
    for stem in ("asphalt", "cfoam_fresh", "cfoam", "cfoam_slab", "concrete",
                 "concrete_reinforced", "glass_clear", "glass_glow", "glass_glow_slab", "glass_clear_slab"):
        yield "decor_" + stem, {"type": "remi:group", "name": "block.gregtech." + stem,
                "contents": ["item:gregtech:" + item for item in
                             {"cfoam": ("cfoam", "cfoam_hardened", "cfoam_hardened_owned"),
                              "cfoam_fresh": ("cfoam_fresh", "cfoam_fresh_owned")}.get(stem, (stem,))],
                "priority": 120}
    for stem, name in (("extruder_shape", "item.gregtech.extruder_shape_bottle"),
                       ("low_heat_extruder_shape", "item.gregtech.low_heat_extruder_shape_bottle"),
                       ("loot_chest", "block.gregtech.loot_chest_dungeon")):
        yield "functional_" + stem, {"type": "remi:group", "name": name,
                "regex": "^item:gregtech:" + stem + "_[a-z0-9_]+$", "priority": 120}
    multi = (java / "registry/GTMultiItemsGen.java").read_text(encoding="utf-8")
    rows = re.findall(r'"([a-z0-9_]+)", "[^"]*", "(food|bottles)", "[^"]*"', multi)
    # MultiItemFood also registers non-food clay balls. Their explicit clay
    # family must not be swallowed by this higher-priority broad food group.
    foods = [item for item, category in rows if category == "food"
             and item != "clay_comb" and not item.endswith("ice_cream")
             and not re.fullmatch(UNFIRED_CLAY, item)
             and item not in {"blue_clay", "brown_clay", "red_clay", "white_clay", "yellow_clay"}]
    drinks = [item for item, category in rows if category == "bottles"]
    ice = [item for item, category in rows if category == "food" and item.endswith("ice_cream")]
    for stem, label, members in (("foods", "itemGroup.gregtech.nature_foods", foods),
                                 ("drinks", "itemGroup.gregtech.fluids", drinks),
                                 ("ice_cream", "item.gregtech.ice_cream", ice)):
        assert len(members) > 1, stem
        yield "functional_" + stem, {"type": "remi:group", "name": label,
                "contents": ["item:gregtech:" + i for i in sorted(set(members))], "priority": 120}
    # Raw clay molds have unique IDs; the fired mold uses shape data on one ID.
    yield "functional_clay_molds", {"type": "remi:group", "name": "item.gregtech.clay_mold",
            "regex": "^item:gregtech:" + UNFIRED_CLAY + "$", "priority": 120}
    yield "glass_panes", {"type": "remi:group", "name": "block.minecraft.glass_pane",
            "regex": "^item:minecraft:(?:[a-z_]+_stained_)?glass_pane$", "priority": 120}
    yield "paper", {"type": "remi:group", "name": "item.minecraft.paper",
            "contents": ["item:gregtech:plate_paper"], "priority": 120}
    yield "real_fluids", {"type": "remi:group", "name": "itemGroup.gregtech.fluids",
            "regex": "^fluid:gregtech:(?!fluid_item_)[a-z0-9_./-]+$", "priority": 120}
