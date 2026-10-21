package com.gregtech.gregtech.api.addon;

import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * One addon fuel generator. It has no tier. Output is a {@link FuelOutput}, not an
 * {@link EnergyOutput}. A single block is the default. Tank capacity defaults to 4000.
 */
public final class FuelGenerator implements AddonMachine {
    /** Furthest fixed output, matching the room past a gas turbine's rear wall. */
    public static final int MAX_BEHIND = 8;
    /** Same tank bound as a behavior machine. */
    public static final int MAX_TANK = 1_024_000_000;

    private final MachineCasing casing;
    private final FuelOutput output;
    private final String fuelMap;
    private final int efficiency;
    private final int tankCapacity;
    private final boolean multi;

    private FuelGenerator(MachineCasing casing, FuelOutput output, String fuelMap, int efficiency,
                          int tankCapacity, boolean multi) {
        this.casing = casing;
        this.output = output;
        this.fuelMap = fuelMap;
        this.efficiency = efficiency;
        this.tankCapacity = tankCapacity;
        this.multi = multi;
    }

    @Override public MachineCasing casing() { return casing; }
    public FuelOutput output() { return output; }
    public String fuelMap() { return fuelMap; }
    public int efficiency() { return efficiency; }
    public int tankCapacity() { return tankCapacity; }
    public boolean single() { return !multi; }
    public boolean multi() { return multi; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FuelGenerator that)) return false;
        return efficiency == that.efficiency && tankCapacity == that.tankCapacity && multi == that.multi
                && casing.equals(that.casing) && output.equals(that.output) && fuelMap.equals(that.fuelMap);
    }

    @Override public int hashCode() {
        return Objects.hash(casing, output, fuelMap, efficiency, tankCapacity, multi);
    }

    public static Builder builder(MachineCasing casing) { return new Builder(casing); }

    public static final class Builder {
        private final MachineCasing casing;
        private FuelOutput output;
        private EnergyInput energy;
        private EnergyOutput energyOutput;
        private MachineIo io;
        private String fuelMap;
        private int efficiency = 10_000;
        private int tankCapacity = 4_000;
        private boolean multi;

        private Builder(MachineCasing casing) { this.casing = casing; }

        public Builder output(FuelOutput value) { output = value; return this; }
        public Builder energy(EnergyInput value) { energy = value; return this; }
        public Builder energyOutput(EnergyOutput value) { energyOutput = value; return this; }
        public Builder io(MachineIo value) { io = value; return this; }
        public Builder fuelMap(String value) { fuelMap = value; return this; }
        public Builder efficiency(int value) { efficiency = value; return this; }
        public Builder tank(int millibuckets) { tankCapacity = millibuckets; return this; }
        public Builder single() { multi = false; return this; }
        public Builder multi() { multi = true; return this; }

        public FuelGenerator build() {
            if (casing == null) throw new IllegalArgumentException("Missing casing");
            if (casing.tierSpecified())
                throw new IllegalArgumentException("Fuel generators cannot declare a tier");
            if (energy != null)
                throw new IllegalArgumentException("Fuel generators cannot declare energy input");
            if (energyOutput != null)
                throw new IllegalArgumentException("Fuel generators cannot declare energy output");
            if (io != null)
                throw new IllegalArgumentException("Fuel generators cannot declare item and fluid faces");
            String id = casing.id();
            String machineType = casing.machineType() == null ? id : casing.machineType();
            if (!machineType.matches("[a-z0-9_]+"))
                throw new IllegalArgumentException("Invalid machine type for " + id + ": " + machineType);
            if (fuelMap == null || !fuelMap.matches("[a-z0-9_.]+"))
                throw new IllegalArgumentException("Invalid fuel map name: " + fuelMap);
            if (output == null) throw new IllegalArgumentException("Missing fuel output for " + id);
            if (efficiency < 1 || efficiency > 100_000)
                throw new IllegalArgumentException("Fuel efficiency out of range for " + id);
            if (tankCapacity < 1 || tankCapacity > MAX_TANK)
                throw new IllegalArgumentException("Invalid machine parameters for " + id);
            return new FuelGenerator(casing.resolve(machineType, false, 0), output, fuelMap, efficiency,
                    tankCapacity, multi);
        }
    }
}
