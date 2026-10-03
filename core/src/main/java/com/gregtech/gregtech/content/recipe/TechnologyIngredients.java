package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.MaterialGroups;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.List;

/** Exact ANY groups used by MultiItemRandomTools crafting rows. */
public final class TechnologyIngredients {
    private TechnologyIngredients() {}
    public record Family(String tag, MaterialPrefix prefix, GTMaterial group) {
        public boolean accepts(MaterialPrefix form, GTMaterial material) {
            return form == prefix && group.getReRegistrations().stream().anyMatch(m -> m.resolve() == material.resolve());
        }
    }
    public static final List<Family> FAMILIES = List.of(
        new Family("any_plastic_curved_plate",MaterialPrefix.plateCurved,MaterialGroups.Plastic),
        new Family("any_rubber_hammer_head",MaterialPrefix.toolHeadHammer,MaterialGroups.Rubber),
        new Family("any_steel_rod",MaterialPrefix.stick,MaterialGroups.Steel),
        new Family("any_diamond_dust",MaterialPrefix.dust,MaterialGroups.Diamond),
        new Family("any_wood_bolt",MaterialPrefix.bolt,MaterialGroups.Wood),
        new Family("any_phosphorus_dust",MaterialPrefix.dust,MaterialGroups.Phosphorus),
        new Family("any_phosphorus_dust_small",MaterialPrefix.dustSmall,MaterialGroups.Phosphorus)
    );
    /** MultiItemTechnological:791-806 / LoaderOreDictReRegistrations:348-365: high USB tiers pay lower tiers. */
    public static List<String> usbTags(String id) {
        var tags = new java.util.ArrayList<String>();
        for (String kind : List.of("stick", "cable")) for (int tier = 1; tier <= 4; tier++)
            if (id.equals("usb" + tier + "_" + kind))
                for (int required = 1; required <= tier; required++)
                    tags.add("technology/usb_" + kind + "s_tier_" + required + "_plus");
        return List.copyOf(tags);
    }
}
