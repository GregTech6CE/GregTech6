package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.blockentity.CFoamBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Only wet GT6 construction foam has a drying block entity. Hardened foam is an ordinary block. */
public class FreshCFoamBlock extends CFoamBlock implements EntityBlock {
    public FreshCFoamBlock(Properties properties) { super(true, properties); }

    // Declared on the class that implements EntityBlock. Inherited copies are not remapped.
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return fresh() ? new CFoamBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!fresh() || level.isClientSide) return null;
        return (l, p, s, be) -> {
            if (be instanceof CFoamBlockEntity foam) foam.tick();
        };
    }
}
