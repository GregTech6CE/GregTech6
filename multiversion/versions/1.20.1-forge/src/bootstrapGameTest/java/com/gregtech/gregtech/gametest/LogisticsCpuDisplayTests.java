package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.machine.LogisticsWireBlock;
import com.gregtech.gregtech.blockentity.machine.LogisticsWireBlockEntity;
import com.gregtech.gregtech.content.logistics.LogisticsCoverType;
import com.gregtech.gregtech.content.logistics.LogisticsCpuDisplay;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTIconSetBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** GT6 CPU meters show last second's work, persist visuals and emit strong/weak redstone. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class LogisticsCpuDisplayTests {
    private static ItemStack cover(LogisticsCoverType type) {
        var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", type.id()));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void previousPassDrivesAllFourMetersAndEmitsRedstone(GameTestHelper helper) {
        helper.assertTrue(LogisticsCpuDisplay.signal(0, 27) == 0
                        && LogisticsCpuDisplay.visual(0, 27) == 0
                        && LogisticsCpuDisplay.signal(1, 27) == 1
                        && LogisticsCpuDisplay.visual(1, 27) == 1
                        && LogisticsCpuDisplay.signal(27, 27) == 15
                        && LogisticsCpuDisplay.visual(27, 27) == 10,
                "GT6's independent 0..15 redstone and 0..10 texture formulas");

        var core = LogisticsNetworkTests.minimumCore(helper);
        var faces = new Direction[]{Direction.UP, Direction.DOWN, Direction.WEST, Direction.EAST};
        var types = new LogisticsCoverType[]{LogisticsCoverType.CPU_LOGIC,
                LogisticsCoverType.CPU_CONTROL, LogisticsCoverType.CPU_STORAGE,
                LogisticsCoverType.CPU_CONVERSION};
        for (int i = 0; i < faces.length; i++)
            helper.assertTrue(core.logisticsCovers().attach(faces[i], cover(types[i])),
                    "CPU display " + types[i] + " attaches to the core");

        BlockPos center = core.getBlockPos().south(2);
        var wire = (LogisticsWireBlock) GTIconSetBlocks.LOGISTICS_WIRE.get();
        BlockPos sourcePos = center.offset(3, 0, 0), sourceChestPos = center.offset(4, 0, 0);
        BlockPos outputPos = center.offset(-3, 0, 0), outputChestPos = center.offset(-4, 0, 0);
        helper.getLevel().setBlock(sourcePos,
                wire.defaultBlockState().setValue(ElectricWireBlock.WEST, true), 3);
        helper.getLevel().setBlock(outputPos,
                wire.defaultBlockState().setValue(ElectricWireBlock.EAST, true), 3);
        helper.getLevel().setBlock(sourceChestPos, Blocks.CHEST.defaultBlockState(), 3);
        helper.getLevel().setBlock(outputChestPos, Blocks.CHEST.defaultBlockState(), 3);
        var source = (LogisticsWireBlockEntity) helper.getLevel().getBlockEntity(sourcePos);
        var output = (LogisticsWireBlockEntity) helper.getLevel().getBlockEntity(outputPos);
        helper.assertTrue(source.logisticsCovers().attach(Direction.EAST,
                        cover(LogisticsCoverType.GENERIC_IMPORT))
                        && output.logisticsCovers().attach(Direction.WEST,
                        cover(LogisticsCoverType.GENERIC_EXPORT)), "routing covers installed");
        ((ChestBlockEntity) helper.getLevel().getBlockEntity(sourceChestPos))
                .setItem(0, new ItemStack(Items.DIAMOND));

        var wallCell = com.gregtech.gregtech.content.logistics.LogisticsCoreStructure.cells().stream()
                .filter(cell -> cell.kind() == com.gregtech.gregtech.content.logistics.LogisticsCoreStructure.Kind.WALL)
                .findFirst().orElseThrow();
        var port = (com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity)
                helper.getLevel().getBlockEntity(wallCell.at(core.getBlockPos(), Direction.NORTH));
        helper.assertTrue(port.doEnergyInjection(GregTechTags.Energy.EU, Direction.UP, 512, 2, true) == 2,
                "galvanized wall powers CPU meter test");
        var result = core.routeNetworkOnce();
        helper.assertTrue(result.itemsMoved() == 1 && result.usedLogic() == 1
                        && result.usedConversion() == 1
                        && core.usedProcessorCounts().control() == 1
                        && core.usedProcessorCounts().storage() == 0,
                "item route records one logic and conversion CPU, a control radius and no storage use");

        var testedCore = core;
        int untilNextPass = (20 - (int) (helper.getLevel().getGameTime() % 20)) % 20;
        if (untilNextPass == 0) untilNextPass = 20;
        helper.runAfterDelay(untilNextPass + 1, () -> {
            for (int i = 0; i < faces.length; i++) {
                int expected = types[i] == LogisticsCoverType.CPU_STORAGE ? 0 : 15;
                int frame = expected == 0 ? 0 : 10;
                helper.assertTrue(testedCore.logisticsCovers().displaySignal(faces[i]) == expected
                                && testedCore.logisticsCovers().displayVisual(faces[i]) == frame
                                && testedCore.getBlockState().getBlock().getSignal(
                                testedCore.getBlockState(), helper.getLevel(), testedCore.getBlockPos(), faces[i])
                                == expected
                                && testedCore.getBlockState().getBlock().getDirectSignal(
                                testedCore.getBlockState(), helper.getLevel(), testedCore.getBlockPos(), faces[i])
                                == expected,
                        types[i] + " shows the previous pass and emits equal weak/direct power");
            }
            var saved = testedCore.saveWithoutMetadata();
            var restored = new com.gregtech.gregtech.blockentity.machine.LogisticsCoreControllerBlockEntity(
                    testedCore.getBlockPos(), testedCore.getBlockState());
            restored.load(saved);
            helper.assertTrue(restored.logisticsCovers().displayVisual(Direction.UP) == 10
                            && restored.logisticsCovers().displaySignal(Direction.DOWN) == 15
                            && restored.usedProcessorCounts().storage() == 0,
                    "CPU cover status and used counters survive a block-entity NBT round trip");
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(testedCore.logisticsCovers().displaySignal(Direction.UP) == 0
                                && testedCore.logisticsCovers().displayVisual(Direction.EAST) == 0
                                && testedCore.logisticsCovers().displaySignal(Direction.DOWN) == 15,
                        "idle logic/conversion meters clear next second while scanned control remains active");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void wireDisplayPersistsAndSynchronizesItsVisual(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(5, 4, 5));
        var wire = (LogisticsWireBlock) GTIconSetBlocks.LOGISTICS_WIRE.get();
        helper.getLevel().setBlock(pos, wire.defaultBlockState(), 3);
        var entity = (LogisticsWireBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity.logisticsCovers().attach(Direction.NORTH,
                cover(LogisticsCoverType.CPU_CONVERSION)), "meter attaches to real logistics wire");
        entity.logisticsCovers().setDisplay(Direction.NORTH, 2, 4);
        int power = LogisticsCpuDisplay.signal(2, 4);
        int visual = LogisticsCpuDisplay.visual(2, 4);
        helper.assertTrue(power > 0 && power < 15 && visual > 0 && visual < 10
                        && wire.getSignal(entity.getBlockState(), helper.getLevel(), pos, Direction.NORTH) == power,
                "partial load has distinct original redstone and visual levels");
        var reloaded = new LogisticsWireBlockEntity(pos, entity.getBlockState());
        reloaded.load(entity.saveWithoutMetadata());
        helper.assertTrue(reloaded.logisticsCovers().displaySignal(Direction.NORTH) == power
                        && reloaded.logisticsCovers().displayVisual(Direction.NORTH) == visual,
                "signal and visual survive disk NBT");
        var synced = new LogisticsWireBlockEntity(pos, entity.getBlockState());
        synced.handleUpdateTag(entity.getUpdateTag());
        helper.assertTrue(synced.logisticsCovers().displayVisual(Direction.NORTH) == visual
                        && entity.getUpdatePacket() != null,
                "the same visual is included in the client block-entity update");
        helper.succeed();
    }
}
