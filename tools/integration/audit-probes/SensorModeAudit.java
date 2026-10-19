import com.gregtech.gregtech.api.sensor.SensorAverage;
import com.gregtech.gregtech.api.sensor.SensorControl;

/** Source-audit observations: current helpers versus the original UT.Code.scale contract. */
public final class SensorModeAudit {
    private static long originalScale(long value, long maximum) {
        return value <= 0 ? 0 : value >= maximum ? 15 : 1 + value * 14 / maximum;
    }

    private static void row(String name, SensorControl.Mode mode, long raw, long maximum, int threshold) {
        long sample = new SensorAverage().sample(raw);
        long originalMaximum = Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, maximum));
        var current = SensorControl.evaluate(mode, sample, maximum, threshold);
        long original = switch (mode) {
            case PERCENT -> originalScale(originalMaximum > 0 ? sample * 100 / originalMaximum : 0, 100);
            case SCALE -> originalScale(sample, threshold);
            case FULL -> sample >= originalMaximum ? 15 : 0;
            default -> throw new IllegalArgumentException("Audit mode");
        };
        System.out.println(name + "|raw=" + raw + "|sample=" + sample + "|maximum=" + maximum
                + "|current_signal=" + current.signal() + "|original_signal=" + original);
    }

    public static void main(String[] args) {
        row("PERCENT_1", SensorControl.Mode.PERCENT, 1, 100, 0);
        row("PERCENT_50", SensorControl.Mode.PERCENT, 50, 100, 0);
        row("SCALE_50", SensorControl.Mode.SCALE, 50, 100, 100);
        row("SCALE_ZERO_THRESHOLD", SensorControl.Mode.SCALE, 1, 100, 0);
        row("SUPERCONDUCTOR_FULL", SensorControl.Mode.FULL, 34359738368L, 34359738368L, 0);
    }
}
