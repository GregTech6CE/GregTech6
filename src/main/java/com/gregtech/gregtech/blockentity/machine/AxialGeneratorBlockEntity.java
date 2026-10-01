package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.api.multiblock.*;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.content.multiblock.AxialGeneratorDefinitions;
import com.gregtech.gregtech.content.multiblock.AxialStructureTransform;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.IFluidHandler;
import java.util.*;

/** Shared solid 3x3x4 assembly and rear shaft output. Storage belongs only to the controller. */
public abstract class AxialGeneratorBlockEntity extends GTEnergyBlockEntity implements MultiblockPortOwner,com.gregtech.gregtech.api.machine.MachineControl.Provider {
    protected long energy;
    protected boolean stopped, overloaded;
    private boolean converting,emitted;
    private boolean rotorFormed,rotorActive;
    private final com.gregtech.gregtech.api.machine.MachineControl control=com.gregtech.gregtech.api.machine.MachineControls.switchable(this,
            ()->!this.stopped,value->{this.stopped=!value||this.overloaded;setChanged();},()->this.converting&&!this.overloaded&&isStructureOk(),()->this.emitted&&!this.overloaded&&isStructureOk());
    @Override public com.gregtech.gregtech.api.machine.MachineControl machineControl(Direction side){return control;}
    private final PartBindings<BlockPos,MultiblockLayout.Role> bindings=new PartBindings<>();
    protected AxialGeneratorBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state) { super(type,pos,state); }
    public AxialGeneratorDefinitions.Grade grade() { return AxialGeneratorDefinitions.grade(getBlockState().getBlock()); }
    public Direction front() { return getBlockState().getValue(DirectionalBlock.FACING); }
    public boolean isStopped() { return stopped; }
    public boolean isOverloaded() { return overloaded; }
    /** Synced visual state for the original GT6 large steam-turbine rotor. */
    public boolean isRotorFormed() { return rotorFormed; }
    public boolean isRotorActive() { return rotorActive; }
    protected boolean isConvertingThisTick() { return converting; }
    protected void updateRotorVisual(boolean formed, boolean active) {
        active &= formed;
        if (rotorFormed == formed && rotorActive == active) return;
        rotorFormed = formed;
        rotorActive = active;
        syncToClient();
    }
    public void toggleStopped() { if(overloaded){overloaded=false;stopped=true;energy=0;}else stopped=!stopped;setChanged(); }
    protected void overload() { overloaded=true;stopped=true;energy=0;setChanged(); }
    public static MultiblockLayout.Role role(BlockPos cell,boolean steam) {
        return role(cell, Direction.NORTH, steam);
    }
    public static MultiblockLayout.Role role(BlockPos cell, Direction front, boolean steam) {
        return AxialStructureTransform.role(front, cell.getX(), cell.getY(), cell.getZ(), steam);
    }
    @Override public boolean isStructureOk() {
        if(level==null||isRemoved())return false;
        var parts=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        for(var cell:grade().cells().keySet()) {
            var pos=AxialStructureTransform.at(worldPosition,front(),cell.getX(),cell.getY(),cell.getZ());
            if(!level.hasChunkAt(pos)||!grade().accepts(cell,level.getBlockState(pos).getBlock())||!(level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity)) {
                bindings.clear(this::release);return false;
            }
            parts.put(pos,role(cell,front(),grade().steam()));
        }
        return bindings.update(parts,p->((MultiblockPortBlockEntity)level.getBlockEntity(p)).canBind(worldPosition),
                (p,r)->((MultiblockPortBlockEntity)level.getBlockEntity(p)).bind(worldPosition,r),this::release);
    }
    private void release(BlockPos pos) { if(level!=null&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part)part.release(worldPosition); }
    @Override public void setRemoved() { bindings.clear(this::release);super.setRemoved(); }
    protected GregTechTags.Tag outputType() { return grade().steam()?GregTechTags.Energy.RU:GregTechTags.Energy.EU; }
    /** GT6 waste-energy converter: one variable-size packet per tick; unused energy dissipates. */
    protected void convertAndEmit() {
        converting=false;emitted=false;
        if(energy<=0)return;
        long output=energy*grade().output()/grade().input();
        if(output>grade().outputMaximum()) { overload();return; }
        if(output>=grade().output()/2) {
            converting=true;
            var target=worldPosition.relative(front().getOpposite(),4);
            if(level.hasChunkAt(target)&&level.getBlockEntity(target) instanceof IEnergyBlock receiver)
                emitted=receiver.doEnergyInjection(outputType(),front(),output,1,true)>0;
        }
        energy=Math.max(0,energy-grade().inputMaximum());setChanged();
    }
    @Override public IFluidHandler portFluids(MultiblockLayout.Role role) { return null; }
    @Override public Collection<GregTechTags.Tag> portEnergyTypes(MultiblockLayout.Role role) { return role==MultiblockLayout.Role.ENERGY_OUTPUT?List.of(outputType()):List.of(); }
    @Override public boolean emitsPortEnergy(MultiblockLayout.Role role,GregTechTags.Tag type) { return role==MultiblockLayout.Role.ENERGY_OUTPUT&&type==outputType(); }
    @Override public long portEnergyOutputSize(MultiblockLayout.Role role,GregTechTags.Tag type) { return emitsPortEnergy(role,type)?grade().output():0; }
    // Output is exclusively pushed at the rear port: no second extractable buffer at the main housing.
    @Override public boolean isEnergyType(GregTechTags.Tag type,Direction side,boolean emitting) { return false; }
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side) { return List.of(); }
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type,Direction side,boolean theoretical) { return false; }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type,Direction side,boolean theoretical) { return false; }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type,Direction side) { return 0; }
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type,Direction side) { return type==outputType()?grade().output():0; }
    @Override public long getEnergyOffered(GregTechTags.Tag type,Direction side,long size) { return 0; }
    @Override public long getEnergyDemanded(GregTechTags.Tag type,Direction side,long size) { return 0; }
    @Override public long doInject(GregTechTags.Tag type,Direction side,long size,long amount,boolean execute) { return 0; }
    @Override public long getEnergyStored(GregTechTags.Tag type,Direction side) { return 0; }
    @Override public long getEnergyCapacity(GregTechTags.Tag type,Direction side) { return 0; }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag);tag.putLong("gt.axial.energy",energy);tag.putBoolean("gt.stopped",stopped);tag.putBoolean("gt.overloaded",overloaded);tag.putBoolean("gt.rotor_formed",rotorFormed);tag.putBoolean("gt.rotor_active",rotorActive); }
    @Override public void load(CompoundTag tag) { super.load(tag);energy=Math.max(0,Math.min(grade().inputMaximum(),tag.getLong("gt.axial.energy")));stopped=tag.getBoolean("gt.stopped");overloaded=tag.getBoolean("gt.overloaded");rotorFormed=tag.getBoolean("gt.rotor_formed");rotorActive=rotorFormed&&tag.getBoolean("gt.rotor_active"); }
}
