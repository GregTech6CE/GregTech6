package com.gregtech.gregtech.api.energy;

import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

/** External-mod energy bridges (IC2/RF stubs for now). */
public final class EnergyCompat {
    private EnergyCompat() {}

    public static long insertEnergyInto(GregTechTags.Tag energyType, @Nullable Direction sideInto, long size, long amount,
                                        @Nullable Object emitter, BlockEntity receiver) {
        return 0;
    }
}
