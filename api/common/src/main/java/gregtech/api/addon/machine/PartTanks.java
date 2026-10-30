/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;

import gregtech.api.addon.AddonLimits;
import gregtech.api.machine.MachineFace;
import java.util.List;

/**
 * Fluid tanks on one structure part. Faces are relative to the part's horizontal facing.
 * An empty face list does not accept pipes on that action. A null side does not insert or extract.
 * Breaking the part does not spill the fluid. Equality compares each face list in its written order.
 *
 * @param count tank count
 * @param capacity capacity of each tank in millibuckets
 * @param insertFaces faces that may insert
 * @param extractFaces faces that may extract
 */
public record PartTanks(int count, int capacity, List<MachineFace> insertFaces, List<MachineFace> extractFaces) {
    public static final int MAX_TANKS = 8;
    public static final int MAX_CAPACITY = AddonLimits.MAX_CAPACITY;

    public PartTanks {
        if (count < 1 || count > MAX_TANKS)
            throw new IllegalArgumentException("Part tanks must be 1 to " + MAX_TANKS + ": " + count);
        if (capacity < 1 || capacity > MAX_CAPACITY)
            throw new IllegalArgumentException("Part tank capacity must be 1 to " + MAX_CAPACITY + ": " + capacity);
        insertFaces = MachineFace.distinct(insertFaces, "insert face");
        extractFaces = MachineFace.distinct(extractFaces, "extract face");
    }

    public static Builder builder(int count) { return new Builder(count); }

    public static final class Builder {
        private final int count;
        private int capacity;
        private List<MachineFace> insertFaces = List.of();
        private List<MachineFace> extractFaces = List.of();

        private Builder(int count) { this.count = count; }

        public Builder capacity(int millibuckets) { capacity = millibuckets; return this; }

        public Builder insert(MachineFace... faces) {
            insertFaces = MachineFace.distinct(MachineFace.listed(faces), "insert face");
            return this;
        }

        public Builder extract(MachineFace... faces) {
            extractFaces = MachineFace.distinct(MachineFace.listed(faces), "extract face");
            return this;
        }

        public PartTanks build() {
            return new PartTanks(count, capacity, insertFaces, extractFaces);
        }
    }
}
