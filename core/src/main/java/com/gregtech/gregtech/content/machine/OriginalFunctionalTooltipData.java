/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from manual station and MultiTileEntityBoilerTank.addToolTips. */
package com.gregtech.gregtech.content.machine;

import com.gregtech.gregtech.api.machine.BoilerSpec;

/** Original functional tooltip choices and arithmetic; no native rendering/storage types. */
public final class OriginalFunctionalTooltipData {
    private OriginalFunctionalTooltipData() {}
    public record Manual(String recipeKey, String usageKey, String preparationKey, String faceKey,
                         boolean magnifier, boolean facingWrench) {}
    private static final Manual MORTAR = new Manual("gt.recipe.mortar", "gt.lang.recipes.mortar.usage", null, "gt.lang.face.top", false, false);
    private static final Manual GRINDSTONE = new Manual("gt.recipe.sharpener", "gt.lang.recipes.grindstone.usage", "gt.lang.recipes.grindstone.init", "gt.lang.face.any.but.sides", false, true);
    private static final Manual SIFTING = new Manual("gt.recipe.sifter", "gt.lang.recipes.sifter.usage", null, "gt.lang.face.top", false, false);
    private static final Manual MIXING = new Manual("gt.recipe.mixer", "gt.lang.recipes.mixingbowl.usage", null, "gt.lang.face.top", true, false);
    private static final Manual BATH = new Manual("gt.recipe.bath", "gt.lang.recipes.bathingsink.usage", null, "gt.lang.face.top", true, false);
    private static final Manual JUICER = new Manual("gt.recipe.juicer", "gt.lang.recipes.juicer.usage", null, "gt.lang.face.top", true, false);
    public static Manual manual(String id) {
        if (id.equals("MORTAR") || id.startsWith("mortar_")) return MORTAR;
        return switch (id) {
            case "GRINDSTONE", "grindstone_block" -> GRINDSTONE;
            case "SIFTING", "sifting_table" -> SIFTING;
            case "mixing_bowl", "mixing_bowl_table" -> MIXING;
            case "bathing_pot", "bathing_pot_table", "bathing_pot_wood", "bathing_pot_table_wood" -> BATH;
            case "juicer" -> JUICER;
            default -> throw new IllegalArgumentException("Unknown original manual station " + id);
        };
    }
    public record Boiler(int efficiency, long heatInput, long heatCapacity, long steamOutput, long steamCapacity) {}
    public static int boilerEfficiency(long saved) { return (int) Math.max(0, Math.min(10000, saved)); }
    public static Boiler boiler(BoilerSpec spec, long savedEfficiency) {
        int efficiency = boilerEfficiency(savedEfficiency);
        return new Boiler(efficiency, spec.heatInputRecommended(), spec.heatCapacity(),
                spec.steamOutput() * efficiency / 10000, spec.steamCapacity());
    }
    public static String efficiencyPercent(int efficiency) {
        return (efficiency / 100) + "." + (efficiency % 100 < 10 ? "0" : "") + (efficiency % 100);
    }
}
