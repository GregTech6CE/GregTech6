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

    /**
     * {@code part} is a GregTech structure part name, {@code air}, a structure casing, an addon part name,
     * a block id such as {@code minecraft:glass}, or a block tag such as {@code #c:glass_blocks}.
     * A block id or tag has to use {@link StructureRole#CASING}.
     */
    public record Cell(int right, int up, int back, String part, StructureRole role) {
        public Cell {
            if (part == null || part.isBlank()) throw new IllegalArgumentException("Missing structure part");
            Objects.requireNonNull(role, "role");
            if (plainBlock(part)) {
                checkPlainBlock(part);
                if (role != StructureRole.CASING)
                    throw new IllegalArgumentException("Plain block '" + part + "' requires the casing role");
            }
        }
    }

    /** A block id contains {@code :}. A block tag starts with {@code #}. Other names stay part names. */
    public static boolean plainBlock(String part) {
        return part != null && (part.startsWith("#") || part.indexOf(':') >= 0);
    }

    /** A plain name must be a resource location. A tag is {@code #} plus that location. */
    public static void checkPlainBlock(String part) {
        String body = part.startsWith("#") ? part.substring(1) : part;
        int colon = body.indexOf(':');
        if (colon <= 0 || colon != body.lastIndexOf(':') || colon == body.length() - 1
                || !body.substring(0, colon).matches("[a-z0-9_.-]+")
                || !body.substring(colon + 1).matches("[a-z0-9/._-]+"))
            throw new IllegalArgumentException("Invalid block name: " + part);
    }
}
