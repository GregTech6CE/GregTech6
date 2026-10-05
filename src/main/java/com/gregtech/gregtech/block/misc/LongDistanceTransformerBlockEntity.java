/**
 * Copyright (c) 2021 GregTech-6 Team
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

package com.gregtech.gregtech.block.misc;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.data.GregTechTags;
import java.util.*;
public class LongDistanceTransformerBlockEntity extends com.gregtech.gregtech.blockentity.GTEnergyBlockEntity implements com.gregtech.gregtech.api.machine.MachineControl.Provider {
    private long distance,maximumVoltage,history; private boolean active; private Set<BlockPos> wires=Set.of();
    public LongDistanceTransformerBlockEntity(BlockPos pos,BlockState state){super(com.gregtech.gregtech.registry.GTBlockEntities.LONG_DIST_TRANSFORMER.get(),pos,state);link=new com.gregtech.gregtech.content.logistics.LongDistanceLink<>(pos,this,()->!isRemoved()&&(level==null||level.hasChunkAt(worldPosition)));}
    private Direction front(){return getBlockState().getValue(LongDistanceTransformerBlock.FACING);}

    private final Map<BlockPos,BlockState> watched=new HashMap<>();
    private boolean stopped;
    private long observedVersion=Long.MIN_VALUE;
    private final com.gregtech.gregtech.content.logistics.LongDistanceLink<BlockPos,LongDistanceTransformerBlockEntity> link;
    private final com.gregtech.gregtech.api.machine.MachineControl control=com.gregtech.gregtech.api.machine.MachineControls.switchable(this,()->!stopped,value->setStopped(!value),()->active,()->active);
    @Override public com.gregtech.gregtech.api.machine.MachineControl machineControl(Direction side){return control;}
    public boolean isStopped(){return stopped;}
    public void setStopped(boolean value){stopped=value;setChanged();}
    public void invalidateRoute(){link.reset();watched.clear();observedVersion=Long.MIN_VALUE;}
    private BlockState observe(BlockPos pos){var state=level.getBlockState(pos);watched.put(pos,state);return state;}
    private boolean changed(){
        long version=LongDistanceTopology.version(level);
        if(version==observedVersion)return false;observedVersion=version;
        return watched.entrySet().stream().anyMatch(e->{
            if(!level.hasChunkAt(e.getKey()))return false; // Cached paths do not keep chunks loaded.
            var current=level.getBlockState(e.getKey());var previous=e.getValue();
            if(current==previous)return false;
            return !(current.getBlock()==previous.getBlock()&&current.getBlock() instanceof LongDistanceTransformerBlock&&current.getValue(LongDistanceTransformerBlock.FACING)==previous.getValue(LongDistanceTransformerBlock.FACING));
        });
    }
    private LongDistanceTransformerBlockEntity receiver(){
        if(level==null||level.isClientSide||stopped)return null;
        if(changed())invalidateRoute();
        if(link.known()&&!worldPosition.equals(link.targetPosition())&&(distance<=0||maximumVoltage<=0))invalidateRoute();
        if(!link.known())scan();
        var receiver=link.targetOwner();
        var position=link.targetPosition();
        if(receiver==null&&position!=null&&!position.equals(worldPosition)&&level.hasChunkAt(position)){
            var entity=level.getBlockEntity(position);
            if(entity instanceof LongDistanceTransformerBlockEntity peer) {link.bind(peer.link);receiver=peer;}
            else if(entity!=null){invalidateRoute();return null;}
        }
        return receiver!=null&&link.claim()?receiver:null;
    }
    public void rescan(){if(link.receiving())return;invalidateRoute();scan();}
    private void scan(){
        if(level==null||level.isClientSide||!link.beginScan())return;
        distance=0;maximumVoltage=0;wires=Set.of();
        watched.clear();observe(worldPosition);
        var start=worldPosition.relative(front().getOpposite());
        if(!(observe(start).getBlock() instanceof LongDistPipeBlock line)||!line.isWire())return;
        var result=com.gregtech.gregtech.content.logistics.LongDistanceNetwork.scan(worldPosition,start,line.networkIdentity(),new com.gregtech.gregtech.content.logistics.LongDistanceNetwork.World<BlockPos,LongDistanceTransformerBlockEntity>(){
            public String identity(BlockPos pos){
                if(level.isOutsideBuildHeight(pos))return null;
                return observe(pos).getBlock() instanceof LongDistPipeBlock candidate && candidate.isWire()?candidate.networkIdentity():null;
            }
            public Iterable<BlockPos> neighbors(BlockPos pos){return Arrays.stream(Direction.values()).map(pos::relative).toList();}
            public LongDistanceTransformerBlockEntity endpoint(BlockPos pos){return level.getBlockEntity(pos) instanceof LongDistanceTransformerBlockEntity peer?peer:null;}
            public BlockPos front(LongDistanceTransformerBlockEntity peer){return peer.worldPosition.relative(peer.front());}
        });
        observedVersion=LongDistanceTopology.version(level);
        if(result!=null){link.bind(result.receiver().link);distance=result.distance();maximumVoltage=line.maximumVoltage();wires=result.lines();setChanged();}
    }
    public void describe(net.minecraft.world.entity.player.Player player){
        var sender=link.senderOwner();
        if(sender!=null){
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.gregtech.long_distance.receiver"),false);
            coordinates(player,"sender",sender.worldPosition);
        }else{
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.gregtech.long_distance."+(receiver()!=null?"target":"no_target")),false);
            var target=link.targetPosition();if(target!=null&&!target.equals(worldPosition))coordinates(player,"target_position",target);
        }
    }
    private void coordinates(net.minecraft.world.entity.player.Player player,String name,BlockPos pos){player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.gregtech.long_distance."+name,pos.getX(),pos.getY(),pos.getZ()),false);}

    public static long loss(long distance) { return com.gregtech.gregtech.content.logistics.LongDistanceRules.loss(distance); }
    @Override public boolean isEnergyType(GregTechTags.Tag type,Direction side,boolean emitting) { return type==GregTechTags.Energy.EU; }
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side) { return List.of(GregTechTags.Energy.EU); }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type,Direction side,boolean theoretical) {
        return type==GregTechTags.Energy.EU && (side==null || side==front()) && (theoretical || receiver()!=null);
    }
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type,Direction side,boolean theoretical) {
        return type==GregTechTags.Energy.EU && (side==null || side==front().getOpposite());
    }
    @Override public long doEnergyInjection(GregTechTags.Tag type,Direction side,long size,long amount,boolean execute) {
        if(type!=GregTechTags.Energy.EU || stopped || size==0 || size==Long.MIN_VALUE || amount<=0
                || side!=null && side!=front() || level==null || level.isClientSide) return 0;
        long magnitude=Math.abs(size);
        var visiting=TRANSFERRING.get(); if(!visiting.add(this)) return 0;
        try {
            // TileEntityBase01Root checks accepting/target before calling the original doInject.
            var receiver=receiver(); if(receiver==null) return 0;
            if(magnitude>voltage()*2) { if(execute) level.destroyBlock(worldPosition,false); return amount; }
            if(magnitude<voltage()/2) return amount; // Original root consumes undersized packets without transmitting.
            if(magnitude>maximumVoltage) {
                if(execute) {rescan();for(var pos:wires) level.setBlockAndUpdate(pos,Blocks.FIRE.defaultBlockState());}
                return amount;
            }
            long output=magnitude-loss(distance);
            if(output<=0 || output<receiver.voltage()/2) return 0;
            if(output>receiver.voltage()*2) { if(execute) level.destroyBlock(receiver.worldPosition,false); return amount; }
            if(!execute) return amount; // Original simulation validates the route, not downstream demand.
            var out=receiver.front().getOpposite(); var pos=receiver.worldPosition.relative(out);
            if(!level.hasChunkAt(pos)) return 0;
            if(level.getBlockEntity(pos) instanceof IEnergyBlock sink) {
                long used=Math.max(0,Math.min(amount,sink.doEnergyInjection(type,out.getOpposite(),size<0?-output:output,amount,true)));
                if(used>0){active=true;receiver.active=true;setChanged();receiver.setChanged();}return used;
            }
            return 0;
        } finally { visiting.remove(this); if(visiting.isEmpty()) TRANSFERRING.remove(); }
    }
    @Override public long getEnergyDemanded(GregTechTags.Tag type,Direction side,long size) { return isEnergyAcceptingFrom(type,side,false)?Long.MAX_VALUE:0; }
    @Override public long getEnergyOffered(GregTechTags.Tag type,Direction side,long size) { return 0; }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type,Direction side) { return type==GregTechTags.Energy.EU?voltage():0; }
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type,Direction side) { return getEnergySizeInputRecommended(type,side); }

    public long voltage(){return ((LongDistanceTransformerBlock)getBlockState().getBlock()).voltage();}
    private static final ThreadLocal<Set<LongDistanceTransformerBlockEntity>> TRANSFERRING=ThreadLocal.withInitial(HashSet::new);
    public void tickActivity(){
        boolean unavailable=link.known()&&link.targetOwner()==null&&!worldPosition.equals(link.targetPosition());
        history=com.gregtech.gregtech.content.logistics.LongDistanceNetwork.nextHistory(history,active,stopped,unavailable);
        int value=com.gregtech.gregtech.content.logistics.LongDistanceNetwork.activity(history,stopped,unavailable);
        active=false;
        if(getBlockState().getValue(LongDistanceTransformerBlock.ACTIVITY)!=value)level.setBlockAndUpdate(worldPosition,getBlockState().setValue(LongDistanceTransformerBlock.ACTIVITY,value));
    }

    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putBoolean("gt.stopped",stopped);var target=link.targetPosition();if(target!=null&&!target.equals(worldPosition))tag.putLong("gt.target",target.asLong());tag.putLong("gt.distance",distance);tag.putLong("gt.throughput",maximumVoltage);tag.putLong("gt.activity.history",history);tag.putBoolean("gt.activity",active);}
    @Override public void load(CompoundTag tag){super.load(tag);stopped=tag.getBoolean("gt.stopped");link.restore(tag.contains("gt.target")?BlockPos.of(tag.getLong("gt.target")):null);watched.clear();distance=tag.getLong("gt.distance");maximumVoltage=tag.getLong("gt.throughput");history=tag.getLong("gt.activity.history");active=tag.getBoolean("gt.activity");wires=Set.of();}
}
