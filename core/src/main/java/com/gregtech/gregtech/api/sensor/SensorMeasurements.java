package com.gregtech.gregtech.api.sensor;

/**
 * Display units of the original sensors: the gauges that show a scaled value instead of a raw count.
 *
 * <p>The four weight-o-meters of GT6 differ <em>only</em> in this scale - the measurement code is the
 * same in all four classes, only the last line differs
 * ({@code gregtech/tileentity/sensors/MultiTileEntityWeightometer*.java:62}):
 *
 * <pre>
 *   Light      MAX_WEIGHT = (B[16]-1)/1000.0     return (long)(rWeightKG*1000);      "in Gramm"
 *   Medium     MAX_WEIGHT =  B[16]-1             return (long) rWeightKG;            "in Kilogramme"
 *   Heavy      MAX_WEIGHT = (B[16]-1)*1000.0     return (long)(rWeightKG/1000);      "in Tons"
 *   SuperHeavy MAX_WEIGHT = (B[16]-1)*1000000.0  return (long)(rWeightKG/1000000);   "in Kilotons"
 * </pre>
 *
 * <p>{@code B[16]} is GT6's {@code 1 << 16}, so every one of them saturates at {@code 65535} counts -
 * the six-cell display has four digits plus a hex mode, and {@code 0xFFFF} is what it can show. The
 * clamp below is therefore not a safety net but the original's own behaviour.
 */
public final class SensorMeasurements {
    private SensorMeasurements() {}

    /** GT6's {@code B[16]-1}: the largest value the six-cell display is built for. */
    public static final long MAX_COUNT = 65535;

    public static long timeOfDayMinutes(long ticks) { return ((Math.floorMod(ticks,24000)+6000)%24000)*60/1000; }
    public static long cubicMetres(long millibuckets) { return Math.max(0, millibuckets) / 1000; }
    public static long cubicDecametres(long millibuckets) { return Math.max(0, millibuckets) / 1_000_000; }
    public static long gibbl(long compressedVolume) { return Math.max(0, compressedVolume) / 1000; }
    public static long kiloGibbl(long compressedVolume) { return Math.max(0, compressedVolume) / 1_000_000; }

    /** GT6 {@code WeightometerHeavy:62}: {@code (long)(rWeightKG/1000)}, "in Tons". */
    public static long tonnes(double kilograms) { return clamp(kilograms / 1000); }

    /** GT6 {@code WeightometerLight:62}: {@code (long)(rWeightKG*1000)}, "in Gramm". */
    public static long grams(double kilograms) { return clamp(kilograms * 1000); }

    /** GT6 {@code WeightometerMedium:62}: {@code (long)rWeightKG}, "in Kilogramme". */
    public static long kilograms(double kilograms) { return clamp(kilograms); }

    /** GT6 {@code WeightometerSuperHeavy:62}: {@code (long)(rWeightKG/1000000)}, "in Kilotons". */
    public static long kilotonnes(double kilograms) { return clamp(kilograms / 1_000_000); }

    private static long clamp(double value) {
        return (long) Math.max(0, Math.min(MAX_COUNT, value));
    }
}
