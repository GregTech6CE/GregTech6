package com.gregtech.gregtech.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.block.MaterialBlock;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.block.stone.StoneVariant;
import com.gregtech.gregtech.data.generated.GT6Materials;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import com.gregtech.gregtech.worldgen.GTFeatures;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunk;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkCorridor;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomEmpty;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomStorage;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonData;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Guards GT6's dungeon ({@code gregapi/worldgen/dungeon/WorldgenDungeonGT.java} + the {@code DungeonChunk*}
 * cell implementations): GT6's registration values, the anchor lattice, the layout algorithm, the cell
 * dispatch and the blocks a corridor cell writes.
 *
 * <p>See {@link GTDungeonLayout} for the port's cell-by-cell generation.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class DungeonTests {

    /** A far-away anchor for the cell tests (the dungeon itself is at {@code remapY(20)}). */
    private static final ChunkPos ANCHOR = new ChunkPos(2062, 2062);

    private static JsonObject resourceJson(String path) throws Exception {
        try (InputStream stream = DungeonTests.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("missing resource " + path);
            return GsonHelper.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }

    /** GT6's registration values and the datapack wiring. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void dungeonConstantsMatchGt6(GameTestHelper helper) throws Exception {
        helper.assertTrue(GTDungeonLayout.PROBABILITY == 100, "GT6 registers the dungeon with probability 100");
        helper.assertTrue(GTDungeonLayout.MIN_SIZE == 3 && GTDungeonLayout.MAX_SIZE == 7,
                "GT6: min size 3, max size 7");
        helper.assertTrue(GTDungeonLayout.MIN_Y == 20 && GTDungeonLayout.MAX_Y == 20,
                "GT6: the dungeon sits at y = 20 above bedrock");
        helper.assertTrue(GTDungeonLayout.ROOM_CHANCE == 6, "GT6: room chance 6");
        helper.assertTrue(GTDungeonLayout.ROOM_ID_COUNT == 1 && GTDungeonLayout.IMPORTANT_ROOM_COUNT == 2,
                "GT6: one room id, two important rooms");
        helper.assertTrue(GTDungeonLayout.GRID_PERIOD == 11 && GTDungeonLayout.ANCHOR_OFFSET == 5,
                "GT6's anchor lattice is abs(chunk) % 11 == 5");
        helper.assertTrue(GTDungeonLayout.SPAWN_CLEARANCE == 368, "GT6 keeps 256 + 7*16 blocks around spawn");
        helper.assertTrue(GTFeatures.DUNGEON.getId().getPath().equals("gt_dungeon"), "feature id");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .containsKey(GTDungeonFeature.CONFIGURED),
                "data/gregtech/worldgen/configured_feature/gt_dungeon.json is loaded");
        JsonObject placed = resourceJson("data/gregtech/worldgen/placed_feature/gt_dungeon.json");
        helper.assertTrue(placed.get("feature").getAsString().equals("gregtech:gt_dungeon"),
                "the placed feature points at the dungeon");
        helper.assertTrue(placed.getAsJsonArray("placement").isEmpty(),
                "an empty placement list is GT6's one attempt per chunk");
        JsonObject modifier = resourceJson("data/gregtech/forge/biome_modifier/gt_dungeon.json");
        helper.assertTrue(modifier.get("biomes").getAsString().equals("#minecraft:is_overworld"),
                "the dungeon is an overworld structure");
        JsonArray features = modifier.getAsJsonArray("features");
        helper.assertTrue(features.size() == 1 && features.get(0).getAsString().equals("gregtech:gt_dungeon"),
                "the biome modifier adds the dungeon feature");
        helper.assertTrue(modifier.get("step").getAsString().equals("underground_ores"),
                "the dungeon runs before the surface decoration");
        // GT6's lists, minus the rooms that need other mods (the Twilight/Aether/Myst portal rooms).
        helper.assertTrue(GTDungeonFeature.ROOMS.size() == 6, "six ported room types, got "
                + GTDungeonFeature.ROOMS.size());
        helper.assertTrue(GTDungeonFeature.DEAD_END.size() == 3, "three ported dead ends, got "
                + GTDungeonFeature.DEAD_END.size());
        helper.assertTrue(GTDungeonFeature.KEY_COUNT == 5, "GT6 hands out five dungeon keys");
        helper.succeed();
    }

    /** GT6's anchor lattice: one dungeon anchor per 11 chunks, and cells map back to it. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void dungeonAnchorLatticeMatchesGt6(GameTestHelper helper) {
        // abs(chunk) % 11 == 5 -> 5, 16, 27 ... and -5, -16, ...
        for (int anchor : new int[]{5, 16, 27, 38, -5, -16, -27}) {
            ChunkPos found = GTDungeonLayout.anchorFor(anchor, anchor);
            helper.assertTrue(found != null && found.x == anchor && found.z == anchor,
                    "chunk " + anchor + " is an anchor, got " + found);
        }
        // Cells up to half a grid away belong to that anchor; anything further is outside the layout.
        for (int offset = -4; offset <= 4; offset++) {
            ChunkPos found = GTDungeonLayout.anchorFor(27 + offset, 27 + offset);
            helper.assertTrue(found != null && found.x == 27 && found.z == 27,
                    "chunk " + (27 + offset) + " belongs to the anchor at 27, got " + found);
        }
        for (int offset : new int[]{-5, 5, 6}) {
            helper.assertTrue(GTDungeonLayout.anchorFor(27 + offset, 27) == null,
                    "chunk " + (27 + offset) + " is outside every layout");
        }
        helper.assertTrue(GTDungeonLayout.anchorFor(0, 0) == null, "the spawn chunk is not a dungeon");
        // Every cell must make the same dungeon-wide gate decision even if one cell has lost its
        // bottom bedrock. GT6 checked only the anchor once before building the entire dungeon;
        // the port cannot safely read an anchor four chunks away during FEATURES generation.
        ServerLevel level = helper.getLevel();
        ChunkPos far = new ChunkPos(4000, 4000);
        BlockPos anchorFloor = new BlockPos(far.x * 16 + 8, level.getMinBuildHeight(), far.z * 16 + 8);
        BlockPos otherFloor = new BlockPos((far.x + 1) * 16 + 8, level.getMinBuildHeight(), far.z * 16 + 8);
        BlockState originalAnchor = level.getBlockState(anchorFloor);
        BlockState originalOther = level.getBlockState(otherFloor);
        try {
            level.setBlock(anchorFloor, Blocks.BEDROCK.defaultBlockState(), 3);
            level.setBlock(otherFloor, Blocks.STONE.defaultBlockState(), 3);
            helper.assertTrue(GTDungeonLayout.passesPosition(level, far, far.x, far.z),
                    "the anchor cell passes the common gate");
            helper.assertTrue(GTDungeonLayout.passesPosition(level, far, far.x + 1, far.z),
                    "a neighbouring cell missing bedrock still belongs to the same dungeon");
        } finally {
            level.setBlock(anchorFloor, originalAnchor, 3);
            level.setBlock(otherFloor, originalOther, 3);
        }
        // GT6 keeps 256 + 7*16 blocks around the spawn clear.
        ChunkPos near = new ChunkPos(5, 5);
        helper.assertFalse(GTDungeonLayout.passesPosition(level, near, near.x, near.z),
                "the dungeon stays away from the spawn");
        long seed = GTDungeonLayout.seedFor(level, ANCHOR);
        helper.assertTrue(seed == GTDungeonLayout.seedFor(level, ANCHOR), "the dungeon seed is stable");
        helper.assertTrue(seed != GTDungeonLayout.seedFor(level, new ChunkPos(ANCHOR.x + 11, ANCHOR.z)),
                "a different anchor gives a different dungeon");
        helper.succeed();
    }

    /** GT6's layout: two important cells, at least two rooms, and only connected corridors survive. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void dungeonLayoutMatchesGt6(GameTestHelper helper) {
        int sizes = 0, layouts = 0;
        for (long seed = 1; seed <= 40; seed++) {
            byte[][] cells = GTDungeonLayout.layout(RandomSource.create(seed));
            layouts++;
            boolean squareless = cells.length < 5 || cells.length > 9;
            if (!squareless) sizes++;
            helper.assertTrue(!squareless, "GT6 lays out 5..9 cells per side, got " + cells.length);
            for (byte[] row : cells) {
                helper.assertTrue(row.length >= 5 && row.length <= 9, "each side is 5..9 cells");
            }
            int barracks = 0, entrance = 0, rooms = 0;
            for (int i = 1; i < cells.length - 1; i++) {
                for (int j = 1; j < cells[i].length - 1; j++) {
                    byte value = cells[i][j];
                    if (value == GTDungeonLayout.BARRACKS) barracks++;
                    if (value == GTDungeonLayout.ENTRANCE) entrance++;
                    if (value > 0) rooms++;
                }
            }
            helper.assertTrue(barracks == 1, "one barracks per dungeon, got " + barracks);
            helper.assertTrue(entrance == 1, "one entrance per dungeon, got " + entrance);
            helper.assertTrue(rooms >= 2, "GT6 keeps scattering rooms until there are two, got " + rooms);
            // After GT6's cleanup no corridor is left with fewer than two connections.
            for (int i = 1; i < cells.length - 1; i++) {
                for (int j = 1; j < cells[i].length - 1; j++) {
                    if (cells[i][j] != GTDungeonLayout.CORRIDOR) continue;
                    int connections = 0;
                    if (cells[i + 1][j] != 0) connections++;
                    if (cells[i - 1][j] != 0) connections++;
                    if (cells[i][j + 1] != 0) connections++;
                    if (cells[i][j - 1] != 0) connections++;
                    helper.assertTrue(connections >= 2,
                            "a surviving corridor has at least two connections, got " + connections);
                }
            }
            // The border ring stays empty (GT6 only fills 1..length-2).
            for (int i = 0; i < cells.length; i++) {
                helper.assertTrue(cells[i][0] == 0 && cells[i][cells[i].length - 1] == 0,
                        "the layout border is empty");
            }
            for (int j = 0; j < cells[0].length; j++) {
                helper.assertTrue(cells[0][j] == 0, "the layout border is empty");
            }
            for (int j = 0; j < cells[cells.length - 1].length; j++) {
                helper.assertTrue(cells[cells.length - 1][j] == 0, "the layout border is empty");
            }
        }
        helper.assertTrue(sizes == layouts, "every layout is 5..9 cells per side");
        // The same seed always produces the same dungeon.
        byte[][] first = GTDungeonLayout.layout(RandomSource.create(4242L));
        byte[][] second = GTDungeonLayout.layout(RandomSource.create(4242L));
        helper.assertTrue(java.util.Arrays.deepEquals(first, second), "the layout is deterministic");
        helper.succeed();
    }

    /** GT6's one-in-a-hundred roll and the per-cell seeds. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void dungeonGateAndSeedsMatchGt6(GameTestHelper helper) {
        int hits = 0;
        for (long seed = 0; seed < 20000; seed++) {
            if (GTDungeonLayout.passesProbability(seed)) hits++;
        }
        helper.assertTrue(hits > 130 && hits < 270, "about 1 in 100 seeds passes, got " + hits);
        long dungeon = 987654321L;
        Set<Long> seeds = new HashSet<>();
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                long cellSeed = GTDungeonFeature.cellSeed(dungeon, i, j);
                helper.assertTrue(cellSeed == GTDungeonFeature.cellSeed(dungeon, i, j),
                        "a cell seed is stable");
                seeds.add(cellSeed);
            }
        }
        helper.assertTrue(seeds.size() == 36, "each cell gets its own seed, got " + seeds.size());
        Set<Long> keySeeds = new HashSet<>();
        for (int key = 0; key < GTDungeonFeature.KEY_COUNT; key++) {
            keySeeds.add(GTDungeonFeature.keySeed(dungeon, key));
        }
        helper.assertTrue(keySeeds.size() == GTDungeonFeature.KEY_COUNT, "the five keys differ");
        helper.succeed();
    }

    /** The dispatcher: a corridor cell gets GT6's shell, and an empty cell declines. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void dungeonCellDispatchMatchesGt6(GameTestHelper helper) {
        ServerLevel server = helper.getLevel();
        WorldGenLevel level = server;

        // A 5x5 layout with one corridor that connects east, and an empty cell at (1, 1).
        byte[][] cells = new byte[5][5];
        cells[2][2] = GTDungeonLayout.CORRIDOR;
        cells[3][2] = GTDungeonLayout.CORRIDOR;
        helper.assertTrue(GTDungeonLayout.connectionCount(cells, 2, 2) == 1,
                "the corridor has one connection");
        int[] cell = GTDungeonLayout.cellOf(ANCHOR, cells, ANCHOR.x, ANCHOR.z);
        helper.assertTrue(cell != null && cell[0] == 2 && cell[1] == 2,
                "the anchor chunk is the middle cell, got " + (cell == null ? "null" : cell[0] + "/" + cell[1]));
        helper.assertTrue(GTDungeonLayout.cellOf(ANCHOR, cells, ANCHOR.x + 3, ANCHOR.z) == null,
                "a chunk outside the layout has no cell");

        StoneType[] rocks = {StoneType.GRANITE_BLACK, StoneType.MARBLE};
        int offsetY = GTDungeonLayout.MIN_Y;
        // Loading the target column first: getHeight answers with the world floor otherwise.
        int minX = (ANCHOR.x - cells.length / 2 + 2) * 16;
        int minZ = (ANCHOR.z - cells[0].length / 2 + 2) * 16;
        int y = GTDungeonLayout.y(level, offsetY);
        server.getBlockState(new BlockPos(minX + 8, y, minZ + 8));
        // The natural terrain here can be a flooded cave. GT6 then builds a glass ceiling,
        // so give the stone-ceiling assertions below a deliberately dry, covered column.
        for (int x = 6; x <= 9; x++) {
            for (int z = 6; z <= 9; z++) {
                server.setBlock(new BlockPos(minX + x, y + 5, minZ + z), Blocks.STONE.defaultBlockState(), 2);
            }
        }
        helper.assertTrue(GTDungeonFeature.generateCell(level, ANCHOR, cells, 2, 2, offsetY, 3, rocks, 77L),
                "the corridor cell is built");
        helper.assertTrue(!GTDungeonFeature.generateCell(level, ANCHOR, cells, 1, 1, offsetY, 3, rocks, 77L),
                "an empty cell declines");

        // GT6's corridor shell: tiled floor, brick walls, air inside, a brick ceiling away from the lamp.
        helper.assertTrue(server.getBlockState(new BlockPos(minX + 5, y, minZ + 5))
                        .is(GTBlocks.getStoneState(rocks[0], StoneVariant.TILES).getBlock()),
                "the floor is the primary rock's tiles");
        helper.assertTrue(server.getBlockState(new BlockPos(minX + 5, y + 1, minZ + 5))
                        .is(GTBlocks.getStoneState(rocks[0], StoneVariant.BRICKS).getBlock()),
                "the lower wall uses the primary rock's bricks");
        helper.assertTrue(server.getBlockState(new BlockPos(minX + 5, y + 2, minZ + 5))
                        .is(GTBlocks.getStoneState(rocks[1], StoneVariant.BRICKS).getBlock()),
                "the middle layer uses the secondary rock's bricks");
        helper.assertTrue(server.getBlockState(new BlockPos(minX + 7, y + 1, minZ + 7)).isAir(),
                "the corridor interior is air");
        helper.assertTrue(server.getBlockState(new BlockPos(minX + 6, y + 4, minZ + 6))
                        .is(GTBlocks.getStoneState(rocks[0], StoneVariant.BRICKS).getBlock()),
                "the ceiling is brick where the lamp is not");
        // The centre lamp: two redstone bricks and two lit lamps.
        helper.assertTrue(server.getBlockState(new BlockPos(minX + 7, y + 4, minZ + 7))
                        .is(GTBlocks.getStoneState(rocks[0], StoneVariant.BRICKS_REDSTONE).getBlock()),
                "the lamp's corner is a redstone brick");
        helper.assertTrue(server.getBlockState(new BlockPos(minX + 7, y + 4, minZ + 8)).is(Blocks.REDSTONE_LAMP)
                        && server.getBlockState(new BlockPos(minX + 7, y + 4, minZ + 8))
                        .getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT),
                "the lamp itself is lit");
        // The east arm opens the wall at x = 10 and lights itself.
        helper.assertTrue(server.getBlockState(new BlockPos(minX + 10, y + 1, minZ + 7)).isAir(),
                "the corridor opens towards its neighbour");
        helper.assertTrue(server.getBlockState(new BlockPos(minX + 12, y + 1, minZ + 5))
                        .is(GTBlocks.getStoneState(rocks[0], StoneVariant.BRICKS).getBlock()),
                "the arm keeps its side wall");
        helper.assertTrue(server.getBlockState(new BlockPos(minX + 13, y + 4, minZ + 7)).is(Blocks.REDSTONE_LAMP)
                        && server.getBlockState(new BlockPos(minX + 13, y + 4, minZ + 7))
                        .getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT),
                "the arm has its own lamp");

        // GT6's other branch: liquid directly above the ceiling makes glowing glass, including
        // the four-block centre light. Rebuild the same cell immediately with two wet columns.
        server.setBlock(new BlockPos(minX + 6, y + 5, minZ + 6), Blocks.WATER.defaultBlockState(), 2);
        server.setBlock(new BlockPos(minX + 7, y + 5, minZ + 7), Blocks.WATER.defaultBlockState(), 2);
        helper.assertTrue(GTDungeonFeature.generateCell(level, ANCHOR, cells, 2, 2, offsetY, 3, rocks, 77L),
                "the corridor can rebuild beneath liquid");
        helper.assertTrue(server.getBlockState(new BlockPos(minX + 6, y + 4, minZ + 6))
                        .is(GTDecorBlocks.GLASS_GLOW.get()),
                "liquid above an ordinary ceiling column selects glowing glass");
        for (int x = 7; x <= 8; x++) {
            for (int z = 7; z <= 8; z++) {
                helper.assertTrue(server.getBlockState(new BlockPos(minX + x, y + 4, minZ + z))
                                .is(GTDecorBlocks.GLASS_GLOW.get()),
                        "liquid above the centre selects four glowing glass blocks");
            }
        }
        helper.succeed();
    }

    /** Every ported cell type accepts a cell, and the fallback room always does. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void dungeonCellTypesAcceptTheirCells(GameTestHelper helper) {
        ServerLevel server = helper.getLevel();
        WorldGenLevel level = server;
        StoneType[] rocks = {StoneType.LIMESTONE, StoneType.SLATE};
        List<GTDungeonChunk> all = new ArrayList<>();
        all.add(GTDungeonFeature.CORRIDOR);
        all.add(GTDungeonFeature.CORRIDOR3);
        all.add(GTDungeonFeature.CORRIDOR4);
        all.add(GTDungeonFeature.ENTRANCE);
        all.add(GTDungeonFeature.BARRACKS);
        all.add(GTDungeonFeature.ROOM_EMPTY);
        all.addAll(GTDungeonFeature.ROOMS);
        all.addAll(GTDungeonFeature.DEAD_END);
        helper.assertTrue(all.size() == 15, "fifteen ported cell types, got " + all.size());
        int index = 4;
        for (GTDungeonChunk chunk : all) {
            helper.assertTrue(chunk.id() != null && !chunk.id().isEmpty(), "every cell type has an id");
            // A 5x5 layout whose middle cell connects in all four directions, so the 3/4-way corridors
            // and the rooms all see a well connected cell.
            byte[][] cells = new byte[5][5];
            cells[2][2] = GTDungeonLayout.CORRIDOR;
            cells[1][2] = GTDungeonLayout.CORRIDOR;
            cells[3][2] = GTDungeonLayout.CORRIDOR;
            cells[2][1] = GTDungeonLayout.CORRIDOR;
            cells[2][3] = GTDungeonLayout.CORRIDOR;
            int minX = (ANCHOR.x - 2 + 2) * 16 + index * 48;
            int minZ = (ANCHOR.z - 2 + 2) * 16;
            int y = GTDungeonLayout.y(level, GTDungeonLayout.MIN_Y);
            server.getBlockState(new BlockPos(minX + 8, y, minZ + 8));
            GTDungeonData data = new GTDungeonData(level, minX, y, minZ, rocks[0], rocks[1], 3, cells, 2, 2, 4,
                    new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                    new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                    RandomSource.create(index));
            boolean accepted = GTDungeonFeature.tryGenerate(chunk, data);
            // A cell may decline (a farm without water, a mining room without bedrock); when it accepts,
            // it must have laid its floor and it must not have thrown.
            if (accepted) {
                boolean built = !server.getBlockState(new BlockPos(minX + 7, y, minZ + 7)).isAir()
                        || !server.getBlockState(new BlockPos(minX + 8, y, minZ + 8)).isAir();
                helper.assertTrue(built, chunk.id() + " accepted its cell but left no floor behind");
            }
            index++;
        }
        helper.assertTrue(new GTDungeonChunkRoomEmpty().generate(new GTDungeonData(level,
                        (ANCHOR.x) * 16 + 600, GTDungeonLayout.y(level, GTDungeonLayout.MIN_Y),
                        (ANCHOR.z) * 16, rocks[0], rocks[1], 0, new byte[5][5], 2, 2, 0,
                        new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                        new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                        RandomSource.create(9L))),
                "the fallback room always accepts its cell");
        helper.assertTrue(GTDungeonFeature.CORRIDOR instanceof GTDungeonChunkCorridor
                        && GTDungeonFeature.ROOM_EMPTY instanceof GTDungeonChunkRoomEmpty,
                "the shell cell types are the ported ones");
        helper.succeed();
    }

    /**
     * The workshop's smithy row is the port's own machinery now ({@link GTDungeonChunkRoomWorkshop}).
     *
     * <p>GT6 rolls the crucible tier once ({@code DungeonChunkRoomWorkshop:130}) and derives the burning
     * box ({@code 1102+tCrucibleType}), the smelting crucible ({@code 1020+tCrucibleType}) and both
     * moulds ({@code 1070+tCrucibleType}) from it, so all four placed blocks have to belong to the same
     * material triple; the grindstone ({@code 32703}), the material anvil ({@code 32034+rnd(4)},
     * BlackSteel/BlueSteel/RedSteel/VanadiumSteel) and the drawer/mass storages ({@code 4011}/{@code 6011})
     * are the port's own blocks at GT6's coordinates.</p>
     */
    @GameTest(template = "test_blueprint_empty", timeoutTicks = 400)
    public static void dungeonWorkshopUsesThePortsOwnMachines(GameTestHelper helper) {
        ServerLevel server = helper.getLevel();
        int x = ANCHOR.x * 16 + 900;
        int z = ANCHOR.z * 16;
        int y = GTDungeonLayout.y(server, GTDungeonLayout.MIN_Y);
        StoneType[] rocks = {StoneType.LIMESTONE, StoneType.SLATE};
        byte[][] cells = new byte[5][5];
        cells[2][2] = GTDungeonLayout.CORRIDOR;
        GTDungeonData data = new GTDungeonData(server, x, y, z, rocks[0], rocks[1], 7, cells, 2, 2, 1,
                new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                RandomSource.create(4242L));
        helper.assertTrue(new com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomWorkshop()
                .generate(data), "the workshop accepts its cell");

        var workshopShelf = server.getBlockEntity(new BlockPos(x + 4, y + 3, z + 1));
        helper.assertTrue(workshopShelf instanceof com.gregtech.gregtech.blockentity.inventory.BookShelfBlockEntity,
                "GT6 workshop's fixed manual shelf exists");
        var shelfItems = ((com.gregtech.gregtech.blockentity.inventory.BookShelfBlockEntity) workshopShelf)
                .inventory();
        String[] expectedManuals = {"Manual_Elements", "Manual_Alloys", "Manual_Smeltery",
                "Manual_Random", "Manual_Extenders", "Manual_Steam", "Manual_Tools", "Manual_Printer"};
        for (int i = 0; i < expectedManuals.length; i++) {
            ItemStack actual = shelfItems.getStackInSlot(i);
            helper.assertTrue(ItemStack.isSameItemSameTags(actual,
                            com.gregtech.gregtech.content.book.GTBooks.bookStack(expectedManuals[i])),
                    "workshop shelf has GT6 manual " + expectedManuals[i] + " in original slot");
        }
        var ductTape = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", "duct_tape"));
        helper.assertTrue(shelfItems.getStackInSlot(8).is(ductTape)
                        && shelfItems.getStackInSlot(9).is(ductTape),
                "workshop shelf retains both original duct tapes");

        BlockState burningBox = server.getBlockState(new BlockPos(x + 14, y + 1, z + 3));
        BlockState crucible = server.getBlockState(new BlockPos(x + 14, y + 2, z + 3));
        BlockState moldWest = server.getBlockState(new BlockPos(x + 14, y + 2, z + 2));
        BlockState moldEast = server.getBlockState(new BlockPos(x + 14, y + 2, z + 4));

        // GT6 ids 1102/1103/1104 burning boxes, 1020/1021/1022 crucibles, 1070/1071/1072 moulds: the
        // port's blocks are selected by the same roll, so the tier has to agree across all four.
        int burningBoxTier = tier(burningBox, com.gregtech.gregtech.registry.GTMachines.BURNING_BOX_SOLID_BRONZE,
                com.gregtech.gregtech.registry.GTMachines.BURNING_BOX_SOLID_INVAR,
                com.gregtech.gregtech.registry.GTMachines.BURNING_BOX_SOLID_STEEL);
        int crucibleTier = tier(crucible, com.gregtech.gregtech.registry.GTMachines.SMELTING_CRUCIBLE_BRONZE,
                com.gregtech.gregtech.registry.GTMachines.SMELTING_CRUCIBLE_INVAR,
                com.gregtech.gregtech.registry.GTMachines.SMELTING_CRUCIBLE_STEEL);
        helper.assertTrue(burningBoxTier >= 0, "the burning box is the port's own block: " + burningBox);
        helper.assertTrue(crucibleTier == burningBoxTier,
                "the crucible uses the burning box's tier (" + crucibleTier + " vs " + burningBoxTier + ")");
        helper.assertTrue(crucible.getBlock() instanceof com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock,
                "the crucible is a smelting crucible");
        helper.assertTrue(burningBox.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)
                        == net.minecraft.core.Direction.WEST,
                "GT6's burning box faces SIDE_X_NEG (west)");

        helper.assertTrue(moldWest.is(moldEast.getBlock()),
                "both moulds are the same tier: " + moldWest + " / " + moldEast);
        helper.assertTrue(moldWest.getBlock() instanceof com.gregtech.gregtech.block.machine.MoldBlock,
                "the smeltery stack carries a mould: " + moldWest);
        com.gregtech.gregtech.block.machine.MoldBlock mold =
                (com.gregtech.gregtech.block.machine.MoldBlock) moldWest.getBlock();
        helper.assertTrue(mold.spec().gt6MetaId() == 1070 + crucibleTier,
                "the mould carries GT6's meta id of the rolled tier: " + mold.spec().gt6MetaId());
        for (int localZ : new int[]{2, 4}) {
            var moldEntity = server.getBlockEntity(new BlockPos(x + 14, y + 2, z + localZ));
            helper.assertTrue(moldEntity instanceof com.gregtech.gregtech.blockentity.machine.MoldBlockEntity
                            && ((com.gregtech.gregtech.blockentity.machine.MoldBlockEntity) moldEntity)
                                    .getMoldShape() != 0,
                    "GT6's workshop mould starts with a castable cavity");
        }
        helper.assertTrue(((com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock) crucible.getBlock())
                        .spec().gt6MetaId() == 1020 + crucibleTier,
                "the crucible carries GT6's meta id of the rolled tier");

        // GT6's grindstone 32703 (facing SIDE_Z_POS) as the port's grindstone block.
        BlockState grindstone = server.getBlockState(new BlockPos(x + 12, y + 1, z + 1));
        helper.assertTrue(grindstone.getBlock() instanceof com.gregtech.gregtech.block.tool.ManualToolBlock tool
                        && tool.kind() == com.gregtech.gregtech.blockentity.tool.ManualToolBlockEntity.Kind.GRINDSTONE,
                "the grindstone is the port's manual grindstone: " + grindstone);
        helper.assertTrue(grindstone.getValue(com.gregtech.gregtech.block.tool.ManualToolBlock.FACING)
                        == net.minecraft.core.Direction.SOUTH,
                "GT6's grindstone faces SIDE_Z_POS (south)");
        var grinder = server.getBlockEntity(new BlockPos(x + 12, y + 1, z + 1));
        helper.assertTrue(grinder instanceof com.gregtech.gregtech.blockentity.tool.ManualToolBlockEntity
                        && ((com.gregtech.gregtech.blockentity.tool.ManualToolBlockEntity) grinder)
                                .stoneUses() >= 1
                        && ((com.gregtech.gregtech.blockentity.tool.ManualToolBlockEntity) grinder)
                                .stoneUses() <= 4
                        && grindstone.getValue(com.gregtech.gregtech.block.tool.ManualToolBlock.STONE),
                "GT6's dungeon grindstone starts with 1..4 visible abrasive uses");

        // GT6's anvil multi-tile 32034+rnd(4): one of the port's four material anvils.
        BlockState anvil = server.getBlockState(new BlockPos(x + 11, y + 1, z + 4));
        helper.assertTrue(anvil.getBlock() instanceof com.gregtech.gregtech.block.tool.MaterialAnvilBlock,
                "the second anvil is a material anvil: " + anvil);
        com.gregtech.gregtech.block.tool.MaterialAnvilBlock materialAnvil =
                (com.gregtech.gregtech.block.tool.MaterialAnvilBlock) anvil.getBlock();
        String anvilMaterial = materialAnvil.material().getName();
        helper.assertTrue(java.util.Set.of("BlackSteel", "BlueSteel", "RedSteel", "VanadiumSteel")
                        .contains(anvilMaterial),
                "GT6's anvil materials are 32034-32037: " + anvilMaterial);
        var anvilEntity = server.getBlockEntity(new BlockPos(x + 11, y + 1, z + 4));
        helper.assertTrue(anvilEntity instanceof com.gregtech.gregtech.blockentity.tool.MaterialAnvilBlockEntity,
                "the workshop's material anvil has its block entity");
        ItemStack initialHammer = ((com.gregtech.gregtech.blockentity.tool.MaterialAnvilBlockEntity) anvilEntity)
                .workpiece(0);
        helper.assertTrue(com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(initialHammer,
                        com.gregtech.gregtech.api.tool.GTToolType.HARD_HAMMER)
                        && com.gregtech.gregtech.api.tool.GTToolHelper.getHead(initialHammer)
                                == GT6Materials.Compounds.VanadiumSteel
                        && com.gregtech.gregtech.api.tool.GTToolHelper.getHandle(initialHammer)
                                == GT6Materials.Woods.Spruce,
                "GT6's workshop anvil carries its Vanadium Steel / Spruce hammer");
        // GT6's vanilla anvil of :138 stays vanilla.
        helper.assertTrue(server.getBlockState(new BlockPos(x + 11, y + 1, z + 1))
                        .getBlock() instanceof net.minecraft.world.level.block.AnvilBlock,
                "GT6's own vanilla anvil stays vanilla");
        helper.assertTrue(server.getBlockState(new BlockPos(x + 1, y + 1, z + 1))
                        .is(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE),
                "GT6's own vanilla workshop crafting table stays vanilla");

        // The drawer (4011) and the two mass storages (6011) are the port's own containers.
        BlockState drawer = server.getBlockState(new BlockPos(x + 1, y + 1, z + 3));
        helper.assertTrue(drawer.getBlock() == com.gregtech.gregtech.registry.GTStorage.DRAWER_QUAD.get(),
                "the compartment drawer is the port's quad drawer: " + drawer);
        helper.assertTrue(drawer.getValue(com.gregtech.gregtech.block.inventory.DrawerQuadBlock.FACING)
                        == net.minecraft.core.Direction.EAST,
                "GT6's drawer faces SIDE_X_POS (east)");
        for (int localY : new int[]{1, 2}) {
            BlockPos storagePos = new BlockPos(x + 4, y + localY, z + 1);
            BlockState storage = server.getBlockState(storagePos);
            helper.assertTrue(storage.getBlock() == com.gregtech.gregtech.registry.GTStorage.MASS_STORAGE.get(),
                    "GT6's mass storage is the port's own block: " + storage);
            helper.assertTrue(storage.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)
                            == net.minecraft.core.Direction.SOUTH,
                    "GT6's mass storage faces SIDE_Z_POS (south)");
            var bulk = server.getBlockEntity(storagePos);
            helper.assertTrue(bulk instanceof com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity,
                    "the rock supply has a bulk storage block entity");
            var supplied = (com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity) bulk;
            StoneType rock = localY == 1 ? rocks[0] : rocks[1];
            int minimum = localY == 1 ? 10_000 : 1_000;
            int maximum = localY == 1 ? 100_000 : 10_000;
            helper.assertTrue(supplied.template().is(GTBlocks.getStone(rock, StoneVariant.COBBLE).asItem())
                            && supplied.stored() >= minimum && supplied.stored() <= maximum,
                    "GT6's mass storage contains the corresponding dungeon cobble in its original range");
        }
        helper.succeed();
    }

    /**
     * The storage room packs GT6's crates, not a stand-in ({@link GTDungeonChunkRoomStorage}).
     *
     * <p>GT6's room stacks one crate multi-tile per cell out of twelve entries of
     * {@code DungeonChunkRoomStorage:135} and tops the raw ore quadrant with raw ore blocks
     * ({@code :179-182}). The port used to place a plain shelf there because it had no crates; now every
     * block of the four quadrants has to be a crate block of the six crate prefixes (or a raw ore block,
     * a pile, or air), and no shelf may appear in the crate volume any more.</p>
     *
     * <p>Several seeds are run because which quadrant rolls which category is random: a single seed can
     * only prove one of the five crate categories.</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void storageRoomPacksGtCrates(GameTestHelper helper) {
        ServerLevel server = helper.getLevel();
        WorldGenLevel level = server;
        StoneType[] rocks = {StoneType.LIMESTONE, StoneType.SLATE};
        int crates = 0;
        int partialCrates = 0;
        int fullCrates = 0;
        int rawBlocks = 0;
        Set<String> forms = new HashSet<>();
        for (int seed = 1; seed <= 24; seed++) {
            int minX = (ANCHOR.x) * 16 + 2000 + seed * 48;
            int minZ = (ANCHOR.z) * 16;
            int y = GTDungeonLayout.y(level, GTDungeonLayout.MIN_Y);
            byte[][] cells = new byte[5][5];
            cells[2][3] = GTDungeonLayout.CORRIDOR;             // the room's single connection (a dead end)
            GTDungeonData data = new GTDungeonData(level, minX, y, minZ, rocks[0], rocks[1], 3, cells, 2, 2, 1,
                    new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                    new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                    RandomSource.create(seed));
            helper.assertTrue(new GTDungeonChunkRoomStorage().generate(data),
                    "the storage room accepts a dead end (seed " + seed + ")");
            for (int ax = 1; ax <= 14; ax++) {
                for (int az = 1; az <= 14; az++) {
                    for (int ay = 1; ay <= 4; ay++) {
                        BlockState state = server.getBlockState(new BlockPos(minX + ax, y + ay, minZ + az));
                        if (state.isAir()) continue;
                        helper.assertFalse(state.getBlock() == GTDecorBlocks.BOOKSHELF.get(),
                                "the crate volume has no shelf stand-in any more: " + state + " at "
                                        + ax + "," + ay + "," + az);
                        if (!(state.getBlock() instanceof MaterialBlock crate)) continue;
                        if (isCratePrefix(crate.prefix())) {
                            crates++;
                            if (crate.prefix().isPartialCrate()) partialCrates++;
                            else fullCrates++;
                            forms.add(crate.prefix().getName());
                        } else if (crate.prefix() == BlockMaterialPrefix.blockRaw) {
                            rawBlocks++;
                        }
                    }
                }
            }
        }
        helper.assertTrue(crates > 0, "the storage room places GT6's crates");
        helper.assertTrue(partialCrates > 0 && fullCrates > 0,
                "GT6's 16-piece and 64-piece crate rolls both produce their own blocks");
        helper.assertTrue(rawBlocks > 0, "the raw ore quadrant tops its crates with raw ore blocks");
        Set<String> allowed = new HashSet<>();
        for (GTDungeonData.CrateForm form : GTDungeonData.CrateForm.values()) {
            allowed.add(form.prefix().getName());
        }
        helper.assertTrue(allowed.containsAll(forms),
                "every crate form is one of GT6's twelve size/form variants " + allowed + ", got " + forms);
        helper.succeed();
    }

    /** Whether a block prefix is one of GT6's twelve crate size/form variants. */
    private static boolean isCratePrefix(BlockMaterialPrefix prefix) {
        for (GTDungeonData.CrateForm form : GTDungeonData.CrateForm.values()) {
            if (form.prefix() == prefix) return true;
        }
        return false;
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void partialAndFullCratesCarryGt6MaterialAmounts(GameTestHelper helper) {
        long unit = com.gregtech.gregtech.api.material.GTValues.U;
        for (GTDungeonData.CrateForm form : GTDungeonData.CrateForm.values()) {
            BlockMaterialPrefix prefix = form.prefix();
            boolean raw = form == GTDungeonData.CrateForm.RAW || form == GTDungeonData.CrateForm.RAW64;
            long pieces = prefix.isPartialCrate() ? 16 : 64;
            helper.assertTrue(prefix.getMaterialWeight() == pieces * unit * (raw ? 2 : 1),
                    form + " must contain the GT6 ore or material unit amount");
            helper.assertTrue(prefix.getRegistryName().equals(com.gregtech.gregtech.data.MaterialPrefix.camelToSnake(prefix.getName())),
                    form + " uses its own GT6 prefix id");
        }
        var crate = GTBlocks.getObject(BlockMaterialPrefix.crateGtIngot, GT6Materials.Elements.Cu);
        helper.assertTrue(crate != null && crate.isPresent(), "copper partial ingot crate is registered");
        helper.assertTrue(com.gregtech.gregtech.data.BlockHarvestPolicy.tool(crate.get())
                        == com.gregtech.gregtech.data.BlockHarvestPolicy.Tool.CROWBAR,
                "GT6 crate harvest tool is the crowbar");
        ItemStack crowbar = com.gregtech.gregtech.item.GTToolItem.create(
                com.gregtech.gregtech.api.tool.GTToolType.CROWBAR,
                com.gregtech.gregtech.content.material.Materials.Steel,
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
        helper.assertTrue(com.gregtech.gregtech.api.tool.GTToolHelper.isSpecialHarvestTool(
                        crowbar, crate.get().defaultBlockState()), "crowbar harvests the crate");
        helper.succeed();
    }

    /** GT6 draws both candidate pile stacks before the final ingot-or-plate choice. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void dungeonPileUsesGt6CandidateDrawOrder(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int minX = ANCHOR.x * 16 + 6000;
        int minZ = ANCHOR.z * 16;
        int y = GTDungeonLayout.y(level, GTDungeonLayout.MIN_Y);
        long seed = 46521L;
        GTDungeonData data = new GTDungeonData(level, minX, y, minZ,
                StoneType.LIMESTONE, StoneType.SLATE, 0, new byte[5][5], 2, 2, 0,
                new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                RandomSource.create(seed));
        RandomSource probe = RandomSource.create(seed);
        String[] materials = {"Copper", "Tin"};
        int ingotMaterial = probe.nextInt(2);
        int ingotCount = 1 + probe.nextInt(64);
        int plateMaterial = probe.nextInt(2);
        int plateCount = 1 + probe.nextInt(64);
        boolean choosePlate = probe.nextBoolean();
        helper.assertTrue(data.ingotsOrPlates(1, 1, 1, 0, materials), "the GT6 pile was placed");
        var entity = level.getBlockEntity(new BlockPos(minX + 1, y + 1, minZ + 1));
        helper.assertTrue(entity instanceof com.gregtech.gregtech.blockentity.misc.PileBlockEntity,
                "the pile stores its drawn candidate");
        var pile = (com.gregtech.gregtech.blockentity.misc.PileBlockEntity) entity;
        helper.assertTrue(pile.kind() == (choosePlate
                        ? GTDungeonData.PileKind.PLATE : GTDungeonData.PileKind.INGOT).blockKind(),
                "kind is selected after both candidate draws");
        helper.assertTrue(pile.count() == (choosePlate ? plateCount : ingotCount),
                "the selected candidate keeps its own stack size");
        helper.assertTrue(com.gregtech.gregtech.blockentity.misc.PileBlockEntity.materialOf(pile.stored())
                        == com.gregtech.gregtech.api.material.GTMaterialRegistry.get(
                        materials[choosePlate ? plateMaterial : ingotMaterial]),
                "the selected candidate keeps its own material");
        helper.succeed();
    }

    /**
     * GT6's crate draws are ordered block-then-material, and the port keeps that order.
     *
     * <p>{@code DungeonData.java:209-210} is
     * {@code aBlocks[next(aBlocks.length)].placeBlock(..., aMaterials[next(aMaterials.length)].mID, ...)}:
     * Java evaluates the receiver (the block) before the arguments (the material), so the crate's block
     * index is the <em>first</em> number out of the stream and the material the second. Every later roll
     * of the cell depends on that count and order, so this test replays the same seed on a probe stream
     * and demands that the placed block is exactly the pair of those two draws - an implementation that
     * drew the material first would place the swapped pair.</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void crateDrawsBlockBeforeMaterial(GameTestHelper helper) {
        ServerLevel server = helper.getLevel();
        WorldGenLevel level = server;
        StoneType[] rocks = {StoneType.LIMESTONE, StoneType.SLATE};
        GTDungeonData.CrateForm[] forms = {GTDungeonData.CrateForm.INGOT, GTDungeonData.CrateForm.PLATE};
        GTMaterial[] materials = {GT6Materials.Elements.Cu, GT6Materials.Elements.Sn};
        long seed = 1234L;
        int minX = (ANCHOR.x) * 16 + 4000;
        int minZ = (ANCHOR.z) * 16;
        int y = GTDungeonLayout.y(level, GTDungeonLayout.MIN_Y);
        GTDungeonData data = new GTDungeonData(level, minX, y, minZ, rocks[0], rocks[1], 0, new byte[5][5], 2, 2, 0,
                new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                RandomSource.create(seed));
        RandomSource probe = RandomSource.create(seed);
        int expectedForm = probe.nextInt(forms.length);
        int expectedMaterial = probe.nextInt(materials.length);
        helper.assertTrue(data.crate(1, 1, 1, forms, materials), "the crate was placed");
        BlockState placed = server.getBlockState(new BlockPos(minX + 1, y + 1, minZ + 1));
        helper.assertTrue(placed.getBlock() instanceof MaterialBlock, "the crate is a material block: " + placed);
        MaterialBlock crate = (MaterialBlock) placed.getBlock();
        helper.assertTrue(crate.prefix() == forms[expectedForm].prefix(),
                "the block draw comes first: expected " + forms[expectedForm] + ", got " + crate.prefix().getName());
        helper.assertTrue(crate.material() == materials[expectedMaterial],
                "the material draw comes second: expected " + materials[expectedMaterial].getName()
                        + ", got " + crate.material().getName());
        // And the port's crate of that pair is the block GT6 would have placed.
        var expected = GTBlocks.getObject(forms[expectedForm].prefix(), materials[expectedMaterial]);
        helper.assertTrue(expected != null && expected.isPresent() && expected.get() == placed.getBlock(),
                "the placed crate is the port's block for that form and material");
        helper.succeed();
    }

    /**
     * The storage room's four fluid tank walls (GT6 {@code DungeonChunkRoomStorage:65-132}).
     *
     * <p>GT6 fills every side without a neighbour cell with one to four 16k/32k/64k containers per cell
     * (wooden barrel {@code 32714}, ironwood barrel {@code 32734}, bronze drum {@code 32102}) or a gas
     * cylinder ({@code 32055} propane / {@code 32056} oxygen and helium), the tank holding one fluid of
     * that container's own seven item table. This test walks the three wall strips (the room's one
     * connection is the +Z side here, so the +X, -X and -Z walls are built) and demands that every block
     * there is one of the five port containers, that each container's amount is its GT6 capacity, and
     * that the fluid inside belongs to that capacity's table.</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void storageRoomBuildsFluidTankWalls(GameTestHelper helper) {
        ServerLevel server = helper.getLevel();
        WorldGenLevel level = server;
        StoneType[] rocks = {StoneType.LIMESTONE, StoneType.SLATE};
        // GT6's three container families with their capacities and fluid tables (:58-63).
        Map<Integer, Integer> capacity = Map.of(0, 16000, 1, 32000, 2, 64000);
        Map<String, Integer> family = Map.of("wood_barrel_treated", 0, "wood_barrel_ironwood", 1,
                "drum_bronze", 2);
        String[][] tables = {
                {"Oil_Creosote", "Oil_Seed", "Lubricant", "Glue", "Latex", "Water", "Purple_Drink"},
                {"Oil_Creosote", "Oil_Seed", "Lubricant", "Glue", "Latex", "Holywater", "Purple_Drink"},
                {"Oil_Normal", "Oil_Normal", "Oil_Soulsand", "Oil_Light", "Oil_Medium", "Oil_Heavy",
                        "Oil_ExtraHeavy"}};
        int barrels = 0;
        int cylinders = 0;
        for (int seed = 1; seed <= 24; seed++) {
            int minX = (ANCHOR.x) * 16 + 3000 + seed * 48;
            int minZ = (ANCHOR.z) * 16;
            int y = GTDungeonLayout.y(level, GTDungeonLayout.MIN_Y);
            byte[][] cells = new byte[5][5];
            cells[2][3] = GTDungeonLayout.CORRIDOR;             // the single connection: the +Z side
            GTDungeonData data = new GTDungeonData(level, minX, y, minZ, rocks[0], rocks[1], 3, cells, 2, 2, 1,
                    new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                    new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                    RandomSource.create(seed));
            helper.assertTrue(new GTDungeonChunkRoomStorage().generate(data),
                    "the storage room accepts a dead end (seed " + seed + ")");
            // The three wall strips (x 12..14 / 1..3 with z 6..9, and x 6..9 with z 1..3), never the
            // crate quadrants in the corners.
            for (int[] strip : new int[][]{{12, 6, 3, 4}, {1, 6, 3, 4}, {6, 1, 4, 3}}) {
                for (int i = 0; i < strip[2]; i++) {
                    for (int j = 0; j < strip[3]; j++) {
                        int ax = strip[0] + i, az = strip[1] + j;
                        for (int ay = 1; ay <= 4; ay++) {
                            BlockPos pos = new BlockPos(minX + ax, y + ay, minZ + az);
                            BlockState state = server.getBlockState(pos);
                            if (state.isAir()) continue;
                            String id = String.valueOf(
                                    net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()));
                            helper.assertTrue(id.startsWith("gregtech:"), "a port block, got " + id);
                            String name = id.substring("gregtech:".length());
                            if (name.equals("fluid_barometer_gas_cylinder")
                                    || name.equals("fluid_barometer_gas_cylinder_stainless_steel")) {
                                cylinders++;
                                continue;
                            }
                            helper.assertTrue(family.containsKey(name),
                                    "the wall strip only carries GT6's containers, got " + name
                                            + " at " + ax + "," + ay + "," + az);
                            barrels++;
                            Integer index = family.get(name);
                            if (server.getBlockEntity(pos)
                                    instanceof com.gregtech.gregtech.blockentity.machine.TankBlockEntity tank) {
                                net.minecraftforge.fluids.FluidStack fluid = tank.getFluidTank().getFluid();
                                helper.assertTrue(fluid.getAmount() == capacity.get(index),
                                        name + " holds GT6's " + capacity.get(index) + " mB, got "
                                                + fluid.getAmount());
                                // Compare the fluids themselves: the port's "Water" is vanilla water
                                // (minecraft:water), so no namespace may be assumed here.
                                Set<net.minecraft.world.level.material.Fluid> allowed = new HashSet<>();
                                for (String field : tables[index]) {
                                    net.minecraftforge.fluids.FluidStack stack =
                                            com.gregtech.gregtech.registry.GTFluids.stack(field, 1);
                                    helper.assertFalse(stack.isEmpty(), "the port has the fluid " + field);
                                    allowed.add(stack.getFluid());
                                }
                                helper.assertTrue(allowed.contains(fluid.getFluid()),
                                        name + " holds one of its own seven fluids, got "
                                                + net.minecraftforge.registries.ForgeRegistries.FLUIDS
                                                .getKey(fluid.getFluid()));
                            }
                        }
                    }
                }
            }
        }
        helper.assertTrue(barrels > 0, "the walls stack GT6's barrels and drums");
        helper.assertTrue(cylinders > 0, "the walls also place GT6's gas cylinders");
        helper.succeed();
    }

    /**
     * GT6 picks a wall tank's fluid with {@code UT.Code.select(NF, tFluids[tType])}, which draws from
     * {@code RNGSUS} - GT6's <em>global</em> RNG, not the dungeon's own stream
     * ({@code DungeonChunkRoomStorage:70}). Every later roll of a cell depends on how many numbers the
     * cell took out of its stream, so the port draws that pick from a stream of its own and this test
     * pins it: after a select, the dungeon stream has to deliver exactly what an untouched one delivers.
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void tankFluidPickLeavesTheDungeonStreamAlone(GameTestHelper helper) {
        ServerLevel server = helper.getLevel();
        WorldGenLevel level = server;
        StoneType[] rocks = {StoneType.LIMESTONE, StoneType.SLATE};
        int minX = (ANCHOR.x) * 16 + 6000;
        int minZ = (ANCHOR.z) * 16;
        int y = GTDungeonLayout.y(level, GTDungeonLayout.MIN_Y);
        GTDungeonData probe = new GTDungeonData(level, minX, y, minZ, rocks[0], rocks[1], 0, new byte[5][5], 2, 2, 0,
                new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                RandomSource.create(7L));
        GTDungeonData control = new GTDungeonData(level, minX, y, minZ, rocks[0], rocks[1], 0, new byte[5][5], 2, 2, 0,
                new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                RandomSource.create(7L));
        String[] fluids = {"Oil_Creosote", "Glue", "Latex"};
        String picked = probe.select(fluids, 3, 1, 7);
        helper.assertTrue(List.of(fluids).contains(picked), "select picks out of the list, got " + picked);
        helper.assertTrue(probe.select(fluids, 3, 1, 7).equals(picked),
                "the same position picks the same fluid (the stream is seeded per position)");
        for (int i = 0; i < 8; i++) {
            helper.assertTrue(probe.next(1000000) == control.next(1000000),
                    "the dungeon stream is untouched by a select (draw " + i + ")");
        }
        helper.succeed();
    }

    /**
     * The bedrock mine grows its pillars out of GT6's own raw ore block
     * ({@code DungeonChunkRoomMiningBedrock:130-132}).
     *
     * <p>The port used to substitute a vanilla deepslate ore here because it had no raw ore block; it has
     * one per material now, so every block of the room's four pillar quadrants has to be a
     * {@code blockRaw} material block whose material is one of GT6's thirteen vein materials - and the
     * vein's own material, since GT6 passes {@code tMaterial.mID} to every one of the three writes.</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void bedrockMineGrowsRawOrePillars(GameTestHelper helper) {
        ServerLevel server = helper.getLevel();
        WorldGenLevel level = server;
        StoneType[] rocks = {StoneType.LIMESTONE, StoneType.SLATE};
        Set<String> veinMaterials = new HashSet<>();
        for (String name : new String[]{"Redstone", "Sulfur", "Hematite", "Pyrolusite", "Apatite",
                "Molybdenite", "Bauxite", "Sphalerite", "Tetrahedrite", "Cassiterite", "Garnierite",
                "Galena"}) {
            veinMaterials.add(name.toLowerCase(java.util.Locale.ROOT));
        }
        int pillars = 0;
        int rooms = 0;
        int lowest = level.getMinBuildHeight() + 5;
        for (int seed = 1; seed <= 24; seed++) {
            int minX = (ANCHOR.x) * 16 + 5000 + seed * 48;
            int minZ = (ANCHOR.z) * 16;
            int y = GTDungeonLayout.y(level, GTDungeonLayout.MIN_Y);
            byte[][] cells = new byte[5][5];
            cells[2][2] = GTDungeonLayout.CORRIDOR;
            GTDungeonData data = new GTDungeonData(level, minX, y, minZ, rocks[0], rocks[1], 3, cells, 2, 2, 4,
                    new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                    new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                    RandomSource.create(seed));
            // The room refuses a cell whose centre has no bedrock under it (GT6 WorldgenOresBedrock:183-185).
            if (!new com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomMiningBedrock().generate(data)) {
                continue;
            }
            rooms++;
            for (int[] quadrant : new int[][]{{1, 1}, {11, 1}, {1, 11}, {11, 11}}) {
                for (int dx = 0; dx < 4; dx++) {
                    for (int dz = 0; dz < 4; dz++) {
                        for (int dy = 0; dy <= 2; dy++) {
                            BlockPos pos = new BlockPos(minX + quadrant[0] + dx, lowest + dy,
                                    minZ + quadrant[1] + dz);
                            BlockState state = server.getBlockState(pos);
                            if (state.isAir()) continue;
                            helper.assertTrue(state.getBlock() instanceof MaterialBlock,
                                    "the pillar is a material block, got " + state);
                            MaterialBlock block = (MaterialBlock) state.getBlock();
                            helper.assertTrue(block.prefix().getName().equals("blockRaw"),
                                    "the pillar is GT6's raw ore block, got " + block.prefix().getName());
                            helper.assertTrue(veinMaterials.contains(
                                            block.material().getName().toLowerCase(java.util.Locale.ROOT)),
                                    "the pillar is of one of GT6's thirteen vein materials, got "
                                            + block.material().getName());
                            pillars++;
                        }
                    }
                }
            }
        }
        helper.assertTrue(rooms > 0, "the bedrock mine accepted at least one cell");
        helper.assertTrue(pillars > 0, "the bedrock mine grew raw ore pillars");
        helper.succeed();
    }

    /**
     * The bedrock mine builds GT6's scaffold and its four corner explosives
     * ({@code DungeonChunkRoomMiningBedrock:55-107} and {@code :122-125}).
     *
     * <p>The port has one {@code gregtech:scaffold} block (GT6 has a brass and a steel multi-tile) and all
     * three dynamite blocks, so the shaft's four pillars, the lattice rim and the crossing walkway are
     * GT6's blocks again - and the four chamber corners carry the boomstick at (6,6), dynamite at (6,9)
     * and (9,6), and the strong dynamite at (9,9), all facing up like GT6's {@code SIDE_Y_POS}.</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void bedrockMineBuildsScaffoldAndExplosives(GameTestHelper helper) {
        ServerLevel server = helper.getLevel();
        WorldGenLevel level = server;
        StoneType[] rocks = {StoneType.LIMESTONE, StoneType.SLATE};
        int floor = level.getMinBuildHeight();
        int chamberBottom = floor + 3;
        int scaffolds = 0;
        int explosives = 0;
        int rooms = 0;
        for (int seed = 1; seed <= 16; seed++) {
            int minX = (ANCHOR.x) * 16 + 7000 + seed * 48;
            int minZ = (ANCHOR.z) * 16;
            int y = GTDungeonLayout.y(level, GTDungeonLayout.MIN_Y);
            // A cell that connects on all four sides: the shaft pillars and the X crossing both trigger.
            byte[][] cells = new byte[5][5];
            cells[2][2] = GTDungeonLayout.CORRIDOR;
            cells[1][2] = GTDungeonLayout.CORRIDOR;
            cells[3][2] = GTDungeonLayout.CORRIDOR;
            cells[2][1] = GTDungeonLayout.CORRIDOR;
            cells[2][3] = GTDungeonLayout.CORRIDOR;
            GTDungeonData data = new GTDungeonData(level, minX, y, minZ, rocks[0], rocks[1], 3, cells, 2, 2, 4,
                    new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                    new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                    RandomSource.create(seed));
            if (!new com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomMiningBedrock().generate(data)) {
                continue;
            }
            rooms++;
            // The four corner explosives of :122-125 face up.
            for (int[] corner : new int[][]{{6, 6}, {6, 9}, {9, 6}, {9, 9}}) {
                BlockState state = server.getBlockState(
                        new BlockPos(minX + corner[0], chamberBottom, minZ + corner[1]));
                helper.assertTrue(state.getBlock() instanceof com.gregtech.gregtech.block.tool.DynamiteBlock,
                        "a chamber corner carries GT6's explosive, got " + state + " at "
                                + corner[0] + "," + corner[1]);
                helper.assertTrue(state.getValue(net.minecraft.world.level.block.DirectionalBlock.FACING)
                                == Direction.UP,
                        "GT6's explosives face up (SIDE_Y_POS)");
                explosives++;
            }
            // The lattice rim (:74-90) and the X crossing walkway (:100) are the port's scaffold.
            for (int[] site : new int[][]{{2, 4}, {13, 4}, {4, 2}, {4, 13}, {8, 8}}) {
                BlockState state = server.getBlockState(new BlockPos(minX + site[0], y, minZ + site[1]));
                if (state.getBlock() instanceof com.gregtech.gregtech.block.tool.ShapedToolBlock scaffold
                        && scaffold.toolId().equals("scaffold")) {
                    scaffolds++;
                }
            }
        }
        helper.assertTrue(rooms > 0, "the bedrock mine accepted at least one cell");
        helper.assertTrue(explosives > 0, "the chamber corners carry GT6's explosives");
        helper.assertTrue(scaffolds > 0, "the lattice carries GT6's scaffold");
        helper.succeed();
    }

    /**
     * The crop farm's gardens plant GT6's own three sapling families
     * ({@code DungeonChunkRoomFarmCrop:160-163}), not just vanilla ones.
     *
     * <p>GT6's write is {@code set(Saplings_AB, next(maxMeta), Saplings_CD, next(maxMeta),
     * Blocks.sapling, next(6))}: three rolls for the three metas and one more for the block pick. The
     * port has one sapling block per GT wood, so the two GT families are the port's own species saplings;
     * this test walks the four garden lines and demands that every sapling there is one, and that GT
     * saplings really appear (the earlier port picked a vanilla one every time).</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void cropFarmPlantsGtSaplings(GameTestHelper helper) {
        ServerLevel server = helper.getLevel();
        WorldGenLevel level = server;
        StoneType[] rocks = {StoneType.LIMESTONE, StoneType.SLATE};
        int gt = 0;
        int vanilla = 0;
        int blueSpruce = 0;
        int flowers = 0;
        int rooms = 0;
        int scanX = 0, scanY = 0, scanZ = 0;    // the last accepted cell, for the flower scan below
        for (int seed = 1; seed <= 24; seed++) {
            int minX = (ANCHOR.x) * 16 + 9000 + seed * 48;
            int minZ = (ANCHOR.z) * 16;
            int y = GTDungeonLayout.y(level, GTDungeonLayout.MIN_Y);
            byte[][] cells = new byte[5][5];
            GTDungeonData data = new GTDungeonData(level, minX, y, minZ, rocks[0], rocks[1], 3, cells, 2, 2, 0,
                    new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                    new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                    RandomSource.create(seed));
            if (!new com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomFarmCrop().generate(data)) {
                continue;
            }
            rooms++;
            scanX = minX;
            scanY = y;
            scanZ = minZ;
            for (int step = 0; step <= 15; step++) {
                for (int[] site : new int[][]{{14, step}, {1, step}, {step, 14}, {step, 1}}) {
                    BlockPos pos = new BlockPos(minX + site[0], y + 2, minZ + site[1]);
                    BlockState state = server.getBlockState(pos);
                    if (!(state.getBlock() instanceof net.minecraft.world.level.block.SaplingBlock)) continue;
                    String id = String.valueOf(
                            net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()));
                    helper.assertTrue(id.endsWith("_sapling") || id.contains("sapling"),
                            "a garden sapling, got " + id);
                    if (id.startsWith("gregtech:")) {
                        gt++;
                        helper.assertTrue(java.util.Set.of("gregtech:sapling_rubber", "gregtech:sapling_maple",
                                        "gregtech:sapling_willow", "gregtech:sapling_blue_mahoe",
                                        "gregtech:sapling_hazel", "gregtech:sapling_cinnamon",
                                        "gregtech:sapling_coconut", "gregtech:sapling_rainbowood",
                                        "gregtech:sapling_bluespruce").contains(id),
                                "GT6 dungeon gardens only choose AB 0..7 or CD 0, got " + id);
                        if (id.equals("gregtech:sapling_bluespruce")) blueSpruce++;
                    } else {
                        vanilla++;
                    }
                }
            }
        }
        helper.assertTrue(rooms > 0, "the crop farm accepted at least one cell");
        helper.assertTrue(gt > 0, "the gardens plant GT6's own saplings (saw " + gt + " GT, "
                + vanilla + " vanilla)");
        helper.assertTrue(blueSpruce > 0, "the CD family contributes GT6's Blue Spruce (saw " + gt
                + " GT saplings, no Blue Spruce)");
        helper.assertTrue(vanilla > 0, "and the six vanilla saplings are still part of GT6's list");
        // The room's loose flowers stand one layer above the tall plants' planter row (GT6 :165-168), so
        // they are the second half of the same "planter is a plant support" question §120 fixed - count
        // them off the same four lines, one layer higher.
        for (int step = 0; step <= 15; step++) {
            for (int[] site : new int[][]{{14, step}, {1, step}, {step, 14}, {step, 1}}) {
                BlockPos pos = new BlockPos(scanX + site[0], scanY + 5, scanZ + site[1]);
                if (server.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.FlowerBlock) {
                    flowers++;
                }
            }
        }
        helper.assertTrue(flowers > 0, "the gardens' loose flowers survive on the port's planter too");
        helper.succeed();
    }

    /**
     * The workshop's bottle crate is the port's own nine bottle crate now
     * ({@code GTDungeonChunkRoomWorkshop:149}, GT6 multi-tile 8762), not the quad drawer stand-in.
     *
     * Legacy helper-level inventory coverage; real nine-cell interactions are tested in BottleCrateRepairTests.
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void workshopPlacesItsBottleCrate(GameTestHelper helper) {
        ServerLevel server = helper.getLevel();
        WorldGenLevel level = server;
        StoneType[] rocks = {StoneType.LIMESTONE, StoneType.SLATE};
        BlockPos pos = helper.absolutePos(new BlockPos(1,1,1));
        server.setBlock(pos, com.gregtech.gregtech.registry.GTStorage.BOTTLE_CRATE.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.WEST), 3);
        helper.assertTrue(server.getBlockEntity(pos)
                instanceof com.gregtech.gregtech.blockentity.inventory.BottleCrateBlockEntity,
                "the bottle crate carries its own block entity");
        var crate = (com.gregtech.gregtech.blockentity.inventory.BottleCrateBlockEntity)
                server.getBlockEntity(pos);
        ItemStack bottle = new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
        for (int i = 0; i < com.gregtech.gregtech.blockentity.inventory.BottleCrateBlockEntity.SLOTS; i++) {
            helper.assertTrue(crate.insert(bottle.copy()).isEmpty(), "bottle " + i + " fits");
        }
        helper.assertFalse(crate.insert(bottle.copy()).isEmpty(),
                "all nine crate slots are occupied");
        helper.assertFalse(crate.extractLast().isEmpty(), "the legacy extraction helper returns one bottle");
        helper.assertTrue(crate.contents().size()
                        == com.gregtech.gregtech.blockentity.inventory.BottleCrateBlockEntity.SLOTS - 1,
                "eight bottles are left, got " + crate.contents().size());
        server.destroyBlock(pos, true);
        helper.runAfterDelay(1, () -> {
            var drops=server.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2));
            helper.assertTrue(drops.stream().anyMatch(e->net.minecraft.world.item.BlockItem.getBlockEntityData(e.getItem())!=null),"broken bottle crate preserves its inventory in the block item");
            helper.succeed();
        });
    }

    /** Which of the three tiers of a smeltery block the placed state is, or {@code -1}. */
    private static int tier(BlockState state,
                            net.minecraftforge.registries.RegistryObject<? extends net.minecraft.world.level.block.Block> bronze,
                            net.minecraftforge.registries.RegistryObject<? extends net.minecraft.world.level.block.Block> invar,
                            net.minecraftforge.registries.RegistryObject<? extends net.minecraft.world.level.block.Block> steel) {
        if (state.getBlock() == bronze.get()) return 0;
        if (state.getBlock() == invar.get()) return 1;
        if (state.getBlock() == steel.get()) return 2;
        return -1;
    }
}
