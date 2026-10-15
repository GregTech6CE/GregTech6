package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A recipe map keeps its static rows ahead of every dynamic provider. The built-in provider
 * stays in front of providers appended for addons, and the first non-null answer wins.
 */
@GameTestHolder("gregtech_dynamic_recipes")
@PrefixGameTestTemplate(false)
public final class DynamicRecipeProviderTests {
    private DynamicRecipeProviderTests() {}

    private static RecipeMap map(String name) {
        var created = new RecipeMap(null, name, "Dynamic providers", "test",
                1, 1, 0, 0, 0, 0, 0, false, false, false, false);
        created.mLogErrors = false;
        return created;
    }

    private static Recipe stickToPaper() {
        return new Recipe(new ItemStack[]{new ItemStack(Items.STICK)},
                new ItemStack[]{new ItemStack(Items.PAPER)}, null, null, null, null, 16, 16, 0);
    }

    private static Recipe found(RecipeMap map) {
        return map.findRecipe(List.of(new ItemStack(Items.STICK)), List.of(), false, 1, 0);
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void laterProviderRunsOnlyWhenTheEarlierOneReturnsNull(GameTestHelper helper) {
        var map = map("gt.recipe.test.dynamic_fallthrough");
        var later = stickToPaper();
        var earlierCalls = new AtomicInteger();
        var laterCalls = new AtomicInteger();
        map.dynamicRecipes((items, fluids) -> { earlierCalls.incrementAndGet(); return null; });
        map.addDynamicRecipes((items, fluids) -> { laterCalls.incrementAndGet(); return later; });
        var found = found(map);
        helper.assertTrue(found == later, "the later provider supplies the row");
        helper.assertTrue(earlierCalls.get() == 1 && laterCalls.get() == 1,
                "a null answer asks the next provider");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void anEarlierResultSkipsLaterProviders(GameTestHelper helper) {
        var map = map("gt.recipe.test.dynamic_first");
        var earlier = stickToPaper();
        var laterCalls = new AtomicInteger();
        map.dynamicRecipes((items, fluids) -> earlier);
        map.addDynamicRecipes((items, fluids) -> { laterCalls.incrementAndGet(); return stickToPaper(); });
        helper.assertTrue(found(map) == earlier, "the built-in provider wins");
        helper.assertTrue(laterCalls.get() == 0, "a non-null answer does not ask the next provider");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void aStaticRowIsFoundBeforeAnyProvider(GameTestHelper helper) {
        var map = map("gt.recipe.test.dynamic_static");
        var stored = map.addRecipe(stickToPaper());
        helper.assertTrue(stored != null, "the static stick row is stored");
        var calls = new AtomicInteger();
        map.dynamicRecipes((items, fluids) -> { calls.incrementAndGet(); return stickToPaper(); });
        map.addDynamicRecipes((items, fluids) -> { calls.incrementAndGet(); return stickToPaper(); });
        helper.assertTrue(found(map) == stored, "the static row is used");
        helper.assertTrue(calls.get() == 0, "providers are not asked after a static match");
        helper.succeed();
    }
}
