package gregtech.content.loot;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.CrateMaterialTables;
import gregtech.content.data.MaterialPrefix;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Weight tables of a material crate, independent of item registration.
 *
 * <p>GT6 decides a crate's form, size and material while generating the storage room. The port's
 * single-block crate carries no block entity, so the contents are resolved when it opens. This class
 * owns the weights; {@code MaterialCrateLoot} resolves the item stacks and rolls.</p>
 *
 * <p>Because a crate no longer knows its layer, the size odds are the per-crate average over GT6's
 * layers: 29/72 for dust and raw ore, 29/144 for gems, rounded to 40% and 20% here.</p>
 */
public final class MaterialCrateDistribution {
    public record Form(MaterialPrefix prefix, int size, double chance) {}

    public record Entry(MaterialPrefix prefix, int size, GTMaterial material, double weight) {}

    public record Category(String name, List<Entry> entries, double total) {}

    /** Twelve GT6 metal crate forms: dust twice, ingot four times, plate twice, then the four full ones. */
    private static final Form[] METAL_FORMS = {
        new Form(MaterialPrefix.dust, 16, 2.0 / 12),
        new Form(MaterialPrefix.ingot, 16, 4.0 / 12),
        new Form(MaterialPrefix.plate, 16, 2.0 / 12),
        new Form(MaterialPrefix.dust, 64, 1.0 / 12),
        new Form(MaterialPrefix.plate, 64, 1.0 / 12),
        new Form(MaterialPrefix.ingot, 64, 2.0 / 12)
    };

    /**
     * Per-crate size odds. GT6 rolls the size per layer (dust/ore 1/2, 1/3, 1/4; gems 1/4, 1/6, 1/8);
     * a crate without layer information gets the average over its layers, 29/72 and 29/144, rounded to
     * 40% and 20% here.
     */
    private static final double FULL_ODDS_DUST = 0.40;

    private static final double FULL_ODDS_GEM = 0.20;

    private MaterialCrateDistribution() {}

    /**
     * The five categories. Entries rejected by {@code available} are removed and the category total is
     * recomputed, so a caller that cannot resolve some items still rolls over the remaining weights.
     */
    public static Category[] categories(Predicate<Entry> available) {
        return new Category[] {
            category("metals", CrateMaterialTables.CRATE_METALS, available, METAL_FORMS),
            category(
                    "dusts",
                    CrateMaterialTables.CRATE_DUSTS,
                    available,
                    new Form(MaterialPrefix.dust, 16, 1 - FULL_ODDS_DUST),
                    new Form(MaterialPrefix.dust, 64, FULL_ODDS_DUST)),
            category(
                    "gems",
                    CrateMaterialTables.CRATE_GEMS,
                    available,
                    new Form(MaterialPrefix.gem, 16, 1 - FULL_ODDS_GEM),
                    new Form(MaterialPrefix.gem, 64, FULL_ODDS_GEM)),
            category("woods", CrateMaterialTables.CRATE_WOODS, available, new Form(MaterialPrefix.plate, 64, 1)),
            category(
                    "ores",
                    CrateMaterialTables.CRATE_ORES,
                    available,
                    new Form(MaterialPrefix.oreRaw, 16, 1 - FULL_ODDS_DUST),
                    new Form(MaterialPrefix.oreRaw, 64, FULL_ODDS_DUST))
        };
    }

    /**
     * Multiplicity in {@code materials} is GT6's weight. Forms with the same prefix and size are merged
     * so the table holds one entry per (form, size, material) combination.
     */
    private static Category category(String name, GTMaterial[] materials, Predicate<Entry> available, Form... forms) {
        Map<String, GTMaterial> byName = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (GTMaterial material : materials) {
            byName.putIfAbsent(material.getName(), material);
            counts.merge(material.getName(), 1, Integer::sum);
        }
        Map<String, Double> weights = new LinkedHashMap<>();
        Map<String, Entry> prototypes = new LinkedHashMap<>();
        for (Form form : forms) {
            for (Map.Entry<String, Integer> material : counts.entrySet()) {
                GTMaterial value = byName.get(material.getKey());
                String key = form.prefix().getName() + "|" + form.size() + "|" + material.getKey();
                double weight = form.chance() * material.getValue() / materials.length;
                weights.merge(key, weight, Double::sum);
                prototypes.putIfAbsent(key, new Entry(form.prefix(), form.size(), value, 0));
            }
        }
        List<Entry> entries = new ArrayList<>(weights.size());
        double total = 0;
        for (Map.Entry<String, Double> weight : weights.entrySet()) {
            Entry prototype = prototypes.get(weight.getKey());
            Entry entry = new Entry(prototype.prefix(), prototype.size(), prototype.material(), weight.getValue());
            if (available != null && !available.test(entry)) continue;
            entries.add(entry);
            total += entry.weight();
        }
        return new Category(name, List.copyOf(entries), total);
    }
}
