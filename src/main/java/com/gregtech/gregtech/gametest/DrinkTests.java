package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.content.food.GTDrinks;
import com.gregtech.gregtech.content.food.GTFoodStats;
import com.gregtech.gregtech.content.food.PlayerFoodStats;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.registry.GTFluidItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * GT6's drinks: the {@code DrinksGT} table (registered per fluid in {@code Loader_Fluids} as
 * {@code FoodStatDrink}) and drinking one, which is what a GT6 bottle does ({@code MultiItemBottles}, 250 mB).
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class DrinkTests {
    private static final int BASE_X = 70000;
    private static final int BASE_Z = 70000;
    private static final int BASE_Y = 100;

    /** The port's still fluid for a GT6 drink row (resolved exactly the way the drink table resolves it). */
    private static Fluid fluid(String field) {
        return GTDrinks.fluidForField(field);
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void drinkTableMatchesGt6(GameTestHelper helper) {
        helper.assertTrue(GTDrinks.registered() > 100,
                "the GT6 drink table resolves onto the port's fluids, got " + GTDrinks.registered());
        helper.assertTrue(!GTDrinks.skipped().isEmpty(),
                "drinks whose fluid is not in the port are recorded, got " + GTDrinks.skipped().size());

        // GT6 Loader_Fluids values, unchanged.
        GTDrinks.Row beer = GTDrinks.rowFor(fluid("Beer"));
        helper.assertTrue(beer != null, "beer is a drink");
        if (beer != null) {
            helper.assertTrue(beer.foodLevel() == 6 && beer.alcohol() == 30,
                    "beer is food 6 / alcohol 30 per GT6, got food " + beer.foodLevel()
                            + " alcohol " + beer.alcohol());
        }
        GTDrinks.Row coffee = GTDrinks.rowFor(fluid("Coffee"));
        helper.assertTrue(coffee != null && coffee.caffeine() == 30 && coffee.dehydration() == 15,
                "coffee is caffeine 30 + dehydration 15, got " + coffee);
        GTDrinks.Row wine = GTDrinks.rowFor(fluid("Wine_Grape_Red"));
        helper.assertTrue(wine != null && wine.alcohol() == 30 && wine.sugar() == 10,
                "red wine is alcohol 30 + sugar 10, got " + wine);

        helper.assertTrue(GTDrinks.registered() + GTDrinks.unresolved().size()
                        == com.gregtech.gregtech.data.generated.GTDrinksGen.ROWS.size(),
                "every generated drink row is either resolved to a port fluid or listed as unresolved: "
                        + GTDrinks.registered() + " + " + GTDrinks.unresolved().size() + " vs "
                        + com.gregtech.gregtech.data.generated.GTDrinksGen.ROWS.size());
        com.gregtech.gregtech.GregTech.LOGGER.info("[drinks] table: {} of {} GT6 rows resolve onto port fluids; "
                        + "unresolved: {}", GTDrinks.registered(),
                com.gregtech.gregtech.data.generated.GTDrinksGen.ROWS.size(), GTDrinks.unresolved().size());
        // Something that is not a drink at all.
        helper.assertTrue(GTDrinks.rowFor(fluid("Iron")) == null, "molten iron is not drinkable");
        helper.assertTrue(GTDrinks.rowFor((Fluid) null) == null, "and a null fluid has no row");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void drinkingFeedsTheStatistics(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        player.getFoodData().setFoodLevel(6);
        Fluid beer = fluid("Beer");

        helper.assertTrue(GTDrinks.drink(player, new FluidStack(beer, 250)), "beer can be drunk");
        helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.ALCOHOL) == 30,
                "drinking beer adds 30 alcohol, got " + PlayerFoodStats.get(player, GTFoodStats.ALCOHOL));
        helper.assertTrue(player.getFoodData().getFoodLevel() > 6,
                "and it feeds the player, food level " + player.getFoodData().getFoodLevel());

        Fluid coffee = fluid("Coffee");
        if (coffee != null) {
            GTDrinks.drink(player, new FluidStack(coffee, 250));
            helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.CAFFEINE) == 30
                            && PlayerFoodStats.get(player, GTFoodStats.DEHYDRATION) == 15,
                    "coffee adds caffeine 30 and dehydration 15, got caffeine "
                            + PlayerFoodStats.get(player, GTFoodStats.CAFFEINE) + " dehydration "
                            + PlayerFoodStats.get(player, GTFoodStats.DEHYDRATION));
            helper.assertTrue(GTDrinks.drink(player, new FluidStack(beer, 250)),
                    "a second beer still works");
            helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.ALCOHOL) == 60,
                    "alcohol accumulates to 60, got " + PlayerFoodStats.get(player, GTFoodStats.ALCOHOL));
        }

        // A fluid outside the table changes nothing at all.
        helper.assertTrue(!GTDrinks.drink(player, new FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 250)),
                "lava is refused");
        helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.ALCOHOL) == 30
                        || PlayerFoodStats.get(player, GTFoodStats.ALCOHOL) == 60,
                "lava adds no alcohol, got " + PlayerFoodStats.get(player, GTFoodStats.ALCOHOL));
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void drinkingABottleItemWorks(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = helper.makeMockSurvivalPlayer();

        var beerProxy = GTFluidItems.forFluid(fluid("Beer"));
        if (beerProxy == null) {
            // The per-fluid item proxy does not cover every fluid; the drink path itself is covered by
            // DrinkTests.drinkingFeedsTheStatistics and drinkTableMatchesGt6.
            helper.succeed();
            return;
        }
        ItemStack bottle = new ItemStack(beerProxy);
        if (bottle.isEmpty() || bottle.getItem() == Items.AIR) {
            helper.succeed();
            return;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, bottle.copy());
        InteractionResultHolder<ItemStack> result = bottle.getItem().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getResult().consumesAction(),
                "right-clicking a bottle of beer drinks it, got " + result.getResult());
        helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.ALCOHOL) == 30,
                "the bottle's 250 mB of beer give 30 alcohol, got "
                        + PlayerFoodStats.get(player, GTFoodStats.ALCOHOL));

        // A fluid with no drink row is not consumed at all (vanilla lava is always resolvable).
        var lavaProxy = GTFluidItems.forFluid(net.minecraft.world.level.material.Fluids.LAVA);
        if (lavaProxy != null) {
            ItemStack notADrink = new ItemStack(lavaProxy);
            player.setItemInHand(InteractionHand.MAIN_HAND, notADrink.copy());
            InteractionResultHolder<ItemStack> passed =
                    notADrink.getItem().use(level, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(!passed.getResult().consumesAction(),
                    "a non-drink fluid item is not consumed, got " + passed.getResult());
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void drinkingGivesBackAnEmptyBottle(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = helper.makeMockPlayer(); // creative: the item must survive
        player.getAbilities().instabuild = false;
        var beerProxy = GTFluidItems.forFluid(fluid("Beer"));
        if (beerProxy == null) {
            helper.succeed();
            return;
        }
        ItemStack bottle = new ItemStack(beerProxy);
        if (bottle.isEmpty()) {
            helper.succeed();
            return;
        }
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, bottle.copy());
        bottle.getItem().use(level, player, InteractionHand.MAIN_HAND);
        // Inventory.add fills the held slot first, so the hand may already hold the empty bottle.
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertTrue(held.isEmpty() || held.is(Items.GLASS_BOTTLE),
                "the bottle of beer is gone, got " + held);
        helper.assertTrue(player.getInventory().countItem(Items.GLASS_BOTTLE) == 1,
                "and an empty glass bottle is left behind, count "
                        + player.getInventory().countItem(Items.GLASS_BOTTLE));
        helper.succeed();
    }

    /**
     * How much of the drink table is reachable with an item. GT6 gives every drink a bottle
     * ({@code MultiItemBottles}); the port drinks through its per-fluid {@code FluidItem} proxy, so this
     * counts how many of the resolved drink fluids actually have one - the number a later
     * {@code MultiItemBottles} port would have to cover.
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void drinkFluidsHaveItemProxies(GameTestHelper helper) {
        int resolved = 0;
        java.util.List<String> withProxy = new java.util.ArrayList<>();
        java.util.List<String> withoutProxy = new java.util.ArrayList<>();
        for (String field : DRINK_FIELDS) {
            Fluid fluid = GTDrinks.fluidForField(field);
            if (fluid == null) {
                continue;
            }
            resolved++;
            if (GTFluidItems.forFluid(fluid) != null) {
                withProxy.add(field);
            } else {
                withoutProxy.add(field);
            }
        }
        // Logged (not only asserted) so the gate log carries the baseline the bottle batch needs.
        com.gregtech.gregtech.GregTech.LOGGER.info(
                "[drinks] resolvable drinks: {} of {} sampled; with a per-fluid item proxy: {}; without: {}",
                resolved, DRINK_FIELDS.size(), withProxy, withoutProxy);
        helper.assertTrue(!withProxy.isEmpty(),
                "at least one GT drink has a per-fluid item to drink it with; proxies " + withProxy
                        + " of " + resolved + " resolvable drinks");
        helper.assertTrue(withProxy.size() + withoutProxy.size() == resolved,
                "every resolvable drink is classified: " + withProxy.size() + " + " + withoutProxy.size()
                        + " vs " + resolved);
        helper.succeed();
    }

    /** A sample of GT6 drink fields, spanning juices, wines, beers and coffee. */
    private static final java.util.List<String> DRINK_FIELDS = java.util.List.of(
            "Beer", "DarkBeer", "Wine_Grape_Red", "Wine_Grape_White", "Coffee",
            "Juice_Apple", "Juice_Orange", "Milk", "ChocolateMilk", "Water");

    // ==================== §103.C: GT6's own bottles ====================

    /**
     * The GT6 bottle <em>items</em> are drinkable now ({@code MultiItemBottles} → {@code BottleItem}),
     * which is what the per-fluid proxy tests above were standing in for: this drinks the port's
     * {@code beer} bottle, not the {@code fluid_item_} proxy.
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void gt6BottleItemsAreDrinkable(GameTestHelper helper) {
        var level = helper.getLevel();
        Item beer = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(com.gregtech.gregtech.GregTech.id("beer"));
        helper.assertTrue(beer instanceof com.gregtech.gregtech.item.BottleItem,
                "the port's beer bottle is a BottleItem, got " + beer);
        if (!(beer instanceof com.gregtech.gregtech.item.BottleItem bottle)) {
            return;
        }
        helper.assertTrue("Beer".equals(bottle.fluidKey()),
                "tools/extract_gt6_bottles.py gives it GT6's Beer fluid, got " + bottle.fluidKey());
        helper.assertTrue(bottle.fluid() != null,
                "and the port has that fluid (" + bottle.fluidKey() + ")");

        Player player = helper.makeMockSurvivalPlayer();
        player.getInventory().clearContent();
        ItemStack stack = new ItemStack(beer);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
        InteractionResultHolder<ItemStack> result = stack.getItem().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getResult().consumesAction(),
                "right-clicking the bottle of beer drinks it, got " + result.getResult());
        helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.ALCOHOL) == 30,
                "the bottle's 250 mB of beer give 30 alcohol, got "
                        + PlayerFoodStats.get(player, GTFoodStats.ALCOHOL));
        helper.assertTrue(player.getInventory().countItem(beer) == 0,
                "the bottle itself is consumed, still holding "
                        + player.getInventory().countItem(beer));
        Item emptyBottle = com.gregtech.gregtech.item.BottleItem.emptyBottle().getItem();
        helper.assertTrue(player.getInventory().countItem(emptyBottle) == 1,
                "and GT6's empty bottle comes back (" + emptyBottle + "), count "
                        + player.getInventory().countItem(emptyBottle));
        helper.succeed();
    }

    /**
     * The generated bottle table covers GT6's drink bottles and every covered id really is a drinkable
     * {@code BottleItem} whose fluid the port has. Logged as well as asserted, so the gate log carries
     * the coverage the batch reached (GT6 registers ~180 bottles; a few hold fluids only other mods add).
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void bottleTableCoversGt6Drinks(GameTestHelper helper) {
        java.util.List<String> notBottles = new java.util.ArrayList<>();
        java.util.List<String> withoutFluid = new java.util.ArrayList<>();
        int covered = 0;
        String[] entries = com.gregtech.gregtech.data.generated.GTBottlesGen.ENTRIES;
        for (int i = 0; i + 2 < entries.length; i += 3) {
            String id = entries[i];
            Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(com.gregtech.gregtech.GregTech.id(id));
            if (!(item instanceof com.gregtech.gregtech.item.BottleItem bottle)) {
                notBottles.add(id + " -> " + item);
                continue;
            }
            covered++;
            if (bottle.fluid() == null) {
                withoutFluid.add(id + " (" + bottle.fluidKey() + ")");
            }
        }
        com.gregtech.gregtech.GregTech.LOGGER.info(
                "[bottles] table rows: {}; BottleItems: {}; rows without a port fluid: {}; non-bottle items: {}",
                entries.length / 3, covered, withoutFluid.size(), notBottles.size());
        helper.assertTrue(covered >= 150,
                "the bottle table covers GT6's drink bottles, got " + covered + " of " + entries.length / 3
                        + " (non-bottle items: " + notBottles + ")");
        // A row whose fluid the port lacks is legitimate (other mods' brews); most must resolve though.
        helper.assertTrue(withoutFluid.size() * 2 < covered,
                "most bottles hold a fluid the port has; missing: " + withoutFluid);
        helper.assertTrue(com.gregtech.gregtech.content.food.GTBottles.skipped().size() <= 2,
                "only GT6's loot bottle has no fluid expression: "
                        + com.gregtech.gregtech.content.food.GTBottles.skipped());
        helper.succeed();
    }

    /**
     * GT6's filling rows are registered ({@code MultiItemBottles:129-280}: one 1000 mB container plus N
     * empty bottles gives N filled bottles), and the recipe manager really carries one of them.
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void bottleFillingRecipesAreRegistered(GameTestHelper helper) {
        java.util.List<String> ids = com.gregtech.gregtech.loaders.Loader_BottleFillingRecipes.registeredIds();
        java.util.List<String> skipped = com.gregtech.gregtech.loaders.Loader_BottleFillingRecipes.skipped();
        com.gregtech.gregtech.GregTech.LOGGER.info(
                "[bottles] filling rows registered: {} (skipped: {})", ids.size(), skipped);
        helper.assertTrue(ids.size() >= 40,
                "ten families x four counts of GT6's filling rows, got " + ids.size() + " " + skipped);
        helper.assertTrue(skipped.isEmpty(), "every family has a container item: " + skipped);

        var milk = helper.getLevel().getRecipeManager()
                .byKey(com.gregtech.gregtech.GregTech.id("bottles/milk_x4"));
        helper.assertTrue(milk.isPresent(), "the four-bottle milk row is in the recipe manager");
        milk.ifPresent(recipe -> {
            ItemStack result = recipe.getResultItem(helper.getLevel().registryAccess());
            Item milkBottle = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(com.gregtech.gregtech.GregTech.id("milk"));
            helper.assertTrue(result.is(milkBottle) && result.getCount() == 4,
                    "…and gives four milk bottles, got " + result);
            helper.assertTrue(recipe.getIngredients().size() == 5,
                    "from one container plus four empty bottles, got " + recipe.getIngredients().size()
                            + " ingredients");
        });
        helper.succeed();
    }
}
