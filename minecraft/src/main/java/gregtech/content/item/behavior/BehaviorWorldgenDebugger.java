package gregtech.content.item.behavior;

import gregtech.content.block.stone.GTStoneBlock;
import gregtech.content.block.stone.StoneType;
import gregtech.content.item.behavior.ItemBehaviors.Outcome;
import gregtech.content.worldgen.GTOreBlockResolver;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** GT6's worldgen wand, restricted to stripping common terrain from one loaded chunk. */
public final class BehaviorWorldgenDebugger {

    /** First layer swept by the original GT6 tool. */
    public static final int MIN_Y = BehaviorChunkEraser.MIN_Y;

    /** Last layer swept by the original GT6 tool. */
    public static final int MAX_Y = BehaviorChunkEraser.MAX_Y;

    private BehaviorWorldgenDebugger() {}

    /** Strips common terrain within the inclusive Y range and returns the number of removed blocks. */
    public static int eraseExceptOres(Level level, int chunkX, int chunkZ, int minY, int maxY) {
        if (!(level instanceof ServerLevel server)) return 0;
        int lowY = Math.max(minY, level.getMinBuildHeight());
        int highY = Math.min(maxY, level.getMaxBuildHeight() - 1);
        if (lowY > highY) return 0;

        var chunk = server.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk == null) return 0;

        ChunkPos pos = chunk.getPos();
        var bounds = new BoundingBox(
                pos.getMinBlockX(), lowY, pos.getMinBlockZ(), pos.getMaxBlockX(), highY, pos.getMaxBlockZ());
        var structures = server.structureManager().startsForStructure(pos, structure -> true).stream()
                .flatMap(start -> start.getPieces().stream())
                .map(piece -> piece.getBoundingBox())
                .filter(bounds::intersects)
                .toList();

        var cursor = new BlockPos.MutableBlockPos();
        int erased = 0;
        for (int x = pos.getMinBlockX(); x <= pos.getMaxBlockX(); x++) {
            for (int z = pos.getMinBlockZ(); z <= pos.getMaxBlockZ(); z++) {
                cursor.set(x, lowY, z);
                for (int y = lowY; y <= highY; y++) {
                    cursor.setY(y);
                    if (!isObscuringTerrain(chunk.getBlockState(cursor)) || insideStructure(structures, cursor))
                        continue;
                    // Keep surface markers and attachments in place when their support is removed.
                    if (level.setBlock(
                            cursor.immutable(),
                            Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
                        erased++;
                    }
                }
            }
        }
        return erased;
    }

    private static boolean insideStructure(List<BoundingBox> structures, BlockPos pos) {
        for (BoundingBox bounds : structures) {
            if (bounds.isInside(pos)) return true;
        }
        return false;
    }

    private static boolean isObscuringTerrain(BlockState state) {
        if (state.isAir()
                || state.hasBlockEntity()
                || state.is(Blocks.BEDROCK)
                || GTOreBlockResolver.oreMaterialOf(state.getBlock()) != null) return false;
        // Deep-ocean pylons are features built from raw GT prismarine, outside structure bounds.
        if (state.getBlock() instanceof GTStoneBlock stone
                && (stone.stoneType() == StoneType.PRISMARINE_LIGHT || stone.stoneType() == StoneType.PRISMARINE_DARK))
            return false;
        return GTOreBlockResolver.isBaseRock(state)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.liquid();
    }

    /** Uses the dimension's build limits, leaving its bottom layer in place. */
    public static Outcome useOn(
            Level level,
            BlockPos pos,
            Direction side,
            @Nullable Player player,
            ItemStack stack,
            float hitX,
            float hitY,
            float hitZ) {
        if (level.isClientSide || player == null || !player.isCreative() || !player.hasPermissions(2))
            return Outcome.refused(stack);
        eraseExceptOres(
                level, pos.getX() >> 4, pos.getZ() >> 4, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 1);
        return Outcome.acted(stack);
    }
}
