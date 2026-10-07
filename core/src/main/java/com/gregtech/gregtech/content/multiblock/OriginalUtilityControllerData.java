/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original17197/17998 registrations and LargeHeatExchanger/LightningRod.addToolTips. */
package com.gregtech.gregtech.content.multiblock;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/** Source defaults for the existing heat exchanger and two lightning output identities. */
public final class OriginalUtilityControllerData {
    private OriginalUtilityControllerData() {}
    public static final int HEAT_SOURCE_ID=17197, LIGHTNING_SOURCE_ID=17998;
    public static final String HEAT_PATH="heat_exchanger_main", LIGHTNING_PATH="lightning_rod_electric_output";
    public static final String HEAT_FUEL_KEY="gt.recipe.fuels.hot";
    public static final int HEAT_RATE=OriginalGeneratorParameters.HEAT_EXCHANGER_RATE, HEAT_EFFICIENCY=10000;
    public static final long LIGHTNING_PACKET=AdvancedControllerRules.LIGHTNING_PACKET,
            LIGHTNING_CAPACITY=AdvancedControllerRules.LIGHTNING_CAPACITY;
    public static final int LIGHTNING_AMPS=16;
    public static final List<String> HEAT_STRUCTURE=keys("heatexchanger",4), LIGHTNING_STRUCTURE=keys("lightningrod",9);
    private static List<String> keys(String name,int count) {
        return IntStream.rangeClosed(1,count).mapToObj(i->"gt.tooltip.multiblock."+name+"."+i).toList();
    }
    private static final Map<String,String> ALIASES=Map.of("lightning_rod_main",LIGHTNING_PATH);
    public static Map<String,String> aliases() { return ALIASES; }
}
