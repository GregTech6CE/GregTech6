package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.api.tool.ToolInteractionTarget;
import com.gregtech.gregtech.api.tool.ToolInteractions;
import com.gregtech.gregtech.blockentity.machine.OriginalLargeBoilerControllerBlockEntity;
import com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerSpecs;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

/** Material-specific GT6 large boiler main barometer, original IDs 17201–17205. */
public final class OriginalLargeBoilerControllerBlock extends HorizontalDirectionalBlock
        implements EntityBlock, ToolInteractionTarget {
    private final OriginalLargeBoilerSpecs.Variant variant;

    public OriginalLargeBoilerControllerBlock(OriginalLargeBoilerSpecs.Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public OriginalLargeBoilerSpecs.Variant variant() { return variant; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isMachineWrench(tool)
                ? ToolInteractionSpec.facing(FACING, MachineRotationType.HORIZONTAL) : null;
    }
    @Override public void toolStateChanged(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof OriginalLargeBoilerControllerBlockEntity boiler)
            boiler.isStructureOk();
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        if (ToolInteractions.use(state, level, pos, player, hand, hit))
            return InteractionResult.sidedSuccess(level.isClientSide);
        if (!(level.getBlockEntity(pos) instanceof OriginalLargeBoilerControllerBlockEntity boiler))
            return InteractionResult.PASS;
        InteractionResult tool = boiler.onToolUse(player, hand);
        if (tool != InteractionResult.PASS) return tool;
        if (boiler.isStructureOk()) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(player, hand,
                    boiler.directFluidHandler())) return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OriginalLargeBoilerControllerBlockEntity(pos, state);
    }
    @Nullable @Override @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide || type != GTBlockEntities.ORIGINAL_LARGE_BOILER.get()) return null;
        return (world, pos, current, be) -> OriginalLargeBoilerControllerBlockEntity.serverTick(
                world, pos, current, (OriginalLargeBoilerControllerBlockEntity) be);
    }

    @Override public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                          List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gt.tooltip.multiblock.largeboiler.1"));
        tooltip.add(Component.translatable("gt.tooltip.multiblock.largeboiler.2"));
        tooltip.add(Component.translatable("gt.tooltip.multiblock.largeboiler.4"));
        tooltip.add(Component.translatable("gt.tooltip.multiblock.largeboiler.water_steam"));
        tooltip.add(Component.translatable("gt.tooltip.multiblock.largeboiler.tier",
                variant.heatInputRecommended(), variant.steamOutput()));
        tooltip.add(Component.translatable("gt.tooltip.multiblock.largeboiler.wall", variant.wall().getName()));
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
        var entity = context.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (entity instanceof OriginalLargeBoilerControllerBlockEntity boiler && boiler.barometerValue() > 4)
            return List.of();
        ItemStack drop = new ItemStack(this);
        if (entity instanceof OriginalLargeBoilerControllerBlockEntity boiler)
            drop.addTagElement("BlockEntityTag", com.gregtech.gregtech.content.cover.ComponentCoverFallback.forItem(boiler,boiler.saveWithoutMetadata()));
        return List.of(drop);
    }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof OriginalLargeBoilerControllerBlockEntity boiler)
            stack.addTagElement("BlockEntityTag", com.gregtech.gregtech.content.cover.ComponentCoverFallback.forItem(boiler,boiler.saveWithoutMetadata()));
        return stack;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                                      @Nullable LivingEntity placer, ItemStack stack) {
        var saved = stack.getTagElement("BlockEntityTag");
        if (saved != null && level.getBlockEntity(pos) instanceof OriginalLargeBoilerControllerBlockEntity boiler)
            boiler.load(saved);
    }
    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.isCreative()
                && level.getBlockEntity(pos) instanceof OriginalLargeBoilerControllerBlockEntity boiler
                && boiler.barometerValue() > 4) boiler.explode();
        super.playerWillDestroy(level, pos, state, player);
    }
}
