package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.addon.EnergyInput;
import com.gregtech.gregtech.api.addon.EnergyOutput;
import com.gregtech.gregtech.blockentity.machine.AddonEnergyPartBlockEntity;
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
 */
public class AddonEnergyPartBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty SATISFIED = BooleanProperty.create("satisfied");

    @Nullable private final EnergyOutput energy;
    private final long capacity;
    @Nullable private final EnergyInput input;
    private final long conditionPerTick;

    public AddonEnergyPartBlock(Properties properties, @Nullable EnergyOutput energy, long capacity,
                                 @Nullable EnergyInput input, long conditionPerTick) {
        super(properties);
        this.energy = energy;
        this.capacity = capacity;
        this.input = input;
        this.conditionPerTick = conditionPerTick;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SATISFIED, false));
    }

    /** Null when this part does not emit. */
    @Nullable public EnergyOutput energy() { return energy; }

    /** Output buffer cap. Zero when this part does not emit. */
    public long capacity() { return capacity; }

    /** Null when this part does not accept energy. */
    @Nullable public EnergyInput input() { return input; }

    public long conditionPerTick() { return conditionPerTick; }

    public boolean condition() { return input != null && conditionPerTick > 0; }

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

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
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
