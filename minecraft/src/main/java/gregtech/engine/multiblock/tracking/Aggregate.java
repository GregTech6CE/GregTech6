package gregtech.engine.multiblock.tracking;

import gregtech.engine.multiblock.MultiblockLayout.Role;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * A constraint over the cells of one compiled pattern. Implementations only read the cached choices,
 * so checking an aggregate never reads the world.
 */
public sealed interface Aggregate permits Aggregate.Count, Aggregate.SameBlock {
    boolean satisfied(Cells cells);

    /** Committed, matched cells of a pattern in layout order. Fixed cells come first, then repeat copies. */
    interface Cells {
        int size();
        int choice(int index);
        int part(int index);
        String plain(int index);
        Role role(int index);
        Block block(int index);
    }

    @FunctionalInterface
    interface CellSelector {
        boolean selects(int part, String plain, Role role);
    }

    static CellSelector part(int part) {
        return (candidate, plain, role) -> candidate == part;
    }

    static CellSelector plain(String plain) {
        return (candidate, candidatePlain, role) -> plain.equals(candidatePlain);
    }

    static CellSelector role(Role role) {
        return (candidate, plain, candidateRole) -> candidateRole == role;
    }

    static CellSelector all() {
        return (candidate, plain, role) -> true;
    }

    /** A count of selected matched cells, bounded inclusively. {@code max < 0} means no upper bound. */
    record Count(CellSelector selector, int min, int max) implements Aggregate {
        public Count {
            if (selector == null) selector = all();
            if (min < 0) throw new IllegalArgumentException("Aggregate minimum must be zero or positive");
            if (max >= 0 && max < min) throw new IllegalArgumentException("Aggregate maximum is below its minimum");
        }

        public static Count exactly(CellSelector selector, int count) {
            return new Count(selector, count, count);
        }

        public static Count atLeast(CellSelector selector, int min) {
            return new Count(selector, min, -1);
        }

        public static Count atMost(CellSelector selector, int max) {
            return new Count(selector, 0, max);
        }

        @Override public boolean satisfied(Cells cells) {
            int count = 0;
            for (int index = 0; index < cells.size(); index++) {
                if (cells.choice(index) < 0) continue;
                if (selector.selects(cells.part(index), cells.plain(index), cells.role(index))) count++;
            }
            return count >= min && (max < 0 || count <= max);
        }
    }

    /** Every selected matched cell must agree on one block identity. */
    record SameBlock(CellSelector selector) implements Aggregate {
        public SameBlock {
            if (selector == null) selector = all();
        }

        @Override public boolean satisfied(Cells cells) {
            Block first = null;
            for (int index = 0; index < cells.size(); index++) {
                if (cells.choice(index) < 0) continue;
                if (!selector.selects(cells.part(index), cells.plain(index), cells.role(index))) continue;
                Block block = cells.block(index);
                if (block == null) return false;
                if (first == null) first = block;
                else if (first != block) return false;
            }
            return true;
        }
    }

    static List<Aggregate> none() {
        return List.of();
    }
}
