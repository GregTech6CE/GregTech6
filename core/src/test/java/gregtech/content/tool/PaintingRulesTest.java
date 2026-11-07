package gregtech.content.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.MaterialTestFixture;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class PaintingRulesTest {
    // GT6's dye materials are DyeBlack..DyeWhite; the tag layer reuses this exact naming.
    private static final List<String> DYE_MATERIALS = List.of(
            "DyeBlack",
            "DyeRed",
            "DyeGreen",
            "DyeBrown",
            "DyeBlue",
            "DyePurple",
            "DyeCyan",
            "DyeLightGray",
            "DyeGray",
            "DyePink",
            "DyeLime",
            "DyeYellow",
            "DyeLightBlue",
            "DyeMagenta",
            "DyeOrange",
            "DyeWhite");

    @Test
    void everyPaintColourNamesItsGt6DyeMaterial() {
        MaterialTestFixture.ensureInitialized();
        for (var dye : PaintingRules.DYES) {
            var name = PaintingRules.dyeMaterial(dye.id());
            assertTrue(DYE_MATERIALS.contains(name), dye.id() + " -> " + name);
            assertTrue(GTMaterialRegistry.get(name).isValid(), "missing material " + name);
            assertTrue(MaterialPrefix.dust.isValidFor(GTMaterialRegistry.get(name)), "missing dust " + name);
        }
    }

    @Test
    void twoWordColoursUseVanillaSpelling() {
        assertEquals("DyeLightGray", PaintingRules.dyeMaterial("light_gray"));
        assertEquals("DyeLightBlue", PaintingRules.dyeMaterial("light_blue"));
    }

    @Test
    void extraDyeMaterialsMapToTheirVanillaColour() {
        MaterialTestFixture.ensureInitialized();
        var expected = Map.ofEntries(
                Map.entry("Carbon", "black"),
                Map.entry("Graphite", "gray"),
                Map.entry("Ashes", "light_gray"),
                Map.entry("DarkAshes", "gray"),
                Map.entry("Cocoa", "brown"),
                Map.entry("Coffee", "brown"),
                Map.entry("Chocolate", "brown"),
                Map.entry("BrownLimonite", "brown"),
                Map.entry("YellowLimonite", "yellow"));
        for (var entry : expected.entrySet()) {
            var material = GTMaterialRegistry.get(entry.getKey());
            assertTrue(material.isValid(), "missing material " + entry.getKey());
            assertTrue(MaterialPrefix.dust.isValidFor(material), "missing dust " + entry.getKey());
            assertEquals(entry.getValue(), PaintingRules.dyeColor(entry.getKey()), entry.getKey());
        }
        assertNull(PaintingRules.dyeColor("Iron"));
    }
}
