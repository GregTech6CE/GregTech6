package com.gregtech.gregtech.content.recipe;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.List;
/** Original BatteryCellRecipes parameters; native stack/registry construction stays in the platforms. */
public final class BatteryCellRecipeRows {
    private BatteryCellRecipeRows(){}
    public record Fill(String empty, String filled, String fluid, int mb, String source) {}

    public static final List<Fill> FILLS = List.of(
            new Fill("lead_acid_cell_empty", "lead_acid_cell_filled",
                    "SulfuricAcid", 2000, "MultiItemTechnological:462"),
            new Fill("alkaline_button_cell_empty", "alkaline_button_cell_filled",
                    "WaterDistilled", 1000, "MultiItemTechnological:467"),
            new Fill("nickel_cadmium_cell_empty", "nickel_cadmium_cell_filled",
                    "WaterDistilled", 1000, "MultiItemTechnological:472"),
            new Fill("lithium_cobalt_cell_empty", "lithium_cobalt_cell_filled",
                    "HydrochloricAcid", 2000, "MultiItemTechnological:477"),
            new Fill("lithium_manganese_cell_empty", "lithium_manganese_cell_filled",
                    "HydrogenFluoride", 2000, "MultiItemTechnological:482"));
}
