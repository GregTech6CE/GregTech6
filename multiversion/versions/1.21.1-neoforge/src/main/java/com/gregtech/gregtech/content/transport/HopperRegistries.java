package com.gregtech.gregtech.content.transport;
import com.gregtech.gregtech.api.machine.HopperSpec;import com.gregtech.gregtech.block.machine.*;import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.world.level.block.Block;import net.minecraft.world.level.block.state.BlockBehaviour;import net.minecraft.world.level.material.MapColor;import net.minecraft.world.item.Item;import net.neoforged.neoforge.registries.*;import net.neoforged.bus.api.IEventBus;
public final class HopperRegistries {
 private HopperRegistries(){}
 public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("gregtech");public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("gregtech");
 private static final java.util.List<DeferredBlock<HopperBlock>> HOPPERS=new java.util.ArrayList<>();private static final java.util.List<DeferredBlock<QueueHopperBlock>> QUEUES=new java.util.ArrayList<>();
 static{for(var row:HopperCatalog.ALL){var spec=row.spec();if(row.queue()){var block=BLOCKS.register(spec.id(),()->new QueueHopperBlock(spec,properties(spec)));QUEUES.add(block);ITEMS.register(spec.id(),()->new QueueHopperBlockItem(block.get(),new Item.Properties().stacksTo(16),spec));}else{var block=BLOCKS.register(spec.id(),()->new HopperBlock(spec,properties(spec)));HOPPERS.add(block);ITEMS.register(spec.id(),()->new HopperBlockItem(block.get(),new Item.Properties().stacksTo(16),spec));}}}
 private static BlockBehaviour.Properties properties(HopperSpec spec){return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(spec.hardness(),spec.blastResistance()).requiresCorrectToolForDrops();}
 public static java.util.List<DeferredBlock<HopperBlock>> hoppers(){return java.util.Collections.unmodifiableList(HOPPERS);}public static java.util.List<DeferredBlock<QueueHopperBlock>> queues(){return java.util.Collections.unmodifiableList(QUEUES);}
 public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);bus.addListener(HopperRegistries::capabilities);}
 private static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event){event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,GTBlockEntities.HOPPER.get(),(be,side)->be.capabilityHandler(side));event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,GTBlockEntities.QUEUE_HOPPER.get(),(be,side)->be.capabilityHandler(side));}
}
