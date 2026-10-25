package gregtech.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Only the four cases that reached later stale assumptions in the catalog batch. */
@GameTestHolder("gt6ce_forge_catalog_followup")
@PrefixGameTestTemplate(false)
public final class ForgeCatalogFollowupTests {
    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void dungeonKeysAreRegisteredInBothLanguages(GameTestHelper helper) throws Exception {
        DungeonKeysAndPortalsTests.dungeonKeysAreRegisteredInBothLanguages(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void allAxialRecipesAcceptEveryChemistryAtOnlyTheOriginalTier(GameTestHelper helper) throws Exception {
        AxialBatteryRecipeTests.allAxialRecipesAcceptEveryChemistryAtOnlyTheOriginalTier(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 600)
    public static void axialRecipesLoadAndToolsRemain(GameTestHelper helper) throws Exception {
        AxialGeneratorTests.axialRecipesLoadAndToolsRemain(helper);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void capacityTintAndEnergyArithmetic(GameTestHelper helper) throws Exception {
        ElectricToolAssemblyTests.capacityTintAndEnergyArithmetic(helper);
    }

}
