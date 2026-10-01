package com.gregtech.gregtech.api.recipe;

import java.math.BigInteger;

/** GT6 work accounting: efficient parallel batches, then optional 4x-power / 2x-speed overclocking. */
public final class MachineWorkCost {
    public record Cost(long minimumPower,long totalWork) {}
    private MachineWorkCost() {}
    public static Cost calculate(long recipePower,long duration,int parallel,boolean scaleDuration,int efficiency,long minimumInput,long maximumInput,boolean cheapOverclocking) {
        if(recipePower<0||recipePower>maximumInput||duration<1||parallel<1||efficiency<1)return null;
        long power=Math.max(1,recipePower);
        BigInteger work=BigInteger.valueOf(power).multiply(BigInteger.valueOf(duration))
                .multiply(BigInteger.valueOf(scaleDuration?parallel:1)).multiply(BigInteger.valueOf(10000))
                .add(BigInteger.valueOf(efficiency-1)).divide(BigInteger.valueOf(efficiency));
        if(!cheapOverclocking)while(power<minimumInput&&power<=maximumInput/4){power*=4;work=work.multiply(BigInteger.TWO);}
        if(work.compareTo(BigInteger.valueOf(Long.MAX_VALUE))>0)return null;
        return new Cost(power,Math.max(1,work.longValue()));
    }
}
