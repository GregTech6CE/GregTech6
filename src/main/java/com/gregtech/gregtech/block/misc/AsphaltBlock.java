package com.gregtech.gregtech.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Asphalt road block — applies a speed boost to entities walking on it. */
public class AsphaltBlock extends Block {
    public AsphaltBlock(Properties properties) { super(properties); }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity instanceof LivingEntity) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.2, 1.0, 1.2));
        }
        super.stepOn(level, pos, state, entity);
    }
}
