package com.gregtech.gregtech.api.addon;

import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * One fuel-generator packet and where it leaves. This is not an {@link EnergyOutput}:
 * the packet is a single per-tick amount, and the position is the front, the back, or
 * a fixed distance behind the front.
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

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FuelOutput that)) return false;
        return perTick == that.perTick && front == that.front && back == that.back
                && behind == that.behind && type.equals(that.type);
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
            if (type == null || type.isBlank())
                throw new IllegalArgumentException("Missing fuel output type");
            if (perTick < 1 || perTick > 1_000_000_000_000L)
                throw new IllegalArgumentException("Fuel output per tick out of range: " + perTick);
            if (!front && !back) {
                if (behind < 1 || behind > FuelGenerator.MAX_BEHIND)
                    throw new IllegalArgumentException("Fuel output distance must be 1.."
                            + FuelGenerator.MAX_BEHIND + ": " + behind);
            } else if (behind != 0) {
                throw new IllegalArgumentException("Adjacent fuel output has no distance");
            }
            return new FuelOutput(type, perTick, front, back, behind);
        }
    }
}
