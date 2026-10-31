package gregtech.content.recipe;

import gregtech.engine.recipe.Recipe;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.material.MaterialTagAliases;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Semantic input groups shared by recipe bootstrap and the built-in server datapack.
 * Membership is collected before world/datapack loading; reloads use the live registry tags.
 * Only interchangeable identities belong here: counts, timing and outputs remain on each recipe.
 */
public final class RecipeInputGroups {
    private static final Map<String, Set<String>> ITEMS = new LinkedHashMap<>();
    private static final Map<String, Set<String>> FLUIDS = new LinkedHashMap<>();

    private RecipeInputGroups() {}

    public record Items(ResourceLocation tag, ItemStack representative) {
        public ItemStack stack(int count) { return representative.copyWithCount(count); }
        public Recipe apply(Recipe recipe, int slot) { return recipe.withItemTagInput(slot, tag); }
    }

    public record Fluids(ResourceLocation tag, FluidStack representative) {
        public FluidStack stack(int amount) { var copy = representative.copy(); copy.setAmount(amount); return copy; }
        public Recipe apply(Recipe recipe, int slot) { return recipe.withFluidTagInput(slot, tag); }
    }

    public static Items items(String path, Collection<ItemStack> choices) { return items(path, choices, true); }

    public static Items exactItems(String path, Collection<ItemStack> choices) { return items(path, choices, false); }

    private static Items items(String path, Collection<ItemStack> choices, boolean equivalents) {
        var ids = new LinkedHashSet<String>();
        ItemStack representative = ItemStack.EMPTY;
        for (var stack : choices) {
            if (stack.isEmpty()) continue;
            if (!ItemStack.matches(stack.copyWithCount(1), new ItemStack(stack.getItem())))
                throw new IllegalArgumentException("Input group cannot discard item data: " + path);
            if (representative.isEmpty()) representative = stack.copyWithCount(1);
            ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            // Preserve the equal-form substitutions that concrete machine inputs already accept.
            // Recolouring uses exactItems: sharing material alone must not erase a panel's identity.
            var form = equivalents ? MaterialEquivalence.form(stack) : null;
            if (form != null) {
                ids.add("#gt6ce:" + form.prefix().getRegistryName() + "/" + MaterialEquivalence.materialName(form.material()));
                String common = MaterialEquivalence.tagPath(form);
                if (common != null) {
                    commonTags(ids, common);
                    commonTags(ids, common.replace("/aluminium", "/aluminum").replace("/quartz", "/nether_quartz").replace("/sulfur", "/sulphur"));
                    String alternate = MaterialTagAliases.alternatePath(common);
                    if (alternate != null) commonTags(ids, alternate);
                }
            }
        }
        return new Items(define(ITEMS, path, ids), representative);
    }

    private static void commonTags(Set<String> values, String path) {
        values.add("#forge:" + path);
//? if >=1.21.1 {
        /*values.add("#c:" + path);
*///?}
    }

    /** External common tags are optional; an absent compatibility mod cannot empty the group. */
    public static synchronized Collection<?> tagValues(String resource, Collection<?> values) {
        String prefix = resource.startsWith("gt6ce:tags/items/") ? "gt6ce:tags/items/" : "gt6ce:tags/item/";
        if (!resource.startsWith(prefix) || !resource.endsWith(".json")
                || !ITEMS.containsKey(resource.substring(prefix.length(), resource.length() - 5))) return values;
        return values.stream().map(value -> value instanceof String id && (id.startsWith("#forge:") || id.startsWith("#c:"))
                ? Map.of("id", id, "required", false) : value).toList();
    }

    public static Fluids fluids(String path, String... fields) {
        var ids = new LinkedHashSet<String>();
        FluidStack representative = FluidStack.EMPTY;
        for (String field : fields) {
            var stack = DyeProcessingRecipes.fluid(field, 1);
            if (representative.isEmpty()) representative = stack;
            ids.add(BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString());
        }
        return new Fluids(define(FLUIDS, path, ids), representative);
    }

    private static synchronized ResourceLocation define(Map<String, Set<String>> catalog, String path, Set<String> ids) {
        if (ids.isEmpty()) throw new IllegalArgumentException("Empty input group: " + path);
        var previous = catalog.putIfAbsent(path, Set.copyOf(ids));
        if (previous != null && !previous.equals(ids)) throw new IllegalStateException("Conflicting input group: " + path);
        return ResourceLocation.fromNamespaceAndPath("gt6ce", path);
    }

    public static synchronized void tags(BiConsumer<String, String> items, BiConsumer<String, String> fluids) {
        ITEMS.forEach((path, members) -> members.forEach(id -> items.accept(path, id)));
        FLUIDS.forEach((path, members) -> members.forEach(id -> fluids.accept(path, id)));
    }

    public record Coolant(String field, int multiplier, Fluids group) {
        public Recipe apply(Recipe recipe) { return group.apply(recipe, 0); }
    }

    private static List<Coolant> coolants;
    public static synchronized List<Coolant> coolants(boolean food) {
        if (coolants == null) coolants = List.of(
                new Coolant("Water", 4, fluids("ordinary_water", "Water", "SpDew", "MnWtr")),
                new Coolant("DistW", 3, fluids("distilled_water", "DistW")),
                new Coolant("Lubricant", 1, fluids("lubricants", "Lubricant", "LubRoCant")));
        return food ? coolants.subList(0, 2) : coolants;
    }
}
