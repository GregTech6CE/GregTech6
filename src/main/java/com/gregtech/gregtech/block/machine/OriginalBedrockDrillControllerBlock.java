package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.blockentity.machine.BedrockDrillControllerBlockEntity;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidUtil;

import javax.annotation.Nullable;
import java.util.List;

/** GT6 numeric ID 17999: real top-center drilling controller, output on its upper side. */
public final class OriginalBedrockDrillControllerBlock extends Block implements EntityBlock {
    public OriginalBedrockDrillControllerBlock(Properties properties) { super(properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BedrockDrillControllerBlockEntity(pos, state);
    }

    @Nullable @Override @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != GTBlockEntities.BEDROCK_DRILL.get()) return null;
        return (world, pos, current, be) -> BedrockDrillControllerBlockEntity.serverTick(
                world, pos, current, (BedrockDrillControllerBlockEntity) be);
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BedrockDrillControllerBlockEntity drill))
            return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var tank = drill.getCapability(ForgeCapabilities.FLUID_HANDLER, hit.getDirection()).resolve().orElse(null);
        if (tank != null && FluidUtil.interactWithFluidHandler(player, hand, tank))
            return InteractionResult.CONSUME;
        if (player.getItemInHand(hand).isEmpty()) {
            var inventory = drill.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve().orElse(null);
            if (inventory != null) {
                ItemStack taken = inventory.extractItem(0, 64, false);
                if (!taken.isEmpty() && !player.getInventory().add(taken)) player.drop(taken, false);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof BedrockDrillControllerBlockEntity drill)
            drill.dropContents();
        super.onRemove(state, level, pos, next, moving);
    }

    @Override public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                          List<Component> tooltip, TooltipFlag flag) {
        com.gregtech.gregtech.client.OriginalControllerTooltips.standalone(
                com.gregtech.gregtech.content.multiblock.OriginalControllerTooltipData.Family.BEDROCK_DRILL,tooltip);
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
        ItemStack stack = new ItemStack(this);
        if (context.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BedrockDrillControllerBlockEntity drill) {
            var data = com.gregtech.gregtech.content.cover.ComponentCoverFallback.forItem(drill,drill.saveWithoutMetadata());
            data.remove("gt.output"); // The output slot is dropped separately by onRemove.
            stack.addTagElement("BlockEntityTag", data);
        }
        return List.of(stack);
    }

    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof BedrockDrillControllerBlockEntity drill)
            stack.addTagElement("BlockEntityTag", com.gregtech.gregtech.content.cover.ComponentCoverFallback.forItem(drill,drill.saveWithoutMetadata()));
        return stack;
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                                      @Nullable LivingEntity placer, ItemStack stack) {
        var saved = stack.getTagElement("BlockEntityTag");
        if (saved != null && level.getBlockEntity(pos) instanceof BedrockDrillControllerBlockEntity drill)
            drill.load(saved);
    }
}
