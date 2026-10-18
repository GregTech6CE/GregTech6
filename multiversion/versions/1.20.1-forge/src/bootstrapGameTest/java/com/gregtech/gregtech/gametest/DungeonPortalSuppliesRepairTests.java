package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.blockentity.inventory.MetalChestBlockEntity;
import com.gregtech.gregtech.content.book.GTBooks;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.registry.GTStorage;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomPortalNether;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonData;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;

/** GT6 DungeonChunkRoomPortalNether:41-48's actual 54-slot supply-chest layout. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class DungeonPortalSuppliesRepairTests {
    private static final BlockPos ORIGIN = new BlockPos(94000, 200, 94000);

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void netherSupplyChestPreservesGt6SlotsAndStacks(GameTestHelper helper) {
        byte[][] cells = new byte[5][5];
        cells[2][2] = 1;
        cells[3][2] = GTDungeonLayout.CORRIDOR;
        int keyCount = GTDungeonFeature.KEY_COUNT;
        GTDungeonData data = new GTDungeonData(helper.getLevel(), ORIGIN.getX(), ORIGIN.getY(), ORIGIN.getZ(),
                StoneType.LIMESTONE, StoneType.SLATE, 3, cells, 2, 2,
                GTDungeonLayout.connectionCount(cells, 2, 2), new long[keyCount],
                new ItemStack[keyCount], new boolean[keyCount], new HashSet<>(), new HashSet<>(),
                RandomSource.create(20260923L));

        helper.assertTrue(new GTDungeonChunkRoomPortalNether().generate(data), "Nether portal room generated");
        BlockPos chestPos = ORIGIN.offset(1, 2, 5);
        helper.assertTrue(helper.getLevel().getBlockState(chestPos).is(GTStorage.reinforcedChest(Materials.Gold)),
                "GT6's gold reinforced chest is at the east branch");
        helper.assertTrue(helper.getLevel().getBlockEntity(chestPos) instanceof MetalChestBlockEntity,
                "the supply chest has an inventory");
        MetalChestBlockEntity chest = (MetalChestBlockEntity) helper.getLevel().getBlockEntity(chestPos);
        helper.assertTrue(chest.inventory().getSlots() == 54, "GT6 chest has 54 slots");

        ItemStack[] expected = new ItemStack[54];
        expected[4] = new ItemStack(Blocks.OBSIDIAN, 16);
        expected[11] = new ItemStack(Blocks.NETHERRACK, 16);
        expected[15] = new ItemStack(Blocks.GLOWSTONE, 16);
        expected[22] = GTBooks.bookStack("Manual_Hunting_Blaze");
        expected[29] = new ItemStack(Items.GHAST_TEAR, 4);
        expected[33] = new ItemStack(Items.BLAZE_ROD, 4);
        var fullMatchBox = ForgeRegistries.ITEMS.getValue(GregTech.id("match_box_full"));
        helper.assertTrue(fullMatchBox != null && fullMatchBox != Items.AIR,
                "the registered full GT matchbox is available");
        expected[40] = new ItemStack(fullMatchBox);

        for (int slot = 0; slot < expected.length; slot++) {
            ItemStack actual = chest.inventory().getStackInSlot(slot);
            ItemStack wanted = expected[slot];
            if (wanted == null) {
                helper.assertTrue(actual.isEmpty(), "GT6 leaves supply slot " + slot + " empty");
            } else {
                helper.assertTrue(actual.getCount() == wanted.getCount()
                                && ItemStack.isSameItemSameTags(actual, wanted),
                        "GT6 supply slot " + slot + " has " + wanted + ", got " + actual);
            }
        }

        MetalChestBlockEntity restored = new MetalChestBlockEntity(chestPos, chest.getBlockState());
        restored.load(chest.saveWithoutMetadata());
        for (int slot : new int[]{4, 11, 15, 22, 29, 33, 40}) {
            helper.assertTrue(ItemStack.isSameItemSameTags(restored.inventory().getStackInSlot(slot), expected[slot])
                            && restored.inventory().getStackInSlot(slot).getCount() == expected[slot].getCount(),
                    "GT6 supply slot " + slot + " survives a world save");
        }
        helper.succeed();
    }
}
