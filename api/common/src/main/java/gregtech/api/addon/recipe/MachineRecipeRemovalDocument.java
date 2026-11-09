/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import java.util.List;

/**
 * Deletes static rows from one existing map, as the {@code gt6ce:machine_removal} datapack type
 * describes it. {@code clear} removes every static row. Otherwise every listed input and output
 * must match.
 *
 * <p>Use {@link MachineRecipeDocuments#removal(java.util.Map)} to parse and validate a JSON
 * object, and {@link MachineRecipeDocuments#canonical(MachineRecipeRemovalDocument)} to write one.</p>
 */
public record MachineRecipeRemovalDocument(
        String map,
        boolean clear,
        List<ItemComponent> itemInputs,
        List<FluidComponent> fluidInputs,
        List<ItemResult> itemOutputs,
        List<FluidComponent> fluidOutputs) {
    public MachineRecipeRemovalDocument {
        if (map == null || map.isBlank()) throw new IllegalArgumentException("map");
        MachineRecipeDocuments.checkMap(map);
        itemInputs = List.copyOf(itemInputs);
        fluidInputs = List.copyOf(fluidInputs);
        itemOutputs = List.copyOf(itemOutputs);
        fluidOutputs = List.copyOf(fluidOutputs);
        boolean matched =
                !itemInputs.isEmpty() || !fluidInputs.isEmpty() || !itemOutputs.isEmpty() || !fluidOutputs.isEmpty();
        if (clear && matched) throw new IllegalArgumentException("clear cannot be combined with a match");
        if (!clear && !matched) throw new IllegalArgumentException("removal needs clear or a match");
    }
}
