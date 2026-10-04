package com.gregtech.gregtech.worldgen;
/** Original spring identities and complete crater executor shared by both loaders. */
public final class FluidSpringRules {
 private FluidSpringRules(){}
 public record Spring(String name,String fluidId,String blockId,int probability,int indicatorType,int amount){}
    public static final java.util.List<Spring> SPRINGS = java.util.List.of(
            new Spring("overworld.fluid.oil.extraheavy", "gregtech:liquid_extra_heavy_oil",
                    "gregtech:liquid_extra_heavy_oil", 400, 2, 6000),
            new Spring("overworld.fluid.oil.heavy", "gregtech:liquid_heavy_oil",
                    "gregtech:liquid_heavy_oil", 400, 2, 6000),
            new Spring("overworld.fluid.oil.medium", "gregtech:liquid_medium_oil",
                    "gregtech:liquid_medium_oil", 400, 2, 6000),
            new Spring("overworld.fluid.oil.light", "gregtech:liquid_light_oil",
                    "gregtech:liquid_light_oil", 400, 2, 6000),
            new Spring("overworld.fluid.gas.natural", "gregtech:gas_natural_gas",
                    "gregtech:gas_natural_gas", 200, 1, 3000),
            new Spring("overworld.fluid.water", "gregtech:watergeothermal",
                    "gregtech:watergeothermal", 100, 3, 500),
            new Spring("overworld.fluid.lava", "minecraft:lava", "minecraft:lava", 200, 1, 1000));
 public static Spring byFluid(String id){return SPRINGS.stream().filter(s->s.fluidId().equals(id)).findFirst().orElse(null);}
 public static String blockPath(Spring spring){return "fluid_spring_"+spring.fluidId().substring(spring.fluidId().indexOf(':')+1);}
 public static int positiveAmount(int amount){return Math.max(1,amount);}
 public static boolean rolls(java.util.function.IntUnaryOperator random,int amount){return random.applyAsInt(positiveAmount(amount))==0;}
 public interface CraterSink {void filler(int x,int relativeY,int z);void fluid(int x,int relativeY,int z);boolean bedrock(int x,int z);boolean spring(int x,int z);}
 public static boolean carve(int minX,int minZ,java.util.function.IntUnaryOperator random,CraterSink sink){boolean placed=false;for(int i=0;i<7;i++)for(int x=minX+i;x<=minX+15-i;x++)for(int z=minZ+i;z<=minZ+15-i;z++){sink.filler(x,1+i,z);if(i>0){sink.fluid(x,i,z);placed=true;}if(i>2&&random.applyAsInt(16)==0&&sink.bedrock(x,z)){if(sink.spring(x,z))placed=true;}}return placed;}
}
