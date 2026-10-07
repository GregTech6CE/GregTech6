/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * MultiTileEntityLargeHeatExchanger: source defaults and bounded fuel-loop arithmetic. */
package com.gregtech.gregtech.content.multiblock;

import java.math.BigInteger;
import com.gregtech.gregtech.data.GregTechTags;

public final class HeatExchangerRules {
    private HeatExchangerRules() {}
    public record Settings(long rate, int efficiency, String fuelMap, String energyId) {
        public Settings {
            rate=Math.max(0,rate); efficiency=Math.max(0,Math.min(10000,efficiency));
        }
        public GregTechTags.Tag energyType() { return GregTechTags.Energy.byId(energyId); }
        public long inputCapacity() { return multiple(rate,10); }
        public long bufferTarget() { return multiple(rate,2); }
        public long outputBackpressure() { return multiple(rate,20); }
    }
    public static final Settings DEFAULTS=new Settings(OriginalGeneratorParameters.HEAT_EXCHANGER_RATE,
            10000,"gt.recipe.fuels.hot",GregTechTags.Energy.HU.getId());
    public static long multiple(long value,int factor) {
        return value>Long.MAX_VALUE/factor?Long.MAX_VALUE:Math.max(0,value)*factor;
    }
    public static long perOutlet(long rate,long energy) { return Math.min(Math.max(0,rate)/8,Math.max(0,energy)/8); }
    public record Charge(long inputUsed,long outputMade,long energyAdded,long batches) {}
    /** Equivalent to the source repeated single-charge loop, including zero-efficiency fuel consumption. */
    public static Charge charge(long input,long inputPerBatch,long output,long outputPerBatch,
                                long buffered,long target,long eut,long duration,int efficiency) {
        if(inputPerBatch<=0||input<inputPerBatch||output<0||outputPerBatch<0||buffered<0||target<=buffered||duration<0)return null;
        var energy=BigInteger.valueOf(eut).abs().multiply(BigInteger.valueOf(duration))
                .multiply(BigInteger.valueOf(Math.max(0,Math.min(10000,efficiency)))).divide(BigInteger.valueOf(10000));
        if(energy.compareTo(BigInteger.valueOf(Long.MAX_VALUE))>0)return null;
        long perBatch=energy.longValue(),count=input/inputPerBatch;
        if(outputPerBatch>0)count=Math.min(count,(Long.MAX_VALUE-output)/outputPerBatch);
        if(perBatch>0) {
            long needed=target-buffered;
            count=Math.min(count,needed/perBatch+(needed%perBatch==0?0:1));
            count=Math.min(count,(Long.MAX_VALUE-buffered)/perBatch);
        }
        return count<=0?null:new Charge(count*inputPerBatch,count*outputPerBatch,count*perBatch,count);
    }
}
