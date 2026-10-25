package com.gregtech.gregtech.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Narrow reruns of the native legacy fixtures corrected during card 55's default-suite audit. */
@GameTestHolder("gt6ce_structure_special_native")
@PrefixGameTestTemplate(false)
public final class StructureSpecialNativeTests {
    @GameTest(template = "test_blueprint_empty", timeoutTicks = 200)
    public static void blueprintDrillUsesNativeHeadsAndPreservesCycleResources(GameTestHelper helper) {
        BlueprintRegressionTests.bedrockDrillConsumesLubricantAndConservesOutput(helper);
    }

    @GameTest(template = "test_blueprint_large", timeoutTicks = 200)
    public static void replacedValveReevaluatesBeforeRestoredFluidIo(GameTestHelper helper) {
        TankValveRepairTests.fluidRulesAndDropRestoreUseOriginalValve(helper);
    }

    @GameTest(template = "coin_pile_space", timeoutTicks = 200)
    public static void drillSelectorReadsActualBedrockFloor(GameTestHelper helper) throws Exception {
        BedrockBrokenOreRepairTests.bedrockDrillSelectsAPlaceableBrokenOre(helper);
    }
}
