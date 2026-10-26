/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;




import gregtech.api.addon.*;

import gregtech.api.machine.*;

import java.util.List;
import java.util.Objects;

/**
 * Fluid tanks on one structure part. Faces are relative to the part's horizontal facing.
 * An empty face list does not accept pipes on that action. A null side does not insert or extract.
 * Breaking the part does not spill the fluid.
 */
public final class PartTanks {
    public static final int MAX_TANKS = 8;
    public static final int MAX_CAPACITY = AddonLimits.live(AddonLimits.MAX_CAPACITY);

    private final int count;
    private final int capacity;
    private final List<MachineFace> insertFaces;
    private final List<MachineFace> extractFaces;

    private PartTanks(int count, int capacity, List<MachineFace> insertFaces, List<MachineFace> extractFaces) {
        this.count = count;
        this.capacity = capacity;
        this.insertFaces = insertFaces;
        this.extractFaces = extractFaces;
    }

    public int count() { return count; }

    public int capacity() { return capacity; }

    public List<MachineFace> insertFaces() { return insertFaces; }

    public List<MachineFace> extractFaces() { return extractFaces; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PartTanks that)) return false;
        return count == that.count && capacity == that.capacity
                && insertFaces.equals(that.insertFaces) && extractFaces.equals(that.extractFaces);
    }

    @Override public String toString() {
        return "PartTanks[count=" + count + ", capacity=" + capacity
                + ", insertFaces=" + insertFaces + ", extractFaces=" + extractFaces + "]";
    }

    @Override public int hashCode() {
        return Objects.hash(count, capacity, insertFaces, extractFaces);
    }

    public static Builder builder(int count) { return new Builder(count); }

    public static final class Builder {
        private final int count;
        private int capacity;
        private boolean capacitySpecified;
        private List<MachineFace> insertFaces = List.of();
        private List<MachineFace> extractFaces = List.of();

        private Builder(int count) { this.count = count; }

        public Builder capacity(int millibuckets) { capacitySpecified = true; capacity = millibuckets; return this; }

        public Builder insert(MachineFace... faces) {
            insertFaces = MachineFace.distinct(MachineFace.listed(faces), "insert face");
            return this;
        }

        public Builder extract(MachineFace... faces) {
            extractFaces = MachineFace.distinct(MachineFace.listed(faces), "extract face");
            return this;
        }

        public PartTanks build() {
            if (count < 1 || count > MAX_TANKS)
                throw new IllegalArgumentException("Part tanks must be 1 to " + MAX_TANKS + ": " + count);
            if (!capacitySpecified || capacity < 1 || capacity > MAX_CAPACITY)
                throw new IllegalArgumentException("Part tank capacity must be 1 to " + MAX_CAPACITY + ": " + capacity);
            return new PartTanks(count, capacity, insertFaces, extractFaces);
        }
    }
}
