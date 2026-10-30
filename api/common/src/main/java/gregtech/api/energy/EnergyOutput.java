/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.energy;

import gregtech.api.machine.MachineFace;
import java.util.List;

/**
 * Energy leaving a behavior machine, or an energy structure part.
 * Faces, type and both packet limits are required. {@code remote()} allows a behavior output
 * with no faces, which is sent only with {@code MachineBehaviorContext.emitTo}.
 * {@code minSize} defaults to 1. {@code allowNegative()} is only for KU and RU, where the sign
 * is direction. A part also names a positive capacity on {@link gregtech.api.addon.machine.LayoutPart}.
 * Equality compares faces without order; {@link #faces()} stores them in {@link MachineFace#index()}
 * order, and the energy type is compared by its canonical name.
 *
 * @param type the energy type
 * @param faces emitting faces in {@link MachineFace#index()} order
 * @param minSize smallest accepted packet size
 * @param maxSize largest accepted packet size
 * @param maxAmount most energy one tick may emit
 * @param allowNegative true for KU or RU, where the sign is direction
 * @param remote true when the output may be declared without faces
 */
public record EnergyOutput(EnergyType type, List<MachineFace> faces, long minSize, long maxSize, long maxAmount,
                           boolean allowNegative, boolean remote) {
    public EnergyOutput {
        if (type == null) throw new IllegalArgumentException("Missing energy output type");
        if (maxSize < 1 || maxAmount < 1)
            throw new IllegalArgumentException("Energy output maximum must be positive");
        if (minSize < 1 || minSize > maxSize)
            throw new IllegalArgumentException("Energy output minimum must sit inside the maximum");
        if (allowNegative && !type.signedDirection())
            throw new IllegalArgumentException("Energy output type cannot be negative");
        faces = MachineFace.canonical(faces, "energy output face");
        if (faces.isEmpty() && !remote)
            throw new IllegalArgumentException("Energy output needs a face");
    }

    public static Builder builder(EnergyType type, long maxSize, long maxAmount) {
        return new Builder(type, maxSize, maxAmount);
    }

    public static final class Builder {
        private final EnergyType type;
        private final long maxSize;
        private final long maxAmount;
        private List<MachineFace> faces = List.of();
        private boolean facesSpecified;
        private long minSize = 1;
        private boolean allowNegative;
        private boolean remote;

        private Builder(EnergyType type, long maxSize, long maxAmount) {
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
            if (type == null) throw new IllegalArgumentException("Missing energy output type");
            if (maxSize < 1 || maxAmount < 1)
                throw new IllegalArgumentException("Energy output maximum must be positive");
            if (minSize < 1 || minSize > maxSize)
                throw new IllegalArgumentException("Energy output minimum must sit inside the maximum");
            if (allowNegative && !type.signedDirection())
                throw new IllegalArgumentException("Energy output type cannot be negative");
            var distinct = MachineFace.distinct(faces, "energy output face");
            if ((!facesSpecified || distinct.isEmpty()) && !remote)
                throw new IllegalArgumentException("Energy output needs a face");
            return new EnergyOutput(type, distinct, minSize, maxSize, maxAmount, allowNegative, remote);
        }
    }
}
