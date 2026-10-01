package com.gregtech.gregtech.block.tool;

import com.gregtech.gregtech.blockentity.SapBagBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * GT6's sap bag block ({@code MultiTileEntitySapBag}): hangs on the side of a tree hole and drains
 * it through {@link SapBagBlockEntity}.
 *
 * <p>Right-clicking hands out the collected resin item first (GT6 {@code ST.add(aPlayer, slot(0))}),
 * then fills the container the player holds from the tank.
 */
public class SapBagBlock extends ShapedToolBlock implements EntityBlock {

    public SapBagBlock(String id, Properties properties) {
        super(id, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SapBagBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> {
            if (be instanceof SapBagBlockEntity bag) bag.collect();
        };
    }
    public InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        // Wrench rotation and the other shared tool interactions keep working.
        InteractionResult shared = super.interact(state, level, pos, player, hand, hit);
        if (shared != InteractionResult.PASS) return shared;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SapBagBlockEntity bag)) return InteractionResult.PASS;
        // GT6: give the stored item first.
        if (!bag.stored().isEmpty()) {
            ItemStack stored = bag.stored().copy();
            bag.setStored(ItemStack.EMPTY);
            if (!player.addItem(stored)) player.drop(stored, false);
            return InteractionResult.CONSUME;
        }
        var filled = com.gregtech.gregtech.api.fluid.HandContainerTransfer.prepare(player.getItemInHand(hand), bag.tank().getFluid(), false);
        if (filled == null) return InteractionResult.PASS;
        bag.drain(filled.amount(), IFluidHandler.FluidAction.EXECUTE);
        com.gregtech.gregtech.api.fluid.HandContainerTransfer.replaceOne(player, hand, filled.container());
        return InteractionResult.CONSUME;
    }

    /** GT6's {@code breakBlock}: the tank is trashed, the stored resin item drops. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SapBagBlockEntity bag
                && !bag.stored().isEmpty()) {
            popResource(level, pos, bag.stored());
            bag.setStored(ItemStack.EMPTY);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SapBagBlockEntity bag) {
            if (!level.isClientSide && !bag.stored().isEmpty()) {
                popResource(level, pos, bag.stored());
                bag.setStored(ItemStack.EMPTY);
            }
            bag.tank().setEmpty(); // GT6 GarbageGT.trash(mTank)
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
