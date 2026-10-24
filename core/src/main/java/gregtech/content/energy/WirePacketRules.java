package gregtech.content.energy;
/**
 * Signed packet loss follows GT6; saturating counters are port overflow protection.
 * World traversal and the per-tick overload state stay in the block entity.
 */
public final class WirePacketRules {
    private WirePacketRules() {}

    public static boolean canTransfer(long voltage, long amperage, long loss) {
        return voltage != Long.MIN_VALUE && amperage > 0 && Math.abs(voltage) > loss;
    }

    /** Called after canTransfer, with a nonnegative loss smaller than the voltage magnitude. */
    public static long afterLoss(long voltage, long loss) {
        return voltage > 0 ? voltage - loss : voltage + loss;
    }

    /** Both counter operands are nonnegative. */
    public static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    /** The caller supplies a nonnegative voltage magnitude and strictly positive amperage. */
    public static long wattage(long magnitude, long amperage) {
        return magnitude > Long.MAX_VALUE / amperage ? Long.MAX_VALUE : magnitude * amperage;
    }
}
