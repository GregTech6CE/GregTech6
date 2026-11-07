package gregtech.content.recipe;

import gregtech.content.recipe.generated.GeneratedRecipeSink;

/**
 * The air variants of the transpiled Roasting rows.
 *
 * <p>{@code Loader_Recipes_Chem:444-465} registers each sulfide row once per {@code FluidsGT.AIR}
 * entry, but the transpiled catalog could only resolve the plain {@code Air} iteration. Every row
 * here copies the shape of the oxygen row it parallels (same solids and SO2 amount) and swaps the
 * oxidiser for {@code Air}, {@code Air_End} or {@code Air_Nether} at the original air volume; the
 * solid outputs keep the original 8000/10000 probability.</p>
 */
public final class AirRoastingRows {
    private static final String[] AIRS = {"Air", "Air_End", "Air_Nether"};
    private static final long[] CHANCES = {8000, 8000, 8000};
    private static final String[] NO_ITEMS = new String[0];

    /** {@code ore}, air volume, SO2 volume, solid outputs ({@code Loader_Recipes_Chem:446-459}). */
    private record OreRow(String material, int air, int sulfurDioxide, String... solids) {}

    private static final OreRow[] ORE_ROWS = {
        new OreRow("Pyrite", 8000, 2000, "omdu:Fe2O3:540540000"),
        new OreRow("Realgar", 4000, 1500, "omdu:As:324324000"),
        new OreRow("Sphalerite", 4000, 1500, "omdu:Zn:324324000"),
        new OreRow("Molybdenite", 6000, 2000, "omdu:Mo:216216000"),
        new OreRow("Stibnite", 5000, 1800, "omdu:Sb:259459200"),
        new OreRow("Pentlandite", 4000, 1411, "omdu:Ni:343401876"),
        new OreRow("Chalcopyrite", 6000, 1500, "omdu:Fe2O3:405405000", "omdu:Cu:162162000"),
        new OreRow("Arsenopyrite", 5000, 1000, "omdu:Fe2O3:540540000", "omdu:As:216216000"),
        new OreRow("Cobaltite", 3000, 1000, "omdu:Co:216216000", "omdu:As:216216000"),
        new OreRow("Galena", 2000, 750, "omdu:Ag:243243000", "omdu:Pb:243243000"),
        new OreRow("Cooperite", 2000, 500, "omdu:Pt:324324000", "omdu:Ni:108108000", "omdu:Pd:108108000"),
        new OreRow("Tetrahedrite", 4000, 1125, "omdu:Cu:243243000", "omdu:Sb:81081000", "omdu:Fe2O3:202702500"),
        new OreRow("Stannite", 5000, 1500, "omdu:Cu:162162000", "omdu:Sn:81081000", "omdu:Fe2O3:202702500"),
        new OreRow("Kesterite", 4000, 1500, "omdu:Cu:162162000", "omdu:Zn:81081000", "omdu:Sn:81081000"),
    };

    /** {@code ore}, air volume, duration, SO2 volume ({@code Loader_Recipes_Chem:461-465}). */
    private record SulfurRow(String item, int air, int ticks, int sulfurDioxide) {}

    private static final SulfurRow[] SULFUR_ROWS = {
        new SulfurRow("i:dust:S:1", 8000, 16, 3000),
        new SulfurRow("i:blockDust:S:1", 72000, 144, 27000),
        new SulfurRow("i:dustTiny:Blaze:1", 8000, 16, 3000),
        new SulfurRow("i:dust:Blaze:1", 72000, 144, 27000),
        new SulfurRow("i:blockDust:Blaze:1", 648000, 1296, 243000),
    };

    private AirRoastingRows() {}

    public static void emit(GeneratedRecipeSink.SourceSink sink) {
        for (String air : AIRS) {
            for (OreRow row : ORE_ROWS) {
                sink.accept(
                        true,
                        "Roasting",
                        16,
                        512,
                        CHANCES,
                        new String[] {"omdu:" + row.material() + ":648648000"},
                        new String[] {"f:" + air + ":" + row.air()},
                        new String[] {"m:SO2:" + row.sulfurDioxide()},
                        row.solids());
            }
        }
        // The plain Air sulfur rows are already part of the transpiled Roasting rows; adding them
        // again would only collide. These copies also leave optimization off like theirs.
        for (String air : new String[] {"Air_End", "Air_Nether"}) {
            for (SulfurRow row : SULFUR_ROWS) {
                sink.accept(
                        false,
                        "Roasting",
                        16,
                        row.ticks(),
                        null,
                        new String[] {row.item()},
                        new String[] {"f:" + air + ":" + row.air()},
                        new String[] {"m:SO2:" + row.sulfurDioxide()},
                        NO_ITEMS);
            }
        }
    }
}
