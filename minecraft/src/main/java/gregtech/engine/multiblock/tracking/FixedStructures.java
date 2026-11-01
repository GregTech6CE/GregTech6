package gregtech.engine.multiblock.tracking;

import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * A declarative definition over fixed cells. The controller supplies one list per facing; every list is
 * compiled into a pattern and into the watch footprint, then cached until the facing changes.
 */
public final class FixedStructures {
    private FixedStructures() {}

    @FunctionalInterface
    public interface Cells {
        List<CompiledPattern.FixedCell> forFacing(Direction front);
    }

    public static StructureDefinition.Declarative declarative(
            TrackedMultiblock machine, Cells cells, List<Aggregate> aggregates) {
        return new Definition(machine, cells, aggregates);
    }

    private record Snapshot(List<CompiledPattern.FixedCell> cells, Footprint footprint) {}

    private static final class Definition implements StructureDefinition.Declarative {
        private final TrackedMultiblock machine;
        private final Cells cells;
        private final List<Aggregate> aggregates;
        private final EnumMap<Direction, Snapshot> snapshots = new EnumMap<>(Direction.class);
        private final EnumMap<Direction, CompiledPattern> patterns = new EnumMap<>(Direction.class);
        private int epoch = -1;

        private Definition(TrackedMultiblock machine, Cells cells, List<Aggregate> aggregates) {
            this.machine = machine;
            this.cells = cells;
            this.aggregates = aggregates == null ? List.of() : List.copyOf(aggregates);
        }

        @Override
        public StructureDefinition.ClaimPolicy claimPolicy() {
            return StructureDefinition.ClaimPolicy.ATOMIC;
        }

        @Override
        public List<Aggregate> aggregates() {
            return aggregates;
        }

        @Override
        public CompiledPattern compile(Direction front) {
            refresh();
            var cached = patterns.get(front);
            if (cached != null) return cached;
            Snapshot snapshot = snapshot(front);
            var pattern = CompiledPattern.compileFixed(machine, front, snapshot.cells(), aggregates);
            patterns.put(front, pattern);
            return pattern;
        }

        @Override
        public Footprint watchFootprint(TrackedMultiblock ignored) {
            return snapshot(LayoutStructures.facing(machine.trackedEntity())).footprint();
        }

        @Override
        public Footprint declaredFootprint(TrackedMultiblock ignored) {
            return watchFootprint(ignored);
        }

        private Snapshot snapshot(Direction front) {
            refresh();
            Snapshot cached = snapshots.get(front);
            if (cached != null) return cached;
            List<CompiledPattern.FixedCell> list = cells.forFacing(front);
            Snapshot built = new Snapshot(list, footprint(list));
            snapshots.put(front, built);
            return built;
        }

        private void refresh() {
            if (epoch != StructureEpoch.get()) {
                patterns.clear();
                snapshots.clear();
                epoch = StructureEpoch.get();
            }
        }

        private Footprint footprint(List<CompiledPattern.FixedCell> list) {
            BlockPos origin = machine.trackedEntity().getBlockPos();
            Footprint.Builder builder = new Footprint.Builder().addController(origin);
            for (CompiledPattern.FixedCell cell : list) {
                Projection projection = cell.cell().role() == gregtech.engine.multiblock.MultiblockLayout.Role.AIR
                        ? Projection.AIR
                        : Projection.BLOCK;
                builder.add(origin.offset(cell.dx(), cell.dy(), cell.dz()), projection);
            }
            return builder.build();
        }
    }
}
