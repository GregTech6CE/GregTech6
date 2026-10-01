package com.gregtech.gregtech.blockentity.energy;

import com.gregtech.gregtech.api.machine.MachineControl;
import com.gregtech.gregtech.block.energy.*;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** GT6 fractional redstone attenuation. Rebuild from sources so disconnected loops cannot latch.
 * Loaded components are solved once per server tick; no static world/network cache or chunk loading.
 */
public final class SignalWireBlockEntity extends BlockEntity implements MachineControl.Provider {
    public static final long UNIT=Integer.MAX_VALUE;
    private static final ThreadLocal<Boolean> SAMPLING=ThreadLocal.withInitial(()->false);
    private long signal, solvedTick=Long.MIN_VALUE;
    private int mode;
    private Direction received;
    public SignalWireBlockEntity(BlockPos pos,BlockState state){super(GTBlockEntities.SIGNAL_WIRE.get(),pos,state);}
    @Override public void onLoad(){
        super.onLoad();
        if(level!=null&&level.isClientSide){requestModelDataUpdate();com.gregtech.gregtech.api.machine.PipeModelData.refreshCrossChunkNeighbors(level,worldPosition);}
    }
    @Override public net.minecraftforge.client.model.data.ModelData getModelData(){
        float[] halves=new float[6];java.util.Arrays.fill(halves,-1);
        if(level!=null)for(var side:Direction.values()){
            var pos=worldPosition.relative(side);
            if(level.hasChunkAt(pos)&&level.getBlockState(pos).getBlock() instanceof SignalWireBlock wire)halves[side.ordinal()]=wire.halfThickness();
        }
        return net.minecraftforge.client.model.data.ModelData.builder().with(com.gregtech.gregtech.api.machine.PipeModelData.NEIGHBOR_HALVES,halves).build();
    }
    public int mode(){return mode;}
    public void setMode(int value){
        if(isRemoved() || level!=null&&level.isClientSide)return;
        mode=Math.max(0,Math.min(15,value));setChanged();solvedTick=Long.MIN_VALUE;
    }
    public long signal(){return signal;}
    public long loss(){return UNIT/((SignalWireBlock)getBlockState().getBlock()).range();}
    public boolean connected(Direction side){return getBlockState().getValue(ElectricWireBlock.propFor(side));}
    public int comparator(){return (int)(signal/UNIT);}
    public int output(Direction side){
        if(SAMPLING.get()||level==null||isRemoved()||signal<=0||side==received||!connected(side))return 0;
        var pos=worldPosition.relative(side);
        if(!level.hasChunkAt(pos))return 0;
        var state=level.getBlockState(pos);
        if(state.getBlock() instanceof SignalWireBlock)return 0;
        int power=(int)((signal+UNIT-1)/UNIT);
        return Math.max(0,power-(state.getBlock() instanceof RedStoneWireBlock||state.isRedstoneConductor(level,pos)?1:0));
    }
    private SignalWireBlockEntity neighbor(Direction side){
        var pos=worldPosition.relative(side);
        return connected(side)&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof SignalWireBlockEntity other
                && !other.isRemoved() && other.connected(side.getOpposite())?other:null;
    }
    private record Wave(SignalWireBlockEntity wire,long value){}
    public void tickNetwork(){
        if(level==null||level.isClientSide||isRemoved()||solvedTick==level.getGameTime())return;
        var component=new LinkedHashSet<SignalWireBlockEntity>();
        var pending=new ArrayDeque<SignalWireBlockEntity>();component.add(this);pending.add(this);
        while(!pending.isEmpty()){
            var wire=pending.remove();
            for(var side:Direction.values()){
                var other=wire.neighbor(side);
                if(other!=null&&component.add(other))pending.add(other);
            }
        }
        var values=new HashMap<SignalWireBlockEntity,Long>();
        var inputs=new HashMap<SignalWireBlockEntity,Direction>();
        var waves=new PriorityQueue<Wave>(Comparator.comparingLong(Wave::value).reversed());
        SAMPLING.set(true);
        try {
            for(var wire:component){
                long value=Math.max(0,wire.mode*UNIT-wire.loss());
                for(var side:Direction.values())if(wire.connected(side)){
                    var pos=wire.worldPosition.relative(side);
                    if(!level.hasChunkAt(pos))continue;
                    var state=level.getBlockState(pos);
                    // Vanilla consumers are not sources, even when strongly powered through their solid body.
                    if(state.getBlock() instanceof SignalWireBlock || state.getBlock() instanceof DispenserBlock
                            || state.getBlock() instanceof HopperBlock || state.getBlock() instanceof PistonBaseBlock
                            || state.getBlock() instanceof TntBlock || state.getBlock() instanceof NoteBlock
                            || state.getBlock() instanceof TrapDoorBlock || state.getBlock() instanceof DoorBlock
                            || state.getBlock() instanceof PoweredRailBlock || state.getBlock() instanceof RedstoneLampBlock)continue;
                    long candidate=level.getSignal(pos,side)*UNIT-wire.loss();
                    if(candidate>value){value=candidate;inputs.put(wire,side);}
                }
                values.put(wire,value);if(value>0)waves.add(new Wave(wire,value));
            }
        } finally {SAMPLING.remove();}
        while(!waves.isEmpty()){
            var wave=waves.remove();
            if(wave.value()!=values.get(wave.wire()))continue;
            long next=wave.value()-wave.wire().loss();
            if(next<=0)continue;
            for(var side:Direction.values()){
                var other=wave.wire().neighbor(side);
                if(other!=null&&next>values.get(other)){
                    values.put(other,next);inputs.put(other,side.getOpposite());waves.add(new Wave(other,next));
                }
            }
        }
        var changed=new ArrayList<SignalWireBlockEntity>();
        for(var wire:component){
            long next=values.get(wire);var input=inputs.get(wire);
            if(wire.signal!=next||wire.received!=input || wire.getBlockState().getValue(SignalWireBlock.POWER)!=(next+UNIT-1)/UNIT)changed.add(wire);
            wire.signal=next;wire.received=input;wire.solvedTick=level.getGameTime();
        }
        // Publish the entire solution before notifying consumers; observers never see half an update.
        for(var wire:changed){
            int power=(int)((wire.signal+UNIT-1)/UNIT);
            var state=wire.getBlockState();
            if(state.getValue(SignalWireBlock.POWER)!=power)level.setBlock(wire.worldPosition,state.setValue(SignalWireBlock.POWER,power),2);
            level.updateNeighborsAt(wire.worldPosition,state.getBlock());
            for(var side:Direction.values())if(wire.connected(side)&&level.hasChunkAt(wire.worldPosition.relative(side)))
                level.updateNeighborsAt(wire.worldPosition.relative(side),state.getBlock());
            level.updateNeighbourForOutputSignal(wire.worldPosition,state.getBlock());
        }
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("mode",mode);}
    @Override public void load(CompoundTag tag){super.load(tag);mode=Math.max(0,Math.min(15,tag.getInt("mode")));signal=0;received=null;solvedTick=Long.MIN_VALUE;}
    @Override public MachineControl machineControl(Direction side){return new MachineControl(){
        public boolean available(){return !isRemoved();}
        public boolean supportsMode(){return true;}
        public int mode(){return available()?SignalWireBlockEntity.this.mode:0;}
        public int setMode(int value){SignalWireBlockEntity.this.setMode(value);return mode();}
        public boolean enabled(){return available();}
        public boolean setEnabled(boolean value){return enabled();}
        public boolean running(){return available()&&signal>0;}
        public boolean active(){return running();}
        public long progress(){return available()?1000*signal/UNIT:0;}
        public long progressMax(){return 16000;}
    };}
}
