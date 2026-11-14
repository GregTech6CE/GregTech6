package gregtech.content.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

/** Locks GT6 {@code WorldgenStoneLayers}' {@code tLastRock}/{@code tLastOre} state. */
class SurfaceRockRulesTest {
    private static final UnaryOperator<String> TARGET = name -> "crushed-" + name;

    @Test
    void initialRockIsDeepslate() {
        SurfaceRockRules.ColumnState state = new SurfaceRockRules.ColumnState();
        assertEquals(SurfaceRockRules.INITIAL_ROCK, state.lastRock());
        assertFalse(state.canPlaceRocks());
    }

    @Test
    void replacedLayerBecomesTheRockMaterial() {
        SurfaceRockRules.ColumnState state = new SurfaceRockRules.ColumnState();
        state.replaced("GraniteRed");
        assertTrue(state.canPlaceRocks());
        assertEquals("GraniteRed", state.lastRock());
    }

    @Test
    void indicatorOreIsUsedOnTheFiftyPercentRoll() {
        SurfaceRockRules.ColumnState state = new SurfaceRockRules.ColumnState();
        state.replaced("GraniteRed");
        state.ore("Tetrahedrite", true);
        assertEquals("Tetrahedrite", state.lastOre());
        assertEquals("crushed-Tetrahedrite", SurfaceRockRules.rockMaterial(state, () -> true, TARGET));
        assertEquals("GraniteRed", SurfaceRockRules.rockMaterial(state, () -> false, TARGET));
    }

    @Test
    void nonIndicatorOreDoesNotReplaceTheRockMaterial() {
        SurfaceRockRules.ColumnState state = new SurfaceRockRules.ColumnState();
        state.replaced("GraniteRed");
        state.ore("Diamond", false);
        assertNull(state.lastOre());
        assertEquals("GraniteRed", SurfaceRockRules.rockMaterial(state, () -> true, TARGET));
    }

    @Test
    void opaqueGroundPreservesExistingLastRock() {
        SurfaceRockRules.ColumnState state = new SurfaceRockRules.ColumnState();
        state.replaced("Basalt");
        state.ore("Chalcopyrite", true);
        state.opaque(true);
        assertTrue(state.canPlaceRocks());
        assertEquals("Basalt", state.lastRock());
        assertEquals("Chalcopyrite", state.lastOre());

        state.opaque(false);
        assertFalse(state.canPlaceRocks());
        assertEquals("Basalt", state.lastRock());
    }

    @Test
    void resetClearsTheOreAndTheRockFlag() {
        SurfaceRockRules.ColumnState state = new SurfaceRockRules.ColumnState();
        state.replaced("GraniteRed");
        state.ore("Tetrahedrite", true);
        state.reset();
        assertFalse(state.canPlaceRocks());
        assertNull(state.lastOre());
        assertEquals("GraniteRed", state.lastRock());
    }

    @Test
    void rollUsesOneIn128() {
        assertTrue(SurfaceRockRules.roll(bound -> 0));
        assertFalse(SurfaceRockRules.roll(bound -> 1));
        assertEquals(128, SurfaceRockRules.CHANCE_DIVISOR);
    }
}
