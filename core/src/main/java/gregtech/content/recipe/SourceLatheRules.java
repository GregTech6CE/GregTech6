package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.SourceLatheMaterialProfiles;
import java.math.BigInteger;
import java.util.List;

/** The eleven generic Lathe pairs in GT6 Loader_Recipes_Handlers:371–393 (3703e403). */
public final class SourceLatheRules {
    private SourceLatheRules() {}
    public static final long EU_PER_TICK = 16;
    public record Rule(MaterialPrefix input, MaterialPrefix output, int outputCount, int easyTicks,
                       boolean excludesLayered, boolean requiresNoLens) {}
    public record Decision(boolean allowed, long ticks, boolean sourceProfile) {}

    /** Lazy prefix binding: built-in material linking does not require prefix bootstrap. */
    private static final class Rows {
        // Retain legacy ten-pair order; the newly recovered conditional pair is appended.
        static final List<Rule> ALL = List.of(
                new Rule(MaterialPrefix.bolt, MaterialPrefix.screw, 1, 2, false, false),
                new Rule(MaterialPrefix.nugget, MaterialPrefix.round, 1, 1, true, false),
                new Rule(MaterialPrefix.plateGem, MaterialPrefix.lens, 1, 16, false, false),
                new Rule(MaterialPrefix.lens, MaterialPrefix.ring, 1, 16, false, false),
                new Rule(MaterialPrefix.bouleGt, MaterialPrefix.stickLong, 3, 64, true, false),
                new Rule(MaterialPrefix.gemChipped, MaterialPrefix.bolt, 1, 4, true, false),
                new Rule(MaterialPrefix.gemFlawed, MaterialPrefix.bolt, 3, 8, true, false),
                new Rule(MaterialPrefix.billet, MaterialPrefix.stick, 1, 16, true, false),
                new Rule(MaterialPrefix.ingot, MaterialPrefix.stick, 1, 16, true, false),
                new Rule(MaterialPrefix.gem, MaterialPrefix.stick, 1, 16, true, false),
                new Rule(MaterialPrefix.plateGem, MaterialPrefix.ring, 1, 16, false, true));
    }

    public static List<Rule> rules() { return Rows.ALL; }

    /** Holder admission and optional pulverization remains belong to the native adapter. */
    public static Decision evaluate(GTMaterial material, Rule rule) {
        if (material == null || !material.resolve().isValid() || material.resolve().has(MaterialProperty.ANTIMATTER))
            return new Decision(false, 0, false);
        var profile = SourceLatheMaterialProfiles.find(material);
        if (profile == null) {
            // Unknown/addon/redirected/overridden materials keep their old ten fixed-duration rows.
            return new Decision(!rule.requiresNoLens(), rule.input() == MaterialPrefix.nugget ? 2 : rule.easyTicks(), false);
        }
        boolean allowed = !profile.coated() && !(rule.excludesLayered() && profile.layered())
                && !(rule.requiresNoLens() && profile.lens());
        long ticks = profile.easy() ? rule.easyTicks() : hardTicks(rule.input().getMaterialWeight(),
                rule.output().getMaterialWeight(), rule.outputCount(), material.getToolQuality());
        return new Decision(allowed, ticks, true);
    }

    /** Ceiling is applied after multiplication, not to a per-quality base duration. */
    public static long hardTicks(long inputUnits, long outputUnits, int outputCount, int quality) {
        BigInteger amount = BigInteger.valueOf(inputUnits).max(
                BigInteger.valueOf(outputUnits).multiply(BigInteger.valueOf(outputCount)));
        BigInteger numerator = amount.multiply(BigInteger.valueOf(64)).multiply(BigInteger.valueOf((long) quality + 1));
        BigInteger unit = BigInteger.valueOf(GTValues.U);
        BigInteger ticks = numerator.add(unit.subtract(BigInteger.ONE)).divide(unit);
        return ticks.max(BigInteger.ONE).min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }
}
