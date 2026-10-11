/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original Loader_MultiTileEntities 10001..10005, 11001..11005, 10161..10165, 11161..11165. */
package com.gregtech.gregtech.content.energy;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.transport.TransportCraftingCatalog.Input;
import com.gregtech.gregtech.content.transport.TransportCraftingCatalog.Row;
import com.gregtech.gregtech.data.MaterialGroups;
import java.util.*;

public final class OriginalThermalCrafting {
    private OriginalThermalCrafting() {}
    private static Input form(String prefix, GTMaterial material) { return new Input("form", prefix, material); }
    private static Input item(String id) { return new Input("item", "gregtech:" + id, null); }
    public static List<Row> rows() {
        var result = new ArrayList<Row>();
        String[] plate = {"plate", "plateDouble", "plateTriple", "plateQuadruple", "plateQuintuple"};
        GTMaterial[] resistor = {MaterialGroups.Cu, Materials.Constantan, Materials.Kanthal, Materials.Nichrome, Materials.Carborundum};
        GTMaterial[] cable = {Materials.Tin, MaterialGroups.Cu, Materials.Gold, Materials.Aluminium, Materials.Platinum};
        for (var variant : OriginalThermalConverter.VARIANTS) {
            int tier = variant.sourceId() % 10 - 1;
            boolean cooler = variant.id().contains("cooler_"), rf = variant.id().startsWith("flux_");
            Map<Character, Input> key;
            List<String> pattern;
            if (rf) {
                pattern = cooler ? List.of("PSP", "PMP", "PSP") : List.of("SSS", "SMS", "SSS");
                var map = new HashMap<Character, Input>();
                map.put('M', item(variant.id().replace("flux_", "electric_")));
                map.put('S', form("stickLong", variant.material()));
                if (cooler) map.put('P', form("plate", variant.material()));
                key = Map.copyOf(map);
            } else if (cooler) {
                pattern = List.of("WPw", "CMC", "xPW");
                key = Map.of('W', new Input("cable", "1", cable[tier]), 'P', form(plate[tier], Materials.Silicon),
                        'C', form(plate[tier], MaterialGroups.Cu), 'M', new Input("casing", "casingMachine", variant.material()),
                        'w', item("tool_wrench"), 'x', item("tool_wire_cutter"));
            } else {
                pattern = List.of("TCT", "CMC", "TCd");
                key = Map.of('M', new Input("casing", "casingMachineDouble", variant.material()),
                        'T', form("screw", variant.material()), 'C', new Input("wire", Integer.toString(1 << tier), resistor[tier]),
                        'd', item("tool_screwdriver"));
            }
            result.add(new Row("thermal/" + variant.id(), pattern, key, "gregtech:" + variant.id(), 1, false, true));
        }
        return List.copyOf(result);
    }
}
