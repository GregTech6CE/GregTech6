package gregtech.content.worldgen;

import java.util.function.IntUnaryOperator;

/** GT6 {@code WorldgenOresLarge}'s surface indicator rocks. */
public final class LargeVeinIndicatorRules {
    private LargeVeinIndicatorRules() {}

    /** GT6 {@code 1 + aRandom.nextInt(3)}. */
    public static int count(IntUnaryOperator nextInt) {
        return 1 + nextInt.applyAsInt(3);
    }

    /** GT6 {@code aRandom.nextInt(3) != 0}: two thirds of the rocks carry a material. */
    public static boolean hasMaterial(IntUnaryOperator nextInt) {
        return nextInt.applyAsInt(3) != 0;
    }

    /** GT6 {@code UT.Code.select(mTop, mTop, mBottom, mBetween, mSpread)}. */
    public static <T> T material(IntUnaryOperator nextInt, T top, T bottom, T between, T spread) {
        return switch (nextInt.applyAsInt(5)) {
            case 0, 1 -> top;
            case 2 -> bottom;
            case 3 -> between;
            default -> spread;
        };
    }

    /** GT6's scan start: {@code tMinY + 25}. */
    public static int scanStart(int minY) {
        return minY + 25;
    }
}
