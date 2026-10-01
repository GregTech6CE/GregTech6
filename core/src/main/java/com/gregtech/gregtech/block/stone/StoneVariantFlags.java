package com.gregtech.gregtech.block.stone;

/** GT6 {@code BlockStones} per-variant flags (indexed by {@link StoneVariant#meta()}). */
public final class StoneVariantFlags {
    public static final boolean[] MOSSY = {
            false, false, true, false, false, true, false, false, false, false,
            false, false, false, false, false, false
    };
    public static final boolean[] MOSSABLE = {
            false, true, false, true, true, false, false, false, false, false,
            false, false, false, false, false, false
    };
    public static final boolean[] SEALABLE = {
            false, false, false, true, false, true, true, true, true, true,
            true, true, true, true, true, true
    };
    public static final boolean[] SPAWNABLE = {
            true, true, true, false, false, false, false, false, false, false,
            false, false, false, false, false, false
    };
    public static final boolean[] PLANTABLE = {
            true, true, true, false, true, true, false, false, false, false,
            false, false, false, false, false, false
    };

    private StoneVariantFlags() {}

    public static boolean isMossy(StoneVariant variant) {
        return MOSSY[variant.meta()];
    }

    public static boolean isMossable(StoneVariant variant) {
        return MOSSABLE[variant.meta()];
    }

    public static boolean isSealable(StoneVariant variant) {
        return SEALABLE[variant.meta()];
    }

    public static boolean spawnsCreatures(StoneVariant variant) {
        return SPAWNABLE[variant.meta()];
    }

    public static boolean plantable(StoneVariant variant) {
        return PLANTABLE[variant.meta()];
    }
}
