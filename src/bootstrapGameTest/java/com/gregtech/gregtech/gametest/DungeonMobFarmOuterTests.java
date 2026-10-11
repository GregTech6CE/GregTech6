package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.machine.ItemPipeBlock;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomFarmMobs;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonData;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonLayout;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonSharedState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/** GT6's eight optional mob-farm platforms, clipped to one 16x16 target chunk per feature call. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class DungeonMobFarmOuterTests {
    private DungeonMobFarmOuterTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void featureDispatchAcceptsFarmPlatformOnLayoutBorder(GameTestHelper helper) {
        byte[][] cells = new byte[5][5];
        cells[1][1] = 1;
        cells[2][1] = GTDungeonLayout.CORRIDOR;
        cells[1][2] = GTDungeonLayout.CORRIDOR;
        // Every corner and edge is a valid outer-platform location, including the last index.
        for (int i = 0; i < 5; i++) for (int j = 0; j < 5; j++) {
            int expected = 0;
            for (var side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                int x = i + side.getStepX(), z = j + side.getStepZ();
                if (x >= 0 && x < 5 && z >= 0 && z < 5 && cells[x][z] != 0) expected++;
            }
            helper.assertTrue(GTDungeonLayout.connectionCount(cells, i, j) == expected,
                    "bounded neighbour count for cell " + i + "," + j);
        }
        long chosenSeed = -1;
        for (long seed = 0; seed < 1000; seed++) {
            if (GTDungeonSharedState.create(cells, seed).cell(1,1).chosenRoom()
                    instanceof GTDungeonChunkRoomFarmMobs) { chosenSeed = seed; break; }
        }
        helper.assertTrue(chosenSeed >= 0, "border-adjacent mob farm found");
        var anchor = new ChunkPos(2072,2072);
        helper.assertTrue(GTDungeonFeature.generateCell(helper.getLevel(), anchor, cells, 0,0,
                GTDungeonLayout.MIN_Y, 3, new StoneType[]{StoneType.LIMESTONE, StoneType.SLATE}, chosenSeed),
                "real feature dispatch generates its empty border cell without indexing outside the layout");
        int y = GTDungeonLayout.y(helper.getLevel(), GTDungeonLayout.MIN_Y);
        helper.assertTrue(helper.getLevel().getBlockState(new BlockPos((anchor.x-2)*16+8,y+43,(anchor.z-2)*16+8))
                        .is(com.gregtech.gregtech.registry.GTBlocks.getStoneState(StoneType.LIMESTONE,
                                com.gregtech.gregtech.block.stone.StoneVariant.TILES).getBlock()),
                "border platform has its roof, not a silently skipped cell");
        helper.succeed();
    }

    private static byte[][] northWestOnly() {
        byte[][] cells = new byte[5][5];
        cells[2][2] = 1;
        cells[1][2] = GTDungeonLayout.CORRIDOR;
        cells[2][1] = GTDungeonLayout.CORRIDOR;
        cells[3][1] = GTDungeonLayout.BARRACKS;
        cells[1][3] = GTDungeonLayout.BARRACKS;
        cells[3][3] = GTDungeonLayout.BARRACKS;
        return cells;
    }

    @GameTest(template = "test_empty")
    public static void gt6ThreeCellQuadrantGateSelectsOnlyPermittedPlatforms(GameTestHelper helper) {
        byte[][] allFree = new byte[5][5];
        allFree[2][2] = 1;
        var complete = GTDungeonChunkRoomFarmMobs.footprint(allFree, 2, 2);
        for (int i = 1; i <= 3; i++) for (int j = 1; j <= 3; j++)
            helper.assertTrue(complete.includes(i, j), "GT6 fully open layout builds all nine platforms");
        byte[][] cells = northWestOnly();
        var footprint = GTDungeonChunkRoomFarmMobs.footprint(cells, 2, 2);
        for (int i = 1; i <= 3; i++) for (int j = 1; j <= 3; j++) {
            boolean expected = (i == 2 && j == 2) || (i <= 2 && j <= 2);
            helper.assertTrue(footprint.includes(i, j) == expected,
                    "GT6 quadrant gate at " + i + "," + j);
        }
        cells[1][1] = GTDungeonLayout.ENTRANCE;
        var blocked = GTDungeonChunkRoomFarmMobs.footprint(cells, 2, 2);
        helper.assertTrue(blocked.includes(2, 2) && !blocked.includes(1, 1)
                        && !blocked.includes(1, 2) && !blocked.includes(2, 1),
                "blocking one of the three required cells removes the whole quadrant");
        helper.succeed();
    }

    private static Map<BlockPos, BlockState> recordOrder(byte[][] cells, int[][] order) {
        var footprint = GTDungeonChunkRoomFarmMobs.footprint(cells, 2, 2);
        Map<BlockPos, BlockState> writes = new HashMap<>();
        int[] owner = new int[2];
        WorldGenLevel recorder = (WorldGenLevel) Proxy.newProxyInstance(
                WorldGenLevel.class.getClassLoader(), new Class<?>[]{WorldGenLevel.class},
                (proxy, method, args) -> {
                    if (!method.getName().equals("setBlock"))
                        throw new AssertionError("mob-farm platform unexpectedly read " + method.getName());
                    BlockPos pos = (BlockPos) args[0];
                    if (pos.getX() >> 4 != owner[0] || pos.getZ() >> 4 != owner[1])
                        throw new AssertionError("mob-farm wrote outside target chunk: " + pos);
                    writes.put(pos.immutable(), (BlockState) args[1]);
                    return true;
                });
        for (int[] cell : order) {
            int i = cell[0], j = cell[1];
            if (!footprint.includes(i, j)) throw new AssertionError("inactive platform in test order");
            owner[0] = i - 2;
            owner[1] = j - 2;
            GTDungeonData data = new GTDungeonData(recorder, owner[0] * 16, 40, owner[1] * 16,
                    StoneType.LIMESTONE, StoneType.SLATE, 3, cells, i, j, 0,
                    new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                    new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                    RandomSource.create(GTDungeonFeature.cellSeed(577L, i, j)));
            GTDungeonChunkRoomFarmMobs.generateOuterPlatform(data);
            GTDungeonChunkRoomFarmMobs.addPipeFragments(data, footprint, i, j);
        }
        return writes;
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void targetChunkOwnsEveryPlatformAndPipeWriteInEitherOrder(GameTestHelper helper) {
        byte[][] cells = northWestOnly();
        int[][] forward = {{2, 2}, {1, 2}, {2, 1}, {1, 1}};
        int[][] reverse = {{1, 1}, {2, 1}, {1, 2}, {2, 2}};
        Map<BlockPos, BlockState> first = recordOrder(cells, forward);
        Map<BlockPos, BlockState> second = recordOrder(cells, reverse);
        helper.assertTrue(first.equals(second) && first.size() > 20000,
                "all four GT6 platform shells and pipe fragments are chunk-order independent");
        BlockState centreWest = first.get(new BlockPos(7, 47, 8));
        BlockState westEast = first.get(new BlockPos(-1, 47, 8));
        BlockState centreNorth = first.get(new BlockPos(8, 47, 7));
        BlockState northSouth = first.get(new BlockPos(8, 47, -1));
        helper.assertTrue(centreWest != null && centreWest.getBlock() instanceof ItemPipeBlock
                        && westEast != null && westEast.getBlock() instanceof ItemPipeBlock
                        && centreNorth != null && centreNorth.getBlock() instanceof ItemPipeBlock
                        && northSouth != null && northSouth.getBlock() instanceof ItemPipeBlock,
                "GT6 west/north pipe runs meet the centre across both chunk borders");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void featureDispatchBuildsEmptyOuterCellWithoutTouchingItsNeighbour(GameTestHelper helper) {
        byte[][] cells = northWestOnly();
        long chosenSeed = -1;
        for (long seed = 0; seed < 1000; seed++) {
            if (GTDungeonSharedState.create(cells, seed).cell(2, 2).chosenRoom()
                    instanceof GTDungeonChunkRoomFarmMobs) {
                chosenSeed = seed;
                break;
            }
        }
        helper.assertTrue(chosenSeed >= 0, "a deterministic GT6 mob-farm room was found");
        ChunkPos anchor = new ChunkPos(2062, 2062);
        int x = (anchor.x - 2 + 1) * 16, z = (anchor.z - 2 + 1) * 16;
        int y = GTDungeonLayout.y(helper.getLevel(), GTDungeonLayout.MIN_Y);
        BlockPos neighbour = new BlockPos(x + 16, y + 7, z + 8);
        BlockState before = helper.getLevel().getBlockState(neighbour);
        helper.assertTrue(GTDungeonFeature.generateCell(helper.getLevel(), anchor, cells, 1, 1,
                        GTDungeonLayout.MIN_Y, 3, new StoneType[]{StoneType.LIMESTONE, StoneType.SLATE},
                        chosenSeed), "a planned empty cell builds its own NW outer platform");
        helper.assertTrue(helper.getLevel().getBlockState(new BlockPos(x + 8, y + 43, z + 8))
                        .is(com.gregtech.gregtech.registry.GTBlocks.getStoneState(
                                StoneType.LIMESTONE, com.gregtech.gregtech.block.stone.StoneVariant.TILES).getBlock()),
                "outer platform owns GT6's upper roof");
        helper.assertTrue(helper.getLevel().getBlockState(new BlockPos(x + 9, y + 7, z + 8))
                        .getBlock() instanceof ItemPipeBlock,
                "diagonal outer platform owns its cross-cell pipe fragment");
        helper.assertTrue(helper.getLevel().getBlockState(neighbour).equals(before),
                "empty-cell generation never wrote into the east neighbouring chunk");
        BlockState nwRoof = helper.getLevel().getBlockState(new BlockPos(x + 8, y + 43, z + 8));
        helper.assertTrue(GTDungeonFeature.generateCell(helper.getLevel(), anchor, cells, 1, 2,
                        GTDungeonLayout.MIN_Y, 3, new StoneType[]{StoneType.LIMESTONE, StoneType.SLATE},
                        chosenSeed), "the western corridor builds its own farm platform after its corridor");
        helper.assertTrue(helper.getLevel().getBlockState(new BlockPos(x + 8, y + 43, z + 24)).equals(nwRoof),
                "corridor-hosted platform has the same GT6 stone roof");
        helper.assertTrue(helper.getLevel().getBlockState(new BlockPos(x + 8, y + 7, z + 16))
                        .getBlock() instanceof ItemPipeBlock,
                "diagonal pipe enters the western corridor cell without a cross-chunk write");
        helper.assertTrue(helper.getLevel().getBlockState(new BlockPos(x + 8, y + 43, z + 8)).equals(nwRoof),
                "later corridor cell generation does not overwrite the already built diagonal platform");
        helper.succeed();
    }
}
