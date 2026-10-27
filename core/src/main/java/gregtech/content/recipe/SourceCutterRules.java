package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.SourceLatheMaterialProfiles;
import gregtech.engine.prefix.BlockMaterialPrefix;
import java.util.List;

/** Generic Cutter handlers at GT6 3703e403, Loader_Recipes_Handlers:628–652. */
public final class SourceCutterRules {
    private SourceCutterRules() {}
    public record Rule(MaterialPrefix input, BlockMaterialPrefix blockInput, MaterialPrefix output,
                       int outputCount, int baseTicks, long eut) {
        public long inputUnits() {
            return input != null ? input.getMaterialWeight() : blockInput.getMaterialWeight();
        }
    }
    public record Coolant(String field, int multiplier) {}
    public record Decision(boolean allowed, long ticks, int fluidAmount, boolean sourceProfile) {}
    // The source array has five entries but its loop is i < 4: no LubRoCant or mineral water.
    private static final List<Coolant> COOLANTS = List.of(new Coolant("Water", 4),
            new Coolant("SpDew", 4), new Coolant("DistW", 3), new Coolant("Lubricant", 1));
    public static List<Coolant> sourceCoolants() { return COOLANTS; }

    private static final class Rows {
        // Preserve the old nineteen-row order; the recovered block route is appended.
        static final List<Rule> ALL = List.of(
                new Rule(MaterialPrefix.stickLong, null, MaterialPrefix.stick, 2, 16, 32),
                new Rule(MaterialPrefix.stick, null, MaterialPrefix.bolt, 4, 48, 32),
                new Rule(MaterialPrefix.plate, null, MaterialPrefix.plateTiny, 8, 64, 32),
                new Rule(MaterialPrefix.plateGem, null, MaterialPrefix.plateGemTiny, 8, 64, 32),
                new Rule(MaterialPrefix.gemChipped, null, MaterialPrefix.plateGemTiny, 2, 16, 32),
                new Rule(MaterialPrefix.gemFlawed, null, MaterialPrefix.plateGemTiny, 4, 32, 32),
                new Rule(MaterialPrefix.gem, null, MaterialPrefix.plateGem, 1, 16, 96),
                new Rule(MaterialPrefix.gemFlawless, null, MaterialPrefix.plateGem, 2, 16, 96),
                new Rule(MaterialPrefix.gemExquisite, null, MaterialPrefix.plateGem, 4, 48, 96),
                new Rule(MaterialPrefix.gemLegendary, null, MaterialPrefix.plateGem, 8, 112, 96),
                new Rule(MaterialPrefix.bouleGt, null, MaterialPrefix.plateGem, 4, 48, 32),
                new Rule(MaterialPrefix.ingotDouble, null, MaterialPrefix.ingot, 2, 16, 32),
                new Rule(MaterialPrefix.ingotTriple, null, MaterialPrefix.ingot, 3, 32, 32),
                new Rule(MaterialPrefix.ingotQuadruple, null, MaterialPrefix.ingot, 4, 48, 32),
                new Rule(MaterialPrefix.ingotQuintuple, null, MaterialPrefix.ingot, 5, 64, 32),
                new Rule(MaterialPrefix.plateDouble, null, MaterialPrefix.plate, 2, 16, 32),
                new Rule(MaterialPrefix.plateTriple, null, MaterialPrefix.plate, 3, 32, 32),
                new Rule(MaterialPrefix.plateQuadruple, null, MaterialPrefix.plate, 4, 48, 32),
                new Rule(MaterialPrefix.plateQuintuple, null, MaterialPrefix.plate, 5, 64, 32),
                new Rule(null, BlockMaterialPrefix.blockSolid, MaterialPrefix.plate, 8, 112, 32));
    }
    public static List<Rule> rules() { return Rows.ALL; }

    /** Native adapter must still require real input/output holders and an available coolant binding. */
    public static Decision evaluate(GTMaterial material, Rule rule, Coolant coolant) {
        if (material == null || !material.resolve().isValid()
                || material.resolve().has(MaterialProperty.ANTIMATTER))
            return new Decision(false, 0, 0, false);
        var profile = SourceLatheMaterialProfiles.findForCutter(material);
        if (profile == null && sourceStonePair(material, rule))
            profile = SourceLatheMaterialProfiles.findForOfficialStoneCutter(material);
        if (profile == null) {
            // Preserve the old water-only base costs for unknown, redirected or overridden contexts.
            return new Decision(rule.blockInput() == null && coolant.field().equals("Water"),
                    rule.baseTicks(), Math.max(16, rule.baseTicks()), false);
        }
        long ticks = (long) rule.baseTicks() * coolant.multiplier();
        long amount = ticks * ((long) material.getToolQuality() + 1);
        // FL.amount binds positive quantities to int. Invalid addon quality must not yield a free-fluid row.
        int fluid = (int) Math.max(0, Math.min(Integer.MAX_VALUE, amount));
        return new Decision(!profile.coated() && fluid > 0, ticks, fluid, true);
    }
    /** OP205–207 and OP196/198 prove only these forms in the bounded official-STONE recovery. */
    private static boolean sourceStonePair(GTMaterial material, Rule rule) {
        if (rule.blockInput() != null) return false;
        if (rule.equals(Rows.ALL.get(0)) || rule.equals(Rows.ALL.get(1)))
            return gregtech.content.data.generated.MaterialForms.has(material, "STICKS");
        return rule.equals(Rows.ALL.get(2))
                && gregtech.content.data.generated.MaterialForms.has(material, "PLATES")
                && (gregtech.content.data.generated.MaterialForms.has(material, "INGOTS")
                    || !gregtech.content.data.generated.MaterialForms.has(material, "GEMS"));
    }

}
