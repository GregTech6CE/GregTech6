package com.gregtech.gregtech.content.tool;
import com.gregtech.gregtech.api.tool.Paintable;import net.minecraft.core.BlockPos;import net.minecraft.core.Direction;import net.minecraft.core.registries.BuiltInRegistries;import net.minecraft.resources.ResourceLocation;import net.minecraft.world.level.Level;import net.minecraft.world.level.block.*;
/** Brokestar spray target policy; original identity/property ownership remains in existing blocks. */
public final class PaintTargets {private PaintTargets(){}
private static final java.util.Map<Block,String> FAMILY=new java.util.HashMap<>();
static{for(var color:net.minecraft.world.item.DyeColor.values())for(String suffix:java.util.List.of("terracotta","stained_glass","stained_glass_pane","wool","carpet"))FAMILY.put(vanilla(color.getName()+"_"+suffix),suffix);FAMILY.put(Blocks.GLASS,"stained_glass");FAMILY.put(Blocks.GLASS_PANE,"stained_glass_pane");FAMILY.put(Blocks.TERRACOTTA,"terracotta");}
private static Block vanilla(String path){return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("minecraft",path));}
private static boolean grass(Block block){var id=BuiltInRegistries.BLOCK.getKey(block);return id.getNamespace().equals("gregtech")&&PaintingRules.GRASS_IDS.contains(id.getPath());}
public static Block colorTarget(Block block,int index){Block target;String suffix=FAMILY.get(block);if(suffix!=null){String color=net.minecraft.world.item.DyeColor.byId(PaintingRules.vanillaDyeId(index)).getName();target=vanilla(color+"_"+suffix);}else if(block==Blocks.GRASS_BLOCK||grass(block)){String id=PaintingRules.grassId(index);if(id==null)return null;target=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech",id));if(target==Blocks.AIR)return null;}else return null;return target==block?null:target;}
public static long color(Level level,BlockPos pos,int index){var be=level.getBlockEntity(pos);if(be instanceof Paintable target){boolean changed=target.isPainted()?target.mixPaint(PaintingRules.DYES.get(index).rgb()):target.paint(PaintingRules.DYES.get(index).rgb());return changed?PaintingRules.BLOCK_COST:0;}
var state=level.getBlockState(pos);Block target=colorTarget(state.getBlock(),index);return target!=null&&level.setBlock(pos,target.defaultBlockState(),3)?PaintingRules.BLOCK_COST:0;}
public static boolean unpaint(Level level,BlockPos pos){if(level.getBlockEntity(pos) instanceof Paintable target&&target.unpaint())return true;var block=level.getBlockState(pos).getBlock();return grass(block)&&level.setBlock(pos,Blocks.GRASS_BLOCK.defaultBlockState(),3);}
}
