package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.damage.GTDamageTypes;
import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.util.GTEntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Shared utilities for smeltery block entities (GT6 {@code TileEntityBase07Paintable} helpers). */
public final class SmelteryBlockEntityHelper {
    private SmelteryBlockEntityHelper() {}

    /** GT6 biome-temperature-to-Kelvin conversion. */
    public static long environmentTemperature(net.minecraft.world.level.LevelReader level, BlockPos pos) {
        if (level == null) return GregTechConstants.DEF_ENV_TEMP;
        float biomeTemp = level.getBiome(pos).value().getBaseTemperature();
        return GregTechConstants.C + Math.round(biomeTemp * 20.0F);
    }

    /** GT6 meltdown: replace block with flowing lava, damage nearby entities with heat. */
    public static void meltdown(Level level, BlockPos pos) {
        if (level == null || level.isClientSide) return;
        level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
        level.setBlock(pos, Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 4), Block.UPDATE_ALL);
        AABB area = new AABB(pos).inflate(0.5);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            entity.hurt(GTDamageTypes.heat(level), 4.0F);
        }
    }

    /** Send block update + BE data packet to nearby players within 64-block radius. */
    public static void syncToClient(BlockEntity be) {
        Level level = be.getLevel();
        if (level == null || level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        be.setChanged();
        BlockState state = be.getBlockState();
        level.sendBlockUpdated(be.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(be);
        double x = be.getBlockPos().getX() + 0.5;
        double y = be.getBlockPos().getY() + 0.5;
        double z = be.getBlockPos().getZ() + 0.5;
        double distSq = 64.0 * 64.0;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(x, y, z) <= distSq) {
                player.connection.send(packet);
            }
        }
    }

    public static byte directionToBit(Direction d) {
        return (byte) (1 << d.ordinal());
    }

    /**
     * Heat damage when picking up a hot solid without protection.
     *
     * <p>This used to be its own formula (a 320&nbsp;K threshold and {@code (T-320)/100} capped at 8).
     * GT6 has exactly one rule for "a surface of temperature T hurts to touch":
     * {@code UT.Entities.applyTemperatureDamage(entity, temperature, 1, 5.0F)} - above 320&nbsp;K it
     * deals {@code max(1, min(5, (T - 300) / 50))}, below 260&nbsp;K it freezes instead - and it also
     * applies GT6's protections (creative mode, a full heat/frost hazard suit, blazes and fire
     * resistance). The port now shares that rule with the fluid pipes and the burning boxes, so the
     * same molten metal cannot burn for 6.8 points here and 5.0 points there.
     */
    public static void applyHeatDamage(Player player, long temperatureK) {
        GTEntityHelper.applyContactTemperatureDamage(player, temperatureK);
    }
}
