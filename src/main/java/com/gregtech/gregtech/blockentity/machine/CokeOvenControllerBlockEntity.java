/**
 * Copyright (c) 2025 GregTech-6 Team
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

package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.content.multiblock.*;
import com.gregtech.gregtech.item.behavior.ItemBehaviors;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Original GT6 17000: fire bricks, ignition, TU recipes and 16 parallel operations. */
public class CokeOvenControllerBlockEntity extends LargeRecipeMachineBlockEntity implements ItemBehaviors.Ignitable {
    private int ignitionTicks;
    private BlockPos fluidOutputTarget;
    public CokeOvenControllerBlockEntity(BlockPos pos,BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.COKE_OVEN.get(),pos,state);
        setSpec(com.gregtech.gregtech.block.machine.CokeOvenControllerBlock.makeSpec());
    }
    @Override public void setSpec(com.gregtech.gregtech.api.machine.BasicMachineSpec spec) {
        super.setSpec(spec);getTanksOutput()[0].setCapacity(Long.MAX_VALUE);
    }
    @Override protected java.util.List<LargeMachineLayouts.Cell> structureCells() {return LargeMachineLayouts.fromShared(OriginalCokeOvenRules.cells());}
    @Override protected void beforeMachineTick() {ignitionTicks=OriginalCokeOvenRules.tickIgnition(ignitionTicks);}
    @Override protected boolean recipeStartAllowed() {return ignitionTicks>0;}
    @Override protected void onProcessFinished() {ignitionTicks=OriginalCokeOvenRules.IGNITION_TICKS;}
    @Override public long onIgnite(Level level,BlockPos pos,Direction side,Player player,ItemStack igniter,
            boolean sneaking,float hitX,float hitY,float hitZ) {
        if(!level.isClientSide) {ignitionTicks=OriginalCokeOvenRules.IGNITION_TICKS;setChanged();}
        return 10000;
    }
    @Override protected IFluidHandler automaticFluidOutputTarget(Level level,BlockPos pos,Direction absolute) {
        if(fluidOutputTarget!=null && level.hasChunkAt(fluidOutputTarget)) {
            var cached=fluidHandler(fluidOutputTarget);if(cached!=null)return cached;
        }
        fluidOutputTarget=null;
        var output=getTanksOutput()[0].getFluid();if(output.isEmpty())return null;
        var front=getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        for(var offset:OriginalCokeOvenRules.fluidTargets()) {
            var target=pos.relative(front.getClockWise(),offset.x()).above(offset.y()).relative(front.getOpposite(),offset.z());
            if(!level.hasChunkAt(target))continue;
            var handler=fluidHandler(target);if(handler==null)continue;
            for(int tank=0;tank<handler.getTanks();tank++) if(handler.isFluidValid(tank,output)) {fluidOutputTarget=target;return handler;}
        }
        return null;
    }
    private IFluidHandler fluidHandler(BlockPos pos) {var entity=level.getBlockEntity(pos); return entity==null?null:entity.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,Direction.UP).resolve().orElse(null);}
    @Override public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);tag.putInt("gt.coke_ignition",ignitionTicks);
    }
    @Override public void load(CompoundTag tag) {
        // Upgrade the old two-slot implementation without creating outputs or converting its HU buffer.
        if(!tag.contains("gt.inventory") && tag.contains("gt.items")) {
            tag=tag.copy();var inventory=tag.getCompound("gt.items").copy();inventory.putInt("Size",10);
            tag.put("gt.inventory",inventory);
            if(tag.contains("gt.output"))tag.put("gt.tanks_output0",tag.getCompound("gt.output").copy());
            tag.remove("gt.heat");tag.remove("gt.progress");
        }
        super.load(tag);getTanksOutput()[0].setCapacity(Long.MAX_VALUE);
        ignitionTicks=Math.max(0,Math.min(OriginalCokeOvenRules.IGNITION_TICKS,tag.getInt("gt.coke_ignition")));fluidOutputTarget=null;
    }
}
