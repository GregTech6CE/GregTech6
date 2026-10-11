package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.machine.crucible.*;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.content.multiblock.OriginalLargeCrucibleParameters;
import java.util.ArrayList;
import java.util.List;

/** Independent original Smeltery:247-320 / Crucible:297-377 / UT.Code.scale fixtures. */
final class CrucibleHazardContracts {
    private static int assertions;
    static int verify() {
        long u=GTValues.U;
        require(OriginalLargeCrucibleParameters.meltDownTemperatureK(2046)==2250, "Steel1.10 limit truncates2250.6");
        require(OriginalLargeCrucibleParameters.meltDownTemperatureK(1941)==2135, "Titanium1.10 limit truncates2135.1");
        require(CrucibleHazards.SMALL.range()==3 && CrucibleHazards.SMALL.temperatureMultiplier()==2F
                && CrucibleHazards.SMALL.displayedFireRange()==4, "small original gas/flame3, damage2, visible4m");
        require(CrucibleHazards.LARGE.range()==5 && CrucibleHazards.LARGE.temperatureMultiplier()==4F
                && CrucibleHazards.LARGE.displayedFireRange()==6, "large original gas/flame5, damage4, visible6m");
        for(var profile:List.of(CrucibleHazards.SMALL,CrucibleHazards.LARGE)) {
            int r=profile.range();
            require(CrucibleHazards.fireTarget(profile,bound->0).equals(new CrucibleHazards.FireTarget(-r,-1,-r,false)), "fire lowest source offsets");
            require(CrucibleHazards.fireTarget(profile,bound->bound-1).equals(new CrucibleHazards.FireTarget(r,r,r,true)), "fire highest source offsets and2/3 flammability roll");
            require(CrucibleHazards.explosionPower(profile,0)==0 && CrucibleHazards.explosionPower(profile,1)==1, "positive minimum blast step");
            require(CrucibleHazards.explosionPower(profile,profile.capacity()/2)==(profile.largeVessel()?4:3), "source stepped half-full blast");
            require(CrucibleHazards.explosionPower(profile,profile.capacity()-1)==profile.explosionScale()-1
                    && CrucibleHazards.explosionPower(profile,profile.capacity())==profile.explosionScale(), "full capacity is the only final blast step");
        }
        require(CrucibleHazards.vaporFireAttempts(1999,16*u)==0, "below2000K material boiling point cannot ignite");
        require(CrucibleHazards.vaporFireAttempts(2000,1)==1 && CrucibleHazards.vaporFireAttempts(2000,u/9)==1
                && CrucibleHazards.vaporFireAttempts(2000,u)==9 && CrucibleHazards.vaporFireAttempts(2000,16*u)==144, "original9 attempts per materialU with positive minimum");
        require(CrucibleHazards.vaporFireAttempts(2000,Long.MAX_VALUE)==Integer.MAX_VALUE, "bindInt without intermediate overflow");
        require(CrucibleHazards.meltdownFireAttempts(2250)==90 && CrucibleHazards.meltdownFireAttempts(2249)==89, "meltdown attempts T/25 truncate");

        var hydrogen=GTMaterialRegistry.get("H");
        var charge=new ArrayList<>(List.of(CrucibleMaterialStack.of(hydrogen,u)));
        var phase=CrucibleProcess.process(charge,3000,3000,true,true);
        require(hydrogen.getDensity()<=0.0012F && charge.isEmpty() && phase.fizzCount()==1 && phase.vapors().isEmpty()
                && phase.explosiveAmount()==0, "low-density gas branch has only fizz, even above boiling");
        var water=GTMaterialRegistry.get("Water");
        charge=new ArrayList<>(List.of(CrucibleMaterialStack.of(water,u)));
        phase=CrucibleProcess.process(charge,water.getBoilingPoint(),293,true,true);
        require(charge.isEmpty() && phase.vaporizedStacks()==1 && phase.vapors().get(0).material()==water
                && phase.vapors().get(0).amount()==u && phase.explosiveAmount()==0, "water vapor captures material and amount, no explosion");
        require(CrucibleHazards.vaporFireAttempts(phase.vapors().get(0).material().getBoilingPoint(),u)==0, "steam has no fake high-vessel-temperature ignition");
        var gunpowder=GTMaterialRegistry.get("Gunpowder");
        charge=new ArrayList<>(List.of(CrucibleMaterialStack.of(gunpowder,u),CrucibleMaterialStack.of(GTMaterialRegistry.get("Cu"),u)));
        phase=CrucibleProcess.process(charge,314,313,true,true);
        require(charge.isEmpty() && phase.explosiveAmount()==u && phase.fizzCount()==1 && phase.vaporizedStacks()==1,
                "flammable explosive vapor destroys remaining contents before later conversion");
        require(phase.vapors().get(0).material()==gunpowder && gunpowder.getBoilingPoint()==2632
                && CrucibleHazards.vaporFireAttempts(2632,u)==9, "burning gunpowder uses2632K gas effects although vessel is314K");
        try { phase.vapors().clear(); throw new IllegalStateException("mutable phase events"); }
        catch(UnsupportedOperationException expected) { assertions++; }
        return assertions;
    }
    private static void require(boolean ok,String why) { assertions++; if(!ok)throw new IllegalStateException(why); }
}
