package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.machine.LogisticsWireBlock;
import com.gregtech.gregtech.block.misc.FilterBlock;
import com.gregtech.gregtech.block.misc.FilterBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LogisticsWireBlockEntity;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.content.logistics.LogisticsCoverInteraction;
import com.gregtech.gregtech.content.logistics.LogisticsCoverType;
import com.gregtech.gregtech.content.logistics.LogisticsFluidRouter;
import com.gregtech.gregtech.content.logistics.LogisticsItemRouter;
import com.gregtech.gregtech.content.logistics.LogisticsNetwork;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTIconSetBlocks;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTMiscBlocks;
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

import java.util.Set;

/** GT6 item semi-filters on unmodified generic logistics buses. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class LogisticsFilterRoutingTests {
    private LogisticsFilterRoutingTests() {}

    private static ItemStack cover(LogisticsCoverType type) {
        var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", type.id()));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static LogisticsWireBlockEntity wire(GameTestHelper helper, BlockPos pos, Direction inward) {
        var block = (LogisticsWireBlock) GTIconSetBlocks.LOGISTICS_WIRE.get();
        helper.setBlock(pos, block.defaultBlockState().setValue(ElectricWireBlock.propFor(inward), true));
        return (LogisticsWireBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static ChestBlockEntity chest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST);
        return (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static FilterBlockEntity filter(GameTestHelper helper, BlockPos pos, boolean prefix, Direction front) {
        var block = prefix ? GTMiscBlocks.FILTER_OREDICT.get() : GTMiscBlocks.FILTER_ITEMS.get();
        helper.setBlock(pos, block.defaultBlockState().setValue(FilterBlock.FACING, front));
        return (FilterBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static LogisticsNetwork.Snapshot network(GameTestHelper helper, BlockPos... wires) {
        var positions = new java.util.HashSet<BlockPos>();
        for (BlockPos wire : wires) positions.add(helper.absolutePos(wire));
        return new LogisticsNetwork.Snapshot(Set.copyOf(positions), 0);
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void whitelistRaisesGenericBusToSemiAndBlacklistRemovesIt(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(3, 3, 3), targetPos = new BlockPos(9, 3, 3);
        var sourceWire = wire(helper, sourcePos, Direction.WEST);
        var targetWire = wire(helper, targetPos, Direction.EAST);
        var source = chest(helper, sourcePos.east());
        var filter = filter(helper, targetPos.west(), false, Direction.WEST);
        var destination = chest(helper, targetPos.west(2));
        helper.assertTrue(sourceWire.logisticsCovers().attach(Direction.EAST, cover(LogisticsCoverType.GENERIC_IMPORT))
                        && targetWire.logisticsCovers().attach(Direction.WEST, cover(LogisticsCoverType.GENERIC_EXPORT)),
                "generic import and export covers attach");
        var taggedDiamonds = new ItemStack(Items.DIAMOND, 5);
        taggedDiamonds.getOrCreateTag().putString("batch", "candidate NBT");
        source.setItem(0, taggedDiamonds);
        source.setItem(1, new ItemStack(Items.EMERALD, 3));
        var template = new ItemStack(Items.DIAMOND);
        filter.setTemplate(0, template);

        var first = LogisticsItemRouter.collect(helper.getLevel(), network(helper, sourcePos, targetPos));
        helper.assertTrue(first.tryPair(LogisticsCoverType.Role.IMPORT, 0,
                LogisticsCoverType.Role.EXPORT, 0, 1) == 0,
                "unmodified generic bus becomes Semi while a whitelist is installed");
        helper.assertTrue(first.tryPair(LogisticsCoverType.Role.IMPORT, 0,
                LogisticsCoverType.Role.EXPORT, 1, 1) == 5
                        && destination.getItem(0).is(Items.DIAMOND) && source.getItem(0).isEmpty(),
                "semi-filter routes a tagged item through an untagged ghost wildcard");

        filter.toggleMode();
        var second = LogisticsItemRouter.collect(helper.getLevel(), network(helper, sourcePos, targetPos));
        helper.assertTrue(second.tryPair(LogisticsCoverType.Role.IMPORT, 0,
                LogisticsCoverType.Role.EXPORT, 1, 1) == 0,
                "a blacklist does not advertise a semi-filter");
        helper.assertTrue(second.tryPair(LogisticsCoverType.Role.IMPORT, 0,
                LogisticsCoverType.Role.EXPORT, 0, 1) == 3
                        && destination.getItem(1).is(Items.EMERALD) && source.getItem(1).isEmpty(),
                "blacklist still passes other items through its generic endpoint");
        helper.succeed();
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void prefixReservesOnlyItsRegisteredFormFromDump(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(3, 3, 3), dumpPos = new BlockPos(9, 3, 3), filterPos = new BlockPos(3, 3, 6);
        var sourceWire = wire(helper, sourcePos, Direction.WEST);
        var dumpWire = wire(helper, dumpPos, Direction.EAST);
        var filterWire = wire(helper, filterPos, Direction.WEST);
        var source = chest(helper, sourcePos.east());
        var dump = chest(helper, dumpPos.west());
        var filter = filter(helper, filterPos.east(), true, Direction.EAST);
        chest(helper, filterPos.east(2));
        helper.assertTrue(sourceWire.logisticsCovers().attach(Direction.EAST, cover(LogisticsCoverType.GENERIC_STORAGE))
                        && dumpWire.logisticsCovers().attach(Direction.WEST, cover(LogisticsCoverType.ITEM_DUMP))
                        && filterWire.logisticsCovers().attach(Direction.EAST, cover(LogisticsCoverType.GENERIC_IMPORT)),
                "storage, dump and a prefix semi-filter join one logistics network");
        ItemStack steelPlate = GTItems.getStack(MaterialPrefix.plate, Materials.Steel, 2);
        ItemStack copperPlate = GTItems.getStack(MaterialPrefix.plate, Materials.Copper, 3);
        ItemStack steelIngot = GTItems.getStack(MaterialPrefix.ingot, Materials.Steel, 4);
        helper.assertTrue(!steelPlate.isEmpty() && !copperPlate.isEmpty() && !steelIngot.isEmpty(),
                "GT material forms required by the prefix test are registered");
        filter.setTemplate(0, steelPlate);
        // SimpleContainer keeps the ItemStack instance and mutates it when extracting.
        // Keep the expected keys independent of the live chest stacks.
        source.setItem(0, copperPlate.copy());
        source.setItem(1, steelIngot.copy());

        var first = LogisticsItemRouter.collect(helper.getLevel(), network(helper, sourcePos, dumpPos, filterPos));
        helper.assertTrue(first.tryDump(1) == 4 && source.getItem(0).getCount() == 3
                        && source.getItem(1).isEmpty() && dump.getItem(0).is(steelIngot.getItem()),
                "prefix reserves every registered plate, not all forms of steel");

        filter.toggleMode();
        var second = LogisticsItemRouter.collect(helper.getLevel(), network(helper, sourcePos, dumpPos, filterPos));
        helper.assertTrue(second.tryDump(1) == 3 && source.getItem(0).isEmpty()
                        && dump.getItem(1).is(copperPlate.getItem()),
                "blacklist removes the prefix's dump reservation");
        filter.clearFilter();
        helper.assertTrue(filter.logisticsItemFilter() == null,
                "an unselected prefix does not advertise a semi-filter");
        helper.succeed();
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void whitelistReservesItemsRegardlessOfNbt(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(3, 3, 3), dumpPos = new BlockPos(9, 3, 3), filterPos = new BlockPos(3, 3, 6);
        var sourceWire = wire(helper, sourcePos, Direction.WEST);
        var dumpWire = wire(helper, dumpPos, Direction.EAST);
        var filterWire = wire(helper, filterPos, Direction.WEST);
        var source = chest(helper, sourcePos.east());
        var dump = chest(helper, dumpPos.west());
        var filter = filter(helper, filterPos.east(), false, Direction.EAST);
        chest(helper, filterPos.east(2));
        helper.assertTrue(sourceWire.logisticsCovers().attach(Direction.EAST, cover(LogisticsCoverType.GENERIC_STORAGE))
                        && dumpWire.logisticsCovers().attach(Direction.WEST, cover(LogisticsCoverType.ITEM_DUMP))
                        && filterWire.logisticsCovers().attach(Direction.EAST, cover(LogisticsCoverType.GENERIC_IMPORT)),
                "a normal Filter, generic storage and dump bus join the network");
        var taggedDiamonds = new ItemStack(Items.DIAMOND, 5);
        taggedDiamonds.getOrCreateTag().putString("batch", "NBT does not affect GT6 reservation");
        source.setItem(0, taggedDiamonds);
        source.setItem(1, new ItemStack(Items.EMERALD, 3));
        filter.setTemplate(0, new ItemStack(Items.DIAMOND));

        var first = LogisticsItemRouter.collect(helper.getLevel(), network(helper, sourcePos, dumpPos, filterPos));
        helper.assertTrue(first.tryDump(1) == 3 && source.getItem(0).getCount() == 5
                        && source.getItem(1).isEmpty() && dump.getItem(0).is(Items.EMERALD),
                "a whitelist's exact item key reserves tagged diamonds but not unrelated items");
        filter.toggleMode();
        var second = LogisticsItemRouter.collect(helper.getLevel(), network(helper, sourcePos, dumpPos, filterPos));
        helper.assertTrue(second.tryDump(1) == 5 && source.getItem(0).isEmpty()
                        && dump.getItem(1).is(Items.DIAMOND),
                "blacklist releases the former whitelist's dump reservation");
        helper.succeed();
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void genericBusDoesNotRouteFluidThroughItemSemiFilter(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(3, 3, 3), targetPos = new BlockPos(9, 3, 3);
        var sourceWire = wire(helper, sourcePos, Direction.WEST);
        var targetWire = wire(helper, targetPos, Direction.EAST);
        filter(helper, sourcePos.east(), true, Direction.EAST);
        helper.setBlock(sourcePos.east(2), GTTanks.DRUM_STEEL.get());
        helper.setBlock(targetPos.west(), GTTanks.DRUM_STEEL.get());
        var source = (TankBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(sourcePos.east(2)));
        var destination = (TankBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(targetPos.west()));
        helper.assertTrue(sourceWire.logisticsCovers().attach(Direction.EAST, cover(LogisticsCoverType.GENERIC_IMPORT))
                        && targetWire.logisticsCovers().attach(Direction.WEST, cover(LogisticsCoverType.GENERIC_EXPORT))
                        && source.getFluidTank().fill(new FluidStack(Fluids.WATER, 1000),
                        IFluidHandler.FluidAction.EXECUTE) == 1000,
                "a prefix filter can relay water but its generic bus is item-only in GT6");
        var first = LogisticsFluidRouter.collect(helper.getLevel(), network(helper, sourcePos, targetPos));
        helper.assertTrue(first.tryRankPair(0, 0, 1) == 0
                        && source.getFluidTank().getAmount() == 1000 && destination.getFluidTank().isEmpty(),
                "generic bus aimed into the semi-filter never transfers fluid");

        sourceWire.logisticsCovers().remove(Direction.EAST);
        helper.assertTrue(sourceWire.logisticsCovers().attach(Direction.EAST, cover(LogisticsCoverType.FLUID_IMPORT))
                        && LogisticsCoverInteraction.setFluidFilter(sourceWire.logisticsCovers(), Direction.EAST,
                        new FluidStack(Fluids.WATER, 1000)),
                "a dedicated fluid import bus can still target that relay");
        var second = LogisticsFluidRouter.collect(helper.getLevel(), network(helper, sourcePos, targetPos));
        helper.assertTrue(second.tryRankPair(0, 2, 1) == 1000
                        && source.getFluidTank().isEmpty() && destination.getFluidTank().getAmount() == 1000,
                "GT6 suppresses only generic fluid routing, not a sampled fluid bus");
        helper.succeed();
    }
}
