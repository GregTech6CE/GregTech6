package gregtech.content.energy;

import gregtech.api.signal.GtSignalCarrier;

/** Current signal-wire arithmetic; world sampling, connectivity and publication stay native. */
public final class SignalWireRules {
    private static final long UNIT = GtSignalCarrier.UNIT;

    private SignalWireRules() {}

    public static long loss(int range) {
        return UNIT / range;
    }

    public static int comparator(long signal) {
        return (int) (signal / UNIT);
    }

    public static int vanillaOutput(long signal, boolean attenuated) {
        int power = (int) ((signal + UNIT - 1) / UNIT);
        return Math.max(0, power - (attenuated ? 1 : 0));
    }
}
