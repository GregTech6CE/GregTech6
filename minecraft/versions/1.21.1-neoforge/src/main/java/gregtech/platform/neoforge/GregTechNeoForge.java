package gregtech.platform.neoforge;

import com.mojang.logging.LogUtils;
import gregtech.api.material.*;
import gregtech.api.mod.ModData;
import gregtech.content.data.MaterialGroups;
import gregtech.content.data.ModReferences;
import gregtech.content.material.VanillaCompositionLoader;
import gregtech.content.material.VanillaUnificationLoader;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialRoleFlags;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.engine.recipe.MachineWorkCost;
import gregtech.platform.neoforge.smeltery.SmelteryRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/** NeoForge lifecycle adapter. Gameplay registrations are added by subsystem ports. */
@Mod(GregTechNeoForge.MOD_ID)
public final class GregTechNeoForge {
    public static final String MOD_ID = gregtech.engine.mod.GregTechIdentity.MOD_ID;
    public static final String NAMESPACE = gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final gregtech.engine.mod.SetupOnce SHARED_SETUP = new gregtech.engine.mod.SetupOnce();

    public GregTechNeoForge(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        gregtech.engine.addon.AddonCoreServices.bind();
        gregtech.engine.addon.AddonPlatformServices.bind();
        gregtech.engine.addon.AddonLoaderServices.bind();
        gregtech.GregTechConfig.prepareDirectory(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        modContainer.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.COMMON,
                gregtech.GregTechConfig.SPEC,
                gregtech.GregTechConfig.COMMON_FILE);
        modContainer.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.CLIENT,
                gregtech.GregTechConfig.CLIENT_SPEC,
                gregtech.GregTechConfig.CLIENT_FILE);
        // No material/metadata holder may initialize before this platform binding.
        ModData.bindPresence(id -> ModList.get().isLoaded(NAMESPACE.equals(id) ? MOD_ID : id));
        GTMaterialRegistry.setLogSink((warning, message) -> {
            if (warning) LOGGER.warn(message);
            else LOGGER.info(message);
        });
        // Same domain holder order as Forge Loader_Data, followed by Loader_Materials.
        PrefixRegistry.ensurePrefixesLoaded();
        ModReferences.UNKNOWN.getClass();
        MaterialGroups.Glowstone.getClass();
        gregtech.engine.addon.AddonMaterials.collectContributors();
        GTMaterialRegistry.init();
        MaterialRoleFlags.apply();
        gregtech.platform.neoforge.fluid.FluidRegistries.register(modEventBus);
        gregtech.content.registry.GTFuelRods.registerAll();
        gregtech.content.registry.GTDungeonKeys.registerAll();
        gregtech.content.registry.GTRadiationProtection.MATERIALS.register(modEventBus);
        gregtech.content.registry.GTColoredBooks.registerAll();
        int materialItems = GTItems.register(modEventBus);
        gregtech.content.registry.GTProjectiles.register(modEventBus);
        new gregtech.bootstrap.content.BlockBootstrap().run();
        gregtech.content.registry.GTWoods.registerAll();
        gregtech.content.registry.GTTreeHoles.registerAll();
        gregtech.content.registry.GTBumbleBlocks.initialize();
        gregtech.content.registry.GTSurfaceBlocks.initialize();
        gregtech.content.registry.GTBlackSands.initialize();
        gregtech.content.registry.GTFluidSprings.initialize();
        gregtech.content.registry.GTGrassSoils.initialize();
        gregtech.content.registry.GTConstructionBlocks.initialize();
        gregtech.content.registry.GTBars.initialize();
        gregtech.content.registry.GTSpikes.initialize();
        gregtech.content.registry.GTMaterialPiles.initialize();
        gregtech.content.registry.GTCoinPiles.initialize();
        gregtech.content.registry.GTBookShelves.initialize();
        gregtech.content.registry.GTSafes.initialize();
        gregtech.content.registry.GTCraftingTables.initialize();
        gregtech.content.registry.GTStorageContainers.initialize();
        gregtech.content.registry.GTRelaysFilters.initialize();
        gregtech.content.registry.GTManualStations.initialize();
        gregtech.content.registry.GTZpmArtifacts.initialize();
        gregtech.content.registry.GTLasers.registerAll();
        gregtech.content.registry.GTSensors.registerAll();
        gregtech.platform.neoforge.machine.AddonSensorRegistration.register(modEventBus);
        gregtech.content.registry.GTLongDistance.initialize();
        gregtech.content.registry.GTTrackBlocks.registerAll();
        gregtech.content.registry.GTEngines.registerAll();
        gregtech.content.registry.GTBoilers.registerAll();
        gregtech.content.registry.GTPumps.registerAll();
        gregtech.content.registry.GTLogisticsTank.initialize();
        gregtech.content.registry.GTSandwich.initialize();
        gregtech.content.registry.GTRemainingDecor.initialize();
        gregtech.content.registry.GTDungeonBlocks.registerAll();
        gregtech.content.registry.GTMetalChests.initialize();
        gregtech.content.registry.GTBlocks.BLOCKS.register(modEventBus);
        gregtech.content.registry.GTBlocks.BLOCK_ITEMS.register(modEventBus);
        gregtech.content.registry.GTBlockEntities.register(modEventBus);
        gregtech.content.worldgen.GTFeatures.register(modEventBus);
        gregtech.platform.neoforge.logistics.StorageRegistries.register(modEventBus);
        gregtech.content.registry.GTMultiItems.register(modEventBus);
        gregtech.content.registry.GTTechnological.registerAll();
        gregtech.content.registry.GTTechnological.ITEMS.register(modEventBus);
        gregtech.platform.neoforge.logistics.LogisticsRegistries.register(modEventBus);
        gregtech.content.registry.GTSounds.bootstrap();
        gregtech.content.transport.fluid.FluidTransportRegistries.register(modEventBus);
        gregtech.platform.neoforge.machine.BasicMachineRegistries.register(modEventBus);
        gregtech.content.registry.GTMultiblocks.registerAddonParts();
        gregtech.content.registry.GTMultiblocks.registerAddonBlocks(modEventBus);
        gregtech.content.recipe.NeoRecipeSerializers.register(modEventBus);
        gregtech.content.registry.GTChemicalBatteries.register(modEventBus);
        gregtech.content.registry.GTWires.register(modEventBus);
        gregtech.content.registry.GTSignalWires.register(modEventBus);
        gregtech.content.registry.GTMenuTypes.register(modEventBus);
        gregtech.content.registry.GTEnergyNodes.register(modEventBus);
        gregtech.content.registry.GTAxles.register(modEventBus);
        gregtech.content.registry.GTGearboxes.register(modEventBus);
        gregtech.content.registry.GTToolItems.register(modEventBus);
        gregtech.content.registry.GTElectricItems.register(modEventBus);
        gregtech.content.registry.GTToolBlocks.register(modEventBus);
        gregtech.content.registry.GTItemPipes.register(modEventBus);
        gregtech.content.registry.AddonPipeRegistration.submit(modEventBus);
        gregtech.content.transport.HopperRegistries.register(modEventBus);
        SmelteryRegistries.register(modEventBus);
        gregtech.content.registry.GTRemainingIconBlocks.initialize();
        gregtech.bootstrap.registration.CreativeTabBootstrap.register(modEventBus);
        LOGGER.info("Queued {} Neo material items from the shared original definitions", materialItems);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> SHARED_SETUP.run(GregTechNeoForge::finishSharedSetup));
    }

    private static void finishSharedSetup() {
        gregtech.content.registry.GTProjectiles.registerDispensers();
        for (var holder : GTItems.allEntries()) {
            var item = holder.get();
            var form = (gregtech.engine.material.MaterialFormItem) item;
            ItemMaterialRegistry.register(item, form.getPrefix(), form.getMaterial());
        }
        gregtech.content.registry.GTDungeonBlocks.registerPotPlants();
        registerBlockCompositions();
        gregtech.platform.neoforge.machine.BasicMachineRegistries.registerCompositions();
        for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals("gt6ce")) continue;
            gregtech.content.machine.MachineConstructionMaterials.block(id.getPath())
                    .ifPresent(data -> ItemMaterialRegistry.register(block.asItem(), data));
        }
        gregtech.content.registry.GTSpecialOreBlocks.registerCompositions();
        gregtech.content.recipe.DiggableRecipes.registerMaterials();
        gregtech.content.recipe.RegisteredWoodSurvivalRecipes.registerMaterials();
        gregtech.content.registry.GTBlackSands.registerCompositions();
        VanillaUnificationLoader.register();
        gregtech.content.material.ModMaterialUnificationLoader.register();
        VanillaCompositionLoader.register();
        gregtech.content.recipe.PanelMaterialRegistration.register();
        gregtech.content.recipe.TransportMaterialRegistration.register();
        gregtech.content.data.MachineRecipeMaps.bootstrap();
        gregtech.content.recipe.GTBumbleBeeRecipes.register();
        LOGGER.info(
                "[gregtech] Native bumblebee scanning display rows: {}",
                gregtech.content.recipe.GTBumbleBeeRecipes.registerDisplayRows());
        gregtech.content.recipe.FermenterFoodRecipes.register();
        gregtech.content.transport.fluid.FermentationAccess.bindOriginalRecipeMaps();
        GTMaterialRegistry.postInit();
        gregtech.content.recipe.MachineRecipeBootstrap.load();
        gregtech.api.addon.GregTechAddons.dispatchRecipesReady(
                new gregtech.api.addon.GregTechAddon.Context("neoforge", "1.21.1"));
        gregtech.engine.addon.ExclusiveOutputMaps.audit();
        gregtech.content.multiblock.PlainStructureBlocks.resolve();
        gregtech.content.registry.MultiblockRegistration.validate();
        LOGGER.info(
                "GT addon API {} ready: {}",
                gregtech.api.addon.GregTechAddons.API_VERSION,
                gregtech.api.addon.GregTechAddons.registeredIds());
        MachineWorkCost.Cost cost = MachineWorkCost.calculate(8, 20, 1, false, 10_000, 8, 32, false);
        if (cost == null || cost.minimumPower() != 8 || cost.totalWork() != 160) {
            throw new IllegalStateException("GT6 shared machine work accounting failed to initialize");
        }
        LOGGER.info(
                "GregTech shared core initialized: {} material objects, minimumPower={}, totalWork={}; "
                        + "Neo material items/blocks/stones and ore-vein/small-ore/stone-layer features registered; "
                        + "vanilla composition and first smeltery machines registered; source integration remains unverified",
                GTMaterialRegistry.allMaterials().size(),
                cost.minimumPower(),
                cost.totalWork());
    }

    private static void registerBlockCompositions() {
        for (var holder : gregtech.content.registry.GTBlocks.allEntries()) {
            var block = holder.get();
            java.util.List<gregtech.api.material.WeightedMaterial> weights;
            if (block instanceof gregtech.content.block.MaterialBlockLike b)
                weights = gregtech.content.block.BlockMaterialWeights.contained(b.material(), b.prefix());
            else if (block instanceof gregtech.content.block.stone.GTStoneBlock b)
                weights = gregtech.content.block.stone.StoneMaterialWeights.contained(
                        b.stoneMaterial(), b.variant(), false);
            else if (block instanceof gregtech.content.block.stone.GTStoneSlabBlock b)
                weights = gregtech.content.block.stone.StoneMaterialWeights.contained(
                        b.stoneMaterial(), b.variant(), true);
            else continue;
            var components = weights.stream()
                    .filter(w -> w.material().isValid() && w.amount() > 0)
                    .map(w -> gregtech.api.material.MaterialComponent.of(w.material(), w.amount()))
                    .toList();
            if (!components.isEmpty())
                ItemMaterialRegistry.register(
                        block.asItem(),
                        new gregtech.engine.material.ItemComposition(null, components, "GT6 block composition", true));
        }
    }
}
