package gregtech.content.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class GTStoneLayersGenTest {
    @Test
    void fiveBusiestLayerVariantsHoldAtMostAQuarterOfTheOreRows() {
        var counts = StoneLayerField.layers().stream()
                .map(layer -> layer.ores().size())
                .sorted(Comparator.reverseOrder())
                .toList();
        int topFive = counts.stream().limit(5).mapToInt(Integer::intValue).sum();
        int total = counts.stream().mapToInt(Integer::intValue).sum();
        assertTrue(topFive * 4 <= total, () -> topFive + " of " + total + " rows in five layer variants");
    }

    @Test
    void chromiteUsesTheDeclaredHeightOfEachLayer() {
        for (var host : List.of("BASALT", "GREENSCHIST")) {
            assertTrue(GTStoneLayersGen.LAYERS.stream()
                    .filter(layer -> host.equals(layer.stoneType()))
                    .flatMap(layer -> layer.ores().stream())
                    .anyMatch(ore ->
                            ore.material().equals("Chromite") && MineralWorldgenRules.stratumOreMinY(ore) == -16));
        }
        assertEquals(10, MineralWorldgenRules.stratumOreMinY(new GTStoneLayersGen.OreDef("Chromite", true, 8, 32, 64)));
    }

    @Test
    void limestoneAndDeepTuffOfferRichAndSparseAluniteRespectively() {
        var deposits = new HashMap<String, GTStoneLayersGen.OreDef>();
        for (var layer : StoneLayerField.layers()) {
            for (var ore : layer.ores()) {
                if (ore.material().equals("Alunite")) deposits.put(layer.material(), ore);
            }
        }
        var primary = deposits.get("Limestone");
        var secondary = deposits.get("Tuff");
        assertNotNull(primary);
        assertNotNull(secondary);
        assertTrue(secondary.chanceDiv() > primary.chanceDiv());
        assertEquals(-47, MineralWorldgenRules.stratumOreMinY(secondary));
        assertEquals(-3, MineralWorldgenRules.stratumOreMaxY(secondary));
    }

    /** GT6's GraniteRed layer: Tantalite U64 30-40, Columbite U64 30-40, Coltan U16 20-50. */
    @Test
    void graniteRedCarriesTheColtanFamily() {
        var graniteRed = GTStoneLayersGen.LAYERS.stream()
                .filter(layer -> "GRANITE_RED".equals(layer.stoneType()))
                .flatMap(layer -> layer.ores().stream())
                .toList();
        assertTrue(graniteRed.contains(new GTStoneLayersGen.OreDef("Tantalite", true, 64, 30, 40)));
        assertTrue(graniteRed.contains(new GTStoneLayersGen.OreDef("Columbite", true, 64, 30, 40)));
        assertTrue(graniteRed.contains(new GTStoneLayersGen.OreDef("Coltan", true, 16, 20, 50)));
    }
}
