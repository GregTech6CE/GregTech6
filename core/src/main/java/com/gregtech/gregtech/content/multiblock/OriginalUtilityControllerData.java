/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original17197/17996/17998/17999 registrations and LargeHeatExchanger/LightningRod.addToolTips. */
package com.gregtech.gregtech.content.multiblock;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/** Source defaults and preserved aliases for specialized utility controllers. */
public final class OriginalUtilityControllerData {
    private OriginalUtilityControllerData() {}
    public static final int HEAT_SOURCE_ID=17197, LIGHTNING_SOURCE_ID=17998;
    public static final String HEAT_PATH="heat_exchanger_main", LIGHTNING_PATH="lightning_rod_electric_output";
    public static final String HEAT_FUEL_KEY="gt.recipe.fuels.hot";
    public static final int HEAT_RATE=OriginalGeneratorParameters.HEAT_EXCHANGER_RATE, HEAT_EFFICIENCY=10000;
    public static final long LIGHTNING_PACKET=AdvancedControllerRules.LIGHTNING_PACKET,
            LIGHTNING_CAPACITY=AdvancedControllerRules.LIGHTNING_CAPACITY;
    public static final int LIGHTNING_AMPS=16;
    public static final List<String> HEAT_STRUCTURE=keys("heatexchanger",4), LIGHTNING_STRUCTURE=keys("lightningrod",9),
            VON_DA_GRAAGG_STRUCTURE=keys("von.da.graagg",4);
    private static List<String> keys(String name,int count) {
        return IntStream.rangeClosed(1,count).mapToObj(i->"gt.tooltip.multiblock."+name+"."+i).toList();
    }
    private static final Map<String,String> ALIASES=Map.of("lightning_rod_main",LIGHTNING_PATH,
            "bedrock_drill_main","bedrock_mining_drill_controller");
    public static Map<String,String> aliases() { return ALIASES; }
}
