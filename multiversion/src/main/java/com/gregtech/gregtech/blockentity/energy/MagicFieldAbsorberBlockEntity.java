/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * MultiTileEntityMagicFieldAbsorber, adapted to the shared modern energy boundary. */
package com.gregtech.gregtech.blockentity.energy;

import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.api.machine.*;
import com.gregtech.gregtech.block.energy.MagicFieldAbsorberBlock;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.content.energy.MagicFieldAbsorberRules;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Collection;

public final class MagicFieldAbsorberBlockEntity extends GTEnergyBlockEntity implements MachineControl.Provider {
    private boolean stopped;
    private boolean check = true;
    private long timer;
    private MagicFieldAbsorberRules.Output output;
    public MagicFieldAbsorberBlockEntity(BlockPos pos, BlockState state) { super(GTBlockEntities.MAGIC_FIELD_ABSORBER.get(), pos, state); }
    public Direction outputSide() { return getBlockState().getValue(MagicFieldAbsorberBlock.FACING); }
    public boolean enabled() { return !stopped; }
    public boolean active() { return enabled() && output != null; }
    public MagicFieldAbsorberRules.Output output() { return output; }
    public void recheck() { check = true; }
    public void setEnabled(boolean enabled) { stopped = !enabled; check = true; setChanged(); }
    @Override public MachineControl machineControl(Direction side) { return MachineControls.switchable(this, this::enabled, this::setEnabled, this::active, this::active); }
    public void serverTick() {
        if (level == null || level.isClientSide) return;
        timer++;
        if (check || timer % 600 == 5) {
            if (!level.hasChunkAt(worldPosition.above())) return;
            var trophy = level.getBlockState(worldPosition.above()).getBlock();
            output = MagicFieldAbsorberRules.trophy(BuiltInRegistries.BLOCK.getKey(trophy).toString(), trophy instanceof AbstractSkullBlock);
            check = false;
        }
        boolean active = active();
        if (getBlockState().getValue(MagicFieldAbsorberBlock.ACTIVE) != active)
            level.setBlock(worldPosition, getBlockState().setValue(MagicFieldAbsorberBlock.ACTIVE, active), 3);
        if (active) EnergyTransfer.emitEnergyToSide(output.type(), outputSide(), output.packetSize(timer), output.packetCount(), this);
    }
    @Override public boolean isEnergyType(GregTechTags.Tag type, Direction side, boolean emitting) { return emitting && GregTechTags.Energy.ALL_GT.contains(type); }
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side) { return GregTechTags.Energy.ALL_GT; }
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type, Direction side, boolean theoretical) {
        return (side == null || side == outputSide()) && isEnergyType(type, side, true);
    }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, Direction side, boolean theoretical) { return false; }
    @Override public long getEnergyDemanded(GregTechTags.Tag type, Direction side, long size) { return 0; }
    @Override public long getEnergyOffered(GregTechTags.Tag type, Direction side, long size) { return 0; } // Push only; no duplicate pull generation.
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type, Direction side) { return 0; }
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type, Direction side) { return output == null ? 64 : output.amount(); }
    @Override public long getEnergySizeOutputMin(GregTechTags.Tag type, Direction side) { return getEnergySizeOutputRecommended(type, side); }
    @Override public long getEnergySizeOutputMax(GregTechTags.Tag type, Direction side) { return getEnergySizeOutputRecommended(type, side); }
//? if >=1.21.1 {
    /*@Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) { super.saveAdditional(tag, lookup); tag.putBoolean("gt.stopped", stopped); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) { super.loadAdditional(tag, lookup); stopped = tag.getBoolean("gt.stopped"); check = true; }
*///?} else {
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putBoolean("gt.stopped", stopped); }
    @Override public void load(CompoundTag tag) { super.load(tag); stopped = tag.getBoolean("gt.stopped"); check = true; }
//?}
}
