package gregtech.content.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.content.material.MaterialTestFixture;
import gregtech.content.material.Materials;
import java.util.HashSet;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class MineralWorldgenRulesTest {
    @Test
    void derivedBedrockKeepsLegacyDepositsWithoutAnOrdinarySource() {
        MaterialTestFixture.ensureInitialized();
        var deposits = OreSourceCatalog.current()
                .bedrockOres("minecraft:overworld", OreSourceCatalog.Geology.WITH_VANILLA_ORES);
        var names = new HashSet<String>();
        for (var deposit : deposits) assertTrue(names.add(deposit.name()));
        assertTrue(names.containsAll(java.util.List.of(
                "hematite",
                "sphalerite",
                "smithsonite",
                "pentlandite",
                "saltpeter",
                "bauxite",
                "cassiterite",
                "chalcopyrite",
                "stolzite",
                "vanadium",
                "gold_a",
                "gold_b",
                "chromite")));
    }

    @Test
    void coltanDistanceStaysAccurateForDistantChunks() {
        var centre = MineralWorldgenRules.coltanCentre(0);
        assertEquals(2_500_000_121L, MineralWorldgenRules.distanceSquared(centre.x(), centre.z(), 50_352, -640));
        assertEquals(1_800_000_000_000_000L, MineralWorldgenRules.distanceSquared(0, 0, -30_000_000, 30_000_000));
    }

    @Test
    void markersBelongToTheTargetChunkRegardlessOfExplorationOrder() {
        var expected = new HashSet<String>();
        long seed = 41891;
        BedrockIndicatorRules.scatter(-8, 8, new Random(seed)::nextInt, (x, z) -> expected.add(x + "," + z));
        for (int direction : new int[] {-1, 1}) {
            var found = new HashSet<String>();
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    int cx = -1 + direction * dx, cz = direction * dz;
                    BedrockIndicatorRules.scatterInto(-1, 0, cx, cz, new Random(seed)::nextInt, (x, z) -> {
                        assertEquals(cx, x >> 4);
                        assertEquals(cz, z >> 4);
                        return found.add(x + "," + z);
                    });
                }
            assertEquals(expected, found);
        }
    }

    @Test
    void disablingOrdinaryPebblesPreservesStratumIndicators() {
        var rock = new SurfaceRockRules.Pebble("Granite", false);
        var indicator = new SurfaceRockRules.Pebble("Chromite", true);
        assertTrue(rock.shouldPlace(true));
        assertFalse(rock.shouldPlace(false));
        assertTrue(indicator.shouldPlace(true));
        assertTrue(indicator.shouldPlace(false));
    }

    @Test
    void legacySmallOreEndsBeforeItsUpperBound() {
        MaterialTestFixture.ensureInitialized();
        var ore = new GTOreVeins.SmallOre("sample", 40, 41, 2, Materials.Copper, GTOreVeins.Distribution.LEGACY);
        var heights = ore.heights("minecraft:the_nether", 0, 127);
        assertEquals(new GTOreVeins.HeightRange(40, 40), heights);
        assertEquals(40, ore.distribution().sample(heights.min(), heights.max(), bound -> bound - 1));
        assertTrue(ore.heights("minecraft:the_nether", 41, 127).isEmpty());
    }
}
