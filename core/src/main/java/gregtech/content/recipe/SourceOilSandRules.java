package gregtech.content.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static gregtech.api.material.GTValues.U;
import static gregtech.content.recipe.SourceOreCrushingRules.Form;
import static gregtech.content.recipe.SourceOreCrushingRules.Resolver;
import static gregtech.content.recipe.SourceOreCrushingRules.Slot;
import static gregtech.content.recipe.SourceOreCrushingRules.Stack;
import static gregtech.content.recipe.SourceOreWashingRules.Availability;
import static gregtech.content.recipe.SourceRecyclingFacts.Decision;
import static gregtech.content.recipe.SourceRecyclingFacts.Identity;

/**
 * The OilSand ore-registration Centrifuge event, not generic material decomposition.
 * Pinned GT6 3703e40308c8c030763fd6297dea8b210d2a77b1: Loader_OreProcessing 196-197/296-303,
 * OP 59/112-125/133/345/653-710, MT 1199/1618, OM 460-467.
 * Native ownership, input/host-mode provenance and Oil_Normal binding are caller responsibilities.
 * Crushing rules supply only direct-stack DTOs; their gates, targets and costs do not apply here.
 */
public final class SourceOilSandRules {
    private SourceOilSandRules() {}

    public static final Identity OIL_SAND = new Identity(9851, "OilSand");
    public static final Identity SAND = new Identity(8100, "Sand");
    public static final long WEIGHT_STANDARD = 2 * U, WEIGHT_RAW_ITEM = 2 * U,
            WEIGHT_DENSE = 4 * U, WEIGHT_RAW_BLOCK = 18 * U;
    public static final int OUTPUT_SLOTS = 6;

    public enum Status { ROW, UNSUPPORTED }
    /** Direct prefix byproduct: no crushing/pulverization target or quality conversion. */
    public record Byproduct(Identity material, long amount) {}
    public record Request(long prefixWeight, List<Byproduct> byproducts) {
        public Request { byproducts = List.copyOf(byproducts); }
    }
    /** The six source call positions, including known absent solids, are retained before native
     * trailing-null normalization. A fluid-only row remains valid when all solid forms are absent. */
    public record Result(Status status, List<Slot> slots, long ticks, long oilMb, String reason) {
        public Result { slots = List.copyOf(slots); }
        public int power() { return 16; }
        public boolean optimize() { return true; }
    }
    /** Source prefix facts only; neither an item matcher nor proof of a broken native mode. */
    public record HostProfile(String sourcePrefix, long prefixWeight, List<Byproduct> byproducts) {
        public HostProfile { byproducts = List.copyOf(byproducts); }
    }

    public static boolean isOilSand(int id, String canonicalName) {
        return OIL_SAND.matches(id, canonicalName);
    }
    /** Unlike Crusher, this event does not exclude DUST_ORE. The source ORE/container predicates
     * must be independently proven; a non-null string by itself does not establish a prefix. */
    public static boolean eligible(boolean orePrefix, boolean container, String sourcePrefix) {
        if (!orePrefix || container || sourcePrefix == null) return false;
        return switch (sourcePrefix) {
            case "oreBedrock", "orePoor", "oreSmall", "oreRich", "oreNormal" -> false;
            default -> true;
        };
    }
    public static HostProfile hostProfile(String nativeHostId) {
        if (nativeHostId == null) return null;
        String sediment = switch (nativeHostId) {
            case "sand" -> "oreSand";
            case "red_sand" -> "oreRedSand";
            case "gravel" -> "oreGravel";
            default -> null;
        };
        // OP 115/117/118: 2U, DUST_ORE, and no byproduct assignment in the source init closure.
        if (sediment != null) return new HostProfile(sediment, WEIGHT_STANDARD, List.of());
        var host = SourceOreCrushingFacts.hostProfile(nativeHostId);
        return host == null ? null : new HostProfile(host.sourcePrefix(), WEIGHT_STANDARD,
                List.of(new Byproduct(host.gangueMaterial(), host.gangueAmount())));
    }
    public static Decision hostDecision(String nativeHostId) {
        if (hostProfile(nativeHostId) != null) return Decision.YES;
        return "bedrock".equals(nativeHostId) ? Decision.NO : Decision.UNKNOWN;
    }
    /** False means unproved mode, not source NO. Sediment, Deepslate and Gneiss broken modes are
     * not inferred merely from their ordinary prefix definitions. */
    public static boolean hasBrokenSourcePair(String nativeHostId) {
        return SourceOreCrushingFacts.hasBrokenSourcePair(nativeHostId);
    }

    public static Result plan(Request request, Resolver resolver) {
        if (request == null || resolver == null) return unsupported("missing planning context");
        if (request.prefixWeight() <= 0) return unsupported("non-positive prefix weight");
        try {
            long ticks = unitsUp(request.prefixWeight(), 64);
            long oilMb = unitsUp(request.prefixWeight(), 250);
            if (ticks > Integer.MAX_VALUE || oilMb > Integer.MAX_VALUE)
                return unsupported("native duration/fluid range");
            var slots = new ArrayList<>(Collections.nCopies(OUTPUT_SLOTS, new Slot(null, 10000)));
            Choice sand = dust(resolver, SAND, Math.multiplyExact(request.prefixWeight(), 2));
            if (sand.availability() == Availability.UNKNOWN) return unsupported("unknown Sand form");
            slots.set(0, new Slot(sand.stack(), 10000));
            // Exactly byproduct(0)..byproduct(4), not all prefix byproducts or material byproducts.
            for (int i = 0; i < Math.min(5, request.byproducts().size()); i++) {
                Byproduct byproduct = request.byproducts().get(i);
                // Keep every possible OM.dust multiply (up to amount*72) in the exact source
                // domain. Extreme external amounts are unsupported, not overflow-derived rows.
                if (byproduct.material() == null || byproduct.amount() < 0 || byproduct.amount() > Long.MAX_VALUE / 72)
                    return unsupported("unresolved/out-of-range prefix byproduct");
                Choice output = dust(resolver, byproduct.material(), byproduct.amount());
                if (output.availability() == Availability.UNKNOWN) return unsupported("unknown byproduct form");
                slots.set(i + 1, new Slot(output.stack(), 10000));
            }
            return new Result(Status.ROW, slots, ticks, oilMb, "source OilSand event");
        } catch (ArithmeticException overflow) {
            return unsupported("numeric range outside checked source domain");
        }
    }

    private record Choice(Availability availability, Stack stack) {}
    /** Local OM.dust lookup: whole/small/tiny/div72 availability fallback and physical blockDust.
     * It resolves the supplied material directly, without either ore target transformation. */
    private static Choice dust(Resolver resolver, Identity material, long amount) {
        if (amount < U / 72) return new Choice(Availability.ABSENT, null);
        var candidates = new ArrayList<Stack>();
        if (amount >= U * 72) candidates.add(new Stack(Form.BLOCK_DUST, material, bindStack(amount / (U * 9))));
        if (amount >= U && (amount >= U * 16 || amount % U == 0))
            candidates.add(new Stack(Form.DUST, material, bindStack(amount / U)));
        if (amount >= U / 4 && (amount >= U * 8 || amount % (U / 4) <= amount % (U / 9)))
            candidates.add(new Stack(Form.DUST_SMALL, material, bindStack(amount / (U / 4))));
        if (amount >= U / 9 && (amount >= U || amount % (U / 9) <= amount % (U / 72)))
            candidates.add(new Stack(Form.DUST_TINY, material, bindStack(amount / (U / 9))));
        candidates.add(new Stack(Form.DUST_DIV72, material, bindStack(amount / (U / 72))));
        for (Stack stack : candidates) {
            Availability found = Objects.requireNonNull(resolver.direct(stack.form(), stack.material(), stack.count()));
            if (found == Availability.UNKNOWN) return new Choice(found, null);
            if (found == Availability.PRESENT) return new Choice(found, stack);
        }
        return new Choice(Availability.ABSENT, null);
    }
    private static int bindStack(long count) { return (int) Math.max(1, Math.min(64, count)); }
    /** UT.Code.units 1677-1684 cancels an exact ratio before multiplication. */
    private static long unitsUp(long amount, long target) {
        long divisor = U;
        if (divisor % target == 0) { divisor /= target; target = 1; }
        else if (target % divisor == 0) { target /= divisor; divisor = 1; }
        long product = Math.multiplyExact(amount, target);
        return Math.addExact(product / divisor, product % divisor == 0 ? 0 : 1);
    }
    private static Result unsupported(String reason) {
        return new Result(Status.UNSUPPORTED, List.of(), 0, 0, reason);
    }
}
