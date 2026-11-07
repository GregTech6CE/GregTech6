package gregtech.gametest;

import gregtech.content.block.stone.StoneType;
import gregtech.content.block.stone.StoneVariant;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * The masonry furnace rows: the block of rock dust smelts back into the plain stone, while the same
 * material's loose dust keeps smelting into the material's own smelting target.
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class StoneVariantRecipeTests {
    private static final String BATCH = "stone_variant_recipes";

    private StoneVariantRecipeTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void dustBlockSmeltsIntoStoneWhileLooseDustKeepsItsTarget(GameTestHelper helper) {
        for (StoneType type : List.of(StoneType.LIMESTONE, StoneType.MARBLE)) {
            var stone = GTBlocks.getStone(type, StoneVariant.STONE);
            helper.assertTrue(stone != null, type.registryId() + " has a plain stone block");

            var dustBlock = GTBlocks.getStack(BlockMaterialPrefix.blockDust, type.material());
            helper.assertFalse(dustBlock.isEmpty(), type.registryId() + " has a blockDust block");
            var blockRow = find(dustBlock);
            helper.assertTrue(blockRow != null, type.registryId() + " blockDust smelts");
            helper.assertTrue(isOutput(blockRow, new ItemStack(stone)),
                    type.registryId() + " blockDust smelts into its stone block");

            var dust = GTItems.getStack(MaterialPrefix.dust, type.material(), 1);
            helper.assertFalse(dust.isEmpty(), type.registryId() + " has a loose dust");
            var single = find(dust);
            var stacked = find(dust.copyWithCount(9));
            helper.assertTrue(single != null, type.registryId() + " dust smelts");
            helper.assertTrue(stacked != null, type.registryId() + " nine dusts smelt");
            helper.assertTrue(sameOutput(stacked, single),
                    type.registryId() + " nine dusts smelt into a different result than one");
            helper.assertFalse(isOutput(single, new ItemStack(stone)),
                    type.registryId() + " loose dust smelts into its stone block");
        }
        helper.succeed();
    }

    /** Every full masonry block rebuilds from two slabs, saws into two slabs, and has both rows. */
    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void everyMasonryBlockHasItsSlabRows(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        var missing = new ArrayList<String>();
        for (StoneType type : StoneType.values()) {
            for (StoneVariant variant : StoneVariant.values()) {
                var full = GTBlocks.getStone(type, variant);
                var slab = GTBlocks.getStoneSlab(type, variant);
                helper.assertTrue(full != null && slab != null,
                        type.registryId() + "_" + variant.registrySuffix() + " has a block and a slab");
                var base = "stone/" + type.registryId() + "/" + variant.registrySuffix();
                if (recipes.byKey(ResourceLocation.fromNamespaceAndPath("gt6ce", base + "_to_slab_by_saw"))
                        .isEmpty()) missing.add(base + "_to_slab_by_saw");
                if (recipes.byKey(ResourceLocation.fromNamespaceAndPath("gt6ce", base + "_slabs_to_block"))
                        .isEmpty()) missing.add(base + "_slabs_to_block");
                if (!sawsTwoSlabs(new ItemStack(full), new ItemStack(slab))) missing.add(base + " cutter row");
            }
        }
        helper.assertTrue(missing.isEmpty(), "missing masonry slab rows: " + missing);
        helper.succeed();
    }

    /** Whether the Cutter turns the full block into exactly two of its slabs. */
    private static boolean sawsTwoSlabs(ItemStack full, ItemStack slab) {
        for (Recipe recipe : MachineRecipeMaps.Cutter.mRecipeList) {
            if (recipe.mInputs.length != 1 || !recipe.mInputs[0].is(full.getItem())) continue;
            for (ItemStack output : recipe.mOutputs)
                if (output.is(slab.getItem()) && output.getCount() == 2) return true;
        }
        return false;
    }

    private static Recipe find(ItemStack input) {
        return MachineRecipeMaps.Furnace.findRecipe(List.of(input), List.of(), false, 1, 1);
    }

    private static boolean isOutput(Recipe recipe, ItemStack expected) {
        return recipe.mOutputs.length > 0 && recipe.mOutputs[0].is(expected.getItem())
                && recipe.mOutputs[0].getCount() == expected.getCount();
    }

    private static boolean sameOutput(Recipe first, Recipe second) {
        return first.mOutputs.length > 0 && second.mOutputs.length > 0
                && first.mOutputs[0].getItem() == second.mOutputs[0].getItem()
                && first.mOutputs[0].getCount() == second.mOutputs[0].getCount();
    }
}
