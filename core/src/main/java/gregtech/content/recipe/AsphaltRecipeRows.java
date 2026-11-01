/* GregTech-6 Team, Loader_Recipes_Other:325-351; LGPL-3.0-or-later. */
package gregtech.content.recipe;

import java.util.*;

public final class AsphaltRecipeRows {
    private AsphaltRecipeRows() {}

    public static final Map<String, List<String>> STONES = Map.ofEntries(
            Map.entry(
                    "DyeGray",
                    List.of(
                            "Stone",
                            "Concrete",
                            "Talc",
                            "Rhyolite",
                            "Gneiss",
                            "Shale",
                            "Oilshale",
                            "Dolomite",
                            "Chert")),
            Map.entry("DyeBlack", List.of("Basalt", "Gabbro", "GraniteBlack", "Deepslate")),
            Map.entry("DyeLightBlue", List.of("Blueschist")),
            Map.entry("DyeOrange", List.of("Limestone")),
            Map.entry("DyeBrown", List.of("Kimberlite")));
}
