package gregtech.gametest;

import gregtech.internal.energy.FaceConfig;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.blockentity.machine.LargeRecipeMachineBlockEntity;
import gregtech.content.multiblock.LargeMachineLayouts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Actual remote chunk borders; no loadedness mocks or copied machine scheduler. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class MachineChunkBoundaryTests {
    private static void set(Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        // CLIENTS alone still propagates shapes through neighbors. The fixture already knows
        // these full-cube machine/casing states; skip that vanilla shape walk, not loadedness checks.
        level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    private static BasicMachineBlockEntity place(GameTestHelper h, String name, BlockPos pos) {
        var block = (BasicMachineBlock) BuiltInRegistries.BLOCK.stream()
                .filter(b -> b instanceof BasicMachineBlock machine && machine.basicSpec().machineName().equals(name))
                .findFirst().orElseThrow();
        set(h.getLevel(), pos, block.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        return (BasicMachineBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    private static void auto(BasicMachineBlockEntity machine, String name) {
        try {
            var method = BasicMachineBlockEntity.class.getDeclaredMethod(name, Level.class, BlockPos.class, int.class);
            method.setAccessible(true);
            method.invoke(machine, machine.getLevel(), machine.getBlockPos(), FaceConfig.FRONT);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Actual auto I/O failed: " + name, failure);
        }
    }

    @GameTest(template="test_empty", timeoutTicks=200)
    public static void formedLargeMachinePollingDoesNotLoadExteriorNeighbor(GameTestHelper h) {
        for (int sign : new int[]{1, -1}) {
            // Both z values are chunk-aligned. North crosses into the preceding chunk even below zero.
            var pos = new BlockPos(19000008, 80, sign * 19000000);
            var outside = pos.north();
            h.assertTrue(!h.getLevel().hasChunkAt(outside), "remote exterior starts unloaded");
            var machine = (LargeRecipeMachineBlockEntity) place(h, "largecentrifuge", pos);
            var cells = LargeMachineLayouts.cells("largecentrifuge");
            for (var cell : cells) set(h.getLevel(), cell.at(pos, Direction.NORTH), cell.block().defaultBlockState());
            h.assertTrue(StructureTestSupport.ok(machine), "registered large centrifuge forms entirely inside its own chunk");
            h.assertTrue(!h.getLevel().hasChunkAt(outside), "shell construction did not load exterior");
            BasicMachineBlockEntity.serverTick(h.getLevel(), pos, machine.getBlockState(), machine);
            h.assertTrue(!h.getLevel().hasChunkAt(outside), "real inherited machine tick must not load exterior for energy polling");
            h.assertTrue(StructureTestSupport.ok(machine), "skipping exterior does not break loaded shell");
            set(h.getLevel(), pos, Blocks.AIR.defaultBlockState());
            for (var cell : cells) set(h.getLevel(), cell.at(pos, Direction.NORTH), Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }

    @GameTest(template="test_empty", timeoutTicks=200)
    public static void unloadedAutomaticIoRetainsStockThenTransfersToLoadedChest(GameTestHelper h) {
        var pos = new BlockPos(-19000008, 80, -19000000);
        var outside = pos.north();
        h.assertTrue(!h.getLevel().hasChunkAt(outside), "remote destination starts unloaded");
        var machine = place(h, "crusher", pos);
        h.assertTrue(!h.getLevel().hasChunkAt(outside), "machine placement did not load neighboring chunk");
        int output = machine.inputSlots();
        machine.inventory().setStackInSlot(output, new ItemStack(Items.STICK, 3));
        for (String name : new String[]{"autoInputItems", "autoInputFluids", "autoOutputFluids", "autoOutputItems"}) {
            auto(machine, name);
            h.assertTrue(!h.getLevel().hasChunkAt(outside), "actual " + name + " must not load an absent neighbor");
            h.assertTrue(machine.inventory().getStackInSlot(output).getCount() == 3,
                    "unavailable neighbor is not empty air; stock remains owned by machine");
        }
        set(h.getLevel(), outside, Blocks.CHEST.defaultBlockState());
        var chest = (ChestBlockEntity) h.getLevel().getBlockEntity(outside);
        h.assertTrue(chest != null && h.getLevel().hasChunkAt(outside), "explicitly placed loaded destination");
        auto(machine, "autoOutputItems");
        int count = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) count += chest.getItem(slot).getCount();
        h.assertTrue(count == 3 && machine.inventory().getStackInSlot(output).isEmpty(), "loaded destination still receives exact stock");
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(outside).inflate(1)).isEmpty(),
                "no deferred air ejection occurred across unavailable border");
        set(h.getLevel(), pos, Blocks.AIR.defaultBlockState());
        set(h.getLevel(), outside, Blocks.AIR.defaultBlockState());
        h.succeed();
    }
}
