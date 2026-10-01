package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** GT6 17221–17224: front RU, rear EU, 75% conversion, 17 walls and 18 copper coils. */
public class LargeDynamoControllerBlockEntity extends AxialGeneratorBlockEntity {
    public LargeDynamoControllerBlockEntity(BlockPos pos,BlockState state) { super(GTBlockEntities.LARGE_DYNAMO.get(),pos,state); }
    public static void serverTick(Level level,BlockPos pos,BlockState state,LargeDynamoControllerBlockEntity machine) { machine.tick(); }
    public void tick() { if(level!=null&&!level.isClientSide&&isStructureOk()&&!overloaded)convertAndEmit(); }
    @Override public boolean isEnergyType(GregTechTags.Tag type,Direction side,boolean emitting) { return !emitting&&type==GregTechTags.Energy.RU&&(side==null||side==front()); }
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side) { return side==null||side==front()?List.of(GregTechTags.Energy.RU):List.of(); }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type,Direction side,boolean theoretical) { return isEnergyType(type,side,false)&&(theoretical||!stopped&&!overloaded&&isStructureOk()); }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type,Direction side) { return type==GregTechTags.Energy.RU?grade().input():0; }
    @Override public long getEnergyDemanded(GregTechTags.Tag type,Direction side,long size) {
        if(size==Long.MIN_VALUE)return 0;size=Math.abs(size);
        return isEnergyAcceptingFrom(type,side,false)&&size>=grade().input()/2&&size<=grade().inputMaximum()?(grade().inputMaximum()-energy)/size:0;
    }
    @Override public synchronized long doEnergyInjection(GregTechTags.Tag type,Direction side,long size,long amount,boolean execute) { return doInject(type,side,size,amount,execute); }
    @Override public long doInject(GregTechTags.Tag type,Direction side,long size,long amount,boolean execute) {
        if(amount<=0||size==Long.MIN_VALUE||!isEnergyAcceptingFrom(type,side,false))return 0;
        size=Math.abs(size);
        if(size<grade().input()/2)return 0;
        if(size>grade().inputMaximum()) { if(execute)overload();return amount; }
        long accepted=Math.min(amount,(grade().inputMaximum()-energy)/size);
        if(execute&&accepted>0){energy+=accepted*size;setChanged();}return accepted;
    }
    @Override public long getEnergyStored(GregTechTags.Tag type,Direction side) { return type==GregTechTags.Energy.RU?energy:0; }
    @Override public long getEnergyCapacity(GregTechTags.Tag type,Direction side) { return type==GregTechTags.Energy.RU?grade().inputMaximum():0; }
    @Override public void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        // Preserve the old RU buffer up to the new physical capacity. Old EU storage no longer exists.
        if(!tag.contains("gt.axial.energy"))energy=Math.max(0,Math.min(grade().inputMaximum(),tag.getLong("gt.ru")));
    }
}
