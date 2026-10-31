package gregtech.gametest;

import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import java.util.ArrayList;
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
 * Registration contracts of {@link RecipeMap}: the single deletion entry, added-row capture and
 * the collision candidates. Each case registers a scratch map and drops it again.
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class RecipeMapRegistrationTests {
    private static final String BATCH = "recipe_map_registration";
    private static final ItemStack[] NO_ITEMS = new ItemStack[0];
    private static final FluidStack[] NO_FLUIDS = new FluidStack[0];

    private RecipeMapRegistrationTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void removalEntryClearsEveryIndex(GameTestHelper helper) {
        var map = map(helper, "gt.test.recipe.removal");
        try {
            var itemRow = row(new ItemStack[] {stack(Items.IRON_INGOT, 1)}, NO_FLUIDS);
            var fluidRow = row(new ItemStack[] {stack(Items.COPPER_INGOT, 1)}, new FluidStack[] {fluid(Fluids.WATER, 100)});
            helper.assertTrue(map.addRecipe(itemRow) == itemRow, "item row registered");
            helper.assertTrue(map.addRecipe(fluidRow) == fluidRow, "fluid row registered");
            helper.assertTrue(map.mRecipeItemMap.get(Items.IRON_INGOT).contains(itemRow), "item index holds the row");
            helper.assertTrue(map.mRecipeFluidMap.get(waterKey()).contains(fluidRow), "fluid index holds the row");

            helper.assertTrue(map.removeAll(List.of(itemRow, fluidRow)) == 2, "both rows removed");
            helper.assertTrue(map.mRecipeList.isEmpty(), "no stored rows remain");
            helper.assertTrue(map.mRecipeItemMap.isEmpty(), "item index is empty");
            helper.assertTrue(map.mRecipeFluidMap.isEmpty(), "fluid index is empty");
            helper.assertTrue(map.removeAll(List.of(itemRow)) == 0, "a second removal is a no-op");
            helper.assertFalse(map.unlink(itemRow), "unlink reports a missing row");
        } finally {
            RecipeMap.unregister("gt.test.recipe.removal");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void removalEntryDropsEveryFluidIndexEntry(GameTestHelper helper) {
        var map = map(helper, "gt.test.recipe.removal.zero");
        try {
            // 1.20.1 Forge normalises a zero-sized stack to an empty one, so a native zero-amount
            // representative only exists on platforms that keep it; both must end without an entry.
            var zero = fluid(Fluids.WATER, 0);
            var row = row(NO_ITEMS, new FluidStack[] {zero});
            helper.assertTrue(map.addRecipe(row) == row, "catalyst row registered");
            if (zero.getFluid() != Fluids.EMPTY)
                helper.assertTrue(map.mRecipeFluidMap.get(waterKey()) != null, "indexed by its fluid");
            helper.assertTrue(map.removeAll(List.of(row)) == 1, "row removed");
            helper.assertTrue(map.mRecipeFluidMap.isEmpty(), "no fluid index entry survives the removal");
        } finally {
            RecipeMap.unregister("gt.test.recipe.removal.zero");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void captureRecordsOnlySurvivingNewRows(GameTestHelper helper) {
        var map = map(helper, "gt.test.recipe.capture");
        try {
            var first = row(new ItemStack[] {stack(Items.IRON_INGOT, 1)}, NO_FLUIDS);
            var second = row(new ItemStack[] {stack(Items.GOLD_INGOT, 1)}, NO_FLUIDS);
            var third = row(new ItemStack[] {stack(Items.COPPER_INGOT, 1)}, NO_FLUIDS);
            var dropped = row(new ItemStack[] {stack(Items.BRICK, 1)}, NO_FLUIDS);

            var captured = new ArrayList<Recipe>();
            map.captureAdditions(captured, () -> {
                map.add(first);
                map.add(first);
                map.add(dropped);
                map.unlink(dropped);
            });
            helper.assertTrue(captured.equals(List.of(first)), "duplicate add and dropped row are not additions: " + captured);

            var outer = new ArrayList<Recipe>();
            var inner = new ArrayList<Recipe>();
            map.captureAdditions(outer, () -> {
                map.captureAdditions(inner, () -> map.add(second));
                map.add(third);
            });
            helper.assertTrue(inner.equals(List.of(second)), "inner scope sees its own addition: " + inner);
            helper.assertTrue(outer.equals(List.of(second, third)), "outer scope sees nested additions: " + outer);

            var aborted = new ArrayList<Recipe>();
            boolean thrown = false;
            try {
                map.captureAdditions(aborted, () -> {
                    map.add(row(new ItemStack[] {stack(Items.EMERALD, 1)}, NO_FLUIDS));
                    throw new IllegalStateException("probe");
                });
            } catch (IllegalStateException expected) {
                thrown = true;
            }
            helper.assertTrue(thrown, "the failure propagates");
            helper.assertTrue(aborted.isEmpty(), "a failing scope records nothing");

            var foreign = new ArrayList<Recipe>();
            var other = row(new ItemStack[] {stack(Items.QUARTZ, 1)}, NO_FLUIDS);
            map.captureAdditions(foreign, () -> {
                var thread = new Thread(() -> map.add(other), "capture-probe");
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            });
            helper.assertTrue(foreign.isEmpty(), "another thread's row is not captured: " + foreign);
            helper.assertTrue(map.mRecipeList.contains(other), "the other thread's row is still registered");
        } finally {
            RecipeMap.unregister("gt.test.recipe.capture");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void collisionKeepsCountsAndDistinctInputs(GameTestHelper helper) {
        var map = map(helper, "gt.test.recipe.collision");
        try {
            var existing = row(new ItemStack[] {stack(Items.IRON_INGOT, 2)}, NO_FLUIDS);
            helper.assertTrue(map.addRecipe(existing) == existing, "base row registered");

            var smaller = row(new ItemStack[] {stack(Items.IRON_INGOT, 1)}, NO_FLUIDS);
            helper.assertTrue(map.addRecipe(smaller) == smaller, "a smaller count is not covered");

            var covered = row(new ItemStack[] {stack(Items.IRON_INGOT, 2)}, NO_FLUIDS);
            helper.assertTrue(map.addRecipe(covered) == null, "the same count is covered");

            var extra = row(new ItemStack[] {stack(Items.IRON_INGOT, 2), stack(Items.GOLD_INGOT, 1)}, NO_FLUIDS);
            helper.assertTrue(map.addRecipe(extra) == null, "an extra input still covers the base row");

            var unrelated = row(new ItemStack[] {stack(Items.GOLD_INGOT, 1)}, NO_FLUIDS);
            helper.assertTrue(map.addRecipe(unrelated) == unrelated, "an unrelated input is accepted");
        } finally {
            RecipeMap.unregister("gt.test.recipe.collision");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void collisionFollowsInPlaceInputChange(GameTestHelper helper) {
        var map = map(helper, "gt.test.recipe.collision.mutation");
        try {
            var existing = row(new ItemStack[] {stack(Items.IRON_INGOT, 1), stack(Items.GOLD_INGOT, 1)}, NO_FLUIDS);
            helper.assertTrue(map.addRecipe(existing) == existing, "base row registered");

            existing.mInputs[1] = stack(Items.COPPER_INGOT, 1);
            map.invalidateLookup();

            var replaced = row(new ItemStack[] {stack(Items.IRON_INGOT, 1), stack(Items.COPPER_INGOT, 1)}, NO_FLUIDS);
            helper.assertTrue(map.addRecipe(replaced) == null, "the changed row still covers the new inputs");

            var original = row(new ItemStack[] {stack(Items.IRON_INGOT, 1), stack(Items.GOLD_INGOT, 1)}, NO_FLUIDS);
            helper.assertTrue(map.addRecipe(original) == original, "the old input is no longer covered");
        } finally {
            RecipeMap.unregister("gt.test.recipe.collision.mutation");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void collisionKeepsPresenceAndAnvilRules(GameTestHelper helper) {
        var presence = map(helper, "gt.test.recipe.collision.presence");
        try {
            var existing = row(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                    new FluidStack[] {fluid(Fluids.WATER, 1)}).withPresenceFluidInputs(0);
            helper.assertTrue(presence.addRecipe(existing) == existing, "presence row registered");

            var withoutFluid = row(new ItemStack[] {stack(Items.IRON_INGOT, 1)}, NO_FLUIDS);
            helper.assertTrue(presence.addRecipe(withoutFluid) == withoutFluid, "a missing presence gate is not covered");

            var withFluid = row(new ItemStack[] {stack(Items.IRON_INGOT, 1)}, new FluidStack[] {fluid(Fluids.WATER, 0)});
            helper.assertTrue(presence.addRecipe(withFluid) == null, "a present fluid covers the gate");
        } finally {
            RecipeMap.unregister("gt.test.recipe.collision.presence");
        }

        var anvil = map(helper, "gt.recipe.anvil.test");
        try {
            var existing = row(new ItemStack[] {stack(Items.IRON_INGOT, 1), ItemStack.EMPTY}, NO_FLUIDS);
            helper.assertTrue(anvil.addRecipe(existing) == existing, "anvil row registered");

            var compact = row(new ItemStack[] {stack(Items.IRON_INGOT, 1)}, NO_FLUIDS);
            helper.assertTrue(anvil.addRecipe(compact) == compact, "a different empty work area is not covered");

            var sameSlots = row(new ItemStack[] {stack(Items.IRON_INGOT, 1), ItemStack.EMPTY}, NO_FLUIDS);
            helper.assertTrue(anvil.addRecipe(sameSlots) == null, "the same empty work area is covered");
        } finally {
            RecipeMap.unregister("gt.recipe.anvil.test");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void collisionHandlesZeroAmountFluids(GameTestHelper helper) {
        var zero = fluid(Fluids.WATER, 0);
        if (zero.getFluid() == Fluids.EMPTY) {
            // This platform stores no native zero-amount representative; nothing can be reached that way.
            helper.succeed();
            return;
        }
        var map = map(helper, "gt.test.recipe.collision.zero");
        try {
            var catalyst = row(NO_ITEMS, new FluidStack[] {zero});
            helper.assertTrue(map.addRecipe(catalyst) == catalyst, "zero-amount catalyst row registered");

            var otherItem = row(new ItemStack[] {stack(Items.IRON_INGOT, 1)}, NO_FLUIDS);
            helper.assertTrue(map.addRecipe(otherItem) == otherItem, "an unrelated row does not reach it");

            var sameFluid = row(NO_ITEMS, new FluidStack[] {fluid(Fluids.WATER, 0)});
            helper.assertTrue(map.addRecipe(sameFluid) == null, "a shared fluid reaches it");
        } finally {
            RecipeMap.unregister("gt.test.recipe.collision.zero");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void collisionFallsBackForManyInputs(GameTestHelper helper) {
        var map = map(helper, "gt.test.recipe.collision.fallback", 12);
        try {
            var existing = row(new ItemStack[] {stack(Items.IRON_INGOT, 1)}, NO_FLUIDS);
            helper.assertTrue(map.addRecipe(existing) == existing, "base row registered");

            var wide = new ItemStack[] {
                    stack(Items.IRON_INGOT, 1), stack(Items.GOLD_INGOT, 1), stack(Items.COPPER_INGOT, 1),
                    stack(Items.BRICK, 1), stack(Items.EMERALD, 1), stack(Items.REDSTONE, 1),
                    stack(Items.LAPIS_LAZULI, 1), stack(Items.AMETHYST_SHARD, 1), stack(Items.QUARTZ, 1),
                    stack(Items.COAL, 1), stack(Items.DIAMOND, 1)};
            var candidate = row(wide, NO_FLUIDS);
            helper.assertTrue(map.addRecipe(candidate) == null, "the pool scan still finds the base row");

            var unrelated = row(new ItemStack[] {stack(Items.GOLD_INGOT, 1), stack(Items.COAL, 1)}, NO_FLUIDS);
            helper.assertTrue(map.addRecipe(unrelated) == unrelated, "an unrelated wide row is accepted");
        } finally {
            RecipeMap.unregister("gt.test.recipe.collision.fallback");
        }
        helper.succeed();
    }

    private static RecipeMap map(GameTestHelper helper, String name) {
        return map(helper, name, 9);
    }

    private static RecipeMap map(GameTestHelper helper, String name, int inputSlots) {
        helper.assertTrue(RecipeMap.RECIPE_MAPS.get(name) == null, "scratch map name is free: " + name);
        return new RecipeMap(null, name, "Test", "textures/gui/test", inputSlots, 9, 0, 9, 9, 0, 0,
                false, false, false, false);
    }

    private static Recipe row(ItemStack[] inputs, FluidStack[] fluidInputs) {
        return new Recipe(inputs, new ItemStack[] {stack(Items.GOLD_INGOT, 1)}, null, null,
                fluidInputs, NO_FLUIDS, 20, 16, 0);
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    private static FluidStack fluid(net.minecraft.world.level.material.Fluid fluid, int amount) {
        return new FluidStack(fluid, amount);
    }

    private static String waterKey() {
//? if >=1.21.1 {
/*        return "minecraft:water";
*///?} else {
        return Fluids.WATER.builtInRegistryHolder().key().location().toString();
//?}
    }
}
