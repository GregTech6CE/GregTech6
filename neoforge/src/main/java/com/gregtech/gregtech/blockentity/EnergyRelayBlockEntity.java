package com.gregtech.gregtech.blockentity;

import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.data.GregTechTags.Tag;
import net.minecraft.core.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
import java.util.function.BiFunction;

/** Native GT packet forwarding. No buffers, energy conversion, absolute-value conversion or chunk loads. */
public abstract class EnergyRelayBlockEntity extends CapabilityRelayBlockEntity implements IEnergyBlock {
    private final Map<Direction,net.neoforged.neoforge.energy.IEnergyStorage> flux=new EnumMap<>(Direction.class);
    private static final com.gregtech.gregtech.content.logistics.RelayVisitSet<EnergyRelayBlockEntity> ACTIVE=new com.gregtech.gregtech.content.logistics.RelayVisitSet<>();
    protected EnergyRelayBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);}
    protected abstract boolean supportsEnergy();
    public net.neoforged.neoforge.energy.IEnergyStorage fluxHandler(Direction side){
        if(isRemoved()||!supportsEnergy()||side==null)return null;
        return flux.computeIfAbsent(side,key->new net.neoforged.neoforge.energy.IEnergyStorage(){
            public int receiveEnergy(int amount,boolean simulate){return amount<=0?0:forward(key,net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,0,e->e.receiveEnergy(amount,simulate));}
            public int extractEnergy(int amount,boolean simulate){return amount<=0?0:forward(key,net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,0,e->e.extractEnergy(amount,simulate));}
            public int getEnergyStored(){return forward(key,net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,0,e->e.getEnergyStored());}
            public int getMaxEnergyStored(){return forward(key,net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,0,e->e.getMaxEnergyStored());}
            public boolean canExtract(){return forward(key,net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,false,e->e.canExtract());}
            public boolean canReceive(){return forward(key,net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,false,e->e.canReceive());}
        });
    }
    private <T> T forwardEnergy(Direction side,T absent,BiFunction<IEnergyBlock,Direction,T> operation){
        if(!supportsEnergy()||side==null||level==null||isRemoved())return absent;
        var visited=ACTIVE.get();if(!visited.add(this))return absent;
        try{
            var destination=target(side);
            if(destination==null)return absent;
            var destinationLevel=targetLevel(destination);
            if(!destinationLevel.hasChunkAt(destination.position()))return absent;
            var entity=destinationLevel.getBlockEntity(destination.position());
            if(entity==null||entity.isRemoved()||!(entity instanceof IEnergyBlock energy))return absent;
            return operation.apply(energy,destination.side());
        }finally{visited.remove(this);if(visited.isEmpty())ACTIVE.remove();}
    }
    @Override public boolean isEnergyType(Tag type,Direction side,boolean emitting){return forwardEnergy(side,false,(e,s)->e.isEnergyType(type,s,emitting));}
    @Override public Collection<Tag> getEnergyTypes(Direction side){return forwardEnergy(side,List.of(),(e,s)->List.copyOf(e.getEnergyTypes(s)));}
    @Override public boolean isEnergyAcceptingFrom(Tag type,Direction side,boolean theoretical){return forwardEnergy(side,false,(e,s)->e.isEnergyAcceptingFrom(type,s,theoretical));}
    @Override public boolean isEnergyEmittingTo(Tag type,Direction side,boolean theoretical){return forwardEnergy(side,false,(e,s)->e.isEnergyEmittingTo(type,s,theoretical));}
    @Override public long doEnergyInjection(Tag type,Direction side,long size,long amount,boolean execute){return forwardEnergy(side,0L,(e,s)->e.doEnergyInjection(type,s,size,amount,execute));}
    @Override public long doEnergyExtraction(Tag type,Direction side,long size,long amount,boolean execute){return forwardEnergy(side,0L,(e,s)->e.doEnergyExtraction(type,s,size,amount,execute));}
    @Override public long getEnergyDemanded(Tag type,Direction side,long size){return size==0||size==Long.MIN_VALUE?0:forwardEnergy(side,0L,(e,s)->e.getEnergyDemanded(type,s,size));}
    @Override public long getEnergyOffered(Tag type,Direction side,long size){return size==0||size==Long.MIN_VALUE?0:forwardEnergy(side,0L,(e,s)->e.getEnergyOffered(type,s,size));}
    @Override public long getEnergySizeInputMin(Tag type,Direction side){return forwardEnergy(side,0L,(e,s)->e.getEnergySizeInputMin(type,s));}
    @Override public long getEnergySizeInputRecommended(Tag type,Direction side){return forwardEnergy(side,0L,(e,s)->e.getEnergySizeInputRecommended(type,s));}
    @Override public long getEnergySizeInputMax(Tag type,Direction side){return forwardEnergy(side,0L,(e,s)->e.getEnergySizeInputMax(type,s));}
    @Override public long getEnergySizeOutputMin(Tag type,Direction side){return forwardEnergy(side,0L,(e,s)->e.getEnergySizeOutputMin(type,s));}
    @Override public long getEnergySizeOutputRecommended(Tag type,Direction side){return forwardEnergy(side,0L,(e,s)->e.getEnergySizeOutputRecommended(type,s));}
    @Override public long getEnergySizeOutputMax(Tag type,Direction side){return forwardEnergy(side,0L,(e,s)->e.getEnergySizeOutputMax(type,s));}
    @Override public boolean hasEnergySurface(Direction side){return forwardEnergy(side,false,(e,s)->e.hasEnergySurface(s));}
    @Override public boolean isEnergyCapacitorType(Tag type,Direction side){return forwardEnergy(side,false,(e,s)->e.isEnergyCapacitorType(type,s));}
    @Override public long getEnergyStored(Tag type,Direction side){return forwardEnergy(side,0L,(e,s)->e.getEnergyStored(type,s));}
    @Override public long getEnergyCapacity(Tag type,Direction side){return forwardEnergy(side,0L,(e,s)->e.getEnergyCapacity(type,s));}
    @Override public Collection<Tag> getEnergyCapacitorTypes(Direction side){return forwardEnergy(side,List.of(),(e,s)->List.copyOf(e.getEnergyCapacitorTypes(s)));}
}
