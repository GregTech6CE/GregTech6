package gregtech.content.recipe.generated;

import static gregtech.content.recipe.generated.GeneratedRows.*;
import static gregtech.engine.recipe.chem.Rounding.CEIL;
import static gregtech.engine.recipe.chem.Rounding.FLOOR;

import gregtech.engine.recipe.chem.ChemRecipe;
import java.util.ArrayList;
import java.util.List;

/**
 * The portable {@code Loader_Recipes_Ores} rows in exact source terms.
 *
 * <p>The port's own ore chains register first and win every collision. Rows this producer does
 * not own stay with their reviewed producers: the magnetic-separator block-dust calls and the
 * fixed purified sifter calls. Optional other-mod sands and fossils are recorded as exclusions;
 * no group or tag stands in for a missing item.</p>
 */
public final class GTOreRecipes {
    private static final String SOURCE = "gt6@3703e403/Loader_Recipes_Ores:row/";

    private GTOreRecipes() {}

    /** {@code easyU235Extraction} gates only the two refined U238 centrifuge rows. */
    public static void register(boolean easyU235Extraction) {
        List<ChemRecipe> rows = new ArrayList<>();
        sluiceRows(rows);
        sourceRows(rows);
        publish("ore recipes", rows);
        if (easyU235Extraction) {
            List<ChemRecipe> easy = new ArrayList<>();
            easyUraniumRows(easy);
            publish("easy U235 extraction recipes", easy);
        } else {
            ChemRecipeResolver.exclude(SOURCE + "79/80",
                    "easy U235 extraction is disabled; the two refined U238 rows are conditional");
        }
        ChemRecipeResolver.exclude(SOURCE + "owned",
                "magnetic-separator block dust and purified sifter rows are owned by their reviewed producers");
        ChemRecipeResolver.exclude(SOURCE + "108-159",
                "the purified-acid vitriol loops are owned by SharedPurifiedAcidRows");
        ChemRecipeResolver.exclude(SOURCE + "54-57/174-190",
                "bedrock, moon and end rows need retired materials; Cryotheum/Pyrotheum loops need retired fluids");
        ChemRecipeResolver.exclude(SOURCE + "63",
                "the volcanic-lava arm requires a fluid this port does not register");
        ChemRecipeResolver.exclude(SOURCE + "45/108/417-443",
                "each four-water loop keeps its water, mineral-water and distilled-water arms; spectral dew is retired");
        ChemRecipeResolver.exclude(SOURCE + "optional",
                "other mods' sands and fossils are absent; the rows are not approximated");
    }

    /** Sluice water loops and the drying/centrifuge rows that turn the sluice fluid back into sand. */
    private static void sluiceRows(List<ChemRecipe> rows) {
        row(SOURCE, "Mixer", 16, 16, 1).item(dust("SluiceSand", units(72072000))).fluid(water(waterUnits(100), FLOOR)).out(nativeFluid("Sluice", 100)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 2).item(dust("SluiceSand", thousandths(1000))).fluid(water(waterUnits(900), FLOOR)).out(nativeFluid("Sluice", 900)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 3).item(dust("SluiceSand", units(72072000))).fluid(nativeFluid("MnWtr", 100)).out(nativeFluid("Sluice", 100)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 4).item(dust("SluiceSand", thousandths(1000))).fluid(nativeFluid("MnWtr", 900)).out(nativeFluid("Sluice", 900)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 5).item(dust("SluiceSand", units(72072000))).fluid(nativeFluid("DistW", 100)).out(nativeFluid("Sluice", 100)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 6).item(dust("SluiceSand", thousandths(1000))).fluid(nativeFluid("DistW", 900)).out(nativeFluid("Sluice", 900)).add(rows);
        row(SOURCE, "Drying", 16, 100, 7).fluid(nativeFluid("Sluice", 100)).out(dust("SluiceSand", units(72072000))).out(nativeFluid("DistW", 50)).add(rows);
        row(SOURCE, "Centrifuge", 64, 16, 8).fluid(nativeFluid("Sluice", 100)).out(form("dustTiny", "Stone", 1), 9640).out(form("dustTiny", "Cu", 2), 100).out(form("dustTiny", "Sn", 1), 100).out(form("dustTiny", "Zn", 1), 100).out(form("dustTiny", "Sb", 1), 100).out(form("dustTiny", "Cr", 1), 100).out(water(waterUnits(50), FLOOR)).add(rows);
        row(SOURCE, "MagneticSeparator", 16, 16, 9).item(form("dustTiny", "SluiceSand", 1)).out(form("dustTiny", "Stone", 1), 9640).out(form("dustTiny", "Fe", 2), 72).out(form("dustTiny", "Nd", 1), 72).out(form("dustTiny", "Ni", 1), 72).out(form("dustTiny", "Co", 1), 72).out(form("dustTiny", "Mn", 1), 72).add(rows);
        row(SOURCE, "MagneticSeparator", 16, 36, 10).item(form("dustSmall", "SluiceSand", 1)).out(form("dustSmall", "Stone", 1), 9640).out(form("dustTiny", "Fe", 2), 162).out(form("dustTiny", "Nd", 1), 162).out(form("dustTiny", "Ni", 1), 162).out(form("dustTiny", "Co", 1), 162).out(form("dustTiny", "Mn", 1), 162).add(rows);
        row(SOURCE, "MagneticSeparator", 16, 144, 11).item(form("dust", "SluiceSand", 1)).out(form("dust", "Stone", 1), 9640).out(form("dustTiny", "Fe", 2), 648).out(form("dustTiny", "Nd", 1), 648).out(form("dustTiny", "Ni", 1), 648).out(form("dustTiny", "Co", 1), 648).out(form("dustTiny", "Mn", 1), 648).add(rows);
    }

    /** Portable ore-processing rows outside the sluice loop. */
    private static void sourceRows(List<ChemRecipe> rows) {
        row(SOURCE, "Drying", 16, 2592, 12).item(form("crushedPurified", "Biotite", 1)).out(form("crushedCentrifuged", "Biotite", 1)).out(gas("Ar", thousandths(90), CEIL)).add(rows);
        row(SOURCE, "Drying", 16, 288, 13).item(form("crushedPurifiedTiny", "Biotite", 1)).out(form("crushedCentrifugedTiny", "Biotite", 1)).out(gas("Ar", thousandths(10), CEIL)).add(rows);
        row(SOURCE, "Centrifuge", 64, 16, 14).item(form("dustTiny", "RareEarth", 1)).out(form("dustTiny", "Nd", 8), 72).out(form("dustTiny", "Y", 8), 72).out(form("dustTiny", "La", 8), 72).out(form("dustTiny", "Ce", 8), 72).out(form("dustTiny", "Cd", 8), 72).out(form("dustTiny", "Cs", 8), 72).add(rows);
        row(SOURCE, "Centrifuge", 64, 36, 15).item(form("dustSmall", "RareEarth", 1)).out(form("dustTiny", "Nd", 8), 162).out(form("dustTiny", "Y", 8), 162).out(form("dustTiny", "La", 8), 162).out(form("dustTiny", "Ce", 8), 162).out(form("dustTiny", "Cd", 8), 162).out(form("dustTiny", "Cs", 8), 162).add(rows);
        row(SOURCE, "Centrifuge", 64, 144, 16).item(form("dust", "RareEarth", 1)).out(form("dustTiny", "Nd", 8), 648).out(form("dustTiny", "Y", 8), 648).out(form("dustTiny", "La", 8), 648).out(form("dustTiny", "Ce", 8), 648).out(form("dustTiny", "Cd", 8), 648).out(form("dustTiny", "Cs", 8), 648).add(rows);
        row(SOURCE, "Centrifuge", 64, 16, 17).item(form("crushedCentrifugedTiny", "RareEarth", 1)).out(form("dustTiny", "Nd", 8), 80).out(form("dustTiny", "Y", 8), 80).out(form("dustTiny", "La", 8), 80).out(form("dustTiny", "Ce", 8), 80).out(form("dustTiny", "Cd", 8), 80).out(form("dustTiny", "Cs", 8), 80).add(rows);
        row(SOURCE, "Centrifuge", 64, 144, 18).item(form("crushedCentrifuged", "RareEarth", 1)).out(form("dustTiny", "Nd", 8), 720).out(form("dustTiny", "Y", 8), 720).out(form("dustTiny", "La", 8), 720).out(form("dustTiny", "Ce", 8), 720).out(form("dustTiny", "Cd", 8), 720).out(form("dustTiny", "Cs", 8), 720).add(rows);
        row(SOURCE, "Centrifuge", 64, 16, 19).item(form("dustTiny", "PlatinumGroupSludge", 1)).out(form("dustTiny", "Ru", 8), 72).out(form("dustTiny", "Rh", 8), 72).out(form("dustTiny", "Pd", 8), 72).out(form("dustTiny", "Os", 8), 72).out(form("dustTiny", "Ir", 8), 72).out(form("dustTiny", "Pt", 8), 72).add(rows);
        row(SOURCE, "Centrifuge", 64, 36, 20).item(form("dustSmall", "PlatinumGroupSludge", 1)).out(form("dustTiny", "Ru", 8), 162).out(form("dustTiny", "Rh", 8), 162).out(form("dustTiny", "Pd", 8), 162).out(form("dustTiny", "Os", 8), 162).out(form("dustTiny", "Ir", 8), 162).out(form("dustTiny", "Pt", 8), 162).add(rows);
        row(SOURCE, "Centrifuge", 64, 144, 21).item(form("dust", "PlatinumGroupSludge", 1)).out(form("dustTiny", "Ru", 8), 648).out(form("dustTiny", "Rh", 8), 648).out(form("dustTiny", "Pd", 8), 648).out(form("dustTiny", "Os", 8), 648).out(form("dustTiny", "Ir", 8), 648).out(form("dustTiny", "Pt", 8), 648).add(rows);
        row(SOURCE, "Centrifuge", 64, 16, 22).item(form("crushedCentrifugedTiny", "PlatinumGroupSludge", 1)).out(form("dustTiny", "Ru", 8), 80).out(form("dustTiny", "Rh", 8), 80).out(form("dustTiny", "Pd", 8), 80).out(form("dustTiny", "Os", 8), 80).out(form("dustTiny", "Ir", 8), 80).out(form("dustTiny", "Pt", 8), 80).add(rows);
        row(SOURCE, "Centrifuge", 64, 144, 23).item(form("crushedCentrifuged", "PlatinumGroupSludge", 1)).out(form("dustTiny", "Ru", 8), 720).out(form("dustTiny", "Rh", 8), 720).out(form("dustTiny", "Pd", 8), 720).out(form("dustTiny", "Os", 8), 720).out(form("dustTiny", "Ir", 8), 720).out(form("dustTiny", "Pt", 8), 720).add(rows);
        row(SOURCE, "Centrifuge", 64, 288, 24).fluid(nativeFluid("Lava", 100)).out(form("dustTiny", "Basalt", 1), 8000).out(form("nugget", "Cu", 8), 250).out(form("nugget", "Sn", 4), 250).out(form("nugget", "Pb", 2), 250).out(form("nugget", "Au", 1), 250).out(form("nugget", "Ag", 1), 250).add(rows);
        row(SOURCE, "Centrifuge", 64, 144, 25).fluid(nativeFluid("Lava_Pahoehoe", 100)).out(form("dustTiny", "Basalt", 1), 9000).out(form("nugget", "Cu", 8), 300).out(form("nugget", "Sn", 4), 300).out(form("nugget", "Pb", 2), 300).out(form("nugget", "Au", 1), 300).out(form("nugget", "Ag", 1), 300).add(rows);
        row(SOURCE, "Centrifuge", 64, 144, 26).fluid(nativeFluid("Lava", 100)).out(form("dustTiny", "Basalt", 4), 9500).out(form("nugget", "Cu", 8), 1000).out(form("nugget", "Sn", 4), 1000).out(form("nugget", "Pb", 2), 1000).out(form("nugget", "Au", 1), 1000).out(form("nugget", "Ag", 1), 1000).add(rows);
        row(SOURCE, "Centrifuge", 64, 144, 27).item(vanilla("soul_sand", 1)).out(form("dust", "Sand", 9), 8000).out(form("dustTiny", "Niter", 9), 1000).out(form("dustTiny", "Coal", 9), 1000).out(form("dustTiny", "Bone", 9), 1000).out(nativeFluid("Oil_Soulsand", 45)).add(rows);
        row(SOURCE, "Centrifuge", 64, 16, 28).item(form("dust", "SoulSand", 1)).out(form("dust", "Sand", 1), 8000).out(form("dustTiny", "Niter", 1), 1000).out(form("dustTiny", "Coal", 1), 1000).out(form("dustTiny", "Bone", 1), 1000).out(nativeFluid("Oil_Soulsand", 5)).add(rows);
        row(SOURCE, "Centrifuge", true, 64, 144, 29).item(blockDust("Netherrack", 1)).out(form("dust", "Stone", 9), 8000).out(form("dustTiny", "S", 18), 1000).out(form("dustTiny", "Redstone", 9), 500).out(form("dustTiny", "Coal", 9), 500).out(form("dustTiny", "Au", 9), 100).add(rows);
        row(SOURCE, "Centrifuge", 64, 16, 30).item(form("dust", "Netherrack", 1)).out(form("dust", "Stone", 1), 8000).out(form("dustTiny", "S", 2), 1000).out(form("dustTiny", "Redstone", 1), 500).out(form("dustTiny", "Coal", 1), 500).out(form("dustTiny", "Au", 1), 100).add(rows);
        row(SOURCE, "Bath", 0, 256, 31).item(form("crushedPurified", "Au", 1)).fluid(liquid("AquaRegia", thousandths(6500), CEIL)).out(form("crushedCentrifuged", "Au", 1), 10000).out(form("crushedCentrifugedTiny", "Cu", 2), 5000).out(form("crushedCentrifugedTiny", "Cu", 2), 5000).out(form("crushedCentrifugedTiny", "Cu", 2), 5000).out(form("crushedCentrifugedTiny", "Cu", 2), 5000).out(form("crushedCentrifugedTiny", "Cu", 2), 5000).out(liquid("ChloroauricAcid", thousandths(3000), CEIL)).out(gas("NO", thousandths(1000), CEIL)).out(water(waterUnits(3000), FLOOR)).add(rows);
        row(SOURCE, "Bath", 0, 256, 32).item(form("crushedPurifiedTiny", "Au", 9)).fluid(liquid("AquaRegia", thousandths(6500), CEIL)).out(form("crushedCentrifuged", "Au", 1), 10000).out(form("crushedCentrifugedTiny", "Cu", 2), 5000).out(form("crushedCentrifugedTiny", "Cu", 2), 5000).out(form("crushedCentrifugedTiny", "Cu", 2), 5000).out(form("crushedCentrifugedTiny", "Cu", 2), 5000).out(form("crushedCentrifugedTiny", "Cu", 2), 5000).out(liquid("ChloroauricAcid", thousandths(3000), CEIL)).out(gas("NO", thousandths(1000), CEIL)).out(water(waterUnits(3000), FLOOR)).add(rows);
        row(SOURCE, "Bath", 0, 256, 33).item(form("crushedPurified", "Cu", 1)).fluid(liquid("AquaRegia", thousandths(6500), CEIL)).out(form("crushedCentrifuged", "Cu", 1), 10000).out(form("crushedCentrifugedTiny", "Co", 2), 5000).out(form("crushedCentrifugedTiny", "Co", 2), 5000).out(form("crushedCentrifugedTiny", "Co", 2), 5000).out(form("crushedCentrifugedTiny", "Co", 2), 5000).out(form("crushedCentrifugedTiny", "Co", 2), 5000).out(liquid("ChloroauricAcid", thousandths(3000), CEIL)).out(gas("NO", thousandths(1000), CEIL)).out(water(waterUnits(3000), FLOOR)).add(rows);
        row(SOURCE, "Bath", 0, 256, 34).item(form("crushedPurifiedTiny", "Cu", 9)).fluid(liquid("AquaRegia", thousandths(6500), CEIL)).out(form("crushedCentrifuged", "Cu", 1), 10000).out(form("crushedCentrifugedTiny", "Co", 2), 5000).out(form("crushedCentrifugedTiny", "Co", 2), 5000).out(form("crushedCentrifugedTiny", "Co", 2), 5000).out(form("crushedCentrifugedTiny", "Co", 2), 5000).out(form("crushedCentrifugedTiny", "Co", 2), 5000).out(liquid("ChloroauricAcid", thousandths(3000), CEIL)).out(gas("NO", thousandths(1000), CEIL)).out(water(waterUnits(3000), FLOOR)).add(rows);
        row(SOURCE, "Bath", 0, 256, 35).item(form("crushedPurified", "Sn", 1)).fluid(liquid("AquaRegia", thousandths(13000), CEIL)).out(form("crushedCentrifuged", "Sn", 1), 10000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(liquid("StannicChloride", thousandths(2500), CEIL)).out(gas("HCl", thousandths(4000), CEIL)).out(gas("NO2", thousandths(3000), CEIL)).add(rows);
        row(SOURCE, "Bath", 0, 256, 36).item(form("crushedPurifiedTiny", "Sn", 9)).fluid(liquid("AquaRegia", thousandths(13000), CEIL)).out(form("crushedCentrifuged", "Sn", 1), 10000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(liquid("StannicChloride", thousandths(2500), CEIL)).out(gas("HCl", thousandths(4000), CEIL)).out(gas("NO2", thousandths(3000), CEIL)).add(rows);
        row(SOURCE, "Bath", 0, 256, 37).item(form("crushedPurified", "Cassiterite", 1)).fluid(liquid("AquaRegia", thousandths(13000), CEIL)).out(form("crushedCentrifuged", "Cassiterite", 1), 10000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(liquid("StannicChloride", thousandths(2500), CEIL)).out(gas("HCl", thousandths(4000), CEIL)).out(gas("NO2", thousandths(3000), CEIL)).add(rows);
        row(SOURCE, "Bath", 0, 256, 38).item(form("crushedPurifiedTiny", "Cassiterite", 9)).fluid(liquid("AquaRegia", thousandths(13000), CEIL)).out(form("crushedCentrifuged", "Cassiterite", 1), 10000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(form("crushedCentrifugedTiny", "Zn", 2), 5000).out(liquid("StannicChloride", thousandths(2500), CEIL)).out(gas("HCl", thousandths(4000), CEIL)).out(gas("NO2", thousandths(3000), CEIL)).add(rows);
        row(SOURCE, "Sifting", 16, 144, 39).item(tech("bop_grass_smoldering", 1)).out(vanilla("dirt", 1), 8000).out(dust("Ash", thousandths(250)), 2000).add(rows);
        row(SOURCE, "Sifting", 16, 144, 40).item(vanilla("dirt", 1)).out(dust("Sand", thousandths(1000)), 4500).out(dust("Stone", thousandths(1000)), 3000).out(dust("ClayBrown", thousandths(1000)), 250).out(tech("clay_ball_brown", 1), 100).add(rows);
        row(SOURCE, "Sifting", 16, 144, 41).item(tech("salt_dirt_1", 1)).out(dust("Sand", thousandths(1000)), 4500).out(dust("Stone", thousandths(1000)), 3000).out(dust("ClayBrown", thousandths(1000)), 250).out(tech("clay_ball_brown", 1), 100).out(form("dust", "NaCl", 1), 4500).add(rows);
        row(SOURCE, "Sifting", 16, 144, 42).item(tech("salt_dirt_2", 1)).out(dust("Sand", thousandths(1000)), 4500).out(dust("Stone", thousandths(1000)), 3000).out(dust("ClayBrown", thousandths(1000)), 250).out(tech("clay_ball_brown", 1), 100).out(form("dust", "NaCl", 1), 9000).add(rows);
        row(SOURCE, "Sifting", 16, 144, 43).item(tech("salt_dirt_3", 1)).out(dust("Sand", thousandths(1000)), 4500).out(dust("Stone", thousandths(1000)), 3000).out(dust("ClayBrown", thousandths(1000)), 250).out(tech("clay_ball_brown", 1), 100).out(form("dust", "NaCl", 2), 9000).add(rows);
        row(SOURCE, "Sifting", 16, 144, 44).item(tech("bop_coarse_loamy", 1)).out(dust("Sand", thousandths(1000)), 4500).out(dust("Stone", thousandths(1000)), 3000).out(dust("Palygorskite", thousandths(1000)), 1000).out(tech("clay_ball_blue", 1), 400).add(rows);
        row(SOURCE, "Sifting", 16, 144, 45).item(tech("bop_coarse_sandy", 1)).out(dust("Sand", thousandths(2000)), 9000).out(dust("Stone", thousandths(1000)), 3000).out(dust("Bentonite", thousandths(1000)), 250).out(tech("clay_ball_yellow", 1), 100).add(rows);
        row(SOURCE, "Sifting", 16, 144, 46).item(tech("bop_dirt_dried", 1)).out(dust("Sand", thousandths(1000)), 9000).out(dust("Stone", thousandths(1000)), 2500).out(dust("Bentonite", thousandths(1000)), 250).out(tech("clay_ball_yellow", 1), 100).add(rows);
        row(SOURCE, "Sifting", 16, 144, 47).item(tech("bop_dirt_hard", 1)).out(dust("Sand", thousandths(1000)), 9000).out(dust("Stone", thousandths(1000)), 2500).out(dust("Bentonite", thousandths(1000)), 250).out(tech("clay_ball_yellow", 1), 100).add(rows);
        row(SOURCE, "Sifting", 16, 144, 48).item(tech("bop_mud", 1)).out(vanilla("dirt", 1), 9000).out(tech("macu_bait_worm", 2), 2000).add(rows);
        row(SOURCE, "Sifting", 16, 144, 49).item(tech("ere_mud", 1)).out(vanilla("dirt", 1), 9000).out(tech("macu_bait_worm", 2), 2000).add(rows);
        row(SOURCE, "Sifting", 16, 144, 50).item(tech("btl_mud", 1)).out(vanilla("dirt", 1), 9000).out(tech("macu_bait_worm", 2), 2000).add(rows);
        row(SOURCE, "Sifting", 16, 144, 51).item(vanilla("dirt", 1)).out(vanilla("dirt", 1), 8000).out(dust("Ash", thousandths(250)), 1000).out(tech("macu_bait_ant", 2), 500).out(tech("macu_bait_maggot", 1), 500).out(tech("macu_bait_worm", 1), 500).add(rows);
        row(SOURCE, "Sifting", 16, 144, 52).item(vanilla("mycelium", 1)).out(vanilla("dirt", 1), 8000).out(vanilla("brown_mushroom", 1), 1000).out(vanilla("red_mushroom", 1), 1000).add(rows);
        row(SOURCE, "Sifting", 16, 144, 53).item(dust("SoulSand", thousandths(1000))).out(dust("Netherrack", thousandths(1000)), 1000).out(form("rockGt", "Netherrack", 1), 500).out(dust("ClayRed", thousandths(1000)), 50).out(tech("clay_ball_red", 1), 50).out(dust("Niter", thousandths(250)), 25).out(dust("NetherQuartz", thousandths(250)), 250).out(form("gem", "NetherQuartz", 1), 500).out(vanilla("bone", 1), 50).out(vanilla("nether_wart", 1), 25).add(rows);
        row(SOURCE, "Sifting", 16, 144, 54).item(vanilla("soul_sand", 1)).out(dust("Netherrack", thousandths(9000)), 1000).out(form("rockGt", "Netherrack", 9), 500).out(dust("ClayRed", thousandths(9000)), 50).out(tech("clay_ball_red", 9), 50).out(dust("Niter", thousandths(2250)), 25).out(dust("NetherQuartz", thousandths(2250)), 250).out(form("gem", "NetherQuartz", 9), 500).out(vanilla("bone", 9), 50).out(vanilla("nether_wart", 9), 25).add(rows);
        row(SOURCE, "Sifting", 16, 144, 55).item(tech("aether_sand", 1)).out(dust("Sand", thousandths(9000)), 9000).out(tech("macu_bait_ant", 1), 2000).add(rows);
        row(SOURCE, "Sifting", 16, 144, 56).item(tech("bop_quicksand", 1)).out(dust("Sand", thousandths(9000)), 9000).out(tech("macu_bait_ant", 1), 2000).add(rows);
        row(SOURCE, "Sifting", 16, 144, 57).item(tech("bop_sand_hard", 1)).out(dust("Sand", thousandths(9000)), 10000).add(rows);
        row(SOURCE, "Sifting", 16, 144, 58).item(tech("tropic_sand_black", 1)).out(form("dust", "Basalt", 3), 7500).out(form("dust", "RedSand", 3), 5000).out(form("nugget", "Au", 6), 2500).out(form("nugget", "Cu", 12), 2500).out(form("nugget", "Ni", 3), 2500).add(rows);
        row(SOURCE, "MagneticSeparator", 16, 144, 59).item(tech("tropic_sand_black", 1)).out(form("dust", "Basalt", 3), 7500).out(form("dust", "RedSand", 6), 5000).out(form("dustTiny", "Au", 12), 2500).out(form("dustTiny", "Cu", 6), 2500).out(form("dustTiny", "Ni", 6), 2500).add(rows);
        row(SOURCE, "Centrifuge", 16, 288, 60).item(tech("tropic_sand_black", 1)).out(form("dust", "Basalt", 6), 7500).out(form("dust", "RedSand", 3), 5000).out(form("dustTiny", "V2O5", 12), 2500).out(form("dustTiny", "Cu", 6), 2500).out(form("dustTiny", "Ni", 3), 2500).add(rows);
        row(SOURCE, "Mixer", 16, 256, 61).item(form("crushedPurified", "U_238", 1)).item(form("dust", "Al", 1)).out(form("crushedCentrifugedTiny", "U_238", 12)).add(rows);
        row(SOURCE, "Mixer", 16, 256, 62).item(form("crushedPurifiedTiny", "U_238", 9)).item(form("dust", "Al", 1)).out(form("crushedCentrifugedTiny", "U_238", 12)).add(rows);
        row(SOURCE, "Mixer", 16, 256, 63).item(form("crushedPurified", "U_238", 1)).item(form("dust", "Mg", 1)).out(form("crushedCentrifugedTiny", "U_238", 12)).add(rows);
        row(SOURCE, "Mixer", 16, 256, 64).item(form("crushedPurifiedTiny", "U_238", 9)).item(form("dust", "Mg", 1)).out(form("crushedCentrifugedTiny", "U_238", 12)).add(rows);
        row(SOURCE, "Mixer", 16, 256, 65).item(form("crushedPurified", "U_235", 1)).item(form("dust", "Al", 1)).out(form("crushedCentrifugedTiny", "U_235", 12)).add(rows);
        row(SOURCE, "Mixer", 16, 256, 66).item(form("crushedPurifiedTiny", "U_235", 9)).item(form("dust", "Al", 1)).out(form("crushedCentrifugedTiny", "U_235", 12)).add(rows);
        row(SOURCE, "Mixer", 16, 256, 67).item(form("crushedPurified", "U_235", 1)).item(form("dust", "Mg", 1)).out(form("crushedCentrifugedTiny", "U_235", 12)).add(rows);
        row(SOURCE, "Mixer", 16, 256, 68).item(form("crushedPurifiedTiny", "U_235", 9)).item(form("dust", "Mg", 1)).out(form("crushedCentrifugedTiny", "U_235", 12)).add(rows);
        row(SOURCE, "Centrifuge", 256, 256, 69).item(form("crushedCentrifugedTiny", "Be", 1)).out(form("dustTiny", "Be", 1), 10000).out(form("dustTiny", "Be_7", 8), 150).out(form("dustTiny", "Be_8", 8), 150).add(rows);
        row(SOURCE, "Centrifuge", 256, 2304, 70).item(form("crushedCentrifuged", "Be", 1)).out(form("dust", "Be", 1), 10000).out(form("dustTiny", "Be_7", 8), 1350).out(form("dustTiny", "Be_8", 8), 1350).add(rows);
        row(SOURCE, "Centrifuge", 256, 256, 71).item(form("crushedCentrifugedTiny", "Li", 1)).out(form("dustTiny", "Li", 1), 10000).out(form("dustTiny", "Li_6", 8), 300).add(rows);
        row(SOURCE, "Centrifuge", 256, 2304, 72).item(form("crushedCentrifuged", "Li", 1)).out(form("dust", "Li", 1), 10000).out(form("dustTiny", "Li_6", 8), 2700).add(rows);
        row(SOURCE, "Centrifuge", 256, 256, 73).item(form("crushedCentrifugedTiny", "B", 1)).out(form("dustTiny", "B", 1), 10000).out(form("dustTiny", "B_11", 8), 300).add(rows);
        row(SOURCE, "Centrifuge", 256, 2304, 74).item(form("crushedCentrifuged", "B", 1)).out(form("dust", "B", 1), 10000).out(form("dustTiny", "B_11", 8), 2700).add(rows);
        row(SOURCE, "Centrifuge", 256, 256, 75).item(form("crushedCentrifugedTiny", "C", 1)).out(form("dustTiny", "C", 1), 10000).out(form("dustTiny", "C_13", 8), 150).out(form("dustTiny", "C_14", 8), 150).add(rows);
        row(SOURCE, "Centrifuge", 256, 2304, 76).item(form("crushedCentrifuged", "C", 1)).out(form("dust", "C", 1), 10000).out(form("dustTiny", "C_13", 8), 1350).out(form("dustTiny", "C_14", 8), 1350).add(rows);
        row(SOURCE, "Centrifuge", 512, 256, 77).item(form("crushedCentrifugedTiny", "Co", 1)).out(form("dustTiny", "Co", 1), 10000).out(form("dustTiny", "Co_60", 8), 300).add(rows);
        row(SOURCE, "Centrifuge", 512, 2304, 78).item(form("crushedCentrifuged", "Co", 1)).out(form("dust", "Co", 1), 10000).out(form("dustTiny", "Co_60", 8), 2700).add(rows);
        row(SOURCE, "Centrifuge", 1024, 256, 81).item(form("crushedCentrifugedTiny", "Pu", 1)).out(form("dustTiny", "Pu", 1), 10000).out(form("dustTiny", "Pu_241", 8), 150).out(form("dustTiny", "Pu_243", 8), 150).add(rows);
        row(SOURCE, "Centrifuge", 1024, 2304, 82).item(form("crushedCentrifuged", "Pu", 1)).out(form("dust", "Pu", 1), 10000).out(form("dustTiny", "Pu_241", 8), 1350).out(form("dustTiny", "Pu_243", 8), 1350).add(rows);
        row(SOURCE, "Centrifuge", 1024, 256, 83).item(form("crushedCentrifugedTiny", "Am", 1)).out(form("dustTiny", "Am", 1), 10000).out(form("dustTiny", "Am_241", 8), 300).add(rows);
        row(SOURCE, "Centrifuge", 1024, 2304, 84).item(form("crushedCentrifuged", "Am", 1)).out(form("dust", "Am", 1), 10000).out(form("dustTiny", "Am_241", 8), 2700).add(rows);
        row(SOURCE, "Centrifuge", 1536, 256, 85).item(form("crushedCentrifugedTiny", "Fl", 1)).out(form("dustTiny", "Fl", 1), 10000).out(form("dustTiny", "Fl_298", 8), 300).add(rows);
        row(SOURCE, "Centrifuge", 1536, 2304, 86).item(form("crushedCentrifuged", "Fl", 1)).out(form("dust", "Fl", 1), 10000).out(form("dustTiny", "Fl_298", 8), 2700).add(rows);
        row(SOURCE, "Centrifuge", true, 16, 288, 87).item(tech("sand_magnetite", 1)).out(form("dust", "Deepslate", 6), 7500).out(form("dust", "RedSand", 3), 5000).out(form("dustTiny", "V2O5", 12), 2500).out(form("dustTiny", "Sn", 6), 2500).out(form("dustTiny", "Zn", 3), 2500).add(rows);
        row(SOURCE, "Centrifuge", true, 16, 288, 88).item(tech("sand_basalt_magnetite", 1)).out(form("dust", "Basalt", 6), 7500).out(form("dust", "RedSand", 3), 5000).out(form("dustTiny", "V2O5", 12), 2500).out(form("dustTiny", "Cu", 6), 2500).out(form("dustTiny", "Ni", 3), 2500).add(rows);
        row(SOURCE, "Centrifuge", true, 16, 288, 89).item(tech("sand_granite_magnetite", 1)).out(form("dust", "GraniteBlack", 6), 7500).out(form("dust", "RedSand", 3), 5000).out(form("dustTiny", "V2O5", 12), 2500).out(form("dustTiny", "Pb", 6), 2500).out(form("dustTiny", "Co", 3), 2500).add(rows);
        row(SOURCE, "Centrifuge", true, 64, 144, 90).item(dust("DarkAsh", units(1297296000))).out(dust("Ash", units(648648000))).out(optional("darkash_second")).add(rows);
    }

    private static void easyUraniumRows(List<ChemRecipe> rows) {
        row(SOURCE, "Centrifuge", true, 1024, 256, 79).item(form("crushedCentrifugedTiny", "U_238", 1)).out(form("dustTiny", "U_238", 1), 10000).out(form("dustTiny", "U_235", 8), 200).out(form("dustTiny", "U_233", 8), 100).add(rows);
        row(SOURCE, "Centrifuge", true, 1024, 2304, 80).item(form("crushedCentrifuged", "U_238", 1)).out(form("dust", "U_238", 1), 10000).out(form("dustTiny", "U_235", 8), 1800).out(form("dustTiny", "U_233", 8), 900).add(rows);
    }
}
