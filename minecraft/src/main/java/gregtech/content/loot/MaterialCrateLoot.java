package gregtech.content.loot;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.content.worldgen.dungeon.GTDungeonData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * The contents of a {@code material_crate}, resolved once from the five GT6 crate material lists.
 *
 * <p>GT6 decides a crate's form, size and material while generating the storage room. The port's
 * single-block crate carries no block entity, so it rolls on opening instead: the five categories stay
 * equally likely, each category keeps GT6's form/size odds and material weights, and entries whose
 * item does not exist in this port are dropped before the category is normalised. A missing material
 * therefore shifts weight only inside its own category, keeping the layer-independent 40%/20% odds.</p>
 */
public final class MaterialCrateLoot {
    private record Form(MaterialPrefix prefix, int size, double chance) {}

    private record Entry(ItemStack prototype, int count, double weight) {}

    private static final class Category {
        final List<Entry> entries;
        final double total;

        Category(List<Entry> entries, double total) {
            this.entries = entries;
            this.total = total;
        }
    }

    /** Twelve GT6 metal crate forms: dust twice, ingot four times, plate twice, then the four full ones. */
    private static final Form[] METAL_FORMS = {
        new Form(MaterialPrefix.dust, 16, 2.0 / 12),
        new Form(MaterialPrefix.ingot, 16, 4.0 / 12),
        new Form(MaterialPrefix.plate, 16, 2.0 / 12),
        new Form(MaterialPrefix.dust, 64, 1.0 / 12),
        new Form(MaterialPrefix.plate, 64, 1.0 / 12),
        new Form(MaterialPrefix.ingot, 64, 2.0 / 12)
    };

    /** Layer-independent odds: GT6's per-layer rolls average to 40% (dust/ore) and 20% (gems). */
    private static final double FULL_ODDS_DUST = 0.40;

    private static final double FULL_ODDS_GEM = 0.20;

    private static volatile Category[] cache;

    private MaterialCrateLoot() {}

    /**
     * One roll: an equal category, then a weighted entry inside it. An empty stack means the current
     * registry has no usable item at all; the caller still hands out the empty crate.
     */
    public static ItemStack roll(RandomSource random) {
        Category[] categories = categories();
        int usable = 0;
        for (Category category : categories) if (category.total > 0) usable++;
        if (usable == 0) return ItemStack.EMPTY;
        int pick = random.nextInt(usable);
        Category chosen = null;
        for (Category category : categories) {
            if (category.total <= 0) continue;
            if (pick-- == 0) {
                chosen = category;
                break;
            }
        }
        if (chosen == null) return ItemStack.EMPTY;
        double roll = random.nextDouble() * chosen.total;
        for (Entry entry : chosen.entries) {
            roll -= entry.weight();
            if (roll < 0) return entry.prototype().copyWithCount(entry.count());
        }
        Entry last = chosen.entries.get(chosen.entries.size() - 1);
        return last.prototype().copyWithCount(last.count());
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
        return new Category[] {
            category(GTDungeonData.CRATE_METALS, METAL_FORMS),
            category(
                    GTDungeonData.CRATE_DUSTS,
                    new Form(MaterialPrefix.dust, 16, 1 - FULL_ODDS_DUST),
                    new Form(MaterialPrefix.dust, 64, FULL_ODDS_DUST)),
            category(
                    GTDungeonData.CRATE_GEMS,
                    new Form(MaterialPrefix.gem, 16, 1 - FULL_ODDS_GEM),
                    new Form(MaterialPrefix.gem, 64, FULL_ODDS_GEM)),
            category(GTDungeonData.CRATE_WOODS, new Form(MaterialPrefix.plate, 64, 1)),
            category(
                    GTDungeonData.CRATE_ORES,
                    new Form(MaterialPrefix.oreRaw, 16, 1 - FULL_ODDS_DUST),
                    new Form(MaterialPrefix.oreRaw, 64, FULL_ODDS_DUST))
        };
    }

    /**
     * Multiplicity in {@code materials} is GT6's weight. Forms with the same prefix and size are merged
     * so the table holds one entry per usable (form, size, material) combination.
     */
    private static Category category(GTMaterial[] materials, Form... forms) {
        Map<String, GTMaterial> byName = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (GTMaterial material : materials) {
            byName.putIfAbsent(material.getName(), material);
            counts.merge(material.getName(), 1, Integer::sum);
        }
        Map<String, Double> weights = new HashMap<>();
        Map<String, ItemStack> prototypes = new HashMap<>();
        Map<String, Integer> sizes = new HashMap<>();
        for (Form form : forms) {
            for (Map.Entry<String, Integer> material : counts.entrySet()) {
                ItemStack stack = GTItems.getStack(form.prefix(), byName.get(material.getKey()), 1);
                if (stack.isEmpty()) continue;
                String key = form.prefix().getName() + "|" + form.size() + "|" + material.getKey();
                double weight = form.chance() * material.getValue() / materials.length;
                weights.merge(key, weight, Double::sum);
                prototypes.putIfAbsent(key, stack);
                sizes.putIfAbsent(key, form.size());
            }
        }
        List<Entry> entries = new ArrayList<>(weights.size());
        double total = 0;
        for (Map.Entry<String, Double> weight : weights.entrySet()) {
            entries.add(new Entry(prototypes.get(weight.getKey()), sizes.get(weight.getKey()), weight.getValue()));
            total += weight.getValue();
        }
        return new Category(List.copyOf(entries), total);
    }
}
