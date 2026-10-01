package com.gregtech.gregtech.blockentity.energy;

import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Lossless LU packet routing. Iterative traversal prevents stack overflow and revisiting cycles. */
public final class LaserFiberBlockEntity extends GTEnergyBlockEntity {
    private static final int MAX_VISITED=4096;
    /** GT6 MultiTileEntityWireLaser.mTransferred / mTransferredLast: LU sent through this fiber. */
    private long transferred;
    private long transferredLast;
    public LaserFiberBlockEntity(BlockPos pos,BlockState state){super(GTBlockEntities.LASER_FIBER.get(),pos,state);}
    public long transferredLast(){return transferredLast;}
    /** GT6 onTick2 moves this tick's transfer count into the sensor-visible previous-tick value. */
    public void rollTransferWindow(){transferredLast=transferred;transferred=0;}
    private void recordTransfer(long size,long packets){
        long magnitude=Math.abs(size);
        long lu=packets>Long.MAX_VALUE/magnitude?Long.MAX_VALUE:packets*magnitude;
        transferred=lu>Long.MAX_VALUE-transferred?Long.MAX_VALUE:transferred+lu;
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
        var visited=new HashSet<BlockPos>();
        var queue=new ArrayDeque<LaserFiberBlockEntity>();
        var parents=new HashMap<LaserFiberBlockEntity,LaserFiberBlockEntity>();
        visited.add(worldPosition);if(side!=null)visited.add(worldPosition.relative(side));queue.add(this);
        long used=0;
        while(!queue.isEmpty() && used<amount && visited.size()<MAX_VISITED){
            var wire=queue.removeFirst();
            for(var output:Direction.values()){
                if(!wire.connected(output))continue;
                var next=wire.worldPosition.relative(output);
                if(!level.hasChunkAt(next)||visited.contains(next))continue;
                var target=level.getBlockEntity(next);
                if(target instanceof LaserFiberBlockEntity fiber){
                    if(fiber.connected(output.getOpposite())){visited.add(next);parents.put(fiber,wire);queue.addLast(fiber);}
                }else if(target instanceof IEnergyBlock receiver && receiver.isEnergyAcceptingFrom(type,output.getOpposite(),false)){
                    visited.add(next);
                    long accepted=Math.max(0,Math.min(amount-used,receiver.doEnergyInjection(type,output.getOpposite(),size,amount-used,execute)));
                    used+=accepted;
                    if(execute && accepted>0)
                        for(LaserFiberBlockEntity path=wire;path!=null;path=parents.get(path))path.recordTransfer(size,accepted);
                    if(used>=amount)break;
                }
                if(visited.size()>=MAX_VISITED)break;
            }
        }
        return used;
    }
}
