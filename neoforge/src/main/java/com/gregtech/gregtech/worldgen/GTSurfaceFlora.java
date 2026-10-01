package com.gregtech.gregtech.worldgen;
import com.gregtech.gregtech.registry.GTBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
/** Platform block/key resolution over the shared original surface flora table. */
public final class GTSurfaceFlora extends SurfaceFloraRules {
 private GTSurfaceFlora(){}
 public static int twigMultiplier(ResourceLocation biome){return SurfaceFloraRules.twigMultiplier(biome==null?null:biome.toString());}
 public static boolean glowtusBiome(ResourceLocation biome){return SurfaceFloraRules.glowtusBiome(biome==null?null:biome.toString());}
 public static Block logBlock(Log log){return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech",log.blockId()));}
 public static Block twigs(){return GTBlocks.TWIGS.get();}
 public static Block glowtus(String colour){return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","glowtus_"+colour));}
}
