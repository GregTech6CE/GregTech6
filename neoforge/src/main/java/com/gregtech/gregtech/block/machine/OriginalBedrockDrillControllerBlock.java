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
import net.neoforged.neoforge.fluids.FluidUtil;

import javax.annotation.Nullable;
import java.util.List;

/** GT6 numeric ID 17999: real top-center drilling controller, output on its upper side. */
public final class OriginalBedrockDrillControllerBlock extends Block implements EntityBlock {
    @Override public com.mojang.serialization.MapCodec<? extends Block> codec(){return com.mojang.serialization.MapCodec.unit(this);}
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

    private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BedrockDrillControllerBlockEntity drill))
            return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var tank = drill.fluidCapability(hit.getDirection());
        if (tank != null && FluidUtil.interactWithFluidHandler(player, hand, tank))
            return InteractionResult.CONSUME;
        if (player.getItemInHand(hand).isEmpty()) {
            var inventory = drill.itemCapability(Direction.UP);
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

    @Override public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                          List<Component> tooltip, TooltipFlag flag) {
        com.gregtech.gregtech.client.OriginalControllerTooltips.standalone(
                com.gregtech.gregtech.content.multiblock.OriginalControllerTooltipData.Family.BEDROCK_DRILL,tooltip);
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
        ItemStack stack = new ItemStack(this);
        if (context.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BedrockDrillControllerBlockEntity drill) {
            var data = com.gregtech.gregtech.content.cover.ComponentCoverFallback.forItem(drill,drill.saveWithId(drill.getLevel().registryAccess()));
            data.remove("gt.output"); // The output slot is dropped separately by onRemove.
            stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(data));
        }
        return List.of(stack);
    }

    @Override public ItemStack getCloneItemStack(BlockState state,net.minecraft.world.phys.HitResult hit,net.minecraft.world.level.LevelReader level,BlockPos pos,net.minecraft.world.entity.player.Player player) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof BedrockDrillControllerBlockEntity drill)
            stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(com.gregtech.gregtech.content.cover.ComponentCoverFallback.forItem(drill,drill.saveWithId(drill.getLevel().registryAccess()))));
        return stack;
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                                      @Nullable LivingEntity placer, ItemStack stack) {
        var saved = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        if (saved != null && level.getBlockEntity(pos) instanceof BedrockDrillControllerBlockEntity drill)
            drill.restorePersistentState(saved.copyTag(),level.registryAccess());
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME->net.minecraft.world.ItemInteractionResult.CONSUME;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){return interact(state,level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);}
}
