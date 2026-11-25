/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.worldgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorldgenDeclarationsTest {
    @Test
    void customDimensionsAreAccepted() {
        OreVeinDeclaration vein = vein("example:custom_dimension", 1, 64);
        assertEquals("example:custom_dimension", vein.dimension());
        assertEquals(HeightCoordinates.LEGACY, vein.coordinates());
        SmallOreDeclaration ore = new SmallOreDeclaration("test_small", "example:custom_dimension", 1, 64, 1, "Iron");
        assertEquals("example:custom_dimension", ore.dimension());
        assertEquals(HeightCoordinates.LEGACY, ore.coordinates());
    }

    @Test
    void dimensionsMustBeResourceLocations() {
        assertThrows(IllegalArgumentException.class, () -> vein("example", 1, 64));
        assertThrows(IllegalArgumentException.class, () -> vein("Bad Dimension!", 1, 64));
        assertThrows(IllegalArgumentException.class,
                () -> new SmallOreDeclaration("test_small", null, 1, 64, 1, "Iron"));
    }

    @Test
    void heightsStayInThePreRemapRange() {
        assertThrows(IllegalArgumentException.class, () -> vein(OreVeinDeclaration.OVERWORLD, -1, 64));
        assertThrows(IllegalArgumentException.class, () -> vein(OreVeinDeclaration.OVERWORLD, 1, 256));
        assertThrows(IllegalArgumentException.class, () -> vein(OreVeinDeclaration.OVERWORLD, 64, 64));
    }

    @Test
    void absoluteHeightsCanBeNegativeAndReachAbove255() {
        var vein = new OreVeinDeclaration("test_vein", OreVeinDeclaration.OVERWORLD, -64, 319, 1, 1, 1,
                "Iron", "Iron", "Iron", "Iron", HeightCoordinates.ABSOLUTE);
        assertEquals(-64, vein.minY());
        assertEquals(319, vein.maxY());
        assertEquals(HeightCoordinates.ABSOLUTE, vein.coordinates());
        var ore = new SmallOreDeclaration("test_ore", SmallOreDeclaration.OVERWORLD,
                -40, -12, 1, "Iron", HeightCoordinates.ABSOLUTE);
        assertEquals(-40, ore.minY());
        assertThrows(IllegalArgumentException.class, () -> new SmallOreDeclaration(
                "test_ore", SmallOreDeclaration.OVERWORLD, -2033, -12, 1, "Iron", HeightCoordinates.ABSOLUTE));
    }

    private static OreVeinDeclaration vein(String dimension, int minY, int maxY) {
        return new OreVeinDeclaration(
                "test_vein", dimension, minY, maxY, 1, 1, 1, "Iron", "Iron", "Iron", "Iron");
    }
}
