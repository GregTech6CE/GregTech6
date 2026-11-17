package gregtech.engine.energy;

/** The conversion ratios and rated buffer sizes shared by steam boilers. */
public final class BoilerRules {
    public static final int HU_PER_WATER = 80;
    public static final int STEAM_PER_WATER = 160;

    private BoilerRules() {}

    public static long bufferCapacity(long steamOutput) {
        return steamOutput * 10_000L;
    }

    public static long ratedHeatInput(long steamOutput) {
        return steamOutput / 2;
    }
}
