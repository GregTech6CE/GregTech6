package com.gregtech.gregtech;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.bootstrap.InitializationStep;
import com.gregtech.gregtech.bootstrap.content.*;
import com.gregtech.gregtech.bootstrap.registration.*;
import com.gregtech.gregtech.network.GTPackets;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;


@Mod(GregTech.MODID)
public class GregTech {
    public static final String MODID = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID;
    public static final String NAMESPACE = com.gregtech.gregtech.api.mod.GregTechIdentity.REGISTRY_NAMESPACE;
    public static final Logger LOGGER = LogUtils.getLogger();

    public GregTech(FMLJavaModLoadingContext context) {
        com.gregtech.gregtech.internal.addon.AddonCoreServices.bind();
        com.gregtech.gregtech.internal.addon.AddonPlatformServices.bind();
        com.gregtech.gregtech.internal.addon.AddonLoaderServices.bind();
        com.gregtech.gregtech.api.mod.ModData.bindPresence(id -> net.minecraftforge.fml.ModList.get().isLoaded(
                NAMESPACE.equals(id) ? MODID : id));
        GTMaterialRegistry.setLogSink((warning, message) -> {
            if (warning) LOGGER.warn(message); else LOGGER.info(message);
        });
        IEventBus modEventBus = context.getModEventBus();

        GTPackets.register();
        com.gregtech.gregtech.registry.GTProjectiles.register(modEventBus);
        com.gregtech.gregtech.worldgen.GTFeatures.register(modEventBus);

        // Base data must precede material items and dependent blocks.
        initialize("content",
                new DataBootstrap(),
                new MaterialBootstrap(),
                new ItemBootstrap(),
                new BlockBootstrap(),
                new ToolBootstrap(modEventBus),
                new FluidBootstrap(modEventBus)
        );

        // Queue machines and creative tabs before submitting deferred registries.
        initialize("machine registration",
                new MachineBlockBootstrap(modEventBus),
                new CreativeTabBootstrap(modEventBus),
                new DeferredRegistrySubmission(modEventBus)
        );

        modEventBus.addListener(this::commonSetup);

        context.registerConfig(ModConfig.Type.COMMON, GregTechConfig.SPEC);
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
            com.gregtech.gregtech.registry.GTProjectiles.registerDispensers();
            com.gregtech.gregtech.registry.GTDungeonBlocks.registerPotPlants();
            initialize("material linking", new MaterialLinkBootstrap(), new MaterialItemRegistration());
            com.gregtech.gregtech.content.recipe.MachineRecipeBootstrap.load();
            com.gregtech.gregtech.api.addon.GregTechAddons.dispatchRecipesReady(
                    new com.gregtech.gregtech.api.addon.GregTechAddon.Context("forge", "1.20.1"));
            LOGGER.info("GT addon API {} ready: {}", com.gregtech.gregtech.api.addon.GregTechAddons.API_VERSION,
                    com.gregtech.gregtech.api.addon.GregTechAddons.registeredIds());
            // Rows that RecipeMap.make cancelled down to nothing and dropped; a non-zero value is
            // expected whenever an optimized table contains an input that cancels against its own output.
            var collapsed = com.gregtech.gregtech.api.recipe.RecipeMap.collapsedSamples();
            LOGGER.info("Recipe tables: {} rows dropped as collapsed by optimization, per map {}, e.g. {}",
                    com.gregtech.gregtech.api.recipe.RecipeMap.COLLAPSED_RECIPES_DROPPED,
                    com.gregtech.gregtech.api.recipe.RecipeMap.collapsedByMap(),
                    collapsed.subList(0, Math.min(8, collapsed.size())));

            long itemCount = ForgeRegistries.ITEMS.getKeys().stream()
                    .filter(id -> NAMESPACE.equals(id.getNamespace())).count();
            long blockCount = ForgeRegistries.BLOCKS.getKeys().stream()
                    .filter(id -> NAMESPACE.equals(id.getNamespace())).count();
            LOGGER.info("{} loaded: {} materials, {} items, {} blocks",
                    MODID, GTMaterialRegistry.allMaterials().size(), itemCount, blockCount);
            if (itemCount == 0) {
                LOGGER.error("{} registered ZERO items — check MaterialPrefix bootstrap / DeferredRegister", MODID);
            }
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path.toLowerCase());
    }
}
