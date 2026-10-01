package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.machine.LogisticsWireBlock;
import com.gregtech.gregtech.blockentity.machine.LogisticsCoreControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LogisticsWireBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity;
import com.gregtech.gregtech.content.logistics.LogisticsCoreStructure;
import com.gregtech.gregtech.content.logistics.LogisticsCoverInteraction;
import com.gregtech.gregtech.content.logistics.LogisticsCoverType;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTIconSetBlocks;
import com.gregtech.gregtech.registry.GTTanks;
import com.gregtech.gregtech.registry.GTStorage;
import com.gregtech.gregtech.registry.GTItems;
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

/** Original GT6 routing phases after the direct Import→Export pair. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class LogisticsStorageRoutingTests {
    private LogisticsStorageRoutingTests() {}

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
                "core receives a 512 EU packet through its galvanized wall");
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void importStoresFluidThenStorageExportsIt(GameTestHelper helper) {
        var core = LogisticsNetworkTests.minimumCore(helper);
        BlockPos center = core.getBlockPos().south(2);
        BlockPos importPos = center.offset(3, 0, 0), storagePos = center.offset(-3, 0, 0);
        var importWire = wire(helper, importPos, Direction.WEST);
        var storageWire = wire(helper, storagePos, Direction.EAST);
        var source = drum(helper, importPos.east());
        var storage = drum(helper, storagePos.west());
        helper.assertTrue(importWire.logisticsCovers().attach(Direction.EAST, cover(LogisticsCoverType.GENERIC_IMPORT))
                        && storageWire.logisticsCovers().attach(Direction.WEST, cover(LogisticsCoverType.FLUID_STORAGE))
                        && LogisticsCoverInteraction.setFluidFilter(storageWire.logisticsCovers(), Direction.WEST,
                        new FluidStack(Fluids.WATER, 1000)),
                "generic import and sampled water storage bus attach to the network");
        helper.assertTrue(source.getFluidTank().fill(new FluidStack(Fluids.WATER, 1000),
                IFluidHandler.FluidAction.EXECUTE) == 1000, "source holds one bucket");
        charge(helper, core);
        var first = core.routeNetworkOnce();
        helper.assertTrue(first.fluidMoved() == 1000 && first.energyCost() == 4
                        && source.getFluidTank().isEmpty() && storage.getFluidTank().getAmount() == 1000,
                "after direct export fails, import fills filtered storage for ceil(1000/250) EU");

        importWire.logisticsCovers().remove(Direction.EAST);
        helper.assertTrue(importWire.logisticsCovers().attach(Direction.EAST, cover(LogisticsCoverType.GENERIC_EXPORT)),
                "the former source can now act as the empty export destination");
        var second = core.routeNetworkOnce();
        helper.assertTrue(second.fluidMoved() == 1000 && second.energyCost() == 4
                        && source.getFluidTank().getAmount() == 1000 && storage.getFluidTank().isEmpty()
                        && core.storedEU() == 504,
                "storage feeds the export in GT6's second routing phase without deleting fluid");
        helper.succeed();
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void genericFluidStorageDefragmentsIntoFilteredStorage(GameTestHelper helper) {
        var core = LogisticsNetworkTests.minimumCore(helper);
        BlockPos center = core.getBlockPos().south(2);
        BlockPos sourcePos = center.offset(3, 0, 0), destinationPos = center.offset(-3, 0, 0);
        var sourceWire = wire(helper, sourcePos, Direction.WEST);
        var destinationWire = wire(helper, destinationPos, Direction.EAST);
        var source = drum(helper, sourcePos.east());
        var destination = drum(helper, destinationPos.west());
        helper.assertTrue(sourceWire.logisticsCovers().attach(Direction.EAST, cover(LogisticsCoverType.GENERIC_STORAGE))
                        && destinationWire.logisticsCovers().attach(Direction.WEST, cover(LogisticsCoverType.FLUID_STORAGE))
                        && LogisticsCoverInteraction.setFluidFilter(destinationWire.logisticsCovers(), Direction.WEST,
                        new FluidStack(Fluids.WATER, 1000)), "generic and filtered storage buses attach");
        source.getFluidTank().fill(new FluidStack(Fluids.WATER, 750), IFluidHandler.FluidAction.EXECUTE);
        charge(helper, core);
        var result = core.routeNetworkOnce();
        helper.assertTrue(result.fluidMoved() == 750 && result.energyCost() == 3
                        && source.getFluidTank().isEmpty() && destination.getFluidTank().getAmount() == 750,
                "GT6 defragmentation promotes generic stored fluid into matching filtered storage");
        helper.succeed();
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void exportPrecedesFilteredStorageEvenAtLowerPriority(GameTestHelper helper) {
        var core = LogisticsNetworkTests.minimumCore(helper);
        BlockPos center = core.getBlockPos().south(2);
        BlockPos sourcePos = center.offset(3, 0, 0), exportPos = center.offset(-3, 0, 0),
                storagePos = center.offset(-3, 1, 0);
        var sourceWire = wire(helper, sourcePos, Direction.WEST);
        var exportWire = wire(helper, exportPos, Direction.EAST);
        var storageWire = wire(helper, storagePos, Direction.EAST);
        for (BlockPos pos : new BlockPos[]{sourcePos.east(), exportPos.west(), storagePos.west()})
            helper.getLevel().setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        var source = (ChestBlockEntity) helper.getLevel().getBlockEntity(sourcePos.east());
        var export = (ChestBlockEntity) helper.getLevel().getBlockEntity(exportPos.west());
        var storage = (ChestBlockEntity) helper.getLevel().getBlockEntity(storagePos.west());
        helper.assertTrue(sourceWire.logisticsCovers().attach(Direction.EAST, cover(LogisticsCoverType.GENERIC_IMPORT))
                        && exportWire.logisticsCovers().attach(Direction.WEST, cover(LogisticsCoverType.GENERIC_EXPORT))
                        && storageWire.logisticsCovers().attach(Direction.WEST, cover(LogisticsCoverType.ITEM_STORAGE))
                        && LogisticsCoverInteraction.setItemFilter(storageWire.logisticsCovers(), Direction.WEST,
                        new ItemStack(Items.DIAMOND)), "an export and higher-rank filtered storage share one network");
        source.setItem(0, new ItemStack(Items.DIAMOND, 3));
        charge(helper, core);
        var first = core.routeNetworkOnce();
        helper.assertTrue(first.itemsMoved() == 3 && export.getItem(0).getCount() == 3
                        && storage.getItem(0).isEmpty(),
                "GT6 tries all exports before any storage bus, regardless of priority rank");
        exportWire.logisticsCovers().remove(Direction.WEST);
        source.setItem(0, new ItemStack(Items.DIAMOND, 2));
        var second = core.routeNetworkOnce();
        helper.assertTrue(second.itemsMoved() == 2 && storage.getItem(0).getCount() == 2
                        && source.getItem(0).isEmpty() && core.storedEU() == 507,
                "when no export remains, the matching filtered storage receives the next batch");
        helper.succeed();
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void dumpOnlyUnreservedGenericStorageItems(GameTestHelper helper) {
        var core = LogisticsNetworkTests.minimumCore(helper);
        BlockPos center = core.getBlockPos().south(2);
        BlockPos sourcePos = center.offset(3, 0, 0), dumpPos = center.offset(-3, 0, 0),
                filterPos = center.offset(3, 1, 0);
        var sourceWire = wire(helper, sourcePos, Direction.WEST);
        var dumpWire = wire(helper, dumpPos, Direction.EAST);
        var filterWire = wire(helper, filterPos, Direction.WEST);
        for (BlockPos pos : new BlockPos[]{sourcePos.east(), dumpPos.west(), filterPos.east()})
            helper.getLevel().setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        var source = (ChestBlockEntity) helper.getLevel().getBlockEntity(sourcePos.east());
        var dump = (ChestBlockEntity) helper.getLevel().getBlockEntity(dumpPos.west());
        source.setItem(0, new ItemStack(Items.DIAMOND, 5));
        source.setItem(1, new ItemStack(Items.EMERALD, 4));
        helper.assertTrue(sourceWire.logisticsCovers().attach(Direction.EAST, cover(LogisticsCoverType.GENERIC_STORAGE))
                        && dumpWire.logisticsCovers().attach(Direction.WEST, cover(LogisticsCoverType.ITEM_DUMP))
                        && filterWire.logisticsCovers().attach(Direction.EAST, cover(LogisticsCoverType.ITEM_IMPORT))
                        && LogisticsCoverInteraction.setItemFilter(filterWire.logisticsCovers(), Direction.EAST,
                        new ItemStack(Items.DIAMOND)), "dump and a diamond reservation enter the network");
        charge(helper, core);
        var result = core.routeNetworkOnce();
        helper.assertTrue(result.itemsMoved() == 4 && result.energyCost() == 4
                        && source.getItem(0).getCount() == 5 && source.getItem(1).isEmpty()
                        && dump.getItem(0).is(Items.EMERALD) && dump.getItem(0).getCount() == 4,
                "dump skips every item named by a filtered bus and spends one EU per dumped item");
        helper.succeed();
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void filledMassStorageDefragmentsAndReservesItsItem(GameTestHelper helper) {
        var core = LogisticsNetworkTests.minimumCore(helper);
        BlockPos center = core.getBlockPos().south(2);
        BlockPos sourcePos = center.offset(3, 0, 0), storagePos = center.offset(-3, 0, 0),
                dumpPos = center.offset(-3, 1, 0);
        var sourceWire = wire(helper, sourcePos, Direction.WEST);
        var storageWire = wire(helper, storagePos, Direction.EAST);
        var dumpWire = wire(helper, dumpPos, Direction.EAST);
        helper.getLevel().setBlock(sourcePos.east(), Blocks.CHEST.defaultBlockState(), 3);
        helper.getLevel().setBlock(storagePos.west(), GTStorage.MASS_STORAGE.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(dumpPos.west(), Blocks.CHEST.defaultBlockState(), 3);
        var source = (ChestBlockEntity) helper.getLevel().getBlockEntity(sourcePos.east());
        var storage = (MassStorageBlockEntity) helper.getLevel().getBlockEntity(storagePos.west());
        var dump = (ChestBlockEntity) helper.getLevel().getBlockEntity(dumpPos.west());
        helper.assertTrue(storage.insert(new ItemStack(Items.DIAMOND, 2)) == 2
                        && sourceWire.logisticsCovers().attach(Direction.EAST, cover(LogisticsCoverType.GENERIC_STORAGE))
                        && storageWire.logisticsCovers().attach(Direction.WEST, cover(LogisticsCoverType.GENERIC_STORAGE))
                        && dumpWire.logisticsCovers().attach(Direction.WEST, cover(LogisticsCoverType.ITEM_DUMP)),
                "filled Mass Storage and chest both join storage routing through generic buses");
        source.setItem(0, new ItemStack(Items.DIAMOND, 5));
        source.setItem(1, new ItemStack(Items.EMERALD, 4));
        charge(helper, core);
        var first = core.routeNetworkOnce();
        helper.assertTrue(first.itemsMoved() == 5 && first.operations() == 1
                        && storage.stored() == 7 && source.getItem(0).isEmpty()
                        && source.getItem(1).getCount() == 4 && dump.getItem(0).isEmpty(),
                "GT6 promotes matching generic inventory into semi-filtered Mass Storage before dumping");
        helper.assertTrue(LogisticsCoverInteraction.cyclePriority(storageWire.logisticsCovers(), Direction.WEST),
                "an explicit Generic priority overrides the automatic semi-filtered rank");
        source.setItem(0, new ItemStack(Items.DIAMOND, 3));
        var second = core.routeNetworkOnce();
        helper.assertTrue(second.itemsMoved() == 4 && second.energyCost() == 4
                        && source.getItem(0).getCount() == 3 && source.getItem(1).isEmpty()
                        && storage.stored() == 7
                        && dump.getItem(0).is(Items.EMERALD) && dump.getItem(0).getCount() == 4
                        && core.storedEU() == 503,
                "Mass Storage's diamond template reserves diamonds from Dump even with an explicit bus priority");
        helper.succeed();
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void massStorageConvertsNuggetsAboveOneStackAndProtectsOnlyItsFamily(GameTestHelper helper) {
        var core = LogisticsNetworkTests.minimumCore(helper);
        BlockPos center = core.getBlockPos().south(2);
        BlockPos sourcePos = center.offset(3, 0, 0), storagePos = center.offset(-3, 0, 0),
                dumpPos = center.offset(-3, 1, 0);
        var sourceWire = wire(helper, sourcePos, Direction.WEST);
        var storageWire = wire(helper, storagePos, Direction.EAST);
        var dumpWire = wire(helper, dumpPos, Direction.EAST);
        helper.getLevel().setBlock(sourcePos.east(), Blocks.CHEST.defaultBlockState(), 3);
        helper.getLevel().setBlock(storagePos.west(), GTStorage.MASS_STORAGE.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(dumpPos.west(), Blocks.CHEST.defaultBlockState(), 3);
        var source = (ChestBlockEntity) helper.getLevel().getBlockEntity(sourcePos.east());
        var storage = (MassStorageBlockEntity) helper.getLevel().getBlockEntity(storagePos.west());
        var dump = (ChestBlockEntity) helper.getLevel().getBlockEntity(dumpPos.west());
        ItemStack plate = GTItems.getStack(MaterialPrefix.plate, Materials.Iron, 2);
        helper.assertTrue(!plate.isEmpty() && storage.insert(new ItemStack(Items.IRON_INGOT, 64)) == 64
                        && sourceWire.logisticsCovers().attach(Direction.EAST, cover(LogisticsCoverType.GENERIC_STORAGE))
                        && storageWire.logisticsCovers().attach(Direction.WEST, cover(LogisticsCoverType.GENERIC_STORAGE))
                        && dumpWire.logisticsCovers().attach(Direction.WEST, cover(LogisticsCoverType.ITEM_DUMP)),
                "a filled iron storage, source chest and Dump bus join the network");
        source.setItem(0, new ItemStack(Items.IRON_NUGGET, 9));
        // Chest stores a mutable ItemStack; retain the original as the expected item key.
        source.setItem(1, plate.copy());
        charge(helper, core);
        var first = core.routeNetworkOnce();
        helper.assertTrue(first.itemsMoved() == 9 && first.energyCost() == 9
                        && storage.stored() == 65 && storage.partialUnits() == 0
                        && source.getItem(0).isEmpty() && source.getItem(1).getCount() == 2
                        && dump.getItem(0).isEmpty(),
                "nine vanilla iron nuggets become one ingot although bulk storage already shows 64 items");

        helper.assertTrue(LogisticsCoverInteraction.cyclePriority(storageWire.logisticsCovers(), Direction.WEST),
                "an explicit Generic priority leaves only the Dump phase available");
        ItemStack namedNuggets = new ItemStack(Items.IRON_NUGGET, 4);
        namedNuggets.getOrCreateTag().putString("audit.custom_name", "keep this item");
        source.setItem(0, namedNuggets);
        var second = core.routeNetworkOnce();
        helper.assertTrue(second.itemsMoved() == 2 && second.energyCost() == 2
                        && source.getItem(0).getCount() == 4 && source.getItem(0).hasTag()
                        && source.getItem(1).isEmpty()
                        && dump.getItem(0).is(plate.getItem()) && dump.getItem(0).getCount() == 2
                        && storage.stored() == 65 && core.storedEU() == 501,
                "Dump reserves even tagged iron nuggets but sends same-material iron plates away");
        helper.succeed();
    }
}
