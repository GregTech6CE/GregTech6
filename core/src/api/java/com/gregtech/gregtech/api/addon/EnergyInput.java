package com.gregtech.gregtech.api.addon;

import java.util.List;
import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * Energy coming into a recipe machine or a behavior machine.
 * Packet bounds, accepted range, faces and capacity are checked by the template, not here:
 * those rules are not the same for the two templates.
 */
public final class EnergyInput {
    private final String type;
    private final long perTick;
    private final boolean rangeSpecified;
    private final long rangeMin;
    private final long rangeMax;
    private final boolean facesSpecified;
    private final List<MachineFace> faces;
    private final boolean capacitySpecified;
    private final long capacity;
    private final boolean anyPacket;

    private EnergyInput(String type, long perTick, boolean rangeSpecified, long rangeMin, long rangeMax,
                        boolean facesSpecified, List<MachineFace> faces, boolean capacitySpecified, long capacity,
                        boolean anyPacket) {
        this.type = type;
        this.perTick = perTick;
        this.rangeSpecified = rangeSpecified;
        this.rangeMin = rangeMin;
        this.rangeMax = rangeMax;
        this.facesSpecified = facesSpecified;
        this.faces = faces;
        this.capacitySpecified = capacitySpecified;
        this.capacity = capacity;
        this.anyPacket = anyPacket;
    }

    public String type() { return type; }

    public long perTick() { return perTick; }

    /** False until {@link Builder#range(long, long)} is called. Both bounds stay 0. */
    public boolean rangeSpecified() { return rangeSpecified; }

    public long rangeMin() { return rangeMin; }

    public long rangeMax() { return rangeMax; }

    /**
     * False when the builder did not name faces. A built machine still returns the template
     * default from {@link #faces()}: six faces for a recipe machine, every face but the front
     * for a behavior machine.
     */
    public boolean facesSpecified() { return facesSpecified; }

    /** An immutable copy. Empty when no faces were named and no template has filled the default. */
    public List<MachineFace> faces() { return List.copyOf(faces); }

    /** True when {@link Builder#capacity(long)} was called, including a call with zero. */
    public boolean capacitySpecified() { return capacitySpecified; }

    /**
     * Stored-energy cap for a behavior machine. Zero means the buffer is the rated input.
     * Recipe machines reject a specified capacity, so a built recipe machine returns 0 here.
     */
    public long capacity() { return capacity; }

    /**
     * True when {@link Builder#anyPacket()} was called. Packet size is not checked and this
     * input does not overload. A declared range cannot be combined with it.
     */
    public boolean anyPacket() { return anyPacket; }

    EnergyInput withFaces(List<MachineFace> resolved) {
        return new EnergyInput(type, perTick, rangeSpecified, rangeMin, rangeMax,
                facesSpecified, List.copyOf(resolved), capacitySpecified, capacity, anyPacket);
    }

    /** Zero capacity is the behavior-machine default, whether or not the builder named it. */
    EnergyInput collapseDefaultCapacity() {
        if (capacity != 0) return this;
        return new EnergyInput(type, perTick, rangeSpecified, rangeMin, rangeMax,
                facesSpecified, faces, false, 0, anyPacket);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EnergyInput that)) return false;
        return perTick == that.perTick && rangeSpecified == that.rangeSpecified
                && rangeMin == that.rangeMin && rangeMax == that.rangeMax
                && facesSpecified == that.facesSpecified && capacitySpecified == that.capacitySpecified
                && capacity == that.capacity && anyPacket == that.anyPacket
                && type.equals(that.type) && faces.equals(that.faces);
    }

    @Override public int hashCode() {
        return Objects.hash(type, perTick, rangeSpecified, rangeMin, rangeMax,
                facesSpecified, faces, capacitySpecified, capacity, anyPacket);
    }

    public static Builder builder(String type, long perTick) { return new Builder(type, perTick); }

    public static final class Builder {
        private final String type;
        private final long perTick;
        private boolean rangeSpecified;
        private long rangeMin;
        private long rangeMax;
        private boolean facesSpecified;
        private List<MachineFace> faces = List.of();
        private boolean capacitySpecified;
        private long capacity;
        private boolean anyPacket;

        private Builder(String type, long perTick) {
            this.type = type;
            this.perTick = perTick;
        }

        /** Accepted packet size. {@code 1 <= min <= max}. Not calling leaves the range unset. */
        public Builder range(long min, long max) {
            rangeSpecified = true;
            rangeMin = min;
            rangeMax = max;
            return this;
        }

        public Builder faces(MachineFace... faces) {
            facesSpecified = true;
            this.faces = faces == null ? List.of() : List.of(faces);
            return this;
        }

        public Builder faces(List<MachineFace> faces) {
            facesSpecified = true;
            this.faces = faces == null ? List.of() : List.copyOf(faces);
            return this;
        }

        /** Zero is a real value for a behavior machine. A recipe machine rejects any call. */
        public Builder capacity(long value) {
            capacitySpecified = true;
            capacity = value;
            return this;
        }

        /** Do not check packet size, and do not overload. Cannot be combined with {@link #range}. */
        public Builder anyPacket() {
            anyPacket = true;
            return this;
        }

        public EnergyInput build() {
            if (type == null || type.isBlank())
                throw new IllegalArgumentException("Missing energy type");
            if (rangeSpecified && (rangeMin < 1 || rangeMax < rangeMin))
                throw new IllegalArgumentException("Invalid energy range: " + rangeMin + ".." + rangeMax);
            if (anyPacket && rangeSpecified)
                throw new IllegalArgumentException("Energy input cannot both limit packets and accept any packet");
            if (capacitySpecified && capacity < 0)
                throw new IllegalArgumentException("Negative energy capacity");
            return new EnergyInput(type, perTick, rangeSpecified, rangeMin, rangeMax, facesSpecified,
                    MachineFace.distinct(faces, "energy face"), capacitySpecified, capacity, anyPacket);
        }
    }
}
