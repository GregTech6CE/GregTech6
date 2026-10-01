package com.gregtech.gregtech.blockentity.energy;

import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.api.machine.PumpSpec;
import com.gregtech.gregtech.block.energy.PumpBlock;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

/** GT6 rotational pump — accepts RU, drains fluid blocks, outputs to adjacent pipes. */
public class PumpBlockEntity extends GTEnergyBlockEntity {
    private PumpSpec spec;
    private long energyBuffer;
    private int scanCooldown;
    private int pumpX, pumpZ;
    private final FluidTankGT tank;
    private LazyOptional<IFluidHandler> fluidCap;

    private static final int SCAN_AREA = 128;
    private static final int ENERGY_TO_START_DRAIN = 8192;
    private static final int ENERGY_PER_DRAIN = 2048;

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.PUMP.get(), pos, state);
        this.tank = new FluidTankGT(16000).setOnChanged(this::setChanged);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getBlockState().getBlock() instanceof PumpBlock pb) {
            this.spec = pb.spec();
        }
        if (level != null && !level.isClientSide) {
            // Register for ticking via the block entity ticker
        }
    }

    private PumpSpec spec() {
        if (spec == null && getBlockState().getBlock() instanceof PumpBlock pb) {
            spec = pb.spec();
        }
        return spec != null ? spec : new PumpSpec("bronze", null, 32, 1);
    }

    public void onRemoved() {}

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyBuffer = tag.getLong("energyBuffer");
        scanCooldown = tag.getInt("scanCooldown");
        pumpX = tag.getInt("pumpX");
        pumpZ = tag.getInt("pumpZ");
        tank.readFromNBT(tag.getCompound("tank"));
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("energyBuffer", energyBuffer);
        tag.putInt("scanCooldown", scanCooldown);
        tag.putInt("pumpX", pumpX);
        tag.putInt("pumpZ", pumpZ);
        CompoundTag tankTag = new CompoundTag();
        tank.writeToNBT(tankTag);
        tag.put("tank", tankTag);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (fluidCap == null) fluidCap = LazyOptional.of(() -> tank);
            return fluidCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (fluidCap != null) {
            fluidCap.invalidate();
            fluidCap = null;
        }
    }

    // --- IEnergyBlock ---

    @Override
    public boolean isEnergyType(GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        return energyType == GregTechTags.Energy.RU;
    }

    @Override
    public java.util.Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return GregTechTags.Energy.RU.asList();
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        if (energyType != GregTechTags.Energy.RU || side == null) return false;
        Direction facing = getBlockState().getValue(PumpBlock.FACING);
        return side == facing.getOpposite();
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return false;
    }

    @Override
    public long doEnergyInjection(GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doInject) {
        if (side == null || energyType != GregTechTags.Energy.RU || amount <= 0 || size == Long.MIN_VALUE
                || !isEnergyAcceptingFrom(energyType, side, false)) return 0;
        long packet = Math.abs(size);
        if (packet < getEnergySizeInputMin(energyType, side)
                || packet > getEnergySizeInputMax(energyType, side)
                || energyBuffer >= ENERGY_TO_START_DRAIN) return 0;
        long room = ENERGY_TO_START_DRAIN - energyBuffer;
        long absorbed = Math.min(amount, room / packet);
        if (doInject && absorbed > 0) {
            energyBuffer += packet * absorbed;
            setChanged();
        }
        return absorbed;
    }

    @Override
    public long doEnergyExtraction(GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doExtract) {
        return 0;
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        if (energyType != GregTechTags.Energy.RU || size == Long.MIN_VALUE) return 0;
        long packet = Math.abs(size);
        if (packet < getEnergySizeInputMin(energyType, side)
                || packet > getEnergySizeInputMax(energyType, side)) return 0;
        long room = ENERGY_TO_START_DRAIN - energyBuffer;
        return room <= 0 ? 0 : room / packet;
    }

    @Override public long getEnergyOffered(GregTechTags.Tag energyType, @Nullable Direction side, long size) { return 0; }
    @Override public long getEnergySizeInputMin(GregTechTags.Tag energyType, @Nullable Direction side) { return 8; }
    @Override public long getEnergySizeOutputMin(GregTechTags.Tag energyType, @Nullable Direction side) { return 0; }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) { return spec().inputSpeed(); }
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) { return 0; }
    @Override public long getEnergySizeInputMax(GregTechTags.Tag energyType, @Nullable Direction side) { return spec().inputSpeed(); }
    @Override public long getEnergySizeOutputMax(GregTechTags.Tag energyType, @Nullable Direction side) { return 0; }

    // --- Tick logic ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, PumpBlockEntity pump) {
        if (level.isClientSide) return;
        // GT6 exports existing contents before checking its RU buffer. Fluid left inside
        // prevents another world source from being removed until an output accepts it.
        pump.pushFluidToNeighbors();
        if (pump.energyBuffer < ENERGY_TO_START_DRAIN || !pump.tank.isEmpty()) return;
        if (pump.scanCooldown > 0) { pump.scanCooldown--; return; }

        // Scan for fluid blocks in front of the pump
        Direction facing = state.getValue(PumpBlock.FACING);
        BlockPos basePos = pos.relative(facing);

        BlockPos checkPos = basePos.offset(pump.pumpX - SCAN_AREA / 2, 0, pump.pumpZ - SCAN_AREA / 2);
        // Scan downward from current X,Z
        for (int dy = 64; dy >= 0 && pump.energyBuffer >= ENERGY_PER_DRAIN; dy--) {
            BlockPos fluidPos = checkPos.atY(checkPos.getY() - dy);
            if (!level.hasChunkAt(fluidPos)) continue;

            BlockState checkState = level.getBlockState(fluidPos);
            FluidState fluidState = checkState.getFluidState();

            if (!fluidState.isEmpty() && fluidState.isSource()) {
                net.minecraft.world.level.material.Fluid fluid = fluidState.getType();
                var bucket = new net.minecraftforge.fluids.FluidStack(fluid, 1000);
                // A world source is indivisible: never remove it for a partial fill,
                // and do not mint a bucket if the block cannot be removed.
                if (pump.tank.fill(bucket, IFluidHandler.FluidAction.SIMULATE) == 1000
                        && level.setBlockAndUpdate(fluidPos, Blocks.AIR.defaultBlockState())) {
                    int accepted = pump.tank.fill(bucket, IFluidHandler.FluidAction.EXECUTE);
                    if (accepted != 1000) {
                        if (accepted > 0) pump.tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                        level.setBlockAndUpdate(fluidPos, checkState);
                        break;
                    }
                    pump.energyBuffer -= ENERGY_PER_DRAIN;
                    pump.setChanged();
                }
                break; // one drain per tick max
            }
        }

        // Move scan position
        pump.pumpZ++;
        if (pump.pumpZ >= SCAN_AREA) {
            pump.pumpZ = 0;
            pump.pumpX++;
            if (pump.pumpX >= SCAN_AREA) {
                pump.pumpX = 0;
            }
        }
        pump.scanCooldown = 2;
    }

    private void pushFluidToNeighbors() {
        if (level == null || tank.isEmpty()) return;
        Direction facing = getBlockState().getValue(PumpBlock.FACING);
        for (Direction side : Direction.values()) {
            if (side == facing || side == facing.getOpposite() || tank.isEmpty()) continue;
            BlockEntity be = level.getBlockEntity(worldPosition.relative(side));
            if (be == null) continue;
            LazyOptional<IFluidHandler> cap = be.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite());
            cap.ifPresent(handler -> {
                var offer = tank.drain(1000, IFluidHandler.FluidAction.SIMULATE);
                if (offer.isEmpty()) return;
                int capacity = handler.fill(offer, IFluidHandler.FluidAction.SIMULATE);
                if (capacity <= 0) return;
                offer.setAmount(Math.min(offer.getAmount(), capacity));
                int received = handler.fill(offer, IFluidHandler.FluidAction.EXECUTE);
                if (received > 0) tank.drain(Math.min(received, offer.getAmount()), IFluidHandler.FluidAction.EXECUTE);
            });
        }
    }
}
