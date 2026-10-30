/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.energy;

import gregtech.api.machine.MachineFace;
import java.util.List;

/**
 * Energy coming into a recipe machine or a behavior machine.
 * Packet bounds, accepted range, faces and capacity are checked by the template, not here:
 * those rules are not the same for the two templates.
 * Two declarations with the same faces in a different order are equal; {@link #faces()} stores
 * them in {@link MachineFace#index()} order. A capacity of zero is the behavior-machine default
 * and is stored as unspecified, so it is indistinguishable from leaving capacity unset.
 *
 * @param type the energy type; {@link EnergyType#ANY} exactly when {@link #dynamic()} is true
 * @param perTick energy consumed each tick
 * @param rangeMin smallest accepted packet, or 0 when no range was declared
 * @param rangeMax largest accepted packet, or 0 when no range was declared
 * @param facesSpecified true when the declaration named faces
 * @param faces accepted faces in {@link MachineFace#index()} order
 * @param capacity stored-energy cap, or 0 when unspecified
 * @param anyPacket true when packet size is not checked
 * @param overload true unless overload was disabled
 * @param dynamic true when the input locks its type from the first accepted packet
 */
public record EnergyInput(EnergyType type, long perTick, long rangeMin, long rangeMax,
                          boolean facesSpecified, List<MachineFace> faces, long capacity,
                          boolean anyPacket, boolean overload, boolean dynamic) {
    public EnergyInput {
        if (type == null) throw new IllegalArgumentException("Missing energy input type");
        if (rangeMin == 0 ? rangeMax != 0 : (rangeMin < 1 || rangeMax < rangeMin))
            throw new IllegalArgumentException("Invalid energy range: " + rangeMin + ".." + rangeMax);
        if (anyPacket && rangeMin != 0)
            throw new IllegalArgumentException("Energy input cannot both limit packets and accept any packet");
        if (capacity < 0)
            throw new IllegalArgumentException("Negative energy capacity");
        if (type.wildcard() != dynamic)
            throw new IllegalArgumentException(dynamic
                    ? "A dynamic energy input must use the wildcard type, not " + type
                    : "The wildcard energy type needs a dynamic input");
        faces = MachineFace.canonical(faces, "energy face");
    }

    /** False until {@link Builder#range(long, long)} is called. Both bounds stay 0. */
    public boolean rangeSpecified() { return rangeMin != 0; }

    /** True when a non-zero capacity is stored. A zero capacity is stored as unspecified. */
    public boolean capacitySpecified() { return capacity != 0; }

    /**
     * Returns a copy with {@code resolved} faces. Duplicate or null faces are rejected;
     * every other value, including a zero capacity, is kept as it is.
     */
    public EnergyInput withFaces(List<MachineFace> resolved) {
        return new EnergyInput(type, perTick, rangeMin, rangeMax, facesSpecified, resolved, capacity,
                anyPacket, overload, dynamic);
    }

    public static Builder builder(EnergyType type, long perTick) { return new Builder(type, perTick); }

    public static final class Builder {
        private EnergyType type;
        private final long perTick;
        private boolean rangeDeclared;
        private long rangeMin;
        private long rangeMax;
        private boolean facesSpecified;
        private List<MachineFace> faces = List.of();
        private long capacity;
        private boolean anyPacket;
        private boolean overload = true;
        private boolean dynamic;

        private Builder(EnergyType type, long perTick) {
            this.type = type;
            this.perTick = perTick;
        }

        /** Accepted packet size. {@code 1 <= min <= max}. Not calling leaves the range unset. */
        public Builder range(long min, long max) {
            rangeDeclared = true;
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
            if (faces == null)
                throw new IllegalArgumentException("Faces must not be null; pass an empty list or omit faces()");
            facesSpecified = true;
            this.faces = MachineFace.listed(faces.toArray(new MachineFace[0]));
            return this;
        }

        /**
         * Zero means the unset default and is stored as unspecified. A recipe machine rejects a
         * non-zero capacity.
         */
        public Builder capacity(long value) {
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
         * may use this; {@link #type()} becomes {@link EnergyType#ANY}. The type passed to
         * {@link #builder} is replaced; {@code EnergyInput.builder(EnergyType.EU, 32).anyType()}
         * is the usual form.
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
            if (type == null) throw new IllegalArgumentException("Missing energy input type");
            if (rangeDeclared && (rangeMin < 1 || rangeMax < rangeMin))
                throw new IllegalArgumentException("Invalid energy range: " + rangeMin + ".." + rangeMax);
            if (anyPacket && rangeDeclared)
                throw new IllegalArgumentException("Energy input cannot both limit packets and accept any packet");
            if (capacity < 0)
                throw new IllegalArgumentException("Negative energy capacity");
            return new EnergyInput(type, perTick, rangeMin, rangeMax, facesSpecified, faces, capacity,
                    anyPacket, overload, dynamic);
        }
    }
}
