package gregtech.content.recipe.generated;

import static gregtech.content.recipe.generated.GeneratedRows.*;
import static gregtech.engine.recipe.chem.Rounding.CEIL;
import static gregtech.engine.recipe.chem.Rounding.FLOOR;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialGroups;
import gregtech.engine.recipe.chem.ChemFluid;
import gregtech.engine.recipe.chem.ChemIngredient;
import gregtech.engine.recipe.chem.ChemRecipe;
import java.util.ArrayList;
import java.util.List;

/**
 * The machine rows of {@code Loader_Recipes_Vanilla}. The file's crafting-table calls stay with
 * the port's hand-written vanilla recipe sets and are not repeated here.
 *
 * <p>Colour loops and the water loops in the source are expanded, so no colour or water variant
 * is silently dropped. Rows that a dedicated explicit producer already owns (textile finishing,
 * survival utilities) are recorded as substitutions instead of being registered a second time.
 * The Lathe stone metavariants are excluded because this port registers explicit native rows for
 * those distinct materials.</p>
 */
public final class GTVanillaRecipes {
    private static final String SOURCE = "gt6@3703e403/Loader_Recipes_Vanilla:row/";

    private static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime",
        "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};

    /** The source iterates water, mineral water and distilled water; spectral dew is retired. */
    private static final String[] WATERS = {"Water", "MnWtr", "DistW"};

    /** GT6's ANY.Blaze family: every blaze-like material keeps the same stick/dust routes. */
    private static final String[] BLAZES = {"Blaze", "Blizz", "Basalz", "Blitz", "Breeze"};

    private GTVanillaRecipes() {}

    public static void register() {
        List<ChemRecipe> rows = new ArrayList<>();
        sourceRows(rows);
        glassRows(rows);
        terracottaRows(rows);
        freezerRows(rows);
        lavaStoneRows(rows);
        tntRows(rows);
        blazeRows(rows);
        woodSawRows(rows);
        caneRows(rows);
        ChemRecipeResolver.exclude(SOURCE + "5-10",
                "the Lathe stone metavariants duplicate this port's explicit native granite/diorite/andesite rows");
        ChemRecipeResolver.exclude(SOURCE + "166/211/754/892",
                "the source iterates four water fluids; spectral dew is retired in this port");
        ChemRecipeResolver.exclude(SOURCE + "188-203/232-246",
                "TextileFinishingRecipes and SurvivalUtilityRecipes own the textile, horse-armour, bucket, "
                        + "obsidian, golden-apple, ender-eye and fire-charge rows");
        ChemRecipeResolver.exclude(SOURCE + "887",
                "the redrock material is not registered in this port");
        ChemRecipeResolver.exclude(SOURCE + "958",
                "the biomass helper registers machines this port does not ship");
        publish("vanilla recipes", rows);
    }

    private static void sourceRows(List<ChemRecipe> rows) {
        row(SOURCE, "Sharpening", 16, 64, 1).item(vanilla("flint", 1)).out(form("toolHeadArrow", "Flint", 8)).out(form("dustTiny", "Flint", 1)).add(rows);
        row(SOURCE, "Sharpening", 16, 64, 2).item(vanilla("glass_pane", 1)).out(form("lens", "Glass", 1)).add(rows);
        row(SOURCE, "Lathe", 16, 16, 3).item(vanilla("glass_pane", 1)).out(form("lens", "Glass", 1)).out(form("dustSmall", "Glass", 1)).add(rows);
        row(SOURCE, "Lathe", 16, 16, 4).item(vanilla("stone", 1)).out(form("stickLong", "Stone", 1)).add(rows);
        row(SOURCE, "Lathe", 16, 16, 5).item(vanilla("granite", 1)).out(form("stickLong", "Stone", 1)).add(rows);
        row(SOURCE, "Lathe", 16, 16, 6).item(vanilla("polished_granite", 1)).out(form("stickLong", "Stone", 1)).add(rows);
        row(SOURCE, "Lathe", 16, 16, 7).item(vanilla("diorite", 1)).out(form("stickLong", "Stone", 1)).add(rows);
        row(SOURCE, "Lathe", 16, 16, 8).item(vanilla("polished_diorite", 1)).out(form("stickLong", "Stone", 1)).add(rows);
        row(SOURCE, "Lathe", 16, 16, 9).item(vanilla("andesite", 1)).out(form("stickLong", "Stone", 1)).add(rows);
        row(SOURCE, "Lathe", 16, 16, 10).item(vanilla("polished_andesite", 1)).out(form("stickLong", "Stone", 1)).add(rows);
        row(SOURCE, "Lathe", 16, 32, 11).kept(tech("module_stone_generator", 1)).out(form("stickLong", "Stone", 1)).add(rows);
        row(SOURCE, "Lathe", 16, 32, 12).kept(tech("module_basalt_generator", 1)).out(form("stickLong", "Basalt", 1)).add(rows);
        row(SOURCE, "Lathe", 16, 32, 13).kept(tech("module_blackstone_generator", 1)).out(form("stickLong", "Blackstone", 1)).add(rows);
        row(SOURCE, "Extruder", 16, 32, 14).kept(tech("module_stone_generator", 1)).item(form("dust", "Redstone", 1)).out(vanilla("obsidian", 1)).add(rows);
        row(SOURCE, "Hammer", 16, 16, 15).item(vanilla("bricks", 1)).out(vanilla("brick", 3)).add(rows);
        row(SOURCE, "Hammer", 16, 16, 16).item(vanilla("brick_stairs", 1)).out(vanilla("brick", 2)).add(rows);
        row(SOURCE, "Hammer", 16, 16, 17).item(vanilla("brick_slab", 1)).out(vanilla("brick", 1)).add(rows);
        row(SOURCE, "Crusher", 16, 64, 18).item(vanilla("bricks", 1)).out(vanilla("brick", 1), 10000).out(vanilla("brick", 1), 9000).out(vanilla("brick", 1), 8000).out(vanilla("brick", 1), 7000).add(rows);
        row(SOURCE, "Crusher", 16, 64, 19).item(vanilla("brick_stairs", 1)).out(vanilla("brick", 1), 10000).out(vanilla("brick", 1), 9000).out(vanilla("brick", 1), 8000).add(rows);
        row(SOURCE, "Crusher", 16, 32, 20).item(vanilla("brick_slab", 1)).out(vanilla("brick", 1), 10000).out(vanilla("brick", 1), 8000).add(rows);
        row(SOURCE, "Hammer", 16, 16, 21).item(vanilla("nether_bricks", 1)).out(vanilla("nether_brick", 3)).add(rows);
        row(SOURCE, "Hammer", 16, 16, 22).item(vanilla("nether_brick_stairs", 1)).out(vanilla("nether_brick", 2)).add(rows);
        row(SOURCE, "Hammer", 16, 16, 23).item(vanilla("nether_brick_fence", 1)).out(vanilla("nether_brick", 2)).add(rows);
        row(SOURCE, "Hammer", 16, 16, 24).item(vanilla("nether_brick_slab", 1)).out(vanilla("nether_brick", 1)).add(rows);
        row(SOURCE, "Crusher", 16, 64, 25).item(vanilla("nether_bricks", 1)).out(vanilla("nether_brick", 1), 10000).out(vanilla("nether_brick", 1), 9000).out(vanilla("nether_brick", 1), 8000).out(vanilla("nether_brick", 1), 7000).add(rows);
        row(SOURCE, "Crusher", 16, 64, 26).item(vanilla("nether_brick_stairs", 1)).out(vanilla("nether_brick", 1), 10000).out(vanilla("nether_brick", 1), 9000).out(vanilla("nether_brick", 1), 8000).add(rows);
        row(SOURCE, "Crusher", 16, 64, 27).item(vanilla("nether_brick_fence", 1)).out(vanilla("nether_brick", 1), 10000).out(vanilla("nether_brick", 1), 9000).out(vanilla("nether_brick", 1), 8000).add(rows);
        row(SOURCE, "Crusher", 16, 32, 28).item(vanilla("nether_brick_slab", 1)).out(vanilla("nether_brick", 1), 10000).out(vanilla("nether_brick", 1), 8000).add(rows);
        row(SOURCE, "Slicer", 16, 16, 29).item(vanilla("leather_helmet", 1)).kept(tech("slicer_shape_split", 1)).out(vanilla("leather", 1)).add(rows);
        row(SOURCE, "Slicer", 16, 16, 30).item(vanilla("leather_chestplate", 1)).kept(tech("slicer_shape_split", 1)).out(vanilla("leather", 2)).add(rows);
        row(SOURCE, "Slicer", 16, 16, 31).item(vanilla("leather_leggings", 1)).kept(tech("slicer_shape_split", 1)).out(vanilla("leather", 2)).add(rows);
        row(SOURCE, "Slicer", 16, 16, 32).item(vanilla("leather_boots", 1)).kept(tech("slicer_shape_split", 1)).out(vanilla("leather", 1)).add(rows);
        row(SOURCE, "Slicer", 16, 16, 33).item(vanilla("melon", 1)).kept(tech("slicer_shape_eigths", 1)).out(vanilla("melon", 8)).out(vanilla("melon_seeds", 1)).add(rows);
        row(SOURCE, "Compressor", 64, 32, 34).item(vanilla("ice", 2)).out(vanilla("packed_ice", 1)).add(rows);
        row(SOURCE, "Compressor", 16, 32, 35).item(dust("Ice", thousandths(1000))).out(vanilla("ice", 1)).add(rows);
        row(SOURCE, "Compressor", 16, 16, 36).item(dust("Ice", thousandths(250))).out(form("gemChipped", "Ice", 1)).add(rows);
        row(SOURCE, "Compressor", 16, 16, 37).item(form("gemChipped", "Ice", 4)).out(vanilla("ice", 1)).add(rows);
        row(SOURCE, "Compressor", 16, 16, 38).item(form("gemFlawed", "Ice", 2)).out(vanilla("ice", 1)).add(rows);
        row(SOURCE, "Compressor", 16, 16, 39).item(form("gem", "Ice", 1)).out(vanilla("ice", 1)).add(rows);
        row(SOURCE, "Compressor", 16, 32, 40).item(vanilla("snow", 1)).out(vanilla("ice", 1)).add(rows);
        row(SOURCE, "Compressor", 16, 32, 41).item(vanilla("snowball", 4)).out(vanilla("snow", 1)).add(rows);
        row(SOURCE, "Compressor", 16, 16, 42).item(vanilla("quartz", 4)).out(vanilla("quartz_block", 1)).add(rows);
        row(SOURCE, "Compressor", 16, 16, 43).item(tech("crop_wheat", 9)).out(tech("bale_wheat", 1)).add(rows);
        row(SOURCE, "Compressor", 16, 32, 44).item(vanilla("sand", 4)).out(vanilla("sandstone", 1)).add(rows);
        row(SOURCE, "Compressor", 16, 32, 45).item(dust("Lapis", thousandths(1000))).out(form("plateGem", "Lapis", 1)).add(rows);
        row(SOURCE, "Compressor", 16, 32, 46).item(dust("Asbestos", thousandths(1000))).out(form("plate", "Asbestos", 1)).add(rows);
        row(SOURCE, "Compressor", 16, 32, 47).item(dust("Lazurite", thousandths(1000))).out(form("plateGem", "Lazurite", 1)).add(rows);
        row(SOURCE, "Compressor", 16, 32, 48).item(dust("Sodalite", thousandths(1000))).out(form("plateGem", "Sodalite", 1)).add(rows);
        row(SOURCE, "Mortar", 16, 32, 83).item(vanilla("bone", 1)).out(tech("dye_bonemeal", 2)).add(rows);
        row(SOURCE, "Mortar", 16, 64, 84).item(tech("wimo_thick_bone", 1)).out(tech("dye_bonemeal", 4)).add(rows);
        row(SOURCE, "Mortar", 16, 16, 85).item(vanilla("flint", 1)).out(form("dust", "Flint", 1)).add(rows);
        row(SOURCE, "Mortar", 16, 32, 86).item(vanilla("gravel", 1)).out(vanilla("flint", 1)).add(rows);
        row(SOURCE, "Mortar", 16, 16, 87).item(vanilla("coal", 1)).out(dust("Coal", thousandths(1000))).add(rows);
        row(SOURCE, "Mortar", 16, 16, 88).item(vanilla("charcoal", 1)).out(dust("Charcoal", thousandths(1000))).add(rows);
        row(SOURCE, "Mortar", 16, 16, 89).item(vanilla("rotten_flesh", 1)).out(form("dust", "MeatRotten", 1)).add(rows);
        row(SOURCE, "Mortar", 16, 16, 90).item(tech("wimo_infected_meat", 1)).out(form("dust", "MeatRotten", 1)).add(rows);
        row(SOURCE, "Shredder", 16, 16, 91).item(vanilla("flint", 1)).out(form("dust", "Flint", 1)).add(rows);
        row(SOURCE, "Shredder", 16, 16, 92).item(vanilla("gravel", 1)).out(vanilla("sand", 1)).add(rows);
        row(SOURCE, "Shredder", 16, 16, 93).item(vanilla("cobweb", 1)).out(vanilla("string", 1)).add(rows);
        row(SOURCE, "Shredder", 16, 16, 94).item(vanilla("sugar_cane", 1)).out(tech("remains_plant", 1)).add(rows);
        row(SOURCE, "Shredder", 16, 16, 95).item(vanilla("cobblestone", 1)).out(dust("Stone", thousandths(9000))).add(rows);
        row(SOURCE, "Shredder", 16, 16, 96).item(vanilla("stone", 1)).out(dust("Stone", thousandths(9000))).add(rows);
        row(SOURCE, "Shredder", 16, 16, 97).item(vanilla("granite", 1)).out(dust("Stone", thousandths(9000))).add(rows);
        row(SOURCE, "Shredder", 16, 16, 98).item(vanilla("polished_granite", 1)).out(dust("Stone", thousandths(9000))).add(rows);
        row(SOURCE, "Shredder", 16, 16, 99).item(vanilla("diorite", 1)).out(dust("Stone", thousandths(9000))).add(rows);
        row(SOURCE, "Shredder", 16, 16, 100).item(vanilla("polished_diorite", 1)).out(dust("Stone", thousandths(9000))).add(rows);
        row(SOURCE, "Shredder", 16, 16, 101).item(vanilla("andesite", 1)).out(dust("Stone", thousandths(9000))).add(rows);
        row(SOURCE, "Shredder", 16, 16, 102).item(vanilla("polished_andesite", 1)).out(dust("Stone", thousandths(9000))).add(rows);
        row(SOURCE, "Shredder", 16, 16, 103).kept(tech("module_stone_generator", 1)).out(dust("Stone", thousandths(1000))).add(rows);
        row(SOURCE, "Shredder", 16, 16, 104).kept(tech("module_basalt_generator", 1)).out(dust("Basalt", thousandths(1000))).add(rows);
        row(SOURCE, "Shredder", 16, 16, 105).kept(tech("module_blackstone_generator", 1)).out(dust("Blackstone", thousandths(1000))).add(rows);
        row(SOURCE, "Shredder", 16, 32, 106).item(vanilla("bone", 1)).out(tech("dye_bonemeal", 4)).add(rows);
        row(SOURCE, "Shredder", 16, 64, 107).item(tech("wimo_thick_bone", 1)).out(tech("dye_bonemeal", 8)).add(rows);
        row(SOURCE, "Mortar", 16, 32, 108).item(form("stick", "Blaze", 1)).out(form("dustTiny", "Blaze", 3)).add(rows);
        row(SOURCE, "Mortar", 16, 64, 109).item(form("stickLong", "Blaze", 1)).out(form("dustTiny", "Blaze", 6)).add(rows);
        row(SOURCE, "Shredder", 16, 32, 110).item(form("stick", "Blaze", 1)).out(form("dustSmall", "Blaze", 2)).add(rows);
        row(SOURCE, "Shredder", 16, 64, 111).item(form("stickLong", "Blaze", 1)).out(form("dust", "Blaze", 1)).add(rows);
        row(SOURCE, "Compressor", 16, 32, 112).item(form("dust", "Blaze", 1)).out(form("plate", "Blaze", 1)).add(rows);
        row(SOURCE, "Compressor", 16, 32, 113).item(form("dustSmall", "Blaze", 4)).out(form("plate", "Blaze", 1)).add(rows);
        row(SOURCE, "Compressor", 16, 32, 114).item(form("dustTiny", "Blaze", 9)).out(form("plate", "Blaze", 1)).add(rows);
        row(SOURCE, "Chisel", 16, 16, 179).item(vanilla("stone", 1)).out(vanilla("stone_bricks", 1)).add(rows);
        row(SOURCE, "Chisel", 16, 16, 180).item(vanilla("granite", 1)).out(vanilla("stone_bricks", 1)).add(rows);
        row(SOURCE, "Chisel", 16, 16, 181).item(vanilla("polished_granite", 1)).out(vanilla("stone_bricks", 1)).add(rows);
        row(SOURCE, "Chisel", 16, 16, 182).item(vanilla("diorite", 1)).out(vanilla("stone_bricks", 1)).add(rows);
        row(SOURCE, "Chisel", 16, 16, 183).item(vanilla("polished_diorite", 1)).out(vanilla("stone_bricks", 1)).add(rows);
        row(SOURCE, "Chisel", 16, 16, 184).item(vanilla("andesite", 1)).out(vanilla("stone_bricks", 1)).add(rows);
        row(SOURCE, "Chisel", 16, 16, 185).item(vanilla("polished_andesite", 1)).out(vanilla("stone_bricks", 1)).add(rows);
        row(SOURCE, "Chisel", 16, 16, 186).item(vanilla("stone_bricks", 1)).out(vanilla("stone_bricks", 1)).add(rows);
        row(SOURCE, "Press", 16, 144, 187).kept(selector(1)).item(form("blockDust", "Redstone", 4)).item(form("blockDust", "Glowstone", 4)).out(vanilla("redstone_lamp", 9)).add(rows);
        row(SOURCE, "Press", 16, 16, 188).kept(selector(1)).item(form("dust", "Redstone", 4)).item(form("dust", "Glowstone", 4)).out(vanilla("redstone_lamp", 1)).add(rows);
        row(SOURCE, "Press", 16, 16, 189).kept(selector(1)).item(form("dust", "Redstone", 16)).item(form("dust", "Glowstone", 4)).out(vanilla("redstone_lamp", 1)).add(rows);
        row(SOURCE, "Press", 16, 16, 190).kept(selector(1)).item(form("dust", "Redstone", 36)).item(form("dust", "Glowstone", 4)).out(vanilla("redstone_lamp", 1)).add(rows);
        row(SOURCE, "Press", 16, 16, 191).kept(selector(1)).item(form("dust", "Redstone", 4)).item(form("dustSmall", "Glowstone", 16)).out(vanilla("redstone_lamp", 1)).add(rows);
        row(SOURCE, "Press", 16, 16, 192).kept(selector(1)).item(form("dust", "Redstone", 16)).item(form("dustSmall", "Glowstone", 16)).out(vanilla("redstone_lamp", 1)).add(rows);
        row(SOURCE, "Press", 16, 16, 193).kept(selector(1)).item(form("dust", "Redstone", 36)).item(form("dustSmall", "Glowstone", 16)).out(vanilla("redstone_lamp", 1)).add(rows);
        row(SOURCE, "Press", 16, 16, 194).kept(selector(1)).item(form("dust", "Redstone", 4)).item(form("dustTiny", "Glowstone", 36)).out(vanilla("redstone_lamp", 1)).add(rows);
        row(SOURCE, "Press", 16, 16, 195).kept(selector(1)).item(form("dust", "Redstone", 16)).item(form("dustTiny", "Glowstone", 36)).out(vanilla("redstone_lamp", 1)).add(rows);
        row(SOURCE, "Press", 16, 16, 196).kept(selector(1)).item(form("dust", "Redstone", 36)).item(form("dustTiny", "Glowstone", 36)).out(vanilla("redstone_lamp", 1)).add(rows);
        row(SOURCE, "Press", 16, 144, 197).kept(selector(1)).item(form("blockDust", "Gunpowder", 4)).item(form("blockDust", "SiO2", 4)).out(vanilla("tnt", 9)).add(rows);
        row(SOURCE, "Press", 16, 16, 198).kept(selector(1)).item(form("dust", "Gunpowder", 4)).item(form("dust", "SiO2", 4)).out(vanilla("tnt", 1)).add(rows);
        row(SOURCE, "Press", 16, 16, 199).kept(selector(1)).item(form("dustSmall", "Gunpowder", 16)).item(form("dust", "SiO2", 4)).out(vanilla("tnt", 1)).add(rows);
        row(SOURCE, "Press", 16, 16, 200).kept(selector(1)).item(form("dustTiny", "Gunpowder", 36)).item(form("dust", "SiO2", 4)).out(vanilla("tnt", 1)).add(rows);
        row(SOURCE, "Press", 16, 16, 201).kept(selector(1)).item(form("dust", "Gunpowder", 4)).item(form("dustSmall", "SiO2", 16)).out(vanilla("tnt", 1)).add(rows);
        row(SOURCE, "Press", 16, 16, 202).kept(selector(1)).item(form("dustSmall", "Gunpowder", 16)).item(form("dustSmall", "SiO2", 16)).out(vanilla("tnt", 1)).add(rows);
        row(SOURCE, "Press", 16, 16, 203).kept(selector(1)).item(form("dustTiny", "Gunpowder", 36)).item(form("dustSmall", "SiO2", 16)).out(vanilla("tnt", 1)).add(rows);
        row(SOURCE, "Press", 16, 16, 204).kept(selector(1)).item(form("dust", "Gunpowder", 4)).item(form("dustTiny", "SiO2", 36)).out(vanilla("tnt", 1)).add(rows);
        row(SOURCE, "Press", 16, 16, 205).kept(selector(1)).item(form("dustSmall", "Gunpowder", 16)).item(form("dustTiny", "SiO2", 36)).out(vanilla("tnt", 1)).add(rows);
        row(SOURCE, "Press", 16, 16, 206).kept(selector(1)).item(form("dustTiny", "Gunpowder", 36)).item(form("dustTiny", "SiO2", 36)).out(vanilla("tnt", 1)).add(rows);
        row(SOURCE, "Centrifuge", 16, 16, 222).item(vanilla("magma_cream", 1)).out(vanilla("blaze_powder", 1)).out(nativeFluid("Slime_Green", 125)).add(rows);
    }

    private static void freezerRows(List<ChemRecipe> rows) {
        // Loader_Recipes_Vanilla:661-672. Spectral dew alone used the x4 duration multiplier.
        for (String water : WATERS) {
            String source = SOURCE + "661/" + water;
            row(source, "Freezer", 16, 16).kept(selector(0))
                    .fluid(waterInput(water, 250)).out(nativeFluid("Ice", 250)).add(rows);
            row(source, "Freezer", 16, 16).kept(selector(1))
                    .fluid(waterInput(water, 250)).out(vanilla("snowball", 1)).add(rows);
            row(source, "Freezer", 16, 32).kept(selector(2))
                    .fluid(waterInput(water, 500)).out(vanilla("snow", 1)).add(rows);
            row(source, "Freezer", 16, 64).kept(selector(3))
                    .fluid(waterInput(water, 1000)).out(vanilla("snow_block", 1)).add(rows);
            row(source, "Freezer", 16, 128).kept(selector(4))
                    .fluid(waterInput(water, 1000)).out(vanilla("ice", 1)).add(rows);
            row(source, "Freezer", 16, 64).kept(selector(9))
                    .fluid(waterInput(water, 1000)).out(dust("Snow", U(1))).add(rows);
            row(source, "Freezer", 16, 128).kept(selector(8))
                    .fluid(waterInput(water, 1000)).out(dust("Ice", U(1))).add(rows);
            row(source, "Freezer", 16, 32).kept(selector(5))
                    .fluid(waterInput(water, 250)).out(form("gemChipped", "Ice", 1)).add(rows);
            row(source, "Freezer", 16, 64).kept(selector(6))
                    .fluid(waterInput(water, 500)).out(form("gemFlawed", "Ice", 1)).add(rows);
            row(source, "Freezer", 16, 128).kept(selector(7))
                    .fluid(waterInput(water, 1000)).out(form("gem", "Ice", 1)).add(rows);
        }
    }

    /** Loader_Recipes_Vanilla:881-886: pahoehoe lava with any water becomes GT basalt. */
    private static void lavaStoneRows(List<ChemRecipe> rows) {
        for (String water : WATERS)
            row(SOURCE + "881/" + water, "Mixer", 16, 16)
                    .fluid(waterInput(water, 50)).fluid(nativeFluid("Lava_Pahoehoe", 1000))
                    .out(vanilla("gt6ce:stone_basalt_stone", 1)).add(rows);
    }

    /** Loader_Recipes_Vanilla:788-800: TNT accepts any silica dust and block form. */
    private static void tntRows(List<ChemRecipe> rows) {
        for (GTMaterial silica : group(MaterialGroups.SiO2)) {
            String name = silica.getName();
            if (!hasForm("dust", name)) continue;
            String source = SOURCE + "788/" + name;
            tnt(rows, source, name, "blockDust", "Gunpowder", "blockDust", 4, 4, 144, 9);
            tnt(rows, source, name, "dust", "Gunpowder", "dust", 4, 4, 16, 1);
            tnt(rows, source, name, "dust", "Gunpowder", "dustSmall", 4, 16, 16, 1);
            tnt(rows, source, name, "dust", "Gunpowder", "dustTiny", 4, 36, 16, 1);
            tnt(rows, source, name, "dustSmall", "Gunpowder", "dust", 16, 4, 16, 1);
            tnt(rows, source, name, "dustSmall", "Gunpowder", "dustSmall", 16, 16, 16, 1);
            tnt(rows, source, name, "dustSmall", "Gunpowder", "dustTiny", 16, 36, 16, 1);
            tnt(rows, source, name, "dustTiny", "Gunpowder", "dust", 36, 4, 16, 1);
            tnt(rows, source, name, "dustTiny", "Gunpowder", "dustSmall", 36, 16, 16, 1);
            tnt(rows, source, name, "dustTiny", "Gunpowder", "dustTiny", 36, 36, 16, 1);
        }
    }

    private static void tnt(List<ChemRecipe> rows, String source, String material,
            String gunpowderPrefix, String gunpowderName, String silicaPrefix,
            int gunpowderCount, int silicaCount, int ticks, int output) {
        row(source, "Press", 16, ticks)
                .kept(selector(1)).item(form(gunpowderPrefix, gunpowderName, gunpowderCount))
                .item(form(silicaPrefix, material, silicaCount))
                .out(vanilla("tnt", output)).add(rows);
    }

    /** Loader_Recipes_Vanilla:702-712: the blaze family's stick and dust routes. */
    private static void blazeRows(List<ChemRecipe> rows) {
        for (String blaze : BLAZES) {
            if (!hasForm("stick", blaze)) continue;
            String source = SOURCE + "702/" + blaze;
            row(source, "Mortar", 16, 32).item(form("stick", blaze, 1))
                    .out(form("dustTiny", blaze, 3)).add(rows);
            if (hasForm("stickLong", blaze))
                row(source, "Mortar", 16, 64).item(form("stickLong", blaze, 1))
                        .out(form("dustTiny", blaze, 6)).add(rows);
            row(source, "Shredder", 16, 32).item(form("stick", blaze, 1))
                    .out(form("dustSmall", blaze, 2)).add(rows);
            if (hasForm("stickLong", blaze))
                row(source, "Shredder", 16, 64).item(form("stickLong", blaze, 1))
                        .out(form("dust", blaze, 1)).add(rows);
            if (!hasForm("plate", blaze)) continue;
            row(source, "Compressor", 16, 32).item(form("dust", blaze, 1))
                    .out(form("plate", blaze, 1)).add(rows);
            if (hasForm("dustSmall", blaze))
                row(source, "Compressor", 16, 32).item(form("dustSmall", blaze, 4))
                        .out(form("plate", blaze, 1)).add(rows);
            if (hasForm("dustTiny", blaze))
                row(source, "Compressor", 16, 32).item(form("dustTiny", blaze, 9))
                        .out(form("plate", blaze, 1)).add(rows);
        }
    }

    /**
     * Loader_Recipes_Vanilla:582-585: every wood's stick splits into bolts, and a long stick into
     * two sticks, through the Cutter's water coolants.
     */
    private static void woodSawRows(List<ChemRecipe> rows) {
        for (GTMaterial wood : group(MaterialGroups.Wood)) {
            String name = wood.getName();
            if (!hasForm("stick", name) || !hasForm("bolt", name)) continue;
            String source = SOURCE + "582/" + name;
            cutter(rows, source + "/bolt", form("stick", name, 1), form("bolt", name, 4), 3);
            if (hasForm("stickLong", name))
                cutter(rows, source + "/long", form("stickLong", name, 1), form("stick", name, 2), 1);
        }
    }

    private static void cutter(List<ChemRecipe> rows, String source, ChemIngredient input,
            ChemIngredient output, int lubricant) {
        row(source, "Cutter", 16, 64).item(input).fluid(nativeFluid("Water", lubricant * 4))
                .out(output).add(rows);
        row(source, "Cutter", 16, 64).item(input).fluid(nativeFluid("MnWtr", lubricant * 4))
                .out(output).add(rows);
        row(source, "Cutter", 16, 48).item(input).fluid(nativeFluid("DistW", lubricant * 3))
                .out(output).add(rows);
    }

    private static ChemFluid waterInput(String water, int millibuckets) {
        return water.equals("Water") ? water(waterUnits(millibuckets), FLOOR) : nativeFluid(water, millibuckets);
    }

    private static void glassRows(List<ChemRecipe> rows) {
        row(SOURCE + "49", "Mortar", 16, 32).item(vanilla("glass", 1)).out(dust("Glass", U(9))).add(rows);
        for (String color : COLORS)
            row(SOURCE + "50-65", "Mortar", 16, 32)
                    .item(vanilla(color + "_stained_glass", 1)).out(dust("Glass", U(9))).add(rows);
        row(SOURCE + "66", "Mortar", 16, 32).item(vanilla("glass_pane", 1)).out(dust("Glass", U(1))).add(rows);
        for (String color : COLORS)
            row(SOURCE + "67-82", "Mortar", 16, 32)
                    .item(vanilla(color + "_stained_glass_pane", 1)).out(dust("Glass", U(1))).add(rows);
    }

    private static void terracottaRows(List<ChemRecipe> rows) {
        for (String color : COLORS)
            row(SOURCE + "115-130", "Bath", 0, 16)
                    .item(vanilla(color + "_terracotta", 1)).fluid(gas("Cl", thousandths(50), CEIL))
                    .out(vanilla("terracotta", 1)).add(rows);
    }

    /** Reed/cane squeezing rows the transpiler skipped because their chance precedes the fluids. */
    private static void caneRows(List<ChemRecipe> rows) {
        row(SOURCE + "826", "Squeezer", 16, 16)
                .item(vanilla("sugar_cane", 1)).out(nativeFluid("Juice_Reed", 100))
                .out(tech("remains_plant", 1), 4000).add(rows);
        row(SOURCE + "864", "Juicer", 16, 16)
                .item(vanilla("sugar_cane", 1)).out(nativeFluid("Juice_Reed", 75))
                .out(tech("remains_plant", 1), 5000).add(rows);
    }

    private static boolean hasForm(String prefixName, String materialName) {
        var material = gregtech.engine.material.GTMaterialRegistry.get(materialName);
        if (material == null || !material.resolve().isValid()) return false;
        var entry = gregtech.content.data.MaterialPrefixes.all().get(prefixName);
        if (entry == null) return false;
        var prefix = entry.getDelegate();
        if (prefix != null)
            return !gregtech.content.registry.GTItems.getStack(prefix, material.resolve(), 1).isEmpty();
        var block = entry.getBlockDelegate();
        return block != null
                && !gregtech.content.registry.GTBlocks.getStack(block, material.resolve()).isEmpty();
    }
}
