package gregtech.engine.recipe.chem;

import java.util.List;

/**
 * One chemistry recipe in source terms. It has no game stacks, so it can be built and tested
 * without a registry. The Minecraft resolver converts it to a {@code Recipe} exactly once.
 */
public record ChemRecipe(
        String map,
        boolean optimize,
        long eut,
        long ticks,
        List<ChemInput> inputs,
        List<ChemFluidInput> fluidInputs,
        List<ChemFluidOutput> fluidOutputs,
        List<ChemOutput> outputs,
        String source) {

    public ChemRecipe {
        if (map == null || map.isBlank()) throw new IllegalArgumentException("Recipe map");
        if (ticks < 1) throw new IllegalArgumentException("Recipe duration");
        if (inputs == null || fluidInputs == null || fluidOutputs == null || outputs == null)
            throw new IllegalArgumentException("Recipe sides");
        if (inputs.isEmpty() && fluidInputs.isEmpty())
            throw new IllegalArgumentException("Chemistry recipe has no inputs: " + source);
        if (outputs.isEmpty() && fluidOutputs.isEmpty())
            throw new IllegalArgumentException("Chemistry recipe has no outputs: " + source);
        inputs = List.copyOf(inputs);
        fluidInputs = List.copyOf(fluidInputs);
        fluidOutputs = List.copyOf(fluidOutputs);
        outputs = List.copyOf(outputs);
        if (source == null || source.isBlank()) throw new IllegalArgumentException("Recipe source");
    }

    public enum Role {
        CONSUMED,
        KEPT,
        PRESENCE
    }

    public record ChemInput(ChemIngredient ingredient, Role role) {
        public ChemInput {
            if (ingredient == null || role == null) throw new IllegalArgumentException("Chem input");
        }
    }

    public record ChemFluidInput(ChemFluid fluid, Role role) {
        public ChemFluidInput {
            if (fluid == null || role == null) throw new IllegalArgumentException("Chem fluid input");
        }
    }

    public record ChemFluidOutput(ChemFluid fluid) {
        public ChemFluidOutput {
            if (fluid == null) throw new IllegalArgumentException("Chem fluid output");
        }
    }

    public record ChemOutput(ChemIngredient ingredient, int chance) {
        public ChemOutput {
            if (ingredient == null) throw new IllegalArgumentException("Chem output");
            if (chance < 1 || chance > 10_000) throw new IllegalArgumentException("Output chance");
        }
    }
}
