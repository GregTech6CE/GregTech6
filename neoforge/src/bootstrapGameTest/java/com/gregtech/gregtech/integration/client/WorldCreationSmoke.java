package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
@net.neoforged.fml.common.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
/** Opt-in fresh normal-world preflight. Bootstrap sources never enter the production jar. */
public final class WorldCreationSmoke {
    private static final boolean ENABLED = Boolean.getBoolean("gregtech.integration.clientCreateWorldSmoke");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean TERMINAL = new AtomicBoolean();
    private static final String ID = UUID.randomUUID().toString();
    private static final String WORLD = "creation-smoke-" + ID;
    private static final long START = System.nanoTime();
    private static int stage, frames;
    private static volatile String observedScreen = "unobserved";
    private static java.util.concurrent.CompletableFuture<JsonObject> probe;
    private static final java.util.concurrent.ScheduledExecutorService WATCHDOG = startWatchdog();
    private static java.util.concurrent.ScheduledExecutorService startWatchdog() {
        if (!ENABLED) return null;
        var executor = Executors.newSingleThreadScheduledExecutor(r -> {
            var thread = new Thread(r, "world-creation-preflight-watchdog"); thread.setDaemon(true); return thread;
        });
        int seconds = Integer.getInteger("gregtech.integration.clientSmokeTimeoutSeconds", 360);
        executor.schedule(() -> fail(new IllegalStateException("World creation timed out at stage " + stage + ", screen " + observedScreen)), seconds, TimeUnit.SECONDS);
        LOGGER.info("WORLD_CREATION_SMOKE_STARTED {}", receipt());
        return executor;
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void screen(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event) {
        if (!ENABLED || TERMINAL.get()) return;
        var minecraft = Minecraft.getInstance();
        observedScreen = event.getScreen().getClass().getName();
        try {
            if (minecraft.getOverlay() != null) return;
            if (stage == 0 && event.getScreen() instanceof TitleScreen) {
                minecraft.options.pauseOnLostFocus = false;
                var root = minecraft.gameDirectory.toPath().toAbsolutePath().normalize();
                if (!root.endsWith("world-creation-smoke-run") || Files.exists(root.resolve("saves").resolve(WORLD)))
                    throw new IllegalStateException("Preflight needs an isolated fresh save");
                stage = 1;
                CreateWorldScreen.openFresh(minecraft,event.getScreen());
            } else if (stage == 1 && event.getScreen() instanceof CreateWorldScreen creation) {
                creation.getUiState().setName(WORLD);
                if (++frames < 10) return;
                LOGGER.info("WORLD_CREATION_DATAPACK_SCREEN_SUCCESS {}", receipt());
                stage = 2; frames = 0;
                click(creation,"selectWorld.create");
            } else if (stage == 2 && event.getScreen() instanceof net.minecraft.client.gui.screens.ConfirmScreen confirm) {
                // Vanilla lifecycle confirmation for this newly created isolated test world only.
                click(confirm,"gui.proceed","gui.yes");
            }
        } catch (Throwable failure) { fail(failure); }
    }
    private static void click(net.minecraft.client.gui.screens.Screen screen, String... keys) {
        for (var child : screen.children()) if (child instanceof Button button && button.active && button.visible
                && button.getMessage().getContents() instanceof TranslatableContents text) {
            for (var key : keys) if (text.getKey().equals(key)) { button.onPress(); return; }
        }
        throw new IllegalStateException("No expected creation button on " + screen.getClass().getName());
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void world(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
        if (!ENABLED || TERMINAL.get() || stage < 2 || stage >= 4) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null || minecraft.getOverlay() != null
                || minecraft.getSingleplayerServer() == null) return;
        observedScreen = "in_game";
        try {
            if (stage == 2) {
                stage = 3; frames = 0;
                var server = minecraft.getSingleplayerServer();
                probe = server.submit(() -> {
                    var result = receipt();
                    int checked = 0;
                    for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                        var input = new ItemStack(item,2); if (input.isEmpty()) continue;
                        var output = com.gregtech.gregtech.recipe.CraftingMaterialForms.canonical(input);
                        if (output.isEmpty() || output.getCount() != 2) throw new IllegalStateException("Canonical output lost count for " + item);
                        checked++;
                    }
                    var ordinary = new ItemStack(Items.BARRIER,3);
                    if (com.gregtech.gregtech.recipe.CraftingMaterialForms.canonical(ordinary) != ordinary)
                        throw new IllegalStateException("Unknown form changed its native item");
                    var named = new ItemStack(Items.IRON_INGOT,3);
                    named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("keep me"));
                    if (com.gregtech.gregtech.recipe.CraftingMaterialForms.canonical(named) != named)
                        throw new IllegalStateException("Stack metadata was discarded");
                    var iron = com.gregtech.gregtech.registry.GTItems.getStack(com.gregtech.gregtech.data.MaterialPrefix.ingot,
                            com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Iron"),3);
                    var canonical = com.gregtech.gregtech.recipe.CraftingMaterialForms.canonical(iron);
                    if (!canonical.is(Items.IRON_INGOT) || canonical.getCount() != 3)
                        throw new IllegalStateException("Known iron form no longer unifies");
                    int recipes = server.getRecipeManager().getRecipes().size();
                    if (recipes < 1000) throw new IllegalStateException("Incomplete actual recipe registry: " + recipes);
                    result.addProperty("canonicalItemsChecked",checked);
                    result.addProperty("recipes",recipes);
                    result.addProperty("serverTicks",server.getTickCount());
                    result.addProperty("dimension",minecraft.level.dimension().location().toString());
                    return result;
                });
            }
            if (++frames < 30 || !probe.isDone()) return;
            var result = probe.join();
            result.addProperty("renderedWorldFrames",frames);
            if (!Files.isRegularFile(minecraft.gameDirectory.toPath().resolve("saves").resolve(WORLD).resolve("level.dat")))
                throw new IllegalStateException("Fresh world has no level.dat");
            stage = 4;
            String file = "world-creation-neoforge-" + ID + ".png";
            event.getGuiGraphics().flush();
            Screenshot.grab(minecraft.gameDirectory,file,minecraft.getMainRenderTarget(),message -> {
                try {
                    var path = minecraft.gameDirectory.toPath().resolve("screenshots").resolve(file).toAbsolutePath();
                    if (!Files.isRegularFile(path) || Files.size(path) == 0) throw new IllegalStateException("Screenshot missing");
                    result.addProperty("screenshot",path.toString());
                    result.addProperty("elapsedMs",TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-START));
                    var receiptPath = minecraft.gameDirectory.toPath().resolve("world-creation-" + ID + ".json");
                    Files.writeString(receiptPath,result.toString());
                    if (TERMINAL.compareAndSet(false,true)) {
                        WATCHDOG.shutdownNow(); LOGGER.info("WORLD_CREATION_SMOKE_SUCCESS {}",result); minecraft.execute(minecraft::stop);
                    }
                } catch (Throwable failure) { fail(failure); }
            });
        } catch (Throwable failure) { fail(failure); }
    }
    private static JsonObject receipt() {
        var result = new JsonObject(); result.addProperty("platform","neoforge"); result.addProperty("id",ID);
        result.addProperty("world",WORLD); result.addProperty("stage",stage);
        result.addProperty("elapsedMs",TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-START)); return result;
    }
    private static void fail(Throwable failure) {
        if (!TERMINAL.compareAndSet(false,true)) return;
        if (WATCHDOG != null) WATCHDOG.shutdownNow();
        LOGGER.error("WORLD_CREATION_SMOKE_FAILED {}",receipt(),failure);
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
    }
}
