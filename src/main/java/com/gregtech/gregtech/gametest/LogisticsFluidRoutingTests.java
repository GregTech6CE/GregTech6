package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.machine.LogisticsWireBlock;
import com.gregtech.gregtech.blockentity.machine.LogisticsCoreControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LogisticsWireBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.content.logistics.LogisticsCoreStructure;
import com.gregtech.gregtech.content.logistics.LogisticsCoverInteraction;
import com.gregtech.gregtech.content.logistics.LogisticsCoverType;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTIconSetBlocks;
import com.gregtech.gregtech.registry.GTTanks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** End-to-end fluid logistics through actual covers, sided drum capabilities and the powered core. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class LogisticsFluidRoutingTests {
    private LogisticsFluidRoutingTests() {}

    private static ItemStack cover(LogisticsCoverType type) {
        var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", type.id()));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static LogisticsWireBlockEntity wire(GameTestHelper helper, BlockPos pos, Direction inward) {
        var block = (LogisticsWireBlock) GTIconSetBlocks.LOGISTICS_WIRE.get();
        helper.getLevel().setBlock(pos, block.defaultBlockState()
                .setValue(ElectricWireBlock.propFor(inward), true), 3);
        return (LogisticsWireBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static TankBlockEntity drum(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlock(pos, GTTanks.DRUM_STEEL.get().defaultBlockState(), 3);
        return (TankBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static void charge(GameTestHelper helper, LogisticsCoreControllerBlockEntity core) {
        var wallCell = LogisticsCoreStructure.cells().stream()
                .filter(cell -> cell.kind() == LogisticsCoreStructure.Kind.WALL).findFirst().orElseThrow();
        var wall = (MultiblockPortBlockEntity) helper.getLevel().getBlockEntity(
                wallCell.at(core.getBlockPos(), Direction.NORTH));
        helper.assertTrue(wall.doEnergyInjection(GregTechTags.Energy.EU, Direction.UP, 512, 1, true) == 1,
                "galvanized wall supplies the routing threshold and transfer cost");
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void filteredFluidRejectsLavaThenMovesTaggedWaterAtGt6Limit(GameTestHelper helper) {
        var core = LogisticsNetworkTests.minimumCore(helper);
        BlockPos center = core.getBlockPos().south(2);
        BlockPos sourceWirePos = center.offset(3, 0, 0);
        BlockPos targetWirePos = center.offset(-3, 0, 0);
        var sourceWire = wire(helper, sourceWirePos, Direction.WEST);
        var targetWire = wire(helper, targetWirePos, Direction.EAST);
        var source = drum(helper, sourceWirePos.east());
        var target = drum(helper, targetWirePos.west());
        helper.assertTrue(sourceWire.logisticsCovers().attach(Direction.EAST,
                        cover(LogisticsCoverType.FLUID_IMPORT))
                        && targetWire.logisticsCovers().attach(Direction.WEST,
                        cover(LogisticsCoverType.FLUID_EXPORT)),
                "the original filtered fluid buses attach to externally facing wire sides");

        FluidStack water = new FluidStack(Fluids.WATER, 20_000);
        water.getOrCreateTag().putString("origin", "source drum");
        helper.assertTrue(source.getFluidTank().fill(water, IFluidHandler.FluidAction.EXECUTE) == 20_000,
                "steel drum accepts the tagged 20,000 mB source fixture");
        FluidStack waterTemplate = new FluidStack(Fluids.WATER, 1000);
        waterTemplate.getOrCreateTag().putString("template", "different NBT");
        helper.assertTrue(LogisticsCoverInteraction.setFluidFilter(sourceWire.logisticsCovers(),
                        Direction.EAST, waterTemplate)
                        && !LogisticsCoverInteraction.setFluidFilter(sourceWire.logisticsCovers(),
                        Direction.EAST, new FluidStack(Fluids.LAVA, 1000))
                        && LogisticsCoverInteraction.setFluidFilter(targetWire.logisticsCovers(),
                        Direction.WEST, new FluidStack(Fluids.LAVA, 1000)),
                "fluid filter is a one-shot sample and ignores only sample amount/NBT");

        var restored = new LogisticsWireBlockEntity(sourceWirePos, sourceWire.getBlockState());
        restored.load(sourceWire.saveWithoutMetadata());
        FluidStack restoredFilter = LogisticsCoverInteraction.fluidFilter(restored.logisticsCovers()
                .get(Direction.EAST));
        helper.assertTrue(restoredFilter.getFluid() == Fluids.WATER && restoredFilter.hasTag()
                        && "different NBT".equals(restoredFilter.getTag().getString("template")),
                "the water sample and its NBT persist with the installed cover");
        charge(helper, core);
        helper.assertTrue(core.scanNetwork().contains(sourceWirePos)
                        && core.scanNetwork().contains(targetWirePos),
                "both filtered fluid buses belong to the formed core's network");

        var blocked = core.routeNetworkOnce();
        helper.assertTrue(blocked.fluidMoved() == 0 && source.getFluidTank().getAmount() == 20_000
                        && target.getFluidTank().isEmpty() && core.storedEU() == 512,
                "opposed water/lava filters cannot consume source fluid or EU");
        helper.assertTrue(LogisticsCoverInteraction.clearFluidFilter(targetWire.logisticsCovers(), Direction.WEST)
                        && LogisticsCoverInteraction.setFluidFilter(targetWire.logisticsCovers(),
                        Direction.WEST, waterTemplate),
                "a reset fluid export accepts a new water sample");

        var first = core.routeNetworkOnce();
        FluidStack arrived = target.getFluidTank().getFluid();
        helper.assertTrue(first.fluidMoved() == 16_000 && first.itemsMoved() == 0
                        && first.operations() == 1 && first.energyCost() == 64
                        && source.getFluidTank().getAmount() == 4000
                        && arrived.getFluid() == Fluids.WATER && arrived.getAmount() == 16_000
                        && arrived.hasTag() && "source drum".equals(arrived.getTag().getString("origin"))
                        && core.storedEU() == 448,
                "one conversion CPU moves at most 16,000 mB, preserves cargo NBT and pays ceil(mB/250) EU");
        var second = core.routeNetworkOnce();
        helper.assertTrue(second.fluidMoved() == 4000 && second.energyCost() == 16
                        && source.getFluidTank().isEmpty() && target.getFluidTank().getAmount() == 20_000
                        && core.storedEU() == 432,
                "a later logic pass transfers the remaining 4,000 mB for 16 EU");
        helper.succeed();
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void genericFluidWinsEqualPriorityButHigherPriorityItemWins(GameTestHelper helper) {
        var core = LogisticsNetworkTests.minimumCore(helper);
        BlockPos center = core.getBlockPos().south(2);
        BlockPos fluidImportPos = center.offset(3, 0, 0);
        BlockPos fluidExportPos = center.offset(-3, 0, 0);
        BlockPos itemImportPos = center.offset(3, 1, 0);
        BlockPos itemExportPos = center.offset(-3, 1, 0);
        var fluidImport = wire(helper, fluidImportPos, Direction.WEST);
        var fluidExport = wire(helper, fluidExportPos, Direction.EAST);
        var itemImport = wire(helper, itemImportPos, Direction.WEST);
        var itemExport = wire(helper, itemExportPos, Direction.EAST);
        var source = drum(helper, fluidImportPos.east());
        var target = drum(helper, fluidExportPos.west());
        helper.getLevel().setBlock(itemImportPos.east(), Blocks.CHEST.defaultBlockState(), 3);
        helper.getLevel().setBlock(itemExportPos.west(), Blocks.CHEST.defaultBlockState(), 3);
        var sourceChest = (ChestBlockEntity) helper.getLevel().getBlockEntity(itemImportPos.east());
        var targetChest = (ChestBlockEntity) helper.getLevel().getBlockEntity(itemExportPos.west());
        helper.assertTrue(fluidImport.logisticsCovers().attach(Direction.EAST,
                        cover(LogisticsCoverType.GENERIC_IMPORT))
                        && fluidExport.logisticsCovers().attach(Direction.WEST,
                        cover(LogisticsCoverType.GENERIC_EXPORT))
                        && itemImport.logisticsCovers().attach(Direction.EAST,
                        cover(LogisticsCoverType.GENERIC_IMPORT))
                        && itemExport.logisticsCovers().attach(Direction.WEST,
                        cover(LogisticsCoverType.GENERIC_EXPORT)),
                "generic buses expose fluid and item channels on real sided inventories");
        helper.assertTrue(source.getFluidTank().fill(new FluidStack(Fluids.WATER, 500),
                        IFluidHandler.FluidAction.EXECUTE) == 500,
                "source drum holds a partial bucket");
        sourceChest.setItem(0, new ItemStack(Items.DIAMOND, 5));
        charge(helper, core);

        var sameRank = core.routeNetworkOnce();
        helper.assertTrue(sameRank.fluidMoved() == 500 && sameRank.itemsMoved() == 0
                        && sameRank.operations() == 1 && sameRank.energyCost() == 2
                        && target.getFluidTank().getAmount() == 500
                        && sourceChest.getItem(0).getCount() == 5 && targetChest.getItem(0).isEmpty()
                        && core.storedEU() == 510,
                "GT6 routes fluids before items when both generic exports have equal priority");

        helper.assertTrue(source.getFluidTank().fill(new FluidStack(Fluids.WATER, 500),
                        IFluidHandler.FluidAction.EXECUTE) == 500,
                "a new 500 mB batch is available for the priority comparison");
        for (int i = 0; i < 3; i++)
            helper.assertTrue(LogisticsCoverInteraction.cyclePriority(itemExport.logisticsCovers(),
                    Direction.WEST), "screwdriver promotes the item export to filtered-rank priority");
        var prioritized = core.routeNetworkOnce();
        helper.assertTrue(prioritized.fluidMoved() == 0 && prioritized.itemsMoved() == 5
                        && prioritized.operations() == 1 && prioritized.energyCost() == 5
                        && source.getFluidTank().getAmount() == 500
                        && target.getFluidTank().getAmount() == 500
                        && sourceChest.getItem(0).isEmpty() && targetChest.getItem(0).getCount() == 5
                        && core.storedEU() == 505,
                "a higher-priority item export takes the next logic pass before generic fluid export");
        helper.succeed();
    }
}
