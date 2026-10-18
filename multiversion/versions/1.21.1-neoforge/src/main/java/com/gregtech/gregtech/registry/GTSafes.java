package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.block.inventory.SafeBlock;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockBehaviour;import net.minecraft.world.level.material.MapColor;import net.neoforged.neoforge.registries.DeferredHolder;
public final class GTSafes {
 private GTSafes(){} private static final java.util.List<DeferredHolder<Block,SafeBlock>> ALL=new java.util.ArrayList<>();
 public static java.util.List<DeferredHolder<Block,SafeBlock>> all(){return java.util.List.copyOf(ALL);}
 public static SafeBlock safe(com.gregtech.gregtech.api.material.GTMaterial material,boolean keyLocked){for(var b:ALL)if(b.get().material()==material&&b.get().keyLocked()==keyLocked)return b.get();return null;}
 public static void initialize(){for(var spec:GTStorageMetals.ALL)for(boolean locked:new boolean[]{false,true}){
  String id=!locked&&spec.suffix().equals("steel")?"safe":(locked?"key_safe_":"safe_")+spec.suffix();
  var b=GTBlocks.BLOCKS.register(id,()->new SafeBlock(spec.material(),locked,BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(spec.hardness()*2,spec.resistance()*2).requiresCorrectToolForDrops()));ALL.add(b);
  GTBlocks.BLOCK_ITEMS.register(id,()->new net.minecraft.world.item.BlockItem(b.get(),new net.minecraft.world.item.Item.Properties().stacksTo(16)));
 }}
}
