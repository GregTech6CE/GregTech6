package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.api.machine.*;
public final class GTEngines {private GTEngines(){}public static void registerAll(){for(var e:com.gregtech.gregtech.content.energy.EngineCatalog.all()){
if(e.spec() instanceof ElectricEngineSpec spec)MachineRegistry.registerElectricEngine(spec);
if(e.spec() instanceof FluxEngineSpec spec)MachineRegistry.registerFluxEngine(spec);
if(e.spec() instanceof SteamEngineSpec spec)MachineRegistry.registerSteamEngine(spec);
if(e.spec() instanceof StrongSteamEngineSpec spec)MachineRegistry.registerStrongSteamEngine(spec);
if(e.spec() instanceof RotationEngineSpec spec)MachineRegistry.registerRotationEngine(spec);
if(e.spec() instanceof DieselEngineSpec spec)MachineRegistry.registerDieselEngine(spec);
}}}
