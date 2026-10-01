package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.item.behavior.ItemBehaviors.Outcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * GT6 {@code Behavior_Chunk_Remover} ({@code Behavior_Chunk_Remover.java:39-45}) — the "Chunk Eraser"
 * debug item ({@code MultiItemRandomTools:519}; the port registers it as {@code gregtech:chunk_eraser}
 * in {@code GTMultiItemsGen.ENTRIES:128}).
 *
 * <h2>What the original does</h2>
 *
 * <p>The whole behaviour is one click and one triple loop. The click is refused while the client side
 * runs ({@code :40}); then the 16×16 column of the chunk that holds the clicked position
 * ({@code (aX & ~15) .. (aX & ~15) + 16}, {@code :41}) is swept from {@link #MIN_Y} to {@link #MAX_Y}
 * inclusive and every position becomes GT6's null block:
 * {@code WD.set(aWorld, tX, tY, tZ, NB, 0, 2)} ({@code :42}). {@code NB} is {@code Blocks.air}
 * ({@code CS.java:850}) and {@code 2} is the flag handed to {@code World.setBlock}, i.e. "tell the
 * clients, skip the neighbour updates". The behaviour then returns {@code T} ({@code :44}) — the click
 * is always handled, even when there was nothing to delete.</p>
 *
 * <p>{@code aPlayer} is never read: the original tests only {@code aWorld.isRemote}, so this debug
 * tool has no build-permission gate. Its tooltip says what it is for — "Deletes Chunks except for the
 * very Bottom" ({@code :48}) — and {@code y = 1} is that "very Bottom": 1.7.10 bedrock sits at
 * {@code y = 0}, so starting at {@code 1} leaves the world's floor alone. The click neither consumes
 * the stack nor damages it.</p>
 *
 * <h2>The port's y range</h2>
 *
 * <p>{@link #MIN_Y}/{@link #MAX_Y} are the original's literals ({@code tY = 1; tY < 250}) and
 * {@link #useOn} uses exactly those; {@link #erase} is the same sweep with the range as a parameter,
 * because that is the seam the tests drive. It clamps the requested range to the level's own build
 * limits — 1.20.1 worlds do not all span 0..255 (the overworld is -64..319, a modded dimension may be
 * smaller) — and {@code Level.getMaxBuildHeight()} is <em>exclusive</em> in 1.20.1
 * ({@code LevelHeightAccessor:11-13,32}), so the highest position that can be touched is one below it.
 * A range that is empty after clamping returns {@code 0} without sweeping.</p>
 *
 * <h2>What {@link #erase} returns</h2>
 *
 * <p>The original has no count at all: its loop discards {@code WD.set}'s result ({@code :42}) and the
 * behaviour answers a plain {@code T}. The count added here is the number of positions that actually
 * held a block and were replaced with air, i.e. the writes that changed something. Air positions are
 * skipped before the write, so a second call on the same chunk returns {@code 0}, and so does a range
 * that was already empty — a caller that wants "how many positions the sweep looked at" has to compute
 * that from the range, not from this value.</p>
 *
 * <h2>Chunks that are not loaded</h2>
 *
 * <p>1.20.1's {@code Level.setBlock} loads the chunk it writes into ({@code Level.getChunkAt}), which a
 * debug wand must not do — erasing near the loaded area would pull chunk after chunk in. Every 16×16
 * column is therefore tested once with {@code LevelReader.hasChunkAt}
 * ({@code LevelReader.java:174-182}, the non-loading test) and skipped as a whole when its chunk is not
 * loaded. All positions of a column share one chunk, so hoisting the test out of the y loop is exactly
 * as strict as testing per position; only the x/z part of the position is read by that test.</p>
 *
 * <h2>Tooltips</h2>
 *
 * <p>The {@code LH.add} registration ({@code :47-49}) is not part of this layer — tooltips belong to the
 * item's registration and this file adds no lang keys.</p>
 */
public final class BehaviorChunkEraser {

    /** GT6 {@code Behavior_Chunk_Remover:41}: {@code tY = 1} — one above GT6's bedrock floor. */
    public static final int MIN_Y = 1;

    /** GT6 {@code Behavior_Chunk_Remover:41}: the loop condition {@code tY < 250}, so 249 is included. */
    public static final int MAX_Y = 249;

    /** GT6 {@code Behavior_Chunk_Remover:41}: {@code (aX & ~15)} to {@code (aX & ~15) + 16}. */
    private static final int CHUNK_SIZE = 16;

    private BehaviorChunkEraser() {}

    /**
     * GT6 {@code Behavior_Chunk_Remover:41-43}: air out the chunk column {@code chunkX}/{@code chunkZ}
     * between {@code minY} and {@code maxY}, both inclusive.
     *
     * <p>The order of the original's loops is kept (x, then z, then y — y innermost), and so is the flag
     * {@code 2} it passes ({@code Block.UPDATE_CLIENTS}: inform the clients, no neighbour updates).
     * Unlike the original, the range is clamped to the level's build limits and an unloaded chunk is
     * left alone, and the return value counts what was really replaced; see the class javadoc.</p>
     *
     * @param level  the level to erase in; the chunk must already be loaded or it is skipped
     * @param chunkX chunk x, not a block x — {@link #useOn} converts with {@code pos.getX() >> 4}
     * @param chunkZ chunk z; {@code pos.getZ() >> 4} in {@link #useOn}
     * @param minY   the lowest y of the sweep, clamped up to {@code level.getMinBuildHeight()}
     * @param maxY   the highest y of the sweep, clamped down to {@code level.getMaxBuildHeight() - 1}
     * @return how many positions held a block and were replaced with air
     */
    public static int erase(Level level, int chunkX, int chunkZ, int minY, int maxY) {
        int lowY = Math.max(minY, level.getMinBuildHeight());
        int highY = Math.min(maxY, level.getMaxBuildHeight() - 1);
        if (lowY > highY) return 0;

        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int erased = 0;
        for (int x = minX; x < minX + CHUNK_SIZE; x++) {
            for (int z = minZ; z < minZ + CHUNK_SIZE; z++) {
                cursor.set(x, lowY, z);
                // hasChunkAt reads only x/z, and it does not load: Level.setBlock would.
                if (!level.hasChunkAt(cursor)) continue;
                for (int y = lowY; y <= highY; y++) {
                    cursor.setY(y);
                    BlockState state = level.getBlockState(cursor);
                    // Air is skipped rather than rewritten: it keeps the sweep cheap and makes the count
                    // mean "blocks that were deleted", which is what the tooltip promises.
                    if (state.isAir()) continue;
                    // A fresh BlockPos goes into setBlock: the client update packet keeps the reference
                    // it is handed (ClientboundBlockUpdatePacket:14-17), so the cursor must not be reused.
                    if (level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)) {
                        erased++;
                    }
                }
            }
        }
        return erased;
    }

    /**
     * GT6 {@code Behavior_Chunk_Remover:39-45} for one click: erase the clicked position's chunk column.
     *
     * <p>{@code side}, {@code player} and the three hit coordinates are part of the behaviour-click
     * signature every class of this layer has, but the original reads none of them — in particular there
     * is no permission gate, which is why this is the one behaviour that does not call
     * {@link ItemBehaviors#mayEdit}. The stack is neither consumed nor damaged; the returned stack is the
     * one that came in.</p>
     *
     * @return GT6's {@code T} ({@code :44}): the click is handled on the server even when the sweep found
     *         nothing to delete, and refused on the client
     */
    public static Outcome useOn(Level level, BlockPos pos, Direction side, @Nullable Player player,
                                ItemStack stack, float hitX, float hitY, float hitZ) {
        if (level.isClientSide) return Outcome.refused(stack);               // :40
        erase(level, pos.getX() >> 4, pos.getZ() >> 4, MIN_Y, MAX_Y);        // :41-43
        return Outcome.acted(stack);                                         // :44
    }
}
