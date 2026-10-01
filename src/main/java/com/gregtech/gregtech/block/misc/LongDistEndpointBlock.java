package com.gregtech.gregtech.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import javax.annotation.Nullable;

/** GT6 Long Distance Transport endpoint — connects LongDistPipe for cross-chunk item/fluid delivery. */
public class LongDistEndpointBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private final boolean fluid;

    public LongDistEndpointBlock(boolean fluid, Properties properties) {
        super(properties);
        this.fluid = fluid;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public boolean isFluid() { return fluid; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LongDistEndpointBlockEntity(pos, state); }
}
