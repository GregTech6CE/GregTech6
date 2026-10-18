package com.gregtech.gregtech.api.addon.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/** The placed machine a behavior reads and writes. Valid during the callbacks on {@link MachineBehavior}. */
public interface MachineBehaviorContext {
    Level level();

    BlockPos pos();

    Direction facing();

    int tier();

    int itemSlots();

    ItemStack item(int slot);

    void setItem(int slot, ItemStack stack);

    /** Puts {@code stack} into output slots, then slots that allow either role. Returns what did not fit. */
    ItemStack insertOutput(ItemStack stack);

    int tanks();

    FluidStack fluid(int tank);

    int capacity(int tank);

    /**
     * Stores {@code stack} in the tank when the tank is empty or already holds that fluid.
     * Returns how much of {@code stack} was stored. This does not ask {@link MachineBehavior#canFill}.
     */
    int fill(int tank, FluidStack stack, boolean simulate);

    /** Removes up to {@code amount} mB. Returns the fluid that left the tank. */
    FluidStack drain(int tank, int amount, boolean simulate);

    /** Energy stored for this tick, in the machine's energy unit. */
    long energy();

    /** Removes {@code amount} from {@link #energy()}. Returns false when there is less than that. */
    boolean consume(long amount);

    boolean toggle(int index);

    void setToggle(int index, boolean on);

    float progress();

    /** Fraction from 0 to 1 shown on the progress arrow. */
    void progress(float fraction);

    /** Lights the working face for this tick. */
    void working(boolean working);

    BehaviorState state();
}
