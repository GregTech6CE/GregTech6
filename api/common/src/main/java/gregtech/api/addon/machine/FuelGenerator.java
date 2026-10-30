/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;

import gregtech.api.addon.AddonLimits;
import gregtech.api.addon.AddonNames;
import gregtech.api.machine.MachineCasing;

/**
 * One addon fuel generator. It has no tier. Output is a {@link FuelOutput}, not an
 * {@link gregtech.api.energy.EnergyOutput}. A single block is the default. Tank capacity defaults to 4000.
 * The machine type is the id unchanged; call {@code machineType(...)} when the id is not a valid
 * machine type (for example it contains a slash).
 *
 * @param casing the resolved block casing
 * @param output the packet that leaves
 * @param fuelMap the fuel map name
 * @param efficiency processing efficiency in parts per 10,000
 * @param tankCapacity fuel tank capacity in millibuckets
 * @param multi true for a multiblock generator
 * @param amperes packets pushed in one tick
 */
public record FuelGenerator(MachineCasing casing, FuelOutput output, String fuelMap, int efficiency,
                            int tankCapacity, boolean multi, int amperes) implements AddonMachine {
    /** Furthest fixed output, matching the room past a gas turbine's rear wall. */
    public static final int MAX_BEHIND = value(8);
    /** Most packets one tick may push. The default is one. */
    public static final int MAX_AMPERES = value(64);
    /** Same tank bound as a behavior machine. */
    public static final int MAX_TANK = AddonLimits.MAX_CAPACITY;

    /** Keeps a value out of the compile-time constant pool so javac cannot inline it. */
    private static int value(int value) { return value; }

    public FuelGenerator {
        if (casing == null) throw new IllegalArgumentException("Missing casing");
        if (casing.tierSpecified())
            throw new IllegalArgumentException("Fuel generators cannot declare a tier");
        String id = casing.id();
        String machineType = casing.machineType() == null ? id : casing.machineType();
        if (!AddonNames.matches(AddonNames.MACHINE_TYPE, machineType))
            throw new IllegalArgumentException("Invalid machine type for " + id + ": " + machineType
                    + (casing.machineType() == null
                            ? "; call machineType(...) when the id is not a valid machine type" : ""));
        if (!AddonNames.matches(AddonNames.RECIPE_MAP, fuelMap))
            throw new IllegalArgumentException("Invalid fuel map name: " + fuelMap);
        if (output == null) throw new IllegalArgumentException("Missing fuel output for " + id);
        if (efficiency < 1 || efficiency > AddonLimits.MAX_EFFICIENCY)
            throw new IllegalArgumentException("Fuel efficiency out of range for " + id);
        if (tankCapacity < 1 || tankCapacity > MAX_TANK)
            throw new IllegalArgumentException("Invalid tank capacity for " + id + ": " + tankCapacity);
        if (amperes < 1 || amperes > MAX_AMPERES)
            throw new IllegalArgumentException("Fuel amperes out of range for " + id + ": " + amperes);
        casing = casing.resolve(machineType, false, 0);
    }

    public boolean single() { return !multi; }

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
        public Builder fuelMap(String value) { fuelMap = value; return this; }
        public Builder efficiency(int value) { efficiency = value; return this; }
        public Builder tank(int millibuckets) { tankCapacity = millibuckets; return this; }
        public Builder single() { multi = false; return this; }
        public Builder multi() { multi = true; return this; }
        /** Packets of {@code perTick} pushed in one tick. Defaults to 1. */
        public Builder amperes(int value) { amperes = value; return this; }

        public FuelGenerator build() {
            return new FuelGenerator(casing, output, fuelMap, efficiency, tankCapacity, multi, amperes);
        }
    }
}
