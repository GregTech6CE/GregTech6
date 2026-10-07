package com.gregtech.gregtech.content.energy;
import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.GregTechTags;
import java.util.*;
/** Original 10080-10099 family. Existing energy_storage IDs become the large boxes they depict. */
public final class BatteryBoxDefinitions {
    private BatteryBoxDefinitions(){}
    public static final List<String> TIERS=List.of("ulv","lv","mv","hv","ev","iv","luv","zpm","uv","xv");
    private static final List<String> NAMES=List.of("ULV","LV","MV","HV","EV","IV","LuV","ZPM","UV","PUV1");
    private static final List<GTMaterial> CASINGS=List.of(Materials.TinAlloy,Materials.SteelGalvanized,
            Materials.Aluminium,Materials.StainlessSteel,Materials.Chromium,Materials.Titanium,
            Materials.Iridium,Materials.OsmiumElemental,Materials.Trinitanium,Materials.Trinaquadalloy);
    public static GTMaterial material(int tier){return CASINGS.get(tier);}
    public static String label(int tier){return NAMES.get(tier);}
    public static List<EnergyNodeSpec> specifications(){
        var result=new ArrayList<EnergyNodeSpec>();
        for(int tier=0;tier<10;tier++)for(boolean large:new boolean[]{false,true}){
            String name=(large?"Large Battery Box (":"Battery Box (")+label(tier)+")";
            result.add(EnergyNodeSpec.builder((large?"energy_storage_":"battery_box_")+TIERS.get(tier),material(tier))
                    .kind(EnergyNodeSpec.Kind.STORAGE).texture("energystorages/battery_electric"+(large?"_large":""))
                    .input(GregTechTags.Energy.EU,8L<<(2*tier)).output(GregTechTags.Energy.EU,8L<<(2*tier))
                    .batterySlots(large?16:4).capacity(0).name(name).build());
        }
        return List.copyOf(result);
    }
}
