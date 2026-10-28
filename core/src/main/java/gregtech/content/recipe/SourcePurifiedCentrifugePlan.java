package gregtech.content.recipe;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure source plan: no Minecraft, registries, ownership changes or recipe registration.
 * Original Ores385-387,448-449,464-470; W389-391,452-453,468-474.
 * A resolver reports actual selected registrations and the source-equivalent ambient fluid result.
 * Generation flags, addon ownership and named-material allowlists are intentionally not inputs.
 */
public final class SourcePurifiedCentrifugePlan {
    private SourcePurifiedCentrifugePlan() {}

    public static final long SOURCE_U = 648648000L;
    public static final long SOURCE_U72 = 9009000L;
    public static final int AMBIENT_K = 293;

    public enum Form {
        DUST,
        DUST_TINY,
        PURIFIED,
        PURIFIED_TINY,
        REFINED_TINY
    }

    public enum State {
        PRESENT,
        ABSENT,
        UNKNOWN
    }

    public enum Status {
        PLANNED,
        SKIPPED_SOURCE_GATE,
        UNRESOLVED
    }

    public enum EnvironmentKind {
        ITEM,
        FLUID,
        UNKNOWN
    }

    /** Binding key is an opaque selected registry identity; foreign registrations are valid. */
    public record FormResult(State state, String bindingKey) {
        public FormResult {
            Objects.requireNonNull(state);
            if (state == State.PRESENT) Objects.requireNonNull(bindingKey);
        }

        public static FormResult present(String key) {
            return new FormResult(State.PRESENT, key);
        }

        public static FormResult absent() {
            return new FormResult(State.ABSENT, null);
        }

        public static FormResult unknown() {
            return new FormResult(State.UNKNOWN, null);
        }
    }
    /** The reference ratio is source evidence, not a current catalog's guessed 1000/144 convention. */
    public record FluidBinding(String bindingKey, long referenceSourceUnits, long referenceMillibuckets) {
        public FluidBinding {
            Objects.requireNonNull(bindingKey);
        }
    }

    public record Environment(EnvironmentKind kind, FluidBinding binding, String evidence) {
        public Environment {
            Objects.requireNonNull(kind);
            Objects.requireNonNull(evidence);
        }

        public static Environment item(String evidence) {
            return new Environment(EnvironmentKind.ITEM, null, evidence);
        }

        public static Environment fluid(String key, long units, long mb) {
            return new Environment(EnvironmentKind.FLUID, new FluidBinding(key, units, mb), "source-bound-fluid");
        }

        public static Environment unknown(String reason) {
            return new Environment(EnvironmentKind.UNKNOWN, null, reason);
        }
    }

    public interface Resolver<M> {
        /** No generator call; PRESENT/ABSENT/UNKNOWN are distinct actual-registration facts. */
        FormResult form(M material, Form form);
        /** Equivalent to source material.fluid(293,U72,true), before U72 quantity conversion.
         * Must distinguish source-known no fluid/solid from unresolved phase or binding.
         * The source checks plasma, gas, liquid in that order; this planner does not guess phase.
         */
        Environment environment(M material, int kelvin);
    }

    public record Request<M>(
            M source, boolean valid, boolean antimatter, boolean bedrock, int toolQuality, List<M> orderedByproducts) {
        public Request {
            Objects.requireNonNull(source);
            orderedByproducts = List.copyOf(orderedByproducts);
        }
    }

    public record Chosen<M>(int byproductIndex, M material, boolean selfFallback) {}
    /** A null binding is a source null output slot, not an instruction to reject the recipe. */
    public record ItemSlot<M>(
            int sourceSlot,
            M material,
            Form selectedForm,
            String bindingKey,
            int count,
            long chance,
            String absenceReason) {
        public boolean present() {
            return bindingKey != null;
        }

        public String sourceSlotIdentity() {
            return "source-output-slot-" + sourceSlot;
        }
    }
    /** Duplicate fluids retain source-slot multiplicity; no merging or chance is applied. */
    public record FluidOutput<M>(int sourceSlot, M material, String bindingKey, int millibuckets) {}

    public record Variant<M>(
            Form inputForm,
            String inputBinding,
            int inputCount,
            long duration,
            long nativeRecipePower,
            List<ItemSlot<M>> rawItemSlots,
            List<Long> rawChances,
            List<FluidOutput<M>> fluidOutputs) {
        public Variant {
            rawItemSlots = List.copyOf(rawItemSlots);
            rawChances = List.copyOf(rawChances);
            fluidOutputs = List.copyOf(fluidOutputs);
        }
        /** Original Recipe893-894 trims trailing null items, not interior nulls; chance array stays raw. */
        public List<ItemSlot<M>> sourceTrimmedItemSlots() {
            int end = rawItemSlots.size();
            while (end > 0 && !rawItemSlots.get(end - 1).present()) end--;
            return List.copyOf(rawItemSlots.subList(0, end));
        }
        /** Optional port projection. The adapter must use each retained slot's own chance. */
        public List<ItemSlot<M>> compactedItemsWithChances() {
            return rawItemSlots.stream().filter(ItemSlot::present).toList();
        }
    }

    public record Outcome<M>(Status status, String reason, List<Chosen<M>> chosen, List<Variant<M>> variants) {
        public Outcome {
            chosen = List.copyOf(chosen);
            variants = List.copyOf(variants);
        }
    }

    private record OutputForm(Form form, FormResult result) {}

    private static final class Unknown extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Unknown(String message) {
            super(message);
        }
    }

    public static <M> Outcome<M> plan(Request<M> request, Resolver<M> resolver) {
        Objects.requireNonNull(resolver);
        if (!request.valid()) return stopped(Status.SKIPPED_SOURCE_GATE, "invalid-source");
        if (request.antimatter()) return stopped(Status.SKIPPED_SOURCE_GATE, "source-antimatter");
        if (request.bedrock()) return stopped(Status.SKIPPED_SOURCE_GATE, "source-bedrock");
        if (request.toolQuality() < 0 || request.toolQuality() > 15)
            return stopped(Status.UNRESOLVED, "quality-outside-source-bind4-domain");
        try {
            FormResult dust = get(resolver, request.source(), Form.DUST);
            if (dust.state() == State.ABSENT) return stopped(Status.SKIPPED_SOURCE_GATE, "no-registered-dust");
            requireKnown(dust, "dust");
            FormResult whole = get(resolver, request.source(), Form.PURIFIED);
            FormResult tiny = get(resolver, request.source(), Form.PURIFIED_TINY);
            if (whole.state() == State.ABSENT || tiny.state() == State.ABSENT)
                return stopped(Status.SKIPPED_SOURCE_GATE, "both-purified-input-forms-required");
            requireKnown(whole, "purified-whole");
            requireKnown(tiny, "purified-tiny");

            var chosen = new ArrayList<Chosen<M>>();
            var environments = new ArrayList<Environment>();
            var roundedTinyMb = new ArrayList<Integer>();
            for (int index = 0; index < 3; index++) {
                boolean fallback = index >= request.orderedByproducts().size();
                M material = fallback
                        ? request.source()
                        : request.orderedByproducts().get(index);
                chosen.add(new Chosen<>(index, material, fallback));
                Environment environment = resolver.environment(material, AMBIENT_K);
                if (environment == null || environment.kind() == EnvironmentKind.UNKNOWN)
                    throw new Unknown("byproduct-environment-unknown-slot-" + index);
                int amount = environment.kind() == EnvironmentKind.FLUID ? tinyFluid(environment.binding()) : 0;
                // Source FL.zero converts zero-volume/invalid fluids to null, selecting item fallback.
                if (amount == 0 && environment.kind() == EnvironmentKind.FLUID)
                    environment = Environment.item("source-rounded-fluid-zero");
                environments.add(environment);
                roundedTinyMb.add(amount);
            }
            OutputForm main = outputForm(resolver, request.source());
            var products = new ArrayList<OutputForm>();
            for (int index = 0; index < 3; index++)
                products.add(
                        environments.get(index).kind() == EnvironmentKind.ITEM
                                ? outputForm(resolver, chosen.get(index).material())
                                : null);
            var variants = new ArrayList<Variant<M>>();
            for (int scale : new int[] {9, 1}) {
                var items = new ArrayList<ItemSlot<M>>();
                var fluids = new ArrayList<FluidOutput<M>>();
                items.add(item(0, request.source(), main, scale, 10000L));
                for (int index = 0; index < 3; index++) {
                    M material = chosen.get(index).material();
                    if (environments.get(index).kind() == EnvironmentKind.FLUID) {
                        long amount = (long) roundedTinyMb.get(index) * scale;
                        if (amount > Integer.MAX_VALUE) throw new Unknown("native-fluid-capacity-overflow");
                        fluids.add(new FluidOutput<>(
                                index + 1,
                                material,
                                environments.get(index).binding().bindingKey(),
                                (int) amount));
                        items.add(new ItemSlot<>(index + 1, material, null, null, scale, 1000L, "fluid-output"));
                    } else items.add(item(index + 1, material, products.get(index), scale, 1000L));
                }
                variants.add(new Variant<>(
                        scale == 9 ? Form.PURIFIED : Form.PURIFIED_TINY,
                        scale == 9 ? whole.bindingKey() : tiny.bindingKey(),
                        1,
                        scale * 16L * (request.toolQuality() + 1L),
                        16L,
                        items,
                        List.of(10000L, 1000L, 1000L, 1000L),
                        fluids));
            }
            return new Outcome<>(Status.PLANNED, "registered-purified-source-rule", chosen, variants);
        } catch (Unknown unresolved) {
            return stopped(Status.UNRESOLVED, unresolved.getMessage());
        }
    }

    private static <M> FormResult get(Resolver<M> resolver, M material, Form form) {
        FormResult value = resolver.form(material, form);
        return value == null ? FormResult.unknown() : value;
    }

    private static void requireKnown(FormResult result, String role) {
        if (result.state() == State.UNKNOWN) throw new Unknown("form-unknown-" + role);
    }

    private static <M> OutputForm outputForm(Resolver<M> resolver, M material) {
        FormResult refined = get(resolver, material, Form.REFINED_TINY);
        requireKnown(refined, "refined-tiny");
        if (refined.state() == State.PRESENT) return new OutputForm(Form.REFINED_TINY, refined);
        FormResult dust = get(resolver, material, Form.DUST_TINY);
        requireKnown(dust, "dust-tiny-fallback");
        return new OutputForm(Form.DUST_TINY, dust);
    }

    private static <M> ItemSlot<M> item(int slot, M material, OutputForm form, int count, long chance) {
        return new ItemSlot<>(
                slot,
                material,
                form.form(),
                form.result().bindingKey(),
                count,
                chance,
                form.result().state() == State.ABSENT ? "neither-refined-tiny-nor-dust-tiny-registered" : null);
    }

    private static int tinyFluid(FluidBinding binding) {
        if (binding == null || binding.referenceSourceUnits() <= 0 || binding.referenceMillibuckets() < 0)
            throw new Unknown("invalid-source-fluid-unit-binding");
        BigInteger[] ratio = BigInteger.valueOf(SOURCE_U72)
                .multiply(BigInteger.valueOf(binding.referenceMillibuckets()))
                .divideAndRemainder(BigInteger.valueOf(binding.referenceSourceUnits()));
        BigInteger rounded = ratio[0].add(ratio[1].signum() == 0 ? BigInteger.ZERO : BigInteger.ONE);
        if (rounded.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0)
            throw new Unknown("native-fluid-capacity-overflow");
        return rounded.intValue();
    }

    private static <M> Outcome<M> stopped(Status status, String reason) {
        return new Outcome<>(status, reason, List.of(), List.of());
    }
}
