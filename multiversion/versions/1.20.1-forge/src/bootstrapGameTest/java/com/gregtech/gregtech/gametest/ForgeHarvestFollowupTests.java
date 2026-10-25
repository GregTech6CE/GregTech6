package com.gregtech.gregtech.gametest;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("gt6ce_forge_harvest_followup") @PrefixGameTestTemplate(false)
public final class ForgeHarvestFollowupTests {
    @GameTest(template="harvest_empty",timeoutTicks=600) public static void blockClicksControlModesAndStatusAndPersistPanels(GameTestHelper h) { BatteryBoxPanelTests.blockClicksControlModesAndStatusAndPersistPanels(h); }
    @GameTest(template="harvest_empty",timeoutTicks=600) public static void directSelectorAndStatusPanelsPersistAndDrop(GameTestHelper h) { ElectricTransformerControlTests.directSelectorAndStatusPanelsPersistAndDrop(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=600) public static void switchReadsOnlyItsFaceAndCrowbarReturnsDefaults(GameTestHelper h) { ControlCoverTests.switchReadsOnlyItsFaceAndCrowbarReturnsDefaults(h); }
    @GameTest(template="test_blueprint_empty",timeoutTicks=600) public static void automaticCoverWakesOnInputWithoutConsumingDuringProbe(GameTestHelper h) { ControlCoverTests.automaticCoverWakesOnInputWithoutConsumingDuringProbe(h); }
}
