package com.gregtech.gregtech.api.material;

/**
 * Item icon set folder under {@code textures/item/material_icons/}, matching GregTech 6 texture sets.
 */
public enum MaterialTextureSet {
    METALLIC,
    DULL,
    SHINY,
    FINE,
    SAND,
    STONE,
    WOOD,
    DIAMOND,
    EMERALD,
    QUARTZ,
    RUBY,
    REDSTONE,
    ROUGH,
    POWDER,
    COPPER,
    CUBE,
    CUBE_SHINY,
    FIERY,
    FLINT,
    FLUID,
    FOOD,
    GAS,
    GEM_HORIZONTAL,
    GEM_VERTICAL,
    GLASS,
    HEX,
    LAPIS,
    LEAF,
    LIGNITE,
    MAGNETIC,
    NETHERSTAR,
    NONE,
    OPAL,
    PAPER,
    PLASMA,
    PRISMARINE,
    RAD,
    RUBBER,
    SHARDS,
    SPACE,
    BRICK;

    /** Texture sets that have shared model JSON under {@code models/item/material/}. */
    public static final MaterialTextureSet[] MODELED = values();

    /** Lowercase folder name used in resource locations (MC normalizes paths to lowercase). */
    public String folder() {
        return name().toLowerCase();
    }
}
