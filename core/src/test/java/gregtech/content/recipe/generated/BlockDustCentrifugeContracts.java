package gregtech.content.recipe.generated;

import java.util.List;

import static gregtech.content.recipe.SourceCentrifugeBlockDustInputs.*;
import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** A physical block holder is never substituted by nine loose dust items. */
final class BlockDustCentrifugeContracts {
    private BlockDustCentrifugeContracts() {}

    static void run() {
        List<Binding> expectedBindings = List.of(
                new Binding("Bedrock", "Bedrock", 8599),
                new Binding("Netherrack", "Netherrack", 8502),
                new Binding("Endstone", "Endstone", 8504),
                new Binding("EndSandWhite", "WhiteEndSand", 8105),
                new Binding("EndSandBlack", "BlackEndSand", 8106),
                new Binding("SpaceRock", "SpaceStone", 8512),
                new Binding("MoonTurf", "MoonTurf", 8514));
        equal(expectedBindings, bindings(), "finite source identities and material IDs");
        for (Binding binding : expectedBindings) {
            equal(binding, binding(binding.sourceField()), "exact field lookup");
            equal(Decision.PRESENT, decide(binding, new Facts(true, true, true, true, true,
                    binding.materialName(), binding.materialId())), "reviewed physical holder");
            equal(Decision.UNRESOLVED, decide(binding, new Facts(false, true, true, true, true,
                    binding.materialName(), binding.materialId())), "missing holder must not become dust9");
            equal(Decision.UNRESOLVED, decide(binding, null), "missing native observations");
            for (int bad = 0; bad < 6; bad++) {
                Facts facts = new Facts(true, bad != 0, bad != 1, bad != 2, bad != 3,
                        bad == 4 ? "other" : binding.materialName(), bad == 5 ? -1 : binding.materialId());
                equal(Decision.UNREVIEWED, decide(binding, facts), "reject mismatched native fact " + bad);
            }
        }
        equal(null, binding("MoonRock"), "do not invent an eighth block family");
        equal(Decision.UNRESOLVED, decide(new Binding("foreign", "other", 123),
                new Facts(true, true, true, true, true, "other", 123)), "caller cannot invent a binding");
        sourceRows();
    }

    private static void sourceRows() {
        List<String> gases = List.of("mu:He:gas:81081000:down", "mu:He_3:gas:11675664:down",
                "mu:NO2:gas:162162000:down", "mu:CO2:gas:162162000:down",
                "mu:H:gas:81081000:down", "mu:D:gas:81081000:down");
        List<Row> expected = List.of(
                centrifuge("cbd:Bedrock:1", 256, 1296, List.of(7000L, 3000L, 3000L, 3000L, 3000L, 3000L),
                        List.of(), "i:dust:Deepslate:9", "i:dustTiny:Adamantite:9", "i:dustTiny:Draconium:9",
                        "i:dustTiny:Monazite:18", "i:dustTiny:Zircon:27", "i:dustTiny:Graphite:27"),
                centrifuge("cbd:Netherrack:1", 64, 144, List.of(8000L, 1000L, 500L, 500L, 100L),
                        List.of(), "i:dust:Stone:9", "i:dustTiny:S:18", "i:dustTiny:Redstone:9",
                        "i:dustTiny:Coal:9", "i:dustTiny:Au:9"),
                endStone("Endstone", gases),
                endStone("EndSandWhite", gases),
                endStone("EndSandBlack", gases),
                centrifuge("cbd:SpaceRock:1", 64, 144, List.of(8000L, 100L, 100L), gases,
                        "i:dust:Sand:9", "i:dustTiny:Pt:9", "i:dustTiny:MeteoricIron:9"),
                centrifuge("cbd:MoonTurf:1", 64, 288, List.of(8000L, 1000L, 1000L, 1000L, 1000L, 100L),
                        gases, "i:dust:Sand:9", "i:dustTiny:Basalt:9", "i:dustTiny:Al2O3:9",
                        "i:dustTiny:Ilmenite:9", "i:dustTiny:MnO2:9", "i:dustTiny:MeteoricIron:9"));
        List<Row> rows = collect(GTOresGen::load);
        equal(expected, rows.stream().filter(r -> r.inputs().stream().anyMatch(s -> s.startsWith("cbd:"))).toList(),
                "seven source-exact ordered block rows, including chance/fluid slots");
        for (Row row : rows) {
            for (List<String> slots : List.of(row.outputs(), row.fluidInputs(), row.fluidOutputs())) {
                check(slots.stream().noneMatch(s -> s.startsWith("cbd:")), "physical-block token is input-only");
            }
            if (row.map().equals("Centrifuge")) {
                check(row.inputs().stream().noneMatch(s -> s.startsWith("i:blockDust:")),
                        "no legacy blockDust token may normalize to dust9");
            }
        }
    }

    private static Row endStone(String field, List<String> gases) {
        return centrifuge("cbd:" + field + ":1", 64, 144, List.of(8000L, 100L, 300L, 100L, 10L),
                gases, "i:dust:Sand:9", "i:dustTiny:Pt:9", "i:dustTiny:WO3:9",
                "i:dustTiny:Endium:9", "i:gem:Sugilite:9");
    }
}
