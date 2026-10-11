/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original smeltery gas/fire/meltdown rules; preexisting companion proximity burn retained. */
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

    /** Retained companion proximity burn; its remaining source alignment is outside this crucible batch. */
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

    /** Original vapor effects use the material boiling point, before the thermal step. */
    public static void vaporize(Level level, BlockPos pos,
            com.gregtech.gregtech.api.machine.crucible.CrucibleHazards.Profile profile,
            com.gregtech.gregtech.api.machine.crucible.CrucibleProcess.Vapor vapor) {
        if (level == null || level.isClientSide) return;
        gasDamage(level, pos, profile, vapor.material().getBoilingPoint());
        igniteAttempts(level, pos, profile,
                com.gregtech.gregtech.api.machine.crucible.CrucibleHazards.vaporFireAttempts(
                        vapor.material().getBoilingPoint(), vapor.amount()));
    }
    private static void gasDamage(Level level, BlockPos pos,
            com.gregtech.gregtech.api.machine.crucible.CrucibleHazards.Profile profile, long temperature) {
        if (temperature < 320) return;
        int r = profile.range();
        AABB area = new AABB(pos.getX()-r, pos.getY()-1, pos.getZ()-r,
                pos.getX()+r+1, pos.getY()+r+1, pos.getZ()+r+1);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area))
            com.gregtech.gregtech.util.GTEntityHelper.applyTemperatureDamage(entity, temperature, profile.temperatureMultiplier());
    }
    public static void crucibleMeltdown(Level level, BlockPos pos,
            com.gregtech.gregtech.api.machine.crucible.CrucibleHazards.Profile profile, long temperature) {
        if (level == null || level.isClientSide) return;
        gasDamage(level, pos, profile, temperature);
        igniteAttempts(level, pos, profile,
                com.gregtech.gregtech.api.machine.crucible.CrucibleHazards.meltdownFireAttempts(temperature));
        // 1.7.10 flowing_lava metadata1 maps to modern LiquidBlock.LEVEL1.
        var lava = Blocks.LAVA.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 1);
        if (profile.largeVessel()) {
            for (int x=-1; x<=1; x++) for (int y=0; y<=2; y++) for (int z=-1; z<=1; z++)
                level.setBlock(pos.offset(x,y,z), lava, 3);
        } else level.setBlock(pos, lava, 3);
    }
    private static void igniteAttempts(Level level, BlockPos pos,
            com.gregtech.gregtech.api.machine.crucible.CrucibleHazards.Profile profile, int attempts) {
        for (int i=0; i<attempts; i++) {
            var target = com.gregtech.gregtech.api.machine.crucible.CrucibleHazards.fireTarget(profile, level.random::nextInt);
            igniteAt(level, pos.offset(target.x(), target.y(), target.z()), target.checkFlammability());
        }
    }
    /** WD.fire's collision/carpet, local flammability, GT block and neighbor checks.
     * The existing pipe-fire protection tag is the modern integration hook for protected magic nodes. */
    public static boolean igniteAt(Level level, BlockPos pos, boolean checkFlammability) {
        if (level == null || level.isClientSide) return false;
        var state = level.getBlockState(pos);
        var block = state.getBlock();
        if (state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)
                || block instanceof net.minecraft.world.level.block.BaseFireBlock
                || state.is(com.gregtech.gregtech.api.fluid.PipeIgnition.PROTECTED)) return false;
        if (!(block instanceof net.minecraft.world.level.block.CarpetBlock)
                && !state.getCollisionShape(level, pos).isEmpty()) return false;
        for (var face : net.minecraft.core.Direction.values())
            if (block.getFlammability(state, level, pos, face)>0)
                return level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
        if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("gregtech")) return false;
        if (!checkFlammability) return level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
        for (var face : net.minecraft.core.Direction.values()) {
            var neighborPos = pos.relative(face);
            var neighbor = level.getBlockState(neighborPos);
            if (neighbor.is(Blocks.CHEST) || neighbor.is(Blocks.TRAPPED_CHEST)
                    || neighbor.getBlock().getFlammability(neighbor, level, neighborPos, face.getOpposite())>0)
                return level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
        }
        return false;
    }
}
