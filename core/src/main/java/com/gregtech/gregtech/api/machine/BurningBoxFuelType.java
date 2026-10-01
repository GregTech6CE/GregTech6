package com.gregtech.gregtech.api.machine;

/** The type of fuel accepted by a burning box (GT6 generator family). */
public enum BurningBoxFuelType {
    /** Solid items (coal, charcoal, wood, etc.) — produces ash. */
    SOLID,
    /** Liquid fluids (oils, biomass, ethanol, etc.) — exhaust voided. */
    LIQUID,
    /** Gas fluids (hydrogen, methane, natural gas, etc.) — exhaust voided. */
    GAS,
    /** Dust fuels + molten calcite — higher efficiency. */
    FLUIDIZED_BED;

    public boolean isFluid() {
        return this == LIQUID || this == GAS;
    }

    public boolean gasOnly() {
        return this == GAS;
    }
}
