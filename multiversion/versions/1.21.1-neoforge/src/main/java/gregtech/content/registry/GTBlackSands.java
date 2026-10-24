package gregtech.content.registry;
import gregtech.content.block.*;import net.minecraft.world.level.block.SoundType;import net.minecraft.world.level.block.state.BlockBehaviour;
public final class GTBlackSands {private GTBlackSands(){}public static void registerCompositions(){
      for(String id:BlackSandDefinitions.IDS){
       var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce",id));
       if(block instanceof BlackSandBlock sand)gregtech.internal.material.ItemMaterialRegistry.register(sand.asItem(),null,sand.material(),gregtech.api.material.GTValues.U*9);
      }
     }
     public static void initialize(){for(String id:BlackSandDefinitions.IDS){net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.block.Block,net.minecraft.world.level.block.Block> block=GTBlocks.BLOCKS.register(id,()->new BlackSandBlock(BlockBehaviour.Properties.of().strength(.5f,.5f).requiresCorrectToolForDrops().sound(SoundType.SAND),BlackSandBlock.spec(id)));GTBlocks.registerBlockItem(id,block);GTBlocks.bind(gregtech.internal.prefix.BlockMaterialPrefix.blockDust,
             gregtech.internal.material.GTMaterialRegistry.get(BlackSandDefinitions.SPECS.get(id).material()),block);}}}
