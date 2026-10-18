package com.gregtech.gregtech.block.energy;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class ElectricTransformerBlock extends EnergyNodeBlock {
    public static final IntegerProperty ACTIVITY=IntegerProperty.create("activity",0,2);
    public ElectricTransformerBlock(EnergyNodeSpec spec,Properties properties){super(spec,properties);registerDefaultState(defaultBlockState().setValue(ACTIVITY,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(ACTIVITY);}
    @Override public boolean isSignalSource(BlockState state){return true;}
    @Override public int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){return level.getBlockEntity(pos) instanceof EnergyNodeBlockEntity node?node.panels().signal(side.getOpposite()):0;}
    @Override public int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){return level.getBlockEntity(pos) instanceof EnergyNodeBlockEntity node?node.panels().strongSignal(side.getOpposite()):0;}
}
