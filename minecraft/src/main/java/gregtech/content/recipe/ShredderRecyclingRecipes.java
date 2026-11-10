package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialComponent;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.generated.RecyclablePrefixes;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.platform.Mc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * GT6's generic shredder (recycling) rows: {@code Loader_Recipes_Handlers}.
 * <p>
 * GT6 loops over every prefix that is {@code RECYCLABLE}, declares no byproducts and is neither an ore,
 * ore-processing or dust-based form, a container, a cable, a wire or a pipe, and registers
 * "<em>1 item → its pulverized remains</em>" in the Shredder. The loop runs twice: the multiplier is 256
 * for materials that are not {@code MORTAR} grindable and 16 for the ones that are, so brittle materials
 * recycle cheaply. The port imports the prefix filter result through
 * {@code tools/extract_gt6_recyclable_prefixes.py} ({@link RecyclablePrefixes}, 107 names) and the
 * {@code MORTAR} flag through {@code tools/extract_gt6_workability.py}.
 * <p>
 * This is GT6's "recycle anything into dust" mechanic: a broken tool head, a spare gear or an old casing
 * either comes back as material or the player has to store it forever.
 */
public final class ShredderRecyclingRecipes {
    /** One registered recycling route, for tests and reports. */
    public record Entry(String input, Recipe recipe) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<String> SKIPPED = new ArrayList<>();
    private static final java.util.Map<String, Integer> SKIP_REASONS = new java.util.TreeMap<>();

    private static boolean registered;

    private ShredderRecyclingRecipes() {}

    public static List<Entry> entries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public static List<String> skipped() {
        return Collections.unmodifiableList(SKIPPED);
    }

    public static int register() {
        if (registered) throw new IllegalStateException("Shredder recycling recipes registered twice");
        registered = true;
        SKIP_REASONS.clear();
        TreeSet<String> missing = new TreeSet<>();
        for (String name : RecyclablePrefixes.NAMES) {
            MaterialPrefix prefix = PrefixRegistry.byName(name);
            var blockPrefix = java.util.Optional.ofNullable(
                            gregtech.content.data.MaterialPrefixes.all().get(name))
                    .map(gregtech.content.data.MaterialPrefixes.Entry::getBlockDelegate)
                    .orElse(null);
            if (prefix == null && blockPrefix == null) missing.add(name);
        }
        for (var entry : ItemMaterialRegistry.entries().entrySet().stream()
                .sorted(Comparator.comparing(entry -> net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(entry.getKey())
                        .toString()))
                .toList()) {
            ItemStack input = new ItemStack(entry.getKey());
            var composition = entry.getValue();
            MaterialPrefix prefix = composition.prefix();
            if (prefix == null
                    || input.getItem() instanceof BlockItem
                    || composition.material().has(MaterialProperty.ANTIMATTER)
                    || !allows(prefix)
                    || composition.material() != null
                            && containerForm(prefix)
                            && !"Empty".equals(composition.material().resolve().getName())) continue;
            recycle(input, prefix, prefix.getName());
        }
        for (var entry : gregtech.content.data.MaterialPrefixes.all().entrySet()) {
            if (!RecyclablePrefixes.NAMES.contains(PrefixRegistry.sourceName(entry.getKey()))) continue;
            var blockPrefix = entry.getValue().getBlockDelegate();
            if (blockPrefix == null) continue;
            for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
                if (material.has(MaterialProperty.ANTIMATTER)) continue;
                var input = gregtech.content.registry.GTBlocks.getStack(blockPrefix, material);
                recycle(input, null, entry.getKey());
            }
        }
        if (!missing.isEmpty()) SKIPPED.add("prefixes the port does not have: " + missing);
        Mc.logger()
                .info(
                        "Registered {} GT6 shredder recycling recipes ({} prefixes missing: {}, skipped: {})",
                        ENTRIES.size(),
                        missing.size(),
                        missing,
                        SKIP_REASONS);
        return ENTRIES.size();
    }

    private static boolean allows(MaterialPrefix prefix) {
        return RecyclablePrefixes.NAMES.contains(PrefixRegistry.sourceName(prefix.getName()));
    }

    /** Same container rule as the source listener; filled cell forms are not empty-shell recycling. */
    private static boolean containerForm(MaterialPrefix prefix) {
        String source = PrefixRegistry.sourceName(prefix.getName());
        return source.equals("chemtube") || source.equals("cell") || source.equals("capcellcon");
    }

    private static void recycle(ItemStack input, MaterialPrefix expectedPrefix, String name) {
        if (input.isEmpty()) return;
        var data = ItemMaterialRegistry.base(input.getItem()).orElse(null);
        if (data == null
                || ItemMaterialRegistry.recoveryRejection(input) != ItemMaterialRegistry.RecoveryRejection.ELIGIBLE) {
            skip("input");
            return;
        }
        if (data.prefix() != expectedPrefix || !data.material().isValid()) {
            skip("composition-override");
            return;
        }
        GTMaterial material = data.material().resolve();
        long totalUnits;
        try {
            totalUnits = data.components().stream()
                    .mapToLong(MaterialComponent::amount)
                    .reduce(0, Math::addExact);
        } catch (ArithmeticException overflow) {
            skip("composition-overflow");
            return;
        }
        if (totalUnits <= 0) {
            skip("empty-composition");
            return;
        }
        var plan = MaterialRecoveryRules.planShredder(data, MachineRecipeMaps.Shredder.mOutputItemsCount);
        if (!plan.accepted()) {
            skip("plan-" + plan.rejection().name().toLowerCase(Locale.ROOT));
            return;
        }
        var outputs = new ArrayList<ItemStack>();
        for (var output : plan.outputs()) {
            for (var pile : output.dust().piles()) {
                var stack = GTItems.getStack(pile.prefix(), output.material(), pile.count());
                if (stack.isEmpty()) {
                    skip("output-binding");
                    return;
                }
                outputs.add(stack);
            }
        }
        if (outputs.isEmpty()) {
            skip("no-output");
            return;
        }
        // Two GT6 passes: multiplier 256 for hard materials, 16 for MORTAR grindable ones.
        long multiplier = gregtech.content.material.MaterialProcessingProperties.isMortarGrindable(material) ? 16 : 256;
        long duration;
        try {
            duration = Math.max(
                    1,
                    (Math.multiplyExact(Math.multiplyExact(totalUnits, multiplier), material.getToolQuality() + 1L)
                                    + GTValues.U
                                    - 1)
                            / GTValues.U);
        } catch (ArithmeticException overflow) {
            skip("duration-overflow");
            return;
        }
        var template = RecipeMap.make(
                true,
                new ItemStack[] {input},
                outputs.toArray(ItemStack[]::new),
                null,
                null,
                null,
                null,
                duration,
                16,
                0);
        if (template == null) return;
        var rule = new gregtech.engine.recipe.MaterialUnitRecipe(
                template,
                0,
                material,
                totalUnits,
                gregtech.api.addon.recipe.MaterialUnitIngredient.Rounding.WHOLE_OUTPUTS);
        rule.source(input, totalUnits);
        var published = rule.publish(MachineRecipeMaps.Shredder);
        if (published == null) skip("existing-owner");
        else ENTRIES.add(new Entry(name, published));
    }

    private static void skip(String reason) {
        SKIP_REASONS.merge(reason, 1, Integer::sum);
    }
}
