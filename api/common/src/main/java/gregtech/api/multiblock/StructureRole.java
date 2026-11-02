/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.multiblock;

/** What a multiblock cell does. The names match GregTech's structure grid. */
public enum StructureRole {
    AIR,
    CASING,
    LOGISTICS,
    HEAT_INPUT,
    CRUCIBLE,
    FLUID_INPUT,
    FLUID_OUTPUT,
    FLUID_IO,
    ITEM_IO,
    ITEM_FLUID_IO,
    ITEM_FLUID_INPUT,
    ITEM_FLUID_OUTPUT,
    ENERGY_INPUT,
    ITEM_FLUID_ENERGY_INPUT,
    ITEM_FLUID_ENERGY,
    ENERGY_OUTPUT;

    /** Shared transport semantics for recipe, behavior and fuel controllers. */
    public boolean allows(PortChannel channel) {
        return switch (channel) {
            case ITEM_INPUT -> this == ITEM_IO || this == ITEM_FLUID_IO || this == ITEM_FLUID_INPUT
                    || this == ITEM_FLUID_ENERGY_INPUT || this == ITEM_FLUID_ENERGY;
            case ITEM_OUTPUT -> this == ITEM_IO || this == ITEM_FLUID_IO || this == ITEM_FLUID_OUTPUT
                    || this == ITEM_FLUID_ENERGY;
            case FLUID_INPUT -> this == FLUID_INPUT || this == FLUID_IO || this == ITEM_FLUID_IO
                    || this == ITEM_FLUID_INPUT || this == ITEM_FLUID_ENERGY_INPUT || this == ITEM_FLUID_ENERGY;
            case FLUID_OUTPUT -> this == FLUID_OUTPUT || this == FLUID_IO || this == ITEM_FLUID_IO
                    || this == ITEM_FLUID_OUTPUT || this == ITEM_FLUID_ENERGY;
            case ENERGY_INPUT -> this == ENERGY_INPUT || this == ITEM_FLUID_ENERGY_INPUT || this == ITEM_FLUID_ENERGY;
            case ENERGY_OUTPUT -> this == ENERGY_OUTPUT || this == ITEM_FLUID_ENERGY;
        };
    }
}
