package gregtech.content.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Locks GT6 {@code WorldgenStoneLayers}' 7-entry window and section-based scan bounds. */
class StoneLayerScanTest {
    private static final String A = "A";
    private static final String B = "B";
    private static final String C = "C";

    @Test
    void windowExposesThreeBlocksAroundCurrentY() {
        StoneLayerScan<String> scan = new StoneLayerScan<>((x, y, z) -> "Y" + y, 4, 10, -2);
        assertEquals(10, scan.y());
        assertEquals("Y7", scan.outerBelow());
        assertEquals("Y8", scan.below());
        assertEquals("Y9", scan.nearBelow());
        assertEquals("Y10", scan.current());
        assertEquals("Y11", scan.nearAbove());
        assertEquals("Y12", scan.above());
        assertEquals("Y13", scan.outerAbove());
    }

    @Test
    void interiorAndCoreFlagsFollowTheWindow() {
        StoneLayerScan<String> scan = new StoneLayerScan<>((x, y, z) -> A, 0, 0, 0);
        assertTrue(scan.isInterior());
        assertTrue(scan.isCore());
        assertFalse(scan.isNearBoundary());
    }

    @Test
    void boundaryFlagsDetectDifferentLayers() {
        StoneLayerScan<String> scan = new StoneLayerScan<>((x, y, z) -> y < 0 ? A : B, 0, 0, 0);
        assertFalse(scan.isInterior());
        assertFalse(scan.isCore());
        assertTrue(scan.isNearBoundary());
    }

    @Test
    void advanceSlidesUnboundedPastOldHeightCap() {
        StoneLayerScan<String> scan = new StoneLayerScan<>(
                (x, y, z) -> {
                    if (y < 144) return A;
                    if (y == 144) return B;
                    return C;
                },
                0,
                142,
                0);
        assertSame(A, scan.current());
        scan.advance();
        assertEquals(143, scan.y());
        assertSame(A, scan.current());
        scan.advance();
        assertEquals(144, scan.y());
        assertSame(B, scan.current());
        scan.advance();
        assertEquals(145, scan.y());
        assertSame(C, scan.current());
    }

    @Test
    void maxScanYIncludesSectionBoundarySurfaceAndClampsToBuildHeight() {
        // Empty chunk (-1) scans nothing.
        assertEquals(-65, StoneLayerScan.maxScanY(-64, 320, -1));
        // Section 7 covers [48..63]; maxScanY reaches 64 so a rock sitting on y=63 is placed.
        assertEquals(64, StoneLayerScan.maxScanY(-64, 320, 7));
        // Section 12 covers [128..143]; maxScanY reaches 144 instead of cutting off at 143.
        assertEquals(144, StoneLayerScan.maxScanY(-64, 320, 12));
        // Mountain section 20 covers [256..271]; high-altitude terrain above Y=144 is included.
        assertEquals(272, StoneLayerScan.maxScanY(-64, 320, 20));
        // Top-most section clamps at maxBuildHeight - 1.
        assertEquals(319, StoneLayerScan.maxScanY(-64, 320, 23));
    }
}
