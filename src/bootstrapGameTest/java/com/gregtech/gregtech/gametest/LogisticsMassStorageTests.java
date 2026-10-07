package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.machine.LogisticsWireBlock;
import com.gregtech.gregtech.blockentity.inventory.LogisticsMassStorageBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LogisticsWireBlockEntity;
import com.gregtech.gregtech.content.logistics.LogisticsCoreStructure;
import com.gregtech.gregtech.content.logistics.LogisticsCoverType;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.BlockHarvestPolicy;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTIconSetBlocks;
import com.gregtech.gregtech.registry.GTStorage;
import com.gregtech.gregtech.registry.GTStorageMetals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** GT6 6200-6259 are independent network hosts, not ordinary storage blocks with bus covers. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class LogisticsMassStorageTests {
    private LogisticsMassStorageTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void sixtyMaterialVariantsHaveRecipesAndDedicatedBlockEntities(GameTestHelper helper) {
        helper.assertTrue(GTStorage.LOGISTICS_MASS_STORAGES.size() == 60
                        && GTStorageMetals.ALL.size() == 60,
                "all 60 GT6 logistics metalset variants are registered");
        for (int i = 0; i < 60; i++) {
            var entry = GTStorage.LOGISTICS_MASS_STORAGES.get(i);
            var spec = GTStorageMetals.ALL.get(i);
            helper.assertTrue(entry.getId().getPath().equals("logistics_mass_storage_" + spec.suffix())
                            && GTBlockEntities.LOGISTICS_MASS_STORAGE.get().isValid(entry.get().defaultBlockState()),
                    "variant " + spec.suffix() + " uses the dedicated logistics block entity type");
            helper.assertTrue(BlockHarvestPolicy.tool(entry.get()) == BlockHarvestPolicy.Tool.WRENCH
                            && BlockHarvestPolicy.level(entry.get()) == spec.material().getToolQuality()
                            && BlockHarvestPolicy.tool(GTStorage.MASS_STORAGES.get(i).get())
                            == BlockHarvestPolicy.Tool.WRENCH
                            && BlockHarvestPolicy.level(GTStorage.MASS_STORAGES.get(i).get())
                            == spec.material().getToolQuality(),
                    "both storages of " + spec.suffix() + " require the GT6 wrench and material quality");
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                    "gregtech", "hand/storage/logistics_mass_storage_" + spec.suffix())).isPresent(),
                    "variant " + spec.suffix() + " has its GT6 survival recipe");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void highTierStorageNeedsMatchingWrench(GameTestHelper helper) {
        int index = java.util.stream.IntStream.range(0, GTStorageMetals.ALL.size())
                .filter(i -> GTStorageMetals.ALL.get(i).material().getToolQuality() > 3)
                .findFirst().orElseThrow();
        var spec = GTStorageMetals.ALL.get(index);
        var block = GTStorage.LOGISTICS_MASS_STORAGES.get(index).get();
        var wrenchTag = BlockTags.create(ResourceLocation.fromNamespaceAndPath("gregtech", "mineable/wrench"));
        helper.assertTrue(block.defaultBlockState().is(wrenchTag)
                        && !block.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE),
                "the logistics casing is dismantled with a wrench, not a pickaxe");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack lowWrench = GTToolItem.create(GTToolType.WRENCH, Materials.Steel,
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
        ItemStack highWrench = GTToolItem.create(GTToolType.WRENCH, spec.material(),
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
        helper.assertTrue(GTToolHelper.getHarvestLevel(lowWrench) < BlockHarvestPolicy.level(block)
                        && GTToolHelper.getHarvestLevel(highWrench) >= BlockHarvestPolicy.level(block),
                "the selected storage material distinguishes the two wrench tiers");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, lowWrench);
        helper.assertTrue(!block.defaultBlockState().canHarvestBlock(helper.getLevel(), pos, player),
                "a low-tier wrench cannot recover the high-tier warehouse");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(Items.DIAMOND_PICKAXE));
        helper.assertTrue(!block.defaultBlockState().canHarvestBlock(helper.getLevel(), pos, player),
                "even a diamond pickaxe is the wrong dismantling tool");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, highWrench);
        helper.assertTrue(block.defaultBlockState().canHarvestBlock(helper.getLevel(), pos, player),
                "a matching-quality wrench can recover the warehouse");
        helper.succeed();
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void storesAndExportsDirectlyWithoutAnyBusCover(GameTestHelper helper) {
        var core = LogisticsNetworkTests.minimumCore(helper);
        BlockPos center = core.getBlockPos().south(2);
        BlockPos sourcePos = center.offset(3, 0, 0);
        BlockPos storagePos = center.offset(-3, 0, 0);
        var wire = (LogisticsWireBlock) GTIconSetBlocks.LOGISTICS_WIRE.get();
        helper.getLevel().setBlock(sourcePos, wire.defaultBlockState()
                .setValue(ElectricWireBlock.WEST, true), 3);
        helper.getLevel().setBlock(sourcePos.east(), Blocks.CHEST.defaultBlockState(), 3);
        helper.getLevel().setBlock(storagePos,
                GTStorage.LOGISTICS_MASS_STORAGES.get(0).get().defaultBlockState(), 3);
        var sourceWire = (LogisticsWireBlockEntity) helper.getLevel().getBlockEntity(sourcePos);
        var source = (ChestBlockEntity) helper.getLevel().getBlockEntity(sourcePos.east());
        var storage = (LogisticsMassStorageBlockEntity) helper.getLevel().getBlockEntity(storagePos);
        var importItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(
                "gregtech", LogisticsCoverType.GENERIC_IMPORT.id()));
        var exportItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(
                "gregtech", LogisticsCoverType.GENERIC_EXPORT.id()));
        helper.assertTrue(importItem != null && exportItem != null
                        && sourceWire.logisticsCovers().attach(Direction.EAST, new ItemStack(importItem))
                        && storage.itemStoragePriority() == 1 && storage.fluidStoragePriority() == 0
                        && core.scanNetwork().contains(storagePos),
                "empty logistics storage joins the core directly as Generic item storage");

        source.setItem(0, new ItemStack(Items.DIAMOND, 5));
        var wallCell = LogisticsCoreStructure.cells().stream()
                .filter(cell -> cell.kind() == LogisticsCoreStructure.Kind.WALL).findFirst().orElseThrow();
        var wall = (com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity)
                helper.getLevel().getBlockEntity(wallCell.at(core.getBlockPos(), Direction.NORTH));
        helper.assertTrue(wall.doEnergyInjection(GregTechTags.Energy.EU,
                Direction.UP, 512, 1, true) == 1, "core accepts a 512 EU packet");
        var incoming = core.routeNetworkOnce();
        helper.assertTrue(incoming.itemsMoved() == 5 && storage.stored() == 5
                        && storage.itemStoragePriority() == 2 && source.getItem(0).isEmpty(),
                "Import deposits into the bare storage and its template raises it to Semi priority");

        var saved = storage.saveWithoutMetadata();
        var restored = new LogisticsMassStorageBlockEntity(storagePos, storage.getBlockState());
        restored.load(saved);
        helper.assertTrue(restored.stored() == 5 && restored.itemStoragePriority() == 2
                        && restored.template().is(Items.DIAMOND),
                "the distinct block entity type retains contents and network priority on reload");

        sourceWire.logisticsCovers().remove(Direction.EAST);
        helper.assertTrue(sourceWire.logisticsCovers().attach(Direction.EAST, new ItemStack(exportItem)),
                "the destination export bus attaches");
        var outgoing = core.routeNetworkOnce();
        helper.assertTrue(outgoing.itemsMoved() == 5 && storage.stored() == 0
                        && source.getItem(0).is(Items.DIAMOND) && source.getItem(0).getCount() == 5,
                "Semi storage exports through the same network without needing a bus on the storage");
        helper.succeed();
    }
}
