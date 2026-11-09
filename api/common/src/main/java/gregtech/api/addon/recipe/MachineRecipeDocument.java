/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One item added to an existing machine recipe map, as the {@code gt6ce:machine} datapack type
 * describes it.
 *
 * <p>{@code priority} is the row's explicit claim when several rows could match the same machine
 * input; a higher value wins and equal values keep the row published first. It is optional and
 * defaults to zero, which is also every built-in row's priority.</p>
 *
 * <p>Use {@link MachineRecipeDocuments#addition(java.util.Map)} to parse and validate a JSON
 * object, and {@link MachineRecipeDocuments#canonical(MachineRecipeDocument)} to write one.</p>
 */
public record MachineRecipeDocument(
        String map,
        List<ItemComponent> itemInputs,
        List<FluidComponent> fluidInputs,
        List<ItemResult> itemOutputs,
        List<FluidComponent> fluidOutputs,
        long euPerTick,
        long duration,
        long special,
        @Nullable MaterialUnitIngredient materialInput,
        long priority) {
    public MachineRecipeDocument {
        if (map == null || map.isBlank()) throw new IllegalArgumentException("map");
        MachineRecipeDocuments.checkMap(map);
        itemInputs = List.copyOf(itemInputs);
        fluidInputs = List.copyOf(fluidInputs);
        itemOutputs = List.copyOf(itemOutputs);
        fluidOutputs = List.copyOf(fluidOutputs);
        if (itemInputs.isEmpty() && fluidInputs.isEmpty() && materialInput == null)
            throw new IllegalArgumentException("machine recipe has no inputs");
        if (itemOutputs.isEmpty() && fluidOutputs.isEmpty())
            throw new IllegalArgumentException("machine recipe has no outputs");
        if (euPerTick < 0) throw new IllegalArgumentException("eu_per_tick");
        if (duration < 1) throw new IllegalArgumentException("duration");
    }

    public MachineRecipeDocument(
            String map,
            List<ItemComponent> itemInputs,
            List<FluidComponent> fluidInputs,
            List<ItemResult> itemOutputs,
            List<FluidComponent> fluidOutputs,
            long euPerTick,
            long duration,
            long special) {
        this(map, itemInputs, fluidInputs, itemOutputs, fluidOutputs, euPerTick, duration, special, null, 0L);
    }

    public MachineRecipeDocument(
            String map,
            List<ItemComponent> itemInputs,
            List<FluidComponent> fluidInputs,
            List<ItemResult> itemOutputs,
            List<FluidComponent> fluidOutputs,
            long euPerTick,
            long duration,
            long special,
            MaterialUnitIngredient materialInput) {
        this(map, itemInputs, fluidInputs, itemOutputs, fluidOutputs, euPerTick, duration, special, materialInput, 0L);
    }
}
