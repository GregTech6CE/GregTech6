package com.gregtech.gregtech.core;

import com.gregtech.gregtech.content.multiblock.HeatExchangerRules;

/** Compare bounded planning against the original per-charge loop, not another bulk formula. */
final class HeatExchangerContracts {
    private static int assertions;
    static int verify() {
        assertions=0;
        check(HeatExchangerRules.tapTank(0)==0,"Empty output permits draining unused hot fuel");
        check(HeatExchangerRules.tapTank(1)==1,"Even one unit of output prevents selecting the input");
        check(HeatExchangerRules.tapTank(Long.MAX_VALUE)==1,"Long output retains tap priority");
        for(int efficiency:new int[]{0,1,3333,5000,10000})for(int fuel:new int[]{0,1,2,19,100})
            for(int initial:new int[]{0,1,31,63,64}) {
                long remaining=fuel,output=7,energy=initial,batches=0;
                while(energy<64&&remaining>=2) {
                    remaining-=2;output+=3;energy+=(37L*efficiency)/10000;batches++;
                }
                var plan=HeatExchangerRules.charge(fuel,2,7,3,initial,64,-37,1,efficiency);
                check(batches==0?plan==null:plan!=null&&plan.inputUsed()==fuel-remaining
                        &&plan.outputMade()==output-7&&plan.energyAdded()==energy-initial&&plan.batches()==batches,
                        "Source repeated charge loop "+efficiency+"/"+fuel+"/"+initial);
            }
        var large=HeatExchangerRules.charge(5_000_000_000L,1,4_000_000_000L,1,0,6_000_000_000L,-1,1,10000);
        check(large!=null&&large.inputUsed()==5_000_000_000L&&large.outputMade()==5_000_000_000L,
                "Long input/output amounts exceed native int stacks without truncation");
        var room=HeatExchangerRules.charge(100,1,Long.MAX_VALUE-2,1,0,100,-1,1,10000);
        check(room!=null&&room.batches()==2,"Final two representable output units do not overflow");
        check(HeatExchangerRules.charge(100,1,0,1,0,100,Long.MIN_VALUE,2,10000)==null,
                "Unrepresentable fuel energy must not consume fuel as if efficiency were zero");
        check(HeatExchangerRules.perOutlet(16384,16387)==2048&&HeatExchangerRules.perOutlet(16384,7)==0,
                "Eight equal integer outlets, remainder below eight dissipates at cycle end");
        var defaults=HeatExchangerRules.DEFAULTS;
        check(defaults.inputCapacity()==163840&&defaults.bufferTarget()==32768&&defaults.outputBackpressure()==327680,
                "Source17197 registered rate and thresholds");
        var custom=new HeatExchangerRules.Settings(Long.MAX_VALUE,20000,defaults.fuelMap(),defaults.energyId());
        check(custom.inputCapacity()==Long.MAX_VALUE&&custom.bufferTarget()==Long.MAX_VALUE&&custom.efficiency()==10000,
                "Positive long settings stay bounded without multiplication wraparound");
        return assertions;
    }
    private static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
}
