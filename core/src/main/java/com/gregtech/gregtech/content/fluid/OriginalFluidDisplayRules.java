/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package com.gregtech.gregtech.content.fluid;

import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.data.FluidCatalog.FluidFlags;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Display branches from gregapi.item.ItemFluidDisplay (GregTech-6 Team,
 * Gregorius Techneticies, LGPL-3.0-or-later). Native adapters supply real stack
 * properties and recipe values; neither material RGB nor furnace time is a fluid property.
 */
public final class OriginalFluidDisplayRules {
    public static final String PREFIX = "gregtech.fluid_display.";
    private static final BigInteger UNIT = BigInteger.valueOf(GTValues.U);

    private OriginalFluidDisplayRules() {}

    public enum Color { DEFAULT, BLUE, YELLOW, RED, GREEN, AQUA, GOLD, DARK_GREEN, DARK_GRAY, WHITE }
    public enum Owner { GT6, VANILLA, OTHER }
    /** Null key denotes literal data (formula, registry identity or formatted number). */
    public record Part(String key, Color color, List<Object> arguments) {
        public Part { arguments = List.copyOf(arguments); }
    }
    public record Line(List<Part> parts) {
        public Line { parts = List.copyOf(parts); }
    }
    public record Facts(String registry, boolean advanced, boolean nonstandard, long amount,
                        String formula, long kelvin, boolean gas, boolean plasma, boolean nativeGas,
                        boolean castable, int density, int luminosity, int viscosity, long flags,
                        boolean acid, boolean magic, boolean industrialLubricant, Owner owner) {}
    public record Fuel(String nameKey, BigInteger scaledPower) {}

    /** Original floor(abs(EUt * duration) * U / first fluid input), without long overflow. */
    public static BigInteger fuelPower(long eut, long duration, long firstInputAmount) {
        if (duration <= 0 || firstInputAmount <= 0) return BigInteger.ZERO;
        return BigInteger.valueOf(eut).abs().multiply(BigInteger.valueOf(duration))
                .multiply(UNIT).divide(BigInteger.valueOf(firstInputAmount));
    }

    public static List<Line> describe(Facts f, List<Fuel> fuels) {
        var lines = new ArrayList<Line>();
        if (f.advanced()) add(lines, "registry", Color.DEFAULT, f.registry());
        if (f.nonstandard()) add(lines, "nonstandard", Color.RED);
        if (f.amount() > 0) add(lines, "amount", Color.BLUE, number(BigInteger.valueOf(f.amount())));
        if (!f.formula().isEmpty()) lines.add(new Line(List.of(literal(f.formula(), Color.YELLOW))));
        add(lines, "temperature", Color.RED, Long.toString(f.kelvin()), Long.toString(f.kelvin() - 273));

        var state = new ArrayList<Part>();
        state.add(part("state", Color.GREEN));
        state.add(part(f.plasma() ? "plasma" : f.gas() ? "gas" : "liquid",
                f.plasma() ? Color.YELLOW : f.gas() ? Color.AQUA : Color.BLUE));
        if (f.plasma() || f.gas()) {
            if (!f.nativeGas()) state.add(part("native_liquid", Color.RED));
            else if (f.plasma()) state.add(part("native_gas_note", Color.GOLD));
        } else if (f.castable()) state.add(part("castable", Color.AQUA));
        lines.add(new Line(state));
        if (!f.plasma() && !f.gas() && f.nativeGas()) add(lines, "native_gas_warning", Color.RED);

        add(lines, f.density() > 0 ? "density_heavier" : f.density() < 0 ? "density_lighter" : "density_equal",
                Color.GREEN, Integer.toString(f.density()));
        if (f.luminosity() != 0) add(lines, "luminosity", Color.YELLOW, Integer.toString(f.luminosity()));
        if (f.viscosity() != 0) add(lines, "viscosity", Color.BLUE, Integer.toString(f.viscosity()));
        if ((f.flags() & FluidFlags.COOKING_OIL) != 0) add(lines, "cooking_oil", Color.DARK_GREEN);
        if ((f.flags() & FluidFlags.SIMPLE) != 0) add(lines, "simple", Color.DARK_GREEN);
        if ((f.flags() & FluidFlags.POWER_CONDUCTING) != 0) {
            add(lines, "conducting", Color.DARK_GREEN);
            add(lines, "no_tanks", Color.GOLD);
        }
        if (f.acid()) add(lines, "acid", Color.GOLD);
        if (f.magic()) add(lines, "magic", Color.GOLD);
        if (f.industrialLubricant()) {
            add(lines, "industrial", Color.GOLD);
            add(lines, "not_flammable", Color.RED);
        } else for (Fuel fuel : fuels) {
            if (fuel.scaledPower().signum() <= 0) continue;
            var parts = new ArrayList<Part>();
            parts.add(new Part(fuel.nameKey(), Color.RED, List.of()));
            parts.add(literal(": ", Color.RED));
            parts.add(literal(number(fuel.scaledPower().divide(UNIT)), Color.WHITE));
            parts.add(part("gu_per_liter", Color.YELLOW));
            if (f.amount() > 1) {
                parts.add(literal("; ", Color.YELLOW));
                parts.add(literal(number(fuel.scaledPower().multiply(BigInteger.valueOf(f.amount())).divide(UNIT)), Color.WHITE));
                parts.add(part("gu_total", Color.YELLOW));
            }
            lines.add(new Line(parts));
        }
        add(lines, switch (f.owner()) {
            case GT6 -> "owner_gt6";
            case VANILLA -> "owner_vanilla";
            case OTHER -> "owner_other";
        }, Color.DARK_GRAY);
        return List.copyOf(lines);
    }

    /** UT.Code.makeString grouping, also retaining extreme recipe values without overflow. */
    public static String number(BigInteger value) {
        if (value.abs().compareTo(BigInteger.valueOf(10_000)) < 0) return value.toString();
        String digits = value.toString();
        int first = value.signum() < 0 ? 1 : 0;
        var result = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > first && (digits.length() - i) % 3 == 0) result.append('_');
            result.append(digits.charAt(i));
        }
        return result.toString();
    }

    private static void add(List<Line> lines, String key, Color color, Object... args) {
        lines.add(new Line(List.of(part(key, color, args))));
    }
    private static Part part(String key, Color color, Object... args) {
        return new Part(PREFIX + key, color, List.of(args));
    }
    private static Part literal(String text, Color color) {
        return new Part(null, color, List.of(text));
    }
}
