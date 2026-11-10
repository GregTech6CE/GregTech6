package gregtech.platform.neoforge;

import com.mojang.logging.LogUtils;
import gregtech.GregTechConfig;
import gregtech.api.addon.GregTechAddon;
import gregtech.api.addon.GregTechAddons;
import gregtech.api.mod.ModData;
import gregtech.content.data.*;
import gregtech.content.multiblock.PlainStructureBlocks;
import gregtech.content.recipe.GTBumbleBeeRecipes;
import gregtech.content.recipe.MachineRecipeBootstrap;
import gregtech.content.registry.*;
import gregtech.engine.addon.*;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialRoleFlags;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.engine.mod.SetupOnce;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.engine.recipe.MachineWorkCost;
import gregtech.platform.neoforge.registry.NeoContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

/** NeoForge lifecycle adapter. Gameplay registrations are added by subsystem ports. */
@Mod(GregTechNeoForge.MOD_ID)
public final class GregTechNeoForge {
    public static final String MOD_ID = GregTechIdentity.MOD_ID;
    public static final String NAMESPACE = GregTechIdentity.REGISTRY_NAMESPACE;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SetupOnce SHARED_SETUP = new SetupOnce();

    public GregTechNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        AddonCoreServices.bind();
        AddonPlatformServices.bind();
        AddonLoaderServices.bind();
        GregTechConfig.prepareDirectory(FMLPaths.CONFIGDIR.get());
        modContainer.registerConfig(ModConfig.Type.COMMON, GregTechConfig.SPEC, GregTechConfig.COMMON_FILE);
        modContainer.registerConfig(ModConfig.Type.CLIENT, GregTechConfig.CLIENT_SPEC, GregTechConfig.CLIENT_FILE);
        // No material/metadata holder may initialize before this platform binding.
        ModData.bindPresence(id -> ModList.get().isLoaded(NAMESPACE.equals(id) ? MOD_ID : id));
        GTMaterialRegistry.setLogSink((warning, message) -> {
            if (warning) LOGGER.warn(message);
            else LOGGER.info(message);
        });
        // Same domain holder order as Forge Loader_Data, followed by Loader_Materials.
        PrefixRegistry.ensurePrefixesLoaded();
        ModReferences.UNKNOWN.getClass();
        AddonMaterials.collectContributors();
        GTMaterialRegistry.init();
        MaterialRoleFlags.apply();
        // Shared content order plus the NeoForge-only families; then one submission for every register.
        NeoContent content = new NeoContent(modEventBus);
        content.bootstrap();
        content.submit();
        // Every dynamic recipe pack is published by one owner; this node contributes its producers.
        RecipePublishing.contributeGenerated("original manual tools", ManualToolRecipePack::contribute);
        RecipePublishing.contributeGenerated("original wood crafting", WoodCraftingRecipePack::contribute);
        RecipePublishing.contributeGenerated("finite bottle filling", BottleFillingRecipePack::contribute);
        RecipePublishing.contributeGenerated("original track crafting", TrackCraftingRecipePack::contribute);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> SHARED_SETUP.run(GregTechNeoForge::finishSharedSetup));
    }

    private static void finishSharedSetup() {
        GTProjectiles.registerDispensers();
        GTDungeonBlocks.registerPotPlants();
        GTSpecialOreBlocks.registerCompositions();
        GTBlackSands.registerCompositions();
        gregtech.content.material.MaterialItemCompositions.registerVanillaBindings();
        gregtech.content.material.MaterialItemCompositions.registerCommon();
        gregtech.engine.material.MaterialFormContent.publish();
        MachineRecipeMaps.bootstrap();
        GTBumbleBeeRecipes.register();
        LOGGER.info(
                "[gregtech] Native bumblebee scanning display rows: {}",
                gregtech.content.recipe.GTBumbleBeeRecipes.registerDisplayRows());
        GTMaterialRegistry.postInit();
        MachineRecipeBootstrap.load();
        GregTechAddons.dispatchRecipesReady(new GregTechAddon.Context("neoforge", "1.21.1"));
        ExclusiveOutputMaps.audit();
        PlainStructureBlocks.resolve();
        MultiblockRegistration.validate();
        LOGGER.info("GT addon API {} ready: {}", GregTechAddons.API_VERSION, GregTechAddons.registeredIds());
        MachineWorkCost.Cost cost = MachineWorkCost.calculate(8, 20, 1, false, 10_000, 8, 32, false);
        if (cost == null || cost.minimumPower() != 8 || cost.totalWork() != 160) {
            throw new IllegalStateException("GT6 shared machine work accounting failed to initialize");
        }
        long itemCount = BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> NAMESPACE.equals(id.getNamespace()))
                .count();
        long blockCount = BuiltInRegistries.BLOCK.keySet().stream()
                .filter(id -> NAMESPACE.equals(id.getNamespace()))
                .count();
        LOGGER.info(
                "{} loaded: {} materials, {} items, {} blocks",
                MOD_ID,
                GTMaterialRegistry.allMaterials().size(),
                itemCount,
                blockCount);
        LOGGER.info(
                "GregTech shared core initialized: {} material objects, minimumPower={}, totalWork={}; "
                        + "Neo material items/blocks/stones and ore-vein/small-ore/stone-layer features registered; "
                        + "vanilla composition and first smeltery machines registered; source integration remains unverified",
                GTMaterialRegistry.allMaterials().size(),
                cost.minimumPower(),
                cost.totalWork());
    }
}
