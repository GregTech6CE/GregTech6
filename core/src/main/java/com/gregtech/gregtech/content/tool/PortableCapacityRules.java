package com.gregtech.gregtech.content.tool;
import com.gregtech.gregtech.api.fluid.PortableFluidContainerSpec;
/** Source measuring-pot/barometer bands and clamp, shared by both storage adapters. */
public final class PortableCapacityRules {
 private PortableCapacityRules(){}public static boolean adjustable(PortableFluidContainerSpec s){return s.shapeId().equals("measuring_pot")||s.shapeId().equals("barometer_gas_cylinder");}
 public static int capacity(PortableFluidContainerSpec s,long value){return (int)Math.max(1,Math.min(s.capacity(),value));}
 public static int adjust(PortableFluidContainerSpec s,int current,double y,boolean precise){if(!adjustable(s))return s.capacity();int step=s.shapeId().equals("measuring_pot")?(y>6/16.0?50:y>4/16.0?10:y>2/16.0?-10:-50):(y>14/16.0?500:y>12/16.0?100:y>10/16.0?50:y>8/16.0?10:y>6/16.0?-10:y>4/16.0?-50:y>2/16.0?-100:-500);if(precise)step/=10;return capacity(s,current+step);}
}
