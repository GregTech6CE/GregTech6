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
    private static final boolean VIEWER_ONLY=Boolean.getBoolean("gregtech.integration.viewerFeedbackOnly");
    private static final boolean PRESENTATION_ONLY=Boolean.getBoolean("gregtech.integration.recipePresentationRuntimeOnly");
    private static final boolean MATERIAL_BUSH_ONLY=Boolean.getBoolean("gregtech.integration.materialBushRuntimeOnly");
    private static final boolean LONG_ONLY=Boolean.getBoolean("gregtech.integration.longDistanceRuntimeOnly");
    private static final boolean COKE_ONLY=Boolean.getBoolean("gregtech.integration.cokeRuntimeSmoke");
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
            event.getGuiGraphics().flush();
            if(PRESENTATION_ONLY)RecipePresentationRuntimeSmoke.screen(minecraft,event.getScreen());
            else if(COKE_ONLY){CokeOvenRuntimeSmoke.screen(minecraft,event.getScreen());ColoredBooksRuntimeSmoke.screen(minecraft,event.getScreen());}
            else if(!LONG_ONLY&&!MATERIAL_BUSH_ONLY&&!PRESENTATION_ONLY) LootBrowserSmoke.frame(event.getScreen());
            if (stage == 0 && event.getScreen() instanceof TitleScreen) {
                minecraft.options.pauseOnLostFocus = false;
                var root = minecraft.gameDirectory.toPath().toAbsolutePath().normalize();
                if (!root.endsWith("world-creation-smoke-run") || Files.exists(root.resolve("saves").resolve(WORLD)))
                    throw new IllegalStateException("Preflight needs an isolated fresh save");
                OriginFeedbackChecks.registry();
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
                    if(!VIEWER_ONLY&&!COKE_ONLY&&!LONG_ONLY&&!MATERIAL_BUSH_ONLY&&!PRESENTATION_ONLY) {
                    checkBatteries(server,result);
                    OriginFeedbackChecks.server(server,result);
                    LootFeedbackChecks.server(server,result);
                    MachineFeedbackChecks.server(server,result);
                    SurfaceFeedbackChecks.server(server,result);
                    SandwichFeedbackChecks.server(server,result);
                    CannedFoodFeedbackChecks.server(server,result);
                    }
                    if(MATERIAL_BUSH_ONLY)MaterialBushRuntimeSmoke.server(server,result);
                    result.addProperty("materialBushRuntimeOnly",MATERIAL_BUSH_ONLY);
                    result.addProperty("recipePresentationRuntimeOnly",PRESENTATION_ONLY);
                    if(LONG_ONLY) LongDistanceRuntimeSmoke.server(server,result);
                    result.addProperty("longDistanceRuntimeOnly",LONG_ONLY);
                    result.addProperty("viewerFeedbackOnly",VIEWER_ONLY);
                    result.addProperty("cokeRuntimeOnly",COKE_ONLY);
                    result.addProperty("canonicalItemsChecked",checked);
                    result.addProperty("recipes",recipes);
                    result.addProperty("serverTicks",server.getTickCount());
                    result.addProperty("dimension",minecraft.level.dimension().location().toString());
                    return result;
                });
            }
            if (++frames < 30 || !probe.isDone() || !emiReady()) return;
            var result = probe.join();
            if(PRESENTATION_ONLY){if(!RecipePresentationRuntimeSmoke.frame(minecraft,result))return;}
            else if(COKE_ONLY) {if(!CokeOvenRuntimeSmoke.frame(minecraft,result))return;}
            else if (!LONG_ONLY&&!MATERIAL_BUSH_ONLY&&!PRESENTATION_ONLY && !LootBrowserSmoke.start(result)) return;
            if(MATERIAL_BUSH_ONLY)MaterialBushRuntimeSmoke.client(minecraft,result);
            if(LONG_ONLY) LongDistanceRuntimeSmoke.client(minecraft,result);
            if(!VIEWER_ONLY&&!COKE_ONLY&&!LONG_ONLY&&!MATERIAL_BUSH_ONLY&&!PRESENTATION_ONLY) {
            MachineFeedbackChecks.client(minecraft,result);
            SurfaceFeedbackChecks.client(minecraft,result);
            SandwichFeedbackChecks.client(minecraft,result);
            CannedFoodFeedbackChecks.client(minecraft,result);
            OriginFeedbackChecks.client(minecraft,result);
            }
            if(!COKE_ONLY&&!LONG_ONLY&&!MATERIAL_BUSH_ONLY&&!PRESENTATION_ONLY&&!ViewerGlassChecks.capture(minecraft,result))return;
            result.addProperty("renderedWorldFrames",frames);
            result.addProperty("emiLoaded",EMI_PRESENT);
            if (!Files.isRegularFile(minecraft.gameDirectory.toPath().resolve("saves").resolve(WORLD).resolve("level.dat")))
                throw new IllegalStateException("Fresh world has no level.dat");
            stage = 4;
            String file = "world-creation-neoforge-" + ID + ".png";
            if(!VIEWER_ONLY&&!COKE_ONLY&&!LONG_ONLY&&!MATERIAL_BUSH_ONLY&&!PRESENTATION_ONLY) {
            OriginFeedbackChecks.renderInventory(event.getGuiGraphics(),minecraft,result);
            SurfaceFeedbackChecks.render(event.getGuiGraphics(),minecraft,result);
            SandwichFeedbackChecks.render(event.getGuiGraphics(),minecraft,result);
            }
            if(COKE_ONLY)ColoredBooksRuntimeSmoke.renderInventory(event.getGuiGraphics(),minecraft);
            if(MATERIAL_BUSH_ONLY)MaterialBushRuntimeSmoke.render(event.getGuiGraphics(),minecraft);
            if(LONG_ONLY)LongDistanceRuntimeSmoke.render(event.getGuiGraphics(),minecraft);
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
    private static final boolean EMI_PRESENT = net.neoforged.fml.ModList.get().isLoaded("emi");
    private static boolean emiReady() throws ReflectiveOperationException {
        if (!EMI_PRESENT) return true;
        var manager=Class.forName("dev.emi.emi.runtime.EmiReloadManager");
        if (((Number)manager.getMethod("getStatus").invoke(null)).intValue()==-1)
            throw new IllegalStateException("EMI reload failed before preflight completion");
        // isLoaded also requires its reload worker to have stopped; do not unload its world early.
        return (boolean)manager.getMethod("isLoaded").invoke(null);
    }
    /** Checks actual loader aliases and stack decoding; it never reads a user save. */
    private static void checkBatteries(net.minecraft.server.MinecraftServer server,JsonObject receipt) {
        var registry=net.minecraft.core.registries.BuiltInRegistries.ITEM;
        int decoded=0,assemblies=0;
        for(var alias:com.gregtech.gregtech.content.energy.BatteryItemMigration.ALIASES) {
            var oldId=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",alias.oldId());
            var targetId=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",alias.target().id());
            var item=registry.get(oldId);
            if(!(item instanceof com.gregtech.gregtech.item.ChemicalBatteryItem battery)
                    ||!registry.getKey(item).equals(targetId)||registry.keySet().contains(oldId))
                throw new IllegalStateException("Placeholder is still registered or alias missing: "+oldId);
            if(battery.spec().capacity()<alias.oldCapacity())throw new IllegalStateException("Migration truncates legal old charge: "+oldId);
            for(long charge:new long[]{0,alias.oldCapacity()}) {
                var stack=new ItemStack(item,1);
                battery.setCharge(stack,charge);
                stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("migration checkpoint"));
                com.gregtech.gregtech.platform.neoforge.StackCustomData.update(stack,tag->tag.putString("migration_probe","keep me"));
                var encoded=(net.minecraft.nbt.CompoundTag)stack.saveOptional(server.registryAccess());
                encoded.putString("id",oldId.toString());
                // Older port items did not write the new chemical max-stack component.
                encoded.getCompound("components").remove("minecraft:max_stack_size");
                var restored=ItemStack.parseOptional(server.registryAccess(),encoded);
                if(!restored.is(item)||restored.getCount()!=1||battery.stored(restored)!=charge
                        ||!restored.getHoverName().equals(stack.getHoverName())
                        ||!com.gregtech.gregtech.platform.neoforge.StackCustomData.read(restored).getString("migration_probe").equals("keep me")
                        ||restored.getMaxStackSize()!=(charge>0?1:16))
                    throw new IllegalStateException("Old battery lost identity/charge/data/stack rules: "+oldId);
                if(!((net.minecraft.nbt.CompoundTag)restored.saveOptional(server.registryAccess())).getString("id").equals(targetId.toString()))
                    throw new IllegalStateException("Migrated battery did not save its canonical ID: "+oldId);
                decoded++;
            }
        }
        for(var spec:com.gregtech.gregtech.content.energy.ChemicalBatterySpec.all()) {
            var item=com.gregtech.gregtech.registry.GTChemicalBatteries.item(spec.chemistry(),spec.tier());
            var tag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","rechargeable_batteries/"+new String[]{"ulv","lv","mv","hv","ev"}[spec.tier()]));
            if(!new ItemStack(item).is(tag))throw new IllegalStateException("Source chemical battery missing from exact-tier group: "+spec.id());

            for(var tool:com.gregtech.gregtech.content.tool.ElectricToolAssembly.values()) {
                if(tool.definition().tier()!=spec.tier())continue;
                var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","electric_tools/"+tool.id+"/steel/"+spec.id());
                var holder=server.getRecipeManager().byKey(id).orElseThrow(()->new IllegalStateException("Missing actual chemical assembly "+id));
                var recipe=holder.value();
                var output=recipe.getResultItem(server.registryAccess());
                if(!(output.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem electric)
                        ||electric.getEnergyCapacity(output,com.gregtech.gregtech.data.GregTechTags.Energy.EU)!=spec.capacity())
                    throw new IllegalStateException("Assembly lost source chemical capacity: "+id);
                var obsolete=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","electric_tools/"+tool.id+"/steel");
                if(server.getRecipeManager().byKey(obsolete).isPresent())throw new IllegalStateException("Placeholder assembly survived: "+obsolete);
                assemblies++;
            }
        }
        receipt.addProperty("legacyBatteryAliasesChecked",com.gregtech.gregtech.content.energy.BatteryItemMigration.ALIASES.size());
        receipt.addProperty("chemicalBatteryStackRoundTrips",decoded);
        receipt.addProperty("sourceChemicalBatteriesChecked",com.gregtech.gregtech.content.energy.ChemicalBatterySpec.all().size());
        receipt.addProperty("sourcePoweredAssemblyRowsChecked",assemblies);
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
        var minecraft=Minecraft.getInstance();
        minecraft.execute(minecraft::stop);
        if(ENABLED&&minecraft.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("world-creation-smoke-run")) {
            var fallback=new Thread(() -> {
                try { Thread.sleep(15000); } catch(InterruptedException interrupted) { return; }
                LOGGER.error("WORLD_CREATION_SMOKE_FAILED_EXIT: normal shutdown was blocked after a failed isolated probe");
                Runtime.getRuntime().halt(1);
            },"failed-isolated-preflight-exit");
            fallback.setDaemon(true);fallback.start();
        }
    }
}
