package gregtech.platform.neoforge;

import com.mojang.logging.LogUtils;
import gregtech.GregTechConfig;
import gregtech.api.addon.GregTechAddon;
import gregtech.api.addon.GregTechAddons;
import gregtech.api.material.*;
import gregtech.api.mod.ModData;
import gregtech.content.block.BlockMaterialWeights;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.content.block.stone.GTStoneSlabBlock;
import gregtech.content.block.stone.StoneMaterialWeights;
import gregtech.content.data.BottleFillingRecipePack;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.ManualToolRecipePack;
import gregtech.content.data.ModReferences;
import gregtech.content.data.RecipePublishing;
import gregtech.content.data.TrackCraftingRecipePack;
import gregtech.content.data.WoodCraftingRecipePack;
import gregtech.content.machine.MachineConstructionMaterials;
import gregtech.content.material.ModMaterialUnificationLoader;
import gregtech.content.material.VanillaCompositionLoader;
import gregtech.content.material.VanillaMaterialCorrections;
import gregtech.content.material.VanillaUnificationLoader;
import gregtech.content.multiblock.PlainStructureBlocks;
import gregtech.content.recipe.DiggableRecipes;
import gregtech.content.recipe.GTBumbleBeeRecipes;
import gregtech.content.recipe.MachineRecipeBootstrap;
import gregtech.content.recipe.PanelMaterialRegistration;
import gregtech.content.recipe.RegisteredWoodSurvivalRecipes;
import gregtech.content.recipe.TransportMaterialRegistration;
import gregtech.content.recipe.VanillaWoodProcessingRecipes;
import gregtech.content.registry.GTBars;
import gregtech.content.registry.GTBlackSands;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTDungeonBlocks;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTProjectiles;
import gregtech.content.registry.GTSignalWires;
import gregtech.content.registry.GTSpecialOreBlocks;
import gregtech.content.registry.GTWires;
import gregtech.content.registry.MultiblockRegistration;
import gregtech.engine.addon.AddonCoreServices;
import gregtech.engine.addon.AddonLoaderServices;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.addon.AddonPlatformServices;
import gregtech.engine.addon.ExclusiveOutputMaps;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialFormItem;
import gregtech.engine.material.MaterialRoleFlags;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.engine.mod.SetupOnce;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.engine.recipe.MachineWorkCost;
import gregtech.platform.neoforge.machine.BasicMachineRegistries;
import gregtech.platform.neoforge.registry.NeoContent;
import java.util.List;
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
        for (var holder : GTItems.allEntries()) {
            var item = holder.get();
            var form = (MaterialFormItem) item;
            ItemMaterialRegistry.register(item, form.getPrefix(), form.getMaterial());
        }
        GTDungeonBlocks.registerPotPlants();
        registerBlockCompositions();
        BasicMachineRegistries.registerCompositions();
        for (var block : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals("gt6ce")) continue;
            MachineConstructionMaterials.block(id.getPath())
                    .ifPresent(data -> ItemMaterialRegistry.register(block.asItem(), data));
        }
        GTSpecialOreBlocks.registerCompositions();
        DiggableRecipes.registerMaterials();
        RegisteredWoodSurvivalRecipes.registerMaterials();
        GTBlackSands.registerCompositions();
        VanillaUnificationLoader.register();
        ModMaterialUnificationLoader.register();
        VanillaCompositionLoader.register();
        VanillaMaterialCorrections.register();
        GTBars.registerCompositions();
        GTWires.registerCompositions();
        GTSignalWires.registerCompositions();
        VanillaWoodProcessingRecipes.registerMaterials();
        PanelMaterialRegistration.register();
        TransportMaterialRegistration.register();
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

    private static void registerBlockCompositions() {
        for (var holder : GTBlocks.allEntries()) {
            var block = holder.get();
            List<MaterialComponent> weights;
            if (block instanceof MaterialBlockLike b)
                weights = BlockMaterialWeights.contained(b.material(), b.prefix());
            else if (block instanceof GTStoneBlock b)
                weights = StoneMaterialWeights.contained(b.stoneMaterial(), b.variant(), false);
            else if (block instanceof GTStoneSlabBlock b)
                weights = StoneMaterialWeights.contained(b.stoneMaterial(), b.variant(), true);
            else continue;
            var components = weights.stream()
                    .filter(w -> w.material().isValid() && w.amount() > 0)
                    .map(w -> MaterialComponent.of(w.material(), w.amount()))
                    .toList();
            if (!components.isEmpty())
                ItemMaterialRegistry.register(
                        block.asItem(), new ItemComposition(null, components, "GT6 block composition", true));
        }
    }
}
