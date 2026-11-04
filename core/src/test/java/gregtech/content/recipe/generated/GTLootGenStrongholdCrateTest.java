package gregtech.content.recipe.generated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GTLootGenStrongholdCrateTest {
    private static final String TABLE = "van.STRONGHOLD_CROSSING";
    private static final String CRATE = "tech:material_crate:1";

    @Test
    void strongholdCrossingUsesTwoMergedMaterialCrateRows() {
        List<String> crates = new ArrayList<>();
        for (String row : GTLootGen.rows()) {
            String[] parts = row.split("\\|");
            if (!parts[0].equals(TABLE) || !parts[4].equals(CRATE)) continue;
            crates.add(parts[1] + "|" + parts[2] + "|" + parts[3]);
        }
        assertEquals(2, crates.size());
        assertEquals(1L, count(crates, "18|4|8"));
        assertEquals(1L, count(crates, "24|8|16"));
        assertEquals(42, crates.stream().mapToInt(spec -> Integer.parseInt(spec.split("\\|")[0])).sum());
    }

    @Test
    void strongholdCrossingHasNoMaterialSpecificCrateRowsLeft() {
        for (String row : GTLootGen.rows()) {
            String[] parts = row.split("\\|");
            if (!parts[0].equals(TABLE)) continue;
            assertFalse(parts[4].contains("crateGt") || parts[4].contains("crate_gt"),
                    () -> "material-specific crate row left: " + row);
        }
    }

    private static long count(List<String> rows, String spec) {
        return rows.stream().filter(spec::equals).count();
    }
}
