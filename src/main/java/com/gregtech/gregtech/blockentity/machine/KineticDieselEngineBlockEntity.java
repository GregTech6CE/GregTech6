package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.api.machine.DieselEngineSpec;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.data.FuelRecipeMaps;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

/** Liquid-fuel → KU diesel engine. Consumes FM.Engine fuels, outputs KU, emits exhaust. */
public class KineticDieselEngineBlockEntity extends EngineBaseBlockEntity implements com.gregtech.gregtech.api.machine.MachineControl.Provider {
    protected boolean emitted;
    private final com.gregtech.gregtech.api.machine.MachineControl control=com.gregtech.gregtech.api.machine.MachineControls.switchable(this,
            ()->!isStopped(),value->setStopped(!value),this::isActive,()->emitted);
    @Override public com.gregtech.gregtech.api.machine.MachineControl machineControl(Direction side){return control;}
    DieselEngineSpec spec;
    boolean active = true; // true = running unless stopped
    boolean stopped;
    FluidTankGT fuelTank = new FluidTankGT(4000).setOnChanged(this::setChanged);
    FluidTankGT exhaustTank = new FluidTankGT(4000).setOnChanged(this::setChanged);

    @Nullable private LazyOptional<IFluidHandler> fluidCap;

    public KineticDieselEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setSpec(DieselEngineSpec spec) {
        this.spec = spec;
        this.faceConfig = defaultFaceConfig();
        long cap = spec.outputRate() * 10;
        fuelTank.setCapacity(cap);
        exhaustTank.setCapacity(cap);
    }

    public DieselEngineSpec spec() { return spec; }
    public boolean isActive() { return active; }

    @Override protected long outputRate() { return spec != null ? spec.outputRate() : 0; }
    @Override protected long inputRate() { return spec != null ? spec.inputRate() : 0; }
    @Override protected GregTechTags.Tag inputEnergyType() { return GregTechTags.Energy.KU; } // doesn't accept external energy

    @Override
    public FaceConfig defaultFaceConfig() {
        return FaceConfig.builder()
                .fluidIn(FaceConfig.LEFT, FaceConfig.RIGHT, FaceConfig.BACK, FaceConfig.TOP)
                .fluidOut(FaceConfig.BOTTOM)
                .energyOut(FaceConfig.FRONT)
                .build();
    }

    public FluidTankGT fuelTank() { return fuelTank; }
    public FluidTankGT exhaustTank() { return exhaustTank; }
    public boolean isStopped() { return stopped; }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
        setChanged();
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    public static <T extends KineticDieselEngineBlockEntity> void serverTick(
            Level level, BlockPos pos, BlockState state, T be) {
        if (be.spec == null) return;
        be.burnFuel();
        be.dischargeExhaust();
        long beforeEmission=be.kuEnergy;be.emitKu();be.emitted=be.kuEnergy<beforeEmission;

        boolean wasActive = be.active;
        be.active = be.kuEnergy > 0;
        if (be.active != wasActive) {
            level.setBlock(pos, state.setValue(com.gregtech.gregtech.block.machine.EngineBlock.LIT, be.active), 3);
        }
    }

    void burnFuel() {
        if (stopped || level == null || fuelTank.isEmpty()) return;
        long limit = outputRate() * 2;
        if (kuEnergy >= limit) return;

        FluidStack tankFluid = fuelTank.getFluid();
        if (tankFluid.isEmpty()) return;
        var recipe = FuelRecipeMaps.Engine.findRecipe(java.util.Collections.emptyList(),
                java.util.List.of(tankFluid), false, 0, 0);
        if (recipe == null) {
            fuelTank.setEmpty();
            return;
        }
        long eut = Math.abs(recipe.mEUt);
        if (eut == 0) return;
        if (recipe.mFluidOutputs != null && recipe.mFluidOutputs.length > 0
                && recipe.mFluidOutputs[0] != null && !recipe.mFluidOutputs[0].isEmpty()) {
            long space = exhaustTank.capacity() - exhaustTank.getAmount();
            if (space < recipe.mFluidOutputs[0].getAmount()) return;
        }

        long energyPerCycle = eut * recipe.mDuration;
        if (energyPerCycle <= 0) return;
        kuEnergy += energyPerCycle;
        fuelTank.drain(1, IFluidHandler.FluidAction.EXECUTE);
        if (recipe.mFluidOutputs != null && recipe.mFluidOutputs.length > 0
                && recipe.mFluidOutputs[0] != null) {
            exhaustTank.fill(recipe.mFluidOutputs[0], IFluidHandler.FluidAction.EXECUTE);
        }
        if (kuEnergy > limit) kuEnergy = limit;
        setChanged();
    }

    void dischargeExhaust() {
        if (exhaustTank.isEmpty() || level == null) return;
        Direction facing = facing();
        BlockPos front = worldPosition.relative(facing);
        var be = level.getBlockEntity(front);
        if (be != null) {
            IFluidHandler target = be.getCapability(ForgeCapabilities.FLUID_HANDLER, facing.getOpposite())
                    .resolve().orElse(null);
            if (target != null) {
                FluidStack drained = exhaustTank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                if (!drained.isEmpty()) {
                    int filled = target.fill(drained, IFluidHandler.FluidAction.SIMULATE);
                    if (filled > 0) {
                        exhaustTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                        target.fill(new FluidStack(drained.getFluid(), filled), IFluidHandler.FluidAction.EXECUTE);
                    }
                }
            }
        }
        FluidStack exhaustFluid = exhaustTank.getFluid();
        if (!exhaustFluid.isEmpty()
                && exhaustFluid.getFluid().getFluidType().isLighterThanAir()
                && level.isEmptyBlock(front)) {
            exhaustTank.setEmpty();
        }
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("stopped", stopped);
        CompoundTag fuelTag = new CompoundTag();
        fuelTank.writeToNBT(fuelTag);
        tag.put("fuelTank", fuelTag);
        CompoundTag exhaustTag = new CompoundTag();
        exhaustTank.writeToNBT(exhaustTag);
        tag.put("exhaustTank", exhaustTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        stopped = tag.getBoolean("stopped");
        if (tag.contains("fuelTank")) fuelTank.readFromNBT(tag.getCompound("fuelTank"));
        if (tag.contains("exhaustTank")) exhaustTank.readFromNBT(tag.getCompound("exhaustTank"));
    }

    // ── Capabilities ─────────────────────────────────────────────────────────

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (fluidCap != null) { fluidCap.invalidate(); fluidCap = null; }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && side != null) {
            if (fluidCap == null || !fluidCap.isPresent()) {
                fluidCap = LazyOptional.of(() -> new DieselFluidHandler(this, side));
            }
            return fluidCap.cast();
        }
        return super.getCapability(cap, side);
    }

    private record DieselFluidHandler(KineticDieselEngineBlockEntity be, Direction side) implements IFluidHandler {
        @Override
        public int getTanks() { return 2; }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? be.fuelTank.getFluid() : be.exhaustTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return (int) (tank == 0 ? be.fuelTank.getCapacity() : be.exhaustTank.getCapacity());
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0) return false;
            return FuelRecipeMaps.Engine.containsInput(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!isFluidValid(0, resource)) return 0;
            return be.fuelTank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return be.exhaustTank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return be.exhaustTank.drain(maxDrain, action);
        }
    }
}
