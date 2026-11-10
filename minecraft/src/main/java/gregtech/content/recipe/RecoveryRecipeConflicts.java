package gregtech.content.recipe;

import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.stack.StackCompat;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Ownership and output checks shared by generic and explicitly owned recovery rows. */
final class RecoveryRecipeConflicts {
    enum Action {
        PUBLISH,
        DUPLICATE,
        SPECIFIC_OWNER,
        REPLACE_GENERIC
    }

    record Resolution(Action action, Recipe existing) {}

    private RecoveryRecipeConflicts() {}

    static Resolution inspect(RecipeMap map, Recipe candidate) {
        Recipe existing = map.findCollision(candidate);
        if (existing == null) existing = guardedCollision(map, candidate);
        if (existing == null) return new Resolution(Action.PUBLISH, null);
        boolean candidateGeneric = generic(candidate);
        boolean existingGeneric = generic(existing);
        if (candidateGeneric && existingGeneric) {
            if (sameOutputs(existing, candidate)) return new Resolution(Action.DUPLICATE, existing);
            throw conflict(candidate, existing);
        }
        if (candidateGeneric) return new Resolution(Action.SPECIFIC_OWNER, existing);
        if (existingGeneric) return new Resolution(Action.REPLACE_GENERIC, existing);
        if (sameOutputs(existing, candidate)) return new Resolution(Action.DUPLICATE, existing);
        throw conflict(candidate, existing);
    }

    static Recipe publish(RecipeMap map, Recipe candidate) {
        var resolution = inspect(map, candidate);
        if (resolution.action() == Action.PUBLISH) return map.addRecipe(candidate, false, false, false);
        if (resolution.action() == Action.REPLACE_GENERIC) {
            if (map.removeAll(List.of(resolution.existing())) == 0)
                throw new IllegalStateException("Could not replace generic recovery row " + resolution.existing().mSource);
            return map.addRecipe(candidate, false, false, false);
        }
        return null;
    }

    static boolean generic(Recipe recipe) {
        return recipe != null && recipe.mSource.endsWith("-generic");
    }

    /**
     * The map deliberately treats guarded inputs as opaque. Recovery rows still share exact item,
     * fluid, timing and catalyst conditions, so compare those facts without interpreting predicates.
     */
    private static Recipe guardedCollision(RecipeMap map, Recipe candidate) {
        for (Recipe existing : map.mRecipeList) {
            if (existing == candidate || !existing.mEnabled || existing.mFakeRecipe) continue;
            if (sameExecution(existing, candidate) && sameInputs(existing, candidate)) return existing;
        }
        return null;
    }

    private static boolean sameExecution(Recipe left, Recipe right) {
        return left.mSpecialValue == right.mSpecialValue
                && left.mPriority == right.mPriority
                && left.mEUt == right.mEUt
                && left.mDuration == right.mDuration
                && left.mNeedsEmptyOutput == right.mNeedsEmptyOutput
                && left.mExactItemInputs == right.mExactItemInputs
                && left.mExplicitCatalystsOnly == right.mExplicitCatalystsOnly
                && left.mRequiresEmptyContainerInputs == right.mRequiresEmptyContainerInputs
                && left.mMaterialRecovery == right.mMaterialRecovery
                && Arrays.equals(left.mChances, right.mChances)
                && Arrays.equals(left.explicitCatalystInputs(), right.explicitCatalystInputs())
                && Arrays.equals(left.explicitCatalystFluidInputs(), right.explicitCatalystFluidInputs())
                && Arrays.equals(left.explicitPresenceFluidInputs(), right.explicitPresenceFluidInputs());
    }

    private static boolean sameInputs(Recipe left, Recipe right) {
        if (left.mInputs.length != right.mInputs.length
                || left.mFluidInputs.length != right.mFluidInputs.length) return false;
        for (int i = 0; i < left.mInputs.length; i++) {
            ItemStack a = left.mInputs[i];
            ItemStack b = right.mInputs[i];
            boolean emptyA = a == null || a.isEmpty();
            boolean emptyB = b == null || b.isEmpty();
            if (emptyA != emptyB || (!emptyA && !ItemStack.matches(a, b))) return false;
        }
        for (int i = 0; i < left.mFluidInputs.length; i++)
            if (!StackCompat.sameFluid(left.mFluidInputs[i], right.mFluidInputs[i])) return false;
        return true;
    }

    private static boolean sameOutputs(Recipe left, Recipe right) {
        if (left.mOutputs.length != right.mOutputs.length
                || left.mFluidOutputs.length != right.mFluidOutputs.length)
            return false;
        for (int i = 0; i < left.mOutputs.length; i++)
            if (!ItemStack.matches(left.mOutputs[i], right.mOutputs[i])) return false;
        for (int i = 0; i < left.mFluidOutputs.length; i++)
            if (!StackCompat.sameFluid(left.mFluidOutputs[i], right.mFluidOutputs[i])) return false;
        return true;
    }

    private static IllegalStateException conflict(Recipe candidate, Recipe existing) {
        return new IllegalStateException("Conflicting recovery rows with the same inputs and conditions: existing "
                + existing.mSource + " and candidate " + candidate.mSource);
    }
}
