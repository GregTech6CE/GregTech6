package gregtech.content.logistics;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Position-based, chunk-safe GT6 logistics flood fill. Never keeps block entities alive. */
public final class LogisticsNetwork {
    public record Snapshot(Set<BlockPos> positions, int farthestDistance) {
        public static Snapshot empty() {
            return new Snapshot(Set.of(), 0);
        }

        public boolean contains(BlockPos pos) {
            return positions.contains(pos);
        }

        public int size() {
            return positions.size();
        }
    }

    private LogisticsNetwork() {}

    /** Seed loaded physical hosts in the core cube, then cross mutual open sides without delegate lookup. */
    public static Snapshot scan(Level level, BlockPos controller, Direction front, int range) {
        int radius = LogisticsCoreLayout.RADIUS;
        BlockPos center = controller.relative(front.getOpposite(), radius);
        var visited = new LinkedHashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        for (int x = -radius; x <= radius; x++)
            for (int y = -radius; y <= radius; y++)
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    // GT6 seeds every ITileEntityLogistics inside the core cube, even
                    // a part whose faces are currently closed; the face test is only
                    // applied when crossing to a neighbouring position.
                    if (level.hasChunkAt(pos)
                            && level.getBlockEntity(pos) instanceof LogisticsHost
                            && visited.add(pos.immutable())) queue.add(pos.immutable());
                }

        int farthest = 0;
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            if (!(level.getBlockEntity(pos) instanceof LogisticsHost host)) continue;
            for (Direction side : Direction.values()) {
                if (!host.canLogistics(side)) continue;
                BlockPos neighbor = pos.relative(side);
                int distance = chebyshev(center, neighbor);
                if (distance > range || visited.contains(neighbor) || !level.hasChunkAt(neighbor)) continue;
                if (!(level.getBlockEntity(neighbor) instanceof LogisticsHost next)
                        || !next.canLogistics(side.getOpposite())) continue;
                visited.add(neighbor.immutable());
                queue.addLast(neighbor.immutable());
                farthest = Math.max(farthest, distance);
            }
        }
        return new Snapshot(Collections.unmodifiableSet(new LinkedHashSet<>(visited)), farthest);
    }

    public static int chebyshev(BlockPos origin, BlockPos target) {
        return Math.max(
                Math.abs(origin.getX() - target.getX()),
                Math.max(Math.abs(origin.getY() - target.getY()), Math.abs(origin.getZ() - target.getZ())));
    }
}
