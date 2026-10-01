package com.gregtech.gregtech.block.inventory;

import com.gregtech.gregtech.api.GTWaterloggable;
import com.gregtech.gregtech.blockentity.inventory.LockerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

/** GT6 Locker: right-click swaps your whole armor set with the stored one. */
public class LockerBlock extends HorizontalDirectionalBlock implements EntityBlock, SimpleWaterloggedBlock {

    public LockerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(GTWaterloggable.WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(GTWaterloggable.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState base = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        return GTWaterloggable.getStateForPlacement(base, ctx);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return GTWaterloggable.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        GTWaterloggable.updateShape(state, level, pos);
        return state;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LockerBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // both hands fire use(); handling the off hand double-triggers
        // (insert+extract in one click, double withdrawals, armor swap-backs)
        if (hand == net.minecraft.world.InteractionHand.OFF_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof LockerBlockEntity locker)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            locker.swapArmor(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof LockerBlockEntity locker) {
            locker.dropContents();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gt.tooltip.locker.1"));
    }
}
