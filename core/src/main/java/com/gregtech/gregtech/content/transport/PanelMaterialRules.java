/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Loader_MultiTileEntities:2043-2082 and gregapi wood dictionary item data. */
package com.gregtech.gregtech.content.transport;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.data.MaterialGroups;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.*;

public final class PanelMaterialRules {
    private PanelMaterialRules() {}
    public static ItemComposition panel(ItemComposition input) {
        var parts = new ArrayList<MaterialComponent>();
        // The source uses OP.screw.dat(ANY.Iron), not the first item matching that tag.
        parts.add(MaterialComponent.of(MaterialGroups.Iron, 6 * MaterialPrefix.screw.getMaterialWeight()));
        if (input != null) parts.addAll(input.components());
        return ReversibleCraftingData.perItem(parts, 6, "GT6 CR.REV decorative panel");
    }
    /** Native GT plank IDs corresponding to source PlankEntry registrations; existing data wins. */
    public static GTMaterial plankMaterial(PanelCatalog.Spec spec) {
        String suffix = spec.material().equals("wood") ? "wood" : spec.material().substring(5);
        String name = switch (suffix) {
            case "rubber" -> "WoodRubber"; case "blue_mahoe" -> "BlueMahoe";
            case "white_mahoe" -> "Mahoe"; case "bluespruce" -> "BlueSpruce";
            case "cinnamon" -> "Cinnamonwood"; case "coconut" -> "Coconutwood";
            case "compressed" -> "WoodCompressed"; case "dry" -> "WoodDead";
            case "rotten" -> "WoodRotten"; case "mossy" -> "WoodMossy";
            case "frozen" -> "WoodFrozen"; case "treated" -> "WoodTreated";
            case "wood" -> "Wood"; case "rainbowood" -> "Rainbowood";
            default -> suffix.substring(0, 1).toUpperCase(Locale.ROOT) + suffix.substring(1);
        };
        var material = GTMaterialRegistry.get(name);
        if (!material.isValid()) throw new IllegalStateException("Missing panel plank material: " + name);
        return material;
    }
}
