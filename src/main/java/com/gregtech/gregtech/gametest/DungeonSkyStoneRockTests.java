package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.RockBlock;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.blockentity.RockBlockEntity;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.GT6Materials;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkBarracks;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;

/** GT6 barracks multi-tile 32074 contains one stack of Sky Stone rocks, not one loose pebble. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class DungeonSkyStoneRockTests {
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void barracksSkyStoneRockKeepsFullStackAndDropsIt(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(48, 70, 48));
        GTDungeonData data = new GTDungeonData(helper.getLevel(), origin.getX(), origin.getY(), origin.getZ(),
                StoneType.LIMESTONE, StoneType.SLATE, 3, new byte[5][5], 2, 2, 0,
                new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                RandomSource.create(81));
        helper.assertTrue(new GTDungeonChunkBarracks().generate(data), "barracks builds");

        BlockPos pos = origin.offset(14, 2, 11);
        var state = helper.getLevel().getBlockState(pos);
        helper.assertTrue(state.is(GTBlocks.ROCK.get()) && state.getBlock() instanceof RockBlock,
                "GT6 placed rock, not empty air");
        var be = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(be instanceof RockBlockEntity, "placed rock stores GT material and quantity");
        RockBlockEntity rock = (RockBlockEntity) be;
        helper.assertTrue(rock.getMaterial().equals(GT6Materials.Stones.SkyStone.getName())
                        && rock.count() >= 16 && rock.count() <= 64,
                "original Sky Stone and 16..64 rock count");
        int amount = rock.count();
        var saved = rock.saveWithoutMetadata();
        rock.setCount(1);
        rock.load(saved);
        helper.assertTrue(rock.count() == amount, "placed stack survives NBT round trip");

        helper.assertTrue(helper.getLevel().destroyBlock(pos, true), "break GT placed rock");
        var expected = GTItems.getStack(MaterialPrefix.rockGt, GT6Materials.Stones.SkyStone, 1);
        helper.assertTrue(!expected.isEmpty(), "Sky Stone rock item is registered");
        helper.runAfterDelay(1, () -> {
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
            helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().getCount() == amount
                            && ItemStack.isSameItemSameTags(drop.getItem(), expected)),
                    "breaking yields the entire Sky Stone stack");
            helper.succeed();
        });
    }
}
