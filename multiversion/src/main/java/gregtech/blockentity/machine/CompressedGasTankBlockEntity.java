package gregtech.blockentity.machine;

import gregtech.api.fluid.FluidHazards;
import gregtech.api.fluid.FluidTankGT;
import gregtech.block.machine.CompressedGasTankBlock;
import gregtech.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
//? if >=1.21.1 {
/*import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

import javax.annotation.Nullable;

/**
 * Stores one gas. Pipes, buckets and funnels cannot fill it.
 * {@link #fillCompressed} is the compressor path. Drain is open, so a fuel machine can take the gas.
 */
public class CompressedGasTankBlockEntity extends BlockEntity implements IFluidHandler,
        gregtech.api.fluid.CompressedGasReceiver {
    private final FluidTankGT tank;

//? if >=1.21.1 {
//?} else {
    @Nullable private LazyOptional<IFluidHandler> fluids = LazyOptional.of(() -> this);
//?}

    public CompressedGasTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tank = new FluidTankGT(declaration().capacity()).setOnChanged(this::setChanged);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CompressedGasTankBlockEntity tank) {
        if (!level.isClientSide) tank.tickServer();
    }

    private void tickServer() {
        FluidStack stored = tank.getFluid();
        if (!stored.isEmpty() && !accepts(stored)) {
            tank.setEmpty();
            setChanged();
        }
    }

    private CompressedGasTankBlock block() {
        return (CompressedGasTankBlock) getBlockState().getBlock();
    }

    private gregtech.api.addon.CompressedGasTankDeclaration declaration() {
        return block().declaration();
    }

    @Override public int receiveCompressedGas(Direction side, FluidStack stack) { return fillCompressed(stack); }

    @Override public FluidStack extractCompressedGas(Direction side, int amount, boolean execute) {
        return drain(amount, execute ? FluidAction.EXECUTE : FluidAction.SIMULATE);
    }

    /** Compressor insertion. Returns the millibuckets stored. */
    public int fillCompressed(FluidStack stack) {
        if (!accepts(stack)) return 0;
        return tank.fill(stack, FluidAction.EXECUTE);
    }

    public boolean accepts(FluidStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Fluid fluid = stack.getFluid();
        var declaration = declaration();
        if (!FluidHazards.isGas(fluid)) return false;
        if (fluidTemperature(stack) >= block().meltingPoint()) return false;
        if (FluidHazards.proofRejects(fluid, true, declaration.acidProof(), declaration.plasmaProof())) return false;
        return !FluidHazards.isMagic(fluid) || declaration.magicProof();
    }

    private static long fluidTemperature(FluidStack stack) {
        var entry = GTFluids.entryForFluid(stack.getFluid());
        return entry == null ? stack.getFluid().getFluidType().getTemperature(stack) : entry.temperature();
    }

//? if >=1.21.1 {
/*    public IFluidHandler fluidCapability(@Nullable Direction side) {
        return isRemoved() ? null : this;
    }
*///?} else {
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && !isRemoved()) {
            if (fluids == null || !fluids.isPresent()) fluids = LazyOptional.of(() -> this);
            return fluids.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        if (fluids != null) fluids.invalidate();
        fluids = null;
    }

    @Override public void reviveCaps() {
        super.reviveCaps();
        fluids = LazyOptional.of(() -> this);
    }
//?}

    @Override public int getTanks() { return 1; }
    @Override public FluidStack getFluidInTank(int index) { return index == 0 ? tank.getFluid() : FluidStack.EMPTY; }
    @Override public int getTankCapacity(int index) { return index == 0 ? tank.getCapacity() : 0; }
    @Override public boolean isFluidValid(int index, FluidStack stack) { return false; }
    @Override public int fill(FluidStack stack, FluidAction action) { return 0; }
    @Override public FluidStack drain(FluidStack stack, FluidAction action) { return tank.drain(stack, action); }
    @Override public FluidStack drain(int amount, FluidAction action) { return tank.drain(amount, action); }

    @Override
//? if >=1.21.1 {
/*    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tank.writeToNBT(tag, lookup);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        tank.readFromNBT(tag, lookup);
    }
*///?} else {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tank.writeToNBT(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tank.readFromNBT(tag);
    }
//?}
}
