package gregtech.content.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Clay mold recipes: the forming recipe, the {@code reclaim_*} recovery rows and the {@code imprint_*}
 * rows that copy a shape into a mold.
 *
 * <p>Imprints come in two shapes. A source that is not cast per material imprints from one item or tag
 * and needs a row of its own ({@link #SINGLE_IMPRINTS}); a source that is cast per material family
 * shares one template over the sixteen families in {@link #FAMILIES}, so the numeric id stays the same
 * and only the family suffix changes. The recovery rows follow {@link ClayMoldCatalog#RAW}, which keeps
 * the pack in step with the molds the mod actually registers.</p>
 *
 * <p>Ids keep their original numeric spelling because {@code OvenRecipeBridge} recognises the
 * {@code clay_molds/imprint_} prefix.</p>
 */
public final class ClayMoldRecipeRules {
    private ClayMoldRecipeRules() {}

    /** Recipe folder shared by every row. */
    public static final String DIRECTORY = "clay_molds";

    /** The empty mold every imprint starts from. */
    public static final String MOLD = "gt6ce:clay_mold";

    private static final String MOLD_SPEC = "item:" + MOLD;
    private static final String CLAY_BALL = "minecraft:clay_ball";
    private static final String TOOL_SHAPELESS = "gt6ce:tool_shapeless";
    private static final String VANILLA_SHAPELESS = "minecraft:crafting_shapeless";
    private static final String FORM_TAG = "gt6ce:molds/forms/";

    /** One ingredient of a shaped recipe: {@code item:<id>} or {@code tag:<id>}. */
    public record Key(char symbol, String spec) {}

    /**
     * One published recipe; {@code path} is relative to {@link #DIRECTORY}. Shaped rows carry
     * {@code pattern} and {@code key}, shapeless rows carry {@code ingredients}. {@code count} is
     * {@code null} where the original JSON leaves the result count implicit.
     */
    public record Recipe(
            String path,
            String type,
            Boolean allowMirror,
            List<String> pattern,
            List<Key> key,
            List<String> ingredients,
            String result,
            Integer count) {
        public boolean shaped() {
            return pattern != null;
        }
    }

    /**
     * The sixteen material families a numeric imprint id expands over, in registration order. Spellings
     * match {@link ClayMoldCatalog.Form}; ids and tags use the lower case form.
     */
    private static final List<String> FAMILIES = List.of(
            "AnyCu",
            "AnyIron",
            "AnyStone",
            "AnyWax",
            "AnyWoodPlastic",
            "ArsenicBronze",
            "ArsenicCopper",
            "Au",
            "Bi",
            "BismuthBronze",
            "Brass",
            "Bronze",
            "Glass",
            "Pb",
            "Sn",
            "Zn");

    /**
     * One imprint row per source item or tag, for shapes that are not cast per material. The empty mold
     * is implicit and only the sources are listed.
     */
    private record Single(int id, String result, List<String> ingredients) {}

    /**
     * One imprint row per family: the optional {@code extra} tag, then the family tag of each entry in
     * {@code forms}, so a repeated form prefix asks for two of the same family tag.
     */
    private record Family(int id, String result, String extra, List<String> forms) {}

    private static Single single(int id, String result, String... ingredients) {
        return new Single(id, result, List.of(ingredients));
    }

    private static Family family(int id, String result, String extra, String... forms) {
        return new Family(id, result, extra, List.of(forms));
    }

    /** The empty mold: clay balls around a rolling pin and a knife. */
    private static final Recipe FORMING = new Recipe(
            DIRECTORY + "/form_clay_mold",
            "gt6ce:tool_shaped",
            false,
            List.of("C C", "CCC", "k R"),
            List.of(
                    new Key('C', "tag:gt6ce:clay_balls"),
                    new Key('k', "tag:forge:tools/knife"),
                    new Key('R', "tag:forge:tools/rolling_pin")),
            null,
            MOLD,
            null);

    /** Shapes imprinted from one item or tag that stands for every material at once. */
    private static final List<Single> SINGLE_IMPRINTS = List.of(
            single(137, "gt6ce:clay_ingot_mold", "tag:" + FORM_TAG + "ingot/brick"),
            single(138, "gt6ce:clay_plate_mold", "tag:forge:glass_panes"),
            single(139, "gt6ce:clay_plate_mold", "tag:minecraft:planks"),
            single(140, "gt6ce:clay_arrow_mold", "item:minecraft:flint"),
            single(141, "gt6ce:clay_arrow_mold", "item:minecraft:arrow"),
            single(142, "gt6ce:clay_sword_mold", "item:minecraft:wooden_sword"),
            single(143, "gt6ce:clay_pickaxe_mold", "item:minecraft:wooden_pickaxe"),
            single(144, "gt6ce:clay_shovel_mold", "item:minecraft:wooden_shovel"),
            single(145, "gt6ce:clay_axe_mold", "item:minecraft:wooden_axe"),
            single(146, "gt6ce:clay_hoe_mold", "item:minecraft:wooden_hoe"),
            single(147, "gt6ce:clay_sword_mold", "item:minecraft:stone_sword"),
            single(148, "gt6ce:clay_pickaxe_mold", "item:minecraft:stone_pickaxe"),
            single(149, "gt6ce:clay_shovel_mold", "item:minecraft:stone_shovel"),
            single(150, "gt6ce:clay_axe_mold", "item:minecraft:stone_axe"),
            single(151, "gt6ce:clay_hoe_mold", "item:minecraft:stone_hoe"),
            single(152, "gt6ce:clay_file_mold", "tag:forge:glass_panes", "tag:forge:glass_panes"),
            single(153, "gt6ce:clay_file_mold", "tag:minecraft:planks", "tag:minecraft:planks"),
            single(154, "gt6ce:clay_file_mold", "item:gt6ce:tool_file"),
            single(155, "gt6ce:clay_sword_mold", "item:gt6ce:tool_sword"),
            single(156, "gt6ce:clay_pickaxe_mold", "item:gt6ce:tool_pickaxe"),
            single(157, "gt6ce:clay_pickaxe_mold", "item:gt6ce:tool_gem_tipped_pickaxe"),
            single(158, "gt6ce:clay_pickaxe_mold", "item:gt6ce:tool_construction_pick"),
            single(159, "gt6ce:clay_spade_mold", "item:gt6ce:tool_spade"),
            single(160, "gt6ce:clay_shovel_mold", "item:gt6ce:tool_shovel"),
            single(161, "gt6ce:clay_universal_spade_mold", "item:gt6ce:tool_universal_spade"),
            single(162, "gt6ce:clay_axe_mold", "item:gt6ce:tool_axe"),
            single(163, "gt6ce:clay_double_axe_mold", "item:gt6ce:tool_double_axe"),
            single(164, "gt6ce:clay_saw_mold", "item:gt6ce:tool_saw"),
            single(165, "gt6ce:clay_hammer_mold", "item:gt6ce:tool_soft_hammer"),
            single(166, "gt6ce:clay_hammer_mold", "item:gt6ce:tool_hammer"),
            single(167, "gt6ce:clay_screwdriver_mold", "item:gt6ce:tool_screwdriver"),
            single(168, "gt6ce:clay_chisel_mold", "item:gt6ce:tool_chisel"),
            single(169, "gt6ce:clay_hoe_mold", "item:gt6ce:tool_hoe"),
            single(170, "gt6ce:clay_sense_mold", "item:gt6ce:tool_sense"),
            single(171, "gt6ce:clay_plow_mold", "item:gt6ce:tool_plow"),
            single(172, "gt6ce:clay_builders_wand_mold", "item:gt6ce:tool_builder_wand"),
            single(175, "gt6ce:clay_sword_mold", "tag:" + FORM_TAG + "toolheadsword/all"),
            single(176, "gt6ce:clay_sword_mold", "tag:" + FORM_TAG + "toolheadrawsword/all"),
            single(177, "gt6ce:clay_pickaxe_mold", "tag:" + FORM_TAG + "toolheadpickaxe/all"),
            single(178, "gt6ce:clay_pickaxe_mold", "tag:" + FORM_TAG + "toolheadrawpickaxe/all"),
            single(179, "gt6ce:clay_spade_mold", "tag:" + FORM_TAG + "toolheadspade/all"),
            single(180, "gt6ce:clay_spade_mold", "tag:" + FORM_TAG + "toolheadrawspade/all"),
            single(181, "gt6ce:clay_shovel_mold", "tag:" + FORM_TAG + "toolheadshovel/all"),
            single(182, "gt6ce:clay_shovel_mold", "tag:" + FORM_TAG + "toolheadrawshovel/all"),
            single(183, "gt6ce:clay_universal_spade_mold", "tag:" + FORM_TAG + "toolheaduniversalspade/all"),
            single(184, "gt6ce:clay_universal_spade_mold", "tag:" + FORM_TAG + "toolheadrawuniversalspade/all"),
            single(185, "gt6ce:clay_axe_mold", "tag:" + FORM_TAG + "toolheadaxe/all"),
            single(186, "gt6ce:clay_axe_mold", "tag:" + FORM_TAG + "toolheadrawaxe/all"),
            single(
                    187,
                    "gt6ce:clay_double_axe_mold",
                    "tag:" + FORM_TAG + "toolheadaxe/all",
                    "tag:" + FORM_TAG + "toolheadaxe/all"),
            single(
                    188,
                    "gt6ce:clay_double_axe_mold",
                    "tag:" + FORM_TAG + "toolheadrawaxe/all",
                    "tag:" + FORM_TAG + "toolheadrawaxe/all"),
            single(189, "gt6ce:clay_double_axe_mold", "tag:" + FORM_TAG + "toolheadaxedouble/all"),
            single(190, "gt6ce:clay_double_axe_mold", "tag:" + FORM_TAG + "toolheadrawaxedouble/all"),
            single(191, "gt6ce:clay_saw_mold", "tag:" + FORM_TAG + "toolheadsaw/all"),
            single(192, "gt6ce:clay_saw_mold", "tag:" + FORM_TAG + "toolheadrawsaw/all"),
            single(193, "gt6ce:clay_hammer_mold", "tag:" + FORM_TAG + "toolheadhammer/all"),
            single(194, "gt6ce:clay_file_mold", "tag:" + FORM_TAG + "toolheadfile/all"),
            single(195, "gt6ce:clay_screwdriver_mold", "tag:" + FORM_TAG + "toolheadscrewdriver/all"),
            single(196, "gt6ce:clay_chisel_mold", "tag:" + FORM_TAG + "toolheadchisel/all"),
            single(197, "gt6ce:clay_chisel_mold", "tag:" + FORM_TAG + "toolheadrawchisel/all"),
            single(198, "gt6ce:clay_arrow_mold", "tag:" + FORM_TAG + "toolheadarrow/all"),
            single(199, "gt6ce:clay_arrow_mold", "tag:" + FORM_TAG + "toolheadrawarrow/all"),
            single(200, "gt6ce:clay_arrow_mold", "tag:" + FORM_TAG + "arrowgtwood/all"),
            single(201, "gt6ce:clay_arrow_mold", "tag:" + FORM_TAG + "arrowgtplastic/all"),
            single(202, "gt6ce:clay_hoe_mold", "tag:" + FORM_TAG + "toolheadhoe/all"),
            single(203, "gt6ce:clay_hoe_mold", "tag:" + FORM_TAG + "toolheadrawhoe/all"),
            single(204, "gt6ce:clay_sense_mold", "tag:" + FORM_TAG + "toolheadsense/all"),
            single(205, "gt6ce:clay_sense_mold", "tag:" + FORM_TAG + "toolheadrawsense/all"),
            single(206, "gt6ce:clay_plow_mold", "tag:" + FORM_TAG + "toolheadplow/all"),
            single(207, "gt6ce:clay_plow_mold", "tag:" + FORM_TAG + "toolheadrawplow/all"),
            single(208, "gt6ce:clay_builders_wand_mold", "tag:" + FORM_TAG + "toolheadbuilderwand/all"));

    /** Shapes imprinted from a form tag of every material family. */
    private static final List<Family> FAMILY_IMPRINTS = List.of(
            family(212, "gt6ce:clay_ingot_mold", null, "ingot"),
            family(213, "gt6ce:clay_billet_mold", null, "billet"),
            family(214, "gt6ce:clay_chunk_mold", null, "chunkGt"),
            family(215, "gt6ce:clay_nugget_mold", null, "nugget"),
            family(216, "gt6ce:clay_plate_mold", null, "plate"),
            family(217, "gt6ce:clay_plate_mold", null, "plateGem"),
            family(218, "gt6ce:clay_tiny_plate_mold", null, "plateTiny"),
            family(219, "gt6ce:clay_tiny_plate_mold", null, "plateGemTiny"),
            family(220, "gt6ce:clay_bolt_mold", null, "bolt"),
            family(221, "gt6ce:clay_rod_mold", null, "stick"),
            family(222, "gt6ce:clay_long_rod_mold", null, "stick", "stick"),
            family(223, "gt6ce:clay_long_rod_mold", null, "stickLong"),
            family(224, "gt6ce:clay_item_casing_mold", null, "casingSmall"),
            family(225, "gt6ce:clay_ring_mold", null, "ring"),
            family(226, "gt6ce:clay_gear_mold", null, "gearGt"),
            family(227, "gt6ce:clay_small_gear_mold", null, "gearGtSmall"),
            family(228, "gt6ce:clay_file_mold", null, "plate", "plate"),
            family(229, "gt6ce:clay_chisel_mold", "forge:glass_panes", "stick"),
            family(230, "gt6ce:clay_chisel_mold", "minecraft:planks", "stick"),
            family(231, "gt6ce:clay_chisel_mold", null, "plate", "stick"));

    /** All 422 rows: the forming recipe, both imprint halves and one recovery row per mold. */
    public static List<Recipe> recipes() {
        var recipes = new ArrayList<Recipe>(422);
        recipes.add(FORMING);
        for (var imprint : SINGLE_IMPRINTS) {
            recipes.add(shapeless(
                    "imprint_" + imprint.id() + "_all",
                    TOOL_SHAPELESS,
                    withMold(imprint.ingredients()),
                    imprint.result(),
                    1));
        }
        for (var imprint : FAMILY_IMPRINTS) {
            for (var family : FAMILIES) {
                String suffix = family.toLowerCase(Locale.ROOT);
                recipes.add(shapeless(
                        "imprint_" + imprint.id() + "_" + suffix,
                        TOOL_SHAPELESS,
                        familyIngredients(imprint, suffix),
                        imprint.result(),
                        1));
            }
        }
        for (var raw : ClayMoldCatalog.RAW) {
            recipes.add(shapeless(
                    "reclaim_" + raw.id(), VANILLA_SHAPELESS, List.of("item:gt6ce:" + raw.id()), CLAY_BALL, 5));
        }
        return List.copyOf(recipes);
    }

    /** The empty mold plus the fixed extra tag, then one form tag per {@code forms} entry. */
    private static List<String> familyIngredients(Family imprint, String family) {
        var sources = new ArrayList<String>(imprint.forms().size() + 1);
        if (imprint.extra() != null) sources.add("tag:" + imprint.extra());
        for (var form : imprint.forms()) {
            sources.add("tag:" + FORM_TAG + form.toLowerCase(Locale.ROOT) + "/" + family);
        }
        return withMold(sources);
    }

    /** The empty mold followed by {@code sources}. */
    private static List<String> withMold(List<String> sources) {
        var ingredients = new ArrayList<String>(sources.size() + 1);
        ingredients.add(MOLD_SPEC);
        ingredients.addAll(sources);
        return List.copyOf(ingredients);
    }

    private static Recipe shapeless(String path, String type, List<String> ingredients, String result, int count) {
        return new Recipe(DIRECTORY + "/" + path, type, null, null, null, ingredients, result, count);
    }
}
