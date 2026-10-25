package com.gregtech.gregtech.integration.gameplay;

import com.gregtech.gregtech.api.mod.GregTechIdentity;
import com.mojang.logging.LogUtils;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.gametest.framework.LogTestReporter;
import net.minecraft.gametest.framework.TestReporter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;

import java.nio.file.Files;
import java.nio.file.Path;

/** Opt-in development reports preserve the normal log reporter as well as XML. */
@EventBusSubscriber(modid = GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD,
        value = Dist.DEDICATED_SERVER)
public final class NeoForgeGameTestReports {
    private NeoForgeGameTestReports() {}

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        String destination = System.getProperty("gregtech.integration.gameTestReport", "");
        if (destination.isBlank() || !GameTestHooks.isGametestServer()) return;
        try {
            Path path = Path.of(destination).toAbsolutePath().normalize();
            Files.createDirectories(path.getParent());
            var xml = new JUnitLikeTestReporter(path.toFile());
            var log = new LogTestReporter();
            GlobalTestReporter.replaceWith(new TestReporter() {
                @Override
                public void onTestFailed(GameTestInfo info) {
                    log.onTestFailed(info);
                    LogUtils.getLogger().error("GT6_GAMETEST_FAILURE {}", info.getTestName(), info.getError());
                    xml.onTestFailed(info);
                }

                @Override
                public void onTestSuccess(GameTestInfo info) {
                    log.onTestSuccess(info);
                    xml.onTestSuccess(info);
                }

                @Override
                public void finish() {
                    log.finish();
                    xml.finish();
                    LogUtils.getLogger().info("GT6_GAMETEST_REPORT_FINISHED {}", path);
                }
            });
            LogUtils.getLogger().info("GT6_GAMETEST_REPORT_STARTED {}", path);
        } catch (Exception failure) {
            throw new IllegalStateException("Could not initialize the requested GameTest XML report", failure);
        }
    }
}
