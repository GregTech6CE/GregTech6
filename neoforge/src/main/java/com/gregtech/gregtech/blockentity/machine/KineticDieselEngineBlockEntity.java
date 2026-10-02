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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

/** Liquid-fuel → RU motor. Preserves complete FM.Engine cycle credit and original one-packet waste semantics. */
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
    @Override public long getEnergyCapacity(GregTechTags.Tag type, @Nullable Direction side) { return type==GregTechTags.Energy.RU?Math.max(kuEnergy,outputRate()*2):0; }

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

        boolean wasActive = be.active;
        be.active = be.kuEnergy > 0;
        if (be.active != wasActive) {
            level.setBlock(pos, state.setValue(com.gregtech.gregtech.block.machine.EngineBlock.LIT, be.active), 3);
        }
    }

    void burnFuel() {
        if(level==null)return;
        while(com.gregtech.gregtech.api.machine.LiquidFuelCycle.needsFuel(kuEnergy,outputRate(),stopped) && !fuelTank.isEmpty()) {
            var tankFluid=fuelTank.getFluid();
            var recipe=FuelRecipeMaps.Engine.findRecipe(java.util.Collections.emptyList(),java.util.List.of(tankFluid),false,0,0);
            if(recipe==null) {if(!FuelRecipeMaps.Engine.containsInput(tankFluid))fuelTank.setEmpty();return;}
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
            setChanged();
        }
    }

    void dischargeExhaust() {
        if (exhaustTank.isEmpty() || level == null) return;
        Direction facing = facing();
        BlockPos front = worldPosition.relative(facing.getOpposite());
        var be = level.getBlockEntity(front);
        if (be != null) {
            IFluidHandler target = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,front,facing);
            if (target != null) {
                FluidStack drained = exhaustTank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                if (!drained.isEmpty()) {
                    int filled = target.fill(drained, IFluidHandler.FluidAction.SIMULATE);
                    if (filled > 0) {
                        exhaustTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                        var moved=drained.copy();moved.setAmount(filled);target.fill(moved, IFluidHandler.FluidAction.EXECUTE);
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
    protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.putBoolean("stopped", stopped);
        CompoundTag fuelTag = new CompoundTag();
        fuelTank.writeToNBT(fuelTag,lookup);
        tag.put("fuelTank", fuelTag);
        CompoundTag exhaustTag = new CompoundTag();
        exhaustTank.writeToNBT(exhaustTag,lookup);
        tag.put("exhaustTank", exhaustTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        stopped = tag.getBoolean("stopped");
        if (tag.contains("fuelTank")) fuelTank.readFromNBT(tag.getCompound("fuelTank"),lookup);
        if (tag.contains("exhaustTank")) exhaustTank.readFromNBT(tag.getCompound("exhaustTank"),lookup);
    }

    // ── Capabilities ─────────────────────────────────────────────────────────

    public IFluidHandler fluidHandler(@Nullable Direction side){return side==null?null:new DieselFluidHandler(this,side);}

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
