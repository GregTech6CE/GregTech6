package com.gregtech.gregtech.integration.gameplay;

import com.gregtech.gregtech.GregTech;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.gametest.framework.LogTestReporter;
import net.minecraft.gametest.framework.TestReporter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.gametest.ForgeGameTestHooks;

import java.nio.file.Files;
import java.nio.file.Path;

/** Opt-in development reports preserve the normal log reporter as well as XML. */
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.DEDICATED_SERVER)
public final class ForgeGameTestReports {
    private ForgeGameTestReports() {}

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        String destination = System.getProperty("gregtech.integration.gameTestReport", "");
        if (destination.isBlank() || !ForgeGameTestHooks.isGametestServer()) return;
        try {
            Path path = Path.of(destination).toAbsolutePath().normalize();
            Files.createDirectories(path.getParent());
            var xml = new JUnitLikeTestReporter(path.toFile());
            var log = new LogTestReporter();
            GlobalTestReporter.replaceWith(new TestReporter() {
                @Override
                public void onTestFailed(GameTestInfo info) {
                    log.onTestFailed(info);
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
                    GregTech.LOGGER.info("GT6_GAMETEST_REPORT_FINISHED {}", path);
                }
            });
            GregTech.LOGGER.info("GT6_GAMETEST_REPORT_STARTED {}", path);
        } catch (Exception failure) {
            throw new IllegalStateException("Could not initialize the requested GameTest XML report", failure);
        }
    }
}
