package gregtech.content.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class MineralWorldgenRulesTest {
    @Test
    void coltanDistanceStaysAccurateForDistantChunks() {
        var centre = MineralWorldgenRules.coltanCentre(0);
        assertEquals(2_500_000_121L, MineralWorldgenRules.distanceSquared(centre.x(), centre.z(), 50_352, -640));
        assertEquals(1_800_000_000_000_000L, MineralWorldgenRules.distanceSquared(0, 0, -30_000_000, 30_000_000));
    }
}
