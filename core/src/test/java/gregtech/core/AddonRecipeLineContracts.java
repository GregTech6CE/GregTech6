package gregtech.core;

import gregtech.api.addon.RecipeKeptInputs;
import gregtech.api.addon.SelectorTags;
import gregtech.internal.recipe.RecipeCatalystInputs;

/** Kept item and fluid inputs, and programmed-circuit numbers. No Minecraft types. */
public final class AddonRecipeLineContracts {
    private static int assertions;

    private AddonRecipeLineContracts() {}

    public static void main(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException("Expected no arguments");
        unmarked();
        marks();
        rejected();
        fluids();
        selector();
        System.out.println("addon recipe line contracts passed: " + assertions);
    }

    private static void unmarked() {
        check(RecipeKeptInputs.normalize(null, 2).length == 0, "an omitted mark list is empty");
        check(RecipeKeptInputs.normalize(new int[0], 2).length == 0, "an empty mark list is empty");
        check(!RecipeCatalystInputs.isCatalyst(false, false, false), "an unmarked ordinary input is consumed");
        RecipeKeptInputs.requirePositiveCount(1, 0);
        assertions++;
    }

    private static void marks() {
        int[] source = {1, 0};
        int[] kept = RecipeKeptInputs.normalize(source, 3);
        source[0] = 9;
        check(kept.length == 2 && kept[0] == 1 && kept[1] == 0, "marks keep caller order and are copied");
        boolean[] marked = new boolean[3];
        for (int index : kept) marked[index] = true;
        check(RecipeCatalystInputs.isCatalyst(false, marked[1], false), "marked index is a catalyst input");
        check(RecipeCatalystInputs.isCatalyst(false, marked[0], false), "marked index is a catalyst input");
        check(!RecipeCatalystInputs.isCatalyst(false, marked[2], false), "an unmarked slot beside marks is consumed");
        check(RecipeCatalystInputs.isCatalyst(false, false, true), "a selector circuit is a catalyst without a mark");
        check(RecipeCatalystInputs.isCatalyst(false, true, true), "marking a selector circuit stays a catalyst");
        check(!RecipeCatalystInputs.isCatalyst(true, false, true), "explicit-only mode consumes an unmarked catalyst item");
        check(RecipeCatalystInputs.isCatalyst(true, true, false), "an explicit mark is a catalyst when explicit-only is on");
        check(RecipeCatalystInputs.isCatalyst(false, true, false) && RecipeCatalystInputs.isCatalyst(true, true, true),
                "a mark is a catalyst either way");
    }

    private static void rejected() {
        expectThrows(() -> RecipeKeptInputs.normalize(new int[]{3}, 3), "outside");
        expectThrows(() -> RecipeKeptInputs.normalize(new int[]{-1}, 1), "outside");
        expectThrows(() -> RecipeKeptInputs.normalize(new int[]{0}, 0), "outside");
        expectThrows(() -> RecipeKeptInputs.normalize(new int[]{0, 0}, 1), "Duplicate");
        expectThrows(() -> RecipeKeptInputs.normalize(new int[]{1, 0, 1}, 2), "Duplicate");
        expectThrows(() -> RecipeKeptInputs.requirePositiveCount(0, 1), "keptInputs");
        expectThrows(() -> RecipeKeptInputs.requirePositiveCount(-2, 0), "keptInputs");
    }

    private static void fluids() {
        check(RecipeKeptInputs.normalizeFluids(null, 2).length == 0, "an omitted fluid mark list is empty");
        int[] source = {1, 0};
        int[] kept = RecipeKeptInputs.normalizeFluids(source, 3);
        source[0] = 9;
        check(kept.length == 2 && kept[0] == 1 && kept[1] == 0, "fluid marks keep caller order and are copied");
        check(RecipeKeptInputs.settle(true, 100, 4, 100) == 100, "a kept fluid stays when the tank holds the declared amount");
        check(RecipeKeptInputs.settle(true, 100, 4, 250) == 250, "a kept fluid is not drained");
        check(RecipeKeptInputs.settle(true, 100, 1, 99) == -1, "a short kept fluid does not match");
        check(RecipeKeptInputs.settle(false, 100, 2, 200) == 0, "an ordinary fluid is drained for the batch");
        check(RecipeKeptInputs.fluidDemand(true, 100, 4) == 100, "a kept fluid is checked once");
        check(RecipeKeptInputs.fluidDemand(false, 100, 4) == 400, "an ordinary fluid scales with the batch");
        expectThrows(() -> RecipeKeptInputs.normalizeFluids(new int[]{3}, 3), "outside");
        expectThrows(() -> RecipeKeptInputs.normalizeFluids(new int[]{0, 0}, 1), "Duplicate");
        RecipeKeptInputs.requirePositiveFluid(1, 2);
        assertions++;
        expectThrows(() -> RecipeKeptInputs.requirePositiveFluid(0, 2), "amount");
        check(RecipeKeptInputs.fluidDemand(false, 0, 4) == 0, "textual zero has no batch demand; native identity uses a separate presence mark");
        expectThrows(() -> RecipeKeptInputs.requirePositiveFluid(-4, 0), "amount");
    }

    private static void selector() {
        check(SelectorTags.require(0) == 0 && SelectorTags.require(24) == 24, "selector tags run from 0 through 24");
        expectThrows(() -> SelectorTags.require(-1), "outside");
        expectThrows(() -> SelectorTags.require(25), "outside");
    }

    private static void expectThrows(Runnable action, String part) {
        assertions++;
        try {
            action.run();
        } catch (IllegalArgumentException failure) {
            String text = failure.getMessage() == null ? "" : failure.getMessage();
            if (!text.contains(part)) throw new AssertionError(part + " but was: " + text, failure);
            return;
        }
        throw new AssertionError(part);
    }

    private static void check(boolean valid, String message) {
        assertions++;
        if (!valid) throw new AssertionError(message);
    }
}
