package com.gregtech.gregtech.content.recipe;

import java.util.ArrayList;
import java.util.List;

/** LoaderWoodDictionary:165-171 / Loader_Recipes_Woods:184-185,243-244.
 * These logs yield 1 plank by hand, 2 by saw and 3 by machine; their log entries
 * explicitly have no stick output. Plank entries still have their normal stick forms.
 */
public final class NaturalWoodCraftingRows {
    private NaturalWoodCraftingRows() {}
    public record Spec(String kind, String material, String barkMaterial) {}
    public static final List<Spec> SPECIES = List.of(
            new Spec("dry", "WoodDead", "Bark"), new Spec("rotten", "WoodRotten", "WoodRotten"),
            new Spec("mossy", "WoodMossy", "WoodMossy"), new Spec("frozen", "WoodFrozen", "Ice"));
    public static final List<String> ROWS = rows();
    private static List<String> rows() {
        List<String> result = new ArrayList<>();
        for (Spec wood : SPECIES) {
            String path = "natural/" + wood.kind(), log = "item:gregtech:log_" + wood.kind();
            String plank = "item:gregtech:planks_" + wood.kind();
            result.add(path + "/log_to_planks_hand|shapeless|nomirror||L=" + log + "|" + plank + "*1|Loader_Recipes_Woods.java:185");
            result.add(path + "/log_to_planks_saw|shaped|nomirror|s/L|s=tool:saw;L=" + log + "|" + plank + "*2|Loader_Recipes_Woods.java:184");
            result.add(path + "/planks_to_sticks_hand|shaped|nomirror|P/P|P=" + plank + "|rod:" + wood.material() + "*2|Loader_Recipes_Woods.java:243");
            result.add(path + "/planks_to_sticks_saw|shaped|nomirror|s/P|s=tool:saw;P=" + plank + "|rod:" + wood.material() + "*2|Loader_Recipes_Woods.java:244");
        }
        return List.copyOf(result);
    }
}
