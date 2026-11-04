package gregtech.content.worldgen;

import java.util.function.IntUnaryOperator;

/** GT6 {@code WorldgenOresBedrock}'s 4/8/16 indicator rock/flower pattern. */
public final class BedrockIndicatorRules {
    /** GT6's three rings, denser in the middle. */
    public static final int[] RINGS = {4, 8, 16};

    private BedrockIndicatorRules() {}

    @FunctionalInterface
    public interface PositionConsumer {
        void accept(int x, int z);
    }

    /** GT6: {@code tD = 4, 8, 16}, each ring placing {@code tD} candidates around the centre. */
    public static void forEachPosition(int minX, int minZ, IntUnaryOperator nextInt, PositionConsumer consumer) {
        for (int d : RINGS) {
            for (int i = 0; i < d; i++) {
                int x = minX + nextInt.applyAsInt(d * 2) + 8 - d;
                int z = minZ + nextInt.applyAsInt(d * 2) + 8 - d;
                consumer.accept(x, z);
            }
        }
    }

    /** GT6: flowers are skipped on one quarter of the positions when indicator rocks are enabled. */
    public static boolean placeFlower(IntUnaryOperator nextInt, boolean hasRocks) {
        return !hasRocks || nextInt.applyAsInt(4) > 0;
    }
}
