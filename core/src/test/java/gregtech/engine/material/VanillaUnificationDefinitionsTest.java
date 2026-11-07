package gregtech.engine.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.api.material.GTValues;
import gregtech.content.data.MaterialPrefix;
import java.util.List;
import org.junit.jupiter.api.Test;

class VanillaUnificationDefinitionsTest {
    @Test
    void vanillaOreFormKeepsTheHost() {
        assertEquals("oreDeepslate", VanillaUnificationDefinitions.vanillaOreForm("deepslate_iron_ore"));
        assertEquals("oreNetherrack", VanillaUnificationDefinitions.vanillaOreForm("nether_quartz_ore"));
        assertEquals("oreNetherrack", VanillaUnificationDefinitions.vanillaOreForm("nether_gold_ore"));
        assertEquals("oreVanillastone", VanillaUnificationDefinitions.vanillaOreForm("iron_ore"));
        assertEquals("oreNetherrack", VanillaUnificationDefinitions.vanillaOreForm("ancient_debris"));
    }

    @Test
    void extraPrefixesExposeSecondaryForms() {
        assertEquals(List.of(MaterialPrefix.plateGem), VanillaUnificationDefinitions.extraPrefixes("glass_pane"));
        assertTrue(VanillaUnificationDefinitions.extraPrefixes("iron_ore").isEmpty());
    }

    @Test
    void stoneRowsCarryTheBlockWeight() {
        for (var path : List.of(
                "stone",
                "cobblestone",
                "mossy_cobblestone",
                "granite",
                "diorite",
                "andesite",
                "deepslate",
                "cobbled_deepslate",
                "tuff")) assertEquals(9 * GTValues.U, weight(path), path);
    }

    private static long weight(String itemPath) {
        return VanillaUnificationDefinitions.entries().stream()
                .filter(entry -> entry.itemPath().equals(itemPath))
                .findFirst()
                .orElseThrow()
                .amount();
    }
}
