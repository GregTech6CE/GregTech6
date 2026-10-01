package com.gregtech.gregtech.block.machine;
import com.gregtech.gregtech.content.multiblock.AxialGeneratorDefinitions;
import com.gregtech.gregtech.blockentity.machine.LargeTurbineControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
public class LargeTurbineControllerBlock extends AxialGeneratorBlock {
    public LargeTurbineControllerBlock(Properties properties) { this(AxialGeneratorDefinitions.STEAM.get(0),properties); }
    public LargeTurbineControllerBlock(AxialGeneratorDefinitions.Grade grade,Properties properties) { super(grade,properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new LargeTurbineControllerBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        if(level.isClientSide||type!=com.gregtech.gregtech.registry.GTBlockEntities.LARGE_TURBINE.get())return null;
        return (l,p,s,be)->((LargeTurbineControllerBlockEntity)be).tick();
    }
}
