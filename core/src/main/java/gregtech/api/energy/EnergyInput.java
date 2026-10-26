/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.energy;

import gregtech.api.machine.MachineFace;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Energy coming into a recipe machine or a behavior machine.
 * Packet bounds, accepted range, faces and capacity are checked by the template, not here:
 * those rules are not the same for the two templates.
 * Two declarations with the same faces in a different order are equal, while {@link #faces()}
 * keeps the written order. A capacity of zero is the behavior-machine default and is
 * indistinguishable from leaving it unset once the machine is built.
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
    private final boolean overload;
    private final boolean dynamic;

    private EnergyInput(String type, long perTick, boolean rangeSpecified, long rangeMin, long rangeMax,
                        boolean facesSpecified, List<MachineFace> faces, boolean capacitySpecified, long capacity,
                        boolean anyPacket, boolean overload, boolean dynamic) {
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
        this.overload = overload;
        this.dynamic = dynamic;
    }

    public String type() { return type; }

    /** True when the input locks the energy type from the first accepted packet. {@code type()} is then {@code *}. */
    public boolean dynamic() { return dynamic; }

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

    /**
     * An immutable copy. Empty when no faces were named and no template has filled the default.
     * Order is preserved as written and does not take part in equality.
     */
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

    /**
     * True unless {@link Builder#overload(boolean)} was called with false.
     * A behavior machine's main input then refuses a packet outside its window
     * and does not overload. The default still overloads above the maximum.
     */
    public boolean overload() { return overload; }

    public EnergyInput withFaces(List<MachineFace> resolved) {
        return new EnergyInput(type, perTick, rangeSpecified, rangeMin, rangeMax,
                facesSpecified, List.copyOf(resolved), capacitySpecified, capacity, anyPacket, overload, dynamic);
    }

    /** Zero capacity is the behavior-machine default, whether or not the builder named it. */
    public EnergyInput collapseDefaultCapacity() {
        if (capacity != 0) return this;
        return new EnergyInput(type, perTick, rangeSpecified, rangeMin, rangeMax,
                facesSpecified, faces, false, 0, anyPacket, overload, dynamic);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EnergyInput that)) return false;
        return perTick == that.perTick && rangeSpecified == that.rangeSpecified
                && rangeMin == that.rangeMin && rangeMax == that.rangeMax
                && facesSpecified == that.facesSpecified && capacitySpecified == that.capacitySpecified
                && capacity == that.capacity && anyPacket == that.anyPacket && overload == that.overload
                && dynamic == that.dynamic && EnergyType.same(type, that.type)
                && MachineFace.sameSet(faces, that.faces);
    }

    @Override public String toString() {
        return "EnergyInput[type=" + type + ", perTick=" + perTick
                + ", range=" + (rangeSpecified ? rangeMin + ".." + rangeMax : "unset")
                + ", faces=" + faces + ", capacity=" + (capacitySpecified ? capacity : "unset")
                + ", anyPacket=" + anyPacket + ", overload=" + overload + ", dynamic=" + dynamic + "]";
    }

    @Override public int hashCode() {
        return Objects.hash(type == null ? null : type.toLowerCase(Locale.ROOT), perTick,
                rangeSpecified, rangeMin, rangeMax, facesSpecified, MachineFace.hashSet(faces),
                capacitySpecified, capacity, anyPacket, overload, dynamic);
    }

    public static Builder builder(String type, long perTick) { return new Builder(type, perTick); }

    public static final class Builder {
        private String type;
        private final long perTick;
        private boolean rangeSpecified;
        private long rangeMin;
        private long rangeMax;
        private boolean facesSpecified;
        private List<MachineFace> faces = List.of();
        private boolean capacitySpecified;
        private long capacity;
        private boolean anyPacket;
        private boolean overload = true;
        private boolean dynamic;

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
            this.faces = MachineFace.listed(faces);
            return this;
        }

        public Builder faces(List<MachineFace> faces) {
            facesSpecified = true;
            this.faces = faces == null ? List.of() : MachineFace.listed(faces.toArray(new MachineFace[0]));
            return this;
        }

        /**
         * Zero is a real value for a behavior machine, and a built machine treats it as the unset
         * default. A recipe machine rejects any call.
         */
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

        /**
         * Lock the energy type from the first accepted packet. Only a behavior machine's main input
         * may use this; {@link #type()} becomes {@link EnergyType#ANY}. The constructor type is
         * replaced; {@code EnergyInput.builder("EU", 32).anyType()} is the usual form.
         */
        public Builder anyType() {
            dynamic = true;
            type = EnergyType.ANY;
            return this;
        }

        /**
         * {@code false} refuses a packet outside the accepted window and does not overload.
         * The default is {@code true}: a packet above the maximum is consumed and overloads.
         * A recipe machine and a condition part reject {@code false}.
         */
        public Builder overload(boolean enabled) {
            overload = enabled;
            return this;
        }

        public EnergyInput build() {
            String resolved = dynamic ? EnergyType.ANY : EnergyType.require(type);
            if (rangeSpecified && (rangeMin < 1 || rangeMax < rangeMin))
                throw new IllegalArgumentException("Invalid energy range: " + rangeMin + ".." + rangeMax);
            if (anyPacket && rangeSpecified)
                throw new IllegalArgumentException("Energy input cannot both limit packets and accept any packet");
            if (capacitySpecified && capacity < 0)
                throw new IllegalArgumentException("Negative energy capacity");
            return new EnergyInput(resolved, perTick, rangeSpecified, rangeMin, rangeMax, facesSpecified,
                    MachineFace.distinct(faces, "energy face"), capacitySpecified, capacity, anyPacket, overload, dynamic);
        }
    }
}
