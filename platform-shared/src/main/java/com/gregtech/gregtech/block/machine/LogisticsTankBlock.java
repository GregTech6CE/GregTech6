package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.machine.TankSpec;
import com.gregtech.gregtech.blockentity.machine.LogisticsTankBlockEntity;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** The GT6 logistics tank has its own network-capable block entity, separate from ordinary drums. */
public final class LogisticsTankBlock extends TankBlock {
    public LogisticsTankBlock(TankSpec spec, Properties properties) { super(spec, properties); }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogisticsTankBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != GTBlockEntities.LOGISTICS_TANK.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<LogisticsTankBlockEntity>)
                (world, pos, blockState, tank) -> TankBlockEntity.serverTick(world, pos, blockState, tank);
    }
}
