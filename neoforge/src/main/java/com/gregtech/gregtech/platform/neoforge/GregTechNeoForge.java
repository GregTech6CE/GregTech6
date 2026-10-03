package com.gregtech.gregtech.platform.neoforge;

import com.gregtech.gregtech.api.recipe.MachineWorkCost;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialRoleFlags;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.loader.VanillaCompositionLoader;
import com.gregtech.gregtech.loader.VanillaUnificationLoader;
import com.gregtech.gregtech.platform.neoforge.smeltery.SmelteryRegistries;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.MaterialGroups;
import com.gregtech.gregtech.data.ModReferences;
import com.gregtech.gregtech.registry.GTItems;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/** NeoForge lifecycle adapter. Gameplay registrations are added by subsystem ports. */
@Mod(GregTechNeoForge.MOD_ID)
public final class GregTechNeoForge {
    public static final String MOD_ID = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID;
    public static final String NAMESPACE = com.gregtech.gregtech.api.mod.GregTechIdentity.REGISTRY_NAMESPACE;
    private static final Logger LOGGER = LogUtils.getLogger();

    public GregTechNeoForge(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,com.gregtech.gregtech.GregTechConfig.SPEC);
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
        GTMaterialRegistry.init();
        MaterialRoleFlags.apply();
        com.gregtech.gregtech.platform.neoforge.fluid.FluidRegistries.register(modEventBus);
        com.gregtech.gregtech.registry.GTFuelRods.registerAll();
        com.gregtech.gregtech.registry.GTDungeonKeys.registerAll();
        com.gregtech.gregtech.registry.GTRadiationProtection.MATERIALS.register(modEventBus);
        int materialItems = GTItems.register(modEventBus);
        new com.gregtech.gregtech.loaders.a.Loader_Blocks().run();
        com.gregtech.gregtech.registry.GTWoods.registerAll();
        com.gregtech.gregtech.registry.GTTreeHoles.registerAll();
        com.gregtech.gregtech.registry.GTBumbleBlocks.initialize();
        com.gregtech.gregtech.registry.GTSurfaceBlocks.initialize();
        com.gregtech.gregtech.registry.GTBlackSands.initialize();
        com.gregtech.gregtech.registry.GTFluidSprings.initialize();
        com.gregtech.gregtech.registry.GTGrassSoils.initialize();
        com.gregtech.gregtech.registry.GTConstructionBlocks.initialize();
        com.gregtech.gregtech.registry.GTBars.initialize();
        com.gregtech.gregtech.registry.GTSpikes.initialize();
        com.gregtech.gregtech.registry.GTMaterialPiles.initialize();
        com.gregtech.gregtech.registry.GTCoinPiles.initialize();
        com.gregtech.gregtech.registry.GTBookShelves.initialize();
        com.gregtech.gregtech.registry.GTSafes.initialize();
        com.gregtech.gregtech.registry.GTCraftingTables.initialize();
        com.gregtech.gregtech.registry.GTStorageContainers.initialize();
        com.gregtech.gregtech.registry.GTRelaysFilters.initialize();
        com.gregtech.gregtech.registry.GTManualStations.initialize();
        com.gregtech.gregtech.registry.GTZpmArtifacts.initialize();
        com.gregtech.gregtech.registry.GTLasers.registerAll();
        com.gregtech.gregtech.registry.GTSensors.registerAll();
        com.gregtech.gregtech.registry.GTLongDistance.initialize();
        com.gregtech.gregtech.registry.GTTrackBlocks.registerAll();
        com.gregtech.gregtech.registry.GTEngines.registerAll();
        com.gregtech.gregtech.registry.GTBoilers.registerAll();
        com.gregtech.gregtech.registry.GTPumps.registerAll();
        com.gregtech.gregtech.registry.GTMagnets.registerAll();
        com.gregtech.gregtech.registry.GTLogisticsTank.initialize();
        com.gregtech.gregtech.registry.GTSandwich.initialize();
        com.gregtech.gregtech.registry.GTRemainingDecor.initialize();
        com.gregtech.gregtech.registry.GTDungeonBlocks.registerAll();
        com.gregtech.gregtech.registry.GTMetalChests.initialize();
        com.gregtech.gregtech.registry.GTBlocks.BLOCKS.register(modEventBus);
        com.gregtech.gregtech.registry.GTBlocks.BLOCK_ITEMS.register(modEventBus);
        com.gregtech.gregtech.registry.GTBlockEntities.register(modEventBus);
        com.gregtech.gregtech.worldgen.GTFeatures.register(modEventBus);
        com.gregtech.gregtech.platform.neoforge.logistics.StorageRegistries.register(modEventBus);
        com.gregtech.gregtech.registry.GTMultiItems.register(modEventBus);
        com.gregtech.gregtech.registry.GTTechnological.registerAll();
        com.gregtech.gregtech.registry.GTTechnological.ITEMS.register(modEventBus);
        com.gregtech.gregtech.platform.neoforge.logistics.LogisticsRegistries.register(modEventBus);
        com.gregtech.gregtech.registry.GTSounds.bootstrap();
        com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries.register(modEventBus);
        com.gregtech.gregtech.platform.neoforge.machine.BasicMachineRegistries.register(modEventBus);
        com.gregtech.gregtech.recipe.NeoRecipeSerializers.register(modEventBus);
        com.gregtech.gregtech.platform.neoforge.energy.LegacyBatteryRegistries.register(modEventBus);
        com.gregtech.gregtech.registry.GTChemicalBatteries.register(modEventBus);
        com.gregtech.gregtech.registry.GTWires.register(modEventBus);
        com.gregtech.gregtech.registry.GTSignalWires.register(modEventBus);
        com.gregtech.gregtech.registry.GTMenuTypes.register(modEventBus);
        com.gregtech.gregtech.registry.GTEnergyNodes.register(modEventBus);
        com.gregtech.gregtech.registry.GTAxles.register(modEventBus);
        com.gregtech.gregtech.registry.GTGearboxes.register(modEventBus);
        com.gregtech.gregtech.registry.GTToolItems.register(modEventBus);
        com.gregtech.gregtech.registry.GTElectricItems.register(modEventBus);
        com.gregtech.gregtech.registry.GTToolBlocks.register(modEventBus);
        com.gregtech.gregtech.registry.GTItemPipes.register(modEventBus);
        com.gregtech.gregtech.content.transport.HopperRegistries.register(modEventBus);
        SmelteryRegistries.register(modEventBus);
        com.gregtech.gregtech.registry.GTRemainingIconBlocks.initialize();
        com.gregtech.gregtech.loaders.b.Loader_Creative.register(modEventBus);
        LOGGER.info("Queued {} Neo material items from the shared original definitions", materialItems);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(this::finishSharedSetup);
    }

    private void finishSharedSetup() {
        for (var holder : GTItems.allEntries()) {
            var item = holder.get();
            ItemMaterialRegistry.register(item, item.getPrefix(), item.getMaterial());
        }
        com.gregtech.gregtech.registry.GTDungeonBlocks.registerPotPlants();
        registerBlockCompositions();
        com.gregtech.gregtech.platform.neoforge.machine.BasicMachineRegistries.registerCompositions();
        com.gregtech.gregtech.registry.GTSpecialOreBlocks.registerCompositions();
        com.gregtech.gregtech.content.recipe.DiggableRecipes.registerMaterials();
        com.gregtech.gregtech.content.recipe.RegisteredWoodSurvivalRecipes.registerMaterials();
        com.gregtech.gregtech.registry.GTBlackSands.registerCompositions();
        VanillaUnificationLoader.register();
        VanillaCompositionLoader.register();
        com.gregtech.gregtech.data.MachineRecipeMaps.bootstrap();
        com.gregtech.gregtech.content.recipe.GTBumbleBeeRecipes.register();
        LOGGER.info("[gregtech] Native bumblebee scanning display rows: {}",com.gregtech.gregtech.content.recipe.GTBumbleBeeRecipes.registerDisplayRows());
        com.gregtech.gregtech.content.recipe.FermenterFoodRecipes.register();
        com.gregtech.gregtech.content.transport.fluid.FermentationAccess.bindOriginalRecipeMaps();
        GTMaterialRegistry.postInit();
        com.gregtech.gregtech.content.recipe.NeoMachineRecipeLoader.load();
        com.gregtech.gregtech.api.addon.GregTechAddons.dispatchRecipesReady(
                new com.gregtech.gregtech.api.addon.GregTechAddon.Context("neoforge", "1.21.1"));
        LOGGER.info("GT addon API {} ready: {}", com.gregtech.gregtech.api.addon.GregTechAddons.API_VERSION,
                com.gregtech.gregtech.api.addon.GregTechAddons.registeredIds());
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
        for (var holder : com.gregtech.gregtech.registry.GTBlocks.allEntries()) {
            var block = holder.get();
            java.util.List<com.gregtech.gregtech.api.material.MaterialChemistry.WeightedMaterial> weights;
            if (block instanceof com.gregtech.gregtech.block.MaterialBlockLike b)
                weights = com.gregtech.gregtech.block.BlockMaterialWeights.contained(b.material(), b.prefix());
            else if (block instanceof com.gregtech.gregtech.block.stone.GTStoneBlock b)
                weights = com.gregtech.gregtech.block.stone.StoneMaterialWeights.contained(b.stoneMaterial(), b.variant(), false);
            else if (block instanceof com.gregtech.gregtech.block.stone.GTStoneSlabBlock b)
                weights = com.gregtech.gregtech.block.stone.StoneMaterialWeights.contained(b.stoneMaterial(), b.variant(), true);
            else continue;
            var components = weights.stream().filter(w -> w.material().isValid() && w.amount() > 0)
                    .map(w -> com.gregtech.gregtech.api.material.MaterialComponent.of(w.material(), w.amount())).toList();
            if (!components.isEmpty()) ItemMaterialRegistry.register(block.asItem(),
                    new com.gregtech.gregtech.api.material.ItemComposition(null, components, "GT6 block composition", true));
        }
    }
}
