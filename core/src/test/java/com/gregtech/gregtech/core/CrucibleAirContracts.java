package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.machine.crucible.*;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.data.GregTechTags;
import java.util.*;

public class CrucibleAirContracts {
    private static int checks;
    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        com.gregtech.gregtech.api.mod.ModData.bindPresence(id -> false);
        GTMaterialRegistry.init();
        long u = GTValues.U;
        check(CrucibleEnergyRules.airAmount(1000,1,16*u)==u,"1000KU pushes 1U air");
        check(CrucibleEnergyRules.airAmount(-1000,1,16*u)==0,"pull stroke adds no air");
        check(CrucibleEnergyRules.airAmount(512,1,u)==332107776L,"512KU exact original U1000 precision");
        check(CrucibleEnergyRules.airAmount(Long.MAX_VALUE,Long.MAX_VALUE,7)==7,"overflow capped at vessel room");
        check(CrucibleEnergyRules.airAmount(512,1,0)==0,"full vessel accepts no extra contents");
        check(CrucibleEnergyRules.accepts(GregTechTags.Energy.KU),"KU accepted");
        check(CrucibleEnergyRules.accepts(GregTechTags.Energy.CU),"CU accepted");
        check(CrucibleEnergyRules.accepts(GregTechTags.Energy.VIS_IGNIS),"Ignis accepted");
        check(!CrucibleEnergyRules.accepts(GregTechTags.Energy.EU),"electricity not a crucible heat source");
        var iron=GTMaterialRegistry.get("WroughtIron").resolve();
        var air=GTMaterialRegistry.get("Air").resolve();
        var steel=GTMaterialRegistry.get("Steel").resolve();
        var content=new ArrayList<>(List.of(CrucibleMaterialStack.of(iron,u),CrucibleMaterialStack.of(air,2*u)));
        long t=steel.getMeltingPoint();
        var mixed=CrucibleProcess.admit(new ArrayList<>(),content,16*u,10,t,t);
        check(mixed!=null&&mixed==t,"piston air can enter at vessel temperature");
        CrucibleProcess.process(content,t,t,true,true);
        check(content.stream().anyMatch(s->s.material.resolve()==steel&&s.amount==u),"air reacts with wrought iron before venting");
        check(content.stream().noneMatch(s->s.material.resolve()==air),"surplus air escapes in same processing tick");
        var gasOnly=new ArrayList<>(List.of(CrucibleMaterialStack.of(air,u)));
        CrucibleProcess.process(gasOnly,293,293,true,true);
        check(gasOnly.isEmpty(),"unreacted air escapes even when cold");
        System.out.println(checks+" focused air/reaction/venting checks passed.");
    }
}
