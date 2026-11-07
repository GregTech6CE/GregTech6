package gregtech.content.recipe.generated;

import static gregtech.content.recipe.generated.GeneratedRows.*;

import gregtech.engine.recipe.chem.ChemRecipe;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code Loader_Recipes_Other:71-100}: every alloy compound also mixes from the tiny, nugget and
 * scrap forms of either component, and the final nested loop pairs any two of those forms.
 *
 * <p>Amounts follow the source literally: 27 small units equal one dust, 3:1 pairs keep the
 * source's 4/12/5 output counts, and durations are the per-row source ticks.</p>
 */
public final class GTCompoundMixerRecipes {
    private static final String SOURCE = "gt6@3703e403/Loader_Recipes_Other:71-100:";
    private static final String[] SMALL_FORMS = {"dustTiny", "nugget", "scrapGt"};

    private GTCompoundMixerRecipes() {}

    public static void register() {
        List<ChemRecipe> rows = new ArrayList<>();
        for (String a : SMALL_FORMS) {
            // 27 small units of the first component against one dust / four small dusts.
            compound(64, a, "Cu", 27, "dust", "Sn", 1, "bronze_compound", 36).add(rows);
            compound(64, a, "Cu", 27, "dust", "Zn", 1, "brass_compound", 36).add(rows);
            compound(64, a, "Cu", 27, "dust", "As", 1, "arsenic_copper_compound", 36).add(rows);
            compound(80, a, "ArsenicCopper", 36, "dust", "Sn", 1, "arsenic_bronze_compound", 45).add(rows);
            compound(80, a, "Bronze", 36, "dust", "As", 1, "arsenic_bronze_compound", 45).add(rows);
            compound(80, a, "Brass", 36, "dust", "Bi", 1, "bismuth_bronze_compound", 45).add(rows);

            compound(64, a, "Cu", 27, "dustSmall", "Sn", 4, "bronze_compound", 36).add(rows);
            compound(64, a, "Cu", 27, "dustSmall", "Zn", 4, "brass_compound", 36).add(rows);
            compound(64, a, "Cu", 27, "dustSmall", "As", 4, "arsenic_copper_compound", 36).add(rows);
            compound(80, a, "ArsenicCopper", 36, "dustSmall", "Sn", 4, "arsenic_bronze_compound", 45).add(rows);
            compound(80, a, "Bronze", 36, "dustSmall", "As", 4, "arsenic_bronze_compound", 45).add(rows);
            compound(80, a, "Brass", 36, "dustSmall", "Bi", 4, "bismuth_bronze_compound", 45).add(rows);

            // One dust / four small dusts of the first component against three small units.
            compound(22, "dust", "Cu", 1, a, "Sn", 3, "bronze_compound", 12).add(rows);
            compound(22, "dust", "Cu", 1, a, "Zn", 3, "brass_compound", 12).add(rows);
            compound(22, "dust", "Cu", 1, a, "As", 3, "arsenic_copper_compound", 12).add(rows);
            compound(80, "dust", "ArsenicCopper", 4, a, "Sn", 9, "arsenic_bronze_compound", 45).add(rows);
            compound(80, "dust", "Bronze", 4, a, "As", 9, "arsenic_bronze_compound", 45).add(rows);
            compound(80, "dust", "Brass", 4, a, "Bi", 9, "bismuth_bronze_compound", 45).add(rows);

            compound(22, "dustSmall", "Cu", 4, a, "Sn", 3, "bronze_compound", 12).add(rows);
            compound(22, "dustSmall", "Cu", 4, a, "Zn", 3, "brass_compound", 12).add(rows);
            compound(22, "dustSmall", "Cu", 4, a, "As", 3, "arsenic_copper_compound", 12).add(rows);
            compound(80, "dustSmall", "ArsenicCopper", 16, a, "Sn", 9, "arsenic_bronze_compound", 45).add(rows);
            compound(80, "dustSmall", "Bronze", 16, a, "As", 9, "arsenic_bronze_compound", 45).add(rows);
            compound(80, "dustSmall", "Brass", 16, a, "Bi", 9, "bismuth_bronze_compound", 45).add(rows);

            // Four mixed compound items plus one small component unit.
            compound(10, "tech", "arsenic_copper_compound", 4, a, "Sn", 1, "arsenic_bronze_compound", 5).add(rows);
            compound(10, "tech", "bronze_compound", 4, a, "As", 1, "arsenic_bronze_compound", 5).add(rows);
            compound(10, "tech", "brass_compound", 4, a, "Bi", 1, "bismuth_bronze_compound", 5).add(rows);

            for (String b : SMALL_FORMS) {
                compound(8, a, "Cu", 3, b, "Sn", 1, "bronze_compound", 4).add(rows);
                compound(8, a, "Cu", 3, b, "Zn", 1, "brass_compound", 4).add(rows);
                compound(8, a, "Cu", 3, b, "As", 1, "arsenic_copper_compound", 4).add(rows);
                compound(10, a, "ArsenicCopper", 4, b, "Sn", 1, "arsenic_bronze_compound", 5).add(rows);
                compound(10, a, "Bronze", 4, b, "As", 1, "arsenic_bronze_compound", 5).add(rows);
                compound(10, a, "Brass", 4, b, "Bi", 1, "bismuth_bronze_compound", 5).add(rows);
            }
        }
        publish("compound mixer recipes", rows);
    }

    private static Row compound(long ticks, String prefixA, String materialA, int countA,
                           String prefixB, String materialB, int countB, String output, int outputCount) {
        return row(SOURCE + prefixA + "+" + prefixB, "Mixer", 16, ticks)
                .item(ingredient(prefixA, materialA, countA))
                .item(ingredient(prefixB, materialB, countB))
                .out(tech(output, outputCount));
    }

    private static gregtech.engine.recipe.chem.ChemIngredient ingredient(
            String prefix, String material, int count) {
        return "tech".equals(prefix) ? tech(material, count) : form(prefix, material, count);
    }
}
