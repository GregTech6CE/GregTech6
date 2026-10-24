package gregtech.platform.neoforge;

import gregtech.api.recipe.MachineWorkCost;
import gregtech.api.material.GTMaterialRegistry;
import gregtech.api.material.MaterialRoleFlags;
import gregtech.api.material.ItemMaterialRegistry;
import gregtech.content.material.VanillaCompositionLoader;
import gregtech.content.material.VanillaUnificationLoader;
import gregtech.platform.neoforge.smeltery.SmelteryRegistries;
import gregtech.api.mod.ModData;
import gregtech.api.prefix.PrefixRegistry;
import gregtech.data.MaterialGroups;
import gregtech.data.ModReferences;
import gregtech.registry.GTItems;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/** NeoForge lifecycle adapter. Gameplay registrations are added by subsystem ports. */
@Mod(GregTechNeoForge.MOD_ID)
public final class GregTechNeoForge {
    public static final String MOD_ID = gregtech.api.mod.GregTechIdentity.MOD_ID;
    public static final String NAMESPACE = gregtech.api.mod.GregTechIdentity.REGISTRY_NAMESPACE;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final gregtech.api.mod.SetupOnce SHARED_SETUP = new gregtech.api.mod.SetupOnce();

    public GregTechNeoForge(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        gregtech.internal.addon.AddonCoreServices.bind();
        gregtech.internal.addon.AddonPlatformServices.bind();
        gregtech.internal.addon.AddonLoaderServices.bind();
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,gregtech.GregTechConfig.SPEC);
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
        gregtech.api.addon.AddonMaterials.collectContributors();
        GTMaterialRegistry.init();
        MaterialRoleFlags.apply();
        gregtech.platform.neoforge.fluid.FluidRegistries.register(modEventBus);
        gregtech.registry.GTFuelRods.registerAll();
        gregtech.registry.GTDungeonKeys.registerAll();
        gregtech.registry.GTRadiationProtection.MATERIALS.register(modEventBus);
        gregtech.registry.GTColoredBooks.registerAll();
        int materialItems = GTItems.register(modEventBus);
        gregtech.registry.GTProjectiles.register(modEventBus);
        new gregtech.bootstrap.content.BlockBootstrap().run();
        gregtech.registry.GTWoods.registerAll();
        gregtech.registry.GTTreeHoles.registerAll();
        gregtech.registry.GTBumbleBlocks.initialize();
        gregtech.registry.GTSurfaceBlocks.initialize();
        gregtech.registry.GTBlackSands.initialize();
        gregtech.registry.GTFluidSprings.initialize();
        gregtech.registry.GTGrassSoils.initialize();
        gregtech.registry.GTConstructionBlocks.initialize();
        gregtech.registry.GTBars.initialize();
        gregtech.registry.GTSpikes.initialize();
        gregtech.registry.GTMaterialPiles.initialize();
        gregtech.registry.GTCoinPiles.initialize();
        gregtech.registry.GTBookShelves.initialize();
        gregtech.registry.GTSafes.initialize();
        gregtech.registry.GTCraftingTables.initialize();
        gregtech.registry.GTStorageContainers.initialize();
        gregtech.registry.GTRelaysFilters.initialize();
        gregtech.registry.GTManualStations.initialize();
        gregtech.registry.GTZpmArtifacts.initialize();
        gregtech.registry.GTLasers.registerAll();
        gregtech.registry.GTSensors.registerAll();
        gregtech.platform.neoforge.machine.AddonSensorRegistration.register(modEventBus);
        gregtech.registry.GTLongDistance.initialize();
        gregtech.registry.GTTrackBlocks.registerAll();
        gregtech.registry.GTEngines.registerAll();
        gregtech.registry.GTBoilers.registerAll();
        gregtech.registry.GTPumps.registerAll();
        gregtech.registry.GTLogisticsTank.initialize();
        gregtech.registry.GTSandwich.initialize();
        gregtech.registry.GTRemainingDecor.initialize();
        gregtech.registry.GTDungeonBlocks.registerAll();
        gregtech.registry.GTMetalChests.initialize();
        gregtech.registry.GTBlocks.BLOCKS.register(modEventBus);
        gregtech.registry.GTBlocks.BLOCK_ITEMS.register(modEventBus);
        gregtech.registry.GTBlockEntities.register(modEventBus);
        gregtech.worldgen.GTFeatures.register(modEventBus);
        gregtech.platform.neoforge.logistics.StorageRegistries.register(modEventBus);
        gregtech.registry.GTMultiItems.register(modEventBus);
        gregtech.registry.GTTechnological.registerAll();
        gregtech.registry.GTTechnological.ITEMS.register(modEventBus);
        gregtech.platform.neoforge.logistics.LogisticsRegistries.register(modEventBus);
        gregtech.registry.GTSounds.bootstrap();
        gregtech.content.transport.fluid.FluidTransportRegistries.register(modEventBus);
        gregtech.platform.neoforge.machine.BasicMachineRegistries.register(modEventBus);
        gregtech.registry.GTMultiblocks.registerAddonParts();
        gregtech.registry.GTMultiblocks.registerAddonBlocks(modEventBus);
        gregtech.recipe.NeoRecipeSerializers.register(modEventBus);
        gregtech.registry.GTChemicalBatteries.register(modEventBus);
        gregtech.registry.GTWires.register(modEventBus);
        gregtech.registry.GTSignalWires.register(modEventBus);
        gregtech.registry.GTMenuTypes.register(modEventBus);
        gregtech.registry.GTEnergyNodes.register(modEventBus);
        gregtech.registry.GTAxles.register(modEventBus);
        gregtech.registry.GTGearboxes.register(modEventBus);
        gregtech.registry.GTToolItems.register(modEventBus);
        gregtech.registry.GTElectricItems.register(modEventBus);
        gregtech.registry.GTToolBlocks.register(modEventBus);
        gregtech.registry.GTItemPipes.register(modEventBus);
        gregtech.registry.AddonPipeRegistration.submit(modEventBus);
        gregtech.content.transport.HopperRegistries.register(modEventBus);
        SmelteryRegistries.register(modEventBus);
        gregtech.registry.GTRemainingIconBlocks.initialize();
        gregtech.bootstrap.registration.CreativeTabBootstrap.register(modEventBus);
        LOGGER.info("Queued {} Neo material items from the shared original definitions", materialItems);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> SHARED_SETUP.run(GregTechNeoForge::finishSharedSetup));
    }

    private static void finishSharedSetup() {
        gregtech.registry.GTProjectiles.registerDispensers();
        for (var holder : GTItems.allEntries()) {
            var item = holder.get();
            var form = (gregtech.api.material.MaterialFormItem) item;
            ItemMaterialRegistry.register(item, form.getPrefix(), form.getMaterial());
        }
        gregtech.registry.GTDungeonBlocks.registerPotPlants();
        registerBlockCompositions();
        gregtech.platform.neoforge.machine.BasicMachineRegistries.registerCompositions();
        for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals("gt6ce")) continue;
            gregtech.content.machine.MachineConstructionMaterials.block(id.getPath())
                    .ifPresent(data -> ItemMaterialRegistry.register(block.asItem(), data));
        }
        gregtech.registry.GTSpecialOreBlocks.registerCompositions();
        gregtech.content.recipe.DiggableRecipes.registerMaterials();
        gregtech.content.recipe.RegisteredWoodSurvivalRecipes.registerMaterials();
        gregtech.registry.GTBlackSands.registerCompositions();
        VanillaUnificationLoader.register();
        VanillaCompositionLoader.register();
        gregtech.content.recipe.PanelMaterialRegistration.register();
        gregtech.content.recipe.TransportMaterialRegistration.register();
        gregtech.data.MachineRecipeMaps.bootstrap();
        gregtech.content.recipe.GTBumbleBeeRecipes.register();
        LOGGER.info("[gregtech] Native bumblebee scanning display rows: {}",gregtech.content.recipe.GTBumbleBeeRecipes.registerDisplayRows());
        gregtech.content.recipe.FermenterFoodRecipes.register();
        gregtech.content.transport.fluid.FermentationAccess.bindOriginalRecipeMaps();
        GTMaterialRegistry.postInit();
        gregtech.content.recipe.MachineRecipeBootstrap.load();
        gregtech.api.addon.GregTechAddons.dispatchRecipesReady(
                new gregtech.api.addon.GregTechAddon.Context("neoforge", "1.21.1"));
        gregtech.api.addon.ExclusiveOutputMaps.audit();
        gregtech.content.multiblock.PlainStructureBlocks.resolve();
        LOGGER.info("GT addon API {} ready: {}", gregtech.api.addon.GregTechAddons.API_VERSION,
                gregtech.api.addon.GregTechAddons.registeredIds());
        MachineWorkCost.Cost cost = MachineWorkCost.calculate(
                8, 20, 1, false, 10_000, 8, 32, false);
        if (cost == null || cost.minimumPower() != 8 || cost.totalWork() != 160) {
            throw new IllegalStateException("GT6 shared machine work accounting failed to initialize");
        }
        LOGGER.info("GregTech shared core initialized: {} material objects, minimumPower={}, totalWork={}; "
                        + "Neo material items/blocks/stones and ore-vein/small-ore/stone-layer features registered; "
                        + "vanilla composition and first smeltery machines registered; source integration remains unverified",
                GTMaterialRegistry.allMaterials().size(), cost.minimumPower(), cost.totalWork());
    }

    private static void registerBlockCompositions() {
        for (var holder : gregtech.registry.GTBlocks.allEntries()) {
            var block = holder.get();
            java.util.List<gregtech.api.material.MaterialChemistry.WeightedMaterial> weights;
            if (block instanceof gregtech.block.MaterialBlockLike b)
                weights = gregtech.block.BlockMaterialWeights.contained(b.material(), b.prefix());
            else if (block instanceof gregtech.block.stone.GTStoneBlock b)
                weights = gregtech.block.stone.StoneMaterialWeights.contained(b.stoneMaterial(), b.variant(), false);
            else if (block instanceof gregtech.block.stone.GTStoneSlabBlock b)
                weights = gregtech.block.stone.StoneMaterialWeights.contained(b.stoneMaterial(), b.variant(), true);
            else continue;
            var components = weights.stream().filter(w -> w.material().isValid() && w.amount() > 0)
                    .map(w -> gregtech.api.material.MaterialComponent.of(w.material(), w.amount())).toList();
            if (!components.isEmpty()) ItemMaterialRegistry.register(block.asItem(),
                    new gregtech.api.material.ItemComposition(null, components, "GT6 block composition", true));
        }
    }
}
