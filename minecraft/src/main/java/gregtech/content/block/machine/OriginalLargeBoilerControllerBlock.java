package gregtech.content.block.machine;

import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.ToolInteractionSpec;
import gregtech.engine.tool.ToolInteractionTarget;
import gregtech.engine.tool.ToolInteractions;
import gregtech.content.blockentity.machine.OriginalLargeBoilerControllerBlockEntity;
import gregtech.content.multiblock.OriginalLargeBoilerSpecs;
import gregtech.content.registry.GTBlockEntities;
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

//? if >=1.21.1 {
    /*@Override protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME->net.minecraft.world.ItemInteractionResult.CONSUME;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return interact(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
*///?} else {
//?}
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
            gregtech.engine.multiblock.tracking.StructureTracking.invalidate(boiler);
    }
//? if >=1.21.1 {
    /*private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
*///?} else {
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
//?}
                                           InteractionHand hand, BlockHitResult hit) {
        if (ToolInteractions.use(state, level, pos, player, hand, hit))
            return InteractionResult.sidedSuccess(level.isClientSide);
        if (!(level.getBlockEntity(pos) instanceof OriginalLargeBoilerControllerBlockEntity boiler))
            return InteractionResult.PASS;
        InteractionResult tool = boiler.onToolUse(player, hand);
        if (tool != InteractionResult.PASS) return tool;
        if (boiler.isStructureOk()) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
//? if >=1.21.1 {
            /*if (net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player, hand,
*///?} else {
            if (net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(player, hand,
//?}
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

//? if >=1.21.1 {
    /*@Override public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
*///?} else {
    @Override public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
//?}
                                          List<Component> tooltip, TooltipFlag flag) {
//? if >=1.21.1 {
        /*var component = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        var data = component == null ? null : component.copyTag();
*///?} else {
        var data = stack.getTagElement("BlockEntityTag");
//?}
        int efficiency = data != null && data.contains("gt.efficiency") ? data.getInt("gt.efficiency") : 10000;
        gregtech.client.FunctionalBlockTooltips.appendLargeBoiler(variant.originalId(), efficiency, getExplosionResistance(), tooltip);
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
        var entity = context.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (entity instanceof OriginalLargeBoilerControllerBlockEntity boiler && boiler.barometerValue() > 4)
            return List.of();
        ItemStack drop = new ItemStack(this);
        if (entity instanceof OriginalLargeBoilerControllerBlockEntity boiler)
//? if >=1.21.1 {
            /*drop.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(gregtech.content.cover.ComponentCoverFallback.forItem(boiler,boiler.saveWithId(boiler.getLevel().registryAccess()))));
*///?} else {
            drop.addTagElement("BlockEntityTag", gregtech.content.cover.ComponentCoverFallback.forItem(boiler,boiler.saveWithoutMetadata()));
//?}
        return List.of(drop);
    }
//? if >=1.21.1 {
    /*@Override public ItemStack getCloneItemStack(BlockState state,net.minecraft.world.phys.HitResult target,net.minecraft.world.level.LevelReader level,BlockPos pos,Player player) {
*///?} else {
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
//?}
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof OriginalLargeBoilerControllerBlockEntity boiler)
//? if >=1.21.1 {
            /*stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(gregtech.content.cover.ComponentCoverFallback.forItem(boiler,boiler.saveWithId(boiler.getLevel().registryAccess()))));
*///?} else {
            stack.addTagElement("BlockEntityTag", gregtech.content.cover.ComponentCoverFallback.forItem(boiler,boiler.saveWithoutMetadata()));
//?}
        return stack;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                                      @Nullable LivingEntity placer, ItemStack stack) {
//? if >=1.21.1 {
        /*var component=stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        var saved=component==null?null:component.copyTag();
*///?} else {
        var saved = stack.getTagElement("BlockEntityTag");
//?}
        if (saved != null && level.getBlockEntity(pos) instanceof OriginalLargeBoilerControllerBlockEntity boiler)
//? if >=1.21.1 {
            /*boiler.loadAdditional(saved,level.registryAccess());
*///?} else {
            boiler.load(saved);
//?}
    }
//? if >=1.21.1 {
    /*@Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
*///?} else {
    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
//?}
        if (!level.isClientSide && !player.isCreative()
                && level.getBlockEntity(pos) instanceof OriginalLargeBoilerControllerBlockEntity boiler
                && boiler.barometerValue() > 4) boiler.explode();
//? if >=1.21.1 {
        /*return super.playerWillDestroy(level, pos, state, player);
*///?} else {
        super.playerWillDestroy(level, pos, state, player);
//?}
    }
}
