package gregtech.content.recipe;

import gregtech.content.data.MaterialPrefix;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import static gregtech.content.recipe.SourcePurifiedAcidData.*;
import static gregtech.content.recipe.SourceOreWashingRules.Availability;
import static gregtech.content.recipe.SourceRecyclingFacts.Identity;
import static gregtech.content.recipe.SourceRecyclingFacts.Decision;

/**
 * Purified-acid Bath algorithms from GT6 3703e40308c8c030763fd6297dea8b210d2a77b1,
 * Loader_Recipes_Ores 108-159. Native identity/phase proof and registration are separate.
 * No ordinary washing eligibility, target conversion or purified/refined dust fallback applies.
 * Missing outputs retain source positions (Recipe 895); unknown context is not source absence.
 */
public final class SourcePurifiedAcidRules {
    private SourcePurifiedAcidRules() {}
    public enum Shape {
        WHOLE(MaterialPrefix.crushedPurified, 1), TINY(MaterialPrefix.crushedPurifiedTiny, 9);
        private final MaterialPrefix prefix; private final int count;
        Shape(MaterialPrefix prefix, int count) { this.prefix = prefix; this.count = count; }
        public MaterialPrefix prefix() { return prefix; }
        public int count() { return count; }
    }
    public enum Status { ROW, NO_ROW, UNSUPPORTED }
    public record Item(MaterialPrefix prefix, Identity material, int count) {}
    /** Null item preserves a known absent source output at its original chance position. */
    public record Slot(Item item, long chance) {}
    public record Candidate(Family family, Identity material, Shape shape) {}
    public record Result(Status status, Item input, List<Slot> outputs, List<Fluid> fluidInputs,
                         List<Fluid> fluidOutputs, String reason) {
        public Result { outputs = List.copyOf(outputs); fluidInputs = List.copyOf(fluidInputs); fluidOutputs = List.copyOf(fluidOutputs); }
        public int ticks() { return 256; }
        public int power() { return 0; }
        public boolean optimize() { return true; }
    }
    public interface Resolver {
        /** Exact direct form and quantity, without purified/refined fallback or target conversion. */
        Availability item(MaterialPrefix prefix, Identity material, int count);
        /** Source absence differs from unavailable native binding: these 18 source phases exist,
         * so an unproved/replaced/missing native holder must be UNKNOWN, not silently discarded. */
        Availability fluid(Phase phase, long millibuckets);
    }
    /** Exact source order: family, literal member order, whole then tiny. Dynamic array follows. */
    public static List<Candidate> fixedCandidates() {
        var result = new ArrayList<Candidate>();
        for (var family:families()) for (var member:family.members()) for (var shape:Shape.values())
            result.add(new Candidate(family, member, shape));
        return List.copyOf(result);
    }
    public static Decision fixedMembership(Family family, Identity material) {
        if (family == null || !families().contains(family) || !reviewedIdentity(material)) return Decision.UNKNOWN;
        return family.members().contains(material) ? Decision.YES : Decision.NO;
    }
    /** Direct list only: no identity/self shortcut, chemical decomposition or nested byproducts.
     * Null/unresolved list entries cannot prove absence. Native provenance is still external. */
    public static Decision directManganeseMembership(List<Identity> directByproducts) {
        if (directByproducts == null) return Decision.UNKNOWN;
        if (directByproducts.contains(MNO2)) return Decision.YES;
        return directByproducts.stream().anyMatch(Objects::isNull) ? Decision.UNKNOWN : Decision.NO;
    }
    /** Only the dynamic loop uses this gate. A positive-default list is not permission evidence;
     * a proven live core input newly acquiring direct MnO2 membership remains eligible. */
    public static Decision manganeseSelector(boolean validMaterial, boolean antimatter, Decision directContainsMno2) {
        if (!validMaterial || antimatter) return Decision.NO;
        return directContainsMno2 == null ? Decision.UNKNOWN : directContainsMno2;
    }
    public static Result fixedPlan(Family family, Identity material, Shape shape, Resolver resolver) {
        Decision member = fixedMembership(family, material);
        if (member != Decision.YES) return empty(member == Decision.NO?Status.NO_ROW:Status.UNSUPPORTED,"fixed source membership");
        if (shape == null || resolver == null) return empty(Status.UNSUPPORTED,"missing shape/resolver");
        Identity tail = switch (family.tail()) { case SELF -> material; case PLATINUM_GROUP_SLUDGE -> PGS; case GRAPHITE -> GRAPHITE; };
        int count = family.tail() == Tail.PLATINUM_GROUP_SLUDGE?8:2;
        var slots = new ArrayList<Slot>();
        slots.add(new Slot(new Item(MaterialPrefix.crushedCentrifuged, material, 1), 10000));
        for (int i = 0; i<5; i++) slots.add(new Slot(new Item(MaterialPrefix.crushedCentrifugedTiny, tail, count), 5000));
        return resolve(new Item(shape.prefix(), material, shape.count()), slots, family.fluidInput(), family.fluidOutputs(), resolver, false);
    }
    public static Result manganesePlan(Identity material, Shape shape, Decision sourceSelector, Resolver resolver) {
        if (sourceSelector != Decision.YES) return empty(sourceSelector == Decision.NO?Status.NO_ROW:Status.UNSUPPORTED,"dynamic source selector");
        if (material == null || shape == null || resolver == null) return empty(Status.UNSUPPORTED,"missing dynamic context");
        // OM.dust(MnCl2, 3U/2) is resolved below, not substituted with a fixed generated dust row.
        var slots = List.of(new Slot(new Item(MaterialPrefix.crushedCentrifuged, material, 1), 10000), new Slot(null, 10000));
        return resolve(new Item(shape.prefix(), material, shape.count()), slots, new Fluid(Phase.HYDROCHLORICACID, 4000),
                List.of(new Fluid(Phase.WATER, 3000), new Fluid(Phase.CHLORINE, 1000)), resolver, true);
    }
    private static Result resolve(Item input, List<Slot> desired, Fluid fluidInput, List<Fluid> desiredFluids,
                                  Resolver resolver, boolean manganese) {
        Availability itemInput = Objects.requireNonNull(resolver.item(input.prefix(), input.material(), input.count()));
        if (itemInput != Availability.PRESENT) return empty(itemInput == Availability.ABSENT?Status.NO_ROW:Status.UNSUPPORTED,"selected input shape unavailable");
        Availability acid = Objects.requireNonNull(resolver.fluid(fluidInput.phase(), fluidInput.millibuckets()));
        if (acid != Availability.PRESENT) return empty(acid == Availability.ABSENT?Status.NO_ROW:Status.UNSUPPORTED,"input acid unavailable");
        var slots = new ArrayList<Slot>();
        for (var desiredSlot:desired) {
            Item item = desiredSlot.item();
            if (item == null) { slots.add(desiredSlot); continue; }
            Availability found = Objects.requireNonNull(resolver.item(item.prefix(), item.material(), item.count()));
            if (found == Availability.UNKNOWN) return empty(Status.UNSUPPORTED,"unknown direct output form");
            slots.add(new Slot(found == Availability.PRESENT?item:null, desiredSlot.chance()));
        }
        if (manganese) {
            Choice dust = manganeseDust(resolver);
            if (dust.availability() == Availability.UNKNOWN) return empty(Status.UNSUPPORTED,"unknown MnCl2 dust form");
            slots.set(1, new Slot(dust.item(), 10000));
        }
        var fluidOutputs = new ArrayList<Fluid>();
        for (var fluid:desiredFluids) {
            Availability found = Objects.requireNonNull(resolver.fluid(fluid.phase(), fluid.millibuckets()));
            if (found == Availability.UNKNOWN) return empty(Status.UNSUPPORTED,"unknown output phase binding");
            if (found == Availability.PRESENT) fluidOutputs.add(fluid); // source removes all null fluids
        }
        if (slots.stream().noneMatch(s -> s.item() != null) && fluidOutputs.isEmpty()) return empty(Status.NO_ROW,"Bath needs at least one output");
        return new Result(Status.ROW, input, slots, List.of(fluidInput), fluidOutputs,"source purified-acid row");
    }
    private record Choice(Availability availability, Item item) {}
    private static Choice manganeseDust(Resolver resolver) {
        // OM 460-466: 1.5U is below 72U and not whole-divisible. Prefer small6, then floor
        // to tiny13; the final 108 div72 stack is source bindStack-clamped to 64.
        for (var item:List.of(new Item(MaterialPrefix.dustSmall, MNCL2, 6), new Item(MaterialPrefix.dustTiny, MNCL2, 13),
                new Item(MaterialPrefix.dustDiv72, MNCL2, 64))) {
            Availability found = Objects.requireNonNull(resolver.item(item.prefix(), item.material(), item.count()));
            if (found == Availability.UNKNOWN) return new Choice(found, null);
            if (found == Availability.PRESENT) return new Choice(found, item);
        }
        return new Choice(Availability.ABSENT, null);
    }
    private static Result empty(Status status, String reason) { return new Result(status, null, List.of(), List.of(), List.of(), reason); }
}
