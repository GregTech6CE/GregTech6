package gregtech.gametest;

import gregtech.internal.multiblock.MultiblockLayout;
import gregtech.internal.multiblock.tracking.*;
import gregtech.content.block.OreBlock;
import gregtech.content.block.OreHostStone;
import gregtech.content.block.machine.*;
import gregtech.content.blockentity.machine.*;
import gregtech.content.logistics.LogisticsCoreStructure;
import gregtech.content.multiblock.*;
import gregtech.jei.MultiblockInfoData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Independent old geometry plus consistency/projection audits for card 55. */
@GameTestHolder("gt6ce_structure_incremental")
@PrefixGameTestTemplate(false)
public final class StructureSpecialFamiliesTests {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;
        final BlockPos origin;
        final LinkedHashMap<BlockPos, BlockState> good = new LinkedHashMap<>();
        final List<BlockPos> touched = new ArrayList<>();
        final net.minecraft.world.level.block.entity.BlockEntity entity;

        Fixture(GameTestHelper h, int index, Block block, Direction facing) {
            this.h = h;
            origin = new BlockPos(26_000_000 + index * 1000, 100, 26_000_000);
            var state = block.defaultBlockState();
            if (state.hasProperty(BlockStateProperties.FACING)) state = state.setValue(BlockStateProperties.FACING, facing);
            else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            put(origin, state);
            entity = h.getLevel().getBlockEntity(origin);
            h.assertTrue(entity instanceof TrackedMultiblock, "registered tracked controller");
        }
        void put(BlockPos pos, BlockState state) {
            touched.add(pos.immutable());
            h.getLevel().setBlock(pos, state, FLAGS);
        }
        void cell(BlockPos pos, BlockState state) { good.put(pos.immutable(), state); put(pos, state); }
        void relative(Map<BlockPos, Block> geometry) {
            geometry.forEach((offset, block) -> cell(origin.offset(offset), block.defaultBlockState()));
        }
        boolean ok() { return StructureTestSupport.ok(entity); }
        TrackedMultiblock machine() { return (TrackedMultiblock) entity; }
        void formed(String message) {
            h.assertTrue(ok(), message);
            h.assertTrue(machine().structureDefinition() instanceof StructureDefinition.Declarative d && d.incremental(),
                    "family uses the incremental path");
            StructureScanProbe.assertInside(entity, machine().structureDefinition().declaredFootprint(machine()));
        }
        void incrementalOnly(Runnable assertions) {
            StructureTracking.consistencyCheck(false);
            try { assertions.run(); }
            finally { StructureTracking.consistencyCheck(true); }
        }
        void fuzz(long seed) {
            formed("fixture starts formed");
            StructureScanProbe.audit(true);
            try {
                var positions = new ArrayList<>(good.keySet());
                var random = new Random(seed);
                System.out.println("[structure-special] seed=" + seed + " steps=300 cells=" + positions.size());
                for (int step = 0; step < 300; step++) {
                    BlockPos pos = positions.get(random.nextInt(positions.size()));
                    put(pos, switch (random.nextInt(4)) {
                        case 0 -> Blocks.AIR.defaultBlockState();
                        case 1 -> Blocks.COBBLESTONE.defaultBlockState();
                        default -> good.get(pos);
                    });
                    ok();
                }
                good.forEach(this::put);
                formed("restored after deterministic fuzz");
            } finally { StructureScanProbe.audit(false); }
        }
        @Override public void close() {
            StructureScanProbe.audit(false);
            // Remove the controller before its members to avoid teardown scans and item drops.
            h.getLevel().setBlock(origin, Blocks.AIR.defaultBlockState(), FLAGS);
            for (var pos : touched) h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
        }
    }

    private static Block find(java.util.function.Predicate<Block> predicate) {
        return BuiltInRegistries.BLOCK.stream().filter(predicate).findFirst().orElseThrow();
    }
    private static BlockState part(int id) { return LargeMachineParts.block(id).defaultBlockState(); }
    private static Block tankBlock(int size) {
        return find(b -> b instanceof TankControllerBlock tank && tank.size() == size);
    }
    private static void buildTank(Fixture f, int size, Direction facing) {
        var machine = (MultiblockTankControllerBlockEntity) f.entity;
        Block wall = machine.valveSpec() == null ? (size == 3
                ? gregtech.content.registry.GTMultiblocks.TANK_WALL.get()
                : gregtech.content.registry.GTMultiblocks.TANK_WALL_DENSE.get())
                : LargeMachineParts.block(machine.valveSpec().wallId());
        int r = size / 2;
        BlockPos centre = f.origin.relative(facing.getOpposite(), r);
        for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
            BlockPos pos = centre.offset(x, y, z);
            if (pos.equals(f.origin)) continue;
            boolean shell = Math.abs(x) == r || Math.abs(y) == r || Math.abs(z) == r;
            f.cell(pos, (shell ? wall : Blocks.AIR).defaultBlockState());
        }
    }
    private static BlockPos tankCentre(Fixture f, int size, Direction facing) {
        return f.origin.relative(facing.getOpposite(), size / 2);
    }

    @GameTest(template = "test_empty", timeoutTicks = 400, batch = "st-special-tank")
    public static void tanksBothSizesAndSixFacings(GameTestHelper h) {
        int index = 0;
        for (int size : new int[]{3, 5}) for (Direction facing : Direction.values()) {
            try (var f = new Fixture(h, index++, tankBlock(size), facing)) {
                buildTank(f, size, facing);
                BlockPos centre = tankCentre(f, size, facing);
                f.formed("tank forms size=" + size + " facing=" + facing);
                f.put(centre, Blocks.WATER.defaultBlockState());
                h.assertTrue(!f.ok(), "world fluid is not an empty tank interior");
                f.put(centre, Blocks.AIR.defaultBlockState());
                f.formed("empty interior restores formation");
                if (facing == Direction.NORTH) f.fuzz(55_000_001L + size);
            }
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400, batch = "st-special-lightning")
    public static void lightningRodFrontierAndHeight(GameTestHelper h) {
        try (var f = new Fixture(h, 20, find(b -> b instanceof OriginalLightningRodControllerBlock), Direction.NORTH)) {
            f.relative(OriginalHighTechControllerLayouts.lightningRod());
            f.formed("lightning base and one rod form");
            BlockPos next = f.origin.above(6);
            h.assertTrue(StructureTracking.tracker(f.entity).watch().contains(next), "next rod segment watched");
            for (int n = 1; n < 12; n++) f.cell(f.origin.above(5 + n), part(18104));
            f.incrementalOnly(() -> {
                f.formed("rod grows through frontier without the full-scan oracle");
                try {
                    var length = LightningRodControllerBlockEntity.class.getDeclaredField("rodLength");
                    length.setAccessible(true);
                    h.assertTrue(length.getInt(f.entity) == 12, "incremental apply updates the live capture length");
                } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            });
            h.assertTrue(StructureTracking.tracker(f.entity).matchedLayers()[0] == 12, "rod count reaches twelve");
            BlockPos cut = f.origin.above(8);
            f.put(cut, Blocks.AIR.defaultBlockState());
            f.formed("shorter rod remains formed");
            h.assertTrue(StructureTracking.tracker(f.entity).matchedLayers()[0] == 3, "prefix stops at cut");
            h.assertTrue(!StructureTracking.tracker(f.entity).watch().contains(f.origin.above(10)), "watch shrinks");
            f.put(cut, part(18104));
            f.formed("rod repair rediscovers unwatched tail");
            h.assertTrue(StructureTracking.tracker(f.entity).matchedLayers()[0] == 12, "tail restored");
            f.fuzz(55_000_002L);
            for (int n = 12; f.origin.getY() + 5 + n < h.getLevel().getMaxBuildHeight(); n++)
                f.cell(f.origin.above(5 + n), part(18104));
            f.formed("rod can reach build limit");
            h.assertTrue(StructureTracking.tracker(f.entity).matchedLayers()[0]
                    == h.getLevel().getMaxBuildHeight() - f.origin.getY() - 5, "height cap matches full scan");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400, batch = "st-special-diagnostic")
    public static void diagnosticListsEveryWrongCellFromTheDefinition(GameTestHelper h) {
        try (var f = new Fixture(h, 40, tankBlock(3), Direction.NORTH)) {
            buildTank(f, 3, Direction.NORTH);
            f.formed("tank forms");
            h.assertTrue(StructureDiagnostic.inspect(f.machine()).isEmpty(), "formed tank has no problems");
            BlockPos broken = tankCentre(f, 3, Direction.NORTH).offset(1, 1, 0);
            f.put(broken, Blocks.AIR.defaultBlockState());
            var problems = StructureDiagnostic.inspect(f.machine());
            h.assertTrue(problems.size() == 1 && problems.get(0).pos().equals(broken),
                    "tank diagnostic lists the broken shell cell: " + problems);
            h.assertTrue(!f.ok(), "the same cell unforms the tank");
            var tank = (TankControllerBlock) f.entity.getBlockState().getBlock();
            f.put(broken, (tank.valveSpec() == null
                    ? gregtech.content.registry.GTMultiblocks.TANK_WALL.get()
                    : LargeMachineParts.block(tank.valveSpec().wallId())).defaultBlockState());
            f.formed("tank repair restores the definition");
            h.assertTrue(StructureDiagnostic.inspect(f.machine()).isEmpty(), "repaired tank has no problems");
        }
        try (var f = new Fixture(h, 41, find(b -> b instanceof OriginalLightningRodControllerBlock), Direction.NORTH)) {
            f.relative(OriginalHighTechControllerLayouts.lightningRod());
            f.formed("lightning rod forms");
            BlockPos base = f.origin.offset(1, 0, 0);
            f.put(base, Blocks.AIR.defaultBlockState());
            var problems = StructureDiagnostic.inspect(f.machine());
            h.assertTrue(problems.stream().anyMatch(problem -> problem.pos().equals(base)),
                    "lightning diagnostic lists the broken base cell: " + problems);
            f.put(base, part(18004));
            BlockPos rod = f.origin.above(5);
            f.put(rod, Blocks.AIR.defaultBlockState());
            h.assertTrue(StructureDiagnostic.inspect(f.machine()).isEmpty(),
                    "an optional rod segment above the minimum is not an error");
            f.formed("a zero-length rod still matches the definition");
            f.put(rod, part(18104));
            f.formed("rod repair restores the definition");
            h.assertTrue(StructureDiagnostic.inspect(f.machine()).isEmpty(), "repaired rod has no problems");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", batch = "st-special-preview")
    public static void previewAndJeiDrawTheDefinitionCells(GameTestHelper h) {
        var tankController = find(b -> b instanceof TankControllerBlock tank && tank.size() == 3);
        var tankInfo = infoFor(tankController);
        var tank = (TankControllerBlock) tankController;
        Block tankWall = tank.valveSpec() == null ? gregtech.content.registry.GTMultiblocks.TANK_WALL.get()
                : LargeMachineParts.block(tank.valveSpec().wallId());
        previewMatches(h, tankInfo, SpecialStructures.tankCells(tank.size(), tankWall, Direction.NORTH), "tank");

        var exchanger = find(b -> b instanceof LargeHeatExchangerControllerBlock);
        var exchangerCells = new ArrayList<CompiledPattern.FixedCell>();
        for (var cell : SharedHeatExchangerStructure.CELLS)
            exchangerCells.add(new CompiledPattern.FixedCell(cell.right(), cell.up(), cell.back(),
                    new LargeMachineLayouts.Cell(cell.right(), cell.up(), cell.back(), cell.part(),
                            MultiblockLayout.Role.valueOf(cell.role().name()))));
        previewMatches(h, infoFor(exchanger), exchangerCells, "exchanger");

        var rod = find(b -> b instanceof OriginalLightningRodControllerBlock);
        var rodInfo = infoFor(rod);
        var rodLayout = SpecialStructures.lightningLayout(true);
        var rodCells = new ArrayList<CompiledPattern.FixedCell>();
        for (var cell : rodLayout.cells()) {
            BlockPos offset = cell.at(BlockPos.ZERO, Direction.NORTH);
            rodCells.add(new CompiledPattern.FixedCell(offset.getX(), offset.getY(), offset.getZ(),
                    new LargeMachineLayouts.Cell(cell.right(), cell.up(), cell.back(), cell.part(), cell.role(),
                            cell.plain(), cell.design(), cell.alternatives())));
        }
        rodCells.add(new CompiledPattern.FixedCell(0, 5, 0, new LargeMachineLayouts.Cell(0, 5, 0, 18104,
                MultiblockLayout.Role.CASING)));
        previewMatches(h, rodInfo, rodCells, "lightning");
        var rodPreview = MultiblockInfoData.previewBlocks(rodInfo);
        h.assertTrue(rodPreview.get(new BlockPos(0, 5, 0)).is(LargeMachineParts.block(18104).asItem()),
                "the preview shows the same rod segment the definition repeats");

        var crusher = BuiltInRegistries.BLOCK.stream()
                .filter(b -> b instanceof BasicMachineBlock machine && machine.basicSpec().machineName().equals("largecrusher"))
                .findFirst().orElseThrow();
        var crusherCells = new ArrayList<CompiledPattern.FixedCell>();
        for (var cell : LargeMachineLayouts.cells("largecrusher")) {
            BlockPos offset = cell.at(BlockPos.ZERO, Direction.NORTH);
            crusherCells.add(new CompiledPattern.FixedCell(offset.getX(), offset.getY(), offset.getZ(), cell));
        }
        previewMatches(h, infoFor(crusher), crusherCells, "large-recipe layout");
        h.succeed();
    }

    private static MultiblockInfoData.Info infoFor(Block block) {
        return MultiblockInfoData.recipes().stream()
                .filter(info -> info.controller().getItem() instanceof net.minecraft.world.item.BlockItem item
                        && item.getBlock() == block)
                .findFirst().orElseThrow(() -> new AssertionError("no preview entry for " + block));
    }

    private static void previewMatches(GameTestHelper h, MultiblockInfoData.Info info,
                                       List<CompiledPattern.FixedCell> cells, String label) {
        var preview = MultiblockInfoData.previewBlocks(info);
        var expected = new LinkedHashMap<BlockPos, Block>();
        for (var cell : cells) {
            Block block = SpecialStructures.previewBlock(cell.cell());
            if (block != Blocks.AIR) expected.put(new BlockPos(cell.dx(), cell.dy(), cell.dz()), block);
        }
        for (var entry : expected.entrySet()) {
            var stack = preview.get(entry.getKey());
            h.assertTrue(stack != null && stack.getItem() instanceof net.minecraft.world.item.BlockItem item
                            && item.getBlock() == entry.getValue(),
                    label + " preview matches the definition at " + entry.getKey() + ": " + stack
                            + " vs " + entry.getValue());
        }
        h.assertTrue(preview.size() == expected.size() + 1,
                label + " preview shows the definition cells plus the controller: " + preview.size()
                        + " vs " + (expected.size() + 1));
    }

    @GameTest(template = "test_empty", timeoutTicks = 400, batch = "st-special-logistics")
    public static void logisticsCpuBoundariesAndCounts(GameTestHelper h) {
        try (var f = new Fixture(h, 21, find(b -> b instanceof LogisticsCoreControllerBlock), Direction.UP)) {
            var cpus = new ArrayList<BlockPos>();
            for (var cell : LogisticsCoreStructure.cells()) {
                BlockPos pos = cell.at(f.origin, Direction.UP);
                f.cell(pos, part(cell.kind() == LogisticsCoreStructure.Kind.VENT ? LogisticsCoreStructure.VENT : LogisticsCoreStructure.WALL));
                if (cell.kind() == LogisticsCoreStructure.Kind.CPU) cpus.add(pos);
            }
            h.assertTrue(!f.ok(), "no CPUs cannot form");
            f.cell(cpus.get(0), part(LogisticsCoreStructure.VERSATILE));
            var machine = (LogisticsCoreControllerBlockEntity) f.entity;
            f.incrementalOnly(() -> {
                f.formed("one versatile CPU is exactly enough");
                h.assertTrue(machine.processorCounts().equals(new LogisticsCoreStructure.Counts(1, 1, 1, 1)),
                        "incremental apply supplies live CPU counts without the full-scan oracle");
            });
            f.cell(cpus.get(1), part(LogisticsCoreStructure.VERSATILE));
            f.formed("one extra CPU is accepted");
            h.assertTrue(machine.processorCounts().logic() == 2, "extra CPU counted");
            f.cell(cpus.get(0), part(LogisticsCoreStructure.WALL));
            f.cell(cpus.get(1), part(LogisticsCoreStructure.WALL));
            for (int n = 0; n < 4; n++) f.cell(cpus.get(n), part(LogisticsCoreStructure.LOGIC + n));
            f.formed("four specialized CPUs satisfy all counters");
            h.assertTrue(machine.processorCounts().equals(new LogisticsCoreStructure.Counts(4, 4, 4, 4)), "specialized CPU weighting");
            f.put(cpus.get(3), part(LogisticsCoreStructure.WALL));
            h.assertTrue(!f.ok(), "one missing CPU category fails aggregate");
            f.put(cpus.get(3), part(LogisticsCoreStructure.CONVERSION));
            f.formed("restoring missing category forms");
            h.assertTrue(machine.structureDefinition().claimPolicy() == StructureDefinition.ClaimPolicy.PER_CELL, "policy declared");
            f.fuzz(55_000_003L);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400, batch = "st-special-oven")
    public static void ovenCoilsMixAndRecover(GameTestHelper h) {
        Block block = find(b -> b instanceof BasicMachineBlock basic && basic.basicSpec().machineName().equals("largeoven"));
        try (var f = new Fixture(h, 22, block, Direction.EAST)) {
            var machine = (LargeRecipeMachineBlockEntity) f.entity;
            var coils = new ArrayList<BlockPos>();
            for (var cell : machine.trackedLayout().cells()) {
                BlockPos pos = cell.at(f.origin, Direction.EAST);
                f.cell(pos, cell.role() == gregtech.internal.multiblock.MultiblockLayout.Role.AIR
                        ? Blocks.AIR.defaultBlockState() : cell.block().defaultBlockState());
                if (cell.part() == 18042) coils.add(pos);
            }
            f.formed("oven with first coil type forms");
            h.assertTrue(coils.size() > 1, "multiple coil cells");
            f.put(coils.get(0), part(18043));
            h.assertTrue(!f.ok(), "mixed coil types fail");
            for (var pos : coils) f.cell(pos, part(18043));
            f.formed("all second coil type forms");
            f.put(coils.get(0), part(18042));
            h.assertTrue(!f.ok(), "switching one coil breaks sameness");
            f.put(coils.get(0), part(18043));
            f.formed("switching back restores sameness");
            f.fuzz(55_000_004L);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400, batch = "st-special-drill")
    public static void drillFloorMembershipAndCycleCandidates(GameTestHelper h) {
        try (var f = new Fixture(h, 23, find(b -> b instanceof OriginalBedrockDrillControllerBlock), Direction.NORTH)) {
            f.relative(OriginalHighTechControllerLayouts.bedrockDrill());
            var machine = (BedrockDrillControllerBlockEntity) f.entity;
            f.formed("ordinary bedrock floor forms");
            h.assertTrue(machine.drillingCandidates().isEmpty(), "ordinary bedrock gives no ore candidates");
            OreBlock large = (OreBlock) find(b -> b instanceof OreBlock ore && ore.prefix().getName().equals("ore"));
            OreBlock small = (OreBlock) find(b -> b instanceof OreBlock ore && ore.prefix().getName().equals("oreSmall")
                    && ore.material() != large.material());
            BlockPos first = f.origin.offset(-1, -5, -1), second = f.origin.offset(1, -5, 1);
            StructureScanProbe.audit(true);
            f.cell(first, large.stateFor(OreHostStone.BEDROCK));
            f.cell(second, small.stateFor(OreHostStone.BEDROCK));
            h.assertTrue(!StructureTracking.tracker(machine).dirty() && machine.isStructureOk(), "ore changes preserve formed bit");
            h.assertTrue(machine.drillingCandidates().equals(List.of(large.material(), large.material(), small.material())),
                    "large ore has weight two, small ore one in deterministic floor order");
            f.cell(first, small.stateFor(OreHostStone.BEDROCK));
            h.assertTrue(machine.drillingCandidates().equals(List.of(small.material(), small.material())), "next cycle sees replacement material");
            f.put(first, small.stateFor(OreHostStone.STONE));
            h.assertTrue(!f.ok(), "same ore block with a non-bedrock host breaks structure");
            f.put(first, small.stateFor(OreHostStone.BEDROCK));
            f.formed("bedrock host restores structure");
            f.fuzz(55_000_005L);
        }
        h.succeed();
    }
}
