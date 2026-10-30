package gregtech.content.blockentity.machine;

import gregtech.api.material.GTMaterial;
import gregtech.engine.fluid.MoltenFluidHandler;
import gregtech.engine.fluid.MoltenFluidPlans;
import gregtech.engine.machine.ITileEntityMold;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
//?}

/** Casting accepts a complete liquid portion and exposes only cooled, extractable products. */
public final class MoldAutomation implements IFluidHandler, IItemHandler {
    public interface Access extends ITileEntityMold {
        long fillMold(GTMaterial material, long amount, long temperature, int side, boolean execute);
        ItemStack automaticOutput();
        ItemStack extractAutomaticOutput(int count, boolean simulate);
    }

    private final Access mold;
    private final int side;

    public MoldAutomation(Access mold, Direction side) {
        this.mold = mold;
        this.side = (side == null ? Direction.UP : side).ordinal();
    }

    @Override public int getSlots() { return 1; }
    @Override public ItemStack getStackInSlot(int slot) { return slot == 0 ? mold.automaticOutput() : ItemStack.EMPTY; }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return slot == 0 && amount > 0 ? mold.extractAutomaticOutput(amount, simulate) : ItemStack.EMPTY;
    }
    @Override public int getSlotLimit(int slot) { return 64; }
    @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }

    @Override public int getTanks() { return 1; }
    // Casting contents are material units, not a drainable fluid tank.
    @Override public FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
    @Override public int getTankCapacity(int tank) {
        return tank == 0 ? MoltenFluidPlans.millibuckets(mold.getMoldRequiredMaterialUnits()) : 0;
    }
    @Override public boolean isFluidValid(int tank, FluidStack fluid) {
        return tank == 0 && mold.isMoldInputSide(side) && !fluid.isEmpty()
                && MoltenFluidHandler.material(fluid) != null;
    }
    @Override public int fill(FluidStack fluid, FluidAction action) {
        if (!isFluidValid(0, fluid)) return 0;
        var material = MoltenFluidHandler.material(fluid);
        long temperature = Math.max(fluid.getFluid().getFluidType().getTemperature(fluid), material.getMeltingPoint());
        var ratio = MoltenFluidHandler.ratio(material, fluid);
        long available = MoltenFluidPlans.toUnits(fluid.getAmount(), ratio);
        long accepted = mold.fillMold(material, available, temperature, side, false);
        int amount = MoltenFluidPlans.millibuckets(accepted, ratio);
        // Never create fractional-mB material or mutate a mold on a capability simulation.
        if (amount <= 0 || amount > fluid.getAmount() || MoltenFluidPlans.toUnits(amount, ratio) != accepted) return 0;
        if (action.execute() && mold.fillMold(material, accepted, temperature, side, true) != accepted)
            throw new IllegalStateException("Casting input changed during fluid transfer");
        return amount;
    }
    @Override public FluidStack drain(FluidStack fluid, FluidAction action) { return FluidStack.EMPTY; }
    @Override public FluidStack drain(int amount, FluidAction action) { return FluidStack.EMPTY; }
}
