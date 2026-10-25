package com.gregtech.gregtech.gametest;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
/** Source-backed processing/catalog replay; sixteen original failures, one JVM. */
@GameTestHolder("gt6ce_forge_processing_regressions") @PrefixGameTestTemplate(false)
public final class ForgeProcessingRegressionTests {
    @GameTest(template="test_blueprint_empty",timeoutTicks=1200) public static void siftingYieldsGemsFromPurifiedOre(GameTestHelper h) { OrePurificationRoutesTests.siftingYieldsGemsFromPurifiedOre(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=1200) public static void sluiceWashesCrushedOreIntoPurifiedOre(GameTestHelper h) { OrePurificationRoutesTests.sluiceWashesCrushedOreIntoPurifiedOre(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=1200) public static void redDyeChainKeepsOriginalAmountsAndResidueChance(GameTestHelper h) { DyeProcessingTests.redDyeChainKeepsOriginalAmountsAndResidueChance(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=1200) public static void allDyeRecipesConsumeActualRegisteredInputs(GameTestHelper h) { DyeProcessingTests.allDyeRecipesConsumeActualRegisteredInputs(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=1200) public static void electronicsInputsExecuteAndSolderLimitsTier(GameTestHelper h) { ElectronicsTests.electronicsInputsExecuteAndSolderLimitsTier(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=1200) public static void printerPrintsThePrinterManual(GameTestHelper h) { GTMainRecipesTests.printerPrintsThePrinterManual(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=1200) public static void printerPrintsTheScannedDictionary(GameTestHelper h) { MaterialDataChainTests.printerPrintsTheScannedDictionary(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=1200) public static void replicatorRebuildsTheScannedMaterial(GameTestHelper h) { MaterialDataChainTests.replicatorRebuildsTheScannedMaterial(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=1200) public static void transpiledCoverageDoesNotRegress(GameTestHelper h) { GeneratedChemistryTests.transpiledCoverageDoesNotRegress(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=1200) public static void woodRowGridsMatchExactlyTheirOwnRow(GameTestHelper h) { WoodRecipeTests.woodRowGridsMatchExactlyTheirOwnRow(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=1200) public static void wireCraftingPreservesConductorAmount(GameTestHelper h) { ManufacturingTests.wireCraftingPreservesConductorAmount(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=1200) public static void multiblockMachinesAreCovered(GameTestHelper h) { MultiblockCraftingTests.multiblockMachinesAreCovered(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=1200) public static void craftingGridConversions(GameTestHelper h) { MaterialFormConversionTests.craftingGridConversions(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=1200) public static void noRegisteredRecipeIsDegenerate(GameTestHelper h) { StoneExtrusionTests.noRegisteredRecipeIsDegenerate(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=1200) public static void shredderOutputsNeverExceedDeclaredMaterial(GameTestHelper h) { VanillaCompositionTests.shredderOutputsNeverExceedDeclaredMaterial(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=1200) public static void wroughtIronIsReachableThroughTheSmelter(GameTestHelper h) { CrucibleTemperatureTests.wroughtIronIsReachableThroughTheSmelter(h); }
}
