package gregtech.content.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

/** Locks GT6 {@code WorldgenOresLarge}'s indicator rock count, material weights and scan start. */
class LargeVeinIndicatorRulesTest {
    @Test
    void countIsOneToThree() {
        assertEquals(1, LargeVeinIndicatorRules.count(bound -> 0));
        assertEquals(3, LargeVeinIndicatorRules.count(bound -> 2));
    }

    @Test
    void materialRollIsTwoThirds() {
        assertFalse(LargeVeinIndicatorRules.hasMaterial(bound -> 0));
        assertTrue(LargeVeinIndicatorRules.hasMaterial(bound -> 1));
        assertTrue(LargeVeinIndicatorRules.hasMaterial(bound -> 2));
    }

    @Test
    void materialSelectionWeightsTopTwice() {
        assertEquals("top", LargeVeinIndicatorRules.material(bound -> 0, "top", "bottom", "between", "spread"));
        assertEquals("top", LargeVeinIndicatorRules.material(bound -> 1, "top", "bottom", "between", "spread"));
        assertEquals("bottom", LargeVeinIndicatorRules.material(bound -> 2, "top", "bottom", "between", "spread"));
        assertEquals("between", LargeVeinIndicatorRules.material(bound -> 3, "top", "bottom", "between", "spread"));
        assertEquals("spread", LargeVeinIndicatorRules.material(bound -> 4, "top", "bottom", "between", "spread"));
    }

    @Test
    void scanStartsTwentyFiveAboveTheBase() {
        assertEquals(75, LargeVeinIndicatorRules.scanStart(50));
    }

    @Test
    void materialDistributionMatchesTheOriginalWeights() {
        Random random = new Random(1234);
        int top = 0, bottom = 0, between = 0, spread = 0;
        for (int i = 0; i < 10000; i++) {
            switch (LargeVeinIndicatorRules.material(random::nextInt, "top", "bottom", "between", "spread")) {
                case "top" -> top++;
                case "bottom" -> bottom++;
                case "between" -> between++;
                default -> spread++;
            }
        }
        assertEquals(4000, top, 100);
        assertEquals(2000, bottom, 100);
        assertEquals(2000, between, 100);
        assertEquals(2000, spread, 100);
    }
}
