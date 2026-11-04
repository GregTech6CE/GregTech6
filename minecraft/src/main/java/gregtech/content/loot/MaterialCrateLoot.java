package gregtech.content.loot;

import gregtech.content.registry.GTItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * The contents of a {@code material_crate}, resolved once from the five GT6 crate material lists.
 *
 * <p>GT6 decides a crate's form, size and material while generating the storage room. The port's
 * single-block crate carries no block entity, so it rolls on opening instead: the five categories stay
 * equally likely and each category keeps GT6's material weights and the per-crate average of its
 * form/size odds. Because a crate no longer knows its layer, the size odds are the average over GT6's
 * layers: 29/72 for dust and raw ore, 29/144 for gems, rounded to 40% and 20% here.</p>
 *
 * <p>The weight tables live in {@link MaterialCrateDistribution} (core); this class resolves the item
 * stacks and rolls. Entries whose item does not exist in this port are skipped when the table is
 * built.</p>
 */
public final class MaterialCrateLoot {
    private record Entry(ItemStack prototype, int count, double weight) {}

    private static final class Category {
        final List<Entry> entries;
        final double total;

        Category(List<Entry> entries, double total) {
            this.entries = entries;
            this.total = total;
        }
    }

    private static volatile Category[] cache;

    private MaterialCrateLoot() {}

    /**
     * One roll: an equal category, then a weighted entry inside it. An empty stack means the current
     * registry has no usable item at all; the caller still hands out the empty crate.
     */
    public static ItemStack roll(RandomSource random) {
        Category[] categories = categories();
        Category chosen = categories[random.nextInt(categories.length)];
        if (chosen.total <= 0) return ItemStack.EMPTY;
        double roll = random.nextDouble() * chosen.total;
        double cumulative = 0;
        for (int i = 0; ; i++) {
            Entry entry = chosen.entries.get(i);
            // The last cumulative is the category total, so roll < total always returns there.
            cumulative = i == chosen.entries.size() - 1 ? chosen.total : cumulative + entry.weight();
            if (roll < cumulative) {
                int count = Math.min(entry.count(), entry.prototype().getMaxStackSize());
                return entry.prototype().copyWithCount(count);
            }
        }
    }

    private static Category[] categories() {
        Category[] result = cache;
        if (result == null) {
            synchronized (MaterialCrateLoot.class) {
                result = cache;
                if (result == null) {
                    result = build();
                    cache = result;
                }
            }
        }
        return result;
    }

    private static Category[] build() {
        MaterialCrateDistribution.Category[] source = MaterialCrateDistribution.categories(
                entry -> !GTItems.getStack(entry.prefix(), entry.material(), 1).isEmpty());
        Category[] result = new Category[source.length];
        for (int i = 0; i < source.length; i++) {
            MaterialCrateDistribution.Category category = source[i];
            List<Entry> entries = new ArrayList<>(category.entries().size());
            for (MaterialCrateDistribution.Entry entry : category.entries()) {
                ItemStack prototype = GTItems.getStack(entry.prefix(), entry.material(), 1);
                entries.add(new Entry(prototype, entry.size(), entry.weight()));
            }
            result[i] = new Category(List.copyOf(entries), category.total());
        }
        return result;
    }
}
