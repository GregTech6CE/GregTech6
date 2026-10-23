package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.api.machine.DieselEngineSpec;
import com.gregtech.gregtech.data.FuelRecipeMaps;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

import javax.annotation.Nullable;

/** Liquid-fuel → RU motor. Preserves complete FM.Engine cycle credit and original one-packet waste semantics. */
public class KineticDieselEngineBlockEntity extends EngineBaseBlockEntity implements com.gregtech.gregtech.api.machine.MachineControl.Provider,
        com.gregtech.gregtech.api.fluid.FluidToolTarget {
    protected boolean emitted;
    private final com.gregtech.gregtech.api.machine.MachineControl control=com.gregtech.gregtech.api.machine.MachineControls.switchable(this,
            ()->!isStopped(),value->setStopped(!value),this::isActive,()->emitted);
    @Override public com.gregtech.gregtech.api.machine.MachineControl machineControl(Direction side){return control;}
    DieselEngineSpec spec;
    boolean active;
    private long activityHistory;
    boolean stopped;
    FluidTankGT fuelTank = new FluidTankGT(4000).setOnChanged(this::setChanged);
    FluidTankGT exhaustTank = new FluidTankGT(4000).setOnChanged(this::setChanged);


//? if >=1.21.1 {
//?} else {
    @Nullable private LazyOptional<IFluidHandler> fluidCap;
//?}

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

    @Override protected GregTechTags.Tag outputEnergyType() { return GregTechTags.Energy.RU; }
    @Override public boolean isEnergyType(GregTechTags.Tag type, @Nullable Direction side, boolean emitting) { return emitting && type==GregTechTags.Energy.RU; }
    @Override public java.util.Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) { return java.util.List.of(GregTechTags.Energy.RU); }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, @Nullable Direction side, boolean theoretical) { return false; }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type, @Nullable Direction side) { return 0; }
    @Override public long getEnergyDemanded(GregTechTags.Tag type, @Nullable Direction side, long size) { return 0; }
    @Override public long doInject(GregTechTags.Tag type, @Nullable Direction side, long size, long amount, boolean execute) { return 0; }
    // The original motor only pushes one packet; pull must not produce a second packet in the same tick.
    @Override public long getEnergyOffered(GregTechTags.Tag type, @Nullable Direction side, long size) { return 0; }
    @Override public long doExtract(GregTechTags.Tag type, @Nullable Direction side, long size, long amount, boolean execute) { return 0; }
    @Override public long getEnergySizeOutputMin(GregTechTags.Tag type, @Nullable Direction side) { return type==GregTechTags.Energy.RU?outputRate():0; }
    @Override public long getEnergySizeOutputMax(GregTechTags.Tag type, @Nullable Direction side) { return type==GregTechTags.Energy.RU?outputRate():0; }
    @Override public long getEnergyCapacity(GregTechTags.Tag type, @Nullable Direction side) { return type==GregTechTags.Energy.RU?Math.max(kuEnergy,outputRate()*2):0; }

    // GT6 fixes RU output to the front; legacy saved face masks must not redirect it.
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type, @Nullable Direction side, boolean theoretical) {
        return type == GregTechTags.Energy.RU && (side == null || side == facing());
    }

    public boolean isEmitting() { return emitted; }

    @Override protected GregTechTags.Tag inputEnergyType() { return GregTechTags.Energy.KU; } // doesn't accept external energy

    @Override
    public FaceConfig defaultFaceConfig() {
        return FaceConfig.builder()
                .fluidIn(FaceConfig.BOTTOM, FaceConfig.TOP, FaceConfig.FRONT, FaceConfig.BACK, FaceConfig.LEFT, FaceConfig.RIGHT)
                .fluidOut(FaceConfig.BOTTOM, FaceConfig.TOP, FaceConfig.FRONT, FaceConfig.BACK, FaceConfig.LEFT, FaceConfig.RIGHT)
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

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, KineticDieselEngineBlockEntity be) {
        if (be.spec == null) return;
        long beforeEmission=be.kuEnergy;
        if(be.kuEnergy >= be.outputRate() && be.outputRate()>0) {
            com.gregtech.gregtech.api.energy.EnergyTransfer.emitEnergyToNetwork(
                    GregTechTags.Energy.RU,be.outputRate(),1,be);
            be.kuEnergy=com.gregtech.gregtech.api.machine.LiquidFuelCycle.afterEmission(be.kuEnergy,be.outputRate());
            be.setChanged();
        }
        be.emitted=be.kuEnergy<beforeEmission;
        be.burnFuel();
        be.dischargeExhaust();

        // Original TE_Behavior_Active_Trinary keeps animation through short refuelling gaps.
        long previousHistory = be.activityHistory;
        be.activityHistory = (be.activityHistory << 1) | (be.active ? 1L : 0L);
        boolean lit = !be.stopped && be.activityHistory != 0;
        if (state.getValue(com.gregtech.gregtech.block.machine.EngineBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(com.gregtech.gregtech.block.machine.EngineBlock.LIT, lit), 3);
        }
        if (previousHistory != be.activityHistory) be.setChanged();
    }

    void burnFuel() {
        if (level == null) return;
        if (com.gregtech.gregtech.api.machine.LiquidFuelCycle.needsFuel(kuEnergy, outputRate(), stopped)) active = false;
        while(com.gregtech.gregtech.api.machine.LiquidFuelCycle.needsFuel(kuEnergy,outputRate(),stopped) && !fuelTank.isEmpty()) {
            var tankFluid=fuelTank.getFluid();
            var recipe=FuelRecipeMaps.Engine.findRecipe(java.util.Collections.emptyList(),java.util.List.of(tankFluid),false,0,0);
            if (recipe == null) {
                // A valid but incomplete batch may wait 64 inactive ticks before changing fuel.
                if (!FuelRecipeMaps.Engine.containsInput(tankFluid) || activityHistory == 0) fuelTank.setEmpty();
                return;
            }
            if(recipe.mFluidInputs==null || recipe.mFluidInputs.length!=1 || recipe.mFluidInputs[0]==null)return;
            int amount=recipe.mFluidInputs[0].getAmount();
            long energy=com.gregtech.gregtech.api.machine.LiquidFuelCycle.recipeEnergy(recipe.mEUt,recipe.mDuration);
            if(amount<=0 || energy<=0)return;
            var requested=tankFluid.copy();requested.setAmount(amount);
            if(fuelTank.drain(requested,IFluidHandler.FluidAction.SIMULATE).getAmount()!=amount)return;
            var exhaust=recipe.mFluidOutputs!=null && recipe.mFluidOutputs.length>0?recipe.mFluidOutputs[0]:null;
            if(exhaust!=null && !exhaust.isEmpty() && exhaustTank.fill(exhaust,IFluidHandler.FluidAction.SIMULATE)!=exhaust.getAmount())return;
            long credited=com.gregtech.gregtech.api.machine.LiquidFuelCycle.credit(kuEnergy,energy);
            if(fuelTank.drain(requested,IFluidHandler.FluidAction.EXECUTE).getAmount()!=amount)throw new IllegalStateException("Diesel fuel drain changed after simulation");
            if(exhaust!=null && !exhaust.isEmpty())exhaustTank.fill(exhaust,IFluidHandler.FluidAction.EXECUTE);
            kuEnergy=credited;
            active = true;
            setChanged();
        }
    }

    void dischargeExhaust() {
        if (exhaustTank.isEmpty() || level == null) return;
        Direction rear = facing().getOpposite();
        BlockPos targetPos = worldPosition.relative(rear);
        if (!level.hasChunkAt(targetPos)) return;
//? if >=1.21.1 {
        /*IFluidHandler target = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, targetPos, rear.getOpposite());
*///?} else {
        var owner = level.getBlockEntity(targetPos);
        IFluidHandler target = owner == null ? null : owner.getCapability(ForgeCapabilities.FLUID_HANDLER, rear.getOpposite()).orElse(null);
//?}
        if (target != null) {
            FluidStack offered = exhaustTank.getFluid().copy();
            // Only remove the amount actually accepted, not a prior simulation result.
            int accepted = target.fill(offered, IFluidHandler.FluidAction.EXECUTE);
            exhaustTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        }
        FluidStack exhaust = exhaustTank.getFluid();
        if (!exhaust.isEmpty()
                && com.gregtech.gregtech.api.fluid.FluidHazards.isGas(exhaust.getFluid())
                && level.getBlockState(targetPos).getCollisionShape(level, targetPos).isEmpty()) {
            exhaustTank.setEmpty();
        }
    }

    /** Original tap access can recover input fuel only after the output tank is empty. */
    @Override
    public IFluidHandler fluidToolHandler(Direction side, boolean filling) {
        if (isRemoved()) return null;
        return filling ? new com.gregtech.gregtech.api.fluid.FluidPort(new DieselFluidHandler(this), true, false) : tap;
    }

    private final IFluidHandler tap = new IFluidHandler() {
        private FluidTankGT tank() { return exhaustTank.isEmpty() ? fuelTank : exhaustTank; }
        public int getTanks() { return 1; }
        public FluidStack getFluidInTank(int slot) { return slot == 0 ? tank().getFluid().copy() : FluidStack.EMPTY; }
        public int getTankCapacity(int slot) { return slot == 0 ? tank().getCapacity() : 0; }
        public boolean isFluidValid(int slot, FluidStack fluid) { return false; }
        public int fill(FluidStack fluid, FluidAction action) { return 0; }
        public FluidStack drain(FluidStack fluid, FluidAction action) { return tank().drain(fluid, action); }
        public FluidStack drain(int amount, FluidAction action) { return tank().drain(amount, action); }
    };

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
//? if >=1.21.1 {
    /*protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
*///?} else {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
//?}
        tag.putBoolean("stopped", stopped);
        tag.putBoolean("dieselActive", active);
        tag.putLong("dieselActivityHistory", activityHistory);
        CompoundTag fuelTag = new CompoundTag();
//? if >=1.21.1 {
        /*fuelTank.writeToNBT(fuelTag,lookup);
*///?} else {
        fuelTank.writeToNBT(fuelTag);
//?}
        tag.put("fuelTank", fuelTag);
        CompoundTag exhaustTag = new CompoundTag();
//? if >=1.21.1 {
        /*exhaustTank.writeToNBT(exhaustTag,lookup);
*///?} else {
        exhaustTank.writeToNBT(exhaustTag);
//?}
        tag.put("exhaustTank", exhaustTag);
    }

    @Override
//? if >=1.21.1 {
    /*protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
*///?} else {
    public void load(CompoundTag tag) {
        super.load(tag);
//?}
        stopped = tag.getBoolean("stopped");
        active = tag.getBoolean("dieselActive");
        activityHistory = tag.getLong("dieselActivityHistory");
//? if >=1.21.1 {
        /*if (tag.contains("fuelTank")) fuelTank.readFromNBT(tag.getCompound("fuelTank"),lookup);
        if (tag.contains("exhaustTank")) exhaustTank.readFromNBT(tag.getCompound("exhaustTank"),lookup);
*///?} else {
        if (tag.contains("fuelTank")) fuelTank.readFromNBT(tag.getCompound("fuelTank"));
        if (tag.contains("exhaustTank")) exhaustTank.readFromNBT(tag.getCompound("exhaustTank"));
//?}
    }

    // ── Capabilities ─────────────────────────────────────────────────────────

//? if >=1.21.1 {
    /*public IFluidHandler fluidHandler(@Nullable Direction side){return isRemoved() ? null : new DieselFluidHandler(this);}
*///?} else {
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (fluidCap != null) { fluidCap.invalidate(); fluidCap = null; }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && !isRemoved()) {
            if (fluidCap == null || !fluidCap.isPresent()) {
                fluidCap = LazyOptional.of(() -> new DieselFluidHandler(this));
            }
            return fluidCap.cast();
        }
        return super.getCapability(cap, side);
    }
//?}

    private record DieselFluidHandler(KineticDieselEngineBlockEntity be) implements IFluidHandler {
        @Override
        public int getTanks() { return 2; }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? be.fuelTank.getFluid().copy() : tank == 1 ? be.exhaustTank.getFluid().copy() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? be.fuelTank.getCapacity() : tank == 1 ? be.exhaustTank.getCapacity() : 0;
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
