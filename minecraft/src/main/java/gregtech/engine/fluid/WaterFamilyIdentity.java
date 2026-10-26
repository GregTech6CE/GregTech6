package gregtech.engine.fluid;

import gregtech.content.registry.GTWorldWaterFluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/** Water-family identity for vanilla behavior checks; never changes registered or stored fluids. */
public final class WaterFamilyIdentity {
    private WaterFamilyIdentity() {}

    /** Match actual water bodies, including tagged mod waters, but not waterlogged solid blocks. */
    public static boolean matchesWaterBlock(BlockState state, Block expected) {
        return state.is(expected) || expected == Blocks.WATER
                && state.getBlock() instanceof LiquidBlock
                && GTWorldWaterFluid.isWaterFamily(state.getFluidState().getType());
    }

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
