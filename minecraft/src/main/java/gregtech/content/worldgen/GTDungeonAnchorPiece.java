package gregtech.content.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The legacy marker piece of {@link GTDungeonStructure}: it places no blocks, and only exists so that
 * worlds generated before the dungeon moved into structure pieces still load.
 *
 * <p>Those worlds store structure starts made of anchor markers at the dungeon's centre chunk; their
 * blocks were already written by {@code GTDungeonGenerator} when the dungeon was a feature, so nothing
 * may be placed again. {@link GTStructures#DUNGEON_ANCHOR} keeps the old piece id registered for exactly
 * that reason. New worlds never create this piece: {@link GTDungeonStructure#findGenerationPoint} builds
 * {@link GTDungeonCellPiece}s instead.</p>
 *
 * <p>Such an old start only references the chunk it sits in, so an old dungeon does not grow into chunks
 * generated after the update. Rebuilding it there would place a second copy of the cells the save already
 * has, which is worse than a dungeon with a cut-off edge.</p>
 */
public final class GTDungeonAnchorPiece extends StructurePiece {

    /** The marker around the given box, as stored in older saves. */
    public GTDungeonAnchorPiece(BoundingBox box) {
        super(GTStructures.DUNGEON_ANCHOR.get(), 0, box);
    }

    /** The piece as it is read back from a chunk's {@code structures.Starts} tag. */
    public GTDungeonAnchorPiece(CompoundTag tag) {
        super(GTStructures.DUNGEON_ANCHOR.get(), tag);
    }

    /** The piece carries no state beyond its bounding box, which {@code StructurePiece.createTag} writes. */
    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        // Nothing to add.
    }

    /** The dungeon's blocks are already in the save; this piece deliberately places none. */
    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            BlockPos pivot) {
        // Deliberately empty, see the class javadoc.
    }
}
