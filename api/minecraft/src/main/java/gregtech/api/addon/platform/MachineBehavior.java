/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import gregtech.api.addon.recipe.CrucibleContent;
import gregtech.api.addon.recipe.CrucibleMold;
import gregtech.api.energy.EnergyType;
import gregtech.api.machine.MachineFace;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import org.jspecify.annotations.Nullable;

/**
 * What one placed behavior machine does. GregTech creates a new instance for every block entity.
 * {@link #acceptFluid} is the pipe and player fill path; override it when a fluid should be
 * stored as a different fluid, such as a concentrate.
 */
public interface MachineBehavior {
    void tick(MachineBehaviorContext context);

    /**
     * Items a player or a pipe may put in this slot. Output slots never call this.
     * This is the machine-wide policy; implement it even when {@link #canInsert(MachineBehaviorContext,
     * int, ItemStack, MachineFace)} is overridden for face-specific rules.
     */
    boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack);

    /**
     * Same check, plus the face the pipe used. The default calls {@link #canInsert} without a face.
     * A null face is the menu or an unsided handler. Override this when a face changes the answer;
     * the no-face method still has to return the machine-wide policy.
     */
    default boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack,
                              @Nullable MachineFace face) {
        return canInsert(context, slot, stack);
    }

    /**
     * Fluids this tank may hold. This is the machine-wide policy; implement it even when
     * {@link #canFill(MachineBehaviorContext, int, FluidStack, MachineFace)} is overridden.
     */
    boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack);

    /**
     * Same check, plus the face the pipe or container used. The default calls {@link #canFill} without a face.
     * A null face is the menu or an unsided handler. Override this when a face changes the answer;
     * the no-face method still has to return the machine-wide policy.
     */
    default boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack,
                            @Nullable MachineFace face) {
        return canFill(context, tank, stack);
    }

    /**
     * Whether this pushed packet may enter. GregTech asks from {@code doInject} after the face
     * check and the buffer check, on the main buffer and the auxiliary buffer. False refuses
     * that packet: it is not stored and it does not overload. The default accepts. A null face
     * means the caller did not name a side. The machine's own pull does not ask.
     */
    default boolean acceptsEnergy(MachineBehaviorContext context, EnergyType type, @Nullable MachineFace face,
                                  long size, long amount) {
        return true;
    }

    /**
     * How much of {@code stack} the tank takes. The default stores that fluid when {@link #canFill}
     * allows it and the tank is empty or already holds it.
     */
    default int acceptFluid(MachineBehaviorContext context, int tank, FluidStack stack, boolean simulate) {
        if (stack == null || stack.isEmpty() || !canFill(context, tank, stack)) return 0;
        return context.fluids().fill(tank, stack, simulate);
    }

    /**
     * The molten material this behavior currently exposes, or {@link CrucibleContent#EMPTY} when it
     * holds none. Reading it removes nothing; the crucible's own storage is unchanged. The default
     * exposes no content; crucibles override this method.
     */
    default CrucibleContent crucibleContent(MachineBehaviorContext context) {
        return CrucibleContent.EMPTY;
    }

    /**
     * Attempts to pour this behavior's molten material into an adjacent mold.
     *
     * <p>The supplied target is an SDK-safe view of the real mold. The behavior owns
     * its material inventory and must call {@link CrucibleMold#fillMold} for each
     * material it wants to offer, reducing its own content by the amount returned.
     * {@code crucibleSide} is the world face of this machine and {@code moldSide} is the
     * world face of the mold that receives the pour.</p>
     *
     * @return true when material was transferred
     */
    default boolean fillMoldAtSide(MachineBehaviorContext context, CrucibleMold mold,
                                   Direction crucibleSide, Direction moldSide) { return false; }

    /**
     * GT signal this machine emits toward {@code side}, in
     * {@link gregtech.api.signal.GtSignalCarrier#UNIT} units. The signal wire clamps
     * the result to {@code [0, 15 * UNIT]}. {@code side} is the direction from the wire toward
     * this block. The default emits nothing, and a machine only
     * becomes a signal source when its declaration sets {@code signalOutput(true)}.
     */
    default long gtSignal(MachineBehaviorContext context, Direction side) { return 0; }

    /** A player flipped this switch. The new value is already stored. */
    default void onToggle(MachineBehaviorContext context, int toggle) {}

    /** The block is about to drop its contents. Inventory and state items are dropped afterwards. */
    default void beforeBreak(MachineBehaviorContext context) {}

    /**
     * A player used this block with a recognized GregTech tool. GregTech asks before a wrench
     * rotates the block, on the side that received the click. Return true to consume the click:
     * the server then spends the usual tool-click durability, and the block does not rotate or open.
     * The default returns false. An unrecognized tool never calls this.
     */
    default boolean onTool(MachineBehaviorContext context, AddonTool tool, Player player, InteractionHand hand,
                           MachineFace face) { return false; }

    /**
     * A player used this block. Return true when the behavior handled it.
     * The default returns false and the machine menu opens. A recognized tool is asked first.
     * A wrench that was not handled, then a fluid container, come before this.
     */
    default boolean onUse(MachineBehaviorContext context, Player player, InteractionHand hand) { return false; }
}
