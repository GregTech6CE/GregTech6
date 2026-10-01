package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.block.machine.OriginalLightningRodControllerBlock;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Original five solid layers plus rod pillar; a strike stores 18000 packets of 32768 EU. */
public class LightningRodControllerBlockEntity extends GTEnergyBlockEntity implements com.gregtech.gregtech.api.multiblock.StructureController {
    public static final long PACKET=com.gregtech.gregtech.content.multiblock.AdvancedControllerRules.LIGHTNING_PACKET, ENERGY_CAPACITY=com.gregtech.gregtech.content.multiblock.AdvancedControllerRules.LIGHTNING_CAPACITY;
    private static final Set<LightningRodControllerBlockEntity> LOADED=Collections.newSetFromMap(new WeakHashMap<>());
    private long energy;
    private int rodLength;
    public LightningRodControllerBlockEntity(BlockPos pos,BlockState state) { super(GTBlockEntities.LIGHTNING_ROD.get(),pos,state); }
    @Override public void onLoad() { super.onLoad();if(level!=null&&!level.isClientSide) LOADED.add(this); }
    @Override public void setRemoved() { LOADED.remove(this);super.setRemoved(); }
    @Override public void onChunkUnloaded() { LOADED.remove(this);super.onChunkUnloaded(); }
    public boolean isStructureOk() {
        rodLength=0;if(level==null||isRemoved()) return false;
        boolean originalId = getBlockState().getBlock() instanceof OriginalLightningRodControllerBlock;
        for(int y=0;y<5;y++) for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) {
            var pos=worldPosition.offset(x,y,z);if(pos.equals(worldPosition)) continue;
            var required = originalId
                    ? LargeMachineParts.block(y%2==0?18004:18041)
                    : y%2==0?GTMultiblocks.LIGHTNING_ROD_WALL.get():GTMultiblocks.LARGE_NIOBIUM_TITANIUM_COIL.get();
            if(!level.hasChunkAt(pos)||!level.getBlockState(pos).is(required)) return false;
        }
        var pillar = originalId?LargeMachineParts.block(18104):GTMultiblocks.LIGHTNING_ROD_PILLAR.get();
        while(worldPosition.getY()+5+rodLength<level.getMaxBuildHeight()
                && level.getBlockState(worldPosition.above(5+rodLength)).is(pillar)) rodLength++;
        return true;
    }
    /** Called by the weather roll; also a deterministic seam for structure/energy regression tests. */
    public boolean captureStrike() {
        if(level==null||level.isClientSide||energy>=PACKET||!isStructureOk()||rodLength==0||worldPosition.getY()+rodLength<100) return false;
        var tip=worldPosition.above(4+rodLength);
        if(!level.canSeeSky(tip.above())) return false;
        energy=ENERGY_CAPACITY;setChanged();
        var lightning=net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
        if(lightning!=null) {lightning.moveTo(tip.getX()+.5,tip.getY(),tip.getZ()+.5);lightning.setVisualOnly(true);level.addFreshEntity(lightning);}
        return true;
    }
    public static void serverTick(Level level,BlockPos pos,BlockState state,LightningRodControllerBlockEntity machine) {
        if(!machine.isStructureOk()) { if(machine.energy!=0) {machine.energy=0;machine.setChanged();}return; }
        if(machine.energy>=PACKET) {
            long sent=0;var below=pos.below();
            if(level.hasChunkAt(below)&&level.getBlockEntity(below) instanceof IEnergyBlock target)
                sent=target.doEnergyInjection(GregTechTags.Energy.EU,Direction.UP,PACKET,Math.min(16,machine.energy/PACKET),true);
            machine.energy-=com.gregtech.gregtech.content.multiblock.AdvancedControllerRules.lightningDrain(machine.energy,sent);machine.setChanged();return;
        }
        if(machine.rodLength>0 && level.random.nextInt(1000000)<Math.min(100,machine.rodLength)
                && (level.isThundering() || level.isRaining()&&level.random.nextInt(10)==0)) {
            int neighbors=1;
            for(var rod:LOADED) if(rod!=machine && !rod.isRemoved() && rod.level==level && rod.rodLength>0
                    && Math.abs(rod.worldPosition.getX()-pos.getX())<256 && Math.abs(rod.worldPosition.getZ()-pos.getZ())<256) neighbors++;
            if(level.random.nextInt(neighbors)==0) machine.captureStrike();
        }
    }
    @Override public boolean isEnergyType(GregTechTags.Tag type,Direction side,boolean emitting) { return emitting&&type==GregTechTags.Energy.EU; }
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side) { return List.of(GregTechTags.Energy.EU); }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type,Direction side,boolean theoretical) { return false; }
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type,Direction side,boolean theoretical) { return type==GregTechTags.Energy.EU&&(side==null||side==Direction.DOWN)&&(theoretical||isStructureOk()); }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type,Direction side) { return 0; }
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type,Direction side) { return type==GregTechTags.Energy.EU?PACKET:0; }
    @Override public long getEnergyDemanded(GregTechTags.Tag type,Direction side,long size) { return 0; }
    @Override public long getEnergyOffered(GregTechTags.Tag type,Direction side,long size) { return size==PACKET&&isEnergyEmittingTo(type,side,false)?Math.min(16,energy/size):0; }
    @Override public long doExtract(GregTechTags.Tag type,Direction side,long size,long amount,boolean execute) {
        long packets=Math.max(0,Math.min(amount,getEnergyOffered(type,side,size)));
        if(execute&&packets>0) {energy-=packets*size;setChanged();}return packets;
    }
    @Override public long getEnergyStored(GregTechTags.Tag type,Direction side) { return type==GregTechTags.Energy.EU?energy:0; }
    @Override public long getEnergyCapacity(GregTechTags.Tag type,Direction side) { return type==GregTechTags.Energy.EU?ENERGY_CAPACITY:0; }
    @Override protected void saveAdditional(CompoundTag tag) {super.saveAdditional(tag);tag.putLong("gt.energy",energy);}
    @Override public void load(CompoundTag tag) {super.load(tag);energy=Math.max(0,Math.min(ENERGY_CAPACITY,tag.getLong("gt.energy")));}
}
