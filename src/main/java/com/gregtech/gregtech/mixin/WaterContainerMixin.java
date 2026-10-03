package com.gregtech.gregtech.mixin;

import com.gregtech.gregtech.api.fluid.WaterFamilyIdentity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/** The common waterlogged-container API, retaining vanilla storage and tick behavior. */
@Mixin(SimpleWaterloggedBlock.class)
public interface WaterContainerMixin {
    /**
     * @author GregTech 6 port team
     * @reason Recognize source water by the shared water family rather than one registry identity.
     */
    @Overwrite
    default boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return WaterFamilyIdentity.forVanillaCheck(fluid) == Fluids.WATER;
    }

    /**
     * @author GregTech 6 port team
     * @reason Normalize only the common water predicate; vanilla waterlogged blocks store vanilla water.
     */
    @Overwrite
    default boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)
                || WaterFamilyIdentity.forVanillaCheck(fluid) != Fluids.WATER) return false;
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, true), 3);
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return true;
    }
}
