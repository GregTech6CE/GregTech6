/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;




import gregtech.api.machine.*;

import java.util.List;
import java.util.Objects;

/**
 * Item slots on one structure part. Faces are relative to the part's horizontal facing.
 * An empty face list does not accept pipes on that action. A player click is
 * {@link LayoutPart.Builder#use()}, not a face. Equality compares each face list in its written order.
 */
public final class PartSlots {
    public static final int MAX_SLOTS = 16;
    public static final int MAX_STACK = 64;

    private final int count;
    private final int stackLimit;
    private final List<MachineFace> insertFaces;
    private final List<MachineFace> extractFaces;
    private final boolean drops;

    private PartSlots(int count, int stackLimit, List<MachineFace> insertFaces,
                      List<MachineFace> extractFaces, boolean drops) {
        this.count = count;
        this.stackLimit = stackLimit;
        this.insertFaces = insertFaces;
        this.extractFaces = extractFaces;
        this.drops = drops;
    }

    public int count() { return count; }

    public int stackLimit() { return stackLimit; }

    public List<MachineFace> insertFaces() { return insertFaces; }

    public List<MachineFace> extractFaces() { return extractFaces; }

    /** Broken parts spill these stacks. State items follow the same flag. */
    public boolean drops() { return drops; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PartSlots that)) return false;
        return count == that.count && stackLimit == that.stackLimit && drops == that.drops
                && insertFaces.equals(that.insertFaces) && extractFaces.equals(that.extractFaces);
    }

    @Override public String toString() {
        return "PartSlots[count=" + count + ", stackLimit=" + stackLimit + ", insertFaces=" + insertFaces
                + ", extractFaces=" + extractFaces + ", drops=" + drops + "]";
    }

    @Override public int hashCode() {
        return Objects.hash(count, stackLimit, insertFaces, extractFaces, drops);
    }

    public static Builder builder(int count) { return new Builder(count); }

    public static final class Builder {
        private final int count;
        private int stackLimit = MAX_STACK;
        private boolean stackSpecified;
        private List<MachineFace> insertFaces = List.of();
        private List<MachineFace> extractFaces = List.of();
        private boolean drops = true;

        private Builder(int count) { this.count = count; }

        public Builder stackLimit(int limit) { stackSpecified = true; stackLimit = limit; return this; }

        public Builder insert(MachineFace... faces) {
            insertFaces = MachineFace.distinct(MachineFace.listed(faces), "insert face");
            return this;
        }

        public Builder extract(MachineFace... faces) {
            extractFaces = MachineFace.distinct(MachineFace.listed(faces), "extract face");
            return this;
        }

        public Builder drops(boolean value) { drops = value; return this; }

        public PartSlots build() {
            if (count < 1 || count > MAX_SLOTS)
                throw new IllegalArgumentException("Part slots must be 1 to " + MAX_SLOTS + ": " + count);
            if (stackSpecified && (stackLimit < 1 || stackLimit > MAX_STACK))
                throw new IllegalArgumentException("Part stack limit must be 1 to " + MAX_STACK + ": " + stackLimit);
            return new PartSlots(count, stackLimit, insertFaces, extractFaces, drops);
        }
    }
}
