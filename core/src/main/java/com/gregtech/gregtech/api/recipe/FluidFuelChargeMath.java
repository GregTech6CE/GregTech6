package com.gregtech.gregtech.api.recipe;
import java.math.BigInteger;
/** Source negative-EUt fuel-charge calculation with exact bounded arithmetic. */
public final class FluidFuelChargeMath {private FluidFuelChargeMath(){}public static long energy(long eut,long duration,int efficiency){var value=BigInteger.valueOf(eut).abs().multiply(BigInteger.valueOf(duration)).multiply(BigInteger.valueOf(efficiency)).divide(BigInteger.valueOf(10000));return value.signum()<=0||value.compareTo(BigInteger.valueOf(Long.MAX_VALUE))>0?0:value.longValue();}}
