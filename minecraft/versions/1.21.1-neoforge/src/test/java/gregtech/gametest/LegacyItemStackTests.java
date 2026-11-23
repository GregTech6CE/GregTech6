package gregtech.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import gregtech.content.blockentity.inventory.SafeBlockEntity;
import gregtech.content.registry.GTSafes;
import gregtech.engine.stack.HandlerNbt;
import gregtech.engine.stack.NbtCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class LegacyItemStackTests {
    @GameTest(template = "test_empty", batch = "legacy_item_nbt")
    public static void forgeSafeContentsSurviveUpgradeAndResave(GameTestHelper helper) throws CommandSyntaxException {
        var old = TagParser.parseTag("""
                {"gt.items":[
                  {Slot:4,id:"minecraft:iron_ingot",Count:64b},
                  {Slot:7,id:"minecraft:diamond_pickaxe",Count:1b,tag:{Damage:13,"gt.marker":42}},
                  {Slot:8,id:"minecraft:coal",Count:0b}
                ]}
                """);
        var lookup = helper.getLevel().registryAccess();
        var safe = new SafeBlockEntity(BlockPos.ZERO, GTSafes.all().get(0).get().defaultBlockState());
        NbtCompat.loadBlockEntity(safe, old, lookup);
        var ingots = safe.inventory().getItem(4);
        var pickaxe = safe.inventory().getItem(7);
        helper.assertTrue(ingots.is(Items.IRON_INGOT) && ingots.getCount() == 64, "old Count and slot must survive");
        helper.assertTrue(safe.inventory().getItem(8).isEmpty(), "a legacy zero-count row stays empty");
        helper.assertTrue(
                pickaxe.is(Items.DIAMOND_PICKAXE) && pickaxe.getDamageValue() == 13,
                "old Damage must become the native damage component");
        var custom = pickaxe.get(DataComponents.CUSTOM_DATA);
        helper.assertTrue(
                custom != null && custom.copyTag().getInt("gt.marker") == 42,
                "unrecognized GT data must survive alongside vanilla components");

        var saved = NbtCompat.saveWithoutMetadata(safe, lookup);
        NbtCompat.loadBlockEntity(safe, saved, lookup);
        helper.assertTrue(
                ItemStack.matches(ingots, safe.inventory().getItem(4))
                        && ItemStack.matches(pickaxe, safe.inventory().getItem(7)),
                "the upgraded safe must retain counts and components after a second save/load");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "legacy_item_nbt")
    public static void privateHandlerKeepsForgeSlotCounts(GameTestHelper helper) throws CommandSyntaxException {
        var payload = TagParser.parseTag("""
                {Size:6,Items:[
                  {Slot:2,id:"minecraft:coal",Count:32b},
                  {Slot:3,id:"minecraft:bread",Count:0b},
                  {Slot:4,id:"minecraft:paper",Count:64b,components:{"minecraft:custom_data":{marker:11}}},
                  {Slot:5,id:"minecraft:stick",count:9}
                ]}
                """);
        var original = payload.copy();
        var inventory = new ItemStackHandler(1);
        HandlerNbt.load(inventory, payload, helper.getLevel().registryAccess());
        helper.assertTrue(
                inventory.getSlots() == 6
                        && inventory.getStackInSlot(2).is(Items.COAL)
                        && inventory.getStackInSlot(2).getCount() == 32,
                "Forge handler size, slot and stack count must survive together");
        helper.assertTrue(
                inventory.getStackInSlot(3).isEmpty()
                        && inventory.getStackInSlot(5).is(Items.STICK)
                        && inventory.getStackInSlot(5).getCount() == 9,
                "empty and already upgraded rows keep their contents");
        var paper = inventory.getStackInSlot(4);
        helper.assertTrue(
                paper.is(Items.PAPER)
                        && paper.getCount() == 64
                        && paper.get(DataComponents.CUSTOM_DATA).copyTag().getInt("marker") == 11,
                "the handler must upgrade a legacy count alongside modern components");
        helper.assertTrue(payload.equals(original), "reading the handler must leave the source tag intact");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "legacy_item_nbt")
    public static void partialMigrationPreservesModernCountAndCustomData(GameTestHelper helper)
            throws CommandSyntaxException {
        var hybrid = TagParser.parseTag("""
                {id:"minecraft:paper",count:3,Count:8b,
                 components:{"minecraft:custom_data":{current:5,shared:7}},tag:{legacy:2,shared:1}}
                """);
        var lookup = helper.getLevel().registryAccess();
        var stack = NbtCompat.readItem(hybrid, lookup);
        helper.assertTrue(stack.is(Items.PAPER) && stack.getCount() == 3, "explicit modern count takes precedence");
        var custom = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        helper.assertTrue(
                custom.getInt("legacy") == 2 && custom.getInt("current") == 5 && custom.getInt("shared") == 7,
                "retain legacy data without replacing modern custom data");
        hybrid.remove("count");
        var withLegacyCount = NbtCompat.readItem(hybrid, lookup);
        helper.assertTrue(
                withLegacyCount.getCount() == 8 && ItemStack.isSameItemSameComponents(stack, withLegacyCount),
                "fall back to the old count while retaining all modern components");
        hybrid.putByte("Count", (byte) 0);
        helper.assertTrue(NbtCompat.readItem(hybrid, lookup).isEmpty(), "modern components cannot revive an empty row");
        helper.succeed();
    }
}
