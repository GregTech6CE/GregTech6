package gregtech.engine.addon;

import java.util.function.IntUnaryOperator;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * Flaw-detector output rule. One roll of the first item chance, on a scale of {@code 10000},
 * selects that item. Every other roll selects the second item. The second chance is not rolled.
 */
public final class ExclusiveOutputRules {
    public static final int SCALE = 10_000;

    private ExclusiveOutputRules() {}

    /** Both declared slots are present. An empty stack in either slot is not a pair. */
    public static boolean isPair(int declared, int nonEmpty) {
        return declared == 2 && nonEmpty == 2;
    }

    public static void requirePair(int declared, int nonEmpty) {
        if (!isPair(declared, nonEmpty))
            throw new IllegalArgumentException("exclusive outputs need exactly 2 item outputs: " + nonEmpty);
    }

    /**
     * Index of the item this roll produces. A first chance of {@link #SCALE} selects the first
     * item without drawing.
     */
    public static int choose(int firstChance, IntUnaryOperator random) {
        if (firstChance < 1 || firstChance > SCALE)
            throw new IllegalArgumentException("Output chance must be 1.." + SCALE + ": " + firstChance);
        if (firstChance == SCALE) return 0;
        return random.applyAsInt(SCALE) < firstChance ? 0 : 1;
    }
}
