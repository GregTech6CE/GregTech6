package gregtech.content.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Locks the GT6 {@code StoneLayer.DEEPSLATE} data used below y<24. */
class GTStoneLayersGenTest {
    @Test
    void deepslateLayerUsesSlateAndDeepslate() {
        assertEquals("SLATE", GTStoneLayersGen.DEEPSLATE.stoneType());
        assertEquals("Deepslate", GTStoneLayersGen.DEEPSLATE.material());
        assertFalse(GTStoneLayersGen.DEEPSLATE.noDeep());
    }

    @Test
    void deepslateLayerKeepsTheOriginalOreRanges() {
        assertTrue(
                GTStoneLayersGen.DEEPSLATE.ores().contains(new GTStoneLayersGen.OreDef("Diamond", false, 64, 0, 12)));
        assertTrue(
                GTStoneLayersGen.DEEPSLATE.ores().contains(new GTStoneLayersGen.OreDef("Redstone", false, 16, 0, 20)));
        assertTrue(GTStoneLayersGen.DEEPSLATE.ores().contains(new GTStoneLayersGen.OreDef("Cu", false, 16, 0, 32)));
    }

    @Test
    void deepslateOresDoNotGenerateIndicators() {
        for (GTStoneLayersGen.OreDef ore : GTStoneLayersGen.DEEPSLATE.ores()) {
            assertFalse(ore.indicators(), ore.material());
        }
    }

    @Test
    void deepslateLayerKeepsTheTailOfTheOriginalTable() {
        assertTrue(GTStoneLayersGen.DEEPSLATE
                .ores()
                .contains(new GTStoneLayersGen.OreDef("Sphalerite", false, 16, 0, 32)));
        assertTrue(GTStoneLayersGen.DEEPSLATE.ores().contains(new GTStoneLayersGen.OreDef("Coal", false, 16, 0, 32)));
        assertTrue(GTStoneLayersGen.DEEPSLATE.ores().contains(new GTStoneLayersGen.OreDef("NaCl", false, 32, 20, 32)));
        assertTrue(
                GTStoneLayersGen.DEEPSLATE.ores().contains(new GTStoneLayersGen.OreDef("Nikolite", false, 32, 0, 20)));
    }

    @Test
    void deepslateOreRulesCarryTheOriginalBiomes() {
        assertEquals(
                List.of(StoneLayerOreRules.BiomeGroup.JUNGLE, StoneLayerOreRules.BiomeGroup.VOLCANIC),
                StoneLayerOreRules.rule(new GTStoneLayersGen.OreDef("Diamond", false, 64, 0, 12))
                        .biomes());
        assertEquals(
                List.of(StoneLayerOreRules.BiomeGroup.DESERT, StoneLayerOreRules.BiomeGroup.VOLCANIC),
                StoneLayerOreRules.rule(new GTStoneLayersGen.OreDef("MgCO3", false, 32, 10, 20))
                        .biomes());
        assertEquals(
                List.of(StoneLayerOreRules.BiomeGroup.OCEAN, StoneLayerOreRules.BiomeGroup.RIVER_LAKE),
                StoneLayerOreRules.rule(new GTStoneLayersGen.OreDef("Borax", false, 32, 10, 20))
                        .biomes());
    }

    @Test
    void tailOreRulesCarryTheOriginalBiomes() {
        assertEquals(
                List.of(StoneLayerOreRules.BiomeGroup.SAVANNA, StoneLayerOreRules.BiomeGroup.SWAMP),
                StoneLayerOreRules.rule(new GTStoneLayersGen.OreDef("Sphalerite", false, 16, 0, 32))
                        .biomes());
        assertEquals(
                List.of(StoneLayerOreRules.BiomeGroup.SHROOM),
                StoneLayerOreRules.rule(new GTStoneLayersGen.OreDef("BrownLimonite", false, 16, 0, 32))
                        .biomes());
        assertEquals(
                List.of(StoneLayerOreRules.BiomeGroup.SWAMP),
                StoneLayerOreRules.rule(new GTStoneLayersGen.OreDef("YellowLimonite", false, 16, 0, 32))
                        .biomes());
        assertEquals(
                List.of(StoneLayerOreRules.BiomeGroup.WOODS),
                StoneLayerOreRules.rule(new GTStoneLayersGen.OreDef("Fe2O3", false, 16, 0, 32))
                        .biomes());
        assertEquals(
                List.of(StoneLayerOreRules.BiomeGroup.SHROOM),
                StoneLayerOreRules.rule(new GTStoneLayersGen.OreDef("Garnierite", false, 16, 0, 32))
                        .biomes());
        assertEquals(
                List.of(StoneLayerOreRules.BiomeGroup.SWAMP),
                StoneLayerOreRules.rule(new GTStoneLayersGen.OreDef("Pentlandite", false, 16, 0, 32))
                        .biomes());
        assertEquals(
                List.of(StoneLayerOreRules.BiomeGroup.PLAINS, StoneLayerOreRules.BiomeGroup.SHROOM),
                StoneLayerOreRules.rule(new GTStoneLayersGen.OreDef("Coal", false, 16, 0, 32))
                        .biomes());
        assertEquals(
                List.of(),
                StoneLayerOreRules.rule(new GTStoneLayersGen.OreDef("NaCl", false, 32, 20, 32))
                        .biomes());
        assertEquals(
                List.of(),
                StoneLayerOreRules.rule(new GTStoneLayersGen.OreDef("Nikolite", false, 32, 0, 20))
                        .biomes());
    }
}
