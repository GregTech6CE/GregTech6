package gregtech.content.transport.fluid;

import gregtech.engine.fluid.FluidHazards;
import net.neoforged.neoforge.fluids.FluidStack;

/** NeoForge fermenter lookup, bound by bootstrap after recipe maps and selector items are available. */
public final class FermentationAccess {
    public record Recipe(FluidStack input, FluidStack output, long power, long duration) {}

    @FunctionalInterface
    public interface Provider {
        /** Match enabled/nonfake Fermenter entries with exactly one selector-tag(0) input and one fluid input. */
        Recipe findForSelectorTagZero(FluidStack input);
    }

    private static volatile Provider provider;
    private static volatile boolean builtinProvider;
    /** Selector stack reused read-only; matching never mutates or exposes it. */
    private static volatile net.minecraft.world.item.ItemStack selectorStack;

    private FermentationAccess() {}

    public static void bind(Provider value) {
        provider = java.util.Objects.requireNonNull(value);
        builtinProvider = false;
    }

    public static boolean isBound() {
        return provider != null;
    }

    /** Whether a bounded negative lookup memo is safe; a custom provider is not proven stateless. */
    public static boolean negativeCacheAllowed() {
        return builtinProvider;
    }

    /** Revision of the built-in fermenter table; caches keyed on recipe state must compare it. */
    public static long recipeRevision() {
        return gregtech.content.data.MachineRecipeMaps.Fermenter.lookupRevision();
    }

    private static net.minecraft.world.item.ItemStack selectorStack() {
        var cached = selectorStack;
        if (cached != null) return cached;
        var selector = gregtech.content.registry.GTTechnological.selectorTag(0);
        if (selector == null) return null;
        return selectorStack = new net.minecraft.world.item.ItemStack(selector);
    }

    /** Call after the real platform recipe maps and selector items have initialized. */
    public static void bindOriginalRecipeMaps() {
        bind(fluid -> {
            var item = selectorStack();
            if (item == null) return null;
            var candidates = gregtech.content.data.MachineRecipeMaps.Fermenter.fluidInputCandidates(fluid);
            var rows = candidates != null ? candidates : gregtech.content.data.MachineRecipeMaps.Fermenter.mRecipeList;
            for (var recipe : rows) {
                if (!recipe.mEnabled || recipe.mFakeRecipe) continue;
                if (recipe.mInputs.length != 1) continue;
                if (recipe.mFluidInputs.length != 1 || recipe.mFluidOutputs.length == 0) continue;
                // Barrel conversion cannot preserve a presence-only input while replacing its tank.
                if (recipe.isPresenceFluidInput(0)) continue;
                var input = recipe.mFluidInputs[0];
                var output = recipe.mFluidOutputs[0];
                if (input == null || output == null || input.getFluid() != fluid.getFluid()) continue;
                // Reject unrelated fluids before material/tag matching; keep recipe order and live flags.
                if (!gregtech.engine.recipe.RecipeInputs.matches(recipe.mInputs[0], item)) continue;
                if (FluidHazards.isGas(input.getFluid()) || FluidHazards.isGas(output.getFluid())) continue;
                return new Recipe(input, output, recipe.mEUt, recipe.mDuration);
            }
            return null;
        });
        builtinProvider = true;
    }

    public static Recipe find(FluidStack input) {
        if (input.isEmpty() || provider == null) return null;
        var recipe = provider.findForSelectorTagZero(input);
        if (recipe == null
                || recipe.input() == null
                || recipe.output() == null
                || recipe.input().isEmpty()
                || recipe.output().isEmpty()
                || recipe.input().getFluid() != input.getFluid()
                || FluidHazards.isGas(recipe.input().getFluid())
                || FluidHazards.isGas(recipe.output().getFluid())) return null;
        return recipe;
    }
}
