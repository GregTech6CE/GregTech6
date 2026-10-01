package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.api.machine.PistonEngineState;
import com.gregtech.gregtech.api.machine.RotationEngineSpec;
import com.gregtech.gregtech.block.machine.EngineBlock;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * GT6 bipolar RU -> KU engine. RU enters on the four faces around the shaft;
 * the two shaft ends emit equal, opposite KU packets and swap polarity every
 * sixteen ticks. Unused output is lost, as for GT6's WASTE_ENERGY engines.
 */
public class KineticRotationEngineBlockEntity extends EngineBaseBlockEntity {
    private static final String RU_BUFFER_KEY = "rotationRuBuffer";
    private static final String NEGATIVE_INPUT_KEY = "rotationNegativeInput";

    RotationEngineSpec spec;
    boolean active;
    private long ruBuffer;
    private boolean negativeInput;
    private int conversionTicks;
    // GT6's startup/idle overcharge protection is transient, not stored in NBT.
    private int overvoltagePrevention;
    private int overvoltageTicks;

    public KineticRotationEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setSpec(RotationEngineSpec spec) {
        this.spec = spec;
        this.faceConfig = defaultFaceConfig();
    }

    /** GT6's bipolar shaft has no user-editable face mask. */
    @Override
    public FaceConfig getFaceConfig() { return defaultFaceConfig(); }

    @Override
    public void setFaceConfig(FaceConfig unused) {
        this.faceConfig = defaultFaceConfig();
        setChanged();
    }

    public RotationEngineSpec spec() { return spec; }
    public boolean isActive() { return active; }
    public long storedRu() { return ruBuffer; }
    public boolean negativeInput() { return negativeInput; }
    public int overvoltagePreventionCount() { return overvoltagePrevention; }

    @Override protected long outputRate() { return spec != null ? spec.outputRate() : 0; }
    @Override protected long inputRate() { return spec != null ? spec.inputRate() : 0; }
    @Override protected GregTechTags.Tag inputEnergyType() { return GregTechTags.Energy.RU; }

    @Override
    public long getEnergySizeInputMin(GregTechTags.Tag type, @Nullable Direction side) {
        if (type != GregTechTags.Energy.RU || inputRate() <= 0) return 0;
        return inputRate() <= 16 ? 1 : inputRate() / 2;
    }

    @Override
    public FaceConfig defaultFaceConfig() {
        return FaceConfig.builder()
                .energyIn(FaceConfig.TOP, FaceConfig.BOTTOM, FaceConfig.LEFT, FaceConfig.RIGHT)
                .energyOut(FaceConfig.FRONT, FaceConfig.BACK)
                .build();
    }

    /** The physical shaft axis, including vertical placement, determines the faces. */
    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, @Nullable Direction side, boolean theoretical) {
        return type == GregTechTags.Energy.RU && (side == null || side.getAxis() != facing().getAxis());
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag type, @Nullable Direction side, boolean theoretical) {
        return type == GregTechTags.Energy.KU && (side == null || side.getAxis() == facing().getAxis());
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag type, @Nullable Direction side, long size) {
        if (!isEnergyAcceptingFrom(type, side, false) || size == 0 || size == Long.MIN_VALUE) return 0;
        long magnitude = Math.abs(size);
        if (magnitude < getEnergySizeInputMin(type, side) || magnitude > inputRate() * 2) return 0;
        return Math.max(0, inputRate() * 2 - ruBuffer) / magnitude;
    }

    @Override
    public long doInject(GregTechTags.Tag type, @Nullable Direction side, long size, long amount, boolean execute) {
        if (!isEnergyAcceptingFrom(type, side, false) || size == 0 || size == Long.MIN_VALUE || amount <= 0) return 0;
        long magnitude = Math.abs(size);
        long capacity = inputRate() * 2;
        if (magnitude < getEnergySizeInputMin(type, side)) return 0;
        if (magnitude > capacity) {
            // TE_Behavior_Energy_Stats consumes the complete packet count even
            // when a single RU packet exceeds its input maximum. A simulated
            // transfer reports the same consumption without changing the tile.
            if (execute) {
                negativeInput = size < 0;
                setChanged();
                if (overvoltagePrevention < 100) {
                    overvoltagePrevention++;
                    ruBuffer = 0;
                } else if (level != null && !level.isClientSide
                        && com.gregtech.gregtech.GregTechConfig.machineOvervoltageExplosions()) {
                    level.removeBlock(worldPosition, false);
                    level.explode(null, worldPosition.getX() + .5, worldPosition.getY() + .5,
                            worldPosition.getZ() + .5, PistonEngineState.voltageTier(size),
                            Level.ExplosionInteraction.BLOCK);
                }
            }
            return amount;
        }
        long accepted = Math.min(amount, Math.max(0, capacity - ruBuffer) / magnitude);
        if (execute && accepted > 0) {
            ruBuffer += accepted * magnitude;
            negativeInput = size < 0;
            setChanged();
        }
        return accepted;
    }

    /** This converter pushes KU in its own tick; it has no pullable KU buffer. */
    @Override
    public long getEnergyOffered(GregTechTags.Tag type, @Nullable Direction side, long size) { return 0; }

    @Override
    public long doExtract(GregTechTags.Tag type, @Nullable Direction side, long size, long amount, boolean execute) {
        return 0;
    }

    @Override
    public long getEnergyStored(GregTechTags.Tag type, @Nullable Direction side) {
        return type == GregTechTags.Energy.RU ? ruBuffer : 0;
    }

    @Override
    public long getEnergyCapacity(GregTechTags.Tag type, @Nullable Direction side) {
        return type == GregTechTags.Energy.RU ? inputRate() * 2 : 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.putLong(RU_BUFFER_KEY, ruBuffer);
        tag.putBoolean(NEGATIVE_INPUT_KEY, negativeInput);
    }

    @Override
    protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        // Older port saves could contain a generic one-output screwdriver mask.
        // The physical four-input/two-output axis is always authoritative.
        faceConfig = defaultFaceConfig();
        ruBuffer = Math.min(inputRate() * 2, Math.max(0, tag.getLong(RU_BUFFER_KEY)));
        negativeInput = tag.getBoolean(NEGATIVE_INPUT_KEY);
    }

    /** First phase: positive on the back, negative on the front. */
    public static Direction positiveOutputSide(Direction facing, long tick) {
        return (tick & 31) < 16 ? facing.getOpposite() : facing;
    }

    private void pollRuFromNeighbors() {
        if (level == null || inputRate() <= 0) return;
        for (Direction side : Direction.values()) {
            if (!isEnergyAcceptingFrom(GregTechTags.Energy.RU, side, false)) continue;
            long demand = getEnergyDemanded(GregTechTags.Energy.RU, side, inputRate());
            if (demand <= 0) break;
            if (!(level.getBlockEntity(worldPosition.relative(side)) instanceof IEnergyBlock source)) continue;
            Direction sourceSide = side.getOpposite();
            if (!source.isEnergyEmittingTo(GregTechTags.Energy.RU, sourceSide, false)) continue;
            long offered = source.getEnergyOffered(GregTechTags.Energy.RU, sourceSide, inputRate());
            if (offered <= 0) continue;
            long extracted = source.doEnergyExtraction(GregTechTags.Energy.RU, sourceSide,
                    inputRate(), Math.min(offered, demand), true);
            if (extracted > 0) doInject(GregTechTags.Energy.RU, side, inputRate(), extracted, true);
        }
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, KineticRotationEngineBlockEntity be) {
        if (be.spec == null) return;
        be.pollRuFromNeighbors();

        // GT6 doBipolar calculates a size for *each* end from the input buffer.
        // With 32 RU and a 16 KU rating, each end gets one 16 KU packet.
        long size = be.inputRate() <= 0 ? 0 : be.ruBuffer * be.outputRate() / be.inputRate();
        long maxSize = be.outputRate() * 2;
        boolean canRun = size >= Math.max(1, be.outputRate() / 2);
        if (canRun && size <= maxSize) {
            Direction positive = positiveOutputSide(be.facing(), be.conversionTicks);
            EnergyTransfer.emitEnergyToSide(GregTechTags.Energy.KU, positive, size, 1, be);
            EnergyTransfer.emitEnergyToSide(GregTechTags.Energy.KU, positive.getOpposite(), -size, 1, be);
        }

        // NBT_WASTE_ENERGY=T: even disconnected or undersupplied engines burn
        // their input each tick. The maximum GT6 waste is twice the input rate.
        if (be.ruBuffer != 0) {
            be.ruBuffer = Math.max(0, be.ruBuffer - be.inputRate() * 2);
            be.setChanged();
        }
        be.conversionTicks = (be.conversionTicks + 1) & 31;

        // GT6 reduces one startup-protection incident every 600 idle ticks.
        // The counter is per placed tile and intentionally resets on reload.
        be.overvoltageTicks = (be.overvoltageTicks + 1) % 600;
        if (be.overvoltageTicks == 5 && !canRun && be.overvoltagePrevention > 0) {
            be.overvoltagePrevention--;
        }

        if (be.active != canRun) {
            be.active = canRun;
            level.setBlock(pos, be.getBlockState().setValue(EngineBlock.LIT, canRun), 3);
        }
    }
}
