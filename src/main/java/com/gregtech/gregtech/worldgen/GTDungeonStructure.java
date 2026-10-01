package com.gregtech.gregtech.worldgen;

import com.mojang.serialization.Codec;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonLayout;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * The structure that vanilla's {@code /locate structure gregtech:gt_dungeon} can find, and that builds
 * <b>nothing</b>.
 *
 * <p>GT6 has no structure for its dungeon: {@code WorldgenDungeonGT} was a 1.7.10 world generator entry
 * that wrote the whole grid of cells ({@code Loader_Worldgen:652}). The port generates that same dungeon as
 * the {@link GTDungeonFeature} feature, cell by cell (see {@code GTDungeonLayout}), so nothing is left for
 * a structure to build - what is missing is only vanilla's <em>knowledge</em> of where the dungeon is, which
 * is what {@code /locate} and structure-related game mechanics read. This class provides exactly that
 * knowledge and nothing else: it declares one {@link GTDungeonAnchorPiece} at every
 * {@link GTDungeonPlacement lattice anchor} that passes GT6's own one-in-a-hundred gate
 * ({@code WorldgenDungeonGT:150}, the same roll {@code GTDungeonFeature} makes), and that piece places no
 * blocks (see its javadoc for why a piece, and not an empty generation point, is what vanilla's
 * {@code StructureCheck} and {@code StructureStart} need). So a structure exists exactly where a dungeon
 * does, and {@code /locate} therefore reports dungeons rather than one anchor out of a hundred.</p>
 *
 * <p>The data files that go with it (all under {@code data/gregtech/}):</p>
 * <ul>
 *   <li>{@code worldgen/structure/gt_dungeon.json} - {@code "type": "gregtech:gt_dungeon"} (this class'
 *       {@link #type()}), the biome tag below as its {@code biomes} and vanilla's
 *       {@code underground_structures} step, which is where 1.20.1 keeps its own dungeons and mineshafts.
 *       The port's dungeon <em>feature</em> runs in the {@code underground_ores} decoration step
 *       ({@code data/gregtech/forge/biome_modifier/gt_dungeon.json}); the two are independent - a structure
 *       start is created at the chunk's {@code STRUCTURE_STARTS} status, long before any decoration.</li>
 *   <li>{@code tags/worldgen/biome/has_structure/gt_dungeon.json} - {@code #minecraft:is_overworld},
 *       GT6's {@code GEN_OVERWORLD} of {@code Loader_Worldgen:652}. The tag is also what
 *       {@code ChunkGeneratorStructureState.hasBiomesForStructureSet} ({@code ChunkGeneratorStructureState:59-65})
 *       reads to decide whether the structure set belongs to a dimension at all.</li>
 *   <li>{@code worldgen/structure_set/gt_dungeons.json} - one entry, this structure at weight 1, with the
 *       {@code gregtech:gt_dungeon_lattice} placement (see {@link GTDungeonPlacement}).</li>
 *   <li>{@code tags/worldgen/structure/gt_dungeon.json} - the structure in its own tag, so
 *       {@code /locate structure #gregtech:gt_dungeon} works as well as the plain id.</li>
 * </ul>
 */
public class GTDungeonStructure extends Structure {

    /** The structure's codec: vanilla's structure settings, like every other structure. */
    public static final Codec<GTDungeonStructure> CODEC = simpleCodec(GTDungeonStructure::new);

    public GTDungeonStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    /**
     * One anchor piece per anchor that GT6's own gate accepts, and nothing at all for the others.
     *
     * <p>GT6 rolls the dungeon once per anchor - {@code WorldgenDungeonGT:150}'s
     * {@code aRandom.nextInt(mProbability) != 0}, i.e. one dungeon per hundred anchors
     * ({@link GTDungeonLayout#passesProbability} of {@link GTDungeonLayout#seedFor}) - and the port's
     * {@link GTDungeonFeature} rolls the very same value from the very same seed. Rolling it here as well
     * is what keeps the two in step: an anchor that fails holds no structure, so vanilla's
     * {@code StructureCheck} answers {@code START_NOT_PRESENT} for it
     * ({@code StructureCheck:91-93} checks this method) and {@code /locate} walks on to an anchor that
     * really holds a dungeon, without forcing a chunk for the empty ones. An anchor that passes returns
     * the one {@link GTDungeonAnchorPiece}, which places no blocks - the dungeon itself is still the
     * feature's, cell by cell.</p>
     *
     * <p>Its box spans that anchor chunk at the Y the feature's dungeon sits at
     * ({@link GTWorldgenScale#remapY} of GT6's {@code GTDungeonLayout.MIN_Y}); the box is what locate and
     * the chunk's structure references see, and the piece never writes a block.</p>
     */
    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        if (!GTDungeonLayout.passesAnchorClearance(chunk)
                || !GTDungeonLayout.passesProbability(GTDungeonLayout.seedFor(context.seed(), chunk))) {
            return Optional.empty();
        }
        int y = GTWorldgenScale.remapY(GTDungeonLayout.MIN_Y);
        BoundingBox box = new BoundingBox(chunk.getMinBlockX(), y, chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), y, chunk.getMaxBlockZ());
        BlockPos center = new BlockPos(chunk.getMiddleBlockX(), y, chunk.getMiddleBlockZ());
        return Optional.of(new Structure.GenerationStub(center,
                (StructurePiecesBuilder pieces) -> pieces.addPiece(new GTDungeonAnchorPiece(box))));
    }

    /** The registered type, {@code gregtech:gt_dungeon} (see {@link GTStructures}). */
    @Override
    public StructureType<?> type() {
        return GTStructures.DUNGEON_TYPE.get();
    }
}
