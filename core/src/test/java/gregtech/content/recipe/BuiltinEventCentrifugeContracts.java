package gregtech.content.recipe;

import java.util.Arrays;
import java.util.List;

import static gregtech.content.recipe.SourceCentrifugeBuiltinEvents.*;
import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** Pure event policy only. Actual loader timing and native capability probes need GameTests. */
public final class BuiltinEventCentrifugeContracts {
    private static final Fact Y = Fact.YES, N = Fact.NO, U = Fact.UNKNOWN;
    private static final IngredientFacts PLAIN = new IngredientFacts(Y, N, N, N, N, N, N);

    private BuiltinEventCentrifugeContracts() {}

    private static Facts defaults(Instance input) {
        return sourceDefaults(input, Y, PLAIN, Shell.ABSENT);
    }

    private static Facts withSlime(Facts facts, SlimeFacts slime) {
        return new Facts(facts.reviewedPlainHolder(), facts.inputCount(), facts.membership(),
                facts.meatSubstitute(), facts.big(), facts.shell(), slime, facts.ingredient());
    }

    private static void decision(Instance input, Facts facts, Decision expected, String reason) {
        Result result = decide(input, facts);
        equal(expected, result.decision(), reason);
        equal(expected == Decision.PLAN, result.plan() != null, "only a positive decision carries a plan");
    }

    public static void run() {
        defaultPlans();
        exclusionPrecedence();
        capabilityFacts();
    }

    private static void defaultPlans() {
        equal(List.of(Instance.VANILLA_EGG, Instance.OWN_EGG, Instance.VANILLA_SLIME,
                Instance.OWN_GOO, Instance.OWN_BORAX), instances(), "five finite source instances");
        String[] ids = {"minecraft:egg", "gt6ce:egg", "minecraft:slime_ball", "gt6ce:sticky_goo", "gt6ce:slimeball"};
        int[] metadata = {-1, 1051, -1, 12098, 12099};
        for (int i = 0; i < ids.length; i++) {
            Instance input = instances().get(i);
            equal(input, instance(ids[i]), "exact registry identity");
            equal(metadata[i], input.originalMeta(), "original instance metadata");
            decision(input, defaults(input), input == Instance.OWN_BORAX ? Decision.BLOCK : Decision.PLAN,
                    "four positive default events and excluded Borax");
            Facts facts = defaults(input);
            for (int count : new int[]{0, 2, Integer.MAX_VALUE}) {
                decision(input, new Facts(Y, count, Y, N, N, Shell.ABSENT, facts.slime(), PLAIN),
                        Decision.DEFER, "do not manufacture a normalized count-one input");
            }
            for (Fact holder : new Fact[]{N, U, null}) {
                decision(input, new Facts(holder, 1, Y, N, N, Shell.ABSENT, facts.slime(), PLAIN),
                        Decision.DEFER, "missing or unreviewed holder");
            }
            decision(input, new Facts(Y, 1, N, N, N, Shell.ABSENT, facts.slime(), PLAIN),
                    Decision.NOT_REGISTERED, "source membership absent");
            decision(input, new Facts(Y, 1, U, N, N, Shell.ABSENT, facts.slime(), PLAIN),
                    Decision.DEFER, "source membership unknown");
        }
        for (String id : new String[]{"foreign:egg", "forge:eggs", "gt6ce:big_egg", "minecraft:slimeball", null}) {
            equal(null, instance(id), "no tag, guessed alias, or foreign instance");
        }
        Plan egg = new Plan(1, true, 16, 64, List.of(new ItemAmount(ItemRole.EGG_WHITE, 1),
                new ItemAmount(ItemRole.EGG_YOLK, 1), new ItemAmount(ItemRole.BONE_MEAL, 1)),
                List.of(), List.of(), List.of(10000L, 10000L, 10000L));
        for (Instance input : List.of(Instance.VANILLA_EGG, Instance.OWN_EGG)) {
            equal(egg, decide(input, defaults(input)).plan(), "exact ordered egg outputs");
        }
        equal(new Plan(1, true, 16, 64, List.of(), List.of(),
                        List.of(new FluidAmount(FluidRole.LATEX, 72), new FluidAmount(FluidRole.GLUE, 250)), List.of()),
                decide(Instance.VANILLA_SLIME, defaults(Instance.VANILLA_SLIME)).plan(), "generic slime source fluid amounts");
        equal(new Plan(1, true, 16, 64, List.of(), List.of(),
                        List.of(new FluidAmount(FluidRole.GLUE, 250)), List.of()),
                decide(Instance.OWN_GOO, defaults(Instance.OWN_GOO)).plan(), "animal goo has no Latex");
        decision(null, null, Decision.DEFER, "unknown instance");
    }

    private static void exclusionPrecedence() {
        Facts slime = defaults(Instance.VANILLA_SLIME);
        Fact[] states = {N, Y, U};
        // Known rejection wins over unknown; otherwise any unknown must defer, never guess absence.
        for (int code = 0; code < 2187; code++) {
            Fact[] flags = new Fact[7];
            int value = code;
            boolean reject = false, unknown = false;
            for (int i = 0; i < flags.length; i++) {
                flags[i] = states[value % 3];
                value /= 3;
                reject |= flags[i] == Y;
                unknown |= flags[i] == U;
            }
            decision(Instance.VANILLA_SLIME, withSlime(slime, new SlimeFacts(flags[0], flags[1], flags[2],
                    flags[3], flags[4], flags[5], flags[6])),
                    reject ? Decision.BLOCK : unknown ? Decision.DEFER : Decision.PLAN, "slime tri-state case " + code);
        }
        Facts goo = defaults(Instance.OWN_GOO);
        decision(Instance.OWN_GOO, withSlime(goo, null), Decision.PLAN, "animal route does not inherit generic exclusions");
        decision(Instance.OWN_BORAX, withSlime(defaults(Instance.OWN_BORAX), slime.slime()),
                Decision.DEFER, "contradicted Borax facts cannot create a fifth positive event");
        equal(N, ingredable(new IngredientFacts(Y, N, N, N, Y, Y, Y)), "durability rejection precedes empty exception");
        equal(U, ingredable(new IngredientFacts(Y, N, N, N, U, Y, Y)), "unknown durability cannot be bypassed");
        equal(Y, ingredable(new IngredientFacts(Y, N, N, N, N, Y, U)), "exact empty precedes wildcard uncertainty");
        equal(N, ingredable(new IngredientFacts(Y, N, N, N, N, N, Y)), "wildcard-only ingredient rejected");
        equal(U, ingredable(new IngredientFacts(Y, N, N, N, N, U, Y)), "unknown exact exception cannot be skipped");
        Facts egg = defaults(Instance.VANILLA_EGG);
        decision(Instance.VANILLA_EGG, new Facts(Y, 1, Y, Y, Y, Shell.UNKNOWN, null, null),
                Decision.BLOCK, "meat substitute blocks before Big/provider uncertainty");
        decision(Instance.VANILLA_EGG, new Facts(Y, 1, Y, N, N, Shell.UNKNOWN, egg.slime(), PLAIN),
                Decision.DEFER, "unknown shell provider cannot silently use bone meal");
        Facts big = new Facts(Y, 1, Y, N, Y, Shell.ABSENT, egg.slime(), PLAIN);
        decision(Instance.VANILLA_EGG, big, Decision.DEFER, "finite default projection does not admit Big eggs");
        equal(new Plan(1, true, 16, 256, List.of(new ItemAmount(ItemRole.EGG_WHITE, 4),
                        new ItemAmount(ItemRole.EGG_YOLK, 4), new ItemAmount(ItemRole.BONE_MEAL, 4)),
                        List.of(), List.of(), List.of(10000L, 10000L, 10000L)),
                sourceRule(Family.EGG, big).plan(), "source Big semantics consume one, output four");
    }

    private static void capabilityFacts() {
        equal(N, positiveFluidCapacity(N, true, null), "observed absent capability");
        equal(N, positiveFluidCapacity(Y, true, List.of()), "observed zero-tank handler");
        equal(N, positiveFluidCapacity(Y, true, List.of(0L, 0L)), "observed zero capacities");
        equal(Y, positiveFluidCapacity(Y, true, List.of(0L, Long.MAX_VALUE)), "positive capacity without overflow");
        equal(U, positiveFluidCapacity(U, true, List.of()), "unknown capability");
        equal(U, positiveFluidCapacity(Y, false, List.of(1L)), "failed probe cannot reuse stale values");
        equal(U, positiveFluidCapacity(N, false, List.of()), "failed absence probe");
        equal(U, positiveFluidCapacity(Y, true, List.of(1L, -1L)), "malformed capacity observation");
        equal(U, positiveFluidCapacity(Y, true, Arrays.asList((Long) null)), "incomplete tank observation");
        equal(U, positiveFluidCapacity(N, true, List.of(0L)), "contradictory absent capability with tanks");
        for (Fact capacity : new Fact[]{Y, U, N}) {
            Facts slime = defaults(Instance.VANILLA_SLIME);
            Facts facts = new Facts(Y, 1, Y, N, N, Shell.ABSENT, slime.slime(),
                    new IngredientFacts(Y, N, capacity, N, N, N, N));
            decision(Instance.VANILLA_SLIME, facts,
                    capacity == Y ? Decision.BLOCK : capacity == U ? Decision.DEFER : Decision.PLAN,
                    "native capacity fact propagates to plan");
        }
    }
}
