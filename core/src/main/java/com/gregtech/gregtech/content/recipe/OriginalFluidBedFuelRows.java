/* SPDX-License-Identifier: LGPL-3.0-or-later */
/* Copyright (c) 2026 GregTech-6 Team */
package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.machine.crucible.CrucibleMath;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.data.GregTechConstants;

import java.util.List;

/**
 * Original GT6 Loader_Fuels:37-43 fluidized-bed charges, with MT:1521-1531 fuel data.
 * Keep these source values local to the recipe import: the port's general furnace metadata
 * is incomplete and must not silently remove six of the original fluidized-bed fuels.
 */
public final class OriginalFluidBedFuelRows {
    private OriginalFluidBedFuelRows() {}

    public record Fuel(int materialId, String sourceName, long furnaceBurnTime,
                       int ashMaterialId, long ashAmount) {}

    public record Form(String prefix, long materialAmount, int calcite) {}

    private static final long U = GTValues.U;

    public static final List<Fuel> FUELS = List.of(
            new Fuel(8336, "Charcoal",       1600, 8200, U / 4),
            new Fuel(8334, "Coal",           1600, 8201, U / 4),
            new Fuel(8349, "CoalCoke",       3200, 8201, U / 9),
            new Fuel(8337, "Lignite",         800, 8201, U / 4),
            new Fuel(8365, "LigniteCoke",    1600, 8201, U / 9),
            new Fuel(8362, "Anthracite",     4800, 8201, U / 2),
            new Fuel(8363, "Prismane",       9600, 8201, U),
            new Fuel(8364, "Lonsdaleite",   19200, 8201, U * 2),
            new Fuel(8390, "PetCoke",        6400, 8201, U / 9),
            new Fuel(8360, "Peat",           2000, 8200, U / 2),
            // Original PeatBituminous has the default zero-amount burning target.
            new Fuel(8361, "PeatBituminous", 2400,    0, 0));

    public static final List<Form> FORMS = List.of(
            new Form("blockDust", U * 9, 648),
            new Form("dust",      U,      72),
            new Form("dustSmall", U / 4,  18),
            new Form("dustTiny",  U / 9,   8),
            new Form("dustDiv72", U / 72,  1));

    /** Original negative-one EU/t recipes encode the whole charge in their duration. */
    public static long heatUnits(Fuel fuel, Form form) {
        long unitHeat = fuel.furnaceBurnTime() * 3 * GregTechConstants.EU_PER_FURNACE_TICK;
        return CrucibleMath.units(unitHeat, U, form.materialAmount(), false);
    }

    /** Original OM.dust chooses the actual representable dust stack after this calculation. */
    public static long ashUnits(Fuel fuel, Form form) {
        return CrucibleMath.units(fuel.ashAmount(), U * 2, form.materialAmount(), false);
    }
}
