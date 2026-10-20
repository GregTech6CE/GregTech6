package com.gregtech.gregtech.api.addon.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.List;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

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

    /**
     * Adds as much of {@code amount} as the buffer can hold.
     * The buffer is the declared capacity, or the rated input maximum when that capacity is 0.
     * Returns how much was stored.
     */
    long generate(long amount);

    /** Stored-energy cap. Zero means the machine still drains its rated input each tick. */
    long capacity();

    /** Changes {@link #capacity()} for later ticks and for the cap at the end of this tick. */
    void setCapacity(long capacity);

    /**
     * Pushes {@code amount} packets of {@code size} out of the declared output faces.
     * Spends {@code size} for each packet a neighbor accepts and returns how many packets left.
     * A request above the declared maximum throws.
     */
    long emit(long size, long amount);

    boolean toggle(int index);

    void setToggle(int index, boolean on);

    float progress();

    /** Fraction from 0 to 1 shown on the progress arrow. */
    void progress(float fraction);

    /** Lights the working face for this tick. */
    void working(boolean working);

    /**
     * True when this machine has no layout, or the layout matched this tick.
     * A machine with a layout is false until that match succeeds.
     */
    boolean structureFormed();

    /** Energy parts bound by the formed layout. Empty when {@link #structureFormed()} is false. */
    List<EnergyPart> energyParts();

    BehaviorState state();

    /**
     * Breaks one block inside the declared range and puts its drops in the output slots.
     * Returns false when the range is closed, the position is outside it or is this machine,
     * the chunk is not loaded, the block is air or unbreakable, the drops do not fit,
     * a break event cancels it, or this tick already broke a block.
     */
    boolean breakBlock(BlockPos pos);

    /**
     * Takes item entities in the cube of {@code radius} around this machine.
     * {@code voidThem} discards them. Otherwise they move into the output slots, and an entity
     * that does not fit completely keeps what is left. Returns how many entities were taken.
     * A radius outside the declaration, or a declaration whose range is 0, takes nothing.
     */
    int collectItems(int radius, boolean voidThem);
}
