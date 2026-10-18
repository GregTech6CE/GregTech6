package com.gregtech.gregtech.worldgen;
import com.gregtech.gregtech.registry.GTBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}
/** Platform block/key resolution over the shared original surface flora table. */
public final class GTSurfaceFlora extends SurfaceFloraRules {
 private GTSurfaceFlora(){}
 public static int twigMultiplier(ResourceLocation biome){return SurfaceFloraRules.twigMultiplier(biome==null?null:biome.toString());}
 public static boolean glowtusBiome(ResourceLocation biome){return SurfaceFloraRules.glowtusBiome(biome==null?null:biome.toString());}
//? if >=1.21.1 {
 /*public static Block logBlock(Log log){return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce",log.blockId()));}
*///?} else {
 public static Block logBlock(Log log){return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gt6ce",log.blockId()));}
//?}
 public static Block twigs(){return GTBlocks.TWIGS.get();}
//? if >=1.21.1 {
 /*public static Block glowtus(String colour){return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce","glowtus_"+colour));}
*///?} else {
 public static Block glowtus(String colour){return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gt6ce","glowtus_"+colour));}
//?}
}
