package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.block.misc.CoinItemExpireHandler;
import com.gregtech.gregtech.blockentity.misc.CoinPileBlockEntity;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** GT6's dropped coins retry every 200 ticks, merge, then spread into a fresh 4x4 pile. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class CoinItemExpireTests {
    private CoinItemExpireTests() {}

    @GameTest(template = "coin_pile_space")
    public static void eachCellHoldsAtMostSixteenAndWholePileTwoHundredFiftySix(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlock(pos, GTDecorBlocks.COIN_PILE.get().defaultBlockState(), 3);
        CoinPileBlockEntity pile = (CoinPileBlockEntity) helper.getLevel().getBlockEntity(pos);
        ItemStack silver = silver();
        helper.assertTrue(pile != null && pile.bindCoin(silver), "a silver coin pile can be bound");
        for (int count = 1; count <= 16; count++) {
            helper.assertTrue(pile.setFaceCount(0, count) == count, "face accepts " + count + " coins");
            var collision = helper.getLevel().getBlockState(pos).getCollisionShape(
                    helper.getLevel(), pos, CollisionContext.empty());
            helper.assertTrue(collision.max(Direction.Axis.Y) == count / 16.0,
                    "GT6 face height is exactly count/16 for " + count + " coins");
        }
        helper.assertTrue(pile.setFaceCount(0, 64) == 16, "GT6 caps each face at sixteen, not 64");
        for (int face = 1; face < CoinPileBlockEntity.FACES; face++) pile.setFaceCount(face, 16);
        helper.assertTrue(pile.total() == 256 && pile.absorbDroppedCoins(silver.copyWithCount(1)) == 0,
                "all sixteen full faces cap the whole pile at 256 coins");
        helper.assertTrue(pile.contents().size() == 4
                        && pile.contents().stream().allMatch(stack -> stack.getCount() == 64),
                "breaking a full pile splits its 256 coins into four drops of 64");
        helper.succeed();
    }

    @GameTest(template = "coin_pile_space")
    public static void expiredCoinsFillMatchingPileThenFormNextPile(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        BlockPos fresh = pos.north(); // GT6 checks self, down, then north.
        helper.getLevel().setBlock(fresh, Blocks.AIR.defaultBlockState(), 3);
        helper.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(pos, GTDecorBlocks.COIN_PILE.get().defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockState(fresh).isAir(),
                "the reserved north cell is empty before the dropped coins settle");
        CoinPileBlockEntity pile = (CoinPileBlockEntity) helper.getLevel().getBlockEntity(pos);
        ItemStack silver = silver();
        helper.assertTrue(pile != null && pile.bindCoin(silver), "silver pile is placed");
        for (int face = 0; face < 15; face++) pile.setFaceCount(face, 16);
        pile.setFaceCount(15, 10);
        helper.assertTrue(pile.total() == 250, "the existing pile has room for six coins");

        ItemEntity entity = dropped(helper, pos, silver.copyWithCount(10), true);
        helper.assertTrue(entity.lifespan == 200, "GT6 coins first expire after 200 ticks");
        helper.assertTrue(entity.onGround(), "the dropped coins are on the ground before expiry");
        ItemExpireEvent event = new ItemExpireEvent(entity, 0);
        CoinItemExpireHandler.onItemExpire(event);
        CoinPileBlockEntity second = (CoinPileBlockEntity) helper.getLevel().getBlockEntity(fresh);
        helper.assertTrue(event.isCanceled(), "coin expiry was handled by the pile hook");
        helper.assertTrue(pile.total() == 256, "six coins filled the old pile to 256");
        helper.assertTrue(second != null, "a new pile formed north of the full pile");
        helper.assertTrue(second.total() == 4, "the four remaining coins entered the new pile");
        helper.assertTrue(entity.isRemoved(), "all ten dropped coins were consumed");
        helper.assertTrue(ItemStack.isSameItemSameTags(second.coinItem(), silver),
                "the adjacent pile keeps the dropped coin material");
        helper.succeed();
    }

    @GameTest(template = "coin_pile_space")
    public static void expiredCoinsNeverMixDifferentMetals(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        helper.getLevel().setBlock(pos.north(), Blocks.AIR.defaultBlockState(), 3);
        helper.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(pos, GTDecorBlocks.COIN_PILE.get().defaultBlockState(), 3);
        CoinPileBlockEntity goldPile = (CoinPileBlockEntity) helper.getLevel().getBlockEntity(pos);
        ItemStack gold = GTItems.getStack(MaterialPrefix.coin, GTMaterialRegistry.get("Gold"));
        helper.assertTrue(goldPile != null && goldPile.add(0, gold) == 1, "gold pile starts with one coin");

        ItemStack silver = silver().copyWithCount(5);
        ItemEntity entity = dropped(helper, pos, silver, true);
        CoinItemExpireHandler.onItemExpire(new ItemExpireEvent(entity, 0));
        CoinPileBlockEntity silverPile = (CoinPileBlockEntity) helper.getLevel().getBlockEntity(pos.north());
        helper.assertTrue(goldPile.total() == 1 && ItemStack.isSameItemSameTags(goldPile.coinItem(), gold),
                "the gold pile did not absorb silver coins");
        helper.assertTrue(silverPile != null && silverPile.total() == 5
                        && ItemStack.isSameItemSameTags(silverPile.coinItem(), silver),
                "the silver coins formed their own pile without loss");
        helper.succeed();
    }

    @GameTest(template = "coin_pile_space")
    public static void airborneCoinRetriesAfterAnotherTwoHundredTicks(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(),
                "the airborne coin test starts with an empty target cell");
        ItemEntity entity = dropped(helper, pos, silver(), false);
        ItemExpireEvent event = new ItemExpireEvent(entity, 0);
        CoinItemExpireHandler.onItemExpire(event);
        helper.assertTrue(event.isCanceled(), "coin expiry was handled while airborne");
        helper.assertTrue(event.getExtraLife() == 200, "GT6 grants another 200 ticks before retrying");
        helper.assertTrue(!entity.isRemoved(), "the airborne coin remains an entity");
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(),
                "an airborne coin never creates a pile before landing");
        helper.succeed();
    }

    private static ItemStack silver() {
        return GTItems.getStack(MaterialPrefix.coin, GTMaterialRegistry.get("Silver"));
    }

    private static ItemEntity dropped(GameTestHelper helper, BlockPos pos, ItemStack stack, boolean grounded) {
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.getX() + .5,
                pos.getY() + .25, pos.getZ() + .5, stack);
        entity.setOnGround(grounded);
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }
}
