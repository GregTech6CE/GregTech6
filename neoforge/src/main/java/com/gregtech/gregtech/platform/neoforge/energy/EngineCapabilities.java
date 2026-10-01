package com.gregtech.gregtech.platform.neoforge.energy;
import com.gregtech.gregtech.registry.*;import com.gregtech.gregtech.blockentity.machine.*;import net.neoforged.bus.api.SubscribeEvent;import net.neoforged.fml.common.EventBusSubscriber;import net.neoforged.neoforge.capabilities.*;
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
public final class EngineCapabilities {private EngineCapabilities(){}@SubscribeEvent @SuppressWarnings({"unchecked","rawtypes"})public static void register(RegisterCapabilitiesEvent event){
 event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,GTBlockEntities.PUMP.get(),(be,side)->be.fluidHandler(side));
 event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,GTBlockEntities.LOGISTICS_TANK.get(),(be,side)->be.capabilityHandler(side));
 event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,GTBlockEntities.STEAM_BOILER.get(),(be,side)->be.fluidHandler(side));
 for(var h:GTEngines.all()){var b=h.get();switch(b.engineType()){
 case FLUX->event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,(net.minecraft.world.level.block.entity.BlockEntityType<KineticFluxEngineBlockEntity>)b.getBeType(),(be,side)->be.energyHandler(side));
 case STEAM->event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,(net.minecraft.world.level.block.entity.BlockEntityType<KineticSteamEngineBlockEntity>)b.getBeType(),(be,side)->be.fluidHandler(side));
 case DIESEL->event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,(net.minecraft.world.level.block.entity.BlockEntityType<KineticDieselEngineBlockEntity>)b.getBeType(),(be,side)->be.fluidHandler(side));default->{}
 }}
}}
