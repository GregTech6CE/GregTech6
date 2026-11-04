package gregtech.content.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Locks GT6 {@code WorldgenOresBedrock}'s 4/8/16 indicator rings and flower roll. */
class BedrockIndicatorRulesTest {
    @Test
    void threeRingsPlaceFourEightAndSixteenCandidates() {
        List<int[]> positions = new ArrayList<>();
        BedrockIndicatorRules.forEachPosition(0, 0, bound -> 0, (x, z) -> positions.add(new int[] {x, z}));
        assertEquals(4 + 8 + 16, positions.size());
    }

    @Test
    void innerRingStartsAtItsLowerBound() {
        List<int[]> positions = new ArrayList<>();
        BedrockIndicatorRules.forEachPosition(0, 0, bound -> 0, (x, z) -> positions.add(new int[] {x, z}));
        assertEquals(4, positions.get(0)[0]);
        assertEquals(4, positions.get(0)[1]);
    }

    @Test
    void outerRingSpansThirtyTwoBlocks() {
        List<int[]> positions = new ArrayList<>();
        BedrockIndicatorRules.forEachPosition(0, 0, bound -> 0, (x, z) -> positions.add(new int[] {x, z}));
        int[] firstOuter = positions.get(12);
        assertEquals(-8, firstOuter[0]);
        assertEquals(-8, firstOuter[1]);

        positions.clear();
        BedrockIndicatorRules.forEachPosition(0, 0, bound -> bound - 1, (x, z) -> positions.add(new int[] {x, z}));
        int[] lastOuter = positions.get(positions.size() - 1);
        assertEquals(23, lastOuter[0]);
        assertEquals(23, lastOuter[1]);
    }

    @Test
    void flowerRollSkipsOneQuarterWhenRocksExist() {
        assertFalse(BedrockIndicatorRules.placeFlower(bound -> 0, true));
        assertTrue(BedrockIndicatorRules.placeFlower(bound -> 1, true));
        assertTrue(BedrockIndicatorRules.placeFlower(bound -> 3, true));
        assertTrue(BedrockIndicatorRules.placeFlower(bound -> 0, false));
    }
}
