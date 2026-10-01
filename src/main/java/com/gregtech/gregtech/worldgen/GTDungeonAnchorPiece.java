package com.gregtech.gregtech.worldgen;

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
 * The one and only piece of {@link GTDungeonStructure}: an anchor marker that places <b>no blocks at
 * all</b>.
 *
 * <p>GT6's dungeon {@code WorldgenDungeonGT} was registered with the 1.7.10 world generator, which had no
 * concept of a structure; the port generates it as a {@link GTDungeonFeature feature}, one cell per chunk
 * (see {@code GTDungeonLayout} for that split). The structure that only exists so vanilla's {@code /locate
 * structure gregtech:gt_dungeon} can answer must therefore build nothing - if it wrote a single block, the
 * dungeon would be generated twice.</p>
 *
 * <p>Why a piece at all, instead of an empty generation point: vanilla decides whether a structure exists
 * through this chain, every step of which needs something non-empty -</p>
 * <ul>
 *   <li>{@code StructureCheck.canCreateStructure} ({@code StructureCheck:91-93}) asks
 *       {@code structure.findValidGenerationPoint(...).isPresent()}; an empty optional makes
 *       {@code StructureCheck.checkStart} ({@code :71-89}) answer {@code START_NOT_PRESENT},</li>
 *   <li>{@code ChunkGenerator.getStructureGeneratingAt} ({@code ChunkGenerator:240-257}) skips every
 *       candidate whose check answers {@code START_NOT_PRESENT}, so {@code /locate} would never report the
 *       dungeon, however correct its placement is,</li>
 *   <li>and once the chunk is forced to {@code STRUCTURE_STARTS}, {@code StructureStart.isValid()}
 *       ({@code StructureStart:116-118}) is {@code !pieceContainer.isEmpty()}
 *       ({@code PiecesContainer:28-30}), so a start with zero pieces is discarded by
 *       {@code Structure.generate} ({@code Structure:84-96}) and never stored in the chunk.</li>
 * </ul>
 *
 * <p>So the port returns a generation point holding exactly this piece - and only where GT6's own
 * one-in-a-hundred gate passed ({@code GTDungeonStructure.findGenerationPoint}), so an anchor without a
 * dungeon holds no structure at all. Blocks can only ever be written by a piece's {@link #postProcess}
 * ({@code StructureStart.placeInChunk:81-96} calls it per piece and does nothing else), and this one writes
 * nothing, which is what the game test asserts by diffing a whole chunk across that call.</p>
 */
public final class GTDungeonAnchorPiece extends StructurePiece {

    /** The piece around the given box; used by {@link GTDungeonStructure}'s generation point. */
    public GTDungeonAnchorPiece(BoundingBox box) {
        super(GTStructures.DUNGEON_PIECE.get(), 0, box);
    }

    /** The piece as it is read back from a chunk's {@code structures.Starts} tag. */
    public GTDungeonAnchorPiece(CompoundTag tag) {
        super(GTStructures.DUNGEON_PIECE.get(), tag);
    }

    /** The piece carries no state beyond its bounding box, which {@code StructurePiece.createTag} writes. */
    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        // Nothing to add.
    }

    /** GT6's dungeon is built by {@link GTDungeonFeature}; this piece deliberately places no blocks. */
    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        // Deliberately empty: the anchor only marks where the dungeon is, see the class javadoc.
    }
}
