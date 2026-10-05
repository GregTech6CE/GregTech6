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

import com.gregtech.gregtech.blockentity.CapabilityRelayBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Original directed pipeline: source back -> pipes -> receiver front -> receiver back inventory. */
public class LongDistEndpointBlockEntity extends CapabilityRelayBlockEntity {
    private long scannedAt=Long.MIN_VALUE;
    private Target destination;
    private LongDistPipeBlock line;
    private final Set<BlockPos> pipes=new HashSet<>();
    private final Map<BlockPos,BlockState> watched=new HashMap<>();
    public LongDistEndpointBlockEntity(BlockPos pos,BlockState state) { super(com.gregtech.gregtech.registry.GTBlockEntities.LONG_DIST_ENDPOINT.get(),pos,state); }
    private boolean fluid() { return ((LongDistEndpointBlock)getBlockState().getBlock()).isFluid(); }
    private Direction facing() { return getBlockState().getValue(LongDistEndpointBlock.FACING); }
    @Override protected boolean supportsItems() { return !fluid(); }
    @Override protected boolean supportsFluids() { return fluid(); }
    @Override protected boolean supportsFluidExtraction() { return false; }
    @Override protected boolean permitsFluid(Direction side, net.minecraftforge.fluids.FluidStack stack) {
        if (stack.isEmpty() || line == null) return false;
        var entry = com.gregtech.gregtech.registry.GTFluids.entryForFluid(stack.getFluid());
        long temperature = entry == null ? stack.getFluid().getFluidType().getTemperature(stack) : entry.temperature();
        return temperature <= line.maximumTemperature();
    }
    @Override protected boolean exposes(Direction side) { return side==facing(); }
    private boolean pipe(BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() instanceof LongDistPipeBlock block
                && block.acceptsPipeline(fluid()) && (line == null || block.sameLine(line));
    }
    @Override protected Target target(Direction side) {
        if(side!=facing()) return null;
        long now=level.getGameTime();
        boolean changed=watched.entrySet().stream().anyMatch(e->!level.hasChunkAt(e.getKey())||level.getBlockState(e.getKey())!=e.getValue());
        boolean valid=destination!=null && !changed && pipes.stream().allMatch(this::pipe);
        if(changed || (destination!=null && !valid) || (com.gregtech.gregtech.content.logistics.LongDistanceRules.rescan(now,scannedAt,valid))) scan(now);
        return destination;
    }
    private void scan(long now) {
        scannedAt=now;destination=null;line=null;pipes.clear();watched.clear();
        watched.put(worldPosition,getBlockState());
        BlockPos start=worldPosition.relative(facing().getOpposite());
        if(level.hasChunkAt(start)) watched.put(start,level.getBlockState(start));
        if(!pipe(start)) return;
        line=(LongDistPipeBlock)level.getBlockState(start).getBlock();
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
                watched.put(next,state);
                if(state.getBlock() instanceof LongDistEndpointBlock endpoint && endpoint.isFluid()==fluid()) {
                    var front=state.getValue(LongDistEndpointBlock.FACING);
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
