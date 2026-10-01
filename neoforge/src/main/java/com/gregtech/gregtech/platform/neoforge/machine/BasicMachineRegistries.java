package com.gregtech.gregtech.platform.neoforge.machine;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.content.machine.BasicMachineDefinitions;
import com.gregtech.gregtech.api.material.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
/** Native registrations from the shared catalog; original large recipe/matter/distillation factories are bound. */
public final class BasicMachineRegistries {
 private BasicMachineRegistries(){}
 public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,"gregtech");
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,"gregtech");
 public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"gregtech");
 public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"gregtech");
 public static final DeferredHolder<MenuType<?>,MenuType<com.gregtech.gregtech.client.gui.BasicMachineContainerMenu>> MENU=MENUS.register("basic_machine",()->IMenuTypeExtension.create(com.gregtech.gregtech.client.gui.BasicMachineContainerMenu::new));
 private static final List<DeferredHolder<Block,BasicMachineBlock>> MACHINES=new ArrayList<>();
 private static final List<DeferredHolder<BlockEntityType<?>,BlockEntityType<BasicMachineBlockEntity>>> TYPES=new ArrayList<>();
 public static List<DeferredHolder<Block,BasicMachineBlock>> all(){return Collections.unmodifiableList(MACHINES);}
 public static List<DeferredHolder<BlockEntityType<?>,BlockEntityType<BasicMachineBlockEntity>>> types(){return Collections.unmodifiableList(TYPES);}
 public static void register(IEventBus bus){
  for(BasicMachineSpec spec:BasicMachineDefinitions.specifications()){

   var block=BLOCKS.register(spec.id(),()->spec.machineName().equals("distillationtower")?new com.gregtech.gregtech.block.machine.MultiblockControllerBlock(spec,BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(spec.hardness(),spec.blastResistance()).requiresCorrectToolForDrops()):new BasicMachineBlock(spec,BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(spec.hardness(),spec.blastResistance()).requiresCorrectToolForDrops()));
   MACHINES.add(block);
   ITEMS.register(spec.id(),()->new BasicMachineItem(block.get(),new Item.Properties().stacksTo(16)));
   var type=ENTITIES.register("be_"+spec.id(),()->{
    AtomicReference<BlockEntityType<BasicMachineBlockEntity>> ref=new AtomicReference<>();
    var value=BlockEntityType.Builder.of((pos,state)->{var be=block.get().createBlockEntity(ref.get(),pos,state);be.setSpec(spec);return be;},block.get()).build(null);
    ref.set(value);block.get().setBeTypeSupplier(()->value);return value;
   });TYPES.add(type);
  }
  BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);MENUS.register(bus);
  bus.addListener(BasicMachineRegistries::capabilities);
  bus.addListener(com.gregtech.gregtech.network.MachinePayloads::register);
 }
 private static void capabilities(RegisterCapabilitiesEvent event){
  event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.BUMBLIARY.get(),com.gregtech.gregtech.blockentity.tool.BumbliaryBlockEntity::itemCapability);
  event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.BEDROCK_DRILL.get(),com.gregtech.gregtech.blockentity.machine.BedrockDrillControllerBlockEntity::itemCapability);
  event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.REACTOR_CORE.get(),com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity::fluidCapability);
  event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.REACTOR_CORE.get(),com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity::itemCapability);
  event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.REACTOR_CORE_2X2.get(),com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity::fluidCapability);
  event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.REACTOR_CORE_2X2.get(),com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity::itemCapability);
  event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.BEDROCK_DRILL.get(),com.gregtech.gregtech.blockentity.machine.BedrockDrillControllerBlockEntity::fluidCapability);
  event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.LARGE_BOILER.get(),com.gregtech.gregtech.blockentity.machine.LargeBoilerControllerBlockEntity::fluidCapability);
  event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.LARGE_CRUCIBLE.get(),com.gregtech.gregtech.blockentity.machine.LargeCrucibleControllerBlockEntity::fluidCapability);
  event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.LARGE_CRUCIBLE.get(),com.gregtech.gregtech.blockentity.machine.LargeCrucibleControllerBlockEntity::itemCapability);
  event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.LARGE_TURBINE.get(),com.gregtech.gregtech.blockentity.machine.LargeTurbineControllerBlockEntity::fluidCapability);
  event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.LARGE_GAS_TURBINE.get(),com.gregtech.gregtech.blockentity.machine.LargeGasTurbineControllerBlockEntity::fluidCapability);
  event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.LARGE_HEAT_EXCHANGER.get(),com.gregtech.gregtech.blockentity.machine.LargeHeatExchangerControllerBlockEntity::fluidCapability);

  event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.ORIGINAL_LARGE_BOILER.get(),com.gregtech.gregtech.blockentity.machine.OriginalLargeBoilerControllerBlockEntity::fluidCapability);
  event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.MULTIBLOCK_TANK.get(),com.gregtech.gregtech.blockentity.machine.MultiblockTankControllerBlockEntity::fluidCapability);
  event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.COKE_OVEN.get(),com.gregtech.gregtech.blockentity.machine.CokeOvenControllerBlockEntity::itemCapability);
  event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.COKE_OVEN.get(),com.gregtech.gregtech.blockentity.machine.CokeOvenControllerBlockEntity::fluidCapability);
  event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.CRYO_DISTILLATION.get(),com.gregtech.gregtech.blockentity.machine.CryoDistillationControllerBlockEntity::fluidCapability);
  event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.MULTIBLOCK_PORT.get(),com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity::itemCapability);
  event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.MULTIBLOCK_PORT.get(),com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity::fluidCapability);
  for(var type:TYPES){event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,type.get(),BasicMachineBlockEntity::itemCapability);event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,type.get(),BasicMachineBlockEntity::fluidCapability);}
 }
 public static void registerCompositions(){
  for(var holder:MACHINES){var spec=holder.get().basicSpec();var components=spec.constructionMaterials().stream().filter(w->w.material().isValid()&&w.amount()>0).map(w->MaterialComponent.of(w.material(),w.amount())).toList();
   if(!components.isEmpty())ItemMaterialRegistry.register(holder.get().asItem(),new ItemComposition(null,components,"GT6 basic machine construction",true));
  }
 }
}
