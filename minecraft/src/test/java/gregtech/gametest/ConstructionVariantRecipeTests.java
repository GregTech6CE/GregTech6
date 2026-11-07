package gregtech.gametest;

import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.plant.WoodSlabCatalog;
import gregtech.content.registry.GTConstructionVariants;
import gregtech.engine.recipe.Recipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * Every palette block and its slab carry the two grid rows of GT6 {@code BlockMetaType:88-95}, and
 * reinforced concrete keeps its hand row from {@code Loader_Blocks:70}.
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ConstructionVariantRecipeTests {
    private static final String BATCH = "construction_variant_recipes";
    private static final List<String> SLAB_FAMILIES = List.of("glass_clear", "glass_glow", "cfoam");

    private ConstructionVariantRecipeTests() {}

    /** Two slabs rebuild every full block, every full block saws back into two slabs. */
    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void everyConstructionSlabPairHasItsCraftingAndCutterRows(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        var missing = new ArrayList<String>();
        for (String family : SLAB_FAMILIES)
            for (DyeColor color : DyeColor.values())
                pair(helper, recipes, missing, family, color.getName(),
                        GTConstructionVariants.coloredBlock(family, color),
                        GTConstructionVariants.coloredBlock(family + "_slab", color));
        helper.assertTrue(missing.isEmpty(), "missing construction slab rows: " + missing);
        helper.succeed();
    }

    /** The sixteen reinforced-concrete hand rows of GT6 Loader_Blocks:70. */
    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void reinforcedConcreteHasItsHandRows(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        var missing = new ArrayList<String>();
        for (DyeColor color : DyeColor.values()) {
            var id = id("construction/concrete_reinforced/" + color.getName() + "/from_stick_and_concrete");
            if (recipes.byKey(id).isEmpty()) missing.add(id.toString());
        }
        helper.assertTrue(missing.isEmpty(), "missing reinforced concrete rows: " + missing);
        helper.succeed();
    }

    /** GT6 BlockMetaType:91 skips the saw rows for planks but not the two-slabs row. */
    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void everyWoodSlabRebuildsItsPlanks(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        var missing = new ArrayList<String>();
        for (String wood : WoodSlabCatalog.WOODS) {
            var id = id("catalog_repairs/slab_" + wood + "_to_planks");
            if (recipes.byKey(id).isEmpty()) missing.add(id.toString());
        }
        helper.assertTrue(missing.isEmpty(), "missing wood slab to planks rows: " + missing);
        helper.succeed();
    }

    private static void pair(
            GameTestHelper helper, RecipeManager recipes, List<String> missing,
            String family, String key, Block full, Block slab) {
        var base = "construction/" + family + "/" + key;
        helper.assertTrue(full != null && slab != null, base + " has a full block and a slab");
        helper.assertFalse(new ItemStack(full).isEmpty(), base + " has a registered item");
        helper.assertFalse(new ItemStack(slab).isEmpty(), base + " slab has a registered item");
        var toBlock = id(base + "/slabs_to_block");
        var toSlab = id(base + "/block_to_slab_by_saw");
        if (recipes.byKey(toBlock).isEmpty()) missing.add(toBlock.toString());
        if (recipes.byKey(toSlab).isEmpty()) missing.add(toSlab.toString());
        if (!sawsTwoSlabs(new ItemStack(full), new ItemStack(slab))) missing.add(base + " cutter row");
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

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", path);
    }
}
