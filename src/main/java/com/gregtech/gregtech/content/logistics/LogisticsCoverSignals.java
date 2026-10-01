package com.gregtech.gregtech.content.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

/** The four GT6 CPU meters emit the same strength as weak and direct redstone. */
public final class LogisticsCoverSignals {
    private LogisticsCoverSignals() {}

    public static int at(BlockGetter level, BlockPos pos, Direction face) {
        return level.getBlockEntity(pos) instanceof LogisticsCoverHost host
                ? host.logisticsCovers().displaySignal(face) : 0;
    }
}
