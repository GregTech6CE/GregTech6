package gregtech.gametest;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.recipe.generated.ChemRecipeResolver;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTTechnological;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.recipe.chem.ChemOutcome;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * One representative row per migrated source table, plus the per-family ledger disposition.
 *
 * <p>The rows below are transcribed from the same source calls as the producers; a family that
 * silently stops producing rows fails its assertion instead of disappearing from the log.</p>
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class GeneratedRecipeTablesTests {
    private static final String BATCH = "generated_recipe_tables";

    private GeneratedRecipeTablesTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void otherRowIsQueryable(GameTestHelper helper) {
        ItemStack copper = stack("dust", "Cu", 3);
        ItemStack tin = stack("dust", "Sn", 1);
        ItemStack compound = tech("bronze_compound", 36);
        helper.assertFalse(compound.isEmpty(), "bronze compound item");
        Recipe row = find(MachineRecipeMaps.Mixer, List.of(copper, tin), List.of());
        helper.assertTrue(row != null, "bronze compound source row");
        helper.assertTrue(hasOutput(row, compound), "3 Cu + 1 Sn yields 36 bronze compound");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void compoundPrefixMatrixIsQueryable(GameTestHelper helper) {
        ItemStack tinyCopper = stack("dustTiny", "Cu", 3);
        ItemStack nuggetTin = stack("nugget", "Sn", 1);
        ItemStack compound = tech("bronze_compound", 4);
        helper.assertFalse(compound.isEmpty(), "bronze compound item");
        helper.assertTrue(find(MachineRecipeMaps.Mixer, List.of(tinyCopper, nuggetTin), List.of()) != null,
                "the tiny/nugget compound matrix is registered");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void vanillaHammerRowIsQueryable(GameTestHelper helper) {
        Recipe row = find(MachineRecipeMaps.Hammer,
                List.of(new ItemStack(Items.BRICKS)), List.of());
        helper.assertTrue(row != null, "bricks hammer row");
        helper.assertTrue(hasOutput(row, new ItemStack(Items.BRICK, 3)), "bricks yield three bricks");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void foodFermenterRowIsQueryable(GameTestHelper helper) {
        ItemStack raw = stack("dust", "MeatRaw", 1);
        ItemStack rotten = stack("dust", "MeatRotten", 1);
        helper.assertFalse(raw.isEmpty(), "raw meat dust");
        helper.assertFalse(rotten.isEmpty(), "rotten meat dust");
        Recipe row = find(MachineRecipeMaps.Fermenter, List.of(raw), List.of());
        helper.assertTrue(row != null, "raw meat fermentation row");
        helper.assertTrue(hasOutput(row, rotten), "raw meat ferments to rotten meat");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void extruderModuleRowIsQueryable(GameTestHelper helper) {
        ItemStack module = tech("module_stone_generator", 1);
        ItemStack shape = tech("extruder_shape_plate", 1);
        ItemStack plate = stack("plate", "Stone", 1);
        helper.assertFalse(module.isEmpty() && shape.isEmpty(), "extruder module and shape");
        helper.assertFalse(plate.isEmpty(), "stone plate");
        Recipe row = find(MachineRecipeMaps.Extruder, List.of(module, shape), List.of());
        helper.assertTrue(row != null, "stone generator plate row");
        helper.assertTrue(hasOutput(row, plate), "the module extrudes a stone plate");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void oreSluiceRowIsQueryable(GameTestHelper helper) {
        ItemStack sand = stack("dust", "SluiceSand", 1);
        FluidStack water = new FluidStack(Fluids.WATER, 900);
        FluidStack sluice = fluid("Sluice", 900);
        helper.assertFalse(sand.isEmpty() && sluice.isEmpty(), "sluice sand and fluid");
        Recipe row = find(MachineRecipeMaps.Mixer, List.of(sand), List.of(water));
        helper.assertTrue(row != null, "sluice mixer row");
        helper.assertTrue(hasFluidOutput(row, sluice), "one sluice sand and 900 mB water yield 900 mB sluice");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void combRowIsQueryable(GameTestHelper helper) {
        ItemStack comb = tech("honey_comb", 1);
        FluidStack honey = fluid("Honey", 100);
        helper.assertFalse(comb.isEmpty() && honey.isEmpty(), "honey comb and honey fluid");
        Recipe row = find(MachineRecipeMaps.Centrifuge, List.of(comb), List.of());
        helper.assertTrue(row != null, "honey comb centrifuge row");
        helper.assertTrue(hasFluidOutput(row, honey), "the honey comb yields 100 mB honey");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void technologyRowIsQueryable(GameTestHelper helper) {
        ItemStack plate = stack("plateTiny", "Steel", 1);
        ItemStack tip = tech("robot_arm_wrench_tip", 1);
        ItemStack wrench = tech("single_use_wrench", 1);
        helper.assertFalse(plate.isEmpty() && tip.isEmpty(), "tool head and tip");
        helper.assertFalse(wrench.isEmpty(), "single-use wrench");
        Recipe row = find(MachineRecipeMaps.Boxinator, List.of(plate, tip), List.of());
        helper.assertTrue(row != null, "wrench boxinator row");
        helper.assertTrue(hasOutput(row, wrench), "the wrench box replaces the tool head");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void temporaryRowIsQueryable(GameTestHelper helper) {
        ItemStack graphite = stack("dust", "Graphite", 1);
        ItemStack shape = tech("extruder_shape_rod", 1);
        ItemStack stick = stack("stick", "Graphite", 1);
        helper.assertFalse(graphite.isEmpty() && shape.isEmpty(), "graphite dust and rod shape");
        helper.assertFalse(stick.isEmpty(), "graphite stick");
        Recipe row = find(MachineRecipeMaps.Extruder, List.of(graphite, shape), List.of());
        helper.assertTrue(row != null, "graphite electrode row");
        helper.assertTrue(hasOutput(row, stick), "one graphite dust extrudes one rod");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void everyFamilyPublishesRows(GameTestHelper helper) {
        for (String family : new String[] {
                "Loader_Recipes_Other:", "Loader_Recipes_Vanilla:", "Loader_Recipes_Food:",
                "Loader_Recipes_Extruder:", "Loader_Recipes_Ores:", "Loader_Recipes_Temporary:",
                "MultiItemFood:", "MultiItemRandomTools:"}) {
            helper.assertTrue(published(family) > 0, "family publishes rows: " + family);
        }
        helper.succeed();
    }

    private static int published(String family) {
        int count = 0;
        for (var result : ChemRecipeResolver.results()) {
            if (!result.source().contains(family)) continue;
            if (result.outcome() == ChemOutcome.PUBLISHED || result.outcome() == ChemOutcome.SUBSTITUTED)
                count++;
        }
        return count;
    }

    private static Recipe find(RecipeMap map, List<ItemStack> items, List<FluidStack> fluids) {
        return map.findRecipe(items, fluids, false, map.mInputItemsCount, map.mOutputItemsCount);
    }

    private static boolean hasOutput(Recipe recipe, ItemStack expected) {
        for (ItemStack output : recipe.mOutputs)
            if (output.is(expected.getItem()) && output.getCount() == expected.getCount()) return true;
        return false;
    }

    private static boolean hasFluidOutput(Recipe recipe, FluidStack expected) {
        for (FluidStack output : recipe.mFluidOutputs)
            if (!output.isEmpty() && output.isFluidEqual(expected) && output.getAmount() == expected.getAmount())
                return true;
        return false;
    }

    private static ItemStack stack(String prefixName, String materialName, int count) {
        GTMaterial material = GTMaterialRegistry.get(materialName);
        if (material == null || !material.resolve().isValid()) return ItemStack.EMPTY;
        MaterialPrefix prefix = null;
        for (MaterialPrefix candidate : gregtech.engine.prefix.PrefixRegistry.all())
            if (candidate.getName().equals(prefixName)) prefix = candidate;
        return prefix == null ? ItemStack.EMPTY : GTItems.getStack(prefix, material.resolve(), count);
    }

    private static ItemStack tech(String id, int count) {
        Item item = GTTechnological.get(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static FluidStack fluid(String key, int millibuckets) {
        FluidStack stack = GTFluids.stack(key, millibuckets);
        return stack == null ? FluidStack.EMPTY : stack;
    }
}
