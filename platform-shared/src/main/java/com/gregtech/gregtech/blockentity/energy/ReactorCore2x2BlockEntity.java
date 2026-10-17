package com.gregtech.gregtech.blockentity.energy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
/** GT6 2x2 means four rods inside one core block, not a casing multiblock. */
public final class ReactorCore2x2BlockEntity extends ReactorCoreBlockEntity {
    public static final int MAX_RODS=4;
    public ReactorCore2x2BlockEntity(BlockPos pos,BlockState state){super(com.gregtech.gregtech.registry.GTBlockEntities.REACTOR_CORE_2X2.get(),pos,state,MAX_RODS);}
}
