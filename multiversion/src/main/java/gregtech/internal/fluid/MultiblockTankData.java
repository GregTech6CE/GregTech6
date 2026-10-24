/* Copyright GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original FluidTankGT reads long contents without discarding over-capacity saves. */
package gregtech.internal.fluid;

import net.minecraft.nbt.CompoundTag;

/** Both actual entity loading and read-only item tooltips use the placed valve capacity. */
public final class MultiblockTankData {

    private MultiblockTankData() {
    }

    /** Original setCapacity changes only the nominal limit; it never discards existing contents. */
    public static void setCapacity(FluidTankGT tank, long capacity) {
        if (capacity < 0) return;
        long storedAmount = tank.getAmount();
        var storedFluid = tank.getFluidLong();
        tank.setCapacity(capacity);
        if (storedAmount != tank.getAmount() && !storedFluid.isEmpty()) tank.setFluid(storedFluid, storedAmount);
    }
//? if >=1.21.1 {
    /*public static void readContents(FluidTankGT tank, CompoundTag data, long capacity, net.minecraft.core.HolderLookup.Provider lookup) {
*///?} else {
    public static void readContents(FluidTankGT tank, CompoundTag data, long capacity) {
//?}
        setCapacity(tank, capacity);
        if (data == null || !data.contains("gt.tank", CompoundTag.TAG_COMPOUND)) return;
        var stored = data.getCompound("gt.tank").copy();
        // Saved size/capacity cannot turn one existing valve identity into another.
        stored.putLong("Capacity", capacity);
//? if >=1.21.1 {
        /*tank.readFromNBT(stored, lookup);
*///?} else {
        tank.readFromNBT(stored);
//?}
    }
}
