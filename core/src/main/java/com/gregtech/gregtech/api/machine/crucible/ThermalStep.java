/**
 * Copyright (c) 2026 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 *
 * Adapted from brokestar233's gregapi.util.CruciblePhysics thermal step and mixing structure.
 * Upstream algorithm author: Gregorius Techneticies; fixed GT6 revision and original notices
 * are recorded in core/provenance/thermal-extraction.json. This adaptation adds checked inputs
 * and overflow rejection, immutable state, and reuses the existing CrucibleMath primitive.
 */
package com.gregtech.gregtech.api.machine.crucible;

import java.util.Objects;

/** Thermal arithmetic only: no material conversion, alloy choice, capacity, hazards or world effects. */
public final class ThermalStep {
    public static final long KG_PER_ENERGY = 100;
    public static final int SUPPLY_COOLDOWN_TICKS = 100;
    public static final int PASSIVE_DRIFT_TICKS = 10;
    private static final double LONG_UPPER_BOUND = 0x1.0p63;

    public enum EnergyKind { HEAT, COOLING }
    private ThermalStep() {}

    /** Original mass is shell plus contents; each whole 100 kg adds one HU per Kelvin. */
    public static long requiredEnergy(double totalMassKg) {
        requireMass(totalMassKg, "totalMassKg");
        double scaled = totalMassKg / KG_PER_ENERGY;
        if (scaled >= LONG_UPPER_BOUND) throw new ArithmeticException("Required energy does not fit a long");
        return Math.addExact(1, (long) scaled);
    }

    /**
     * Captures pre-step temperature, consumes whole signed conversions, retains the remainder,
     * then applies the original supply window, one-Kelvin drift and min(200K,ambientK) floor.
     */
    public static ThermalState advance(ThermalState state, long ambientK, double totalMassKg) {
        Objects.requireNonNull(state, "state");
        requireKelvin(ambientK, "ambientK");
        long required = requiredEnergy(totalMassKg);
        long conversions = state.energyHU() / required;
        long energy = state.energyHU();
        long temperature = state.temperatureK();
        int cooldown = state.cooldownTicks() > 0 ? state.cooldownTicks() - 1 : state.cooldownTicks();
        if (conversions != 0) {
            energy = Math.subtractExact(energy, Math.multiplyExact(conversions, required));
            temperature = Math.addExact(temperature, conversions);
            cooldown = SUPPLY_COOLDOWN_TICKS;
        }
        if (cooldown <= 0) {
            cooldown = PASSIVE_DRIFT_TICKS;
            if (temperature > ambientK) temperature = Math.subtractExact(temperature, 1);
            if (temperature < ambientK) temperature = Math.addExact(temperature, 1);
        }
        temperature = Math.max(temperature, Math.min(200, ambientK));
        return new ThermalState(temperature, state.temperatureK(), energy, cooldown);
    }

    /**
     * Energy kind determines heating/cooling, independently of signed packet size. Direction,
     * supported energy tags and whether a platform accepts the packet remain platform decisions.
     */
    public static ThermalState receive(ThermalState state, EnergyKind kind, long packetSize, long packetCount) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(kind, "kind");
        if (packetCount < 0) throw new IllegalArgumentException("Packet count must be nonnegative");
        if (packetSize == Long.MIN_VALUE) throw new ArithmeticException("Packet magnitude cannot represent MIN_VALUE");
        long magnitude = Math.multiplyExact(Math.abs(packetSize), packetCount);
        long incoming = kind == EnergyKind.COOLING ? -magnitude : magnitude;
        long stored = Math.addExact(state.energyHU(), incoming);
        return new ThermalState(state.temperatureK(), state.previousTemperatureK(), stored, state.cooldownTicks());
    }

    /** Existing mass includes the shell. Retains the original long-cast mass quantization. */
    public static long mixTemperature(long existingK, long incomingK, double existingMassKg, double incomingMassKg) {
        requireKelvin(existingK, "existingK");
        requireKelvin(incomingK, "incomingK");
        requireMass(existingMassKg, "existingMassKg");
        requireMass(incomingMassKg, "incomingMassKg");
        double total = existingMassKg + incomingMassKg;
        requireMass(total, "totalMassKg");
        if (total == 0) return existingK;
        if (total >= LONG_UPPER_BOUND) throw new ArithmeticException("Mixing mass does not fit a long");
        long totalMass = (long) total;
        long existingMass = (long) existingMassKg;
        if (totalMass == 0) throw new IllegalArgumentException("Positive mixing mass must reach one whole kilogram");
        long delta = Math.abs(Math.subtractExact(existingK, incomingK));
        // CrucibleMath intentionally keeps its imported primitive behavior; this boundary rejects overflow.
        Math.multiplyExact(delta, existingMass);
        long retained = CrucibleMath.units(delta, totalMass, existingMass, false);
        return existingK > incomingK ? Math.addExact(incomingK, retained) : Math.subtractExact(incomingK, retained);
    }

    /** Mixing changes current temperature only; energy, prior-temperature and cooldown are untouched. */
    public static ThermalState mix(ThermalState state, long incomingK, double existingMassKg, double incomingMassKg) {
        Objects.requireNonNull(state, "state");
        return new ThermalState(mixTemperature(state.temperatureK(), incomingK, existingMassKg, incomingMassKg),
                state.previousTemperatureK(), state.energyHU(), state.cooldownTicks());
    }

    private static void requireMass(double massKg, String label) {
        if (!Double.isFinite(massKg) || massKg < 0) throw new IllegalArgumentException(label + " must be finite and nonnegative");
    }
    private static void requireKelvin(long temperatureK, String label) {
        if (temperatureK < 0) throw new IllegalArgumentException(label + " must be nonnegative absolute Kelvin");
    }
}
