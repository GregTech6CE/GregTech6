package com.gregtech.gregtech.api.energy;

/**
 * GT6's voltage tier table ({@code CS.V}) and tier lookups ({@code UT.Code.tier}/{@code tierMax}/
 * {@code tierMin}), ported because several GT6 mechanics are expressed in <em>tier indices</em>
 * rather than volts - most visibly {@code UT.Entities.applyElectricityDamage}, whose damage is
 * {@code tierMax(voltage) * amperage * 4} (so an ULV wire carrying 8&nbsp;V deals nothing at all and a
 * 512&nbsp;V wire deals three times an LV one, per ampere).
 *
 * <p>The port's machine tiers use the same ladder ({@code BasicMachineOriginalParams} gives tier 5 the
 * 8192&nbsp;V machines), so the indices line up with GT6's {@code MT}/{@code RM} tier numbering.
 */
public final class GTVoltageTiers {
    /** GT6 {@code CS.V} - the maximum voltage of each tier, ULV first. */
    public static final long[] VOLTAGES = {
            8L, 32L, 128L, 512L, 2048L, 8192L, 32768L, 131072L, 524288L, 2097152L,
            8388608L, 33554432L, 134217728L, 536870912L, 2147483648L, 8589934592L};

    /** GT6 tier names in {@link #VOLTAGES} order (see {@code WireTooltips}' ladder). */
    public static final String[] NAMES = {
            "ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV",
            "UEV", "UIV", "UMV", "UXV", "MAX", "MAX+"};

    private GTVoltageTiers() {}

    /** GT6 {@code UT.Code.tier}/{@code tierMax}: the index of the tier {@code size} belongs to. */
    public static int tierMax(long size) {
        long value = Math.abs(size);
        for (int i = 0; i < VOLTAGES.length; i++) {
            if (value <= VOLTAGES[i]) {
                return i;
            }
        }
        return VOLTAGES.length;
    }

    /** GT6 {@code UT.Code.tierMin}: the index of the highest tier strictly below {@code size}. */
    public static int tierMin(long size) {
        long value = Math.abs(size);
        for (int i = 0; i < VOLTAGES.length; i++) {
            if (value < VOLTAGES[i]) {
                return Math.max(0, i - 1);
            }
        }
        return VOLTAGES.length - 1;
    }

    /** The tier's own maximum voltage, e.g. {@code maxVoltageOf(32)} = 32 and {@code maxVoltageOf(40)} = 128. */
    public static long maxVoltageOf(long size) {
        int tier = tierMax(size);
        return tier < VOLTAGES.length ? VOLTAGES[tier] : VOLTAGES[VOLTAGES.length - 1];
    }

    /** The tier name for a voltage, e.g. {@code "LV"} for 32; {@code "MAX+"} above the table. */
    public static String nameOf(long size) {
        int tier = tierMax(size);
        return tier < NAMES.length ? NAMES[tier] : NAMES[NAMES.length - 1];
    }
}
