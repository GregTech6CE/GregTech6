package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.block.misc.CFoamBlock;
import com.gregtech.gregtech.item.behavior.ItemBehaviors.Consumable;
import com.gregtech.gregtech.item.behavior.ItemBehaviors.Outcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * GT6 {@code Behavior_Spray_Foam_Remover} ({@code Behavior_Spray_Foam_Remover.java:46-132}) — the
 * C-Foam Removal Spray.
 *
 * <h2>The original</h2>
 *
 * <p>{@code remove(...)} ({@code :95-115}) asks, in this order: an {@code ITileEntityFoamable} block
 * entity ({@code :99}), an {@code IBlockFoamable} block ({@code :101}), an IC2 cable's
 * {@code foamed} byte ({@code :103-111}), then the IC2 foam and reinforced-foam blocks
 * ({@code :113}). The first two answer with {@code 10} uses; a C-Foam <em>slab</em> costs {@code 5}
 * instead ({@code :101}, {@code aBlock instanceof BlockCFoamFresh && SIDES_VALID[mSide]}). Note the
 * gate is {@code aUses < 1} ({@code :96}), not {@code < 10} — a nearly empty can still sprays a whole
 * block and simply runs negative afterwards, which is how GT6's own code empties it.</p>
 *
 * <p>Both C-Foam blocks are foamable, and both answer {@code removeFoam} by setting the block to air
 * ({@code BlockCFoamFresh:115-117} for the wet one, {@code BlockCFoam:63-65} for the hardened one), so
 * the spray removes hardened foam too. The wet/hardened pair is in the port as
 * {@code gregtech:cfoam_fresh} / {@code gregtech:cfoam} ({@code CFoamBlock}, {@code GTDecorBlocks:96-97}).</p>
 *
 * <h2>Port limits</h2>
 *
 * <ul>
 *   <li><b>No slabs.</b> The port registers the two full C-Foam blocks only, so the {@code 5} use
 *       branch ({@code :101}) has nothing to apply to; {@link #SLAB_REMOVE_COST} records the number
 *       and the test pins it against the {@code 10} the port does use.</li>
 *   <li><b>No IC2 and no {@code ITileEntityFoamable}.</b> The IC2 foam/reinforced-foam/scaffold
 *       branches ({@code :113}, and {@code Behavior_Spray_Foam:129-130} on the placement side) and
 *       the machine-face foaming ({@code :99}) need content the port does not have.</li>
 * </ul>
 */
public final class BehaviorSprayFoamRemover {

    /** GT6 {@code MultiItemRandomTools:279}: {@code new Behavior_Spray_Foam_Remover(Spray_Empty, Spray_Foam_Remover_Used, Spray_Foam_Remover, 256)}. */
    public static final long USES = 256L;

    /** GT6 {@code Behavior_Spray_Foam_Remover:54}: {@code mUses = aUses * 10}. */
    public static final int USES_MULTIPLIER = 10;

    /** GT6 {@code :101}: a full C-Foam block costs this many uses. */
    public static final int REMOVE_COST = 10;

    /** GT6 {@code :101}: the same block as a slab costs this many. Nothing in the port is a foam slab. */
    public static final int SLAB_REMOVE_COST = 5;

    private BehaviorSprayFoamRemover() {}

    /** The port's can triple: the full id is registered first, the {@code _2} one is the used can. */
    public static Consumable cFoamRemovalSpray() {
        return new Consumable(ItemBehaviors.stack("gregtech:empty_spray_can"),
                ItemBehaviors.stack("gregtech:c_foam_removal_spray_2"),
                ItemBehaviors.stack("gregtech:c_foam_removal_spray"),
                USES * USES_MULTIPLIER);
    }

    /** GT6 {@code Behavior_Spray_Foam_Remover:58-93}: one click. */
    public static Outcome useOn(Level level, BlockPos pos, Direction side, @Nullable Player player, ItemStack stack,
                                float hitX, float hitY, float hitZ) {
        return ItemBehaviors.useSpray(level, pos, side, player, stack, cFoamRemovalSpray(),
                BehaviorSprayFoamRemover::remove, hitX, hitY, hitZ);
    }

    /**
     * GT6 {@code :95-115} for the port's content: wet and hardened C-Foam are set to air.
     *
     * @return {@link #REMOVE_COST} when a foam block was removed, {@code 0} otherwise
     */
    public static long remove(Level level, BlockPos pos, Direction side, long uses, @Nullable Player player,
                              ItemStack can, float hitX, float hitY, float hitZ) {
        if (uses < 1L) return 0L;                                     // :96
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CFoamBlock)) return 0L;      // :101, :113 replaced
        // GT6's removeFoam is `setBlockToAir` with WD's flag 3 on both foam blocks.
        if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) return 0L;
        return REMOVE_COST;
    }
}
