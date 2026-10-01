package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.damage.GTDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Fire-related utilities for smeltery machines — proximity burn and fire spread.
 * Separate from {@link SmelteryBlockEntityHelper} so fire mechanics can be reused independently.
 */
public final class SmelteryFireHelper {
    private SmelteryFireHelper() {}

    /** Trigger proximity burn for nearby entities. GT6 baseline: temperature above ~350K burns. */
    public static void proximityBurn(Level level, BlockPos pos, long temperatureK) {
        if (level == null || level.isClientSide) return;
        if (temperatureK <= 350) return;

        float damage = Math.min(8.0F, (temperatureK - 320) / 40.0F);
        if (damage <= 0) return;

        AABB area = new AABB(pos).inflate(1.0 / 16.0);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            double dist = entity.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (dist <= 1.0) {
                entity.hurt(GTDamageTypes.heat(level), damage);
            }
        }
    }

    /**
     * Try to ignite nearby blocks when crucible contents vaporize at high temperature.
     * GT6 behavior: 7x7x7 cube centered on the crucible, 20% chance per position.
     */
    public static void tryIgniteNearby(Level level, BlockPos pos, long temperatureK) {
        if (level == null || level.isClientSide) return;
        if (temperatureK < 400) return;

        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (level.random.nextInt(5) != 0) continue;
                    BlockPos targetPos = pos.offset(dx, dy, dz);
                    if (level.isEmptyBlock(targetPos)) {
                        level.setBlock(targetPos, Blocks.FIRE.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
}
