package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * GT6's food/alcohol fermentation chain (original {@code Loader_Recipes_Food:605-649}).
 * <p>
 * The port only had the two chemical fermenter recipes ({@code Biomass → Methane}), so the
 * Fermenter machine and the sealed barrel had almost nothing to do: every fermentation route
 * from GT6's food chain — milk → spoiled milk, juice → cider/wine, cider → vinegar,
 * mash → beer/spirits, honeydew → short mead — was missing.
 * </p>
 * <p>
 * Every recipe is fluid-in/fluid-out at 16 EU/t (GT6's fermenter rate), which is also the shape
 * the barrel uses: {@code TileEntityBase08Barrel} looks the machine recipe up by fluid alone.
 * </p>
 */
public final class FermenterFoodRecipes {
    private FermenterFoodRecipes() {}

    /** GT6 {@code Loader_Recipes_Food:605-645}: input fluid, output fluid, duration in ticks. */
    // GT6's two remaining food recipes (Loader_Recipes_Food:614-615) ferment the GregTech-Craft
    // potion fluids "potion.goldenapplejuice" and "potion.idunsapplejuice"; this port has no such
    // fluids, so they are recorded as missing content rather than invented.
    private static final String[][] RECIPES = FermentationRecipeDefinitions.RECIPES;

    /** GT6's fermenter rate: 16 EU/t for every one of these recipes. */
    private static final long EU_T = 16;
    /** GT6 registers 50 mB in for every food fermentation. */
    private static final int INPUT_MB = 50;
    /** The standard output is half the input; the vinegar/spirit steps yield a fifth. */
    private static final int OUTPUT_MB = 25;
    private static final int SLOW_OUTPUT_MB = 10;

    private static final TreeSet<String> MISSING = new TreeSet<>();

    /** Fluid fields of the original recipes this port has no fluid for. */
    public static TreeSet<String> missingFluids() { return new TreeSet<>(MISSING); }

    /** Registers the chain and returns how many recipes were added. */
    public static int register() {
        MISSING.clear();
        List<String> wines = flagged(RegisteredFluids.FluidFlags.WINE);
        List<String> fruitJuices = flagged(RegisteredFluids.FluidFlags.FRUIT_JUICE);

        int count = 0;
        for (String[] entry : RECIPES) {
            int duration = Integer.parseInt(entry[2]);
            int outMb = duration == 128 ? SLOW_OUTPUT_MB : OUTPUT_MB;
            if (add(entry[0], entry[1], INPUT_MB, outMb, duration)) count++;
        }

        // GT6 Loader_Recipes_Food:646-649 — every wine can be fortified and every fruit juice
        // becomes "Wine_Fruit". GT6 drives those two groups from FluidsGT.WINE / FRUIT_JUICE,
        // which in this port are the WINE / FRUIT_JUICE fluid flags, and uses the same 50 mB
        // input, so the map's collision check drops the group recipe wherever GT6 already has a
        // dedicated one for that fluid (juice → cider, wine → vinegar).
        for (String wine : wines) {
            if (isFluid(wine, "Wine_Fortified")) continue;
            if (add(wine, "Wine_Fortified", INPUT_MB, SLOW_OUTPUT_MB, 128)) count++;
        }
        for (String juice : fruitJuices) {
            if (add(juice, "Wine_Fruit", INPUT_MB, OUTPUT_MB, 64)) count++;
        }
        return count;
    }

    private static boolean add(String inField, String outField, int inMb, int outMb, int duration) {
        FluidStack input = GTFluids.stack(inField, inMb);
        FluidStack output = GTFluids.stack(outField, outMb);
        if (input == null || output == null) {
            if (input == null) MISSING.add(inField);
            if (output == null) MISSING.add(outField);
            return false;
        }
        return addStack(input, output, duration);
    }

    private static boolean addStack(FluidStack input, FluidStack output, int duration) {
        // GT6's food recipes carry ST.tag(0) — the circuit selector with damage 0 — as their item
        // input, exactly like the transpiled chemical fermenter recipes. The port's RecipeMap
        // requires at least one input stack, and keeping the original marker is what the chemical
        // recipes already do, so both fermenter sources stay consistent.
        Item item = com.gregtech.gregtech.registry.GTTechnological.selectorTag(0);
        ItemStack[] inputs = item == null
                ? RecipeMap.ZL_IS : new ItemStack[]{new ItemStack(item)};
        // addRecipeX returns null when the map rejects the recipe (validation or collision); only
        // a stored recipe counts as registered.
        return MachineRecipeMaps.Fermenter.addRecipeX(true, EU_T, duration, inputs, input, output,
                RecipeMap.ZL_IS) != null;
    }

    private static boolean isFluid(String field, String otherField) {
        FluidStack stack = GTFluids.stack(field, 1);
        FluidStack other = GTFluids.stack(otherField, 1);
        return stack != null && other != null && other.getFluid() == stack.getFluid();
    }

    /** Field names of every registered fluid carrying a flag (GT6's group-driven recipes). */
    private static List<String> flagged(long flag) {
        // Sorted so the group loops are deterministic: the recipe map's collision check keeps the
        // first of two recipes with the same input fluid, and the fluid registry is a hash map.
        TreeSet<String> result = new TreeSet<>();
        for (var entry : RegisteredFluids.all().entrySet()) {
            if (entry.getValue().hasFlag(flag)) result.add(entry.getKey());
        }
        return new ArrayList<>(result);
    }
}
