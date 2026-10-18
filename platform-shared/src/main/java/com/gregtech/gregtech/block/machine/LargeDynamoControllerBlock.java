package com.gregtech.gregtech.block.machine;
import com.gregtech.gregtech.content.multiblock.AxialGeneratorDefinitions;
import com.gregtech.gregtech.blockentity.machine.LargeDynamoControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
public class LargeDynamoControllerBlock extends AxialGeneratorBlock {
    public LargeDynamoControllerBlock(Properties properties) { this(AxialGeneratorDefinitions.DYNAMO.get(0),properties); }
    public LargeDynamoControllerBlock(AxialGeneratorDefinitions.Grade grade,Properties properties) { super(grade,properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new LargeDynamoControllerBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        if(level.isClientSide||type!=com.gregtech.gregtech.registry.GTBlockEntities.LARGE_DYNAMO.get())return null;
        return (l,p,s,be)->((LargeDynamoControllerBlockEntity)be).tick();
    }
}
