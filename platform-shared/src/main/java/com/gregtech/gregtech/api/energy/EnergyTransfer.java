package com.gregtech.gregtech.api.energy;

import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.util.GTEnergySides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Energy network helpers (GT6 {@code ITileEntityEnergy.Util}).
 */
public final class EnergyTransfer {
    private EnergyTransfer() {}

    public static long emitEnergyToNetwork(GregTechTags.Tag energyType, long size, long amount, IEnergyBlock emitter) {
        if (!(emitter instanceof BlockEntity blockEntity) || amount <= 0) {
            return 0;
        }
        long used = 0;
        for (Direction side : GTEnergySides.ALL) {
            if (emitter.isEnergyEmittingTo(energyType, side, false)) {
                used += emitEnergyToSide(energyType, side, size, amount - used, blockEntity);
                if (used >= amount) {
                    break;
                }
            }
        }
        return used;
    }

    public static long emitEnergyToSide(GregTechTags.Tag energyType, Direction sideOutOf, long size, long amount,
                                        BlockEntity emitter) {
        if (amount <= 0 || emitter.getLevel() == null) {
            return 0;
        }
        BlockEntity receiver = GTEnergySides.getNeighbor(emitter.getLevel(), emitter.getBlockPos(), sideOutOf);
        if (receiver == null) {
            return 0;
        }
        return insertEnergyInto(energyType, GTEnergySides.receiverSide(sideOutOf), size, amount, emitter, receiver);
    }

    public static long insertEnergyInto(GregTechTags.Tag energyType, @Nullable Direction sideInto, long size, long amount,
                                        @Nullable Object emitter, BlockEntity receiver) {
        if (receiver instanceof IEnergyBlock energyBlock) {
            return energyBlock.doEnergyInjection(energyType, sideInto, size, amount, true);
        }
        return EnergyCompat.insertEnergyInto(energyType, sideInto, size, amount, emitter, receiver);
    }

    public static long extractEnergyFrom(GregTechTags.Tag energyType, @Nullable Direction sideOutOf, long size, long amount,
                                         BlockEntity receiver) {
        if (receiver instanceof IEnergyBlock energyBlock) {
            return energyBlock.doEnergyExtraction(energyType, sideOutOf, size, amount, true);
        }
        return EnergyCompat.extractEnergyFrom(energyType, sideOutOf, size, amount, receiver);
    }

    /** Units transferred per tick at rated output (HU/Steam amount or EU packet count). */
    public static long unitsFromEuRate(long euPerTick, GregTechTags.Tag energyType) {
        if (energyType == GregTechTags.Energy.STEAM) {
            return euPerTick * GregTechConstants.STEAM_PER_EU;
        }
        return euPerTick;
    }
}
