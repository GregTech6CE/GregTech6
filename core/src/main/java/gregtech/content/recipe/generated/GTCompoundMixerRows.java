package gregtech.content.recipe.generated;

/**
 * GT6 {@code Loader_Recipes_Other:71-100}: every alloy compound also mixes from the tiny,
 * nugget and scrap forms of either component, and the final nested loop pairs any two of
 * those forms. The transpiler only emits the plain dust and small-dust rows, so the
 * remaining prefix matrix is expressed here instead of copying 141 rows by hand.
 *
 * <p>Amounts follow the source literally: 27 small units equal one dust, 3:1 pairs keep
 * the source's 4/12/5 output counts, and durations are the per-row source ticks.</p>
 */
public final class GTCompoundMixerRows {
    private GTCompoundMixerRows() {}

    private static final String[] NONE = new String[0];
    private static final String[] SMALL_FORMS = {"dustTiny", "nugget", "scrapGt"};

    public static void load() {
        for (String a : SMALL_FORMS) {
            // 27 small units of the first component against one dust / four small dusts.
            row(64, a, "Cu", 27, "dust", "Sn", 1, "bronze_compound", 36);
            row(64, a, "Cu", 27, "dust", "Zn", 1, "brass_compound", 36);
            row(64, a, "Cu", 27, "dust", "As", 1, "arsenic_copper_compound", 36);
            row(80, a, "ArsenicCopper", 36, "dust", "Sn", 1, "arsenic_bronze_compound", 45);
            row(80, a, "Bronze", 36, "dust", "As", 1, "arsenic_bronze_compound", 45);
            row(80, a, "Brass", 36, "dust", "Bi", 1, "bismuth_bronze_compound", 45);

            row(64, a, "Cu", 27, "dustSmall", "Sn", 4, "bronze_compound", 36);
            row(64, a, "Cu", 27, "dustSmall", "Zn", 4, "brass_compound", 36);
            row(64, a, "Cu", 27, "dustSmall", "As", 4, "arsenic_copper_compound", 36);
            row(80, a, "ArsenicCopper", 36, "dustSmall", "Sn", 4, "arsenic_bronze_compound", 45);
            row(80, a, "Bronze", 36, "dustSmall", "As", 4, "arsenic_bronze_compound", 45);
            row(80, a, "Brass", 36, "dustSmall", "Bi", 4, "bismuth_bronze_compound", 45);

            // One dust / four small dusts of the first component against three small units.
            row(22, "dust", "Cu", 1, a, "Sn", 3, "bronze_compound", 12);
            row(22, "dust", "Cu", 1, a, "Zn", 3, "brass_compound", 12);
            row(22, "dust", "Cu", 1, a, "As", 3, "arsenic_copper_compound", 12);
            row(80, "dust", "ArsenicCopper", 4, a, "Sn", 9, "arsenic_bronze_compound", 45);
            row(80, "dust", "Bronze", 4, a, "As", 9, "arsenic_bronze_compound", 45);
            row(80, "dust", "Brass", 4, a, "Bi", 9, "bismuth_bronze_compound", 45);

            row(22, "dustSmall", "Cu", 4, a, "Sn", 3, "bronze_compound", 12);
            row(22, "dustSmall", "Cu", 4, a, "Zn", 3, "brass_compound", 12);
            row(22, "dustSmall", "Cu", 4, a, "As", 3, "arsenic_copper_compound", 12);
            row(80, "dustSmall", "ArsenicCopper", 16, a, "Sn", 9, "arsenic_bronze_compound", 45);
            row(80, "dustSmall", "Bronze", 16, a, "As", 9, "arsenic_bronze_compound", 45);
            row(80, "dustSmall", "Brass", 16, a, "Bi", 9, "bismuth_bronze_compound", 45);

            // Four mixed compound items plus one small component unit.
            row(10, "tech", "arsenic_copper_compound", 4, a, "Sn", 1, "arsenic_bronze_compound", 5);
            row(10, "tech", "bronze_compound", 4, a, "As", 1, "arsenic_bronze_compound", 5);
            row(10, "tech", "brass_compound", 4, a, "Bi", 1, "bismuth_bronze_compound", 5);

            for (String b : SMALL_FORMS) {
                row(8, a, "Cu", 3, b, "Sn", 1, "bronze_compound", 4);
                row(8, a, "Cu", 3, b, "Zn", 1, "brass_compound", 4);
                row(8, a, "Cu", 3, b, "As", 1, "arsenic_copper_compound", 4);
                row(10, a, "ArsenicCopper", 4, b, "Sn", 1, "arsenic_bronze_compound", 5);
                row(10, a, "Bronze", 4, b, "As", 1, "arsenic_bronze_compound", 5);
                row(10, a, "Brass", 4, b, "Bi", 1, "bismuth_bronze_compound", 5);
            }
        }
    }

    private static void row(long ticks, String prefixA, String materialA, int countA,
                            String prefixB, String materialB, int countB, String output, int outputCount) {
        GeneratedRecipeSink.register("Mixer", 16, ticks, null,
                new String[]{spec(prefixA, materialA, countA), spec(prefixB, materialB, countB)},
                NONE, NONE, new String[]{"tech:" + output + ":" + outputCount});
    }

    private static String spec(String prefix, String material, int count) {
        return prefix.equals("tech")
                ? "tech:" + material + ":" + count
                : "i:" + prefix + ":" + material + ":" + count;
    }
}
