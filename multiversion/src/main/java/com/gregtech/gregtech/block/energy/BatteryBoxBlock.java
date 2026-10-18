package com.gregtech.gregtech.block.energy;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Original battery-box buffer indicator, distinct from the charge inside its batteries. */
public final class BatteryBoxBlock extends EnergyNodeBlock {
    public static final IntegerProperty CHARGE_STATE=IntegerProperty.create("charge_state",0,2);
    public BatteryBoxBlock(EnergyNodeSpec spec,Properties properties){super(spec,properties);registerDefaultState(defaultBlockState().setValue(CHARGE_STATE,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
        super.createBlockStateDefinition(builder);builder.add(CHARGE_STATE);
    }
    @Override public boolean isSignalSource(BlockState state){return true;}
    @Override public int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){
        return level.getBlockEntity(pos) instanceof EnergyNodeBlockEntity node?node.panels().signal(side.getOpposite()):0;
    }
    @Override public int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){
        return level.getBlockEntity(pos) instanceof EnergyNodeBlockEntity node?node.panels().strongSignal(side.getOpposite()):0;
    }
}
