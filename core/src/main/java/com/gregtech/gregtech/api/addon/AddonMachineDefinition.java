package com.gregtech.gregtech.api.addon;

import com.gregtech.gregtech.api.machine.BasicMachineParameters;
import com.gregtech.gregtech.api.machine.MachineControllerKind;

import java.util.List;
import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */
/**
 * @deprecated Use {@link MachineDeclaration}. Removal is 0.10.
 */
@Deprecated(since = "0.4", forRemoval = true)
@AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.AddonMachineDefinition", removeAt = "0.10")
public record AddonMachineDefinition(
        BasicMachineParameters parameters,
        String recipeMapName,
        MachineControllerKind kind,
        int efficiency,
        boolean constantEnergy,
        List<String> structureKeys
) {
    public AddonMachineDefinition {
        Objects.requireNonNull(parameters, "parameters");
        if (recipeMapName == null || !recipeMapName.matches("[a-z0-9_.]+"))
            throw new IllegalArgumentException("Invalid recipe map name: " + recipeMapName);
        if (kind != MachineControllerKind.BASIC && kind != MachineControllerKind.LARGE_RECIPE)
            throw new IllegalArgumentException("Addon machine '" + parameters.id()
                    + "' cannot use controller kind " + kind);
        if (efficiency < 1 || efficiency > 100_000)
            throw new IllegalArgumentException("Invalid efficiency for " + parameters.id());
        structureKeys = List.copyOf(structureKeys == null ? List.of() : structureKeys);
        for (String key : structureKeys) {
            if (key == null || !key.matches("[a-z0-9_.]+"))
                throw new IllegalArgumentException("Invalid structure key for " + parameters.id() + ": " + key);
        }
    }
}
