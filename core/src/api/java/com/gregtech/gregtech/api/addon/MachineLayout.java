package com.gregtech.gregtech.api.addon;

import java.util.List;
import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/** One addon multiblock structure. The controller occupies the origin and is not a cell. */
public record MachineLayout(String machineName, List<Cell> cells) {
    public MachineLayout {
        if (machineName == null || !machineName.matches("[a-z0-9_]+"))
            throw new IllegalArgumentException("Invalid layout name: " + machineName);
        cells = List.copyOf(Objects.requireNonNull(cells, "cells"));
        if (cells.isEmpty()) throw new IllegalArgumentException("Layout '" + machineName + "' is empty");
    }

    /** {@code part} is a GregTech structure part name, {@code air}, a structure casing, or an addon part name. */
    public record Cell(int right, int up, int back, String part, StructureRole role) {
        public Cell {
            if (part == null || part.isBlank()) throw new IllegalArgumentException("Missing structure part");
            Objects.requireNonNull(role, "role");
        }
    }
}
