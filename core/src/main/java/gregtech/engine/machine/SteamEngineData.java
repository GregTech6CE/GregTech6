package gregtech.engine.machine;

/** Shared interface for {@link SteamEngineSpec} and {@link StrongSteamEngineSpec}. */
public sealed interface SteamEngineData extends EngineSpec permits SteamEngineSpec, StrongSteamEngineSpec {
    int efficiency();

    int steamCapacity();

    default long outputAtPressure(int state) {
        return outputRate() * (Math.max(0, Math.min(31, state)) + 1) / 16;
    }

    /** The first sampled pressure step whose output exceeds half of rated power, including rounding. */
    default double minimumRunningFill() {
        for (int state = 0; state < 32; state++) if (outputAtPressure(state) > outputRate() / 2) return state / 32.0;
        return 1;
    }

    @Override
    default EngineType type() {
        return EngineType.STEAM;
    }

    /** Steam consumed per operation (mB). */
    default int steamPerOp() {
        return 200;
    }
    /** Distilled water produced per operation (mB). */
    default int waterPerOp() {
        return 1;
    }
}
