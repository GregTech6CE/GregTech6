/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.energy;

import gregtech.api.machine.MachineFace;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Energy leaving a behavior machine, or an energy structure part.
 * Faces, type and both packet limits are required. {@code remote()} allows a behavior output
 * with no faces, which is sent only with {@code MachineBehaviorContext.emitTo}.
 * {@code minSize} defaults to 1. {@code allowNegative()} is only for KU and RU, where the sign
 * is direction. A part also names a positive capacity on {@link gregtech.api.addon.machine.LayoutPart}.
 * Equality compares faces without order and the energy type case-insensitively.
 */
public final class EnergyOutput {
    private final String type;
    private final List<MachineFace> faces;
    private final long minSize;
    private final long maxSize;
    private final long maxAmount;
    private final boolean allowNegative;
    private final boolean remote;

    private EnergyOutput(String type, List<MachineFace> faces, long minSize, long maxSize, long maxAmount,
                         boolean allowNegative, boolean remote) {
        this.type = type;
        this.faces = faces;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.maxAmount = maxAmount;
        this.allowNegative = allowNegative;
        this.remote = remote;
    }

    public String type() { return type; }

    public List<MachineFace> faces() { return List.copyOf(faces); }

    /** Smallest accepted packet size. A negative packet is checked by its absolute value. */
    public long minSize() { return minSize; }

    public long maxSize() { return maxSize; }

    public long maxAmount() { return maxAmount; }

    /** True only for KU or RU. The sign is direction, not a negative amount of energy. */
    public boolean allowNegative() { return allowNegative; }

    /**
     * True when this output may be declared without faces. Such an output does not emit to a
     * neighbor; the behavior sends it with {@code MachineBehaviorContext.emitTo}.
     * An output with faces may also set this flag and support both paths.
     */
    public boolean remote() { return remote; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EnergyOutput that)) return false;
        return minSize == that.minSize && maxSize == that.maxSize && maxAmount == that.maxAmount
                && allowNegative == that.allowNegative && remote == that.remote
                && EnergyType.same(type, that.type) && MachineFace.sameSet(faces, that.faces);
    }

    @Override public String toString() {
        return "EnergyOutput[type=" + type + ", faces=" + faces + ", minSize=" + minSize + ", maxSize=" + maxSize
                + ", maxAmount=" + maxAmount + ", allowNegative=" + allowNegative + ", remote=" + remote + "]";
    }

    @Override public int hashCode() {
        return Objects.hash(type == null ? null : type.toLowerCase(Locale.ROOT),
                MachineFace.hashSet(faces), minSize, maxSize, maxAmount, allowNegative, remote);
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
        private boolean remote;

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
            this.faces = MachineFace.listed(faces);
            return this;
        }

        /**
         * Allows this output to be declared without faces, for a behavior machine that sends
         * with {@code MachineBehaviorContext.emitTo}. It does not add a face. A structure part
         * cannot use it.
         */
        public Builder remote() {
            remote = true;
            return this;
        }

        public EnergyOutput build() {
            String resolved = EnergyType.require(type, "Missing energy output type");
            if (maxSize < 1 || maxAmount < 1)
                throw new IllegalArgumentException("Energy output maximum must be positive");
            if (minSize < 1 || minSize > maxSize)
                throw new IllegalArgumentException("Energy output minimum must sit inside the maximum");
            if (allowNegative && !EnergyType.signedDirection(resolved))
                throw new IllegalArgumentException("Energy output type cannot be negative");
            var distinct = MachineFace.distinct(faces, "energy output face");
            if ((!facesSpecified || distinct.isEmpty()) && !remote)
                throw new IllegalArgumentException("Energy output needs a face");
            return new EnergyOutput(resolved, distinct, minSize, maxSize, maxAmount, allowNegative, remote);
        }
    }
}
