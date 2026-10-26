package gregtech.content.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import static gregtech.api.material.GTValues.U;
import static gregtech.content.recipe.SourceOreWashingRules.Availability;
import static gregtech.content.recipe.SourceRecyclingFacts.Decision;
import static gregtech.content.recipe.SourceRecyclingFacts.Identity;

/** Pinned GT6 3703e40308c8c030763fd6297dea8b210d2a77b1,
 * RecipeMapHandlerCrushing 50-140 / RecipeMapHammer 88-118 / OM 460-467.
 * Caller proves native identity, source prefix/host classification, and relevant source tag.
 * Runtime input/host metadata is consumed, not overwritten from imported source defaults.
 * No global matcher, material metadata, units, or recipe-map behavior is changed here. */
public final class SourceOreCrushingRules {
    private SourceOreCrushingRules() {}
    public enum Machine { CRUSHER, HAMMER }
    public enum Kind { STANDARD, DENSE, RAW_ITEM, RAW_BLOCK, POOR, SMALL_RICH_NORMAL }
    public enum Form { CRUSHED, CRUSHED_TINY, DUST, DUST_SMALL, DUST_TINY, DUST_DIV72, GEM, BLOCK_DUST }
    public enum Hexorium { NONE, BLACK_WHITE, RED_GREEN_BLUE }
    public enum Status { ROW, NO_ROW, UNSUPPORTED }
    public static final Identity CINNABAR = new Identity(9114, "Cinnabar");
    public static final int SOURCE_CRUSHER_CAPACITY = 12, SOURCE_HAMMER_CAPACITY = 1;
    public record Bonus(Decision pulverizingCinnabar, boolean mercuryByproduct,
                        boolean cinnabarByproduct, boolean redstoneByproduct) {
        public Bonus { Objects.requireNonNull(pulverizingCinnabar); }
    }
    /** These byproduct flags are contains(input.byproducts, exact canonical identity), not counts;
     * repeated entries do not add the same bonus again. Target byproducts are irrelevant. */
    public record Input(Identity material, Identity crushingTarget, long crushingUnitsPerU,
                        int oreMultiplier, int processingMultiplier, int quality, Bonus bonus,
                        Hexorium hexorium) {}
    /** Quality belongs to material, not crushingTarget. amount is the prefix's gangue amount. */
    public record Gangue(Identity material, long amount, int quality,
                         Identity crushingTarget, long crushingUnitsPerU) {}
    public record Request(Machine machine, Kind kind, Input input, boolean hexoriumAdjustedHost,
                          List<Gangue> gangue, int outputCapacity) {
        public Request { gangue = List.copyOf(gangue); }
    }
    /** Native-representable direct stack. Original Hammer zero stacks are returned as UNSUPPORTED,
     * never silently promoted to Crusher's minimum-one yield. */
    public record Stack(Form form, Identity material, int count) {
        public Stack {
            Objects.requireNonNull(form); Objects.requireNonNull(material);
            if (count < 1) throw new IllegalArgumentException("Native output count must be positive");
        }
    }
    /** Null stack is an explicit source empty slot, notably an unavailable Cinnabar bonus gem. */
    public record Slot(Stack stack, long chance) {}
    public record Result(Status status, List<Slot> slots, long ticks, String reason) {
        public Result { slots = List.copyOf(slots); }
        public int power() { return 16; }
        public boolean optimize() { return false; }
    }
    @FunctionalInterface public interface Resolver {
        /** Physical direct form existence; no generation flags or mass-changing fallback. */
        Availability direct(Form form, Identity material, int count);
    }
    private record Choice(Availability availability, Stack stack) {}

    /** Source input gate only. UNKNOWN host/item provenance is kept outside this boolean predicate.
     * PFAA sand rejection is Crusher-only; source Hammer does not check it. */
    public static boolean eligible(Machine machine, boolean validData, boolean antimatter,
            boolean orePrefix, boolean bedrock, boolean dustOre, boolean container, boolean pfaaSand) {
        return machine != null && validData && !antimatter && orePrefix && !bedrock && !dustOre && !container
                && !(machine == Machine.CRUSHER && pfaaSand);
    }
    public static Result plan(Request request, Resolver resolver) {
        if (request == null || resolver == null) return result(Status.UNSUPPORTED, "missing planning context");
        try { return checkedPlan(request, resolver); }
        catch (ArithmeticException overflow) { return result(Status.UNSUPPORTED, "numeric range outside checked source domain"); }
    }
    private static Result checkedPlan(Request r, Resolver resolver) {
        if (r.machine() == null || r.kind() == null || r.input() == null)
            return result(Status.UNSUPPORTED, "unresolved machine/input kind");
        if (r.kind() == Kind.SMALL_RICH_NORMAL) return result(Status.NO_ROW, "source TODO returns no row");
        Input in = r.input();
        if (in.material() == null || in.crushingTarget() == null || in.bonus() == null || in.hexorium() == null)
            return result(Status.UNSUPPORTED, "unresolved material/bonus/Hexorium context");
        if (in.crushingUnitsPerU() < 0 || in.oreMultiplier() < 0 || in.processingMultiplier() < 0)
            return result(Status.UNSUPPORTED, "negative runtime amount/multiplier");
        if (r.hexoriumAdjustedHost() && r.kind() != Kind.STANDARD && r.kind() != Kind.DENSE)
            return result(Status.UNSUPPORTED, "inconsistent source prefix classification");
        boolean crusher = r.machine() == Machine.CRUSHER;
        long oreMultiplier = in.oreMultiplier();
        if (crusher && r.hexoriumAdjustedHost())
            oreMultiplier += in.hexorium() == Hexorium.BLACK_WHITE ? 1 : in.hexorium() == Hexorium.RED_GREEN_BLUE ? -1 : 0;
        if (oreMultiplier < 0) return result(Status.UNSUPPORTED, "negative adjusted multiplier");
        long multiplier = Math.multiplyExact(oreMultiplier, in.processingMultiplier());
        multiplier = Math.multiplyExact(multiplier, r.kind() == Kind.RAW_BLOCK ? (crusher ? 2 : 9)
                : !crusher && r.kind() == Kind.DENSE ? 2 : 1);
        boolean poor = r.kind() == Kind.POOR;
        if (poor) multiplier = Math.multiplyExact(multiplier, crusher ? 3 : 2);
        long requested = units(in.crushingUnitsPerU(), multiplier, false);
        int count = crusher ? bindStack(requested) : Math.toIntExact(requested); // NO Hammer 64 clamp.
        if (!crusher && count == 0) return result(Status.UNSUPPORTED, "zero Hammer output is not natively representable");
        Choice main = choose(resolver, in.crushingTarget(), count,
                poor ? List.of(Form.CRUSHED_TINY, Form.DUST_TINY)
                        : crusher ? List.of(Form.CRUSHED, Form.DUST, Form.GEM) : List.of(Form.CRUSHED, Form.DUST));
        if (main.availability() != Availability.PRESENT) return fromChoice(main, "primary output");
        long quality = Math.max(1, (long) in.quality() + 1);
        long ticks = Math.multiplyExact(Math.multiplyExact(poor ? 16L : crusher ? 128L : 32L, count), quality);
        if (ticks > Integer.MAX_VALUE) return result(Status.UNSUPPORTED, "source duration int range");
        if (poor) {
            return new Result(Status.ROW, List.of(new Slot(main.stack(), 10000)), Math.max(1, ticks), "poor early return: no gangue/bonus");
        }
        int primarySlots = crusher ? r.kind() == Kind.RAW_BLOCK ? 9 : r.kind() == Kind.DENSE ? 4 : 2 : 1;
        int capacity = crusher ? r.outputCapacity() : Math.max(1, r.outputCapacity());
        // The source accesses the next chance slot even if no bonus is generated.
        if (capacity < primarySlots + (crusher ? 1 : 0))
            return result(Status.UNSUPPORTED, "map too small for source primary/bonus indexing");
        var slots = new ArrayList<>(Collections.nCopies(capacity, new Slot(null, 10000)));
        for (int i = 0; i < primarySlots; i++) slots.set(i, new Slot(main.stack(), 10000));
        int next = primarySlots;
        if (crusher) {
            if (r.kind() == Kind.RAW_BLOCK) {
                ticks = Math.multiplyExact(ticks, 9);
                if (ticks > Integer.MAX_VALUE) return result(Status.UNSUPPORTED, "source raw-block duration int range");
                ticks /= 2;
            }
            if (r.kind() == Kind.DENSE) ticks = Math.multiplyExact(ticks, 2);
            if (ticks > Integer.MAX_VALUE) return result(Status.UNSUPPORTED, "source dense duration int range");
            if (in.bonus().pulverizingCinnabar() == Decision.UNKNOWN)
                return result(Status.UNSUPPORTED, "unknown source Cinnabar tag");
            long chance = bonusChance(in.bonus());
            if (chance > 0) {
                int bonusCount = r.kind() == Kind.RAW_BLOCK ? 9 : r.kind() == Kind.DENSE ? 2 : 1;
                Choice bonus = choose(resolver, CINNABAR, bonusCount, List.of(Form.GEM));
                if (bonus.availability() == Availability.UNKNOWN) return fromChoice(bonus, "bonus gem");
                // Known absence reserves a NULL slot; it does not reject the row or move gangue left.
                slots.set(next++, new Slot(bonus.stack(), chance));
            }
        }
        for (Gangue gangue : r.gangue()) {
            if (gangue.material() == null) return result(Status.UNSUPPORTED, "unresolved gangue identity");
            if (gangue.amount() < 0) return result(Status.UNSUPPORTED, "negative gangue amount");
            // Charge before inspecting capacity/form. Quality is the original gangue material's.
            ticks = Math.addExact(ticks, units(gangue.amount(), Math.multiplyExact(64L, Math.max(1, (long) gangue.quality() + 1)), true));
            if (next < capacity) {
                if (gangue.crushingTarget() == null) return result(Status.UNSUPPORTED, "unresolved gangue target");
                if (gangue.crushingUnitsPerU() < 0) return result(Status.UNSUPPORTED, "negative gangue target amount");
                Choice output = dust(resolver, gangue.crushingTarget(), units(gangue.amount(), gangue.crushingUnitsPerU(), false));
                if (output.availability() == Availability.UNKNOWN) return fromChoice(output, "gangue output");
                if (output.availability() == Availability.PRESENT) slots.set(next++, new Slot(output.stack(), 10000));
                // Unlike bonus, absent gangue output does NOT advance the slot cursor.
            }
        }
        if (ticks > Integer.MAX_VALUE) return result(Status.UNSUPPORTED, "source duration int range");
        return new Result(Status.ROW, slots, Math.max(1, ticks), "source handler row");
    }
    public static long bonusChance(Bonus b) {
        if (b.pulverizingCinnabar() == Decision.UNKNOWN) throw new IllegalArgumentException("unresolved source tag");
        return (b.pulverizingCinnabar() == Decision.YES ? 2500 : 0) + (b.mercuryByproduct() ? 1000 : 0)
                + (b.cinnabarByproduct() ? 500 : 0) + (b.redstoneByproduct() ? 500 : 0);
    }
    private static Choice choose(Resolver resolver, Identity material, int count, List<Form> forms) {
        for (Form form : forms) {
            Availability found = Objects.requireNonNull(resolver.direct(form, material, count));
            if (found == Availability.UNKNOWN) return new Choice(found, null);
            if (found == Availability.PRESENT) return new Choice(found, new Stack(form, material, count));
        }
        return new Choice(Availability.ABSENT, null);
    }
    /** Exact source OM.dust candidates and conditions, including physical blockDust and availability
     * fallback. Existing MaterialRecoveryRules.dust returns only one candidate and omits blockDust,
     * so it cannot implement this source lookup without a LOCAL extension; no global rewrite. */
    private static Choice dust(Resolver resolver, Identity material, long amount) {
        if (amount < U / 72) return new Choice(Availability.ABSENT, null);
        var candidates = new ArrayList<Stack>();
        if (amount >= U * 72) candidates.add(new Stack(Form.BLOCK_DUST, material, bindStack(amount / (U * 9))));
        if (amount >= U && (amount >= U * 16 || amount % U == 0)) candidates.add(new Stack(Form.DUST, material, bindStack(amount / U)));
        if (amount >= U / 4 && (amount >= U * 8 || amount % (U / 4) <= amount % (U / 9)))
            candidates.add(new Stack(Form.DUST_SMALL, material, bindStack(amount / (U / 4))));
        if (amount >= U / 9 && (amount >= U || amount % (U / 9) <= amount % (U / 72)))
            candidates.add(new Stack(Form.DUST_TINY, material, bindStack(amount / (U / 9))));
        candidates.add(new Stack(Form.DUST_DIV72, material, bindStack(amount / (U / 72))));
        for (Stack stack : candidates) {
            Availability found = resolver.direct(stack.form(), stack.material(), stack.count());
            if (found == Availability.UNKNOWN) return new Choice(found, null);
            if (found == Availability.PRESENT) return new Choice(found, stack);
        }
        return new Choice(Availability.ABSENT, null);
    }
    private static int bindStack(long count) { return (int) Math.max(1, Math.min(64, count)); }
    /** UT.Code.units 1677-1684 cancels these exact ratios before multiplying. Keep its safe
     * arithmetic domain; overflow is explicitly unsupported rather than producing wrapped recipes. */
    private static long units(long amount, long targetUnits, boolean roundUp) {
        if (targetUnits == 0) return 0;
        if (targetUnits == U) return amount;
        long original = U;
        if (original % targetUnits == 0) { original /= targetUnits; targetUnits = 1; }
        else if (targetUnits % original == 0) { targetUnits /= original; original = 1; }
        long product = Math.multiplyExact(amount, targetUnits);
        return Math.addExact(product / original, roundUp && product % original > 0 ? 1 : 0);
    }
    private static Result fromChoice(Choice choice, String reason) {
        return result(choice.availability() == Availability.UNKNOWN ? Status.UNSUPPORTED : Status.NO_ROW, reason);
    }
    private static Result result(Status status, String reason) { return new Result(status, List.of(), 0, reason); }
}
