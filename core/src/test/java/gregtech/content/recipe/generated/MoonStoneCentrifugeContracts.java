package gregtech.content.recipe.generated;

import java.util.List;

import static gregtech.content.recipe.SourceCentrifugeBlockDustInputs.*;
import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** Original Moon Stone dust recipe; unrelated to the seven physical block-input rules. */
final class MoonStoneCentrifugeContracts {
    private MoonStoneCentrifugeContracts() {}

    static void run() {
        Row expected = centrifuge("i:dust:MoonRock:1", 64, 16,
                List.of(8000L, 500L, 400L, 300L, 100L, 50L), List.of(),
                "i:dust:Basalt:1", "i:dustTiny:SiO2:1", "i:dustTiny:Al2O3:1",
                "i:dustTiny:Ilmenite:1", "i:dustTiny:RareEarth:1", "i:dustTiny:Pt:1");
        for (boolean easyU235 : new boolean[]{false, true}) {
            Row actual = only(collect(() -> GTOresGen.load(easyU235)),
                    r -> r.inputs().stream().anyMatch(s -> s.contains(":MoonRock:")), "MoonRock");
            equal(expected, actual, "one dust, six ordered outputs and per-unit chances in either U235 mode");
            check(!actual.outputs().contains(actual.inputs().get(0)), "optimization cannot cancel the input");
        }
        equal(7, bindings().size(), "physical block-input scope remains seven");
        equal(null, binding("MoonRock"), "Moon Stone must not gain an invented blockDust route");
    }
}
