package gregtech.content.recipe;

import gregtech.content.recipe.generated.GeneratedRecipeSink;

/** Sulfide air-roasting uses the original probabilistic solids and exact material fractions. */
public final class AirRoastingRows {
    private AirRoastingRows() {}

    public static void emit(GeneratedRecipeSink.SourceSink sink) {
        for (String air : new String[] {"Air", "Air_End", "Air_Nether"}) {
            sink.accept(
                    true,
                    "Roasting",
                    16,
                    512,
                    new long[] {8000},
                    new String[] {"omdu:Pyrite:648648000"},
                    new String[] {"f:" + air + ":8000"},
                    new String[] {"mu:SO2:gas:1297296000:down"},
                    new String[] {"omdu:Fe2O3:540540000"});
            sink.accept(
                    true,
                    "Roasting",
                    16,
                    512,
                    new long[] {8000},
                    new String[] {"omdu:Realgar:648648000"},
                    new String[] {"f:" + air + ":4000"},
                    new String[] {"mu:SO2:gas:972972000:down"},
                    new String[] {"omdu:As:324324000"});
            sink.accept(
                    true,
                    "Roasting",
                    16,
                    512,
                    new long[] {8000},
                    new String[] {"omdu:Sphalerite:648648000"},
                    new String[] {"f:" + air + ":4000"},
                    new String[] {"mu:SO2:gas:972972000:down"},
                    new String[] {"omdu:Zn:324324000"});
            sink.accept(
                    true,
                    "Roasting",
                    16,
                    512,
                    new long[] {8000},
                    new String[] {"omdu:Molybdenite:648648000"},
                    new String[] {"f:" + air + ":6000"},
                    new String[] {"mu:SO2:gas:1297296000:down"},
                    new String[] {"omdu:Mo:216216000"});
            sink.accept(
                    true,
                    "Roasting",
                    16,
                    512,
                    new long[] {8000},
                    new String[] {"omdu:Stibnite:648648000"},
                    new String[] {"f:" + air + ":5000"},
                    new String[] {"mu:SO2:gas:1167566400:down"},
                    new String[] {"omdu:Sb:259459200"});
            sink.accept(
                    true,
                    "Roasting",
                    16,
                    512,
                    new long[] {8000},
                    new String[] {"omdu:Pentlandite:648648000"},
                    new String[] {"f:" + air + ":4000"},
                    new String[] {"mu:SO2:gas:915738336:down"},
                    new String[] {"omdu:Ni:343401876"});
            sink.accept(
                    true,
                    "Roasting",
                    16,
                    512,
                    new long[] {8000, 8000},
                    new String[] {"omdu:Chalcopyrite:648648000"},
                    new String[] {"f:" + air + ":6000"},
                    new String[] {"mu:SO2:gas:972972000:down"},
                    new String[] {"omdu:Fe2O3:405405000", "omdu:Cu:162162000"});
            sink.accept(
                    true,
                    "Roasting",
                    16,
                    512,
                    new long[] {8000, 8000},
                    new String[] {"omdu:Arsenopyrite:648648000"},
                    new String[] {"f:" + air + ":5000"},
                    new String[] {"mu:SO2:gas:648648000:down"},
                    new String[] {"omdu:Fe2O3:540540000", "omdu:As:216216000"});
            sink.accept(
                    true,
                    "Roasting",
                    16,
                    512,
                    new long[] {8000, 8000},
                    new String[] {"omdu:Cobaltite:648648000"},
                    new String[] {"f:" + air + ":3000"},
                    new String[] {"mu:SO2:gas:648648000:down"},
                    new String[] {"omdu:Co:216216000", "omdu:As:216216000"});
            sink.accept(
                    true,
                    "Roasting",
                    16,
                    512,
                    new long[] {8000, 8000},
                    new String[] {"omdu:Galena:648648000"},
                    new String[] {"f:" + air + ":2000"},
                    new String[] {"mu:SO2:gas:486486000:down"},
                    new String[] {"omdu:Ag:243243000", "omdu:Pb:243243000"});
            sink.accept(
                    true,
                    "Roasting",
                    16,
                    512,
                    new long[] {8000, 8000, 8000},
                    new String[] {"omdu:Cooperite:648648000"},
                    new String[] {"f:" + air + ":2000"},
                    new String[] {"mu:SO2:gas:324324000:down"},
                    new String[] {"omdu:Pt:324324000", "omdu:Ni:108108000", "omdu:Pd:108108000"});
            sink.accept(
                    true,
                    "Roasting",
                    16,
                    512,
                    new long[] {8000, 8000, 8000},
                    new String[] {"omdu:Tetrahedrite:648648000"},
                    new String[] {"f:" + air + ":4000"},
                    new String[] {"mu:SO2:gas:729729000:down"},
                    new String[] {"omdu:Cu:243243000", "omdu:Sb:81081000", "omdu:Fe2O3:202702500"});
            sink.accept(
                    true,
                    "Roasting",
                    16,
                    512,
                    new long[] {8000, 8000, 8000},
                    new String[] {"omdu:Stannite:648648000"},
                    new String[] {"f:" + air + ":5000"},
                    new String[] {"mu:SO2:gas:972972000:down"},
                    new String[] {"omdu:Cu:162162000", "omdu:Sn:81081000", "omdu:Fe2O3:202702500"});
            sink.accept(
                    true,
                    "Roasting",
                    16,
                    512,
                    new long[] {8000, 8000, 8000},
                    new String[] {"omdu:Kesterite:648648000"},
                    new String[] {"f:" + air + ":4000"},
                    new String[] {"mu:SO2:gas:972972000:down"},
                    new String[] {"omdu:Cu:162162000", "omdu:Zn:81081000", "omdu:Sn:81081000"});
        }
    }
}
