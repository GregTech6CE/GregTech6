package gregtech.api.machine;

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
}
