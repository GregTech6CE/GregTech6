package gregtech.integration.client;

import com.google.gson.JsonObject;
import gregtech.internal.recipe.Recipe;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.jei.RecipeMapCategory;
import gregtech.content.recipe.MachineRecipeOverlay;
import com.mojang.logging.LogUtils;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
//?}
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Opt-in client proof for the machine-recipe overlay. It asks the live JEI or EMI registry, then exits.
 * Bootstrap sources stay out of the production jar.
 */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
//?}
public final class MachineOverlayViewerSmoke {
    private static final boolean ENABLED = Boolean.getBoolean("gregtech.integration.machineOverlayViewerSmoke");
    private static final String PHASE = System.getProperty("gregtech.integration.machineOverlayViewerPhase", "create");
    private static final String WORLD = "overlay-viewer";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean TERMINAL = new AtomicBoolean();
    private static final long START = System.nanoTime();
    private static int stage, frames, startupApplies, seenRows, viewerFrames;
    private static boolean counted;
    private static String victimId = System.getProperty("gregtech.integration.machineOverlayVictim", "");
    private static CompletableFuture<Void> reload;
    private static volatile String observed = "unobserved";
    private static final java.util.concurrent.ScheduledExecutorService WATCHDOG = startWatchdog();

    private MachineOverlayViewerSmoke() {}

    private static java.util.concurrent.ScheduledExecutorService startWatchdog() {
        if (!ENABLED) return null;
        var executor = Executors.newSingleThreadScheduledExecutor(r -> {
            var thread = new Thread(r, "machine-overlay-viewer-watchdog");
            thread.setDaemon(true);
            return thread;
        });
        executor.schedule(() -> fail(new IllegalStateException("overlay viewer timed out at stage " + stage + " screen " + observed)),
                Integer.getInteger("gregtech.integration.clientSmokeTimeoutSeconds", 720), TimeUnit.SECONDS);
        LOGGER.info("MACHINE_OVERLAY_VIEWER_STARTED phase {}", PHASE);
        return executor;
    }

    @SubscribeEvent
    public static void screen(ScreenEvent.Render.Post event) {
        if (!ENABLED || TERMINAL.get() || !"create".equals(PHASE)) return;
        var minecraft = Minecraft.getInstance();
        observed = event.getScreen().getClass().getName();
        try {
            if (minecraft.getOverlay() != null) return;
            if (event.getScreen() instanceof ConfirmScreen confirm) {
                click(confirm, "gui.proceed", "gui.yes");
                return;
            }
            if (stage >= 2) return;
            if (stage == 0 && event.getScreen() instanceof TitleScreen) {
                minecraft.options.pauseOnLostFocus = false;
                minecraft.options.renderDistance().set(6);
                var root = minecraft.gameDirectory.toPath().toAbsolutePath().normalize();
                if (!root.getFileName().toString().startsWith("machine-overlay") || Files.exists(root.resolve("saves").resolve(WORLD)))
                    throw new IllegalStateException("create phase needs a fresh isolated client directory");
                stage = 1;
                frames = 0;
                CreateWorldScreen.openFresh(minecraft, event.getScreen());
            } else if (stage == 1 && event.getScreen() instanceof CreateWorldScreen creation) {
                creation.getUiState().setName(WORLD);
                if (++frames < 10) return;
                stage = 2;
                frames = 0;
                click(creation, "selectWorld.create");
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public static void world(RenderGuiEvent.Post event) {
        if (!ENABLED || TERMINAL.get()) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.getOverlay() != null) return;
        observed = "in_game";
        try {
            var integrated = minecraft.getSingleplayerServer();
            if ("join".equals(PHASE)) {
                if (integrated != null) throw new IllegalStateException("dedicated client joined its own integrated server");
                if (++frames < 40 || !viewerReady(victimId)) return;
                var result = MachineRecipeOverlay.lastResult();
                if (MachineRecipeOverlay.applications() < 1 || result.added() != 3 || result.removed() != 1 || result.rejected() != 0)
                    throw new IllegalStateException("dedicated client overlay " + MachineRecipeOverlay.applications() + " " + result);
                finish(minecraft, result);
                return;
            }
            if (integrated == null) return;
            if ("reopen".equals(PHASE)) {
                if (++frames < 40) return;
                if (victimId.isBlank()) victimId = Files.readString(minecraft.gameDirectory.toPath().resolve("overlay-victim.txt")).trim();
                if (!viewerReady(victimId)) return;
                var result = MachineRecipeOverlay.lastResult();
                if (MachineRecipeOverlay.applications() < 1 || result.added() != 3 || result.removed() != 1 || result.rejected() != 0)
                    throw new IllegalStateException("reopened world overlay " + MachineRecipeOverlay.applications() + " " + result);
                finish(minecraft, result);
                return;
            }
            if (stage < 2 || ++frames < 40) return;
            if (stage == 4) {
                if (!viewerReady(victimId)) return;
                Files.writeString(minecraft.gameDirectory.toPath().resolve("overlay-victim.txt"), victimId);
                var server = minecraft.getSingleplayerServer();
                if (server != null) server.saveEverything(false, false, true);
                finish(minecraft, MachineRecipeOverlay.lastResult());
                return;
            }
            if (!counted) {
                startupApplies = MachineRecipeOverlay.applications();
                counted = true;
            }
            if (reload != null) {
                if (!reload.isDone()) return;
                reload.join();
                reload = null;
                var result = MachineRecipeOverlay.lastResult();
                int delta = MachineRecipeOverlay.applications() - startupApplies;
                if (result.added() != 3 || result.removed() != 1 || result.rejected() != 0)
                    throw new IllegalStateException("probe overlay " + result);
                if (stage == 2) {
                    if (delta != 1) throw new IllegalStateException("singleplayer applied the overlay " + delta + " times");
                    seenRows = MachineRecipeOverlay.staticRows();
                    stage = 3;
                    startupApplies = MachineRecipeOverlay.applications();
                    reload = integrated.reloadResources(selected(integrated, true));
                    return;
                }
                if (delta != 1 || MachineRecipeOverlay.staticRows() != seenRows)
                    throw new IllegalStateException("second reload changed the overlay");
                stage = 4;
                viewerFrames = 0;
                return;
            }
            if (stage == 2 && victimId.isBlank()) {
                victimId = victim();
                writePack(integrated);
                startupApplies = MachineRecipeOverlay.applications();
                reload = integrated.reloadResources(selected(integrated, true));
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static void finish(Minecraft minecraft, MachineRecipeOverlay.Result result) throws Exception {
        var receipt = new JsonObject();
        receipt.addProperty("phase", PHASE);
        receipt.addProperty("applications", MachineRecipeOverlay.applications());
        receipt.addProperty("added", result.added());
        receipt.addProperty("removed", result.removed());
        receipt.addProperty("rejected", result.rejected());
        receipt.addProperty("staticRows", MachineRecipeOverlay.staticRows());
        receipt.addProperty("victim", victimId);
        receipt.addProperty("integratedServer", minecraft.getSingleplayerServer() != null);
        receipt.addProperty("viewer", viewer());
        receipt.addProperty("viewerHeartToNetherStar", true);
        receipt.addProperty("viewerVictimHidden", true);
        receipt.addProperty("elapsedMs", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - START));
        var path = minecraft.gameDirectory.toPath().resolve("overlay-viewer-" + PHASE + ".json");
        Files.writeString(path, receipt.toString());
        if (TERMINAL.compareAndSet(false, true)) {
            if (WATCHDOG != null) WATCHDOG.shutdownNow();
            LOGGER.info("MACHINE_OVERLAY_VIEWER_SUCCESS {}", receipt);
            minecraft.execute(minecraft::stop);
        }
    }

    private static boolean viewerReady(String victim) throws ReflectiveOperationException {
        var rows = switch (viewer()) {
            case "jei" -> Jei.crusher();
            case "emi" -> Emi.crusher();
            default -> throw new IllegalStateException("overlay viewer smoke needs JEI or EMI");
        };
        if (rows == null) return false;
        try {
            assertViewer(victim, rows);
            return true;
        } catch (RuntimeException waiting) {
            if (++viewerFrames > 200) throw waiting;
            return false;
        }
    }

    private static String viewer() {
        if (ModList.get().isLoaded("jei")) return "jei";
        return ModList.get().isLoaded("emi") ? "emi" : "none";
    }

    private record Row(List<ItemStack> inputs, List<ItemStack> outputs) {}

    private static void assertViewer(String victim, List<Row> rows) {
        if (victim == null || victim.isBlank()) throw new IllegalStateException("removed crusher input was not recorded");
//? if >=1.21.1 {
/*        Item removed = BuiltInRegistries.ITEM.get(ResourceLocation.parse(victim));
*///?} else {
        Item removed = BuiltInRegistries.ITEM.get(new ResourceLocation(victim));
//?}
        boolean heart = false, stillThere = false;
        for (Row row : rows) {
            if (has(row.inputs(), Items.HEART_OF_THE_SEA) && has(row.outputs(), Items.NETHER_STAR)) heart = true;
            if (has(row.inputs(), removed)) stillThere = true;
        }
        if (!heart) throw new IllegalStateException(viewer() + " crusher page has no heart of the sea to nether star row");
        if (stillThere) throw new IllegalStateException(viewer() + " still shows the removed crusher input " + victim);
    }

    private static boolean has(List<ItemStack> stacks, Item item) {
        for (var stack : stacks) if (stack.is(item)) return true;
        return false;
    }

    private static List<ItemStack> present(ItemStack[] stacks) {
        var list = new ArrayList<ItemStack>();
        if (stacks != null) for (var stack : stacks) if (stack != null && !stack.isEmpty()) list.add(stack);
        return list;
    }

    /** JEI types stay in here so an EMI-only client never loads them. */
    private static final class Jei {
        static List<Row> crusher() {
            var runtime = RecipePresentationJeiSmoke.runtime;
            if (runtime == null) return null;
            var rows = new ArrayList<Row>();
            var type = RecipeMapCategory.recipeType(MachineRecipeMaps.Crusher);
            for (Recipe recipe : runtime.getRecipeManager().createRecipeLookup(type).get().toList())
                rows.add(new Row(present(recipe.mInputs), present(recipe.mOutputs)));
            return rows;
        }
    }

    private static final class Emi {
        static List<Row> crusher() throws ReflectiveOperationException {
            // EMI publishes its manager before the sorted bake finishes; wait for the whole reload.
            if (!(boolean) Class.forName("dev.emi.emi.runtime.EmiReloadManager").getMethod("isLoaded").invoke(null)) return null;
            if (Class.forName("dev.emi.emi.registry.EmiRecipes").getField("activeWorker").get(null) != null) return null;
//? if >=1.21.1 {
/*            var category = ResourceLocation.fromNamespaceAndPath("gt6ce", MachineRecipeMaps.Crusher.mNameInternal.toLowerCase(Locale.ROOT));
*///?} else {
            var category = new ResourceLocation("gt6ce", MachineRecipeMaps.Crusher.mNameInternal.toLowerCase(Locale.ROOT));
//?}
            var rows = new ArrayList<Row>();
            for (var recipe : EmiApi.getRecipeManager().getRecipes())
                if (recipe.getCategory().getId().equals(category))
                    rows.add(new Row(stacks(recipe.getInputs()), stacks(recipe.getOutputs())));
            return rows.isEmpty() ? null : rows;
        }

        private static List<ItemStack> stacks(List<? extends EmiIngredient> ingredients) {
            var list = new ArrayList<ItemStack>();
            for (var ingredient : ingredients)
                for (var stack : ingredient.getEmiStacks()) {
                    var item = stack.getItemStack();
                    if (!item.isEmpty()) list.add(item);
                }
            return list;
        }
    }

    private static String victim() {
        var map = MachineRecipeMaps.Crusher;
        for (var recipe : map.mRecipeList) {
            ItemStack found = null;
            boolean single = true;
            for (var stack : recipe.mInputs) {
                if (stack == null || stack.isEmpty()) continue;
                if (found != null) { single = false; break; }
                found = stack;
            }
            if (!single || found == null || found.getCount() != 1) continue;
            if (found.is(Items.HEART_OF_THE_SEA) || found.is(Items.SOUL_SAND) || found.is(Items.SOUL_SOIL)) continue;
            Item item = found.getItem();
            int count = 0;
            for (var other : map.mRecipeList)
                for (var stack : other.mInputs)
                    if (stack != null && stack.is(item)) { count++; break; }
            if (count == 1) return BuiltInRegistries.ITEM.getKey(item).toString();
        }
        throw new IllegalStateException("crusher has no unique one-count input");
    }

    private static void writePack(net.minecraft.server.MinecraftServer server) throws Exception {
        Path root = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("gt-machine-overlay-probe");
//? if >=1.21.1 {
/*        Path addition = root.resolve("data/gt6ce/recipe/machine/heart_of_the_sea.json");
        Path tagged = root.resolve("data/gt6ce/recipe/machine/soul_fire.json");
        Path removal = root.resolve("data/gt6ce/recipe/machine/remove_unique.json");
*///?} else {
        Path addition = root.resolve("data/gt6ce/recipes/machine/heart_of_the_sea.json");
        Path tagged = root.resolve("data/gt6ce/recipes/machine/soul_fire.json");
        Path removal = root.resolve("data/gt6ce/recipes/machine/remove_unique.json");
//?}
        Files.createDirectories(addition.getParent());
        Files.writeString(root.resolve("pack.mcmeta"),
//? if >=1.21.1 {
/*                "{\"pack\":{\"pack_format\":48,\"description\":\"GT machine overlay viewer\"}}");
*///?} else {
                "{\"pack\":{\"pack_format\":15,\"description\":\"GT machine overlay viewer\"}}");
//?}
        String map = MachineRecipeMaps.Crusher.mNameInternal;
        Files.writeString(addition, addition(map, "minecraft:heart_of_the_sea", "minecraft:nether_star"));
        Files.writeString(tagged, tagAddition(map, "minecraft:soul_fire_base_blocks", "minecraft:diamond"));
        Files.writeString(removal, "{\"type\":\"gt6ce:machine_removal\",\"map\":\"" + map
                + "\",\"item_inputs\":[{\"item\":\"" + victimId + "\",\"count\":1}]}");
        server.getPackRepository().reload();
    }

    private static java.util.List<String> selected(net.minecraft.server.MinecraftServer server, boolean probe) {
        var ids = new ArrayList<>(server.getPackRepository().getSelectedIds());
        if (probe && !ids.contains("file/gt-machine-overlay-probe")) ids.add("file/gt-machine-overlay-probe");
        return ids;
    }

    private static String addition(String map, String item, String output) {
        return "{\"type\":\"gt6ce:machine\",\"map\":\"" + map + "\",\"item_inputs\":[{\"item\":\"" + item
                + "\"}],\"item_outputs\":[{\"item\":\"" + output + "\"}],\"eu_per_tick\":0,\"duration\":16}";
    }

    private static String tagAddition(String map, String tag, String output) {
        return "{\"type\":\"gt6ce:machine\",\"map\":\"" + map + "\",\"item_inputs\":[{\"tag\":\"" + tag
                + "\"}],\"item_outputs\":[{\"item\":\"" + output + "\"}],\"eu_per_tick\":0,\"duration\":4}";
    }

    private static void click(net.minecraft.client.gui.screens.Screen screen, String... keys) {
        for (var child : screen.children()) if (child instanceof Button button && button.active && button.visible
                && button.getMessage().getContents() instanceof TranslatableContents text) {
            for (var key : keys) if (text.getKey().equals(key)) { button.onPress(); return; }
        }
        throw new IllegalStateException("missing button on " + screen.getClass().getName());
    }

    private static void fail(Throwable failure) {
        if (!TERMINAL.compareAndSet(false, true)) return;
        LOGGER.error("MACHINE_OVERLAY_VIEWER_FAILED", failure);
        if (WATCHDOG != null) WATCHDOG.shutdownNow();
        var minecraft = Minecraft.getInstance();
        if (minecraft != null) minecraft.execute(minecraft::stop);
    }
}
