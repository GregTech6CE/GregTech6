package com.gregtech.gregtech.content.multiblock;
import java.util.*;
/** Original 17211-17234 rating tables and pure conversion/layout policy. */
public final class OriginalGeneratorParameters {private OriginalGeneratorParameters(){}
 public record GasGrade(String id,String rotor,String casingMaterial,int input,int output,int wallId,float hardness){public int inputMaximum(){return input*2;}public int outputMaximum(){return output*2;}}
    public static final List<GasGrade> GRADES = List.of(
            new GasGrade("large_gas_turbine_main", "Magnalium", "StainlessSteel", 6144, 4096, 18022, 6),
            new GasGrade("large_gas_turbine_trinitanium", "Trinitanium", "Titanium", 12288, 8192, 18026, 9),
            new GasGrade("large_gas_turbine_graphene", "Graphene", "TungstenSteel", 24576, 16384, 18023, 12.5f),
            new GasGrade("large_gas_turbine_vibramantium", "Vibramantium", "Adamantium", 196608, 131072, 18025, 100));
 public record AxialGrade(String id,int materialIndex,boolean steam,int input,int output){public int inputMaximum(){return input*2;}public int outputMaximum(){return output*2;}}
 public static final List<AxialGrade> STEAM=grades(true),DYNAMO=grades(false);
 private static List<AxialGrade> grades(boolean steam){var list=new ArrayList<AxialGrade>();String[] steamIds={"large_turbine_main","large_steam_turbine_trinitanium","large_steam_turbine_graphene","large_steam_turbine_vibramantium"};String[] dynamoIds={"large_dynamo_main","large_dynamo_titanium","large_dynamo_tungstensteel","large_dynamo_adamantium"};for(int i=0;i<4;i++){var material=GRADES.get(i);list.add(new AxialGrade((steam?steamIds:dynamoIds)[i],i,steam,steam?material.input()*2:material.output(),steam?material.output():material.output()*3/4));}return List.copyOf(list);}
 public static boolean usesCopperCoil(boolean steam,int back){return !steam&&(back==1||back==2);}
 public static long convert(long energy,int output,int input){return energy*output/input;}
 public static final int STEAM_CONDENSATION_RATIO=170,HEAT_EXCHANGER_RATE=16384;
}
