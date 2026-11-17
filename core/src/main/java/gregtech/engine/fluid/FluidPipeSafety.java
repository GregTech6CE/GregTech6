package gregtech.engine.fluid;

/** Pipe temperature rules from GT6 MultiTileEntityPipeFluid.onServerTickPre. */
public final class FluidPipeSafety {
    private FluidPipeSafety() {}

    /** First occupied channel replaces the previous tick's temperature, even when colder. */
    public static long observeTemperature(long current, long fluid, boolean first) {
        return first ? fluid : Math.max(current, fluid);
    }

    /** Only a tick with no occupied channel drifts, by exactly one kelvin. */
    public static long emptyTemperature(long current, long environment) {
        return current + Long.compare(environment, current);
    }
}
