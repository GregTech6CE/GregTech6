package gregtech.content.blockentity.machine;

import gregtech.api.addon.machine.BehaviorMachine;
import gregtech.api.addon.platform.FluidAccess;
import gregtech.engine.fluid.FluidHazards;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

import java.util.List;

final class BehaviorMachineTanks implements FluidAccess {
    private final Tank[] tanks;
    private final Runnable markChanged;

    BehaviorMachineTanks(List<BehaviorMachine.FluidSlot> slots, Runnable markChanged) {
        tanks = new Tank[slots.size()];
        for (int i = 0; i < tanks.length; i++) tanks[i] = new Tank(slots.get(i));
        this.markChanged = markChanged;
    }

    Tank[] entries() { return tanks; }

    @Override public int tanks() { return tanks.length; }
    @Override public FluidStack snapshot(int tank) { return tanks[tank].copy(); }
    @Override public int capacity(int tank) { return tanks[tank].capacity; }

    @Override
    public int fill(int tank, FluidStack stack, boolean simulate) {
        int taken = tanks[tank].fill(stack, simulate);
        if (!simulate && taken > 0) markChanged.run();
        return taken;
    }

    @Override
    public FluidStack drain(int tank, int amount, boolean simulate) {
        FluidStack drained = tanks[tank].drain(amount, simulate);
        if (!simulate && !drained.isEmpty()) markChanged.run();
        return drained;
    }

    int receiveCompressedGas(FluidStack stack) {
        if (stack == null || stack.isEmpty() || !FluidHazards.isGas(stack.getFluid())) return 0;
        FluidStack remaining = stack.copy();
        int stored = 0;
        for (Tank tank : tanks) {
            if (remaining.isEmpty()) break;
            int taken = tank.fill(remaining, true);
            if (taken <= 0) continue;
            tank.fill(remaining, false);
            stored += taken;
            remaining.setAmount(remaining.getAmount() - taken);
        }
        if (stored > 0) markChanged.run();
        return stored;
    }

    FluidStack extractCompressedGas(int amount, boolean execute) {
        if (amount <= 0) return FluidStack.EMPTY;
        for (Tank tank : tanks) {
            FluidStack preview = tank.drain(amount, true);
            if (preview.isEmpty() || !FluidHazards.isGas(preview.getFluid())) continue;
            if (!execute) return preview;
            FluidStack taken = tank.drain(amount, false);
            if (!taken.isEmpty()) markChanged.run();
            return taken;
        }
        return FluidStack.EMPTY;
    }

    void write(CompoundTag tag) {
        ListTag fluids = new ListTag();
        for (Tank tank : tanks) {
            CompoundTag row = new CompoundTag();
            ResourceLocation name = BuiltInRegistries.FLUID.getKey(tank.fluid);
            row.putString("Fluid", name == null ? "" : name.toString());
            row.putInt("Amount", tank.amount);
            fluids.add(row);
        }
        tag.put("gt.fluids", fluids);
    }

    void read(CompoundTag tag) {
        ListTag fluids = tag.getList("gt.fluids", Tag.TAG_COMPOUND);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].amount = 0;
            tanks[i].fluid = Fluids.EMPTY;
            if (i >= fluids.size()) continue;
            CompoundTag row = fluids.getCompound(i);
            ResourceLocation name = ResourceLocation.tryParse(row.getString("Fluid"));
            Fluid fluid = name == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(name);
            int amount = row.getInt("Amount");
            if (fluid != null && fluid != Fluids.EMPTY && amount > 0) {
                tanks[i].fluid = fluid;
                tanks[i].amount = Math.min(tanks[i].capacity, amount);
            }
        }
    }

    static final class Tank {
        final int capacity;
        Fluid fluid = Fluids.EMPTY;
        int amount;

        Tank(BehaviorMachine.FluidSlot slot) { this.capacity = slot.capacity(); }

        FluidStack copy() {
            return amount <= 0 || fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, amount);
        }

        int fill(FluidStack stack, boolean simulate) {
            if (stack == null || stack.isEmpty()) return 0;
            if (amount > 0 && fluid != stack.getFluid()) return 0;
            int taken = Math.min(capacity - amount, stack.getAmount());
            if (taken <= 0) return 0;
            if (!simulate) {
                fluid = stack.getFluid();
                amount += taken;
            }
            return taken;
        }

        FluidStack drain(int requested, boolean simulate) {
            if (amount <= 0 || requested <= 0 || fluid == Fluids.EMPTY) return FluidStack.EMPTY;
            int taken = Math.min(amount, requested);
            Fluid drained = fluid;
            if (!simulate) {
                amount -= taken;
                if (amount == 0) fluid = Fluids.EMPTY;
            }
            return new FluidStack(drained, taken);
        }
    }
}
