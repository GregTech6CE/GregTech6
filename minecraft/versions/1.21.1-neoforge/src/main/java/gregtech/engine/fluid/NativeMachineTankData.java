package gregtech.engine.fluid;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public final class NativeMachineTankData {

    private NativeMachineTankData() {}

    public static void restore(FluidTank tank, CompoundTag tag, HolderLookup.Provider lookup) {
        var fluid = gregtech.engine.stack.NbtCompat.readFluid(
                tag.contains("Fluid") ? tag.getCompound("Fluid") : tag, lookup);
        long amount = tag.contains("Fluid") && tag.contains("Amount") ? tag.getLong("Amount") : fluid.getAmount();
        if (!fluid.isEmpty()) fluid.setAmount((int) Math.max(0, Math.min(tank.getCapacity(), amount)));
        tank.setFluid(fluid);
    }
}
