package gregtech.content.recipe.generated;

import static gregtech.content.recipe.generated.GeneratedRows.*;
import static gregtech.engine.recipe.chem.FluidPhase.LIQUID;
import static gregtech.engine.recipe.chem.Rounding.CEIL;
import static gregtech.engine.recipe.chem.Rounding.FLOOR;

import gregtech.engine.recipe.chem.ChemRecipe;
import java.util.ArrayList;
import java.util.List;

/**
 * GT6's comb processing: the {@code RM.Centrifuge.addRecipe1} rows of
 * {@code MultiItemFood.java:259-274}, which turn a comb into its fluid, wax and side products.
 *
 * <p>GT6 registers those rows inside its food item class. The port's comb items therefore had no
 * machine row until this producer transcribed them. Other mods' optional tails (Forestry
 * propolis, Et Futurum chorus fruit) are recorded as exclusions instead of being approximated.</p>
 */
public final class GTCombRecipes {
    private static final String SOURCE = "gt6@3703e403/MultiItemFood:";

    private GTCombRecipes() {}

    public static void register() {
        List<ChemRecipe> rows = new ArrayList<>();
        honeyRows(rows);
        fluidRows(rows);
        waxRows(rows);
        materialRows(rows);
        ChemRecipeResolver.exclude(SOURCE + "259/260/263/265",
                "Forestry propolis and Et Futurum chorus fruit are other mods' items; the row keeps its own outputs");
        ChemRecipeResolver.exclude(SOURCE + "602-785",
                "the food slicer rows in MultiItemFood are owned by FoodItemRecipes");
        publish("comb recipes", rows);
    }

    private static void honeyRows(List<ChemRecipe> rows) {
        row(SOURCE + "259", "Centrifuge", 16, 64)
                .item(tech("honey_comb", 1)).out(nativeFluid("Honey", 100))
                .out(dust("WaxBee", U(1)), 1000).add(rows);
        row(SOURCE + "260", "Centrifuge", 16, 64)
                .item(tech("water_comb", 1)).out(water(waterUnits(1000), FLOOR))
                .out(dust("WaxBee", U(1)), 1000).add(rows);
        row(SOURCE + "267", "Centrifuge", 16, 64)
                .item(tech("shroomy_comb", 1)).out(nativeFluid("Soup_Mushroom", 1000))
                .out(vanilla("red_mushroom_block", 1), 6000)
                .out(vanilla("brown_mushroom_block", 1), 6000).add(rows);
    }

    private static void fluidRows(List<ChemRecipe> rows) {
        row(SOURCE + "261", "Centrifuge", 16, 64)
                .item(tech("magic_comb", 1)).out(nativeFluid("Ambrosia", 100))
                .out(dust("WaxMagic", U(1)), 1000).add(rows);
        row(SOURCE + "262", "Centrifuge", 16, 64)
                .item(tech("nether_comb", 1)).out(nativeFluid("Blaze", 1000))
                .out(dust("WaxRefractory", U(1)), 1000).add(rows);
        row(SOURCE + "263", "Centrifuge", 16, 64)
                .item(tech("end_comb", 1)).out(nativeFluid("Dragon_Breath", 125))
                .out(dust("Endstone", U(1)), 1000).add(rows);
        row(SOURCE + "264", "Centrifuge", 16, 64)
                .item(tech("rock_comb", 1)).out(nativeFluid("Concrete", 1000))
                .out(dust("Stone", U(1)), 1000).add(rows);
        row(SOURCE + "266", "Centrifuge", 16, 64)
                .item(tech("frozen_comb", 1)).out(nativeFluid("Ice", 1000))
                .out(dust("Ice", U(1)), 1000).add(rows);
        row(SOURCE + "268", "Centrifuge", 16, 64)
                .item(tech("sandy_comb", 1)).out(nativeFluid("Juice_Cactus", 100))
                .out(vanilla("sand", 1), 1000).add(rows);
        row(SOURCE + "269", "Centrifuge", 16, 64)
                .item(tech("soul_comb", 1)).out(nativeFluid("Oil_Soulsand", 50))
                .out(dust("WaxSoulful", U(1)), 9000).out(dust("SoulSand", U(1)), 9000).add(rows);
        row(SOURCE + "270", "Centrifuge", 16, 64)
                .item(tech("amnesic_comb", 1)).out(nativeFluid("Lubricant", 1000))
                .out(dust("WaxAmnesic", U(1)), 1000).add(rows);
    }

    private static void waxRows(List<ChemRecipe> rows) {
        row(SOURCE + "265", "Centrifuge", 16, 64)
                .item(tech("jungle_comb", 1)).out(liquid("Chocolate", U(1), CEIL))
                .out(dust("Cocoa", U(1)), 1000).out(vanilla("string", 1), 1000).add(rows);
        row(SOURCE + "272", "Centrifuge", 16, 64)
                .item(tech("royal_comb", 1)).out(nativeFluid("Honey", 50))
                .out(nativeFluid("RoyalJelly", 10)).out(dust("WaxBee", U(1))).add(rows);
        row(SOURCE + "274", "Centrifuge", true, 16, 64)
                .item(tech("sticky_comb", 1)).out(nativeFluid("Latex", 1000))
                .out(dust("WaxBee", U(1)))
                .out(optional("sticky_resin"), 3000).add(rows);
    }

    private static void materialRows(List<ChemRecipe> rows) {
        row(SOURCE + "271", "Centrifuge", 16, 64)
                .item(tech("clay_comb", 1)).out(nativeFluid("Concrete", 1000))
                .out(dust("Clay", U(1)), 2000).out(dust("ClayBrown", U(1)), 2000)
                .out(dust("ClayRed", U(1)), 2000).out(dust("Bentonite", U(1)), 2000)
                .out(dust("Palygorskite", U(1)), 2000).out(dust("Kaolinite", U(1)), 2000).add(rows);
        row(SOURCE + "276", "Centrifuge", 16, 64)
                .item(tech("military_comb", 1)).out(nativeFluid("Potion_Harm_1", 50))
                .out(dust("Bone", U(1))).out(vanilla("bone", 1), 500)
                .out(vanilla("rotten_flesh", 1), 500).out(vanilla("spider_eye", 1), 250).add(rows);
        row(SOURCE + "277", "Centrifuge", 16, 64)
                .item(tech("pyro_comb", 1)).out(nativeFluid("Blaze", 72))
                .out(dust("Blaze", U(1, 9))).out(form("stick", "Blaze", 1), 1000).add(rows);
        row(SOURCE + "278", "Centrifuge", 16, 64)
                .item(tech("cryo_comb", 1)).out(nativeFluid("Ice", 500))
                .out(dust("Blizz", U(1, 9))).out(form("stick", "Blizz", 1), 1000).add(rows);
        row(SOURCE + "279", "Centrifuge", 16, 64)
                .item(tech("aero_comb", 1)).out(nativeFluid("Dragon_Breath", 50))
                .out(dust("Blitz", U(1, 9))).out(form("stick", "Blitz", 1), 500)
                .out(form("stick", "Breeze", 1), 500).add(rows);
        row(SOURCE + "280", "Centrifuge", 16, 64)
                .item(tech("tera_comb", 1)).out(nativeFluid("Concrete", 1000))
                .out(dust("Basalz", U(1, 9))).out(form("stick", "Basalz", 1), 1000).add(rows);
    }
}
