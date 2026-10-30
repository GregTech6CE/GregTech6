/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;

import gregtech.api.addon.AddonLimits;
import gregtech.api.energy.EnergyOutput;
import gregtech.api.energy.EnergyType;

/**
 * One fuel-generator packet and where it leaves. This is not an {@link EnergyOutput}:
 * the packet is a single per-tick amount, and the position is {@link Placement#FRONT},
 * {@link Placement#BACK}, or {@link Placement#BEHIND}. Equality compares the energy type by
 * its canonical name, the same as {@link EnergyOutput}.
 *
 * @param type the energy type
 * @param perTick energy emitted each tick
 * @param placement where the packet leaves
 * @param behind distance behind the front, or 0 for an adjacent placement
 */
public record FuelOutput(EnergyType type, long perTick, Placement placement, int behind) {
    public FuelOutput {
        if (type == null) throw new IllegalArgumentException("Missing fuel output type");
        if (perTick < 1 || perTick > AddonLimits.MAX_PER_TICK)
            throw new IllegalArgumentException("Fuel output per tick out of range: " + perTick);
        if (placement == null) throw new IllegalArgumentException("Missing fuel output placement");
        if (placement == Placement.BEHIND) {
            if (behind < 1 || behind > FuelGenerator.MAX_BEHIND)
                throw new IllegalArgumentException("Fuel output distance must be 1.."
                        + FuelGenerator.MAX_BEHIND + ": " + behind);
        } else if (behind != 0) {
            throw new IllegalArgumentException("Adjacent fuel output has no distance");
        }
    }

    public boolean front() { return placement == Placement.FRONT; }

    public boolean back() { return placement == Placement.BACK; }

    /** Front, back, or a distance behind the front. */
    public enum Placement { FRONT, BACK, BEHIND }

    public static Builder builder(EnergyType type, long perTick) { return new Builder(type, perTick); }

    public static final class Builder {
        private final EnergyType type;
        private final long perTick;
        private Placement placement = Placement.FRONT;
        private int behind;

        private Builder(EnergyType type, long perTick) {
            this.type = type;
            this.perTick = perTick;
        }

        public Builder front() { placement = Placement.FRONT; behind = 0; return this; }

        public Builder back() { placement = Placement.BACK; behind = 0; return this; }

        public Builder behind(int distance) { placement = Placement.BEHIND; behind = distance; return this; }

        public FuelOutput build() {
            return new FuelOutput(type, perTick, placement, behind);
        }
    }
}
