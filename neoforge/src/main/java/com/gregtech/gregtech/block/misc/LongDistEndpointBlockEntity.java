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
public class LongDistEndpointBlockEntity extends com.gregtech.gregtech.blockentity.CapabilityRelayBlockEntity implements com.gregtech.gregtech.api.machine.MachineControl.Provider {
    private long temperature;
    public LongDistEndpointBlockEntity(BlockPos pos,BlockState state){super(com.gregtech.gregtech.registry.GTBlockEntities.LONG_DIST_ENDPOINT.get(),pos,state);link=new com.gregtech.gregtech.content.logistics.LongDistanceLink<>(pos,this,()->!isRemoved()&&(level==null||level.hasChunkAt(worldPosition)));}
    private Direction front(){return getBlockState().getValue(LongDistEndpointBlock.FACING);}

    private final Map<BlockPos,BlockState> watched=new HashMap<>();
    private boolean stopped;
    private long observedVersion=Long.MIN_VALUE;
    private final com.gregtech.gregtech.content.logistics.LongDistanceLink<BlockPos,LongDistEndpointBlockEntity> link;
    private final com.gregtech.gregtech.api.machine.MachineControl control=com.gregtech.gregtech.api.machine.MachineControls.switchable(this,()->!stopped,value->setStopped(!value),()->false,()->false);
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
    private LongDistEndpointBlockEntity receiver(){
        if(level==null||level.isClientSide||stopped)return null;
        if(changed())invalidateRoute();
        if(fluid()&&link.known()&&!worldPosition.equals(link.targetPosition())&&temperature<=0)invalidateRoute();
        if(!link.known())scan();
        var receiver=link.targetOwner();
        var position=link.targetPosition();
        if(receiver==null&&position!=null&&!position.equals(worldPosition)&&level.hasChunkAt(position)){
            var entity=level.getBlockEntity(position);
            if(entity instanceof LongDistEndpointBlockEntity peer && peer.fluid()==fluid()) {link.bind(peer.link);receiver=peer;}
            else if(entity!=null){invalidateRoute();return null;}
        }
        return receiver!=null&&link.claim()?receiver:null;
    }
    public void rescan(){if(link.receiving())return;invalidateRoute();scan();}
    private void scan(){
        if(level==null||level.isClientSide||!link.beginScan())return;
        temperature=0;
        watched.clear();observe(worldPosition);
        var start=worldPosition.relative(front().getOpposite());
        if(!(observe(start).getBlock() instanceof LongDistPipeBlock line)||!line.acceptsPipeline(fluid()))return;
        var result=com.gregtech.gregtech.content.logistics.LongDistanceNetwork.scan(worldPosition,start,line.networkIdentity(),new com.gregtech.gregtech.content.logistics.LongDistanceNetwork.World<BlockPos,LongDistEndpointBlockEntity>(){
            public String identity(BlockPos pos){
                if(level.isOutsideBuildHeight(pos))return null;
                return observe(pos).getBlock() instanceof LongDistPipeBlock candidate && candidate.acceptsPipeline(fluid())?candidate.networkIdentity():null;
            }
            public Iterable<BlockPos> neighbors(BlockPos pos){return Arrays.stream(Direction.values()).map(pos::relative).toList();}
            public LongDistEndpointBlockEntity endpoint(BlockPos pos){return level.getBlockEntity(pos) instanceof LongDistEndpointBlockEntity peer&&peer.fluid()==fluid()?peer:null;}
            public BlockPos front(LongDistEndpointBlockEntity peer){return peer.worldPosition.relative(peer.front());}
        });
        observedVersion=LongDistanceTopology.version(level);
        if(result!=null){link.bind(result.receiver().link);temperature=line.maximumTemperature();setChanged();}
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


    private boolean fluid(){return ((LongDistEndpointBlock)getBlockState().getBlock()).isFluid();}
    @Override protected boolean supportsItems(){return !fluid();}
    @Override protected boolean supportsFluids(){return fluid();}
    @Override protected boolean supportsFluidExtraction(){return false;}
    @Override protected boolean permitsFluid(Direction side,net.neoforged.neoforge.fluids.FluidStack stack){
        if(stack.isEmpty())return false;
        var entry=com.gregtech.gregtech.registry.GTFluids.entryForFluid(stack.getFluid());
        return (entry==null?stack.getFluid().getFluidType().getTemperature(stack):entry.temperature())<=temperature;
    }
    @Override protected Target target(Direction side){var peer=receiver();return peer==null?null:new Target(peer.worldPosition.relative(peer.front().getOpposite()),peer.front());}

    @Override protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup){super.saveAdditional(tag,lookup);tag.putBoolean("gt.stopped",stopped);var target=link.targetPosition();if(target!=null&&!target.equals(worldPosition))tag.putLong("gt.target",target.asLong());tag.putLong("gt.temperature",temperature);}
    @Override protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup){super.loadAdditional(tag,lookup);stopped=tag.getBoolean("gt.stopped");link.restore(tag.contains("gt.target")?BlockPos.of(tag.getLong("gt.target")):null);watched.clear();temperature=tag.getLong("gt.temperature");}
}
