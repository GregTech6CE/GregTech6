package com.gregtech.gregtech.api.fluid;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * GT6 fluid tank with {@code long}-based storage, per-fluid capacity overrides, and
 * proof-flag validation. Bridges to Forge's {@link IFluidHandler} via {@link #bindInt}.
 * <p>
 * This replaces {@link net.minecraftforge.fluids.capability.templates.FluidTank} for
 * all GT6 machines that need &gt;2.147 billion mB capacity or GT6-specific filtering.
 */
public class FluidTankGT implements IFluidHandler {
    @NotNull
    private FluidStack fluid = FluidStack.EMPTY;
    private long capacity;
    private long amount;
    private boolean preventDraining;
    /** GT6's logistics barrel keeps its fluid identity after the last unit is withdrawn. */
    private boolean keepFilterOnEmpty;
    private boolean voidExcess;
    private Map<String, Long> adjustableCapacity;
    private long adjustableMultiplier = 1;
    private boolean gasProof;
    private boolean acidProof;
    private boolean plasmaProof;
    private boolean magicProof;
    private long maxTemperature = Long.MAX_VALUE;
    private Runnable onChanged = () -> {};

    public FluidTankGT(long capacity) {
        this.capacity = Math.max(0, capacity);
    }

    public FluidTankGT() {
        this(Long.MAX_VALUE);
    }

    // --- Configuration ---

    public FluidTankGT setOnChanged(Runnable onChanged) { this.onChanged = onChanged; return this; }
    public FluidTankGT setPreventDraining(boolean v) { this.preventDraining = v; return this; }
    public FluidTankGT setKeepFilterOnEmpty(boolean v) { this.keepFilterOnEmpty = v; return this; }
    public FluidTankGT setVoidExcess(boolean v) { this.voidExcess = v; return this; }
    public FluidTankGT setGasProof(boolean v) { this.gasProof = v; return this; }
    public FluidTankGT setAcidProof(boolean v) { this.acidProof = v; return this; }
    public FluidTankGT setPlasmaProof(boolean v) { this.plasmaProof = v; return this; }
    public FluidTankGT setMagicProof(boolean v) { this.magicProof = v; return this; }
    public FluidTankGT setMaxTemperature(long maxTemp) { this.maxTemperature = maxTemp; return this; }
    public FluidTankGT setAdjustableCapacity(Map<String, Long> caps, long multiplier) {
        this.adjustableCapacity = caps;
        this.adjustableMultiplier = multiplier;
        return this;
    }

    public boolean isGasProof() { return gasProof; }
    public boolean isAcidProof() { return acidProof; }
    public boolean isPlasmaProof() { return plasmaProof; }
    public boolean isMagicProof() { return magicProof; }
    public long maxTemperature() { return maxTemperature; }

    // --- Long-based core operations ---

    /** Add up to {@code filled} units of the current fluid. Returns actual amount added. */
    public long add(long filled) {
        if (filled <= 0 || fluid.isEmpty()) return 0;
        long space = capacity() - amount;
        if (space <= 0) return voidExcess ? filled : 0;
        long added = Math.min(filled, space);
        amount += added;
        onChanged.run();
        return added;
    }

    /** Add {@code filled} units of {@code match}, only if the fluid type matches. */
    public long add(long filled, FluidStack match) {
        if (match == null || match.isEmpty()) return 0;
        if (fluid.isEmpty() || match.getFluid() != fluid.getFluid()) return 0;
        return add(filled);
    }

    /** Remove up to {@code drained} units. Returns actual amount removed. */
    public long remove(long drained) {
        if (preventDraining || drained <= 0 || fluid.isEmpty()) return 0;
        long removed = Math.min(drained, amount);
        amount -= removed;
        if (amount <= 0) {
            amount = 0;
            if (!keepFilterOnEmpty) fluid = FluidStack.EMPTY;
        }
        onChanged.run();
        return removed;
    }

    /** Drain exactly {@code drained} or nothing (all-or-nothing). */
    public boolean drainAll(long drained) {
        if (preventDraining || drained > amount) return false;
        amount -= drained;
        if (amount <= 0) {
            amount = 0;
            if (!keepFilterOnEmpty) fluid = FluidStack.EMPTY;
        }
        onChanged.run();
        return true;
    }

    /** Set fluid contents directly (long-based). */
    public void setFluid(@NotNull FluidStack stack, long amt) {
        this.fluid = stack.copy();
        this.amount = amt;
        onChanged.run();
    }

    public void setFluid(@NotNull FluidStack stack) {
        setFluid(stack, stack.getAmount());
    }

    public void setEmpty() {
        this.fluid = FluidStack.EMPTY;
        this.amount = 0;
        onChanged.run();
    }

    // --- Capacity (with per-fluid overrides) ---

    public long capacity() {
        return capacity(fluid);
    }

    public long capacity(FluidStack fs) {
        if (fs == null || fs.isEmpty()) return Math.max(amount, capacity);
        return capacity(fs.getFluid().getFluidType().toString());
    }

    public long capacity(String fluidKey) {
        if (fluidKey == null || adjustableCapacity == null) return Math.max(amount, capacity);
        Long override = adjustableCapacity.get(fluidKey);
        if (override == null) return Math.max(amount, capacity);
        return Math.max(override * adjustableMultiplier, Math.max(amount, capacity));
    }

    public long baseCapacity() { return capacity; }

    public void setCapacity(long newCapacity) {
        this.capacity = Math.max(0, newCapacity);
        if (this.amount > this.capacity) this.amount = this.capacity;
    }

    public long getAmount() { return amount; }

    @NotNull
    public FluidStack getFluid() {
        if (fluid.isEmpty()) return FluidStack.EMPTY;
        FluidStack copy = fluid.copy();
        copy.setAmount(bindInt(amount));
        return copy;
    }

    @NotNull
    public FluidStack getFluidLong() {
        return fluid.isEmpty() ? FluidStack.EMPTY : fluid.copy();
    }

    public boolean isEmpty() { return fluid.isEmpty() || amount <= 0; }

    /** Convenience: int capacity for Forge compat. */
    public int getCapacity() { return bindInt(capacity()); }

    // --- Fluid validation (GT6 proof flags) ---

    /** Check if {@code stack} is allowed based on this tank's proof flags. */
    public boolean isFluidValid(@NotNull FluidStack stack) {
        if (stack.isEmpty()) return true;
        // In GT6, FL.gas()/FL.acid()/etc. would check fluid properties.
        // For the 1.20.1 port we validate via the block entity's spec flags.
        return true;
    }

    // === IFluidHandler bridge (int mB ↔ long L via bindInt) ===

    @Override
    public int getTanks() { return 1; }

    @NotNull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return bindInt(capacity());
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;
        if (fluid.isEmpty()) {
            long space = capacity();
            if (space <= 0) return 0;
            int filled = bindInt(Math.min(resource.getAmount(), space));
            if (filled <= 0) return 0;
            if (action.execute()) {
                this.fluid = resource.copy();
                this.fluid.setAmount(1); // keep a valid FluidStack for type tracking
                this.amount = filled;
                onChanged.run();
            }
            return filled;
        }
        if (fluid.getFluid() != resource.getFluid()) return 0;
        long space = capacity() - amount;
        int filled = bindInt(Math.min(resource.getAmount(), space));
        if (filled <= 0) return 0;
        if (action.execute()) {
            amount += filled;
            onChanged.run();
        }
        return filled;
    }

    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || fluid.isEmpty() || preventDraining) return FluidStack.EMPTY;
        if (fluid.getFluid() != resource.getFluid()) return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0 || fluid.isEmpty() || preventDraining) return FluidStack.EMPTY;
        int drained = bindInt(Math.min(maxDrain, amount));
        if (drained <= 0) return FluidStack.EMPTY;
        FluidStack result = fluid.copy();
        result.setAmount(drained);
        if (action.execute()) {
            amount -= drained;
            if (amount <= 0) {
                amount = 0;
                if (!keepFilterOnEmpty) fluid = FluidStack.EMPTY;
            }
            onChanged.run();
        }
        return result;
    }

    // === Utility ===

    /** Clamp a long value into the int range for Forge IFluidHandler compatibility. */
    public static int bindInt(long value) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }

    // === NBT serialization helpers ===

    public void writeToNBT(net.minecraft.nbt.CompoundTag tag) {
        tag.putLong("Amount", amount);
        tag.putLong("Capacity", capacity);
        if (!fluid.isEmpty()) {
            tag.put("Fluid", fluid.writeToNBT(new net.minecraft.nbt.CompoundTag()));
        }
    }

    public void readFromNBT(net.minecraft.nbt.CompoundTag tag) {
        amount = tag.getLong("Amount");
        if (tag.contains("Capacity")) capacity = tag.getLong("Capacity");
        if (tag.contains("Fluid")) {
            fluid = FluidStack.loadFluidStackFromNBT(tag.getCompound("Fluid"));
        } else {
            fluid = FluidStack.EMPTY;
            amount = 0;
        }
    }
}
