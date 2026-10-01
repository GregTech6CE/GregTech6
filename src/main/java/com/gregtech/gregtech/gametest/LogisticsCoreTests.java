package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.machine.LogisticsCoreControllerBlock;
import com.gregtech.gregtech.blockentity.machine.LogisticsCoreControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity;
import com.gregtech.gregtech.content.logistics.LogisticsCoreStructure;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTLasers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Focused GT6 17997 geometry, wall energy, accounting and manufacturing regression coverage. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class LogisticsCoreTests {
    private static final BlockPos ORIGIN = new BlockPos(12, 4, 10);

    static LogisticsCoreControllerBlockEntity build(GameTestHelper helper, Direction front) {
        helper.setBlock(ORIGIN, GTLasers.LOGISTICS_CORE.get().defaultBlockState()
                .setValue(LogisticsCoreControllerBlock.FACING, front));
        BlockPos absolute = helper.absolutePos(ORIGIN);
        for (var cell : LogisticsCoreStructure.cells()) {
            int id = switch (cell.kind()) {
                case CPU -> LogisticsCoreStructure.VERSATILE;
                case VENT -> LogisticsCoreStructure.VENT;
                case WALL -> LogisticsCoreStructure.WALL;
            };
            helper.getLevel().setBlock(cell.at(absolute, front), LargeMachineParts.block(id).defaultBlockState(), 3);
        }
        var core = (LogisticsCoreControllerBlockEntity) helper.getLevel().getBlockEntity(absolute);
        helper.assertTrue(core.isStructureOk(), "original 5x5x5 Logistics Core forms facing " + front);
        return core;
    }

    private static MultiblockPortBlockEntity part(LogisticsCoreControllerBlockEntity core,
            Direction front, LogisticsCoreStructure.Kind kind) {
        var cell = LogisticsCoreStructure.cells().stream().filter(c -> c.kind() == kind).findFirst().orElseThrow();
        return (MultiblockPortBlockEntity) core.getLevel().getBlockEntity(cell.at(core.getBlockPos(), front));
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void gt6CubeAndProcessorEconomics(GameTestHelper helper) {
        helper.assertTrue(LogisticsCoreStructure.cells().size() == 124, "controller replaces one of 125 cells");
        for (var kind : LogisticsCoreStructure.Kind.values()) {
            long expected = switch (kind) { case CPU -> 27; case VENT -> 53; case WALL -> 44; };
            helper.assertTrue(LogisticsCoreStructure.cells().stream().filter(c -> c.kind() == kind).count()
                    == expected, kind + " count must match GT6");
        }
        var core = build(helper, Direction.EAST);
        var allVersatile = core.processorCounts();
        helper.assertTrue(allVersatile.equals(new LogisticsCoreStructure.Counts(27, 27, 27, 27)),
                "each Versatile processor contributes one to all four counters");
        helper.assertTrue(allVersatile.routingThreshold() == 46784
                        && allVersatile.energyCapacity() == 186752
                        && allVersatile.fixedEnergyPerPass() == 128
                        && LogisticsCoreStructure.range(allVersatile) == 29,
                "threshold, buffer, fixed cost per 20-tick pass and network radius are distinct GT6 values");
        var cpuCell = LogisticsCoreStructure.cells().stream()
                .filter(c -> c.kind() == LogisticsCoreStructure.Kind.CPU).findFirst().orElseThrow();
        BlockPos cpu = cpuCell.at(core.getBlockPos(), Direction.EAST);
        helper.getLevel().setBlock(cpu, LargeMachineParts.block(LogisticsCoreStructure.LOGIC).defaultBlockState(), 3);
        helper.assertTrue(core.processorCounts().equals(new LogisticsCoreStructure.Counts(30, 26, 26, 26)),
                "Logic Quadcore contributes four logic in place of one Versatile");
        helper.getLevel().setBlock(cpu, LargeMachineParts.block(LogisticsCoreStructure.WALL).defaultBlockState(), 3);
        helper.assertTrue(core.processorCounts().equals(new LogisticsCoreStructure.Counts(26, 26, 26, 26)),
                "cheap interior wall contributes no processors");
        helper.getLevel().setBlock(cpu, Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(!core.isStructureOk(), "missing interior processor or wall breaks the structure");
        helper.succeed();
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void galvanizedWallsAreTheOnlyEuInputs(GameTestHelper helper) {
        var core = build(helper, Direction.NORTH);
        var wall = part(core, Direction.NORTH, LogisticsCoreStructure.Kind.WALL);
        var vent = part(core, Direction.NORTH, LogisticsCoreStructure.Kind.VENT);
        helper.assertTrue(wall.isEnergyAcceptingFrom(GregTechTags.Energy.EU, Direction.UP, false)
                        && !vent.isEnergyAcceptingFrom(GregTechTags.Energy.EU, Direction.UP, false)
                        && !core.isEnergyAcceptingFrom(GregTechTags.Energy.EU, Direction.NORTH, false),
                "only the 44 galvanized wall parts admit EU");
        helper.assertTrue(wall.getEnergySizeInputMin(GregTechTags.Energy.EU, Direction.UP) == 256
                        && wall.getEnergySizeInputRecommended(GregTechTags.Energy.EU, Direction.UP) == 512
                        && wall.getEnergySizeInputMax(GregTechTags.Energy.EU, Direction.UP) == 1024
                        && wall.getEnergyDemanded(GregTechTags.Energy.EU, Direction.UP, 512) == 1024,
                "GT6 EU packet range and fixed demanded-packet count");
        helper.assertTrue(wall.doEnergyInjection(GregTechTags.Energy.LU, Direction.UP, 512, 1, true) == 0,
                "wall does not accept laser energy");
        helper.assertTrue(wall.doEnergyInjection(GregTechTags.Energy.EU, Direction.UP, 512, 2, false) == 2
                        && core.storedEU() == 0, "simulation is inert");
        helper.assertTrue(wall.doEnergyInjection(GregTechTags.Energy.EU, Direction.UP, 512, 2, true) == 2
                        && core.storedEU() == 1024, "actual wall injection charges the controller");
        BlockPos broken = wall.getBlockPos();
        helper.getLevel().setBlock(broken, Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(!core.isStructureOk() && !wall.isEnergyAcceptingFrom(GregTechTags.Energy.EU, Direction.UP, false),
                "breaking one wall immediately unforms and releases its port");
        helper.getLevel().setBlock(broken, LargeMachineParts.block(LogisticsCoreStructure.WALL).defaultBlockState(), 3);
        helper.assertTrue(core.isStructureOk(), "repairing the wall reforms the machine");
        helper.succeed();
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void fixedEuChargeFollowsOriginalSecondGate(GameTestHelper helper) {
        var core = build(helper, Direction.NORTH);
        var wall = part(core, Direction.NORTH, LogisticsCoreStructure.Kind.WALL);
        int untilMidSecond = (10 - (int) (helper.getLevel().getGameTime() % 20) + 20) % 20;
        if (untilMidSecond == 0) untilMidSecond = 20;
        helper.runAfterDelay(untilMidSecond, () -> {
            helper.assertTrue(wall.doEnergyInjection(GregTechTags.Energy.EU, Direction.UP, 512, 2, true) == 2,
                    "wall accepts two packets at mid-second");
            helper.runAfterDelay(9, () ->
                    helper.assertTrue(core.storedEU() == 1024, "fixed charge waits for the next second pass"));
            helper.runAfterDelay(11, () -> {
                helper.assertTrue(core.storedEU() == 896, "one second pass charges 128 EU exactly once");
                helper.getLevel().setBlock(wall.getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
                helper.assertTrue(!core.isStructureOk(), "broken wall invalidates the core");
                helper.runAfterDelay(20, () -> {
                    helper.assertTrue(core.storedEU() == 876,
                            "unformed core still pays GT6's 20 EU fixed charge each second");
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void chargePersistsAndOriginalRecipeLoads(GameTestHelper helper) {
        var core = build(helper, Direction.SOUTH);
        var wall = part(core, Direction.SOUTH, LogisticsCoreStructure.Kind.WALL);
        wall.doEnergyInjection(GregTechTags.Energy.EU, Direction.UP, 512, 3, true);
        var saved = core.saveWithoutMetadata();
        var restored = new LogisticsCoreControllerBlockEntity(core.getBlockPos(), core.getBlockState());
        restored.load(saved);
        helper.assertTrue(restored.storedEU() == 1536, "stored EU survives an NBT round trip");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                "gregtech", "machines/multiblock/logistics_core")).isPresent(),
                "GT6 CCC/PMP/CCC Logistics Core recipe resolves in the generated pack");
        helper.succeed();
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void ceilingFloorAndWrenchUseTheSixFaceStructure(GameTestHelper helper) {
        var block = (LogisticsCoreControllerBlock) GTLasers.LOGISTICS_CORE.get();
        var wrench = com.gregtech.gregtech.item.GTToolItem.create(
                com.gregtech.gregtech.api.tool.GTToolType.WRENCH,
                com.gregtech.gregtech.content.material.Materials.Steel,
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
        for (Direction side : Direction.values())
            helper.assertTrue(block.toolInteraction(block.defaultBlockState(), wrench)
                    .allows(block.defaultBlockState(), side), "wrench selects " + side + " on original six-faced core");
        var floor = build(helper, Direction.DOWN);
        helper.assertTrue(floor.processorCounts().valid(), "controller forms on the bottom face");
        var ceiling = build(helper, Direction.UP);
        helper.assertTrue(ceiling.processorCounts().valid(), "controller forms on the top face");
        helper.succeed();
    }
}
