package gregtech.engine.recipe;

import com.mojang.logging.LogUtils;
import gregtech.engine.material.MaterialUnification;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Bounded warnings only: diagnostics never reject a row or prevent startup. */
public final class RecipeDiagnostics {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final int DETAIL_LIMIT = 8;
    private static final Map<RecipeMap, Integer> REJECTED = new IdentityHashMap<>();

    private RecipeDiagnostics() {}

    static void rejected(RecipeMap map, Recipe existing, Recipe candidate) {
        // Fake viewer rows intentionally share inputs with real recipes.
        if (candidate.fakeRecipe) return;
        int count = REJECTED.merge(map, 1, Integer::sum);
        if (count <= DETAIL_LIMIT) LOGGER.warn("[{}] Recipe duplicate (same inputs and conditions, kept first): existing {}; skipped {}",
                map.internalName, describe(existing), describe(candidate));
    }

    /**
     * Linear pass per map also catches producers using the explicit collision-check bypass.
     * A row whose inputs are already present under the same conditions is a duplicate; the same
     * inputs under different conditions (special value, catalyst role, priority, energy, duration,
     * probability) are a legitimate variant and are counted separately. Arbitrary predicates and
     * special slots cannot be compared safely; do not infer equality.
     */
    public static void report() {
        long started = System.nanoTime();
        int totalDuplicates = 0;
        int totalVariants = 0;
        for (RecipeMap map : RecipeMap.RECIPE_MAP_LIST) {
            Map<Conditions, Recipe> first = new HashMap<>();
            Map<Inputs, Recipe> variants = new HashMap<>();
            int duplicates = 0;
            int variantsRetained = 0;
            for (Recipe recipe : map.recipeRows) {
                if (!recipe.enabled || recipe.fakeRecipe
                        || recipe.hasItemInputGuards() || recipe.hasSameItemInputPredicates()) continue;
                Conditions conditions = conditions(recipe);
                if (conditions.inputs().items().isEmpty() && conditions.inputs().fluids().isEmpty()) continue;
                Recipe existing = first.putIfAbsent(conditions, recipe);
                if (existing != null && existing != recipe) {
                    if (++duplicates <= DETAIL_LIMIT) LOGGER.warn("[{}] Duplicate machine recipe (same inputs and conditions): first {}; later {}",
                            map.internalName, describe(existing), describe(recipe));
                    continue;
                }
                Recipe sameInputs = variants.putIfAbsent(conditions.inputs(), recipe);
                if (sameInputs != null && sameInputs != recipe) variantsRetained++;
            }
            long[] priorities = map.dynamicProviderPriorities();
            if (priorities.length > 0) LOGGER.info("[{}] Dynamic provider priorities: {}", map.internalName,
                    Arrays.toString(priorities));
            int rejected = REJECTED.getOrDefault(map, 0);
            if (rejected > 0 || duplicates > 0 || variantsRetained > 0) LOGGER.warn(
                    "[{}] Recipe diagnostics: {} duplicate rows skipped, {} duplicates retained, "
                            + "{} condition variants retained; first {} details shown",
                    map.internalName, rejected, duplicates, variantsRetained, DETAIL_LIMIT);
            totalDuplicates += duplicates;
            totalVariants += variantsRetained;
        }
        LOGGER.info("Machine recipe duplicate scan: {} duplicates, {} condition variants, {} ms",
                totalDuplicates, totalVariants, (System.nanoTime() - started) / 1_000_000L);
        REJECTED.clear();
    }

    private record StackKey(Object type, Object data, int amount, boolean retained, boolean presence) {}
    private record Inputs(Map<StackKey, Integer> items, Map<StackKey, Integer> fluids) {}
    private record Conditions(Inputs inputs, long specialValue, long priority, long euPerTick, long duration,
                              boolean exact, boolean explicitCatalysts, boolean emptyContainers,
                              List<Long> chances) {}

    private static Conditions conditions(Recipe recipe) {
        Map<StackKey, Integer> items = new HashMap<>(), fluids = new HashMap<>();
        for (int i = 0; i < recipe.itemInputs.length; i++) {
            ItemStack stack = recipe.itemInputs[i];
            if (stack == null || stack.isEmpty()) continue;
            if (!recipe.exactItemInputs) stack = MaterialUnification.canonical(stack);
            Object data = gregtech.engine.stack.StackData.dataKey(stack);
            var key = new StackKey(recipe.itemTag(i) == null ? stack.getItem() : recipe.itemTag(i), data, stack.getCount(),
                    recipe.isCatalystInput(i), false);
            items.merge(key, 1, Integer::sum);
        }
        for (int i = 0; i < recipe.fluidInputs.length; i++) {
            var stack = recipe.fluidInputs[i];
            if (stack == null || stack.getFluid() == net.minecraft.world.level.material.Fluids.EMPTY) continue;
//? if >=1.21.1 {
            /*Object data = stack.getComponentsPatch();
*///?} else {
            Object data = stack.getTag();
//?}
            var key = new StackKey(recipe.fluidTag(i) == null ? stack.getFluid() : recipe.fluidTag(i), data, stack.getAmount(),
                    recipe.isCatalystFluidInput(i), recipe.isPresenceFluidInput(i));
            fluids.merge(key, 1, Integer::sum);
        }
        var chances = new ArrayList<Long>(recipe.outputChances.length);
        for (long chance : recipe.outputChances) chances.add(chance);
        return new Conditions(new Inputs(items, fluids), recipe.specialValue, recipe.priority,
                recipe.energyPerTick, recipe.durationTicks, recipe.exactItemInputs,
                recipe.explicitCatalystsOnly, recipe.requiresEmptyContainerInputs, List.copyOf(chances));
    }

    private static String describe(Recipe recipe) {
        List<String> inputs = new ArrayList<>(), outputs = new ArrayList<>();
        for (ItemStack stack : recipe.itemInputs) if (stack != null && !stack.isEmpty())
            inputs.add(stack.getCount() + "x " + BuiltInRegistries.ITEM.getKey(stack.getItem()));
        for (var stack : recipe.fluidInputs) if (stack != null && !stack.isEmpty())
            inputs.add(stack.getAmount() + "mB " + BuiltInRegistries.FLUID.getKey(stack.getFluid()));
        for (ItemStack stack : recipe.itemOutputs) if (stack != null && !stack.isEmpty())
            outputs.add(stack.getCount() + "x " + BuiltInRegistries.ITEM.getKey(stack.getItem()));
        for (var stack : recipe.fluidOutputs) if (stack != null && !stack.isEmpty())
            outputs.add(stack.getAmount() + "mB " + BuiltInRegistries.FLUID.getKey(stack.getFluid()));
        return inputs + " -> " + outputs + " (power=" + recipe.energyPerTick + "/t, duration=" + recipe.durationTicks
                + "t, chances=" + Arrays.toString(recipe.outputChances) + ", special=" + recipe.specialValue
                + ", priority=" + recipe.priority
                + (recipe.source.isEmpty() ? "" : ", source=" + recipe.source) + ")";
    }
}
