/* Original GT6 MultiItemRandomTools and Loader_MultiTileEntities; LGPL-3.0-or-later. */
package gregtech.data;

import gregtech.api.material.GTMaterialRegistry;
import gregtech.registry.GTStorageMetals;
import java.util.*;

/** Source utility recipes omitted by the initial partial import. */
public final class UtilityCraftingRows {
    private UtilityCraftingRows() {}
    public static final List<OriginalSurvivalRecipes.Row> ROWS = rows();
    private static List<OriginalSurvivalRecipes.Row> rows() {
        var rows = new ArrayList<OriginalSurvivalRecipes.Row>();
        // The three upper RF grades were omitted by the first energy-node import.
        for (var tier : gregtech.content.energy.FluxRotaryCatalog.TIERS.subList(2, 5)) {
            rows.add(new OriginalSurvivalRecipes.Row("flux_motor_" + tier.tier(), 1, tier.material(), "GMG", Map.of(
                    'M', "item:gt6ce:electric_motor_" + tier.tier(), 'G', "mat:gearGt")));
            rows.add(new OriginalSurvivalRecipes.Row("flux_dynamo_" + tier.tier(), 1, tier.material(), "SMS|SGS", Map.of(
                    'M', "item:gt6ce:electric_dynamo_" + tier.tier(), 'G', "mat:gearGt", 'S', "mat:stickLong")));
        }
        for (var spec : gregtech.content.energy.HeatExchangerCatalog.ALL)
            rows.add(new OriginalSurvivalRecipes.Row(spec.id(), 1, spec.material(), "PCP|OwO|PMP", Map.of(
                    'P', spec.dense() ? "mat:plateQuadruple@Lead" : "mat:plate@Lead",
                    'C', spec.material() == ImportedMaterialData.Invar ? "tag:gt6ce:technology/any_copper_" + (spec.dense() ? "dense_plate" : "double_plate")
                            : "mat:" + (spec.dense() ? "plateDense" : "plateDouble") + "@AnnealedCopper",
                    'O', "pipe:any_" + (spec.dense() ? "large" : "small") + "@Copper",
                    'M', "block:" + (spec.dense() ? "casingMachineQuadruple" : "casingMachine"),
                    'w', "item:gt6ce:tool_wrench")));
        // MultiItemRandomTools:126-132; tools retain their original durability use.
        for (var raw : gregtech.content.recipe.ClayUtilityCatalog.ROWS)
            rows.add(new OriginalSurvivalRecipes.Row(raw.raw(),1,GTMaterialRegistry.get("Clay"),raw.pattern(),
                    Map.of('C',"tag:gt6ce:clay_balls",'k',"item:gt6ce:tool_knife",'R',"item:gt6ce:tool_rolling_pin")));
        // MultiItemRandomTools:240,388,534,536-537.
        rows.add(new OriginalSurvivalRecipes.Row("empty_spray_can",1,GTMaterialRegistry.get("Tin"),"Rf|Cs",
                Map.of('R',"item:minecraft:redstone",'C',"mat:plateCurved",'f',"item:gt6ce:tool_file",'s',"item:gt6ce:tool_saw")));
        rows.add(new OriginalSurvivalRecipes.Row("quicksilver_thermometer",1,GTMaterialRegistry.get("Copper")," GD|GQG|PG ",
                Map.of('G',"mat:plate@Glass",'D',"item:minecraft:red_dye",'Q',"tag:gt6ce:quicksilver",'P',"mat:plate")));
        rows.add(new OriginalSurvivalRecipes.Row("electrometer",1,GTMaterialRegistry.get("SteelGalvanized"),"TXT|PCP|TdT",
                Map.of('X',"mat:wireFine@Copper",'P',"mat:plate",'T',"mat:screw",'C',"circuit:1",'d',"item:gt6ce:tool_screwdriver")));
        rows.add(new OriginalSurvivalRecipes.Row("tachometer",1,GTMaterialRegistry.get("Brass"),"TXT|PCP|TdT",
                Map.of('X',"mat:gearGtSmall",'P',"mat:plate",'T',"mat:screw",'C',"item:minecraft:redstone",'d',"item:gt6ce:tool_screwdriver")));
        // Loader_MultiTileEntities metalset:138. One canonical locker per material.
        for (var spec : GTStorageMetals.ALL) {
            String id=spec.suffix().equals("stainless_steel")?"locker":"locker_"+spec.suffix();
            rows.add(new OriginalSurvivalRecipes.Row(id,1,spec.material(),"SdS|LCL|TMT",
                    Map.of('S',"mat:stick",'d',"item:gt6ce:tool_screwdriver",'L',"item:minecraft:leather",
                            'C',"item:gt6ce:chest_"+spec.suffix(),'T',"mat:screw",'M',"block:casingMachine")));
        }
        return List.copyOf(rows);
    }
}
