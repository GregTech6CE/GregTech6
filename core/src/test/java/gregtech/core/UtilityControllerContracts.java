package gregtech.core;

import gregtech.api.material.*;
import gregtech.internal.material.*;
import gregtech.content.machine.MachineConstructionMaterials;
import gregtech.content.multiblock.OriginalUtilityControllerData;
import gregtech.content.data.SourceBlockProperties;
import java.util.*;

/** Fixed Loader 17197/17998 inputs and specialized source tooltip arithmetic. */
final class UtilityControllerContracts {
    private static int assertions;
    static int verify() {
        assertions = 0;
        check(OriginalUtilityControllerData.HEAT_SOURCE_ID == 17197
                && OriginalUtilityControllerData.LIGHTNING_SOURCE_ID == 17998, "Original utility controller identities");
        check(OriginalUtilityControllerData.HEAT_RATE == 16384
                && OriginalUtilityControllerData.HEAT_EFFICIENCY == 10000, "Source default heat rate / full efficiency");
        check(OriginalUtilityControllerData.HEAT_FUEL_KEY.equals("gt.recipe.fuels.hot"), "Source FM.Hot map identity");
        check(OriginalUtilityControllerData.HEAT_STRUCTURE.size() == 4
                && OriginalUtilityControllerData.LIGHTNING_STRUCTURE.size() == 9, "Four heat / nine lightning source descriptions");
        check(OriginalUtilityControllerData.LIGHTNING_PACKET == 32768
                && OriginalUtilityControllerData.LIGHTNING_AMPS == 16, "Original custom VREC[6] packet and amp tooltip");
        check(OriginalUtilityControllerData.LIGHTNING_CAPACITY == 589824000L, "Original 18000 * VREC[6] per strike");
        composition("heat_exchanger_main", Map.of("W", 36L, "AnnealedCopper", 54L, "Cu", 24L));
        composition("lightning_rod_electric_output", Map.of("W", 8L, "NiobiumTitanium", 16L, "Pt", 2L, "Sapphire", 2L));
        check(OriginalUtilityControllerData.LIGHTNING_PATH.equals("lightning_rod_electric_output"),
                "Utility controllers use their canonical source paths");
        check(MachineConstructionMaterials.block("lightning_rod_electric_output").orElseThrow().recoverable(),
                "Canonical lightning controller preserves exact source REV");
        for (var path : List.of("heat_exchanger_main", "lightning_rod_electric_output")) {
            var p = SourceBlockProperties.block(path).orElseThrow();
            check(p.sourceId() == (path.equals("heat_exchanger_main") ? 17197 : 17998)
                    && p.tool().equals("wrench"), "Original utility source harvest identity " + path);
        }
        check(SourceBlockProperties.block("lightning_rod_electric_output").orElseThrow().material().resolve()
                == GTMaterialRegistry.get("Tungsten").resolve(), "Original lightning harvest material is tungsten");
        composition("von_da_graagg_generator", Map.of("NetherStar",1L,"Pt",2L,"Ruby",2L,"SteelGalvanized",8L,"EnderEye",1L));
        check(MachineConstructionMaterials.block("bedrock_mining_drill_controller").orElseThrow().recoverable(),
                "Canonical drill retains recoverable source quantities");
        check(SourceBlockProperties.block("bedrock_mining_drill_controller").orElseThrow().sourceId()==17999,
                "Canonical drill keeps source titanium harvest identity");
        for(long energy : new long[]{0,15,16,255,256,2048,4095,4096,Long.MAX_VALUE}) {
            int range=gregtech.content.multiblock.VonDaGraaggRules.range(energy,true);
            int expected=(int)Math.min(255,energy/16);
            check(range==expected && gregtech.content.multiblock.VonDaGraaggRules.range(energy,false)==0,
                    "Original byte range boundary and broken tower " + energy);
        }
        var raw=gregtech.internal.sensor.SensorControl.evaluate(
                gregtech.internal.sensor.SensorControl.Mode.DISPLAY,255,256,0);
        var percent=gregtech.internal.sensor.SensorControl.evaluate(
                gregtech.internal.sensor.SensorControl.Mode.PERCENT,255,256,0);
        check(raw.displayed()==255 && percent.displayed()==99,"Raw range and percentage remain distinct selectable readings");
        return assertions;
    }
    private static void composition(String path, Map<String, Long> units) {
        var composition = MachineConstructionMaterials.block(path).orElseThrow();
        var actual = new HashMap<String, Long>();
        for (var part : composition.components()) actual.put(part.material().resolve().getName(), part.amount());
        var expected = new HashMap<String, Long>();
        units.forEach((m, n) -> expected.put(GTMaterialRegistry.get(m).resolve().getName(), n * GTValues.U));
        check(composition.recoverable() && actual.equals(expected), "Known controller-only CR.REV inputs " + path);
    }
    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
}
