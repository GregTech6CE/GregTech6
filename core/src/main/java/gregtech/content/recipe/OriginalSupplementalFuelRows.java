/* SPDX-License-Identifier: LGPL-3.0-or-later */
/* Copyright (c) 2026 GregTech-6 Team */
package gregtech.content.recipe;

import gregtech.content.data.FluidCatalog.FluidFlags;

import java.util.ArrayList;
import java.util.List;

/**
 * Burn/Gas declarations omitted by the earlier Loader_Fuels import (original:45-189).
 * Registry names include only the original FL.list aliases; category rows use the original
 * RUM/WHISKEY/VINEGAR memberships. Burn and Engine intentionally have different efficiencies.
 */
public final class OriginalSupplementalFuelRows {
    private OriginalSupplementalFuelRows() {}

    public record Row(String kind, long eut, long duration, int amount, long category,
                      List<String> registryNames, String output, int outputAmount,
                      String output2, int output2Amount) {}

    public static final List<Row> ROWS = rows();

    private static List<Row> rows() {
        var rows = new ArrayList<Row>();
        rows.add(burn(4, 1, "glue"));
        rows.add(burn(16, 4, "coconutmilk"));
        rows.add(burn(16, 2, "canolaoil"));
        rows.add(burn(16, 1, "plantoil"));
        rows.add(burn(16, 36, "heavyoil"));
        rows.add(burn(16, 24, "hotcrude"));
        rows.add(burn(16, 18, "lightoil"));
        rows.add(burn(16, 4, "tar", "tarfluid"));

        rows.add(burn(128, 9, "rc jet fuel"));
        rows.add(burn(64, 5, "kerosine", "kerosene"));
        rows.add(burn(64, 5, "petrol", "gasoline"));
        rows.add(burn(64, 5, "diesel"));
        rows.add(burn(64, 6, "fuel"));
        rows.add(burn(64, 9, "nitrofuel"));
        rows.add(category(FluidFlags.RUM));
        rows.add(category(FluidFlags.WHISKEY));
        rows.add(category(FluidFlags.VINEGAR));
        rows.add(burn(16, 6, "binnie.vodka", "potion.vodka", "vodka"));
        rows.add(burn(16, 12, "potion.dragonblood", "potion.goldencider", "potion.notchesbrew"));
        rows.add(burn(16, 9, "bioethanol", "ethanol"));
        rows.add(burn(16, 9, "rc ethanol"));
        rows.add(burn(64, 6, "biodiesel"));
        rows.add(burn(64, 9, "biofuel"));
        rows.add(burn(16, 5, "hootch"));
        rows.add(burn(32, 9, "fire_water"));
        rows.add(burn(64, 4, "rocket_fuel"));

        paired(rows, 16, 1, 2, 2, "Water", 3, null, 0, "hydrogen");
        paired(rows, 16, 1, 2, 2, "GenLiquid_HeavyWater", 3, null, 0, "deuterium");
        paired(rows, 16, 1, 2, 2, "GenLiquid_TritiatedWater", 3, null, 0, "tritium");
        paired(rows, 64, 24, 30, 5, "Water", 6, "CarbonDioxide", 3, "methane");
        paired(rows, 64, 24, 30, 5, "Water", 6, "CarbonDioxide", 3,
                "gas_natural_gas", "naturalgas", "gas.natural");
        paired(rows, 64, 24, 30, 20, "Water", 6, "CarbonDioxide", 3, "ic2biogas");
        rows.add(new Row("burn", 64, 42, 7, 0, List.of("lpg"), "Water", 7, "CarbonDioxide", 6));
        paired(rows, 64, 42, 56, 7, "Water", 7, "CarbonDioxide", 6, "butane");
        paired(rows, 64, 30, 40, 5, "Water", 5, "CarbonDioxide", 4, "propane");
        paired(rows, 64, 4, 5, 1, "Water", 1, "CarbonDioxide", 1, "ethylene");
        paired(rows, 64, 3, 4, 1, "Water", 1, "CarbonDioxide", 1, "propylene");
        return List.copyOf(rows);
    }

    private static Row burn(long eut, long duration, String... names) {
        return new Row("burn", eut, duration, 1, 0, List.of(names), "CarbonDioxide", 1, null, 0);
    }

    private static Row category(long category) {
        return new Row("burn", 16, 6, 1, category, List.of(), "CarbonDioxide", 1, null, 0);
    }

    private static void paired(List<Row> rows, long eut, long burnDuration, long gasDuration,
                               int amount, String output, int outputAmount, String output2,
                               int output2Amount, String... names) {
        rows.add(new Row("burn", eut, burnDuration, amount, 0, List.of(names),
                output, outputAmount, output2, output2Amount));
        rows.add(new Row("gas", eut, gasDuration, amount, 0, List.of(names),
                output, outputAmount, output2, output2Amount));
    }
}
