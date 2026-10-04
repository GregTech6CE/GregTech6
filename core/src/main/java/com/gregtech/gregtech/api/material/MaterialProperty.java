package com.gregtech.gregtech.api.material;

/**
 * Material behavior flags, analogous to GregTech {@code TD.Properties} / {@code TD.Processing}
 * and {@code TD.ItemGenerator}.
 */
public enum MaterialProperty {
    ELEMENT,
    PARTICLE,
    METAL,
    METALLOID,
    NONMETAL,
    GAS,
    LIQUID,
    MOLTEN,
    DUST,
    INGOT,
    GEM,
    ORE,
    ALLOY,
    WOOD,
    STONE,
    FLAMMABLE,
    /** GT6 TD.Properties.EXPLOSIVE: storage forms react to ignition and other explosions. */
    EXPLOSIVE,
    /** GT6 {@code TD.Processing.MELTING} — exempts low-temp flammable burnoff in crucibles. */
    MELTING,
    /** GT6 {@code TD.Properties.UNBURNABLE}. */
    UNBURNABLE,
    /** GT6 {@code TD.Properties.ACID} — corrodes non-acid-proof molds, faucets and crucibles. */
    ACID,
    MAGNETIC,
    MAGICAL,
    ANTIMATTER,
    HIDDEN,
    /** Generic unit material for recipe substitution markers. */
    UNIT,
    /** Standard dust pile. */
    GENERATE_DUST,
    GENERATE_INGOT,
    GENERATE_NUGGET,
    GENERATE_PLATE,
    GENERATE_ORE,
    /** Rods, bolts, screws, long rods. */
    GENERATE_STICKS,
    /** Gears, rings, springs, rotors, casings, etc. */
    GENERATE_PARTS,
    GENERATE_FOIL,
    GENERATE_WIRE,
    GENERATE_MULTIINGOT,
    GENERATE_MULTIPLATE,
    GENERATE_DENSEPLATE,
    GENERATE_LENS,
    GENERATE_RAIL,
    GENERATE_PLANT,
    GENERATE_PROJECTILE,
    /** Crushed / purified ore chain. */
    GENERATE_ORE_PROCESSING,
    /** Impure / purified / refined dusts. */
    GENERATE_DIRTY_DUST,
    /** Tool head blanks and finished heads. */
    TOOL_HEAD,
    /** Can be worked on an anvil (cart wheels, hot ingots, etc.). */
    SMITHABLE,
    /**
     * GT6 {@code TD.Processing.NEVER_FURNACE} — the material may not be produced by furnace
     * smelting. GT6 flags Iron, Wrought Iron, Steel, Titanium and Tungsten with it; those metals
     * have to be melted in a crucible instead, and {@code Loader_Recipes_Furnace:122-130} replaces
     * any furnace recipe whose result carries this flag with {@code scrapGt}.
     */
    NEVER_FURNACE,
    /** Silicon boules and similar  - force-added per material in GT6. */
    BOULE
}
