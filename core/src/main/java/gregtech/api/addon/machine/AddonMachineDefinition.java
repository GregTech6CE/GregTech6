package gregtech.api.addon.machine;

import gregtech.api.addon.recipe.*;






import gregtech.api.addon.*;

import gregtech.api.machine.BasicMachineParameters;
import gregtech.api.machine.MachineControllerKind;

import java.util.List;
import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */
/**
 * @deprecated Use {@link RecipeMachine}. Removal is 0.10.
 */
@Deprecated(since = "0.0", forRemoval = true)
@AddonApiRemoval(id = "gregtech.api.addon.machine.AddonMachineDefinition", removeAt = "0.10")
public record AddonMachineDefinition(
        BasicMachineParameters parameters,
        String recipeMapName,
        MachineControllerKind kind,
        int efficiency,
        boolean constantEnergy,
        List<String> structureKeys,
        String auxiliaryEnergyType,
        long auxiliaryEnergyIn,
        long auxiliaryEnergyMin,
        long auxiliaryEnergyMax,
        long auxiliaryEnergyCapacity,
        int auxiliaryFaceMask,
        boolean startupEnergy
) {
    /** Older declarations without a second input. */
    public AddonMachineDefinition(BasicMachineParameters parameters, String recipeMapName,
                                  MachineControllerKind kind, int efficiency, boolean constantEnergy,
                                  List<String> structureKeys) {
        this(parameters, recipeMapName, kind, efficiency, constantEnergy, structureKeys,
                null, 0, 0, 0, 0, 0, false);
    }
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
        if (auxiliaryEnergyType != null) {
            if (!auxiliaryEnergyType.matches("[A-Za-z]+"))
                throw new IllegalArgumentException("Invalid auxiliary energy type for " + parameters.id());
            if (auxiliaryEnergyIn < 1 || auxiliaryEnergyMin < 1 || auxiliaryEnergyMax < auxiliaryEnergyMin
                    || auxiliaryEnergyCapacity < 1 || auxiliaryFaceMask == 0)
                throw new IllegalArgumentException("Invalid auxiliary energy for " + parameters.id());
        } else if (startupEnergy || auxiliaryEnergyIn != 0 || auxiliaryEnergyMin != 0 || auxiliaryEnergyMax != 0
                || auxiliaryEnergyCapacity != 0 || auxiliaryFaceMask != 0) {
            throw new IllegalArgumentException("Auxiliary energy fields need a type: " + parameters.id());
        }
    }
}
