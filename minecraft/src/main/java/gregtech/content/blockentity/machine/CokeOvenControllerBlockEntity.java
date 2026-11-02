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

package gregtech.content.blockentity.machine;

import gregtech.content.multiblock.*;
//? if >=1.21.1 {
/*import gregtech.content.item.behavior.ManualToolBehaviorAccess;
*///?} else {
import gregtech.content.item.behavior.ItemBehaviors;
//?}
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

/** Original GT6 17000: fire bricks, ignition, TU recipes and 16 parallel operations. */
//? if >=1.21.1 {
/*public class CokeOvenControllerBlockEntity extends LargeRecipeMachineBlockEntity implements ManualToolBehaviorAccess.Ignitable {
*///?} else {
public class CokeOvenControllerBlockEntity extends LargeRecipeMachineBlockEntity implements ItemBehaviors.Ignitable {
//?}
    private int ignitionTicks;
    public boolean ignitionReady() { return ignitionTicks > 0; }
    private gregtech.engine.multiblock.StructureOutput fluidOutputTarget;
    public CokeOvenControllerBlockEntity(BlockPos pos,BlockState state) {
        super(gregtech.content.registry.GTBlockEntities.COKE_OVEN.get(),pos,state);
        setSpec(gregtech.content.block.machine.CokeOvenControllerBlock.makeSpec());
    }
    @Override public void setSpec(gregtech.engine.machine.BasicMachineSpec spec) {
        super.setSpec(spec);getTanksOutput()[0].setCapacity(Long.MAX_VALUE);
    }
    @Override protected void beforeMachineTick() {ignitionTicks=OriginalCokeOvenRules.tickIgnition(ignitionTicks);}
    @Override protected boolean recipeStartAllowed() {return ignitionTicks>0;}
    @Override protected void onProcessFinished() {ignitionTicks=OriginalCokeOvenRules.IGNITION_TICKS;}
    @Override public long onIgnite(Level level,BlockPos pos,Direction side,Player player,ItemStack igniter,
            boolean sneaking,float hitX,float hitY,float hitZ) {
        if(!level.isClientSide) {ignitionTicks=OriginalCokeOvenRules.IGNITION_TICKS;setChanged();}
        return 10000;
    }
    @Override protected void autoOutputFluids(Level level,BlockPos pos,int side) {
        var tank=getTanksOutput()[0];if(tank.isEmpty())return;
        if(fluidOutputTarget!=null && gregtech.engine.multiblock.StructureOutputs.fluid(
                this,fluidOutputTarget,getTanksInput().length,tank,Integer.MAX_VALUE)>0)return;
        fluidOutputTarget=null;
        for(var target:OriginalMultiblockOutputRules.COKE_FLUIDS) {
            if(gregtech.engine.multiblock.StructureOutputs.fluid(this,target,getTanksInput().length,tank,Integer.MAX_VALUE)>0) {
                fluidOutputTarget=target;return;
            }
        }
    }
    @Override protected void autoOutputItems(Level level,BlockPos pos,int side) {} // GT6 has no automatic item outlet.
//? if >=1.21.1 {
    /*@Override public void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);tag.putInt("gt.coke_ignition",ignitionTicks);
*///?} else {
    @Override public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);tag.putInt("gt.coke_ignition",ignitionTicks);
//?}
    }
//? if >=1.21.1 {
    /*@Override public void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
*///?} else {
    @Override public void load(CompoundTag tag) {
//?}
        // Upgrade the old two-slot implementation without creating outputs or converting its HU buffer.
        if(!tag.contains("gt.inventory") && tag.contains("gt.items")) {
            tag=tag.copy();var inventory=tag.getCompound("gt.items").copy();inventory.putInt("Size",10);
            tag.put("gt.inventory",inventory);
            if(tag.contains("gt.output"))tag.put("gt.tanks_output0",tag.getCompound("gt.output").copy());
            tag.remove("gt.heat");tag.remove("gt.progress");
        }
//? if >=1.21.1 {
        /*super.loadAdditional(tag,lookup);getTanksOutput()[0].setCapacity(Long.MAX_VALUE);
*///?} else {
        super.load(tag);getTanksOutput()[0].setCapacity(Long.MAX_VALUE);
//?}
        ignitionTicks=Math.max(0,Math.min(OriginalCokeOvenRules.IGNITION_TICKS,tag.getInt("gt.coke_ignition")));fluidOutputTarget=null;
    }
}
