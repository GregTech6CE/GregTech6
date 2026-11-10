package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialComponent;
import gregtech.api.material.MaterialProperty;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.generated.RecyclablePrefixes;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemCompositionSource;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.engine.recipe.MaterialUnitRecipe;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.stack.StackCompat;
import gregtech.platform.Mc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private static final String GENERIC_SOURCE = "recovery:shredder-generic";

    /** One registered recycling route, for tests and reports. */
    public record Entry(String input, Recipe recipe) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<Entry> TAG_ENTRIES = new ArrayList<>();
    private static final List<String> SKIPPED = new ArrayList<>();
    private static final Map<String, Integer> SKIP_REASONS = new java.util.TreeMap<>();
    private static final Map<MaterialUnitRecipe, Recipe> TAG_OWNED = new IdentityHashMap<>();

    private static boolean registered;

    private ShredderRecyclingRecipes() {}

    public static List<Entry> entries() {
        var entries = new ArrayList<Entry>(ENTRIES.size() + TAG_ENTRIES.size());
        entries.addAll(ENTRIES);
        entries.addAll(TAG_ENTRIES);
        return Collections.unmodifiableList(entries);
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
        for (var entry : sortedEntries()) {
            var composition = entry.getValue();
            if (isTagOwned(composition)) continue;
            if (entry.getKey() instanceof BlockItem) continue;
            recycle(entry.getKey(), ENTRIES);
        }
        for (var entry : gregtech.content.data.MaterialPrefixes.all().entrySet()) {
            if (!RecyclablePrefixes.NAMES.contains(PrefixRegistry.sourceName(entry.getKey()))) continue;
            var blockPrefix = entry.getValue().getBlockDelegate();
            if (blockPrefix == null) continue;
            for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
                if (material.has(MaterialProperty.ANTIMATTER)) continue;
                var input = gregtech.content.registry.GTBlocks.getStack(blockPrefix, material);
                if (!(input.getItem() instanceof BlockItem)) continue;
                publish(input, ENTRIES, new RecyclableForm(null, PrefixRegistry.sourceName(entry.getKey())));
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

    /** Rebuilds only tag-derived rows; explicit startup rows stay in the captured recipe baseline. */
    public static int rebuildTagRows() {
        if (!registered || !ItemMaterialRegistry.tagInputReady()) return TAG_ENTRIES.size();
        var planned = new ArrayList<Planned>();
        for (var entry : sortedEntries()) {
            var composition = entry.getValue();
            if (isTagOwned(composition)) planned.add(prepare(entry.getKey(), composition));
        }
        clearTagRows();
        try {
            for (var candidate : planned) {
                if (candidate == null || candidate.rule() == null) continue;
                beforePublish(candidate.rule().template);
                var published = candidate.rule().publish(MachineRecipeMaps.Shredder);
                if (published == null) {
                    skip("tag-existing-owner");
                    continue;
                }
                TAG_OWNED.put(candidate.rule(), published);
                TAG_ENTRIES.add(new Entry(candidate.name(), published));
            }
        } catch (RuntimeException | Error failure) {
            clearTagRows();
            throw failure;
        }
        return TAG_ENTRIES.size();
    }

    public static void clearTagRows() {
        for (var rule : List.copyOf(TAG_OWNED.keySet())) rule.remove();
        TAG_OWNED.clear();
        TAG_ENTRIES.clear();
    }

    public static Set<Recipe> owned() {
        return Set.copyOf(TAG_OWNED.values());
    }

    private static boolean allows(MaterialPrefix prefix) {
        return RecyclablePrefixes.NAMES.contains(PrefixRegistry.sourceName(prefix.getName()));
    }

    /** Same container rule as the source listener; filled cell forms are not empty-shell recycling. */
    private static boolean containerForm(MaterialPrefix prefix) {
        String source = PrefixRegistry.sourceName(prefix.getName());
        return source.equals("chemtube") || source.equals("cell") || source.equals("capcellcon");
    }

    private static List<Map.Entry<net.minecraft.world.item.Item, ItemComposition>> sortedEntries() {
        return ItemMaterialRegistry.entries().entrySet().stream()
                .sorted(Comparator.comparing(entry -> net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(entry.getKey())
                        .toString()))
                .toList();
    }

    private static boolean isTagOwned(ItemComposition composition) {
        return composition.sources().stream().anyMatch(source -> source.kind() == ItemCompositionSource.Kind.FORM_TAG);
    }

    private static void recycle(net.minecraft.world.item.Item item, List<Entry> destination) {
        ItemStack input = new ItemStack(item);
        var data = ItemMaterialRegistry.base(item).orElse(null);
        var form = recyclableForm(input, data);
        if (form == null) return;
        publish(input, destination, form);
    }

    private static void publish(ItemStack input, List<Entry> destination, RecyclableForm form) {
        var data = ItemMaterialRegistry.base(input.getItem()).orElse(null);
        if (data == null || form == null) return;
        var candidate = prepare(input, data, form);
        if (candidate == null || candidate.rule() == null) return;
        beforePublish(candidate.rule().template);
        var published = candidate.rule().publish(MachineRecipeMaps.Shredder);
        if (published == null) skip("existing-owner");
        else destination.add(new Entry(candidate.name(), published));
    }

    private static Planned prepare(net.minecraft.world.item.Item item, ItemComposition data) {
        ItemStack input = new ItemStack(item);
        var form = recyclableForm(input, data);
        return form == null ? null : prepare(input, data, form);
    }

    private static Planned prepare(ItemStack input, ItemComposition data, RecyclableForm form) {
        if (input.isEmpty()) return null;
        if (data == null
                || ItemMaterialRegistry.recoveryRejection(input) != ItemMaterialRegistry.RecoveryRejection.ELIGIBLE) {
            skip("input");
            return null;
        }
        if (data.prefix() != form.prefix() || !data.material().isValid()) {
            skip("composition-override");
            return null;
        }
        GTMaterial material = data.material().resolve();
        long totalUnits;
        try {
            totalUnits = data.components().stream()
                    .mapToLong(MaterialComponent::amount)
                    .reduce(0, Math::addExact);
        } catch (ArithmeticException overflow) {
            skip("composition-overflow");
            return null;
        }
        if (totalUnits <= 0) {
            skip("empty-composition");
            return null;
        }
        var plan = MaterialRecoveryRules.planShredder(data, MachineRecipeMaps.Shredder.mOutputItemsCount);
        if (!plan.accepted()) {
            skip("plan-" + plan.rejection().name().toLowerCase(Locale.ROOT));
            return null;
        }
        var outputs = new ArrayList<ItemStack>();
        for (var output : plan.outputs()) {
            for (var pile : output.dust().piles()) {
                var stack = GTItems.getStack(pile.prefix(), output.material(), pile.count());
                if (stack.isEmpty()) {
                    skip("output-binding");
                    return null;
                }
                outputs.add(stack);
            }
        }
        if (outputs.isEmpty()) {
            skip("no-output");
            return null;
        }
        var template = RecipeMap.make(
                true,
                new ItemStack[] {input},
                outputs.toArray(ItemStack[]::new),
                null,
                null,
                null,
                null,
                plan.duration(),
                16,
                0);
        if (template == null) return null;
        template.withSource(GENERIC_SOURCE);
        var rule = new MaterialUnitRecipe(
                template,
                0,
                material,
                totalUnits,
                gregtech.api.addon.recipe.MaterialUnitIngredient.Rounding.WHOLE_OUTPUTS);
        rule.source(input, totalUnits);
        return new Planned(form.name(), rule);
    }

    private static void beforePublish(Recipe candidate) {
        var collision = MachineRecipeMaps.Shredder.findCollision(candidate);
        if (collision != null && GENERIC_SOURCE.equals(collision.mSource)) {
            if (sameOutputs(collision, candidate)) return;
            throw conflict(candidate, collision);
        }
    }

    private static IllegalStateException conflict(Recipe candidate, Recipe existing) {
        return new IllegalStateException("Conflicting Shredder recovery rows for "
                + candidate.mInputs[0].getHoverName().getString() + ": existing "
                + existing.mOutputs.length + " outputs at " + existing.mDuration + "t, candidate "
                + candidate.mOutputs.length + " outputs at " + candidate.mDuration + "t");
    }

    private static boolean sameOutputs(Recipe left, Recipe right) {
        if (left.mOutputs.length != right.mOutputs.length
                || left.mFluidOutputs.length != right.mFluidOutputs.length
                || !java.util.Arrays.equals(left.mChances, right.mChances)) return false;
        for (int i = 0; i < left.mOutputs.length; i++)
            if (!ItemStack.matches(left.mOutputs[i], right.mOutputs[i])) return false;
        for (int i = 0; i < left.mFluidOutputs.length; i++)
            if (!StackCompat.sameFluid(left.mFluidOutputs[i], right.mFluidOutputs[i])) return false;
        return true;
    }

    private static RecyclableForm recyclableForm(ItemStack input, ItemComposition data) {
        if (data == null || data.material() == null || data.material().has(MaterialProperty.ANTIMATTER)) return null;
        MaterialPrefix prefix = data.prefix();
        if (prefix != null) {
            return allows(prefix)
                            && !(containerForm(prefix)
                                    && !"Empty".equals(data.material().resolve().getName()))
                    ? new RecyclableForm(prefix, prefix.getName())
                    : null;
        }
        if (input.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof MaterialBlockLike block
                && RecyclablePrefixes.NAMES.contains(
                        PrefixRegistry.sourceName(block.prefix().getName()))) {
            return new RecyclableForm(
                    null, PrefixRegistry.sourceName(block.prefix().getName()));
        }
        return null;
    }

    private static void skip(String reason) {
        SKIP_REASONS.merge(reason, 1, Integer::sum);
    }

    private record RecyclableForm(MaterialPrefix prefix, String name) {}

    private record Planned(String name, MaterialUnitRecipe rule) {}
}
