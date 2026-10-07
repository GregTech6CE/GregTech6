/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original 17110/17198/17199 registrations and their inherited BasicMachine defaults. */
package com.gregtech.gregtech.content.multiblock;
import com.gregtech.gregtech.api.energy.MachineFaceMasks;
import com.gregtech.gregtech.api.machine.BasicMachineParameters;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.content.machine.OriginalMachineMaterialData;
import com.gregtech.gregtech.data.BasicMachineOriginalParams;
import java.util.List;
public final class OriginalAdvancedControllerData {
    private OriginalAdvancedControllerData() {}
    public static boolean handles(String name) {return name.equals("implosioncompressor")||name.equals("fusionreactor")||name.equals("largemassfab");}
    public static List<String> structureKeys(String name) {
        String family;int count;
        switch(name) {
            case "implosioncompressor"->{family="implosioncompressor";count=2;}
            case "fusionreactor"->{family="fusionreactor";count=7;}
            case "largemassfab"->{family="matterfabricator";count=8;}
            default->throw new IllegalArgumentException("Original advanced controller:"+name);
        }
        final String prefix="gt.tooltip.multiblock."+family+".";
        return java.util.stream.IntStream.rangeClosed(1,count).mapToObj(i->prefix+i).toList();
    }
    public static boolean cheapOverclocking(String name) {return name.equals("largemassfab");}
    public static String chargedEnergy(String name) {return name.equals("fusionreactor")?"LU":"TU";}
    public static MachineFaceMasks faces(String name) {
        if(!handles(name))throw new IllegalArgumentException(name);
        int output=name.equals("fusionreactor")?-1:MachineFaceMasks.BOTTOM;
        return new MachineFaceMasks(63,63,63,63,63,0,-1,output,-1,output);
    }
    public static BasicMachineParameters parameters(String name,String id) {
        if(!handles(name))throw new IllegalArgumentException(name);
        var source=BasicMachineOriginalParams.find(name,1);
        String hull=switch(name) {case "implosioncompressor"->"TungstenSteel";case "fusionreactor"->"SteelGalvanized";default->"Pb";};
        return new BasicMachineParameters(id,GTMaterialRegistry.get(hull),name,source.energyType(),1,
                source.energyInput(),0,source.hardness(),source.resistance(),faces(name),
                OriginalMachineMaterialData.weights(name,1),source.parallel(),source.energyInputMin(),source.energyInputMax());
    }
}
