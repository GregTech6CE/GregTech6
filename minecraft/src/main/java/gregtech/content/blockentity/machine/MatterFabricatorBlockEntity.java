package gregtech.content.blockentity.machine;

import gregtech.content.data.GregTechTags;
import gregtech.engine.multiblock.MultiblockLayout.Role;
import net.minecraft.core.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Original 17199 machine parameters; shared executor owns work and output persistence. */
public final class MatterFabricatorBlockEntity extends LargeRecipeMachineBlockEntity {
    public MatterFabricatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag type, Direction side) {
        return type == GregTechTags.Energy.QU ? 1 : 0;
    }

    @Override
    public long portEnergyInputRecommended(Role role, GregTechTags.Tag type) {
        return getEnergySizeInputRecommended(type, null);
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, Direction side, boolean theoretical) {
        return type == GregTechTags.Energy.QU
                && isStructureOk()
                && super.isEnergyAcceptingFrom(type, side, theoretical);
    }
}
