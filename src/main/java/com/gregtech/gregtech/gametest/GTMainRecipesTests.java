package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.content.book.GTBooks;
import com.gregtech.gregtech.content.recipe.GTMainRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * GT6's main-class rows ({@code GT6_Main.java:326-403}, {@link GTMainRecipes}) — in particular the
 * printer recipe that hands out the "Scanner &amp; Printer Manual": the port's printer map used to be
 * empty, so the machine was unusable and that manual unobtainable outside loot.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class GTMainRecipesTests {

    private static boolean hasInput(Recipe recipe, ItemStack wanted) {
        for (ItemStack stack : recipe.mInputs) {
            if (stack != null && !stack.isEmpty() && stack.is(wanted.getItem())) return true;
        }
        return false;
    }

    private static ItemStack item(String id) {
        var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", id));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** The printer prints the Printer manual from a plain book and black dye. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void printerPrintsThePrinterManual(GameTestHelper h) {
        int rows = MachineRecipeMaps.Printer.mRecipeList.size();
        h.assertTrue(rows >= 1, "the printer recipe map has rows: " + rows);
        boolean found = false;
        for (Recipe recipe : MachineRecipeMaps.Printer.mRecipeList) {
            boolean book = hasInput(recipe, new ItemStack(Items.BOOK));
            boolean dye = false;
            for (var fluid : recipe.mFluidInputs) {
                if (fluid != null && fluid.getAmount() > 0) dye = true;
            }
            for (ItemStack out : recipe.mOutputs) {
                if (out == null || !out.is(Items.WRITTEN_BOOK)) continue;
                String title = out.getTag() == null ? null : out.getTag().getString("title");
                if (title != null && title.equals(GTBooks.titleOf("Manual_Printer")) && book && dye) {
                    found = true;
                }
            }
        }
        h.assertTrue(found, "the printer prints '" + GTBooks.titleOf("Manual_Printer")
                + "' from a book plus dye fluid; rows: " + MachineRecipeMaps.Printer.mRecipeList.size());
        h.succeed();
    }

    /** The boxinator folds eight paper and a compass into a map ({@code GT6_Main:351}). */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void boxinatorFoldsMaps(GameTestHelper h) {
        boolean found = false;
        for (Recipe recipe : MachineRecipeMaps.Boxinator.mRecipeList) {
            boolean paper = false;
            boolean compass = false;
            for (ItemStack stack : recipe.mInputs) {
                if (stack == null || stack.isEmpty()) continue;
                if (stack.is(Items.PAPER) && stack.getCount() == 8) paper = true;
                if (stack.is(Items.COMPASS)) compass = true;
            }
            for (ItemStack out : recipe.mOutputs) {
                if (out != null && out.is(Items.MAP) && paper && compass) found = true;
            }
        }
        h.assertTrue(found, "8 paper + compass -> map is registered in the boxinator");
        h.succeed();
    }

    /** The display rows exist, are marked fake, and the port recorded what it could not show. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void displayRowsAreFake(GameTestHelper h) {
        int rows = GTMainRecipes.entries().size();
        h.assertTrue(rows >= 5, "GT6 main-class rows registered: " + rows + " " + GTMainRecipes.entries()
                + " (skipped: " + GTMainRecipes.skipped() + ")");
        h.assertTrue(GTMainRecipes.skipped().size() >= 5, "rows recorded as skipped: "
                + GTMainRecipes.skipped().size());
        long fake = MachineRecipeMaps.Unboxinator.mRecipeList.stream().filter(r -> r.mFakeRecipe).count();
        h.assertTrue(fake >= 5, "unboxinator display rows: " + fake);
        h.assertTrue(MachineRecipeMaps.ScannerVisuals.mRecipeList.stream().anyMatch(r -> r.mFakeRecipe),
                "the scanner display map has rows again");
        h.succeed();
    }
}
