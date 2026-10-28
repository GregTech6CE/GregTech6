package gregtech;

import com.mojang.logging.LogUtils;
import gregtech.bootstrap.InitializationStep;
import gregtech.bootstrap.content.*;
import gregtech.bootstrap.registration.*;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.network.GTPackets;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

@Mod(GregTech.MODID)
public class GregTech {
    public static final String MODID = gregtech.engine.mod.GregTechIdentity.MOD_ID;
    public static final String NAMESPACE = gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;
    public static final Logger LOGGER = LogUtils.getLogger();

    public GregTech(FMLJavaModLoadingContext context) {
        gregtech.engine.addon.AddonCoreServices.bind();
        gregtech.engine.addon.AddonPlatformServices.bind();
        gregtech.engine.addon.AddonLoaderServices.bind();
        gregtech.api.mod.ModData.bindPresence(
                id -> net.minecraftforge.fml.ModList.get().isLoaded(NAMESPACE.equals(id) ? MODID : id));
        GTMaterialRegistry.setLogSink((warning, message) -> {
            if (warning) LOGGER.warn(message);
            else LOGGER.info(message);
        });
        IEventBus modEventBus = context.getModEventBus();

        GTPackets.register();
        gregtech.content.registry.GTProjectiles.register(modEventBus);
        gregtech.content.worldgen.GTFeatures.register(modEventBus);

        // Base data must precede material items and dependent blocks.
        initialize(
                "content",
                new DataBootstrap(),
                new MaterialBootstrap(),
                new ItemBootstrap(),
                new BlockBootstrap(),
                new ToolBootstrap(modEventBus),
                new FluidBootstrap(modEventBus));

        // Queue machines and creative tabs before submitting deferred registries.
        initialize(
                "machine registration",
                new MachineBlockBootstrap(modEventBus),
                new CreativeTabBootstrap(modEventBus),
                new DeferredRegistrySubmission(modEventBus));

        modEventBus.addListener(this::commonSetup);

        GregTechConfig.prepareDirectory(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
        context.registerConfig(ModConfig.Type.COMMON, GregTechConfig.SPEC, GregTechConfig.COMMON_FILE);
        context.registerConfig(ModConfig.Type.CLIENT, GregTechConfig.CLIENT_SPEC, GregTechConfig.CLIENT_FILE);
    }

    private static void initialize(String phase, InitializationStep... steps) {
        for (InitializationStep step : steps) {
            try {
                step.run();
            } catch (RuntimeException failure) {
                String name = step.getClass().getSimpleName();
                LOGGER.error("[{}] {} initialization failed at {}", MODID, phase, name, failure);
                throw new IllegalStateException(phase + " initialization failed: " + name, failure);
            }
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            gregtech.content.registry.GTProjectiles.registerDispensers();
            gregtech.content.registry.GTDungeonBlocks.registerPotPlants();
            initialize("material linking", new MaterialLinkBootstrap(), new MaterialItemRegistration());
            gregtech.content.recipe.MachineRecipeBootstrap.load();
            gregtech.api.addon.GregTechAddons.dispatchRecipesReady(
                    new gregtech.api.addon.GregTechAddon.Context("forge", "1.20.1"));
            gregtech.engine.addon.ExclusiveOutputMaps.audit();
            gregtech.content.multiblock.PlainStructureBlocks.resolve();
            LOGGER.info(
                    "GT addon API {} ready: {}",
                    gregtech.api.addon.GregTechAddons.API_VERSION,
                    gregtech.api.addon.GregTechAddons.registeredIds());
            // Rows that RecipeMap.make cancelled down to nothing and dropped; a non-zero value is
            // expected whenever an optimized table contains an input that cancels against its own output.
            var collapsed = gregtech.engine.recipe.RecipeMap.collapsedSamples();
            LOGGER.info(
                    "Recipe tables: {} rows dropped as collapsed by optimization, per map {}, e.g. {}",
                    gregtech.engine.recipe.RecipeMap.collapsedRecipesDropped(),
                    gregtech.engine.recipe.RecipeMap.collapsedByMap(),
                    collapsed.subList(0, Math.min(8, collapsed.size())));

            long itemCount = ForgeRegistries.ITEMS.getKeys().stream()
                    .filter(id -> NAMESPACE.equals(id.getNamespace()))
                    .count();
            long blockCount = ForgeRegistries.BLOCKS.getKeys().stream()
                    .filter(id -> NAMESPACE.equals(id.getNamespace()))
                    .count();
            LOGGER.info(
                    "{} loaded: {} materials, {} items, {} blocks",
                    MODID,
                    GTMaterialRegistry.allMaterials().size(),
                    itemCount,
                    blockCount);
            if (itemCount == 0) {
                LOGGER.error("{} registered ZERO items — check MaterialPrefix bootstrap / DeferredRegister", MODID);
            }
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path.toLowerCase());
    }
}
