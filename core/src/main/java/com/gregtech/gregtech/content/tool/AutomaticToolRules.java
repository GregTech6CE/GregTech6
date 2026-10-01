package com.gregtech.gregtech.content.tool;
import java.util.List;
/** Original baseline automatic-tool identities, packet work and recharge timing. */
public final class AutomaticToolRules {private AutomaticToolRules(){}
 public record Profile(String id,boolean igniter,float hardness,long input,int quality){}
 public static final List<Profile> ALL=List.of(new Profile("auto_igniter_steel",true,6,8,0),new Profile("auto_igniter_aluminium",true,4,32,1),new Profile("auto_igniter_stainless",true,7,128,2),new Profile("auto_igniter_titanium",true,9,512,3),new Profile("auto_igniter_tungsten",true,12,2048,4),new Profile("auto_igniter_ultimet",true,14,8192,5),new Profile("auto_hammer_steel",false,6,8,1),new Profile("auto_hammer_aluminium",false,4,32,2),new Profile("auto_hammer_titanium",false,9,128,3),new Profile("auto_hammer_tungsten",false,12,512,4));
 public static Profile profile(String id){return ALL.stream().filter(p->p.id().equals(id)).findFirst().orElseThrow();}
 public static boolean igniterDue(long time){return time%10==0;}public static final int RECHARGE_STEPS=10;
 public static long hammerStored(long stored,long size,long amount){return stored+Math.min((Long.MAX_VALUE/20-stored)/size,amount)*size;}
 public static long hammerBudget(long stored){return stored*10;}public static long igniterBudget(long stored){return stored*20;}
 public static long hammerSaved(long stored){return Math.min(Math.max(0,stored),Long.MAX_VALUE/20);}
 public static boolean overvoltage(long size,long input){return size>input*2||size< -input*2;}
}
