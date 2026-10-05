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
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Decorates returned native capabilities without replacing providers or caching cover state. */
@Mixin(value=BlockCapability.class,remap=false)
public abstract class ComponentCoverCapabilitiesMixin {
    @Inject(method="getCapability",at=@At("RETURN"),cancellable=true,remap=false,require=1)
    private void gregtech$gate(Level level,BlockPos pos,BlockState state,BlockEntity entity,Object context,CallbackInfoReturnable<Object> cir){
        if(!(context instanceof Direction side)||cir.getReturnValue()==null)return;
        var owner=entity==null?level.getBlockEntity(pos):entity;
        if(!ComponentCoverFallback.uses(owner))return;
        var host=(PanelCoverHost)owner;var result=cir.getReturnValue();
        if((Object)this==Capabilities.ItemHandler.BLOCK)cir.setReturnValue(ComponentCoverAccess.items(host,side,(net.neoforged.neoforge.items.IItemHandler)result));
        else if((Object)this==Capabilities.FluidHandler.BLOCK)cir.setReturnValue(ComponentCoverAccess.fluids(host,side,(net.neoforged.neoforge.fluids.capability.IFluidHandler)result));
    }
}
