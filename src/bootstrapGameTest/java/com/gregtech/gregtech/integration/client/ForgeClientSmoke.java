package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.slf4j.Logger;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Opt-in development smoke only: an actual rendered title screen and saved screenshot.
 * This bootstrap source set is excluded from the production mod jar.
 */
@Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeClientSmoke {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PLATFORM = "forge";
    private static final String MINECRAFT_VERSION = "1.20.1";
    private static final boolean ENABLED =
            Boolean.getBoolean("gregtech.integration.clientSmoke");
    private static final int REQUIRED_RENDERED_FRAMES = 5;
    // Both vanilla title screens may fade their widgets in over two seconds.
    private static final long TITLE_SETTLE_NANOS = TimeUnit.MILLISECONDS.toNanos(2500);
    private static final long STARTED_AT = System.nanoTime();
    private static final AtomicBoolean TERMINAL = new AtomicBoolean();
    private static final int DEFAULT_TIMEOUT_SECONDS = 120;
    private static int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
    // Immutable snapshots cross from the render thread to the watchdog without reading
    // Minecraft's mutable screen/overlay state on a background thread.
    private static volatile ClientSnapshot lastSnapshot =
            new ClientSnapshot("unobserved", "unobserved", false, 0, 0, "unobserved");
    private static final ScheduledExecutorService WATCHDOG = startWatchdog();

    // These fields are accessed only by the client/render thread.
    private static Screen observedTitle;
    private static long firstTitleFrameAt;
    private static int renderedFrames;
    private static boolean captureRequested;

    private ForgeClientSmoke() {}

    private static ScheduledExecutorService startWatchdog() {
        if (!ENABLED) return null;
        try {
            String configured = System.getProperty("gregtech.integration.clientSmokeTimeoutSeconds");
            if (configured != null) timeoutSeconds = Integer.parseInt(configured.trim());
            if (timeoutSeconds < 30 || timeoutSeconds > 300) {
                throw new IllegalArgumentException("clientSmokeTimeoutSeconds must be within 30..300");
            }
        } catch (RuntimeException invalidConfiguration) {
            fail("configuration", invalidConfiguration);
            return null;
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "gregtech-forge-client-smoke-watchdog");
            thread.setDaemon(true);
            return thread;
        });
        executor.schedule(() -> fail("timeout",
                new TimeoutException("No completed title-screen screenshot within "
                        + timeoutSeconds + " seconds")),
                timeoutSeconds, TimeUnit.SECONDS);
        LOGGER.info("CLIENT_SMOKE_STARTED {}", identity());
        return executor;
    }

    @SubscribeEvent
    public static void afterScreenRender(ScreenEvent.Render.Post event) {
        if (!ENABLED) return;
        Minecraft minecraft = Minecraft.getInstance();
        try {
            snapshotState(minecraft, "render_thread");
            if (TERMINAL.get() || captureRequested) return;
            if (!(event.getScreen() instanceof TitleScreen)
                    || minecraft.screen != event.getScreen()
                    || minecraft.getOverlay() != null) {
                observedTitle = null;
                renderedFrames = 0;
                firstTitleFrameAt = 0;
                snapshotState(minecraft, "render_thread");
                return;
            }
            if (observedTitle != event.getScreen()) {
                observedTitle = event.getScreen();
                firstTitleFrameAt = System.nanoTime();
                renderedFrames = 0;
            }
            renderedFrames++;
            snapshotState(minecraft, "render_thread");
            if (renderedFrames < REQUIRED_RENDERED_FRAMES
                    || System.nanoTime() - firstTitleFrameAt < TITLE_SETTLE_NANOS) {
                return;
            }

            String fileName = "gregtech-client-smoke-" + PLATFORM + "-"
                    + MINECRAFT_VERSION + "-" + UUID.randomUUID() + ".png";
            Path screenshot = minecraft.gameDirectory.toPath()
                    .resolve(Screenshot.SCREENSHOT_DIR).resolve(fileName)
                    .toAbsolutePath().normalize();
            // A fresh UUID prevents an old PNG from satisfying a failed/cancelled grab.
            if (Files.exists(screenshot)) {
                throw new IOException("Screenshot path unexpectedly already exists: " + screenshot);
            }
            captureRequested = true;
            snapshotState(minecraft, "render_thread");
            int capturedFrames = renderedFrames;
            // ScreenEvent.Post runs before GameRenderer's final flush in both versions.
            event.getGuiGraphics().flush();
            Screenshot.grab(minecraft.gameDirectory, fileName,
                    minecraft.getMainRenderTarget(),
                    message -> afterScreenshotSaved(minecraft, screenshot, capturedFrames, message));
        } catch (Throwable failure) {
            fail("render_or_capture", failure);
        }
    }

    private static void afterScreenshotSaved(Minecraft minecraft, Path screenshot,
            int capturedFrames, Component message) {
        if (TERMINAL.get()) return;
        try {
            if (message != null
                    && message.getContents() instanceof TranslatableContents contents
                    && "screenshot.failure".equals(contents.getKey())) {
                throw new IOException("Vanilla screenshot failure callback: " + message.getString());
            }
            if (!Files.isRegularFile(screenshot) || Files.size(screenshot) == 0) {
                throw new IOException("Screenshot callback has no saved PNG (possibly cancelled or redirected): "
                        + screenshot);
            }
            BufferedImage image = ImageIO.read(screenshot.toFile());
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new IOException("Saved screenshot is not a decodable image: " + screenshot);
            }
            int width = image.getWidth();
            int height = image.getHeight();
            image.flush();
            JsonObject result = identity();
            result.addProperty("screenshot", screenshot.toString());
            result.addProperty("width", width);
            result.addProperty("height", height);
            result.addProperty("renderedFrames", capturedFrames);
            result.addProperty("screen", TitleScreen.class.getName());
            // The vanilla IO worker invokes the callback only after writeToFile returns.
            // Finish validation before scheduling any client stop.
            minecraft.execute(() -> finishSuccess(minecraft, result));
        } catch (Throwable failure) {
            fail("screenshot_callback", failure);
        }
    }

    private static void finishSuccess(Minecraft minecraft, JsonObject result) {
        if (!TERMINAL.compareAndSet(false, true)) return;
        if (WATCHDOG != null) WATCHDOG.shutdownNow();
        LOGGER.info("CLIENT_SMOKE_SUCCESS {}", result);
        minecraft.stop();
    }

    private static void fail(String phase, Throwable failure) {
        if (!TERMINAL.compareAndSet(false, true)) return;
        if (WATCHDOG != null) WATCHDOG.shutdownNow();
        JsonObject result = identity();
        result.addProperty("phase", phase);
        result.addProperty("error", failure.toString());
        addSnapshot(result, lastSnapshot);
        // Log first: a stuck client thread must never suppress the failure receipt.
        LOGGER.error("CLIENT_SMOKE_FAILED {}", result, failure);
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                // If the main thread can still service tasks, record its current state
                // before stopping. This supplemental marker is not a second FAILED.
                minecraft.execute(() -> {
                    try {
                        snapshotState(minecraft, "main_thread_at_failure");
                        JsonObject current = identity();
                        current.addProperty("phase", phase);
                        addSnapshot(current, lastSnapshot);
                        LOGGER.warn("CLIENT_SMOKE_FAILURE_STATE {}", current);
                    } catch (Throwable diagnosticFailure) {
                        LOGGER.error("CLIENT_SMOKE_DIAGNOSTIC_ERROR", diagnosticFailure);
                    } finally {
                        minecraft.stop();
                    }
                });
            }
        } catch (Throwable schedulingFailure) {
            LOGGER.error("CLIENT_SMOKE_STOP_SCHEDULING_ERROR", schedulingFailure);
        }
    }

    private static void snapshotState(Minecraft minecraft, String source) {
        Object overlay = minecraft.getOverlay();
        lastSnapshot = new ClientSnapshot(
                minecraft.screen == null ? "none" : minecraft.screen.getClass().getName(),
                overlay == null ? "none" : overlay.getClass().getName(),
                captureRequested, renderedFrames, System.nanoTime(), source);
    }

    private static void addSnapshot(JsonObject result, ClientSnapshot snapshot) {
        result.addProperty("screen", snapshot.screen());
        result.addProperty("overlay", snapshot.overlay());
        result.addProperty("captureRequested", snapshot.captureRequested());
        result.addProperty("renderedFrames", snapshot.renderedFrames());
        result.addProperty("snapshotSource", snapshot.source());
        result.addProperty("snapshotAgeMs", snapshot.sampledAtNanos() == 0 ? -1
                : TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - snapshot.sampledAtNanos()));
    }

    private record ClientSnapshot(String screen, String overlay, boolean captureRequested,
            int renderedFrames, long sampledAtNanos, String source) {}

    private static JsonObject identity() {
        JsonObject result = new JsonObject();
        result.addProperty("platform", PLATFORM);
        result.addProperty("minecraft", MINECRAFT_VERSION);
        result.addProperty("timeoutSeconds", timeoutSeconds);
        result.addProperty("elapsedMs",
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - STARTED_AT));
        return result;
    }
}
