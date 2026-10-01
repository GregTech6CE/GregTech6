package com.gregtech.gregtech.item.behavior;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import javax.annotation.Nullable;
/** Original manual-tool permission, obstruction and ignition boundary. */
public final class ManualToolBehaviorAccess {
 private ManualToolBehaviorAccess(){}
 public record Outcome(boolean acted,ItemStack stack){public static Outcome refused(ItemStack stack){return new Outcome(false,stack);}public static Outcome acted(ItemStack stack){return new Outcome(true,stack);}}
    private static final double PX_P_2 = 0.125D, PX_N_2 = 0.875D, PX_P_4 = 0.25D, PX_N_4 = 0.75D;

    /**
     * GT6 {@code WD.obstructed} ({@code WD.java:127-148}): whether the block <em>behind</em> the
     * clicked face reaches into that face far enough that the player cannot put a tool through it.
     *
     * <p>The original works on the neighbour's 1.7.10 collision bounding box and asks, per side,
     * whether the box fills at least the quarter of the neighbour's own block touching the shared
     * plane ({@code PX_N[4] = 0.75} / {@code PX_P[4] = 0.25}) and spans more than one pixel in both
     * tangential axes ({@code > PX_P[2] = 0.125} and {@code < PX_N[2] = 0.875}). All six branches are
     * kept verbatim; {@code shape.bounds()} is the same union box
     * {@code getCollisionBoundingBoxFromPool} returned. Trapdoors, doors and ladders never obstruct
     * ({@code WD.java:136}), and a block without a collision box never does
     * ({@code WD.java:138}).</p>
     *
     * @param level the level the clicked block stands in
     * @param pos   the clicked block; the neighbour is {@code pos.relative(side)}
     * @param side  the clicked face
     * @return {@code true} when the face is blocked, which is what makes the tape and the igniters
     *         refuse to act
     */
    public static boolean obstructed(Level level, BlockPos pos, Direction side) {
        BlockPos neighbour = pos.relative(side);
        if (!level.hasChunkAt(neighbour)) return false;
        BlockState state = level.getBlockState(neighbour);
        // WD.java:136 - these three are explicitly never obstructing.
        if (state.getBlock() instanceof TrapDoorBlock
                || state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof LadderBlock) {
            return false;
        }
        var shape = state.getCollisionShape(level, neighbour);
        // WD.java:138 - no collision box at all means no obstruction.
        if (shape.isEmpty()) return false;
        AABB box = shape.bounds();
        boolean spansX = box.maxX > PX_P_2 && box.minX < PX_N_2;
        boolean spansY = box.maxY > PX_P_2 && box.minY < PX_N_2;
        boolean spansZ = box.maxZ > PX_P_2 && box.minZ < PX_N_2;
        return switch (side) {
            case DOWN -> box.maxY > PX_N_4 && spansX && spansZ;
            case UP -> box.minY < PX_P_4 && spansX && spansZ;
            case NORTH -> box.maxZ > PX_N_4 && spansX && spansY;
            case SOUTH -> box.minZ < PX_P_4 && spansX && spansY;
            case WEST -> box.maxX > PX_N_4 && spansY && spansZ;
            case EAST -> box.minX < PX_P_4 && spansY && spansZ;
        };
    }

    /** GT6 {@code UT.Entities.hasInfiniteItems} ({@code UT.java:3187-3189}): creative mode only. */
    public static boolean creative(@Nullable Player player) {
        return player != null && player.getAbilities().instabuild;
    }

    /**
     * GT6's {@code aPlayer.canPlayerEdit(x, y, z, side, stack)} gate, in its 1.20.1 form:
     * {@code player.mayBuild()} plus {@code level.mayInteract(player, pos)}.
     *
     * <p>A {@code null} player is GT6's auto-tool case and always passes, which is why this returns
     * {@code true} rather than {@code false} for it.</p>
     */
    public static boolean mayEdit(Level level, @Nullable Player player, BlockPos pos) {
        if (player == null) return true;
        return player.mayBuild() && level.mayInteract(player, pos);
    }

    public interface Ignitable {
        /**
         * @param side     the clicked face, in GT6's own side order translated to {@link Direction}
         * @param player   the player, or {@code null} for GT6's auto-tool igniter
         * @param igniter  the stack that is igniting
         * @param sneaking whether the player sneaks, which some GT6 machines use as a mode switch
         * @return GT6's tool-click return value, i.e. the durability cost in 1/10000 units;
         *         {@code 0} when this block does not react to the igniter
         */
        long onIgnite(Level level, BlockPos pos, Direction side, @Nullable Player player, ItemStack igniter,
                      boolean sneaking, float hitX, float hitY, float hitZ);
    }

    /**
     * GT6's {@code IBlockToolable.Util.onToolClick(TOOL_igniter, ...)} — ask the block to react, and
     * report its durability cost.
     *
     * <p>The block entity is asked first and the block second, which is the resolution order
     * {@code WD.te(..., aDelegator = T)} produces in every other behaviour of this package. The
     * return value is handed back unchanged so a caller can apply GT6's own conversion
     * ({@code Behavior_FlintAndTinder:58} uses {@code units(damage, 10000, 100, T)} for a tool,
     * {@code Behavior_Lighter:112} uses {@code units(damage, 10000, 1, T)} for a fuel unit).</p>
     *
     * @return the cost in GT6's 1/10000 units, or {@code 0} when nothing reacted
     */
    public static long igniteToolClick(Level level, BlockPos pos, Direction side, @Nullable Player player,
                                       ItemStack igniter, boolean sneaking, float hitX, float hitY, float hitZ) {
        Ignitable target = level.getBlockEntity(pos) instanceof Ignitable be ? be
                : level.getBlockState(pos).getBlock() instanceof Ignitable block ? block : null;
        if (target == null) return 0L;
        return target.onIgnite(level, pos, side, player, igniter, sneaking, hitX, hitY, hitZ);
    }

    /** GT6 {@code ToolCompat:206}: a TNT block ignited by a GT igniter costs one full unit. */
    public static final long TNT_COST = 10000L;

    /**
     * The vanilla half of GT6's {@code TOOL_igniter} ({@code ToolCompat.java:201-223}): a GT igniter
     * that no block claims lights a fire block in front of the clicked face.
     *
     * <p>The original's conditions, in order: the target must be air
     * ({@code aWorld.isAirBlock}, {@code ToolCompat:217}) and the clicked block must have oxygen
     * ({@code WD.oxygen}, {@code ToolCompat:218}) — which is {@code WD.java:396-398}, i.e. a
     * Galacticraft-only check that is unconditionally {@code true} without that mod, so the port
     * keeps only the air test. The fire state comes from
     * {@link BaseFireBlock#getState(net.minecraft.world.level.BlockGetter, BlockPos)}, the 1.20.1
     * replacement for GT6's bare {@code Blocks.fire}.</p>
     *
     * <p><b>Not ported from {@code ToolCompat:203-213}:</b> the Forestry Candle and the Twilight
     * Forest Lamp of Cinders branches — neither mod exists in the port.</p>
     *
     * @return whether a fire block was placed, i.e. whether the operation cost a use
     */
    public static boolean lightVanillaFire(Level level, @Nullable Player player, BlockPos pos, Direction side,
                                           ItemStack igniter) {
        BlockPos target = pos.relative(side);
        if (!level.hasChunkAt(target)) return false;
        if (!level.isEmptyBlock(target)) return false;
        if (!mayEdit(level, player, target)) return false;
        return level.setBlock(target, BaseFireBlock.getState(level, target), 3);
    }

    /**
     * GT6 {@code ToolCompat:203-207}: a TNT block is primed instead of being set on fire, and the
     * block is removed. 1.20.1 folds both halves into
     * {@link TntBlock#onCaughtFire(BlockState, Level, BlockPos, Direction, net.minecraft.world.entity.LivingEntity)}.
     *
     * @return whether the block was TNT and got primed
     */
    public static boolean primeTnt(Level level, BlockPos pos, @Nullable Player player) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof TntBlock tnt)) return false;
        tnt.onCaughtFire(state, level, pos, null, player);
        level.removeBlock(pos, false);
        return true;
    }
}
