package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.machine.LogisticsWireBlock;
import com.gregtech.gregtech.blockentity.machine.LogisticsWireBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity;
import com.gregtech.gregtech.content.cover.CoverItems;
import com.gregtech.gregtech.content.logistics.LogisticsCoreStructure;
import com.gregtech.gregtech.content.logistics.LogisticsCoverInteraction;
import com.gregtech.gregtech.content.logistics.LogisticsCoverType;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
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

/** W3 regression: GT6 logistics-cover attachment and a real item routing path. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class LogisticsCoverRepairTests {
    private static ItemStack cover(LogisticsCoverType type) {
        var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", type.id()));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void attachmentDisconnectsAndPersistsWithoutWrongHost(GameTestHelper helper) {
        for (var type : LogisticsCoverType.values())
            helper.assertTrue(!cover(type).isEmpty() && type.id().equals(CoverItems.behavior(cover(type))),
                    "all 14 original logistics cover ids dispatch: " + type.id());

        BlockPos wirePos = helper.absolutePos(new BlockPos(5, 4, 5));
        var block = (LogisticsWireBlock) GTIconSetBlocks.LOGISTICS_WIRE.get();
        helper.getLevel().setBlock(wirePos, block.defaultBlockState()
                .setValue(ElectricWireBlock.WEST, true), 3);
        BlockPos neighborPos = wirePos.west();
        helper.getLevel().setBlock(neighborPos, block.defaultBlockState()
                .setValue(ElectricWireBlock.EAST, true), 3);
        var wire = (LogisticsWireBlockEntity) helper.getLevel().getBlockEntity(wirePos);
        helper.assertTrue(wire.logisticsCovers().attach(Direction.WEST, cover(LogisticsCoverType.ITEM_IMPORT)),
                "item import bus installs on a logistics wire");
        helper.assertTrue(!helper.getLevel().getBlockState(wirePos).getValue(ElectricWireBlock.WEST)
                        && !helper.getLevel().getBlockState(neighborPos).getValue(ElectricWireBlock.EAST),
                "installing a logistics cover severs both ends of the connector");
        var sample = new ItemStack(Items.DIAMOND);
        sample.getOrCreateTag().putString("sample", "filter");
        helper.assertTrue(LogisticsCoverInteraction.setItemFilter(wire.logisticsCovers(), Direction.WEST, sample),
                "right-click item template is stored on the installed cover");
        for (int i = 0; i < 4; i++)
            helper.assertTrue(LogisticsCoverInteraction.cycleTargetStackSize(wire.logisticsCovers(), Direction.WEST),
                    "wire cutter cycles the import quota");
        helper.assertTrue(LogisticsCoverInteraction.cyclePriority(wire.logisticsCovers(), Direction.WEST)
                        && LogisticsCoverInteraction.targetStackSize(wire.logisticsCovers().get(Direction.WEST)) == 4,
                "screwdriver priority and wire-cutter quota have separate bit fields");

        var restored = new LogisticsWireBlockEntity(wirePos, helper.getLevel().getBlockState(wirePos));
        restored.load(wire.saveWithoutMetadata());
        ItemStack stored = restored.logisticsCovers().get(Direction.WEST);
        helper.assertTrue(LogisticsCoverType.of(stored) == LogisticsCoverType.ITEM_IMPORT
                        && LogisticsCoverInteraction.targetStackSize(stored) == 4
                        && LogisticsCoverInteraction.matches(LogisticsCoverInteraction.itemFilter(stored),
                        new ItemStack(Items.DIAMOND)),
                "cover, quota and NBT-insensitive item template survive save/load");

        BlockPos orphanPos = wirePos.above(2);
        helper.getLevel().setBlock(orphanPos,
                LargeMachineParts.block(LogisticsCoreStructure.WALL).defaultBlockState(), 3);
        var orphan = (MultiblockPortBlockEntity) helper.getLevel().getBlockEntity(orphanPos);
        helper.assertTrue(!orphan.logisticsCovers().attach(Direction.UP, cover(LogisticsCoverType.ITEM_EXPORT)),
                "unbound ordinary multiblock parts cannot impersonate a logistics host");

        var core = LogisticsCoreTests.build(helper, Direction.NORTH);
        var wallCell = LogisticsCoreStructure.cells().stream()
                .filter(cell -> cell.kind() == LogisticsCoreStructure.Kind.WALL).findFirst().orElseThrow();
        var boundWall = (MultiblockPortBlockEntity) helper.getLevel().getBlockEntity(
                wallCell.at(core.getBlockPos(), Direction.NORTH));
        helper.assertTrue(core.logisticsCovers().attach(Direction.NORTH, cover(LogisticsCoverType.CPU_LOGIC))
                        && boundWall.logisticsCovers().attach(Direction.UP,
                        cover(LogisticsCoverType.GENERIC_IMPORT)),
                "the controller and its bound logistics wall both accept cover faces");
        var wallReloaded = new MultiblockPortBlockEntity(boundWall.getBlockPos(), boundWall.getBlockState());
        wallReloaded.load(boundWall.saveWithoutMetadata());
        helper.assertTrue(LogisticsCoverType.of(wallReloaded.logisticsCovers().get(Direction.UP))
                        == LogisticsCoverType.GENERIC_IMPORT,
                "a bound wall's cover survives a block-entity NBT round trip");
        helper.getLevel().setBlock(wirePos, Blocks.AIR.defaultBlockState(), 3);
        helper.runAfterDelay(1, () -> {
            var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(wirePos).inflate(2));
            helper.assertTrue(drops.stream().anyMatch(drop -> LogisticsCoverType.of(drop.getItem())
                    == LogisticsCoverType.ITEM_IMPORT), "installed cover drops when its wire is broken");
            helper.succeed();
        });
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void filteredImportRoutesToHighestPriorityExportWithQuota(GameTestHelper helper) {
        var core = LogisticsNetworkTests.minimumCore(helper);
        BlockPos center = core.getBlockPos().south(2);
        var wire = (LogisticsWireBlock) GTIconSetBlocks.LOGISTICS_WIRE.get();

        BlockPos sourceWire = center.offset(3, 0, 0), sourceChest = center.offset(4, 0, 0);
        BlockPos filteredWire = center.offset(-3, 0, 0), filteredChest = center.offset(-4, 0, 0);
        BlockPos genericWire = center.offset(-3, 1, 0), genericChest = center.offset(-4, 1, 0);
        helper.getLevel().setBlock(sourceWire, wire.defaultBlockState()
                .setValue(ElectricWireBlock.WEST, true), 3);
        helper.getLevel().setBlock(filteredWire, wire.defaultBlockState()
                .setValue(ElectricWireBlock.EAST, true), 3);
        helper.getLevel().setBlock(genericWire, wire.defaultBlockState()
                .setValue(ElectricWireBlock.EAST, true), 3);
        for (BlockPos pos : new BlockPos[]{sourceChest, filteredChest, genericChest})
            helper.getLevel().setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);

        var source = (LogisticsWireBlockEntity) helper.getLevel().getBlockEntity(sourceWire);
        var filtered = (LogisticsWireBlockEntity) helper.getLevel().getBlockEntity(filteredWire);
        var generic = (LogisticsWireBlockEntity) helper.getLevel().getBlockEntity(genericWire);
        helper.assertTrue(source.logisticsCovers().attach(Direction.EAST, cover(LogisticsCoverType.ITEM_IMPORT))
                        && filtered.logisticsCovers().attach(Direction.WEST, cover(LogisticsCoverType.ITEM_EXPORT))
                        && generic.logisticsCovers().attach(Direction.WEST, cover(LogisticsCoverType.GENERIC_EXPORT)),
                "import, filtered export and generic export are installed on network faces");
        var template = new ItemStack(Items.DIAMOND);
        template.getOrCreateTag().putString("sample", "different from cargo");
        LogisticsCoverInteraction.setItemFilter(source.logisticsCovers(), Direction.EAST, template);
        LogisticsCoverInteraction.setItemFilter(filtered.logisticsCovers(), Direction.WEST, template);
        for (int i = 0; i < 4; i++) {
            LogisticsCoverInteraction.cycleTargetStackSize(source.logisticsCovers(), Direction.EAST);
            LogisticsCoverInteraction.cycleTargetStackSize(filtered.logisticsCovers(), Direction.WEST);
        }
        var input = (ChestBlockEntity) helper.getLevel().getBlockEntity(sourceChest);
        var output = (ChestBlockEntity) helper.getLevel().getBlockEntity(filteredChest);
        var fallback = (ChestBlockEntity) helper.getLevel().getBlockEntity(genericChest);
        input.setItem(0, new ItemStack(Items.DIAMOND, 10));

        var wallCell = LogisticsCoreStructure.cells().stream()
                .filter(cell -> cell.kind() == LogisticsCoreStructure.Kind.WALL).findFirst().orElseThrow();
        var wall = (MultiblockPortBlockEntity) helper.getLevel().getBlockEntity(
                wallCell.at(core.getBlockPos(), Direction.NORTH));
        helper.assertTrue(wall.doEnergyInjection(GregTechTags.Energy.EU, Direction.UP, 512, 1, true) == 1,
                "core receives power through its galvanized wall");
        var network = core.scanNetwork();
        helper.assertTrue(network.contains(sourceWire) && network.contains(filteredWire)
                        && network.contains(genericWire), "all three buses are within control range");
        var result = core.routeNetworkOnce();
        helper.assertTrue(result.itemsMoved() == 4 && result.operations() == 1
                        && input.getItem(0).getCount() == 6 && output.getItem(0).getCount() == 4
                        && fallback.getItem(0).isEmpty() && core.storedEU() == 508,
                "filtered export wins over generic, quota is four, and each item costs one EU");
        LogisticsCoverInteraction.clearItemFilter(filtered.logisticsCovers(), Direction.WEST);
        LogisticsCoverInteraction.setItemFilter(filtered.logisticsCovers(), Direction.WEST,
                new ItemStack(Items.GOLD_INGOT));
        var second = core.routeNetworkOnce();
        helper.assertTrue(second.itemsMoved() == 4 && output.getItem(0).getCount() == 4
                        && fallback.getItem(0).getCount() == 4,
                "a mismatched filtered export yields to the generic export without deleting items");
        helper.succeed();
    }
}
