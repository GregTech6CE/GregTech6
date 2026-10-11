package com.gregtech.gregtech.gametest;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.gregtech.gregtech.block.stone.GTStoneSlabBlock;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.block.stone.StoneVariant;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.worldgen.GTDungeonAnchorPiece;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import com.gregtech.gregtech.worldgen.GTDungeonPlacement;
import com.gregtech.gregtech.worldgen.GTDungeonStructure;
import com.gregtech.gregtech.worldgen.GTStructures;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkBarracks;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkEntrance;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomFarmCrop;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomPortalNether;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonData;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The two user visible halves of the dungeon this batch adds: it can be found with vanilla's locate system
 * ({@code /locate structure gregtech:gt_dungeon}), and its rooms use the port's stone slabs, the way GT6's
 * {@code mSlabs[SIDE_*]} half blocks do.
 *
 * <h2>Finding the dungeon ({@code GTDungeonStructure}, {@code GTDungeonPlacement}, {@code GTStructures})</h2>
 * <p>GT6's dungeon is a feature in the port ({@link GTDungeonFeature}), which vanilla's structure system
 * cannot see, so the port registers a structure that generates nothing on top of it. The tests below walk
 * the whole locate path: the registries and the four data files
 * ({@link #dungeonStructureIsRegisteredForLocate}), the anchor lattice the placement and the feature have
 * to agree on ({@link #dungeonPlacementMatchesTheFeatureLattice}), the lookup itself
 * ({@link #dungeonLocateFindsTheAnchor}) and the promise that the structure adds no blocks
 * ({@link #dungeonStructurePlacesNoBlocks}). Both the structure and the feature roll GT6's
 * one-in-a-hundred gate ({@code WorldgenDungeonGT:150}) from the anchor's seed, so a structure exists
 * exactly where a dungeon does - which is what makes locate answer with a dungeon rather than with one
 * anchor out of a hundred.</p>
 *
 * <h2>The environment's two limits, and what the tests do about them</h2>
 * <p>A Forge game test runs in the flat world of {@code GameTestServer}, which the vanilla code creates
 * with {@code WorldOptions(0L, false, false)} ({@code GameTestServer:64}) - no structure generation - and
 * whose preset replaces the overworld's structure sets with
 * {@code [minecraft:strongholds, minecraft:villages]} ({@code data/minecraft/worldgen/world_preset/flat.json}),
 * which excludes every modded set. So in a game test</p>
 * <ul>
 *   <li>{@code ServerLevel.findNearestMapStructure} answers {@code null} by its very first line
 *       ({@code ServerLevel:1123}), and</li>
 *   <li>the level's own generator state does not hold the dungeon's structure set, so
 *       {@code ChunkGenerator.findNearestMapStructure}'s candidate map ({@code ChunkGenerator:122-133})
 *       stays empty.</li>
 * </ul>
 * <p>Both are properties of the test world, not of the port. The tests therefore assert everything that
 * does not depend on them for real - the structure state a normal overworld builds
 * ({@code ChunkGenerator:102-104} is exactly {@code ChunkGeneratorStructureState.createForNormal} over all
 * registered structure sets, and {@link #dungeonStructureIsRegisteredForLocate} builds that state), the
 * placement's own answers, {@code StructureCheck} and the structure start - and mirror the two loops that
 * only walk those answers ({@code ChunkGenerator:158-181} and {@code :216-257}) in
 * {@link #locateDungeonAnchor}, line by line. {@link #dungeonStructurePlacesNoBlocks} says in its own
 * javadoc which single line of {@code ChunkGenerator.createStructures} the flat preset makes unreachable
 * and what the test does instead.</p>
 *
 * <h2>The slabs ({@code GTDungeonData#slab} and the rooms that use it)</h2>
 * <p>GT6's {@code BlockMetaType.mSlabs} ({@code BlockMetaType:74-81}) holds six half blocks of every rock,
 * one per side ({@code CS:516-521}: {@code SIDE_Y_NEG = 0}, {@code SIDE_Y_POS = 1}, {@code SIDE_Z_NEG = 2},
 * {@code SIDE_Z_POS = 3}, {@code SIDE_X_NEG = 4}, {@code SIDE_X_POS = 5}), and their bounds
 * ({@code BlockMetaType:121-128}) are the port's {@link GTStoneSlabBlock} collision boxes exactly.
 * {@link #dungeonSlabsMatchGt6SidesAndRocks} pins that mapping, and
 * {@link #dungeonRoomsBuildGt6Slabs} asserts the real block states the four converted rooms write - the
 * farm's plot rims ({@code DungeonChunkRoomFarmCrop:42-55}), the barracks' inner walls
 * ({@code DungeonChunkBarracks:48-51}), the entrance's staircase ({@code DungeonChunkEntrance:118-193}) and
 * the portal rooms' kerbs and soul sand rows ({@code DungeonChunkRoomPortal:43-158},
 * {@code DungeonChunkRoomPortalNether:77-84}).</p>
 *
 * <p>The suite keeps to its own area at 66000/66000, 2000 blocks above {@code PileBlockTests}' 64000, and
 * {@link #reset} empties it before every test, so the file is re-runnable against a world it already ran
 * in.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class DungeonLocateAndSlabsTests {

    /** The suite's own base, clear of every other suite's (the highest is 64000, see PileBlockTests). */
    private static final int BASE_X = 66000;
    private static final int BASE_Z = 66000;
    private static final int BASE_Y = 200;

    /** One 32-block site per built cell, plus the first for the slab helper's own writes. */
    private static final int SITE_SLABS = 0, SITE_FARM = 1, SITE_BARRACKS = 2, SITE_ENTRANCE = 3, SITE_NETHER = 4;
    private static final int SITES = 5;

    /** GT6's lattice anchor these tests drive: {@code abs(27) % 11 == 5} (see {@code GTDungeonLayout}). */
    private static final ChunkPos LATTICE = new ChunkPos(27, 27);
    /** A second lattice point, for the "the structure places nothing" check. */
    private static final ChunkPos SECOND_LATTICE = new ChunkPos(27, 60);

    /** The dungeon's two rocks, GT6's {@code BlocksGT.stones[rnd]}. */
    private static final StoneType PRIMARY = StoneType.LIMESTONE, SECONDARY = StoneType.SLATE;
    private static final long SEED = 20260916L;

    /**
     * GT6's side constants ({@code CS:516-521}), copied as literals: {@code BlockMetaType.mSlabs} is
     * indexed with them, and the port maps them onto vanilla's {@code Direction} by ordinal.
     */
    private static final int GT6_SIDE_Y_NEG = 0, GT6_SIDE_Y_POS = 1, GT6_SIDE_Z_NEG = 2, GT6_SIDE_Z_POS = 3,
            GT6_SIDE_X_NEG = 4, GT6_SIDE_X_POS = 5;

    private static JsonObject resourceJson(String path) throws Exception {
        try (InputStream stream = DungeonLocateAndSlabsTests.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("missing resource " + path);
            return GsonHelper.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }

    /** One of the suite's own sites, 32 blocks apart inside its reserved area. */
    private static BlockPos site(int index) {
        return new BlockPos(BASE_X + index * 32, BASE_Y, BASE_Z);
    }

    private static BlockState at(GameTestHelper helper, BlockPos origin, int x, int y, int z) {
        return helper.getLevel().getBlockState(origin.offset(x, y, z));
    }

    private static void check(List<String> problems, boolean condition, String message) {
        if (!condition) problems.add(message);
    }

    /** Empties the suite's own sites, so every test starts from air. */
    private static void reset(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int index = 0; index < SITES; index++) {
            BlockPos origin = site(index);
            // One cell is 16 blocks wide and reaches 13 blocks up (the entrance's shaft) and 4 down
            // (the pillar under it).
            level.getChunkAt(origin);
            for (int x = origin.getX() - 2; x <= origin.getX() + 17; x++) {
                for (int y = origin.getY() - 4; y <= origin.getY() + 14; y++) {
                    for (int z = origin.getZ() - 2; z <= origin.getZ() + 17; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (!level.getBlockState(pos).isAir()) level.removeBlock(pos, false);
                    }
                }
            }
        }
    }

    /** A cell of the suite's own area, with the two rocks and the seed every test here uses. */
    private static GTDungeonData data(ServerLevel level, BlockPos origin, byte[][] cells) {
        int keys = GTDungeonFeature.KEY_COUNT;
        return new GTDungeonData(level, origin.getX(), origin.getY(), origin.getZ(), PRIMARY, SECONDARY, 3, cells,
                2, 2, GTDungeonLayout.connectionCount(cells, 2, 2), new long[keys], new ItemStack[keys],
                new boolean[keys], new HashSet<>(), new HashSet<>(), RandomSource.create(SEED));
    }

    /**
     * A room cell whose only neighbour is west; every other side is empty, which is what makes the crop
     * farm build its gardens ({@code DungeonChunkRoomFarmCrop:152-226}) and keeps the room's own rims
     * clear of them.
     */
    private static byte[][] roomCell() {
        byte[][] cells = new byte[5][5];
        cells[2][2] = 1;
        cells[1][2] = GTDungeonLayout.CORRIDOR;
        return cells;
    }

    /** A dead end whose only connection leads east, so GT6's +X block is the one that fires. */
    private static byte[][] deadEndCell() {
        byte[][] cells = new byte[5][5];
        cells[2][2] = 1;
        cells[3][2] = GTDungeonLayout.CORRIDOR;
        return cells;
    }

    /** The block position locate reports for an anchor: the chunk's corner plus the placement's offset. */
    private static BlockPos anchorPos(ChunkPos anchor) {
        return new BlockPos(anchor.getMinBlockX(), 0, anchor.getMinBlockZ());
    }

    /**
     * The nearest spawn-clear lattice point to {@code from} whose probability gate answers {@code pass}
     * for this world's seed. Both the feature and structure reject anchors inside GT6's spawn clearance
     * before they can generate a dungeon, regardless of the one-in-a-hundred roll. Keep both the passing
     * and rejected samples outside that area so the test isolates the probability gate. The spiral walks
     * the regions around {@code from} until either turns up.
     */
    private static ChunkPos latticePoint(long worldSeed, ChunkPos from, boolean pass) {
        int regionX = GTDungeonLayout.regionOf(from.x), regionZ = GTDungeonLayout.regionOf(from.z);
        for (int radius = 0; radius <= 40; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    ChunkPos candidate = new ChunkPos(GTDungeonLayout.anchorOfRegion(regionX + dx),
                            GTDungeonLayout.anchorOfRegion(regionZ + dz));
                    if (!GTDungeonLayout.passesAnchorClearance(candidate)) continue;
                    if (GTDungeonLayout.passesProbability(GTDungeonLayout.seedFor(worldSeed, candidate)) == pass) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    // -- the structure, its set and its data files -----------------------------------------------

    /** The structure, its structure set, its two code registries and the four data files vanilla reads. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void dungeonStructureIsRegisteredForLocate(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();

        // The structure itself, with the type that names its codec (data/gregtech/worldgen/structure/...).
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        check(problems, structures.containsKey(GTStructures.DUNGEON), "gregtech:gt_dungeon is not registered");
        Structure dungeon = structures.get(GTStructures.DUNGEON);
        check(problems, dungeon instanceof GTDungeonStructure, "gt_dungeon is not the port's structure");
        check(problems, dungeon != null && dungeon.type() == GTStructures.DUNGEON_TYPE.get(),
                "gt_dungeon does not use the registered structure type");
        check(problems, BuiltInRegistries.STRUCTURE_TYPE.containsKey(GTStructures.DUNGEON_TYPE.getId()),
                "gregtech:gt_dungeon is not a registered structure type");
        check(problems, BuiltInRegistries.STRUCTURE_PLACEMENT.containsKey(GTStructures.DUNGEON_LATTICE.getId()),
                "gregtech:gt_dungeon_lattice is not a registered placement type");
        check(problems, BuiltInRegistries.STRUCTURE_PIECE.containsKey(GTStructures.DUNGEON_PIECE.getId()),
                "gregtech:gt_dungeon_anchor is not a registered piece type");

        // The structure set, holding exactly that structure at weight 1.
        var sets = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        check(problems, sets.containsKey(GTStructures.DUNGEON_SET), "gregtech:gt_dungeons is not registered");
        StructureSet set = sets.get(GTStructures.DUNGEON_SET);
        check(problems, set != null && set.structures().size() == 1
                        && set.structures().get(0).structure().is(GTStructures.DUNGEON)
                        && set.structures().get(0).weight() == 1,
                "the dungeon's structure set does not hold the dungeon at weight 1");
        // Locate only ever searches these two placement types (ChunkGenerator:142 and :153-156), so the
        // dungeon's placement has to be a random spread - see GTDungeonPlacement's javadoc.
        check(problems, set != null && set.placement() instanceof GTDungeonPlacement,
                "the dungeon's structure set does not use gregtech:gt_dungeon_lattice");
        check(problems, set != null && set.placement() instanceof RandomSpreadStructurePlacement,
                "vanilla's locate does not search this placement type");
        RandomSpreadStructurePlacement spread = set != null && set.placement() instanceof RandomSpreadStructurePlacement s
                ? s : null;
        check(problems, spread != null && spread.spacing() == GTDungeonLayout.GRID_PERIOD && spread.separation() == 0,
                "the placement is not GT6's grid of 11 chunks with no separation");

        // The biome gate: ChunkGeneratorStructureState.hasBiomesForStructureSet (:59-65) only admits a
        // structure set whose structure's biomes overlap the dimension's biome source - this is the plain
        // #minecraft:is_overworld tag GT6's GEN_OVERWORLD stands for.
        BiomeSource biomes = level.getChunkSource().getGenerator().getBiomeSource();
        Set<Holder<Biome>> possible = biomes.possibleBiomes();
        check(problems, dungeon != null && dungeon.biomes().stream().anyMatch(possible::contains),
                "the dungeon's biomes do not overlap this dimension's, so no dimension would hold it");

        // The state a normal overworld builds (ChunkGenerator:102-104) has to list the placement locate
        // walks; with an empty list ChunkGenerator.findNearestMapStructure gives up at :132-133.
        RandomState randomState = level.getChunkSource().randomState();
        HolderLookup.RegistryLookup<StructureSet> allSets = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        ChunkGeneratorStructureState state = ChunkGeneratorStructureState.createForNormal(randomState,
                level.getSeed(), biomes, allSets);
        check(problems, state.possibleStructureSets().stream().anyMatch(holder -> holder.is(GTStructures.DUNGEON_SET)),
                "an overworld's structure state would not hold the dungeon's structure set");
        List<StructurePlacement> placements = dungeon == null ? List.of()
                : state.getPlacementsForStructure(structures.getHolderOrThrow(GTStructures.DUNGEON));
        check(problems, placements.stream().anyMatch(placement -> placement instanceof GTDungeonPlacement),
                "locate would not find the dungeon's placement for the dungeon structure");

        // The tag ServerLevel.findNearestMapStructure looks up before it calls the generator (:1126-1129).
        var tag = structures.getTag(GTStructures.DUNGEON_TAG);
        check(problems, tag.isPresent(), "gregtech:gt_dungeon is not in the structure tag of the same name");
        check(problems, tag.isPresent() && tag.get().size() == 1 && tag.get().contains(structures.getHolderOrThrow(GTStructures.DUNGEON)),
                "the dungeon's structure tag does not hold exactly the dungeon");

        // The data files themselves, with the exact values the placement and the structure need.
        JsonObject structureJson = resourceJson("data/gregtech/worldgen/structure/gt_dungeon.json");
        check(problems, structureJson.get("type").getAsString().equals("gregtech:gt_dungeon"),
                "the structure's type is " + structureJson.get("type").getAsString());
        check(problems, structureJson.get("biomes").getAsString().equals("#gregtech:has_structure/gt_dungeon"),
                "the structure does not use the has_structure tag");
        check(problems, structureJson.get("step").getAsString().equals("underground_structures"),
                "the structure is not in the underground structures step");
        check(problems, structureJson.has("spawn_overrides")
                        && structureJson.getAsJsonObject("spawn_overrides").size() == 0,
                "the dungeon spawns nothing of its own");

        JsonObject setJson = resourceJson("data/gregtech/worldgen/structure_set/gt_dungeons.json");
        JsonObject placementJson = setJson.getAsJsonObject("placement");
        check(problems, placementJson.get("type").getAsString().equals("gregtech:gt_dungeon_lattice"),
                "the structure set's placement type is " + placementJson.get("type").getAsString());
        check(problems, placementJson.get("spacing").getAsInt() == GTDungeonLayout.GRID_PERIOD,
                "the placement's spacing is not GT6's grid period of 11");
        check(problems, placementJson.get("separation").getAsInt() == 0,
                "the placement separates chunks off, which GT6's lattice does not");
        check(problems, placementJson.get("salt").getAsInt() == GTDungeonPlacement.SALT,
                "the placement's salt is " + placementJson.get("salt").getAsInt());
        check(problems, setJson.getAsJsonArray("structures").size() == 1
                        && setJson.getAsJsonArray("structures").get(0).getAsJsonObject()
                        .get("structure").getAsString().equals("gregtech:gt_dungeon"),
                "the structure set points somewhere else");
        // The codec has to be read the way the loader reads it, and it has to be a MapCodecCodec: DFU's
        // KeyDispatchCodec (behind StructurePlacement.CODEC) only hands the JSON object to a MapCodecCodec,
        // anything else makes it look for a nested "value" entry and fail with "Not a JSON object: null" -
        // which is how the first version of this structure set broke the whole datapack load. Parsing
        // GTDungeonPlacement.CODEC directly would not have seen that.
        check(problems, GTDungeonPlacement.CODEC instanceof MapCodec.MapCodecCodec,
                "the placement codec is not a MapCodecCodec: " + GTDungeonPlacement.CODEC.getClass().getName());
        var dispatched = StructurePlacement.CODEC.parse(JsonOps.INSTANCE, placementJson);
        check(problems, dispatched.result().isPresent(), "the placement JSON does not decode: " + dispatched);
        check(problems, dispatched.result().isPresent()
                        && dispatched.result().get() instanceof GTDungeonPlacement
                        && ((GTDungeonPlacement) dispatched.result().get()).spacing() == GTDungeonLayout.GRID_PERIOD
                        && ((GTDungeonPlacement) dispatched.result().get()).separation() == 0,
                "the dispatched placement is not GT6's lattice: " + dispatched);
        JsonObject wrongSpacing = placementJson.deepCopy();
        wrongSpacing.addProperty("spacing", GTDungeonLayout.GRID_PERIOD + 1);
        check(problems, StructurePlacement.CODEC.parse(JsonOps.INSTANCE, wrongSpacing).error().isPresent(),
                "the placement codec accepts a spacing that is not GT6's lattice");
        // And the whole file through the ops the loader uses (RegistryDataLoader:140-143).
        var loaded = StructureSet.DIRECT_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, level.registryAccess()),
                setJson);
        check(problems, loaded.result().isPresent(), "the structure set file does not load: " + loaded);
        check(problems, loaded.result().isPresent() && loaded.result().get().structures().size() == 1
                        && loaded.result().get().structures().get(0).structure().is(GTStructures.DUNGEON)
                        && loaded.result().get().placement() instanceof GTDungeonPlacement,
                "the loaded structure set does not hold the dungeon and the lattice: " + loaded);

        JsonObject biomeTag = resourceJson("data/gregtech/tags/worldgen/biome/has_structure/gt_dungeon.json");
        check(problems, biomeTag.getAsJsonArray("values").size() == 1
                        && biomeTag.getAsJsonArray("values").get(0).getAsString().equals("#minecraft:is_overworld"),
                "the has_structure biome tag is not GT6's overworld");
        JsonObject structureTag = resourceJson("data/gregtech/tags/worldgen/structure/gt_dungeon.json");
        check(problems, structureTag.getAsJsonArray("values").size() == 1
                        && structureTag.getAsJsonArray("values").get(0).getAsString().equals("gregtech:gt_dungeon"),
                "the structure tag does not name the dungeon");

        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    // -- the lattice -----------------------------------------------------------------------------

    /**
     * The placement and the feature have to accept the very same chunks: GT6 generates from the chunk that
     * satisfies {@code abs(chunk) % 11 == 5} on both axes ({@code WorldgenDungeonGT:151-152}), which is what
     * {@link GTDungeonLayout#anchorFor} answers for a chunk and what {@code /locate} now has to report.
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void dungeonPlacementMatchesTheFeatureLattice(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        StructureSet set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(GTStructures.DUNGEON_SET);
        helper.assertTrue(set != null, "gregtech:gt_dungeons is not loaded");
        StructurePlacement placement = set.placement();
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        long seed = level.getSeed();

        // Every chunk of a 121 x 121 area: the placement accepts it exactly when the feature has an anchor
        // for it, and an accepted chunk is its own anchor.
        for (int chunkX = -60; chunkX <= 60; chunkX++) {
            for (int chunkZ = -60; chunkZ <= 60; chunkZ++) {
                boolean accepted = placement.isStructureChunk(state, chunkX, chunkZ);
                boolean anchor = GTDungeonLayout.isAnchor(chunkX) && GTDungeonLayout.isAnchor(chunkZ);
                if (accepted != anchor) {
                    problems.add("the placement " + (accepted ? "accepts" : "rejects") + " chunk " + chunkX
                            + "/" + chunkZ + " which the feature's lattice does not");
                    break;
                }
                if (!accepted) continue;
                ChunkPos featureAnchor = GTDungeonLayout.anchorFor(chunkX, chunkZ);
                if (featureAnchor == null || featureAnchor.x != chunkX || featureAnchor.z != chunkZ) {
                    problems.add("the feature has no anchor for " + chunkX + "/" + chunkZ
                            + ", but the placement does: " + featureAnchor);
                    break;
                }
            }
            if (!problems.isEmpty()) break;
        }
        // A chunk just off the lattice is refused by both.
        check(problems, !placement.isStructureChunk(state, 21, 21) && GTDungeonLayout.anchorFor(21, 21) == null,
                "chunk 21/21 lies 5 chunks from the anchor at 16, outside the layout, but is a dungeon");
        check(problems, !placement.isStructureChunk(state, 11, 11) && GTDungeonLayout.anchorFor(11, 11) == null,
                "chunk 11/11 is the gap between the anchors at 5 and 16, but is a dungeon");

        // Locate asks a placement for the structure chunk of a grid region (ChunkGenerator:227); every
        // answer has to be that region's anchor, for any seed - GT6's lattice holds no randomness.
        for (int region = -6; region <= 6; region++) {
            int probe = region * GTDungeonLayout.GRID_PERIOD + 3;
            int expected = GTDungeonLayout.anchorOfRegion(region);
            for (long anySeed : new long[]{seed, seed + 1L, -seed}) {
                ChunkPos potential = ((RandomSpreadStructurePlacement) placement)
                        .getPotentialStructureChunk(anySeed, probe, probe);
                if (potential.x != expected || potential.z != expected) {
                    problems.add("the region " + region + " answers with " + potential
                            + " instead of its anchor " + expected);
                    break;
                }
            }
            check(problems, GTDungeonLayout.isAnchor(expected) && GTDungeonLayout.regionOf(expected) == region,
                    "the anchor " + expected + " of the region " + region + " is not GT6's lattice point");
            check(problems, placement.isStructureChunk(state, expected, expected),
                    "the placement does not accept its own region anchor " + expected);
        }
        // The anchor the locate test drives, straight from the feature's own math.
        check(problems, ((RandomSpreadStructurePlacement) placement).getPotentialStructureChunk(seed, LATTICE.x, LATTICE.z)
                        .equals(LATTICE),
                "the placement answers " + ((RandomSpreadStructurePlacement) placement)
                        .getPotentialStructureChunk(seed, LATTICE.x, LATTICE.z) + " for the lattice chunk " + LATTICE);
        check(problems, GTDungeonLayout.anchorFor(LATTICE.x, LATTICE.z) != null
                        && GTDungeonLayout.anchorFor(LATTICE.x, LATTICE.z).equals(LATTICE),
                "the feature does not build a dungeon from " + LATTICE);
        // The placement is the plain lattice: GT6's one-in-a-hundred gate is the dungeon's and the
        // structure's, not the placement's (see GTDungeonPlacement's javadoc). Both sides of that gate use
        // the same number - the feature reads level.getSeed(), the structure and the chunk generation read
        // ChunkGeneratorStructureState.getLevelSeed() (ChunkGenerator:437/481).
        check(problems, state.getLevelSeed() == level.getSeed(),
                "the generator state's seed " + state.getLevelSeed() + " is not the level's " + level.getSeed());

        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    // -- the lookup ------------------------------------------------------------------------------

    /**
     * The lookup itself: locate walks the ring of grid regions around the queried position
     * ({@code ChunkGenerator:158-181} + {@code :216-237}), asks the placement for each region's chunk, and
     * then applies vanilla's own gates - the structure has to be reachable
     * ({@code StructureCheck.checkStart}, which is also where GT6's one-in-a-hundred gate shows: an anchor
     * that fails it has no generation point, so it answers {@code START_NOT_PRESENT} and costs not even a
     * chunk), the chunk has to hold a valid start once it is generated to {@code STRUCTURE_STARTS}
     * ({@code ChunkGenerator:240-257}), and the answer is
     * {@code placement.getLocatePos(start.getChunkPos())}.
     *
     * <p>Both directions are asserted: an anchor whose gate passed is answered with its own block position,
     * and an anchor whose gate failed is not answered at all - which is what makes {@code /locate} point at
     * a dungeon instead of at one anchor out of a hundred.</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void dungeonLocateFindsTheAnchor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Holder<Structure> holder = structures.getHolderOrThrow(GTStructures.DUNGEON);
        Structure dungeon = holder.value();

        // A normal overworld's structure state (see the class javadoc).
        BiomeSource biomes = level.getChunkSource().getGenerator().getBiomeSource();
        RandomState randomState = level.getChunkSource().randomState();
        HolderLookup.RegistryLookup<StructureSet> allSets = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        ChunkGeneratorStructureState state = ChunkGeneratorStructureState.createForNormal(randomState,
                level.getSeed(), biomes, allSets);

        // The nearest lattice point that GT6's gate accepts - the nearest anchor that really holds a
        // dungeon, straight from the port's own anchor math.
        ChunkPos anchor = latticePoint(level.getSeed(), LATTICE, true);
        check(problems, anchor != null, "no dungeon anchor within 40 grid steps of " + LATTICE);
        if (anchor == null) {
            helper.assertTrue(false, String.join("; ", problems));
            return;
        }
        BlockPos anchorPos = anchorPos(anchor);
        check(problems, GTDungeonLayout.anchorFor(anchor.x, anchor.z) != null
                        && GTDungeonLayout.anchorFor(anchor.x, anchor.z).equals(anchor),
                "the feature does not build a dungeon from the anchor " + anchor);
        check(problems, GTDungeonLayout.passesProbability(GTDungeonLayout.seedFor(level.getSeed(), anchor)),
                "the anchor " + anchor + " does not pass GT6's own gate");
        check(problems, GTDungeonLayout.passesAnchorClearance(anchor),
                "the anchor " + anchor + " falls inside GT6's spawn clearance");

        // Locate from the anchor's own block position: ring 0 has to answer with this very anchor.
        BlockPos found = locateDungeonAnchor(level, state, holder, dungeon, anchorPos, 2, false);
        check(problems, anchorPos.equals(found), "locate answered " + found + " instead of " + anchorPos);
        // From a neighbouring chunk of the same region and from the anchor of the very next grid step,
        // the answer is the same anchor - the region's own lattice point.
        ChunkPos neighbour = new ChunkPos(anchor.x + 2, anchor.z + 2);
        check(problems, GTDungeonLayout.regionOf(neighbour.x) == GTDungeonLayout.regionOf(anchor.x)
                        && GTDungeonLayout.regionOf(neighbour.z) == GTDungeonLayout.regionOf(anchor.z),
                "chunk " + neighbour + " is not in the anchor's own grid region");
        check(problems, anchorPos.equals(locateDungeonAnchor(level, state, holder, dungeon,
                        new BlockPos(neighbour.getMiddleBlockX(), 64, neighbour.getMiddleBlockZ()), 0, false)),
                "locate does not answer the region's anchor from a neighbouring chunk");
        ChunkPos next = new ChunkPos(GTDungeonLayout.anchorOfRegion(GTDungeonLayout.regionOf(anchor.x) + 1),
                GTDungeonLayout.anchorOfRegion(GTDungeonLayout.regionOf(anchor.z) + 1));
        check(problems, GTDungeonLayout.anchorFor(next.x, next.z) != null
                        && GTDungeonLayout.anchorFor(next.x, next.z).equals(next),
                "the next lattice chunk " + next + " is not a lattice point of the feature");
        boolean nextHasDungeon = GTDungeonLayout.passesAnchorClearance(next)
                && GTDungeonLayout.passesProbability(GTDungeonLayout.seedFor(level.getSeed(), next));
        BlockPos atNext = locateDungeonAnchor(level, state, holder, dungeon, anchorPos(next), 0, false);
        check(problems, nextHasDungeon ? anchorPos(next).equals(atNext) : atNext == null,
                "locate answered " + atNext + " for the lattice chunk " + next + ", whose gate "
                        + (nextHasDungeon ? "passed" : "failed"));

        // The other direction: an anchor whose gate failed holds no dungeon, so locate reports nothing for
        // it - and the chunk is not even forced for it (StructureCheck answers START_NOT_PRESENT).
        ChunkPos rejected = latticePoint(level.getSeed(), LATTICE, false);
        check(problems, rejected != null && !rejected.equals(anchor),
                "the failing anchor is not a second lattice point");
        if (rejected != null) {
            check(problems, !GTDungeonLayout.passesProbability(GTDungeonLayout.seedFor(level.getSeed(), rejected)),
                    "the failing anchor " + rejected + " passed GT6's gate after all");
            check(problems, locateDungeonAnchor(level, state, holder, dungeon, anchorPos(rejected), 0, false) == null,
                    "locate answers an anchor whose gate GT6's dungeon did not pass");
        }

        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    /**
     * Vanilla's locate, mirrored from {@code ChunkGenerator:120-186} and {@code :216-257} because a game
     * test world excludes modded structure sets (see the class javadoc). Every decision inside is vanilla's
     * own: {@code ChunkGeneratorStructureState.getPlacementsForStructure} for the candidate list,
     * {@code RandomSpreadStructurePlacement.getPotentialStructureChunk} for the region's chunk,
     * {@code StructureManager.checkStructurePresence} and
     * {@code StructureManager.getStartForStructure} for the start, and
     * {@code StructurePlacement.getLocatePos} for the answer.
     *
     * <p>The one thing the mirror does not reproduce is that a real overworld's chunk generation stores
     * the start by itself ({@code ChunkGenerator.createStructures}). The flat test world's structure
     * overrides make that unreachable here, so {@link #anchorStart} makes the same call and stores it,
     * after which the rest of the chain is vanilla's again.</p>
     */
    private static BlockPos locateDungeonAnchor(ServerLevel level, ChunkGeneratorStructureState state,
                                                Holder<Structure> holder, Structure dungeon, BlockPos origin,
                                                int radius, boolean skipExistingChunks) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        int centerX = SectionPos.blockToSectionCoord(origin.getX());
        int centerZ = SectionPos.blockToSectionCoord(origin.getZ());
        long seed = state.getLevelSeed();

        for (int ring = 0; ring <= radius; ring++) {
            for (StructurePlacement placement : state.getPlacementsForStructure(holder)) {
                // ChunkGenerator:153-156 skips every placement that is not a random spread.
                if (!(placement instanceof RandomSpreadStructurePlacement spread)) continue;
                int spacing = spread.spacing();
                for (int j = -ring; j <= ring; j++) {
                    for (int k = -ring; k <= ring; k++) {
                        // ChunkGenerator:224: only the border of the ring is searched.
                        if (Math.abs(j) != ring && Math.abs(k) != ring) continue;
                        ChunkPos candidate = spread.getPotentialStructureChunk(seed,
                                centerX + spacing * j, centerZ + spacing * k);
                        // ChunkGenerator:240-257, in its own order: the presence check first - this is where
                        // the structure's own gate shows - and the chunk's start only afterwards.
                        StructureCheckResult presence = level.structureManager()
                                .checkStructurePresence(candidate, dungeon, skipExistingChunks);
                        if (presence == StructureCheckResult.START_NOT_PRESENT) continue;
                        if (!skipExistingChunks && presence == StructureCheckResult.START_PRESENT) {
                            return placement.getLocatePos(candidate);
                        }
                        ChunkAccess chunk = level.getChunk(candidate.x, candidate.z, ChunkStatus.STRUCTURE_STARTS);
                        anchorStart(level, generator, randomState, dungeon, candidate);
                        StructureStart start = level.structureManager()
                                .getStartForStructure(SectionPos.bottomOf(chunk), dungeon, chunk);
                        if (start == null || !start.isValid()) continue;
                        if (skipExistingChunks && !start.canBeReferenced()) continue;
                        return placement.getLocatePos(start.getChunkPos());
                    }
                }
            }
        }
        return null;
    }

    /**
     * The start of the anchor chunk, as {@code ChunkGenerator.createStructures} would have stored it. A
     * game test world replaces the overworld's structure sets with {@code [minecraft:strongholds,
     * minecraft:villages]} ({@code data/minecraft/worldgen/world_preset/flat.json}), so no structure system
     * of this level ever reaches the dungeon and the test makes that one call itself - with the very
     * generation context vanilla uses.
     */
    private static StructureStart anchorStart(ServerLevel level, ChunkGenerator generator, RandomState randomState,
                                              Structure dungeon, ChunkPos pos) {
        ChunkAccess chunk = level.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS);
        StructureManager manager = level.structureManager();
        StructureStart existing = manager.getStartForStructure(SectionPos.bottomOf(chunk), dungeon, chunk);
        if (existing != null && existing.isValid()) return existing;
        StructureStart start = dungeon.generate(level.registryAccess(), generator, generator.getBiomeSource(),
                randomState, level.getStructureManager(), level.getSeed(), pos, 0, level, dungeon.biomes()::contains);
        if (start.isValid()) manager.setStartForStructure(SectionPos.bottomOf(chunk), dungeon, start, chunk);
        return start.isValid() ? start : null;
    }

    // -- the structure generates nothing ---------------------------------------------------------

    /**
     * The structure exists so locate can answer; the dungeon's blocks still come from
     * {@link GTDungeonFeature}. This asserts both halves: an anchor whose gate passed carries a valid start
     * with exactly one piece, that piece is the port's {@link GTDungeonAnchorPiece}, and running the piece -
     * {@code StructureStart.placeInChunk}, the only way a structure can write a block
     * ({@code StructureStart:81-96}) - leaves every one of the chunk's block states untouched. An anchor
     * whose gate failed carries no start at all, which is what keeps locate from reporting empty anchors.
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void dungeonStructurePlacesNoBlocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        Structure dungeon = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(GTStructures.DUNGEON);
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();

        check(problems, dungeon != null, "the dungeon structure is not registered");
        if (dungeon == null) {
            helper.assertTrue(false, String.join("; ", problems));
            return;
        }
        // The nearest anchor around the second lattice point that really holds a dungeon.
        ChunkPos quiet = latticePoint(level.getSeed(), SECOND_LATTICE, true);
        check(problems, quiet != null, "no dungeon anchor within 40 grid steps of " + SECOND_LATTICE);
        ChunkPos rejected = latticePoint(level.getSeed(), SECOND_LATTICE, false);
        if (quiet == null || rejected == null) {
            helper.assertTrue(false, String.join("; ", problems));
            return;
        }
        check(problems, anchorStart(level, generator, randomState, dungeon, rejected) == null,
                "the anchor " + rejected + ", whose gate GT6's dungeon did not pass, holds a structure start");

        StructureStart start = anchorStart(level, generator, randomState, dungeon, quiet);
        check(problems, start != null, "the anchor chunk " + quiet + " holds no structure start");
        if (start == null) {
            helper.assertTrue(false, String.join("; ", problems));
            return;
        }
        check(problems, start.isValid(), "the start of the anchor chunk is not valid, so locate cannot see it");
        check(problems, start.getChunkPos().equals(quiet),
                "the start belongs to " + start.getChunkPos() + " instead of the anchor " + quiet);
        check(problems, start.getPieces().size() == 1 && start.getPieces().get(0) instanceof GTDungeonAnchorPiece,
                "the start holds " + start.getPieces().size() + " pieces, expected the one anchor piece");
        check(problems, start.getBoundingBox().minX() == quiet.getMinBlockX()
                        && start.getBoundingBox().maxX() == quiet.getMaxBlockX()
                        && start.getBoundingBox().minZ() == quiet.getMinBlockZ()
                        && start.getBoundingBox().maxZ() == quiet.getMaxBlockZ(),
                "the anchor piece's box is not its own chunk: " + start.getBoundingBox());

        // The proof itself: a whole chunk, before and after the piece has run.
        ChunkPos pos = start.getChunkPos();
        ChunkAccess chunk = level.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS);
        BlockState[] before = snapshot(level, pos);
        BoundingBox box = new BoundingBox(pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ(),
                pos.getMaxBlockX(), level.getMaxBuildHeight() - 1, pos.getMaxBlockZ());
        start.placeInChunk(level, level.structureManager(), generator, RandomSource.create(1L), box, pos);
        BlockState[] after = snapshot(level, pos);
        int changed = 0;
        String firstChange = "";
        for (int index = 0; index < before.length; index++) {
            if (before[index] == after[index]) continue;
            changed++;
            if (firstChange.isEmpty()) {
                int y = level.getMinBuildHeight() + index / 256;
                int z = pos.getMinBlockZ() + (index / 16) % 16;
                int x = pos.getMinBlockX() + index % 16;
                firstChange = " (" + x + "," + y + "," + z + "): " + before[index] + " -> " + after[index];
            }
        }
        check(problems, changed == 0, "the structure wrote " + changed + " blocks" + firstChange);
        check(problems, chunk.getStartForStructure(dungeon) == start,
                "the anchor chunk does not carry the start the test stored");

        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    /** Every block state of a chunk, in a fixed order, for a before/after comparison. */
    private static BlockState[] snapshot(ServerLevel level, ChunkPos pos) {
        int minY = level.getMinBuildHeight();
        int height = level.getMaxBuildHeight() - minY;
        BlockState[] states = new BlockState[16 * 16 * height];
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    states[(y * 16 + z) * 16 + x] = level.getBlockState(
                            new BlockPos(pos.getMinBlockX() + x, minY + y, pos.getMinBlockZ() + z));
                }
            }
        }
        return states;
    }

    // -- the slabs -------------------------------------------------------------------------------

    /**
     * GT6's six slab sides and the two rocks: GT6 indexes {@code mSlabs} with its side constants
     * ({@code CS:516-521}) and fills the half that side names ({@code BlockMetaType:121-128}); this pins
     * that mapping onto vanilla's {@link Direction} ordinals and the port's {@link GTStoneSlabBlock}, and
     * checks the rock GT6 picks by local height ({@code aY == 2 ? mSecondary : mPrimary},
     * {@code DungeonData:136-150}).
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void dungeonSlabsMatchGt6SidesAndRocks(GameTestHelper helper) {
        reset(helper);
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        BlockPos origin = site(SITE_SLABS);
        GTDungeonData data = data(level, origin, roomCell());

        // GT6's side constants are vanilla's ordinals, so a GT6 mSlabs index is a Direction as it stands.
        Direction[] gt6Sides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        check(problems, Direction.values()[GT6_SIDE_Y_NEG] == gt6Sides[GT6_SIDE_Y_NEG]
                        && Direction.values()[GT6_SIDE_Y_POS] == gt6Sides[GT6_SIDE_Y_POS]
                        && Direction.values()[GT6_SIDE_Z_NEG] == gt6Sides[GT6_SIDE_Z_NEG]
                        && Direction.values()[GT6_SIDE_Z_POS] == gt6Sides[GT6_SIDE_Z_POS]
                        && Direction.values()[GT6_SIDE_X_NEG] == gt6Sides[GT6_SIDE_X_NEG]
                        && Direction.values()[GT6_SIDE_X_POS] == gt6Sides[GT6_SIDE_X_POS],
                "vanilla's Direction order is not GT6's CS:516-521 side order");

        // Local Y 1 uses the primary rock, local Y 2 the secondary one (GT6's DungeonData:136-150).
        for (int side = 0; side < gt6Sides.length; side++) {
            Direction direction = gt6Sides[side];
            check(problems, data.smoothSlab(side, 1, 0, direction), "the slab at side " + side + " was refused");
            checkSlab(problems, helper, origin, side, 1, 0, GTBlocks.getStoneSlab(PRIMARY, StoneVariant.SMOOTH),
                    direction, "the primary rock's smooth slab of GT6's side " + side);
            data.smoothSlab(side, 2, 0, direction);
            checkSlab(problems, helper, origin, side, 2, 0, GTBlocks.getStoneSlab(SECONDARY, StoneVariant.SMOOTH),
                    direction, "the secondary rock's smooth slab of GT6's side " + side);
        }
        // The variant is a parameter, and the top/bottom shorthand is GT6's SIDE_Y_POS / SIDE_Y_NEG.
        data.tilesSlab(8, 1, 0, Direction.UP);
        checkSlab(problems, helper, origin, 8, 1, 0, GTBlocks.getStoneSlab(PRIMARY, StoneVariant.TILES),
                Direction.UP, "GT6's mSlabs[SIDE_Y_POS] tile slab");
        data.slab(9, 1, 0, StoneVariant.BRICKS, true);
        checkSlab(problems, helper, origin, 9, 1, 0, GTBlocks.getStoneSlab(PRIMARY, StoneVariant.BRICKS),
                Direction.UP, "the topHalf shorthand");
        data.slab(10, 1, 0, StoneVariant.BRICKS, false);
        checkSlab(problems, helper, origin, 10, 1, 0, GTBlocks.getStoneSlab(PRIMARY, StoneVariant.BRICKS),
                Direction.DOWN, "the bottomHalf shorthand");
        // A kerb run, the shape GT6's portal rooms build their rims with: bottom half tile slabs.
        data.slabKerbs(1, 3, 3, 4, 1, 0);
        for (int step = 0; step < 4; step++) {
            checkSlab(problems, helper, origin, 1 + step, 3, 3, GTBlocks.getStoneSlab(PRIMARY, StoneVariant.TILES),
                    Direction.DOWN, "the kerb slab at step " + step);
        }
        // The port used to place the full block here; nothing may be a full block any more.
        check(problems, !at(helper, origin, 1, 1, 0).is(GTBlocks.getStone(PRIMARY, StoneVariant.SMOOTH)),
                "the slab position holds the full smooth block instead");

        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    /** A slab of the expected rock and variant, filling the half GT6's side constant names. */
    private static void checkSlab(List<String> problems, GameTestHelper helper, BlockPos origin, int x, int y, int z,
                                  Block slab, Direction side, String what) {
        BlockState state = at(helper, origin, x, y, z);
        if (slab == null) {
            problems.add(what + ": the port has no slab for that rock and variant");
            return;
        }
        if (!state.is(slab)) {
            problems.add(what + " at (" + x + "," + y + "," + z + ") is " + state + " instead of " + slab);
            return;
        }
        if (state.getValue(GTStoneSlabBlock.FACING) != side) {
            problems.add(what + " at (" + x + "," + y + "," + z + ") faces "
                    + state.getValue(GTStoneSlabBlock.FACING) + " instead of " + side);
            return;
        }
        if (!fillsGt6Half(state.getShape(helper.getLevel(), origin.offset(x, y, z), CollisionContext.empty()), side)) {
            problems.add(what + " at (" + x + "," + y + "," + z + ") does not fill GT6's half of " + side);
        }
    }

    /**
     * GT6's slab bounds ({@code BlockMetaType:121-128}): the half towards {@code SIDE_X_NEG} and
     * {@code SIDE_Y_NEG} and {@code SIDE_Z_NEG} is the low one, the half towards the positive sides the
     * high one, and the other axis spans the whole block.
     */
    private static boolean fillsGt6Half(VoxelShape shape, Direction side) {
        return near(shape.min(Direction.Axis.X), side == Direction.EAST ? 0.5D : 0.0D)
                && near(shape.max(Direction.Axis.X), side == Direction.WEST ? 0.5D : 1.0D)
                && near(shape.min(Direction.Axis.Y), side == Direction.UP ? 0.5D : 0.0D)
                && near(shape.max(Direction.Axis.Y), side == Direction.DOWN ? 0.5D : 1.0D)
                && near(shape.min(Direction.Axis.Z), side == Direction.SOUTH ? 0.5D : 0.0D)
                && near(shape.max(Direction.Axis.Z), side == Direction.NORTH ? 0.5D : 1.0D);
    }

    private static boolean near(double value, double expected) {
        return Math.abs(value - expected) < 1.0E-4D;
    }

    /**
     * The four rooms that used to place full blocks where GT6 places slabs, asserted on the real block
     * states of a built cell. GT6's slab orientations are quoted line by line, because they are what makes
     * the difference visible: a kerb is its bottom half, a rim its half towards the plot, and the
     * entrance's flight alternates the two.
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void dungeonRoomsBuildGt6Slabs(GameTestHelper helper) {
        reset(helper);
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        BlockPos farm = site(SITE_FARM);
        BlockPos barn = site(SITE_BARRACKS);
        BlockPos stairs = site(SITE_ENTRANCE);
        BlockPos nether = site(SITE_NETHER);
        Block smooth = GTBlocks.getStoneSlab(PRIMARY, StoneVariant.SMOOTH);
        Block tiles = GTBlocks.getStoneSlab(PRIMARY, StoneVariant.TILES);
        Block bricks = GTBlocks.getStoneSlab(PRIMARY, StoneVariant.BRICKS);

        // GT6's crop farm :42-55: the plot rims, one GT6 slab orientation per wall.
        GTDungeonData farmData = data(level, farm, roomCell());
        check(problems, new GTDungeonChunkRoomFarmCrop().generate(farmData), "the crop farm declined its cell");
        checkSlab(problems, helper, farm, 1, 1, 5, smooth, Direction.NORTH, "the plot rim of the z = 5 wall");
        checkSlab(problems, helper, farm, 1, 1, 10, smooth, Direction.SOUTH, "the plot rim of the z = 10 wall");
        checkSlab(problems, helper, farm, 5, 1, 1, smooth, Direction.WEST, "the plot rim of the x = 5 wall");
        checkSlab(problems, helper, farm, 10, 1, 1, smooth, Direction.EAST, "the plot rim of the x = 10 wall");
        checkSlab(problems, helper, farm, 1, 6, 5, smooth, Direction.NORTH, "the upper rim at local 6");
        // GT6 :153-215: the four gardens' rims, on the sides without a neighbour.
        checkSlab(problems, helper, farm, 14, 3, 5, smooth, Direction.SOUTH, "the rim of the +X garden");
        checkSlab(problems, helper, farm, 14, 3, 10, smooth, Direction.NORTH, "the rim of the +X garden");
        checkSlab(problems, helper, farm, 5, 3, 14, smooth, Direction.EAST, "the rim of the +Z garden");
        checkSlab(problems, helper, farm, 5, 3, 1, smooth, Direction.EAST, "the rim of the -Z garden");
        // The farm's rims are the only slabs there: the plots themselves stay GT6's farmland.
        check(problems, at(helper, farm, 1, 1, 1).is(Blocks.FARMLAND),
                "the farm lost the farmland of its plot");

        // GT6's barracks :48-51: the inner room's walls, brick slabs, half towards the inner room.
        GTDungeonData barnData = data(level, barn, roomCell());
        check(problems, new GTDungeonChunkBarracks().generate(barnData), "the barracks declined its cell");
        checkSlab(problems, helper, barn, 1, 1, 5, bricks, Direction.NORTH, "the barracks wall at z = 5");
        checkSlab(problems, helper, barn, 1, 1, 10, bricks, Direction.SOUTH, "the barracks wall at z = 10");
        checkSlab(problems, helper, barn, 5, 1, 1, bricks, Direction.WEST, "the barracks wall at x = 5");
        checkSlab(problems, helper, barn, 10, 1, 1, bricks, Direction.EAST, "the barracks wall at x = 10");
        checkSlab(problems, helper, barn, 1, 6, 5, bricks, Direction.NORTH, "the barracks wall's top row");
        // GT6 :54-65: the smooth corner posts of those walls stay full blocks.
        check(problems, at(helper, barn, 4, 1, 5).is(GTBlocks.getStoneState(PRIMARY, StoneVariant.SMOOTH).getBlock()),
                "the barracks lost the full smooth corner of its walls");

        // GT6's entrance :118-193: the spiral staircase, mSlabs[0] (bottom) and mSlabs[1] (top) alternating.
        // The pillar is stopped by a plug under the cell, so the flight is the first turn GT6 always builds.
        for (int x = 6; x <= 9; x++) {
            for (int z = 6; z <= 9; z++) level.setBlock(stairs.offset(x, -3, z), Blocks.STONE.defaultBlockState(), 2);
        }
        GTDungeonData stairData = data(level, stairs, roomCell());
        check(problems, new GTDungeonChunkEntrance().generate(stairData), "the entrance declined its cell");
        checkSlab(problems, helper, stairs, 10, 1, 6, smooth, Direction.DOWN, "the stair tread mSlabs[0]");
        checkSlab(problems, helper, stairs, 10, 1, 7, smooth, Direction.UP, "the stair tread mSlabs[1]");
        checkSlab(problems, helper, stairs, 4, 1, 9, smooth, Direction.DOWN, "the stair tread mSlabs[0]");
        checkSlab(problems, helper, stairs, 4, 1, 8, smooth, Direction.UP, "the stair tread mSlabs[1]");
        // GT6 :81/99/133: the shaft's core lining is a full smooth block, not a slab.
        check(problems, at(helper, stairs, 6, 1, 7).is(GTBlocks.getStoneState(PRIMARY, StoneVariant.SMOOTH).getBlock()),
                "the entrance lost the full smooth block of its shaft core");

        // GT6's portal room :43-158: the tile kerbs, all of them mSlabs[0], GT6's bottom half.
        GTDungeonData netherData = data(level, nether, deadEndCell());
        check(problems, new GTDungeonChunkRoomPortalNether().generate(netherData),
                "the Nether portal room declined its cell");
        checkSlab(problems, helper, nether, 1, 1, 4, tiles, Direction.DOWN, "the kerb of the +X block");
        checkSlab(problems, helper, nether, 7, 1, 5, tiles, Direction.DOWN, "the kerb's column");
        checkSlab(problems, helper, nether, 6, 1, 11, tiles, Direction.DOWN, "the kerb's far row");
        // GT6's Nether portal room :77-84: the soul sand rows - mSlabs[SIDE_Y_POS] above the wart, and the
        // wall slabs mSlabs[SIDE_Z_NEG] / mSlabs[SIDE_Z_POS].
        checkSlab(problems, helper, nether, 5, 3, 1, smooth, Direction.UP, "the slab above a soul sand row");
        checkSlab(problems, helper, nether, 5, 3, 14, smooth, Direction.UP, "the slab above a soul sand row");
        checkSlab(problems, helper, nether, 5, 1, 2, smooth, Direction.NORTH, "the row's inner wall slab");
        checkSlab(problems, helper, nether, 5, 1, 13, smooth, Direction.SOUTH, "the row's inner wall slab");
        check(problems, at(helper, nether, 5, 1, 1).is(Blocks.SOUL_SAND), "the soul sand row itself");

        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }
}
