package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.addon.EnergyOutput;
import com.gregtech.gregtech.blockentity.machine.AddonEnergyPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Addon energy part. Output faces are relative to {@link #FACING}. Casings do not use this block. */
public class AddonEnergyPartBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private final EnergyOutput energy;
    private final long capacity;

    public AddonEnergyPartBlock(Properties properties, EnergyOutput energy, long capacity) {
        super(properties);
        this.energy = energy;
        this.capacity = capacity;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public EnergyOutput energy() { return energy; }

    public long capacity() { return capacity; }

//? if >=1.21.1 {
    /*@Override public com.mojang.serialization.MapCodec<AddonEnergyPartBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }
*///?} else {
//?}

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
