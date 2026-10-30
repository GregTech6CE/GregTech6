/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.machine;

/**
 * Which block entity a basic-machine block creates. Addon recipe machines may only use
 * {@link #BASIC} and {@link #LARGE_RECIPE}. {@link #BEHAVIOR} is the addon shell and is
 * created by {@code BehaviorMachineBlock}, not by a recipe controller.
 *
 * <p>{@link #DISTILLATION}, {@link #COMPACT_MATTER} and {@link #MATTER} are host-internal
 * kinds for built-in machines; addons must not use them.
 */
public enum MachineControllerKind {
    BASIC,
    LARGE_RECIPE,
    /** Host-internal kind for the built-in distillation controller. */
    DISTILLATION,
    /** Host-internal kind for the built-in compact-matter controller. */
    COMPACT_MATTER,
    /** Host-internal kind for the built-in matter controller. */
    MATTER,
    BEHAVIOR;
}
