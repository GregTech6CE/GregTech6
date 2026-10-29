package gregtech.content.item.behavior;

//? if >=1.21.1 {
/*import gregtech.content.item.behavior.ManualToolBehaviorAccess.Outcome;
*///?} else {
import gregtech.content.item.behavior.ItemBehaviors.Outcome;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Remote activator binding and activation, ported from GT6 Behavior_Remote (GregTech-6 Team).
 *
 * <p>Each dimension stores up to 64 coordinates. Sneak-click toggles a loaded target; normal use
 * activates targets within 128 blocks on each axis. A target returns true to keep its binding,
 * or false to remove it after activation. Block entities are checked before plain blocks.
 *
 * <p>The dimension key uses the modern resource location; c/x/y/z indexed fields retain the
 * original marker layout. Numeric 1.7.10 dimension keys are not migrated here.
 *
 * <p>Manual binding and activation currently omit the original beeps/click. Automatic addCoords
 * already plays a success sound. Item adapters do not yet append the original range tooltip.
 */
public final class BehaviorRemote {

    /** GT6 {@code Behavior_Remote:56} (and {@code Behavior_Remote:95}): {@code tList.size() >= 64} per dimension. */
    public static final int MAX_COORDS = 64;

    /** GT6 {@code Behavior_Remote:80}: {@code Math.abs(...) <= 128} on every axis, from the player. */
    public static final int RANGE = 128;

    private BehaviorRemote() {
    }

    /** GT6's non-toggle binding path used by powered drills. A full remote is skipped. */
    public static boolean addCoords(ItemStack stack, Level level, BlockPos pos) {
        if (level.isClientSide) return false;
//? if >=1.21.1 {
        /*var coords = getCoords(gregtech.platform.neoforge.StackCustomData.read(stack), level);
*///?} else {
        var coords = getCoords(stack.getTag(), level);
//?}
        if (coords.size() >= MAX_COORDS) return false;
        if (coords.contains(pos)) return true;
        if (target(level, pos) == null) return false;
        coords.add(pos.immutable());
//? if >=1.21.1 {
        /*gregtech.platform.neoforge.StackCustomData.update(stack, tag -> setCoords(tag, level, coords));
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(),
*///?} else {
        setCoords(stack.getOrCreateTag(), level, coords);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.get(),
//?}
                net.minecraft.sounds.SoundSource.BLOCKS, .5F, 1F);
        return true;
    }

    /**
     * GT6's NBT key for one dimension: {@code "gt.remote.dim." + aDimension}
     * ({@code Behavior_Remote:113,123,133}).
     *
     * <p>GT6 appends the numeric dimension id; 1.20.1 has none, so the port appends the dimension's
     * {@code ResourceLocation} — see the class javadoc for that one deviation.</p>
     *
     * @return the key, e.g. {@code gt.remote.dim.minecraft:overworld}
     */

    public static String dimKey(Level level) {
        return "gt.remote.dim." + level.dimension().location();
    }

    /**
     * GT6 {@code Behavior_Remote.getCoords}: the coordinates bound to a stack in this
     * dimension.
     *
     * <p>The read loop is the original's, marker first: {@code i = -1} and
     * {@code while (compound.contains("c" + (++i)))}, so a list that lost its markers
     * (or one written by a different layout) stops at the first gap instead of reading garbage.</p>
     *
     * @param tag   the stack's tag; {@code null} reads as an empty list, like GT6's own
     *              {@code if (aNBT == null) return rList}
     * @param level the level whose dimension key is read
     * @return a fresh mutable list, empty when nothing is bound — the callers of this layer are expected
     *         to edit it and hand it to {@link #setCoords} (GT6's list is an {@code ArrayListNoNulls})
     */

    public static List<BlockPos> getCoords(@Nullable CompoundTag tag, Level level) {
        List<BlockPos> coords = new ArrayList<>();
        if (tag == null) return coords;
        CompoundTag dimension = tag.getCompound(dimKey(level));
        if (dimension.isEmpty()) return coords;
        int index = -1;
        while (dimension.contains("c" + (++index))) {
            coords.add(new BlockPos(dimension.getInt("x" + index), dimension.getInt("y" + index),
                    dimension.getInt("z" + index)));
        }
        return coords;
    }

    /**
     * GT6 {@code Behavior_Remote.setCoords}: replace this dimension's list.
     *
     * <p>Field for field the original's writer: index {@code i} from {@code 0}, a boolean
     * {@code "c" + i} that is only the end marker for {@link #getCoords}, then {@code "x"/"y"/"z" + i}.
     * An empty list removes the dimension's compound rather than storing an empty
     * one, so an item that has nothing bound carries no tag for that dimension.</p>
     *
     * @param tag   the tag to write into — the stack's own tag, which the caller stores back; must not
     *               be {@code null} (GT6 dereferences it as well)
     * @param level the level whose dimension key is written
     * @param coords the surviving coordinates; an empty list removes the entry
     */

    public static void setCoords(CompoundTag tag, Level level, List<BlockPos> coords) {
        String key = dimKey(level);
        if (coords.isEmpty()) {
            tag.remove(key);
            return;
        }
        CompoundTag dimension = new CompoundTag();
        for (int index = 0, size = coords.size(); index < size; index++) {
            BlockPos coord = coords.get(index);
            dimension.putBoolean("c" + index, true);
            // - the end marker only
            dimension.putInt("x" + index, coord.getX());
            dimension.putInt("y" + index, coord.getY());
            dimension.putInt("z" + index, coord.getZ());
        }
        tag.put(key, dimension);
    }

    /**
     * GT6 {@code WD.te(aWorld, tCoords, F)} tested against {@code ITileEntityRemoteActivateable}
     * ({@code Behavior_Remote:60,81,98}) — the remote-activatable at a coordinate, or {@code null}.
     *
     * <p>Nothing is resolved for a coordinate whose chunk is not loaded: GT6's {@code F} argument is
     * "do not load", and asking the level for a block would load the chunk. The block entity is asked
     * first and the block second, because the port's implementer may be either — the dynamite is a
     * plain block with no block entity.</p>
     *
     * @param level the level the coordinate belongs to
     * @param pos   the bound coordinate
     * @return the interface to call, or {@code null} when there is nothing remote-activatable there
     */
    @Nullable

    private static RemoteActivatable target(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        if (level.getBlockEntity(pos) instanceof RemoteActivatable entity) return entity;
        return level.getBlockState(pos).getBlock() instanceof RemoteActivatable block ? block : null;
    }

    /**
     * GT6 {@code Behavior_Remote.onItemUseFirst} — the binding half: sneak-click a block
     * to toggle its coordinate.
     *
     * <p>The gates are the original's and in its order: never on the client side, never
     * without a player, only while sneaking — a plain right-click is the activation half, see
     * {@link #useInAir} — and only where the player may build. GT6's {@code aPlayer} cannot be
     * {@code null}, but the port's behaviour-click signature allows it (the auto-tool case elsewhere in
     * this layer), so a {@code null} player refuses instead of throwing at {@code isSneaking()}.</p>
     *
     * <p>The four chat lines are GT6's own words, sent with
     * {@code Player.displayClientMessage}, the 1.20.1 form of {@code UT.Entities.sendchat}; the
     * accompanying beeps are not played (see the class javadoc). The list is written back in every
     * branch and the stack is neither consumed nor damaged.</p>
     *
     * @return GT6's {@code T} once the gates are passed — the click is handled even when
     *         nothing was bound; {@code F} on a gate
     */

    public static Outcome useOn(Level level, BlockPos pos, Direction side, @Nullable Player player,
            ItemStack stack, float hitX, float hitY, float hitZ) {
        if (level.isClientSide) return Outcome.refused(stack);
        if (player == null) return Outcome.refused(stack);
        if (!player.isShiftKeyDown()) return Outcome.refused(stack);
//? if >=1.21.1 {
        /*if (!ManualToolBehaviorAccess.mayEdit(level, player, pos)) return Outcome.refused(stack);
        //
*///?} else {
        if (!ItemBehaviors.mayEdit(level, player, pos)) return Outcome.refused(stack);
//?}

//? if >=1.21.1 {
        /*CompoundTag tag = gregtech.platform.neoforge.StackCustomData.read(stack);
        // UT.NBT.getNBT
*///?} else {
        CompoundTag tag = stack.getOrCreateTag();
        // UT.NBT.getNBT
//?}
        List<BlockPos> coords = getCoords(tag, level);
        if (coords.contains(pos)) {
            message(player, "Coordinates removed!");
            coords.remove(pos);
        } else if (coords.size() >= MAX_COORDS) {
            message(player, "Cant hold more than 64 Coordinates per Dimension!");
        } else if (target(level, pos) != null) {
            message(player, "Coordinates added!");
            coords.add(pos.immutable());
        } else {
            message(player, "This cannot be added!");
        }
//? if >=1.21.1 {
        /*gregtech.platform.neoforge.StackCustomData.update(stack, nbt -> setCoords(nbt, level, coords));
        //
*///?} else {
        setCoords(tag, level, coords);
//?}
        return Outcome.acted(stack);
    }

    /**
     * GT6 {@code Behavior_Remote.onItemRightClick} — the activation half: fire every
     * bound coordinate of this dimension that is within {@link #RANGE} blocks of the player.
     *
     * <p>The gates are the original's: never on the client side, only while <em>not</em>
     * sneaking (sneaking is the binding half), and only when the stack carries a tag at all — an item
     * that has never bound anything is left completely alone. GT6 reads {@code aPlayer} without a
     * {@code null} test here too, and this signature allows {@code null}, so it refuses like
     * {@link #useOn} does.</p>
     *
     * <p>The keep rule is GT6's, coordinate by coordinate: inside the range the
     * coordinate survives only if the block is a {@link RemoteActivatable} <em>and</em> its
     * {@code remoteActivate} answers {@code true} — a switch stays, a fired charge does not; outside the
     * range the coordinate is kept unread and untouched, so a remote can hold a list that reaches beyond
     * its activation radius. The survivors are written back, and the
     * {@code MC_CLICK} sound is not played (see the class javadoc).</p>
     *
     * <p>The stack that comes in is the stack that goes out — only its tag changes — so a caller can
     * pass the held stack straight through.</p>
     *
     * @return GT6's "handled, this is the stack"; {@code F} on a gate
     */

    public static Outcome useInAir(Level level, @Nullable Player player, ItemStack stack) {
        if (level.isClientSide) return Outcome.refused(stack);
        if (player == null) return Outcome.refused(stack);
        if (player.isShiftKeyDown()) return Outcome.refused(stack);
//? if >=1.21.1 {
        /*CompoundTag tag = gregtech.platform.neoforge.StackCustomData.read(stack);
        if (tag.isEmpty()) return Outcome.refused(stack);
        //
*///?} else {
        CompoundTag tag = stack.getTag();
        if (tag == null) return Outcome.refused(stack);
//?}

        List<BlockPos> kept = new ArrayList<>();
        for (BlockPos coords : getCoords(tag, level)) {
            if (inRange(coords, player)) {
                RemoteActivatable activatable = target(level, coords);
                // - the coordinate stays only when the block answers "keep me bound".
                if (activatable != null && activatable.remoteActivate(level, coords)) kept.add(coords);
            } else {
                kept.add(coords);
            }
        }
//? if >=1.21.1 {
        /*gregtech.platform.neoforge.StackCustomData.update(stack, nbt -> setCoords(nbt, level, kept));
        //
*///?} else {
        setCoords(tag, level, kept);
//?}
        return Outcome.acted(stack);
    }

    /**
     * GT6 {@code Behavior_Remote:80}: {@code Math.abs(tCoords.posX - aPlayer.posX) <= 128} and the same
     * test for {@code y} and {@code z} — a cube around the player, not a sphere.
     */

    private static boolean inRange(BlockPos coords, Player player) {
        return Math.abs(coords.getX() - player.getX()) <= RANGE
        && Math.abs(coords.getY() - player.getY()) <= RANGE
        && Math.abs(coords.getZ() - player.getZ()) <= RANGE;
    }

    /**
     * GT6's {@code UT.Entities.sendchat(aPlayer, …)} in 1.20.1: a chat line for
     * that one player, not an action-bar message — the second argument {@code false} is what makes it
     * land in the chat, where GT6's chat messages go.
     */

    private static void message(Player player, String text) {
        player.displayClientMessage(Component.literal(text), false);
    }
}
