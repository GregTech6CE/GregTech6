package gregtech.api.addon;

import gregtech.api.multiblock.StructureGrid;

import java.util.List;
import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */
/**
 * @deprecated Use {@link StructureLayout}. Removal is 0.10.
 */
@Deprecated(since = "0.0", forRemoval = true)
@AddonApiRemoval(id = "gregtech.api.addon.AddonMachineLayout", removeAt = "0.10")
public record AddonMachineLayout(String machineName, List<Cell> cells) {
    public AddonMachineLayout {
        if (machineName == null || !machineName.matches("[a-z0-9_]+"))
            throw new IllegalArgumentException("Invalid layout name: " + machineName);
        cells = List.copyOf(Objects.requireNonNull(cells, "cells"));
        if (cells.isEmpty()) throw new IllegalArgumentException("Layout '" + machineName + "' is empty");
    }

    /** {@code part} is a {@code SharedLargeMachineParts.Part.name}, {@code air}, or one of the structure casings. */
    public record Cell(int right, int up, int back, String part, StructureGrid.Role role) {
        public Cell {
            if (part == null || part.isBlank()) throw new IllegalArgumentException("Missing structure part");
            Objects.requireNonNull(role, "role");
        }
    }
}
