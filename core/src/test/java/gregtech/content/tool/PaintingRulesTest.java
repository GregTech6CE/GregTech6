package gregtech.content.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.MaterialTestFixture;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.List;
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
}
