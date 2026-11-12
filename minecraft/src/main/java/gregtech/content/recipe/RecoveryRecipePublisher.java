package gregtech.content.recipe;

import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.stack.StackCompat;
import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * The single publication and conflict boundary for recovery recipe families.
 *
 * <p>Ownership is stored on the recipe instead of inferred from its diagnostic source. Guarded rows
 * cannot use {@link RecipeMap#findCollision(Recipe)}, so this publisher queries the map's item-input
 * indexes and compares execution conditions only inside the matching buckets.</p>
 */
public final class RecoveryRecipePublisher {
    private static final Logger LOGGER = LogUtils.getLogger();

    public enum Action {
        PUBLISH,
        DUPLICATE,
        EXPLICIT_OWNER,
        REPLACE_GENERIC
    }

    public record Resolution(Action action, Recipe existing) {}

    public static final class RecoveryConflictException extends IllegalStateException {
        private RecoveryConflictException(String message) {
            super(message);
        }
    }

    private RecoveryRecipePublisher() {}

    /** Marks a recipe as the generic fallback of one recovery family. */
    public static Recipe generic(Recipe recipe, String family) {
        return mark(recipe, family, Recipe.RecoveryOwnership.Scope.GENERIC);
    }

    /** Marks a recipe as the explicit owner of an input or receiver-specific route. */
    public static Recipe explicit(Recipe recipe, String family) {
        return mark(recipe, family, Recipe.RecoveryOwnership.Scope.EXPLICIT);
    }

    public static Recipe publish(RecipeMap map, Recipe candidate) {
        return publish(map, candidate, false);
    }

    public static Recipe publish(RecipeMap map, Recipe candidate, boolean hidden) {
        return publish(map, candidate, hidden, false);
    }

    /**
     * Publishes an external, datapack, or addon-derived row while isolating a row conflict. Structural
     * failures still propagate and cause the enclosing generation to fail.
     */
    public static Recipe publishIsolated(RecipeMap map, Recipe candidate) {
        return publish(map, candidate, false, true);
    }

    private static Recipe publish(RecipeMap map, Recipe candidate, boolean hidden, boolean isolated) {
        Resolution resolution;
        try {
            resolution = inspect(map, candidate);
        } catch (RecoveryConflictException conflict) {
            if (!isolated) throw conflict;
            LOGGER.warn("[gregtech] Skipping an isolated recovery row: {}", conflict.getMessage());
            return null;
        }
        if (resolution.action() == Action.DUPLICATE || resolution.action() == Action.EXPLICIT_OWNER) return null;
        if (resolution.action() == Action.REPLACE_GENERIC) {
            if (map.removeAll(List.of(resolution.existing())) == 0)
                throw new IllegalStateException(
                        "Could not replace generic recovery row " + resolution.existing().mSource);
        }
        return map.addRecipe(candidate, false, false, hidden);
    }

    public static Resolution inspect(RecipeMap map, Recipe candidate) {
        if (map == null || candidate == null || candidate.mRecoveryOwnership == null)
            throw new IllegalArgumentException("Recovery publication requires a map, row and ownership");
        Recipe existing = map.findCollision(candidate);
        if (existing == null) existing = indexedCollision(map, candidate);
        if (existing == null) return new Resolution(Action.PUBLISH, null);

        boolean candidateGeneric = generic(candidate);
        boolean existingGeneric = generic(existing);
        if (candidateGeneric && existingGeneric) {
            if (sameOutputs(existing, candidate)) return new Resolution(Action.DUPLICATE, existing);
            throw conflict(candidate, existing);
        }
        if (candidateGeneric) return new Resolution(Action.EXPLICIT_OWNER, existing);
        if (existingGeneric) return new Resolution(Action.REPLACE_GENERIC, existing);
        if (sameOutputs(existing, candidate)) return new Resolution(Action.DUPLICATE, existing);
        throw conflict(candidate, existing);
    }

    public static boolean generic(Recipe recipe) {
        return recipe != null
                && recipe.mRecoveryOwnership != null
                && recipe.mRecoveryOwnership.scope() == Recipe.RecoveryOwnership.Scope.GENERIC;
    }

    /** Finds an already indexed row with the same owned inputs, ignoring execution conditions. */
    public static Recipe matchingInputs(Recipe candidate, Collection<Recipe> rows) {
        if (rows == null) return null;
        for (Recipe row : rows) if (sameOwnedInputs(row, candidate)) return row;
        return null;
    }

    private static Recipe mark(Recipe recipe, String family, Recipe.RecoveryOwnership.Scope scope) {
        if (recipe == null) throw new IllegalArgumentException("Missing recovery recipe");
        recipe.withRecoveryOwnership(family, scope);
        return recipe;
    }

    private static boolean sameOwnedInputs(Recipe left, Recipe right) {
        if (left.mInputs.length != right.mInputs.length || left.mFluidInputs.length != right.mFluidInputs.length)
            return false;
        for (int i = 0; i < left.mInputs.length; i++) {
            ItemStack a = left.mInputs[i];
            ItemStack b = right.mInputs[i];
            boolean emptyA = a == null || a.isEmpty();
            boolean emptyB = b == null || b.isEmpty();
            if (emptyA != emptyB || (!emptyA && (a.getCount() != b.getCount() || !StackCompat.sameItem(a, b))))
                return false;
        }
        for (int i = 0; i < left.mFluidInputs.length; i++)
            if (!StackCompat.sameFluid(left.mFluidInputs[i], right.mFluidInputs[i])) return false;
        return true;
    }

    /**
     * Guarded rows are opaque to the map's collision signature. Item-input pools are already the
     * authoritative reverse index, so publication only visits rows reachable from this candidate.
     */
    private static Recipe indexedCollision(RecipeMap map, Recipe candidate) {
        Set<Recipe> checked = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ItemStack input : candidate.mInputs) {
            if (input == null || input.isEmpty()) continue;
            for (Recipe existing : map.mRecipeItemMap.getOrDefault(input.getItem(), List.of())) {
                if (existing == candidate || !existing.mEnabled || existing.mFakeRecipe || !checked.add(existing))
                    continue;
                if (sameExecution(existing, candidate) && sameInputs(existing, candidate)) return existing;
            }
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
        if (left.mInputs.length != right.mInputs.length || left.mFluidInputs.length != right.mFluidInputs.length)
            return false;
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

    public static boolean sameOutputs(Recipe left, Recipe right) {
        if (left.mOutputs.length != right.mOutputs.length || left.mFluidOutputs.length != right.mFluidOutputs.length)
            return false;
        for (int i = 0; i < left.mOutputs.length; i++) {
            var first = left.mOutputs[i];
            var second = right.mOutputs[i];
            if (first.getCount() != second.getCount() || !StackCompat.sameItem(first, second)) return false;
        }
        for (int i = 0; i < left.mFluidOutputs.length; i++)
            if (!StackCompat.sameFluid(left.mFluidOutputs[i], right.mFluidOutputs[i])) return false;
        return true;
    }

    private static RecoveryConflictException conflict(Recipe candidate, Recipe existing) {
        return new RecoveryConflictException("Conflicting recovery rows with the same inputs and conditions: existing "
                + existing.mSource + " and candidate " + candidate.mSource);
    }
}
