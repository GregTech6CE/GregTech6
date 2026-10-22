package com.gregtech.gregtech.api.addon;

import java.util.List;
import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * Energy leaving a behavior machine, or an energy structure part.
 * Faces, type and both packet limits are required. {@code minSize} defaults to 1.
 * {@code allowNegative()} is only for KU and RU, where the sign is direction.
 * A part also names a positive capacity on {@link LayoutPart}.
 */
public final class EnergyOutput {
    private final String type;
    private final List<MachineFace> faces;
    private final long minSize;
    private final long maxSize;
    private final long maxAmount;
    private final boolean allowNegative;

    private EnergyOutput(String type, List<MachineFace> faces, long minSize, long maxSize, long maxAmount,
                         boolean allowNegative) {
        this.type = type;
        this.faces = faces;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.maxAmount = maxAmount;
        this.allowNegative = allowNegative;
    }

    public String type() { return type; }

    public List<MachineFace> faces() { return List.copyOf(faces); }

    /** Smallest accepted packet size. A negative packet is checked by its absolute value. */
    public long minSize() { return minSize; }

    public long maxSize() { return maxSize; }

    public long maxAmount() { return maxAmount; }

    /** True only for KU or RU. The sign is direction, not a negative amount of energy. */
    public boolean allowNegative() { return allowNegative; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EnergyOutput that)) return false;
        return minSize == that.minSize && maxSize == that.maxSize && maxAmount == that.maxAmount
                && allowNegative == that.allowNegative && type.equals(that.type) && faces.equals(that.faces);
    }

    @Override public int hashCode() {
        return Objects.hash(type, faces, minSize, maxSize, maxAmount, allowNegative);
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
        private long minSize = 1;
        private boolean allowNegative;

        private Builder(String type, long maxSize, long maxAmount) {
            this.type = type;
            this.maxSize = maxSize;
            this.maxAmount = maxAmount;
        }

        /** Inclusive lower bound. Defaults to 1 and must not exceed {@code maxSize}. */
        public Builder minSize(long value) { minSize = value; return this; }

        /** KU and RU may leave as a negative packet. The sign is direction. EU and HU reject this. */
        public Builder allowNegative() { allowNegative = true; return this; }

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
            if (minSize < 1 || minSize > maxSize)
                throw new IllegalArgumentException("Energy output minimum must sit inside the maximum");
            if (allowNegative && !signedDirection(type))
                throw new IllegalArgumentException("Energy output type cannot be negative");
            var distinct = MachineFace.distinct(faces, "energy output face");
            if (!facesSpecified || distinct.isEmpty())
                throw new IllegalArgumentException("Energy output needs a face");
            return new EnergyOutput(type, distinct, minSize, maxSize, maxAmount, allowNegative);
        }

        private static boolean signedDirection(String type) {
            return type.equalsIgnoreCase("KU") || type.equalsIgnoreCase("RU");
        }
    }
}
