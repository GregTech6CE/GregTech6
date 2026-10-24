package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.addon.AddonMaterials;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.Set;

/** Bounded per-form source generation facts, not a ban on actual external registrations.
 * Original OP174-178 / W178-182 require CRYSTAL for upper grades; Ice MT1013 / W1020
 * has TRANSPARENT, not CRYSTAL. OP613-615 / W617-619 force only common/flawed/chipped.
 * Existing IDs remain registered. The native resolver checks supported startup external overrides before this table.
 * A later crafting-grade tag alias alone is not a supported Sifting output binding.
 * All omitted identities/forms remain UNKNOWN; the earlier material-wide profile is unchanged.
 */
public final class ReviewedSiftingGemForms {
    private ReviewedSiftingGemForms() {}
    private record Entry(int id,String name,Set<String> absentPrefixes) {}
    private static final Entry[] ENTRIES={
        new Entry(9802,"Ice",Set.of("gemLegendary","gemExquisite","gemFlawless"))
    };
    public static ReviewedOreSourceProfile.Decision decision(GTMaterial material,MaterialPrefix prefix) {
        if(material==null||prefix==null)return ReviewedOreSourceProfile.Decision.UNKNOWN;
        material=material.resolve();
        if(!material.isValid()||!GTMaterialRegistry.isCoreMaterial(material)||AddonMaterials.owns(material))
            return ReviewedOreSourceProfile.Decision.UNKNOWN;
        for(var row:ENTRIES)if(row.id==material.getId()&&row.name.equals(material.getName())
                &&row.absentPrefixes.contains(prefix.getName()))return ReviewedOreSourceProfile.Decision.NO;
        return ReviewedOreSourceProfile.Decision.UNKNOWN;
    }
}
