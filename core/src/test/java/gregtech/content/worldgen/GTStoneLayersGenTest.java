package gregtech.content.worldgen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GTStoneLayersGenTest {
    /** GT6's GraniteRed layer: Tantalite U64 30-40, Columbite U64 30-40, Coltan U16 20-50. */
    @Test
    void graniteRedCarriesTheColtanFamily() {
        GTStoneLayersGen.LayerDef graniteRed = GTStoneLayersGen.LAYERS.stream()
                .filter(layer -> "GRANITE_RED".equals(layer.stoneType()))
                .filter(layer -> !layer.ores().isEmpty())
                .findFirst()
                .orElseThrow();
        assertTrue(graniteRed.ores().contains(new GTStoneLayersGen.OreDef("Tantalite", true, 64, 30, 40)));
        assertTrue(graniteRed.ores().contains(new GTStoneLayersGen.OreDef("Columbite", true, 64, 30, 40)));
        assertTrue(graniteRed.ores().contains(new GTStoneLayersGen.OreDef("Coltan", true, 16, 20, 50)));
    }
}
