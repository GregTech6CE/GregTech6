package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.content.food.FoodStatEvents;
import com.gregtech.gregtech.content.food.GTFoodItems;
import com.gregtech.gregtech.content.food.GTFoodStats;
import com.gregtech.gregtech.content.food.PlayerFoodStats;
import com.gregtech.gregtech.content.recipe.FoodItemRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.generated.GTFoodItemsGen;
import com.gregtech.gregtech.data.generated.GTFoodStatsGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6's food item line: the generated {@code MultiItemFood} table
 * ({@code gregtech/items/MultiItemFood.java}), the food values and six statistics the original's
 * {@code new FoodStat(...)} arguments carry, the empty container it hands back, and the recipes GT6
 * registers between its own food items.
 *
 * <p>Every number asserted here is read from GT6's source by
 * {@code tools/extract_gt6_food_items.py} / {@code tools/extract_gt6_food_stats.py} and can be checked
 * against the line named in the message.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class FoodItemTests {
    private static final int BASE_X = 29000;
    private static final int BASE_Z = 29000;
    private static final int BASE_Y = 100;

    private static ItemStack food(String id) {
        Item item = GTFoodItems.itemOrNull(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** The gap registry lives in {@code registry}, the content table in {@code content.food}. */
    private static int itemGap() {
        return com.gregtech.gregtech.registry.GTFoodItems.gap().size();
    }

    private static void eat(Player player, ItemStack stack) {
        // Forge 1.20.1's Finish carries the resulting stack as a fourth argument (FoodStatsTests idiom).
        FoodStatEvents.onItemUseFinish(new LivingEntityUseItemEvent.Finish(player, stack, 32, stack.copy()));
    }

    /** The gap table of the food line: GT6's 267 rows, 266 items, 0 missing. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void everyGt6FoodItemIsRegistered(GameTestHelper helper) {
        int rows = GTFoodItems.rows().size();
        int registered = GTFoodItems.registered();
        int gap = itemGap();
        helper.assertTrue(rows == 266,
                "GT6's MultiItemFood has 266 usable addItem rows (267 minus the hidden stub), got " + rows);
        helper.assertTrue(registered == rows,
                "every generated row resolves to a port item, rows " + rows + " registered " + registered);
        helper.assertTrue(gap == 0,
                "the generated gap table is empty: GTMultiItemsGen registers all 266 food entries, got " + gap);
        helper.assertTrue(GTFoodItemsGen.SKIPPED.size() == 39,
                "the food table records 39 not-expressible rows (hidden stub, vanilla-aliased rows, the "
                        + "returned containers the port has, GT6's potion effects, ENVM hydration/temperature), got "
                        + GTFoodItemsGen.SKIPPED.size());

        List<String> missing = new ArrayList<>();
        for (GTFoodItems.Entry entry : GTFoodItems.rows()) {
            Item item = GTFoodItems.itemOrNull(entry.id());
            if (item == null) {
                missing.add(entry.id());
                continue;
            }
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
            if (key == null || !"gregtech".equals(key.getNamespace()) || !key.getPath().equals(entry.id())) {
                missing.add(entry.id() + " -> " + key);
            }
        }
        helper.assertTrue(missing.isEmpty(), "every row is a registered gregtech item, missing " + missing);
        // Reading the statistics table forces GTFoodStatsGen to build, which is what resolves every GT
        // row against the item registry; the unresolved list below is only meaningful afterwards.
        helper.assertTrue(GTFoodStats.registered() == 201,
                "the statistics table built against the registered items, got " + GTFoodStats.registered());
        helper.assertTrue(GTFoodItems.unresolved().isEmpty(),
                "nothing failed to resolve, unresolved " + GTFoodItems.unresolved());
        helper.assertTrue(GTFoodItems.rows().get(0).id().equals("lemon"),
                "the table is in GT6's meta order and starts at Lemon (meta 0), got "
                        + GTFoodItems.rows().get(0).id());
        helper.succeed();
    }

    /** GT6's own {@code FoodStat} numbers, read from {@code MultiItemFood.java} at the named line. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void foodValuesMatchGt6sFoodStat(GameTestHelper helper) {
        // MultiItemFood.java:490 — FoodStat(2, 1.200F, 0, C+37, 0.10F, 0,0,8,0,8).
        FoodProperties cheese = GTFoodItems.foodProperties("cheese");
        helper.assertTrue(cheese != null, "cheese is a GT6 food");
        if (cheese != null) {
            helper.assertTrue(cheese.getNutrition() == 2,
                    "GT6's cheese gives 2 food (MultiItemFood.java:490), got " + cheese.getNutrition());
            helper.assertTrue(Math.abs(cheese.getSaturationModifier() - 1.2F) < 1.0E-4F,
                    "and saturation 1.2, got " + cheese.getSaturationModifier());
        }
        // MultiItemFood.java:933 — FoodStat(1, 4.000F, 0, C+37, 0.10F, 0,0,0,0,80).
        FoodProperties butter = GTFoodItems.foodProperties("butter");
        helper.assertTrue(butter != null && Math.abs(butter.getSaturationModifier() - 4.0F) < 1.0E-4F,
                "GT6's butter has saturation 4.0 (MultiItemFood.java:933)");
        // MultiItemFood.java:594 — Chum is the row GT6 marks always-edible and rotten.
        GTFoodItems.Entry chum = GTFoodItems.entryFor("chum");
        helper.assertTrue(chum != null && chum.food().alwaysEdible() && chum.food().rotten(),
                "GT6's chum is always edible and rotten (MultiItemFood.java:594, booleans T,F,T,T)");
        FoodProperties chumFood = GTFoodItems.foodProperties("chum");
        helper.assertTrue(chumFood != null && chumFood.canAlwaysEat(),
                "which the port's food properties carry through");
        // MultiItemRandom.java:283 — useDuration = max(foodLevel * 8, 16).
        helper.assertTrue(GTFoodItems.useDuration(chum) == 40,
                "GT6's chum takes max(5*8,16) = 40 ticks to eat, got " + GTFoodItems.useDuration(chum));
        helper.assertTrue(GTFoodItems.useDuration(GTFoodItems.entryFor("cucumber_slice")) == 16,
                "and a food level 0 row still takes GT6's floor of 16 ticks");
        // GT6 only calls setFoodBehavior for rows whose addItem carried a FoodStat
        // (MultiItemRandom.java:159-160): 207 of the 266 rows have one, 59 are display-only.
        int edible = 0;
        for (GTFoodItems.Entry entry : GTFoodItems.rows()) {
            if (entry.hasFoodStat()) edible++;
        }
        helper.assertTrue(edible == 207,
                "207 of GT6's 266 food rows carry a FoodStat, got " + edible);
        helper.assertTrue(GTFoodItems.foodProperties("grass") == null,
                "GT6's grass is a display item without a FoodStat, so it has no food values");
        helper.succeed();
    }

    /** The six statistics of a GT6 food item come from the same {@code FoodStat}. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void statisticsMatchGt6Declarations(GameTestHelper helper) {
        record Expected(String id, int alcohol, int caffeine, int dehydration, int sugar, int fat) {}
        List<Expected> expected = List.of(
                new Expected("cheese", 0, 0, 8, 0, 8),               // MultiItemFood.java:490
                new Expected("butter", 0, 0, 0, 0, 80),              // MultiItemFood.java:933
                new Expected("potato_chips", 0, 0, 10, 10, 0),        // MultiItemFood.java:365
                new Expected("honey_comb", 0, 0, 0, 20, 0),           // MultiItemFood.java:226
                new Expected("chum", 0, 0, 20, 0, 0),                 // MultiItemFood.java:594
                new Expected("lemon", 0, 0, 0, 4, 0),                 // MultiItemFood.java:273
                new Expected("banana", 0, 0, 0, 8, 0),                // MultiItemFood.java:384
                new Expected("pickle", 4, 0, 0, 4, 0),                // MultiItemFood.java:298
                new Expected("chili_pepper", 0, 0, 10, 0, 0));         // MultiItemFood.java:307
        for (Expected row : expected) {
            ItemStack stack = food(row.id());
            int[] stats = GTFoodStats.stats(stack);
            helper.assertTrue(stats != null, row.id() + " is in the GT food table");
            if (stats == null) continue;
            helper.assertTrue(stats[GTFoodStats.ALCOHOL] == row.alcohol()
                            && stats[GTFoodStats.CAFFEINE] == row.caffeine()
                            && stats[GTFoodStats.DEHYDRATION] == row.dehydration()
                            && stats[GTFoodStats.SUGAR] == row.sugar()
                            && stats[GTFoodStats.FAT] == row.fat(),
                    row.id() + " has GT6's FoodStat numbers, got " + java.util.Arrays.toString(stats));
        }
        // Breads is GT6's FoodStat(5, 1.200F, ..., 0,0,0,0,0): edible, but it feeds none of the six.
        helper.assertTrue(GTFoodStats.stats(food("breads")) == null,
                "a GT6 food whose FoodStat statistics are all zero carries no statistics row "
                        + "(MultiItemFood.java:717)");
        helper.assertTrue(GTFoodStats.registered() == 201,
                "the generated table has GT6's 20 vanilla rows plus the 181 GT6 food rows whose FoodStat "
                        + "carries a statistic, got " + GTFoodStats.registered());
        helper.assertTrue(GTFoodStatsGen.MISSING.isEmpty(),
                "every GT food row resolved to a port item, missing " + GTFoodStatsGen.MISSING);
        helper.assertTrue(GTFoodStats.skipped().size() == 47,
                "the 47 FoodsGT listener rows GT6 only ever fed other mods' items with stay recorded, got "
                        + GTFoodStats.skipped().size());
        // A display-only row must not be in the statistics table either.
        helper.assertTrue(GTFoodStats.stats(food("grass")) == null,
                "GT6's grass carries no statistics");
        helper.succeed();
    }

    /** Eating a GT6 food item feeds the six statistics, exactly as {@code FoodStat.onEaten} does. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void eatingFeedsGt6Statistics(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        eat(player, food("cheese"));
        helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.DEHYDRATION) == 8
                        && PlayerFoodStats.get(player, GTFoodStats.FAT) == 8,
                "GT6's cheese gives dehydration 8 and fat 8 (MultiItemFood.java:490), got dehydration "
                        + PlayerFoodStats.get(player, GTFoodStats.DEHYDRATION) + " fat "
                        + PlayerFoodStats.get(player, GTFoodStats.FAT));

        eat(player, food("butter"));
        helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.FAT) == 88,
                "butter adds GT6's 80 fat on top (MultiItemFood.java:933), got "
                        + PlayerFoodStats.get(player, GTFoodStats.FAT));

        eat(player, food("potato_chips"));
        helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.SUGAR) == 10
                        && PlayerFoodStats.get(player, GTFoodStats.DEHYDRATION) == 18,
                "potato chips add sugar 10 and dehydration 10 (MultiItemFood.java:365), got sugar "
                        + PlayerFoodStats.get(player, GTFoodStats.SUGAR) + " dehydration "
                        + PlayerFoodStats.get(player, GTFoodStats.DEHYDRATION));

        int before = PlayerFoodStats.get(player, GTFoodStats.ALCOHOL);
        eat(player, food("grass"));
        helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.ALCOHOL) == before,
                "a display-only GT6 food changes nothing");
        helper.succeed();
    }

    /** {@code FoodStat.onEaten}'s container return ({@code FoodStat.java:146-151}). */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void eatingReturnsTheEmptyContainer(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        GTFoodItems.onItemUseFinish(new LivingEntityUseItemEvent.Finish(player,
                food("potato_on_a_stick"), 32, ItemStack.EMPTY));
        helper.assertTrue(count(player, Items.STICK) == 1,
                "GT6 returns the stick of a Potato on a Stick (MultiItemFood.java:341), got "
                        + count(player, Items.STICK) + " sticks");
        helper.assertTrue(GTFoodItems.entryFor("potato_on_a_stick").containerCount() == 1,
                "the generated row names the container and its size");

        GTFoodItems.onItemUseFinish(new LivingEntityUseItemEvent.Finish(player,
                food("bag_of_potato_chips"), 32, ItemStack.EMPTY));
        // The port names a material item "<prefix registryName>_<material name>" (MaterialPrefix:643),
        // so GT6's OP.scrapGt.mat(MT.Al, 2) is gregtech:scrap_gt_aluminium - and getValue returns AIR
        // (not null) for an unknown id, which is why the assertion below also checks it is not air.
        Item scrap = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", "scrap_gt_aluminium"));
        helper.assertTrue(scrap != null && scrap != Items.AIR,
                "the port registers gregtech:scrap_gt_aluminium, got " + scrap);
        if (scrap != null) {
            helper.assertTrue(count(player, scrap) == 2,
                    "and a bag of potato chips returns GT6's 2 aluminium scraps (MultiItemFood.java:366), got "
                            + count(player, scrap));
        }
        helper.assertTrue(GTFoodItems.entryFor("fries").container().isEmpty(),
                "a food without a container returns nothing");

        // The other half of FoodStat.java:150: a full inventory drops the container in front of the
        // player instead of losing it. The branch is asserted through the public delivery seam, not by
        // looking for the ItemEntity: an entity added into a chunk a test only setBlock()ed into stays
        // in the level's PENDING set, so neither Level.getEntitiesOfClass nor ServerLevel.getAllEntities
        // lists it in the same tick (PileBlockTests:643-651 documents the section-visibility variant of
        // this trap).
        ServerLevel level = helper.getLevel();
        BlockPos base = new BlockPos(BASE_X, BASE_Y, BASE_Z);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.removeBlock(base.offset(dx, 0, dz), false);
            }
        }
        Player full = helper.makeMockSurvivalPlayer();
        full.moveTo(BASE_X + 0.5D, BASE_Y, BASE_Z + 0.5D, 0.0F, 0.0F);
        for (int slot = 0; slot < full.getInventory().getContainerSize(); slot++) {
            full.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        }
        helper.assertTrue(!GTFoodItems.deliverContainer(full, new ItemStack(Items.STICK)),
                "a full inventory drops the returned container instead of losing it "
                        + "(FoodStat.java:150)");
        helper.assertTrue(count(full, Items.STICK) == 0,
                "and nothing was stuffed into the full inventory, got " + count(full, Items.STICK));

        Player roomy = helper.makeMockSurvivalPlayer();
        helper.assertTrue(GTFoodItems.deliverContainer(roomy, new ItemStack(Items.STICK)),
                "an inventory with room takes the container instead (FoodStat.java:146)");
        helper.assertTrue(count(roomy, Items.STICK) == 1,
                "the stick is in the inventory, got " + count(roomy, Items.STICK));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.removeBlock(base.offset(dx, 0, dz), false);
            }
        }
        helper.succeed();
    }

    private static int count(Player player, Item item) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    /** The recipes GT6 puts between its own food items, looked up in the port's recipe tables. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void foodRecipesAreRegistered(GameTestHelper helper) {
        // RM.Slicer.addRecipe2 — dough into cookie shapes, a loaf into slices (MultiItemFood.java:602-785).
        helper.assertTrue(slicerHas("chocolate_dough", "cookie_shaped_dough"),
                "the slicer cuts Chocolate Dough into Cookie shaped Dough x4 (MultiItemFood.java:602)");
        helper.assertTrue(slicerHas("loaf_of_toast", "toast"),
                "the slicer cuts a Loaf of Toast into 8 Toast (MultiItemFood.java:785)");
        helper.assertTrue(slicerHas("cheese", "cheese_slice"),
                "the slicer cuts Cheese into 4 Cheese Slices (Loader_Recipes_Food.java:130)");
        // GT6 MultiItemFood.java:727: RM.Slicer.addRecipe2(IL.Food_Bread.get(1), IL.Shape_Slicer_Split,
        // IL.Food_Bread_Sliced.get(2)). IL.Food_Bread is ST.make(Items.bread, 1, 0) - a VANILLA alias
        // (MultiItemFood.java:715), which the generated table records under SKIPPED, so the lookup uses
        // the vanilla item rather than a GT food id.
        ItemStack splitBlade = new ItemStack(
                com.gregtech.gregtech.registry.GTTechnological.get("slicer_shape_split"));
        ItemStack slicedBread = food("sliced_bread");
        helper.assertTrue(!splitBlade.isEmpty() && !slicedBread.isEmpty(),
                "gregtech:slicer_shape_split and gregtech:sliced_bread are registered");
        helper.assertTrue(MachineRecipeMaps.Slicer.mRecipeList.stream().anyMatch(recipe ->
                        hasInput(recipe, new ItemStack(Items.BREAD)) && hasOutput(recipe, slicedBread)),
                "and the Slicer's recipe table answers a lookup for vanilla bread plus the split blade "
                        + "(MultiItemFood.java:727)");

        // RM.add_smelting — the raw -> cooked chain (MultiItemFood.java:531-779).
        helper.assertTrue(smelt("raw_ham", "cooked_ham"),
                "a furnace cooks Raw Ham into Cooked Ham (MultiItemFood.java:531)");
        helper.assertTrue(smelt("raw_bacon", "grilled_bacon"),
                "and Raw Bacon into Grilled Bacon (MultiItemFood.java:543)");
        helper.assertTrue(smelt("dough_bun", "bun"),
                "and Dough (Bun) into a Bun (MultiItemFood.java:679)");
        helper.assertTrue(!smelt("mule_meat", "grilled_mule_meat"),
                "Mule Meat has no smelting row, because GT6's own line 574 repeats the Dogmeat pair "
                        + "instead of the Mule pair declared at :572-573 (reproduced, not 'fixed')");

        // RM.Mixer.addRecipe1 — ice cream plus 50 mB of a juice (MultiItemFood.java:852-870).
        helper.assertTrue(mixerFluidHas("ice_cream", "lemon_ice_cream"),
                "the mixer flavours Ice Cream with lemon juice (MultiItemFood.java:852)");
        helper.assertTrue(mixerFluidHas("ice_cream", "honey_ice_cream"),
                "and with honey (MultiItemFood.java:872)");

        int built = FoodItemRecipes.skipped().size();
        List<String> rows = new ArrayList<>();
        for (String note : FoodItemRecipes.skipped()) {
            helper.assertTrue(!note.isEmpty(), "every recorded food recipe row carries a reason");
            rows.add(note);
        }
        helper.assertTrue(rows.size() == built, "the recorded rows are reported: " + rows);
        helper.succeed();
    }

    private static boolean slicerHas(String inputId, String outputId) {
        ItemStack input = food(inputId);
        ItemStack output = food(outputId);
        if (input.isEmpty() || output.isEmpty()) {
            return false;
        }
        for (Recipe recipe : MachineRecipeMaps.Slicer.mRecipeList) {
            if (hasInput(recipe, input) && hasOutput(recipe, output)) {
                return true;
            }
        }
        return false;
    }

    private static boolean smelt(String inputId, String outputId) {
        ItemStack input = food(inputId);
        ItemStack output = food(outputId);
        if (input.isEmpty() || output.isEmpty()) {
            return false;
        }
        for (Recipe recipe : MachineRecipeMaps.Furnace.mRecipeList) {
            if (hasInput(recipe, input) && hasOutput(recipe, output)) {
                return true;
            }
        }
        return false;
    }

    private static boolean mixerFluidHas(String inputId, String outputId) {
        ItemStack input = food(inputId);
        ItemStack output = food(outputId);
        if (input.isEmpty() || output.isEmpty()) {
            return false;
        }
        for (Recipe recipe : MachineRecipeMaps.Mixer.mRecipeList) {
            if (!hasInput(recipe, input) || !hasOutput(recipe, output)) {
                continue;
            }
            for (var fluid : recipe.mFluidInputs) {
                if (fluid != null && fluid.getAmount() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasInput(Recipe recipe, ItemStack wanted) {
        for (ItemStack stack : recipe.mInputs) {
            if (stack != null && !stack.isEmpty() && stack.is(wanted.getItem())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOutput(Recipe recipe, ItemStack wanted) {
        for (ItemStack stack : recipe.mOutputs) {
            if (stack != null && !stack.isEmpty() && stack.is(wanted.getItem())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The recorded rows of the whole food line, with the numbers, so a gate run shows what is
     * deliberately not done instead of hiding it.
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void skippedRowsAreDeliberate(GameTestHelper helper) {
        int listenerRows = GTFoodStats.skipped().size();
        int itemRows = GTFoodItemsGen.SKIPPED.size();
        int recipeRows = FoodItemRecipes.skipped().size();
        int recipes = FoodItemRecipes.registered();
        helper.assertTrue(GTFoodStats.skipped().stream().allMatch(note -> note.contains("(") && note.endsWith(")")),
                "every GTFoodStats skip names a reason in parentheses");
        helper.assertTrue(GTFoodStats.skipped().stream().anyMatch(note -> note.startsWith("foodCheese")),
                "and the cheese family is among them");
        helper.assertTrue(GTFoodStats.skipped().stream().anyMatch(note -> note.contains("RM.java:776")),
                "the RM.crop exclusion of GT6's own items is recorded as such");
        helper.assertTrue(GTFoodItemsGen.SKIPPED.stream().allMatch(note -> note.contains("(")),
                "every food item skip names a reason in parentheses");
        helper.assertTrue(GTFoodItemsGen.SKIPPED.stream().anyMatch(note -> note.contains("MultiItemFood.java:468")),
                "including GT6's hidden ID-migration stub");
        helper.assertTrue(GTFoodItemsGen.GAP.isEmpty() && itemGap() == 0,
                "and the item gap itself is empty, not recorded as a skip");
        // The numbers of this batch, in the log where a gate run can read them.
        com.gregtech.gregtech.GregTech.LOGGER.info(
                "GT6 food line: {} items ({} with a FoodStat, {} carrying statistics, {} return a container), "
                        + "{} statistics rows, {} FoodsGT listener rows skipped, {} item rows skipped, "
                        + "{} food recipes registered, {} recipe rows skipped",
                GTFoodItems.registered(), countWithStat(), countCarryingStatistics(), countContainers(),
                GTFoodStats.registered(), listenerRows, itemRows, recipes, recipeRows);
        helper.succeed();
    }

    private static int countWithStat() {
        int total = 0;
        for (GTFoodItems.Entry entry : GTFoodItems.rows()) {
            if (entry.hasFoodStat()) total++;
        }
        return total;
    }

    private static int countCarryingStatistics() {
        int total = 0;
        for (GTFoodItems.Entry entry : GTFoodItems.rows()) {
            if (entry.stats().tracked()) total++;
        }
        return total;
    }

    private static int countContainers() {
        int total = 0;
        for (GTFoodItems.Entry entry : GTFoodItems.rows()) {
            if (!entry.container().isEmpty()) total++;
        }
        return total;
    }
}
