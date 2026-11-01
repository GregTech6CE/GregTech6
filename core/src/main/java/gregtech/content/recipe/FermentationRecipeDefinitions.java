package gregtech.content.recipe;
/** Complete original named food/alcohol fermentation rows, retaining durations and fluid fields. */
public final class FermentationRecipeDefinitions {
    private FermentationRecipeDefinitions() {}

    public static final String[][] RECIPES = {
        {"Milk", "Milk_Spoiled", "64"},
        {"Honeydew", "ShortMead", "64"},
        {"Juice_Ananas", "Cider_Ananas", "64"},
        {"Juice_Apple", "Cider_Apple", "64"},
        {"Juice_AppleGrC", "Cider_Apple", "64"},
        {"Cider_Apple", "Vinegar_Apple", "128"},
        {"Mash_Hops", "Beer_Dark", "64"},
        {"Beer_Dark", "Beer_Dragonblood", "128"},
        {"Juice_Reed", "Rum_White", "64"},
        {"Rum_White", "Vinegar_Cane", "128"},
        {"Rum_Dark", "Vinegar_Cane", "128"},
        {"Wine_Grape_Green", "Vinegar_Grape", "128"},
        {"Wine_Grape_Purple", "Vinegar_Grape", "128"},
        {"Juice_Grape_Green", "Wine_Grape_Green", "64"},
        {"Juice_Grape_Purple", "Wine_Grape_Purple", "64"},
        {"Juice_Banana", "Wine_Banana", "64"},
        {"Juice_Carrot", "Wine_Carrot", "64"},
        {"Juice_Lemon", "Wine_Lemon", "64"},
        {"Juice_Cranberry", "Wine_Cranberry", "64"},
        {"Juice_Tomato", "Wine_Tomato", "64"},
    };
}
