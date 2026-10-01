package com.gregtech.gregtech.api.energy;

import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.data.GregTechConstants;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities;
import javax.annotation.Nullable;

/** FE capabilities adapted to the shared GregTech packet bridge. */
public final class EnergyCompat {
    private EnergyCompat() {}
    @Nullable private static IEnergyStorage storage(BlockEntity receiver, @Nullable Direction side) {
        if (receiver == null || receiver.isRemoved()) return null;
        return receiver.getLevel() == null ? null : receiver.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, receiver.getBlockPos(), side);
    }
    private static long fePacketSize(GregTechTags.Tag type, long size) {
        if (size == 0 || size == Long.MIN_VALUE) return 0;
        long magnitude = Math.abs(size);
        int factor = type == GregTechTags.Energy.EU ? GregTechConstants.RF_PER_EU
                : type == GregTechTags.Energy.MJ ? GregTechConstants.RF_PER_MJ
                : type == GregTechTags.Energy.RF ? 1 : 0;
        return factor == 0 || magnitude > Integer.MAX_VALUE / factor ? 0 : magnitude * factor;
    }
    public static long insertEnergyInto(GregTechTags.Tag type, @Nullable Direction side, long size, long amount,
                                        @Nullable Object emitter, BlockEntity receiver) {
        if (amount <= 0) return 0;
        long packet = fePacketSize(type, size);
        IEnergyStorage storage = packet > 0 ? storage(receiver, side) : null;
        return storage == null || !storage.canReceive() ? 0 : EnergyBridge.pushPacketTrain(storage::receiveEnergy, packet, amount);
    }
    public static long extractEnergyFrom(GregTechTags.Tag type, @Nullable Direction side, long size, long amount,
                                         BlockEntity receiver) {
        if (amount <= 0 || type != GregTechTags.Energy.EU || size == Long.MIN_VALUE) return 0;
        IEnergyStorage storage = fePacketSize(type, size) > 0 ? storage(receiver, side) : null;
        return storage == null || !storage.canExtract() ? 0 : EnergyBridge.extractFe(storage::extractEnergy, size, amount);
    }
}
