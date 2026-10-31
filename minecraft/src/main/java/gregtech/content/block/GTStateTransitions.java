package gregtech.content.block;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Shares the Cartesian state index of a GT block instead of storing a neighbour matrix per state. */
public final class GTStateTransitions {
    private GTStateTransitions() {}

    private static final Map<Block, WeakReference<Index>> INDICES = new WeakHashMap<>();

    public static synchronized Table<Property<?>, Comparable<?>, BlockState> create(
            BlockState state, Map<Map<Property<?>, Comparable<?>>, ?> possibleStates) {
        // Leave other mods' state initialization and non-Cartesian definitions untouched.
        if (!state.getBlock().getClass().getName().startsWith("gregtech.")) return null;
        var reference = INDICES.get(state.getBlock());
        var index = reference == null ? null : reference.get();
        if (index == null) {
            index = Index.create(state, possibleStates);
            if (index == null) return null;
            INDICES.put(state.getBlock(), new WeakReference<>(index));
        }
        int ordinal = index.ordinal(state.getValues());
        if (ordinal < 0 || index.states[ordinal] != state) return null;
        return new Neighbours(index, ordinal);
    }

    private record Axis(int stride, Map<Comparable<?>, Integer> values) {}

    private record Index(Map<Property<?>, Axis> axes, BlockState[] states) {
        static Index create(BlockState sample, Map<Map<Property<?>, Comparable<?>>, ?> possibleStates) {
            var axes = new HashMap<Property<?>, Axis>();
            long count = 1;
            for (var property : sample.getProperties()) {
                var values = new HashMap<Comparable<?>, Integer>();
                for (var value : property.getPossibleValues()) values.put(value, values.size());
                if (values.isEmpty() || count * values.size() > Integer.MAX_VALUE) return null;
                axes.put(property, new Axis((int) count, Map.copyOf(values)));
                count *= values.size();
            }
            if (count != possibleStates.size()) return null;
            var index = new Index(Map.copyOf(axes), new BlockState[(int) count]);
            for (var entry : possibleStates.entrySet()) {
                if (!(entry.getValue() instanceof BlockState state) || state.getBlock() != sample.getBlock())
                    return null;
                int ordinal = index.ordinal(entry.getKey());
                if (ordinal < 0 || index.states[ordinal] != null) return null;
                index.states[ordinal] = state;
            }
            return index;
        }

        int ordinal(Map<Property<?>, Comparable<?>> values) {
            if (values.size() != axes.size()) return -1;
            int ordinal = 0;
            for (var entry : axes.entrySet()) {
                var raw = values.get(entry.getKey());
                if (raw == null) return -1;
                var value = entry.getValue().values.get(raw);
                if (value == null) return -1;
                ordinal += entry.getValue().stride * value;
            }
            return ordinal;
        }

        BlockState neighbour(int ordinal, Object property, Object value) {
            if (property == null || value == null) return null;
            var axis = axes.get(property);
            if (axis == null) return null;
            var next = axis.values.get(value);
            if (next == null) return null;
            int current = ordinal / axis.stride % axis.values.size();
            return current == next ? null : states[ordinal + (next - current) * axis.stride];
        }
    }

    private static final class Neighbours extends ForwardingTable<Property<?>, Comparable<?>, BlockState> {
        private final Index index;
        private final int ordinal;
        private volatile Table<Property<?>, Comparable<?>, BlockState> materialized;

        Neighbours(Index index, int ordinal) {
            this.index = index;
            this.ordinal = ordinal;
        }

        @Override
        public BlockState get(Object property, Object value) {
            var table = materialized;
            return table == null ? index.neighbour(ordinal, property, value) : table.get(property, value);
        }

        @Override
        protected synchronized Table<Property<?>, Comparable<?>, BlockState> delegate() {
            if (materialized == null) {
                // Normal state changes only call get(). Preserve the complete Table contract for
                // integrations inspecting or changing the private neighbour table through accessors.
                Table<Property<?>, Comparable<?>, BlockState> table = HashBasedTable.create();
                var state = index.states[ordinal];
                for (var entry : state.getValues().entrySet())
                    for (var value : entry.getKey().getPossibleValues())
                        if (!value.equals(entry.getValue()))
                            table.put(entry.getKey(), value, index.neighbour(ordinal, entry.getKey(), value));
                materialized = table.isEmpty() ? table : ArrayTable.create(table);
            }
            return materialized;
        }
    }
}
