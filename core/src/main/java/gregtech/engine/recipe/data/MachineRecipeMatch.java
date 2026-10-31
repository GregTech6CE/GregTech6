package gregtech.engine.recipe.data;

/** Static-row matching for {@link MachineRecipeRemovalSpec}. Dynamic providers are not visible here. */
public final class MachineRecipeMatch {
    private MachineRecipeMatch() {}

    /** A recipe reduced to ids, counts and chances. Empty stacks are already omitted. */
    public interface RecipeView {
        int inputItems();

        String inputItem(int index);

        default String inputItemTag(int index) {
            return null;
        }

        int inputItemCount(int index);

        int inputFluids();

        String inputFluid(int index);

        default String inputFluidTag(int index) {
            return null;
        }

        long inputFluidAmount(int index);

        int outputItems();

        String outputItem(int index);

        int outputItemCount(int index);

        int outputItemChance(int index);

        int outputFluids();

        String outputFluid(int index);

        long outputFluidAmount(int index);
    }

    public interface TagQuery {
        boolean itemInTag(String itemId, String tagId);

        default boolean fluidInTag(String fluidId, String tagId) {
            return false;
        }

        default boolean itemTagsOverlap(String a, String b) {
            return a.equals(b);
        }

        default boolean fluidTagsOverlap(String a, String b) {
            return a.equals(b);
        }
    }

    public static boolean matches(MachineRecipeRemovalSpec spec, RecipeView recipe, TagQuery tags) {
        if (spec.clear()) return true;
        var usedItems = new boolean[recipe.inputItems()];
        for (var need : spec.itemInputs()) {
            int found = -1;
            for (int i = 0; i < recipe.inputItems(); i++) {
                if (usedItems[i]
                        || !itemMatches(
                                need, recipe.inputItem(i), recipe.inputItemTag(i), recipe.inputItemCount(i), tags))
                    continue;
                found = i;
                break;
            }
            if (found < 0) return false;
            usedItems[found] = true;
        }
        var usedFluids = new boolean[recipe.inputFluids()];
        for (var need : spec.fluidInputs()) {
            int found = -1;
            for (int i = 0; i < recipe.inputFluids(); i++) {
                if (usedFluids[i]
                        || !fluidInputMatches(
                                need, recipe.inputFluid(i), recipe.inputFluidTag(i), recipe.inputFluidAmount(i), tags))
                    continue;
                found = i;
                break;
            }
            if (found < 0) return false;
            usedFluids[found] = true;
        }
        var usedOutputs = new boolean[recipe.outputItems()];
        for (var need : spec.itemOutputs()) {
            int found = -1;
            for (int i = 0; i < recipe.outputItems(); i++) {
                if (usedOutputs[i] || !outputMatches(need, recipe, i)) continue;
                found = i;
                break;
            }
            if (found < 0) return false;
            usedOutputs[found] = true;
        }
        var usedFluidOutputs = new boolean[recipe.outputFluids()];
        for (var need : spec.fluidOutputs()) {
            int found = -1;
            for (int i = 0; i < recipe.outputFluids(); i++) {
                if (usedFluidOutputs[i]
                        || !fluidMatches(
                                need.fluid(), need.amount(), recipe.outputFluid(i), recipe.outputFluidAmount(i)))
                    continue;
                found = i;
                break;
            }
            if (found < 0) return false;
            usedFluidOutputs[found] = true;
        }
        return true;
    }

    private static boolean itemMatches(ItemComponent need, String item, String tag, int count, TagQuery tags) {
        if (need.count() != 0 && need.count() != count) return false;
        if (tag != null)
            return need.tag() != null ? tags.itemTagsOverlap(need.tag(), tag) : tags.itemInTag(need.item(), tag);
        if (need.item() != null) return need.item().equals(item);
        return tags.itemInTag(item, need.tag());
    }

    private static boolean fluidInputMatches(
            FluidComponent need, String fluid, String tag, long amount, TagQuery tags) {
        if (need.amount() != 0 && need.amount() != amount) return false;
        if (tag != null)
            return need.tag() != null ? tags.fluidTagsOverlap(need.tag(), tag) : tags.fluidInTag(need.fluid(), tag);
        return need.tag() != null
                ? tags.fluidInTag(fluid, need.tag())
                : need.fluid().equals(fluid);
    }

    private static boolean outputMatches(ItemResult need, RecipeView recipe, int index) {
        if (!need.item().equals(recipe.outputItem(index))) return false;
        if (need.count() != 0 && need.count() != recipe.outputItemCount(index)) return false;
        return need.chance() == 0 || need.chance() == recipe.outputItemChance(index);
    }

    private static boolean fluidMatches(String fluid, long amount, String actual, long actualAmount) {
        if (!fluid.equals(actual)) return false;
        return amount == 0 || amount == actualAmount;
    }
}
