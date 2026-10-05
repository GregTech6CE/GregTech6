package com.gregtech.gregtech.data;

import com.gregtech.gregtech.api.recipe.RecipeMap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fuel recipe maps, ported from GT6 {@code gregapi.data.FM}.
 *
 * <p>Fuel "recipes" hold one fluid (or item) input, optional exhaust outputs, and a
 * negative {@code mEUt} (energy produced per tick) with {@code mDuration} = burn ticks
 * per recipe charge — total energy per charge is {@code |EUt| * duration}. Generators
 * look their fuel up here; JEI lists each non-empty map as a category.</p>
 */
public class FuelRecipeMaps {
    protected FuelRecipeMaps() {}

    private static final String GUI = "gregtech:textures/gui/machines/";

    public static final RecipeMap
            FluidBed = fromDefinition(FuelRecipeMapDefinitions.FluidBed),
            Burn = fromDefinition(FuelRecipeMapDefinitions.Burn),
            Gas = fromDefinition(FuelRecipeMapDefinitions.Gas),
            Hot = fromDefinition(FuelRecipeMapDefinitions.Hot),
            Plasma = fromDefinition(FuelRecipeMapDefinitions.Plasma),
            Engine = fromDefinition(FuelRecipeMapDefinitions.Engine),
            Turbine = fromDefinition(FuelRecipeMapDefinitions.Turbine),
            Magic = fromDefinition(FuelRecipeMapDefinitions.Magic);

    private static com.gregtech.gregtech.api.recipe.RecipeMap fromDefinition(com.gregtech.gregtech.api.recipe.RecipeMapSpec spec) {
        var map=new com.gregtech.gregtech.api.recipe.RecipeMap(null,spec.mNameInternal,spec.mNameLocal,spec.mGUIPath,
                spec.mInputItemsCount,spec.mOutputItemsCount,spec.mMinimalInputItems,
                spec.mInputFluidCount,spec.mOutputFluidCount,spec.mMinimalInputFluids,spec.mMinimalInputs,
                spec.mNeedsOutputs,spec.mCombinePower,spec.mUseBucketSizeIn,spec.mUseBucketSizeOut);
        map.viewer(spec.mViewerAllowed,spec.mShowVoltageAmperage);
        map.specialValueLabel(spec.mSpecialValuePre,spec.mSpecialValueMultiplier,spec.mSpecialValuePost);
        if(spec.mInstantRecipes)map.instantRecipes();return map;
    }

    // ── Legacy stub API (kept for the transpiled data references) ──────────

    public record MapEntry(String unlocalizedName, String displayName) {}

    private static final Map<String, MapEntry> REGISTRY = new LinkedHashMap<>();

    private static MapEntry map(String field, String key, String display) {
        MapEntry entry = new MapEntry(key, display);
        REGISTRY.put(field, entry);
        return entry;
    }

    public static final MapEntry
            Diesel = map("Diesel", "gt.recipe.diesel", "Diesel Generator"),
            GasEntry = map("Gas", "gt.recipe.gas", "Gas Turbine"),
            HotFluid = map("HotFluid", "gt.recipe.hotfluid", "Hot Fluid Generator");

    public static Map<String, MapEntry> all() {
        return Map.copyOf(REGISTRY);
    }

    public static void bootstrap() {
        if (REGISTRY.isEmpty() || Burn == null) {
            throw new IllegalStateException("FM failed to initialize");
        }
    }
}
