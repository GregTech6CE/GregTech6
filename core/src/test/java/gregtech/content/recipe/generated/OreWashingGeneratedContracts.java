package gregtech.content.recipe.generated;

import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** The six bad loop representatives must not reappear beside direct, source-owned Cryotheum rows. */
final class OreWashingGeneratedContracts {
    private OreWashingGeneratedContracts() {}
    static void run() {
        var rows = collect(GTOresGen::load);
        check(rows.stream().flatMap(row -> row.fluidInputs().stream())
                .noneMatch(fluid -> fluid.startsWith("m:Cryotheum:")),
                "lossy Amber/Glowstone Cryotheum loop representatives removed");
        equal(8L, rows.stream().filter(row -> row.map().equals("Mixer")
                && row.fluidOutputs().stream().anyMatch(fluid -> fluid.startsWith("f:Sluice:"))).count(),
                "eight unrelated source Sluice Mixer rows retained");
    }
}
