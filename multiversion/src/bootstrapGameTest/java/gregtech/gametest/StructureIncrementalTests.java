package gregtech.gametest;

import gregtech.internal.multiblock.MultiblockLayout;
import gregtech.internal.multiblock.tracking.Aggregate;
import gregtech.internal.multiblock.tracking.CompiledPattern;
import gregtech.internal.multiblock.tracking.PatternState;
import gregtech.internal.multiblock.tracking.StructureDefinition;
import gregtech.internal.multiblock.tracking.StructureScanProbe;
import gregtech.internal.multiblock.tracking.StructureScheduler;
import gregtech.internal.multiblock.tracking.StructureTracking;
import gregtech.internal.multiblock.tracking.TrackedMultiblock;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.multiblock.LargeMachineLayouts;
import gregtech.content.multiblock.PlainStructureBlocks;
import gregtech.content.multiblock.SharedLargeMachineLayouts;
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
                        : ((gregtech.content.blockentity.machine.LargeRecipeMachineBlockEntity) entity).trackedLayout();
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
        return gregtech.content.multiblock.LargeMachineParts.block(id).defaultBlockState();
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
