/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original Loader_MultiTileEntities 10031..10035 / 11031..11035. */
package com.gregtech.gregtech.content.energy;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.transport.TransportCraftingCatalog.Input;
import com.gregtech.gregtech.content.transport.TransportCraftingCatalog.Row;
import com.gregtech.gregtech.data.MaterialGroups;
import java.util.*;

public final class OriginalMagnetCrafting {
    private OriginalMagnetCrafting() {}
    public static List<Row> rows() {
        var rows = new ArrayList<Row>();
        String[] tiers = {"lv", "mv", "hv", "ev", "iv"};
        for (var spec : MagnetMachineDefinitions.specifications()) {
            int tier = Arrays.asList(tiers).indexOf(spec.id().substring(spec.id().lastIndexOf('_') + 1));
            boolean rf = spec.id().startsWith("flux_");
            var key = rf ? Map.of('S', new Input("form", "stickLong", spec.material()),
                    'M', new Input("item", "gregtech:electromagnet_" + tiers[tier], null))
                    : Map.of('C', new Input("wire", Integer.toString(1 << tier), tier < 2 ? MaterialGroups.Cu : Materials.AnnealedCopper),
                    'M', new Input("casing", "casingMachine", spec.material()),
                    'x', new Input("item", "gregtech:tool_wire_cutter", null),
                    'w', new Input("item", "gregtech:tool_wrench", null));
            rows.add(new Row("magnets/" + spec.id(), rf ? List.of("SSS", "SMS", "SSS") : List.of("CxC", "CMC", "CwC"),
                    key, "gregtech:" + spec.id(), 1, false, true));
        }
        return List.copyOf(rows);
    }
}
