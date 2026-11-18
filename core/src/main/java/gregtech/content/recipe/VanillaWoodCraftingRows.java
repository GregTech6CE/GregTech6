package gregtech.content.recipe;

import static gregtech.content.recipe.WoodCraftingRow.Source.woods;
import static gregtech.content.recipe.WoodCraftingRow.Tool.FILE;
import static gregtech.content.recipe.WoodCraftingRow.Tool.SAW;
import static gregtech.content.recipe.WoodCraftingRow.shaped;
import static gregtech.content.recipe.WoodCraftingRow.shapeless;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.recipe.WoodCraftingRow.Item;
import gregtech.content.recipe.WoodCraftingRow.Key;
import gregtech.content.recipe.WoodCraftingRow.MaterialForm;
import java.util.ArrayList;
import java.util.List;

/**
 * GT6 {@code Loader_Recipes_Woods} and applied to the vanilla woods the port
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

    private record Wood(
            String id,
            String material,
            String log,
            String wood,
            String strippedLog,
            String strippedWood,
            String planks) {}

    private record Form(String name, String item, boolean stripped) {}

    private static final List<Wood> WOODS = List.of(
            new Wood("oak", "Oak", "oak_log", "oak_wood", "stripped_oak_log", "stripped_oak_wood", "oak_planks"),
            new Wood(
                    "spruce",
                    "Spruce",
                    "spruce_log",
                    "spruce_wood",
                    "stripped_spruce_log",
                    "stripped_spruce_wood",
                    "spruce_planks"),
            new Wood(
                    "birch",
                    "Birch",
                    "birch_log",
                    "birch_wood",
                    "stripped_birch_log",
                    "stripped_birch_wood",
                    "birch_planks"),
            new Wood(
                    "jungle",
                    "Junglewood",
                    "jungle_log",
                    "jungle_wood",
                    "stripped_jungle_log",
                    "stripped_jungle_wood",
                    "jungle_planks"),
            new Wood(
                    "acacia",
                    "Acacia",
                    "acacia_log",
                    "acacia_wood",
                    "stripped_acacia_log",
                    "stripped_acacia_wood",
                    "acacia_planks"),
            new Wood(
                    "dark_oak",
                    "DarkOak",
                    "dark_oak_log",
                    "dark_oak_wood",
                    "stripped_dark_oak_log",
                    "stripped_dark_oak_wood",
                    "dark_oak_planks"),
            new Wood(
                    "mangrove",
                    "Mangrove",
                    "mangrove_log",
                    "mangrove_wood",
                    "stripped_mangrove_log",
                    "stripped_mangrove_wood",
                    "mangrove_planks"),
            new Wood(
                    "cherry",
                    "Cherrywood",
                    "cherry_log",
                    "cherry_wood",
                    "stripped_cherry_log",
                    "stripped_cherry_wood",
                    "cherry_planks"),
            new Wood(
                    "crimson",
                    "Crimsonwood",
                    "crimson_stem",
                    "crimson_hyphae",
                    "stripped_crimson_stem",
                    "stripped_crimson_hyphae",
                    "crimson_planks"),
            new Wood(
                    "warped",
                    "Warpedwood",
                    "warped_stem",
                    "warped_hyphae",
                    "stripped_warped_stem",
                    "stripped_warped_hyphae",
                    "warped_planks"));

    public static final List<WoodCraftingRow> ROWS = build();

    private static List<WoodCraftingRow> build() {
        List<WoodCraftingRow> rows = new ArrayList<>();
        for (Wood wood : WOODS) {
            var planks = new Item("minecraft:" + wood.planks());
            var longRod = new MaterialForm(MaterialPrefix.stickLong, wood.material());
            for (var form : List.of(
                    new Form("log", wood.log(), false),
                    new Form("wood", wood.wood(), false),
                    new Form("stripped_log", wood.strippedLog(), true),
                    new Form("stripped_wood", wood.strippedWood(), true))) {
                String path = wood.id() + "/" + form.name();
                var item = new Item("minecraft:" + form.item());
                var log = new Key('L', item);
                var saw = new Key('s', SAW);
                var file = new Key('f', FILE);
                boolean stripped = form.stripped();
                rows.add(shaped(
                        path + "_to_sticks",
                        woods(stripped ? 203 : 183),
                        longRod,
                        stripped ? 3 : 2,
                        List.of("sLf"),
                        saw,
                        log,
                        file));
                rows.add(shaped(
                        path + "_to_planks_saw",
                        woods(stripped ? 204 : 184),
                        planks,
                        stripped ? 5 : 4,
                        List.of("s", "L"),
                        saw,
                        log));
                rows.add(shapeless(
                        path + "_to_planks_hand", woods(stripped ? 205 : 185), planks, stripped ? 3 : 2, item));
            }
        }
        return List.copyOf(rows);
    }
}
