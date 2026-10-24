package gregtech.blockentity.machine;

import gregtech.api.addon.FuelGenerator;
import gregtech.api.addon.FuelOutput;
import gregtech.api.energy.EnergyBlockViews;
import gregtech.api.energy.EnergyTransfer;
import gregtech.api.energy.IEnergyBlock;
//? if >=1.21.1 {
/*import gregtech.api.fluid.NativeMachineTankData;
*///?} else {
//?}
import gregtech.api.recipe.FluidFuelBatch;
import gregtech.api.recipe.Recipe;
import gregtech.api.recipe.RecipeMap;
import gregtech.block.machine.FuelGeneratorBlock;
import gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? if >=1.21.1 {
/*import net.minecraft.core.HolderLookup;
*///?} else {
//?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
//?}

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluid fuel to one energy packet per tick. The buffer spends the packet only after a receiver accepts it.
 * Built-in engines and turbines do not use this class.
 */
public class FuelGeneratorBlockEntity extends BlockEntity implements IEnergyBlock, IFluidHandler,
        gregtech.api.machine.MachineControl.Provider {
    private final FluidTank fuel;
    private final List<FluidTank> exhaust;
    private final RecipeMap recipes;
    private long stored;
    private boolean stopped;
    protected boolean formed = true;
    private boolean emitted;
    private GregTechTags.Tag energy;
//? if >=1.21.1 {
/**///?} else {
    @Nullable private LazyOptional<IFluidHandler> fluids;
//?}
    private final gregtech.api.machine.MachineControl control = gregtech.api.machine.MachineControls.switchable(this,
            () -> !stopped, value -> { stopped = !value; setChanged(); }, () -> stored > 0, () -> emitted);

    public FuelGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        FuelGenerator declaration = declaration();
        recipes = RecipeMap.RECIPE_MAPS.get(declaration.fuelMap());
        int outputs = recipes == null ? 0 : recipes.mOutputFluidCount;
        fuel = new FluidTank(declaration.tankCapacity(), stack -> recipes != null && recipes.containsInput(stack)) {
            @Override protected void onContentsChanged() { setChanged(); }
        };
        var tanks = new ArrayList<FluidTank>();
        for (int i = 0; i < outputs; i++) {
            tanks.add(new FluidTank(declaration.tankCapacity()) {
                @Override protected void onContentsChanged() { setChanged(); }
            });
        }
        exhaust = List.copyOf(tanks);
    }

    public FuelGenerator declaration() {
        return ((FuelGeneratorBlock) getBlockState().getBlock()).declaration();
    }

    public long storedEnergy() { return stored; }
    public boolean isStopped() { return stopped; }
    public boolean isFormed() { return formed; }
    public int fuelAmount() { return fuel.getFluidAmount(); }
    public int exhaustAmount() {
        int amount = 0;
        for (FluidTank tank : exhaust) amount += tank.getFluidAmount();
        return amount;
    }

    @Override public gregtech.api.machine.MachineControl machineControl(Direction side) { return control; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FuelGeneratorBlockEntity be) { be.tick(); }

    public void tick() {
        emitted = false;
        if (level == null || level.isClientSide) return;
        boolean running = refreshStructure() && !stopped;
        if (running) {
            pullCompressedGas();
            burn();
            emit();
        }
        updateAppearance();
    }

    /** Multiblock controllers replace this with the structure check. */
    protected boolean refreshStructure() { return true; }

    /** Takes up to 1000 mB from adjacent compressed gas drums when the fuel map accepts that gas. */
    private void pullCompressedGas() {
        int budget = Math.min(1000, fuel.getCapacity() - fuel.getFluidAmount());
        if (budget <= 0 || level == null) return;
        for (Direction side : Direction.values()) {
            if (budget <= 0) return;
            BlockPos neighbor = worldPosition.relative(side);
            if (!level.hasChunkAt(neighbor)) continue;
            if (!(level.getBlockEntity(neighbor) instanceof gregtech.api.fluid.CompressedGasReceiver receiver))
                continue;
            Direction face = side.getOpposite();
            FluidStack preview = receiver.extractCompressedGas(face, budget, false);
            if (preview.isEmpty() || !fuel.isFluidValid(preview)) continue;
            int room = fuel.fill(preview, IFluidHandler.FluidAction.SIMULATE);
            if (room <= 0) continue;
            FluidStack taken = receiver.extractCompressedGas(face, room, true);
            if (taken.isEmpty()) continue;
            int storedAmount = fuel.fill(taken, IFluidHandler.FluidAction.EXECUTE);
            if (storedAmount < taken.getAmount()) {
                FluidStack leftover = taken.copy();
                leftover.setAmount(taken.getAmount() - storedAmount);
                receiver.receiveCompressedGas(face, leftover);
            }
            budget -= storedAmount;
        }
    }

    private void burn() {
        if (recipes == null || fuel.isEmpty()) return;
        long room = Math.multiplyExact(declaration().output().perTick(), 2) - stored;
        if (room <= 0) return;
        var outputs = new ArrayList<FluidStack>();
        int[] capacities = new int[exhaust.size()];
        for (int i = 0; i < exhaust.size(); i++) {
            outputs.add(exhaust.get(i).getFluid().copy());
            capacities[i] = exhaust.get(i).getCapacity();
        }
        boolean matched = false;
        for (Recipe recipe : recipes.mRecipeList) {
            var plan = FluidFuelBatch.plan(recipe, fuel.getFluid().copy(), outputs, capacities, room, declaration().efficiency());
            if (plan == null || plan.energy() <= 0 || plan.energy() > Long.MAX_VALUE - stored) continue;
            fuel.setFluid(plan.input());
            for (int i = 0; i < exhaust.size(); i++) exhaust.get(i).setFluid(plan.outputs().get(i));
            stored += plan.energy();
            matched = true;
            setChanged();
            break;
        }
        if (!matched && !recipes.containsInput(fuel.getFluid())) {
            fuel.setFluid(FluidStack.EMPTY);
            setChanged();
        }
    }

    private void emit() {
        long packet = declaration().output().perTick();
        if (packet < 1 || stored < packet || energyType() == null) return;
        long packets = Math.min(declaration().amperes(), stored / packet);
        FuelOutput output = declaration().output();
        long sent = output.behind() > 0 ? injectBehind(packet, packets)
                : output.front()
                ? EnergyTransfer.emitEnergyToSide(energyType(), front(), packet, packets, this)
                : EnergyTransfer.emitEnergyToSide(energyType(), front().getOpposite(), packet, packets, this);
        if (sent > 0) {
            stored -= packet * sent;
            emitted = true;
            setChanged();
        }
    }

    private long injectBehind(long packet, long packets) {
        Direction back = front().getOpposite();
        BlockPos target = worldPosition.relative(back, declaration().output().behind());
        if (level == null || !level.hasChunkAt(target)) return 0;
        IEnergyBlock receiver = EnergyBlockViews.view(level.getBlockEntity(target));
        if (receiver == null) return 0;
        return receiver.doEnergyInjection(energyType(), back.getOpposite(), packet, packets, true);
    }

    private void updateAppearance() {
        BlockState state = getBlockState();
        boolean running = stored > 0;
        if (state.getValue(FuelGeneratorBlock.RUNNING) == running && state.getValue(FuelGeneratorBlock.LIT) == emitted) return;
        level.setBlock(worldPosition, state.setValue(FuelGeneratorBlock.RUNNING, running).setValue(FuelGeneratorBlock.LIT, emitted), 3);
    }

    protected Direction front() { return getBlockState().getValue(FuelGeneratorBlock.FACING); }

    protected boolean acceptsFluid(@Nullable Direction side) { return true; }

//? if >=1.21.1 {
/*    public IFluidHandler fluidCapability(@Nullable Direction side) {
        return !isRemoved() && acceptsFluid(side) ? this : null;
    }

*///?} else {
//?}
    private GregTechTags.Tag energyType() {
        if (energy != null) return energy;
        String name = declaration().output().type();
        energy = GregTechTags.Energy.ALL.stream().filter(tag -> tag.getShortName().equalsIgnoreCase(name)).findFirst().orElse(null);
        return energy;
    }

    private boolean outputs(GregTechTags.Tag type, @Nullable Direction side) {
        if (type != energyType() || declaration().output().behind() > 0) return false;
        if (side == null) return true;
        return declaration().output().front() ? side == front() : side == front().getOpposite();
    }

    @Override public boolean isEnergyType(GregTechTags.Tag type, @Nullable Direction side, boolean emitting) {
        return emitting && outputs(type, side);
    }
    @Override public java.util.Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return energyType() == null ? List.of() : List.of(energyType());
    }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, @Nullable Direction side, boolean theoretical) { return false; }
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type, @Nullable Direction side, boolean theoretical) {
        return outputs(type, side) && (theoretical || !stopped && formed && stored >= declaration().output().perTick());
    }
    @Override public long doEnergyInjection(GregTechTags.Tag type, @Nullable Direction side, long size, long amount, boolean doInject) { return 0; }
    @Override public long getEnergyDemanded(GregTechTags.Tag type, @Nullable Direction side, long size) { return 0; }
    @Override public long doEnergyExtraction(GregTechTags.Tag type, @Nullable Direction side, long size, long amount, boolean doExtract) { return 0; }
    @Override public long getEnergyOffered(GregTechTags.Tag type, @Nullable Direction side, long size) { return 0; }
    @Override public long getEnergySizeInputMin(GregTechTags.Tag type, @Nullable Direction side) { return 0; }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type, @Nullable Direction side) { return 0; }
    @Override public long getEnergySizeInputMax(GregTechTags.Tag type, @Nullable Direction side) { return 0; }
    @Override public long getEnergySizeOutputMin(GregTechTags.Tag type, @Nullable Direction side) {
        return outputs(type, side) ? declaration().output().perTick() : 0;
    }
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type, @Nullable Direction side) {
        return getEnergySizeOutputMin(type, side);
    }
    @Override public long getEnergySizeOutputMax(GregTechTags.Tag type, @Nullable Direction side) {
        return getEnergySizeOutputMin(type, side);
    }
    @Override public long getEnergyStored(GregTechTags.Tag type, @Nullable Direction side) {
        return type == energyType() ? stored : 0;
    }
    @Override public long getEnergyCapacity(GregTechTags.Tag type, @Nullable Direction side) {
        return type == energyType() ? Math.max(stored, declaration().output().perTick() * 2) : 0;
    }

    @Override public int getTanks() { return 1 + exhaust.size(); }
    @Override public FluidStack getFluidInTank(int tank) {
        if (tank == 0) return fuel.getFluid().copy();
        return tank > 0 && tank <= exhaust.size() ? exhaust.get(tank - 1).getFluid().copy() : FluidStack.EMPTY;
    }
    @Override public int getTankCapacity(int tank) {
        if (tank == 0) return fuel.getCapacity();
        return tank > 0 && tank <= exhaust.size() ? exhaust.get(tank - 1).getCapacity() : 0;
    }
    @Override public boolean isFluidValid(int tank, FluidStack stack) { return tank == 0 && fuel.isFluidValid(stack); }
    @Override public int fill(FluidStack stack, FluidAction action) { return fuel.fill(stack, action); }
    @Override public FluidStack drain(FluidStack stack, FluidAction action) {
        for (FluidTank tank : exhaust) {
            FluidStack drained = tank.drain(stack, action);
            if (!drained.isEmpty()) return drained;
        }
        return FluidStack.EMPTY;
    }
    @Override public FluidStack drain(int amount, FluidAction action) {
        for (FluidTank tank : exhaust) {
            FluidStack drained = tank.drain(amount, action);
            if (!drained.isEmpty()) return drained;
        }
        return FluidStack.EMPTY;
    }

//? if >=1.21.1 {
/*    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        writeMachine(tag, lookup);
*///?} else {
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        if (fluids != null) fluids.invalidate();
        fluids = null;
//?}
    }

    @Override
//? if >=1.21.1 {
/*    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        readMachine(tag, lookup);
*///?} else {
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && acceptsFluid(side)) {
            if (fluids == null || !fluids.isPresent()) fluids = LazyOptional.of(() -> this);
            return fluids.cast();
        }
        return super.getCapability(cap, side);
//?}
    }

//? if >=1.21.1 {
/*    protected void writeMachine(CompoundTag tag, HolderLookup.Provider lookup) {
*///?} else {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeMachine(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        readMachine(tag);
    }

    protected void writeMachine(CompoundTag tag) {
//?}
        tag.putLong("gt.energy", stored);
        tag.putBoolean("gt.stopped", stopped);
        tag.putBoolean("gt.formed", formed);
//? if >=1.21.1 {
/*        tag.put("gt.fuel", fuel.writeToNBT(lookup, new CompoundTag()));
        for (int i = 0; i < exhaust.size(); i++) tag.put("gt.exhaust" + i, exhaust.get(i).writeToNBT(lookup, new CompoundTag()));
*///?} else {
        tag.put("gt.fuel", fuel.writeToNBT(new CompoundTag()));
        for (int i = 0; i < exhaust.size(); i++) tag.put("gt.exhaust" + i, exhaust.get(i).writeToNBT(new CompoundTag()));
//?}
    }

//? if >=1.21.1 {
/*    protected void readMachine(CompoundTag tag, HolderLookup.Provider lookup) {
*///?} else {
    protected void readMachine(CompoundTag tag) {
//?}
        stored = Math.max(0, tag.getLong("gt.energy"));
        stopped = tag.getBoolean("gt.stopped");
        formed = !tag.contains("gt.formed") || tag.getBoolean("gt.formed");
//? if >=1.21.1 {
/*        if (tag.contains("gt.fuel")) NativeMachineTankData.restore(fuel, tag.getCompound("gt.fuel"), lookup);
*///?} else {
        if (tag.contains("gt.fuel")) fuel.readFromNBT(tag.getCompound("gt.fuel"));
//?}
        for (int i = 0; i < exhaust.size(); i++) {
//? if >=1.21.1 {
/*            if (tag.contains("gt.exhaust" + i)) NativeMachineTankData.restore(exhaust.get(i), tag.getCompound("gt.exhaust" + i), lookup);
*///?} else {
            if (tag.contains("gt.exhaust" + i)) exhaust.get(i).readFromNBT(tag.getCompound("gt.exhaust" + i));
//?}
        }
    }
}
