package com.gregtech.gregtech.api.tool;

import net.minecraft.core.Direction;

/** Tool work performed without a player; budget and return value are GT6 tool wear units. */
public interface PoweredToolTarget {
    default long usePoweredHammer(Direction side, long budget, int quality) { return 0; }
    default boolean usePoweredIgniter(Direction side, long budget, int quality) { return false; }
}
