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
        SmallOreDeclaration ore = new SmallOreDeclaration("test_small", "example:custom_dimension", 1, 64, 1, "Iron");
        assertEquals("example:custom_dimension", ore.dimension());
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
        assertThrows(IllegalArgumentException.class, () -> vein("test_vein", -1, 64));
        assertThrows(IllegalArgumentException.class, () -> vein("test_vein", 1, 256));
        assertThrows(IllegalArgumentException.class, () -> vein("test_vein", 64, 64));
    }

    private static OreVeinDeclaration vein(String dimension, int minY, int maxY) {
        return new OreVeinDeclaration(
                "test_vein", dimension, minY, maxY, 1, 1, 1, "Iron", "Iron", "Iron", "Iron");
    }
}
