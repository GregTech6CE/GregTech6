package gregtech.content.recipe;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6 {@code Loader_Recipes_Woods:183-185} and {@code :203-205} applied to the vanilla woods the port
 * already processes in machines ({@code VanillaWoodProcessingRecipes.WOODS} plus the two nether stem
 * woods). Those entries used GT6's own beam blocks; in the port the beam identity is the modern
 * stripped log.
 *
 * <p>The vanilla {@code <wood>_planks} recipes that consumed the whole log family are disabled by
 * {@code VanillaCraftingRecipePack}; these rows replace them for all four forms the vanilla recipe
 * accepted: raw log/stem, raw wood/hyphae, stripped log/stem and stripped wood/hyphae. Raw forms keep
 * GT6's NERFED_WOOD 2/4 hand/saw counts; stripped forms use the old beam 3/5 counts.</p>
 */
public final class VanillaWoodCraftingRows {
    private VanillaWoodCraftingRows() {}

    public static final String[] ROWS = build();

    private static String[] build() {
        // id, material, raw log/stem, raw wood/hyphae, stripped log/stem, stripped wood/hyphae, planks
        String[][] woods = {
                {"oak", "Oak", "oak_log", "oak_wood", "stripped_oak_log", "stripped_oak_wood", "oak_planks"},
                {"spruce", "Spruce", "spruce_log", "spruce_wood", "stripped_spruce_log", "stripped_spruce_wood", "spruce_planks"},
                {"birch", "Birch", "birch_log", "birch_wood", "stripped_birch_log", "stripped_birch_wood", "birch_planks"},
                {"jungle", "Junglewood", "jungle_log", "jungle_wood", "stripped_jungle_log", "stripped_jungle_wood", "jungle_planks"},
                {"acacia", "Acacia", "acacia_log", "acacia_wood", "stripped_acacia_log", "stripped_acacia_wood", "acacia_planks"},
                {"dark_oak", "DarkOak", "dark_oak_log", "dark_oak_wood", "stripped_dark_oak_log", "stripped_dark_oak_wood", "dark_oak_planks"},
                {"mangrove", "Mangrove", "mangrove_log", "mangrove_wood", "stripped_mangrove_log", "stripped_mangrove_wood", "mangrove_planks"},
                {"cherry", "Cherrywood", "cherry_log", "cherry_wood", "stripped_cherry_log", "stripped_cherry_wood", "cherry_planks"},
                {"crimson", "Crimsonwood", "crimson_stem", "crimson_hyphae", "stripped_crimson_stem", "stripped_crimson_hyphae", "crimson_planks"},
                {"warped", "Warpedwood", "warped_stem", "warped_hyphae", "stripped_warped_stem", "stripped_warped_hyphae", "warped_planks"},
        };
        List<String> rows = new ArrayList<>();
        for (String[] wood : woods) {
            String id = wood[0];
            String material = wood[1];
            String planks = "item:minecraft:" + wood[6];
            String[] names = {"log", "wood", "stripped_log", "stripped_wood"};
            for (int form = 0; form < names.length; form++) {
                String item = "item:minecraft:" + wood[2 + form];
                boolean stripped = form >= 2;
                int sticks = stripped ? 3 : 2;
                int saw = stripped ? 5 : 4;
                int hand = stripped ? 3 : 2;
                String source = "Loader_Recipes_Woods.java:" + (stripped ? "203" : "183");
                String sourceSaw = "Loader_Recipes_Woods.java:" + (stripped ? "204" : "184");
                String sourceHand = "Loader_Recipes_Woods.java:" + (stripped ? "205" : "185");
                rows.add(id + "/" + names[form] + "_to_sticks|shaped|nomirror|sLf|s=tool:saw;L=" + item
                        + ";f=tool:file|rodlong:" + material + "*" + sticks + "|" + source);
                rows.add(id + "/" + names[form] + "_to_planks_saw|shaped|nomirror|s/L|s=tool:saw;L=" + item
                        + "|" + planks + "*" + saw + "|" + sourceSaw);
                rows.add(id + "/" + names[form] + "_to_planks_hand|shapeless|nomirror||L=" + item
                        + "|" + planks + "*" + hand + "|" + sourceHand);
            }
        }
        return rows.toArray(String[]::new);
    }
}
