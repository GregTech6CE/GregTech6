package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.item.behavior.ItemBehaviors.Outcome;
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
 * GT6 {@code Behavior_Remote} ({@code Behavior_Remote.java:43-146}) — the Remote Activator
 * ({@code MultiItemRandomTools:515}; the port registers it as {@code gregtech:remote_activator} in
 * {@code GTMultiItemsGen.ENTRIES:124}). Its tooltip is the whole feature in one line: "Activates up to
 * 64 Blocks within a Range of 128m" ({@code :138}).
 *
 * <h2>The two halves</h2>
 *
 * <ul>
 *   <li><b>Binding</b> — {@code onItemUseFirst} ({@code :47-73}), the port's {@link #useOn}: sneak and
 *       right-click a block to toggle its coordinate in the item's list. A coordinate the list already
 *       holds is removed ({@code :52-55}); a full list of {@link #MAX_COORDS} refuses the new one
 *       ({@code :56-58}); otherwise the block is added only when it is a {@link RemoteActivatable}
 *       ({@code :59-64}) and refused with "This cannot be added!" when it is not ({@code :65-68}). The
 *       list is written back in <em>every</em> case ({@code :70}) and the click answers {@code T}
 *       ({@code :72}).</li>
 *   <li><b>Activation</b> — {@code onItemRightClick} ({@code :76-90}), the port's {@link #useInAir}: a
 *       right-click <em>without</em> sneaking fires every bound coordinate of this dimension that lies
 *       within {@link #RANGE} blocks on all three axes ({@code :80-82}). A coordinate inside the range
 *       survives only when the block there is a {@link RemoteActivatable} and its
 *       {@code remoteActivate} returned {@code true} — GT6's "keep me bound" contract, which the dynamite
 *       answers with {@code false} on purpose, so a fired charge drops out of the list. A coordinate
 *       outside the range is kept untouched ({@code :83-85}).</li>
 * </ul>
 *
 * <h2>The NBT layout, field for field</h2>
 *
 * <p>One compound per dimension, and inside it, for each index {@code i} from {@code 0}, a boolean
 * {@code "c" + i} followed by the ints {@code "x" + i}, {@code "y" + i} and {@code "z" + i}
 * ({@code :126-132}). The boolean carries no coordinate information at all: GT6 writes it purely as the
 * end marker its reader tests, walking {@code i = -1} while the compound still has {@code "c" + (++i)}
 * and stopping at the first missing one ({@code :115-117}). An empty list <em>removes</em> the
 * dimension's compound instead of writing an empty one ({@code :122-123}).</p>
 *
 * <p><b>One deviation, on purpose:</b> GT6 keys the compound by the numeric dimension id
 * ({@code "gt.remote.dim." + aWorld.provider.dimensionId}, {@code :113,123,133}), because 1.7.10
 * dimensions are numbers. 1.20.1 dimensions are {@code ResourceKey}s, so {@link #dimKey} uses
 * {@code level.dimension().location()} instead — {@code gt.remote.dim.minecraft:overworld} — and the
 * rest of the layout is unchanged. The tag therefore survives a round trip, but an item carried from
 * GT6 itself (or from a world whose dimension ids do not match the vanilla keys) will not find its
 * coordinates; there is no numeric id to fall back to in 1.20.1.</p>
 *
 * <h2>Finding the block to bind and fire</h2>
 *
 * <p>GT6 resolves a tile entity at the coordinate ({@code WD.te(aWorld, tCoords, F)} — never loading an
 * unloaded chunk, {@code :60,81,98}) and tests it against {@code ITileEntityRemoteActivateable}. The
 * port's implementer may be a plain block instead: the dynamite implements activation on its block
 * ({@code com.gregtech.gregtech.block.tool.DynamiteBlock}), so {@link #target} asks the block entity
 * first and the block second, the same resolution order {@code ItemBehaviors.igniteToolClick} uses, and
 * reports nothing for an unloaded coordinate.</p>
 *
 * <h2>What is not ported</h2>
 *
 * <ul>
 *   <li>The two sounds: four {@code SFX.GT_BEEP} beeps on binding ({@code :54,58,63,67}) and an
 *       {@code SFX.MC_CLICK} on activation ({@code :88}). This layer plays no sound of its own, so the
 *       audible half of the feedback is reduced to the chat line, which is the half the player reads.</li>
 *   <li>{@code LH.add("gt.behaviour.remote", …)} and {@code getAdditionalToolTips} ({@code :137-145}):
 *       tooltips belong to the item's registration, and this file adds no lang keys.</li>
 * </ul>
 */
public final class BehaviorRemote {

    /** GT6 {@code Behavior_Remote:56} (and {@code :95}): {@code tList.size() >= 64} per dimension. */
    public static final int MAX_COORDS = 64;

    /** GT6 {@code Behavior_Remote:80}: {@code Math.abs(...) <= 128} on every axis, from the player. */
    public static final int RANGE = 128;

    private BehaviorRemote() {}

    /** GT6's non-toggle binding path used by powered drills. A full remote is skipped. */
    public static boolean addCoords(ItemStack stack, Level level, BlockPos pos) {
        if (level.isClientSide) return false;
        var coords = getCoords(stack.getTag(), level);
        if (coords.size() >= MAX_COORDS) return false;
        if (coords.contains(pos)) return true;
        if (target(level, pos) == null) return false;
        coords.add(pos.immutable());
        setCoords(stack.getOrCreateTag(), level, coords);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.get(),
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
     * GT6 {@code Behavior_Remote.getCoords} ({@code :110-119}): the coordinates bound to a stack in this
     * dimension.
     *
     * <p>The read loop is the original's, marker first: {@code i = -1} and
     * {@code while (compound.contains("c" + (++i)))} ({@code :115}), so a list that lost its markers
     * (or one written by a different layout) stops at the first gap instead of reading garbage.</p>
     *
     * @param tag   the stack's tag; {@code null} reads as an empty list, like GT6's own
     *              {@code if (aNBT == null) return rList} ({@code :112})
     * @param level the level whose dimension key is read
     * @return a fresh mutable list, empty when nothing is bound — the callers of this layer are expected
     *         to edit it and hand it to {@link #setCoords} (GT6's list is an {@code ArrayListNoNulls})
     */
    public static List<BlockPos> getCoords(@Nullable CompoundTag tag, Level level) {
        List<BlockPos> coords = new ArrayList<>();
        if (tag == null) return coords;
        CompoundTag dimension = tag.getCompound(dimKey(level));                 // :113
        if (dimension.isEmpty()) return coords;                                 // :114
        int i = -1;
        while (dimension.contains("c" + (++i))) {                               // :115
            coords.add(new BlockPos(dimension.getInt("x" + i), dimension.getInt("y" + i),
                    dimension.getInt("z" + i)));                                // :116
        }
        return coords;
    }

    /**
     * GT6 {@code Behavior_Remote.setCoords} ({@code :121-135}): replace this dimension's list.
     *
     * <p>Field for field the original's writer: index {@code i} from {@code 0}, a boolean
     * {@code "c" + i} that is only the end marker for {@link #getCoords}, then {@code "x"/"y"/"z" + i}
     * ({@code :126-132}). An empty list removes the dimension's compound rather than storing an empty
     * one ({@code :122-123}), so an item that has nothing bound carries no tag for that dimension.</p>
     *
     * @param tag    the tag to write into — the stack's own tag, which the caller stores back; must not
     *               be {@code null} (GT6 dereferences it as well)
     * @param level  the level whose dimension key is written
     * @param coords the surviving coordinates; an empty list removes the entry
     */
    public static void setCoords(CompoundTag tag, Level level, List<BlockPos> coords) {
        String key = dimKey(level);
        if (coords.isEmpty()) {
            tag.remove(key);                                                    // :122-123
            return;
        }
        CompoundTag dimension = new CompoundTag();
        for (int i = 0, size = coords.size(); i < size; i++) {                   // :126-132
            BlockPos coord = coords.get(i);
            dimension.putBoolean("c" + i, true);                                // :128 - the end marker only
            dimension.putInt("x" + i, coord.getX());
            dimension.putInt("y" + i, coord.getY());
            dimension.putInt("z" + i, coord.getZ());
        }
        tag.put(key, dimension);                                                // :133
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
     * GT6 {@code Behavior_Remote.onItemUseFirst} ({@code :47-73}) — the binding half: sneak-click a block
     * to toggle its coordinate.
     *
     * <p>The gates are the original's and in its order ({@code :48}): never on the client side, never
     * without a player, only while sneaking — a plain right-click is the activation half, see
     * {@link #useInAir} — and only where the player may build. GT6's {@code aPlayer} cannot be
     * {@code null}, but the port's behaviour-click signature allows it (the auto-tool case elsewhere in
     * this layer), so a {@code null} player refuses instead of throwing at {@code isSneaking()}.</p>
     *
     * <p>The four chat lines are GT6's own words ({@code :53,57,62,66}), sent with
     * {@code Player.displayClientMessage}, the 1.20.1 form of {@code UT.Entities.sendchat}; the
     * accompanying beeps are not played (see the class javadoc). The list is written back in every
     * branch ({@code :70-71}) and the stack is neither consumed nor damaged.</p>
     *
     * @return GT6's {@code T} ({@code :72}) once the gates are passed — the click is handled even when
     *         nothing was bound; {@code F} on a gate
     */
    public static Outcome useOn(Level level, BlockPos pos, Direction side, @Nullable Player player,
                                ItemStack stack, float hitX, float hitY, float hitZ) {
        if (level.isClientSide) return Outcome.refused(stack);                          // :48
        if (player == null) return Outcome.refused(stack);
        if (!player.isShiftKeyDown()) return Outcome.refused(stack);                     // :48
        if (!ItemBehaviors.mayEdit(level, player, pos)) return Outcome.refused(stack);    // :48

        CompoundTag tag = stack.getOrCreateTag();                                       // :49 UT.NBT.getNBT
        List<BlockPos> coords = getCoords(tag, level);                                  // :50
        if (coords.contains(pos)) {                                                     // :52-55
            message(player, "Coordinates removed!");
            coords.remove(pos);
        } else if (coords.size() >= MAX_COORDS) {                                       // :56-58
            message(player, "Cant hold more than 64 Coordinates per Dimension!");
        } else if (target(level, pos) != null) {                                        // :59-64
            message(player, "Coordinates added!");
            coords.add(pos.immutable());
        } else {                                                                        // :65-68
            message(player, "This cannot be added!");
        }
        setCoords(tag, level, coords);                                                  // :70-71
        return Outcome.acted(stack);                                                    // :72
    }

    /**
     * GT6 {@code Behavior_Remote.onItemRightClick} ({@code :76-90}) — the activation half: fire every
     * bound coordinate of this dimension that is within {@link #RANGE} blocks of the player.
     *
     * <p>The gates are the original's ({@code :77}): never on the client side, only while <em>not</em>
     * sneaking (sneaking is the binding half), and only when the stack carries a tag at all — an item
     * that has never bound anything is left completely alone. GT6 reads {@code aPlayer} without a
     * {@code null} test here too, and this signature allows {@code null}, so it refuses like
     * {@link #useOn} does.</p>
     *
     * <p>The keep rule is GT6's, coordinate by coordinate ({@code :79-86}): inside the range the
     * coordinate survives only if the block is a {@link RemoteActivatable} <em>and</em> its
     * {@code remoteActivate} answers {@code true} — a switch stays, a fired charge does not; outside the
     * range the coordinate is kept unread and untouched, so a remote can hold a list that reaches beyond
     * its activation radius ({@code :83-85}). The survivors are written back ({@code :87}), and the
     * {@code MC_CLICK} sound is not played (see the class javadoc).</p>
     *
     * <p>The stack that comes in is the stack that goes out — only its tag changes — so a caller can
     * pass the held stack straight through.</p>
     *
     * @return GT6's "handled, this is the stack" ({@code :89}); {@code F} on a gate
     */
    public static Outcome useInAir(Level level, @Nullable Player player, ItemStack stack) {
        if (level.isClientSide) return Outcome.refused(stack);                           // :77
        if (player == null) return Outcome.refused(stack);
        if (player.isShiftKeyDown()) return Outcome.refused(stack);                      // :77
        CompoundTag tag = stack.getTag();
        if (tag == null) return Outcome.refused(stack);                                  // :77

        List<BlockPos> kept = new ArrayList<>();
        for (BlockPos coords : getCoords(tag, level)) {                                  // :79
            if (inRange(coords, player)) {                                               // :80
                RemoteActivatable activatable = target(level, coords);
                // :82 - the coordinate stays only when the block answers "keep me bound".
                if (activatable != null && activatable.remoteActivate(level, coords)) kept.add(coords);
            } else {
                kept.add(coords);                                                        // :83-85
            }
        }
        setCoords(tag, level, kept);                                                     // :87
        return Outcome.acted(stack);                                                     // :89
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
     * GT6's {@code UT.Entities.sendchat(aPlayer, …)} ({@code :53,57,62,66}) in 1.20.1: a chat line for
     * that one player, not an action-bar message — the second argument {@code false} is what makes it
     * land in the chat, where GT6's chat messages go.
     */
    private static void message(Player player, String text) {
        player.displayClientMessage(Component.literal(text), false);
    }
}
