package com.gregtech.gregtech.api.addon;

import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * One addon fuel generator. {@code efficiency} is parts per 10000. {@code energy} is the output
 * short name, such as {@code RU}. {@code outputPerTick} is one packet. A multiblock uses a
 * {@link MachineLayout} whose name is {@code machineType}. Output is the front face, the back
 * face, or a fixed number of blocks behind the front. There is no arbitrary offset.
 */
public record FuelGeneratorDeclaration(
        String id,
        String material,
        String machineType,
        String fuelMap,
        int efficiency,
        String energy,
        long outputPerTick,
        int tankCapacity,
        Output output,
        Kind kind
) {
    /** Furthest fixed output, matching the room past a gas turbine's rear wall. */
    public static final int MAX_BEHIND = 8;
    /** Same tank bound as a behavior machine. */
    public static final int MAX_TANK = 1_024_000_000;

    public FuelGeneratorDeclaration {
        if (id == null || !id.matches("[a-z0-9/._-]+"))
            throw new IllegalArgumentException("Invalid machine ID: " + id);
        if (material == null || material.isBlank())
            throw new IllegalArgumentException("Missing casing material for " + id);
        if (machineType == null || !machineType.matches("[a-z0-9_]+"))
            throw new IllegalArgumentException("Invalid machine type for " + id + ": " + machineType);
        if (fuelMap == null || !fuelMap.matches("[a-z0-9_.]+"))
            throw new IllegalArgumentException("Invalid fuel map name: " + fuelMap);
        if (energy == null || energy.isBlank())
            throw new IllegalArgumentException("Missing energy type for " + id);
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(kind, "kind");
        if (efficiency < 1 || efficiency > 100_000)
            throw new IllegalArgumentException("Fuel efficiency out of range for " + id);
        if (outputPerTick < 1 || outputPerTick > 1_000_000_000_000L || tankCapacity < 1 || tankCapacity > MAX_TANK)
            throw new IllegalArgumentException("Invalid machine parameters for " + id);
    }

    public enum Kind { SINGLE, MULTI }

    /** Where one packet leaves. {@code BEHIND} injects into the block {@code distance} steps past the front. */
    public record Output(Side side, int distance) {
        public enum Side { FRONT, BACK, BEHIND }

        public Output {
            Objects.requireNonNull(side, "output");
            if (side == Side.BEHIND) {
                if (distance < 1 || distance > MAX_BEHIND)
                    throw new IllegalArgumentException("Fuel output distance must be 1.." + MAX_BEHIND + ": " + distance);
            } else if (distance != 0) {
                throw new IllegalArgumentException("Adjacent fuel output has no distance");
            }
        }

        public static Output front() { return new Output(Side.FRONT, 0); }
        public static Output back() { return new Output(Side.BACK, 0); }
        public static Output behind(int distance) { return new Output(Side.BEHIND, distance); }
    }

    public static Builder builder(String id, String material) { return new Builder(id, material); }

    public static final class Builder {
        private final String id;
        private final String material;
        private String machineType;
        private String fuelMap;
        private int efficiency = 10_000;
        private String energy;
        private long outputPerTick;
        private int tankCapacity = 4_000;
        private Output output = Output.front();
        private Kind kind = Kind.SINGLE;

        private Builder(String id, String material) {
            this.id = id;
            this.material = material;
            this.machineType = id;
        }

        public Builder machineType(String value) { machineType = value; return this; }
        public Builder fuelMap(String value) { fuelMap = value; return this; }
        public Builder efficiency(int value) { efficiency = value; return this; }
        public Builder energy(String shortName, long perTick) { energy = shortName; outputPerTick = perTick; return this; }
        public Builder tank(int millibuckets) { tankCapacity = millibuckets; return this; }
        public Builder outputFront() { output = Output.front(); return this; }
        public Builder outputBack() { output = Output.back(); return this; }
        public Builder outputBehind(int distance) { output = Output.behind(distance); return this; }
        public Builder single() { kind = Kind.SINGLE; return this; }
        public Builder multi() { kind = Kind.MULTI; return this; }

        public FuelGeneratorDeclaration build() {
            return new FuelGeneratorDeclaration(id, material, machineType, fuelMap, efficiency, energy,
                    outputPerTick, tankCapacity, output, kind);
        }
    }
}
