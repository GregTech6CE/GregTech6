package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Shared base for KU generator engines. Uses GT6 packet-count energy model. */
public abstract class EngineBaseBlockEntity extends GTEnergyBlockEntity {
    protected long kuEnergy;
    protected FaceConfig faceConfig;

    protected EngineBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ── Abstract ───────────────────────────────────────────────────────────

    protected abstract long outputRate();
    protected abstract long inputRate();
    protected abstract GregTechTags.Tag inputEnergyType();
    public abstract FaceConfig defaultFaceConfig();

    /** Whether the engine is ready to output KU to the energy network.
     *  Steam engines override this to gate on 20 % preheat threshold. */
    protected boolean isReadyToOutput() { return true; }

    /** Maximum KU this engine offers per tick via {@link #getEnergyOffered}.
     *  Steam engines override this to {@code computeOutput()} to match
     *  GT6 variable-output mechanics and prevent buffer dumping. */
    protected long maxKuOutputPerTick() { return Long.MAX_VALUE; }

    // ── FaceConfig ─────────────────────────────────────────────────────────

    protected FaceConfig resolvedFaceConfig() {
        return faceConfig != null ? faceConfig : defaultFaceConfig();
    }

    public FaceConfig getFaceConfig() { return resolvedFaceConfig(); }

    public void setFaceConfig(FaceConfig fc) { this.faceConfig = fc; setChanged(); }

    // ── IEnergyBlock — type checks ────────────────────────────────────────

    @Override
    public boolean isEnergyType(GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        if (emitting) return energyType == GregTechTags.Energy.KU;
        return energyType == inputEnergyType();
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        List<GregTechTags.Tag> types = new ArrayList<>(2);
        types.add(GregTechTags.Energy.KU);
        types.add(inputEnergyType());
        return types;
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        if (energyType == GregTechTags.Energy.KU) return false;
        if (energyType != inputEnergyType()) return false;
        if (side == null) return true;
        return FaceConfig.has(resolvedFaceConfig().energyInputs(), relativeDir(side));
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        if (energyType != GregTechTags.Energy.KU) return false;
        if (side == null) return true;
        int rel = relativeDir(side);
        int outputs = resolvedFaceConfig().energyOutputs();
        if (outputs != 0) return FaceConfig.has(outputs, rel);
        return !FaceConfig.has(resolvedFaceConfig().energyInputs(), rel);
    }

    // ── IEnergyBlock — size queries ───────────────────────────────────────

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (energyType != inputEnergyType()) return 0;
        return GregTechTags.Energy.isSizeIrrelevant(energyType) ? 1 : inputRate();
    }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (energyType != GregTechTags.Energy.KU) return 0;
        return outputRate();
    }

    // ── IEnergyBlock — offered/demanded (returns packet count) ────────────

    @Override
    public long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        if (energyType != inputEnergyType() || size <= 0) return 0;
        long capacity = outputRate() * 2;
        long free = Math.max(0, capacity - kuEnergy);
        return free / size;
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        if (energyType == inputEnergyType()) return 0;
        if (energyType == GregTechTags.Energy.KU && size > 0 && isReadyToOutput()) {
            return Math.min(kuEnergy, maxKuOutputPerTick()) / size;
        }
        return 0;
    }

    // ── IEnergyBlock — injection/extraction hooks (packet-count model) ────

    @Override
    public long doInject(GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doInject) {
        if (energyType != inputEnergyType() || amount <= 0) return 0;
        long total = amount * size;
        long capacity = outputRate() * 2;
        long space = Math.max(0, capacity - kuEnergy);
        long toStore = Math.min(total, space);
        long accepted = toStore / size;
        if (doInject && accepted > 0) {
            kuEnergy += accepted * size;
            setChanged();
        }
        return accepted;
    }

    @Override
    public long doExtract(GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doExtract) {
        if (energyType != GregTechTags.Energy.KU || amount <= 0 || size <= 0 || !isReadyToOutput()) return 0;
        long total = amount * size;
        long toTake = Math.min(kuEnergy, total);
        long extracted = toTake / size;
        if (doExtract && extracted > 0) {
            kuEnergy -= extracted * size;
            setChanged();
        }
        return extracted;
    }

    @Override
    public long getEnergyStored(GregTechTags.Tag energyType, @Nullable Direction side) {
        return energyType == GregTechTags.Energy.KU ? kuEnergy : 0;
    }

    @Override
    public long getEnergyCapacity(GregTechTags.Tag energyType, @Nullable Direction side) {
        return energyType == GregTechTags.Energy.KU ? outputRate() * 2 : 0;
    }

    /** Internal KU buffer for heat color / status display. */
    public long getKuEnergy() { return kuEnergy; }

    // ── NBT ────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("kuEnergy", kuEnergy);
        if (faceConfig != null) {
            tag.putInt("fc_energyIn", faceConfig.energyInputs());
            tag.putInt("fc_energyOut", faceConfig.energyOutputs());
            tag.putInt("fc_fluidIn", faceConfig.fluidInputs());
            tag.putInt("fc_fluidOut", faceConfig.fluidOutputs());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        kuEnergy = tag.getLong("kuEnergy");
        if (tag.contains("fc_energyIn")) {
            faceConfig = new FaceConfig(
                    0, 0,
                    tag.getInt("fc_fluidIn"), tag.getInt("fc_fluidOut"),
                    tag.getInt("fc_energyIn"), tag.getInt("fc_energyOut"),
                    -1, -1, -1, -1);
        }
    }

    // ── Tick helpers ───────────────────────────────────────────────────────

    /** Poll input energy from adjacent blocks (same pattern as BasicMachineBlockEntity.pollEnergy). */
    protected void pollInputEnergy(long size) {
        FaceConfig fc = resolvedFaceConfig();
        if (fc.energyInputs() == 0 || level == null) return;
        long capacity = outputRate() * 2;
        if (kuEnergy >= capacity) return;

        for (Direction dir : Direction.values()) {
            if (!FaceConfig.has(fc.energyInputs(), relativeDir(dir))) continue;
            if (kuEnergy >= capacity) break;

            var be = level.getBlockEntity(worldPosition.relative(dir));
            if (!(be instanceof com.gregtech.gregtech.api.energy.IEnergyBlock source)) continue;
            Direction srcSide = dir.getOpposite();
            if (!source.isEnergyEmittingTo(inputEnergyType(), srcSide, false)) continue;

            long offered = source.getEnergyOffered(inputEnergyType(), srcSide, size);
            if (offered <= 0) continue;

            long extracted = source.doEnergyExtraction(inputEnergyType(), srcSide, size, offered, true);
            if (extracted > 0) {
                kuEnergy += extracted * size;
                if (kuEnergy > capacity) kuEnergy = capacity;
                setChanged();
            }
        }
    }

    /** Emit KU from buffer to adjacent receivers. */
    protected void emitKu() {
        if (kuEnergy <= 0 || level == null) return;
        long size = outputRate();
        if (size <= 0) return;
        long packets = kuEnergy / size;
        if (packets <= 0) return;
        long emitted = EnergyTransfer.emitEnergyToNetwork(
                GregTechTags.Energy.KU, size, packets, this);
        if (emitted > 0) {
            kuEnergy -= emitted * size;
            setChanged();
        }
    }

    protected Direction facing() {
        BlockState st = getBlockState();
        if (st.hasProperty(net.minecraft.world.level.block.DirectionalBlock.FACING)) {
            return st.getValue(net.minecraft.world.level.block.DirectionalBlock.FACING);
        }
        return Direction.NORTH;
    }

    // ── Direction transformation (6-way DirectionalBlock) ──────────────────

    /** Convert a world-side {@link Direction} to a machine-relative face index
     *  ({@link FaceConfig#BOTTOM}..{@link FaceConfig#BACK}), accounting for the block's facing. */
    protected int relativeDir(Direction side) {
        return rotateAbsoluteToRelative(facing(), side);
    }

    /** Convert a machine-relative face index back to a world {@link Direction}. */
    protected Direction relativeToAbsolute(int relDir) {
        if (relDir == FaceConfig.BOTTOM) return Direction.DOWN;
        if (relDir == FaceConfig.TOP)    return Direction.UP;
        Direction f = facing();
        return switch (f) {
            case NORTH -> switch (relDir) {
                case FaceConfig.LEFT  -> Direction.WEST;
                case FaceConfig.RIGHT -> Direction.EAST;
                case FaceConfig.FRONT -> Direction.NORTH;
                case FaceConfig.BACK  -> Direction.SOUTH;
                default -> Direction.NORTH;
            };
            case SOUTH -> switch (relDir) {
                case FaceConfig.LEFT  -> Direction.EAST;
                case FaceConfig.RIGHT -> Direction.WEST;
                case FaceConfig.FRONT -> Direction.SOUTH;
                case FaceConfig.BACK  -> Direction.NORTH;
                default -> Direction.SOUTH;
            };
            case WEST -> switch (relDir) {
                case FaceConfig.LEFT  -> Direction.SOUTH;
                case FaceConfig.RIGHT -> Direction.NORTH;
                case FaceConfig.FRONT -> Direction.WEST;
                case FaceConfig.BACK  -> Direction.EAST;
                default -> Direction.WEST;
            };
            case EAST -> switch (relDir) {
                case FaceConfig.LEFT  -> Direction.NORTH;
                case FaceConfig.RIGHT -> Direction.SOUTH;
                case FaceConfig.FRONT -> Direction.EAST;
                case FaceConfig.BACK  -> Direction.WEST;
                default -> Direction.EAST;
            };
            case UP -> switch (relDir) {
                case FaceConfig.LEFT  -> Direction.WEST;
                case FaceConfig.RIGHT -> Direction.EAST;
                case FaceConfig.FRONT -> Direction.UP;
                case FaceConfig.BACK  -> Direction.DOWN;
                default -> Direction.UP;
            };
            case DOWN -> switch (relDir) {
                case FaceConfig.LEFT  -> Direction.WEST;
                case FaceConfig.RIGHT -> Direction.EAST;
                case FaceConfig.FRONT -> Direction.DOWN;
                case FaceConfig.BACK  -> Direction.UP;
                default -> Direction.DOWN;
            };
        };
    }

    /** Transform an absolute world {@link Direction} to a machine-relative face index,
     *  given the block's facing (the direction the machine front points toward).
     *  Extended for 6-way {@link net.minecraft.world.level.block.DirectionalBlock}. */
    static int rotateAbsoluteToRelative(Direction facing, Direction side) {
        if (side == Direction.DOWN) {
            return switch (facing) {
                case DOWN -> FaceConfig.FRONT;
                case UP   -> FaceConfig.BACK;
                default   -> FaceConfig.BOTTOM;
            };
        }
        if (side == Direction.UP) {
            return switch (facing) {
                case UP   -> FaceConfig.FRONT;
                case DOWN -> FaceConfig.BACK;
                default   -> FaceConfig.TOP;
            };
        }
        return switch (facing) {
            case NORTH -> switch (side) {
                case NORTH -> FaceConfig.FRONT; case SOUTH -> FaceConfig.BACK;
                case WEST  -> FaceConfig.RIGHT; case EAST  -> FaceConfig.LEFT;
                default -> side.get3DDataValue();
            };
            case SOUTH -> switch (side) {
                case SOUTH -> FaceConfig.FRONT; case NORTH -> FaceConfig.BACK;
                case EAST  -> FaceConfig.RIGHT; case WEST  -> FaceConfig.LEFT;
                default -> side.get3DDataValue();
            };
            case WEST -> switch (side) {
                case WEST  -> FaceConfig.FRONT; case EAST  -> FaceConfig.BACK;
                case SOUTH -> FaceConfig.RIGHT; case NORTH -> FaceConfig.LEFT;
                default -> side.get3DDataValue();
            };
            case EAST -> switch (side) {
                case EAST  -> FaceConfig.FRONT; case WEST  -> FaceConfig.BACK;
                case NORTH -> FaceConfig.RIGHT; case SOUTH -> FaceConfig.LEFT;
                default -> side.get3DDataValue();
            };
            case UP -> switch (side) {
                case UP    -> FaceConfig.FRONT; case DOWN  -> FaceConfig.BACK;
                case WEST  -> FaceConfig.LEFT;  case EAST  -> FaceConfig.RIGHT;
                case NORTH -> FaceConfig.TOP;   case SOUTH -> FaceConfig.BOTTOM;
                default -> side.get3DDataValue();
            };
            case DOWN -> switch (side) {
                case DOWN  -> FaceConfig.FRONT; case UP    -> FaceConfig.BACK;
                case WEST  -> FaceConfig.LEFT;  case EAST  -> FaceConfig.RIGHT;
                case NORTH -> FaceConfig.TOP;   case SOUTH -> FaceConfig.BOTTOM;
                default -> side.get3DDataValue();
            };
        };
    }

}
