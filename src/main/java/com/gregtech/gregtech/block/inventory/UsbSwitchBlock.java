package com.gregtech.gregtech.block.inventory;

import com.gregtech.gregtech.blockentity.inventory.UsbSwitchBlockEntity;
import com.gregtech.gregtech.content.cover.PanelCoverInteraction;
import com.gregtech.gregtech.content.data.UsbDataMedia;
import com.gregtech.gregtech.registry.GTBlockEntities;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Two GT6 data switches: 19000 selects one of 16 sticks; 19001 selects one file on a HDD. */
public class UsbSwitchBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public enum Kind { USB, HDD }
    private final Kind kind;

    public UsbSwitchBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }
    public Kind kind() { return kind; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UsbSwitchBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                            BlockEntityType<T> type) {
        return !level.isClientSide && type == GTBlockEntities.USB_SWITCH.get()
                ? (l, p, s, be) -> ((UsbSwitchBlockEntity) be).serverTick() : null;
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !(level.getBlockEntity(pos) instanceof UsbSwitchBlockEntity machine))
            return InteractionResult.PASS;
        InteractionResult panel = PanelCoverInteraction.use(machine, player, hand, hit, true);
        if (panel != InteractionResult.PASS) return panel;
        ItemStack held = player.getItemInHand(hand);
        if (UsbDataMedia.isStick(held)) {
            if (!level.isClientSide) machine.writeFromHeldStick(held, hit.getDirection());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer server) server.openMenu(machine);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos,
                                   BlockState next, boolean movedByPiston) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof UsbSwitchBlockEntity machine) {
            for (int slot = 0; slot < machine.items().getSlots(); slot++) {
                ItemStack stack = machine.items().getStackInSlot(slot);
                machine.items().setStackInSlot(slot, ItemStack.EMPTY);
                if (!stack.isEmpty()) popResource(level, pos, stack.copy());
            }
            for (Direction side : Direction.values()) {
                ItemStack cover = machine.removeCover(side);
                if (!cover.isEmpty()) popResource(level, pos, cover);
            }
        }
        super.onRemove(state, level, pos, next, movedByPiston);
    }
    @Override public List<ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        return List.of(new ItemStack(this));
    }
    @Override public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                          List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(kind == Kind.USB
                ? "gt.tooltip.usb_switch" : "gt.tooltip.hdd_switch")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
