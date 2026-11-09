package gregtech.gametest;

import gregtech.content.food.AfkHungerEvents;
import gregtech.content.food.GTDrinks;
import gregtech.content.food.GTFoodItems;
import gregtech.content.food.GTFoodStats;
import gregtech.content.food.SandwichNutrition;
import gregtech.content.item.BottleItem;
import gregtech.content.nuclear.PlayerRadiation;
import gregtech.content.recipe.BottleMachineRecipes;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Runtime guards for RFC #363: the five statistics are gone, radiation and the bottles stay. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class NutritionRemovalTests {
    private static final String BATCH = "nutrition_removal";
    private static final Set<String> REMOVED_DRINK_FIELDS =     Set.of(
            "potion.sake",
            "potion.dressing",
            "apricotsmoothie",
            "cherrysmoothie",
            "elderberrysmoothie",
            "grapefruitsmoothie",
            "limesmoothie",
            "orangesmoothie",
            "peachsmoothie",
            "pearsmoothie",
            "plumsmoothie",
            "redgrapesmoothie",
            "whitegrapesmoothie",
            "icetea",
            "bawls",
            "holywater",
            "purpledrink",
            "rottendrink",
            "maplesap",
            "rainbowsap",
            "coconutmilk",
            "currantjuice",
            "figjuice",
            "kiwijuice",
            "mangojuice",
            "papayajuice",
            "persimmonjuice",
            "starfruitjuice",
            "hellderberryjuice",
            "grc.applecider0",
            "grapejuice",
            "grc.grapewine0",
            "potatojuice",
            "juice",
            "hopsmash",
            "soymilk",
            "chillysauce",
            "bbqsauce",
            "maplesyrup",
            "coconutcream",
            "coconutsmoothie",
            "pina.colada",
            "currantsmoothie",
            "figsmoothie",
            "kiwismoothie",
            "mangosmoothie",
            "papayasmoothie",
            "persimmonsmoothie",
            "starfruitsmoothie",
            "grapesmoothie",
            "purplegrapesmoothie",
            "fruitsmoothie",
            "wine",
            "ricardosanchez",
            "vinegar",
            "medicine.laxative",
            "medicine.heal",
            "binnie.vodka",
            "potion.leninade",
            "darkbeer",
            "potion.dragonblood",
            "potion.hotsauce",
            "potion.diabolosauce",
            "potion.diablosauce",
            "potion.diablosauce.strong");

    private NutritionRemovalTests() {}

    private static Player player(GameTestHelper helper) {
//? if >=1.21.1 {
        /*return helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
*///?} else {
        return helper.makeMockPlayer();
//?}
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void closedLoopBottlesStayRegisteredWithoutFluid(GameTestHelper helper) {
        List<String> inert =         List.of(
                "apricot_smoothie",
                "barbecue_sauce",
                "bawls",
                "cherry_smoothie",
                "chili_sauce",
                "coconut_cream",
                "coconut_milk",
                "coconut_smoothie",
                "currant_juice",
                "currant_smoothie",
                "dark_beer",
                "diablo_sauce",
                "diabolo_sauce",
                "dragon_blood",
                "dressing",
                "elderberry_smoothie",
                "fig_juice",
                "fig_smoothie",
                "froot_smoothie",
                "grape_juice_3",
                "grape_juice_4",
                "grape_smoothie",
                "grape_smoothie_2",
                "grape_smoothie_3",
                "grape_smoothie_4",
                "grape_vinegar",
                "grapefruit_smoothie",
                "holy_water",
                "hops_mash",
                "hot_sauce",
                "juice",
                "kiwi_juice",
                "kiwi_smoothie",
                "laxative",
                "leninade",
                "lime_smoothie",
                "mango_juice",
                "mango_smoothie",
                "maple_sap",
                "maple_syrup",
                "medicine",
                "orange_smoothie",
                "papaya_juice",
                "papaya_smoothie",
                "peach_smoothie",
                "pear_smoothie",
                "persimmon_juice",
                "persimmon_smoothie",
                "pi_a_colada",
                "plum_smoothie",
                "potato_juice",
                "purple_drink",
                "rainbow_sap",
                "ricardo_sanchez",
                "rotten_drink",
                "sake",
                "soy_milk",
                "starfruit_juice",
                "starfruit_smoothie",
                "there_is_no_cow_sauce",
                "vodka",
                "wine");
        for (String id : inert) {
            Item item = GTFoodItems.itemOrNull(id);
            helper.assertTrue(item instanceof BottleItem, id + " is still a bottle item");
            helper.assertTrue(((BottleItem) item).fluid() == null, id + " has no fluid");
        }
        helper.assertTrue(BottleMachineRecipes.missing().containsAll(inert), "the inert bottles are reported missing");
        helper.assertTrue(
                GTDrinks.unresolved().stream().noneMatch(REMOVED_DRINK_FIELDS::contains),
                "removed drink rows are not left unresolved");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void medicalRadiationRemovalStaysIntact(GameTestHelper helper) {
        Item banana = GTFoodItems.itemOrNull("banana");
        Item radaway = GTFoodItems.itemOrNull("radaway");
        Item cureAll = GTFoodItems.itemOrNull("cure_all");
        helper.assertTrue(banana != null && radaway != null && cureAll != null, "food and medicine items exist");
        helper.assertTrue(GTFoodStats.radiation(new ItemStack(banana)) == null, "banana adds no radiation");
        helper.assertTrue(GTFoodStats.radiation(new ItemStack(radaway)) == -50, "radaway keeps -50");
        helper.assertTrue(GTFoodStats.radiation(new ItemStack(cureAll)) == -999, "cure_all keeps its sentinel");

        Player player = player(helper);
        PlayerRadiation.change(player, 200);
        helper.assertTrue(PlayerRadiation.dose(player) == 127, "the radiation dose clamps at 127");
        PlayerRadiation.change(player, -999);
        helper.assertTrue(PlayerRadiation.dose(player) == 0, "cure_all resets the dose to zero");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void afkHungerStillUsesThePlayerTickPhase(GameTestHelper helper) {
        var player = MultiblockBehaviorApiTests.menuPlayer(helper);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        try {
            player.getInventory().add(new ItemStack(Items.STONE, 64));
            player.tickCount = 1199;
            player.getFoodData().setFoodLevel(20);
            AfkHungerEvents.apply(player);
            helper.assertTrue(player.getFoodData().getExhaustionLevel() == 0.0F, "off-phase ticks do nothing");
            player.tickCount = 1200;
            AfkHungerEvents.apply(player);
            helper.assertTrue(player.getFoodData().getExhaustionLevel() >= 1.0F, "the AFK phase adds exhaustion");
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void keptDrinkAndDressingStillWork(GameTestHelper helper) {
        Item juice = GTFoodItems.itemOrNull("apple_juice");
        helper.assertTrue(juice instanceof BottleItem, "apple_juice is still a bottle item");
        helper.assertTrue(((BottleItem) juice).fluid() != null, "apple_juice still resolves its fluid");
        helper.assertTrue(GTDrinks.rowFor(((BottleItem) juice).fluid()) != null, "apple_juice still has a drink row");

        Item dressing = GTFoodItems.itemOrNull("dressing");
        helper.assertTrue(dressing != null, "dressing stays registered");
        helper.assertTrue(SandwichNutrition.food(new ItemStack(dressing)) > 0, "dressing still feeds a sandwich");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void removedDamageTypesAreGone(GameTestHelper helper) {
        var damageTypes = helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        for (String path : List.of("alcohol", "caffeine", "dehydration", "sugar", "fat")) {
            helper.assertTrue(
                    !damageTypes.containsKey(ResourceLocation.tryParse("gt6ce:" + path)),
                    path + " is no longer registered");
        }
        helper.assertTrue(
                damageTypes.containsKey(ResourceLocation.tryParse("gt6ce:radiation")),
                "radiation stays registered");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void fallbackBottleConsumesInSurvivalAndNotInCreative(GameTestHelper helper) {
        Item sake = GTFoodItems.itemOrNull("sake");
        helper.assertTrue(sake instanceof BottleItem, "sake is still a bottle item");

        var survival = MultiblockBehaviorApiTests.menuPlayer(helper);
        survival.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        survival.getFoodData().setFoodLevel(0);
        try {
            ItemStack stack = new ItemStack(sake);
            survival.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var result = ((BottleItem) sake).use(helper.getLevel(), survival, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == InteractionResult.CONSUME, "sake starts a drink");
            ((BottleItem) sake).finishUsingItem(stack, helper.getLevel(), survival);
            helper.assertTrue(stack.getCount() == 0, "sake is consumed in survival");
            helper.assertTrue(
                    survival.getInventory().items.stream()
                            .anyMatch(item -> item.is(BottleItem.emptyBottle().getItem())),
                    "survival returns an empty bottle");
        } finally {
            survival.discard();
        }

        var creative = MultiblockBehaviorApiTests.menuPlayer(helper);
        creative.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        try {
            ItemStack stack = new ItemStack(sake);
            creative.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var result = ((BottleItem) sake).use(helper.getLevel(), creative, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == InteractionResult.CONSUME, "creative starts a drink");
            ((BottleItem) sake).finishUsingItem(stack, helper.getLevel(), creative);
            helper.assertTrue(stack.getCount() == 1, "creative does not consume the bottle");
        } finally {
            creative.discard();
        }
        helper.succeed();
    }
}
