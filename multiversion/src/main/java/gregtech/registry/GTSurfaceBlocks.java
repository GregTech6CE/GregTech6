package gregtech.registry;
import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockBehaviour;import net.minecraft.world.level.material.MapColor;import net.minecraft.world.item.*;import gregtech.block.misc.DiggableBlock;import gregtech.block.wood.FallenLogBlock;
public final class GTSurfaceBlocks {
 private GTSurfaceBlocks(){}
 private static void reg(String id,java.util.function.Supplier<Block> create){var block=RegisteredBlock.registerBlock(id,create);GTBlocks.BLOCK_ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties()));}
 public static void initializeDiggables(){for(var kind:DiggableBlock.Variant.values())reg(kind.iconName(),()->new DiggableBlock(kind));}
 public static void initialize(){
  initializeDiggables();
  for(var kind:FallenLogBlock.Kind.values())reg("log_"+kind.name().toLowerCase(java.util.Locale.ROOT),()->new FallenLogBlock(kind,BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1,3).sound(SoundType.WOOD)));
  GTBushes.registerAll();
 }
}
