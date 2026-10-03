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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Opt-in development smoke only: an actual rendered title screen and saved screenshot.
 * This bootstrap source set is excluded from the production mod jar.
 */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.GAME)
public final class NeoForgeClientSmoke {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PLATFORM = "neoforge";
    private static final String MINECRAFT_VERSION = "1.21.1";
    private static final boolean WORLD_ENABLED = Boolean.getBoolean("gregtech.integration.clientWorldSmoke");
    private static final boolean ENABLED = WORLD_ENABLED || Boolean.getBoolean("gregtech.integration.clientSmoke");
    private static final String WORLD_PHASE = System.getProperty("gregtech.integration.clientWorldPhase", "");
    private static final String WORLD_NAME = System.getProperty("gregtech.integration.clientWorldName", "");
    private static final String WORLD_ID = System.getProperty("gregtech.integration.clientWorldId", "");
    private static final String ORIGINAL_SPECIMEN = "cb23b5a6-244c-4102-bce8-0b817bc7f390";
    private static final net.minecraft.core.BlockPos CRUSHER = new net.minecraft.core.BlockPos(0, 240, 6);
    private static final net.minecraft.core.BlockPos MIXER = new net.minecraft.core.BlockPos(0, 240, 8);
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
    private static boolean modelsChecked;
    private static java.util.List<net.minecraft.world.item.ItemStack> gallery;
    private static volatile int worldStage;
    private static int worldFrames;
    private static long worldStageAt;
    private static java.util.concurrent.CompletableFuture<JsonObject> serverAction;
    private static net.minecraft.client.server.IntegratedServer worldServer;
    private static final java.util.List<JsonObject> worldCaptures = new java.util.ArrayList<>();
    private static final JsonObject worldProof = new JsonObject();

    private NeoForgeClientSmoke() {}

    private static ScheduledExecutorService startWatchdog() {
        if (!ENABLED) return null;
        try {
            String configured = System.getProperty("gregtech.integration.clientSmokeTimeoutSeconds");
            if (configured != null) timeoutSeconds = Integer.parseInt(configured.trim());
            if (timeoutSeconds < 30 || timeoutSeconds > (WORLD_ENABLED ? 600 : 300)) {
                throw new IllegalArgumentException("Invalid client smoke timeout budget");
            }
            if (WORLD_ENABLED && (!WORLD_NAME.matches("client-world-[a-z0-9-]+")
                    || !(WORLD_PHASE.equals("prepare") || WORLD_PHASE.equals("verify") || WORLD_PHASE.equals("emi"))
                    || !UUID.fromString(WORLD_ID).toString().equals(WORLD_ID))) {
                throw new IllegalArgumentException("World smoke requires an isolated world name, phase and UUID");
            }
        } catch (RuntimeException invalidConfiguration) {
            fail("configuration", invalidConfiguration);
            return null;
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "gregtech-neoforge-client-smoke-watchdog");
            thread.setDaemon(true);
            return thread;
        });
        executor.schedule(() -> fail("timeout",
                new TimeoutException("No completed client smoke within "
                        + timeoutSeconds + " seconds")),
                timeoutSeconds, TimeUnit.SECONDS);
        LOGGER.info("{} {}", marker("STARTED"), identity());
        return executor;
    }

    @SubscribeEvent
    public static void afterScreenRender(ScreenEvent.Render.Post event) {
        if (!ENABLED) return;
        Minecraft minecraft = Minecraft.getInstance();
        try {
            snapshotState(minecraft, "render_thread");
            if (TERMINAL.get() || captureRequested) return;
            if (WORLD_ENABLED) {
                afterWorldScreen(minecraft, event);
                return;
            }
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
            if (Boolean.getBoolean("gregtech.integration.clientModelSmoke")) {
                checkAndRenderModels(minecraft, event.getGuiGraphics());
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

    /** One opt-in batch in the existing smoke: actual registry models and actual item renderer. */
    private static void checkAndRenderModels(Minecraft minecraft, net.minecraft.client.gui.GuiGraphics graphics) {
        if (!modelsChecked) {
            var manager = minecraft.getModelManager();
            var missing = manager.getMissingModel();
            int materials = 0;
            int fluids = 0;
            for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                if (!(item instanceof com.gregtech.gregtech.item.MaterialItem)
                        && !(item instanceof com.gregtech.gregtech.platform.neoforge.fluid.FluidDisplayItem)) continue;
                var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
                var model = manager.getModel(net.minecraft.client.resources.model.ModelResourceLocation.inventory(id));
                if (model == null || model == missing) throw new IllegalStateException("Missing final inventory model: " + id);
                if (item instanceof com.gregtech.gregtech.item.MaterialItem) materials++; else fluids++;
            }
            if (materials != com.gregtech.gregtech.registry.GTItems.allEntries().size() || fluids == 0)
                throw new IllegalStateException("Incomplete registry model check");
            gallery = new java.util.ArrayList<>();
            MaterialTooltipSmoke.check();
            var ids = new com.google.gson.JsonArray();
            for (String id : new String[]{"ingot_iron", "plate_copper", "gear_gt_bronze", "coin_gold", "fluid_item_reedwater"}) {
                var key = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech", id);
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key);
                if (item == net.minecraft.world.item.Items.AIR) throw new IllegalStateException("Missing gallery item: " + key);
                gallery.add(new net.minecraft.world.item.ItemStack(item));
                ids.add(key.toString());
            }
            var crusher = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                    .filter(item -> item instanceof net.minecraft.world.item.BlockItem blockItem
                            && blockItem.getBlock() instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock machine
                            && machine.basicSpec().machineName().equals("crusher"))
                    .findFirst().orElseThrow(() -> new IllegalStateException("No registered crusher"));
            gallery.add(new net.minecraft.world.item.ItemStack(crusher));
            ids.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(crusher).toString());
            ToolIconSmoke.check(minecraft, gallery);
            if (net.neoforged.fml.ModList.get().isLoaded("jei")) JeiToolSlotSmoke.check();
            SpringIconSmoke.check(minecraft, gallery);
            LayeredItemSmoke.check(minecraft, gallery);
            var plants = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
            PlantIconSmoke.check(minecraft, plants);
            gallery = new java.util.ArrayList<>();
            MachineModelSmoke.check(minecraft, gallery);
            ids = new com.google.gson.JsonArray();
            for (var stack : gallery) ids.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            if (net.neoforged.fml.ModList.get().isLoaded("emi")) EmiMachineSmoke.check();
            var result = new JsonObject();
            result.addProperty("materialModels", materials);
            result.addProperty("fluidModels", fluids);
            result.addProperty("missingFinalModels", 0);
            result.add("galleryItems", ids);
            LOGGER.info("CLIENT_MODEL_SMOKE_SUCCESS {}", result);
            modelsChecked = true;
        }
        graphics.fill(10, 60, 270, 85 + ((gallery.size() + 5) / 6) * 38, 0xD0000000);
        graphics.drawString(minecraft.font, "GT layered models", 16, 66, 0xFFFFFF);
        for (int i = 0; i < gallery.size(); i++) {
            graphics.pose().pushPose();
            graphics.pose().translate(18 + (i % 6) * 42, 84 + (i / 6) * 38, 0);
            graphics.pose().scale(2, 2, 1);
            graphics.renderItem(gallery.get(i), 0, 0);
            graphics.pose().popPose();
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
        LOGGER.info("{} {}", marker("SUCCESS"), result);
        minecraft.stop();
    }

    private static void fail(String phase, Throwable failure) {
        if (!TERMINAL.compareAndSet(false, true)) return;
        if (WATCHDOG != null) WATCHDOG.shutdownNow();
        JsonObject result = identity();
        result.addProperty(WORLD_ENABLED ? "reason" : "phase", phase);
        result.addProperty("error", failure.toString());
        addSnapshot(result, lastSnapshot);
        // Log first: a stuck client thread must never suppress the failure receipt.
        LOGGER.error("{} {}", marker("FAILED"), result, failure);
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                // If the main thread can still service tasks, record its current state
                // before stopping. This supplemental marker is not a second FAILED.
                minecraft.execute(() -> {
                    try {
                        snapshotState(minecraft, "main_thread_at_failure");
                        JsonObject current = identity();
                        current.addProperty(WORLD_ENABLED ? "reason" : "phase", phase);
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
        if (WORLD_ENABLED) {
            result.addProperty("phase", WORLD_PHASE);
            result.addProperty("worldName", WORLD_NAME);
            result.addProperty("specimenId", WORLD_ID);
            result.addProperty("pid", ProcessHandle.current().pid());
            result.addProperty("stage", worldStage);
        }
        return result;
    }

    private static String marker(String phase) {
        return (WORLD_ENABLED ? "CLIENT_WORLD_SMOKE_" : "CLIENT_SMOKE_") + phase;
    }

    private static void stage(int next) {
        worldStage = next;
        worldFrames = 0;
        worldStageAt = System.nanoTime();
    }

    private static boolean settle(int frames) {
        return ++worldFrames >= frames && System.nanoTime() - worldStageAt > TimeUnit.MILLISECONDS.toNanos(500);
    }

    private static net.minecraft.server.level.ServerPlayer serverPlayer(Minecraft minecraft) {
        var player = worldServer.getPlayerList().getPlayer(minecraft.player.getUUID());
        if (player == null) throw new IllegalStateException("No actual integrated-server player");
        return player;
    }

    private static com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity machine(
            net.minecraft.core.BlockPos pos) {
        if (worldServer.overworld().getBlockEntity(pos) instanceof
                com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity machine) return machine;
        throw new IllegalStateException("No actual saved machine at " + pos);
    }

    private static void requireStock(net.minecraft.world.item.ItemStack stack) {
        var name = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (stack.isEmpty() || stack.getCount() != 1 || name == null
                || !name.getString().equals("client-gui-stock/" + WORLD_ID)
                || !(stack.getItem() instanceof com.gregtech.gregtech.item.MaterialItem material)
                || material.getPrefix() != com.gregtech.gregtech.data.MaterialPrefix.gemChipped
                || material.getMaterial().resolve() != com.gregtech.gregtech.content.material.Materials.Diamond.resolve())
            throw new IllegalStateException("Named chipped diamond stock did not survive actual menu/save path: " + stack
                    + ", name=" + (name == null ? "none" : name.getString()));
    }

    private static void requireWater(net.neoforged.neoforge.fluids.FluidStack fluid, int amount) {
        var name = fluid.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (fluid.getFluid() != net.minecraft.world.level.material.Fluids.WATER || fluid.getAmount() != amount
                || name == null || !name.getString().equals("machine-water/" + ORIGINAL_SPECIMEN))
            throw new IllegalStateException("Actual named mixer water differs: " + fluid);
    }

    private static JsonObject prepareClientWorld(Minecraft minecraft) {
        var level = worldServer.overworld();
        if (!(level.getBlockEntity(new net.minecraft.core.BlockPos(0,240,0)) instanceof
                net.minecraft.world.level.block.entity.ChestBlockEntity chest)
                || chest.getCustomName() == null || !chest.getCustomName().getString()
                        .equals("gregtech-dedicated-smoke/" + ORIGINAL_SPECIMEN))
            throw new IllegalStateException("Copied original machine-world marker is absent");
        var crusher = machine(CRUSHER);
        var mixer = machine(MIXER);
        if (!crusher.spec().id().equals("crusher_bronze") || !mixer.spec().id().equals("mixer_bronze")
                || crusher.machineControl(null).enabled() || mixer.machineControl(null).enabled())
            throw new IllegalStateException("Copied stopped machine identities changed");
        var player = serverPlayer(minecraft);
        if (WORLD_PHASE.equals("prepare")) {
            if (!crusher.inventory().getStackInSlot(0).isEmpty())
                throw new IllegalStateException("Refusing to overwrite existing crusher input");
            requireWater(mixer.getFluidInTank(0), 1000);
            // Supplied specimen/footing in the copied world; never claim survival acquisition.
            for (int x=-4; x<=4; x++) for (int z=-3; z<=13; z++) {
                var pos = new net.minecraft.core.BlockPos(x,239,z);
                if (level.getBlockState(pos).isAir())
                    level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            }
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getInventory().clearContent();
            var stock = com.gregtech.gregtech.registry.GTItems.getStack(com.gregtech.gregtech.data.MaterialPrefix.gemChipped,
                    com.gregtech.gregtech.content.material.Materials.Diamond);
            stock.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("client-gui-stock/" + WORLD_ID));
            player.getInventory().setItem(9, stock);
            player.inventoryMenu.broadcastChanges();
        } else {
            requireStock(crusher.inventory().getStackInSlot(0));
            requireWater(mixer.getFluidInTank(0),1500);
            if (!player.getInventory().getItem(9).isEmpty())
                throw new IllegalStateException("Saved player still owns the transferred stock");
        }
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,worldServer);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,worldServer);
        level.setDayTime(6000);
        player.connection.teleport(3.5,240,10.5,135,18);
        var result=identity();
        result.addProperty("serverClass",worldServer.getClass().getName());
        result.addProperty("originalSpecimenId",ORIGINAL_SPECIMEN);
        result.addProperty("copiedStoppedMachinesLoaded",true);
        result.addProperty("restoredTransferredStock",WORLD_PHASE.equals("verify"));
        result.addProperty("restoredNamedWaterAmount",mixer.getFluidInTank(0).getAmount());
        return result;
    }

    private static void afterWorldScreen(Minecraft minecraft, ScreenEvent.Render.Post event) throws Exception {
        if (WORLD_PHASE.equals("emi") && worldStage==20
                && event.getScreen().getClass().getName().equals("dev.emi.emi.screen.RecipeScreen")) {
            if (settle(30)) captureWorld(minecraft,event.getGuiGraphics(),"extruder-plate",22);
            return;
        }
        if (worldStage==0 && event.getScreen() instanceof TitleScreen && minecraft.getOverlay()==null) {
            Path dir=minecraft.gameDirectory.toPath().toAbsolutePath().normalize();
            if (!dir.endsWith("client-world-smoke-run") || !Files.isRegularFile(dir.resolve("saves").resolve(WORLD_NAME).resolve("level.dat")))
                throw new IllegalStateException("World smoke refuses a non-isolated/missing copied world");
            stage(1);
            minecraft.execute(()-> {
                try {
                    // Acknowledge only the experimental metadata warning of the supplied disposable copy.
                    // Recovery, account, resource-pack and low-disk dialogs are not accepted by this probe.
                    try(var access=minecraft.getLevelSource().createAccess(WORLD_NAME)) {
                        var path=access.getLevelPath(net.minecraft.world.level.storage.LevelResource.LEVEL_DATA_FILE);
                        var data=net.minecraft.nbt.NbtIo.readCompressed(path,net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                        data.getCompound("Data").putBoolean("confirmedExperimentalSettings",true);
                        net.minecraft.nbt.NbtIo.writeCompressed(data,path);
                    }
                    minecraft.options.renderDistance().set(4);
                    minecraft.options.simulationDistance().set(5);
                    minecraft.createWorldOpenFlows().openWorld(WORLD_NAME,
                            ()->fail("world_open_cancelled",new IllegalStateException("Copied world open cancelled")));
                } catch(Throwable failure) { fail("world_open",failure); }
            });
            return;
        }
        if (!(event.getScreen() instanceof com.gregtech.gregtech.client.gui.BasicMachineScreen screen)) return;
        var menu=screen.getMenu();
        if (worldStage==5 && menu.machineName().equals("crusher")) {
            if (WORLD_PHASE.equals("prepare")) {
                int playerBase=menu.slots.size()-36;
                requireStock(menu.getSlot(playerBase).getItem());
                minecraft.gameMode.handleInventoryMouseClick(menu.containerId,playerBase,0,
                        net.minecraft.world.inventory.ClickType.QUICK_MOVE,minecraft.player);
                stage(6);
            } else {
                requireStock(menu.getSlot(0).getItem());
                stage(7);
            }
        }
        if (worldStage==6 && menu.machineName().equals("crusher")) {
            if (System.nanoTime()-worldStageAt > TimeUnit.SECONDS.toNanos(10))
                throw new IllegalStateException("Shift-click did not settle on both actual inventories in 10 seconds");
            if (menu.getSlot(0).getItem().isEmpty()) return;
            requireStock(menu.getSlot(0).getItem());
            if (!menu.getSlot(menu.slots.size()-36).getItem().isEmpty()) return;
            if (serverAction==null) serverAction=worldServer.submit(()-> {
                var stock=machine(CRUSHER).inventory().getStackInSlot(0);
                if(!stock.isEmpty())requireStock(stock);
                var result=identity();result.addProperty("stockReady",!stock.isEmpty());return result;
            });
            if (serverAction.isDone()) {
                boolean ready=serverAction.get().get("stockReady").getAsBoolean();serverAction=null;
                if(ready)stage(7);
            }
        }
        if (worldStage==7 && menu.machineName().equals("crusher") && settle(8)) {
            requireStock(menu.getSlot(0).getItem());
            worldProof.addProperty("actualRightClickCrusherMenu",true);
            worldProof.addProperty("namedItemClientServerTransferOrReload",true);
            captureWorld(minecraft,event.getGuiGraphics(),"crusher-gui",8);
        }
        if (worldStage==10 && menu.machineName().equals("mixer")) {
            var stock=menu.getSlot(0).getItem();
            var name=stock.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
            if (!stock.is(net.minecraft.world.item.Items.COPPER_INGOT) || stock.getCount()!=3 || name==null
                    || !name.getString().equals("machine-stock/"+ORIGINAL_SPECIMEN)) return;
            var fluid=menu.slots.stream().filter(slot -> slot instanceof com.gregtech.gregtech.client.gui.SlotFluid sf
                    && sf.isInput() && sf.tankIndex()==0).map(slot -> ((com.gregtech.gregtech.client.gui.SlotFluid)slot).fluid())
                    .findFirst().orElseThrow();
            if (fluid.isEmpty()) return;
            requireWater(fluid,WORLD_PHASE.equals("prepare")?1000:1500);
            if (WORLD_PHASE.equals("prepare")) {
                serverAction=worldServer.submit(()-> {
                    var mixer=machine(MIXER);var added=mixer.getFluidInTank(0).copy();added.setAmount(500);
                    if (mixer.getTanksInput()[0].fill(added,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)!=500)
                        throw new IllegalStateException("Real mixer tank did not accept supplied matching 500mB");
                    mixer.setChanged();return identity();
                });
                stage(11);
            } else stage(12);
        }
        if (worldStage==11 && menu.machineName().equals("mixer") && serverAction.isDone()) {
            serverAction.get();
            var fluid=menu.slots.stream().filter(slot -> slot instanceof com.gregtech.gregtech.client.gui.SlotFluid sf
                    && sf.isInput() && sf.tankIndex()==0).map(slot -> ((com.gregtech.gregtech.client.gui.SlotFluid)slot).fluid())
                    .findFirst().orElseThrow();
            if(fluid.getAmount()!=1500)return;
            requireWater(fluid,1500);serverAction=null;stage(12);
        }
        if (worldStage==12 && menu.machineName().equals("mixer") && settle(8)) {
            worldProof.addProperty("actualRightClickMixerMenu",true);
            worldProof.addProperty("namedFluidClientAmount",1500);
            worldProof.addProperty("liveFluidPacketUpdate",WORLD_PHASE.equals("prepare"));
            captureWorld(minecraft,event.getGuiGraphics(),"mixer-gui",13);
        }
    }

    @SubscribeEvent
    public static void worldTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (!WORLD_ENABLED || TERMINAL.get() || captureRequested) return;
        var minecraft=Minecraft.getInstance();
        try {
            snapshotState(minecraft,"world_client_tick");
            if (WORLD_PHASE.equals("emi") && worldStage!=14) {
                if (worldStage==1 && minecraft.level!=null && minecraft.player!=null && minecraft.getSingleplayerServer()!=null) {
                    worldServer=minecraft.getSingleplayerServer();
                    ToolIconSmoke.check(minecraft,new java.util.ArrayList<>());
                    SpringIconSmoke.check(minecraft,new java.util.ArrayList<>());
                    serverAction=worldServer.submit(()-> { SpringIconSmoke.prepareWorld(worldServer.overworld(),serverPlayer(minecraft));return identity(); });
                    stage(17);
                } else if (worldStage==17 && serverAction.isDone()) {
                    serverAction.get();serverAction=null;stage(18);
                } else if (worldStage==18) {
                    var recipe=EmiMachineSmoke.installedRecipe();
                    if (recipe!=null && SpringIconSmoke.checkWorld(minecraft)) {
                        worldProof.addProperty("jeiAbsent",true);
                        worldProof.addProperty("nativeEmiOutputLookup",true);
                        minecraft.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(minecraft.player));
                        dev.emi.emi.api.EmiApi.displayRecipe(recipe);
                        stage(20);
                    }
                } else if (worldStage==22 && minecraft.screen!=null) {
                    minecraft.setScreen(null);
                } else if (worldStage==21) {
                    stage(14);
                    minecraft.level.disconnect();
                    minecraft.disconnect(new TitleScreen());
                }
                return;
            }
            if (worldStage==1 && minecraft.level!=null && minecraft.player!=null && minecraft.getSingleplayerServer()!=null) {
                worldServer=minecraft.getSingleplayerServer();
                serverAction=worldServer.submit(()->prepareClientWorld(minecraft));stage(2);
            } else if (worldStage==2 && serverAction.isDone()) {
                worldProof.add("loadedWorld",serverAction.get());serverAction=null;stage(3);
            } else if (worldStage==4 || worldStage==8) {
                if(worldStage==8)minecraft.player.closeContainer();
                int next=worldStage==4?5:9;
                int z=worldStage==4?6:8;
                serverAction=worldServer.submit(()-> { serverPlayer(minecraft).connection.teleport(2.5,240,z+0.5,90,15);return identity(); });
                stage(next);
            } else if ((worldStage==5 || worldStage==9) && serverAction!=null && serverAction.isDone()
                    && minecraft.screen==null && Math.abs(minecraft.player.getX()-2.5)<0.2) {
                serverAction.get();serverAction=null;
                var pos=worldStage==5?CRUSHER:MIXER;
                minecraft.gameMode.useItemOn(minecraft.player,net.minecraft.world.InteractionHand.MAIN_HAND,
                        new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(1,240.5,pos.getZ()+0.5),
                                net.minecraft.core.Direction.EAST,pos,false));
                if(worldStage==9)stage(10);
            } else if (worldStage==13) {
                minecraft.player.closeContainer();
                if(serverAction==null)serverAction=worldServer.submit(()-> {
                    requireStock(machine(CRUSHER).inventory().getStackInSlot(0));
                    requireWater(machine(MIXER).getFluidInTank(0),1500);return identity();
                });
                if(serverAction.isDone()) {
                    serverAction.get();serverAction=null;stage(14);
                    // Match vanilla PauseScreen: close the level connection before waiting for server shutdown.
                    minecraft.level.disconnect();
                    minecraft.disconnect(new TitleScreen());
                }
            } else if (worldStage==14 && minecraft.level==null && minecraft.player==null
                    && minecraft.getSingleplayerServer()==null && worldServer.isShutdown()) {
                var result=identity();result.addProperty("normalIntegratedServerStop",true);
                result.addProperty("savedWorldExists",Files.isRegularFile(minecraft.gameDirectory.toPath().resolve("saves").resolve(WORLD_NAME).resolve("level.dat")));
                result.add("proof",worldProof);var captures=new com.google.gson.JsonArray();worldCaptures.forEach(captures::add);result.add("captures",captures);
                finishSuccess(minecraft,result);
            }
        } catch(Throwable failure) { fail("world_stage_"+worldStage,failure); }
    }

    @SubscribeEvent
    public static void afterWorldGui(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
        if (!WORLD_ENABLED || TERMINAL.get() || captureRequested) return;
        var minecraft=Minecraft.getInstance();
        try {
            if (WORLD_PHASE.equals("emi") && worldStage==22 && minecraft.screen==null) {
                if (SpringIconSmoke.checkWorld(minecraft) && settle(30)) {
                    worldProof.addProperty("synchronizedWorldSpringModels",true);
                    checkAndRenderModels(minecraft,event.getGuiGraphics());
                    captureWorld(minecraft,event.getGuiGraphics(),"tools-and-springs",21);
                }
                return;
            }
            if(worldStage!=3)return;
            if (minecraft.screen!=null || minecraft.level==null || minecraft.player==null
                    || minecraft.player.getY()<239 || !minecraft.level.getBlockState(CRUSHER).is(
                            net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","crusher_bronze"))))return;
            if(settle(30))captureWorld(minecraft,event.getGuiGraphics(),"world",4);
        } catch(Throwable failure) { fail("world_render",failure); }
    }

    private static void captureWorld(Minecraft minecraft,net.minecraft.client.gui.GuiGraphics graphics,String role,int next) {
        captureRequested=true;graphics.flush();
        String name="gregtech-client-world-neoforge-1.21.1-"+WORLD_PHASE+"-"+role+"-"+UUID.randomUUID()+".png";
        Path path=minecraft.gameDirectory.toPath().resolve(Screenshot.SCREENSHOT_DIR).resolve(name).toAbsolutePath().normalize();
        Screenshot.grab(minecraft.gameDirectory,name,minecraft.getMainRenderTarget(),message-> {
            try {
                if(message!=null && message.getContents() instanceof TranslatableContents text && text.getKey().equals("screenshot.failure"))
                    throw new IOException("World screenshot write failed: "+message.getString());
                var image=ImageIO.read(path.toFile());
                if(image==null)throw new IOException("World screenshot did not decode");
                var receipt=identity();receipt.addProperty("role",role);receipt.addProperty("screenshot",path.toString());
                receipt.addProperty("width",image.getWidth());receipt.addProperty("height",image.getHeight());image.flush();
                minecraft.execute(()-> { worldCaptures.add(receipt);LOGGER.info("CLIENT_WORLD_CAPTURE_SUCCESS {}",receipt);captureRequested=false;stage(next); });
            } catch(Throwable failure) { fail("world_screenshot_"+role,failure); }
        });
    }
}
