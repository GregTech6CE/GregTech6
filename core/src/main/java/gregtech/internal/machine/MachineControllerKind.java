package gregtech.internal.machine;

import gregtech.content.multiblock.SharedLargeMachineLayouts;

/**
     * Which block entity a basic-machine block creates. Addon recipe machines may only use
     * {@link #BASIC} and {@link #LARGE_RECIPE}. {@link #BEHAVIOR} is the addon shell and is
     * created by {@code BehaviorMachineBlock}, not by a recipe controller.
     */
public enum MachineControllerKind {
    BASIC,
    LARGE_RECIPE,
    DISTILLATION,
    COMPACT_MATTER,
    MATTER,
    BEHAVIOR;

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
