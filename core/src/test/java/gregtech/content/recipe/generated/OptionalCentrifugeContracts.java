package gregtech.content.recipe.generated;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static gregtech.content.recipe.SourceCentrifugeOptionalBindings.*;
import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** Synthetic provider facts and source rows; does not claim installed optional-mod coverage. */
final class OptionalCentrifugeContracts {
    private OptionalCentrifugeContracts() {}

    static void run() {
        priorities();
        providersFailClosed();
        sourceRows();
    }

    private static Binding binding(State state, Symbol symbol) {
        return switch (state) {
            case PRESENT -> Binding.present("synthetic:" + symbol.name().toLowerCase(Locale.ROOT));
            case ABSENT -> Binding.absent("synthetic absence");
            case UNKNOWN -> Binding.unknown("synthetic unknown");
        };
    }

    private static Snapshot forge(Map<String, String> mods) {
        return new Snapshot("1.20.1", "forge", true, mods);
    }

    private static void priorities() {
        for (Plan plan : List.of(Plan.DARK_ASH, Plan.STICKY)) {
            int combinations = (int) Math.pow(3, plan.priority().size());
            for (int code = 0; code < combinations; code++) {
                State[] states = new State[plan.priority().size()];
                int remaining = code;
                int first = -1;
                for (int i = 0; i < states.length; i++) {
                    states[i] = State.values()[remaining % 3];
                    remaining /= 3;
                    if (first < 0 && states[i] != State.ABSENT) first = i;
                }
                int[] calls = {0};
                Selection result = select(plan, symbol -> {
                    calls[0]++;
                    return binding(states[plan.priority().indexOf(symbol)], symbol);
                });
                Choice expected = first < 0 ? Choice.DEFAULT
                        : states[first] == State.PRESENT ? Choice.ITEM : Choice.DEFER;
                equal(expected, result.choice(), plan + " priority case " + code);
                equal(first < 0 ? states.length : first + 1, calls[0], "short-circuit after first decision");
                equal(first < 0 ? null : plan.priority().get(first), result.source(), "earliest source wins");
            }
        }
        equal(new Target(TargetKind.MATERIAL_SPEC, "omdu:Ash:648648000"),
                Plan.DARK_ASH.fallback(), "DarkAsh default remains a separate Ash output");
        equal(new Target(TargetKind.EXACT_ITEM, "gt6ce:rubber_resin"),
                Plan.STICKY.fallback(), "Sticky default is own resin");
        equal(UnknownTreatment.DEFER_ROW, Plan.DARK_ASH.unknownTreatment(), "unknown DarkAsh defers row");
        equal(UnknownTreatment.PRESERVE_EXISTING_PREFIX, Plan.STICKY.unknownTreatment(),
                "unknown Sticky preserves its existing Wax/Latex prefix");
        equal(Choice.DEFER, select(Plan.DARK_ASH, symbol -> null).choice(), "missing binding is not absence");
    }

    private static void providersFailClosed() {
        for (Symbol symbol : Symbol.values()) {
            equal(State.ABSENT, resolve(symbol, forge(Map.of()), id -> {
                throw new AssertionError("absent provider must not probe items");
            }).state(), "provider absence: " + symbol);
            equal(State.UNKNOWN, resolve(symbol,
                    new Snapshot("1.20.1", "forge", false, Map.of()), id -> true).state(),
                    "incomplete mod snapshot: " + symbol);
        }
        Snapshot ie = forge(Map.of("immersiveengineering", "1.20.1-10.2.0-183"));
        equal(State.PRESENT, resolve(Symbol.IE_SLAG, ie,
                id -> id.equals("immersiveengineering:slag")).state(), "reviewed Forge holder");
        equal(State.UNKNOWN, resolve(Symbol.IE_SLAG, ie, id -> false).state(), "missing holder is not absence");
        equal(State.UNKNOWN, resolve(Symbol.IE_SLAG, ie, id -> {
            throw new IllegalStateException("failed native probe");
        }).state(), "failed holder probe is not absence");
        equal(State.UNKNOWN, resolve(Symbol.IE_SLAG,
                forge(Map.of("immersiveengineering", "10.2.0-183")), id -> true).state(),
                "release shorthand is not an exact runtime version");
        equal(State.PRESENT, resolve(Symbol.IE_SLAG,
                new Snapshot("1.21.1", "neoforge", true, Map.of("immersiveengineering", "12.4.2-194")),
                id -> id.equals("immersiveengineering:slag")).state(), "reviewed NeoForge holder");
        equal(State.UNKNOWN, resolve(Symbol.IE_SLAG,
                new Snapshot("1.21.1", "forge", true, Map.of("immersiveengineering", "12.4.2-194")),
                id -> true).state(), "mapping cannot cross loaders");
        equal(State.PRESENT, resolve(Symbol.TE_SLAG,
                forge(Map.of("thermal", "11.0.5", "cofh_core", "11.0.2")),
                id -> id.equals("thermal:slag")).state(), "reviewed Thermal dependency pair");
        for (Map<String, String> mods : List.of(Map.of("thermal", "11.0.5"),
                Map.of("thermal", "11.0.5", "cofh_core", "11.0.3"))) {
            equal(State.UNKNOWN, resolve(Symbol.TE_SLAG, forge(mods), id -> true).state(),
                    "missing or unreviewed dependency cannot supply fallback");
        }
        equal(State.ABSENT, resolve(Symbol.FR_STICKY, forge(Map.of("forestry", "2.11.1")), id -> {
            throw new AssertionError("reviewed omission must not probe guessed items");
        }).state(), "reviewed Forestry omission");
        equal(State.UNKNOWN, resolve(Symbol.FR_STICKY,
                forge(Map.of("forestry", "2.11.2")), id -> true).state(), "other Forestry versions unknown");
        equal(State.UNKNOWN, resolve(Symbol.IE_SLAG,
                forge(Map.of("ImmersiveEngineering", "old", "immersiveengineering", "1.20.1-10.2.0-183")),
                id -> true).state(), "conflicting provider identities");
        for (String id : List.of("IC2", "IC2-Classic-Spmod", "ic2")) {
            equal(State.UNKNOWN, resolve(Symbol.IC2_RESIN,
                    forge(Map.of(id, "unreviewed")), item -> true).state(), "unreviewed IC2 " + id);
        }
        Snapshot blocked = forge(Map.of("thermal", "unreviewed", "immersiveengineering", "1.20.1-10.2.0-183"));
        equal(Choice.DEFER, select(Plan.DARK_ASH, blocked, id -> {
            throw new AssertionError("earlier UNKNOWN must prevent lower-priority holder probes");
        }).choice(), "known lower provider cannot bypass an earlier unknown");
    }

    private static void sourceRows() {
        List<Row> rows = collect(() -> {
            GTCombGen.load();
            GTOresGen.load();
            GTChemGen.load();
            GTOtherGen.load();
        });
        Row dark = only(rows, r -> r.inputs().contains("omdu:DarkAsh:1297296000"), "DarkAsh");
        equal(centrifuge("omdu:DarkAsh:1297296000", 64, 144, null, List.of(),
                "omdu:Ash:648648000", "copt:darkash_second:1"), dark, "source DarkAsh slots and units");
        Row sticky = only(rows, r -> r.inputs().contains("tech:sticky_comb:1"), "Sticky");
        equal(centrifuge("tech:sticky_comb:1", 16, 64, List.of(10000L, 3000L), List.of("f:Latex:144"),
                "i:dust:WaxBee:1", "copt:sticky_resin:1"), sticky, "source Sticky slots and raw chances");
        int optional = 0;
        for (Row row : rows) {
            for (List<String> slots : List.of(row.inputs(), row.fluidInputs(), row.fluidOutputs())) {
                check(slots.stream().noneMatch(s -> s.startsWith("copt:")), "optional token is output-only");
            }
            for (int i = 0; i < row.outputs().size(); i++) {
                if (!row.outputs().get(i).startsWith("copt:")) continue;
                optional++;
                check(row.equals(dark) || row.equals(sticky), "only reviewed optional producers");
                equal(1, i, "optional second output does not reindex chances");
                equal(2, row.outputs().size(), "optional output remains final");
            }
        }
        equal(2, optional, "exactly the two reviewed optional slots");
    }
}
