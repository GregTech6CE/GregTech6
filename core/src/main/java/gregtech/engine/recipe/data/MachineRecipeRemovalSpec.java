package gregtech.engine.recipe.data;

import java.util.List;

/**
 * Deletes static rows from one existing map.
 * {@code clear} removes every static row. Otherwise every listed input and output must match.
 */
public record MachineRecipeRemovalSpec(
        String map,
        boolean clear,
        List<ItemComponent> itemInputs,
        List<FluidComponent> fluidInputs,
        List<ItemResult> itemOutputs,
        List<FluidComponent> fluidOutputs) {
    public MachineRecipeRemovalSpec {
        if (map == null || map.isBlank()) throw new IllegalArgumentException("map");
        itemInputs = List.copyOf(itemInputs);
        fluidInputs = List.copyOf(fluidInputs);
        itemOutputs = List.copyOf(itemOutputs);
        fluidOutputs = List.copyOf(fluidOutputs);
        boolean matched = !itemInputs.isEmpty() || !fluidInputs.isEmpty() || !itemOutputs.isEmpty() || !fluidOutputs.isEmpty();
        if (clear && matched) throw new IllegalArgumentException("clear cannot be combined with a match");
        if (!clear && !matched) throw new IllegalArgumentException("removal needs clear or a match");
    }
}
