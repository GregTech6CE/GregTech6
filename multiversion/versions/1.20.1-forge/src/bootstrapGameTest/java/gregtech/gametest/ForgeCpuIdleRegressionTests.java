package gregtech.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Single-case rerun for the powered previous-pass/idle CPU meter lifecycle. */
@GameTestHolder("gt6ce_forge_cpu_idle")
@PrefixGameTestTemplate(false)
public final class ForgeCpuIdleRegressionTests {
    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void previousPassDrivesAllFourMetersAndEmitsRedstone(GameTestHelper helper) {
        LogisticsCpuDisplayTests.previousPassDrivesAllFourMetersAndEmitsRedstone(helper);
    }
}
