package gregtech.engine.recipe.chem;

import gregtech.api.material.GTValues;
import gregtech.content.material.catalog.MaterialFluidUnitFacts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
                MaterialFluidUnitFacts.millibuckets(GTValues.U / 50, MaterialFluidUnitFacts.SOURCE_UNIT, 1000, false));
    }

    @Test
    void cryoliteTwentyUnitsUseTheMoltenBinding() {
        int millibuckets =
                MaterialFluidUnitFacts.millibuckets(20 * GTValues.U, MaterialFluidUnitFacts.SOURCE_UNIT, 144, false);
        assertEquals(2880, millibuckets);
    }

    @Test
    void aluminiumFluorideProductionAndElectrolysisShareOneRule() {
        assertEquals(
                576,
                MaterialFluidUnitFacts.millibuckets(4 * GTValues.U, MaterialFluidUnitFacts.SOURCE_UNIT, 144, false));
        assertEquals(
                1152,
                MaterialFluidUnitFacts.millibuckets(8 * GTValues.U, MaterialFluidUnitFacts.SOURCE_UNIT, 144, false));
        assertEquals(
                1728,
                MaterialFluidUnitFacts.millibuckets(12 * GTValues.U, MaterialFluidUnitFacts.SOURCE_UNIT, 144, false));
    }
}
