package gregtech.engine.recipe;

import java.util.List;

/** Conservative whole-domain collision checks: a single overlapping member never rejects a tag. */
final class RecipeTagInputs {
    private RecipeTagInputs() {}

    static boolean covers(Recipe existing, Recipe candidate) {
        if (existing.mFakeRecipe || existing.hasItemInputGuards() || existing.hasSameItemInputPredicates()
                || candidate.hasItemInputGuards() || candidate.hasSameItemInputPredicates()
                || existing.mInputs.length != candidate.mInputs.length
                || existing.mFluidInputs.length != candidate.mFluidInputs.length) return false;
        return pair(0, new boolean[candidate.mInputs.length], (a, b) -> itemCovers(existing, a, candidate, b))
                && pair(0, new boolean[candidate.mFluidInputs.length], (a, b) -> fluidCovers(existing, a, candidate, b));
    }

    private static boolean itemCovers(Recipe a, int ai, Recipe b, int bi) {
        var required = a.mInputs[ai];
        var offered = b.mInputs[bi];
        if (required == null || required.isEmpty() || offered == null || offered.isEmpty())
            return (required == null || required.isEmpty()) && (offered == null || offered.isEmpty());
        if (required.getCount() > offered.getCount() || a.isCatalystInput(ai) != b.isCatalystInput(bi)) return false;
        if (a.itemTag(ai) != null && a.itemTag(ai).equals(b.itemTag(bi))) return true;
        var choices = b.itemTag(bi) == null ? List.of(offered) : b.viewerInputAlternatives(bi);
        if (choices.isEmpty()) return false;
        for (var stack : choices) {
            if (a.itemTag(ai) != null) {
                if (!a.matchesItemInput(ai, stack)) return false;
            } else {
                // Fluid tags must not change concrete-item collision rules. Equal-material
                // aliases may deliberately have distinct outputs (e.g. legacy wood species).
//? if >=1.21.1 {
                /*if (!net.minecraft.world.item.ItemStack.isSameItemSameComponents(required, stack)) return false;
*///?} else {
                if (!net.minecraft.world.item.ItemStack.isSameItemSameTags(required, stack)) return false;
//?}
            }
        }
        return true;
    }

    private static boolean fluidCovers(Recipe a, int ai, Recipe b, int bi) {
        var required = a.mFluidInputs[ai];
        var offered = b.mFluidInputs[bi];
        if (required == null || required.isEmpty() || offered == null || offered.isEmpty())
            return (required == null || required.isEmpty()) && (offered == null || offered.isEmpty());
        if (a.mFluidInputs[ai].getAmount() > b.mFluidInputs[bi].getAmount()
                || a.isPresenceFluidInput(ai) != b.isPresenceFluidInput(bi)
                || a.isCatalystFluidInput(ai) != b.isCatalystFluidInput(bi)) return false;
        if (a.fluidTag(ai) != null && a.fluidTag(ai).equals(b.fluidTag(bi))) return true;
        var choices = b.viewerFluidInputAlternatives(bi);
        return !choices.isEmpty() && choices.stream().allMatch(stack -> a.matchesFluidInput(ai, stack));
    }

    private static boolean pair(int input, boolean[] used, java.util.function.BiPredicate<Integer, Integer> covers) {
        if (input == used.length) return true;
        for (int candidate = 0; candidate < used.length; candidate++) {
            if (used[candidate] || !covers.test(input, candidate)) continue;
            used[candidate] = true;
            if (pair(input + 1, used, covers)) return true;
            used[candidate] = false;
        }
        return false;
    }
}
