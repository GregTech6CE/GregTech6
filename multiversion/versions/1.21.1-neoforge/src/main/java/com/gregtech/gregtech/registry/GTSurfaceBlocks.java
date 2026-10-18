package com.gregtech.gregtech.registry;
import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockBehaviour;import net.minecraft.world.level.material.MapColor;import net.minecraft.world.item.*;import com.gregtech.gregtech.block.misc.DiggableBlock;import com.gregtech.gregtech.block.wood.FallenLogBlock;
public final class GTSurfaceBlocks {
 private GTSurfaceBlocks(){}
 private static void reg(String id,java.util.function.Supplier<Block> create){var block=GTBlocks.BLOCKS.register(id,create);GTBlocks.BLOCK_ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties()));}
 public static void initialize(){
  for(var kind:DiggableBlock.Variant.values())reg(kind.iconName(),()->new DiggableBlock(kind));
  reg("diggable_clay",()->new DiggableBlock(DiggableBlock.Variant.BROWN_CLAY));reg("diggable_peat",()->new DiggableBlock(DiggableBlock.Variant.TURF));
  for(var kind:FallenLogBlock.Kind.values())reg("log_"+kind.name().toLowerCase(java.util.Locale.ROOT),()->new FallenLogBlock(kind,BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1,3).sound(SoundType.WOOD)));
  GTBushes.registerAll();
 }
}
