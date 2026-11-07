package gregtech.content.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PaintStateTest {
    @Test
    void whitePaintChangesAnUnpaintedTarget() {
        var state = new PaintState();
        assertFalse(state.painted());
        assertTrue(state.paint(0xFFFFFF));
        assertTrue(state.painted());
        assertEquals(0xFFFFFF, state.color());
    }

    @Test
    void repeatingTheCurrentColourDoesNothing() {
        var state = new PaintState();
        state.paint(0xFF0000);
        assertFalse(state.paint(0xFF0000));
        assertTrue(state.paint(0x00FF00));
        assertEquals(0x00FF00, state.color());
    }

    @Test
    void clearedStatePaintsWhiteAgain() {
        var state = new PaintState();
        state.paint(0xFF0000);
        assertTrue(state.clear());
        assertFalse(state.painted());
        assertFalse(state.clear());
        assertTrue(state.paint(0xFFFFFF));
        assertTrue(state.painted());
    }

    @Test
    void mixingUsesTheCurrentColourOnlyAfterPainting() {
        var state = new PaintState();
        assertTrue(state.mix(0xFF0000));
        assertEquals(0xFF0000, state.color());
        assertTrue(state.mix(0x0000FF));
        assertEquals(PaintingRules.mix(0xFF0000, 0x0000FF), state.color());
        assertFalse(state.paint(state.color()));
    }
}
