package com.gregtech.gregtech;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.loaders.IGTLoader;
import com.gregtech.gregtech.loaders.a.*;
import com.gregtech.gregtech.loaders.b.*;
import com.gregtech.gregtech.loaders.c.Loader_Recipes_OreProcessing;
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

import java.util.List;

@Mod(GregTech.MODID)
public class GregTech {
    public static final String MODID = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID;
    public static final String NAMESPACE = com.gregtech.gregtech.api.mod.GregTechIdentity.REGISTRY_NAMESPACE;
    public static final Logger LOGGER = LogUtils.getLogger();

    public GregTech(FMLJavaModLoadingContext context) {
        com.gregtech.gregtech.api.mod.ModData.bindPresence(id -> net.minecraftforge.fml.ModList.get().isLoaded(
                NAMESPACE.equals(id) ? MODID : id));
        GTMaterialRegistry.setLogSink((warning, message) -> {
            if (warning) LOGGER.warn(message); else LOGGER.info(message);
        });
        IEventBus modEventBus = context.getModEventBus();

        GTPackets.register();
        com.gregtech.gregtech.worldgen.GTFeatures.register(modEventBus);

        // Phase A — Data, materials, basic items/blocks, fluids, tools
        List<IGTLoader> phaseA = List.of(
                new Loader_Data(),
                new Loader_Materials(),
                new Loader_Items(),
                new Loader_Blocks(),
                new Loader_Tools(modEventBus),
                new Loader_Fluids(modEventBus)
        );

        for (IGTLoader loader : phaseA) {
            try {
                loader.run();
            } catch (Exception e) {
                LOGGER.error("[{}] Phase A loader '{}' failed", MODID, loader.getClass().getSimpleName(), e);
                throw new IllegalStateException("Phase A failed: " + loader.getClass().getSimpleName(), e);
            }
        }

        // Phase B — MultiTileEntities (wires/pipes/tanks/machines), Creative tabs
        List<IGTLoader> phaseB = List.of(
                new Loader_MultiTileEntities(modEventBus),
                new Loader_Creative(modEventBus),
                new Loader_Submit(modEventBus)
        );

        for (IGTLoader loader : phaseB) {
            try {
                loader.run();
            } catch (Exception e) {
                LOGGER.error("[{}] Phase B loader '{}' failed", MODID, loader.getClass().getSimpleName(), e);
                throw new IllegalStateException("Phase B failed: " + loader.getClass().getSimpleName(), e);
            }
        }

        modEventBus.addListener(this::commonSetup);

        context.registerConfig(ModConfig.Type.COMMON, GregTechConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            com.gregtech.gregtech.registry.GTDungeonBlocks.registerPotPlants();
            // Phase C — Post-registration (material linking, recipes, F3+H tooltips)
            List<IGTLoader> phaseC = List.of(
                    new Loader_MaterialPost(),
                    new Loader_MaterialRegistration(),
                    new Loader_Recipes_OreProcessing(),
                    new com.gregtech.gregtech.loaders.c.Loader_Recipes_Parts(),
                    () -> LOGGER.info("Registered {} original vanilla processing recipes", com.gregtech.gregtech.content.recipe.VanillaProcessingRecipes.register()),
                    () -> LOGGER.info("Registered {} GT6 diggable processing rows", com.gregtech.gregtech.content.recipe.DiggableRecipes.register()),
                    () -> LOGGER.info("Registered {} vanilla block processing recipes", com.gregtech.gregtech.content.recipe.VanillaBlockProcessingRecipes.register()),
                    () -> LOGGER.info("Registered {} vanilla wood processing recipes", com.gregtech.gregtech.content.recipe.VanillaWoodProcessingRecipes.register()),
                    () -> LOGGER.info("Registered {} GT tree and nether wood survival recipes", com.gregtech.gregtech.content.recipe.RegisteredWoodSurvivalRecipes.register()),
                    () -> LOGGER.info("Registered {} original crop processing recipes", com.gregtech.gregtech.content.recipe.CropProcessingRecipes.register()),
                    () -> LOGGER.info("Registered {} original dye and flower processing recipes", com.gregtech.gregtech.content.recipe.DyeProcessingRecipes.register()),
                    () -> LOGGER.info("Registered {} original textile finishing recipes", com.gregtech.gregtech.content.recipe.TextileFinishingRecipes.register()),
                    () -> LOGGER.info("Registered {} original biological material recipes", com.gregtech.gregtech.content.recipe.BiologicalMaterialRecipes.register()),
                    () -> LOGGER.info("Registered {} GT6 Glowtus processing recipes", com.gregtech.gregtech.content.recipe.GlowtusProcessingRecipes.register()),
                    () -> LOGGER.info("Registered {} GT6 bedrock flower processing recipes", com.gregtech.gregtech.content.recipe.BedrockFlowerProcessingRecipes.register()),
                    () -> LOGGER.info("Registered {} original survival utility recipes", com.gregtech.gregtech.content.recipe.SurvivalUtilityRecipes.register()),
                    () -> LOGGER.info("Registered {} original food fermentation recipes (GT6 Loader_Recipes_Food)",
                            com.gregtech.gregtech.content.recipe.FermenterFoodRecipes.register()),
                    () -> LOGGER.info("Registered {} crucible smelting recipes (GT6 RM.CrucibleSmelting)",
                            com.gregtech.gregtech.content.recipe.CrucibleSmeltingRecipes.register()),
                    () -> LOGGER.info("Registered {} original structural welding recipes",com.gregtech.gregtech.content.recipe.StructuralPartRecipes.register()),
                    () -> LOGGER.info("Registered {} original machine casing welding recipes",com.gregtech.gregtech.content.recipe.MachineCasingRecipes.register()),
                    () -> LOGGER.info("Registered {} original capsule cell extrusion recipes",com.gregtech.gregtech.content.recipe.CapsuleCellRecipes.register()),
                    () -> LOGGER.info("Registered {} original capsule cell recycling recipes",com.gregtech.gregtech.content.recipe.CapsuleCellRecipes.registerRecycling()),
                    () -> LOGGER.info("Registered {} original implosion recipes",com.gregtech.gregtech.content.recipe.ImplosionRecipes.register()),
                    () -> LOGGER.info("Registered {} original anvil recipes",com.gregtech.gregtech.content.tool.AnvilRecipeDefinitions.register()),
                    () -> LOGGER.info("Registered {} stone and tool survival recipes",com.gregtech.gregtech.content.recipe.StoneAndToolSurvivalRecipes.register()),
                    () -> LOGGER.info("Registered {} GT6 masonry variant rows",com.gregtech.gregtech.content.recipe.StoneVariantRecipes.register()),
                    () -> LOGGER.info("Registered {} original electronics recipes", com.gregtech.gregtech.content.recipe.ElectronicsRecipes.register()),
                    () -> LOGGER.info("Registered {} wire working recipes", com.gregtech.gregtech.content.recipe.WireProcessingRecipes.register()),
                    () -> LOGGER.info("Registered {} polymer forming recipes", com.gregtech.gregtech.content.recipe.PolymerFormingRecipes.register()),
                    () -> LOGGER.info("Registered {} graphene nanofabrication recipes", com.gregtech.gregtech.content.recipe.GrapheneNanofabricationRecipes.register()),
                    () -> LOGGER.info("Registered {} original reactor rod recipes", com.gregtech.gregtech.content.nuclear.ReactorRodRecipes.register()),
                    new com.gregtech.gregtech.loaders.c.Loader_Recipes_Alloys(),
                    new com.gregtech.gregtech.loaders.c.Loader_Recipes_Decomp(),
                    new com.gregtech.gregtech.loaders.c.Loader_Recipes_Fuels(),
                    new com.gregtech.gregtech.loaders.c.Loader_Recipes_Chem(),
                    new com.gregtech.gregtech.loaders.c.Loader_Recipes_Fusion(),
                    new com.gregtech.gregtech.loaders.c.Loader_Recipes_Matter(),
                    () -> LOGGER.info("Registered {} vanilla material recovery recipes", com.gregtech.gregtech.content.recipe.VanillaRecoveryRecipes.register()),
                    () -> LOGGER.info("Registered {} vehicle packaging recipes", com.gregtech.gregtech.content.recipe.VehiclePackagingRecipes.register()),
                    // GT6 Loader_Recipes_Handlers:217-550 — generic per-material form conversions
                    // (dust <-> dust block, ingot <-> ingot block, plate -> dense plate, tiny/small piles).
                    // It sits after the hand-written loaders so their specific recipes win collisions,
                    // and before the bulk-transpiled tables so this vetted table wins against them.
                    () -> LOGGER.info("Registered {} GT6 material form conversions",
                            com.gregtech.gregtech.content.recipe.MaterialFormConversionRecipes.register()),
                    // GT6 Loader_Recipes_Handlers:79-112 — the mortar's pulverized-remains rows, gated on
                    // the imported MORTAR / BRITTLE / FOOD material flags (tools/extract_gt6_workability.py).
                    () -> LOGGER.info("Registered {} GT6 mortar grinding recipes",
                            com.gregtech.gregtech.content.recipe.MortarGrindingRecipes.register()),
                    // GT6 Loader_Recipes_Handlers:254-258 + :454-456 — bullets from the Press and their
                    // recovery in the Unboxinator (the press molds are real items in this port).
                    () -> LOGGER.info("Registered {} GT6 ammunition recipes",
                            com.gregtech.gregtech.content.recipe.PressAmmunitionRecipes.register()),
                    // GT6 Loader_Recipes_Handlers:320-359 — the rest of the Welder's prefix rows
                    // (multi-ingots, bolt/stick conversions, rotors) with GT6's easy/hard duration split.
                    () -> LOGGER.info("Registered {} GT6 welder rows",
                            com.gregtech.gregtech.content.recipe.WelderFamilyRecipes.register()),
                    // GT6 Loader_Recipes_Handlers:324-343 — curved plates + a circuit selector weld into
                    // the per-size pipes (item pipes for the materials that have them, fluid pipes else).
                    () -> LOGGER.info("Registered {} GT6 pipe welding rows",
                            com.gregtech.gregtech.content.recipe.PipeRecipes.register()),
                    // GT6 Loader_Recipes_Handlers:113-145 — the sharpening (grindstone) rows: raw tool
                    // heads into finished ones and the grinding rows with pulverized remains.
                    () -> LOGGER.info("Registered {} GT6 sharpening recipes",
                            com.gregtech.gregtech.content.recipe.SharpeningRecipes.register()),
                    // GT6 Loader_Recipes_Handlers:60-77 — autoclave crystal growth from dust and small
                    // dust piles (steam powered, CRYSTALLISABLE flag from the workability import).
                    () -> LOGGER.info("Registered {} GT6 autoclave crystallisation recipes",
                            com.gregtech.gregtech.content.recipe.AutoclaveRecipes.register()),
                    // GT6 Loader_Recipes_Handlers:157-172 — the anvil's ore grinding rows (rock and ore
                    // chunks into dust and piles), gated on MORTAR and GT6's selfcrush() condition.
                    () -> LOGGER.info("Registered {} GT6 anvil shredding recipes",
                            com.gregtech.gregtech.content.recipe.AnvilShreddingRecipes.register()),
                    // GT6 Loader_Recipes_Handlers:152-155 — the generic shredder recycling rows for the
                    // imported RECYCLABLE prefix list (tools/extract_gt6_recyclable_prefixes.py).
                    () -> LOGGER.info("Registered {} GT6 shredder recycling recipes",
                            com.gregtech.gregtech.content.recipe.ShredderRecyclingRecipes.register()),
                    // GT6 Loader_Recipes_Handlers:150-156 — the crusher's gem ladder, boule and rock rows.
                    () -> LOGGER.info("Registered {} GT6 crusher rows",
                            com.gregtech.gregtech.content.recipe.CrusherFamilyRecipes.register()),
                    // GT6 Loader_Recipes_Furnace:137-177 — every ore-processing form (dust, piles, gem
                    // ladder, rock, raw ore, crushed ore) smelts back into its metal in a furnace, plus
                    // the clay family's ceramic firing rows.
                    () -> LOGGER.info("Registered {} GT6 furnace smelting rows",
                            com.gregtech.gregtech.content.recipe.FurnaceSmeltingRecipes.register()),
                    // GT6 MultiItemTechnological:396-403 — the gas laser emitters are filled in the
                    // Canning Machine from the empty emitter plus one unit of the laser gas.
                    () -> LOGGER.info("Registered {} GT6 laser emitter filling recipes",
                            com.gregtech.gregtech.content.recipe.LaserEmitterRecipes.register()),
                    // GT6 MultiItemTechnological:462-482 — the five filled battery cells are canned
                    // together with their electrolyte (H2SO4, distilled water, HCl, HF).
                    () -> LOGGER.info("Registered {} GT6 battery cell filling recipes",
                            com.gregtech.gregtech.content.recipe.BatteryCellRecipes.register()),
                    // GT6_Main:326-403 — the rows the original adds from its own main class: the printer's
                    // book printing, the boxinator's map folding, and the scanner/unboxinator displays.
                    () -> LOGGER.info("Registered {} GT6 main-class recipes",
                            com.gregtech.gregtech.content.recipe.GTMainRecipes.register()),
                    // GT6's material data chain: RecipeMapScannerMolecular / RecipeMapPrinter /
                    // RecipeMapReplicator all override findRecipe() to read the NBT a scanner wrote
                    // onto a USB stick, so the port installs them as dynamic providers.
                    () -> LOGGER.info("Registered GT6 material data chain (scanner/printer/replicator): {}",
                            com.gregtech.gregtech.content.recipe.GTMaterialDataRecipes.register()),
                    // §108: GT6's food recipes that only need items and fluids the port already has -
                    // four families of Loader_Recipes_Food (furnace/slicer/mixer/boxinator).
                    () -> LOGGER.info("Registered {} GT6 food item recipes",
                            com.gregtech.gregtech.content.recipe.FoodItemRecipes.register()),
                    // GT6's registration-pattern hand recipes (pipes, anvils, storage, 1x wires) live in
                    // Loader_HandToolCraftingRecipes: they are injected on ServerStartedEvent, like the
                    // other crafting-grid tables that need the recipe manager.
                    // reviewed against the original by hand, and RecipeMap's collision check keeps the
                    // first recipe registered for a given input. Running the bulk-transpiled data last
                    // therefore means it can only *fill* maps, never displace a vetted recipe.
                    () -> com.gregtech.gregtech.loaders.c.GTGeneratedChem.loadAll()
            );

            for (IGTLoader loader : phaseC) {
                try {
                    loader.run();
                } catch (Exception e) {
                    LOGGER.error("[{}] Phase C loader '{}' failed", MODID, loader.getClass().getSimpleName(), e);
                    throw new IllegalStateException("Phase C failed: " + loader.getClass().getSimpleName(), e);
                }
            }
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
