package com.gregtech.gregtech.content.energy;
import java.util.List;
/** Original GTElectricItems five retained battery IDs; capacities in EU and original tiers. */
public final class LegacyBatteryDefinitions {
 private LegacyBatteryDefinitions(){}
 public record Spec(String id,String name,long capacity,int tier){}
 public static final List<Spec> ALL=List.of(new Spec("battery_lv","LV Battery",100_000,1),new Spec("battery_mv","MV Battery",400_000,2),new Spec("battery_hv","HV Battery",1_600_000,3),new Spec("battery_ev","EV Battery",6_400_000,4),new Spec("battery_iv","IV Battery",25_600_000,5));
 public static Spec get(String id){return ALL.stream().filter(spec->spec.id().equals(id)).findFirst().orElseThrow();}
 public static long capacity(String id){return get(id).capacity();}
}
