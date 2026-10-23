/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later. Original IDs 10130..10149. */
package com.gregtech.gregtech.content.energy;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.data.GregTechTags;
import java.util.ArrayList;
import java.util.List;

public final class CrystalChargerDefinitions {
    private CrystalChargerDefinitions() {}
    public static List<EnergyNodeSpec> specifications() {
        var result = new ArrayList<EnergyNodeSpec>();
        for (int tier = 0; tier < 10; tier++) for (boolean large : new boolean[]{false, true}) {
            long size = 8L << (2 * tier);
            result.add(EnergyNodeSpec.builder((large ? "large_crystal_charger_" : "crystal_charger_") + BatteryBoxDefinitions.TIERS.get(tier), BatteryBoxDefinitions.material(tier))
                    .kind(EnergyNodeSpec.Kind.STORAGE).texture("energystorages/crystal_laser" + (large ? "_large" : ""))
                    .input(GregTechTags.Energy.LU, size).output(GregTechTags.Energy.LU, size)
                    .batterySlots(large ? 16 : 4).capacity(0)
                    .name((large ? "Large Crystal Charger (T" : "Crystal Charger (T") + tier + ")").build());
        }
        return List.copyOf(result);
    }
}
