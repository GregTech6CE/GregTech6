package com.gregtech.gregtech.content.food;

import com.gregtech.gregtech.data.generated.GTFoodItemsGen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GT6's food items ({@code gregtech/items/MultiItemFood.java}): what each of them feeds you and what it
 * hands back.
 *
 * <p>The port registers the food <em>items</em> from {@code GTMultiItemsGen} (transpiled by
 * {@code tools/transpile_gt6_multiitems.py}, one entry per GT6 {@code addItem(...)} row), and every one
 * of GT6's 266 food items is already there. What was missing is the content of their
 * {@code new FoodStat(...)} argument, which is a separate generated table
 * ({@link GTFoodItemsGen}, {@code tools/extract_gt6_food_items.py}) exactly like
 * {@link GTBottles} is for the bottles.
 *
 * <p>Three GT6 behaviours hang off that table:
 * <ol>
 *   <li><b>Vanilla food values.</b> GT6's {@code FoodStat} food level and saturation are what
 *       {@code MultiItemRandom} hands to vanilla's food data verbatim
 *       ({@code MultiItemRandom.java:297-304}: {@code addStats(foodLevel, saturation)}), and 1.20.1's
 *       {@code FoodData.eat(nutrition, saturationModifier)} computes the same
 *       {@code nutrition * saturationModifier * 2}. {@link #foodProperties(String)} returns them so the
 *       registry can build the real {@link FoodProperties} instead of one placeholder for all food.</li>
 *   <li><b>The six statistics.</b> {@code FoodStat.onEaten} pushes them into {@code EntityFoodTracker}
 *       ({@code FoodStat.java:164-172}); the port reads them from {@link GTFoodStats}, which is generated
 *       from this same table by {@code tools/extract_gt6_food_stats.py}.</li>
 *   <li><b>The empty container.</b> {@code FoodStat.onEaten} returns {@code mEmptyContainer} to the
 *       player ({@code FoodStat.java:146-151}) - eating a Potato on a Stick gives the stick back, a bag
 *       of chips gives the foil scraps. {@link #onItemUseFinish} does that here.</li>
 * </ol>
 *
 * <p>Not carried over, and recorded in {@link GTFoodItemsGen#SKIPPED} rather than invented: GT6's
 * {@code PotionsGT} potion effects and the ENVM body-temperature/hydration numbers.
 */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class GTFoodItems {
    /**
     * GT6 {@code FoodStat}'s food numbers: {@code aFoodLevel}/{@code aSaturation} (vanilla food data),
     * {@code aHydration}/{@code aTemperature} (ENVM only), {@code aAlwaysEdible}/{@code aIsRotten}.
     */
    public record Food(int level, float saturation, int hydration, int temperature,
                       boolean alwaysEdible, boolean rotten) {}

    /** GT6 {@code FoodStat}'s six statistics, in {@link GTFoodStats}' order. */
    public record Stats(int alcohol, int caffeine, int dehydration, int sugar, int fat, int radiation) {
        /** Whether any of the six is non-zero (GT6 only tracks what changes). */
        public boolean tracked() {
            return alcohol != 0 || caffeine != 0 || dehydration != 0
                    || sugar != 0 || fat != 0 || radiation != 0;
        }
    }

    /**
     * One GT6 food item: the port's item id, its GT6 display name, its GT6 {@code addItem} meta id (the
     * texture number in {@code GT6resourcepack}), the food values, the statistics, the empty container
     * and the {@code MultiItemFood.java} line all of it comes from.
     */
    public record Entry(String id, String display, int meta, Food food, Stats stats,
                        String container, int containerCount, boolean hasFoodStat, int line) {}

    private static Entry fromShared(FoodNutritionRows.Entry r) { var f=r.food();var q=r.stats();return new Entry(r.id(),r.display(),r.meta(),new Food(f.level(),f.saturation(),f.hydration(),f.temperature(),f.alwaysEdible(),f.rotten()),new Stats(q.alcohol(),q.caffeine(),q.dehydration(),q.sugar(),q.fat(),q.radiation()),r.container(),r.containerCount(),r.hasFoodStat(),r.line()); }
    private static final List<Entry> ROWS=GTFoodItemsGen.ROWS.stream().map(GTFoodItems::fromShared).toList();
    private static final Map<String, Entry> BY_ID = new LinkedHashMap<>();
    private static final List<String> MISSING_ITEMS = new ArrayList<>();

    static {
        for (Entry entry : ROWS) {
            BY_ID.put(entry.id(), entry);
        }
    }

    private GTFoodItems() {}

    /** GT6 food rows, in GT6's {@code addItem} meta order. */
    public static List<Entry> rows() {
        return ROWS;
    }

    /** The row of a port item id (GT6's display name spelled the way the port registers it), or null. */
    public static Entry entryFor(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    /** The row of an item, or null when GT6 never declared it as food. */
    public static Entry entryFor(Item item) {
        if (item == null) {
            return null;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return key == null || !"gregtech".equals(key.getNamespace()) ? null : entryFor(key.getPath());
    }

    /** How many of GT6's food items the port registers (all 266 of {@code MultiItemFood}). */
    public static int registered() {
        return BY_ID.size();
    }

    /** GT6 food rows this table cannot express, with the reason (never dropped silently). */
    public static List<String> skipped() {
        return GTFoodItemsGen.SKIPPED;
    }

    /**
     * The port item of a generated row, resolved against the item registry. Returns null before the items
     * are registered, which is why nothing here runs during mod construction: the ids are only needed once
     * the player eats something.
     *
     * <p>A miss is recorded in {@link #unresolved()}: this is the accessor the generated statistics table
     * uses, where a missing id would silently lose a row.</p>
     */
    public static Item item(String id) {
        Item item = itemOrNull(id);
        if (item == null) {
            MISSING_ITEMS.add(id);
        }
        return item;
    }

    /** The same lookup without recording a miss, for callers that keep their own skip list. */
    public static Item itemOrNull(String id) {
        if(id==null)return null; Item item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",id));return item==net.minecraft.world.item.Items.AIR?null:item;
    }

    /** Ids {@link #item(String)} could not resolve (a real gap, asserted by the GameTest). */
    public static List<String> unresolved() {
        return List.copyOf(MISSING_ITEMS);
    }

    /** The GT6 FoodStat item recipe time: {@code max(foodLevel * 8, 16)} ticks ({@code MultiItemRandom.java:283}). */
    public static int useDuration(Entry entry) {
        return entry == null || !entry.hasFoodStat() ? 0 : Math.max(entry.food().level() * 8, 16);
    }

    /**
     * The vanilla {@link FoodProperties} of a GT6 food item, or null when GT6 declared no
     * {@code FoodStat} for it (GT6's grass, tusks, hooves and combs are not edible at all - only the rows
     * whose {@code addItem} call carried a {@code FoodStat} get {@code setFoodBehavior},
     * {@code MultiItemRandom.java:159-160}).
     */
    public static FoodProperties foodProperties(String id) {
        Entry entry = entryFor(id);
        if (entry == null || !entry.hasFoodStat()) {
            return null;
        }
        Food food = entry.food();
        FoodProperties.Builder builder = new FoodProperties.Builder()
                .nutrition(food.level())
                .saturationModifier(food.saturation());
        if (food.alwaysEdible()) {
            builder.alwaysEdible();
        }
        return builder.build();
    }

    /** Item properties for a GT6 food item id: the port's usual stack size plus GT6's food values. */
    public static Item.Properties itemProperties(String id) {
        Item.Properties properties = new Item.Properties().stacksTo(64);
        FoodProperties food = foodProperties(id);
        return food == null ? properties : properties.food(food);
    }

    /**
     * GT6 {@code FoodStat.onEaten}'s container return ({@code FoodStat.java:146-151}): the eaten item's
     * {@code mEmptyContainer} goes to the player, and not in creative mode
     * ({@code UT.Entities.hasInfiniteItems}, {@code FoodStat.java:146}).
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || player.getAbilities().instabuild) {
            return;
        }
        Entry entry = entryFor(event.getItem().getItem());
        if (entry == null || entry.container().isEmpty() || entry.containerCount() <= 0) {
            return;
        }
        Item container = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.container()));
        if (container == null || container==net.minecraft.world.item.Items.AIR) {
            MISSING_ITEMS.add(entry.container());
            return;
        }
        ItemStack stack = new ItemStack(container, entry.containerCount());
        deliverContainer(player, stack);
    }

    /**
     * The delivery half of {@code FoodStat.java:146-151}: the empty container goes into the player's
     * inventory, and when there is no room it is dropped in front of them instead of being lost.
     *
     * <p>Public so a GameTest can assert the branch directly. The dropped {@code ItemEntity} cannot be
     * the test's evidence: an entity added to a chunk a test merely {@code setBlock}ed into stays in
     * the level's <em>pending</em> set - its section is not tracked yet - so neither
     * {@code Level.getEntitiesOfClass} nor even {@code ServerLevel.getAllEntities} lists it in the same
     * tick ({@code PersistentEntitySectionManager}, the trap {@code PileBlockTests:643-651} documents).</p>
     *
     * @return {@code true} when the stack went into the inventory, {@code false} when it was dropped
     */
    public static boolean deliverContainer(Player player, ItemStack stack) {
        if (player.getInventory().add(stack)) return true;
        player.drop(stack, false);
        return false;
    }
}
