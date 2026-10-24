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

package gregtech.mixin;
import gregtech.content.cover.*;
//? if >=1.21.1 {
/*import net.minecraft.core.*;
import net.minecraft.world.level.Level;
*///?} else {
import net.minecraft.core.Direction;
//?}
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=1.21.1 {
/*import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.*;
*///?} else {
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if >=1.21.1 {

/*/^* Decorates returned native capabilities without replacing providers or caching cover state. ^/
@Mixin(value=BlockCapability.class,remap=false)
*///?} else {
@Mixin(value={
    gregtech.content.blockentity.CapabilityRelayBlockEntity.class,
    gregtech.content.blockentity.EnergyRelayBlockEntity.class,
    gregtech.content.blockentity.SapBagBlockEntity.class,
    gregtech.content.blockentity.energy.EnergyNodeBlockEntity.class,
    gregtech.content.blockentity.energy.LaserConverterBlockEntity.class,
    gregtech.content.blockentity.energy.PumpBlockEntity.class,
    gregtech.content.blockentity.energy.ReactorCoreBlockEntity.class,
    gregtech.content.blockentity.energy.ZpmDischargerBlockEntity.class,
    gregtech.content.blockentity.inventory.BookShelfBlockEntity.class,
    gregtech.content.blockentity.inventory.BottleCrateBlockEntity.class,
    gregtech.content.blockentity.inventory.DrawerQuadBlockEntity.class,
    gregtech.content.blockentity.inventory.EnderGarbageBlockEntity.class,
    gregtech.content.blockentity.inventory.EnderGarbageDumpBlockEntity.class,
    gregtech.content.blockentity.inventory.MassStorageBlockEntity.class,
    gregtech.content.blockentity.inventory.MetalChestBlockEntity.class,
    gregtech.content.blockentity.inventory.UsbSwitchBlockEntity.class,
    gregtech.content.blockentity.machine.BasicMachineBlockEntity.class,
    gregtech.content.blockentity.machine.BedrockDrillControllerBlockEntity.class,
    gregtech.content.blockentity.machine.BoilerTankBlockEntity.class,
    gregtech.content.blockentity.machine.BurningBoxBlockEntity.class,
    gregtech.content.blockentity.machine.CokeOvenControllerBlockEntity.class,
    gregtech.content.blockentity.machine.FluidPipeBlockEntity.class,
    gregtech.content.blockentity.machine.HopperBlockEntity.class,
    gregtech.content.blockentity.machine.ImplosionCompressorControllerBlockEntity.class,
    gregtech.content.blockentity.machine.ItemPipeBlockEntity.class,
    gregtech.content.blockentity.machine.KineticDieselEngineBlockEntity.class,
    gregtech.content.blockentity.machine.KineticSteamEngineBlockEntity.class,
    gregtech.content.blockentity.machine.LargeCrucibleControllerBlockEntity.class,
    gregtech.content.blockentity.machine.LargeGasTurbineControllerBlockEntity.class,
    gregtech.content.blockentity.machine.LargeHeatExchangerControllerBlockEntity.class,
    gregtech.content.blockentity.machine.LargeTurbineControllerBlockEntity.class,
    gregtech.content.blockentity.machine.MultiblockPortBlockEntity.class,
    gregtech.content.blockentity.machine.MultiblockTankControllerBlockEntity.class,
    gregtech.content.blockentity.machine.OriginalLargeBoilerControllerBlockEntity.class,
    gregtech.content.blockentity.machine.PistonEngineBlockEntity.class,
    gregtech.content.blockentity.machine.QueueHopperBlockEntity.class,
    gregtech.content.blockentity.machine.SmeltingCrucibleBlockEntity.class,
    gregtech.content.blockentity.machine.TankBlockEntity.class,
    gregtech.content.blockentity.misc.BumbleHiveBlockEntity.class,
    gregtech.content.blockentity.sensor.SensorBlockEntity.class,
    gregtech.content.blockentity.tool.BumbliaryBlockEntity.class,
    gregtech.content.blockentity.tool.CoinMoldBlockEntity.class,
    gregtech.content.blockentity.tool.DustFunnelBlockEntity.class,
    gregtech.content.blockentity.tool.MaterialAnvilBlockEntity.class,
    gregtech.content.blockentity.tool.PortableContainerBlockEntity.class,
    gregtech.content.blockentity.tool.ProcessingToolBlockEntity.class
},remap=false)
//?}
public abstract class ComponentCoverCapabilitiesMixin {
//? if >=1.21.1 {
//?} else {
    @org.spongepowered.asm.mixin.Unique
    private gregtech.content.cover.ForgeCoverCapabilityCache gregtech$coverCapabilityCache;
//?}
//? if >=1.21.1 {
    /*@Inject(method="getCapability",at=@At("RETURN"),cancellable=true,remap=false,require=1)
    private void gregtech$gate(Level level,BlockPos pos,BlockState state,BlockEntity entity,Object context,CallbackInfoReturnable<Object> cir){
        if(!(context instanceof Direction side)||cir.getReturnValue()==null)return;
        var owner=entity==null?level.getBlockEntity(pos):entity;
        if(!ComponentCoverFallback.uses(owner))return;
        var host=(PanelCoverHost)owner;var result=cir.getReturnValue();
        if((Object)this==Capabilities.ItemHandler.BLOCK)cir.setReturnValue(ComponentCoverAccess.items(host,side,(net.neoforged.neoforge.items.IItemHandler)result));
        else if((Object)this==Capabilities.FluidHandler.BLOCK)cir.setReturnValue(ComponentCoverAccess.fluids(host,side,(net.neoforged.neoforge.fluids.capability.IFluidHandler)result));
*///?} else {
    @Inject(method="getCapability",at=@At("RETURN"),cancellable=true,remap=false,require=0)
    private void gregtech$gate(Capability<?> capability,Direction side,CallbackInfoReturnable<LazyOptional<?>> cir){
        var owner=(BlockEntity)(Object)this;var result=cir.getReturnValue();
        if(side==null||result==null||!ComponentCoverFallback.uses(owner))return;
        var host=(PanelCoverHost)owner;
        if (capability != ForgeCapabilities.ITEM_HANDLER && capability != ForgeCapabilities.FLUID_HANDLER) return;
        if (gregtech$coverCapabilityCache == null) gregtech$coverCapabilityCache = new ForgeCoverCapabilityCache();
        if (capability == ForgeCapabilities.ITEM_HANDLER)
            cir.setReturnValue(gregtech$coverCapabilityCache.wrap(ForgeCapabilities.ITEM_HANDLER, side,
                    result.cast(), raw -> ComponentCoverAccess.items(host, side, raw)));
        else cir.setReturnValue(gregtech$coverCapabilityCache.wrap(ForgeCapabilities.FLUID_HANDLER, side,
                result.cast(), raw -> ComponentCoverAccess.fluids(host, side, raw)));
//?}
    }
}
