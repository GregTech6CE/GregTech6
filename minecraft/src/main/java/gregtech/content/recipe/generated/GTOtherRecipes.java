package gregtech.content.recipe.generated;

import static gregtech.content.recipe.generated.GeneratedRows.*;
import static gregtech.engine.recipe.chem.FluidPhase.LIQUID;
import static gregtech.engine.recipe.chem.Rounding.CEIL;
import static gregtech.engine.recipe.chem.Rounding.FLOOR;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialGroups;
import gregtech.engine.recipe.chem.ChemRecipe;
import java.util.ArrayList;
import java.util.List;

/**
 * The portable {@code Loader_Recipes_Other} machine rows in exact source terms.
 *
 * <p>The source's wax and sapphire groups, and its nested compound-prefix loops, are expanded
 * so a group never collapses to one representative. Rows that name other mods' content are not
 * approximated; they and the port's retired materials are recorded as exclusions.</p>
 */
public final class GTOtherRecipes {
    private static final String SOURCE = "gt6@3703e403/Loader_Recipes_Other:row/";

    private GTOtherRecipes() {}

    public static void register() {
        List<ChemRecipe> rows = new ArrayList<>();
        waxRows(rows);
        compoundBaseRows(rows);
        sapphireRows(rows);
        sourceRows(rows);
        ChemRecipeResolver.exclude(SOURCE + "other-mod",
                "rows naming other mods' items or machines are absent; they are not approximated");
        ChemRecipeResolver.exclude(SOURCE + "retired",
                "rows whose materials or fluids this port retired were removed with those definitions");
        publish("other recipes", rows);
    }

    /** {@code OM.dust(tWax, U)} and {@code U4} for every member of the wax group. */
    private static void waxRows(List<ChemRecipe> rows) {
        for (GTMaterial wax : group(MaterialGroups.Wax)) {
            row(SOURCE + "50/" + wax.getName(), "Compressor", 16, 16)
                    .item(dust(wax.getName(), U(1))).out(form("plate", wax.getName(), 1)).add(rows);
            row(SOURCE + "51/" + wax.getName(), "Compressor", 16, 16)
                    .item(dust(wax.getName(), U(1, 4))).out(form("foil", wax.getName(), 1)).add(rows);
        }
    }

    /** The plain dust and small-dust compound rows; GTCompoundMixerRecipes owns the prefix matrix. */
    private static void compoundBaseRows(List<ChemRecipe> rows) {
        row(SOURCE, "Mixer", 16, 64, 3).item(form("dust", "Cu", 3)).item(form("dust", "Sn", 1)).out(tech("bronze_compound", 36)).add(rows);
        row(SOURCE, "Mixer", 16, 64, 4).item(form("dust", "Cu", 3)).item(form("dust", "Zn", 1)).out(tech("brass_compound", 36)).add(rows);
        row(SOURCE, "Mixer", 16, 64, 5).item(form("dust", "Cu", 3)).item(form("dust", "As", 1)).out(tech("arsenic_copper_compound", 36)).add(rows);
        row(SOURCE, "Mixer", 16, 80, 6).item(form("dust", "ArsenicCopper", 4)).item(form("dust", "Sn", 1)).out(tech("arsenic_bronze_compound", 45)).add(rows);
        row(SOURCE, "Mixer", 16, 80, 7).item(tech("arsenic_copper_compound", 36)).item(form("dust", "Sn", 1)).out(tech("arsenic_bronze_compound", 45)).add(rows);
        row(SOURCE, "Mixer", 16, 80, 8).item(form("dust", "Bronze", 4)).item(form("dust", "As", 1)).out(tech("arsenic_bronze_compound", 45)).add(rows);
        row(SOURCE, "Mixer", 16, 80, 9).item(tech("bronze_compound", 36)).item(form("dust", "As", 1)).out(tech("arsenic_bronze_compound", 45)).add(rows);
        row(SOURCE, "Mixer", 16, 80, 10).item(form("dust", "Brass", 4)).item(form("dust", "Bi", 1)).out(tech("bismuth_bronze_compound", 45)).add(rows);
        row(SOURCE, "Mixer", 16, 80, 11).item(tech("brass_compound", 36)).item(form("dust", "Bi", 1)).out(tech("bismuth_bronze_compound", 45)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 12).item(form("dustSmall", "Cu", 3)).item(form("dustSmall", "Sn", 1)).out(tech("bronze_compound", 9)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 13).item(form("dustSmall", "Cu", 3)).item(form("dustSmall", "Zn", 1)).out(tech("brass_compound", 9)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 14).item(form("dustSmall", "Cu", 3)).item(form("dustSmall", "As", 1)).out(tech("arsenic_copper_compound", 9)).add(rows);
        row(SOURCE, "Mixer", 16, 80, 15).item(form("dustSmall", "ArsenicCopper", 16)).item(form("dustSmall", "Sn", 4)).out(tech("arsenic_bronze_compound", 45)).add(rows);
        row(SOURCE, "Mixer", 16, 80, 16).item(tech("arsenic_copper_compound", 36)).item(form("dustSmall", "Sn", 4)).out(tech("arsenic_bronze_compound", 45)).add(rows);
        row(SOURCE, "Mixer", 16, 80, 17).item(form("dustSmall", "Bronze", 16)).item(form("dustSmall", "As", 4)).out(tech("arsenic_bronze_compound", 45)).add(rows);
        row(SOURCE, "Mixer", 16, 80, 18).item(tech("bronze_compound", 36)).item(form("dustSmall", "As", 4)).out(tech("arsenic_bronze_compound", 45)).add(rows);
        row(SOURCE, "Mixer", 16, 80, 19).item(form("dustSmall", "Brass", 16)).item(form("dustSmall", "Bi", 4)).out(tech("bismuth_bronze_compound", 45)).add(rows);
        row(SOURCE, "Mixer", 16, 80, 20).item(tech("brass_compound", 36)).item(form("dustSmall", "Bi", 4)).out(tech("bismuth_bronze_compound", 45)).add(rows);    }

    /** {@code ANY.Sapphire.mToThis}: both the exact-unit and the ninth-unit Energium recipes. */
    private static void sapphireRows(List<ChemRecipe> rows) {
        for (GTMaterial sapphire : group(MaterialGroups.Sapphire)) {
            String name = sapphire.getName();
            row(SOURCE + "117/" + name, "Mixer", 16, 144)
                    .item(dust(name, U(4))).item(dust("Redstone", U(5))).out(dust("EnergiumRed", U(9))).add(rows);
            row(SOURCE + "118/" + name, "Mixer", 16, 144)
                    .item(dust(name, U(4, 9))).item(dust("Redstone", U(5, 9)))
                    .out(dust("EnergiumRed", U(1))).add(rows);
            row(SOURCE + "119/" + name, "Mixer", 16, 144)
                    .item(dust(name, U(4))).item(dust("Nikolite", U(5))).out(dust("EnergiumCyan", U(9))).add(rows);
            row(SOURCE + "120/" + name, "Mixer", 16, 144)
                    .item(dust(name, U(4, 9))).item(dust("Nikolite", U(5, 9)))
                    .out(dust("EnergiumCyan", U(1))).add(rows);
        }
    }

    private static void sourceRows(List<ChemRecipe> rows) {
        row(SOURCE, "Distillery", 16, 16, 25).kept(selector(0)).fluid(nativeFluid("Resin", 10)).out(nativeFluid("Turpentine", 6)).out(nativeFluid("DistW", 3)).add(rows);
        row(SOURCE, "Distillery", 16, 16, 26).kept(selector(0)).fluid(nativeFluid("Resin_Spruce", 10)).out(nativeFluid("Turpentine", 6)).out(nativeFluid("DistW", 3)).add(rows);
        row(SOURCE, "Bath", 0, 16, 27).item(dust("Coal", thousandths(1000))).fluid(water(waterUnits(125), FLOOR)).out(dust("HydratedCoal", thousandths(1000))).add(rows);
        row(SOURCE, "Mixer", 16, 64, 28).item(form("gem", "ChargedCertusQuartz", 1)).item(form("gem", "NetherQuartz", 1)).item(form("dust", "Redstone", 1)).fluid(water(waterUnits(500), FLOOR)).out(form("gem", "Fluix", 2)).add(rows);
        row(SOURCE, "Mixer", 16, 64, 29).item(form("dust", "ChargedCertusQuartz", 1)).item(form("dust", "NetherQuartz", 1)).item(form("dust", "Redstone", 1)).fluid(water(waterUnits(500), FLOOR)).out(form("dust", "Fluix", 2)).add(rows);
        row(SOURCE, "Mixer", 16, 64, 30).item(form("gem", "ChargedCertusQuartz", 1)).item(form("gem", "MilkyQuartz", 1)).item(form("dust", "Redstone", 1)).fluid(water(waterUnits(500), FLOOR)).out(form("gem", "Fluix", 2)).add(rows);
        row(SOURCE, "Mixer", 16, 64, 31).item(form("dust", "ChargedCertusQuartz", 1)).item(form("dust", "MilkyQuartz", 1)).item(form("dust", "Redstone", 1)).fluid(water(waterUnits(500), FLOOR)).out(form("dust", "Fluix", 2)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 32).item(dust("ConstructionFoam", thousandths(1000))).fluid(water(waterUnits(100), FLOOR)).out(nativeFluid("CFoam", 100)).add(rows);
        row(SOURCE, "Mixer", 16, 144, 33).item(form("blockDust", "Concrete", 1)).fluid(water(waterUnits(1125), FLOOR)).out(nativeFluid("Concrete", 1296)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 34).item(form("dust", "Concrete", 1)).fluid(water(waterUnits(125), FLOOR)).out(nativeFluid("Concrete", 144)).add(rows);
        row(SOURCE, "Mixer", 16, 1296, 35).item(form("blockDust", "Stone", 9)).item(form("blockDust", "Calcite", 1)).item(form("blockDust", "Ash", 1)).out(form("blockDust", "Concrete", 11)).add(rows);
        row(SOURCE, "Slicer", 16, 16, 36).item(vanilla("paper", 1)).kept(tech("slicer_shape_grid", 1)).out(form("plateTiny", "Paper", 9)).add(rows);
        row(SOURCE, "Bath", 0, 16, 37).item(vanilla("paper", 2)).kept(selector(2)).fluid(nativeFluid("Glue", 125)).out(form("plateDouble", "Paper", 1)).add(rows);
        row(SOURCE, "Bath", 0, 32, 38).item(vanilla("paper", 3)).kept(selector(3)).fluid(nativeFluid("Glue", 250)).out(form("plateTriple", "Paper", 1)).add(rows);
        row(SOURCE, "Bath", 0, 48, 39).item(vanilla("paper", 4)).kept(selector(4)).fluid(nativeFluid("Glue", 375)).out(form("plateQuadruple", "Paper", 1)).add(rows);
        row(SOURCE, "Bath", 0, 64, 40).item(vanilla("paper", 5)).kept(selector(5)).fluid(nativeFluid("Glue", 500)).out(form("plateQuintuple", "Paper", 1)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 41).item(dust("Redstone", thousandths(1000))).item(dust("Basalz", U(1, 9))).item(dust("Clay", thousandths(1000))).item(dust("Obsidian", thousandths(1000))).out(dust("Petrotheum", thousandths(2000))).add(rows);
        row(SOURCE, "Mixer", 16, 64, 42).item(tech("clay_ball_white", 2)).item(dust("SiO2", thousandths(1000))).item(dust("PotassiumFeldspar", thousandths(1000))).out(dust("Porcelain", thousandths(4000))).add(rows);
        row(SOURCE, "Mixer", 16, 64, 43).item(dust("Kaolinite", thousandths(2000))).item(dust("SiO2", thousandths(1000))).item(dust("PotassiumFeldspar", thousandths(1000))).out(dust("Porcelain", thousandths(4000))).add(rows);
        row(SOURCE, "Mixer", 16, 16, 44).item(dust("Kaolinite", thousandths(500))).item(dust("SiO2", thousandths(250))).item(dust("PotassiumFeldspar", thousandths(250))).out(dust("Porcelain", thousandths(1000))).add(rows);
        row(SOURCE, "Mixer", 16, 64, 45).item(dust("C", thousandths(2000))).item(dust("SoulSand", thousandths(1000))).item(dust("Blaze", U(1, 9))).out(dust("Gunpowder", thousandths(3000))).add(rows);
        row(SOURCE, "Mixer", 16, 64, 46).item(dust("Charcoal", thousandths(2000))).item(dust("SoulSand", thousandths(1000))).item(dust("Blaze", U(1, 9))).out(dust("Gunpowder", thousandths(3000))).add(rows);
        row(SOURCE, "Mixer", 16, 64, 47).item(dust("Coal", thousandths(1000))).item(dust("SoulSand", thousandths(1000))).item(dust("Blaze", U(1, 9))).out(dust("Gunpowder", thousandths(3000))).add(rows);
        row(SOURCE, "Mixer", 16, 64, 48).item(dust("CoalCoke", thousandths(1000))).item(dust("SoulSand", thousandths(1000))).item(dust("Blaze", U(1, 9))).out(dust("Gunpowder", thousandths(3000))).add(rows);
        row(SOURCE, "Mixer", 16, 64, 49).item(dust("C", thousandths(2000))).item(dust("SoulSand", thousandths(1000))).item(dust("S", thousandths(1000))).out(dust("Gunpowder", thousandths(3000))).add(rows);
        row(SOURCE, "Mixer", 16, 64, 50).item(dust("Charcoal", thousandths(2000))).item(dust("SoulSand", thousandths(1000))).item(dust("S", thousandths(1000))).out(dust("Gunpowder", thousandths(3000))).add(rows);
        row(SOURCE, "Mixer", 16, 64, 51).item(dust("Coal", thousandths(1000))).item(dust("SoulSand", thousandths(1000))).item(dust("S", thousandths(1000))).out(dust("Gunpowder", thousandths(3000))).add(rows);
        row(SOURCE, "Mixer", 16, 64, 52).item(dust("CoalCoke", thousandths(1000))).item(dust("SoulSand", thousandths(1000))).item(dust("S", thousandths(1000))).out(dust("Gunpowder", thousandths(3000))).add(rows);
        row(SOURCE, "Mixer", 16, 16, 53).item(dust("C", thousandths(500))).item(dust("SoulSand", thousandths(250))).item(dust("S", thousandths(250))).out(dust("Gunpowder", thousandths(750))).add(rows);
        row(SOURCE, "Mixer", 16, 16, 54).item(dust("Charcoal", thousandths(500))).item(dust("SoulSand", thousandths(250))).item(dust("S", thousandths(250))).out(dust("Gunpowder", thousandths(750))).add(rows);
        row(SOURCE, "Mixer", 16, 16, 55).item(dust("Coal", thousandths(250))).item(dust("SoulSand", thousandths(250))).item(dust("S", thousandths(250))).out(dust("Gunpowder", thousandths(750))).add(rows);
        row(SOURCE, "Mixer", 16, 16, 56).item(dust("CoalCoke", thousandths(250))).item(dust("SoulSand", thousandths(250))).item(dust("S", thousandths(250))).out(dust("Gunpowder", thousandths(750))).add(rows);
        row(SOURCE, "Mixer", 16, 8, 57).item(dust("C", U(2, 9))).item(dust("SoulSand", U(1, 9))).item(dust("S", U(1, 9))).out(dust("Gunpowder", U(1, 3))).add(rows);
        row(SOURCE, "Mixer", 16, 8, 58).item(dust("Charcoal", U(2, 9))).item(dust("SoulSand", U(1, 9))).item(dust("S", U(1, 9))).out(dust("Gunpowder", U(1, 3))).add(rows);
        row(SOURCE, "Mixer", 16, 8, 59).item(dust("Coal", U(1, 9))).item(dust("SoulSand", U(1, 9))).item(dust("S", U(1, 9))).out(dust("Gunpowder", U(1, 3))).add(rows);
        row(SOURCE, "Mixer", 16, 8, 60).item(dust("CoalCoke", U(1, 9))).item(dust("SoulSand", U(1, 9))).item(dust("S", U(1, 9))).out(dust("Gunpowder", U(1, 3))).add(rows);
        row(SOURCE, "Mixer", 16, 16, 61).item(dust("Redstone", thousandths(1000))).item(dust("Coal", thousandths(1000))).item(dust("S", thousandths(1000))).fluid(nativeFluid("Blaze", 144)).out(dust("Pyrotheum", thousandths(2000))).add(rows);
        row(SOURCE, "Mixer", 16, 16, 62).item(dust("Redstone", thousandths(1000))).item(dust("Blaze", U(1, 9))).item(dust("Coal", thousandths(1000))).item(dust("S", thousandths(1000))).out(dust("Pyrotheum", thousandths(2000))).add(rows);
        row(SOURCE, "Mixer", 16, 32, 63).item(dust("Fe", thousandths(1000))).item(dust("LiveRoot", thousandths(1000))).item(dust("Au", U(1, 9))).out(dust("IronWood", thousandths(2000))).add(rows);
        row(SOURCE, "Mixer", 16, 32, 64).item(dust("Steel", thousandths(1000))).item(dust("LiveRoot", thousandths(1000))).item(dust("Au", U(1, 9))).out(dust("IronWood", thousandths(2000))).add(rows);
        row(SOURCE, "Mixer", 16, 32, 65).item(dust("Knightmetal", thousandths(1000))).item(dust("LiveRoot", thousandths(1000))).item(dust("Au", U(1, 9))).out(dust("IronWood", thousandths(2000))).add(rows);
        row(SOURCE, "Mixer", 16, 32, 66).item(dust("BrownLimonite", thousandths(4000))).item(dust("LiveRoot", thousandths(1000))).item(dust("Au", U(1, 9))).out(dust("IronWood", thousandths(2000))).add(rows);
        row(SOURCE, "Mixer", 16, 32, 67).item(dust("YellowLimonite", thousandths(4000))).item(dust("LiveRoot", thousandths(1000))).item(dust("Au", U(1, 9))).out(dust("IronWood", thousandths(2000))).add(rows);
        row(SOURCE, "Mixer", 16, 64, 68).item(dust("Fe2O3", thousandths(5000))).item(dust("LiveRoot", thousandths(2000))).item(dust("Au", U(2, 9))).out(dust("IronWood", thousandths(4000))).add(rows);
        row(SOURCE, "Mixer", 16, 96, 69).item(dust("Magnetite", thousandths(7000))).item(dust("LiveRoot", thousandths(3000))).item(dust("Au", U(1, 3))).out(dust("IronWood", thousandths(6000))).add(rows);
        row(SOURCE, "Mixer", 16, 96, 70).item(dust("BasalticMineralSand", thousandths(7000))).item(dust("LiveRoot", thousandths(3000))).item(dust("Au", U(1, 3))).out(dust("IronWood", thousandths(6000))).add(rows);
        row(SOURCE, "Mixer", 16, 96, 71).item(dust("GraniticMineralSand", thousandths(7000))).item(dust("LiveRoot", thousandths(3000))).item(dust("Au", U(1, 3))).out(dust("IronWood", thousandths(6000))).add(rows);
        row(SOURCE, "Lightning", 16, 2048, 72).kept(selector(1)).item(form("gem", "CertusQuartz", 1)).out(form("gem", "ChargedCertusQuartz", 1)).add(rows);
        row(SOURCE, "Press", 16, 16, 73).item(form("dust", "SiO2", 2)).item(form("dust", "Gunpowder", 2)).item(vanilla("string", 1)).out(tech("boomstick", 1)).add(rows);
        row(SOURCE, "Press", 16, 144, 74).item(form("blockDust", "SiO2", 2)).item(form("blockDust", "Gunpowder", 2)).item(vanilla("string", 9)).out(tech("boomstick", 9)).add(rows);
        row(SOURCE, "Press", 16, 16, 75).kept(selector(1)).item(form("dust", "Dynamite", 1)).item(vanilla("string", 1)).out(tech("dynamite", 1)).add(rows);
        row(SOURCE, "Press", 16, 144, 76).kept(selector(1)).item(form("blockDust", "Dynamite", 1)).item(vanilla("string", 9)).out(tech("dynamite", 9)).add(rows);
        row(SOURCE, "Press", 16, 64, 77).kept(selector(2)).item(form("dust", "Dynamite", 2)).item(vanilla("string", 1)).out(tech("dynamite_strong", 1)).add(rows);
        row(SOURCE, "Press", 16, 576, 78).kept(selector(2)).item(form("blockDust", "Dynamite", 2)).item(vanilla("string", 9)).out(tech("dynamite_strong", 9)).add(rows);
        row(SOURCE, "Press", 16, 16, 79).kept(tech("shape_press_bulletcasingsmall", 1)).item(form("dustDiv72", "Gunpowder", 8)).item(form("plateTiny", "Brass", 1)).out(form("bulletGtSmall", "Empty", 1)).add(rows);
        row(SOURCE, "Press", 16, 32, 80).kept(tech("shape_press_bulletcasingmedium", 1)).item(form("dustDiv72", "Gunpowder", 16)).item(form("plateTiny", "Brass", 2)).out(form("bulletGtMedium", "Empty", 1)).add(rows);
        row(SOURCE, "Press", 16, 64, 81).kept(tech("shape_press_bulletcasinglarge", 1)).item(form("dustDiv72", "Gunpowder", 24)).item(form("plateTiny", "Brass", 3)).out(form("bulletGtLarge", "Empty", 1)).add(rows);
        row(SOURCE, "Press", 16, 16, 82).kept(tech("shape_press_bulletcasingsmall", 1)).item(form("dustTiny", "Gunpowder", 1)).item(form("plateTiny", "Brass", 1)).out(form("bulletGtSmall", "Empty", 1)).add(rows);
        row(SOURCE, "Press", 16, 32, 83).kept(tech("shape_press_bulletcasingmedium", 1)).item(form("dustTiny", "Gunpowder", 2)).item(form("plateTiny", "Brass", 2)).out(form("bulletGtMedium", "Empty", 1)).add(rows);
        row(SOURCE, "Press", 16, 64, 84).kept(tech("shape_press_bulletcasinglarge", 1)).item(form("dustTiny", "Gunpowder", 3)).item(form("plateTiny", "Brass", 3)).out(form("bulletGtLarge", "Empty", 1)).add(rows);
        row(SOURCE, "Press", 16, 144, 85).kept(tech("shape_press_bulletcasingsmall", 1)).item(form("dustSmall", "Gunpowder", 4)).item(form("plateTiny", "Brass", 9)).out(form("bulletGtSmall", "Empty", 9)).add(rows);
        row(SOURCE, "Press", 16, 288, 86).kept(tech("shape_press_bulletcasingmedium", 1)).item(form("dustSmall", "Gunpowder", 8)).item(form("plateTiny", "Brass", 18)).out(form("bulletGtMedium", "Empty", 9)).add(rows);
        row(SOURCE, "Press", 16, 192, 87).kept(tech("shape_press_bulletcasinglarge", 1)).item(form("dustSmall", "Gunpowder", 4)).item(form("plateTiny", "Brass", 9)).out(form("bulletGtLarge", "Empty", 3)).add(rows);
        row(SOURCE, "Press", 16, 144, 88).kept(tech("shape_press_bulletcasingsmall", 1)).item(form("dust", "Gunpowder", 1)).item(form("plateTiny", "Brass", 9)).out(form("bulletGtSmall", "Empty", 9)).add(rows);
        row(SOURCE, "Press", 16, 288, 89).kept(tech("shape_press_bulletcasingmedium", 1)).item(form("dust", "Gunpowder", 2)).item(form("plateTiny", "Brass", 18)).out(form("bulletGtMedium", "Empty", 9)).add(rows);
        row(SOURCE, "Press", 16, 192, 90).kept(tech("shape_press_bulletcasinglarge", 1)).item(form("dust", "Gunpowder", 1)).item(form("plateTiny", "Brass", 9)).out(form("bulletGtLarge", "Empty", 3)).add(rows);
        row(SOURCE, "Centrifuge", 64, 16, 91).fluid(liquid("FishOil", units(81081000), CEIL)).out(liquid("Hg", units(1), CEIL)).add(rows);
        row(SOURCE, "Coagulator", 0, 256, 92).fluid(nativeFluid("Latex", 16)).out(form("nugget", "Rubber", 1)).add(rows);
        row(SOURCE, "Loom", 16, 16, 93).kept(selector(10)).item(form("plantGtFiber", "Cu", 4)).out(tech("rope", 1)).add(rows);
        row(SOURCE, "Loom", 16, 16, 94).kept(selector(10)).item(vanilla("string", 4)).out(tech("rope_silk", 1)).add(rows);
        row(SOURCE, "Loom", 16, 16, 95).kept(selector(10)).item(form("wireFine", "Plastic", 4)).out(tech("rope_plastic", 1)).add(rows);
        row(SOURCE, "Loom", 16, 16, 96).kept(selector(10)).item(vanilla("vine", 4)).out(tech("rope_vine", 1)).add(rows);
        row(SOURCE, "Loom", 16, 64, 97).kept(selector(10)).item(form("wireFine", "Steel", 4)).out(tech("rope_steel", 1)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 98).item(form("gem", "Coal", 1)).out(form("gem", "CoalCoke", 1)).out(liquid("Creosote", thousandths(500), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 99).item(form("nugget", "Coal", 9)).out(form("ingot", "CoalCoke", 1)).out(liquid("Creosote", thousandths(500), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 100).item(form("chunkGt", "Coal", 4)).out(form("ingot", "CoalCoke", 1)).out(liquid("Creosote", thousandths(500), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 7200, 101).item(form("billet", "Coal", 3)).out(form("ingot", "CoalCoke", 2)).out(liquid("Creosote", thousandths(1000), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 102).item(form("ingot", "Coal", 1)).out(form("ingot", "CoalCoke", 1)).out(liquid("Creosote", thousandths(500), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 7200, 103).item(form("oreRaw", "Coal", 1)).out(form("ingot", "CoalCoke", 2)).out(liquid("Creosote", thousandths(1000), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 104).item(form("crushed", "Coal", 1)).out(form("ingot", "CoalCoke", 1)).out(liquid("Creosote", thousandths(500), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 105).item(form("crushedTiny", "Coal", 9)).out(form("ingot", "CoalCoke", 1)).out(liquid("Creosote", thousandths(500), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 106).item(form("crushedPurified", "Coal", 1)).out(form("chunkGt", "CoalCoke", 1)).out(form("chunkGt", "CoalCoke", 1)).out(form("chunkGt", "CoalCoke", 1)).out(form("chunkGt", "CoalCoke", 1)).out(form("chunkGt", "CoalCoke", 1)).out(liquid("Creosote", thousandths(500), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 107).item(form("crushedPurifiedTiny", "Coal", 9)).out(form("chunkGt", "CoalCoke", 1)).out(form("chunkGt", "CoalCoke", 1)).out(form("chunkGt", "CoalCoke", 1)).out(form("chunkGt", "CoalCoke", 1)).out(form("chunkGt", "CoalCoke", 1)).out(liquid("Creosote", thousandths(500), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 108).item(form("crushedCentrifuged", "Coal", 1)).out(form("chunkGt", "CoalCoke", 1)).out(form("chunkGt", "CoalCoke", 1)).out(form("chunkGt", "CoalCoke", 1)).out(form("chunkGt", "CoalCoke", 1)).out(form("chunkGt", "CoalCoke", 1)).out(form("chunkGt", "CoalCoke", 1)).out(liquid("Creosote", thousandths(500), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 109).item(form("crushedCentrifugedTiny", "Coal", 9)).out(form("chunkGt", "CoalCoke", 1)).out(form("chunkGt", "CoalCoke", 1)).out(form("chunkGt", "CoalCoke", 1)).out(form("chunkGt", "CoalCoke", 1)).out(form("chunkGt", "CoalCoke", 1)).out(form("chunkGt", "CoalCoke", 1)).out(liquid("Creosote", thousandths(500), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 32400, 110).item(form("blockIngot", "Coal", 1)).out(form("blockIngot", "CoalCoke", 1)).out(liquid("Creosote", thousandths(4500), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 32400, 111).item(form("blockGem", "Coal", 1)).out(form("blockGem", "CoalCoke", 1)).out(liquid("Creosote", thousandths(4500), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 112).item(form("gem", "Lignite", 1)).out(form("gem", "LigniteCoke", 1)).out(liquid("Creosote", thousandths(750), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 113).item(form("nugget", "Lignite", 9)).out(form("ingot", "LigniteCoke", 1)).out(liquid("Creosote", thousandths(750), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 114).item(form("chunkGt", "Lignite", 4)).out(form("ingot", "LigniteCoke", 1)).out(liquid("Creosote", thousandths(750), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 7200, 115).item(form("billet", "Lignite", 3)).out(form("ingot", "LigniteCoke", 2)).out(liquid("Creosote", thousandths(1500), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 116).item(form("ingot", "Lignite", 1)).out(form("ingot", "LigniteCoke", 1)).out(liquid("Creosote", thousandths(750), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 7200, 117).item(form("oreRaw", "Lignite", 1)).out(form("ingot", "LigniteCoke", 2)).out(liquid("Creosote", thousandths(1500), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 118).item(form("crushed", "Lignite", 1)).out(form("ingot", "LigniteCoke", 1)).out(liquid("Creosote", thousandths(750), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 119).item(form("crushedTiny", "Lignite", 9)).out(form("ingot", "LigniteCoke", 1)).out(liquid("Creosote", thousandths(750), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 120).item(form("crushedPurified", "Lignite", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(liquid("Creosote", thousandths(750), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 121).item(form("crushedPurifiedTiny", "Lignite", 9)).out(form("chunkGt", "LigniteCoke", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(liquid("Creosote", thousandths(750), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 122).item(form("crushedCentrifuged", "Lignite", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(liquid("Creosote", thousandths(750), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 123).item(form("crushedCentrifugedTiny", "Lignite", 9)).out(form("chunkGt", "LigniteCoke", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(form("chunkGt", "LigniteCoke", 1)).out(liquid("Creosote", thousandths(750), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 32400, 124).item(form("blockIngot", "Lignite", 1)).out(form("blockIngot", "LigniteCoke", 1)).out(liquid("Creosote", thousandths(6750), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 32400, 125).item(form("blockGem", "Lignite", 1)).out(form("blockGem", "LigniteCoke", 1)).out(liquid("Creosote", thousandths(6750), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 126).item(form("dust", "Oilshale", 1)).out(form("dustTiny", "Asphalt", 1)).out(liquid("Oil", thousandths(250), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 7200, 127).item(form("oreRaw", "Oilshale", 1)).out(form("dustTiny", "Asphalt", 2)).out(liquid("Oil", thousandths(500), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 128).item(form("crushed", "Oilshale", 1)).out(form("dustTiny", "Asphalt", 1)).out(liquid("Oil", thousandths(250), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 129).item(form("crushedTiny", "Oilshale", 9)).out(form("dustTiny", "Asphalt", 1)).out(liquid("Oil", thousandths(250), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 130).item(form("crushedPurified", "Oilshale", 1)).out(form("dustTiny", "Asphalt", 1)).out(liquid("Oil", thousandths(250), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 131).item(form("crushedPurifiedTiny", "Oilshale", 9)).out(form("dustTiny", "Asphalt", 1)).out(liquid("Oil", thousandths(250), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 132).item(form("crushedCentrifuged", "Oilshale", 1)).out(form("dustTiny", "Asphalt", 1)).out(liquid("Oil", thousandths(250), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 3600, 133).item(form("crushedCentrifugedTiny", "Oilshale", 9)).out(form("dustTiny", "Asphalt", 1)).out(liquid("Oil", thousandths(250), CEIL)).add(rows);
        row(SOURCE, "CokeOven", 0, 32400, 134).item(form("blockDust", "Oilshale", 1)).out(form("dust", "Asphalt", 1)).out(liquid("Oil", thousandths(2250), CEIL)).add(rows);
        row(SOURCE, "Nanofab", 16, 64, 135).kept(selector(0)).item(form("dustDiv72", "C", 18)).out(form("foil", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 136).kept(selector(0)).item(form("dustTiny", "C", 9)).out(form("foil", "Graphene", 4)).add(rows);
        row(SOURCE, "Nanofab", 16, 64, 137).kept(selector(0)).item(form("dustSmall", "C", 1)).out(form("foil", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 138).kept(selector(0)).item(form("dust", "C", 1)).out(form("foil", "Graphene", 4)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 139).kept(selector(1)).item(form("dustTiny", "C", 9)).out(form("plate", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 140).kept(selector(1)).item(form("dustSmall", "C", 4)).out(form("plate", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 141).kept(selector(1)).item(form("dust", "C", 1)).out(form("plate", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 142).kept(selector(2)).item(form("dustTiny", "C", 9)).out(form("plateCurved", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 143).kept(selector(2)).item(form("dustSmall", "C", 4)).out(form("plateCurved", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 144).kept(selector(2)).item(form("dust", "C", 1)).out(form("plateCurved", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 32, 145).kept(selector(4)).item(form("dustDiv72", "C", 9)).out(form("wireFine", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 146).kept(selector(4)).item(form("dustTiny", "C", 9)).out(form("wireFine", "Graphene", 8)).add(rows);
        row(SOURCE, "Nanofab", 16, 64, 147).kept(selector(4)).item(form("dustSmall", "C", 1)).out(form("wireFine", "Graphene", 2)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 148).kept(selector(4)).item(form("dust", "C", 1)).out(form("wireFine", "Graphene", 8)).add(rows);
        row(SOURCE, "Nanofab", 16, 32, 149).kept(selector(6)).item(form("dustDiv72", "C", 9)).out(form("bolt", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 150).kept(selector(6)).item(form("dustTiny", "C", 9)).out(form("bolt", "Graphene", 8)).add(rows);
        row(SOURCE, "Nanofab", 16, 64, 151).kept(selector(6)).item(form("dustSmall", "C", 1)).out(form("bolt", "Graphene", 2)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 152).kept(selector(6)).item(form("dust", "C", 1)).out(form("bolt", "Graphene", 8)).add(rows);
        row(SOURCE, "Nanofab", 16, 128, 153).kept(selector(7)).item(form("dustDiv72", "C", 36)).out(form("stick", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 154).kept(selector(7)).item(form("dustTiny", "C", 9)).out(form("stick", "Graphene", 2)).add(rows);
        row(SOURCE, "Nanofab", 16, 128, 155).kept(selector(7)).item(form("dustSmall", "C", 2)).out(form("stick", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 156).kept(selector(7)).item(form("dust", "C", 1)).out(form("stick", "Graphene", 2)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 157).kept(selector(8)).item(form("dustTiny", "C", 9)).out(form("stickLong", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 158).kept(selector(8)).item(form("dustSmall", "C", 4)).out(form("stickLong", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 159).kept(selector(8)).item(form("dust", "C", 1)).out(form("stickLong", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 64, 160).kept(selector(9)).item(form("dustDiv72", "C", 18)).out(form("ring", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 161).kept(selector(9)).item(form("dustTiny", "C", 9)).out(form("ring", "Graphene", 4)).add(rows);
        row(SOURCE, "Nanofab", 16, 64, 162).kept(selector(9)).item(form("dustSmall", "C", 1)).out(form("ring", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 163).kept(selector(9)).item(form("dust", "C", 1)).out(form("ring", "Graphene", 4)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 164).kept(selector(10)).item(form("dustTiny", "C", 9)).out(form("gearGtSmall", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 165).kept(selector(10)).item(form("dustSmall", "C", 4)).out(form("gearGtSmall", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 256, 166).kept(selector(10)).item(form("dust", "C", 1)).out(form("gearGtSmall", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 1024, 167).kept(selector(11)).item(form("dustTiny", "C", 36)).out(form("gearGt", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 1024, 168).kept(selector(11)).item(form("dustSmall", "C", 16)).out(form("gearGt", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 1024, 169).kept(selector(11)).item(form("dust", "C", 4)).out(form("gearGt", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 1088, 170).kept(selector(12)).item(form("dustSmall", "C", 17)).out(form("rotor", "Graphene", 1)).add(rows);
        row(SOURCE, "Nanofab", 16, 4352, 171).kept(selector(12)).item(form("dust", "C", 17)).out(form("rotor", "Graphene", 4)).add(rows);
        row(SOURCE, "Replicator", 16, 144, 172).kept(selector(0)).fluid(nativeFluid("Ender", 250)).out(form("gem", "EnderPearl", 1)).add(rows);
        row(SOURCE, "Massfab", 16, 144, 173).kept(selector(0)).item(form("dustDiv72", "AncientDebris", 1)).out(nativeFluid("Ender", 144)).add(rows);
        row(SOURCE, "Massfab", 16, 1152, 174).kept(selector(0)).item(form("dustTiny", "AncientDebris", 1)).out(nativeFluid("Ender", 1152)).add(rows);
        row(SOURCE, "Massfab", 16, 2592, 175).kept(selector(0)).item(form("dustSmall", "AncientDebris", 1)).out(nativeFluid("Ender", 2592)).add(rows);
        row(SOURCE, "Massfab", 16, 10368, 176).kept(selector(0)).item(form("dust", "AncientDebris", 1)).out(nativeFluid("Ender", 10368)).add(rows);
        row(SOURCE, "Massfab", 16, 10368, 177).kept(selector(0)).item(form("ingot", "AncientDebris", 1)).out(nativeFluid("Ender", 10368)).add(rows);
        row(SOURCE, "Massfab", 16, 93312, 178).kept(selector(0)).item(form("blockDust", "AncientDebris", 1)).out(nativeFluid("Ender", 93312)).add(rows);
        row(SOURCE, "Massfab", 16, 93312, 179).kept(selector(0)).item(form("blockIngot", "AncientDebris", 1)).out(nativeFluid("Ender", 93312)).add(rows);
        row(SOURCE, "Replicator", 16, 144, 180).kept(selector(0)).fluid(nativeFluid("Ender", 144)).out(form("gem", "EnderPearl", 1)).add(rows);
        row(SOURCE, "Replicator", 16, 18, 181).kept(selector(0)).fluid(nativeFluid("Redstone", 25)).out(form("gemChipped", "Redstone", 1)).add(rows);
        row(SOURCE, "Replicator", 16, 36, 182).kept(selector(1)).fluid(nativeFluid("Redstone", 50)).out(form("gemFlawed", "Redstone", 1)).add(rows);
        row(SOURCE, "Replicator", 16, 72, 183).kept(selector(2)).fluid(nativeFluid("Redstone", 100)).out(form("gem", "Redstone", 1)).add(rows);
        row(SOURCE, "Replicator", 16, 144, 184).kept(selector(3)).fluid(nativeFluid("Redstone", 200)).out(form("gemFlawless", "Redstone", 1)).add(rows);
        row(SOURCE, "Replicator", 16, 288, 185).kept(selector(4)).fluid(nativeFluid("Redstone", 400)).out(form("gemExquisite", "Redstone", 1)).add(rows);
        row(SOURCE, "Replicator", 16, 576, 186).kept(selector(5)).fluid(nativeFluid("Redstone", 800)).out(form("gemLegendary", "Redstone", 1)).add(rows);
        row(SOURCE, "Replicator", 16, 18, 187).kept(selector(0)).fluid(nativeFluid("Redstone", 36)).out(form("gemChipped", "Redstone", 1)).add(rows);
        row(SOURCE, "Replicator", 16, 36, 188).kept(selector(1)).fluid(nativeFluid("Redstone", 72)).out(form("gemFlawed", "Redstone", 1)).add(rows);
        row(SOURCE, "Replicator", 16, 72, 189).kept(selector(2)).fluid(nativeFluid("Redstone", 144)).out(form("gem", "Redstone", 1)).add(rows);
        row(SOURCE, "Replicator", 16, 144, 190).kept(selector(3)).fluid(nativeFluid("Redstone", 288)).out(form("gemFlawless", "Redstone", 1)).add(rows);
        row(SOURCE, "Replicator", 16, 288, 191).kept(selector(4)).fluid(nativeFluid("Redstone", 576)).out(form("gemExquisite", "Redstone", 1)).add(rows);
        row(SOURCE, "Replicator", 16, 576, 192).kept(selector(5)).fluid(nativeFluid("Redstone", 1152)).out(form("gemLegendary", "Redstone", 1)).add(rows);    }
}
