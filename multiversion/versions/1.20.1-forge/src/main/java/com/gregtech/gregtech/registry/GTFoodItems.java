package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.data.generated.GTFoodItemsGen;
import com.gregtech.gregtech.item.TechItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * The registry for GT6 food items the port does <em>not</em> have yet - the gap registry of the food
 * line.
 *
 * <p>The gap is generated, not hand-written: {@code tools/extract_gt6_food_items.py} parses every
 * {@code addItem(...)} row of GT6's {@code gregtech/items/MultiItemFood.java} and lines the 266 real
 * items up 1:1 against the {@code food} category of {@link GTMultiItemsGen} (the port's transpiled
 * multi-item table). That sweep has run and the result is in {@link GTFoodItemsGen#GAP}: <b>no GT6
 * food item is missing</b>. GT6 declares 267 rows, of which</p>
 * <ul>
 *   <li>266 are registered by {@link GTMultiItems} (the same 266 the extractor matched by id),</li>
 *   <li>1 is GT6's own hidden ID-migration stub ({@code MultiItemFood.java:468}: an empty display name
 *       and {@code TD.Creative.HIDDEN}), which GT6 does not register as a usable item either.</li>
 * </ul>
 *
 * <p>So this file registers nothing today, and that is a measured fact rather than an omission: what
 * the food line was actually missing is the <em>content</em> of those items - their
 * {@code new FoodStat(...)} numbers, which live in
 * {@link com.gregtech.gregtech.content.food.GTFoodItems} / {@code GTFoodStats} - not the items
 * themselves. The class stays because it is the single place a future row of the gap belongs: a crop
 * the port cannot express today (see {@code GTFoodItemsGen.SKIPPED}, e.g. GT6's own ore-dictionary
 * names for other mods' crops) would be added here, with {@link GTFoodItemsGen#GAP} listing it.</p>
 */
public final class GTFoodItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, GregTech.NAMESPACE);

    private static final List<String> REGISTERED = new ArrayList<>();

    private GTFoodItems() {}

    /** GT6 food item ids the port does not register, from the generated gap table. */
    public static List<String> gap() {
        return GTFoodItemsGen.GAP;
    }

    /** How many gap items this registry created (0 while the food line is complete). */
    public static int registered() {
        return REGISTERED.size();
    }

    /** Registers the gap items; each one gets GT6's own food values through the generated table. */
    public static int registerAll() {
        for (String id : GTFoodItemsGen.GAP) {
            ITEMS.register(id, () -> new TechItem(id, true,
                    com.gregtech.gregtech.content.food.GTFoodItems.itemProperties(id)));
            REGISTERED.add(id);
        }
        GregTech.LOGGER.info("GT6 food line: {} items in MultiItemFood, {} registered by GTMultiItems, "
                        + "{} registered here as a gap, {} recorded in SKIPPED",
                com.gregtech.gregtech.content.food.GTFoodItems.rows().size(),
                com.gregtech.gregtech.content.food.GTFoodItems.registered(),
                REGISTERED.size(),
                GTFoodItemsGen.SKIPPED.size());
        return REGISTERED.size();
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
