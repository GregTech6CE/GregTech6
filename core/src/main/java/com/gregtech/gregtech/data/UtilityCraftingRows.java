/* Original GT6 MultiItemRandomTools and Loader_MultiTileEntities; LGPL-3.0-or-later. */
package com.gregtech.gregtech.data;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.registry.GTStorageMetals;
import java.util.*;

/** Source utility recipes omitted by the initial partial import. */
public final class UtilityCraftingRows {
    private UtilityCraftingRows() {}
    public static final List<OriginalSurvivalRecipes.Row> ROWS = rows();
    private static List<OriginalSurvivalRecipes.Row> rows() {
        var rows = new ArrayList<OriginalSurvivalRecipes.Row>();
        // MultiItemRandomTools:126-132; tools retain their original durability use.
        for (String[] raw : new String[][] {
                {"clay_basin", "CkC|CRC| C "}, {"clay_faucet", "C C|kCR"},
                {"clay_crossing", "kCR|CCC| C "}, {"clay_juicer", "kCR|CCC"},
                {"clay_bowl", "k R|C C|CCC"}})
            rows.add(new OriginalSurvivalRecipes.Row(raw[0],1,GTMaterialRegistry.get("Clay"),raw[1],
                    Map.of('C',"tag:gregtech:clay_balls",'k',"item:gregtech:tool_knife",'R',"item:gregtech:tool_rolling_pin")));
        // MultiItemRandomTools:240,388,534,536-537.
        rows.add(new OriginalSurvivalRecipes.Row("empty_spray_can",1,GTMaterialRegistry.get("Tin"),"Rf|Cs",
                Map.of('R',"item:minecraft:redstone",'C',"mat:plateCurved",'f',"item:gregtech:tool_file",'s',"item:gregtech:tool_saw")));
        rows.add(new OriginalSurvivalRecipes.Row("quicksilver_thermometer",1,GTMaterialRegistry.get("Copper")," GD|GQG|PG ",
                Map.of('G',"mat:plate@Glass",'D',"item:minecraft:red_dye",'Q',"tag:gregtech:quicksilver",'P',"mat:plate")));
        rows.add(new OriginalSurvivalRecipes.Row("electrometer",1,GTMaterialRegistry.get("SteelGalvanized"),"TXT|PCP|TdT",
                Map.of('X',"mat:wireFine@Copper",'P',"mat:plate",'T',"mat:screw",'C',"circuit:1",'d',"item:gregtech:tool_screwdriver")));
        rows.add(new OriginalSurvivalRecipes.Row("tachometer",1,GTMaterialRegistry.get("Brass"),"TXT|PCP|TdT",
                Map.of('X',"mat:gearGtSmall",'P',"mat:plate",'T',"mat:screw",'C',"item:minecraft:redstone",'d',"item:gregtech:tool_screwdriver")));
        // Loader_MultiTileEntities metalset:138. One canonical locker per material.
        for (var spec : GTStorageMetals.ALL) {
            String id=spec.suffix().equals("stainless_steel")?"locker":"locker_"+spec.suffix();
            rows.add(new OriginalSurvivalRecipes.Row(id,1,spec.material(),"SdS|LCL|TMT",
                    Map.of('S',"mat:stick",'d',"item:gregtech:tool_screwdriver",'L',"item:minecraft:leather",
                            'C',"item:gregtech:chest_"+spec.suffix(),'T',"mat:screw",'M',"block:casingMachine")));
        }
        return List.copyOf(rows);
    }
}
