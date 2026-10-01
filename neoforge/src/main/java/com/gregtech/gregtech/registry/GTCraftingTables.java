package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.block.misc.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockBehaviour;import net.minecraft.world.level.material.MapColor;import net.neoforged.neoforge.registries.DeferredHolder;
public final class GTCraftingTables {private GTCraftingTables(){}public static final java.util.List<DeferredHolder<Block,AdvancedCraftingTableBlock>> ADVANCED=new java.util.ArrayList<>();public static final java.util.List<DeferredHolder<Block,ChargingCraftingTableBlock>> CHARGING=new java.util.ArrayList<>();
 public static void initialize(){for(var spec:GTStorageMetals.ALL){
  String suffix=spec.suffix().equals("steel")?"":"_"+spec.suffix();
  var a=GTBlocks.BLOCKS.register("advanced_crafting_table"+suffix,()->new AdvancedCraftingTableBlock(spec.material(),BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(spec.hardness(),spec.resistance()).requiresCorrectToolForDrops()));ADVANCED.add(a);
  GTBlocks.BLOCK_ITEMS.register("advanced_crafting_table"+suffix,()->new net.minecraft.world.item.BlockItem(a.get(),new net.minecraft.world.item.Item.Properties().stacksTo(16)));
  var c=GTBlocks.BLOCKS.register("charging_crafting_table"+suffix,()->new ChargingCraftingTableBlock(spec.material(),BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(spec.hardness(),spec.resistance()).requiresCorrectToolForDrops()));CHARGING.add(c);
  GTBlocks.BLOCK_ITEMS.register("charging_crafting_table"+suffix,()->new net.minecraft.world.item.BlockItem(c.get(),new net.minecraft.world.item.Item.Properties()));
 }}
}
