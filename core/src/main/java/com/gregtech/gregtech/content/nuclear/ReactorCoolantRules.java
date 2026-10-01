package com.gregtech.gregtech.content.nuclear;
import java.util.*;
/** Original heat conversion units; identity lookup never accepts a fluid by substring. */
public enum ReactorCoolantRules {
    DISTILLED("DistW","Steam",80,160,1,true),
    INDUSTRIAL("Coolant_IC2","Coolant_IC2_Hot",20,1,1,false),
    TIN("GenMolten_Tin","Hot_Molten_Tin",40,1,3,false),
    SODIUM("GenMolten_Sodium","Hot_Molten_Sodium",30,1,6,false),
    SEMIHEAVY("GenLiquid_SemiheavyWater","Hot_Semi_Heavy_Water",40,1,1,true),
    HEAVY("GenLiquid_HeavyWater","Hot_Heavy_Water",50,1,1,true),
    TRITIATED("GenLiquid_TritiatedWater","Hot_Tritiated_Water",60,1,1,true),
    LITHIUM_CHLORIDE("GenLiquid_LithiumChloride","Hot_Molten_LiCl",15,1,1,false),
    CO2("CarbonDioxide","Hot_Carbon_Dioxide",20,1,1,false),
    HELIUM("Helium","Hot_Helium",30,1,1,false),
    THORIUM_SALT("Thorium_Salt","GenLiquid_LithiumChloride",2560000,1,1,false);
    public final String input,output; public final int heat,expansion,heatDivider;public final boolean moderates;
    ReactorCoolantRules(String i,String o,int h,int e,int d,boolean m){input=i;output=o;heat=h;expansion=e;heatDivider=d;moderates=m;}
    public int self(ReactorRodCatalog.Rod r){return switch(this){case INDUSTRIAL->r.self()*4;case CO2->r.self()*3;case LITHIUM_CHLORIDE->r.self()*5;case THORIUM_SALT->0;default->r.self();};}
    public int emission(ReactorRodCatalog.Rod r){return switch(this){case INDUSTRIAL->r.emission()*4;case HELIUM,LITHIUM_CHLORIDE,THORIUM_SALT->r.emission()/2;default->r.emission();};}
    public int divisor(ReactorRodCatalog.Rod r){return Math.max(1,switch(this){case INDUSTRIAL->r.divisor()*2;case TIN,SODIUM,THORIUM_SALT->r.divisor()-1;default->r.divisor();});}
    public int maximum(ReactorRodCatalog.Rod r){return switch(this){case HEAVY->(r.maximum()+7)/8;case TRITIATED->(r.maximum()+15)/16;case LITHIUM_CHLORIDE->r.maximum()+(r.maximum()+3)/4;case THORIUM_SALT->r.maximum()*4;default->r.maximum();};}
}
