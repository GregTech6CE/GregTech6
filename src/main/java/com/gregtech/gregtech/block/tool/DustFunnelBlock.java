package com.gregtech.gregtech.block.tool;

import com.gregtech.gregtech.blockentity.tool.DustFunnelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/** GT6 Dust Funnel (MultiTileEntityDustFunnel): feeds dusts into the crucible below. */
public class DustFunnelBlock extends Block implements EntityBlock {

    // GT6 bounds: full top half + a centered spout below
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 8, 0, 16, 16, 16),
            Block.box(2, 0, 2, 14, 8, 14));

    public DustFunnelBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DustFunnelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != com.gregtech.gregtech.registry.GTBlockEntities.DUST_FUNNEL.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<DustFunnelBlockEntity>) DustFunnelBlockEntity::serverTick;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // both hands fire use(); handling the off hand double-triggers
        if (hand == InteractionHand.OFF_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof DustFunnelBlockEntity funnel)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isShiftKeyDown() && held.isEmpty()) {
                ItemStack out = funnel.retrieve();
                if (!out.isEmpty() && !player.addItem(out)) player.drop(out, false);
            } else if (!held.isEmpty()) {
                int taken = funnel.insert(held);
                if (taken > 0 && !player.getAbilities().instabuild) held.shrink(taken);
                else if (taken == 0) player.displayClientMessage(
                        Component.translatable("message.gregtech.dust_funnel.only_dusts"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof DustFunnelBlockEntity funnel) {
            funnel.dropContents();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gt.tooltip.dust_funnel.1").withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gt.tooltip.dust_funnel.2").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
