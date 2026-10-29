/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.energy;

/**
 * GT6's voltage tier table ({@code CS.V}) and tier lookups ({@code UT.Code.tier}/{@code tierMax}/
 * {@code tierMin}), ported because several GT6 mechanics are expressed in <em>tier indices</em>
 * rather than volts - most visibly {@code UT.Entities.applyElectricityDamage}, whose damage is
 * {@code tierMax(voltage) * amperage * 4} (so an ULV wire carrying 8&nbsp;V deals nothing at all and a
 * 512&nbsp;V wire deals three times an LV one, per ampere).
 *
 * <p>The port's machine tiers use the same ladder (its tier 5 machines run at 8192&nbsp;V), so the
 * indices line up with GT6's {@code MT}/{@code RM} tier numbering.
 */
public final class GTVoltageTiers {
    private static final long[] VOLTAGE_TABLE = {
            8L, 32L, 128L, 512L, 2048L, 8192L, 32768L, 131072L, 524288L, 2097152L,
            8388608L, 33554432L, 134217728L, 536870912L, 2147483648L, 8589934592L};

    private static final String[] NAME_TABLE = {
            "ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "PUV1",
            "PUV2", "PUV3", "PUV4", "PUV5", "XV", "XV"};

    private GTVoltageTiers() {}

    /** Number of voltage tiers in the table. */
    public static int tierCount() { return VOLTAGE_TABLE.length; }

    /** GT6 {@code CS.V}: maximum voltage of tier {@code index}, ULV first. */
    public static long tierVoltage(int index) {
        if (index < 0 || index >= VOLTAGE_TABLE.length)
            throw new IndexOutOfBoundsException("Tier " + index + " outside 0.." + (VOLTAGE_TABLE.length - 1));
        return VOLTAGE_TABLE[index];
    }

    /** GT6 {@code CS.VN}: name of tier {@code index}, ULV first. The last two tiers are both {@code XV}. */
    public static String tierName(int index) {
        if (index < 0 || index >= NAME_TABLE.length)
            throw new IndexOutOfBoundsException("Tier " + index + " outside 0.." + (NAME_TABLE.length - 1));
        return NAME_TABLE[index];
    }

    /**
     * GT6 {@code UT.Code.tier}/{@code tierMax}: the index of the tier {@code size} belongs to.
     * A value above the table returns 16, as GT6 does; that is not a valid index, so clamp it to
     * 15 before indexing. {@code Long.MIN_VALUE} keeps GT6's signed-magnitude behavior and returns tier 0.
     */
    public static int tierMax(long size) {
        long value = Math.abs(size);
        for (int i = 0; i < VOLTAGE_TABLE.length; i++) {
            if (value <= VOLTAGE_TABLE[i]) {
                return i;
            }
        }
        return VOLTAGE_TABLE.length;
    }

    /** GT6 {@code UT.Code.tierMin}: the index of the highest tier strictly below {@code size}. */
    public static int tierMin(long size) {
        long value = Math.abs(size);
        for (int i = 0; i < VOLTAGE_TABLE.length; i++) {
            if (value < VOLTAGE_TABLE[i]) {
                return Math.max(0, i - 1);
            }
        }
        return VOLTAGE_TABLE.length - 1;
    }

    /** The tier's own maximum voltage, e.g. {@code maxVoltageOf(32)} = 32 and {@code maxVoltageOf(40)} = 128. */
    public static long maxVoltageOf(long size) {
        int tier = tierMax(size);
        return tier < VOLTAGE_TABLE.length ? VOLTAGE_TABLE[tier] : VOLTAGE_TABLE[VOLTAGE_TABLE.length - 1];
    }

    /** The tier name for a voltage, e.g. {@code "LV"} for 32; {@code "XV"} above the table. */
    public static String nameOf(long size) {
        int tier = tierMax(size);
        return tier < NAME_TABLE.length ? NAME_TABLE[tier] : NAME_TABLE[NAME_TABLE.length - 1];
    }
}
