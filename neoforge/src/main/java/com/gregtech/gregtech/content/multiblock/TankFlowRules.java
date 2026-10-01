package com.gregtech.gregtech.content.multiblock;

import net.minecraft.core.Direction;

/** GT6 controllers auto-emit horizontally, gases freely, and other fluids with gravity. */
public final class TankFlowRules {
    private TankFlowRules() {}
    public static boolean canAutoOutput(Direction face, boolean gas, int density) {
        return com.gregtech.gregtech.api.multiblock.StructureGrid.canAutoOutput(face.getStepY(),gas,density);
    }
}
