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
        if (candidate.mFakeRecipe) return;
        int count = REJECTED.merge(map, 1, Integer::sum);
        if (count <= DETAIL_LIMIT) LOGGER.warn("[{}] Recipe duplicate (same inputs and conditions, kept first): existing {}; skipped {}",
                map.mNameInternal, describe(existing), describe(candidate));
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
            for (Recipe recipe : map.mRecipeList) {
                if (!recipe.mEnabled || recipe.mFakeRecipe || recipe.mSpecialItems != null
                        || recipe.hasItemInputGuards() || recipe.hasSameItemInputPredicates()) continue;
                Conditions conditions = conditions(recipe);
                if (conditions.inputs().items().isEmpty() && conditions.inputs().fluids().isEmpty()) continue;
                Recipe existing = first.putIfAbsent(conditions, recipe);
                if (existing != null && existing != recipe) {
                    if (++duplicates <= DETAIL_LIMIT) LOGGER.warn("[{}] Duplicate machine recipe (same inputs and conditions): first {}; later {}",
                            map.mNameInternal, describe(existing), describe(recipe));
                    continue;
                }
                Recipe sameInputs = variants.putIfAbsent(conditions.inputs(), recipe);
                if (sameInputs != null && sameInputs != recipe) variantsRetained++;
            }
            long[] priorities = map.dynamicProviderPriorities();
            if (priorities.length > 0) LOGGER.info("[{}] Dynamic provider priorities: {}", map.mNameInternal,
                    Arrays.toString(priorities));
            int rejected = REJECTED.getOrDefault(map, 0);
            if (rejected > 0 || duplicates > 0 || variantsRetained > 0) LOGGER.warn(
                    "[{}] Recipe diagnostics: {} duplicate rows skipped, {} duplicates retained, "
                            + "{} condition variants retained; first {} details shown",
                    map.mNameInternal, rejected, duplicates, variantsRetained, DETAIL_LIMIT);
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
        for (int i = 0; i < recipe.mInputs.length; i++) {
            ItemStack stack = recipe.mInputs[i];
            if (stack == null || stack.isEmpty()) continue;
            if (!recipe.mExactItemInputs) stack = MaterialUnification.canonical(stack);
//? if >=1.21.1 {
            /*Object data = stack.getComponentsPatch();
*///?} else {
            Object data = stack.getTag();
//?}
            var key = new StackKey(recipe.itemTag(i) == null ? stack.getItem() : recipe.itemTag(i), data, stack.getCount(),
                    recipe.isCatalystInput(i), false);
            items.merge(key, 1, Integer::sum);
        }
        for (int i = 0; i < recipe.mFluidInputs.length; i++) {
            var stack = recipe.mFluidInputs[i];
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
        var chances = new ArrayList<Long>(recipe.mChances.length);
        for (long chance : recipe.mChances) chances.add(chance);
        return new Conditions(new Inputs(items, fluids), recipe.mSpecialValue, recipe.mPriority,
                recipe.mEUt, recipe.mDuration, recipe.mExactItemInputs,
                recipe.mExplicitCatalystsOnly, recipe.mRequiresEmptyContainerInputs, List.copyOf(chances));
    }

    private static String describe(Recipe recipe) {
        List<String> inputs = new ArrayList<>(), outputs = new ArrayList<>();
        for (ItemStack stack : recipe.mInputs) if (stack != null && !stack.isEmpty())
            inputs.add(stack.getCount() + "x " + BuiltInRegistries.ITEM.getKey(stack.getItem()));
        for (var stack : recipe.mFluidInputs) if (stack != null && !stack.isEmpty())
            inputs.add(stack.getAmount() + "mB " + BuiltInRegistries.FLUID.getKey(stack.getFluid()));
        for (ItemStack stack : recipe.mOutputs) if (stack != null && !stack.isEmpty())
            outputs.add(stack.getCount() + "x " + BuiltInRegistries.ITEM.getKey(stack.getItem()));
        for (var stack : recipe.mFluidOutputs) if (stack != null && !stack.isEmpty())
            outputs.add(stack.getAmount() + "mB " + BuiltInRegistries.FLUID.getKey(stack.getFluid()));
        return inputs + " -> " + outputs + " (power=" + recipe.mEUt + "/t, duration=" + recipe.mDuration
                + "t, chances=" + Arrays.toString(recipe.mChances) + ", special=" + recipe.mSpecialValue
                + ", priority=" + recipe.mPriority
                + (recipe.mSource.isEmpty() ? "" : ", source=" + recipe.mSource) + ")";
    }
}
