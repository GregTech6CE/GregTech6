package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTMultiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Cryogenic Distillation Tower multiblock controller. 3x3x5 tall tower
 * (3 wide × 5 high × 3 deep). Accepts HU, processes fluids at low
 * temperature for high-purity separation (GT6 F6.4).
 */
public class CryoDistillationControllerBlockEntity extends GTEnergyBlockEntity implements com.gregtech.gregtech.api.multiblock.StructureController {

    static final long HEAT_CAPACITY = 256_000;
    static final long HU_PER_OP = 1024;

    long heat;
    long progress;
    long maxProgress;
    final FluidTankGT inputTank = new FluidTankGT(64_000).setOnChanged(this::setChanged);
    final FluidTankGT[] outputTanks = new FluidTankGT[]{
            new FluidTankGT(32_000).setOnChanged(this::setChanged),
            new FluidTankGT(32_000).setOnChanged(this::setChanged),
            new FluidTankGT(32_000).setOnChanged(this::setChanged),
    };

    long lastCheckTime;
    boolean structureOk;

    @Nullable LazyOptional<IFluidHandler> fluidCap;

    public CryoDistillationControllerBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.CRYO_DISTILLATION.get(), pos, state);
    }

    public boolean isStructureOk() {
        if (level == null) return false;
        long now = level.getGameTime();
        if (lastCheckTime != 0 && now - lastCheckTime < 40) return structureOk;
        lastCheckTime = now;
        structureOk = validate();
        return structureOk;
    }

    private boolean validate() {
        return com.gregtech.gregtech.content.multiblock.ControllerStructureLayouts.matches(this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   CryoDistillationControllerBlockEntity be) {
        if (!be.isStructureOk()) return;
        if (level.getGameTime() % 20 == 0) {
            be.tickProcessing(level);
        }
        be.pushOutputs(level);
    }

    void tickProcessing(Level level) {
        if (heat < HU_PER_OP || inputTank.isEmpty()) return;
        // Simple distillation: 1 mB input → split across 3 outputs
        FluidStack input = inputTank.getFluid();
        if (input.isEmpty() || input.getAmount() < 100) return;
        int space = 0;
        for (FluidTankGT t : outputTanks) space += (int)(t.getCapacity() - t.getAmount());
        if (space < 30) return;

        heat -= HU_PER_OP;
        int amt = Math.min(100, input.getAmount());
        inputTank.drain(amt, IFluidHandler.FluidAction.EXECUTE);
        FluidStack distilled = input.copy();
        distilled.setAmount(amt / 3);
        for (FluidTankGT t : outputTanks) {
            if (!distilled.isEmpty()) {
                t.fill(distilled.copy(), IFluidHandler.FluidAction.EXECUTE);
            }
        }
        setChanged();
    }

    void pushOutputs(Level level) {
        for (Direction dir : Direction.values()) {
            var be = level.getBlockEntity(worldPosition.relative(dir));
            if (be == null) continue;
            IFluidHandler target = be.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite())
                    .resolve().orElse(null);
            if (target == null) continue;
            for (FluidTankGT t : outputTanks) {
                if (t.isEmpty()) continue;
                FluidStack offer = t.drain(1000, IFluidHandler.FluidAction.SIMULATE);
                if (offer.isEmpty()) continue;
                int filled = target.fill(offer, IFluidHandler.FluidAction.SIMULATE);
                if (filled <= 0) continue;
                FluidStack moved = t.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                if (!moved.isEmpty()) target.fill(moved, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    // ── Energy ───────────────────────────────────────────────────────────────

    @Override public boolean isEnergyType(GregTechTags.Tag e, @Nullable Direction side, boolean emitting) {
        return !emitting && e == GregTechTags.Energy.HU;
    }
    @Override public java.util.Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) { return List.of(GregTechTags.Energy.HU); }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag e, @Nullable Direction side, boolean th) { return e == GregTechTags.Energy.HU; }
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag e, @Nullable Direction side, boolean th) { return false; }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag e, @Nullable Direction s) { return e == GregTechTags.Energy.HU ? 1024 : 0; }
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag e, @Nullable Direction s) { return 0; }
    @Override public long getEnergyOffered(GregTechTags.Tag e, @Nullable Direction s, long size) { return 0; }
    @Override public long getEnergyDemanded(GregTechTags.Tag e, @Nullable Direction s, long size) {
        if (e != GregTechTags.Energy.HU || size <= 0) return 0;
        return Math.max(0, HEAT_CAPACITY - heat) / size;
    }
    @Override public long doInject(GregTechTags.Tag e, @Nullable Direction s, long size, long amount, boolean doInject) {
        if (e != GregTechTags.Energy.HU || amount <= 0 || size <= 0) return 0;
        long space = Math.max(0, HEAT_CAPACITY - heat);
        long accepted = Math.min(amount * size, space) / size;
        if (doInject && accepted > 0) { heat += accepted * size; setChanged(); }
        return accepted;
    }
    @Override public long getEnergyStored(GregTechTags.Tag e, @Nullable Direction s) { return e == GregTechTags.Energy.HU ? heat : 0; }
    @Override public long getEnergyCapacity(GregTechTags.Tag e, @Nullable Direction s) { return e == GregTechTags.Energy.HU ? HEAT_CAPACITY : 0; }

    // ── Capabilities ─────────────────────────────────────────────────────────

    @Override
    public <T> @org.jetbrains.annotations.NotNull LazyOptional<T> getCapability(
            @org.jetbrains.annotations.NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (fluidCap == null || !fluidCap.isPresent()) fluidCap = LazyOptional.of(() -> new DistillationFluidHandler());
            return fluidCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        if (fluidCap != null) { fluidCap.invalidate(); fluidCap = null; }
    }

    private class DistillationFluidHandler implements IFluidHandler {
        @Override public int getTanks() { return 1 + outputTanks.length; }
        @Override public @org.jetbrains.annotations.NotNull FluidStack getFluidInTank(int tank) {
            if (tank == 0) return inputTank.getFluid();
            int o = tank - 1;
            return o < outputTanks.length ? outputTanks[o].getFluid() : FluidStack.EMPTY;
        }
        @Override public int getTankCapacity(int tank) {
            if (tank == 0) return (int) inputTank.getCapacity();
            int o = tank - 1;
            return o < outputTanks.length ? (int) outputTanks[o].getCapacity() : 0;
        }
        @Override public boolean isFluidValid(int tank, @org.jetbrains.annotations.NotNull FluidStack stack) { return tank == 0; }
        @Override public int fill(FluidStack r, FluidAction a) { return inputTank.fill(r, a); }
        @Override public @org.jetbrains.annotations.NotNull FluidStack drain(FluidStack r, FluidAction a) { return FluidStack.EMPTY; }
        @Override public @org.jetbrains.annotations.NotNull FluidStack drain(int max, FluidAction a) {
            for (FluidTankGT t : outputTanks) {
                FluidStack d = t.drain(max, a);
                if (!d.isEmpty()) return d;
            }
            return FluidStack.EMPTY;
        }
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("gt.heat", heat);
        CompoundTag fi = new CompoundTag(); inputTank.writeToNBT(fi); tag.put("gt.input", fi);
        for (int i = 0; i < outputTanks.length; i++) {
            CompoundTag t = new CompoundTag(); outputTanks[i].writeToNBT(t); tag.put("gt.output" + i, t);
        }
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        heat = tag.getLong("gt.heat");
        if (tag.contains("gt.input")) inputTank.readFromNBT(tag.getCompound("gt.input"));
        for (int i = 0; i < outputTanks.length; i++) {
            if (tag.contains("gt.output" + i)) outputTanks[i].readFromNBT(tag.getCompound("gt.output" + i));
        }
    }
}
