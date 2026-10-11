package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;

/** Separate production-environment test mod. Never included in the delivered GT jar. */
@Mod("gregtech_delivery_probe")
public final class ProductionClientSmoke {
    private final long started = System.nanoTime();
    private final AtomicBoolean terminal = new AtomicBoolean();
    private long titleAt;
    private int frames;
    private boolean capturing;
    private JsonObject surfaceChecks;

    public ProductionClientSmoke() {
        LogUtils.getLogger().info("PRODUCTION_SMOKE_STARTED neoforge");
        NeoForge.EVENT_BUS.addListener(this::render);
    }

    private void render(ScreenEvent.Render.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (terminal.get() || capturing) return;
        if (!(event.getScreen() instanceof TitleScreen) || client.getOverlay() != null) {
            titleAt = 0;
            frames = 0;
            return;
        }
        if (titleAt == 0) titleAt = System.nanoTime();
        if (++frames < 5 || System.nanoTime() - titleAt < TimeUnit.SECONDS.toNanos(3)) return;
        capturing = true;
        try {
            var graphics=event.getGuiGraphics();
            // Keep existing model assertions, while reserving this capture for the panel atlas.
            graphics.pose().pushPose();graphics.pose().translate(-5000,0,0);
            try {
                surfaceChecks=SurfaceDeliveryChecks.capture(client,graphics);
                surfaceChecks.add("sandwich",SandwichDeliveryChecks.capture(client,graphics));
                surfaceChecks.add("canvas",CanvasDeliveryChecks.capture(client,graphics));
            } finally { graphics.pose().popPose(); }
            surfaceChecks.add("decorativePanels",DecorativePanelDeliveryChecks.capture(client,graphics));
            surfaceChecks.add("commonBlockTooltips",CommonBlockDeliveryChecks.verify());
            surfaceChecks.add("smelteryTooltipsAndMaterials",SmelteryDeliveryChecks.verify());
            surfaceChecks.add("multiblockTankTooltipsAndMaterials",MultiblockTankDeliveryChecks.verify());
            surfaceChecks.add("processControllerTooltipsAndMaterials",ProcessControllerDeliveryChecks.verify());
            surfaceChecks.add("largeRecipeControllerTooltipsAndMaterials",LargeRecipeControllerDeliveryChecks.verify());
            surfaceChecks.add("sourceBasicMachineTooltips",BasicMachineSourceDeliveryChecks.verify());
            surfaceChecks.add("advancedControllerTooltipsAndMaterials",AdvancedControllerDeliveryChecks.verify());
            surfaceChecks.add("generatorSourceTooltipsAndMaterials",GeneratorTooltipDeliveryChecks.verify());
            surfaceChecks.add("utilityControllerTooltipsAndMaterials",UtilityControllerDeliveryChecks.verify());
            surfaceChecks.add("sensorSourceTooltipsAndMaterials",SensorSourceDeliveryChecks.verify());
            surfaceChecks.add("originalMultiblockParts",MultiblockPartDeliveryChecks.verify());
            surfaceChecks.add("originalChinese",LanguageDeliveryChecks.capture(client,graphics));
        }
        catch(Throwable error){terminal.set(true);LogUtils.getLogger().error("PRODUCTION_SMOKE_FAILED",error);client.execute(client::stop);return;}
        String name = "production-neoforge-" + UUID.randomUUID() + ".png";
        var screenshot = client.gameDirectory.toPath().resolve("screenshots").resolve(name).toAbsolutePath();
        event.getGuiGraphics().flush();
        Screenshot.grab(client.gameDirectory, name, client.getMainRenderTarget(), message -> {
            try {
                if (!Files.isRegularFile(screenshot) || Files.size(screenshot) == 0) throw new IllegalStateException("Missing screenshot");
                var image = ImageIO.read(screenshot.toFile());
                if (image == null) throw new IllegalStateException("Undecodable screenshot");
                JsonObject receipt = new JsonObject();
                receipt.add("surfaceChecks",surfaceChecks);
                receipt.addProperty("platform", "neoforge");
                receipt.addProperty("screen", TitleScreen.class.getName());
                receipt.addProperty("screenshot", screenshot.toString());
                receipt.addProperty("width", image.getWidth());
                receipt.addProperty("height", image.getHeight());
                receipt.addProperty("renderedFrames", frames);
                receipt.addProperty("elapsedMs", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
                image.flush();
                client.execute(() -> {
                    if (terminal.compareAndSet(false, true)) {
                        LogUtils.getLogger().info("PRODUCTION_SMOKE_SUCCESS {}", receipt);
                        client.stop();
                    }
                });
            } catch (Exception error) {
                if (terminal.compareAndSet(false, true)) {
                    LogUtils.getLogger().error("PRODUCTION_SMOKE_FAILED", error);
                    client.execute(client::stop);
                }
            }
        });
    }
}
