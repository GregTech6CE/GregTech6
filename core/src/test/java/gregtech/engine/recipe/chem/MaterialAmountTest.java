package gregtech.engine.recipe.chem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gregtech.api.material.GTValues;
import gregtech.content.recipe.OriginalMaterialFluidUnits;
import org.junit.jupiter.api.Test;

/** Material units are exact and only material fluids cross a reviewed millibucket boundary. */
final class MaterialAmountTest {
    @Test
    void ninthOfAUnitStaysExact() {
        assertEquals(72_072_000L, MaterialAmount.fraction(1, 9).units());
        assertNotEquals(MaterialAmount.fraction(1, 9), MaterialAmount.fraction(111, 1000));
    }

    @Test
    void thousandthIsOnlyItsOwnValue() {
        assertEquals(71_999_928L, MaterialAmount.fraction(111, 1000).units());
    }

    @Test
    void nonIntegralFractionsAreRejectedInsteadOfTruncated() {
        assertThrows(IllegalArgumentException.class, () -> MaterialAmount.fraction(1, 17));
    }

    @Test
    void fractionConstantU50IsOneFiftiethOfAUnit() {
        assertEquals(GTValues.U / 50, MaterialAmount.fraction(1, 50).units());
        assertEquals(
                20,
                OriginalMaterialFluidUnits.millibuckets(
                        GTValues.U / 50, OriginalMaterialFluidUnits.SOURCE_UNIT, 1000, false));
    }

    @Test
    void cryoliteTwentyUnitsUseTheMoltenBinding() {
        int millibuckets = OriginalMaterialFluidUnits.millibuckets(
                20 * GTValues.U, OriginalMaterialFluidUnits.SOURCE_UNIT, 144, false);
        assertEquals(2880, millibuckets);
    }

    @Test
    void aluminiumFluorideProductionAndElectrolysisShareOneRule() {
        assertEquals(
                576,
                OriginalMaterialFluidUnits.millibuckets(
                        4 * GTValues.U, OriginalMaterialFluidUnits.SOURCE_UNIT, 144, false));
        assertEquals(
                1152,
                OriginalMaterialFluidUnits.millibuckets(
                        8 * GTValues.U, OriginalMaterialFluidUnits.SOURCE_UNIT, 144, false));
        assertEquals(
                1728,
                OriginalMaterialFluidUnits.millibuckets(
                        12 * GTValues.U, OriginalMaterialFluidUnits.SOURCE_UNIT, 144, false));
    }
}
