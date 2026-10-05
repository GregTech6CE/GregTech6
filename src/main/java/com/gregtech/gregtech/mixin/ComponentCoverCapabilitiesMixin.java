/**
 * Copyright (c) 2021 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package com.gregtech.gregtech.mixin;
import com.gregtech.gregtech.content.cover.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value={
    com.gregtech.gregtech.blockentity.CapabilityRelayBlockEntity.class,
    com.gregtech.gregtech.blockentity.EnergyRelayBlockEntity.class,
    com.gregtech.gregtech.blockentity.SapBagBlockEntity.class,
    com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity.class,
    com.gregtech.gregtech.blockentity.energy.LaserConverterBlockEntity.class,
    com.gregtech.gregtech.blockentity.energy.PumpBlockEntity.class,
    com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity.class,
    com.gregtech.gregtech.blockentity.energy.ZpmDischargerBlockEntity.class,
    com.gregtech.gregtech.blockentity.inventory.BookShelfBlockEntity.class,
    com.gregtech.gregtech.blockentity.inventory.BottleCrateBlockEntity.class,
    com.gregtech.gregtech.blockentity.inventory.DrawerQuadBlockEntity.class,
    com.gregtech.gregtech.blockentity.inventory.EnderGarbageBlockEntity.class,
    com.gregtech.gregtech.blockentity.inventory.EnderGarbageDumpBlockEntity.class,
    com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity.class,
    com.gregtech.gregtech.blockentity.inventory.MetalChestBlockEntity.class,
    com.gregtech.gregtech.blockentity.inventory.UsbSwitchBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.BedrockDrillControllerBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.BoilerTankBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.BurningBoxBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.CokeOvenControllerBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.CryoDistillationControllerBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.HopperBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.ImplosionCompressorControllerBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.ItemPipeBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.KineticDieselEngineBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.KineticSteamEngineBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.LargeBoilerControllerBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.LargeCrucibleControllerBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.LargeGasTurbineControllerBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.LargeHeatExchangerControllerBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.LargeTurbineControllerBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.MultiblockTankControllerBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.OriginalLargeBoilerControllerBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.PistonEngineBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.QueueHopperBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.SmeltingCrucibleBlockEntity.class,
    com.gregtech.gregtech.blockentity.machine.TankBlockEntity.class,
    com.gregtech.gregtech.blockentity.misc.BumbleHiveBlockEntity.class,
    com.gregtech.gregtech.blockentity.sensor.SensorBlockEntity.class,
    com.gregtech.gregtech.blockentity.tool.BumbliaryBlockEntity.class,
    com.gregtech.gregtech.blockentity.tool.CoinMoldBlockEntity.class,
    com.gregtech.gregtech.blockentity.tool.DustFunnelBlockEntity.class,
    com.gregtech.gregtech.blockentity.tool.MaterialAnvilBlockEntity.class,
    com.gregtech.gregtech.blockentity.tool.PortableContainerBlockEntity.class,
    com.gregtech.gregtech.blockentity.tool.ProcessingToolBlockEntity.class
},remap=false)
public abstract class ComponentCoverCapabilitiesMixin {
    @Inject(method="getCapability",at=@At("RETURN"),cancellable=true,remap=false,require=0)
    private void gregtech$gate(Capability<?> capability,Direction side,CallbackInfoReturnable<LazyOptional<?>> cir){
        var owner=(BlockEntity)(Object)this;var result=cir.getReturnValue();
        if(side==null||result==null||!ComponentCoverFallback.uses(owner))return;
        var host=(PanelCoverHost)owner;
        if(capability==ForgeCapabilities.ITEM_HANDLER)cir.setReturnValue(result.lazyMap(raw->ComponentCoverAccess.items(host,side,(net.minecraftforge.items.IItemHandler)raw)));
        else if(capability==ForgeCapabilities.FLUID_HANDLER)cir.setReturnValue(result.lazyMap(raw->ComponentCoverAccess.fluids(host,side,(net.minecraftforge.fluids.capability.IFluidHandler)raw)));
    }
}
