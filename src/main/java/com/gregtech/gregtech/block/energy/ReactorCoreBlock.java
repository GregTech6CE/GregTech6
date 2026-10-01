package com.gregtech.gregtech.block.energy;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.content.nuclear.ReactorPorts;

import com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity;
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

import javax.annotation.Nullable;
import java.util.List;

/** GT6 single-rod reactor core with shared rod/container interaction. */
public class ReactorCoreBlock extends Block implements EntityBlock, com.gregtech.gregtech.api.tool.ToolInteractionTarget {

    public ReactorCoreBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(ReactorPorts.HOT, net.minecraft.core.Direction.DOWN).setValue(ReactorPorts.COLD, net.minecraft.core.Direction.DOWN));
    }

    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ReactorPorts.HOT, ReactorPorts.COLD);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorCoreBlockEntity(pos, state);
    }

    /** GT6 {@code MultiTileEntityReactorCore:303}: touching a running core burns and irradiates. */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        com.gregtech.gregtech.content.nuclear.ReactorHazards.contact(level, pos, entity);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != com.gregtech.gregtech.registry.GTBlockEntities.REACTOR_CORE.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<ReactorCoreBlockEntity>)
                ReactorCoreBlockEntity::serverTick;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if(level.getBlockEntity(pos) instanceof ReactorCoreBlockEntity core) return com.gregtech.gregtech.content.nuclear.ReactorInteraction.use(core,player,hand,hit);
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gt.tooltip.reactor.updated"));
    }
    @Override public void onRemove(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof com.gregtech.gregtech.api.inventory.BlockContents contents) {
            contents.dropContents();
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){
        var stack=new ItemStack(this);
        if(builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity core){
            var tag=core.saveWithoutMetadata();tag.remove("gt.rods");tag.remove("gt.overflow");tag.remove("gt.neutrons");tag.putBoolean("gt.stopped",true);stack.getOrCreateTag().put("BlockEntityTag",tag);
        }
        ReactorPorts.saveItemState(stack, state);
        return java.util.List.of(stack);
    }
    @Override public com.gregtech.gregtech.api.tool.ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return ReactorPorts.tool(tool);
    }
}
