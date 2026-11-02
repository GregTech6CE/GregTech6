package gregtech.content.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.GregTechConstants;
import gregtech.content.data.ImportedMaterialData;
import gregtech.content.data.generated.GT6Materials;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Regression guard for the solid-fuel rows restored from GT6 {@code MT.java}. */
class FuelParityTest {
    @BeforeAll
    static void initMaterials() {
        MaterialTestFixture.ensureInitialized();
    }

    @Test
    void ligniteMatchesTheOriginalFuelRow() {
        assertFuel(ImportedMaterialData.Lignite, 800, ImportedMaterialData.DarkAsh, GregTechConstants.U4, true, false);
    }

    @Test
    void charcoalUsesTheOriginalAsh() {
        assertFuel(ImportedMaterialData.Charcoal, 1600, ImportedMaterialData.Ash, GregTechConstants.U4, true, true);
    }

    @Test
    void otherMissingFuelRowsMatchTheOriginal() {
        assertFuel(ImportedMaterialData.Peat, 2000, ImportedMaterialData.Ash, GregTechConstants.U2, true, false);
        assertFuel(ImportedMaterialData.Blaze, 4800, null, 0, false, true);
        assertFuel(ImportedMaterialData.PetrifiedWood, 50, null, 0, true, false);
        assertFuel(GT6Materials.Ores.Oilsands, 6000, null, 0, false, false);
        assertFuel(ImportedMaterialData.CrudeOil, 12000, null, 0, false, false);
        assertFuel(
                GT6Materials.Ores.Oilshale, 400, ImportedMaterialData.STONES.Stone, GregTechConstants.U2, true, false);
    }

    @Test
    void elementalFuelRowsMatchTheOriginal() {
        assertFuel(ImportedMaterialData.Li, 2000, null, 0, false, true);
        assertFuel(ImportedMaterialData.Li_6, 2000, null, 0, false, true);
        assertFuel(ImportedMaterialData.C, 800, null, 0, true, true);
        assertFuel(ImportedMaterialData.C_13, 800, null, 0, true, true);
        assertFuel(ImportedMaterialData.C_14, 800, null, 0, true, true);
        assertFuel(ImportedMaterialData.Na, 4000, null, 0, false, true);
        assertFuel(ImportedMaterialData.Mg, 800, null, 0, true, true);
        assertFuel(ImportedMaterialData.P, 1000, null, 0, true, true);
        assertFuel(ImportedMaterialData.S, 1000, null, 0, true, true);
    }

    private static void assertFuel(
            GTMaterial material, long burnTime, GTMaterial ash, long ashAmount, boolean flammable, boolean melting) {
        assertTrue(material.isValid(), material.getName());
        assertEquals(burnTime, material.getFurnaceBurnTime(), material.getName() + " burn time");
        assertEquals(ashAmount, material.getTargetBurningAmount(), material.getName() + " ash amount");
        if (ash != null) {
            assertSame(ash, material.getTargetBurningMaterial(), material.getName() + " ash");
        }
        assertEquals(flammable, material.has(MaterialProperty.FLAMMABLE), material.getName() + " FLAMMABLE");
        assertEquals(melting, material.has(MaterialProperty.MELTING), material.getName() + " MELTING");
    }
}
