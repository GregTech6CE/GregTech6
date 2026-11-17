package gregtech.content.data;

import gregtech.content.recipe.RecipeMapSpec;
import gregtech.engine.recipe.RecipeMap;

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

    public static final RecipeMap FluidBed = fromDefinition(FuelRecipeMapDefinitions.FluidBed),
            Burn = fromDefinition(FuelRecipeMapDefinitions.Burn),
            Gas = fromDefinition(FuelRecipeMapDefinitions.Gas),
            Hot = fromDefinition(FuelRecipeMapDefinitions.Hot),
            Plasma = fromDefinition(FuelRecipeMapDefinitions.Plasma),
            Engine = fromDefinition(FuelRecipeMapDefinitions.Engine),
            Turbine = fromDefinition(FuelRecipeMapDefinitions.Turbine),
            Magic = fromDefinition(FuelRecipeMapDefinitions.Magic);

    /** Original fuel display order; shared by consumers of the native fuel maps. */
    public static final java.util.List<RecipeMap> FUEL_MAP_LIST =
            java.util.List.of(FluidBed, Burn, Gas, Hot, Plasma, Engine, Turbine, Magic);

    private static RecipeMap fromDefinition(RecipeMapSpec spec) {
        var map = new RecipeMap(spec);
        registerFuelMap(map);
        return map;
    }

    /** Initializes built-in fuel maps before dependent registries are populated. */
    public static void bootstrap() {
        if (Burn == null) throw new IllegalStateException("Fuel maps failed to initialize");
    }
}
