package gregtech.content.blockentity.machine;

import gregtech.engine.fluid.FluidPort;
import gregtech.engine.multiblock.MultiblockLayout;
import gregtech.content.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
*///?} else {
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
//?}

import javax.annotation.Nullable;

/** GT6 17211–17214: a steam batch drives two consecutive RU pulses; 170 L condense to 1 L. */
public class LargeTurbineControllerBlockEntity extends AxialGeneratorBlockEntity implements IFluidHandler {
    private long pending;
    private int steamCounter;
    private final FluidTank steam = new FluidTank(grade().inputMaximum() * 4, LargeTurbineControllerBlockEntity::isSteam) {

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank water = new FluidTank(grade().inputMaximum() * 4) {

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler input = new FluidPort(this, true, false), output = new FluidPort(this, false, true);
//? if >=1.21.1 {
//?} else {
    private LazyOptional<IFluidHandler> fluids = LazyOptional.of(() -> this);
//?}
    public LargeTurbineControllerBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.LARGE_TURBINE.get(), pos, state);
    }

    private static boolean isSteam(FluidStack stack) {
        var fluid = GTFluids.still("Steam");
        return !stack.isEmpty() && fluid != null && GTFluids.isPresent(fluid) && stack.getFluid() == fluid.get();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LargeTurbineControllerBlockEntity machine) {
        machine.tick();
    }

    public void tick() {
        if (level == null || level.isClientSide) return;
        boolean formed = structureOk();
        if (!formed || overloaded) {
            updateRotorVisual(formed, false);
            return;
        }
        if (pending > 0) {
            energy += pending;
            pending = 0;
            setChanged();
        }
        else if (!stopped && steam.getFluidAmount() >= grade().input()) {
            int amount = steam.getFluidAmount();
            steam.drain(amount, FluidAction.EXECUTE);
            energy += amount / 2;
            pending = amount / 2;
            int condensed = (steamCounter + amount) / gregtech.content.multiblock.OriginalGeneratorParameters.STEAM_CONDENSATION_RATIO;
            steamCounter = (steamCounter + amount) % gregtech.content.multiblock.OriginalGeneratorParameters.STEAM_CONDENSATION_RATIO;
            var distilled = GTFluids.still("DistW");
            if (condensed > 0 && distilled != null && GTFluids.isPresent(distilled)) water.fill(new FluidStack(distilled.get(), condensed),
                    FluidAction.EXECUTE);
            // As in GT6 the distilled-water tank voids excess; a full output never blocks the shaft.
            setChanged();
        }
        convertAndEmit();
        if (overloaded) pending = 0;
        updateRotorVisual(true, !overloaded && isConvertingThisTick());
    }

    /** GT6 plunger empties the input tank first, then the condensed water tank. */
    public int purgeFluid() {
        var tank = steam.isEmpty() ? water : steam;
        return tank.drain(tank.getFluidAmount(), FluidAction.EXECUTE).getAmount();
    }

    @Override
    public IFluidHandler portFluids(MultiblockLayout.Role role) {
        return switch (role) {
            case FLUID_INPUT -> input;
            case FLUID_OUTPUT -> output;
            case FLUID_IO -> this;
            default -> null;
        };
    }

    @Override
    public IFluidHandler portFluidTank(MultiblockLayout.Role role, int tank) {
        if (role == null || tank < 0 || tank >= getTanks()) return null;
        return new gregtech.engine.fluid.FluidPort(tank == 0 ? steam : water,
                tank == 0 && role.allows(gregtech.api.multiblock.PortChannel.FLUID_INPUT),
                tank > 0 && role.allows(gregtech.api.multiblock.PortChannel.FLUID_OUTPUT));
    }

    @Override
    public int getTanks() {
        return 2;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank == 0 ? steam.getFluid().copy() : tank == 1 ? water.getFluid().copy() : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 ? steam.getCapacity() : tank == 1 ? water.getCapacity() : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 && !stopped && !overloaded && isSteam(stack);
    }

    @Override
    public int fill(FluidStack stack, FluidAction action) {
        int filled = isFluidValid(0, stack) ? steam.fill(stack, action) : 0;
        if (action.execute() && level != null) conversionMeter.input(level.getGameTime(), filled);
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack stack, FluidAction action) {
        return water.drain(stack, action);
    }

    @Override
    public FluidStack drain(int amount, FluidAction action) {
        return water.drain(amount, action);
    }
//? if >=1.21.1 {
    /*public IFluidHandler fluidCapability(Direction side) {
        return !isRemoved() && (side == null || side == front()) ? this : null;
    }
*///?} else {
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == ForgeCapabilities.FLUID_HANDLER && (side == null || side == front()) ? fluids.cast() : super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluids.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        fluids = LazyOptional.of(() -> this);
    }
//?}

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        tag.putLong("gt.pending", pending);
        tag.putInt("gt.steam_counter", steamCounter);
        tag.put("gt.steam", gregtech.engine.stack.NbtCompat.writeTank(steam, new CompoundTag(), lookup));
        tag.put("gt.water", gregtech.engine.stack.NbtCompat.writeTank(water, new CompoundTag(), lookup));
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        pending = Math.max(0, Math.min(grade().inputMaximum(), tag.getLong("gt.pending")));
        steamCounter = Math.floorMod(tag.getInt("gt.steam_counter"),
                gregtech.content.multiblock.OriginalGeneratorParameters.STEAM_CONDENSATION_RATIO);
        gregtech.engine.stack.NbtCompat.readTank(steam, tag.getCompound("gt.steam"), lookup);
        if (!steam.isEmpty() && !isSteam(steam.getFluid())) steam.setFluid(FluidStack.EMPTY);
        gregtech.engine.stack.NbtCompat.readTank(water, tag.getCompound("gt.water"), lookup);
    }
}
