package gregtech.content.recipe.generated;

import static gregtech.content.recipe.generated.GeneratedRows.*;

import gregtech.engine.recipe.chem.ChemIngredient;
import gregtech.engine.recipe.chem.ChemRecipe;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code Loader_Recipes_Extruder}: stone, basalt and blackstone shapes, extruded either from the
 * generator module or from the vanilla block.
 *
 * <p>The source repeats one call per module, material and shape; those loops are expanded here.
 * The vanilla stone metadata variants (granite, diorite, andesite) are excluded: GT6 treated them
 * as ordinary stone, while the port registers explicit native rows for their distinct materials.
 * The source's Netherlicious/Nedit blackstone and basalt brick alternatives are other mods'
 * items and are excluded as well.</p>
 */
public final class GTExtruderRecipes {
    private static final String SOURCE = "gt6@3703e403/Loader_Recipes_Extruder:";
    private static final String[] FAMILIES = {"extruder_shape_", "low_heat_extruder_shape_"};

    private record Shape(String tool, String output, int ticks) {}

    private static final Shape[] MODULE_SHAPES = {
        new Shape("plate", "plate", 64),
        new Shape("curvedplate", "plateCurved", 64),
        new Shape("rod", "stick", 32),
        new Shape("longrod", "stickLong", 64),
        new Shape("bolt", "bolt", 16),
        new Shape("shovelhead", "toolHeadRawShovel", 64),
        new Shape("swordblade", "toolHeadRawSword", 128),
        new Shape("hoehead", "toolHeadRawHoe", 128),
        new Shape("pickaxehead", "toolHeadRawPickaxe", 192),
        new Shape("axehead", "toolHeadRawAxe", 192),
        new Shape("gear", "gearGt", 256),
        new Shape("smallgear", "gearGtSmall", 64),
        new Shape("hammerhead", "toolHeadHammer", 384),
    };

    /** Vanilla-block shapes: block count, ticks and output amount differ from module rows. */
    private record BlockShape(String tool, String prefix, String vanillaId, int ticks, int blocks, int amount) {}

    private static final BlockShape[] BLOCK_SHAPES = {
        new BlockShape("plate", "plate", null, 32, 1, 1),
        new BlockShape("curvedplate", "plateCurved", null, 32, 1, 1),
        new BlockShape("rod", "stick", null, 32, 1, 2),
        new BlockShape("longrod", "stickLong", null, 32, 1, 1),
        new BlockShape("bolt", "bolt", null, 8, 1, 8),
        new BlockShape("ingot", null, "stone_bricks", 32, 1, 1),
        new BlockShape("block", null, "stone", 32, 1, 1),
        new BlockShape("shovelhead", "toolHeadRawShovel", null, 32, 1, 1),
        new BlockShape("swordblade", "toolHeadRawSword", null, 64, 2, 1),
        new BlockShape("hoehead", "toolHeadRawHoe", null, 64, 2, 1),
        new BlockShape("pickaxehead", "toolHeadRawPickaxe", null, 96, 3, 1),
        new BlockShape("axehead", "toolHeadRawAxe", null, 96, 3, 1),
        new BlockShape("gear", "gearGt", null, 128, 4, 1),
        new BlockShape("smallgear", "gearGtSmall", null, 32, 1, 1),
        new BlockShape("hammerhead", "toolHeadHammer", null, 192, 6, 1),
    };

    private GTExtruderRecipes() {}

    public static void register() {
        List<ChemRecipe> rows = new ArrayList<>();
        moduleRows(rows);
        vanillaBlockRows(rows, "stone");
        vanillaBlockRows(rows, "cobblestone");
        ChemRecipeResolver.exclude(SOURCE + "43-51/68-76",
                "blackstone and basalt ingot/block shapes only produce another mod's brick forms");
        ChemRecipeResolver.exclude(SOURCE + "100-317",
                "granite, diorite and andesite are distinct materials in this port; its native stone rows own them");
        publish("extruder recipes", rows);
    }

    private static void moduleRows(List<ChemRecipe> rows) {
        for (String family : FAMILIES) {
            for (String[] module : new String[][] {
                    {"module_blackstone_generator", "Blackstone"},
                    {"module_basalt_generator", "Basalt"},
                    {"module_stone_generator", "Stone"}}) {
                for (Shape shape : MODULE_SHAPES)
                    row(SOURCE + module[1] + "/" + family + shape.tool, "Extruder", 16, shape.ticks)
                            .kept(tech(module[0], 1)).kept(tech(family + shape.tool, 1))
                            .out(form(shape.output, module[1], 1)).add(rows);
                if (!module[1].equals("Stone")) continue;
                // The stone generator turns its block into vanilla stone bricks and stone.
                row(SOURCE + "Stone/" + family + "ingot", "Extruder", 16, 64)
                        .kept(tech(module[0], 1)).kept(tech(family + "ingot", 1))
                        .out(vanilla("stone_bricks", 1)).add(rows);
                row(SOURCE + "Stone/" + family + "block", "Extruder", 16, 64)
                        .kept(tech(module[0], 1)).kept(tech(family + "block", 1))
                        .out(vanilla("stone", 1)).add(rows);
            }
        }
    }

    private static void vanillaBlockRows(List<ChemRecipe> rows, String block) {
        for (String family : FAMILIES) {
            for (BlockShape shape : BLOCK_SHAPES) {
                ChemIngredient output = shape.prefix() == null
                        ? vanilla(shape.vanillaId(), shape.amount())
                        : form(shape.prefix(), "Stone", shape.amount());
                row(SOURCE + block + "/" + family + shape.tool, "Extruder", 16, shape.ticks())
                        .item(vanilla(block, shape.blocks())).kept(tech(family + shape.tool, 1))
                        .out(output).add(rows);
            }
        }
    }
}
