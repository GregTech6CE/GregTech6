package gregtech.content.recipe;
import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.addon.AddonMaterials;
import gregtech.api.material.MaterialProperty;

import java.util.ArrayList;
import java.util.List;
/** Bounded source-policy adapter. Furnace permission is the source INPUT's FURNACE tag,
 * not its smelting target's permission, MELTING, BLACKLISTED_SMELTER or fluid phase.
 * Loader_OreProcessing311/345-348 applies this to the runtime crushing material for ordinary ore;
 * Loader_Recipes_Furnace193 applies it to the original input material for the form listener.
 * Unknown/addon/changed metadata retains each caller's pre-existing compatibility branch.
 */
public final class OreSourcePolicy {
    private OreSourcePolicy() {}
    /** Pure observations used to test default/override boundaries without mutating the registry.
     * Native callers must obtain ownership observations from the actual registry, not matching names. */
    public record FurnaceContext(SourceRecyclingFacts.Identity material, boolean coreMaterial, boolean addonMaterial,
            boolean melting, SourceRecyclingFacts.Identity target, boolean coreTarget, boolean addonTarget,
            long targetUnitsPerU) {}

    public static SourceRecyclingFacts.Decision furnaceDecision(GTMaterial material) {
        var facts = reviewedFurnaceFacts(material);
        return facts == null ? SourceRecyclingFacts.Decision.UNKNOWN : facts.furnace();
    }

    /** Null is unsupported context, not a reviewed source prohibition. Hidden/form availability
     * remain separate caller guards; HIDDEN is not claimed equivalent to the source UNUSED tag. */
    public static SourceRecyclingFacts.Facts reviewedFurnaceFacts(GTMaterial material) {
        if (material == null || !material.isValid()) return null;
        var resolved = material.resolve();
        if (!resolved.isValid()) return null;
        var target = resolved.getTargetSmeltingMaterial();
        if (target == null || !target.isValid()) return null;
        target = target.resolve();
        if (!target.isValid()) return null;
        return reviewedFurnaceContext(new FurnaceContext(
                new SourceRecyclingFacts.Identity(resolved.getId(), resolved.getName()),
                GTMaterialRegistry.isCoreMaterial(resolved), AddonMaterials.owns(resolved),
                resolved.has(MaterialProperty.MELTING),
                new SourceRecyclingFacts.Identity(target.getId(), target.getName()),
                GTMaterialRegistry.isCoreMaterial(target), AddonMaterials.owns(target),
                resolved.getTargetSmeltingAmount()));
    }

    public static SourceRecyclingFacts.Facts reviewedFurnaceContext(FurnaceContext context) {
        if (context == null || context.material() == null || context.target() == null
                || !context.coreMaterial() || context.addonMaterial()
                || !context.coreTarget() || context.addonTarget()) return null;
        var identity = context.material();
        var facts = SourceRecyclingFacts.find(identity.id(), identity.name());
        var defaults = SourceRecyclingFacts.contextDefaults(identity.id(), identity.name());
        return facts != null && defaults != null && defaults.matches(context.melting(),
                context.target().id(), context.target().name(), context.targetUnitsPerU()) ? facts : null;
    }

    public static boolean furnaceInput(GTMaterial material) {
        if(material==null||!material.isValid())return false;
        var resolved=material.resolve();
        var decision = furnaceDecision(resolved);
        if (decision != SourceRecyclingFacts.Decision.UNKNOWN)
            return FurnaceSmeltingRules.reviewedAllows(decision, resolved.has(MaterialProperty.HIDDEN));
        // Preserve this caller's old bounded profile/MELTING compatibility behavior only for UNKNOWN.
        return ReviewedOreSourceProfile.decision(resolved, ReviewedOreSourceProfile.Rule.FURNACE)
                .orLegacy(resolved.has(MaterialProperty.MELTING));
    }
    /** Reviewed native inputs belong to the later source furnace listener, not legacy dust-first producers. */
    public static boolean usesReviewedFurnaceListener(GTMaterial material) {
        if (material == null || !material.isValid()) return false;
        var resolved = material.resolve();
        var decision = furnaceDecision(resolved);
        if (decision != SourceRecyclingFacts.Decision.UNKNOWN)
            return FurnaceSmeltingRules.reviewedAllows(decision, resolved.has(MaterialProperty.HIDDEN));
        // Do not reserve unknown inputs unless their pre-existing reviewed listener path already did.
        return ReviewedOreSourceProfile.decision(material, ReviewedOreSourceProfile.Rule.FURNACE)
                == ReviewedOreSourceProfile.Decision.YES && FurnaceSmeltingRules.allows(material);
    }
    /** Loader_Recipes_Ores389-393: only opposite magnetic class, declaration order unchanged. */
    public static List<GTMaterial> magneticCandidates(GTMaterial material) {
        var out=new ArrayList<GTMaterial>();
        boolean magnetic=magneticClass(material);
        for(var byproduct:material.getByProducts())
            if(byproduct!=null&&byproduct.isValid()&&magneticClass(byproduct)!=magnetic)
                out.add(byproduct);
        return out;
    }
    /** Direct paired-source magnetic positives; omitted/addon identities retain legacy flags. */
    private static boolean magneticClass(GTMaterial material) {
        var m=material.resolve();
        return ReviewedMagneticMaterials.sourcePositive(m)||m.has(MaterialProperty.MAGNETIC);
    }

}
