package gregtech.engine.recipe;

import java.util.BitSet;

/**
 * Index marks for one recipe input group. The bit set is allocated on the first mark, so recipes
 * without marks read nothing into existence; large indexes grow it normally.
 */
public final class RecipeInputMarks {
    /** Shared empty export; callers only read it. */
    public static final int[] NO_INDEXES = new int[0];

    private BitSet bits;

    public void set(int index) {
        if (index < 0) throw new IndexOutOfBoundsException("Input index " + index);
        if (bits == null) bits = new BitSet();
        bits.set(index);
    }

    public boolean get(int index) {
        return bits != null && bits.get(index);
    }

    public boolean isEmpty() {
        return bits == null || bits.isEmpty();
    }

    /** Marked indexes in ascending order. */
    public int[] toArray() {
        return bits == null ? NO_INDEXES : bits.stream().toArray();
    }
}
