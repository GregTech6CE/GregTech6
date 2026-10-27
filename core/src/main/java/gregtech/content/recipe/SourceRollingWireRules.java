package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.SourceLatheMaterialProfiles;
import gregtech.engine.prefix.BlockMaterialPrefix;
import java.math.BigInteger;
import java.util.List;

/** Available RollingMill and Wiremill pairs from GT6 Handlers:261–295 at 3703e403. */
public final class SourceRollingWireRules {
    private SourceRollingWireRules() {}
    public static final long EU_PER_TICK = 16;
    public enum Machine { ROLLING_MILL, WIREMILL }
    public record Rule(Machine machine, MaterialPrefix input, BlockMaterialPrefix blockInput,
                       MaterialPrefix output, int outputCount, int easyTicks, int legacyTicks, int multiplier) {
        public long inputUnits() { return input == null ? blockInput.getMaterialWeight() : input.getMaterialWeight(); }
    }
    public record Decision(boolean allowed, long ticks, boolean sourceProfile) {}
    private static final class Rows {
        // Existing seven rolling and two fine-wire pairs retain order. Only the known block route is new.
        static final List<Rule> ALL = List.of(
                new Rule(Machine.ROLLING_MILL, MaterialPrefix.nugget, null, MaterialPrefix.plateTiny, 1, 1, 2, 256),
                new Rule(Machine.ROLLING_MILL, MaterialPrefix.ingot, null, MaterialPrefix.plate, 1, 16, 16, 256),
                new Rule(Machine.ROLLING_MILL, MaterialPrefix.ingotDouble, null, MaterialPrefix.plateDouble, 1, 32, 32, 256),
                new Rule(Machine.ROLLING_MILL, MaterialPrefix.ingotTriple, null, MaterialPrefix.plateTriple, 1, 48, 48, 256),
                new Rule(Machine.ROLLING_MILL, MaterialPrefix.ingotQuadruple, null, MaterialPrefix.plateQuadruple, 1, 64, 64, 256),
                new Rule(Machine.ROLLING_MILL, MaterialPrefix.ingotQuintuple, null, MaterialPrefix.plateQuintuple, 1, 80, 80, 256),
                new Rule(Machine.ROLLING_MILL, MaterialPrefix.plateCurved, null, MaterialPrefix.plate, 1, 16, 16, 256),
                new Rule(Machine.WIREMILL, MaterialPrefix.stick, null, MaterialPrefix.wireFine, 4, 8, 8, 128),
                new Rule(Machine.WIREMILL, MaterialPrefix.stickLong, null, MaterialPrefix.wireFine, 8, 16, 16, 128),
                new Rule(Machine.ROLLING_MILL, null, BlockMaterialPrefix.blockSolid, MaterialPrefix.plateDense, 1, 144, 0, 256));
    }
    public static List<Rule> rules() { return Rows.ALL; }

    public static Decision evaluate(GTMaterial material, Rule rule) {
        if (material == null || !material.resolve().isValid() || material.resolve().has(MaterialProperty.ANTIMATTER)
                || !material.resolve().has(MaterialProperty.SMITHABLE)) return new Decision(false, 0, false);
        var profile = SourceLatheMaterialProfiles.findForRollingWire(material);
        if (profile == null) return new Decision(rule.blockInput() == null, rule.legacyTicks(), false);
        return new Decision(!profile.coated(), profile.easy() ? rule.easyTicks()
                : hardTicks(rule.inputUnits(), rule.output().getMaterialWeight() * rule.outputCount(),
                        rule.multiplier(), material.getToolQuality()), true);
    }

    /** Physical wire remains owned by its native producer, including its exact legacy fallback policy. */
    public static Decision physicalWire(GTMaterial material, boolean legacyAllowed, long legacyTicks, boolean netheriteFamily) {
        if (material == null || !material.resolve().isValid()) return new Decision(false, 0, false);
        var profile = SourceLatheMaterialProfiles.findForRollingWire(material);
        if (profile == null) return new Decision(legacyAllowed, legacyTicks, false);
        // Only Netherite has independently proven source admission beyond the legacy family policy.
        boolean provenAddition = netheriteFamily && material == gregtech.content.material.Materials.Netherite
                && material.getId() == 8745 && material.getName().equals("Netherite");
        return new Decision((legacyAllowed || provenAddition) && material.has(MaterialProperty.SMITHABLE)
                && !material.has(MaterialProperty.ANTIMATTER) && !profile.coated(), profile.easy() ? 16 : hardTicks(GTValues.U, GTValues.U, 128, material.getToolQuality()), true);
    }

    public static long hardTicks(long inputUnits, long outputUnits, int multiplier, int quality) {
        BigInteger numerator = BigInteger.valueOf(inputUnits).max(BigInteger.valueOf(outputUnits))
                .multiply(BigInteger.valueOf(multiplier)).multiply(BigInteger.valueOf((long) quality + 1));
        BigInteger unit = BigInteger.valueOf(GTValues.U);
        return numerator.add(unit.subtract(BigInteger.ONE)).divide(unit)
                .max(BigInteger.ONE).min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }
}
