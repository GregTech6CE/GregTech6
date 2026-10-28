package gregtech.engine.sensor;

/**
 * Original eight mode identities and UT.Code.scale redstone endpoints.
 */
public final class SensorControl {
    public enum Mode {
        DISPLAY,
        PERCENT,
        GREATER,
        EQUAL,
        SMALLER,
        SCALE,
        FULL,
        NOT_FULL
    }

    public record Reading(long displayed, int signal) {}

    private SensorControl() {}

    public static Reading evaluate(Mode mode, long value, long maximum, int threshold) {
        long number = switch (mode) {
            case DISPLAY -> value;
            case PERCENT -> scaled(value, maximum, 100, Integer.MAX_VALUE);
            case FULL, NOT_FULL -> 100;
            default -> threshold;
        };
        int signal = switch (mode) {
            case DISPLAY -> 0;
            case PERCENT -> redstoneScale(number, 100);
            case GREATER -> value > threshold ? 15 : 0;
            case EQUAL -> value == threshold ? 15 : 0;
            case SMALLER -> value < threshold ? 15 : 0;
            case SCALE -> redstoneScale(value, threshold);
            case FULL -> value >= maximum ? 15 : 0;
            case NOT_FULL -> value < maximum ? 15 : 0;
        };
        return new Reading(number, signal);
    }
    /** Only empty/full readings use 0/15; partial nonzero readings use 1..14. */
    private static int redstoneScale(long value, long maximum) {
        if (value <= 0) return 0;
        if (value >= maximum) return 15;
        return 1 + (int) scaled(value, maximum, 14, 14);
    }
    /** Percentage display uses integer division, independently of the redstone scale. */
    private static long scaled(long value, long maximum, int factor, int limit) {
        if (maximum <= 0 || value <= 0) return 0;
        return java.math.BigInteger.valueOf(value)
                .multiply(java.math.BigInteger.valueOf(factor))
                .divide(java.math.BigInteger.valueOf(maximum))
                .min(java.math.BigInteger.valueOf(limit))
                .longValue();
    }

    public static int thresholdChange(double x, double y, boolean hex) {
        int sign = x >= 9 && x <= 11 ? -1 : x >= 12 && x <= 14 ? 1 : 0;
        int step =
                y >= 6 && y <= 8 ? (hex ? 256 : 100) : y >= 9 && y <= 11 ? (hex ? 16 : 10) : y >= 12 && y <= 14 ? 1 : 0;
        return sign * step;
    }

    public static boolean hasThreshold(Mode mode) {
        return mode == Mode.GREATER || mode == Mode.EQUAL || mode == Mode.SMALLER || mode == Mode.SCALE;
    }
}
