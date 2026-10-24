package gregtech.content.recipe;
import gregtech.internal.material.GTMaterial;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.material.MaterialProperty;

import java.util.ArrayList;
import java.util.List;
/** Proposed bounded source-policy adapter; not yet installed.
 * Built-in FURNACE uses reviewed tri-state source evidence; unknown/addon legacy behavior remains unchanged
 * until addon declarations expose an explicit furnace permission. This does not remove addon rows.
 */
public final class OreSourcePolicy {
    private OreSourcePolicy() {}
    public static boolean furnaceInput(GTMaterial material) {
        if(material==null||!material.isValid())return false;
        var resolved=material.resolve();
        return ReviewedOreSourceProfile.decision(resolved, ReviewedOreSourceProfile.Rule.FURNACE)
                .orLegacy(resolved.has(MaterialProperty.MELTING));
    }
    /** Reviewed native inputs belong to the later source furnace listener, not legacy dust-first producers. */
    public static boolean usesReviewedFurnaceListener(GTMaterial material) {
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
    /** Bounded positive source flags missing from legacy imports; never infer negatives from absence.
     * MT original OREMATS.Fe2O3(9104), Magnetite(9122) / paired w declarations: MAGNETIC_PASSIVE.
     * Recipe-local overlay only: no global material flags, addon rows or identities changed.
     */
    private static boolean magneticClass(GTMaterial material) {
        var m=material.resolve();
        if(GTMaterialRegistry.isCoreMaterial(m)&&!gregtech.internal.addon.AddonMaterials.owns(m)) {
            if((m.getId()==9104&&m.getName().equals("Hematite"))
                    ||(m.getId()==9122&&m.getName().equals("Magnetite")))return true;
        }
        return m.has(MaterialProperty.MAGNETIC); // outside reviewed positives retains legacy
    }

}
