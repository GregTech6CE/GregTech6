package gregtech.content.recipe;

import java.util.List;

/** Finite C-only projection of five reviewed original default event instances (four positives).
 * No OreDictionary/tag discovery, provider lookup, platform holder access, or registration occurs here.
 * Source evidence: original 3703e40308c8c030763fd6297dea8b210d2a77b1 / W607771f74acb0fe647f7e9412676e72094799545;
 * independent listener oracle e3f31064cc2074cf21a2a7546033017241124aa6443a114d35ccdca4b7b6cac1.
 * Supplied changed/unknown relevant facts are never replaced by the default facts below.
 */
public final class SourceCentrifugeBuiltinEvents {
    private SourceCentrifugeBuiltinEvents() {}
    public enum Fact { YES, NO, UNKNOWN }
    public enum Family { EGG, GENERIC_SLIME, ANIMAL_SLIME }
    public enum Shell { ABSENT, REVIEWED_PRESENT, UNKNOWN }
    public enum Decision { PLAN, BLOCK, DEFER, NOT_REGISTERED }
    public enum ItemRole { EGG_WHITE, EGG_YOLK, BONE_MEAL, REVIEWED_BIRB_SHELL }
    public enum FluidRole { LATEX, GLUE }
    public enum Instance {
        VANILLA_EGG("minecraft:egg", -1, Family.EGG),
        OWN_EGG("gt6ce:egg", 1051, Family.EGG),
        VANILLA_SLIME("minecraft:slime_ball", -1, Family.GENERIC_SLIME),
        OWN_GOO("gt6ce:sticky_goo", 12098, Family.ANIMAL_SLIME),
        OWN_BORAX("gt6ce:slimeball", 12099, Family.GENERIC_SLIME);
        private final String registryId;
        private final int originalMeta;
        private final Family family;
        Instance(String registryId, int originalMeta, Family family) {
            this.registryId = registryId; this.originalMeta = originalMeta; this.family = family;
        }
        public String registryId() { return registryId; }
        /** -1 for vanilla identities, never a wildcard metadata admission. */
        public int originalMeta() { return originalMeta; }
        public Family family() { return family; }
    }
    public static List<Instance> instances() { return List.of(Instance.values()); }
    /** Exact case-sensitive registry ID only; no alias, namespace scan or tag acceptance. */
    public static Instance instance(String exactRegistryId) {
        for (Instance input : Instance.values()) if (input.registryId().equals(exactRegistryId)) return input;
        return null;
    }
    public record IngredientFacts(Fact valid, Fact gtContainerTool, Fact positiveFluidCapacity,
                                  Fact containerRemainder, Fact containerDurability,
                                  Fact exactEmptySpecial, Fact wildcardEmptySpecial) {}
    public record SlimeFacts(Fact tar, Fact ticOwned, Fact swet, Fact pink, Fact rice, Fact borax, Fact animal) {}
    public record Facts(Fact reviewedPlainHolder, int inputCount, Fact membership,
                        Fact meatSubstitute, Fact big, Shell shell,
                        SlimeFacts slime, IngredientFacts ingredient) {}
    public record ItemAmount(ItemRole role, int count) {}
    public record FluidAmount(FluidRole role, int millibuckets) {}
    public record Plan(int inputCount, boolean optimize, long power, long ticks,
                       List<ItemAmount> itemOutputs, List<FluidAmount> fluidInputs,
                       List<FluidAmount> fluidOutputs, List<Long> chances) {
        public Plan {
            itemOutputs = List.copyOf(itemOutputs); fluidInputs = List.copyOf(fluidInputs);
            fluidOutputs = List.copyOf(fluidOutputs); chances = List.copyOf(chances);
        }
    }
    public record Result(Decision decision, String reason, Plan plan) {
        public Result {
            if (decision == null || reason == null || (decision == Decision.PLAN) != (plan != null))
                throw new IllegalArgumentException("Only PLAN carries output data");
        }
    }

    /** These membership facts are source-instance data, NOT inferred from native tags[].
     * Holder/ingredient/shell facts still require the caller's actual local observations.
     * Do not call again to overwrite an explicitly changed or UNKNOWN fact.
     */
    public static Facts sourceDefaults(Instance input, Fact reviewedPlainHolder,
                                       IngredientFacts ingredient, Shell shell) {
        if (input == null) return null;
        return new Facts(reviewedPlainHolder, 1, Fact.YES, Fact.NO, Fact.NO, shell,
                new SlimeFacts(Fact.NO, Fact.NO, Fact.NO, Fact.NO, Fact.NO,
                        input == Instance.OWN_BORAX ? Fact.YES : Fact.NO,
                        input == Instance.OWN_GOO ? Fact.YES : Fact.NO), ingredient);
    }

    /** Read-only capacity observation reduction. Failed/incomplete/contradictory probes defer.
     * The platform adapter, not this pure helper, must inspect the real item's capability.
     */
    public static Fact positiveFluidCapacity(Fact capabilityPresent, boolean observationComplete,
                                             List<Long> capacities) {
        if (!observationComplete || unknown(capabilityPresent)) return Fact.UNKNOWN;
        if (capabilityPresent == Fact.NO)
            return capacities == null || capacities.isEmpty() ? Fact.NO : Fact.UNKNOWN;
        if (capacities == null) return Fact.UNKNOWN;
        boolean positive = false;
        for (Long capacity : capacities) {
            if (capacity == null || capacity < 0) return Fact.UNKNOWN;
            positive |= capacity > 0;
        }
        return positive ? Fact.YES : Fact.NO;
    }

    /** ST.ingredable order: invalid/tool/capacity/remainder/durability before exact-empty
     * exception, and exact-empty before wildcard rejection. Known rejection is decisive;
     * otherwise an unknown earlier relevant predicate cannot be skipped by a later exception.
     */
    public static Fact ingredable(IngredientFacts f) {
        if (f == null) return Fact.UNKNOWN;
        Fact[] rejects = {f.gtContainerTool(), f.positiveFluidCapacity(), f.containerRemainder(), f.containerDurability()};
        if (f.valid() == Fact.NO || anyYes(rejects)) return Fact.NO;
        if (unknown(f.valid()) || anyUnknown(rejects)) return Fact.UNKNOWN;
        if (f.exactEmptySpecial() == Fact.YES) return Fact.YES;
        if (unknown(f.exactEmptySpecial())) return Fact.UNKNOWN;
        if (f.wildcardEmptySpecial() == Fact.YES) return Fact.NO;
        return unknown(f.wildcardEmptySpecial()) ? Fact.UNKNOWN : Fact.YES;
    }

    /** The only production policy entry: five finite instances, never arbitrary Big/foreign inputs. */
    public static Result decide(Instance input, Facts f) {
        if (input == null) return result(Decision.DEFER, "UNSUPPORTED_EXACT_INSTANCE");
        Result result = sourceRule(input.family(), f);
        // Big source behavior remains tested below without turning this default-only projection
        // into a new dynamic event service. Meat-substitute rejection still takes precedence.
        if (result.decision() == Decision.PLAN && input.family() == Family.EGG && f.big() != Fact.NO)
            return result(Decision.DEFER, "NONDEFAULT_BIG_EVENT_OUT_OF_SCOPE");
        if (result.decision() == Decision.PLAN && input == Instance.OWN_BORAX)
            return result(Decision.DEFER, "NONDEFAULT_BORAX_MEMBERSHIP_OUT_OF_SCOPE");
        return result;
    }

    /** Package-private source semantics for direct core contracts, not an external event API. */
    static Result sourceRule(Family family, Facts f) {
        if (f == null || f.reviewedPlainHolder() != Fact.YES || unknown(f.membership()))
            return result(Decision.DEFER, "UNREVIEWED_HOLDER_OR_EVENT");
        if (f.membership() == Fact.NO) return result(Decision.NOT_REGISTERED, "SOURCE_EVENT_ABSENT");
        if (f.inputCount() != 1) return result(Decision.DEFER, "SOURCE_NORMALIZED_INPUT_ONE_REQUIRED");
        if (family == Family.ANIMAL_SLIME)
            return planned(64, List.of(), List.of(new FluidAmount(FluidRole.GLUE, 250)), List.of());
        if (family == Family.GENERIC_SLIME) {
            SlimeFacts s = f.slime();
            if (s == null) return result(Decision.DEFER, "SLIME_MEMBERSHIP_UNKNOWN");
            Fact[] excluded = {s.tar(), s.ticOwned(), s.swet(), s.pink(), s.rice(), s.borax(), s.animal()};
            Fact ingredient = ingredable(f.ingredient());
            if (anyYes(excluded) || ingredient == Fact.NO) return result(Decision.BLOCK, "GENERIC_SLIME_EXCLUSION");
            if (anyUnknown(excluded) || ingredient == Fact.UNKNOWN) return result(Decision.DEFER, "GENERIC_SLIME_FACT_UNKNOWN");
            return planned(64, List.of(), List.of(new FluidAmount(FluidRole.LATEX, 72), new FluidAmount(FluidRole.GLUE, 250)), List.of());
        }
        if (family == Family.EGG) {
            if (f.meatSubstitute() == Fact.YES) return result(Decision.BLOCK, "MEAT_SUBSTITUTE");
            if (unknown(f.meatSubstitute()) || unknown(f.big()) || f.shell() == null || f.shell() == Shell.UNKNOWN)
                return result(Decision.DEFER, "EGG_OR_SHELL_FACT_UNKNOWN");
            int amount = f.big() == Fact.YES ? 4 : 1;
            ItemRole shell = f.shell() == Shell.REVIEWED_PRESENT ? ItemRole.REVIEWED_BIRB_SHELL : ItemRole.BONE_MEAL;
            return planned(64L * amount, List.of(new ItemAmount(ItemRole.EGG_WHITE, amount),
                    new ItemAmount(ItemRole.EGG_YOLK, amount), new ItemAmount(shell, amount)), List.of(), List.of(10000L, 10000L, 10000L));
        }
        return result(Decision.DEFER, "UNKNOWN_FAMILY");
    }
    private static Result planned(long ticks, List<ItemAmount> items, List<FluidAmount> fluids, List<Long> chances) {
        return new Result(Decision.PLAN, "REVIEWED_SOURCE_DEFAULT", new Plan(1, true, 16, ticks, items, List.of(), fluids, chances));
    }
    private static Result result(Decision d, String reason) { return new Result(d, reason, null); }
    private static boolean unknown(Fact f) { return f == null || f == Fact.UNKNOWN; }
    private static boolean anyYes(Fact[] values) { for (Fact f : values) if (f == Fact.YES) return true; return false; }
    private static boolean anyUnknown(Fact[] values) { for (Fact f : values) if (unknown(f)) return true; return false; }
}
