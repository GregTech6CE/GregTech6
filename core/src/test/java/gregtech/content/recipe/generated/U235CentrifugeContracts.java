package gregtech.content.recipe.generated;

import java.util.List;

import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** Only the two source U238 refined-input calls are gated; the no-arg loader defaults off. */
final class U235CentrifugeContracts {
    private U235CentrifugeContracts() {}

    static void run() {
        List<Row> enabled = collect(() -> GTOresGen.load(true));
        List<Row> disabled = collect(() -> GTOresGen.load(false));
        equal(disabled, collect(GTOresGen::load), "default-off source loader");
        List<Row> gated = enabled.stream().filter(U235CentrifugeContracts::gated).toList();
        equal(List.of(
                centrifuge("i:crushedCentrifugedTiny:U_238:1", 1024, 256,
                        List.of(10000L, 200L, 100L), List.of(),
                        "i:dustTiny:U_238:1", "i:dustTiny:U_235:8", "i:dustTiny:U_233:8"),
                centrifuge("i:crushedCentrifuged:U_238:1", 1024, 2304,
                        List.of(10000L, 1800L, 900L), List.of(),
                        "i:dust:U_238:1", "i:dustTiny:U_235:8", "i:dustTiny:U_233:8")),
                gated, "two source-exact opt-in rows");
        check(disabled.stream().noneMatch(U235CentrifugeContracts::gated), "default cannot leak either row");
        equal(enabled.stream().filter(r -> !gated(r)).toList(), disabled,
                "all unrelated rows, ordering, and optimize flags are independent of the gate");
        equal(20L, disabled.stream().filter(r -> r.map().equals("Centrifuge")
                && r.inputs().size() == 1 && r.inputs().get(0).startsWith("i:crushedCentrifuged")).count(),
                "other twenty refined Centrifuge calls remain enabled");
        equal(4L, disabled.stream().filter(r -> r.map().equals("Mixer")
                && r.inputs().stream().anyMatch(s -> s.contains(":U_238:"))).count(),
                "four U238 Mixer rows are not gated");
    }

    private static boolean gated(Row row) {
        return row.map().equals("Centrifuge") && (row.inputs().equals(List.of("i:crushedCentrifugedTiny:U_238:1"))
                || row.inputs().equals(List.of("i:crushedCentrifuged:U_238:1")));
    }
}
