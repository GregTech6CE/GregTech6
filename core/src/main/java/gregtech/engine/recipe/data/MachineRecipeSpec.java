package gregtech.engine.recipe.data;

import java.util.List;

/** One datapack row added to an existing machine recipe map. */
public record MachineRecipeSpec(
        String map,
        List<ItemComponent> itemInputs,
        List<FluidComponent> fluidInputs,
        List<ItemResult> itemOutputs,
        List<FluidComponent> fluidOutputs,
        long euPerTick,
        long duration,
        long special,
        gregtech.api.addon.recipe.MaterialUnitIngredient materialInput) {
    public MachineRecipeSpec(
            String map,
            List<ItemComponent> items,
            List<FluidComponent> fluids,
            List<ItemResult> outputs,
            List<FluidComponent> fluidOutputs,
            long power,
            long duration,
            long special) {
        this(map, items, fluids, outputs, fluidOutputs, power, duration, special, null);
    }

    public MachineRecipeSpec {
        if (map == null || map.isBlank()) throw new IllegalArgumentException("map");
        itemInputs = List.copyOf(itemInputs);
        fluidInputs = List.copyOf(fluidInputs);
        itemOutputs = List.copyOf(itemOutputs);
        fluidOutputs = List.copyOf(fluidOutputs);
        if (fluidOutputs.stream().anyMatch(fluid -> fluid.tag() != null))
            throw new IllegalArgumentException("fluid output does not take a tag");
        if (itemInputs.isEmpty() && fluidInputs.isEmpty() && materialInput == null)
            throw new IllegalArgumentException("machine recipe has no inputs");
        if (itemOutputs.isEmpty() && fluidOutputs.isEmpty())
            throw new IllegalArgumentException("machine recipe has no outputs");
        if (euPerTick < 0) throw new IllegalArgumentException("eu_per_tick");
        if (duration < 1) throw new IllegalArgumentException("duration");
    }
}
