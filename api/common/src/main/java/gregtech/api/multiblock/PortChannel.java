/* SPDX-License-Identifier: LGPL-3.0-or-later */
package gregtech.api.multiblock;

/** Independent actions on a controller-backed structure port. */
public enum PortChannel {
    ITEM_INPUT, ITEM_OUTPUT, FLUID_INPUT, FLUID_OUTPUT, ENERGY_INPUT, ENERGY_OUTPUT;

    public boolean energy() { return this == ENERGY_INPUT || this == ENERGY_OUTPUT; }
}
