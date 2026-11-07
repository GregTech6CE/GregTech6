package gregtech.content.recipe.generated;

import static gregtech.content.recipe.generated.GeneratedRows.*;
import static gregtech.engine.recipe.chem.Rounding.CEIL;
import static gregtech.engine.recipe.chem.Rounding.FLOOR;

import gregtech.engine.recipe.chem.ChemRecipe;
import java.util.ArrayList;
import java.util.List;

/**
 * The machine rows of {@code Loader_Recipes_Vanilla}. The file's crafting-table calls stay with
 * the port's hand-written vanilla recipe sets and are not repeated here.
 *
 * <p>Colour loops and the water loops in the source are expanded, so no colour or water variant
 * is silently dropped. The six GT6 stone metadata variants are excluded: this port registers
 * explicit native rows for granite, diorite and andesite, whose materials are distinct here.</p>
 */
public final class GTVanillaRecipes {
    private static final String SOURCE = "gt6@3703e403/Loader_Recipes_Vanilla:row/";

    private static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime",
        "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};

    /** The source iterates water, mineral water and distilled water; spectral dew is retired. */
    private static final String[] WATERS = {"Water", "MnWtr", "DistW"};

    private GTVanillaRecipes() {}

    public static void register() {
        List<ChemRecipe> rows = new ArrayList<>();
        sourceRows(rows);
        glassRows(rows);
        terracottaRows(rows);
        waterRows(rows);
        caneRows(rows);
        ChemRecipeResolver.exclude(SOURCE + "5-10",
                "the Lathe stone metavariants duplicate this port's explicit native granite/diorite/andesite rows");
        ChemRecipeResolver.exclude(SOURCE + "166/211/754/892",
                "the source iterates four water fluids; spectral dew is retired in this port");
        ChemRecipeResolver.exclude(SOURCE + "958",
                "the biomass helper registers machines this port does not ship");
        publish("vanilla recipes", rows);
    }

    private static void sourceRows(List<ChemRecipe> rows) {
        row(SOURCE, "Sharpening", 16, 64, 1).item(vanilla("flint", 1)).out(form("toolHeadArrow", "Flint", 8)).out(form("dustTiny", "Flint", 1)).add(rows);
        row(SOURCE, "Sharpening", 16, 64, 2).item(vanilla("glass_pane", 1)).out(form("lens", "Glass", 1)).add(rows);
        row(SOURCE, "Lathe", 16, 16, 3).item(vanilla("glass_pane", 1)).out(form("lens", "Glass", 1)).out(form("dustSmall", "Glass", 1)).add(rows);
        row(SOURCE, "Lathe", 16, 16, 4).item(vanilla("stone", 1)).out(form("stickLong", "Stone", 1)).add(rows);
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
        row(SOURCE, "Loom", 16, 16, 163).kept(selector(0)).item(vanilla("string", 4)).out(vanilla("white_wool", 1)).add(rows);
        row(SOURCE, "Loom", 16, 64, 164).kept(selector(1)).item(vanilla("string", 4)).out(vanilla("cobweb", 1)).add(rows);
        row(SOURCE, "Loom", 16, 16, 165).kept(selector(0)).item(vanilla("sugar_cane", 1)).out(vanilla("paper", 1)).add(rows);
        row(SOURCE, "Loom", 64, 128, 167).item(vanilla("leather", 6)).item(form("plate", "Au", 8)).out(vanilla("golden_horse_armor", 1)).add(rows);
        row(SOURCE, "Loom", 64, 128, 168).item(vanilla("leather", 6)).item(form("plateGem", "Diamond", 8)).out(vanilla("diamond_horse_armor", 1)).add(rows);
        row(SOURCE, "Loom", 64, 128, 169).item(vanilla("leather", 6)).item(form("ring", "Steel", 2)).item(form("stick", "Steel", 3)).out(vanilla("saddle", 1)).add(rows);
        row(SOURCE, "Loom", 16, 128, 170).kept(selector(4)).item(vanilla("leather", 5)).out(vanilla("leather_helmet", 1)).add(rows);
        row(SOURCE, "Loom", 16, 128, 171).kept(selector(5)).item(vanilla("leather", 8)).out(vanilla("leather_chestplate", 1)).add(rows);
        row(SOURCE, "Loom", 16, 128, 172).kept(selector(6)).item(vanilla("leather", 7)).out(vanilla("leather_leggings", 1)).add(rows);
        row(SOURCE, "Loom", 16, 128, 173).kept(selector(7)).item(vanilla("leather", 4)).out(vanilla("leather_boots", 1)).add(rows);
        row(SOURCE, "Loom", 96, 128, 174).kept(selector(4)).item(form("ring", "Steel", 5)).out(vanilla("chainmail_helmet", 1)).add(rows);
        row(SOURCE, "Loom", 96, 128, 175).kept(selector(5)).item(form("ring", "Steel", 8)).out(vanilla("chainmail_chestplate", 1)).add(rows);
        row(SOURCE, "Loom", 96, 128, 176).kept(selector(6)).item(form("ring", "Steel", 7)).out(vanilla("chainmail_leggings", 1)).add(rows);
        row(SOURCE, "Loom", 96, 128, 177).kept(selector(7)).item(form("ring", "Steel", 4)).out(vanilla("chainmail_boots", 1)).add(rows);
        row(SOURCE, "RollBender", 16, 256, 178).item(form("plateCurved", "Fe", 3)).out(vanilla("bucket", 1)).add(rows);
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
        row(SOURCE, "Bath", 0, 128, 207).item(vanilla("golden_apple", 1)).fluid(liquid("Au", U(64), CEIL)).out(vanilla("enchanted_golden_apple", 1)).add(rows);
        row(SOURCE, "Bath", 0, 16, 208).item(vanilla("carrot", 1)).fluid(liquid("Au", U(8, 9), CEIL)).out(vanilla("golden_carrot", 1)).add(rows);
        row(SOURCE, "Bath", 0, 16, 209).item(vanilla("melon", 1)).fluid(liquid("Au", U(8, 9), CEIL)).out(vanilla("glistering_melon_slice", 1)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 210).item(vanilla("melon", 1)).item(form("nugget", "Au", 8)).out(vanilla("glistering_melon_slice", 1)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 212).item(dust("EnderPearl", thousandths(1000))).item(dust("Blaze", U(1, 9))).out(dust("EnderEye", thousandths(1000))).add(rows);
        row(SOURCE, "Mixer", 16, 144, 213).item(dust("EnderPearl", thousandths(9000))).item(dust("Blaze", thousandths(1000))).out(dust("EnderEye", thousandths(9000))).add(rows);
        row(SOURCE, "Mixer", 16, 16, 214).item(dust("Sugar", thousandths(1000))).item(vanilla("spider_eye", 1)).item(vanilla("brown_mushroom", 1)).out(vanilla("fermented_spider_eye", 1)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 215).item(form("gemChipped", "Sugar", 4)).item(vanilla("spider_eye", 1)).item(vanilla("brown_mushroom", 1)).out(vanilla("fermented_spider_eye", 1)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 216).item(dust("Coal", thousandths(1000))).item(dust("Blaze", U(1, 9))).item(dust("Gunpowder", thousandths(1000))).out(vanilla("fire_charge", 3)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 217).item(dust("Charcoal", thousandths(1000))).item(dust("Blaze", U(1, 9))).item(dust("Gunpowder", thousandths(1000))).out(vanilla("fire_charge", 3)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 218).item(dust("CoalCoke", thousandths(1000))).item(dust("Blaze", U(1, 9))).item(dust("Gunpowder", thousandths(1000))).out(vanilla("fire_charge", 3)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 219).item(dust("LigniteCoke", thousandths(1000))).item(dust("Blaze", U(1, 9))).item(dust("Gunpowder", thousandths(1000))).out(vanilla("fire_charge", 3)).add(rows);
        row(SOURCE, "Electrolyzer", 64, 576, 220).kept(selector(0)).item(vanilla("sand", 1)).out(dust("SiO2", thousandths(9000))).add(rows);
        row(SOURCE, "Electrolyzer", 64, 64, 221).kept(selector(0)).item(dust("Sand", thousandths(1000))).out(dust("SiO2", thousandths(1000))).add(rows);
        row(SOURCE, "Centrifuge", 16, 16, 222).item(vanilla("magma_cream", 1)).out(vanilla("blaze_powder", 1)).out(nativeFluid("Slime_Green", 125)).add(rows);
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
        for (String color : COLORS) {
            row(SOURCE + "115-130", "Bath", 0, 16)
                    .item(vanilla(color + "_terracotta", 1)).fluid(gas("Cl", thousandths(50), CEIL))
                    .out(vanilla("terracotta", 1)).add(rows);
            row(SOURCE + "131-146", "Bath", 0, 16)
                    .item(vanilla(color + "_stained_glass", 1)).fluid(gas("Cl", thousandths(50), CEIL))
                    .out(vanilla("glass", 1)).add(rows);
            row(SOURCE + "147-162", "Bath", 0, 16)
                    .item(vanilla(color + "_stained_glass_pane", 1)).fluid(gas("Cl", thousandths(20), CEIL))
                    .out(vanilla("glass_pane", 1)).add(rows);
        }
    }

    /** The two source water loops, with spectral dew recorded as an absence. */
    private static void waterRows(List<ChemRecipe> rows) {
        for (String water : WATERS) {
            row(SOURCE + "754/" + water, "Bath", 0, 16)
                    .item(vanilla("sugar_cane", 1)).fluid(nativeFluid(water, 250))
                    .out(vanilla("paper", 1)).add(rows);
            row(SOURCE + "892/" + water, "Mixer", 16, 16)
                    .fluid(nativeFluid(water, 50)).fluid(nativeFluid("Lava", 1000))
                    .out(vanilla("obsidian", 1)).add(rows);
        }
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
}
