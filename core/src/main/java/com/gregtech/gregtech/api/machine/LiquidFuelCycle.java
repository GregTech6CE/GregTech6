package com.gregtech.gregtech.api.machine;

/** Original GT6 liquid motor: one output packet per tick, then replenish below two packets. */
public final class LiquidFuelCycle {
    private LiquidFuelCycle() {}

    public static boolean needsFuel(long energy, long rate, boolean stopped) {
        return !stopped && rate > 0 && energy < Math.multiplyExact(rate, 2);
    }

    public static long recipeEnergy(long eut, long duration) {
        if (eut == Long.MIN_VALUE || duration <= 0) return 0;
        return Math.multiplyExact(Math.abs(eut), duration);
    }

    public static long credit(long energy, long cycleEnergy) {
        return Math.addExact(Math.max(0, energy), cycleEnergy);
    }

    /** GT6 deliberately spends an offered packet even when the receiver accepts none. */
    public static long afterEmission(long energy, long rate) {
        return rate > 0 && energy >= rate ? energy - rate : Math.max(0, energy);
    }
}
