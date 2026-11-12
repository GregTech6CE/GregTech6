/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Loader_MultiTileEntities and gregapi wood dictionary item data. */
package gregtech.content.transport;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialComponent;
import gregtech.content.data.MaterialGroups;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ReversibleCraftingData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

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
        String suffix =
                spec.material().equals("wood") ? "wood" : spec.material().substring(5);
        String name = switch (suffix) {
            case "rubber" -> "WoodRubber";
            case "blue_mahoe" -> "BlueMahoe";
            case "bluespruce" -> "BlueSpruce";
            case "cinnamon" -> "Cinnamonwood";
            case "coconut" -> "Coconutwood";
            case "compressed" -> "WoodCompressed";
            case "dry" -> "WoodDead";
            case "rotten" -> "WoodRotten";
            case "mossy" -> "WoodMossy";
            case "frozen" -> "WoodFrozen";
            case "treated" -> "WoodTreated";
            case "wood" -> "Wood";
            case "rainbowood" -> "Rainbowood";
            default ->
                Arrays.stream(suffix.split("_"))
                        .map(part -> part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1))
                        .collect(java.util.stream.Collectors.joining());
        };
        var material = GTMaterialRegistry.get(name);
        if (!material.isValid()) throw new IllegalStateException("Missing panel plank material: " + name);
        return material;
    }
}
