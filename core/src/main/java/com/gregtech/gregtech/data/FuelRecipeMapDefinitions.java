package com.gregtech.gregtech.data;
import com.gregtech.gregtech.api.recipe.RecipeMapSpec;
/** Exact original GT6 fuel-map schemas; MC stacks belong to the platforms. */
public final class FuelRecipeMapDefinitions {private FuelRecipeMapDefinitions(){}
    private static final String GUI = "gregtech:textures/gui/machines/";

    public static final RecipeMapSpec
            FluidBed = new RecipeMapSpec(null, "gt.recipe.fuels.fluidbed", "Fluidized Bed Fuels", GUI + "Default", 1, 1, 0, 1, 1, 0, 1, false, false, true, true),
            Burn     = new RecipeMapSpec(null, "gt.recipe.fuels.burn",     "Burnable Fuels",      GUI + "Default", 0, 0, 0, 1, 1, 1, 1, false, false, false, false),
            Gas      = new RecipeMapSpec(null, "gt.recipe.fuels.gas",      "Gas Fuels",           GUI + "Default", 0, 0, 0, 1, 2, 1, 1, false, false, false, false),
            Hot      = new RecipeMapSpec(null, "gt.recipe.fuels.hot",      "Hot Fuels",           GUI + "Default", 0, 0, 0, 1, 1, 1, 1, false, false, false, false),
            Plasma   = new RecipeMapSpec(null, "gt.recipe.fuels.plasma",   "Plasma Fuels",        GUI + "Default", 0, 0, 0, 1, 1, 1, 1, false, false, false, false),
            Engine   = new RecipeMapSpec(null, "gt.recipe.fuels.engine",   "Engine Fuels",        GUI + "Default", 0, 0, 0, 1, 1, 1, 1, false, false, false, false),
            Turbine  = new RecipeMapSpec(null, "gt.recipe.fuels.turbine",  "Turbine Fuels",       GUI + "Default", 0, 0, 0, 1, 1, 1, 1, false, false, false, false),
            Magic    = new RecipeMapSpec(null, "gt.recipe.fuels.magic",    "Magic Fuels",         GUI + "Default", 1, 1, 0, 1, 1, 0, 1, false, false, false, false);

}
