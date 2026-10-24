package gregtech.gametest;

import gregtech.api.addon.platform.MachineBehaviorContext;
import gregtech.internal.multiblock.MultiblockLayout;
import gregtech.internal.multiblock.tracking.ClientFormedSync;
import gregtech.internal.multiblock.tracking.SectionWatchMask;
import gregtech.internal.multiblock.tracking.StructureChunkWatch;
import gregtech.internal.multiblock.tracking.StructureEpoch;
import gregtech.internal.multiblock.tracking.StructureScanProbe;
import gregtech.internal.multiblock.tracking.StructureScheduler;
import gregtech.internal.multiblock.tracking.StructureTracker;
import gregtech.internal.multiblock.tracking.StructureTracking;
import gregtech.internal.multiblock.tracking.StructureTrackingMode;
import gregtech.internal.multiblock.tracking.TrackedMultiblock;
import gregtech.block.machine.AddonEnergyPartBlock;
import gregtech.block.machine.AxialGeneratorBlock;
import gregtech.block.machine.BasicMachineBlock;
import gregtech.block.machine.BedrockDrillControllerBlock;
import gregtech.block.machine.CokeOvenControllerBlock;
import gregtech.block.machine.CryoDistillationControllerBlock;
import gregtech.block.machine.LargeCrucibleControllerBlock;
import gregtech.block.machine.LightningRodControllerBlock;
import gregtech.block.machine.LogisticsCoreControllerBlock;
import gregtech.block.machine.MultiblockControllerBlock;
import gregtech.block.machine.MultiblockPortBlock;
import gregtech.block.machine.OriginalBedrockDrillControllerBlock;
import gregtech.block.machine.OriginalLargeBoilerControllerBlock;
import gregtech.block.machine.OriginalLightningRodControllerBlock;
import gregtech.block.machine.TankControllerBlock;
import gregtech.block.machine.VonDaGraaggControllerBlock;
import gregtech.blockentity.machine.BehaviorMachineBlockEntity;
import gregtech.blockentity.machine.ImplosionCompressorControllerBlockEntity;
import gregtech.blockentity.machine.MultiblockPortBlockEntity;
import gregtech.blockentity.machine.MultiblockTankControllerBlockEntity;
import gregtech.blockentity.machine.OriginalLargeBoilerControllerBlockEntity;
import gregtech.content.logistics.LogisticsCoreStructure;
import gregtech.content.multiblock.ControllerStructureLayouts;
import gregtech.content.multiblock.FusionStructure;
import gregtech.content.multiblock.GasTurbineDefinitions;
import gregtech.content.multiblock.LargeMachineLayouts;
import gregtech.content.multiblock.LargeMachineParts;
import gregtech.content.multiblock.MatterFabricatorStructure;
import gregtech.content.multiblock.PlainStructureBlocks;
import gregtech.content.multiblock.SharedHeatExchangerStructure;
import gregtech.content.multiblock.TurbineStructure;
import gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * Query and scan stay apart. Counters are printed for every family, and the phase-1 contracts that do not
 * need a later increment or claim table are exercised here.
 */
@GameTestHolder("gt6ce_structure_tracking")
@PrefixGameTestTemplate(false)
public final class StructureTrackingTests {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private StructureTrackingTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 400, batch = "st-phase0")
    public static void idleAndPortQueriesScanOnlyInLegacy(GameTestHelper h) {
        StructureScanProbe.enable(true);
        try {
            // A 1x1 template only reserves a few blocks. Fusion reaches about 20 blocks
            // from this origin, which lands inside the next batch and drops items there.
            BlockPos origin = new BlockPos(24_000_000, 80, 24_000_000);
            var seen = new LinkedHashMap<String, Block>();
            for (Block block : ControllerStructureLayouts.all().keySet()) {
                String family = family(block);
                if (family != null) seen.putIfAbsent(family, block);
            }
            h.assertTrue(seen.keySet().containsAll(List.of(
                    "tank", "boiler", "crucible", "graagg", "lightning", "drill", "axial", "large-recipe")),
                    "preview layouts cover the built-in families: " + seen.keySet());
            for (var entry : seen.entrySet()) {
                measure(h, entry.getKey(), origin, (level, at, cells) -> {
                    put(level, at, facingNorth(entry.getValue()), cells);
                    for (var cell : ControllerStructureLayouts.cells(entry.getValue()).entrySet())
                        put(level, at.offset(cell.getKey()), cell.getValue().defaultBlockState(), cells);
                    return level.getBlockEntity(at);
                });
            }
            measure(h, "implosion", origin, StructureTrackingTests::implosion);
            measure(h, "fusion", origin, StructureTrackingTests::fusion);
            measure(h, "gas-turbine", origin, StructureTrackingTests::gasTurbine);
            measure(h, "logistics", origin, StructureTrackingTests::logistics);
            measure(h, "heat-exchanger", origin, StructureTrackingTests::exchanger);
            measure(h, "matter", origin, StructureTrackingTests::matter);
            measure(h, "fuel-turbine", origin, StructureTrackingTests::fuelTurbine);
            withLayouts(Map.of("irrigator", stoneLayout()), () ->
                    measure(h, "behavior", origin, (level, at, cells) -> {
                        var machine = behavior(level, at, "irrigator", cells);
                        put(level, at.east(), Blocks.STONE.defaultBlockState(), cells);
                        return machine;
                    }));
        } finally {
            StructureScanProbe.enable(false);
            StructureScanProbe.reset();
            StructureTrackingMode.overrideForTests(null);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200, batch = "st-break")
    public static void brokenShellUnformsAtEndOfTickAndRepairs(GameTestHelper h) {
        var cells = new ArrayList<BlockPos>();
        try {
            BlockPos origin = h.absolutePos(new BlockPos(4, 4, 4));
            var machine = (ImplosionCompressorControllerBlockEntity) implosion(h.getLevel(), origin, cells);
            h.assertTrue(StructureTestSupport.ok(machine), "implosion forms");
            BlockPos wall = firstSolid(origin, ImplosionCompressorControllerBlockEntity.LAYOUT);
            h.getLevel().setBlock(wall, Blocks.AIR.defaultBlockState(), FLAGS);
            h.assertTrue(!machine.isStructureOk(), "the formed bit falls in the same tick as the break");
            StructureScheduler.flush(h.getLevel());
            h.assertTrue(!machine.isStructureOk(), "end of tick keeps the broken shell unformed");
            h.getLevel().setBlock(wall, block("implosion_compressor_wall").defaultBlockState(), FLAGS);
            h.assertTrue(!machine.isStructureOk(), "a repair is not scanned until the end of the tick");
            StructureScheduler.flush(h.getLevel());
            h.assertTrue(machine.isStructureOk(), "the repaired shell forms at the end of the tick");
        } finally {
            clear(h.getLevel(), cells);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200, batch = "st-fifty")
    public static void fiftyCellEditsEvaluateEachMachineOnce(GameTestHelper h) {
        withLayouts(Map.of("irrigator", stoneLayout()), () -> {
            var cells = new ArrayList<BlockPos>();
            var machines = new ArrayList<BehaviorMachineBlockEntity>();
            try {
                BlockPos origin = new BlockPos(26_000_000, 80, 26_000_000);
                for (int i = 0; i < 50; i++) {
                    BlockPos at = origin.offset((i % 10) * 3, 0, (i / 10) * 3);
                    var machine = behavior(h.getLevel(), at, "irrigator", cells);
                    put(h.getLevel(), at.east(), Blocks.STONE.defaultBlockState(), cells);
                    machines.add(machine);
                    h.assertTrue(StructureTestSupport.ok(machine), "row machine forms");
                }
                StructureScanProbe.enable(true);
                StructureScanProbe.reset();
                for (var machine : machines)
                    h.getLevel().setBlock(machine.getBlockPos().east(), Blocks.AIR.defaultBlockState(), FLAGS);
                StructureScheduler.flush(h.getLevel());
                int scans = StructureScanProbe.scans();
                h.assertTrue(scans >= 50 && scans < 100, "fifty broken machines are evaluated once, scans=" + scans);
                for (var machine : machines) h.assertTrue(!machine.isStructureOk(), "every edited machine is unformed");
            } finally {
                StructureScanProbe.enable(false);
                clear(h.getLevel(), cells);
            }
        });
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200, batch = "st-repeat")
    public static void repeatLayerAddsAndDropsTheMiddle(GameTestHelper h) {
        var layer = new LargeMachineLayouts.Cell(1, 0, 0, 18100, MultiblockLayout.Role.ITEM_FLUID_INPUT);
        var repeat = new LargeMachineLayouts.Repeat("layers", 1, 1, 4, 1, 1, List.of(layer));
        var layout = new LargeMachineLayouts.Layout(List.of(), List.of(repeat));
        withLayouts(Map.of("irrigator", layout), () -> {
            var cells = new ArrayList<BlockPos>();
            try {
                BlockPos at = h.absolutePos(new BlockPos(3, 3, 3));
                var machine = behavior(h.getLevel(), at, "irrigator", cells);
                BlockPos low = at.east().above(1);
                BlockPos middle = at.east().above(2);
                BlockPos high = at.east().above(3);
                put(h.getLevel(), low, part(18100), cells);
                put(h.getLevel(), middle, part(18100), cells);
                h.assertTrue(StructureTestSupport.ok(machine), "two repeat layers form");
                h.assertTrue(repeats(machine, "layers") == 2, "two complete layers are counted");
                h.assertTrue(port(h, low).isBoundTo(at) && port(h, middle).isBoundTo(at), "both layer ports bind");
                put(h.getLevel(), high, part(18100), cells);
                StructureScheduler.flush(h.getLevel());
                h.assertTrue(machine.isStructureOk() && repeats(machine, "layers") == 3, "the added layer counts");
                h.assertTrue(port(h, high).isBoundTo(at), "the new layer port binds");
                h.getLevel().setBlock(middle, Blocks.AIR.defaultBlockState(), FLAGS);
                StructureScheduler.flush(h.getLevel());
                h.assertTrue(machine.isStructureOk() && repeats(machine, "layers") == 1, "a hole stops the repeat at the first layer");
                h.assertTrue(port(h, low).isBoundTo(at), "the remaining layer stays bound");
                h.assertTrue(h.getLevel().getBlockState(middle).isAir() && !port(h, high).isBoundTo(at),
                        "the upper port is released when the middle layer is gone");
            } finally {
                clear(h.getLevel(), cells);
            }
        });
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200, batch = "st-share")
    public static void removingOneControllerLetsTheOtherForm(GameTestHelper h) {
        var east = new LargeMachineLayouts.Layout(List.of(
                new LargeMachineLayouts.Cell(1, 0, 0, 18100, MultiblockLayout.Role.ITEM_FLUID_INPUT)), List.of());
        var west = new LargeMachineLayouts.Layout(List.of(
                new LargeMachineLayouts.Cell(-1, 0, 0, 18100, MultiblockLayout.Role.ITEM_FLUID_INPUT)), List.of());
        withLayouts(Map.of("irrigator", east, "sampleminer", west), () -> {
            var cells = new ArrayList<BlockPos>();
            try {
                BlockPos port = h.absolutePos(new BlockPos(6, 3, 6));
                put(h.getLevel(), port, part(18100), cells);
                var first = behavior(h.getLevel(), port.west(), "irrigator", cells);
                var second = behavior(h.getLevel(), port.east(), "sampleminer", cells);
                h.assertTrue(StructureTestSupport.ok(first), "the first controller claims the shared port");
                StructureScheduler.flush(h.getLevel());
                h.assertTrue(!second.isStructureOk() && port(h, port).isBoundTo(first.getBlockPos()),
                        "the loaded owner keeps the port");
                h.getLevel().setBlock(first.getBlockPos(), Blocks.AIR.defaultBlockState(), FLAGS);
                h.assertTrue(!second.isStructureOk(), "the waiting controller is not scanned inside the break");
                StructureScheduler.flush(h.getLevel());
                h.assertTrue(second.isStructureOk() && port(h, port).isBoundTo(second.getBlockPos()),
                        "releasing the port lets the other controller form at the end of the tick");
            } finally {
                clear(h.getLevel(), cells);
            }
        });
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200, batch = "st-reload")
    public static void shellUnloadStopsAndReloadKeepsContents(GameTestHelper h) {
        var cells = new ArrayList<BlockPos>();
        try {
            // Stay inside BlockPos's 26-bit X/Z (max 33,554,431) and clear of the gametest grid.
            BlockPos base = new BlockPos(33_000_000, 80, 33_000_000);
            MultiblockLayout.Cell sample = ImplosionCompressorControllerBlockEntity.LAYOUT.cells().stream()
                    .filter(cell -> cell.role() != MultiblockLayout.Role.AIR)
                    .filter(cell -> {
                        BlockPos at = cell.at(BlockPos.ZERO, Direction.NORTH);
                        return at.getX() != 0 || at.getZ() != 0;
                    })
                    .findFirst().orElseThrow();
            BlockPos local = sample.at(BlockPos.ZERO, Direction.NORTH);
            int originX = (base.getX() & ~15) + 8;
            int originZ = (base.getZ() & ~15) + 8;
            if (local.getX() != 0) {
                int boundary = ((base.getX() >> 4) + 1) << 4;
                originX = local.getX() > 0 ? boundary - local.getX() : boundary - 1 - local.getX();
            } else {
                int boundary = ((base.getZ() >> 4) + 1) << 4;
                originZ = local.getZ() > 0 ? boundary - local.getZ() : boundary - 1 - local.getZ();
            }
            BlockPos origin = new BlockPos(originX, base.getY(), originZ);
            BlockPos shellPos = sample.at(origin, Direction.NORTH);
            h.getLevel().getChunk(new ChunkPos(origin).x, new ChunkPos(origin).z);
            h.getLevel().getChunk(new ChunkPos(shellPos).x, new ChunkPos(shellPos).z);
            var machine = (ImplosionCompressorControllerBlockEntity) implosion(h.getLevel(), origin, cells);
            h.assertTrue(new ChunkPos(origin).x != new ChunkPos(shellPos).x
                    || new ChunkPos(origin).z != new ChunkPos(shellPos).z, "the shell crosses a chunk border");
            h.assertTrue(StructureTestSupport.ok(machine), "cross-chunk implosion forms");
            h.assertTrue(machine.inventory().getSlots() > 0, "implosion has an inventory");
            machine.inventory().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
            var energy = machine.spec().energyTag();
            long size = Math.max(1L, machine.getEnergySizeInputRecommended(energy, null));
            if (machine.doInject(energy, null, size, 1, true) > 0) size = machine.getEnergyTick();
            else size = machine.getEnergyTick();
            long seeded = size;
            ChunkPos shell = new ChunkPos(shellPos);
            StructureScheduler.chunkUnloadedForTests(h.getLevel(), shell);
            h.assertTrue(!machine.isStructureOk(), "unloading the shell chunk stops the machine");
            h.assertTrue(machine.inventory().getStackInSlot(0).getCount() == 7, "inventory stays while the shell is unloaded");
            h.assertTrue(machine.getEnergyTick() == seeded, "energy stays while the shell is unloaded");
            StructureScheduler.chunkLoadedForTests(h.getLevel(), shell);
            h.assertTrue(StructureTracking.tracker(machine).dirty(), "the load path queues the machine without the safety net");
            StructureScheduler.flush(h.getLevel());
            h.assertTrue(machine.isStructureOk(), "the reloaded shell forms again");
            h.assertTrue(machine.inventory().getStackInSlot(0).is(Items.DIAMOND)
                    && machine.inventory().getStackInSlot(0).getCount() == 7, "inventory survives the reload");
            h.assertTrue(machine.getEnergyTick() == seeded, "energy survives the reload");
        } finally {
            clear(h.getLevel(), cells);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200, batch = "st-epoch")
    public static void epochReloadFollowsTheNewLayoutAndTag(GameTestHelper h) {
        var planks = new LargeMachineLayouts.Layout(List.of(
                new LargeMachineLayouts.Cell(1, 0, 0, 0, MultiblockLayout.Role.CASING, "#minecraft:planks")), List.of());
        var wool = new LargeMachineLayouts.Layout(List.of(
                new LargeMachineLayouts.Cell(1, 0, 0, 0, MultiblockLayout.Role.CASING, "#minecraft:wool")), List.of());
        withLayouts(Map.of("irrigator", planks), () -> {
            var cells = new ArrayList<BlockPos>();
            try {
                BlockPos at = h.absolutePos(new BlockPos(3, 4, 3));
                var machine = behavior(h.getLevel(), at, "irrigator", cells);
                put(h.getLevel(), at.east(), Blocks.OAK_PLANKS.defaultBlockState(), cells);
                h.assertTrue(StructureTestSupport.ok(machine), "the plank tag forms");
                StructureEpoch.bump();
                StructureScheduler.flush(h.getLevel());
                h.assertTrue(machine.isStructureOk(), "an unchanged layout stays formed after reload");
                layouts().put("irrigator", wool);
                StructureEpoch.bump();
                StructureScheduler.flush(h.getLevel());
                h.assertTrue(!machine.isStructureOk(), "the reloaded wool tag rejects oak planks");
                h.getLevel().setBlock(at.east(), Blocks.WHITE_WOOL.defaultBlockState(), FLAGS);
                StructureScheduler.flush(h.getLevel());
                h.assertTrue(machine.isStructureOk(), "the new tag accepts wool");
            } finally {
                clear(h.getLevel(), cells);
            }
        });
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200, batch = "st-wrench")
    public static void wrenchAndFacingRecheckTankBoilerAndLogistics(GameTestHelper h) {
        BlockPos origin = new BlockPos(32_000_000, 80, 32_000_000);
        recheckFacing(h, "tank", origin, (level, at, cells) -> preview(level, at, familyBlock("tank"), cells));
        recheckFacing(h, "boiler", origin, (level, at, cells) -> preview(level, at, familyBlock("boiler"), cells));
        recheckFacing(h, "logistics", origin, StructureTrackingTests::logistics);
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200, batch = "st-piston")
    public static void pistonPushesAPlainCasingOutOfTheLayout(GameTestHelper h) {
        var previous = layouts().put("irrigator", stoneLayout());
        var cells = new ArrayList<BlockPos>();
        BlockPos at = h.absolutePos(new BlockPos(4, 3, 4));
        try {
            var machine = behavior(h.getLevel(), at, "irrigator", cells);
            BlockPos stone = at.east();
            put(h.getLevel(), stone, Blocks.STONE.defaultBlockState(), cells);
            h.assertTrue(StructureTestSupport.ok(machine), "stone casing forms");
            BlockPos piston = stone.south();
            put(h.getLevel(), piston, Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH), cells);
            h.getLevel().setBlock(piston.south(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
            cells.add(piston.south().immutable());
            h.runAfterDelay(8, () -> {
                try {
                    h.assertTrue(!h.getLevel().getBlockState(stone).is(Blocks.STONE), "the piston moved the casing");
                    h.assertTrue(!machine.isStructureOk(), "the pushed casing unforms the machine");
                } finally {
                    if (previous == null) layouts().remove("irrigator");
                    else layouts().put("irrigator", previous);
                    clear(h.getLevel(), cells);
                }
                h.succeed();
            });
        } catch (RuntimeException | Error failure) {
            if (previous == null) layouts().remove("irrigator");
            else layouts().put("irrigator", previous);
            clear(h.getLevel(), cells);
            throw failure;
        }
    }

    @GameTest(template = "test_empty", timeoutTicks = 200, batch = "st-nolayout")
    public static void noLayoutFormsOnTheFirstQueryAndALayoutChecksCells(GameTestHelper h) {
        var previous = layouts().get("irrigator");
        var cells = new ArrayList<BlockPos>();
        try {
            layouts().remove("irrigator");
            BlockPos at = h.absolutePos(new BlockPos(3, 3, 3));
            var open = behavior(h.getLevel(), at, "irrigator", cells);
            h.assertTrue(open.isStructureOk(), "a behavior machine with no layout is formed on the first query");
            h.assertTrue(StructureTracking.tracker(open).state() == StructureTracker.State.FORMED, "it does not wait for the scheduler");
            layouts().put("irrigator", stoneLayout());
            BlockPos other = at.offset(4, 0, 0);
            var laid = behavior(h.getLevel(), other, "irrigator", cells);
            h.assertTrue(!laid.isStructureOk(), "a layout machine is unformed until its cells are scanned");
            put(h.getLevel(), other.east(), Blocks.STONE.defaultBlockState(), cells);
            h.assertTrue(!laid.isStructureOk(), "placing the cell does not scan inside the same call");
            StructureScheduler.flush(h.getLevel());
            h.assertTrue(laid.isStructureOk(), "the cell is checked at the end of the tick");
        } finally {
            if (previous == null) layouts().remove("irrigator");
            else layouts().put("irrigator", previous);
            clear(h.getLevel(), cells);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400, batch = "st-budget")
    public static void twoHundredMachinesShareTheCellBudget(GameTestHelper h) {
        withLayouts(Map.of("irrigator", stoneLayout()), () -> {
            var cells = new ArrayList<BlockPos>();
            var machines = new ArrayList<BehaviorMachineBlockEntity>();
            StructureScheduler.setCellBudgetForTests(20);
            try {
                BlockPos origin = new BlockPos(28_000_000, 80, 28_000_000);
                for (int i = 0; i < 200; i++) {
                    BlockPos at = origin.offset((i % 20) * 3, 0, (i / 20) * 3);
                    machines.add(behavior(h.getLevel(), at, "irrigator", cells));
                    put(h.getLevel(), at.east(), Blocks.STONE.defaultBlockState(), cells);
                    StructureTracking.attach((TrackedMultiblock) h.getLevel().getBlockEntity(at));
                }
                StructureScheduler.tick(h.getLevel());
                int first = formed(machines);
                h.assertTrue(first > 0 && first < 200, "the first tick forms only a budget slice, formed=" + first);
                int ticks = 1;
                while (formed(machines) < 200 && ticks < 40) {
                    StructureScheduler.tick(h.getLevel());
                    ticks++;
                }
                h.assertTrue(formed(machines) == 200 && ticks <= 25,
                        "all 200 machines form within the budget, ticks=" + ticks + " formed=" + formed(machines));
            } finally {
                StructureScheduler.setCellBudgetForTests(-1);
                clear(h.getLevel(), cells);
            }
        });
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200, batch = "st-bucket")
    public static void bucketClickFollowsTheSyncedFormedBit(GameTestHelper h) {
        BlockPos origin = h.absolutePos(new BlockPos(8, 10, 8));
        var tankCells = new ArrayList<BlockPos>();
        var boilerCells = new ArrayList<BlockPos>();
        try {
            var tank = (MultiblockTankControllerBlockEntity) preview(h.getLevel(), origin, familyBlock("tank"), tankCells);
            CompoundTag early = new CompoundTag();
            ClientFormedSync.write(tank, early);
            h.assertTrue(early.contains(ClientFormedSync.KEY) && !early.getBoolean(ClientFormedSync.KEY),
                    "the first packet says unformed while the tracker has not scanned");
            h.assertTrue(!tank.isStructureOk(), "server agrees before the scan");
            var player = player(h.getLevel(), "TankBucket");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            h.assertTrue(click(tank, player).contains("PASS") && tank.fluidHandler().getFluidInTank(0).isEmpty(),
                    "a click before the structure is known does not move fluid");
            h.assertTrue(StructureTestSupport.ok(tank), "tank forms");
            CompoundTag formed = new CompoundTag();
            ClientFormedSync.write(tank, formed);
            ClientFormedSync.read(tank, formed);
            h.assertTrue(formed.getBoolean(ClientFormedSync.KEY) && ClientFormedSync.get(tank), "the formed bit is in the update packet");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            String filled = click(tank, player);
            h.assertTrue(filled.contains("CONSUME") || filled.contains("SUCCESS"), "a formed tank accepts the bucket: " + filled);
            h.assertTrue(tank.fluidHandler().getFluidInTank(0).getAmount() == 1000, "exactly one bucket is stored");
            BlockPos tankShell = tankCells.stream()
                    .filter(pos -> !pos.equals(origin) && !h.getLevel().getBlockState(pos).isAir())
                    .findFirst().orElseThrow();
            h.getLevel().setBlock(tankShell, Blocks.AIR.defaultBlockState(), FLAGS);
            h.assertTrue(!tank.isStructureOk(), "the broken tank is unformed immediately");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            h.assertTrue(click(tank, player).contains("PASS") && tank.fluidHandler().getFluidInTank(0).getAmount() == 1000,
                    "an unformed tank lets the click pass and does not add a second bucket");

            clear(h.getLevel(), tankCells);
            var boiler = (OriginalLargeBoilerControllerBlockEntity) preview(h.getLevel(), origin, familyBlock("boiler"), boilerCells);
            CompoundTag boilerEarly = new CompoundTag();
            ClientFormedSync.write(boiler, boilerEarly);
            h.assertTrue(!boilerEarly.getBoolean(ClientFormedSync.KEY), "a new boiler packet starts unformed");
            h.assertTrue(StructureTestSupport.ok(boiler), "boiler forms");
            CompoundTag boilerFormed = new CompoundTag();
            ClientFormedSync.write(boiler, boilerFormed);
            h.assertTrue(boilerFormed.getBoolean(ClientFormedSync.KEY), "a formed boiler publishes the sync bit");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            String boiled = click(boiler, player);
            h.assertTrue((boiled.contains("CONSUME") || boiled.contains("SUCCESS")) && boiler.waterAmount() == 1000,
                    "a formed boiler takes one bucket: " + boiled + " water=" + boiler.waterAmount());
            BlockPos boilerShell = boilerCells.stream()
                    .filter(pos -> !pos.equals(origin) && !h.getLevel().getBlockState(pos).isAir())
                    .findFirst().orElseThrow();
            h.getLevel().setBlock(boilerShell, Blocks.AIR.defaultBlockState(), FLAGS);
            long water = boiler.waterAmount();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            h.assertTrue(!boiler.isStructureOk() && click(boiler, player).contains("PASS") && boiler.waterAmount() == water,
                    "an unformed boiler does not take water");
        } finally {
            clear(h.getLevel(), tankCells);
            clear(h.getLevel(), boilerCells);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200, batch = "st-ctrl")
    public static void controllerUnloadDoesNotChangeLoadedParts(GameTestHelper h) {
        var cells = new ArrayList<BlockPos>();
        try {
            BlockPos origin = h.absolutePos(new BlockPos(4, 4, 4));
            var machine = (ImplosionCompressorControllerBlockEntity) implosion(h.getLevel(), origin, cells);
            h.assertTrue(StructureTestSupport.ok(machine), "implosion forms");
            BlockPos wall = firstSolid(origin, ImplosionCompressorControllerBlockEntity.LAYOUT);
            var port = port(h, wall);
            BlockState before = h.getLevel().getBlockState(wall);
            int design = port.structureDesign();
            h.assertTrue(port.isBoundTo(origin), "the part is bound");
            StructureTracking.onChunkUnloaded(machine);
            machine.setRemoved();
            h.assertTrue(h.getLevel().getBlockState(wall).equals(before), "a loaded part keeps its block state");
            h.assertTrue(port.structureDesign() == design, "a loaded part keeps its design number");
            h.assertTrue(port.isBoundTo(origin), "unload does not release the part");
        } finally {
            clear(h.getLevel(), cells);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200, batch = "st-pending")
    public static void pendingChunkBecomesAvailableWithoutTheSafetyNet(GameTestHelper h) {
        ChunkPos far = new ChunkPos(30_000, 30_000);
        BlockPos probe = new BlockPos(far.getMinBlockX(), h.getLevel().getMinBuildHeight(), far.getMinBlockZ());
        h.assertTrue(!h.getLevel().hasChunkAt(probe), "the far chunk starts unloaded");
        StructureScheduler.chunkLoadedForTests(h.getLevel(), far);
        h.assertTrue(StructureScheduler.pendingForTests(h.getLevel(), far.toLong()),
                "a load reported before the chunk is readable stays pending");
        StructureScheduler.flush(h.getLevel());
        h.assertTrue(StructureScheduler.pendingForTests(h.getLevel(), far.toLong()), "an unavailable chunk is not promoted");
        h.getLevel().getChunk(far.x, far.z);
        StructureScheduler.flush(h.getLevel());
        h.assertTrue(!StructureScheduler.pendingForTests(h.getLevel(), far.toLong()), "the next tick promotes a readable chunk");

        var cells = new ArrayList<BlockPos>();
        try {
            BlockPos origin = h.absolutePos(new BlockPos(4, 4, 4));
            var machine = (ImplosionCompressorControllerBlockEntity) implosion(h.getLevel(), origin, cells);
            h.assertTrue(StructureTestSupport.ok(machine), "implosion forms");
            ChunkPos chunk = new ChunkPos(origin);
            StructureScheduler.chunkUnloadedForTests(h.getLevel(), chunk);
            StructureTracker tracker = StructureTracking.tracker(machine);
            h.assertTrue(!machine.isStructureOk() && tracker.state() == StructureTracker.State.SUSPENDED && !tracker.dirty(),
                    "the unload leaves the machine suspended and out of the dirty queue");
            StructureScanProbe.enable(true);
            StructureScanProbe.reset();
            StructureScheduler.chunkLoadedForTests(h.getLevel(), chunk);
            h.assertTrue(tracker.dirty() && !tracker.safety(), "chunk load queues a normal scan, not a safety scan");
            StructureScheduler.flush(h.getLevel());
            h.assertTrue(machine.isStructureOk() && StructureScanProbe.scans() >= 1, "the machine recovers on that tick");
        } finally {
            StructureScanProbe.enable(false);
            clear(h.getLevel(), cells);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100, batch = "st-mask")
    public static void sectionMaskWatchesIndexesPast31(GameTestHelper h) {
        h.assertTrue(SectionWatchMask.wordsFor(33) == 1 && SectionWatchMask.wordsFor(65) == 2,
                "the mask grows a word at section 64");
        var mask = new SectionWatchMask(80);
        mask.set(32, true);
        mask.set(40, true);
        mask.set(64, true);
        h.assertTrue(mask.get(32) && mask.get(40) && mask.get(64) && !mask.get(33) && !mask.get(63),
                "bits 32, 40 and 64 are independent");
        BlockPos pos = h.absolutePos(new BlockPos(1, 2, 1));
        LevelChunk chunk = h.getLevel().getChunkAt(pos);
        int top = chunk.getSectionIndex(h.getLevel().getMaxBuildHeight() - 1);
        h.assertTrue(top < 32, "this test dimension has no section 32; top index is " + top);
        StructureChunkWatch watch = (StructureChunkWatch) (Object) chunk;
        watch.gt$structureWatch(40, true);
        watch.gt$structureWatch(64, true);
        h.assertTrue(watch.gt$structureWatched(40) && watch.gt$structureWatched(64) && !watch.gt$structureWatched(41),
                "a live chunk stores section bits past 31");
        watch.gt$structureWatch(40, false);
        watch.gt$structureWatch(64, false);
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200, batch = "st-props")
    public static void satisfiedCrucibleFormAndFluidLevelDoNotDirty(GameTestHelper h) {
        Block emitter = BuiltInRegistries.BLOCK.stream().filter(block -> block instanceof AddonEnergyPartBlock).findFirst().orElse(null);
        h.assertTrue(emitter != null, "an addon energy part exists so SATISFIED can flip");
        String plain = BuiltInRegistries.BLOCK.getKey(emitter).toString();
        var layout = new LargeMachineLayouts.Layout(List.of(
                new LargeMachineLayouts.Cell(1, 0, 0, 0, MultiblockLayout.Role.CASING, plain),
                new LargeMachineLayouts.Cell(-1, 0, 0, 0, MultiblockLayout.Role.CASING, "anyfluid")), List.of());
        withLayouts(Map.of("irrigator", layout), () -> {
            resolved().put(plain, emitter);
            var cells = new ArrayList<BlockPos>();
            var crucibleCells = new ArrayList<BlockPos>();
            try {
                BlockPos at = h.absolutePos(new BlockPos(4, 4, 4));
                var machine = behavior(h.getLevel(), at, "irrigator", cells);
                put(h.getLevel(), at.east(), emitter.defaultBlockState(), cells);
                put(h.getLevel(), at.west(), Blocks.WATER.defaultBlockState(), cells);
                h.assertTrue(StructureTestSupport.ok(machine), "layout family forms");
                var tracker = StructureTracking.tracker(machine);
                h.assertTrue(tracker != null && !tracker.dirty() && machine.isStructureOk(),
                        "the layout is quiet before the property edits");
                StructureScanProbe.enable(true);
                StructureScanProbe.reset();
                BlockState part = h.getLevel().getBlockState(at.east());
                for (int i = 0; i < 100; i++) {
                    h.getLevel().setBlock(at.east(), part.setValue(AddonEnergyPartBlock.SATISFIED, (i & 1) == 0), FLAGS);
                }
                h.assertTrue(StructureScanProbe.scans() == 0 && !tracker.dirty() && machine.isStructureOk(),
                        "SATISFIED does not dirty a layout machine scans=" + StructureScanProbe.scans()
                                + " dirty=" + tracker.dirty() + " formed=" + machine.isStructureOk()
                                + " east=" + h.getLevel().getBlockState(at.east()));
                h.getLevel().setBlock(at.west(), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 4), FLAGS);
                h.assertTrue(StructureScanProbe.scans() == 0 && !tracker.dirty() && machine.isStructureOk(),
                        "fluid level does not dirty a layout machine scans=" + StructureScanProbe.scans()
                                + " dirty=" + tracker.dirty() + " formed=" + machine.isStructureOk()
                                + " west=" + h.getLevel().getBlockState(at.west()));

                clear(h.getLevel(), cells);
                BlockPos crucibleAt = h.absolutePos(new BlockPos(8, 8, 8));
                var crucible = preview(h.getLevel(), crucibleAt, familyBlock("crucible"), crucibleCells);
                h.assertTrue(StructureTestSupport.ok(crucible), "crucible forms");
                BlockPos wall = crucibleCells.stream().filter(pos -> !pos.equals(crucibleAt))
                        .filter(pos -> h.getLevel().getBlockState(pos).hasProperty(MultiblockPortBlock.CRUCIBLE_FORMED))
                        .findFirst().orElseThrow();
                StructureScanProbe.reset();
                BlockState state = h.getLevel().getBlockState(wall);
                h.getLevel().setBlock(wall, state.setValue(MultiblockPortBlock.CRUCIBLE_FORMED, !state.getValue(MultiblockPortBlock.CRUCIBLE_FORMED)), FLAGS);
                var crucibleTracker = StructureTracking.tracker(crucible);
                h.assertTrue(StructureScanProbe.scans() == 0 && crucibleTracker != null && !crucibleTracker.dirty()
                                && StructureTracking.query((TrackedMultiblock) crucible),
                        "CRUCIBLE_FORMED does not dirty a custom structure");
            } finally {
                StructureScanProbe.enable(false);
                clear(h.getLevel(), cells);
                clear(h.getLevel(), crucibleCells);
            }
        });
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200, batch = "st-expedite")
    public static void exhaustedBudgetStillRepairsAFormedMachineThisTick(GameTestHelper h) {
        var stone = new LargeMachineLayouts.Cell(1, 0, 0, 0, MultiblockLayout.Role.CASING, "minecraft:stone",
                -1, List.of(new gregtech.content.multiblock.SharedLargeMachineLayouts.Choice(18100, null)));
        var layout = new LargeMachineLayouts.Layout(List.of(stone), List.of());
        withLayouts(Map.of("irrigator", layout), () -> {
            resolved().put("minecraft:stone", Blocks.STONE);
            var cells = new ArrayList<BlockPos>();
            try {
                BlockPos at = h.absolutePos(new BlockPos(3, 3, 3));
                var machine = behavior(h.getLevel(), at, "irrigator", cells);
                put(h.getLevel(), at.east(), Blocks.STONE.defaultBlockState(), cells);
                h.assertTrue(StructureTestSupport.ok(machine), "alternative casing forms");
                StructureScheduler.setCellBudgetForTests(0);
                h.getLevel().setBlock(at.east(), part(18100), FLAGS);
                h.assertTrue(!machine.isStructureOk(), "a legal swap stops the machine until the end of the tick");
                StructureScheduler.flush(h.getLevel());
                h.assertTrue(machine.isStructureOk(), "the swap is scanned ahead of an exhausted budget and the machine runs again");
                h.getLevel().setBlock(at.east(), Blocks.AIR.defaultBlockState(), FLAGS);
                h.assertTrue(!machine.isStructureOk(), "breaking a formed casing stops it immediately");
                StructureScheduler.flush(h.getLevel());
                h.assertTrue(!machine.isStructureOk(), "the broken shell is still unformed after the expedited scan");
            } finally {
                StructureScheduler.setCellBudgetForTests(-1);
                clear(h.getLevel(), cells);
            }
        });
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200, batch = "st-safety")
    public static void safetyRescanDoesNotBindOrMarkChunks(GameTestHelper h) {
        var cells = new ArrayList<BlockPos>();
        try {
            BlockPos origin = h.absolutePos(new BlockPos(4, 4, 4));
            var machine = (ImplosionCompressorControllerBlockEntity) implosion(h.getLevel(), origin, cells);
            h.assertTrue(StructureTestSupport.ok(machine), "implosion forms");
            BlockPos wall = firstSolid(origin, ImplosionCompressorControllerBlockEntity.LAYOUT);
            var port = port(h, wall);
            int design = port.structureDesign();
            h.assertTrue(port.isBoundTo(origin), "part bound before the safety scan");
            LevelChunk chunk = h.getLevel().getChunkAt(wall);
            chunk.setUnsaved(false);
            StructureScanProbe.enable(true);
            StructureScanProbe.reset();
            StructureTracking.evaluateForTests(machine, true);
            h.assertTrue(machine.isStructureOk(), "the safety scan leaves a good structure formed");
            h.assertTrue(StructureScanProbe.binds() == 0 && StructureScanProbe.releases() == 0,
                    "the safety scan does not bind or release, binds=" + StructureScanProbe.binds()
                            + " releases=" + StructureScanProbe.releases());
            h.assertTrue(port.isBoundTo(origin) && port.structureDesign() == design, "design number is not written again");
            h.assertTrue(!chunk.isUnsaved(), "the part chunk is not marked for save");
        } finally {
            StructureScanProbe.enable(false);
            clear(h.getLevel(), cells);
        }
        h.succeed();
    }

    private static void measure(GameTestHelper h, String family, BlockPos origin, Placer placer) {
        var cells = new ArrayList<BlockPos>();
        StructureTrackingMode.overrideForTests(null);
        try {
            BlockEntity entity = placer.place(h.getLevel(), origin, cells);
            h.assertTrue(entity instanceof TrackedMultiblock, family + " controller");
            h.assertTrue(StructureTestSupport.ok(entity), family + " forms and its reads stay inside the declared footprint");
            StructureScanProbe.reset();
            for (int i = 0; i < 100; i++) StructureTracking.query((TrackedMultiblock) entity);
            for (int i = 0; i < 20; i++) StructureTracking.query((TrackedMultiblock) entity);
            for (int i = 0; i < 20; i++) StructureTracking.query((TrackedMultiblock) entity);
            int trackedReads = StructureScanProbe.blockReads();
            h.assertTrue(StructureScanProbe.scans() == 0, family + " tracked idle and port queries do not scan");
            StructureTrackingMode.overrideForTests("legacy");
            StructureScanProbe.reset();
            for (int i = 0; i < 140; i++) StructureTracking.query((TrackedMultiblock) entity);
            h.assertTrue(StructureScanProbe.scans() > 0, family + " legacy queries scan");
            System.out.println("[structure-tracking] family=" + family
                    + " legacyScans=" + StructureScanProbe.scans()
                    + " legacyBlockReads=" + StructureScanProbe.blockReads()
                    + " legacyEntityReads=" + StructureScanProbe.entityReads()
                    + " legacyChunkChecks=" + StructureScanProbe.chunkChecks()
                    + " legacyBinds=" + StructureScanProbe.binds()
                    + " legacyReleases=" + StructureScanProbe.releases()
                    + " trackedScans=0 trackedBlockReads=" + trackedReads);
        } finally {
            StructureTrackingMode.overrideForTests(null);
            clear(h.getLevel(), cells);
        }
    }

    private static void recheckFacing(GameTestHelper h, String family, BlockPos origin, Placer placer) {
        var cells = new ArrayList<BlockPos>();
        try {
            BlockEntity entity = placer.place(h.getLevel(), origin, cells);
            h.assertTrue(StructureTestSupport.ok(entity), family + " forms, state=" + entity.getBlockState());
            BlockState turned = entity.getBlockState();
            Direction facing = turned.hasProperty(HorizontalDirectionalBlock.FACING)
                    ? turned.getValue(HorizontalDirectionalBlock.FACING) : turned.getValue(DirectionalBlock.FACING);
            Direction next = facing.getClockWise();
            if (turned.hasProperty(HorizontalDirectionalBlock.FACING))
                turned = turned.setValue(HorizontalDirectionalBlock.FACING, next);
            else turned = turned.setValue(DirectionalBlock.FACING, next);
            h.getLevel().setBlock(origin, turned, FLAGS);
            BlockEntity live = h.getLevel().getBlockEntity(origin);
            if (live != null && live.getBlockState().getBlock() instanceof TankControllerBlock tank)
                tank.toolStateChanged(h.getLevel(), origin, live.getBlockState());
            else if (live != null && live.getBlockState().getBlock() instanceof OriginalLargeBoilerControllerBlock boiler)
                boiler.toolStateChanged(h.getLevel(), origin, live.getBlockState());
            else if (live != null && live.getBlockState().getBlock() instanceof LogisticsCoreControllerBlock logistics)
                logistics.toolStateChanged(h.getLevel(), origin, live.getBlockState());
            h.assertTrue(live instanceof TrackedMultiblock && !StructureTracking.query((TrackedMultiblock) live),
                    family + " drops formed when its facing changes");
            StructureScheduler.flush(h.getLevel());
            h.assertTrue(live instanceof TrackedMultiblock && !StructureTracking.query((TrackedMultiblock) live),
                    family + " is checked again against the new facing");
        } finally {
            clear(h.getLevel(), cells);
        }
    }

    private static BlockEntity preview(ServerLevel level, BlockPos origin, Block block, List<BlockPos> cells) {
        put(level, origin, facingNorth(block), cells);
        for (var cell : ControllerStructureLayouts.cells(block).entrySet())
            put(level, origin.offset(cell.getKey()), cell.getValue().defaultBlockState(), cells);
        return level.getBlockEntity(origin);
    }

    private static BlockEntity implosion(ServerLevel level, BlockPos origin, List<BlockPos> cells) {
        put(level, origin, facingNorth(block("implosion_compressor_main")), cells);
        Block wall = block("implosion_compressor_wall");
        for (var cell : ImplosionCompressorControllerBlockEntity.LAYOUT.cells()) {
            BlockState state = cell.role() == MultiblockLayout.Role.AIR
                    ? Blocks.AIR.defaultBlockState() : wall.defaultBlockState();
            put(level, cell.at(origin, Direction.NORTH), state, cells);
        }
        return level.getBlockEntity(origin);
    }

    private static BlockEntity fusion(ServerLevel level, BlockPos origin, List<BlockPos> cells) {
        put(level, origin, facingNorth(block("fusion_reactor_main")), cells);
        for (var cell : FusionStructure.CELLS)
            put(level, cell.at(origin, Direction.NORTH), cell.block().defaultBlockState(), cells);
        return level.getBlockEntity(origin);
    }

    private static BlockEntity gasTurbine(ServerLevel level, BlockPos origin, List<BlockPos> cells) {
        var controller = GasTurbineDefinitions.blocks().get(0).get();
        var grade = GasTurbineDefinitions.grade(controller);
        put(level, origin, facingNorth(controller), cells);
        for (var cell : TurbineStructure.LAYOUT.cells())
            put(level, cell.at(origin, Direction.NORTH), grade.wall().defaultBlockState(), cells);
        return level.getBlockEntity(origin);
    }

    private static BlockEntity logistics(ServerLevel level, BlockPos origin, List<BlockPos> cells) {
        put(level, origin, facingNorth(block("logistics_core")), cells);
        for (var cell : LogisticsCoreStructure.cells()) {
            int id = switch (cell.kind()) {
                case CPU -> LogisticsCoreStructure.VERSATILE;
                case VENT -> LogisticsCoreStructure.VENT;
                case WALL -> LogisticsCoreStructure.WALL;
            };
            put(level, cell.at(origin, Direction.NORTH), part(id), cells);
        }
        return level.getBlockEntity(origin);
    }

    private static BlockEntity exchanger(ServerLevel level, BlockPos origin, List<BlockPos> cells) {
        put(level, origin, facingNorth(block("heat_exchanger_main")), cells);
        for (var cell : SharedHeatExchangerStructure.CELLS)
            put(level, origin.offset(cell.right(), cell.up(), cell.back()), part(cell.part()), cells);
        return level.getBlockEntity(origin);
    }

    private static BlockEntity matter(ServerLevel level, BlockPos origin, List<BlockPos> cells) {
        Block controller = BuiltInRegistries.BLOCK.stream()
                .filter(block -> block instanceof BasicMachineBlock basic && basic.basicSpec().machineName().equals("largemassfab"))
                .findFirst().orElseThrow();
        put(level, origin, facingNorth(controller), cells);
        for (var cell : MatterFabricatorStructure.CELLS)
            put(level, cell.at(origin, Direction.NORTH), cell.block().defaultBlockState(), cells);
        return level.getBlockEntity(origin);
    }

    private static BlockEntity fuelTurbine(ServerLevel level, BlockPos origin, List<BlockPos> cells) {
        put(level, origin, facingNorth(block("addon_sample", "sample_turbine")), cells);
        for (var cell : LargeMachineLayouts.cells("sample_turbine"))
            put(level, cell.at(origin, Direction.NORTH), cell.block().defaultBlockState(), cells);
        return level.getBlockEntity(origin);
    }

    private static BehaviorMachineBlockEntity behavior(ServerLevel level, BlockPos origin, String name, List<BlockPos> cells) {
        String path = switch (name) {
            case "sampleminer" -> "sample_miner";
            default -> name;
        };
        put(level, origin, facingNorth(block("addon_sample", path)), cells);
        return (BehaviorMachineBlockEntity) level.getBlockEntity(origin);
    }

    private static LargeMachineLayouts.Layout stoneLayout() {
        resolved().put("minecraft:stone", Blocks.STONE);
        return new LargeMachineLayouts.Layout(List.of(
                new LargeMachineLayouts.Cell(1, 0, 0, 0, MultiblockLayout.Role.CASING, "minecraft:stone")), List.of());
    }

    private static Block familyBlock(String family) {
        for (Block block : ControllerStructureLayouts.all().keySet())
            if (family.equals(StructureTrackingTests.family(block))) return block;
        throw new AssertionError(family);
    }

    private static String family(Block block) {
        if (block instanceof TankControllerBlock) return "tank";
        if (block instanceof OriginalLargeBoilerControllerBlock) return "boiler";
        if (block instanceof LargeCrucibleControllerBlock) return "crucible";
        if (block instanceof VonDaGraaggControllerBlock) return "graagg";
        if (block instanceof LightningRodControllerBlock || block instanceof OriginalLightningRodControllerBlock) return "lightning";
        if (block instanceof OriginalBedrockDrillControllerBlock || block instanceof BedrockDrillControllerBlock) return "drill";
        if (block instanceof AxialGeneratorBlock) return "axial";
        if (block instanceof CokeOvenControllerBlock || block instanceof CryoDistillationControllerBlock
                || block instanceof MultiblockControllerBlock) return "large-recipe";
        return null;
    }

    private static BlockPos firstSolid(BlockPos origin, MultiblockLayout layout) {
        var cell = layout.cells().stream().filter(it -> it.role() != MultiblockLayout.Role.AIR).findFirst().orElseThrow();
        return cell.at(origin, Direction.NORTH);
    }

    private static BlockState part(int id) {
        return LargeMachineParts.block(id).defaultBlockState();
    }

    private static BlockState facingNorth(Block block) {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(HorizontalDirectionalBlock.FACING))
            return state.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        if (state.hasProperty(DirectionalBlock.FACING))
            return state.setValue(DirectionalBlock.FACING, Direction.NORTH);
        return state;
    }

    private static void put(ServerLevel level, BlockPos pos, BlockState state, List<BlockPos> cells) {
        level.setBlock(pos, state, FLAGS);
        cells.add(pos.immutable());
    }

    private static void clear(ServerLevel level, List<BlockPos> cells) {
        for (int i = cells.size() - 1; i >= 0; i--) {
            BlockPos pos = cells.get(i);
            if (level.hasChunkAt(pos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
        }
        cells.clear();
    }

    private static Block block(String path) {
        return block("gt6ce", path);
    }

    private static Block block(String namespace, String path) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
        if (block == null || block == Blocks.AIR) throw new AssertionError("Missing block " + namespace + ":" + path);
        return block;
    }

    private static MultiblockPortBlockEntity port(GameTestHelper h, BlockPos pos) {
        return (MultiblockPortBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    private static int formed(List<BehaviorMachineBlockEntity> machines) {
        int count = 0;
        for (var machine : machines) if (machine.isStructureOk()) count++;
        return count;
    }

    private static int repeats(BehaviorMachineBlockEntity machine, String key) {
        try {
            var field = BehaviorMachineBlockEntity.class.getDeclaredField("context");
            field.setAccessible(true);
            return ((MachineBehaviorContext) field.get(machine)).structureRepeats(key);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static Player player(ServerLevel level, String name) {
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), name));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        return player;
    }

    private static String click(BlockEntity entity, Player player) {
        BlockState state = entity.getBlockState();
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(entity.getBlockPos()), Direction.UP, entity.getBlockPos(), false);
        Method method = declared(state.getBlock().getClass(), "interact");
        if (method == null) method = declared(state.getBlock().getClass(), "use");
        if (method == null) throw new AssertionError("No bucket click on " + state.getBlock().getClass().getSimpleName());
        method.setAccessible(true);
        try {
            return String.valueOf(method.invoke(state.getBlock(), state, entity.getLevel(), entity.getBlockPos(),
                    player, InteractionHand.MAIN_HAND, hit));
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static Method declared(Class<?> type, String name) {
        for (Method method : type.getDeclaredMethods())
            if (method.getName().equals(name) && method.getParameterCount() == 6) return method;
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, LargeMachineLayouts.Layout> layouts() {
        try {
            var field = LargeMachineLayouts.class.getDeclaredField("LAYOUTS");
            field.setAccessible(true);
            return (Map<String, LargeMachineLayouts.Layout>) field.get(null);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Block> resolved() {
        try {
            var field = PlainStructureBlocks.class.getDeclaredField("RESOLVED");
            field.setAccessible(true);
            return (Map<String, Block>) field.get(null);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static void withLayouts(Map<String, LargeMachineLayouts.Layout> replacements, Runnable body) {
        var layouts = layouts();
        var previous = new LinkedHashMap<String, LargeMachineLayouts.Layout>();
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

    @FunctionalInterface
    private interface Placer {
        BlockEntity place(ServerLevel level, BlockPos origin, List<BlockPos> cells);
    }
}
