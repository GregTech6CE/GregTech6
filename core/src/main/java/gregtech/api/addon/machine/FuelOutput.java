/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;




import gregtech.api.addon.*;

import gregtech.api.energy.*;

import java.util.Objects;

/**
 * One fuel-generator packet and where it leaves. This is not an {@link EnergyOutput}:
 * the packet is a single per-tick amount, and the position is {@link Placement#FRONT},
 * {@link Placement#BACK}, or {@link Placement#BEHIND}.
 */
public final class FuelOutput {
    private final String type;
    private final long perTick;
    private final boolean front;
    private final boolean back;
    private final int behind;

    private FuelOutput(String type, long perTick, boolean front, boolean back, int behind) {
        this.type = type;
        this.perTick = perTick;
        this.front = front;
        this.back = back;
        this.behind = behind;
    }

    public String type() { return type; }

    public long perTick() { return perTick; }

    public boolean front() { return front; }

    public boolean back() { return back; }

    /** Zero when the packet leaves the front or the back face. Otherwise 1 through {@link FuelGenerator#MAX_BEHIND}. */
    public int behind() { return behind; }

    /** Front, back, or a distance behind the front. */
    public enum Placement { FRONT, BACK, BEHIND }

    public Placement placement() {
        if (front) return Placement.FRONT;
        if (back) return Placement.BACK;
        return Placement.BEHIND;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FuelOutput that)) return false;
        return perTick == that.perTick && front == that.front && back == that.back
                && behind == that.behind && type.equals(that.type);
    }

    @Override public String toString() {
        return "FuelOutput[type=" + type + ", perTick=" + perTick + ", placement="
                + (front ? "FRONT" : back ? "BACK" : "BEHIND(" + behind + ")") + "]";
    }

    @Override public int hashCode() {
        return Objects.hash(type, perTick, front, back, behind);
    }

    public static Builder builder(String type, long perTick) { return new Builder(type, perTick); }

    public static final class Builder {
        private final String type;
        private final long perTick;
        private boolean front = true;
        private boolean back;
        private int behind;

        private Builder(String type, long perTick) {
            this.type = type;
            this.perTick = perTick;
        }

        public Builder front() { front = true; back = false; behind = 0; return this; }

        public Builder back() { front = false; back = true; behind = 0; return this; }

        public Builder behind(int distance) { front = false; back = false; behind = distance; return this; }

        public FuelOutput build() {
            String resolved = EnergyType.require(type, "Missing fuel output type");
            if (perTick < 1 || perTick > AddonLimits.MAX_PER_TICK)
                throw new IllegalArgumentException("Fuel output per tick out of range: " + perTick);
            if (!front && !back) {
                if (behind < 1 || behind > FuelGenerator.MAX_BEHIND)
                    throw new IllegalArgumentException("Fuel output distance must be 1.."
                            + FuelGenerator.MAX_BEHIND + ": " + behind);
            } else if (behind != 0) {
                throw new IllegalArgumentException("Adjacent fuel output has no distance");
            }
            return new FuelOutput(resolved, perTick, front, back, behind);
        }
    }
}
