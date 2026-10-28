package gregtech.content.energy;

/** Display policy only: never used for boiler operation or explosion decisions. */
public final class BoilerGauge {
    private static final double[] COLOR_STOPS = {.5, .70, .80, .92, 1};
    private static final int[] COLORS = {0x188C16, 0x49C522, 0xE0D51A, 0xEF8A14, 0xE52323};

    private BoilerGauge() {}

    /** Green spans 50–70%, blending to yellow by 80%, then orange and red. */
    public static int colorAt(double position) {
        if (position < .5) return 0xB8B8B8;
        for (int i = 1; i < COLOR_STOPS.length; i++) {
            if (position > COLOR_STOPS[i]) continue;
            double mix = Math.max(0, (position - COLOR_STOPS[i - 1]) / (COLOR_STOPS[i] - COLOR_STOPS[i - 1]));
            int rgb = 0;
            for (int shift = 0; shift <= 16; shift += 8) {
                int from = COLORS[i - 1] >> shift & 255, to = COLORS[i] >> shift & 255;
                rgb |= (int) Math.round(from + (to - from) * mix) << shift;
            }
            return rgb;
        }
        return COLORS[COLORS.length - 1];
    }

    /** Once running, the warm-up half joins the start of the green pressure range. */
    public static int colorAt(double position, boolean running) {
        return running && position < .5 ? COLORS[0] : colorAt(position);
    }

    public static double fill(long steam, long capacity) {
        return capacity <= 0 ? 0 : Math.max(0, Math.min(1, (double) steam / capacity));
    }

    public static int percent(long steam, long capacity) {
        if (steam <= 0 || capacity <= 0) return 0;
        if (steam >= capacity) return 100;
        return java.math.BigInteger.valueOf(steam)
                .multiply(java.math.BigInteger.valueOf(100))
                .divide(java.math.BigInteger.valueOf(capacity))
                .intValue();
    }

    public static boolean highPressure(long steam, long capacity) {
        return capacity > 0 && steam >= capacity - capacity / 10;
    }

    /** OmniOcular's half-capacity running indication, with an explicit dry-hot warning. */
    public static String state(long steam, long capacity, long water, long heat) {
        if (water <= 0 && heat > 0) return "dry";
        if (capacity > 0 && steam >= capacity - capacity / 2) return "running";
        return water > 0 ? "preheating" : "stopped";
    }
}
