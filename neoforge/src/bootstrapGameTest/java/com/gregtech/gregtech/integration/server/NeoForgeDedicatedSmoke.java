package com.gregtech.gregtech.integration.server;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;

/** Opt-in ordinary dedicated-server world restart smoke, excluded from the mod jar. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, value = Dist.DEDICATED_SERVER,
        bus = EventBusSubscriber.Bus.GAME)
public final class NeoForgeDedicatedSmoke {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PLATFORM = "neoforge";
    private static final String MINECRAFT_VERSION = "1.21.1";
    private static final String PHASE = System.getProperty("gregtech.integration.serverSmokePhase", "");
    private static final boolean ENABLED = !PHASE.isEmpty();
    private static final String SESSION_ID = UUID.randomUUID().toString();
    private static final int REQUIRED_TICKS = 200;
    private static final AtomicBoolean TERMINAL = new AtomicBoolean();
    private static volatile MinecraftServer activeServer;
    private static volatile ScheduledExecutorService watchdog;
    private static volatile long startedAt;
    private static volatile int observedTicks;
    private static volatile String specimenId;
    private static volatile BlockPos specimenPos;
    private static volatile Path worldRoot;
    // Only the server thread writes these lifecycle flags.
    private static boolean worldReadVerified;
    private static boolean stopRequested;
    private static boolean normalStoppingObserved;

    private NeoForgeDedicatedSmoke() {}

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        if (!ENABLED) return;
        activeServer = event.getServer();
        startedAt = System.nanoTime();
        try {
            if (!(activeServer instanceof DedicatedServer) || activeServer instanceof GameTestServer) {
                throw new IllegalStateException("Smoke requires an ordinary DedicatedServer, not GameTestServer");
            }
            if (!"prepare".equals(PHASE) && !"verify".equals(PHASE)) {
                throw new IllegalArgumentException("serverSmokePhase must be prepare or verify");
            }
            String configuredId = System.getProperty("gregtech.integration.serverSmokeId");
            if (configuredId == null || !UUID.fromString(configuredId).toString().equals(configuredId)) {
                throw new IllegalArgumentException("serverSmokeId must be an explicitly supplied canonical UUID");
            }
            specimenId = configuredId;
            worldRoot = activeServer.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            ServerLevel level = activeServer.overworld();
            specimenPos = position(level);
            watchdog = Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "gregtech-neoforge-dedicated-smoke-watchdog");
                thread.setDaemon(true);
                return thread;
            });
            watchdog.schedule(() -> fail("timeout", new TimeoutException(
                    "No completed dedicated phase and stop within 120 seconds of ServerStarted")),
                    120, TimeUnit.SECONDS);
            LOGGER.info("SERVER_SMOKE_STARTED {}", identity());
            // Explicitly load the real overworld chunk; no synthetic NBT round trip.
            level.getChunk(specimenPos);
            level.getChunk(specimenPos.east());
            if ("prepare".equals(PHASE)) {
                prepare(level);
                verifyWorld(level);
                LOGGER.info("SERVER_SMOKE_PREPARED {}", identity());
            } else {
                // Never create/repair the specimen in verify: it must have loaded from disk.
                verifyWorld(level);
                worldReadVerified = true;
                LOGGER.info("SERVER_SMOKE_WORLD_VERIFIED {}", identity());
            }
        } catch (Throwable failure) {
            fail("start_or_world_check", failure);
        }
    }

    private static BlockPos position(ServerLevel level) {
        String x = System.getProperty("gregtech.integration.serverSmokeX");
        String y = System.getProperty("gregtech.integration.serverSmokeY");
        String z = System.getProperty("gregtech.integration.serverSmokeZ");
        BlockPos pos;
        if (x == null && y == null && z == null) {
            pos = level.getSharedSpawnPos().offset(8, 12, 8);
        } else {
            if (x == null || y == null || z == null) {
                throw new IllegalArgumentException("Supply all serverSmokeX/Y/Z coordinates together");
            }
            pos = new BlockPos(Integer.parseInt(x), Integer.parseInt(y), Integer.parseInt(z));
        }
        if (Math.abs((long) pos.getX()) >= 29999983 || Math.abs((long) pos.getZ()) >= 29999983
                || level.isOutsideBuildHeight(pos) || level.isOutsideBuildHeight(pos.east())) {
            throw new IllegalArgumentException("Specimen coordinates are outside valid world bounds");
        }
        return pos.immutable();
    }

    private static void prepare(ServerLevel level) {
        if (!level.getBlockState(specimenPos).isAir() || !level.getBlockState(specimenPos.east()).isAir()) {
            throw new IllegalStateException("Prepare refuses to overwrite non-air blocks; use a fresh isolated world/position");
        }
        if (!level.setBlock(specimenPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL)
                || !level.setBlock(specimenPos.east(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL)) {
            throw new IllegalStateException("Could not place the real chest and adjacent dirt");
        }
        if (!(level.getBlockEntity(specimenPos) instanceof ChestBlockEntity chest)) {
            throw new IllegalStateException("Placed chest has no ChestBlockEntity");
        }
        // 1.21.1 names are applied through vanilla item components, not the removed setter.
        ItemStack namedChest = new ItemStack(Items.CHEST);
        namedChest.set(DataComponents.CUSTOM_NAME, Component.literal("gregtech-dedicated-smoke/" + specimenId));
        chest.applyComponentsFromItemStack(namedChest);
        chest.setItem(0, new ItemStack(Items.COPPER_INGOT, 3));
        chest.setItem(1, new ItemStack(Items.IRON_INGOT, 1));
        chest.setChanged();
    }

    private static void verifyWorld(ServerLevel level) {
        if (!level.getBlockState(specimenPos).is(Blocks.CHEST)
                || level.getBlockState(specimenPos).getValue(ChestBlock.TYPE) != ChestType.SINGLE
                || !level.getBlockState(specimenPos.east()).is(Blocks.DIRT)) {
            throw new IllegalStateException("Real chest block/type or adjacent dirt does not match specimen");
        }
        if (!(level.getBlockEntity(specimenPos) instanceof ChestBlockEntity chest)
                || chest.getContainerSize() != 27 || chest.getCustomName() == null
                || !("gregtech-dedicated-smoke/" + specimenId).equals(chest.getCustomName().getString())) {
            throw new IllegalStateException("Chest entity, inventory size or persisted specimen UUID does not match");
        }
        if (!chest.getItem(0).is(Items.COPPER_INGOT) || chest.getItem(0).getCount() != 3
                || !chest.getItem(1).is(Items.IRON_INGOT) || chest.getItem(1).getCount() != 1) {
            throw new IllegalStateException("Real chest inventory is not 3 copper ingots plus 1 iron ingot");
        }
        for (int slot = 2; slot < chest.getContainerSize(); slot++) {
            if (!chest.getItem(slot).isEmpty()) {
                throw new IllegalStateException("Unexpected item in specimen chest slot " + slot);
            }
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        tick(event.getServer());
    }

    private static void tick(MinecraftServer server) {
        if (!ENABLED || server != activeServer || TERMINAL.get() || stopRequested) return;
        try {
            observedTicks++;
            if (observedTicks < REQUIRED_TICKS) return;
            verifyWorld(server.overworld());
            stopRequested = true;
            LOGGER.info("SERVER_SMOKE_STOP_REQUESTED {}", identity());
            // The ordinary loop exits and stopServer saves players/chunks and closes levels.
            server.halt(false);
        } catch (Throwable failure) {
            fail("tick_or_stop", failure);
        }
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        if (!ENABLED || event.getServer() != activeServer) return;
        normalStoppingObserved = true;
        LOGGER.info("SERVER_SMOKE_STOPPING {}", identity());
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        if (!ENABLED || event.getServer() != activeServer) return;
        try {
            LOGGER.info("SERVER_SMOKE_STOPPED {}", identity());
            if (TERMINAL.get()) return;
            if (!stopRequested || !normalStoppingObserved || observedTicks != REQUIRED_TICKS) {
                throw new IllegalStateException("Server stopped without the requested 200-tick normal lifecycle");
            }
            Path levelData = worldRoot.resolve("level.dat");
            Path region = worldRoot.resolve("region").resolve("r." + (specimenPos.getX() >> 9)
                    + "." + (specimenPos.getZ() >> 9) + ".mca");
            if (!Files.isRegularFile(levelData) || Files.size(levelData) == 0) {
                throw new IOException("Stopped server has no nonempty level.dat: " + levelData);
            }
            requireSavedChunk(region);
            JsonObject result = identity();
            result.addProperty("normalStopObserved", true);
            result.addProperty("restartWorldReadVerified", worldReadVerified);
            result.addProperty("levelDat", levelData.toString());
            result.addProperty("levelDatBytes", Files.size(levelData));
            result.addProperty("region", region.toString());
            result.addProperty("regionBytes", Files.size(region));
            if (TERMINAL.compareAndSet(false, true)) LOGGER.info("SERVER_SMOKE_SUCCESS {}", result);
        } catch (Throwable failure) {
            // Already stopped; do not schedule another shutdown task.
            fail("stopped_or_saved_files", failure);
        } finally {
            if (watchdog != null) watchdog.shutdownNow();
        }
    }

    private static void requireSavedChunk(Path region) throws IOException {
        if (!Files.isRegularFile(region) || Files.size(region) < 8192) {
            throw new IOException("Specimen chunk has no valid region header: " + region);
        }
        int index = (specimenPos.getX() >> 4 & 31) + (specimenPos.getZ() >> 4 & 31) * 32;
        try (DataInputStream input = new DataInputStream(Files.newInputStream(region))) {
            input.skipNBytes(index * 4L);
            int location = input.readInt();
            int sector = location >>> 8;
            int count = location & 255;
            if (sector < 2 || count == 0 || (long) (sector + count) * 4096 > Files.size(region)) {
                throw new IOException("Specimen chunk has no allocated saved region sectors: " + region);
            }
        }
        // Region allocation is save evidence, not an NBT-content proof. The separate
        // verify JVM must load and check the actual block entity, UUID and all 27 slots.
    }

    private static void fail(String reason, Throwable failure) {
        if (!TERMINAL.compareAndSet(false, true)) return;
        JsonObject result = identity();
        result.addProperty("reason", reason);
        result.addProperty("error", failure.toString());
        LOGGER.error("SERVER_SMOKE_FAILED {}", result, failure);
        if (watchdog != null) watchdog.shutdownNow();
        MinecraftServer server = activeServer;
        if (server != null && !server.isStopped()) {
            try {
                server.execute(() -> server.halt(false));
            } catch (Throwable schedulingFailure) {
                LOGGER.error("SERVER_SMOKE_STOP_SCHEDULING_ERROR", schedulingFailure);
            }
        }
    }

    private static JsonObject identity() {
        JsonObject result = new JsonObject();
        result.addProperty("platform", PLATFORM);
        result.addProperty("minecraft", MINECRAFT_VERSION);
        result.addProperty("phase", PHASE);
        result.addProperty("sessionId", SESSION_ID);
        result.addProperty("pid", ProcessHandle.current().pid());
        result.addProperty("specimenId", specimenId);
        result.addProperty("serverClass", activeServer == null ? "unobserved" : activeServer.getClass().getName());
        result.addProperty("worldRoot", worldRoot == null ? null : worldRoot.toString());
        if (specimenPos != null) {
            result.addProperty("x", specimenPos.getX());
            result.addProperty("y", specimenPos.getY());
            result.addProperty("z", specimenPos.getZ());
            result.addProperty("dirtX", specimenPos.getX() + 1);
            result.addProperty("dirtY", specimenPos.getY());
            result.addProperty("dirtZ", specimenPos.getZ());
        }
        result.addProperty("copperCount", 3);
        result.addProperty("ironCount", 1);
        result.addProperty("observedTicks", observedTicks);
        result.addProperty("elapsedMs", startedAt == 0 ? 0
                : TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
        result.addProperty("restartWorldReadVerified", worldReadVerified);
        return result;
    }
}
