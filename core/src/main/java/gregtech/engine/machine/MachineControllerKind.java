/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.engine.machine;

/**
 * Host selection of the block entity created for a machine. Addon recipe machines resolve to
 * {@link #BASIC} or {@link #LARGE_RECIPE}. {@link #BEHAVIOR} identifies the addon shell
 * created by {@code BehaviorMachineBlock}.
 *
 * <p>{@link #DISTILLATION}, {@link #COMPACT_MATTER} and {@link #MATTER} are host-internal
 * kinds for built-in machines.</p>
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
