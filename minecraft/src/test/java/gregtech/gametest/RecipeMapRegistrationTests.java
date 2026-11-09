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

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void collisionKeepsConditionVariantsAndRejectsDuplicates(GameTestHelper helper) {
        var map = map(helper, "gt.test.recipe.conditions");
        try {
            var base = row(new ItemStack[] {stack(Items.IRON_INGOT, 1)}, NO_FLUIDS);
            helper.assertTrue(map.addRecipe(base) == base, "base row registered");

            var differentPower = new Recipe(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                    new ItemStack[] {stack(Items.GOLD_INGOT, 1)}, null, null, NO_FLUIDS, NO_FLUIDS, 20, 32, 0);
            helper.assertTrue(map.addRecipe(differentPower) == differentPower,
                    "a different energy is a legitimate condition variant");

            var differentSpecial = new Recipe(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                    new ItemStack[] {stack(Items.GOLD_INGOT, 1)}, null, null, NO_FLUIDS, NO_FLUIDS, 20, 16, 300);
            helper.assertTrue(map.addRecipe(differentSpecial) == differentSpecial,
                    "a different special value is a legitimate condition variant");

            var duplicate = new Recipe(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                    new ItemStack[] {stack(Items.GOLD_INGOT, 1)}, null, null, NO_FLUIDS, NO_FLUIDS, 20, 16, 0);
            helper.assertTrue(map.addRecipe(duplicate) == null,
                    "the same inputs and conditions are still rejected as a duplicate");
        } finally {
            RecipeMap.unregister("gt.test.recipe.conditions");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void lookupPrefersExplicitPriorityOverRegistrationOrder(GameTestHelper helper) {
        var map = map(helper, "gt.test.recipe.priority");
        try {
            var high = new Recipe(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                    new ItemStack[] {stack(Items.DIAMOND, 1)}, null, null, NO_FLUIDS, NO_FLUIDS, 20, 16, 0)
                    .withPriority(10);
            var low = new Recipe(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                    new ItemStack[] {stack(Items.GOLD_INGOT, 1)}, null, null, NO_FLUIDS, NO_FLUIDS, 20, 16, 0);
            // Register the winner first, then the loser: the answer must not depend on either order.
            helper.assertTrue(map.addRecipe(high) == high, "high-priority row registered");
            helper.assertTrue(map.addRecipe(low) == low, "low-priority row is a distinct variant");

            var found = map.findRecipe(java.util.List.of(stack(Items.IRON_INGOT, 1)), java.util.List.of(),
                    false, 9, 9);
            helper.assertTrue(found == high, "the explicit priority wins regardless of order: " + found);

            var second = map(helper, "gt.test.recipe.priority.reverse");
            try {
                var laterHigh = new Recipe(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                        new ItemStack[] {stack(Items.DIAMOND, 1)}, null, null, NO_FLUIDS, NO_FLUIDS, 20, 16, 0)
                        .withPriority(10);
                var reverseLow = new Recipe(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                        new ItemStack[] {stack(Items.GOLD_INGOT, 1)}, null, null, NO_FLUIDS, NO_FLUIDS, 20, 16, 0);
                helper.assertTrue(second.addRecipe(reverseLow) == reverseLow, "low-priority row registered first");
                helper.assertTrue(second.addRecipe(laterHigh) == laterHigh, "high-priority row registered later");
                var reverse = second.findRecipe(java.util.List.of(stack(Items.IRON_INGOT, 1)), java.util.List.of(),
                        false, 9, 9);
                helper.assertTrue(reverse == laterHigh, "priority beats later publication order: " + reverse);
            } finally {
                RecipeMap.unregister("gt.test.recipe.priority.reverse");
            }
        } finally {
            RecipeMap.unregister("gt.test.recipe.priority");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void dynamicProvidersFollowPriorityAndFallThrough(GameTestHelper helper) {
        var map = map(helper, "gt.test.recipe.dynamic.priority");
        try {
            var lowRow = new Recipe(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                    new ItemStack[] {stack(Items.GOLD_INGOT, 1)}, null, null, NO_FLUIDS, NO_FLUIDS, 20, 16, 0);
            var highRow = new Recipe(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                    new ItemStack[] {stack(Items.DIAMOND, 1)}, null, null, NO_FLUIDS, NO_FLUIDS, 20, 16, 0);
            map.addDynamicRecipes(0, (items, fluids) -> lowRow);
            map.addDynamicRecipes(10, (items, fluids) -> highRow);
            helper.assertTrue(java.util.Arrays.equals(new long[] {10, 0}, map.dynamicProviderPriorities()),
                    "providers are ordered by priority: " + java.util.Arrays.toString(map.dynamicProviderPriorities()));

            var found = map.findRecipe(java.util.List.of(stack(Items.IRON_INGOT, 1)), java.util.List.of(),
                    false, 9, 9);
            helper.assertTrue(found == highRow, "the higher-priority provider answers first: " + found);

            // A provider whose row does not fit the actual input must not hide a later provider.
            var other = map(helper, "gt.test.recipe.dynamic.fallthrough");
            try {
                var mismatched = new Recipe(new ItemStack[] {stack(Items.GOLD_INGOT, 1)},
                        new ItemStack[] {stack(Items.DIAMOND, 1)}, null, null, NO_FLUIDS, NO_FLUIDS, 20, 16, 0);
                var fallback = new Recipe(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                        new ItemStack[] {stack(Items.GOLD_INGOT, 1)}, null, null, NO_FLUIDS, NO_FLUIDS, 20, 16, 0);
                other.addDynamicRecipes(10, (items, fluids) -> mismatched);
                other.addDynamicRecipes(0, (items, fluids) -> fallback);
                var resolved = other.findRecipe(java.util.List.of(stack(Items.IRON_INGOT, 1)), java.util.List.of(),
                        false, 9, 9);
                helper.assertTrue(resolved == fallback, "an unusable high-priority answer falls through: " + resolved);
            } finally {
                RecipeMap.unregister("gt.test.recipe.dynamic.fallthrough");
            }
        } finally {
            RecipeMap.unregister("gt.test.recipe.dynamic.priority");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void adapterPublishesDatapackAndAddonRowsWithProvenance(GameTestHelper helper) {
        var map = map(helper, "gt.test.recipe.adapter");
        try {
            var datapack = new Recipe(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                    new ItemStack[] {stack(Items.GOLD_INGOT, 1)}, null, null, NO_FLUIDS, NO_FLUIDS, 20, 16, 0)
                    .withPriority(5);
            helper.assertTrue(gregtech.engine.recipe.MachineRecipeAdapter.addStatic(
                            map, datapack, "datapack:test") == datapack, "datapack row published");
            helper.assertTrue("datapack:test".equals(datapack.mSource),
                    "datapack provenance recorded: " + datapack.mSource);
            helper.assertTrue(datapack.mPriority == 5, "datapack priority recorded");

            var addon = new Recipe(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                    new ItemStack[] {stack(Items.COPPER_INGOT, 1)}, null, null, NO_FLUIDS, NO_FLUIDS, 20, 16, 0);
            helper.assertTrue(gregtech.engine.recipe.MachineRecipeAdapter.addStatic(
                            map, addon, gregtech.engine.recipe.MachineRecipeAdapter.SOURCE_ADDON) == addon,
                    "an addon row with another priority is a distinct variant");
            helper.assertTrue(gregtech.engine.recipe.MachineRecipeAdapter.SOURCE_ADDON.equals(addon.mSource),
                    "addon provenance recorded: " + addon.mSource);
            var found = map.findRecipe(java.util.List.of(stack(Items.IRON_INGOT, 1)), java.util.List.of(),
                    false, 9, 9);
            helper.assertTrue(found == datapack, "the datapack priority outranks the neutral addon row: " + found);

            var duplicate = new Recipe(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                    new ItemStack[] {stack(Items.GOLD_INGOT, 1)}, null, null, NO_FLUIDS, NO_FLUIDS, 20, 16, 0)
                    .withPriority(5);
            helper.assertTrue(gregtech.engine.recipe.MachineRecipeAdapter.addStatic(
                            map, duplicate, gregtech.engine.recipe.MachineRecipeAdapter.SOURCE_ADDON) == null,
                    "the adapter still rejects identical conditions");

            var dynamicMap = map(helper, "gt.test.recipe.adapter.dynamic");
            try {
                var low = new Recipe(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                        new ItemStack[] {stack(Items.GOLD_INGOT, 1)}, null, null, NO_FLUIDS, NO_FLUIDS, 20, 16, 0);
                var high = new Recipe(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                        new ItemStack[] {stack(Items.DIAMOND, 1)}, null, null, NO_FLUIDS, NO_FLUIDS, 20, 16, 0);
                gregtech.engine.recipe.MachineRecipeAdapter.addDynamic(dynamicMap, 1, (items, fluids) -> low);
                gregtech.engine.recipe.MachineRecipeAdapter.addDynamic(dynamicMap, 9, (items, fluids) -> high);
                helper.assertTrue(java.util.Arrays.equals(
                                new long[] {9, 1}, dynamicMap.dynamicProviderPriorities()),
                        "adapter dynamic providers keep priority order: "
                                + java.util.Arrays.toString(dynamicMap.dynamicProviderPriorities()));
                helper.assertTrue(dynamicMap.findRecipe(
                                java.util.List.of(stack(Items.IRON_INGOT, 1)), java.util.List.of(), false, 9, 9) == high,
                        "the higher-priority adapter provider answers first");
            } finally {
                RecipeMap.unregister("gt.test.recipe.adapter.dynamic");
            }
        } finally {
            RecipeMap.unregister("gt.test.recipe.adapter");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void removalMatchesRowsRegardlessOfPriorityAndSource(GameTestHelper helper) {
        var recipe = new Recipe(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                new ItemStack[] {stack(Items.GOLD_INGOT, 1)}, null, null, NO_FLUIDS, NO_FLUIDS, 20, 16, 0)
                .withPriority(7).withSource("datapack:test");
        var match = removal("""
                {"map":"gt.test.recipe.removal.priority",
                 "item_inputs":[{"item":"minecraft:iron_ingot"}],
                 "item_outputs":[{"item":"minecraft:gold_ingot"}]}
                """);
        helper.assertTrue(gregtech.engine.recipe.data.MachineRecipeMatch.matches(match, view(recipe), NO_TAGS),
                "priority and provenance do not keep a row out of a datapack removal");
        var other = removal("""
                {"map":"gt.test.recipe.removal.priority",
                 "item_inputs":[{"item":"minecraft:iron_ingot"}],
                 "item_outputs":[{"item":"minecraft:diamond"}]}
                """);
        helper.assertFalse(gregtech.engine.recipe.data.MachineRecipeMatch.matches(other, view(recipe), NO_TAGS),
                "a different output still fails the removal condition");
        helper.succeed();
    }

    private static gregtech.api.addon.recipe.MachineRecipeRemovalDocument removal(String json) {
        return gregtech.api.addon.recipe.MachineRecipeDocuments.removal(
                gregtech.engine.recipe.data.JsonMaps.object(json));
    }

    private static final gregtech.engine.recipe.data.MachineRecipeMatch.TagQuery NO_TAGS =
            new gregtech.engine.recipe.data.MachineRecipeMatch.TagQuery() {
                public boolean itemInTag(String itemId, String tagId) { return false; }
            };

    private static gregtech.engine.recipe.data.MachineRecipeMatch.RecipeView view(Recipe recipe) {
        var inputs = new java.util.ArrayList<ItemStack>();
        for (var input : recipe.mInputs) if (input != null && !input.isEmpty()) inputs.add(input);
        var outputs = new java.util.ArrayList<ItemStack>();
        for (var output : recipe.mOutputs) if (output != null && !output.isEmpty()) outputs.add(output);
        return new gregtech.engine.recipe.data.MachineRecipeMatch.RecipeView() {
            public int inputItems() { return inputs.size(); }
            public String inputItem(int index) {
                return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(inputs.get(index).getItem()).toString();
            }
            public int inputItemCount(int index) { return inputs.get(index).getCount(); }
            public int inputFluids() { return 0; }
            public String inputFluid(int index) { return ""; }
            public long inputFluidAmount(int index) { return 0; }
            public int outputItems() { return outputs.size(); }
            public String outputItem(int index) {
                return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(outputs.get(index).getItem()).toString();
            }
            public int outputItemCount(int index) { return outputs.get(index).getCount(); }
            public int outputItemChance(int index) { return recipe.getOutputChance(index); }
            public int outputFluids() { return 0; }
            public String outputFluid(int index) { return ""; }
            public long outputFluidAmount(int index) { return 0; }
        };
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void presenceAndConsumedFluidRowsCoexist(GameTestHelper helper) {
        var map = map(helper, "gt.test.recipe.presence.consume");
        try {
            var presence = row(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                    new FluidStack[] {fluid(Fluids.WATER, 1)}).withPresenceFluidInputs(0);
            var consumed = row(new ItemStack[] {stack(Items.IRON_INGOT, 1)},
                    new FluidStack[] {fluid(Fluids.WATER, 100)});
            helper.assertTrue(map.addRecipe(presence) == presence, "presence row registered");
            helper.assertTrue(map.addRecipe(consumed) == consumed,
                    "same-input consumed row is a condition variant, not a duplicate");
            helper.assertTrue(map.mRecipeList.size() == 2, "both condition variants remain");
        } finally {
            RecipeMap.unregister("gt.test.recipe.presence.consume");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void guardedRowDoesNotRejectTheWideRow(GameTestHelper helper) {
        var map = map(helper, "gt.test.recipe.guard.wide");
        try {
            var guarded = row(new ItemStack[] {stack(Items.IRON_INGOT, 1)}, NO_FLUIDS)
                    .withItemInputGuard(0, offered -> offered.getCount() == 1);
            var wide = row(new ItemStack[] {stack(Items.IRON_INGOT, 1)}, NO_FLUIDS);
            helper.assertTrue(map.addRecipe(guarded) == guarded, "guarded row registered");
            helper.assertTrue(map.addRecipe(wide) == wide,
                    "an opaque guard does not prove the wide row is a duplicate");
            helper.assertTrue(map.mRecipeList.size() == 2, "both rows remain");
        } finally {
            RecipeMap.unregister("gt.test.recipe.guard.wide");
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
