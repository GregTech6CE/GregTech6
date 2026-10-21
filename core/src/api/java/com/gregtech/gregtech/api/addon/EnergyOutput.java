package com.gregtech.gregtech.api.addon;

import java.util.List;
import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * Energy leaving a behavior machine, or an energy structure part.
 * Faces, type and both packet limits are required. A part also names a positive capacity
 * on {@link LayoutPart}.
 */
public final class EnergyOutput {
    private final String type;
    private final List<MachineFace> faces;
    private final long maxSize;
    private final long maxAmount;

    private EnergyOutput(String type, List<MachineFace> faces, long maxSize, long maxAmount) {
        this.type = type;
        this.faces = faces;
        this.maxSize = maxSize;
        this.maxAmount = maxAmount;
    }

    public String type() { return type; }

    public List<MachineFace> faces() { return List.copyOf(faces); }

    public long maxSize() { return maxSize; }

    public long maxAmount() { return maxAmount; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EnergyOutput that)) return false;
        return maxSize == that.maxSize && maxAmount == that.maxAmount
                && type.equals(that.type) && faces.equals(that.faces);
    }

    @Override public int hashCode() {
        return Objects.hash(type, faces, maxSize, maxAmount);
    }

    public static Builder builder(String type, long maxSize, long maxAmount) {
        return new Builder(type, maxSize, maxAmount);
    }

    public static final class Builder {
        private final String type;
        private final long maxSize;
        private final long maxAmount;
        private List<MachineFace> faces = List.of();
        private boolean facesSpecified;

        private Builder(String type, long maxSize, long maxAmount) {
            this.type = type;
            this.maxSize = maxSize;
            this.maxAmount = maxAmount;
        }

        public Builder faces(MachineFace... faces) {
            facesSpecified = true;
            this.faces = faces == null ? List.of() : List.of(faces);
            return this;
        }

        public EnergyOutput build() {
            if (type == null || type.isBlank())
                throw new IllegalArgumentException("Missing energy output type");
            if (maxSize < 1 || maxAmount < 1)
                throw new IllegalArgumentException("Energy output maximum must be positive");
            var distinct = MachineFace.distinct(faces, "energy output face");
            if (!facesSpecified || distinct.isEmpty())
                throw new IllegalArgumentException("Energy output needs a face");
            return new EnergyOutput(type, distinct, maxSize, maxAmount);
        }
    }
}
