package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.content.multiblock.*;
import com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Original 17199 machine parameters; shared executor owns work and output persistence. */
public final class MatterFabricatorBlockEntity extends LargeRecipeMachineBlockEntity {
    public MatterFabricatorBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);}
    @Override protected long inputMinimum(){return 1;}
    @Override protected long inputMaximum(){return 2097152;}
    @Override protected boolean requiresConstantEnergy(){return false;}
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type,Direction side){return type==GregTechTags.Energy.QU?1:0;}
    @Override public long portEnergyInputRecommended(Role role,GregTechTags.Tag type){return getEnergySizeInputRecommended(type,null);}
    @Override protected boolean validStructureParts() {
        int control=0,conversion=0;
        var front=getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        for(var cell:MatterFabricatorStructure.CELLS)if(cell.part()==18202||cell.part()==18204) {
            var pos=cell.at(worldPosition,front);if(!level.hasChunkAt(pos))return false;
            var block=level.getBlockState(pos).getBlock();
            if(block==LargeMachineParts.block(18202))control++;
            else if(block==LargeMachineParts.block(18204))conversion++;
            else return false;
        }
        return control==4&&conversion==4;
    }
    @Override protected boolean matchesPart(LargeMachineLayouts.Cell cell,Block expected,Block actual) {
        return cell.part()==18202||cell.part()==18204
                ?actual==LargeMachineParts.block(18202)||actual==LargeMachineParts.block(18204):actual==expected;
    }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type,Direction side,boolean theoretical) {
        return type==GregTechTags.Energy.QU&&isStructureOk()&&super.isEnergyAcceptingFrom(type,side,theoretical);
    }
}
