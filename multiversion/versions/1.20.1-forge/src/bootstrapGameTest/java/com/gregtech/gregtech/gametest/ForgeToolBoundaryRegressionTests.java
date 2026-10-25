package com.gregtech.gregtech.gametest;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
/** Fourteen original tool/structure failures in one native runtime. */
@GameTestHolder("gt6ce_forge_tool_boundaries") @PrefixGameTestTemplate(false)
public final class ForgeToolBoundaryRegressionTests {
    @GameTest(template="test_blueprint_large",timeoutTicks=1200) public static void builderWandIsFreeInCreative(GameTestHelper h) { DebugToolBehaviorTests.builderWandIsFreeInCreative(h); }
    @GameTest(template="test_blueprint_large",timeoutTicks=1200) public static void builderWandCopiesTheSurfaceOneLayerUp(GameTestHelper h) { DebugToolBehaviorTests.builderWandCopiesTheSurfaceOneLayerUp(h); }
    @GameTest(template="test_blueprint_large",timeoutTicks=1200) public static void builderWandSkipsBlockedPositions(GameTestHelper h) { DebugToolBehaviorTests.builderWandSkipsBlockedPositions(h); }
    @GameTest(template="test_blueprint_large",timeoutTicks=1200) public static void everyToolHasGt6sRecipeFamily(GameTestHelper h) { ToolAssemblyTests.everyToolHasGt6sRecipeFamily(h); }
    @GameTest(template="test_blueprint_large",timeoutTicks=1200) public static void earlyToolsComeFromRocksAndFlint(GameTestHelper h) { ToolAssemblyTests.earlyToolsComeFromRocksAndFlint(h); }
    @GameTest(template="test_blueprint_large",timeoutTicks=1200) public static void headAndHandleRowsStayShapeless(GameTestHelper h) { ToolAssemblyTests.headAndHandleRowsStayShapeless(h); }
    @GameTest(template="test_blueprint_large",timeoutTicks=1200) public static void dispatcherRoutesEachFamily(GameTestHelper h) { ItemBehaviorTests.dispatcherRoutesEachFamily(h); }
    @GameTest(template="test_blueprint_large",timeoutTicks=1200) public static void survivalDrillUsesLastChargeAndBindsFirstAvailableHotbarRemote(GameTestHelper h) { DrillDynamiteTests.survivalDrillUsesLastChargeAndBindsFirstAvailableHotbarRemote(h); }
    @GameTest(template="test_blueprint_large",timeoutTicks=1200) public static void attachmentPhaseAndFlowRules(GameTestHelper h) { FluidToolVariantTests.attachmentPhaseAndFlowRules(h); }
    @GameTest(template="test_blueprint_large",timeoutTicks=1200) public static void dynamoFourGradesAndElectricalBoundaries(GameTestHelper h) { AxialGeneratorTests.dynamoFourGradesAndElectricalBoundaries(h); }
    @GameTest(template="test_blueprint_large",timeoutTicks=1200) public static void largeCrusherUsesOriginalEfficiencyAndRetainsInterruptedWork(GameTestHelper h) { BlueprintRegressionTests.largeCrusherUsesOriginalEfficiencyAndRetainsInterruptedWork(h); }
    @GameTest(template="test_blueprint_large",timeoutTicks=1200) public static void bronzePumpCanAccumulateOriginalBufferWithoutSimulationMutation(GameTestHelper h) { PumpTransferRepairTests.bronzePumpCanAccumulateOriginalBufferWithoutSimulationMutation(h); }
    @GameTest(template="test_blueprint_large",timeoutTicks=1200) public static void tierBuffersAutoOutputAndCoolDropNbt(GameTestHelper h) { OriginalLargeBoilerRepairTests.tierBuffersAutoOutputAndCoolDropNbt(h); }
    @GameTest(template="test_blueprint_large",timeoutTicks=1200) public static void miniPortalSourceActivation(GameTestHelper h) { DungeonKeysAndPortalsTests.miniPortalSourceActivation(h); }
}
