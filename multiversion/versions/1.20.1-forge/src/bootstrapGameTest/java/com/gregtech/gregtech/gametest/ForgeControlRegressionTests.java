package com.gregtech.gregtech.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Focused replay of existing control/CPU-meter fixtures without the default 1045-test suite. */
@GameTestHolder("gt6ce_forge_control_regressions")
@PrefixGameTestTemplate(false)
public final class ForgeControlRegressionTests {
    @GameTest(template = "test_blueprint_empty", timeoutTicks = 200)
    public static void liveControlsSixSidesAndCycles(GameTestHelper helper) {
        UniversalExtenderTests.liveControlsSixSidesAndCycles(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 200)
    public static void stopSaveResumePendingOutput(GameTestHelper helper) {
        UniversalExtenderTests.stopSaveResumePendingOutput(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 200)
    public static void sourceRedstoneAndComparatorRouting(GameTestHelper helper) {
        UniversalExtenderTests.sourceRedstoneAndComparatorRouting(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 200)
    public static void generatorFamiliesControlAndSave(GameTestHelper helper) {
        MachineControlTests.generatorFamiliesControlAndSave(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 200)
    public static void pistonModesMapToOriginalOddPowerSteps(GameTestHelper helper) {
        MachineControlTests.pistonModesMapToOriginalOddPowerSteps(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 200)
    public static void reactorModeMasksAndShutdownNeutrons(GameTestHelper helper) {
        MachineControlTests.reactorModeMasksAndShutdownNeutrons(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 200)
    public static void playerToolsOperateControlsAndUnsupportedTargetsDoNotWear(GameTestHelper helper) {
        MachineControlTests.playerToolsOperateControlsAndUnsupportedTargetsDoNotWear(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 200)
    public static void remoteEnableCannotClearAxialOverload(GameTestHelper helper) {
        MachineControlTests.remoteEnableCannotClearAxialOverload(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 200)
    public static void redstoneSelectorReadsOnlyInstalledFace(GameTestHelper helper) {
        PanelCoverTests.redstoneSelectorReadsOnlyInstalledFace(helper);
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void previousPassDrivesAllFourMetersAndEmitsRedstone(GameTestHelper helper) {
        LogisticsCpuDisplayTests.previousPassDrivesAllFourMetersAndEmitsRedstone(helper);
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void wireDisplayPersistsAndSynchronizesItsVisual(GameTestHelper helper) {
        LogisticsCpuDisplayTests.wireDisplayPersistsAndSynchronizesItsVisual(helper);
    }

}
