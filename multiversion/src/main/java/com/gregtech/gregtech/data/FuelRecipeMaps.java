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

    private static final String GUI = "gt6ce:textures/gui/machines/";
    private static final java.util.Set<String> FUEL_NAMES = new java.util.LinkedHashSet<>();

    /** True for a built-in fuel map and for a fuel map an addon declared. */
    public static boolean isFuelMap(String name) {
        return name != null && FUEL_NAMES.contains(name);
    }

    /** Records a map that stores produced energy as a negative {@code mEUt}. */
    public static void registerFuelMap(RecipeMap map) {
        if (map != null && map.mNameInternal != null) FUEL_NAMES.add(map.mNameInternal);
    }

    /**
     * Datapack rows write a positive {@code eu_per_tick}. On a fuel map that number is the energy
     * produced per tick, stored negative so {@code FluidFuelBatch} can burn the row.
     */
    public static long storedEu(RecipeMap map, long euPerTick) {
        if (map != null && isFuelMap(map.mNameInternal) && euPerTick > 0) return -euPerTick;
        return euPerTick;
    }

    public static final RecipeMap
            FluidBed = fromDefinition(FuelRecipeMapDefinitions.FluidBed),
            Burn = fromDefinition(FuelRecipeMapDefinitions.Burn),
            Gas = fromDefinition(FuelRecipeMapDefinitions.Gas),
            Hot = fromDefinition(FuelRecipeMapDefinitions.Hot),
            Plasma = fromDefinition(FuelRecipeMapDefinitions.Plasma),
            Engine = fromDefinition(FuelRecipeMapDefinitions.Engine),
            Turbine = fromDefinition(FuelRecipeMapDefinitions.Turbine),
            Magic = fromDefinition(FuelRecipeMapDefinitions.Magic);

    /** Original fuel display order; shared by consumers of the native fuel maps. */
    public static final java.util.List<RecipeMap> FUEL_MAP_LIST = java.util.List.of(
            FluidBed, Burn, Gas, Hot, Plasma, Engine, Turbine, Magic);

    private static com.gregtech.gregtech.api.recipe.RecipeMap fromDefinition(com.gregtech.gregtech.api.recipe.RecipeMapSpec spec) {
        var map=new com.gregtech.gregtech.api.recipe.RecipeMap(null,spec.mNameInternal,spec.mNameLocal,spec.mGUIPath,
                spec.mInputItemsCount,spec.mOutputItemsCount,spec.mMinimalInputItems,
                spec.mInputFluidCount,spec.mOutputFluidCount,spec.mMinimalInputFluids,spec.mMinimalInputs,
                spec.mNeedsOutputs,spec.mCombinePower,spec.mUseBucketSizeIn,spec.mUseBucketSizeOut);
        map.viewer(spec.mViewerAllowed,spec.mShowVoltageAmperage);
        map.specialValueLabel(spec.mSpecialValuePre,spec.mSpecialValueMultiplier,spec.mSpecialValuePost);
        if(spec.mInstantRecipes)map.instantRecipes();
        registerFuelMap(map);
        return map;
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
