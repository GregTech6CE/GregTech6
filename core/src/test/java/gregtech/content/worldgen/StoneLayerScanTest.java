package gregtech.content.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Locks GT6 {@code WorldgenStoneLayers}' 7-entry window and {@code noDeep} substitution. */
class StoneLayerScanTest {
    private record Layer(String name, boolean noDeep) {}

    private static final Layer A = new Layer("A", false);
    private static final Layer B = new Layer("B", false);
    private static final Layer C = new Layer("C", false);
    private static final Layer NO_DEEP = new Layer("NoDeep", true);
    private static final Layer DEEP = new Layer("Deep", false);

    @Test
    void windowIsCentredOnTheCurrentY() {
        StoneLayerScan<Layer> scan = new StoneLayerScan<>(
                (x, y, z) -> switch (y) {
                    case -2 -> A;
                    case 0 -> B;
                    default -> C;
                },
                Layer::noDeep,
                DEEP,
                -7,
                0,
                0,
                0);
        assertSame(B, scan.current());
        assertSame(A, scan.below());
        assertSame(C, scan.above());
        assertSame(C, scan.outerBelow());
        assertSame(C, scan.outerAbove());
    }

    @Test
    void interiorAndCoreFlagsFollowTheWindow() {
        StoneLayerScan<Layer> scan = new StoneLayerScan<>((x, y, z) -> A, Layer::noDeep, DEEP, -7, 0, 0, 0);
        assertTrue(scan.isInterior());
        assertTrue(scan.isCore());
        assertFalse(scan.isNearBoundary());
    }

    @Test
    void boundaryFlagsDetectDifferentLayers() {
        StoneLayerScan<Layer> scan = new StoneLayerScan<>((x, y, z) -> y < 0 ? A : B, Layer::noDeep, DEEP, -7, 0, 0, 0);
        assertFalse(scan.isInterior());
        assertFalse(scan.isCore());
        assertTrue(scan.isNearBoundary());
    }

    @Test
    void noDeepLayersBecomeDeepslateBelowTheThreshold() {
        StoneLayerScan<Layer> scan = new StoneLayerScan<>((x, y, z) -> NO_DEEP, Layer::noDeep, DEEP, -7, 0, -10, 0);
        assertSame(DEEP, scan.current());
        scan.advance();
        assertSame(DEEP, scan.current());
    }

    @Test
    void noDeepLayersStayAboveTheThreshold() {
        StoneLayerScan<Layer> scan = new StoneLayerScan<>((x, y, z) -> NO_DEEP, Layer::noDeep, DEEP, -7, 0, 0, 0);
        assertSame(NO_DEEP, scan.current());
    }

    @Test
    void advanceSlidesTheWindowByOne() {
        StoneLayerScan<Layer> scan = new StoneLayerScan<>(
                (x, y, z) -> switch (y) {
                    case 0 -> A;
                    case 1 -> B;
                    default -> C;
                },
                Layer::noDeep,
                DEEP,
                -7,
                0,
                0,
                0);
        assertSame(A, scan.current());
        scan.advance();
        assertEquals(1, scan.y());
        assertSame(B, scan.current());
    }
}
