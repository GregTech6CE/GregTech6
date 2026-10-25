package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.energy.EnergyBlockViews;
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
public class LightningRodControllerBlockEntity extends GTEnergyBlockEntity implements com.gregtech.gregtech.api.multiblock.StructureController,
        com.gregtech.gregtech.api.multiblock.tracking.TrackedMultiblock {
    public static final long PACKET = com.gregtech.gregtech.content.multiblock.AdvancedControllerRules.LIGHTNING_PACKET,
            ENERGY_CAPACITY = com.gregtech.gregtech.content.multiblock.AdvancedControllerRules.LIGHTNING_CAPACITY;
    private static final Set<LightningRodControllerBlockEntity> LOADED = Collections.newSetFromMap(new WeakHashMap<>());
    private long energy;
    private int rodLength;

    public LightningRodControllerBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.LIGHTNING_ROD.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            LOADED.add(this);
            com.gregtech.gregtech.api.multiblock.tracking.StructureTracking.attach(this);
        }
    }

    @Override
    public void setRemoved() {
        LOADED.remove(this);
        com.gregtech.gregtech.api.multiblock.tracking.StructureTracking.onRemoved(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        LOADED.remove(this);
        com.gregtech.gregtech.api.multiblock.tracking.StructureTracking.onChunkUnloaded(this);
        super.onChunkUnloaded();
    }

    @Override public com.gregtech.gregtech.api.multiblock.tracking.StructureDefinition structureDefinition() {
        return com.gregtech.gregtech.content.multiblock.SpecialStructures.lightning(this);
    }
    @Override public boolean applyStructure(com.gregtech.gregtech.api.multiblock.tracking.PatternMatch match) {
        rodLength = match.counts().getOrDefault("rod", 0);
        return true;
    }
    @Override public void releaseStructureClaims() { rodLength = 0; }
    @Override public boolean isStructureOk() {
        return com.gregtech.gregtech.api.multiblock.tracking.StructureTracking.query(this);
    }
    private boolean structureFormed;
    @Override public void structureFormedChanged(boolean formed) { structureFormed = formed; }
    private boolean structureOk() {
        return structureFormed;
    }
    /** Called by the weather roll; also a deterministic seam for structure/energy regression tests. */
    public boolean captureStrike() {
        if (level == null || level.isClientSide || energy >= PACKET || !structureOk() || rodLength == 0
                || worldPosition.getY() + rodLength < 100) return false;
        var tip = worldPosition.above(4 + rodLength);
        if (!level.canSeeSky(tip.above())) return false;
        energy = ENERGY_CAPACITY;
        setChanged();
        var lightning = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
        if (lightning != null) {
            lightning.moveTo(tip.getX() + .5, tip.getY(), tip.getZ() + .5);
            lightning.setVisualOnly(true);
            level.addFreshEntity(lightning);
        }
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LightningRodControllerBlockEntity machine) {
        if (!machine.structureOk()) {
            if (machine.energy != 0) {
                machine.energy = 0;
                machine.setChanged();
            }
            return;
        }
        if (machine.energy >= PACKET) {
            long sent = 0;
            var below = pos.below();
            IEnergyBlock target = level.hasChunkAt(below) ? EnergyBlockViews.view(level.getBlockEntity(below)) : null;
            if (target != null) sent = target.doEnergyInjection(GregTechTags.Energy.EU, Direction.UP, PACKET, Math.min(16, machine.energy / PACKET),
                    true);
            machine.energy -= com.gregtech.gregtech.content.multiblock.AdvancedControllerRules.lightningDrain(machine.energy, sent);
            machine.setChanged();
            return;
        }
        if (machine.rodLength > 0 && level.random.nextInt(1000000)<Math.min(100, machine.rodLength)
                && (level.isThundering() || level.isRaining() && level.random.nextInt(10) == 0)) {
            int neighbors = 1;
            for (var rod : LOADED)
                if (rod != machine && !rod.isRemoved() && rod.level == level && rod.rodLength > 0
                        && Math.abs(rod.worldPosition.getX() - pos.getX()) < 256
                        && Math.abs(rod.worldPosition.getZ() - pos.getZ()) < 256)
                    neighbors++;
            if (level.random.nextInt(neighbors) == 0) machine.captureStrike();
        }
    }

    @Override
    public boolean isEnergyType(GregTechTags.Tag type, Direction side, boolean emitting) {
        return emitting && type == GregTechTags.Energy.EU;
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(Direction side) {
        return List.of(GregTechTags.Energy.EU);
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, Direction side, boolean theoretical) {
        return false;
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag type, Direction side, boolean theoretical) {
        return type == GregTechTags.Energy.EU && (side == null || side == Direction.DOWN) && (theoretical || isStructureOk());
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag type, Direction side) {
        return 0;
    }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag type, Direction side) {
        return type == GregTechTags.Energy.EU ? PACKET : 0;
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag type, Direction side, long size) {
        return 0;
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag type, Direction side, long size) {
        return size == PACKET && isEnergyEmittingTo(type, side, false) ? Math.min(16, energy / size) : 0;
    }

    @Override
    public long doExtract(GregTechTags.Tag type, Direction side, long size, long amount, boolean execute) {
        long packets = Math.max(0, Math.min(amount, getEnergyOffered(type, side, size)));
        if (execute && packets > 0) {
            energy -= packets * size;
            setChanged();
        }
        return packets;
    }

    @Override
    public long getEnergyStored(GregTechTags.Tag type, Direction side) {
        return type == GregTechTags.Energy.EU ? energy : 0;
    }

    @Override
    public long getEnergyCapacity(GregTechTags.Tag type, Direction side) {
        return type == GregTechTags.Energy.EU ? ENERGY_CAPACITY : 0;
    }
//? if >=1.21.1 {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putLong("gt.energy", energy);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        energy = Math.max(0, Math.min(ENERGY_CAPACITY, tag.getLong("gt.energy")));
    }

    public void restorePersistentState(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        loadAdditional(tag, lookup);
    }
*///?} else {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("gt.energy", energy);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy = Math.max(0, Math.min(ENERGY_CAPACITY, tag.getLong("gt.energy")));
    }
//?}
}
