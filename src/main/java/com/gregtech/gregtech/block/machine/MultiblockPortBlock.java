package com.gregtech.gregtech.block.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MultiblockPortBlock extends Block implements EntityBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty CRUCIBLE_FORMED =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("crucible_formed");
    public MultiblockPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CRUCIBLE_FORMED, false));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CRUCIBLE_FORMED);
    }
    @Override public RenderShape getRenderShape(BlockState state) {
        return state.getValue(CRUCIBLE_FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.getValue(CRUCIBLE_FORMED) ? net.minecraft.world.phys.shapes.Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }
    @Override public int getLightBlock(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.getValue(CRUCIBLE_FORMED) ? 0 : super.getLightBlock(state, level, pos);
    }
    @Override public net.minecraft.world.InteractionResult use(BlockState state, net.minecraft.world.level.Level level,
            BlockPos pos, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity part)
            return com.gregtech.gregtech.content.logistics.LogisticsCoverInteraction.use(
                    part, level, player, hand, hit.getDirection());
        return net.minecraft.world.InteractionResult.PASS;
    }
    @Override public void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity part)
            part.logisticsCovers().dropAll();
        super.onRemove(state, level, pos, next, moving);
    }
    /** Structural bindings are transient; breaking a part returns only the part itself. */
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder context) {
        return java.util.List.of(new net.minecraft.world.item.ItemStack(this));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity(pos,state);
    }
    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level,
                                   BlockPos pos, net.minecraft.core.Direction side) {
        return com.gregtech.gregtech.content.logistics.LogisticsCoverSignals.at(level, pos, side);
    }
    @Override public int getDirectSignal(BlockState state, net.minecraft.world.level.BlockGetter level,
                                         BlockPos pos, net.minecraft.core.Direction side) {
        return getSignal(state, level, pos, side);
    }
}
