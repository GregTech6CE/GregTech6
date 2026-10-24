package gregtech.content.recipe;

import gregtech.internal.mod.SetupOnce;
import gregtech.bootstrap.InitializationStep;
import gregtech.content.nuclear.ReactorRodRecipes;
import gregtech.content.tool.AnvilRecipeDefinitions;
import gregtech.content.recipe.generated.GeneratedRecipeResolver;
import gregtech.data.FuelRecipeMaps;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.function.IntSupplier;

/**
 * Ordered machine-recipe initialization shared by both loaders.
 * Run after material bindings; addon recipes-ready callbacks run after this completes.
 * The two conditional steps preserve the platforms' established initialization order.
 */
public final class MachineRecipeBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SetupOnce SETUP = new SetupOnce();

    private MachineRecipeBootstrap() {}

    public static void load() {
        SETUP.run(MachineRecipeBootstrap::loadRecipes);
    }

    private static void loadRecipes() {
        runStep("OreProcessingRecipeLoader", new OreProcessingRecipeLoader());
        runStep("PartRecipeLoader", new PartRecipeLoader());
        register("VanillaProcessingRecipes", VanillaProcessingRecipes::register);
        register("DiggableRecipes", DiggableRecipes::register);
        register("VanillaBlockProcessingRecipes", VanillaBlockProcessingRecipes::register);
        register("VanillaWoodProcessingRecipes", VanillaWoodProcessingRecipes::register);
        register("RegisteredWoodSurvivalRecipes", RegisteredWoodSurvivalRecipes::register);
        register("CropProcessingRecipes", CropProcessingRecipes::register);
        register("DyeProcessingRecipes", DyeProcessingRecipes::register);
        register("TextileFinishingRecipes", TextileFinishingRecipes::register);
        register("BiologicalMaterialRecipes", BiologicalMaterialRecipes::register);
        register("GlowtusProcessingRecipes", GlowtusProcessingRecipes::register);
        register("BedrockFlowerProcessingRecipes", BedrockFlowerProcessingRecipes::register);
        register("SurvivalUtilityRecipes", SurvivalUtilityRecipes::register);
        // NeoForge binds fermentation earlier, before material post-initialization.
//? if >=1.21.1 {
//?} else {
        register("FermenterFoodRecipes", FermenterFoodRecipes::register);
//?}
        register("CrucibleSmeltingRecipes", CrucibleSmeltingRecipes::register);
        register("StructuralPartRecipes", StructuralPartRecipes::register);
        register("MachineCasingRecipes", MachineCasingRecipes::register);
        register("CapsuleCellRecipes", CapsuleCellRecipes::register);
        register("PowerCellRecipes", PowerCellRecipes::register);
        register("CapsuleCellRecipes.registerRecycling", CapsuleCellRecipes::registerRecycling);
        register("ImplosionRecipes", ImplosionRecipes::register);
        register("AnvilRecipeDefinitions", AnvilRecipeDefinitions::register);
        register("StoneAndToolSurvivalRecipes", StoneAndToolSurvivalRecipes::register);
        register("StoneVariantRecipes", StoneVariantRecipes::register);
        register("ElectronicsRecipes", ElectronicsRecipes::register);
        register("WireProcessingRecipes", WireProcessingRecipes::register);
        register("PolymerFormingRecipes", PolymerFormingRecipes::register);
        register("GrapheneNanofabricationRecipes", GrapheneNanofabricationRecipes::register);
        register("ReactorRodRecipes", ReactorRodRecipes::register);
        runStep("AlloyRecipeLoader", new AlloyRecipeLoader());
        runStep("DecompositionRecipeLoader", new DecompositionRecipeLoader());
        // Forge initializes fuel maps with its base data. NeoForge does so here.
//? if >=1.21.1 {
        /*runStep("fuel maps", FuelRecipeMaps::bootstrap);
*///?}
        runStep("FuelRecipeLoader", new FuelRecipeLoader());
        runStep("ChemicalRecipeLoader", new ChemicalRecipeLoader());
        runStep("FusionRecipeLoader", new FusionRecipeLoader());
        runStep("MatterRecipeLoader", new MatterRecipeLoader());
        register("VanillaRecoveryRecipes", VanillaRecoveryRecipes::register);
        register("VehiclePackagingRecipes", VehiclePackagingRecipes::register);

        // Specific recipes win collisions. Generic form handlers follow them,
        // and the imported bulk tables remain last.
        register("MaterialFormConversionRecipes", MaterialFormConversionRecipes::register);
        register("MortarGrindingRecipes", MortarGrindingRecipes::register);
        register("PressAmmunitionRecipes", PressAmmunitionRecipes::register);
        register("WelderFamilyRecipes", WelderFamilyRecipes::register);
        register("PipeRecipes", PipeRecipes::register);
        register("SharpeningRecipes", SharpeningRecipes::register);
        register("AutoclaveRecipes", AutoclaveRecipes::register);
        register("AnvilShreddingRecipes", AnvilShreddingRecipes::register);
        register("ShredderRecyclingRecipes", ShredderRecyclingRecipes::register);
        register("CrusherFamilyRecipes", CrusherFamilyRecipes::register);
        register("FurnaceSmeltingRecipes", FurnaceSmeltingRecipes::register);
        register("LaserEmitterRecipes", LaserEmitterRecipes::register);
        register("BatteryCellRecipes", BatteryCellRecipes::register);
        register("GTMainRecipes", GTMainRecipes::register);
        register("GTMaterialDataRecipes", GTMaterialDataRecipes::register);
        register("FoodItemRecipes", FoodItemRecipes::register);

        // Imported rows only fill gaps; never run these before the explicit recipes.
        runStep("imported recipe tables", GeneratedRecipeResolver::loadAll);
    }

    private static void register(String name, IntSupplier registration) {
        runStep(name, () -> LOGGER.info("Registered {} recipes from {}", registration.getAsInt(), name));
    }

    private static void runStep(String name, InitializationStep step) {
        try {
            step.run();
        } catch (RuntimeException failure) {
            throw new IllegalStateException("Machine recipe initialization failed: " + name, failure);
        }
    }
}
