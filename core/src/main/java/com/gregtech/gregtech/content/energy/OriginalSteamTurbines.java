/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Loader_MultiTileEntities1512..1548 and MultiTileEntityTurbineSteam. */
package com.gregtech.gregtech.content.energy;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.transport.TransportCraftingCatalog.Input;
import com.gregtech.gregtech.content.transport.TransportCraftingCatalog.Row;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.data.MaterialGroups;
import java.util.*;

/** Source name identifies the four crafted rotors; hull identity comes from Kinetic_T. */
public final class OriginalSteamTurbines {
    private OriginalSteamTurbines() {}
    public record Variant(String id, int sourceId, GTMaterial hull, GTMaterial rotor, long input, long output, String name) {}
    private static Variant variant(String suffix, int sourceId, GTMaterial hull, GTMaterial rotor, long input, long output, String name) {
        return new Variant("steam_turbine_"+suffix, sourceId, hull, rotor, input, output, "Steam Turbine ("+name+")");
    }
    public static final List<Variant> VARIANTS = List.of(
            variant("bronze",1512,Materials.Bronze,Materials.Bronze,48,16,"Bronze"),
            variant("brass",1515,Materials.Bronze,Materials.Brass,72,24,"Brass"),
            variant("invar",1518,Materials.Bronze,Materials.Invar,96,32,"Invar"),
            variant("steel",1522,MaterialGroups.Steel,MaterialGroups.Steel,192,64,"Steel"),
            variant("chromium",1525,MaterialGroups.Steel,Materials.Chromium,288,96,"Chromium"),
            variant("ironwood",1527,MaterialGroups.Steel,Materials.Ironwood,384,128,"Ironwood"),
            variant("steeleaf",1528,MaterialGroups.Steel,Materials.Steeleaf,384,128,"Steeleaf"),
            variant("thaumium",1529,MaterialGroups.Steel,Materials.Thaumium,384,128,"Thaumium"),
            variant("titanium",1530,Materials.Titanium,Materials.Titanium,768,256,"Titanium"),
            variant("fiery_steel",1531,Materials.Titanium,Materials.FierySteel,768,256,"Fiery Steel"),
            variant("aluminium",1535,Materials.Titanium,Materials.Aluminium,1152,384,"Aluminium"),
            variant("magnalium",1538,Materials.Titanium,Materials.Magnalium,1536,512,"Magnalium"),
            variant("void_metal",1540,Materials.Tungstensteel,Materials.VoidMetal,2304,768,"Void Metal"),
            variant("trinitanium",1545,Materials.Tungstensteel,Materials.Trinitanium,3072,1024,"Trinitanium"),
            variant("graphene",1548,Materials.Tungstensteel,Materials.Graphene,6144,2048,"Graphene"));
    public static boolean handles(EnergyNodeSpec spec) { return spec.kind()==EnergyNodeSpec.Kind.TURBINE && spec.id().startsWith("steam_turbine_"); }
    public static int efficiency(EnergyNodeSpec spec) { return (int)(10000L*spec.outputRate()*2/spec.inputRate()); }
    public static List<EnergyNodeSpec> specifications() {
        // Original ANY.Steel steals Steel's appearance/output. Keep the group in recipes,
        // but use that concrete identity at the native block boundary.
        return VARIANTS.stream().map(v -> EnergyNodeSpec.builder(v.id(),v.hull()==MaterialGroups.Steel ? Materials.Steel : v.hull()).kind(EnergyNodeSpec.Kind.TURBINE)
                .texture("turbines/rotation_steam").input(GregTechTags.Energy.STEAM,v.input()).output(GregTechTags.Energy.RU,v.output())
                .capacity(v.input()*2).name(v.name()).build()).toList();
    }
    public static List<Row> rows() {
        return VARIANTS.stream().map(v -> new Row("turbines/"+v.id(),List.of("TwT","GSG","TMT"),Map.of(
                'T',new Input("form","rotor",v.rotor()),'G',new Input("form","gearGt",v.hull()),
                'S',new Input("form","stickLong",v.hull()),'M',new Input("casing","casingMachineDouble",v.hull()),
                'w',new Input("item","gregtech:tool_wrench",null)),"gregtech:"+v.id(),1,false,true)).toList();
    }
}
