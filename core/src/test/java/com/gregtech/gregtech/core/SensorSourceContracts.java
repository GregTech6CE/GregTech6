package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.sensor.*;
import com.gregtech.gregtech.content.machine.MachineConstructionMaterials;
import com.gregtech.gregtech.data.SourceBlockProperties;
import com.gregtech.gregtech.data.generated.GTSensorRecipesGen;
import java.util.*;

/** Current original 21 registrations and raw compressed-volume scaling, rather than fluid level. */
final class SensorSourceContracts {
    private static int assertions;
    static int verify() {
        assertions = 0;
        var expectedIds = Set.of(31000,31001,31002,31003,31004,31005,31006,31007,
                31010,31011,31012,31013,31015,31016,31017,31018,31019,31020,31021,31022,31023);
        check(SensorCatalog.ALL.size() == 21 && SensorCatalog.ALL.stream().map(SensorCatalog.Entry::originalId)
                .collect(java.util.stream.Collectors.toSet()).equals(expectedIds), "All current original sensors including 31023");
        check(GTSensorRecipesGen.ROWS.size() == 21, "One source crafting recipe for every sensor");
        for (var entry : SensorCatalog.ALL) {
            var recipe = GTSensorRecipesGen.ROWS.stream().filter(r -> r.machineId() == entry.originalId()).findFirst().orElseThrow();
            check(recipe.blockId().equals(entry.id()), "Exact recipe / registered source identity " + entry.id());
            var props = SourceBlockProperties.block(entry.id()).orElseThrow();
            check(props.sourceId() == entry.originalId() && props.tool().equals("pickaxe") && props.handHarvestable()
                    && props.explicitLevel() == 1 && props.material() == null, "Source aUtilMetal, explicit1, null casing " + entry.id());
            check(MachineConstructionMaterials.block(entry.id()).orElseThrow().recoverable(), "Known source CR.REV record " + entry.id());
        }
        check(SensorCatalog.byKind("ENERGY").descriptionKey().equals("gt.tooltip.sensor.electrometer"), "Electric flow description, not generic stored-energy hint");
        check(SensorCatalog.byKind("WEIGHTOMETRIC").descriptionKey().equals("gt.tooltip.sensor.heavyweightometer"), "Existing weightometric identity is original heavy meter");
        check(SensorCatalog.byKind("KILOGIBBLOMETER").originalId() == 31023
                && SensorPanelRules.unit("KILOGIBBLOMETER").equals("gibbl") && SensorPanelRules.color("KILOGIBBLOMETER") == 0xFFFF00,
                "New sensor retains source Gibbl symbol / yellow icon");
        var recipe = GTSensorRecipesGen.ROWS.stream().filter(r -> r.machineId() == 31023).findFirst().orElseThrow();
        check(recipe.pattern().equals(List.of("WPW", "BXB", "WPW")) && recipe.keys().get('X').equals("mat:gem@Diamond"), "Original kilo-Gibbl recipe diamond center");
        var expected = Map.of("TinAlloy",GTValues.U*17/4,"RedAlloy",GTValues.U/2,"Diamond",GTValues.U);
        var actual = new HashMap<String,Long>();
        for (var part : MachineConstructionMaterials.block("sensor_kilogibblometer").orElseThrow().components())
            actual.put(part.material().resolve().getName(), part.amount());
        check(actual.equals(expected), "Two double plates /two bolts /four fine wires /one diamond exact quantities");
        check(SensorMeasurements.gibbl(1_234_567) == 1234 && SensorMeasurements.kiloGibbl(1_234_567) == 1,
                "Source Gibbl /1000 and kilo-Gibbl /1000000");
        check(SensorMeasurements.gibbl(999) == 0 && SensorMeasurements.gibbl(1000) == 1
                && SensorMeasurements.kiloGibbl(999999) == 0 && SensorMeasurements.kiloGibbl(1000000) == 1,
                "Source integer truncation at both scale boundaries");
        for (int display = 0; display < 6; display++) for (int input = 0; input < 6; input++)
            check(SensorPanelRules.validInputSide(display,input) == (display != input), "Independent input excludes display " + display + "/" + input);
        check(!SensorPanelRules.validInputSide(2, -1) && !SensorPanelRules.validInputSide(2, 6)
                && !SensorPanelRules.validInputSide(-1,2), "Invalid saved directions rejected");
        return assertions;
    }
    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
}
