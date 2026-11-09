package gregtech.content.food;

import gregtech.content.data.generated.GTFoodStatsGen;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code CS.FoodsGT}, reduced to the medical items that lower radiation.
 *
 * <p>GT6 keys the table by {@code ItemStack} (NBT-sensitive first, then a stack-size-1 fallback) and fills
 * it from {@code MultiItemFood} plus the ore-dictionary listeners of {@code Loader_Recipes_Food} /
 * {@code Loader_Recipes_Crops}. The port has no ore dictionary and no NBT-carrying meta items, so the table
 * is an identity map from {@link Item} to its radiation removal value, hand-written as the two medical
 * rows in {@link GTFoodStatsGen}.
 *
 * <p>Radiation shares its own tracker with {@link gregtech.content.nuclear.PlayerRadiation}, exactly as
 * GT6's {@code mRadiation} shares {@code EntityFoodTracker} with its radiation branch. Food and drink no
 * longer add radiation; only reactors and the two medical items change the dose.
 */
public final class GTFoodStats {
    private static final Map<Item, Integer> BY_ITEM = new IdentityHashMap<>();

    /** One {@code FoodsGT.put} row: the item and its radiation value. */
    public record Row(Item item, int radiation) {}

    static {
        for (Row row : GTFoodStatsGen.ROWS) {
            BY_ITEM.put(row.item(), row.radiation());
        }
    }

    private GTFoodStats() {}

    /** GT6 {@code FoodsGT.get}: the radiation value of a food, or null when it is not in the table. */
    public static Integer radiation(ItemStack stack) {
        return stack.isEmpty() ? null : BY_ITEM.get(stack.getItem());
    }

    /** How many food items carry a radiation value. */
    public static int registered() {
        return BY_ITEM.size();
    }
}
