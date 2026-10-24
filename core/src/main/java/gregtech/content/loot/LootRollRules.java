package gregtech.content.loot;

import java.util.function.IntUnaryOperator;

/** Weighted rows and inclusive counts; original Forge table-roll counts used an exclusive maximum. */
public final class LootRollRules {
    private LootRollRules() {}

    public static int pick(int[] weights, IntUnaryOperator random) {
        if (weights.length == 0) return -1;
        int total = 0;
        for (int w : weights) total += w;
        int pick = random.applyAsInt(total);
        for (int i = 0; i < weights.length; i++) {
            pick -= weights[i];
            if (pick < 0) return i;
        }
        return -1;
    }

    public static int count(int min, int max, IntUnaryOperator random) {
        return min + (max > min ? random.applyAsInt(max - min + 1) : 0);
    }
}
