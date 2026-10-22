package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.addon.EnergyInput;
import com.gregtech.gregtech.api.addon.EnergyOutput;
import com.gregtech.gregtech.api.addon.LayoutPart;
import com.gregtech.gregtech.blockentity.machine.AddonEnergyPartBlockEntity;
import com.gregtech.gregtech.blockentity.machine.FunctionalPartRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import javax.annotation.Nullable;

/**
 * Addon energy part. Faces are relative to {@link #FACING}.
 * {@link #SATISFIED} is written by the server when a condition input was paid this tick.
 * Casings do not use this block. One part either emits or accepts.
 * Slots, a tick, a click or named fields may be added on the same part.
 */
public class AddonEnergyPartBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty SATISFIED = BooleanProperty.create("satisfied");

    private final String addonId;
    private final LayoutPart declaration;

    public AddonEnergyPartBlock(Properties properties, String addonId, LayoutPart declaration) {
        super(properties);
        this.addonId = addonId;
        this.declaration = declaration;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SATISFIED, false));
    }

    public String addonId() { return addonId; }

    public LayoutPart declaration() { return declaration; }

    public boolean functional() { return declaration.functional(); }

    /** Null when this part does not emit. */
    @Nullable public EnergyOutput energy() { return declaration.energy(); }

    /** Output buffer cap. Zero when this part does not emit. */
    public long capacity() { return declaration.energyCapacity(); }

    /** Null when this part does not accept energy. */
    @Nullable public EnergyInput input() { return declaration.energyInput(); }

    public long conditionPerTick() { return declaration.conditionPerTick(); }

    public boolean condition() { return declaration.condition(); }

//? if >=1.21.1 {
    /*@Override public com.mojang.serialization.MapCodec<AddonEnergyPartBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }
*///?} else {
//?}

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SATISFIED);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AddonEnergyPartBlockEntity(pos, state);
    }

    @Nullable
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != com.gregtech.gregtech.registry.GTBlockEntities.ADDON_ENERGY_PART.get()) return null;
        return (lvl, pos, blockState, be) -> AddonEnergyPartBlockEntity.serverTick(lvl, pos, blockState, (AddonEnergyPartBlockEntity) be);
    }

//? if >=1.21.1 {
/*    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state,
            Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (!declaration.functional()) return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return switch (FunctionalPartInteraction.interact(level, pos, player, hand, hit)) {
            case SUCCESS -> net.minecraft.world.ItemInteractionResult.SUCCESS;
            case CONSUME -> net.minecraft.world.ItemInteractionResult.CONSUME;
            default -> net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }

    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (!declaration.functional()) return net.minecraft.world.InteractionResult.PASS;
        return FunctionalPartInteraction.interact(level, pos, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
    }
*///?} else {
    @Override public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (!declaration.functional()) return net.minecraft.world.InteractionResult.PASS;
        return FunctionalPartInteraction.interact(level, pos, player, hand, hit);
    }
//?}

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof FunctionalPartRuntime.Host host && host.function() != null)
            host.function().dropInto(level, pos);
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity part)
            part.logisticsCovers().dropAll();
        super.onRemove(state, level, pos, next, moving);
    }

    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder context) {
        return java.util.List.of(new net.minecraft.world.item.ItemStack(asItem()));
    }
}
