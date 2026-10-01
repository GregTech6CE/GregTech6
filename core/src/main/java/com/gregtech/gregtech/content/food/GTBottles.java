package com.gregtech.gregtech.content.food;

import com.gregtech.gregtech.data.generated.GTBottlesGen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GT6's drink bottles ({@code MultiItemBottles}): which fluid each of the port's bottle items holds.
 *
 * <p>The port registers the bottle <em>items</em> from {@code GTMultiItemsGen} (transpiled by
 * {@code tools/transpile_gt6_multiitems.py}); the fluid inside them is a separate generated column
 * ({@link GTBottlesGen}, {@code tools/extract_gt6_bottles.py}) because GT6 keeps it in the same
 * {@code addItem(...)} call. A bottle is 250&nbsp;mB of that fluid and reads its numbers from
 * {@link GTDrinks} through {@code FoodStatFluid}, exactly like GT6's {@code MultiItemBottles}.
 *
 * <p>The stored value is GT6's <em>fluid expression</em> ({@code Beer}, {@code potion.x},
 * {@code HolyWater}), not a port fluid id: it is resolved with the port's own lookups
 * ({@link GTDrinks#fluidForField}) so renamed port fluids need no change in the generated table.
 */
public final class GTBottles {
    /** Bottle item id → GT6 fluid expression, in GT6 file order. */
    private static final Map<String, String> FLUID_BY_ID = new LinkedHashMap<>();

    static {
        String[] entries = GTBottlesGen.ENTRIES;
        for (int i = 0; i + 2 < entries.length; i += 3) {
            FLUID_BY_ID.put(entries[i], entries[i + 2]);
        }
    }

    private GTBottles() {}

    /** The GT6 fluid expression of a bottle item id, or {@code null} when the id is not a drink bottle. */
    public static String fluidKeyOf(String itemId) {
        return itemId == null ? null : FLUID_BY_ID.get(itemId);
    }

    /** How many bottles the generated table covers (GT6 registers ~180; the rest are other mods' bottles). */
    public static int registered() {
        return FLUID_BY_ID.size();
    }

    /** Bottle rows without a readable fluid expression, with the reason (GT6's loot bottle has none). */
    public static List<String> skipped() {
        return GTBottlesGen.SKIPPED;
    }
}
