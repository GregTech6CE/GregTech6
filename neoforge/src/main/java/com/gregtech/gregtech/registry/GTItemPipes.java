package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.api.machine.ItemPipeSpec;import com.gregtech.gregtech.api.material.GTMaterial;import com.gregtech.gregtech.block.machine.ItemPipeBlock;
import net.minecraft.world.item.*;import net.neoforged.neoforge.registries.*;import net.neoforged.bus.api.IEventBus;
public final class GTItemPipes {
 private GTItemPipes(){}
 public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("gregtech");public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("gregtech");
 private static final java.util.List<DeferredBlock<ItemPipeBlock>> ALL=new java.util.ArrayList<>();public static DeferredBlock<ItemPipeBlock> PIPE_MEDIUM_BRASS;
 public static DeferredBlock<ItemPipeBlock> register(String id,GTMaterial material,ItemPipeSpec.ItemPipeSize size,long step,int inv,boolean routing){var spec=ItemPipeSpec.of(id,material,size,step,inv,routing);var block=BLOCKS.register(id,()->new ItemPipeBlock(spec,ItemPipeBlock.defaultProperties(spec)));ALL.add(block);ITEMS.register(id,()->new com.gregtech.gregtech.block.machine.ItemPipeBlockItem(block.get(),new Item.Properties().stacksTo(stackSize(size))));return block;}
 public static int stackSize(ItemPipeSpec.ItemPipeSize size){return switch(size){case MEDIUM,RESTRICTIVE_MEDIUM->64;case LARGE,RESTRICTIVE_LARGE->32;case HUGE,RESTRICTIVE_HUGE->16;};}
 public static java.util.List<DeferredBlock<ItemPipeBlock>> all(){return java.util.Collections.unmodifiableList(ALL);}
 public static void register(IEventBus bus){if(!ALL.isEmpty())throw new IllegalStateException("Item pipes registered twice");com.gregtech.gregtech.content.transport.ItemPipeDefinitions.register();BLOCKS.register(bus);ITEMS.register(bus);bus.addListener(GTItemPipes::capabilities);}
 private static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event){event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,GTBlockEntities.ITEM_PIPE.get(),(be,side)->be.capabilityHandler(side));}
}
