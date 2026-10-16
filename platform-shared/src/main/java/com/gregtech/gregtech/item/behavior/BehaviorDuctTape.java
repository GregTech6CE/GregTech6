package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.item.behavior.ItemBehaviors.Consumable;
import com.gregtech.gregtech.item.behavior.ItemBehaviors.Outcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * GT6 {@code Behavior_Duct_Tape} ({@code Behavior_Duct_Tape.java:41-116}): the three tape rolls.
 *
 * <h2>What the original does</h2>
 *
 * <ul>
 *   <li>the click is refused outright while the stack is stacked, while the client side runs, or
 *       when the player may not edit the block ({@code :55});</li>
 *   <li>a full roll turns into its used roll and starts at {@code mUses}
 *       ({@code :63-67}) — and that conversion sticks even when the click itself does nothing,
 *       because the counter is written back unconditionally at {@code :76-78};</li>
 *   <li>the used roll calls {@code tape(...)} ({@code :68-75}) and subtracts what it reports;
 *       at zero uses the stack either turns into the empty item or is consumed
 *       ({@code :80-87});</li>
 *   <li>{@code tape(...)} ({@code :91-99}) does nothing at all when the clicked face is obstructed
 *       — {@code WD.obstructed}, i.e. the player cannot reach through the block behind it — and
 *       otherwise forwards to {@code IBlockToolable.Util.onToolClick(TOOL_ducttape, aUses, mQuality,
 *       ...)}, which is how a GT6 mass storage gets sealed
 *       ({@code MultiTileEntityMassStorage:184-194}: it answers with {@code max(100, stackSize)}
 *       damage, i.e. "this many uses of tape").</li>
 * </ul>
 *
 * <h2>The port's three rolls</h2>
 *
 * <p>The port registers the same six items ({@code GTMultiItemsGen.ENTRIES:140-145}: {@code tape} /
 * {@code tape_2}, {@code duct_tape} / {@code duct_tape_2} and the BrainTech roll plus its {@code _2}
 * variant — the registered-first one is the full roll, the {@code _2} one the used roll, matching
 * {@code MultiItemRandomTools:555-556}, {@code :564-565} and {@code :573-574}). The qualities and
 * use counts are the original's constructor arguments:
 * {@link #TAPE_QUALITY}/{@link #TAPE_USES} ({@code :559}),
 * {@link #DUCT_TAPE_QUALITY}/{@link #DUCT_TAPE_USES} ({@code :568}) and
 * {@link #BRAIN_TAPE_QUALITY}/{@link #BRAIN_TAPE_USES} ({@code :577}).</p>
 *
 * <p>{@code TOOL_ducttape} has exactly one implementor in GT6: Mass Storage.
 * The port's {@code MassStorageBlockEntity} implements {@link Tapeable} and
 * keeps contents inside the harvested item only while taped.</p>
 */
public final class BehaviorDuctTape {

    /** GT6 {@code MultiItemRandomTools:559}: {@code new Behavior_Duct_Tape(null, Tape_Used, Tape, 0, 10000)}. */
    public static final long TAPE_QUALITY = 0L, TAPE_USES = 10000L;

    /** GT6 {@code MultiItemRandomTools:568}: {@code (..., Duct_Tape_Used, Duct_Tape, 1, 100000)}. */
    public static final long DUCT_TAPE_QUALITY = 1L, DUCT_TAPE_USES = 100000L;

    /** GT6 {@code MultiItemRandomTools:577}: {@code (..., Brain_Tape_Used, Brain_Tape, 2, 10000000)}. */
    public static final long BRAIN_TAPE_QUALITY = 2L, BRAIN_TAPE_USES = 10000000L;

    /** Port item ids of the three used rolls (the {@code _2} half of each registration pair). */
    public static final String TAPE_USED_ITEM = "gregtech:tape_2", TAPE_FULL_ITEM = "gregtech:tape";
    public static final String DUCT_TAPE_USED_ITEM = "gregtech:duct_tape_2", DUCT_TAPE_FULL_ITEM = "gregtech:duct_tape";
    public static final String BRAIN_TAPE_USED_ITEM = "gregtech:braintech_aerospace_advanced_reinforced_duct_tape_fal_84_2";
    public static final String BRAIN_TAPE_FULL_ITEM = "gregtech:braintech_aerospace_advanced_reinforced_duct_tape_fal_84";

    private BehaviorDuctTape() {}

    /**
     * A block (or block entity) that can be repaired with duct tape — the port's half of GT6's
     * {@code IBlockToolable.onToolClick(TOOL_ducttape, ...)}.
     *
     * <p>GT6 passes the tape's remaining uses and its quality in, and takes the consumed uses back
     * out; the one implementor answers {@code max(100, slot(1).stackSize)}
     * ({@code MultiTileEntityMassStorage:193}), i.e. "one use per item I have to contain".</p>
     */
    public interface Tapeable {
        /**
         * @param side     the clicked face
         * @param player   the player, or {@code null} for GT6's auto-tool case
         * @param tape     the tape stack that is being spent
         * @param uses     the uses the tape still has
         * @param quality  GT6's {@code mQuality} (0 tape, 1 duct tape, 2 brain tape)
         * @param sneaking whether the player sneaks
         * @return the uses this target consumed, {@code 0} when it cannot be taped
         */
        long onTape(Level level, BlockPos pos, Direction side, @Nullable Player player, ItemStack tape,
                    long uses, long quality, boolean sneaking, float hitX, float hitY, float hitZ);
    }

    /** The consumable of a roll, chosen by GT6's quality number. */
    @Nullable
    public static Consumable consumable(long quality) {
        if (quality == TAPE_QUALITY) {
            return new Consumable(ItemStack.EMPTY, ItemBehaviors.stack(TAPE_USED_ITEM),
                    ItemBehaviors.stack(TAPE_FULL_ITEM), TAPE_USES);
        }
        if (quality == DUCT_TAPE_QUALITY) {
            return new Consumable(ItemStack.EMPTY, ItemBehaviors.stack(DUCT_TAPE_USED_ITEM),
                    ItemBehaviors.stack(DUCT_TAPE_FULL_ITEM), DUCT_TAPE_USES);
        }
        if (quality == BRAIN_TAPE_QUALITY) {
            return new Consumable(ItemStack.EMPTY, ItemBehaviors.stack(BRAIN_TAPE_USED_ITEM),
                    ItemBehaviors.stack(BRAIN_TAPE_FULL_ITEM), BRAIN_TAPE_USES);
        }
        return null;
    }

    /** The roll a stack is, by item identity, or {@code null} when it is none of the six. */
    @Nullable
    public static Consumable consumableOf(ItemStack stack) {
        for (long quality : new long[]{TAPE_QUALITY, DUCT_TAPE_QUALITY, BRAIN_TAPE_QUALITY}) {
            Consumable can = consumable(quality);
            if (can != null && can.matches(stack)) return can;
        }
        return null;
    }

    /**
     * GT6 {@code Behavior_Duct_Tape:54-89} for one click: the tape roll plus the block it targets.
     *
     * <p>The order is the original's: the stack-size and edit gates first ({@code :55}), then the
     * full → used conversion ({@code :63-67}), then the target lookup and {@link #tape}, then the
     * counter write-back and the empty/consume step ({@code :76-87}). Note that a refused click
     * still returns the converted stack, exactly like the original, which writes the new counter
     * outside the {@code if (tUsed > 0)} block.</p>
     *
     * @param player the clicking player; a {@code null} player is GT6's auto-tool case and never
     *               consumes tape ({@code :92} short-circuits the same way)
     * @return whether the click did something, plus the stack to store back
     */
    public static Outcome useOn(Level level, BlockPos pos, Direction side, @Nullable Player player,
                                ItemStack stack, float hitX, float hitY, float hitZ) {
        return useOn(level, pos, side, target(level, pos), player, stack, hitX, hitY, hitZ);
    }

    /**
     * The same click with the target already resolved — GT6's shape, where {@code tape(...)} is handed
     * the coordinates and {@code IBlockToolable.Util.onToolClick} resolves the block itself. Splitting
     * it out keeps the whole chain (conversion, counter, empty step) independently testable.
     */
    public static Outcome useOn(Level level, BlockPos pos, Direction side, @Nullable Tapeable target,
                                @Nullable Player player, ItemStack stack, float hitX, float hitY, float hitZ) {
        if (level.isClientSide) return Outcome.refused(stack);
        if (stack.getCount() != 1) return Outcome.refused(stack);                       // :55
        if (!ItemBehaviors.mayEdit(level, player, pos)) return Outcome.refused(stack);   // :55
        Consumable can = consumableOf(stack);
        if (can == null) return Outcome.refused(stack);

        ItemStack prepared = can.prepared(stack);                                       // :63-67
        boolean sneaking = player != null && player.isShiftKeyDown();
        long spent = tape(level, pos, side, target, player, prepared, can.remaining(prepared),
                quality(can), sneaking, hitX, hitY, hitZ);
        if (spent <= 0) return Outcome.refused(prepared);                               // :76-78 keeps the counter
        return Outcome.acted(can.spent(prepared, spent));                               // :80-87
    }

    /**
     * GT6 {@code Behavior_Duct_Tape.tape(...)} ({@code :91-99}): hand the click to the block, but
     * only when the clicked face is not obstructed.
     *
     * <p>{@code aPlayer == null}, an invalid side and {@code FakePlayer} all take the original's
     * "go ahead" branch; the port has no {@code FakePlayer} for an item click and {@link Direction}
     * cannot be invalid, so only the obstructed test and the {@code null} player remain. A
     * {@code null} player still reaches {@code onTape} (GT6's {@code onToolClick} tolerates
     * {@code null} and every machine-side caller passes it), it is only the per-item bookkeeping in
     * {@link #useOn} that refuses.</p>
     *
     * <p>The target is a parameter rather than a lookup, which is the same shape GT6 has: its
     * {@code tape} receives the already-resolved {@code aPlayer} and hands the coordinates to
     * {@code IBlockToolable.Util.onToolClick}, which resolves the block itself. Passing it in keeps
     * the "obstructed faces are never taped" rule independently testable.</p>
     *
     * @param target the block that would be taped, or {@code null} when none is there
     * @param uses   the uses the tape has left
     * @return the uses the target consumed, {@code 0} when nothing happened
     */
    public static long tape(Level level, BlockPos pos, Direction side, @Nullable Tapeable target,
                            @Nullable Player player, ItemStack stack, long uses, long quality, boolean sneaking,
                            float hitX, float hitY, float hitZ) {
        if (ItemBehaviors.obstructed(level, pos, side)) return 0L;                      // :92
        if (target == null) return 0L;
        return Math.max(0L, target.onTape(level, pos, side, player, stack, uses, quality, sneaking,
                hitX, hitY, hitZ));
    }

    /** The tapeable block entity at a position, or the block when the entity is not one. */
    @Nullable
    public static Tapeable target(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof Tapeable be) return be;
        return level.getBlockState(pos).getBlock() instanceof Tapeable block ? block : null;
    }

    private static long quality(Consumable can) {
        if (can.uses() == TAPE_USES) return TAPE_QUALITY;
        if (can.uses() == DUCT_TAPE_USES) return DUCT_TAPE_QUALITY;
        return BRAIN_TAPE_QUALITY;
    }
}
