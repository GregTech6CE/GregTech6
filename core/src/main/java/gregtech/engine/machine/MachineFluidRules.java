package gregtech.engine.machine;

/** Fluid buffers of GT6's recipe-processing machines, independent of the fluid implementation. */
public final class MachineFluidRules {
    public static final long INPUT_BASE_CAPACITY = 1000;

    private MachineFluidRules() {}

    public static long inputCapacityMultiplier(int configuredParallel) {
        return Math.max(1, configuredParallel) * 2L;
    }

    /**
     * GT6 {@code MultiTileEntityBasicMachine.canOutput} checks the existing amount before starting
     * a batch. This is backpressure, not a tank capacity: the last batch may cross the threshold.
     */
    public static long outputBackpressure(int recipeOutputAmount, int configuredParallel) {
        return Math.max(16000L, 1L + Math.max(0, recipeOutputAmount) * (long) Math.max(1, configuredParallel));
    }
}
