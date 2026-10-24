/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon;

/** A recipe machine, a behavior machine, or a fuel generator. */
public sealed interface AddonMachine permits RecipeMachine, BehaviorMachine, FuelGenerator {
    MachineCasing casing();
}
