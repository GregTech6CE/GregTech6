package gregtech.gametest;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.recipe.generated.GeneratedRecipeResolver;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * The imported tables' {@code i:blockDust} rows resolve to blocks of dust, not to nine dusts, so
 * they neither duplicate nor shadow the same material's loose-dust rows.
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class GeneratedRecipeImportTests {
    private static final String BATCH = "generated_recipe_imports";

    private GeneratedRecipeImportTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void blockDustImportsKeepTheirBlockForm(GameTestHelper helper) {
        // Roasting: one sulfur block next to the loose-dust row, both with their own air volume.
        var sulfur = GeneratedRecipeResolver.material("S");
        helper.assertTrue(sulfur != null, "sulfur is a known material");
        var sulfurBlock = blockDust(sulfur);
        helper.assertFalse(sulfurBlock.isEmpty(), "sulfur has a blockDust block");
        helper.assertTrue(firstInput(MachineRecipeMaps.Roasting, sulfurBlock, 1) != null,
                "the sulfur blockDust roasting row is registered");
        var sulfurDust = GTItems.getStack(MaterialPrefix.dust, sulfur, 1);
        helper.assertFalse(sulfurDust.isEmpty(), "sulfur has a loose dust");
        helper.assertTrue(firstInput(MachineRecipeMaps.Roasting, sulfurDust, 1) != null,
                "the loose sulfur dust roasting row is still registered");
        boolean blockAir = false;
        for (Recipe row : MachineRecipeMaps.Roasting.mRecipeList) {
            for (ItemStack input : row.mInputs) {
                helper.assertTrue(!input.is(sulfurDust.getItem()) || input.getCount() == 1,
                        "no roasting row consumes several sulfur dusts as a block stand-in");
            }
            if (row.mInputs.length > 0 && row.mInputs[0].is(sulfurBlock.getItem())
                    && row.mFluidInputs.length > 0 && row.mFluidInputs[0].getAmount() == 72000) {
                blockAir = true;
            }
        }
        helper.assertTrue(blockAir, "the sulfur block row keeps the original air volume");

        // Mixer: the sodium hydroxide row is two blocks, next to the loose dust row it used to duplicate.
        var hydroxide = GeneratedRecipeResolver.material("NaOH");
        helper.assertTrue(hydroxide != null, "sodium hydroxide is a known material");
        var hydroxideBlock = blockDust(hydroxide);
        helper.assertFalse(hydroxideBlock.isEmpty(), "sodium hydroxide has a blockDust block");
        var hydroxideBlockRow = firstInput(MachineRecipeMaps.Mixer, hydroxideBlock, 2);
        helper.assertTrue(hydroxideBlockRow != null, "the sodium hydroxide blockDust mixer row is registered");
        var hydroxideDust = GTItems.getStack(MaterialPrefix.dust, hydroxide, 18);
        helper.assertTrue(firstInput(MachineRecipeMaps.Mixer, hydroxideDust, 18) != null,
                "the loose dust mixer row is still registered");
        helper.succeed();
    }

    private static ItemStack blockDust(GTMaterial material) {
        return material == null ? ItemStack.EMPTY : GTBlocks.getStack(BlockMaterialPrefix.blockDust, material);
    }

    /** First row of the map whose first input is {@code item} in the given count, or null. */
    private static Recipe firstInput(RecipeMap map, ItemStack expected, int count) {
        if (expected.isEmpty()) return null;
        for (Recipe row : map.mRecipeList) {
            if (!row.mEnabled || row.mFakeRecipe || row.mInputs.length == 0) continue;
            ItemStack input = row.mInputs[0];
            if (input.is(expected.getItem()) && input.getCount() == count) return row;
        }
        return null;
    }
}
