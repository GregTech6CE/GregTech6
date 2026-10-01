package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.block.OreBlock;
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
 * GT6 {@code Behavior_Worldgen_Debugger} ({@code Behavior_Worldgen_Debugger.java:43-55}) — the
 * "Worldgen Debug Wand" ({@code MultiItemRandomTools:520}; the port registers it as
 * {@code gregtech:worldgen_debug_wand} in {@code GTMultiItemsGen.ENTRIES:129}), the tool that makes an
 * ore vein visible by deleting everything around it.
 *
 * <h2>What the original does</h2>
 *
 * <p>The same click and the same triple loop as {@link BehaviorChunkEraser} — client side refused
 * ({@code :44}), the 16×16 column of the clicked chunk ({@code :45}), {@code tY = 1} to
 * {@code tY < 250} — with one difference inside the innermost loop ({@code :46-52}): the block at the
 * position is read first, and</p>
 *
 * <ul>
 *   <li>when it is an {@code IPrefixBlock} the position is <em>kept</em> and the original instead calls
 *       {@code ((ITileEntity) aWorld.getTileEntity(x, y, z)).onAdjacentBlockChange(x, y, z)}
 *       ({@code :47-49}), which lets the block re-examine its surroundings — a GT prefix block has a
 *       block entity in GT6 ({@code PrefixBlock} implements {@code ITileEntityProvider},
 *       {@code PrefixBlock.java:84});</li>
 *   <li>everything else becomes air, exactly the chunk remover's {@code WD.set(..., NB, 0, 2)}
 *       ({@code :51}).</li>
 * </ul>
 *
 * <p>So the tool clears a chunk and leaves the GT blocks standing, which is what its tooltip says:
 * "Currently deletes a single Chunk, except for bottom most Bedrock and GT6 Ores"
 * ({@code :64}). Like the chunk remover it always returns {@code T} ({@code :54}), reads no player and
 * no permission ({@code :44} tests only {@code aWorld.isRemote}), and neither consumes nor damages the
 * stack.</p>
 *
 * <h2>The port's "is a GT prefix block" predicate</h2>
 *
 * <p>GT6's {@code IPrefixBlock} ({@code gregapi/block/IPrefixBlock.java:27}) is implemented by exactly
 * one class, {@code gregapi.block.prefixblock.PrefixBlock} ({@code PrefixBlock.java:84}), and that class
 * backs the whole prefix-material family: {@code CS.BlocksGT} declares the ore blocks
 * ({@code ore}, {@code oreBedrock}, {@code oreSmall}, …, {@code CS.java:1633-1637}) and the material
 * blocks ({@code blockIngot}, {@code crateGtDust}, …, {@code :1627-1631}) as {@code IPrefixBlock}.
 * The worldgen-relevant half of that family is what a vein is made of, and in the port that is
 * {@link com.gregtech.gregtech.block.OreBlock} — one ore block per material with the host rock in its
 * {@code stone} state property, standing in for GT6's per-stone ore blocks. So the predicate here is
 * {@code state.getBlock() instanceof OreBlock}.</p>
 *
 * <p>Two honest notes on that mapping. The port's wider analogue of {@code IPrefixBlock} is
 * {@code com.gregtech.gregtech.block.MaterialBlockLike} (implemented by {@link
 * com.gregtech.gregtech.block.OreBlock} and {@code com.gregtech.gregtech.block.MaterialBlock}), and
 * GT6's {@code instanceof IPrefixBlock} would have kept the material-block family as well; the frozen
 * predicate of this behaviour is the ore half, which is the half the wand's purpose needs.</p>
 *
 * <p><b>No port counterpart for the {@code onAdjacentBlockChange} branch.</b> GT6 notifies the ore's
 * block entity so it can re-read its neighbours; in the port §104 gave the ore blocks <em>no block
 * entity at all</em> — {@code OreBlock} deliberately does not implement {@code EntityBlock}, and the
 * ore's host rock lives in a block state property instead — so there is nothing to notify and the
 * branch has no body to port. The ore is simply left standing, which is the whole visible effect the
 * debug wand has.</p>
 *
 * <h2>Ranges, chunks and the return value</h2>
 *
 * <p>{@link #eraseExceptOres} is {@link BehaviorChunkEraser#erase}'s clamp, chunk check and counting
 * applied to this loop (see that class for why: 1.20.1's build limits are not 0..255, the upper limit is
 * exclusive, and {@code Level.setBlock} would load an unloaded chunk). The count is again the number of
 * positions that held a block and were replaced with air — the GT blocks this tool keeps are not
 * counted, and a position that was already air is not counted either.</p>
 *
 * <h2>Tooltips</h2>
 *
 * <p>{@code LH.add} ({@code :57-65}) is not part of this layer; the port adds no lang keys here.</p>
 */
public final class BehaviorWorldgenDebugger {

    /** GT6 {@code Behavior_Worldgen_Debugger:45}: {@code tY = 1}, inherited from the chunk remover. */
    public static final int MIN_Y = BehaviorChunkEraser.MIN_Y;

    /** GT6 {@code Behavior_Worldgen_Debugger:45}: {@code tY < 250}, so 249 is the last swept position. */
    public static final int MAX_Y = BehaviorChunkEraser.MAX_Y;

    /** GT6 {@code Behavior_Worldgen_Debugger:45}: {@code (aX & ~15)} to {@code (aX & ~15) + 16}. */
    private static final int CHUNK_SIZE = 16;

    private BehaviorWorldgenDebugger() {}

    /**
     * GT6 {@code Behavior_Worldgen_Debugger:45-53}: air out the chunk column {@code chunkX}/{@code chunkZ}
     * between {@code minY} and {@code maxY} inclusive, but keep every GT ore.
     *
     * <p>The original's loop order is kept (x, then z, then y) and so is the order of its two tests — the
     * ore test first ({@code :47}), then the deletion of everything else ({@code :51}) — and the flag
     * {@code 2} it passes ({@code Block.UPDATE_CLIENTS}: inform the clients, no neighbour updates). Unlike
     * the original, the range is clamped to the level's build limits, an unloaded chunk is skipped whole,
     * and the return value counts what was really replaced; see the class javadoc.</p>
     *
     * @param level  the level to sweep; the chunk must already be loaded or the column is skipped
     * @param chunkX chunk x, not a block x — {@link #useOn} converts with {@code pos.getX() >> 4}
     * @param chunkZ chunk z; {@code pos.getZ() >> 4} in {@link #useOn}
     * @param minY   the lowest y of the sweep, clamped up to {@code level.getMinBuildHeight()}
     * @param maxY   the highest y of the sweep, clamped down to {@code level.getMaxBuildHeight() - 1}
     * @return how many positions held a block that was not an ore and were replaced with air
     */
    public static int eraseExceptOres(Level level, int chunkX, int chunkZ, int minY, int maxY) {
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
                    // GT6 :47-49 - a GT prefix block stays, and its block entity is told about the change.
                    // The port's ore blocks have no block entity (§104), so only the "stays" half is left.
                    if (state.getBlock() instanceof OreBlock) continue;
                    // Air is skipped rather than rewritten, exactly like the chunk remover does it.
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
     * GT6 {@code Behavior_Worldgen_Debugger:43-55} for one click: sweep the clicked position's chunk column,
     * keeping the ores.
     *
     * <p>{@code side}, {@code player} and the three hit coordinates belong to the behaviour-click signature
     * of this layer but the original reads none of them, and it has no permission gate either — the debug
     * wand is exempt from it in the dispatcher for the same reason the chunk remover is. The stack is
     * neither consumed nor damaged.</p>
     *
     * @return GT6's {@code T} ({@code :54}): handled on the server even when the sweep deleted nothing,
     *         refused on the client
     */
    public static Outcome useOn(Level level, BlockPos pos, Direction side, @Nullable Player player,
                                ItemStack stack, float hitX, float hitY, float hitZ) {
        if (level.isClientSide) return Outcome.refused(stack);                            // :44
        eraseExceptOres(level, pos.getX() >> 4, pos.getZ() >> 4, MIN_Y, MAX_Y);           // :45-53
        return Outcome.acted(stack);                                                      // :54
    }
}
