package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.blockentity.CapabilityRelayBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Original directed pipeline: source back -> pipes -> receiver front -> receiver back inventory. */
public class LongDistEndpointBlockEntity extends CapabilityRelayBlockEntity {
    private long scannedAt=Long.MIN_VALUE;
    private Target destination;
    private final Set<BlockPos> pipes=new HashSet<>();
    private final Map<BlockPos,BlockState> endpoints=new HashMap<>();
    public LongDistEndpointBlockEntity(BlockPos pos,BlockState state) { super(com.gregtech.gregtech.registry.GTBlockEntities.LONG_DIST_ENDPOINT.get(),pos,state); }
    private boolean fluid() { return ((LongDistEndpointBlock)getBlockState().getBlock()).isFluid(); }
    private Direction facing() { return getBlockState().getValue(LongDistEndpointBlock.FACING); }
    @Override protected boolean supportsItems() { return !fluid(); }
    @Override protected boolean supportsFluids() { return fluid(); }
    @Override protected boolean exposes(Direction side) { return side==facing(); }
    private boolean pipe(BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() instanceof LongDistPipeBlock block
                && block.acceptsPipeline(fluid());
    }
    @Override protected Target target(Direction side) {
        if(side!=facing()) return null;
        long now=level.getGameTime();
        boolean valid=destination!=null && pipes.stream().allMatch(this::pipe)
                && endpoints.entrySet().stream().allMatch(e->level.hasChunkAt(e.getKey())&&level.getBlockState(e.getKey())==e.getValue());
        if((destination!=null && !valid) || (com.gregtech.gregtech.content.logistics.LongDistanceRules.rescan(now,scannedAt,valid))) scan(now);
        return destination;
    }
    private void scan(long now) {
        scannedAt=now;destination=null;pipes.clear();endpoints.clear();
        BlockPos start=worldPosition.relative(facing().getOpposite());
        if(!pipe(start)) return;
        var queue=new ArrayDeque<BlockPos>();var visited=new HashSet<BlockPos>();
        var sources=new HashSet<BlockPos>();var receivers=new HashSet<BlockPos>();
        queue.add(start);visited.add(start);
        while(!queue.isEmpty()) {
            var current=queue.remove();
            if(!level.hasChunkAt(current)) return;
            if(!pipe(current)) continue;
            pipes.add(current);
            if(pipes.size()>com.gregtech.gregtech.content.logistics.LongDistanceRules.MAX_SCAN) return;
            for(var direction:Direction.values()) {
                var next=current.relative(direction);
                if(!level.hasChunkAt(next)) return;
                var state=level.getBlockState(next);
                if(state.getBlock() instanceof LongDistEndpointBlock endpoint && endpoint.isFluid()==fluid()) {
                    var front=state.getValue(LongDistEndpointBlock.FACING);
                    endpoints.put(next,state);
                    if(next.relative(front).equals(current)) receivers.add(next);
                    if(next.relative(front.getOpposite()).equals(current)) sources.add(next);
                } else if(pipe(next) && visited.add(next)) queue.add(next);
            }
        }
        if(!com.gregtech.gregtech.content.logistics.LongDistanceRules.unique(sources.size(),sources.contains(worldPosition),receivers.size())) return;
        var receiver=receivers.iterator().next();var front=level.getBlockState(receiver).getValue(LongDistEndpointBlock.FACING);
        destination=new Target(receiver.relative(front.getOpposite()),front);
    }
}
