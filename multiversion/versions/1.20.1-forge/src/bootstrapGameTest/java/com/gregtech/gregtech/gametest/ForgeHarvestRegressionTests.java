package com.gregtech.gregtech.gametest;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
/** Targeted replay of thirteen original lifecycle, cover and capability failures. */
@GameTestHolder("gt6ce_forge_harvest_regressions") @PrefixGameTestTemplate(false)
public final class ForgeHarvestRegressionTests {
    @GameTest(template="test_blueprint_empty",timeoutTicks=600)
    public static void machineDropCarriesPendingWorkWithoutInventoryDuplication(GameTestHelper h) { PortRegressionTests.machineDropCarriesPendingWorkWithoutInventoryDuplication(h); }
    @GameTest(template="harvest_empty",timeoutTicks=600)
    public static void aPipeDropsItsCoversWhenBroken(GameTestHelper h) { PipeCoverTests.aPipeDropsItsCoversWhenBroken(h); }
    @GameTest(template="coin_pile_space",timeoutTicks=600)
    public static void funnelAndBothApiariesRestoreAutomation(GameTestHelper h) { ToolCapabilityLifecycleTests.funnelAndBothApiariesRestoreAutomation(h); }
    @GameTest(template="harvest_empty",timeoutTicks=600)
    public static void brokenExtenderDropsConfiguredPanelOnce(GameTestHelper h) { PanelCoverTests.brokenExtenderDropsConfiguredPanelOnce(h); }
    @GameTest(template="harvest_empty",timeoutTicks=600)
    public static void blockClicksControlModesAndStatusAndPersistPanels(GameTestHelper h) { BatteryBoxPanelTests.blockClicksControlModesAndStatusAndPersistPanels(h); }
    @GameTest(template="harvest_empty",timeoutTicks=600)
    public static void directSelectorAndStatusPanelsPersistAndDrop(GameTestHelper h) { ElectricTransformerControlTests.directSelectorAndStatusPanelsPersistAndDrop(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=600)
    public static void steamDrivesDynamoAndPreservesFluidsOnBreak(GameTestHelper h) { AxialGeneratorTests.steamDrivesDynamoAndPreservesFluidsOnBreak(h); }
    @GameTest(template="test_empty",timeoutTicks=600)
    public static void fluxLaserCapabilityAndStopPersist(GameTestHelper h) { OpticalEnergyRegressionTests.fluxLaserCapabilityAndStopPersist(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=600)
    public static void switchReadsOnlyItsFaceAndCrowbarReturnsDefaults(GameTestHelper h) { ControlCoverTests.switchReadsOnlyItsFaceAndCrowbarReturnsDefaults(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=600)
    public static void rebootWindowsAndMachineTimerPersist(GameTestHelper h) { ControlCoverTests.rebootWindowsAndMachineTimerPersist(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=600)
    public static void automaticCoverWakesOnInputWithoutConsumingDuringProbe(GameTestHelper h) { ControlCoverTests.automaticCoverWakesOnInputWithoutConsumingDuringProbe(h); }
    @GameTest(template="test_empty",timeoutTicks=600)
    public static void fluidBurningBoxCapabilitiesAreCached(GameTestHelper h) { CapabilityHandleTests.fluidBurningBoxCapabilitiesAreCached(h); }
    @GameTest(template="harvest_empty",timeoutTicks=600)
    public static void pressureValveVentsAGasAndHeatsWhatStandsInFront(GameTestHelper h) { CoverBehaviorTests.pressureValveVentsAGasAndHeatsWhatStandsInFront(h); }
}
