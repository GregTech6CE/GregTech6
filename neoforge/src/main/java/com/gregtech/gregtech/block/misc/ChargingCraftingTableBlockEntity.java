package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.api.energy.*;
import com.gregtech.gregtech.api.energy.item.IItemEnergy;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;


import net.neoforged.neoforge.energy.IEnergyStorage;
import javax.annotation.Nullable;
import java.util.Collection;

/** GT6 MultiTileEntityChargingCraftingTable: one packet per tool slot; no block energy buffer. */
public final class ChargingCraftingTableBlockEntity extends AdvancedCraftingTableBlockEntity implements IEnergyBlock {
    private boolean energyCapsValid=true;

    public ChargingCraftingTableBlockEntity(BlockPos pos,BlockState state){super(com.gregtech.gregtech.registry.GTBlockEntities.CHARGING_CRAFTING_TABLE.get(),pos,state);}
    @Override public boolean charging(){return true;}
    @Override public boolean isEnergyType(GregTechTags.Tag type,@Nullable Direction side,boolean emitting){return type!=null&&!emitting;}
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side){return GregTechTags.Energy.ALL;}
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type,@Nullable Direction side,boolean theoretical){return !isRemoved()&&energyCapsValid&&isEnergyType(type,side,false);}
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type,@Nullable Direction side,boolean theoretical){return false;}
    @Override public long doEnergyInjection(GregTechTags.Tag type,@Nullable Direction side,long size,long amount,boolean execute){return EnergyBlockDefaults.doEnergyInjection(this,type,side,size,amount,execute);}
    @Override public long doInject(GregTechTags.Tag type,@Nullable Direction side,long size,long amount,boolean execute){
        if(level==null||level.isClientSide||isRemoved()||!energyCapsValid||type==null||size==0||size==Long.MIN_VALUE||amount<=0)return 0;
        long accepted=0;
        for(int slot=16;slot<=20&&accepted<amount;slot++){
            ItemStack original=items().getStackInSlot(slot);
            if(original.isEmpty()||!(original.getItem() instanceof IItemEnergy energy))continue;
            // Simulate on a copy as well: probing demand must not add NBT to an untouched battery.
            ItemStack copy=original.copy();
            long used=energy.doEnergyInjection(type,copy,size,1,level,worldPosition,execute);
            if(used<=0)continue;
            accepted++;
            if(execute)items().setStackInSlot(slot,copy);
        }
        return accepted;
    }
    @Override public long getEnergyDemanded(GregTechTags.Tag type,@Nullable Direction side,long size){return doEnergyInjection(type,side,size,Long.MAX_VALUE,false);}
    @Override public long doEnergyExtraction(GregTechTags.Tag type,@Nullable Direction side,long size,long amount,boolean execute){return 0;}
    @Override public long getEnergyOffered(GregTechTags.Tag type,@Nullable Direction side,long size){return 0;}
    @Override public long getEnergySizeInputMin(GregTechTags.Tag type,@Nullable Direction side){return 1;}
    @Override public long getEnergySizeInputMax(GregTechTags.Tag type,@Nullable Direction side){return Long.MAX_VALUE;}
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type,@Nullable Direction side){return Long.MAX_VALUE;}
    @Override public long getEnergySizeOutputMin(GregTechTags.Tag type,@Nullable Direction side){return 0;}
    @Override public long getEnergySizeOutputMax(GregTechTags.Tag type,@Nullable Direction side){return 0;}
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type,@Nullable Direction side){return 0;}
    private final IEnergyStorage fluxHandler=new IEnergyStorage(){
        public int receiveEnergy(int maximum,boolean simulate){return (int)doEnergyInjection(GregTechTags.Energy.RF,null,1,maximum,!simulate);}
        public int extractEnergy(int maximum,boolean simulate){return 0;}
        public int getEnergyStored(){return 0;}
        public int getMaxEnergyStored(){return 0;}
        public boolean canExtract(){return false;}
        public boolean canReceive(){return energyCapsValid&&!isRemoved();}
    };
    public IEnergyStorage flux(){return isRemoved()?null:fluxHandler;}
}
