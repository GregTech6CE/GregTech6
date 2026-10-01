package com.gregtech.gregtech.block.energy;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.content.nuclear.ReactorPorts;

import com.gregtech.gregtech.blockentity.energy.ReactorCore2x2BlockEntity;
import com.gregtech.gregtech.item.FuelRodItem;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Four-rod core with a directional hot-fluid outlet. */
public class ReactorCore2x2Block extends HorizontalDirectionalBlock implements EntityBlock, com.gregtech.gregtech.api.tool.ToolInteractionTarget {

    public ReactorCore2x2Block() {
        super(Properties.of()
                .strength(8.0F, 18.0F).noOcclusion()
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ReactorPorts.HOT, Direction.DOWN).setValue(ReactorPorts.COLD, Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ReactorPorts.HOT, ReactorPorts.COLD);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ReactorCore2x2BlockEntity(pos, state);
    }

    /** GT6 {@code MultiTileEntityReactorCore:303} is the shared base of the 1x1 and the 2x2 core. */
    @Override
    public void entityInside(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                             @NotNull net.minecraft.world.entity.Entity entity) {
        com.gregtech.gregtech.content.nuclear.ReactorHazards.contact(level, pos, entity);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof ReactorCore2x2BlockEntity r)
                ReactorCore2x2BlockEntity.serverTick(lvl, pos, st, r);
        };
    }

    @Override
    public @NotNull InteractionResult use(
            @NotNull BlockState state, @NotNull Level level,
            @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand,
            @NotNull BlockHitResult hit) {
        if(level.getBlockEntity(pos) instanceof ReactorCore2x2BlockEntity core) return com.gregtech.gregtech.content.nuclear.ReactorInteraction.use(core,player,hand,hit);
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable BlockGetter level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
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
