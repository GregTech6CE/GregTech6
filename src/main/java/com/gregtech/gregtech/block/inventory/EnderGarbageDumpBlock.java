package com.gregtech.gregtech.block.inventory;

import com.gregtech.gregtech.api.GTWaterloggable;
import com.gregtech.gregtech.blockentity.inventory.EnderGarbageDumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

/** GT6 Ender Garbage Dump: retrieves trash from the global garbage piles (admin block). */
public class EnderGarbageDumpBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {

    public EnderGarbageDumpBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GTWaterloggable.WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GTWaterloggable.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return GTWaterloggable.getStateForPlacement(defaultBlockState(), ctx);
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
        return new EnderGarbageDumpBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != com.gregtech.gregtech.registry.GTBlockEntities.ENDER_GARBAGE_DUMP.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<EnderGarbageDumpBlockEntity>)
                EnderGarbageDumpBlockEntity::serverTick;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // both hands fire use(); handling the off hand double-triggers
        if (hand == InteractionHand.OFF_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof EnderGarbageDumpBlockEntity dump)) return InteractionResult.PASS;
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            sp.openMenu(dump);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gt.tooltip.garbage_dump.1")
                .withStyle(net.minecraft.ChatFormatting.AQUA));
        tooltip.add(Component.translatable("gt.tooltip.garbage_dump.2")
                .withStyle(net.minecraft.ChatFormatting.AQUA));
        tooltip.add(Component.translatable("gt.tooltip.garbage_dump.3")
                .withStyle(net.minecraft.ChatFormatting.AQUA));
    }
}
