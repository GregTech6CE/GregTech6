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
        // Matching, not a first-match scan: a broad removal condition must not consume the one
        // input that a later, more specific condition can only match. Each condition and entry are
        // compared once, then bipartite matching decides the assignment, so large lists stay
        // polynomial instead of enumerating every permutation.
        return assign(
                        spec.itemInputs(),
                        recipe.inputItems(),
                        (need, i) -> itemMatches(
                                need, recipe.inputItem(i), recipe.inputItemTag(i), recipe.inputItemCount(i), tags))
                && assign(
                        spec.fluidInputs(),
                        recipe.inputFluids(),
                        (need, i) -> fluidInputMatches(
                                need, recipe.inputFluid(i), recipe.inputFluidTag(i), recipe.inputFluidAmount(i), tags))
                && assign(spec.itemOutputs(), recipe.outputItems(), (need, i) -> outputMatches(need, recipe, i))
                && assign(
                        spec.fluidOutputs(),
                        recipe.outputFluids(),
                        (need, i) -> fluidMatches(
                                need.fluid(), need.amount(), recipe.outputFluid(i), recipe.outputFluidAmount(i)));
    }

    @FunctionalInterface
    private interface SlotTest<T> {
        boolean accepts(T need, int index);
    }

    /** One need per distinct recipe entry; adjacency is built once and solved as a matching. */
    private static <T> boolean assign(java.util.List<T> needs, int size, SlotTest<T> accepts) {
        if (needs.isEmpty()) return true;
        if (needs.size() > size) return false;
        var adjacency = new boolean[needs.size()][size];
        for (int need = 0; need < needs.size(); need++)
            for (int slot = 0; slot < size; slot++) adjacency[need][slot] = accepts.accepts(needs.get(need), slot);
        var slotOwner = new int[size];
        java.util.Arrays.fill(slotOwner, -1);
        for (int need = 0; need < needs.size(); need++)
            if (!augment(need, adjacency, slotOwner, new boolean[size])) return false;
        return true;
    }

    /** Kuhn's augmenting path: a need may displace the need currently holding a slot. */
    private static boolean augment(int need, boolean[][] adjacency, int[] slotOwner, boolean[] seen) {
        for (int slot = 0; slot < slotOwner.length; slot++) {
            if (seen[slot] || !adjacency[need][slot]) continue;
            seen[slot] = true;
            if (slotOwner[slot] < 0 || augment(slotOwner[slot], adjacency, slotOwner, seen)) {
                slotOwner[slot] = need;
                return true;
            }
        }
        return false;
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
