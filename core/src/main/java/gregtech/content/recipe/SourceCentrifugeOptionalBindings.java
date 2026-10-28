package gregtech.content.recipe;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/** Finite, C-local source optional-holder policy. No platform classes, tag scan, registration,
 * global ModReferences change, or unification is performed here. PRESENT means the exact
 * reviewed mapping's supplied holder probe succeeded; it is not proof an optional mod ran.
 * Different-ID forks are outside this explicit provider contract, not universal vetoes.
 */
public final class SourceCentrifugeOptionalBindings {
    private SourceCentrifugeOptionalBindings() {}

    public enum State {
        PRESENT,
        ABSENT,
        UNKNOWN
    }

    public enum Symbol {
        TE_SLAG("IL.TE_Slag"),
        IE_SLAG("IL.IE_Slag"),
        FZ_SLUDGE("IL.FZ_Sludge"),
        FR_STICKY("IL.FR_Propolis_Sticky"),
        IC2_RESIN("IL.IC2_Resin");
        private final String sourceField;

        Symbol(String sourceField) {
            this.sourceField = sourceField;
        }

        public String sourceField() {
            return sourceField;
        }
    }

    public enum TargetKind {
        EXACT_ITEM,
        MATERIAL_SPEC
    }

    public record Target(TargetKind kind, String value) {
        public Target {
            Objects.requireNonNull(kind);
            Objects.requireNonNull(value);
        }
    }

    public record Snapshot(String minecraft, String loader, boolean complete, Map<String, String> mods) {
        public Snapshot {
            mods = Map.copyOf(Objects.requireNonNull(mods));
        }
    }
    /** null registryId is mandatory for ABSENT/UNKNOWN, preventing accidental fallback IDs. */
    public record Binding(State state, String registryId, String reason) {
        public Binding {
            Objects.requireNonNull(state);
            Objects.requireNonNull(reason);
            if ((state == State.PRESENT) != (registryId != null))
                throw new IllegalArgumentException("Only PRESENT may carry an exact item ID");
            if (registryId != null && !registryId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
                throw new IllegalArgumentException("Exact registry ID required");
        }

        public static Binding present(String id) {
            return new Binding(State.PRESENT, id, "REVIEWED_EXACT_HOLDER");
        }

        public static Binding absent(String why) {
            return new Binding(State.ABSENT, null, why);
        }

        public static Binding unknown(String why) {
            return new Binding(State.UNKNOWN, null, why);
        }
    }

    public record Mapping(
            String minecraft,
            String loader,
            String providerId,
            String providerVersion,
            State declaredState,
            String registryId,
            Map<String, String> requiredVersions,
            String sourceCommit,
            String metadataEvidence) {
        public Mapping {
            Objects.requireNonNull(minecraft);
            Objects.requireNonNull(loader);
            Objects.requireNonNull(providerId);
            Objects.requireNonNull(providerVersion);
            Objects.requireNonNull(declaredState);
            Objects.requireNonNull(sourceCommit);
            Objects.requireNonNull(metadataEvidence);
            requiredVersions = Map.copyOf(requiredVersions);
            if (declaredState == State.UNKNOWN || (declaredState == State.PRESENT) != (registryId != null))
                throw new IllegalArgumentException("Mapping must be reviewed presence or reviewed omission");
        }
    }

    public record Provider(Symbol symbol, List<String> recognizedIds, List<Mapping> mappings) {
        public Provider {
            Objects.requireNonNull(symbol);
            recognizedIds = List.copyOf(recognizedIds);
            mappings = List.copyOf(mappings);
            if (recognizedIds.isEmpty() || new HashSet<>(recognizedIds).size() != recognizedIds.size())
                throw new IllegalArgumentException("Finite unique provider IDs required");
        }
    }

    private static Mapping present(
            String mc,
            String loader,
            String id,
            String version,
            String item,
            Map<String, String> dependencies,
            String commit,
            String evidence) {
        return new Mapping(mc, loader, id, version, State.PRESENT, item, dependencies, commit, evidence);
    }

    private static Mapping absent(String mc, String loader, String id, String version, String commit, String evidence) {
        return new Mapping(mc, loader, id, version, State.ABSENT, null, Map.of(), commit, evidence);
    }

    /** Exact versions only. Forge IE runtime version includes its Minecraft prefix; release-tag
     * shorthand 10.2.0-183 is deliberately not admitted. Unreviewed versions remain UNKNOWN. */
    private static final Map<Symbol, Provider> PROVIDERS = Map.of(
            Symbol.TE_SLAG,
                    new Provider(
                            Symbol.TE_SLAG,
                            List.of("ThermalExpansion", "thermal"),
                            List.of(
                                    present(
                                            "1.20.1",
                                            "forge",
                                            "thermal",
                                            "11.0.5",
                                            "thermal:slag",
                                            Map.of("cofh_core", "11.0.2"),
                                            "b465053b16bfac4ab93bc151fff0d8280d75f10c",
                                            "Thermal Core11.0.5 + CoFH Core11.0.2 (6c9344a71c8c0c89e7d9fade1b679c3df466bfca) registration/metadata"))),
            Symbol.IE_SLAG,
                    new Provider(
                            Symbol.IE_SLAG,
                            List.of("ImmersiveEngineering", "immersiveengineering"),
                            List.of(
                                    present(
                                            "1.20.1",
                                            "forge",
                                            "immersiveengineering",
                                            "1.20.1-10.2.0-183",
                                            "immersiveengineering:slag",
                                            Map.of(),
                                            "e63e4824800945eccf3684200a2c2270e2e1cdf2",
                                            "build.gradle295 MC-prefixed project version -> mods.toml ${version}"),
                                    present(
                                            "1.21.1",
                                            "neoforge",
                                            "immersiveengineering",
                                            "12.4.2-194",
                                            "immersiveengineering:slag",
                                            Map.of(),
                                            "583a182549d284a35c813b55ac2a0d1fddcf945a",
                                            "12.4.2-194 exact release registration/metadata"))),
            Symbol.FZ_SLUDGE, new Provider(Symbol.FZ_SLUDGE, List.of("factorization"), List.of()),
            Symbol.FR_STICKY,
                    new Provider(
                            Symbol.FR_STICKY,
                            List.of("Forestry", "forestry"),
                            List.of(
                                    absent(
                                            "1.20.1",
                                            "forge",
                                            "forestry",
                                            "2.11.1",
                                            "f6e36720b002cce20b1987dfe74d57a7dc7a6aea",
                                            "EnumPropolis has no STICKY; registration iterates enum, stale model ignored"),
                                    absent(
                                            "1.21.1",
                                            "neoforge",
                                            "forestry",
                                            "3.0.0-alpha6",
                                            "6c2b822c01f277452e659e7e8bce0c9d08ccc1e4",
                                            "EnumPropolis has no STICKY; registration iterates enum"))),
            Symbol.IC2_RESIN, new Provider(Symbol.IC2_RESIN, List.of("IC2", "IC2-Classic-Spmod", "ic2"), List.of()));

    public static Provider provider(Symbol symbol) {
        return PROVIDERS.get(Objects.requireNonNull(symbol));
    }

    public static Map<Symbol, Provider> providers() {
        return PROVIDERS;
    }

    public static Binding resolve(Symbol symbol, Snapshot snapshot, Predicate<String> exactHolderAvailable) {
        Provider p = provider(symbol);
        if (snapshot == null || !snapshot.complete()) return Binding.unknown("INCOMPLETE_MOD_LIST");
        List<String> loaded =
                p.recognizedIds().stream().filter(snapshot.mods()::containsKey).toList();
        // Provider absence is an independent fact: do NOT require a modern item mapping here.
        if (loaded.isEmpty()) return Binding.absent("RECOGNIZED_PROVIDERS_ABSENT");
        if (loaded.size() != 1) return Binding.unknown("MULTIPLE_RECOGNIZED_PROVIDERS");
        String id = loaded.get(0), version = snapshot.mods().get(id);
        List<Mapping> candidates = p.mappings().stream()
                .filter(m -> Objects.equals(snapshot.minecraft(), m.minecraft())
                        && Objects.equals(snapshot.loader(), m.loader())
                        && id.equals(m.providerId())
                        && Objects.equals(version, m.providerVersion()))
                .toList();
        if (candidates.size() != 1) return Binding.unknown("UNREVIEWED_PROVIDER_PLATFORM_VERSION");
        Mapping mapping = candidates.get(0);
        for (var dep : mapping.requiredVersions().entrySet())
            if (!Objects.equals(snapshot.mods().get(dep.getKey()), dep.getValue()))
                return Binding.unknown("UNREVIEWED_REQUIRED_PROVIDER_VERSION");
        if (mapping.declaredState() == State.ABSENT) return Binding.absent("REVIEWED_ITEM_OMITTED");
        if (exactHolderAvailable == null) return Binding.unknown("EXACT_HOLDER_NOT_OBSERVED");
        try {
            return exactHolderAvailable.test(mapping.registryId())
                    ? Binding.present(mapping.registryId())
                    : Binding.unknown("EXPECTED_EXACT_HOLDER_MISSING");
        } catch (RuntimeException failedProbe) {
            return Binding.unknown("EXACT_HOLDER_PROBE_FAILED");
        }
    }

    /** Default targets are declarations, NOT claims that the default stack exists. The C adapter
     * still resolves the material spec/own item and preserves source slot/count/OM semantics. */
    public enum UnknownTreatment {
        DEFER_ROW,
        PRESERVE_EXISTING_PREFIX
    }

    public enum Plan {
        DARK_ASH(
                "darkash_second",
                List.of(Symbol.TE_SLAG, Symbol.IE_SLAG, Symbol.FZ_SLUDGE),
                new Target(TargetKind.MATERIAL_SPEC, "omdu:Ash:648648000"),
                UnknownTreatment.DEFER_ROW),
        STICKY(
                "sticky_resin",
                List.of(Symbol.FR_STICKY, Symbol.IC2_RESIN),
                new Target(TargetKind.EXACT_ITEM, "gt6ce:rubber_resin"),
                UnknownTreatment.PRESERVE_EXISTING_PREFIX);
        private final String token;
        private final List<Symbol> priority;
        private final Target fallback;
        private final UnknownTreatment unknownTreatment;

        Plan(String token, List<Symbol> priority, Target fallback, UnknownTreatment unknownTreatment) {
            this.token = token;
            this.priority = List.copyOf(priority);
            this.fallback = fallback;
            this.unknownTreatment = unknownTreatment;
        }

        public String token() {
            return token;
        }

        public List<Symbol> priority() {
            return priority;
        }

        public Target fallback() {
            return fallback;
        }

        public UnknownTreatment unknownTreatment() {
            return unknownTreatment;
        }

        public static Plan fromToken(String token) {
            for (Plan plan : values()) if (plan.token.equals(token)) return plan;
            return null;
        }
    }

    public enum Choice {
        ITEM,
        DEFAULT,
        DEFER
    }

    public record Selection(Choice choice, Target target, Symbol source, String reason) {
        public Selection {
            Objects.requireNonNull(choice);
            Objects.requireNonNull(reason);
            if ((choice == Choice.DEFER) != (target == null)) throw new IllegalArgumentException("Defer has no target");
            if (choice == Choice.ITEM && (source == null || target.kind() != TargetKind.EXACT_ITEM))
                throw new IllegalArgumentException("Named item requires source symbol and exact ID");
            if (choice == Choice.DEFAULT && source != null)
                throw new IllegalArgumentException("Default is not a named optional item");
        }
    }

    public static Selection choose(Plan plan, Snapshot snapshot, Predicate<String> exactHolderAvailable) {
        return select(plan, snapshot, exactHolderAvailable);
    }

    public static Selection select(Plan plan, Snapshot snapshot, Predicate<String> exactHolderAvailable) {
        return select(plan, symbol -> resolve(symbol, snapshot, exactHolderAvailable));
    }
    /** A pure priority fold, also usable by isolated contracts without any registry or mod installation. */
    public static Selection select(Plan plan, Function<Symbol, Binding> bindings) {
        Objects.requireNonNull(plan);
        Objects.requireNonNull(bindings);
        for (Symbol symbol : plan.priority()) {
            Binding binding = bindings.apply(symbol);
            if (binding == null) return new Selection(Choice.DEFER, null, symbol, "MISSING_BINDING_OBSERVATION");
            if (binding.state() == State.UNKNOWN) return new Selection(Choice.DEFER, null, symbol, binding.reason());
            if (binding.state() == State.PRESENT)
                return new Selection(
                        Choice.ITEM, new Target(TargetKind.EXACT_ITEM, binding.registryId()), symbol, binding.reason());
        }
        return new Selection(Choice.DEFAULT, plan.fallback(), null, "ALL_RECOGNIZED_OPTIONALS_ABSENT");
    }
}
