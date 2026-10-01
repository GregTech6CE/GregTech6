package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Collection;
import java.util.List;

/** Packet input and persistence shared by automatic tools. */
public abstract class AutoToolBlockEntity extends GTEnergyBlockEntity {
    protected long energy;
    protected boolean stopped;
    protected AutoToolBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state) { super(type,pos,state); }
    protected abstract GregTechTags.Tag energyType();
    public abstract void tick();
    public Direction facing() { return getBlockState().getValue(AutoToolBlock.FACING); }
    public long input() { return ((AutoToolBlock)getBlockState().getBlock()).input(); }
    public int quality() { return ((AutoToolBlock)getBlockState().getBlock()).quality(); }
    public boolean stopped() { return stopped; }
    public void setStopped(boolean value) { stopped=value; syncToClient(); }
    public long storedEnergy() { return energy; }
    @Override public boolean isEnergyType(GregTechTags.Tag type,Direction side,boolean emitting) { return !emitting && type==energyType(); }
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side) { return List.of(energyType()); }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type,Direction side,boolean theoretical) {
        return isEnergyType(type,side,false) && (theoretical || !stopped) && acceptsSide(side);
    }
    protected abstract boolean acceptsSide(Direction side);
    @Override public long getEnergyDemanded(GregTechTags.Tag type,Direction side,long size) { return isEnergyAcceptingFrom(type,side,false)?1:0; }
    @Override public long getEnergyOffered(GregTechTags.Tag type,Direction side,long size) { return 0; }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type,Direction side) { return input(); }
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type,Direction side) { return 0; }
    protected boolean overvoltage(long size) {
        if (Math.abs(size)<=input()*2) return false;
        if(level!=null && !level.isClientSide) level.explode(null,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,2,net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
        return true;
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag);tag.putLong("gt.energy",energy);tag.putBoolean("gt.stopped",stopped); }
    @Override public void load(CompoundTag tag) { super.load(tag);energy=Math.max(0,tag.getLong("gt.energy"));stopped=tag.getBoolean("gt.stopped"); }
}
