/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

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

/** The placed machine a behavior reads and writes. Valid during the callbacks on {@link MachineBehavior}. */
public interface MachineBehaviorContext {
    /** Unified snapshot reads and explicit writes for this machine's inventory. */
    default InventoryAccess inventoryAccess() {
        return BehaviorAccess.inventory(this::itemSlots, this::liveItem, this::setItem);
    }

    /** Unified snapshot reads and mB operations for this machine's tanks. */
    default FluidAccess fluidAccess() {
        return BehaviorAccess.fluids(this::tanks, this::fluid, this::tankCapacity, this::fill, this::drain);
    }

    Level level();

    BlockPos pos();

    Direction facing();

    int tier();

    int itemSlots();

    /**
     * Live inventory stack read used by {@link #inventoryAccess()}. Mutating it directly does not
     * guarantee save/synchronization notifications; use {@link #setItem(int, ItemStack)} for writes.
     * Addons should call {@link InventoryAccess#snapshot(int)} instead of this method.
     */
    ItemStack liveItem(int slot);

    void setItem(int slot, ItemStack stack);

    /** Puts {@code stack} into output slots, then slots that allow either role. Returns what did not fit. */
    ItemStack insertOutput(ItemStack stack);

    int tanks();

    FluidStack fluid(int tank);

    /** Capacity of tank {@code tank} in millibuckets. */
    int tankCapacity(int tank);

    /**
     * Stores {@code stack} in the tank when the tank is empty or already holds that fluid.
     * Returns how much of {@code stack} was stored. This does not ask {@link MachineBehavior#canFill}.
     */
    int fill(int tank, FluidStack stack, boolean simulate);

    /** Removes up to {@code amount} mB. Returns the fluid that left the tank. */
    FluidStack drain(int tank, int amount, boolean simulate);

    /** Energy operations with explicit packet/unit results and ownership of transferred energy. */
    default MachineEnergy energyAccess() { return MachineEnergy.of(this); }

    /**
     * Energy stored in the main input, in the machine's energy unit.
     * A machine that declares no main input returns 0. {@link #generate} and
     * {@link #emitPacketsFromFaces(long, long)} still move energy. The auxiliary buffer is
     * {@link #auxiliaryStored(String)}.
     */
    long mainInputStored();

    /** Removes {@code amount} from {@link #mainInputStored()}. Returns false when there is less than that, or when there is no main input. */
    boolean consume(long amount);

    /**
     * Adds as much of {@code amount} as the buffer can hold.
     * The buffer is the declared capacity, or the rated input maximum when that capacity is 0.
     * A machine with no main input holds one full output packet ({@code maxSize} times {@code maxAmount}).
     * Returns how much was stored.
     */
    long generate(long amount);

    /** Stored-energy cap. Zero means the machine still drains its rated input each tick. */
    long energyCapacity();

    /** Changes {@link #energyCapacity()} for later ticks and for the cap at the end of this tick. */
    void setCapacity(long capacity);

    /**
     * Pushes {@code amount} packets of {@code size} out of the declared output faces.
     * Spends {@code abs(size)} energy units for each packet a neighbor accepts.
     * Returns the number of accepted packets, not remaining packets or energy units.
     * A request above the declared maximum throws. Prefer {@link #energyAccess()} for a typed result.
     * A named bound part is {@link #moveUnitsToPart(String, long, long)}.
     */
    long emitPacketsFromFaces(long size, long amount);

    /**
     * Sends {@code amount} packets of {@code size} to the block entity at {@code target},
     * entering it through {@code side}. It uses the declared output type and packet limits,
     * spends {@code abs(size)} stored energy units for each packet the target accepts, and returns
     * the accepted packet count, not remaining packets or energy units. The declared output
     * faces are not used. A null {@code side} means no
     * side. A request above the declared maximum throws, as {@link #emitPacketsFromFaces(long, long)} does.
     * A target in an unloaded chunk, without a block entity, or with no energy receiver accepts
     * nothing. The target is looked up for this call only and is not kept.
     */
    long emitTo(BlockPos target, Direction side, long size, long amount);

    /**
     * Charges a bound two-way part from the controller channel and arms {@code amount}
     * packets of {@code size}. The part emits them from its own faces on its next server tick.
     * Returns energy units stored, not a packet count; a partial packet may fit.
     * This does not spend {@link #mainInputStored()}; the behavior keeps its own accounting and must
     * debit exactly these returned units from its source. Returns 0 when the part is missing, is not a two-way part,
     * or its output type differs from {@code type}. A request the part's output declaration
     * rejects throws.
     */
    long pushToPart(String part, String type, long size, long amount);

    /**
     * Moves up to {@code amount} packets of {@code size} from this machine's stored energy
     * into the named bound part and arms it. The part emits on its own tick. Returns energy
     * units moved out of this machine, not a packet count; a partial packet may fit.
     * This does not require the machine to declare output faces,
     * but a machine whose stored energy has a declared type only feeds a matching part.
     * A request the part's output declaration rejects throws.
     */
    long moveUnitsToPart(String part, long size, long amount);

    boolean toggle(int index);

    void setToggle(int index, boolean on);

    float progress();

    /** Sets the progress arrow fraction from 0 to 1. */
    void setProgress(float fraction);

    /** Lights the working face for this tick. */
    void working(boolean working);

    /**
     * True when this machine has no layout, or the layout matched this tick.
     * A machine with a layout is false until that match succeeds.
     */
    boolean structureFormed();

    /** Energy parts bound by the formed layout. Empty when {@link #structureFormed()} is false. */
    List<EnergyPart> energyParts();

    /**
     * How many copies of the repeat group {@code key} matched this tick.
     * Zero when the structure is not formed, the key is unknown, or the layout has no such group.
     */
    int structureRepeats(String key);

    /**
     * Addon parts bound by the formed layout, energy parts and functional parts together.
     * A casing is not included. Empty when {@link #structureFormed()} is false.
     * {@link #energyParts()} is the energy view of this same binding.
     */
    List<BoundPart> parts();

    /**
     * True when no bound part is a condition, or every bound condition part is satisfied.
     * An unformed structure has no bound parts, so this is true. The behavior tick still runs.
     */
    boolean conditionsMet();

    /**
     * Auxiliary buffer for {@code type}, in that energy's unit.
     * This is not {@link #mainInputStored()}. An unknown type, or a machine with no auxiliary input, is 0.
     */
    long auxiliaryStored(String type);

    /**
     * Removes {@code amount} from the auxiliary buffer of {@code type}.
     * Returns false when the type is not the auxiliary type or the buffer is short.
     */
    boolean consume(String type, long amount);

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

    /**
     * Stores {@code stack} in the compressed-gas receiver on {@code face}.
     * A single-block drum accepts on every side. A behavior machine accepts on its valve face.
     * Returns how many millibuckets that receiver stored.
     * A missing receiver, a pipe, or an ordinary tank accepts nothing.
     * This writes immediately. The behavior removes the fluid from its own tank.
     */
    int pushCompressedGas(Direction face, FluidStack stack);
}
