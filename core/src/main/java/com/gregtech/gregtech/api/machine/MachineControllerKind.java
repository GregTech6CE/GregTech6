package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.content.multiblock.SharedLargeMachineLayouts;

/** Which block entity a basic-machine block creates. Addon machines may only use {@link #BASIC} and {@link #LARGE_RECIPE}. */
public enum MachineControllerKind {
    BASIC,
    LARGE_RECIPE,
    DISTILLATION,
    COMPACT_MATTER,
    MATTER;

    /**
     * Built-in selection. Special controllers win over a layout of the same name, matching the
     * historical {@code createBlockEntity} order.
     */
    public static MachineControllerKind builtIn(String machineName) {
        return switch (machineName) {
            case "massfab" -> COMPACT_MATTER;
            case "largemassfab" -> MATTER;
            case "distillationtower" -> DISTILLATION;
            default -> SharedLargeMachineLayouts.cells(machineName) != null ? LARGE_RECIPE : BASIC;
        };
    }
}
