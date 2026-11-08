package gregtech.gametest;

import gregtech.content.worldgen.GTDungeonCellPiece;
import gregtech.content.worldgen.GTDungeonGenerator;
import gregtech.content.worldgen.GTStructures;
import gregtech.content.worldgen.dungeon.DungeonLayoutRules;
import gregtech.content.worldgen.dungeon.GTDungeonChunkRoomFarmMobs;
import gregtech.content.worldgen.dungeon.GTDungeonLayout;
import gregtech.content.worldgen.dungeon.GTDungeonSharedState;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * The dungeon is declared by a structure now, so the structure start must cover exactly the cells the
 * cell generator writes: one full-column piece per written cell, and nothing for the empty border cells
 * (unless the mob farm's outer platform uses them).
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class DungeonStructureTests {
    private static final String BATCH = "dungeon_structure";

    private DungeonStructureTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 40)
    public static void cellPiecesCoverTheWholeLayout(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(GTStructures.DUNGEON);
        if (structure == null) {
            helper.fail("gt6ce:gt_dungeon is not registered");
            return;
        }
        ChunkPos anchor = findDungeonAnchor(level.getSeed());
        if (anchor == null) {
            helper.fail("no dungeon anchor with a passing roll in the searched lattice");
            return;
        }
        Structure.GenerationContext context = new Structure.GenerationContext(
                level.registryAccess(),
                level.getChunkSource().getGenerator(),
                level.getChunkSource().getGenerator().getBiomeSource(),
                level.getChunkSource().randomState(),
                level.getStructureManager(),
                level.getSeed(),
                anchor,
                level,
                biome -> true);
        Optional<Structure.GenerationStub> stub = structure.findValidGenerationPoint(context);
        if (stub.isEmpty()) {
            helper.fail("an anchor that passed GT6's roll returned no generation point");
            return;
        }
        PiecesContainer pieces = stub.get().getPiecesBuilder().build();
        helper.assertTrue(!pieces.isEmpty(), "the dungeon start has pieces");

        long seed = GTDungeonLayout.seedFor(level.getSeed(), anchor);
        GTDungeonLayout.Plan plan = GTDungeonLayout.plan(seed);
        byte[][] cells = plan.cells();
        GTDungeonSharedState shared = GTDungeonSharedState.create(cells, seed);
        GTDungeonChunkRoomFarmMobs.Footprint farm = GTDungeonChunkRoomFarmMobs.footprint(cells, shared);
        Set<Long> written = new HashSet<>();
        for (int i = 0; i < cells.length; i++) {
            for (int j = 0; j < cells[i].length; j++) {
                if (!GTDungeonGenerator.writesCell(cells, shared, farm, i, j)) continue;
                written.add(chunkKey(
                        DungeonLayoutRules.cellChunkX(anchor.x, cells.length, i),
                        DungeonLayoutRules.cellChunkZ(anchor.z, cells[i].length, j)));
            }
        }

        Set<Long> covered = new HashSet<>();
        for (StructurePiece piece : pieces.pieces()) {
            helper.assertTrue(piece instanceof GTDungeonCellPiece, "every piece is a dungeon cell piece");
            covered.add(chunkKey(piece.getBoundingBox().minX() >> 4, piece.getBoundingBox().minZ() >> 4));
        }
        helper.assertTrue(covered.equals(written), "the pieces cover exactly the cells the generator writes");
        helper.succeed();
    }

    /**
     * The first anchor that passes GT6's gates, searched over the lattice near the origin. Pure chunk
     * arithmetic: nothing is generated or loaded.
     */
    private static ChunkPos findDungeonAnchor(long worldSeed) {
        for (int x = 23; x <= 400; x++) {
            if (!GTDungeonLayout.isAnchor(x)) continue;
            for (int z = 23; z <= 400; z++) {
                if (!GTDungeonLayout.isAnchor(z)) continue;
                ChunkPos anchor = new ChunkPos(x, z);
                if (!GTDungeonLayout.passesAnchorClearance(anchor)) continue;
                if (GTDungeonLayout.passesProbability(GTDungeonLayout.seedFor(worldSeed, anchor))) return anchor;
            }
        }
        return null;
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }
}
