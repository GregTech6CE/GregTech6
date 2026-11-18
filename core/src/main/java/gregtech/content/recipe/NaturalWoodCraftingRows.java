package gregtech.content.recipe;

import static gregtech.content.recipe.WoodCraftingRow.Source.woods;
import static gregtech.content.recipe.WoodCraftingRow.Tool.SAW;
import static gregtech.content.recipe.WoodCraftingRow.shaped;
import static gregtech.content.recipe.WoodCraftingRow.shapeless;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.recipe.WoodCraftingRow.Item;
import gregtech.content.recipe.WoodCraftingRow.Key;
import gregtech.content.recipe.WoodCraftingRow.MaterialForm;
import java.util.ArrayList;
import java.util.List;

/** LoaderWoodDictionary / Loader_Recipes_Woods.
 * These logs yield 1 plank by hand, 2 by saw and 3 by machine; their log entries
 * explicitly have no stick output. Plank entries still have their normal stick forms.
 */
public final class NaturalWoodCraftingRows {
    private NaturalWoodCraftingRows() {}

    public record Spec(String kind, String material, String barkMaterial) {}

    public static final List<Spec> SPECIES = List.of(
            new Spec("dry", "WoodDead", "Bark"), new Spec("rotten", "WoodRotten", "WoodRotten"),
            new Spec("mossy", "WoodMossy", "WoodMossy"), new Spec("frozen", "WoodFrozen", "Ice"));
    public static final List<WoodCraftingRow> ROWS = rows();

    private static List<WoodCraftingRow> rows() {
        List<WoodCraftingRow> result = new ArrayList<>();
        for (Spec wood : SPECIES) {
            String path = "natural/" + wood.kind();
            var log = new Item("gt6ce:log_" + wood.kind());
            var plank = new Item("gt6ce:planks_" + wood.kind());
            var stick = new MaterialForm(MaterialPrefix.stick, wood.material());
            var saw = new Key('s', SAW);
            var plankKey = new Key('P', plank);
            result.add(shapeless(path + "/log_to_planks_hand", woods(185), plank, 1, log));
            result.add(shaped(
                    path + "/log_to_planks_saw", woods(184), plank, 2, List.of("s", "L"), saw, new Key('L', log)));
            result.add(shaped(path + "/planks_to_sticks_hand", woods(243), stick, 2, List.of("P", "P"), plankKey));
            result.add(shaped(path + "/planks_to_sticks_saw", woods(244), stick, 2, List.of("s", "P"), saw, plankKey));
        }
        return List.copyOf(result);
    }
}
