/* Copyright GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original FluidTankGT reads long contents without discarding over-capacity saves. */
package com.gregtech.gregtech.api.fluid;

import net.minecraft.nbt.CompoundTag;

/** Both actual entity loading and read-only item tooltips use the placed valve capacity. */
public final class MultiblockTankData {
    private MultiblockTankData() {}
    /** Original setCapacity changes only the nominal limit; it never discards existing contents. */
    public static void setCapacity(FluidTankGT tank, long capacity) {
        if(capacity < 0) return;
        long amount=tank.getAmount();
        var fluid=tank.getFluidLong();
        tank.setCapacity(capacity);
        if(amount != tank.getAmount() && !fluid.isEmpty()) tank.setFluid(fluid,amount);
    }
    public static void readContents(FluidTankGT tank, CompoundTag data, long capacity) {
        setCapacity(tank,capacity);
        if(data == null || !data.contains("gt.tank", CompoundTag.TAG_COMPOUND)) return;
        var stored=data.getCompound("gt.tank").copy();
        // Saved size/capacity cannot turn one existing valve identity into another.
        stored.putLong("Capacity",capacity);
        tank.readFromNBT(stored);
    }
}
