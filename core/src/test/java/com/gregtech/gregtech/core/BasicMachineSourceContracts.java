package com.gregtech.gregtech.core;
import com.gregtech.gregtech.content.machine.OriginalBasicMachineRules;
import com.gregtech.gregtech.api.energy.MachineFaceMasks;
import com.gregtech.gregtech.api.recipe.MachineWorkCost;
import java.io.*;
import java.nio.charset.StandardCharsets;
public final class BasicMachineSourceContracts {
    private static int assertions;
    public static int verify() {
        int rows=0;
        try(var input=BasicMachineSourceContracts.class.getResourceAsStream("/gregtech/basic-machine-source-flags.tsv")) {
            if(input==null)throw new AssertionError("Original247 source rows missing");
            for(var line:new String(input.readAllBytes(),StandardCharsets.UTF_8).split("\\R")) {
                if(line.isBlank()||line.startsWith("#"))continue;
                var p=line.split("\t");var name=p[0];int tier=Integer.parseInt(p[1]);
                check(OriginalBasicMachineRules.handles(name,tier),"source identity "+p[2]);
                check(OriginalBasicMachineRules.cheapOverclocking(name,tier)==Boolean.parseBoolean(p[3]),"source cheap "+p[2]);
                check(OriginalBasicMachineRules.efficiency(name,tier)==Integer.parseInt(p[4]),"source efficiency "+p[2]);
                check(OriginalBasicMachineRules.requiresIgnition(name,tier)==Boolean.parseBoolean(p[5]),"source ignition "+p[2]);
                check(OriginalBasicMachineRules.noConstantPower(name,tier)==Boolean.parseBoolean(p[6]),"source constant power "+p[2]);
                var spec=com.gregtech.gregtech.content.machine.BasicMachineCatalog.specifications().stream().filter(s->s.machineName().equals(name)&&s.tier()==tier).findFirst().orElseThrow();
                var f=spec.faceConfig();int[] actual={f.itemInputs(),f.itemOutputs(),f.fluidInputs(),f.fluidOutputs(),f.energyInputs(),f.itemAutoInput(),f.itemAutoOutput(),f.fluidAutoInput(),f.fluidAutoOutput()};
                for(int i=0;i<actual.length;i++)check(actual[i]==Integer.parseInt(p[7+i]),"source face field"+i+" registry"+p[2]);rows++;
            }
        }catch(IOException error){throw new AssertionError(error);}
        check(rows==247,"all source variants");
        check(!OriginalBasicMachineRules.handles("massfab",6)&&!OriginalBasicMachineRules.handles("largecentrifuge",1),"unknown tiers and dedicated controllers excluded");
        check(OriginalBasicMachineRules.io(0,63,-1)==null,"source zero recipe slots suppress IO");
        check(OriginalBasicMachineRules.io(1,0,MachineFaceMasks.TOP)==null,"source empty face mask suppresses IO");
        var all=OriginalBasicMachineRules.io(1,63,-1);
        check(all.any()&&all.faces().isEmpty(),"source any/no auto");
        var auto=OriginalBasicMachineRules.io(1,63,MachineFaceMasks.BACK);
        check(auto.any()&&auto.faces().size()==1&&auto.faces().get(0).automatic(),"source auto otherwise any");
        var partial=OriginalBasicMachineRules.io(1,(1<<MachineFaceMasks.TOP)|(1<<MachineFaceMasks.LEFT),MachineFaceMasks.LEFT);
        check(!partial.any()&&partial.faces().size()==2&&partial.faces().get(0).side()==MachineFaceMasks.TOP&&!partial.faces().get(0).automatic()&&partial.faces().get(1).automatic(),"source restricted face order and auto marker");
        check(OriginalBasicMachineRules.unitKey("LU").equals("gt.td.short.energy.light")&&OriginalBasicMachineRules.unitKey("QU").equals("gt.td.short.energy.quantum"),"source energy identities");
        check(OriginalBasicMachineRules.IGNITION_TICKS==40&&OriginalBasicMachineRules.IGNITION_NBT.equals("gt.ignite"),"original ignition timer and key");
        var mixer=MachineWorkCost.calculate(8,10,1,false,OriginalBasicMachineRules.efficiency("electricmixer",1),16,64,OriginalBasicMachineRules.cheapOverclocking("electricmixer",1));
        check(mixer.minimumPower()==32&&mixer.totalWork()==320,"source half efficiency plus normal overclock");
        var roaster=MachineWorkCost.calculate(8,10,1,false,10000,16,64,OriginalBasicMachineRules.cheapOverclocking("roaster",1));
        check(roaster.minimumPower()==8&&roaster.totalWork()==80,"source cheap overclock preserves total work");
        return assertions;
    }
    private static void check(boolean ok,String label){assertions++;if(!ok)throw new AssertionError(label);}
}
