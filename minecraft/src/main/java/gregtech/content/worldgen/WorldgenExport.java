package gregtech.content.worldgen;

import gregtech.platform.Mc;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;

/** Spreads sampling across server ticks and publishes the completed snapshot on the I/O pool. */
final class WorldgenExport {
    private static final int CHUNKS_PER_TICK = 8;
    private static final long NANOS_PER_TICK = 2_000_000L;
    private static final Map<MinecraftServer, WorldgenExport> ACTIVE = new ConcurrentHashMap<>();

    private final CommandSourceStack source;
    private final WorldgenOreSample.Export sample;
    private CompletableFuture<Path> output;

    private WorldgenExport(CommandSourceStack source, int side) {
        this.source = source;
        sample = new WorldgenOreSample.Export(
                source.getLevel(), new ChunkPos(BlockPos.containing(source.getPosition())), side);
    }

    static int start(CommandSourceStack source, int side) {
        if (ACTIVE.putIfAbsent(source.getServer(), new WorldgenExport(source, side)) != null) {
            source.sendFailure(Component.translatable("commands.gt6ce.worldgen.export.busy"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.gt6ce.worldgen.export.started", side * side), false);
        return 1;
    }

    static void tick(MinecraftServer server) {
        var job = ACTIVE.get(server);
        if (job == null) return;
        try {
            if (job.advance()) ACTIVE.remove(server, job);
        } catch (RuntimeException error) {
            ACTIVE.remove(server, job);
            Mc.logger()
                    .warn(
                            "Could not export worldgen sample in {} at {}",
                            job.source.getLevel().dimension().location(),
                            job.sample.origin,
                            error);
            job.source.sendFailure(Component.translatable("commands.gt6ce.worldgen.export.error"));
        }
    }

    static void stop(MinecraftServer server) {
        ACTIVE.remove(server);
    }

    private boolean advance() {
        if (output != null) {
            if (!output.isDone()) return false;
            var path = output.join();
            source.sendSuccess(
                    () -> Component.translatable("commands.gt6ce.worldgen.export.done", sample.size(), path.toString()),
                    false);
            return true;
        }
        long start = System.nanoTime();
        for (int n = 0; n < CHUNKS_PER_TICK; n++) {
            var pos = sample.position(sample.size());
            var chunk = source.getLevel().getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk == null) {
                source.sendFailure(Component.translatable("commands.gt6ce.worldgen.export.unloaded", pos.x, pos.z));
                return true;
            }
            sample.add(chunk);
            if (sample.size() == sample.side * sample.side) {
                output = write(sample);
                break;
            }
            if (System.nanoTime() - start >= NANOS_PER_TICK) break;
        }
        return false;
    }

    private static CompletableFuture<Path> write(WorldgenOreSample.Export sample) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return sample.write();
                    } catch (IOException error) {
                        throw new UncheckedIOException(error);
                    }
                },
                Util.ioPool());
    }
}
