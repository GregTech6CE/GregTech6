package com.gregtech.gregtech.block.misc;

import net.minecraft.core.BlockPos;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
import net.minecraft.world.level.block.state.BlockState;

public class LongDistanceTransformerBlockEntity extends GTEnergyBlockEntity {
    private boolean stopped;
    private Route cachedRoute;
    private final Map<BlockPos,BlockState> watched = new HashMap<>();
    private static final ThreadLocal<Set<LongDistanceTransformerBlockEntity>> TRANSFERRING = ThreadLocal.withInitial(HashSet::new);
    private record Route(LongDistanceTransformerBlockEntity receiver, int distance, Set<BlockPos> wires, long maximumVoltage) {}
    public LongDistanceTransformerBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.LONG_DIST_TRANSFORMER.get(), pos, state);
    }
    private Direction front() { return getBlockState().getValue(LongDistanceTransformerBlock.FACING); }
    public long voltage() { return ((LongDistanceTransformerBlock)getBlockState().getBlock()).voltage(); }
    public boolean isStopped() { return stopped; }
    public void setStopped(boolean value) { stopped=value; setChanged(); }
    private boolean wire(BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() instanceof LongDistPipeBlock pipe && pipe.isWire();
    }
    /** Loaded homogeneous network only. Reject ambiguous forks instead of choosing a random endpoint. */
    private Route route() {
        if(level==null || level.isClientSide || stopped) return null;
        if(cachedRoute!=null && !cachedRoute.receiver().isRemoved()
                && watched.entrySet().stream().allMatch(e->level.hasChunkAt(e.getKey()) && level.getBlockState(e.getKey())==e.getValue()))
            return cachedRoute.receiver().stopped ? null : cachedRoute;
        cachedRoute=null;watched.clear();
        var result=scan();
        if(result!=null) cachedRoute=result;
        return result;
    }
    private Route scan() {
        watched.put(worldPosition,getBlockState());
        var start=worldPosition.relative(front().getOpposite());
        if(!wire(start)) return null;
        var wireBlock=(LongDistPipeBlock)level.getBlockState(start).getBlock();
        watched.put(start,level.getBlockState(start));
        var queue=new ArrayDeque<BlockPos>(); var distance=new HashMap<BlockPos,Integer>();
        var wires=new HashSet<BlockPos>(); var sources=new HashSet<BlockPos>();
        var receivers=new HashMap<LongDistanceTransformerBlockEntity,Integer>();
        queue.add(start); distance.put(start,0);
        while(!queue.isEmpty()) {
            var pos=queue.remove(); wires.add(pos); if(wires.size()>com.gregtech.gregtech.content.logistics.LongDistanceRules.MAX_SCAN) return null;
            for(var side:Direction.values()) {
                var next=pos.relative(side);
                if(!level.hasChunkAt(next)) return null;
                watched.put(next,level.getBlockState(next));
                if(wire(next)) {
                    if(level.getBlockState(next).getBlock()!=wireBlock) return null;
                    if(!distance.containsKey(next)) { distance.put(next,distance.get(pos)+1); queue.add(next); }
                } else if(level.getBlockEntity(next) instanceof LongDistanceTransformerBlockEntity endpoint) {
                    if(next.relative(endpoint.front().getOpposite()).equals(pos)) sources.add(next);
                    if(next.relative(endpoint.front()).equals(pos)) receivers.putIfAbsent(endpoint,distance.get(pos)+1);
                }
            }
        }
        if(!com.gregtech.gregtech.content.logistics.LongDistanceRules.unique(sources.size(),sources.contains(worldPosition),receivers.size())) return null;
        var receiver=receivers.keySet().iterator().next();
        return receiver==this || receiver.stopped ? null : new Route(receiver,receivers.get(receiver),wires,wireBlock.maximumVoltage());
    }
    public static long loss(long distance) { return com.gregtech.gregtech.content.logistics.LongDistanceRules.loss(distance); }
    @Override public boolean isEnergyType(GregTechTags.Tag type,Direction side,boolean emitting) { return type==GregTechTags.Energy.EU; }
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side) { return List.of(GregTechTags.Energy.EU); }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type,Direction side,boolean theoretical) {
        return type==GregTechTags.Energy.EU && !stopped && (side==null || side==front()) && (theoretical || route()!=null);
    }
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type,Direction side,boolean theoretical) {
        return type==GregTechTags.Energy.EU && !stopped && (side==null || side==front().getOpposite());
    }
    @Override public long doEnergyInjection(GregTechTags.Tag type,Direction side,long size,long amount,boolean execute) {
        if(type!=GregTechTags.Energy.EU || stopped || size==0 || size==Long.MIN_VALUE || amount<=0
                || side!=null && side!=front() || level==null || level.isClientSide) return 0;
        long magnitude=Math.abs(size);
        var active=TRANSFERRING.get(); if(!active.add(this)) return 0;
        try {
            var route=route(); if(route==null) return 0;
            if(magnitude<voltage()/2) return amount; // Original root consumes undersized packets without transmitting.
            if(magnitude>voltage()*2) { if(execute) level.destroyBlock(worldPosition,false); return amount; }
            if(magnitude>route.maximumVoltage()) {
                if(execute) for(var pos:route.wires()) level.setBlockAndUpdate(pos,Blocks.FIRE.defaultBlockState());
                return amount;
            }
            long output=magnitude-loss(route.distance()); var receiver=route.receiver();
            if(output<=0 || output<receiver.voltage()/2) return 0;
            if(output>receiver.voltage()*2) { if(execute) level.destroyBlock(receiver.worldPosition,false); return amount; }
            if(!execute) return amount; // Original simulation validates the route, not downstream demand.
            var out=receiver.front().getOpposite(); var pos=receiver.worldPosition.relative(out);
            if(!level.hasChunkAt(pos)) return 0;
            if(level.getBlockEntity(pos) instanceof IEnergyBlock sink)
                return Math.max(0,Math.min(amount,sink.doEnergyInjection(type,out.getOpposite(),size<0?-output:output,amount,true)));
            return 0;
        } finally { active.remove(this); if(active.isEmpty()) TRANSFERRING.remove(); }
    }
    @Override public long getEnergyDemanded(GregTechTags.Tag type,Direction side,long size) { return isEnergyAcceptingFrom(type,side,false)?Long.MAX_VALUE:0; }
    @Override public long getEnergyOffered(GregTechTags.Tag type,Direction side,long size) { return 0; }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type,Direction side) { return type==GregTechTags.Energy.EU?voltage():0; }
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type,Direction side) { return getEnergySizeInputRecommended(type,side); }
    @Override protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) { super.saveAdditional(tag,lookup); tag.putBoolean("gt.stopped",stopped); }
    @Override protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) { super.loadAdditional(tag,lookup); stopped=tag.getBoolean("gt.stopped"); cachedRoute=null; watched.clear(); }
}
