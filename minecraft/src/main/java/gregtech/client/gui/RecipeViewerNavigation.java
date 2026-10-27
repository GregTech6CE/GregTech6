package gregtech.client.gui;

import gregtech.engine.recipe.RecipeMap;
import java.util.function.Predicate;

/** Optional viewers install callbacks; machine screens never load JEI or EMI classes. */
public final class RecipeViewerNavigation {
    private static Predicate<RecipeMap> emi = map -> false;
    private static Predicate<RecipeMap> jei = map -> false;
    private static boolean emiAvailable, jeiAvailable;

    private RecipeViewerNavigation() {}

    public static void registerEmi(Predicate<RecipeMap> opener) { emi = opener; emiAvailable = true; }
    public static void registerJei(Predicate<RecipeMap> opener) { jei = opener; jeiAvailable = true; }
    public static void clearJei() { jei = map -> false; jeiAvailable = false; }
    public static boolean available() { return emiAvailable || jeiAvailable; }

    public static boolean open(RecipeMap map) {
        return map != null && map.mViewerAllowed && (emi.test(map) || jei.test(map));
    }
}
