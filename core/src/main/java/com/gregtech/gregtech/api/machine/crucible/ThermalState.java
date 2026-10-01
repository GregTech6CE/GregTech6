package com.gregtech.gregtech.api.machine.crucible;

/** Immutable integer-Kelvin thermal state; signed HU includes stored cooling energy. */
public record ThermalState(long temperatureK, long previousTemperatureK, long energyHU, int cooldownTicks) {
    public ThermalState {
        if (temperatureK < 0 || previousTemperatureK < 0)
            throw new IllegalArgumentException("Absolute Kelvin temperatures must be nonnegative");
        // Expired legacy countdowns may be negative; advance normalizes them like the original tick.
        // Every long energy value is representable, including valid CU accumulations at MIN_VALUE.
    }
}
