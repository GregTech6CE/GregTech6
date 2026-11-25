package gregtech.content.worldgen;

import gregtech.content.worldgen.WorldgenLocate.Area;
import gregtech.content.worldgen.WorldgenLocate.Hit;
import gregtech.content.worldgen.WorldgenLocate.Target;
import gregtech.platform.Mc;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.CompletionException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;

/** Reads existing region locations and section palettes without loading chunks into the world. */
final class SavedWorldgenBlocks {
    private static final int MAX_CHUNKS = 2048;

    private final Area area;
    private final Target target;
    private boolean incomplete;

    private SavedWorldgenBlocks(Area area, Target target) {
        this.area = area;
        this.target = target;
    }

    static Hit find(ServerLevel level, Area area, Target target, Set<Long> checked) {
        return new SavedWorldgenBlocks(area, target).find(level, checked);
    }

    private Hit find(ServerLevel level, Set<Long> checked) {
        Path root = level.getServer().getWorldPath(LevelResource.ROOT);
        Path path = DimensionType.getStorageFolder(level.dimension(), root).resolve("region");
        Comparator<ChunkPos> byDistance = Comparator.comparingDouble(pos ->
                area.origin().distSqr(pos.getMiddleBlockPosition(area.origin().getY())));
        var nearest = new PriorityQueue<ChunkPos>(MAX_CHUNKS, byDistance.reversed());
        int existing = 0;
        var header = ByteBuffer.allocate(4096);
        for (int rx = area.minX() >> 9; rx <= area.maxX() >> 9; rx++) {
            for (int rz = area.minZ() >> 9; rz <= area.maxZ() >> 9; rz++) {
                var file = path.resolve("r." + rx + "." + rz + ".mca");
                if (!Files.isRegularFile(file)) continue;
                header.clear();
                try (var channel = FileChannel.open(file, StandardOpenOption.READ)) {
                    while (header.hasRemaining() && channel.read(header) > 0) {}
                    if (header.hasRemaining()) throw new IOException("Truncated region location table");
                } catch (IOException error) {
                    incomplete = true;
                    Mc.logger().warn("Skipping unreadable region {} during worldgen locate", file, error);
                    continue;
                }
                header.flip();
                for (int i = 0; header.remaining() >= Integer.BYTES; i++) {
                    if (header.getInt() == 0) continue;
                    int x = (rx << 5) + (i & 31), z = (rz << 5) + (i >> 5);
                    if (x < (area.minX() >> 4)
                            || x > (area.maxX() >> 4)
                            || z < (area.minZ() >> 4)
                            || z > (area.maxZ() >> 4)
                            || checked.contains(ChunkPos.asLong(x, z))) continue;
                    existing++;
                    var pos = new ChunkPos(x, z);
                    if (nearest.size() < MAX_CHUNKS) nearest.add(pos);
                    else if (byDistance.compare(pos, nearest.element()) < 0) {
                        nearest.remove();
                        nearest.add(pos);
                    }
                }
            }
        }
        var chunks = new ArrayList<>(nearest);
        chunks.sort(byDistance);
        for (var pos : chunks) {
            if (!level.getServer().isRunning()) break;
            try {
                var data = level.getChunkSource().chunkMap.read(pos).join();
                if (data.isEmpty()) continue;
                var match = block(data.get(), pos);
                if (match != null) return new Hit(match, "saved");
            } catch (CompletionException error) {
                incomplete = true;
                Mc.logger()
                        .warn(
                                "Skipping unreadable chunk {} in {} during worldgen locate",
                                pos,
                                level.dimension().location(),
                                error);
            }
        }
        if (incomplete) return new Hit(null, "incomplete");
        return new Hit(null, existing > MAX_CHUNKS ? "limited" : "missing");
    }

    private BlockPos block(CompoundTag chunk, ChunkPos pos) {
        String status = chunk.getString("Status");
        if (!"minecraft:full".equals(status) && !"full".equals(status)) return null;
        var sections = chunk.getList("sections", Tag.TAG_COMPOUND);
        BlockPos best = null;
        for (int s = 0; s < sections.size(); s++) {
            var section = sections.getCompound(s);
            if (!section.contains("block_states", Tag.TAG_COMPOUND)) continue;
            int sectionY = section.getByte("Y");
            try {
                var match = section(section.getCompound("block_states"), pos, sectionY);
                if (match != null && (best == null || match.distSqr(area.origin()) < best.distSqr(area.origin())))
                    best = match;
            } catch (RuntimeException error) {
                incomplete = true;
                Mc.logger()
                        .warn(
                                "Skipping unreadable block palette in chunk {} section Y={} during worldgen locate",
                                pos,
                                sectionY,
                                error);
            }
        }
        return best;
    }

    private BlockPos section(CompoundTag states, ChunkPos pos, int sectionY) {
        var palette = states.getList("palette", Tag.TAG_COMPOUND);
        if (palette.isEmpty() || palette.size() > 4096)
            throw new IllegalArgumentException("Invalid block palette size " + palette.size());
        boolean[] matching = new boolean[palette.size()];
        boolean any = false;
        for (int p = 0; p < matching.length; p++) {
            var state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), palette.getCompound(p));
            matching[p] = target.test(state);
            any |= matching[p];
        }
        if (!any) return null;
        int bits = Math.max(4, Integer.SIZE - Integer.numberOfLeadingZeros(matching.length - 1));
        var indices = matching.length == 1 ? null : new SimpleBitStorage(bits, 4096, states.getLongArray("data"));
        int baseY = sectionY << 4;
        BlockPos best = null;
        for (int i = 0; i < 4096; i++) {
            int entry = indices == null ? 0 : indices.get(i);
            if (entry >= matching.length) throw new IllegalArgumentException("Invalid block palette index " + entry);
            if (!matching[entry]) continue;
            int x = pos.getMinBlockX() + (i & 15), z = pos.getMinBlockZ() + ((i >> 4) & 15);
            if (!area.contains(x, z)) continue;
            int y = baseY + (i >> 8);
            if (best == null || area.origin().distToLowCornerSqr(x, y, z) < best.distSqr(area.origin()))
                best = new BlockPos(x, y, z);
        }
        return best;
    }
}
