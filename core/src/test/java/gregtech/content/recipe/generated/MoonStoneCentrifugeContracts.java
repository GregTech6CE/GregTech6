package gregtech.content.recipe.generated;

import java.util.List;

import static gregtech.content.recipe.SourceCentrifugeBlockDustInputs.*;
import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** Original Moon Stone dust and physical-block recipes remain distinct source rows. */
final class MoonStoneCentrifugeContracts {
    private MoonStoneCentrifugeContracts() {}

    static void run() {
        Row expected = centrifuge("i:dust:MoonRock:1", 64, 16,
                List.of(8000L, 500L, 400L, 300L, 100L, 50L), List.of(),
                "i:dust:Basalt:1", "i:dustTiny:SiO2:1", "i:dustTiny:Al2O3:1",
                "i:dustTiny:Ilmenite:1", "i:dustTiny:RareEarth:1", "i:dustTiny:Pt:1");
        Row expectedBlock = centrifuge("cbd:MoonRock:1", 64, 144,
                List.of(8000L, 500L, 400L, 300L, 100L, 50L), List.of(),
                "i:dust:Basalt:9", "i:dustTiny:SiO2:9", "i:dustTiny:Al2O3:9",
                "i:dustTiny:Ilmenite:9", "i:dustTiny:RareEarth:9", "i:dustTiny:Pt:9");
        for (boolean easyU235 : new boolean[]{false, true}) {
            List<Row> rows = collect(() -> GTOresGen.load(easyU235));
            Row actual = only(rows, r -> r.inputs().contains("i:dust:MoonRock:1"), "MoonRock dust");
            equal(expected, actual, "one dust, six ordered outputs and per-unit chances in either U235 mode");
            check(!actual.outputs().contains(actual.inputs().get(0)), "optimization cannot cancel the input");
            equal(expectedBlock, only(rows, r -> r.inputs().contains("cbd:MoonRock:1"), "MoonRock block"),
                    "one physical block, nine-unit outputs and unchanged chances in either U235 mode");
            equal(2L, rows.stream().filter(r -> r.inputs().stream().anyMatch(s -> s.contains(":MoonRock:")))
                    .count(), "only the distinct source dust and physical-block rows");
        }
        equal(8, bindings().size(), "physical block-input scope remains finite");
        equal(new Binding("MoonRock", "MoonStone", 8513), binding("MoonRock"),
                "source MoonRock is the existing native MoonStone material");
    }
}
