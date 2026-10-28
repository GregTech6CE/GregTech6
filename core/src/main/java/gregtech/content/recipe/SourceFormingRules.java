package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.SourceLatheMaterialProfiles;
import java.math.BigInteger;
import java.util.List;

/** Six generic forming pairs from GT6 Loader_Recipes_Handlers:298–316 at 3703e403. */
public final class SourceFormingRules {
    private SourceFormingRules() {}

    public static final long EU_PER_TICK = 16;

    public enum Machine {
        ROLL_BENDER,
        CLUSTER_MILL,
        ROLL_FORMER
    }

    public record Rule(
            Machine machine,
            MaterialPrefix input,
            int inputCount,
            MaterialPrefix output,
            int outputCount,
            int easyTicks,
            int multiplier) {}

    public record Decision(boolean allowed, long ticks, boolean sourceProfile) {}

    private static final class Rows {
        static final List<Rule> ALL = List.of(
                new Rule(Machine.ROLL_BENDER, MaterialPrefix.plate, 1, MaterialPrefix.plateCurved, 1, 16, 256),
                new Rule(Machine.ROLL_BENDER, MaterialPrefix.stick, 1, MaterialPrefix.ring, 2, 4, 256),
                new Rule(Machine.ROLL_BENDER, MaterialPrefix.stickLong, 1, MaterialPrefix.spring, 1, 16, 256),
                new Rule(Machine.ROLL_BENDER, MaterialPrefix.wireFine, 2, MaterialPrefix.springSmall, 1, 4, 256),
                new Rule(Machine.CLUSTER_MILL, MaterialPrefix.plate, 1, MaterialPrefix.foil, 4, 16, 256),
                new Rule(Machine.ROLL_FORMER, MaterialPrefix.plate, 1, MaterialPrefix.railGt, 4, 16, 64));
    }

    public static List<Rule> rules() {
        return Rows.ALL;
    }

    public static Decision evaluate(GTMaterial material, Rule rule) {
        if (material == null
                || !material.resolve().isValid()
                || material.resolve().has(MaterialProperty.ANTIMATTER)) return new Decision(false, 0, false);
        var profile = SourceLatheMaterialProfiles.findForForming(material);
        if (profile == null)
            return material.resolve().has(MaterialProperty.SMITHABLE)
                    ? new Decision(true, rule.easyTicks(), false)
                    : new Decision(false, 0, false);
        if (!profile.smithable()) return new Decision(false, 0, true);
        boolean allowed = rule.machine() != Machine.ROLL_FORMER || !profile.coated();
        return new Decision(
                allowed, profile.easy() ? rule.easyTicks() : hardTicks(rule, material.getToolQuality()), true);
    }

    /** Multiply complete input/output quantities before applying the exact source ceiling. */
    public static long hardTicks(Rule rule, int quality) {
        BigInteger input =
                BigInteger.valueOf(rule.input().getMaterialWeight()).multiply(BigInteger.valueOf(rule.inputCount()));
        BigInteger output =
                BigInteger.valueOf(rule.output().getMaterialWeight()).multiply(BigInteger.valueOf(rule.outputCount()));
        BigInteger numerator = input.max(output)
                .multiply(BigInteger.valueOf(rule.multiplier()))
                .multiply(BigInteger.valueOf((long) quality + 1));
        BigInteger unit = BigInteger.valueOf(GTValues.U);
        return numerator
                .add(unit.subtract(BigInteger.ONE))
                .divide(unit)
                .max(BigInteger.ONE)
                .min(BigInteger.valueOf(Long.MAX_VALUE))
                .longValue();
    }
}
