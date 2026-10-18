package com.gregtech.gregtech.blockentity.behavior;

import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.nbt.CompoundTag;

/** Internal energy storage buffer (GT6 {@code TE_Behavior_Energy_Capacitor}). */
public final class EnergyCapacitorBehavior {
    public long energy;
    public long capacity;

    public EnergyCapacitorBehavior(long capacity) {
        this.capacity = Math.max(0, capacity);
    }

    public void load(CompoundTag tag) {
        if (tag.contains(GregTechConstants.NBT_ENERGY)) {
            energy = tag.getLong(GregTechConstants.NBT_ENERGY);
        }
    }

    public void save(CompoundTag tag) {
        tag.putLong(GregTechConstants.NBT_ENERGY, energy);
    }

    public long room() {
        return Math.max(0, capacity - energy);
    }

    public boolean isFull() {
        return energy >= capacity;
    }
}
