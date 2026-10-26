package gregtech.content.recipe.generated;

import java.util.List;
import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** Only the eighteen fixed purified imports move to the source-owned Sifter producer. */
final class OreSiftingGeneratedContracts {
    private OreSiftingGeneratedContracts() {}
    static void run() {
        var rows = collect(GTOresGen::load);
        var sifting = rows.stream().filter(row -> row.map().equals("Sifting")).toList();
        equal(21, sifting.size(), "all unrelated imported Sifter rows retained");
        check(sifting.stream().flatMap(row -> row.inputs().stream()).noneMatch(input ->
                input.startsWith("i:crushedPurified:") || input.startsWith("i:crushedPurifiedTiny:")),
                "no late imported fixed rows can compete with the source-owned purified producer");
        for (String input : List.of("omd:SoulSand:1000", "v:soul_sand:1", "tech:tropic_sand_black:1"))
            only(sifting, row -> row.inputs().equals(List.of(input)), "unrelated soil row remains " + input);
        equal(8L, rows.stream().filter(row -> row.map().equals("Centrifuge")
                && row.inputs().stream().anyMatch(input -> input.startsWith("cbd:"))).count(),
                "physical Centrifuge rows unaffected");
        equal(8L, rows.stream().filter(row -> row.map().equals("Mixer") && row.fluidOutputs().contains("f:Sluice:100")
                || row.map().equals("Mixer") && row.fluidOutputs().contains("f:Sluice:900")).count(),
                "unrelated eight Sluice fluid mixer calls retained");
    }
}
