package com.gregtech.gregtech.api.fluid;

/** Independent physical hazard checks from GT6 MultiTileEntityPipeFluid.onServerTickPre. */
public final class FluidPipeSafety {
    private FluidPipeSafety() {}

    public record Losses(int gas, int plasma, int acid) {}

    /** WD.fire with checkFlammability=false: only carpet/no-collision targets may be replaced. */
    public static boolean canIgnite(boolean excluded, boolean eligibleShape, boolean gtBlock, boolean flammable) {
        return !excluded && eligibleShape && (!gtBlock || flammable);
    }

    /** Magic is independent of physical hazards; GT6 checks it before gas/plasma/acid. */
    public static int magicLoss(boolean magical, boolean gas, boolean magicProof) {
        return magical && !magicProof ? (gas ? 16 : 4) : 0;
    }

    public static Losses losses(boolean gas, boolean plasma, boolean acid,
                                boolean gasProof, boolean plasmaProof, boolean acidProof) {
        return new Losses(gas && !gasProof ? 8 : 0, plasma && !plasmaProof ? 64 : 0,
                acid && !acidProof ? 16 : 0);
    }

    /** First occupied channel replaces the previous tick's temperature, even when colder. */
    public static long observeTemperature(long current, long fluid, boolean first) {
        return first ? fluid : Math.max(current, fluid);
    }

    /** Only a tick with no occupied channel drifts, by exactly one kelvin. */
    public static long emptyTemperature(long current, long environment) {
        return current + Long.compare(environment, current);
    }
}
