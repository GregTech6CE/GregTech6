package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.block.energy.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.capabilities.*;
import java.util.*;
/** Complete original node catalog. Real one/four-rod reactors share the original catalog. */
public final class GTEnergyNodes {
 private GTEnergyNodes(){}
 public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,"gregtech");
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,"gregtech");
 public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"gregtech");
 public static final DeferredHolder<Block,ReactorCoreBlock> REACTOR_CORE_BLOCK=BLOCKS.register("reactor_core",()->new ReactorCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(8,12).requiresCorrectToolForDrops()));
 public static final DeferredHolder<Block,ReactorCore2x2Block> REACTOR_CORE_2X2=BLOCKS.register("reactor_core_2x2",ReactorCore2x2Block::new);
 public static final DeferredHolder<Block,ReactorCasingBlock> REACTOR_CASING=BLOCKS.register("reactor_casing",ReactorCasingBlock::new);
 static{ITEMS.register("reactor_core",()->new BlockItem(REACTOR_CORE_BLOCK.get(),new Item.Properties()));ITEMS.register("reactor_core_2x2",()->new BlockItem(REACTOR_CORE_2X2.get(),new Item.Properties()));ITEMS.register("reactor_casing",()->new BlockItem(REACTOR_CASING.get(),new Item.Properties()));}
 private static final List<DeferredHolder<Block,EnergyNodeBlock>> ALL=new ArrayList<>();
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity>> ENERGY_NODE=ENTITIES.register("energy_node",()->BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity::new,supportedBlocks()).build(null));
 private static Block[] supportedBlocks(){var blocks=new ArrayList<Block>();ALL.forEach(holder->blocks.add(holder.get()));GTGearboxes.allTransformers().forEach(holder->blocks.add(holder.get()));GTChemicalBatteries.legacy().forEach(holder->blocks.add(holder.get()));return blocks.toArray(Block[]::new);}
 public static List<DeferredHolder<Block,EnergyNodeBlock>> all(){return Collections.unmodifiableList(ALL);}
 private static void add(EnergyNodeSpec spec){
  var block=BLOCKS.register(spec.id(),()->{var props=BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4,4).requiresCorrectToolForDrops();if(spec.kind()==EnergyNodeSpec.Kind.SOLAR)props=props.noOcclusion();return spec.kind()==EnergyNodeSpec.Kind.MAGNET?new MagnetMachineBlock(spec,props):spec.batterySlots()>0?new BatteryBoxBlock(spec,props):spec.id().startsWith("transformer_")?new ElectricTransformerBlock(spec,props):spec.kind()==EnergyNodeSpec.Kind.SOLAR?new SolarPanelBlock(spec,props):new EnergyNodeBlock(spec,props);});
  ALL.add(block);ITEMS.register(spec.id(),()->new BlockItem(block.get(),new Item.Properties()));
 }
 public static void register(IEventBus bus){if(!ALL.isEmpty())throw new IllegalStateException("Energy nodes registered twice");com.gregtech.gregtech.content.energy.EnergyNodeDefinitions.specifications().forEach(GTEnergyNodes::add);com.gregtech.gregtech.content.energy.MagnetMachineDefinitions.specifications().forEach(GTEnergyNodes::add);BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);bus.addListener(GTEnergyNodes::capabilities);}
 private static void capabilities(RegisterCapabilitiesEvent event){var type=ENERGY_NODE.get();event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,type,com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity::energyCapability);event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,type,com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity::itemCapability);event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,type,com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity::fluidCapability);}
}
