package gregtech.content.worldgen;

import com.mojang.serialization.Codec;
import gregtech.content.worldgen.dungeon.DungeonLayoutRules;
import gregtech.content.worldgen.dungeon.GTDungeonChunkRoomFarmMobs;
import gregtech.content.worldgen.dungeon.GTDungeonLayout;
import gregtech.content.worldgen.dungeon.GTDungeonSharedState;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * GT6's dungeon as a vanilla structure: one piece per cell, written by vanilla while each cell's chunk is
 * generated.
 *
 * <p>GT6 has no structure for its dungeon: {@code WorldgenDungeonGT} was a 1.7.10 world generator entry
 * that wrote the whole grid of cells ({@code Loader_Worldgen}). The port keeps the same cell contents
 * ({@link GTDungeonGenerator}) but lets vanilla's structure system drive them, because only that system
 * gives the dungeon the placement order and the coverage modern worldgen needs:</p>
 * <ul>
 *   <li>{@link #findGenerationPoint} rebuilds the whole layout from the dungeon seed - the same stream
 *       {@link GTDungeonLayout#plan} hands the cell generator - and returns one {@link GTDungeonCellPiece}
 *       per cell that produces blocks. Vanilla therefore calls {@code StructureStart.placeInChunk} for
 *       every dungeon chunk, and the whole dungeon sits inside the structure's bounding box, which is what
 *       {@code StructureCheck}, {@code STRUCTURE_REFERENCES} and {@code /locate} read.</li>
 *   <li>The data file puts the structure in vanilla's {@code top_layer_modification} step: structures of a
 *       step are placed before that step's features, so every dungeon cell is written after the chunk's
 *       decoration (geodes are {@code local_modifications}, ore veins {@code underground_ores}, ...). The
 *       sanctuary therefore wins against the small features that used to carve into it.</li>
 *   <li>Before a cell is written, {@link #intersectsLargeStructure} refuses it when a stronghold, trial
 *       chamber or ancient city covers that chunk, so the dungeon yields to the large structures instead
 *       of overwriting them.</li>
 * </ul>
 *
 * <p>The data files that go with it (all under {@code data/gt6ce/}):</p>
 * <ul>
 *   <li>{@code worldgen/structure/gt_dungeon.json} - {@code "type": "gt6ce:gt_dungeon"} (this class'
 *       {@link #type()}), the biome tag below as its {@code biomes} and the
 *       {@code top_layer_modification} step described above.</li>
 *   <li>{@code tags/worldgen/biome/has_structure/gt_dungeon.json} - {@code #minecraft:is_overworld},
 *       GT6's {@code GEN_OVERWORLD} of {@code Loader_Worldgen}. The tag is also what
 *       {@code ChunkGeneratorStructureState.hasBiomesForStructureSet} reads to decide whether the
 *       structure set belongs to a dimension at all.</li>
 *   <li>{@code worldgen/structure_set/gt_dungeons.json} - one entry, this structure at weight 1, with the
 *       {@code gt6ce:gt_dungeon_lattice} placement (see {@link GTDungeonPlacement}).</li>
 *   <li>{@code tags/worldgen/structure/gt_dungeon.json} - the structure in its own tag, so
 *       {@code /locate structure #gt6ce:gt_dungeon} works as well as the plain id.</li>
 * </ul>
 */
public class GTDungeonStructure extends Structure {

    /** The structure's codec: vanilla's structure settings, like every other structure. */
//? if >=1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<GTDungeonStructure> CODEC = simpleCodec(GTDungeonStructure::new);
*///?} else {
    public static final Codec<GTDungeonStructure> CODEC = simpleCodec(GTDungeonStructure::new);
//?}

    /**
     * The structures the dungeon yields to. Looked up by id at placement time, so a version that does not
     * have one of them (trial chambers arrived in 1.21) simply skips it.
     */
    private static final List<ResourceLocation> LARGE_STRUCTURES = List.of(
            ResourceLocation.fromNamespaceAndPath("minecraft", "stronghold"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "trial_chambers"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "ancient_city"));

    public GTDungeonStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    /**
     * One piece per cell that produces blocks, plus GT6's own gates.
     *
     * <p>GT6 rolls the dungeon once per anchor - {@code WorldgenDungeonGT}'s
     * {@code aRandom.nextInt(mProbability) != 0}, i.e. one dungeon per hundred anchors
     * ({@link GTDungeonLayout#passesProbability} of {@link GTDungeonLayout#seedFor}) - and
     * {@link GTDungeonLayout#plan} rebuilds every later draw (Y offset, colour, rocks, layout) from the
     * same seed, so the structure and the cell generator cannot disagree about where the dungeon is or
     * what it contains.</p>
     *
     * <p>Each piece's box is its cell's full 16x16 column: the entrance tower reaches the surface and the
     * mining room digs to bedrock, so the dungeon's own Y band is not fixed and a narrower box would be
     * wrong.</p>
     */
    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        if (!gregtech.GregTechConfig.Worldgen.DUNGEONS.enabled()) return Optional.empty();
        ChunkPos chunk = context.chunkPos();
        long seed = GTDungeonLayout.seedFor(context.seed(), chunk);
        if (!GTDungeonLayout.passesAnchorClearance(chunk) || !GTDungeonLayout.passesProbability(seed)) {
            return Optional.empty();
        }
        GTDungeonLayout.Plan plan = GTDungeonLayout.plan(seed);
        byte[][] cells = plan.cells();
        GTDungeonSharedState shared = GTDungeonSharedState.create(cells, seed);
        GTDungeonChunkRoomFarmMobs.Footprint farm = GTDungeonChunkRoomFarmMobs.footprint(cells, shared);
        int minY = context.heightAccessor().getMinBuildHeight();
        int maxY = context.heightAccessor().getMaxBuildHeight() - 1;
        BlockPos center = new BlockPos(chunk.getMiddleBlockX(), minY, chunk.getMiddleBlockZ());
        return Optional.of(new Structure.GenerationStub(center, (StructurePiecesBuilder pieces) -> {
            for (int i = 0; i < cells.length; i++) {
                for (int j = 0; j < cells[i].length; j++) {
                    if (!GTDungeonGenerator.writesCell(cells, shared, farm, i, j)) continue;
                    int chunkX = DungeonLayoutRules.cellChunkX(chunk.x, cells.length, i);
                    int chunkZ = DungeonLayoutRules.cellChunkZ(chunk.z, cells[0].length, j);
                    BoundingBox box = new BoundingBox(
                            chunkX * 16, minY, chunkZ * 16, chunkX * 16 + 15, maxY, chunkZ * 16 + 15);
                    pieces.addPiece(new GTDungeonCellPiece(chunk, i, j, box));
                }
            }
        }));
    }

    /**
     * Whether a large structure already covers the given cell. The dungeon is written in the last
     * decoration step, after those structures' pieces exist, so it has to step aside instead of carving
     * through them. A cell is skipped when any start of {@link #LARGE_STRUCTURES} intersects it; the
     * dungeon keeps its other cells, so the sanctuary simply has a hole where the other structure is.
     */
    static boolean intersectsLargeStructure(
            WorldGenLevel level, StructureManager structureManager, ChunkPos chunkPos, BoundingBox cellBox) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Set<Structure> avoided = new HashSet<>();
        for (ResourceLocation id : LARGE_STRUCTURES) {
            Structure structure = registry.get(id);
            if (structure != null) avoided.add(structure);
        }
        if (avoided.isEmpty()) return false;
        for (StructureStart start : structureManager.startsForStructure(chunkPos, avoided::contains)) {
            if (start.getBoundingBox().intersects(cellBox)) return true;
        }
        return false;
    }

    /** The registered type, {@code gt6ce:gt_dungeon} (see {@link GTStructures}). */
    @Override
    public StructureType<?> type() {
        return GTStructures.DUNGEON_TYPE.get();
    }
}
