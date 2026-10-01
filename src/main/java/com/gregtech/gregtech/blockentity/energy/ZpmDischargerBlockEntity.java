package com.gregtech.gregtech.blockentity.energy;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.block.energy.ZpmDischargerBlock;
import com.gregtech.gregtech.content.energy.ZpmEnergy;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.api.energy.EnergyTransfer;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import java.util.*;
/** GT6 11170/11171: one artifact, QU buffer, one front packet per tick. */
public final class ZpmDischargerBlockEntity extends GTEnergyBlockEntity implements com.gregtech.gregtech.api.machine.MachineControl.Provider {
    private long buffer;
    private boolean stopped;
    private boolean emitted;
    private final com.gregtech.gregtech.api.machine.MachineControl control=com.gregtech.gregtech.api.machine.MachineControls.switchable(this,
            ()->!this.stopped,value->{this.stopped=!value;setChanged();},()->getBlockState().getValue(ZpmDischargerBlock.ACTIVE),()->emitted);
    @Override public com.gregtech.gregtech.api.machine.MachineControl machineControl(Direction side){return control;}
    private int tick;
    private final ItemStackHandler inventory=new ItemStackHandler(1){
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return isRemoved()?stack:super.insertItem(slot,stack,simulate);}
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate){return isRemoved()?ItemStack.EMPTY:super.extractItem(slot,amount,simulate);}
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return ZpmEnergy.isModule(stack);}
        @Override protected void onContentsChanged(int slot){setChanged();}
    };
    private LazyOptional<IItemHandler> items=LazyOptional.of(()->inventory);
    public ZpmDischargerBlockEntity(BlockPos p,BlockState s){super(com.gregtech.gregtech.registry.GTBlockEntities.ZPM_DISCHARGER.get(),p,s);}
    public IItemHandler inventory(){return inventory;}
    public boolean stopped(){return stopped;}
    public void toggleStopped(){stopped=!stopped;setChanged();}
    public GregTechTags.Tag outputType(){return ((ZpmDischargerBlock)getBlockState().getBlock()).electric()?GregTechTags.Energy.EU:GregTechTags.Energy.QU;}
    private Direction front(){return getBlockState().getValue(ZpmDischargerBlock.FACING);}
    public long totalEnergy(){return buffer+ZpmEnergy.stored(inventory.getStackInSlot(0));}
    public void tick(){
        if(level==null||level.isClientSide)return;
        emitted=false;
        var module=inventory.getStackInSlot(0);
        boolean loaded=ZpmEnergy.isModule(module)&&module.getCount()==1;
        if(tick++%20==0&&loaded&&buffer<ZpmEnergy.PACKET*80){
            long packets=buffer<ZpmEnergy.PACKET*40?40:20;
            long taken=Math.min(packets,ZpmEnergy.stored(module)/ZpmEnergy.PACKET)*ZpmEnergy.PACKET;
            if(taken>0){ZpmEnergy.set(module,ZpmEnergy.stored(module)-taken);buffer+=taken;setChanged();}
        }
        if(!stopped&&loaded&&buffer>=ZpmEnergy.PACKET){
            long used=EnergyTransfer.emitEnergyToSide(outputType(),front(),ZpmEnergy.PACKET,1,this);
            if(used>0){emitted=true;buffer-=ZpmEnergy.PACKET;setChanged();}
        }
        boolean active=!stopped&&loaded&&buffer>=ZpmEnergy.PACKET;
        var state=getBlockState();
        if(state.getValue(ZpmDischargerBlock.ACTIVE)!=active||state.getValue(ZpmDischargerBlock.LOADED)!=loaded)
            level.setBlockAndUpdate(worldPosition,state.setValue(ZpmDischargerBlock.ACTIVE,active).setValue(ZpmDischargerBlock.LOADED,loaded));
    }
    @Override public boolean isEnergyType(GregTechTags.Tag t,Direction side,boolean emitting){return emitting&&t==outputType();}
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side){return List.of(outputType());}
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag t,Direction s,boolean theoretical){return false;}
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag t,Direction s,boolean theoretical){return t==outputType()&&(s==null||s==front())&&(theoretical||!stopped);}
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag t,Direction s){return 0;}
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag t,Direction s){return ZpmEnergy.PACKET;}
    @Override public long getEnergySizeOutputMin(GregTechTags.Tag t,Direction s){return ZpmEnergy.PACKET;}
    @Override public long getEnergySizeOutputMax(GregTechTags.Tag t,Direction s){return ZpmEnergy.PACKET;}
    @Override public long getEnergyDemanded(GregTechTags.Tag t,Direction s,long size){return 0;}
    @Override public long getEnergyOffered(GregTechTags.Tag t,Direction s,long size){return 0;}
    @Override public Collection<GregTechTags.Tag> getEnergyCapacitorTypes(Direction side){return List.of(GregTechTags.Energy.QU);}
    @Override public long getEnergyStored(GregTechTags.Tag t,Direction s){return t==GregTechTags.Energy.QU?totalEnergy():0;}
    @Override public long getEnergyCapacity(GregTechTags.Tag t,Direction s){return t==GregTechTags.Energy.QU?ZpmEnergy.CAPACITY+ZpmEnergy.PACKET*320:0;}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> c,Direction s){return c==ForgeCapabilities.ITEM_HANDLER&&!isRemoved()?items.cast():super.getCapability(c,s);}
    @Override public void invalidateCaps(){super.invalidateCaps();items.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();items=LazyOptional.of(()->inventory);}
    @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.put("gt.zpm.inventory",inventory.serializeNBT());t.putLong("gt.zpm.buffer",buffer);t.putBoolean("gt.stopped",stopped);}
    @Override public void load(CompoundTag t){super.load(t);if(t.contains("gt.zpm.inventory"))inventory.deserializeNBT(t.getCompound("gt.zpm.inventory"));buffer=Math.max(0,Math.min(ZpmEnergy.PACKET*320,t.getLong("gt.zpm.buffer")));stopped=t.getBoolean("gt.stopped");}
}
