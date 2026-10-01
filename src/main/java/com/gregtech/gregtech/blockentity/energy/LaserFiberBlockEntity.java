package com.gregtech.gregtech.blockentity.energy;

import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
import com.gregtech.gregtech.content.energy.LaserPacketRouter;

/** Lossless LU packet routing. Iterative traversal prevents stack overflow and revisiting cycles. */
public final class LaserFiberBlockEntity extends GTEnergyBlockEntity {
    /** GT6 MultiTileEntityWireLaser.mTransferred / mTransferredLast: LU sent through this fiber. */
    private long transferred;
    private long transferredLast;
    public LaserFiberBlockEntity(BlockPos pos,BlockState state){super(GTBlockEntities.LASER_FIBER.get(),pos,state);}
    public long transferredLast(){return transferredLast;}
    /** GT6 onTick2 moves this tick's transfer count into the sensor-visible previous-tick value. */
    public void rollTransferWindow(){transferredLast=transferred;transferred=0;}
    private void recordTransfer(long size,long packets){
        transferred=LaserPacketRouter.recordedUnits(transferred,size,packets);
    }
    private boolean connected(Direction side){
        // onPlace may update connections before LevelChunk finishes assigning the entity's initial state.
        // The world state also immediately reflects cutter edits; never route using the stale initial cache.
        var state=level==null?getBlockState():level.getBlockState(worldPosition);
        return !isRemoved() && state.getBlock() instanceof com.gregtech.gregtech.block.energy.LaserFiberBlock
                && (side==null||state.getValue(ElectricWireBlock.propFor(side)));
    }
    @Override public boolean isEnergyType(GregTechTags.Tag type,Direction side,boolean emitting){return type==GregTechTags.Energy.LU;}
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side){return List.of(GregTechTags.Energy.LU);}
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type,Direction side,boolean theoretical){return type==GregTechTags.Energy.LU&&connected(side);}
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type,Direction side,boolean theoretical){return isEnergyAcceptingFrom(type,side,theoretical);}
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type,Direction side){return type==GregTechTags.Energy.LU?Long.MAX_VALUE:0;}
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type,Direction side){return getEnergySizeInputRecommended(type,side);}
    @Override public long getEnergySizeInputMin(GregTechTags.Tag type,Direction side){return 0;}
    @Override public long getEnergySizeOutputMin(GregTechTags.Tag type,Direction side){return 0;}
    @Override public long getEnergySizeInputMax(GregTechTags.Tag type,Direction side){return getEnergySizeInputRecommended(type,side);}
    @Override public long getEnergySizeOutputMax(GregTechTags.Tag type,Direction side){return getEnergySizeInputRecommended(type,side);}
    @Override public long getEnergyOffered(GregTechTags.Tag type,Direction side,long size){return 0;}
    @Override public long getEnergyDemanded(GregTechTags.Tag type,Direction side,long size){return isEnergyAcceptingFrom(type,side,false)?Long.MAX_VALUE:0;}
    @Override public synchronized long doEnergyInjection(GregTechTags.Tag type,Direction side,long size,long amount,boolean execute){
        if(level==null||size==0||size==Long.MIN_VALUE||amount<=0||!isEnergyAcceptingFrom(type,side,false))return 0;
        return LaserPacketRouter.route(worldPosition,side==null?-1:side.ordinal(),size,amount,execute,new LaserPacketRouter.Network<BlockPos>(){
            public BlockPos neighbor(BlockPos node,int direction){return node.relative(Direction.values()[direction]);}
            public boolean loaded(BlockPos node){return level.hasChunkAt(node);}
            public boolean fiber(BlockPos node){return level.getBlockEntity(node) instanceof LaserFiberBlockEntity;}
            public boolean connected(BlockPos node,int direction){return level.getBlockEntity(node) instanceof LaserFiberBlockEntity fiber&&fiber.connected(Direction.values()[direction]);}
            public boolean accepts(BlockPos node,int direction){return level.getBlockEntity(node) instanceof IEnergyBlock receiver&&receiver.isEnergyAcceptingFrom(type,Direction.values()[direction],false);}
            public long inject(BlockPos node,int direction,long size,long amount,boolean execute){return level.getBlockEntity(node) instanceof IEnergyBlock receiver?receiver.doEnergyInjection(type,Direction.values()[direction],size,amount,execute):0;}
            public void record(BlockPos node,long size,long accepted){if(level.getBlockEntity(node) instanceof LaserFiberBlockEntity fiber)fiber.recordTransfer(size,accepted);}
        });
    }
}
