package gregtech.content.recipe.generated;

import static gregtech.content.recipe.generated.GeneratedRows.*;
import static gregtech.engine.recipe.chem.Rounding.FLOOR;

import gregtech.engine.recipe.chem.ChemRecipe;
import java.util.ArrayList;
import java.util.List;

/**
 * The portable {@code Loader_Recipes_Temporary} rows in exact source terms.
 *
 * <p>Most of that loader rewrites other mods' recipes; only the rows that name this port's own
 * content are transcribed here. The spectral-dew coolant rows and the potion rows are recorded
 * as exclusions because the port retired spectral dew and does not register the wart prefix.</p>
 */
public final class GTTemporaryRecipes {
    private static final String SOURCE = "gt6@3703e403/Loader_Recipes_Temporary:";

    private GTTemporaryRecipes() {}

    public static void register() {
        List<ChemRecipe> rows = new ArrayList<>();
        extruderRows(rows);
        coolantRows(rows);
        reedRows(rows);
        sandRows(rows);
        recordExclusions();
        publish("temporary recipes", rows);
    }

    /** Graphite electrodes wait for the source's pyrolitic route; the extruder rows stay. */
    private static void extruderRows(List<ChemRecipe> rows) {
        row(SOURCE + "54", "Extruder", 512, 512)
                .item(form("dust", "Graphite", 1)).kept(tech("extruder_shape_rod", 1))
                .out(form("stick", "Graphite", 1)).add(rows);
        row(SOURCE + "55", "Extruder", 512, 512)
                .item(form("dust", "Graphite", 2)).kept(tech("extruder_shape_longrod", 1))
                .out(form("stickLong", "Graphite", 1)).add(rows);
    }

    /** Lapis-family coolant rows: one unit with distilled water, two with the other waters. */
    private static void coolantRows(List<ChemRecipe> rows) {
        for (String lapis : new String[] {"Lapis", "Lazurite", "Sodalite"}) {
            row(SOURCE + "52/" + lapis, "Injector", 16, 16)
                    .item(dust(lapis, U(1))).fluid(nativeFluid("DistW", 1000))
                    .out(nativeFluid("Coolant_IC2", 1000)).add(rows);
            row(SOURCE + "52/" + lapis, "Injector", 16, 16)
                    .item(dust(lapis, U(2))).fluid(nativeFluid("MnWtr", 1000))
                    .out(nativeFluid("Coolant_IC2", 1000)).add(rows);
            row(SOURCE + "52/" + lapis, "Injector", 16, 16)
                    .item(dust(lapis, U(2))).fluid(water(waterUnits(1000), FLOOR))
                    .out(nativeFluid("Coolant_IC2", 1000)).add(rows);
        }
    }

    /** Netherlicious reed and foxfire rows. */
    private static void reedRows(List<ChemRecipe> rows) {
        // Loader_Recipes_Temporary:339 iterates waters(125, 100): mineral water 125, distilled 100.
        row(SOURCE + "339/Water", "Bath", 0, 16)
                .item(tech("neli_reed", 1)).fluid(water(waterUnits(125), FLOOR))
                .out(vanilla("paper", 1)).add(rows);
        row(SOURCE + "339/MnWtr", "Bath", 0, 16)
                .item(tech("neli_reed", 1)).fluid(nativeFluid("MnWtr", 125))
                .out(vanilla("paper", 1)).add(rows);
        row(SOURCE + "339/DistW", "Bath", 0, 16)
                .item(tech("neli_reed", 1)).fluid(nativeFluid("DistW", 100))
                .out(vanilla("paper", 1)).add(rows);
        row(SOURCE + "341", "Loom", 16, 16)
                .kept(selector(0)).item(tech("neli_reed", 1)).out(vanilla("paper", 1)).add(rows);
        // The source squeezes and juices the reed before shredding it; both remain applicable.
        row(SOURCE + "342", "Squeezer", 16, 16)
                .item(tech("neli_reed", 1)).out(nativeFluid("Juice_Reed", 10))
                .out(tech("remains_plant", 1), 4000).add(rows);
        row(SOURCE + "343", "Juicer", 16, 16)
                .item(tech("neli_reed", 1)).out(nativeFluid("Juice_Reed", 5))
                .out(tech("remains_plant", 1), 5000).add(rows);
        row(SOURCE + "344", "Shredder", 16, 16)
                .item(tech("neli_reed", 1)).out(tech("remains_plant", 1)).add(rows);
        row(SOURCE + "352", "Mixer", 16, 16)
                .item(tech("neli_foxfire_lily", 1)).item(form("dustTiny", "Blaze", 1))
                .out(tech("neli_foxfire_powder", 1)).add(rows);
        row(SOURCE + "353", "Mixer", 16, 144)
                .item(tech("neli_foxfire_lily", 9)).item(form("dust", "Blaze", 1))
                .out(tech("neli_foxfire_powder", 9)).add(rows);
    }

    /** Rockhounding sands. */
    private static void sandRows(List<ChemRecipe> rows) {
        row(SOURCE + "602", "Sifting", 16, 200)
                .item(tech("rh_sand_olivine", 1)).out(form("gem", "Peridot", 1), 5000)
                .out(form("dust", "Peridot", 1), 5000).add(rows);
        row(SOURCE + "603", "Sifting", 16, 200)
                .item(tech("rh_sand_gypsum", 1)).out(form("dust", "Gypsum", 1), 9000)
                .out(form("dust", "S", 1), 1000).add(rows);
        row(SOURCE + "605", "Sifting", 16, 200)
                .item(tech("rh_sand_magnetite", 1)).out(form("dust", "GraniticMineralSand", 1), 9900)
                .out(form("rockGt", "GraniteBlack", 1), 500).out(form("nugget", "Au", 1), 500).add(rows);
        row(SOURCE + "606", "MagneticSeparator", 16, 144)
                .item(tech("rh_sand_magnetite", 1)).out(form("dust", "GraniticMineralSand", 1), 9900)
                .out(form("rockGt", "GraniteBlack", 1), 500).out(form("nugget", "Au", 1), 500)
                .out(form("dustTiny", "Au", 2), 500).add(rows);
        row(SOURCE + "607", "Centrifuge", 16, 256)
                .item(tech("rh_sand_magnetite", 1)).out(form("dust", "GraniticMineralSand", 1), 9000)
                .out(form("dust", "V2O5", 1), 1000).add(rows);
        // The tropical variant needs another mod's shells and pearls; the plain sand branch applies.
        row(SOURCE + "612", "Sifting", 16, 200)
                .item(tech("rh_sand_coral", 1)).out(vanilla("sand", 1), 9900)
                .out(form("gem", "Azurite", 1), 200).out(form("gem", "Eudialyte", 1), 100)
                .out(form("gem", "Zr", 1), 50).add(rows);
    }

    private static void recordExclusions() {
        for (String lapis : new String[] {"Lapis", "Lazurite", "Sodalite"})
            ChemRecipeResolver.exclude(SOURCE + "55/" + lapis,
                    "spectral dew is retired in this port; the row is not re-expressed");
        ChemRecipeResolver.exclude(SOURCE + "355-363",
                "plantGtWart prefix and the potion fluids are not registered in this port");
        ChemRecipeResolver.exclude(SOURCE + "390-460",
                "other mods' blocks, dyes and plants (hexorium, wolfsbane, Netherlicious wares)");
        ChemRecipeResolver.exclude(SOURCE + "345",
                "pulverizing only registers IC2/ThermalExpansion machines");
        ChemRecipeResolver.exclude(SOURCE + "44",
                "the source removes a crafting recipe; no machine row exists");
    }
}
