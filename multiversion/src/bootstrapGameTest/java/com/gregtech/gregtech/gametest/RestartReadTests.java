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

/** Phase 2: only native chunk loading and queries; no block placement, NBT injection or tank fill. */
@GameTestHolder("gregtech_restart_read")
@PrefixGameTestTemplate(false)
public final class RestartReadTests {
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void readDisposableFixture(GameTestHelper h) throws Exception {
        String enabled = System.getProperty("neoforge.enabledGameTestNamespaces",
                System.getProperty("forge.enabledGameTestNamespaces", ""));
        h.assertTrue("gregtech_restart_read".equals(enabled), "exact single phase namespace required");
        h.assertTrue(!RestartFixture.warm, "warm=false proves write phase did not run in this JVM");
        var world = RestartFixture.world(h);
        var marker = RestartFixture.readMarker(world);
        h.assertTrue("gt6-bee-reactor-restart-v3-y304".equals(marker.getProperty("contract")), "known fixture contract required");
        h.assertTrue(world.toString().equals(marker.getProperty("world")), "same canonical world, not copied fixture");
        h.assertTrue(!Long.toString(ProcessHandle.current().pid()).equals(marker.getProperty("pid")), "separate JVM required");
        String nonce = marker.getProperty("nonce");
        h.assertTrue(nonce != null && !nonce.isBlank(), "unique persisted fixture nonce required");
        RestartFixture.assertState(h, nonce);
        RestartFixture.compareState(h, marker);
        RestartFixture.continueOneTick(h);
        com.mojang.logging.LogUtils.getLogger().info("GT6_RESTART_READ warm=false world={} pid={} writerPid={} nonce={}",
                world, ProcessHandle.current().pid(), marker.getProperty("pid"), nonce);
        h.succeed();
    }
}
