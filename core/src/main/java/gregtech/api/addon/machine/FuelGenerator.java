/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;




import gregtech.api.addon.*;

import gregtech.api.energy.*;

import gregtech.api.machine.*;

import java.util.Objects;

/**
 * One addon fuel generator. It has no tier. Output is a {@link FuelOutput}, not an
 * {@link EnergyOutput}. A single block is the default. Tank capacity defaults to 4000.
 */
public final class FuelGenerator implements AddonMachine {
    /** Furthest fixed output, matching the room past a gas turbine's rear wall. */
    public static final int MAX_BEHIND = AddonLimits.live(8);
    /** Most packets one tick may push. The default is one. */
    public static final int MAX_AMPERES = AddonLimits.live(64);
    /** Same tank bound as a behavior machine. */
    public static final int MAX_TANK = AddonLimits.live(AddonLimits.MAX_CAPACITY);

    private final MachineCasing casing;
    private final FuelOutput output;
    private final String fuelMap;
    private final int efficiency;
    private final int tankCapacity;
    private final boolean multi;
    private final int amperes;

    private FuelGenerator(MachineCasing casing, FuelOutput output, String fuelMap, int efficiency,
                          int tankCapacity, boolean multi, int amperes) {
        this.casing = casing;
        this.output = output;
        this.fuelMap = fuelMap;
        this.efficiency = efficiency;
        this.tankCapacity = tankCapacity;
        this.multi = multi;
        this.amperes = amperes;
    }

    @Override public MachineCasing casing() { return casing; }
    public FuelOutput output() { return output; }
    public String fuelMap() { return fuelMap; }
    public int efficiency() { return efficiency; }
    public int tankCapacity() { return tankCapacity; }
    public boolean single() { return !multi; }
    public boolean multi() { return multi; }
    /** Packets pushed in one tick. Defaults to 1. */
    public int amperes() { return amperes; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FuelGenerator that)) return false;
        return efficiency == that.efficiency && tankCapacity == that.tankCapacity && multi == that.multi
                && amperes == that.amperes
                && casing.equals(that.casing) && output.equals(that.output) && fuelMap.equals(that.fuelMap);
    }

    @Override public String toString() {
        return "FuelGenerator[casing=" + casing + ", output=" + output + ", fuelMap=" + fuelMap
                + ", efficiency=" + efficiency + ", tankCapacity=" + tankCapacity + ", multi=" + multi
                + ", amperes=" + amperes + "]";
    }

    @Override public int hashCode() {
        return Objects.hash(casing, output, fuelMap, efficiency, tankCapacity, multi, amperes);
    }

    public static Builder builder(MachineCasing casing) { return new Builder(casing); }

    public static final class Builder {
        private final MachineCasing casing;
        private FuelOutput output;
        private String fuelMap;
        private int efficiency = AddonLimits.DEFAULT_EFFICIENCY;
        private int tankCapacity = 4_000;
        private boolean multi;
        private int amperes = 1;

        private Builder(MachineCasing casing) { this.casing = casing; }

        public Builder output(FuelOutput value) { output = value; return this; }
        public Builder energy(EnergyInput value) {
            if (value != null) throw new IllegalArgumentException("Fuel generators cannot declare energy input");
            return this;
        }
        public Builder energyOutput(EnergyOutput value) {
            if (value != null) throw new IllegalArgumentException("Fuel generators cannot declare energy output");
            return this;
        }
        public Builder io(MachineIo value) {
            if (value != null) throw new IllegalArgumentException("Fuel generators cannot declare item and fluid faces");
            return this;
        }
        public Builder fuelMap(String value) { fuelMap = value; return this; }
        public Builder efficiency(int value) { efficiency = value; return this; }
        public Builder tank(int millibuckets) { tankCapacity = millibuckets; return this; }
        public Builder single() { multi = false; return this; }
        public Builder multi() { multi = true; return this; }
        /** Packets of {@code perTick} pushed in one tick. Defaults to 1. */
        public Builder amperes(int value) { amperes = value; return this; }

        public FuelGenerator build() {
            if (casing == null) throw new IllegalArgumentException("Missing casing");
            if (casing.tierSpecified())
                throw new IllegalArgumentException("Fuel generators cannot declare a tier");
            String id = casing.id();
            String machineType = casing.machineType() == null ? id : casing.machineType();
            if (!AddonNames.matches(AddonNames.MACHINE_TYPE, machineType))
                throw new IllegalArgumentException("Invalid machine type for " + id + ": " + machineType);
            if (!AddonNames.matches(AddonNames.RECIPE_MAP, fuelMap))
                throw new IllegalArgumentException("Invalid fuel map name: " + fuelMap);
            if (output == null) throw new IllegalArgumentException("Missing fuel output for " + id);
            if (efficiency < 1 || efficiency > AddonLimits.MAX_EFFICIENCY)
                throw new IllegalArgumentException("Fuel efficiency out of range for " + id);
            if (tankCapacity < 1 || tankCapacity > MAX_TANK)
                throw new IllegalArgumentException("Invalid tank capacity for " + id + ": " + tankCapacity);
            if (amperes < 1 || amperes > MAX_AMPERES)
                throw new IllegalArgumentException("Fuel amperes out of range for " + id + ": " + amperes);
            return new FuelGenerator(casing.resolve(machineType, false, 0), output, fuelMap, efficiency,
                    tankCapacity, multi, amperes);
        }
    }
}
