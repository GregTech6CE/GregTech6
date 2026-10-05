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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Adds lazy cover storage only when an eligible GT entity lacks native cover slots. */
@Mixin(BlockEntity.class)
public abstract class ComponentCoverHostMixin implements FallbackCoverHost {
    @Unique private ComponentCoverStorage gregtech$components;
    @Override public ComponentCoverStorage gregtechComponentStorage(boolean create) {
        if(gregtech$components==null&&create)gregtech$components=new ComponentCoverStorage(this);
        return gregtech$components;
    }
    @Override public net.minecraftforge.items.IItemHandler componentItems(Direction side){
        var owner=(BlockEntity)(Object)this;
        if(!ComponentCoverFallback.uses(owner)){
            if(owner instanceof net.minecraftforge.items.IItemHandler handler)return handler;
            if(owner.getLevel()==null)return null;
            return owner.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,null).orElse(null);
        }
        return owner.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,side).orElse(null);
    }
    @Override public net.minecraftforge.fluids.capability.IFluidHandler componentFluids(Direction side){
        var owner=(BlockEntity)(Object)this;
        if(!ComponentCoverFallback.uses(owner)){
            if(owner instanceof net.minecraftforge.fluids.capability.IFluidHandler handler)return handler;
            if(owner.getLevel()==null)return null;
            return owner.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,null).orElse(null);
        }
        return owner.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,side).orElse(null);
    }
    @Override public ItemStack getCover(Direction side){return gregtech$components==null?ItemStack.EMPTY:gregtech$components.get(side);}
    @Override public PanelCoverRuntime panels(){return gregtechComponentStorage(true).panels();}
    @Override public boolean componentTicks(){return ComponentCoverFallback.eligible((BlockEntity)(Object)this);}
    @Override public boolean attachCover(Direction side,ItemStack stack){
        if(!componentTicks())return false;
        boolean attached=gregtechComponentStorage(true).attach(side,stack);
        if(attached)ComponentCoverFallback.track((BlockEntity)(Object)this);
        return attached;
    }
    @Override public ItemStack removeCover(Direction side){return gregtech$components==null?ItemStack.EMPTY:gregtech$components.remove(side);}
    @Inject(method="saveWithoutMetadata",at=@At("RETURN"),require=1)
    private void gregtech$save(CallbackInfoReturnable<CompoundTag> cir){ComponentCoverFallback.write((BlockEntity)(Object)this,cir.getReturnValue());}
    @Inject(method="loadStatic",at=@At("RETURN"),require=1)
    private static void gregtech$load(BlockPos pos,BlockState state,CompoundTag tag,CallbackInfoReturnable<BlockEntity> cir){ComponentCoverFallback.read(cir.getReturnValue(),tag);}
    @Inject(method="load",at=@At("RETURN"),require=1)
    private void gregtech$reload(CompoundTag tag,CallbackInfo ci){ComponentCoverFallback.read((BlockEntity)(Object)this,tag);}
    @Inject(method="clearRemoved",at=@At("RETURN"),require=1)
    private void gregtech$revive(CallbackInfo ci){
        if(gregtech$components!=null&&gregtech$components.hasCovers())ComponentCoverFallback.track((BlockEntity)(Object)this);
    }
    @Inject(method="setRemoved",at=@At("HEAD"),require=1)
    private void gregtech$remove(CallbackInfo ci){ComponentCoverFallback.removing((BlockEntity)(Object)this);}
}
