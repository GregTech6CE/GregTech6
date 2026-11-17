package gregtech.content.data;

import gregtech.content.recipe.RecipeMapSpec;

/** Exact original GT6 fuel-map schemas; MC stacks belong to the platforms. */
public final class FuelRecipeMapDefinitions {
    private FuelRecipeMapDefinitions() {}

    private static final String GUI = "gt6ce:textures/gui/machines/";

    public static final RecipeMapSpec FluidBed = RecipeMapSpec.builder(
                    "gt.recipe.fuels.fluidbed", "Fluidized Bed Fuels", GUI + "Default")
            .items(1, 1, 0)
            .fluids(1, 1, 0)
            .minimumInputs(1)
            .bucketInput(true)
            .bucketOutput(true)
            .build();

    public static final RecipeMapSpec Burn = RecipeMapSpec.builder(
                    "gt.recipe.fuels.burn", "Burnable Fuels", GUI + "Default")
            .items(0, 0, 0)
            .fluids(1, 1, 1)
            .minimumInputs(1)
            .build();

    public static final RecipeMapSpec Gas = RecipeMapSpec.builder("gt.recipe.fuels.gas", "Gas Fuels", GUI + "Default")
            .items(0, 0, 0)
            .fluids(1, 2, 1)
            .minimumInputs(1)
            .build();

    public static final RecipeMapSpec Hot = RecipeMapSpec.builder("gt.recipe.fuels.hot", "Hot Fuels", GUI + "Default")
            .items(0, 0, 0)
            .fluids(1, 1, 1)
            .minimumInputs(1)
            .build();

    public static final RecipeMapSpec Plasma = RecipeMapSpec.builder(
                    "gt.recipe.fuels.plasma", "Plasma Fuels", GUI + "Default")
            .items(0, 0, 0)
            .fluids(1, 1, 1)
            .minimumInputs(1)
            .build();

    public static final RecipeMapSpec Engine = RecipeMapSpec.builder(
                    "gt.recipe.fuels.engine", "Engine Fuels", GUI + "Default")
            .items(0, 0, 0)
            .fluids(1, 1, 1)
            .minimumInputs(1)
            .build();

    public static final RecipeMapSpec Turbine = RecipeMapSpec.builder(
                    "gt.recipe.fuels.turbine", "Turbine Fuels", GUI + "Default")
            .items(0, 0, 0)
            .fluids(1, 1, 1)
            .minimumInputs(1)
            .build();

    public static final RecipeMapSpec Magic = RecipeMapSpec.builder(
                    "gt.recipe.fuels.magic", "Magic Fuels", GUI + "Default")
            .items(1, 1, 0)
            .fluids(1, 1, 0)
            .minimumInputs(1)
            .build();
}
