package com.gregtech.gregtech.content.energy;

import java.util.function.BooleanSupplier;
import java.util.function.LongUnaryOperator;

/** GT6 MultiTileEntitySolarPanelElectric: one tick's output, never an accumulating battery. */
public final class SolarPanelEnergy {
    public record Conditions(boolean daytime, boolean raining, boolean thundering,
                             boolean twilightForest, float rainfall) {}
    private final long output;
    private long energy, timer;
    private boolean stopped, active, emitting, sky, check = true;

    public SolarPanelEnergy(long output) {
        if (output < 8) throw new IllegalArgumentException("Solar output must be at least 8");
        this.output = output;
    }

    public void tick(BooleanSupplier skyCheck, Conditions conditions, LongUnaryOperator emit) {
        timer++;
        // The source leaves its energy and running flags untouched while switched off.
        if (stopped) return;
        if (check || timer % 600 == 5) {
            check = false;
            sky = skyCheck.getAsBoolean();
        }
        energy = generation(output, sky, conditions);
        active = energy >= output / 8;
        emitting = active && emit.applyAsLong(energy) > 0;
        if (emitting) energy = 0;
    }

    public static long generation(long output, boolean sky, Conditions conditions) {
        if (!sky || conditions.thundering()) return 0;
        if (conditions.twilightForest()) return output / 2;
        boolean wetRain = conditions.raining() && conditions.rainfall() > 0;
        return conditions.daytime() ? (wetRain ? output / 8 : output) : (wetRain ? 0 : output / 8);
    }

    public void checkSky() { check = true; }
    public boolean enabled() { return !stopped; }
    public boolean enabled(boolean value) {
        if (stopped && value) check = true;
        stopped = !value;
        return value;
    }
    public long energy() { return energy; }
    public boolean active() { return active; }
    public boolean emitting() { return emitting; }

    /** Exact all-or-nothing source extraction, with safe bounds for invalid external requests. */
    public long extract(long size, long amount, boolean execute) {
        if (size == 0 || size == Long.MIN_VALUE || amount <= 0) return 0;
        long packet = Math.abs(size);
        if (amount > energy / packet) return 0;
        if (execute) energy -= packet * amount;
        return amount;
    }

    public void restore(long energy, boolean active, boolean emitting, boolean stopped) {
        // Migrate old port accumulators to at most one rated tick; never retain phantom storage.
        this.energy = Math.max(0, Math.min(output, energy));
        this.active = active;
        this.emitting = emitting;
        this.stopped = stopped;
        check = true;
    }
}
