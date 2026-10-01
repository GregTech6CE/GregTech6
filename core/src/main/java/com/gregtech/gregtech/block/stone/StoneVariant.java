package com.gregtech.gregtech.block.stone;

import java.util.Locale;

/** GT6 {@code BlockStones} meta variants (16 forms per rock type). */
public enum StoneVariant {
    STONE(0, "stone", "Stone"),
    COBBLE(1, "cobble", "Cobblestone"),
    COBBLE_MOSSY(2, "cobble_mossy", "Mossy Cobblestone"),
    BRICKS(3, "bricks", "Bricks"),
    BRICKS_CRACKED(4, "bricks_cracked", "Cracked Bricks"),
    BRICKS_MOSSY(5, "bricks_mossy", "Mossy Bricks"),
    BRICKS_CHISELED(6, "bricks_chiseled", "Chiseled Bricks"),
    SMOOTH(7, "smooth", "Smooth"),
    BRICKS_REINFORCED(8, "bricks_reinforced", "Reinforced Bricks"),
    BRICKS_REDSTONE(9, "bricks_redstone", "Redstone Bricks"),
    TILES(10, "tiles", "Tiles"),
    SMALL_TILES(11, "small_tiles", "Small Tiles"),
    SMALL_BRICKS(12, "small_bricks", "Small Bricks"),
    WINDMILL_TILES_A(13, "windmill_tiles_a", "Windmill Tiles A"),
    WINDMILL_TILES_B(14, "windmill_tiles_b", "Windmill Tiles B"),
    SQUARE_BRICKS(15, "square_bricks", "Square Bricks");

    private final int meta;
    private final String suffix;
    private final String display;

    StoneVariant(int meta, String suffix, String display) {
        this.meta = meta;
        this.suffix = suffix;
        this.display = display;
    }

    public int meta() {
        return meta;
    }

    public String registrySuffix() {
        return suffix;
    }

    public String displayName() {
        return display;
    }

    /** Texture file name under {@code textures/block/stones/{folder}/}. */
    public String textureName() {
        return suffix;
    }

    public static StoneVariant byMeta(int meta) {
        for (StoneVariant variant : values()) {
            if (variant.meta == meta) {
                return variant;
            }
        }
        throw new IllegalArgumentException("Unknown stone variant meta: " + meta);
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
