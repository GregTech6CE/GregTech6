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
            FluidBed = new RecipeMap(null, "gt.recipe.fuels.fluidbed", "Fluidized Bed Fuels", GUI + "Default", 1, 1, 0, 1, 1, 0, 1, false, false, true, true),
            Burn     = new RecipeMap(null, "gt.recipe.fuels.burn",     "Burnable Fuels",      GUI + "Default", 0, 0, 0, 1, 1, 1, 1, false, false, false, false),
            Gas      = new RecipeMap(null, "gt.recipe.fuels.gas",      "Gas Fuels",           GUI + "Default", 0, 0, 0, 1, 2, 1, 1, false, false, false, false),
            Hot      = new RecipeMap(null, "gt.recipe.fuels.hot",      "Hot Fuels",           GUI + "Default", 0, 0, 0, 1, 1, 1, 1, false, false, false, false),
            Plasma   = new RecipeMap(null, "gt.recipe.fuels.plasma",   "Plasma Fuels",        GUI + "Default", 0, 0, 0, 1, 1, 1, 1, false, false, false, false),
            Engine   = new RecipeMap(null, "gt.recipe.fuels.engine",   "Engine Fuels",        GUI + "Default", 0, 0, 0, 1, 1, 1, 1, false, false, false, false),
            Turbine  = new RecipeMap(null, "gt.recipe.fuels.turbine",  "Turbine Fuels",       GUI + "Default", 0, 0, 0, 1, 1, 1, 1, false, false, false, false),
            Magic    = new RecipeMap(null, "gt.recipe.fuels.magic",    "Magic Fuels",         GUI + "Default", 1, 1, 0, 1, 1, 0, 1, false, false, false, false);

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
