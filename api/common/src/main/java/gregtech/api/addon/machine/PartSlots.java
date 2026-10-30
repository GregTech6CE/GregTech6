/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;

import gregtech.api.machine.MachineFace;
import java.util.List;

/**
 * Item slots on one structure part. Faces are relative to the part's horizontal facing.
 * An empty face list does not accept pipes on that action. A player click is
 * {@link LayoutPart.Builder#use()}, not a face. Equality compares each face list in its written order.
 *
 * @param count slot count
 * @param stackLimit per-slot stack limit
 * @param insertFaces faces that may insert
 * @param extractFaces faces that may extract
 * @param drops true when broken parts spill these stacks
 */
public record PartSlots(int count, int stackLimit, List<MachineFace> insertFaces, List<MachineFace> extractFaces,
                        boolean drops) {
    public static final int MAX_SLOTS = 16;
    public static final int MAX_STACK = 64;

    public PartSlots {
        if (count < 1 || count > MAX_SLOTS)
            throw new IllegalArgumentException("Part slots must be 1 to " + MAX_SLOTS + ": " + count);
        if (stackLimit < 1 || stackLimit > MAX_STACK)
            throw new IllegalArgumentException("Part stack limit must be 1 to " + MAX_STACK + ": " + stackLimit);
        insertFaces = MachineFace.distinct(insertFaces, "insert face");
        extractFaces = MachineFace.distinct(extractFaces, "extract face");
    }

    public static Builder builder(int count) { return new Builder(count); }

    public static final class Builder {
        private final int count;
        private int stackLimit = MAX_STACK;
        private List<MachineFace> insertFaces = List.of();
        private List<MachineFace> extractFaces = List.of();
        private boolean drops = true;

        private Builder(int count) { this.count = count; }

        public Builder stackLimit(int limit) { stackLimit = limit; return this; }

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
            return new PartSlots(count, stackLimit, insertFaces, extractFaces, drops);
        }
    }
}
