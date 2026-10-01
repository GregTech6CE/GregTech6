package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.blockentity.CFoamBlockEntity;
import com.gregtech.gregtech.item.behavior.ItemBehaviors.Consumable;
import com.gregtech.gregtech.item.behavior.ItemBehaviors.Outcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * GT6 {@code Behavior_Spray_Foam_Hardener} ({@code Behavior_Spray_Foam_Hardener.java:46-133}) — the
 * Hardening Spray.
 *
 * <h2>The original</h2>
 *
 * <p>{@code harden(...)} ({@code :95-116}) tries the same four routes as the removal spray and in the
 * same order — {@code ITileEntityFoamable} ({@code :99}), {@code IBlockFoamable} ({@code :101}), the
 * IC2 cable's {@code foamed} byte ({@code :103-111}), the two IC2 foam blocks ({@code :113-114}) —
 * and reports {@code 10} uses for a full block, {@code 5} for a C-Foam slab
 * ({@code :101}, {@code aBlock instanceof BlockCFoamFresh && SIDES_VALID[mSide]}).</p>
 *
 * <p>Only the wet block dries: {@code BlockCFoamFresh.dryFoam} swaps in {@code BlocksGT.CFoam} with
 * the same metadata ({@code BlockCFoamFresh:110-112}), while {@code BlockCFoam.dryFoam} answers
 * {@code F} ({@code BlockCFoam:57-60}), so an already hardened block costs nothing.</p>
 *
 * <h2>Why this one is a clean port</h2>
 *
 * <p>The port's wet foam already owns exactly that operation: {@link CFoamBlockEntity#dry()} is
 * documented as GT6's {@code dryFoam} and swaps the block for {@code gregtech:cfoam}
 * ({@code CFoamBlockEntity:43-54}), reporting whether it did. GT6's {@code MultiTileEntityCFoam}
 * reaches it through {@code ITileEntityFoamable} ({@code MultiTileEntityCFoam:118-124}), which is the
 * first branch {@code :99} tests — so the port's hardener is that branch and nothing else, and a
 * hardened block simply has no block entity to find ({@code :101}/{@code :57-60} answering
 * {@code F}).</p>
 *
 * <p>The {@code aUses < 1} gate ({@code :96}) is the original's, not {@code < 10}: see
 * {@link BehaviorSprayFoamRemover} for why.</p>
 */
public final class BehaviorSprayFoamHardener {

    /** GT6 {@code MultiItemRandomTools:288}: {@code (Spray_Empty, Spray_Foam_Hardener_Used, Spray_Foam_Hardener, 256)}. */
    public static final long USES = 256L;

    /** GT6 {@code Behavior_Spray_Foam_Hardener:54}: {@code mUses = aUses * 10}. */
    public static final int USES_MULTIPLIER = 10;

    /** GT6 {@code :99,101}: hardening a full C-Foam block costs this many uses. */
    public static final int HARDEN_COST = 10;

    /** GT6 {@code :101}: the same block as a slab costs this many. Nothing in the port is a foam slab. */
    public static final int SLAB_HARDEN_COST = 5;

    private BehaviorSprayFoamHardener() {}

    /** The port's can triple: the full id is registered first, the {@code _2} one is the used can. */
    public static Consumable hardeningSpray() {
        return new Consumable(ItemBehaviors.stack("gregtech:empty_spray_can"),
                ItemBehaviors.stack("gregtech:hardening_spray_2"),
                ItemBehaviors.stack("gregtech:hardening_spray"),
                USES * USES_MULTIPLIER);
    }

    /** GT6 {@code Behavior_Spray_Foam_Hardener:58-93}: one click. */
    public static Outcome useOn(Level level, BlockPos pos, Direction side, @Nullable Player player, ItemStack stack,
                                float hitX, float hitY, float hitZ) {
        return ItemBehaviors.useSpray(level, pos, side, player, stack, hardeningSpray(),
                BehaviorSprayFoamHardener::harden, hitX, hitY, hitZ);
    }

    /**
     * GT6 {@code :95-116} for the port's content: the wet foam dries.
     *
     * @return {@link #HARDEN_COST} when the foam dried, {@code 0} when it was already hard or not
     *         foam at all
     */
    public static long harden(Level level, BlockPos pos, Direction side, long uses, @Nullable Player player,
                              ItemStack can, float hitX, float hitY, float hitZ) {
        if (uses < 1L) return 0L;                                             // :96
        if (!(level.getBlockEntity(pos) instanceof CFoamBlockEntity foam)) return 0L; // :99 / :101→F
        return foam.dry() ? HARDEN_COST : 0L;                                 // :99
    }
}
