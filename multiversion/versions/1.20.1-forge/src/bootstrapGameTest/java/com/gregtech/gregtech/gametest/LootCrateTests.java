package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.LootCrateBlock;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import com.gregtech.gregtech.registry.GTToolItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Guards GT6's Loot Crate ({@code MultiTileEntityLootCrate}): prying it open with a <em>crowbar</em>
 * drops one random vanilla loot roll plus the crate itself.
 *
 * <p>The block was a purely decorative placeable before this batch; GT6's tool interaction
 * ({@code onToolClick} with {@code TOOL_crowbar}) and the vanilla loot roll
 * ({@code ST.generateOneVanillaLoot}) were missing.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class LootCrateTests {
    private static final int BASE_X = 38000;
    private static final int BASE_Z = 38000;
    private static final int BASE_Y = 210;

    private static BlockPos base(int index) {
        return new BlockPos(BASE_X + index * 8, BASE_Y, BASE_Z);
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                net.minecraft.core.Direction.UP, pos, false);
    }

    /** Places a crate on a clean stone floor and clears leftovers from earlier runs. */
    private static BlockPos placeCrate(ServerLevel level, BlockPos pos) {
        level.getBlockState(pos);
        for (var entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(6))) entity.discard();
        for (int dy = -1; dy <= 2; dy++) level.setBlock(pos.above(dy), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, GTDecorBlocks.LOOT_CRATE.get().defaultBlockState(), 3);
        return pos;
    }

    private static int droppedItems(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(6)).size();
    }

    /** The crowbar opens the crate: the block is gone, the crate and a vanilla loot roll drop. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void crowbarOpensTheCrate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = placeCrate(level, base(0));
        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, GTToolItems.empty(GTToolType.CROWBAR));
        InteractionResult result = GTDecorBlocks.LOOT_CRATE.get().use(level.getBlockState(pos), level, pos, player,
                InteractionHand.MAIN_HAND, hit(pos));
        helper.assertTrue(result.consumesAction(), "a crowbar interacts with the crate");
        helper.assertTrue(level.getBlockState(pos).isAir(), "the crate is consumed (GT6 setToAir)");
        List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(6));
        boolean crate = entities.stream().anyMatch(e -> e.getItem().is(LootCrateBlock.crateStack().getItem()));
        helper.assertTrue(crate, "GT6 hands the crate itself back (IL.Crate)");
        helper.assertTrue(entities.size() >= 2,
                "a vanilla loot roll is dropped as well, got " + entities.size() + " items");
        for (ItemEntity entity : entities) {
            helper.assertTrue(!entity.getItem().isEmpty(), "no empty loot stacks");
        }
        helper.succeed();
    }

    /** Anything else (e.g. a stick) leaves the crate alone. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void withoutACrowbarNothingHappens(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = placeCrate(level, base(1));
        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        InteractionResult result = GTDecorBlocks.LOOT_CRATE.get().use(level.getBlockState(pos), level, pos, player,
                InteractionHand.MAIN_HAND, hit(pos));
        helper.assertTrue(result == InteractionResult.PASS, "a stick is not a crowbar");
        helper.assertTrue(level.getBlockState(pos).is(GTDecorBlocks.LOOT_CRATE.get()), "the crate is still there");
        helper.assertTrue(droppedItems(level, pos) == 0, "nothing was dropped");
        helper.succeed();
    }

    /** GT6's vanilla loot roll: a random chest table, never an empty result. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void vanillaLootRollsAlwaysGiveSomething(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(LootCrateBlock.VANILLA_TABLES.size() == 10,
                "GT6's LOOT_TABLES_VANILLA list, got " + LootCrateBlock.VANILLA_TABLES.size());
        helper.assertTrue(LootCrateBlock.VANILLA_TABLES.get(0).getPath().equals("chests/simple_dungeon"),
                "the dungeon chest is GT6's fallback and comes first");
        int empty = 0;
        for (int seed = 0; seed < 40; seed++) {
            RandomSource random = RandomSource.create(seed);
            ResourceLocation table = LootCrateBlock.randomTable(random);
            helper.assertTrue(LootCrateBlock.VANILLA_TABLES.contains(table), "table from the GT6 list: " + table);
            List<ItemStack> loot = LootCrateBlock.rollVanillaLoot(level, helper.absolutePos(new BlockPos(1, 2, 1)),
                    RandomSource.create(seed));
            if (loot.isEmpty()) empty++;
            for (ItemStack stack : loot) {
                helper.assertTrue(!stack.isEmpty(), "no empty stacks in " + table);
            }
        }
        helper.assertTrue(empty == 0, "every roll of a vanilla chest table gives items (" + empty + " empty)");
        helper.succeed();
    }
}
