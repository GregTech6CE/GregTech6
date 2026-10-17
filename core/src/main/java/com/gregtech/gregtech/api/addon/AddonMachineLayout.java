package com.gregtech.gregtech.api.addon;

import com.gregtech.gregtech.api.multiblock.StructureGrid;

import java.util.List;
import java.util.Objects;

/** One addon multiblock structure. The controller occupies the origin and is not a cell. */
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
