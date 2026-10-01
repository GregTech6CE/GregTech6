package com.gregtech.gregtech.blockentity.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** GT6 20411-20415: QU, cheap overclocking and tier-dependent matter conversion efficiency. */
public final class CompactMatterFabricatorBlockEntity extends BasicMachineBlockEntity {
    public CompactMatterFabricatorBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);}
    @Override protected boolean cheapOverclocking(){return true;}
    @Override protected boolean requiresConstantEnergy(){return false;}
    @Override protected int efficiency(){return 5000+1250*(Math.max(1,Math.min(5,spec().tier()))-1);}
}
