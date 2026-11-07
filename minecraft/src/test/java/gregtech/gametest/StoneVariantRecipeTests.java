package gregtech.gametest;

import gregtech.content.block.stone.StoneType;
import gregtech.content.block.stone.StoneVariant;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import java.util.List;
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
