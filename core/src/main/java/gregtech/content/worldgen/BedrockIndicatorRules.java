package gregtech.content.worldgen;

import java.util.function.IntUnaryOperator;

/** Column sampling from GT6 {@code WorldgenOresBedrock}: smaller areas have higher marker density. */
public final class BedrockIndicatorRules {
    private BedrockIndicatorRules() {}

    @FunctionalInterface
    public interface Placement {
        boolean place(int x, int z);
    }

    /** Try four columns within 8×8, eight within 16×16, and sixteen within 32×32. */
    public static int scatter(int centerX, int centerZ, IntUnaryOperator random, Placement placement) {
        int placed = 0;
        for (int radius = 4; radius <= 16; radius *= 2) {
            for (int attempt = 0; attempt < radius; attempt++) {
                int x = centerX + random.applyAsInt(2 * radius) - radius;
                int z = centerZ + random.applyAsInt(2 * radius) - radius;
                if (placement.place(x, z)) placed++;
            }
        }
        return placed;
    }
}
