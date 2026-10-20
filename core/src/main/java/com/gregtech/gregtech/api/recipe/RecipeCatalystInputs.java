package com.gregtech.gregtech.api.recipe;

/** Whether one recipe input is retained. Explicit marks win; catalyst items count only when the recipe does not list marks exclusively. */
public final class RecipeCatalystInputs {
    private RecipeCatalystInputs() {}

    /** The predicate {@code Recipe.isCatalystInput} applies to one slot. */
    public static boolean isCatalyst(boolean explicitCatalystsOnly, boolean marked, boolean itemCatalyst) {
        return marked || !explicitCatalystsOnly && itemCatalyst;
    }
}
