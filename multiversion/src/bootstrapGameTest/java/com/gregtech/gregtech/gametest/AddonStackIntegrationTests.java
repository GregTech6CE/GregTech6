package com.gregtech.gregtech.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Small migration regression, separate from the upstream overlay benchmark batch. */
@GameTestHolder("gt6ce_addon_stack")
@PrefixGameTestTemplate(false)
public final class AddonStackIntegrationTests {
    @GameTest(template = "empty", batch = "gt6ce_addon_stack", timeoutTicks = 200)
    public static void furnaceOverlayKeepsDeletesAndCustomRows(GameTestHelper helper) {
        int checks = MachineRecipeOverlayTests.verifyFurnaceMirrorOverlay(helper.getLevel().getServer());
        helper.assertTrue(checks == 5, "all Furnace mirror/reload cases must run");
        helper.succeed();
    }
}
