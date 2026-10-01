package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.machine.LogisticsWireBlock;
import com.gregtech.gregtech.blockentity.machine.LogisticsCoreControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LogisticsWireBlockEntity;
import com.gregtech.gregtech.content.logistics.LogisticsCoreStructure;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.registry.GTIconSetBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** W2: original Logistics Wire connection/range and fixed private buffers. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class LogisticsNetworkTests {
    static LogisticsCoreControllerBlockEntity minimumCore(GameTestHelper helper) {
        var core = LogisticsCoreTests.build(helper, Direction.NORTH);
        boolean keep = true;
        for (var cell : LogisticsCoreStructure.cells()) if (cell.kind() == LogisticsCoreStructure.Kind.CPU) {
            if (keep) { keep = false; continue; }
            helper.getLevel().setBlock(cell.at(core.getBlockPos(), Direction.NORTH),
                    LargeMachineParts.block(LogisticsCoreStructure.WALL).defaultBlockState(), 3);
        }
        helper.assertTrue(core.processorCounts().equals(new LogisticsCoreStructure.Counts(1, 1, 1, 1)),
                "one Versatile processor and 26 cheap walls form a minimal core");
        return core;
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void mutualWireConnectionsAndControlRadius(GameTestHelper helper) {
        var core = minimumCore(helper);
        BlockPos center = core.getBlockPos().south(2);
        BlockPos first = center.offset(3, 2, 0);
        BlockPos second = center.offset(4, 2, 0);
        var wire = (LogisticsWireBlock) GTIconSetBlocks.LOGISTICS_WIRE.get();
        helper.getLevel().setBlock(first, wire.defaultBlockState()
                .setValue(ElectricWireBlock.WEST, true), 3);
        helper.getLevel().setBlock(second, wire.defaultBlockState()
                .setValue(ElectricWireBlock.WEST, true), 3);
        helper.assertTrue(helper.getLevel().getBlockState(first).getValue(ElectricWireBlock.EAST),
                "placing the second wire connects the first reciprocally");
        var firstHost = (LogisticsWireBlockEntity) helper.getLevel().getBlockEntity(first);
        helper.assertTrue(firstHost.canLogistics(Direction.WEST) && firstHost.canLogistics(Direction.EAST)
                        && !firstHost.canLogistics(Direction.NORTH),
                "wire advertises only its actual connected faces");
        var shortRange = core.scanNetwork();
        helper.assertTrue(shortRange.contains(first) && !shortRange.contains(second)
                        && shortRange.farthestDistance() == 3 && shortRange.size() == 126,
                "one control CPU gives Chebyshev radius three from cube centre");

        var cpu = LogisticsCoreStructure.cells().stream().filter(c -> c.kind() == LogisticsCoreStructure.Kind.CPU)
                .skip(1).findFirst().orElseThrow().at(core.getBlockPos(), Direction.NORTH);
        helper.assertTrue(shortRange.contains(cpu)
                        && !((com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity)
                        helper.getLevel().getBlockEntity(cpu)).canLogistics(Direction.NORTH),
                "GT6 seeds interior parts in the initial cube but closed processor faces do not relay");
        helper.getLevel().setBlock(cpu, LargeMachineParts.block(LogisticsCoreStructure.CONTROL).defaultBlockState(), 3);
        helper.assertTrue(core.processorCounts().control() == 5 && core.scanNetwork().contains(second),
                "Quadcore control adds four metres of cubic network range");
        helper.getLevel().setBlock(first, helper.getLevel().getBlockState(first)
                .setValue(ElectricWireBlock.WEST, false), 3);
        helper.assertTrue(!core.scanNetwork().contains(first) && !core.networkSnapshot().contains(second),
                "a severed wire side prevents traversal even if downstream wires still point back");
        helper.succeed();
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void fixedItemAndFluidBuffersPersist(GameTestHelper helper) {
        var core = minimumCore(helper);
        helper.assertTrue(core.bufferItems().getSlots() == 108 && core.bufferTankCount() == 108
                        && core.bufferTank(0).getTankCapacity(0) == 16_000
                        && core.bufferTank(107).getTankCapacity(0) == 16_000,
                "GT6 retains 108 item slots and 108 16000-L tanks even with one Storage processor");
        core.bufferItems().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
        core.bufferItems().setStackInSlot(107, new ItemStack(Items.APPLE, 3));
        core.bufferTank(0).fill(new FluidStack(Fluids.WATER, 16_000), IFluidHandler.FluidAction.EXECUTE);
        core.bufferTank(107).fill(new FluidStack(Fluids.WATER, 8000), IFluidHandler.FluidAction.EXECUTE);
        var saved = core.saveWithoutMetadata();
        var restored = new LogisticsCoreControllerBlockEntity(core.getBlockPos(), core.getBlockState());
        restored.load(saved);
        helper.assertTrue(restored.bufferItems().getStackInSlot(0).getCount() == 7
                        && restored.bufferItems().getStackInSlot(107).getCount() == 3,
                "both ends of the fixed inventory survive save/load");
        helper.assertTrue(restored.bufferTank(0).getFluidInTank(0).getAmount() == 16_000
                        && restored.bufferTank(107).getFluidInTank(0).getAmount() == 8000,
                "both ends of the fixed tank array survive save/load");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                "gregtech", "machines/multiblock/logistics_wire")).isPresent(),
                "GT6 PEP/dFx/POP Logistics Wire recipe is available in survival");
        helper.succeed();
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void breakingCoreDropsPrivateItemBuffer(GameTestHelper helper) {
        var core = minimumCore(helper);
        core.bufferItems().setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));
        BlockPos pos = core.getBlockPos();
        helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(core.bufferItems().getStackInSlot(0).isEmpty(), "private contents were removed from the BE");
        helper.runAfterDelay(1, () -> {
            var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(3));
            helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(Items.DIAMOND)
                            && item.getItem().getCount() == 5), "stored items drop when the core is broken");
            helper.succeed();
        });
    }
}
