package com.gregtech.gregtech.content.transport.fluid;
import com.gregtech.gregtech.api.machine.TankSpec;import com.gregtech.gregtech.content.material.Materials;
public final class LogisticsTankSpec {private LogisticsTankSpec(){}public static TankSpec spec(){return TankSpec.of("logistics_tank",Materials.Tungsten,TankSpec.TankType.LOGISTICS_BARREL,1_000_000,true,true,true,true,false,1f,10f).withMaxTemperature(100_000);}}
