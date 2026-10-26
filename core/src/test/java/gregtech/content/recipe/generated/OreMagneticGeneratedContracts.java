package gregtech.content.recipe.generated;

import java.util.List;
import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** Retire only the three projected block calls; retain every direct dust input and the other maps. */
final class OreMagneticGeneratedContracts {
    private OreMagneticGeneratedContracts() {}
    static void run() {
        var rows = collect(GTOresGen::load);
        var magnetic = rows.stream().filter(row -> row.map().equals("MagneticSeparator")).toList();
        check(magnetic.stream().flatMap(row -> row.inputs().stream()).noneMatch(input -> input.startsWith("i:blockDust:")),
                "physical Magnetic Separator rows must not use dust-nine resolver");
        for (String material : List.of("SluiceSand", "Bedrock", "MoonTurf")) {
            only(magnetic, row -> row.inputs().equals(List.of("i:dust:" + material + ":1")),
                    "ordinary dust retained for " + material);
        }
        for (String material : List.of("SluiceSand", "MoonTurf")) {
            for (String prefix : List.of("dustTiny", "dustSmall"))
                only(magnetic, row -> row.inputs().equals(List.of("i:" + prefix + ":" + material + ":1")),
                        "smaller dust retained for " + material + "/" + prefix);
        }
        equal(8L, rows.stream().filter(row -> row.map().equals("Centrifuge")
                && row.inputs().stream().anyMatch(input -> input.startsWith("cbd:"))).count(),
                "eight Centrifuge physical rows unaffected");
        equal(12L, rows.stream().filter(row -> row.map().equals("Sifting") && row.inputs().stream().anyMatch(input ->
                List.of("ChargedCertusQuartz", "CertusQuartz", "NetherQuartz", "VoidQuartz", "MilkyQuartz", "Fluix")
                        .stream().anyMatch(material -> input.equals("i:crushedPurified:" + material + ":1")
                                || input.equals("i:crushedPurifiedTiny:" + material + ":1")))).count(),
                "twelve Sifter special rows remain unchanged for their separate batch");
    }
}
