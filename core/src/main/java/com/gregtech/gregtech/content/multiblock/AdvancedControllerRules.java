package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.api.multiblock.StructureGrid;

/** Shared parameters for bedrock drills and lightning rod output. */
public final class AdvancedControllerRules {
    private AdvancedControllerRules() {}

    public static final long DRILL_CAPACITY = 40000,
            DRILL_WORK = 32768,
            LIGHTNING_PACKET = 32768,
            LIGHTNING_CAPACITY = 18000 * LIGHTNING_PACKET;
    public static final int DRILL_LUBRICANT = 100;

    public static StructureGrid.Role drillRole(int x, int y, int z) {
        return y == -4 ? StructureGrid.Role.CASING
                : y == -1 && ((x == 0) != (z == 0)) ? StructureGrid.Role.ENERGY_INPUT
                : StructureGrid.Role.FLUID_INPUT;
    }

    public static long lightningDrain(long energy, long sentPackets) {
        return Math.min(energy, Math.max(1, Math.min(16, sentPackets)) * LIGHTNING_PACKET);
    }
}
