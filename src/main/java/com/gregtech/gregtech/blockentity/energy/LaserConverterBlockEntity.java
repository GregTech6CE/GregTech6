package com.gregtech.gregtech.blockentity.energy;

import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.block.energy.LaserConverterBlock;
import com.gregtech.gregtech.content.energy.LaserSpec;
import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import java.util.*;

/** Packet-preserving conversion, with the original 50% EU/LU and 8:1 RF/LU ratios. */
public final class LaserConverterBlockEntity extends GTEnergyBlockEntity implements com.gregtech.gregtech.api.machine.MachineControl.Provider {
    private long energy;
    private boolean stopped;
    private boolean emitted;
    private final com.gregtech.gregtech.api.machine.MachineControl control=com.gregtech.gregtech.api.machine.MachineControls.switchable(this,
            ()->!this.stopped,value->{this.stopped=!value;setChanged();},()->getBlockState().getValue(LaserConverterBlock.ACTIVE),()->emitted);
    @Override public com.gregtech.gregtech.api.machine.MachineControl machineControl(Direction side){return control;}
    private LazyOptional<IEnergyStorage> forgeEnergy = createForgeEnergy();
    private LazyOptional<IEnergyStorage> createForgeEnergy() { return LazyOptional.of(() -> new IEnergyStorage() {
        public int receiveEnergy(int amount,boolean simulate) {
            if(stopped || amount<=0)return 0;
            int accepted=(int)Math.min(amount, spec().input()*2-energy);
            if(!simulate && accepted>0){energy+=accepted;setChanged();}
            return accepted;
        }
        public int extractEnergy(int amount,boolean simulate){return 0;}
        public int getEnergyStored(){return (int)energy;}
        public int getMaxEnergyStored(){return (int)(spec().input()*2);}
        public boolean canExtract(){return false;}
        public boolean canReceive(){return !stopped;}
    }); }
    public LaserConverterBlockEntity(BlockPos pos,BlockState state){super(GTBlockEntities.LASER_CONVERTER.get(),pos,state);}
    public LaserSpec spec(){return ((LaserConverterBlock)getBlockState().getBlock()).spec();}
    public Direction front(){return getBlockState().getValue(LaserConverterBlock.FACING);}
    public void toggleStopped(){stopped=!stopped;setChanged();}
    public void tick(){
        if(level==null||level.isClientSide)return;
        long output=energy*spec().output()/spec().input();
        boolean active=!stopped && output>=Math.max(1,spec().output()/2);
        emitted=active&&EnergyTransfer.emitEnergyToSide(spec().outputType(),front(),output,1,this)>0;
        // These four GT6 converters waste their per-tick supply even if their output is disconnected.
        if(energy!=0){energy=0;setChanged();}
        if(getBlockState().getValue(LaserConverterBlock.ACTIVE)!=active)
            level.setBlockAndUpdate(worldPosition,getBlockState().setValue(LaserConverterBlock.ACTIVE,active));
    }
    @Override public boolean isEnergyType(GregTechTags.Tag type,Direction side,boolean emitting){return type==(emitting?spec().outputType():spec().inputType());}
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side){return List.of(spec().inputType(),spec().outputType());}
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type,Direction side,boolean theoretical){return type==spec().inputType()&&(theoretical||!stopped)&&(side==null||(spec().backInputOnly()?side==front().getOpposite():side!=front()));}
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type,Direction side,boolean theoretical){return type==spec().outputType()&&(side==null||side==front());}
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type,Direction side){return type==spec().inputType()?spec().input():0;}
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type,Direction side){return type==spec().outputType()?spec().output():0;}
    @Override public long getEnergyDemanded(GregTechTags.Tag type,Direction side,long size){return isEnergyAcceptingFrom(type,side,false)&&size>0?(spec().input()*2-energy)/size:0;}
    @Override public long getEnergyOffered(GregTechTags.Tag type,Direction side,long size){return 0;}
    @Override public long doInject(GregTechTags.Tag type,Direction side,long size,long amount,boolean execute){
        if(size<=0||amount<=0||size>spec().input()*2||!isEnergyAcceptingFrom(type,side,false))return 0;
        long accepted=Math.min(amount,(spec().input()*2-energy)/size);
        if(execute&&accepted>0){energy+=size*accepted;setChanged();}
        return accepted;
    }
    @Override public long getEnergyStored(GregTechTags.Tag type,Direction side){return type==spec().inputType()?energy:0;}
    @Override public long getEnergyCapacity(GregTechTags.Tag type,Direction side){return type==spec().inputType()?spec().input()*2:0;}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){
        return cap==ForgeCapabilities.ENERGY && spec().kind()==LaserSpec.Kind.FLUX && (side==null||side!=front())?forgeEnergy.cast():super.getCapability(cap,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();forgeEnergy.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();forgeEnergy=createForgeEnergy();}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putLong("gt.laser.energy",energy);tag.putBoolean("gt.stopped",stopped);}
    @Override public void load(CompoundTag tag){super.load(tag);energy=Math.max(0,Math.min(spec().input()*2,tag.getLong("gt.laser.energy")));stopped=tag.getBoolean("gt.stopped");}
}
