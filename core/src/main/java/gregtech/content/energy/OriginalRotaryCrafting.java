/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original Loader_MultiTileEntities 10021..10025, 10111..10115, 11021..11025 and 11111..11115. */
package gregtech.content.energy;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialGroups;
import gregtech.content.material.Materials;
import gregtech.content.transport.TransportCraftingCatalog.Input;
import gregtech.content.transport.TransportCraftingCatalog.Row;
import java.util.*;

/**
 * Source motor and dynamo recipes, split by converter family. Flux devices are built around the
 * same-tier electric device, so both families have to come from one table.
 */
public final class OriginalRotaryCrafting {
    private OriginalRotaryCrafting() {}

    private static final String[] TIERS = {"lv", "mv", "hv", "ev", "iv"};
    private static final GTMaterial[] MATERIALS = {
        Materials.SteelGalvanized, Materials.Aluminium, Materials.StainlessSteel, Materials.Chromium, Materials.Titanium
    };
    private static final GTMaterial[] MAGNETS = {
        Materials.IronMagnetic,
        Materials.SteelMagnetic,
        Materials.SteelMagnetic,
        Materials.NeodymiumMagnetic,
        Materials.NeodymiumMagnetic
    };
    private static final GTMaterial[] WIRES = {
        MaterialGroups.Cu,
        MaterialGroups.Cu,
        Materials.AnnealedCopper,
        Materials.AnnealedCopper,
        Materials.AnnealedCopper
    };
    private static final int[] WIRE_SIZES = {1, 2, 4, 8, 16};

    private static Input form(String prefix, GTMaterial material) {
        return new Input("form", prefix, material);
    }

    private static Input item(String id) {
        return new Input("item", id, null);
    }

    private static Input casing(GTMaterial material) {
        return new Input("casing", "casingMachineDouble", material);
    }

    public static List<Row> rows() {
        var rows = new ArrayList<Row>();
        for (int i = 0; i < TIERS.length; i++) {
            rows.add(new Row(
                    "rotary/electric_motor_" + TIERS[i],
                    List.of("TIT", "CMC", "TGd"),
                    Map.of(
                            'T', form("screw", MATERIALS[i]),
                            'I', form("stickLong", MAGNETS[i]),
                            'M', casing(MATERIALS[i]),
                            'C', new Input("wire", Integer.toString(WIRE_SIZES[i]), WIRES[i]),
                            'G', form("gearGt", MATERIALS[i]),
                            'd', item("gt6ce:tool_screwdriver")),
                    "gt6ce:electric_motor_" + TIERS[i],
                    1,
                    false,
                    true));
            rows.add(new Row(
                    "rotary/electric_dynamo_" + TIERS[i],
                    List.of("TGT", "CMC", "TId"),
                    Map.of(
                            'T', form("screw", MATERIALS[i]),
                            'G', form("gearGt", MATERIALS[i]),
                            'M', casing(MATERIALS[i]),
                            'C', new Input("wire", Integer.toString(WIRE_SIZES[i]), WIRES[i]),
                            'I', form("stickLong", MAGNETS[i]),
                            'd', item("gt6ce:tool_screwdriver")),
                    "gt6ce:electric_dynamo_" + TIERS[i],
                    1,
                    false,
                    true));
        }
        for (var tier : FluxRotaryCatalog.TIERS) {
            rows.add(new Row(
                    "rotary/flux_motor_" + tier.tier(),
                    List.of("GMG"),
                    Map.of(
                            'G', form("gearGt", tier.material()),
                            'M', item("gt6ce:electric_motor_" + tier.tier())),
                    "gt6ce:flux_motor_" + tier.tier(),
                    1,
                    false,
                    true));
            rows.add(new Row(
                    "rotary/flux_dynamo_" + tier.tier(),
                    List.of("SMS", "SGS"),
                    Map.of(
                            'S', form("stickLong", tier.material()),
                            'M', item("gt6ce:electric_dynamo_" + tier.tier()),
                            'G', form("gearGt", tier.material())),
                    "gt6ce:flux_dynamo_" + tier.tier(),
                    1,
                    false,
                    true));
        }
        return List.copyOf(rows);
    }
}
