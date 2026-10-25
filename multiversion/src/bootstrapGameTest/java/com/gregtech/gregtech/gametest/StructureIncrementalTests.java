package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.multiblock.MultiblockLayout;
import com.gregtech.gregtech.api.multiblock.tracking.Aggregate;
import com.gregtech.gregtech.api.multiblock.tracking.CompiledPattern;
import com.gregtech.gregtech.api.multiblock.tracking.PatternState;
import com.gregtech.gregtech.api.multiblock.tracking.StructureDefinition;
import com.gregtech.gregtech.api.multiblock.tracking.StructureScanProbe;
import com.gregtech.gregtech.api.multiblock.tracking.StructureScheduler;
import com.gregtech.gregtech.api.multiblock.tracking.StructureTracking;
import com.gregtech.gregtech.api.multiblock.tracking.TrackedMultiblock;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.machine.FuelTurbineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.FusionReactorControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.ImplosionCompressorControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LargeCrucibleControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LargeGasTurbineControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LargeHeatExchangerControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LargeTurbineControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.OriginalLargeBoilerControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.VonDaGraaggControllerBlockEntity;
import com.gregtech.gregtech.content.multiblock.FusionStructure;
import com.gregtech.gregtech.content.multiblock.LargeMachineLayouts;
import com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerSpecs;
import com.gregtech.gregtech.content.multiblock.PlainStructureBlocks;
import com.gregtech.gregtech.content.multiblock.SharedHeatExchangerStructure;
import com.gregtech.gregtech.content.multiblock.SharedLargeCrucibleStructure;
import com.gregtech.gregtech.content.multiblock.SharedLargeMachineLayouts;
import com.gregtech.gregtech.content.multiblock.TurbineStructure;
import com.gregtech.gregtech.content.multiblock.VonDaGraaggRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * Card 53: declarative incremental evaluation. The matcher/aggregate machinery is exercised on
 * synthetic patterns, one-cell cost is measured with the read probe, and each migrated family is
 * fuzzed with the test-build consistency check comparing every incremental verdict to a full scan.
 */
@GameTestHolder("gt6ce_structure_incremental")
@PrefixGameTestTemplate(false)
public final class StructureIncrementalTests {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int FUZZ_STEPS = 2000;

    private StructureIncrementalTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 200, batch = "st-inc-aggregate")
    public static void syntheticAggregatesRotationAndChunkReload(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        resolved().put("minecraft:stone", Blocks.STONE);
        resolved().put("minecraft:dirt", Blocks.DIRT);
        BlockPos origin = new BlockPos(27_000_000, 80, 27_000_000);
        List<BlockPos> placed = new ArrayList<>();
        try {
            var counts = new LargeMachineLayouts.Layout(List.of(
                    plain(1, 0, 0, "minecraft:stone"),
                    plain(2, 0, 0, "minecraft:stone"),
                    plain(3, 0, 0, "minecraft:stone")), List.of());
            var counting = CompiledPattern.compile(dummy(), counts, Direction.NORTH,
                    List.of(Aggregate.Count.exactly(Aggregate.plain("minecraft:stone"), 3)));
            var state = new PatternState(counting, level.getHeight());
            state.evaluate(level, origin);
            h.assertTrue(!state.complete(), "an empty pattern is incomplete");
            BlockPos first = counts.cells().get(0).at(origin, Direction.NORTH);
            BlockPos second = counts.cells().get(1).at(origin, Direction.NORTH);
            BlockPos third = counts.cells().get(2).at(origin, Direction.NORTH);
            place(level, first, Blocks.STONE, placed);
            place(level, second, Blocks.STONE, placed);
            place(level, third, Blocks.STONE, placed);
            state.applyChanges(origin, Set.of(first.asLong(), second.asLong(), third.asLong()), Set.of());
            state.evaluate(level, origin);
            h.assertTrue(state.complete(), "three stone cells satisfy the exact count");

            place(level, second, Blocks.DIRT, placed);
            state.applyChanges(origin, Set.of(second.asLong()), Set.of());
            state.evaluate(level, origin);
            h.assertTrue(!state.complete(), "one replaced cell breaks the exact count");

            // Same-block aggregate: both cells accept stone or dirt, but not a mix.
            var mixed = new LargeMachineLayouts.Layout(List.of(
                    plain(1, 0, 0, "minecraft:stone", "minecraft:dirt"),
                    plain(2, 0, 0, "minecraft:stone", "minecraft:dirt")), List.of());
            var sameness = CompiledPattern.compile(dummy(), mixed, Direction.NORTH,
                    List.of(new Aggregate.SameBlock(Aggregate.all())));
            var sameState = new PatternState(sameness, level.getHeight());
            BlockPos left = mixed.cells().get(0).at(origin, Direction.NORTH);
            BlockPos right = mixed.cells().get(1).at(origin, Direction.NORTH);
            place(level, left, Blocks.STONE, placed);
            place(level, right, Blocks.STONE, placed);
            sameState.evaluate(level, origin);
            h.assertTrue(sameState.complete(), "two stone cells share a block");
            place(level, right, Blocks.DIRT, placed);
            sameState.applyChanges(origin, Set.of(right.asLong()), Set.of());
            sameState.evaluate(level, origin);
            h.assertTrue(!sameState.complete(), "a mixed pair fails the same-block aggregate");

            // The same compiled layout for another facing resolves different offsets.
            var turned = CompiledPattern.compile(dummy(), counts, Direction.EAST,
                    List.of(Aggregate.Count.exactly(Aggregate.plain("minecraft:stone"), 3)));
            BlockPos south = counts.cells().get(0).at(origin, Direction.EAST);
            h.assertTrue(!south.equals(first) && south.getZ() != first.getZ(), "rotation moves a fixed offset");

            // A reloaded chunk is re-read even when no change event was delivered.
            place(level, first, Blocks.STONE, placed);
            place(level, second, Blocks.STONE, placed);
            place(level, third, Blocks.STONE, placed);
            var reloadState = new PatternState(counting, level.getHeight());
            reloadState.evaluate(level, origin);
            h.assertTrue(reloadState.complete(), "the restored pattern is complete");
            place(level, third, Blocks.DIRT, placed);
            long chunk = ChunkPos.asLong(third.getX() >> 4, third.getZ() >> 4);
            reloadState.applyChanges(origin, Set.of(), Set.of(chunk));
            reloadState.evaluate(level, origin);
            h.assertTrue(!reloadState.complete(), "a reloaded chunk sees the changed cell");
        } finally {
            clear(level, placed);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200, batch = "st-inc-match")
    public static void matcherCandidatesMatchTheScanOrder(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = new BlockPos(27_200_000, 80, 27_200_000);
        List<BlockPos> placed = new ArrayList<>();
        try {
            var layout = new LargeMachineLayouts.Layout(List.of(
                    new LargeMachineLayouts.Cell(1, 0, 0, 18000, MultiblockLayout.Role.CASING),
                    plain(2, 0, 0, "#minecraft:base_stone_overworld"),
                    plain(3, 0, 0, "anyfluid#minecraft:water")), List.of());
            var pattern = CompiledPattern.compile(dummy(), layout, Direction.NORTH, List.of());
            BlockPos partPos = layout.cells().get(0).at(origin, Direction.NORTH);
            BlockPos stonePos = layout.cells().get(1).at(origin, Direction.NORTH);
            BlockPos waterPos = layout.cells().get(2).at(origin, Direction.NORTH);
            place(level, partPos, part(18000), placed);
            place(level, stonePos, Blocks.STONE.defaultBlockState(), placed);
            place(level, waterPos, Blocks.WATER.defaultBlockState(), placed);
            var state = new PatternState(pattern, level.getHeight());
            state.evaluate(level, origin);
            h.assertTrue(state.complete(), "part, block tag and fluid tag cells accept their states");

            place(level, stonePos, Blocks.DIRT.defaultBlockState(), placed);
            state.applyChanges(origin, Set.of(stonePos.asLong()), Set.of());
            state.evaluate(level, origin);
            h.assertTrue(!state.complete(), "a block outside the tag is rejected");

            place(level, stonePos, Blocks.STONE.defaultBlockState(), placed);
            place(level, waterPos, Blocks.LAVA.defaultBlockState(), placed);
            state.applyChanges(origin, Set.of(stonePos.asLong(), waterPos.asLong()), Set.of());
            state.evaluate(level, origin);
            h.assertTrue(!state.complete(), "a fluid outside the fluid tag is rejected");
        } finally {
            clear(level, placed);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400, batch = "st-inc-cost")
    public static void oneCellEditReadsOnlyItsLayerAndTheFrontier(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var layer = new LargeMachineLayouts.Cell(1, 0, 0, 18100, MultiblockLayout.Role.ITEM_FLUID_INPUT);
        var repeat = new LargeMachineLayouts.Repeat("layers", 1, 1, 12, 1, 1, List.of(layer));
        var layout = new LargeMachineLayouts.Layout(List.of(), List.of(repeat));
        List<BlockPos> placed = new ArrayList<>();
        try {
            withLayouts(Map.of("irrigator", layout), () -> {
                BlockPos origin = new BlockPos(27_400_000, 80, 27_400_000);
                put(level, origin, facingNorth(block("addon_sample", "irrigator")), placed);
                List<BlockPos> layers = new ArrayList<>();
                for (int copy = 0; copy < 12; copy++) {
                    BlockPos pos = origin.east().above(1 + copy);
                    place(level, pos, part(18100), placed);
                    layers.add(pos);
                }
                BlockEntity entity = level.getBlockEntity(origin);
                h.assertTrue(entity instanceof TrackedMultiblock, "behavior controller");
                h.assertTrue(StructureTestSupport.ok(entity), "twelve repeat layers form");
                var tracker = StructureTracking.tracker(entity);
                // StructureTestSupport initializes on first use and turns the consistency scan on.
                boolean consistency = StructureTracking.consistencyCheck();
                StructureTracking.consistencyCheck(false);
                try {
                    StructureScanProbe.enable(true);
                    StructureScanProbe.reset();
                    place(level, layers.get(5), Blocks.AIR.defaultBlockState(), new ArrayList<>());
                    StructureScheduler.flush(level);
                    int brokenMatchers = StructureScanProbe.matchers();
                    int brokenReads = StructureScanProbe.blockReads();
                    System.out.println("[structure-incremental] cost after break state=" + tracker.state()
                            + " layers=" + java.util.Arrays.toString(tracker.matchedLayers())
                            + " matchers=" + brokenMatchers + " blockReads=" + brokenReads);
                    h.assertTrue(tracker.matchedLayers()[0] == 5,
                            "a hole truncates the repeat to five layers, layers="
                                    + java.util.Arrays.toString(tracker.matchedLayers()));
                    h.assertTrue(StructureTestSupport.ok(entity), "the truncated structure stays formed");
                    h.assertTrue(brokenMatchers <= 2,
                            "one break evaluates only that layer, matchers=" + brokenMatchers);
                    h.assertTrue(brokenReads <= 4, "one break reads only the dirty cell, reads=" + brokenReads);

                    StructureScanProbe.reset();
                    place(level, layers.get(5), part(18100), new ArrayList<>());
                    StructureScheduler.flush(level);
                    int repairedMatchers = StructureScanProbe.matchers();
                    System.out.println("[structure-incremental] cost breakMatchers=" + brokenMatchers
                            + " breakBlockReads=" + brokenReads + " repairMatchers=" + repairedMatchers);
                    h.assertTrue(StructureTestSupport.ok(entity), "the repaired stack forms again");
                    h.assertTrue(repairedMatchers <= 8,
                            "repair re-reads the broken layer and the layers above, matchers=" + repairedMatchers);
                } finally {
                    StructureTracking.consistencyCheck(consistency);
                    StructureScanProbe.enable(false);
                    StructureScanProbe.reset();
                }
            });
        } finally {
            clear(level, placed);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 600, batch = "st-fixed-fusion")
    public static void fusionFixedPatternTracksProcessorCountsAndShellEdits(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = new BlockPos(28_600_000, 80, 28_600_000);
        List<BlockPos> placed = new ArrayList<>();
        Map<BlockPos, BlockState> shell = new LinkedHashMap<>();
        try {
            for (var cell : FusionStructure.CELLS)
                shell.putIfAbsent(cell.at(origin, Direction.NORTH), cell.block().defaultBlockState());
            int minX = shell.keySet().stream().mapToInt(BlockPos::getX).min().orElseThrow() - 1;
            int maxX = shell.keySet().stream().mapToInt(BlockPos::getX).max().orElseThrow() + 1;
            int minY = shell.keySet().stream().mapToInt(BlockPos::getY).min().orElseThrow() - 1;
            int maxY = shell.keySet().stream().mapToInt(BlockPos::getY).max().orElseThrow() + 1;
            int minZ = shell.keySet().stream().mapToInt(BlockPos::getZ).min().orElseThrow() - 1;
            int maxZ = shell.keySet().stream().mapToInt(BlockPos::getZ).max().orElseThrow() + 1;
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ)))
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);

            Block controller = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "fusion_reactor_main"));
            h.assertTrue(controller instanceof BasicMachineBlock basic && "fusionreactor".equals(basic.basicSpec().machineName()),
                    "registered fusion controller exists on this loader");
            put(level, origin, controller.defaultBlockState().setValue(BasicMachineBlock.FACING, Direction.NORTH), placed);
            for (var entry : shell.entrySet()) put(level, entry.getKey(), entry.getValue(), placed);
            BlockEntity entity = level.getBlockEntity(origin);
            h.assertTrue(entity instanceof FusionReactorControllerBlockEntity, "dedicated fusion executor");
            h.assertTrue(StructureTestSupport.ok(entity), "fusion ring starts formed");

            BlockPos firstProcessor = null, secondType = null;
            for (var cell : FusionStructure.CELLS) {
                if (cell.part() == 18200 && firstProcessor == null) firstProcessor = cell.at(origin, Direction.NORTH);
                if (cell.part() == 18201 && secondType == null) secondType = cell.at(origin, Direction.NORTH);
            }
            place(level, firstProcessor, part(18201), placed);
            place(level, secondType, part(18200), placed);
            h.assertTrue(StructureTestSupport.ok(entity), "processors may swap positions while the 3/12/12 counts hold");
            place(level, firstProcessor, part(18202), placed);
            h.assertTrue(!StructureTestSupport.ok(entity), "changing one processor type fails the 3/12/12 count");
            place(level, firstProcessor, part(18201), placed);
            h.assertTrue(StructureTestSupport.ok(entity), "restoring the processor type forms again");
            place(level, firstProcessor, part(18200), placed);
            place(level, secondType, part(18201), placed);
            h.assertTrue(StructureTestSupport.ok(entity), "the original processor assignment forms again");

            BlockPos shellCell = shell.keySet().iterator().next();
            place(level, shellCell, Blocks.AIR.defaultBlockState(), placed);
            h.assertTrue(!StructureTestSupport.ok(entity), "a missing shell cell breaks the ring");
            place(level, shellCell, shell.get(shellCell), placed);
            h.assertTrue(StructureTestSupport.ok(entity), "replacing the shell cell forms again");

            List<BlockPos> cells = new ArrayList<>(shell.keySet());
            Random random = new Random(54_000_001L);
            System.out.println("[structure-incremental] fuzz family=fusion seed=54000001 steps=300 cells=" + cells.size());
            for (int step = 0; step < 300; step++) {
                BlockPos pos = cells.get(random.nextInt(cells.size()));
                int roll = random.nextInt(10);
                if (roll < 5) place(level, pos, shell.get(pos), placed);
                else if (roll < 8) place(level, pos, Blocks.AIR.defaultBlockState(), placed);
                else place(level, pos, Blocks.COBBLESTONE.defaultBlockState(), placed);
                StructureScheduler.flush(level);
            }
            for (var entry : shell.entrySet()) place(level, entry.getKey(), entry.getValue(), placed);
            StructureScheduler.flush(level);
            h.assertTrue(StructureTestSupport.ok(entity), "fusion recovers after the fuzz");
        } finally {
            clear(level, placed);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400, batch = "st-fixed-implosion")
    public static void implosionFixedPatternTracksAirAndShellEdits(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = new BlockPos(28_800_000, 80, 28_800_000);
        List<BlockPos> placed = new ArrayList<>();
        List<BlockPos> walls = new ArrayList<>();
        BlockPos airCell = null;
        try {
            Block controller = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "implosion_compressor_main"));
            h.assertTrue(controller != Blocks.AIR, "registered implosion controller exists on this loader");
            List<BlockPos> cells = new ArrayList<>();
            for (var cell : ImplosionCompressorControllerBlockEntity.LAYOUT.cells())
                cells.add(cell.at(origin, Direction.NORTH));
            for (BlockPos pos : cells) level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
            put(level, origin, controller.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), placed);
            for (var cell : ImplosionCompressorControllerBlockEntity.LAYOUT.cells()) {
                BlockPos pos = cell.at(origin, Direction.NORTH);
                if (cell.role() == MultiblockLayout.Role.AIR) {
                    airCell = pos;
                    place(level, pos, Blocks.AIR.defaultBlockState(), placed);
                } else {
                    place(level, pos, part(18023), placed);
                    walls.add(pos);
                }
            }
            BlockEntity entity = level.getBlockEntity(origin);
            h.assertTrue(entity instanceof ImplosionCompressorControllerBlockEntity, "dedicated implosion executor");
            h.assertTrue(StructureTestSupport.ok(entity), "implosion shell starts formed");

            BlockPos wall = walls.get(0);
            place(level, wall, Blocks.AIR.defaultBlockState(), placed);
            h.assertTrue(!StructureTestSupport.ok(entity), "a missing wall breaks the shell");
            place(level, wall, part(18023), placed);
            h.assertTrue(StructureTestSupport.ok(entity), "replacing the wall forms again");
            place(level, airCell, part(18023), placed);
            h.assertTrue(!StructureTestSupport.ok(entity), "a filled air cell breaks the shell");
            place(level, airCell, Blocks.AIR.defaultBlockState(), placed);
            h.assertTrue(StructureTestSupport.ok(entity), "clearing the air cell forms again");

            Random random = new Random(54_000_002L);
            System.out.println("[structure-incremental] fuzz family=implosion seed=54000002 steps=200 cells=" + walls.size());
            for (int step = 0; step < 200; step++) {
                BlockPos pos = walls.get(random.nextInt(walls.size()));
                int roll = random.nextInt(10);
                if (roll < 5) place(level, pos, part(18023), placed);
                else if (roll < 8) place(level, pos, Blocks.AIR.defaultBlockState(), placed);
                else place(level, pos, Blocks.COBBLESTONE.defaultBlockState(), placed);
                StructureScheduler.flush(level);
            }
            for (BlockPos pos : walls) place(level, pos, part(18023), placed);
            StructureScheduler.flush(level);
            h.assertTrue(StructureTestSupport.ok(entity), "implosion recovers after the fuzz");
        } finally {
            clear(level, placed);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400, batch = "st-fixed-boiler")
    public static void boilerFixedPatternTracksWallsAndAir(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = new BlockPos(29_000_000, 80, 29_000_000);
        List<BlockPos> placed = new ArrayList<>();
        Map<BlockPos, BlockState> shell = new LinkedHashMap<>();
        BlockPos airCell = null;
        try {
            Block controller = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "stainless_steel_boiler_main_barometer"));
            h.assertTrue(controller != Blocks.AIR, "registered boiler controller exists on this loader");
            List<BlockPos> cells = new ArrayList<>();
            for (var cell : OriginalLargeBoilerSpecs.LAYOUT.cells())
                if (!(cell.right() == 0 && cell.up() == 0 && cell.back() == 0))
                    cells.add(cell.at(origin, Direction.NORTH));
            for (BlockPos pos : cells) level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
            put(level, origin, controller.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), placed);
            BlockEntity entity = level.getBlockEntity(origin);
            h.assertTrue(entity instanceof OriginalLargeBoilerControllerBlockEntity, "dedicated boiler executor");
            int wall = ((OriginalLargeBoilerControllerBlockEntity) entity).variant().denseWallId();
            for (var cell : OriginalLargeBoilerSpecs.LAYOUT.cells()) {
                if (cell.right() == 0 && cell.up() == 0 && cell.back() == 0) continue;
                BlockPos pos = cell.at(origin, Direction.NORTH);
                if (cell.role() == MultiblockLayout.Role.AIR) {
                    airCell = pos;
                    place(level, pos, Blocks.AIR.defaultBlockState(), placed);
                } else {
                    BlockState state = (cell.role() == MultiblockLayout.Role.HEAT_INPUT ? part(18101) : part(wall));
                    place(level, pos, state, placed);
                    shell.put(pos.immutable(), state);
                }
            }
            h.assertTrue(StructureTestSupport.ok(entity), "boiler shell starts formed");

            BlockPos wallCell = shell.keySet().iterator().next();
            place(level, wallCell, Blocks.AIR.defaultBlockState(), placed);
            h.assertTrue(!StructureTestSupport.ok(entity), "a missing wall breaks the boiler");
            place(level, wallCell, shell.get(wallCell), placed);
            h.assertTrue(StructureTestSupport.ok(entity), "replacing the wall forms again");
            place(level, airCell, part(wall), placed);
            h.assertTrue(!StructureTestSupport.ok(entity), "a filled air cell breaks the boiler");
            place(level, airCell, Blocks.AIR.defaultBlockState(), placed);
            h.assertTrue(StructureTestSupport.ok(entity), "clearing the air cell forms again");

            List<BlockPos> cellsList = new ArrayList<>(shell.keySet());
            Random random = new Random(54_000_003L);
            System.out.println("[structure-incremental] fuzz family=boiler seed=54000003 steps=200 cells=" + cellsList.size());
            for (int step = 0; step < 200; step++) {
                BlockPos pos = cellsList.get(random.nextInt(cellsList.size()));
                int roll = random.nextInt(10);
                if (roll < 5) place(level, pos, shell.get(pos), placed);
                else if (roll < 8) place(level, pos, Blocks.AIR.defaultBlockState(), placed);
                else place(level, pos, Blocks.COBBLESTONE.defaultBlockState(), placed);
                StructureScheduler.flush(level);
            }
            for (var entry : shell.entrySet()) place(level, entry.getKey(), entry.getValue(), placed);
            StructureScheduler.flush(level);
            h.assertTrue(StructureTestSupport.ok(entity), "boiler recovers after the fuzz");
        } finally {
            clear(level, placed);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400, batch = "st-fixed-gas-turbine")
    public static void gasTurbineFixedPatternTracksShellEdits(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = new BlockPos(29_200_000, 80, 29_200_000);
        List<BlockPos> placed = new ArrayList<>();
        Map<BlockPos, BlockState> shell = new LinkedHashMap<>();
        try {
            Block controller = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "magnalium_gas_turbine_main_housing"));
            h.assertTrue(controller != Blocks.AIR, "registered gas turbine controller exists on this loader");
            List<BlockPos> cells = new ArrayList<>();
            for (var cell : TurbineStructure.LAYOUT.cells()) cells.add(cell.at(origin, Direction.NORTH));
            for (BlockPos pos : cells) level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
            put(level, origin, controller.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH), placed);
            BlockEntity entity = level.getBlockEntity(origin);
            h.assertTrue(entity instanceof LargeGasTurbineControllerBlockEntity, "gas turbine executor");
            BlockState wall = part(((LargeGasTurbineControllerBlockEntity) entity).grade().wallId());
            for (var cell : TurbineStructure.LAYOUT.cells()) {
                BlockPos pos = cell.at(origin, Direction.NORTH);
                place(level, pos, wall, placed);
                shell.put(pos.immutable(), wall);
            }
            h.assertTrue(StructureTestSupport.ok(entity), "gas turbine shell starts formed");

            BlockPos wallCell = shell.keySet().iterator().next();
            place(level, wallCell, Blocks.AIR.defaultBlockState(), placed);
            h.assertTrue(!StructureTestSupport.ok(entity), "a missing wall breaks the gas turbine");
            place(level, wallCell, shell.get(wallCell), placed);
            h.assertTrue(StructureTestSupport.ok(entity), "replacing the wall forms again");

            List<BlockPos> cellsList = new ArrayList<>(shell.keySet());
            Random random = new Random(54_000_004L);
            System.out.println("[structure-incremental] fuzz family=gas_turbine seed=54000004 steps=200 cells=" + cellsList.size());
            for (int step = 0; step < 200; step++) {
                BlockPos pos = cellsList.get(random.nextInt(cellsList.size()));
                int roll = random.nextInt(10);
                if (roll < 5) place(level, pos, shell.get(pos), placed);
                else if (roll < 8) place(level, pos, Blocks.AIR.defaultBlockState(), placed);
                else place(level, pos, Blocks.COBBLESTONE.defaultBlockState(), placed);
                StructureScheduler.flush(level);
            }
            for (var entry : shell.entrySet()) place(level, entry.getKey(), entry.getValue(), placed);
            StructureScheduler.flush(level);
            h.assertTrue(StructureTestSupport.ok(entity), "gas turbine recovers after the fuzz");

            // Six-axis facing: rebuild the same layout against the vertical front.
            for (BlockPos pos : shell.keySet()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
            shell.clear();
            Direction up = Direction.UP;
            level.setBlock(origin, level.getBlockState(origin).setValue(DirectionalBlock.FACING, up), FLAGS);
            for (var cell : TurbineStructure.LAYOUT.cells()) {
                BlockPos pos = com.gregtech.gregtech.content.multiblock.AxialStructureTransform.at(
                        origin, up, cell.right(), cell.up(), cell.back());
                place(level, pos, wall, placed);
                shell.put(pos.immutable(), wall);
            }
            StructureScheduler.flush(level);
            h.assertTrue(StructureTestSupport.ok(entity), "gas turbine forms against a vertical front");
        } finally {
            clear(level, placed);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400, batch = "st-fixed-axial")
    public static void axialGeneratorFixedPatternTracksShellAndVerticalFacing(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = new BlockPos(29_400_000, 80, 29_400_000);
        List<BlockPos> placed = new ArrayList<>();
        Map<BlockPos, BlockState> shell = new LinkedHashMap<>();
        try {
            Block controller = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "magnalium_steam_turbine_main_housing"));
            h.assertTrue(controller != Blocks.AIR, "registered axial generator exists on this loader");
            put(level, origin, controller.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH), placed);
            BlockEntity entity = level.getBlockEntity(origin);
            h.assertTrue(entity instanceof LargeTurbineControllerBlockEntity, "axial generator executor");
            var grade = ((LargeTurbineControllerBlockEntity) entity).grade();
            shell.clear();
            for (var cell : grade.cells().keySet()) {
                BlockPos pos = com.gregtech.gregtech.content.multiblock.AxialStructureTransform.at(
                        origin, Direction.NORTH, cell.getX(), cell.getY(), cell.getZ());
                BlockState state = axialCell(grade, cell);
                place(level, pos, state, placed);
                shell.put(pos.immutable(), state);
            }
            h.assertTrue(StructureTestSupport.ok(entity), "axial generator shell starts formed");

            BlockPos wallCell = shell.keySet().iterator().next();
            place(level, wallCell, Blocks.AIR.defaultBlockState(), placed);
            h.assertTrue(!StructureTestSupport.ok(entity), "a missing wall breaks the axial generator");
            place(level, wallCell, shell.get(wallCell), placed);
            h.assertTrue(StructureTestSupport.ok(entity), "replacing the wall forms again");

            List<BlockPos> cellsList = new ArrayList<>(shell.keySet());
            Random random = new Random(54_000_005L);
            System.out.println("[structure-incremental] fuzz family=axial seed=54000005 steps=200 cells=" + cellsList.size());
            for (int step = 0; step < 200; step++) {
                BlockPos pos = cellsList.get(random.nextInt(cellsList.size()));
                int roll = random.nextInt(10);
                if (roll < 5) place(level, pos, shell.get(pos), placed);
                else if (roll < 8) place(level, pos, Blocks.AIR.defaultBlockState(), placed);
                else place(level, pos, Blocks.COBBLESTONE.defaultBlockState(), placed);
                StructureScheduler.flush(level);
            }
            for (var entry : shell.entrySet()) place(level, entry.getKey(), entry.getValue(), placed);
            StructureScheduler.flush(level);
            h.assertTrue(StructureTestSupport.ok(entity), "axial generator recovers after the fuzz");

            for (BlockPos pos : shell.keySet()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
            shell.clear();
            level.setBlock(origin, level.getBlockState(origin).setValue(DirectionalBlock.FACING, Direction.UP), FLAGS);
            for (var cell : grade.cells().keySet()) {
                BlockPos pos = com.gregtech.gregtech.content.multiblock.AxialStructureTransform.at(
                        origin, Direction.UP, cell.getX(), cell.getY(), cell.getZ());
                BlockState state = axialCell(grade, cell);
                place(level, pos, state, placed);
                shell.put(pos.immutable(), state);
            }
            StructureScheduler.flush(level);
            h.assertTrue(StructureTestSupport.ok(entity), "axial generator forms against a vertical front");
        } finally {
            clear(level, placed);
        }
        h.succeed();
    }

    private static BlockState axialCell(com.gregtech.gregtech.content.multiblock.AxialGeneratorDefinitions.Grade grade,
                                        BlockPos cell) {
        boolean coil = com.gregtech.gregtech.content.multiblock.OriginalGeneratorParameters.usesCopperCoil(
                grade.steam(), cell.getZ());
        return part(coil ? 18040 : grade.materials().wallId());
    }

    @GameTest(template = "test_empty", timeoutTicks = 200, batch = "st-fixed-fuel-turbine")
    public static void fuelTurbineFixedPatternFollowsItsLayout(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = new BlockPos(29_600_000, 80, 29_600_000);
        List<BlockPos> placed = new ArrayList<>();
        Map<BlockPos, BlockState> shell = new LinkedHashMap<>();
        try {
            Block controller = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("addon_sample", "sample_turbine"));
            h.assertTrue(controller != Blocks.AIR, "sample turbine exists with the addon sample");
            put(level, origin, controller.defaultBlockState(), placed);
            BlockEntity entity = level.getBlockEntity(origin);
            h.assertTrue(entity instanceof FuelTurbineBlockEntity, "fuel turbine executor");
            var layout = LargeMachineLayouts.cells("sample_turbine");
            h.assertTrue(layout != null && !layout.isEmpty(), "sample turbine layout");
            for (var cell : layout) {
                BlockPos pos = cell.at(origin, Direction.NORTH);
                BlockState state = cell.block().defaultBlockState();
                place(level, pos, state, placed);
                shell.put(pos.immutable(), state);
            }
            h.assertTrue(StructureTestSupport.ok(entity), "fuel turbine starts formed");
            h.assertTrue(((FuelTurbineBlockEntity) entity).isFormed(), "formed bit mirrors the tracker");

            BlockPos wall = shell.keySet().iterator().next();
            place(level, wall, Blocks.AIR.defaultBlockState(), placed);
            h.assertTrue(!StructureTestSupport.ok(entity), "a missing wall breaks the fuel turbine");
            h.assertTrue(!((FuelTurbineBlockEntity) entity).isFormed(), "formed bit falls with the tracker");
            place(level, wall, shell.get(wall), placed);
            h.assertTrue(StructureTestSupport.ok(entity), "replacing the wall forms again");
            h.assertTrue(((FuelTurbineBlockEntity) entity).isFormed(), "formed bit rises with the tracker");

            Random random = new Random(54_000_009L);
            System.out.println("[structure-incremental] fuzz family=fuel_turbine seed=54000009 steps=100 cells=" + shell.size());
            for (int step = 0; step < 100; step++) {
                BlockPos pos = wall;
                int roll = random.nextInt(10);
                if (roll < 5) place(level, pos, shell.get(pos), placed);
                else if (roll < 8) place(level, pos, Blocks.AIR.defaultBlockState(), placed);
                else place(level, pos, Blocks.COBBLESTONE.defaultBlockState(), placed);
                StructureScheduler.flush(level);
            }
            for (var entry : shell.entrySet()) place(level, entry.getKey(), entry.getValue(), placed);
            StructureScheduler.flush(level);
            h.assertTrue(StructureTestSupport.ok(entity), "fuel turbine recovers after the fuzz");
        } finally {
            clear(level, placed);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400, batch = "st-fixed-exchanger")
    public static void heatExchangerFixedPatternTracksItsShell(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = new BlockPos(29_700_000, 80, 29_700_000);
        List<BlockPos> placed = new ArrayList<>();
        Map<BlockPos, BlockState> shell = new LinkedHashMap<>();
        try {
            Block controller = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "heat_exchanger_main"));
            h.assertTrue(controller != Blocks.AIR, "registered heat exchanger exists on this loader");
            put(level, origin, controller.defaultBlockState(), placed);
            BlockEntity entity = level.getBlockEntity(origin);
            h.assertTrue(entity instanceof LargeHeatExchangerControllerBlockEntity, "heat exchanger executor");
            for (var cell : SharedHeatExchangerStructure.CELLS) {
                BlockPos pos = origin.offset(cell.right(), cell.up(), cell.back());
                BlockState state = part(cell.part());
                place(level, pos, state, placed);
                shell.put(pos.immutable(), state);
            }
            h.assertTrue(StructureTestSupport.ok(entity), "heat exchanger shell starts formed");

            BlockPos wall = shell.keySet().iterator().next();
            place(level, wall, Blocks.AIR.defaultBlockState(), placed);
            h.assertTrue(!StructureTestSupport.ok(entity), "a missing cell breaks the heat exchanger");
            place(level, wall, shell.get(wall), placed);
            h.assertTrue(StructureTestSupport.ok(entity), "replacing the cell forms again");

            List<BlockPos> cellsList = new ArrayList<>(shell.keySet());
            Random random = new Random(54_000_006L);
            System.out.println("[structure-incremental] fuzz family=exchanger seed=54000006 steps=200 cells=" + cellsList.size());
            for (int step = 0; step < 200; step++) {
                BlockPos pos = cellsList.get(random.nextInt(cellsList.size()));
                int roll = random.nextInt(10);
                if (roll < 5) place(level, pos, shell.get(pos), placed);
                else if (roll < 8) place(level, pos, Blocks.AIR.defaultBlockState(), placed);
                else place(level, pos, Blocks.COBBLESTONE.defaultBlockState(), placed);
                StructureScheduler.flush(level);
            }
            for (var entry : shell.entrySet()) place(level, entry.getKey(), entry.getValue(), placed);
            StructureScheduler.flush(level);
            h.assertTrue(StructureTestSupport.ok(entity), "heat exchanger recovers after the fuzz");
        } finally {
            clear(level, placed);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400, batch = "st-fixed-tower")
    public static void vonDaGraaggFixedPatternTracksItsShell(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = new BlockPos(29_800_000, 80, 29_800_000);
        List<BlockPos> placed = new ArrayList<>();
        Map<BlockPos, BlockState> shell = new LinkedHashMap<>();
        try {
            Block controller = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "von_da_graagg_generator"));
            h.assertTrue(controller != Blocks.AIR, "registered Von Da Graagg tower exists on this loader");
            put(level, origin, controller.defaultBlockState(), placed);
            BlockEntity entity = level.getBlockEntity(origin);
            h.assertTrue(entity instanceof VonDaGraaggControllerBlockEntity, "Von Da Graagg executor");
            for (var cell : VonDaGraaggRules.layout()) {
                BlockPos pos = origin.offset(cell.x(), cell.y(), cell.z());
                BlockState state = part(cell.partId());
                place(level, pos, state, placed);
                shell.put(pos.immutable(), state);
            }
            h.assertTrue(StructureTestSupport.ok(entity), "Von Da Graagg shell starts formed");

            BlockPos wall = shell.keySet().iterator().next();
            place(level, wall, Blocks.AIR.defaultBlockState(), placed);
            h.assertTrue(!StructureTestSupport.ok(entity), "a missing cell breaks the tower");
            place(level, wall, shell.get(wall), placed);
            h.assertTrue(StructureTestSupport.ok(entity), "replacing the cell forms again");

            List<BlockPos> cellsList = new ArrayList<>(shell.keySet());
            Random random = new Random(54_000_007L);
            System.out.println("[structure-incremental] fuzz family=tower seed=54000007 steps=200 cells=" + cellsList.size());
            for (int step = 0; step < 200; step++) {
                BlockPos pos = cellsList.get(random.nextInt(cellsList.size()));
                int roll = random.nextInt(10);
                if (roll < 5) place(level, pos, shell.get(pos), placed);
                else if (roll < 8) place(level, pos, Blocks.AIR.defaultBlockState(), placed);
                else place(level, pos, Blocks.COBBLESTONE.defaultBlockState(), placed);
                StructureScheduler.flush(level);
            }
            for (var entry : shell.entrySet()) place(level, entry.getKey(), entry.getValue(), placed);
            StructureScheduler.flush(level);
            h.assertTrue(StructureTestSupport.ok(entity), "Von Da Graagg recovers after the fuzz");
        } finally {
            clear(level, placed);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400, batch = "st-fixed-crucible")
    public static void crucibleFixedPatternTracksShellAirAndVisual(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = new BlockPos(29_900_000, 80, 29_900_000);
        List<BlockPos> placed = new ArrayList<>();
        Map<BlockPos, BlockState> shell = new LinkedHashMap<>();
        BlockPos airCell = null;
        try {
            Block controller = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "large_stainless_steel_crucible"));
            h.assertTrue(controller != Blocks.AIR, "registered large crucible exists on this loader");
            put(level, origin, controller.defaultBlockState(), placed);
            BlockEntity entity = level.getBlockEntity(origin);
            h.assertTrue(entity instanceof LargeCrucibleControllerBlockEntity, "large crucible executor");
            int wall = ((LargeCrucibleControllerBlockEntity) entity).variant().wallId();
            for (var cell : SharedLargeCrucibleStructure.CELLS) {
                BlockPos pos = origin.offset(cell.x(), cell.y(), cell.z());
                if (cell.air()) {
                    airCell = pos;
                    place(level, pos, Blocks.AIR.defaultBlockState(), placed);
                } else {
                    BlockState state = part(wall);
                    place(level, pos, state, placed);
                    shell.put(pos.immutable(), state);
                }
            }
            h.assertTrue(StructureTestSupport.ok(entity), "large crucible starts formed");
            h.assertTrue(((LargeCrucibleControllerBlockEntity) entity).isFormedForRendering(), "controller visual follows the formed bit");

            // Scenario 19: a part-only visual property must not invalidate the structure.
            BlockPos visualPart = shell.keySet().iterator().next();
            BlockState visualState = level.getBlockState(visualPart);
            var visualProperty = com.gregtech.gregtech.block.machine.MultiblockPortBlock.CRUCIBLE_FORMED;
            h.assertTrue(visualState.hasProperty(visualProperty), "crucible part exposes its visual property");
            level.setBlock(visualPart, visualState.setValue(visualProperty, !visualState.getValue(visualProperty)), FLAGS);
            h.assertTrue(!StructureTracking.tracker(entity).dirty(), "a part visual change alone does not dirty the tracker");
            h.assertTrue(StructureTestSupport.ok(entity), "the crucible stays formed after a part visual change");
            level.setBlock(visualPart, visualState, FLAGS);

            BlockPos wallCell = shell.keySet().iterator().next();
            place(level, wallCell, Blocks.AIR.defaultBlockState(), placed);
            h.assertTrue(!StructureTestSupport.ok(entity), "a missing wall breaks the crucible");
            h.assertTrue(!((LargeCrucibleControllerBlockEntity) entity).isFormedForRendering(), "controller visual falls with the formed bit");
            place(level, wallCell, shell.get(wallCell), placed);
            h.assertTrue(StructureTestSupport.ok(entity), "replacing the wall forms again");
            place(level, airCell, part(wall), placed);
            h.assertTrue(!StructureTestSupport.ok(entity), "a filled air cell breaks the crucible");
            place(level, airCell, Blocks.AIR.defaultBlockState(), placed);
            h.assertTrue(StructureTestSupport.ok(entity), "clearing the air cell forms again");

            List<BlockPos> cellsList = new ArrayList<>(shell.keySet());
            Random random = new Random(54_000_008L);
            System.out.println("[structure-incremental] fuzz family=crucible seed=54000008 steps=200 cells=" + cellsList.size());
            for (int step = 0; step < 200; step++) {
                BlockPos pos = cellsList.get(random.nextInt(cellsList.size()));
                int roll = random.nextInt(10);
                if (roll < 5) place(level, pos, shell.get(pos), placed);
                else if (roll < 8) place(level, pos, Blocks.AIR.defaultBlockState(), placed);
                else place(level, pos, Blocks.COBBLESTONE.defaultBlockState(), placed);
                StructureScheduler.flush(level);
            }
            for (var entry : shell.entrySet()) place(level, entry.getKey(), entry.getValue(), placed);
            StructureScheduler.flush(level);
            h.assertTrue(StructureTestSupport.ok(entity), "large crucible recovers after the fuzz");
        } finally {
            clear(level, placed);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400, batch = "st-inc-fuzz")
    public static void fuzzMigratedFamilies(GameTestHelper h) {
        fuzz(h, "behavior", block("addon_sample", "irrigator"), behaviorLayout(),
                new BlockPos(27_600_000, 80, 27_600_000), 53_000_001L);
        fuzz(h, "distillationtower", machineBlock("distillationtower"), null,
                new BlockPos(27_800_000, 80, 27_800_000), 53_000_002L);
        fuzz(h, "cokeoven", machineBlock("cokeoven"), null,
                new BlockPos(28_000_000, 80, 28_000_000), 53_000_003L);
        fuzz(h, "largemassfab", machineBlock("largemassfab"), null,
                new BlockPos(28_200_000, 80, 28_200_000), 53_000_004L);
        h.succeed();
    }

    private static void fuzz(GameTestHelper h, String label, Block controller, LargeMachineLayouts.Layout override,
                             BlockPos origin, long seed) {
        ServerLevel level = h.getLevel();
        List<BlockPos> placed = new ArrayList<>();
        Map<String, LargeMachineLayouts.Layout> layouts = override == null ? Map.of() : Map.of("irrigator", override);
        withLayouts(layouts, () -> {
            try {
                put(level, origin, facingNorth(controller), placed);
                BlockEntity entity = level.getBlockEntity(origin);
                h.assertTrue(entity instanceof TrackedMultiblock, label + " controller");
                LargeMachineLayouts.Layout layout = override != null ? override
                        : ((com.gregtech.gregtech.blockentity.machine.LargeRecipeMachineBlockEntity) entity).trackedLayout();
                h.assertTrue(layout != null, label + " has a layout");
                List<Placement> placements = placeLayout(level, origin, Direction.NORTH, layout, placed);
                h.assertTrue(StructureTestSupport.ok(entity), label + " starts formed");

                Random random = new Random(seed);
                System.out.println("[structure-incremental] fuzz family=" + label + " seed=" + seed
                        + " steps=" + FUZZ_STEPS);
                Direction front = Direction.NORTH;
                for (int step = 0; step < FUZZ_STEPS; step++) {
                    if (step > 0 && step % 50 == 0) {
                        for (Placement placement : placements)
                            level.setBlock(placement.pos(), Blocks.AIR.defaultBlockState(), FLAGS);
                        front = front.getClockWise();
                        level.setBlock(origin, level.getBlockState(origin).setValue(
                                HorizontalDirectionalBlock.FACING, front), FLAGS);
                        placements = placeLayout(level, origin, front, layout, placed);
                        StructureScheduler.flush(level);
                        continue;
                    }
                    int roll = random.nextInt(10);
                    Placement placement = placements.get(random.nextInt(placements.size()));
                    if (roll < 5) {
                        place(level, placement.pos(), placement.good(), placed);
                    } else if (roll < 8) {
                        place(level, placement.pos(), Blocks.AIR.defaultBlockState(), placed);
                    } else {
                        place(level, placement.pos(), Blocks.COBBLESTONE.defaultBlockState(), placed);
                    }
                    if (step > 0 && step % 97 == 0) {
                        ChunkPos chunk = new ChunkPos(placement.pos());
                        StructureScheduler.chunkUnloadedForTests(level, chunk);
                        StructureScheduler.chunkLoadedForTests(level, chunk);
                    }
                    StructureScheduler.flush(level);
                }
                for (Placement placement : placements)
                    level.setBlock(placement.pos(), placement.good(), FLAGS);
                StructureScheduler.flush(level);
                h.assertTrue(StructureTestSupport.ok(entity), label + " recovers after the fuzz");
            } finally {
                clear(level, placed);
            }
        });
    }

    private static List<Placement> placeLayout(ServerLevel level, BlockPos origin, Direction front,
                                               LargeMachineLayouts.Layout layout, List<BlockPos> placed) {
        List<Placement> out = new ArrayList<>();
        for (var cell : layout.cells()) out.add(placeCell(level, cell.at(origin, front), cell, placed));
        for (var repeat : layout.repeats()) {
            int copies = Math.max(1, repeat.min());
            for (int copy = 0; copy < copies; copy++) {
                for (var cell : repeat.cells())
                    out.add(placeCell(level, cell.at(origin, front, repeat.absoluteUp(cell, copy)), cell, placed));
            }
        }
        return out;
    }

    private static Placement placeCell(ServerLevel level, BlockPos pos, LargeMachineLayouts.Cell cell,
                                       List<BlockPos> placed) {
        BlockState state = cell.role() == MultiblockLayout.Role.AIR
                ? Blocks.AIR.defaultBlockState() : cell.block().defaultBlockState();
        place(level, pos, state, placed);
        return new Placement(pos.immutable(), state);
    }

    private record Placement(BlockPos pos, BlockState good) {}

    private static LargeMachineLayouts.Layout behaviorLayout() {
        var fixed = new LargeMachineLayouts.Cell(1, 0, 0, 18100, MultiblockLayout.Role.ITEM_FLUID_INPUT);
        var layer = new LargeMachineLayouts.Cell(2, 0, 0, 18100, MultiblockLayout.Role.ITEM_FLUID_INPUT);
        var repeat = new LargeMachineLayouts.Repeat("layers", 1, 1, 6, 1, 1, List.of(layer));
        return new LargeMachineLayouts.Layout(List.of(fixed), List.of(repeat));
    }

    private static LargeMachineLayouts.Cell plain(int right, int up, int back, String plain) {
        return new LargeMachineLayouts.Cell(right, up, back, 0, MultiblockLayout.Role.CASING, plain);
    }

    private static LargeMachineLayouts.Cell plain(int right, int up, int back, String plain, String alternative) {
        return new LargeMachineLayouts.Cell(right, up, back, 0, MultiblockLayout.Role.CASING, plain, -1,
                List.of(new SharedLargeMachineLayouts.Choice(0, alternative)));
    }

    private static TrackedMultiblock dummy() {
        return new TrackedMultiblock() {
            @Override public boolean scanStructure() { return false; }
            @Override public StructureDefinition structureDefinition() { return null; }
        };
    }

    private static Block machineBlock(String machineName) {
        for (Block block : BuiltInRegistries.BLOCK)
            if (block instanceof BasicMachineBlock basic && machineName.equals(basic.basicSpec().machineName()))
                return block;
        throw new AssertionError("Missing machine block " + machineName);
    }

    private static Block block(String namespace, String path) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
        if (block == null || block == Blocks.AIR) throw new AssertionError("Missing block " + namespace + ":" + path);
        return block;
    }

    private static BlockState facingNorth(Block block) {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(HorizontalDirectionalBlock.FACING))
            return state.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        return state;
    }

    private static BlockState part(int id) {
        return com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(id).defaultBlockState();
    }

    private static void put(ServerLevel level, BlockPos pos, BlockState state, List<BlockPos> placed) {
        level.setBlock(pos, state, FLAGS);
        placed.add(pos.immutable());
    }

    private static void place(ServerLevel level, BlockPos pos, Block block, List<BlockPos> placed) {
        put(level, pos, block.defaultBlockState(), placed);
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state, List<BlockPos> placed) {
        put(level, pos, state, placed);
    }

    private static void clear(ServerLevel level, List<BlockPos> placed) {
        for (int index = placed.size() - 1; index >= 0; index--) {
            BlockPos pos = placed.get(index);
            if (level.hasChunkAt(pos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
        }
        placed.clear();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, LargeMachineLayouts.Layout> layouts() {
        try {
            Field field = LargeMachineLayouts.class.getDeclaredField("LAYOUTS");
            field.setAccessible(true);
            return (Map<String, LargeMachineLayouts.Layout>) field.get(null);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Block> resolved() {
        try {
            Field field = PlainStructureBlocks.class.getDeclaredField("RESOLVED");
            field.setAccessible(true);
            return (Map<String, Block>) field.get(null);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static void withLayouts(Map<String, LargeMachineLayouts.Layout> replacements, Runnable body) {
        Map<String, LargeMachineLayouts.Layout> layouts = layouts();
        Map<String, LargeMachineLayouts.Layout> previous = new LinkedHashMap<>();
        for (var entry : replacements.entrySet()) previous.put(entry.getKey(), layouts.put(entry.getKey(), entry.getValue()));
        try {
            body.run();
        } finally {
            for (var entry : previous.entrySet()) {
                if (entry.getValue() == null) layouts.remove(entry.getKey());
                else layouts.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
