package gregtech.engine.recipe;

import gregtech.api.addon.recipe.MaterialUnitIngredient;
import java.util.Collection;

/**
 * The single publication entry for every producer that adds rows to a machine recipe map: built-in
 * content, datapack and KubeJS markers, and addon callbacks. All of them pass the same priority,
 * collision and provenance handling here, so the reload lifecycle and the diagnostics observe one
 * contract instead of per-entry-point copies.
 *
 * <p>A row's {@link Recipe#mPriority} is its explicit claim on a machine input. Higher wins at
 * lookup time; equal priorities keep the row published first, never the backing collection's
 * iteration order. {@link Recipe#mSource} records which entry point published the row and is shown
 * by {@link RecipeDiagnostics} and the recipe dump.</p>
 */
public final class MachineRecipeAdapter {
    public static final String SOURCE_BUILT_IN = "built-in";
    public static final String SOURCE_DATAPACK = "datapack";
    public static final String SOURCE_ADDON = "addon";
    /**
     * Addon providers use the neutral priority because the addon API does not expose a priority
     * field; they are ordered among themselves by the order the addon registers them.
     */
    public static final long ADDON_DYNAMIC_PRIORITY = 0L;

    private MachineRecipeAdapter() {}

    /** Publishes a static row with the map's ordinary collision and validation rules. */
    public static Recipe addStatic(RecipeMap map, Recipe recipe, String source) {
        return addStatic(map, recipe, source, true, false, false, map.mLogErrors);
    }

    /** Publishes a static or display row with explicit registration flags. */
    public static Recipe addStatic(
            RecipeMap map,
            Recipe recipe,
            String source,
            boolean checkForCollisions,
            boolean fake,
            boolean hidden,
            boolean logErrors) {
        if (map == null || recipe == null) return null;
        recipe.withSource(source);
        if (fake) return map.addFakeRecipe(checkForCollisions, recipe);
        return map.addRecipe(recipe, checkForCollisions, false, hidden, logErrors);
    }

    /** Publishes the material-unit rule a row declares; the map stores the generated batches. */
    public static Recipe addMaterial(
            RecipeMap map, Recipe template, int slot, MaterialUnitIngredient ingredient, String source) {
        template.withSource(source);
        return MaterialUnitRecipes.add(map, template, slot, ingredient);
    }

    /** Registers a dynamic provider at an explicit priority; see {@link RecipeMap#addDynamicRecipes(long, RecipeMap.DynamicRecipes)}. */
    public static void addDynamic(RecipeMap map, long priority, RecipeMap.DynamicRecipes provider) {
        map.addDynamicRecipes(priority, provider);
    }

    /** Removes rows through the single deletion entry, keeping every index in step. */
    public static int remove(RecipeMap map, Collection<Recipe> rows) {
        return map.removeAll(rows);
    }
}
