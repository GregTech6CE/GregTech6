package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.misc.BumbleHiveBlock;
import com.gregtech.gregtech.block.misc.FixedBumbleHiveBlock;
import com.gregtech.gregtech.blockentity.misc.BumbleHiveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_issues")
@PrefixGameTestTemplate(false)
public final class IssueHiveTests {
    @GameTest(template="test_empty", timeoutTicks=40)
    public static void sixteenHiveVariantsHavePlainDistinctItemsAndValidEntities(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        var items = new java.util.HashSet<net.minecraft.world.item.Item>();
        for (var color : DyeColor.values()) {
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech", "bumble_hive_" + color.getName()));
            h.assertTrue(block instanceof FixedBumbleHiveBlock hive && hive.color() == color, "fixed registered color " + color);
            h.assertTrue(block.defaultBlockState().getProperties().isEmpty(), "no color or other variant property");
            var stack = new ItemStack(block);
            h.assertTrue(!stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA) && !stack.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA) && !stack.has(net.minecraft.core.component.DataComponents.BLOCK_STATE), "variant does not require stack NBT");
            items.add(stack.getItem());
            h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
            h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof BumbleHiveBlockEntity hive && hive.getType().isValid(block.defaultBlockState()), "all fixed variants retain colonies");
        }
        h.assertTrue(items.size() == 16, "sixteen different registered items");
        h.succeed();
    }

    @GameTest(template="test_empty", timeoutTicks=40)
    public static void legacyColorMigrationPreservesAllInventorySlots(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        var legacy = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech", "bumble_hive"));
        for (var color : DyeColor.values()) {
            h.getLevel().setBlockAndUpdate(pos, legacy.defaultBlockState().setValue(BumbleHiveBlock.COLOR, color));
            var hive = (BumbleHiveBlockEntity)h.getLevel().getBlockEntity(pos);
            for (int i = 0; i < BumbleHiveBlockEntity.SLOTS; i++) hive.inventory().setStackInSlot(i, new ItemStack(Items.HONEYCOMB, i + 1));
            var ticker = ((BumbleHiveBlock)legacy).getTicker(h.getLevel(), hive.getBlockState(), com.gregtech.gregtech.registry.GTBlockEntities.BUMBLE_HIVE.get());
            ticker.tick(h.getLevel(), pos, hive.getBlockState(), hive);
            var migrated = (BumbleHiveBlockEntity)h.getLevel().getBlockEntity(pos);
            h.assertTrue(migrated.getBlockState().getBlock() instanceof FixedBumbleHiveBlock fixed && fixed.color() == color, "server tick migrates old color " + color);
            for (int i = 0; i < BumbleHiveBlockEntity.SLOTS; i++) h.assertTrue(migrated.inventory().getStackInSlot(i).getCount() == i + 1, "migration retains slot " + i);
        }
        h.succeed();
    }
}
