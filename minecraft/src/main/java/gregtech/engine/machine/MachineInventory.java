package gregtech.engine.machine;

import gregtech.engine.fluid.FluidTankGT;
import gregtech.engine.recipe.RecipeMap;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Items, input tanks and output tanks of a basic machine, including the block-facing fluid contract. */
public final class MachineInventory implements IFluidHandler {
    private final MachineItemHandler items;
    private final MachineItemHandler potionInput;
    private final FluidTankGT[] tanksInput;
    private final FluidTankGT[] tanksOutput;
    private final int inputCount;
    private final int potionCount;
    private final boolean originalFluidBuffers;

    public MachineInventory(RecipeMap recipeMap, int parallelLimit, boolean originalFluidBuffers, Runnable onChanged) {
        this.originalFluidBuffers = originalFluidBuffers;
        this.inputCount = recipeMap.mInputItemsCount;
        this.potionCount = recipeMap.mInputPotionCount;
        this.items = new MachineItemHandler(inputCount + recipeMap.mOutputItemsCount, inputCount, onChanged);
        this.potionInput = potionCount > 0 ? new PotionInput(onChanged) : null;
        if (potionCount > 0)
            this.items.setInputFilter(stack -> !(stack.getItem() instanceof net.minecraft.world.item.PotionItem));
        this.tanksInput = new FluidTankGT[recipeMap.mInputFluidCount];
        this.tanksOutput = new FluidTankGT[recipeMap.mOutputFluidCount];
        for (int i = 0; i < tanksInput.length; i++) {
            tanksInput[i] = new FluidTankGT(originalFluidBuffers ? MachineFluidRules.INPUT_BASE_CAPACITY : 8000)
                    .setAdjustableCapacity(recipeMap.mMinInputTankSizes,
                            originalFluidBuffers ? MachineFluidRules.inputCapacityMultiplier(parallelLimit) : 1)
                    .setOnChanged(onChanged);
        }
        for (int i = 0; i < tanksOutput.length; i++) {
            tanksOutput[i] = new FluidTankGT(originalFluidBuffers ? Long.MAX_VALUE : 8000).setOnChanged(onChanged);
        }
    }

    /** Saved capacities are not machine settings. Keep excess contents available for extraction. */
    public void restoreConfiguredTankCapacities() {
        if (!originalFluidBuffers) return;
        for (var tank : tanksInput) tank.setCapacityPreservingContents(MachineFluidRules.INPUT_BASE_CAPACITY);
        for (var tank : tanksOutput) tank.setCapacityPreservingContents(Long.MAX_VALUE);
    }

    public MachineItemHandler items() { return items; }
    public MachineItemHandler potionInput() { return potionInput; }
    public FluidTankGT[] tanksInput() { return tanksInput; }
    public FluidTankGT[] tanksOutput() { return tanksOutput; }
    public int inputCount() { return inputCount; }
    public int potionCount() { return potionCount; }

    /** Ordinary input slots followed by the dedicated potion slots, in matching order. */
    public List<ItemStack> matchingInputItems() {
        var result = inputItems();
        if (potionInput != null) result.add(potionInput.getStackInSlot(0));
        return result;
    }

    /** Matching inputs followed by the output slots, used for the recipe lookup's empty-output test. */
    public List<ItemStack> matchingSlots() {
        var result = matchingInputItems();
        for (int i = inputCount; i < items.getSlots(); i++) result.add(items.getStackInSlot(i));
        return result;
    }

    /** Applies a consumption plan produced from {@link #matchingInputItems()}. */
    public void applyRemainingItems(List<ItemStack> remaining) {
        for (int i = 0; i < inputCount; i++) items.setStackInSlot(i, remaining.get(i));
        if (potionInput != null) potionInput.setStackInSlot(0, remaining.get(inputCount));
    }

    public boolean hasItemInputSpace() {
        if (potionInput != null && potionInput.getStackInSlot(0).isEmpty()) return true;
        for (int i = 0; i < items.inputCount(); i++) {
            var stack = items.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < Math.min(stack.getMaxStackSize(), items.getSlotLimit(i))) return true;
        }
        return false;
    }

    public boolean hasItemOutput() {
        for (int i = items.inputCount(); i < items.getSlots(); i++)
            if (!items.getStackInSlot(i).isEmpty()) return true;
        return false;
    }

    public boolean hasFluidInputSpace() {
        for (var tank : tanksInput) if (tank.isEmpty() || tank.getAmount() < tank.capacity()) return true;
        return false;
    }

    public boolean hasFluidOutput() {
        for (var tank : tanksOutput) if (!tank.isEmpty()) return true;
        return false;
    }

    public List<ItemStack> inputItems() {
        var result = new ArrayList<ItemStack>(inputCount);
        for (int i = 0; i < inputCount; i++) result.add(items.getStackInSlot(i));
        return result;
    }

    public List<FluidStack> inputFluids() {
        var result = new ArrayList<FluidStack>(tanksInput.length);
        for (var tank : tanksInput) result.add(tank.getFluidInTank(0));
        return result;
    }

    public List<FluidStack> outputTankFluids() {
        var result = new ArrayList<FluidStack>(tanksOutput.length);
        for (var tank : tanksOutput) result.add(tank.getFluidInTank(0));
        return result;
    }

    public static Iterable<FluidStack> contents(FluidTankGT[] tanks) {
        if (tanks == null) return Collections.emptyList();
        FluidStack[] list = new FluidStack[tanks.length];
        for (int i = 0; i < tanks.length; i++) list[i] = tanks[i].getFluid();
        return Arrays.asList(list);
    }

    @Override
    public int getTanks() { return tanksInput.length + tanksOutput.length; }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        if (tank < tanksInput.length) return tanksInput[tank].getFluid();
        int outIdx = tank - tanksInput.length;
        return outIdx < tanksOutput.length ? tanksOutput[outIdx].getFluid() : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        if (tank < tanksInput.length) return tanksInput[tank].getCapacity();
        int outIdx = tank - tanksInput.length;
        return outIdx < tanksOutput.length ? tanksOutput[outIdx].getCapacity() : 0;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return tank < tanksInput.length;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        for (int i = 0; i < tanksInput.length; i++) {
            if (tanksInput[i].isFluidValid(resource)) {
                int filled = tanksInput[i].fill(resource, action);
                if (filled > 0) return filled;
            }
        }
        return 0;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        for (int i = 0; i < tanksOutput.length; i++) {
            FluidStack drained = tanksOutput[i].drain(resource, action);
            if (!drained.isEmpty()) return drained;
        }
        return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        for (int i = 0; i < tanksOutput.length; i++) {
            FluidStack drained = tanksOutput[i].drain(maxDrain, action);
            if (!drained.isEmpty()) return drained;
        }
        return FluidStack.EMPTY;
    }

    /** One-slot input that only accepts vanilla potion items. */
    private static final class PotionInput extends MachineItemHandler {
        PotionInput(Runnable onChanged) { super(1, 1, onChanged); }

        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && stack.getItem() instanceof net.minecraft.world.item.PotionItem;
        }

        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }
    }
}
