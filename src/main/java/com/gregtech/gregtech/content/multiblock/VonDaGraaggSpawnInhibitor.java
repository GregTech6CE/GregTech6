package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.blockentity.machine.VonDaGraaggControllerBlockEntity;
import com.gregtech.gregtech.block.stone.GTStoneBlock;
import com.gregtech.gregtech.block.stone.StoneVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

/** Forge 1.20 equivalent of GT6's MOB_SPAWN_INHIBITORS list. */
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VonDaGraaggSpawnInhibitor {
    private static final WeakHashMap<ServerLevel, Set<BlockPos>> ACTIVE = new WeakHashMap<>();

    private VonDaGraaggSpawnInhibitor() {}

    public static void register(ServerLevel level, BlockPos pos) {
        ACTIVE.computeIfAbsent(level, ignored -> new HashSet<>()).add(pos.immutable());
    }

    public static void unregister(ServerLevel level, BlockPos pos) {
        Set<BlockPos> positions = ACTIVE.get(level);
        if (positions != null) positions.remove(pos);
    }

    /** GT6 checks a square horizontal range and exempts mossy cobblestone within ±5 blocks. */
    public static boolean inhibits(ServerLevel level, BlockPos controller, int range, BlockPos spawn) {
        if (range <= 0 || Math.abs(spawn.getX() - controller.getX()) > range
                || Math.abs(spawn.getZ() - controller.getZ()) > range) return false;
        for (int dy = -5; dy <= 5; dy++) {
            BlockPos nearby = spawn.offset(0, dy, 0);
            if (level.isInWorldBounds(nearby)) {
                var state = level.getBlockState(nearby);
                if (state.is(Blocks.MOSSY_COBBLESTONE)
                        || state.getBlock() instanceof GTStoneBlock gtStone
                        && gtStone.variant() == StoneVariant.COBBLE_MOSSY) return false;
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onSpawnPositionCheck(MobSpawnEvent.PositionCheck event) {
        ServerLevel level = event.getLevel().getLevel();
        Set<BlockPos> positions = ACTIVE.get(level);
        if (positions == null || positions.isEmpty()) return;
        BlockPos spawn = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        for (BlockPos controller : positions) {
            if (!level.hasChunkAt(controller)) continue;
            if (level.getBlockEntity(controller) instanceof VonDaGraaggControllerBlockEntity machine
                    && inhibits(level, controller, machine.currentRange(), spawn)) {
                event.setResult(Event.Result.DENY);
                return;
            }
        }
    }
}
