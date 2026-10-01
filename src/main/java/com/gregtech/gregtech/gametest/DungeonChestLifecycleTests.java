package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.blockentity.inventory.MetalChestBlockEntity;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.registry.GTLootChests;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonData;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.HashSet;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class DungeonChestLifecycleTests {
    @GameTest(template = "test_empty")
    public static void dungeonChestKeepsLootAndFixedSupplies(GameTestHelper h) {
        var level = h.getLevel();
        var origin = h.absolutePos(BlockPos.ZERO);
        var data = new GTDungeonData(level, origin.getX(), origin.getY(), origin.getZ(),
                StoneType.LIMESTONE, StoneType.SLATE, 3, new byte[5][5], 2, 2, 0,
                new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(), RandomSource.create(64000L));
        h.assertTrue(data.chest(1, 1, 1, "gregtech_repair:shelf_paper"), "dungeon places GT loot chest");
        var chest = (MetalChestBlockEntity) level.getBlockEntity(origin.offset(1, 1, 1));
        chest.inventory().setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
        chest.load(chest.saveWithoutMetadata());
        chest.generateLootIfNeeded();
        h.assertTrue(count(chest, Items.PAPER) == 14 && count(chest, Items.DIAMOND) == 3,
                "pending table survives reload and preserves fixed supplies");
        for (int i = 0; i < 54; i++) chest.inventory().setStackInSlot(i, ItemStack.EMPTY);
        chest.load(chest.saveWithoutMetadata());
        h.assertTrue(chest.generateLootIfNeeded() == 0 && count(chest, Items.PAPER) == 0, "consumed loot stays consumed");
        h.assertTrue(data.chestContents(3, 1, 1, new ItemStack(Items.IRON_INGOT, 23)), "GT supply chest placed");
        var supplied = (MetalChestBlockEntity) level.getBlockEntity(origin.offset(3, 1, 1));
        h.assertTrue(supplied.getBlockState().is(com.gregtech.gregtech.registry.GTStorage.reinforcedChest(
                com.gregtech.gregtech.content.material.Materials.Gold)), "portal supplies use GT6 gold-reinforced chest 502");
        supplied.load(supplied.saveWithoutMetadata());
        h.assertTrue(count(supplied, Items.IRON_INGOT) == 23, "fixed supplies survive reload");
        supplied.load(new net.minecraft.nbt.CompoundTag());
        h.assertTrue(supplied.inventory().getSlots() == 54 && count(supplied, Items.IRON_INGOT) == 0,
                "empty NBT clears contents without losing slots");
        h.assertTrue(data.safe(5, 1, 1, "gregtech_repair:shelf_paper", 445L, true), "dungeon places actual key safe");
        var locked = (com.gregtech.gregtech.blockentity.inventory.SafeBlockEntity) level.getBlockEntity(origin.offset(5, 1, 1));
        locked.load(locked.saveWithoutMetadata());
        h.assertTrue(locked.keyId() == 445L && !locked.opened() && locked.inventory().isEmpty(), "dungeon key and unopened loot survive reload");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void brokenLootChestCannotRerollWhenPlaced(GameTestHelper h) {
        var level = h.getLevel();
        var relative = new BlockPos(1, 1, 1);
        var pos = h.absolutePos(relative);
        var block = GTLootChests.LOOT_CHESTS.get(0).get();
        h.setBlock(relative.below(), Blocks.STONE);
        h.setBlock(relative, block);
        var chest = (MetalChestBlockEntity) level.getBlockEntity(pos);
        chest.setDungeonLoot(net.minecraft.resources.ResourceLocation.parse("gregtech_repair:shelf_paper"), 4321);
        var area = new AABB(pos).inflate(2);
        level.getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
        level.getEntitiesOfClass(ExperienceOrb.class, area).forEach(ExperienceOrb::discard);
        level.destroyBlock(pos, true);
        var drops = level.getEntitiesOfClass(ItemEntity.class, area);
        int papers = drops.stream().filter(e -> e.getItem().is(Items.PAPER)).mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(papers == 14, "breaking unopened chest drops its contents exactly once");
        var item = drops.stream().map(ItemEntity::getItem).filter(s -> s.is(block.asItem())).findFirst().orElse(ItemStack.EMPTY).copy();
        h.assertTrue(!item.isEmpty() && BlockItem.getBlockEntityData(item).getBoolean("GTLootGenerated"), "drop remembers consumed loot");
        drops.forEach(ItemEntity::discard);
        int experience = level.getEntitiesOfClass(ExperienceOrb.class, area).stream().mapToInt(ExperienceOrb::getValue).sum();
        var player = h.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(.5, 0, .5), Direction.UP, pos.below(), false);
        var result = ((BlockItem) item.getItem()).place(new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, item, hit));
        h.assertTrue(result.consumesAction(), "actual block item places successfully");
        var replaced = (MetalChestBlockEntity) level.getBlockEntity(pos);
        h.assertTrue(replaced.lootGenerated() && replaced.generateLootIfNeeded() == 0, "placing and opening cannot reroll");
        for (int i = 0; i < 54; i++) h.assertTrue(replaced.inventory().getStackInSlot(i).isEmpty(), "contents are not duplicated into block item");
        h.assertTrue(experience == level.getEntitiesOfClass(ExperienceOrb.class, area).stream().mapToInt(ExperienceOrb::getValue).sum(),
                "placing consumed chest does not create more experience");
        h.succeed();
    }

    private static int count(MetalChestBlockEntity chest, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < chest.inventory().getSlots(); slot++) {
            var stack = chest.inventory().getStackInSlot(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }
}
