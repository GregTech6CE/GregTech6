package gregtech.content.recipe;

import static gregtech.content.recipe.WoodCraftingRow.Source.woods;
import static gregtech.content.recipe.WoodCraftingRow.Tool.SAW;
import static gregtech.content.recipe.WoodCraftingRow.shaped;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.recipe.WoodCraftingRow.Key;
import gregtech.content.recipe.WoodCraftingRow.MaterialForm;
import gregtech.content.recipe.WoodCraftingRow.Source;
import gregtech.content.recipe.WoodCraftingRow.Tag;
import java.util.List;

/** Original wood bolt and treated-wood gear patterns. */
public final class TreatedWoodCraftingRows {
    private TreatedWoodCraftingRows() {}

    private static final Key SAW_KEY = new Key('s', SAW);
    private static final Key PLANK = new Key('P', new MaterialForm(MaterialPrefix.plate, "WoodTreated"));

    public static final List<WoodCraftingRow> ROWS = List.of(
            shaped(
                    "treated/wood_bolt",
                    new Source("Loader_OreProcessing.java", 194),
                    new MaterialForm(MaterialPrefix.bolt, "Wood"),
                    2,
                    List.of("s ", " S"),
                    SAW_KEY,
                    new Key('S', new Tag("gt6ce:ammunition/sticks_wood"))),
            shaped(
                    "treated/small_gear",
                    woods(217),
                    new MaterialForm(MaterialPrefix.gearGtSmall, "WoodTreated"),
                    1,
                    List.of("P ", " s"),
                    PLANK,
                    SAW_KEY),
            shaped(
                    "treated/gear",
                    woods(218),
                    new MaterialForm(MaterialPrefix.gearGt, "WoodTreated"),
                    1,
                    List.of("BPB", "PsP", "BPB"),
                    new Key('B', new MaterialForm(MaterialPrefix.bolt, "WoodTreated")),
                    PLANK,
                    SAW_KEY));
}
