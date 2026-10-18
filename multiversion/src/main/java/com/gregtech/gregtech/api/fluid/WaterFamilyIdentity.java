package com.gregtech.gregtech.api.fluid;

import com.gregtech.gregtech.registry.GTWorldWaterFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/** Water-family identity for vanilla behavior checks; never changes registered or stored fluids. */
public final class WaterFamilyIdentity {
    private WaterFamilyIdentity() {}

    public static Fluid forVanillaCheck(FluidState state) {
        Fluid fluid = state.getType();
        if (!GTWorldWaterFluid.isWaterFamily(fluid)) return fluid;
        return state.isSource() ? Fluids.WATER : Fluids.FLOWING_WATER;
    }

    public static Fluid forVanillaCheck(Fluid fluid) {
        return GTWorldWaterFluid.isWaterFamily(fluid) && fluid.defaultFluidState().isSource()
                ? Fluids.WATER : fluid;
    }
}
