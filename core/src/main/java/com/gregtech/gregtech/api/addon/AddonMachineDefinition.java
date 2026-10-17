package com.gregtech.gregtech.api.addon;

import com.gregtech.gregtech.api.machine.BasicMachineParameters;
import com.gregtech.gregtech.api.machine.MachineControllerKind;

import java.util.List;
import java.util.Objects;

/**
 * One addon recipe machine. {@code constantEnergy} is the non-timed rule: the machine draws a
 * constant amount whenever it is not a TU machine. {@code efficiency} is parts per 10000.
 */
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
