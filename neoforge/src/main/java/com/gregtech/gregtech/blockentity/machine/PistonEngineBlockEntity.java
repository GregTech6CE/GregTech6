package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.machine.PistonEngineState;
import com.gregtech.gregtech.block.machine.EngineBlock;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** GT6 electric/flux piston engine: back input, front signed output, 32 power settings. */
public abstract class PistonEngineBlockEntity extends EngineBaseBlockEntity implements com.gregtech.gregtech.api.machine.MachineControl.Provider {
    protected final PistonEngineState pistonState = new PistonEngineState();
    private boolean emitted;
    private final com.gregtech.gregtech.api.machine.MachineControl control=com.gregtech.gregtech.api.machine.MachineControls.switchable(this,
            ()->!pistonState.stopped(),value->{pistonState.setStopped(!value);syncToClient();},()->pistonState.active(),()->emitted,
            ()->pistonState.controlMode(),value->{pistonState.setControlMode(value);syncToClient();});
    @Override public com.gregtech.gregtech.api.machine.MachineControl machineControl(Direction side){return control;}
    private long engineTimer;
    private final java.util.Map<Direction, net.neoforged.neoforge.energy.IEnergyStorage> fluxPorts = new java.util.EnumMap<>(Direction.class);
    protected PistonEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Override public FaceConfig defaultFaceConfig() { return FaceConfig.builder().energyIn(FaceConfig.BACK).energyOut(FaceConfig.FRONT).build(); }
    @Override protected FaceConfig resolvedFaceConfig() { return defaultFaceConfig(); }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, Direction side, boolean theoretical) {
        return type == inputEnergyType() && side == facing().getOpposite() && (theoretical || !pistonState.stopped());
    }
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type, Direction side, boolean theoretical) {
        return type == GregTechTags.Energy.KU && side == facing();
    }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type, Direction side) { return type == inputEnergyType() ? inputRate() : 0; }
    @Override public long getEnergyDemanded(GregTechTags.Tag type, Direction side, long size) {
        if (!isEnergyAcceptingFrom(type, side, false)) return 0;
        return pistonState.accept(size, Long.MAX_VALUE, inputRate() * 2, false);
    }
    @Override public long doInject(GregTechTags.Tag type, Direction side, long size, long amount, boolean execute) {
        if (!isEnergyAcceptingFrom(type, side, false) || size == 0 || size == Long.MIN_VALUE || amount <= 0) return 0;
        if (Math.abs(size) > inputRate() * 2) {
            if (execute && level != null && !level.isClientSide) {
                if (com.gregtech.gregtech.GregTechConfig.machineOvervoltageExplosions()) {
                    level.removeBlock(worldPosition, false);
                    level.explode(null, worldPosition.getX()+.5, worldPosition.getY()+.5, worldPosition.getZ()+.5,
                            type == GregTechTags.Energy.EU ? PistonEngineState.voltageTier(size) : .1F, Level.ExplosionInteraction.BLOCK);
                }
            }
            return amount;
        }
        long accepted = pistonState.accept(size, amount, inputRate() * 2, execute);
        if (execute && accepted > 0) setChanged();
        return accepted;
    }
    // A piston emits during its own tick. Extraction must not duplicate that emission.
    @Override public long getEnergyOffered(GregTechTags.Tag type, Direction side, long size) { return 0; }
    @Override public long doExtract(GregTechTags.Tag type, Direction side, long size, long amount, boolean execute) { return 0; }
    @Override public long getEnergyStored(GregTechTags.Tag type, Direction side) { return type == inputEnergyType() ? pistonState.energy() : 0; }
    @Override public long getEnergyCapacity(GregTechTags.Tag type, Direction side) { return type == inputEnergyType() ? inputRate() * 2 : 0; }
    public net.neoforged.neoforge.energy.IEnergyStorage energyHandler(Direction side) {
        if(inputEnergyType()!=GregTechTags.Energy.RF||side==null||side!=facing().getOpposite())return null;
        return fluxPorts.computeIfAbsent(side,face->new net.neoforged.neoforge.energy.IEnergyStorage(){
            public int receiveEnergy(int amount,boolean simulate){return (int)doInject(GregTechTags.Energy.RF,face,1,amount,!simulate);}
            public int extractEnergy(int amount,boolean simulate){return 0;}
            public int getEnergyStored(){return (int)Math.min(Integer.MAX_VALUE,pistonState.energy());}
            public int getMaxEnergyStored(){return (int)Math.min(Integer.MAX_VALUE,inputRate()*2);}
            public boolean canExtract(){return false;}
            public boolean canReceive(){return isEnergyAcceptingFrom(GregTechTags.Energy.RF,face,false);}
        });
    }
    public void cyclePower() { pistonState.cycleMode(); syncToClient(); }
    public void toggleStopped() { pistonState.toggleStopped(); syncToClient(); }
    public long operatingInput() { return pistonState.input(inputRate()); }
    public long operatingOutput() { return pistonState.output(outputRate()); }
    public int coreColor() { return pistonState.coreColor(); }
    @Override public void handleUpdateTag(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.handleUpdateTag(tag,lookup);
        if (level != null && level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
    }
    public net.minecraft.network.chat.Component statusDescription() {
        return net.minecraft.network.chat.Component.translatable("message.gregtech.engine.piston.status",
                pistonState.mode() + 1, operatingInput(), inputEnergyType().getShortName(), operatingOutput(),
                net.minecraft.network.chat.Component.translatable("message.gregtech.engine.piston."
                        + (pistonState.stopped() ? "stopped" : pistonState.active() ? "running" : "idle")));
    }
    protected void tickPiston() {
        if (level == null || level.isClientSide) return;
        boolean wasActive = pistonState.active();
        long packet = pistonState.tick(++engineTimer, inputRate(), outputRate());
        emitted=packet!=0&&EnergyTransfer.emitEnergyToSide(GregTechTags.Energy.KU, facing(), packet, 1, this)>0;
        if (wasActive != pistonState.active()) {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(EngineBlock.LIT, pistonState.active()));
            syncToClient();
        }
        if (packet != 0) setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.putLong("piston.inputEnergy", pistonState.energy());
        tag.putInt("piston.mode", pistonState.mode());
        tag.putInt("piston.phase", pistonState.piston());
        tag.putBoolean("piston.active", pistonState.active());
        tag.putBoolean("piston.stopped", pistonState.stopped());
    }
    @Override protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        // Old kuEnergy actually stored unconverted input. Retain it as input once.
        pistonState.restore(tag.contains("piston.inputEnergy") ? tag.getLong("piston.inputEnergy") : kuEnergy,
                tag.contains("piston.mode") ? tag.getInt("piston.mode") : 15,
                tag.getInt("piston.phase"), tag.getBoolean("piston.active"), tag.getBoolean("piston.stopped"));
        kuEnergy = 0;
    }
}
