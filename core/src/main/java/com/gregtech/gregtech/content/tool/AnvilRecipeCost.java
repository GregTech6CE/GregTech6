package com.gregtech.gregtech.content.tool;
import com.gregtech.gregtech.api.material.GTValues;
public final class AnvilRecipeCost {private AnvilRecipeCost(){}
    public static long duration(long units,int quality) {
        var numerator=java.math.BigInteger.valueOf(units).multiply(java.math.BigInteger.valueOf(64L*(Math.max(0,quality)+1L)));
        var unit=java.math.BigInteger.valueOf(GTValues.U);
        return numerator.add(unit.subtract(java.math.BigInteger.ONE)).divide(unit).max(java.math.BigInteger.ONE)
                .min(java.math.BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }
}
