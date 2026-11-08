package gregtech.content.worldgen;

import gregtech.content.worldgen.dungeon.GTDungeonLayout;
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
 * One chunk-sized cell of GT6's dungeon, written while vanilla generates that cell's chunk.
 *
 * <p>{@link GTDungeonStructure} creates one piece per cell that produces blocks, so the structure's
 * bounding box is the whole dungeon and vanilla's {@code StructureCheck} knows every dungeon chunk.
 * Vanilla calls {@link #postProcess} for the piece that intersects the chunk being decorated
 * ({@code StructureStart.placeInChunk}); the piece rebuilds the dungeon-wide values from the world seed
 * and hands that one cell to {@link GTDungeonGenerator}, the same cell code the port used when the
 * dungeon was a feature.</p>
 *
 * <p>The piece type id stays stable between saves via {@link GTStructures#DUNGEON_CELL}; the anchor
 * chunk and the cell indices are all the piece needs on disk, because the layout, rocks and colour are
 * derived from the world seed.</p>
 */
public final class GTDungeonCellPiece extends StructurePiece {

    private static final String ANCHOR_X = "AnchorX";
    private static final String ANCHOR_Z = "AnchorZ";
    private static final String CELL_I = "CellI";
    private static final String CELL_J = "CellJ";

    private final ChunkPos anchor;
    private final int cellI, cellJ;

    /** The piece around one cell, built by {@link GTDungeonStructure#findGenerationPoint}. */
    public GTDungeonCellPiece(ChunkPos anchor, int i, int j, BoundingBox box) {
        super(GTStructures.DUNGEON_CELL.get(), 0, box);
        this.anchor = anchor;
        this.cellI = i;
        this.cellJ = j;
    }

    /** The piece as it is read back from a chunk's {@code structures.Starts} tag. */
    public GTDungeonCellPiece(CompoundTag tag) {
        super(GTStructures.DUNGEON_CELL.get(), tag);
        this.anchor = new ChunkPos(tag.getInt(ANCHOR_X), tag.getInt(ANCHOR_Z));
        this.cellI = tag.getInt(CELL_I);
        this.cellJ = tag.getInt(CELL_J);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt(ANCHOR_X, anchor.x);
        tag.putInt(ANCHOR_Z, anchor.z);
        tag.putInt(CELL_I, cellI);
        tag.putInt(CELL_J, cellJ);
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            BlockPos pivot) {
        // A cell that would overwrite a stronghold, trial chamber or ancient city is left alone. The
        // check runs at placement time because that is the only point where the other structure starts
        // of this chunk are visible; every cell decides on its own, so no dungeon-wide state is needed.
        if (GTDungeonStructure.intersectsLargeStructure(level, structureManager, chunkPos, getBoundingBox())) return;
        long seed = GTDungeonLayout.seedFor(level, anchor);
        GTDungeonGenerator.generateCell(level, anchor, GTDungeonLayout.plan(seed), cellI, cellJ, seed);
    }
}
