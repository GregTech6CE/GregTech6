package gregtech.content.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StoneLayerScanTest {
    @Test
    void maxScanYIncludesSectionBoundarySurfaceAndClampsToBuildHeight() {
        // Empty chunk (-1) scans nothing.
        assertEquals(-65, StoneLayerField.maxScanY(-64, 320, -1));
        // Section 7 covers [48..63]; maxScanY reaches 64 so a rock sitting on y=63 is placed.
        assertEquals(64, StoneLayerField.maxScanY(-64, 320, 7));
        // Section 12 covers [128..143]; maxScanY reaches 144 instead of cutting off at 143.
        assertEquals(144, StoneLayerField.maxScanY(-64, 320, 12));
        // Mountain section 20 covers [256..271]; high-altitude terrain above Y=144 is included.
        assertEquals(272, StoneLayerField.maxScanY(-64, 320, 20));
        // Top-most section clamps at maxBuildHeight - 1.
        assertEquals(319, StoneLayerField.maxScanY(-64, 320, 23));
    }
}
