package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.BookShelfBlock;
import com.gregtech.gregtech.block.misc.CoinPileBlock;
import com.gregtech.gregtech.blockentity.inventory.BookShelfBlockEntity;
import com.gregtech.gregtech.blockentity.misc.CoinPileBlockEntity;
import com.gregtech.gregtech.content.tool.CoinGeometry;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class BookLootAndCoinTests {
    @GameTest(template = "test_empty")
    public static void shelfUsesLootTablesOnceAndKeepsReservedItems(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, GTDecorBlocks.BOOKSHELF.get());
        var absolute = helper.absolutePos(pos);
        var shelf = (BookShelfBlockEntity) helper.getLevel().getBlockEntity(absolute);
        shelf.setDungeonLoot(ResourceLocation.parse("gregtech_repair:shelf_paper"), ResourceLocation.parse("gregtech_repair:shelf_maps"), 1234);
        shelf.inventory().setStackInSlot(13, new ItemStack(Items.TRIPWIRE_HOOK));
        shelf.load(shelf.saveWithoutMetadata()); // Pending generation survives save/load.
        var player = helper.makeMockPlayer();
        GTDecorBlocks.BOOKSHELF.get().use(shelf.getBlockState(), helper.getLevel(), absolute, player,
                InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(absolute).add(.5, .75, 0), Direction.NORTH, absolute, false));
        helper.assertTrue(shelf.inventory().getStackInSlot(13).is(Items.TRIPWIRE_HOOK), "preplaced dungeon key is not overwritten");
        int paper = 0, maps = 0;
        for (int slot = 0; slot < 28; slot++) {
            var stack = shelf.inventory().getStackInSlot(slot);
            if (slot != 13 && !stack.isEmpty()) {
                helper.assertTrue(stack.is(slot < 14 ? Items.PAPER : Items.FILLED_MAP), "requested per-face table supplies the item");
                if (slot < 14) paper++; else maps++;
            }
            shelf.inventory().setStackInSlot(slot, ItemStack.EMPTY);
        }
        helper.assertTrue(paper > 0 && maps > 0, "both loot tables actually ran");
        shelf.load(shelf.saveWithoutMetadata());
        shelf.generateDungeonLoot();
        helper.assertTrue(shelf.contents().isEmpty(), "consumed loot cannot regenerate after save/load");
        for (int slot = 0; slot < 12; slot++) shelf.inventory().setStackInSlot(slot, new ItemStack(Items.BOOK));
        helper.assertTrue(GTDecorBlocks.BOOKSHELF.get().getEnchantPowerBonus(shelf.getBlockState(), helper.getLevel(), absolute) == 1,
                "twelve plain books supply one GT6 enchanting-power unit");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void coinsKeepExactCellsHeightsAndMintRelief(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, GTDecorBlocks.COIN_PILE.get());
        var absolute = helper.absolutePos(pos);
        var pile = (CoinPileBlockEntity) helper.getLevel().getBlockEntity(absolute);
        var tag = new CompoundTag();
        tag.putByte(CoinPileBlockEntity.NBT_STACKSIZE + 0, (byte) 1);
        tag.putByte(CoinPileBlockEntity.NBT_STACKSIZE + 15, (byte) 16);
        tag.put(CoinPileBlockEntity.NBT_COIN, CoinPileBlock.defaultCoin().save(new CompoundTag()));
        pile.load(tag);
        var shape = pile.getBlockState().getCollisionShape(helper.getLevel(), absolute);
        helper.assertTrue(!Shapes.joinIsNotEmpty(shape, Shapes.create(new AABB(.5, 0, .5, .75, 1, .75)), BooleanOp.AND), "empty cells do not collide");
        helper.assertTrue(!Shapes.joinIsNotEmpty(shape, Shapes.create(new AABB(0, .0625, 0, .25, 1, .25)), BooleanOp.AND), "one coin is only one sixteenth high");
        helper.assertTrue(Shapes.joinIsNotEmpty(shape, Shapes.create(new AABB(.75, .9, .75, 1, 1, 1)), BooleanOp.AND), "sixteen coins reach full height in their original corner");
        helper.assertTrue(CoinGeometry.depth(0, 0) == 3 && CoinGeometry.depth(0, 7) == 0, "GT6 mint silhouette retains cut corners and raised rim");
        helper.assertTrue(CoinGeometry.pixels(1).stream().anyMatch(b -> b.minY > 0), "mint engraving is real geometry");
        var client = new CoinPileBlockEntity(absolute, pile.getBlockState());
        client.load(pile.getUpdatePacket().getTag());
        helper.assertTrue(client.faceCount(0) == 1 && client.faceCount(15) == 16 && client.faceCount(7) == 0, "individual cells synchronize");
        helper.succeed();
    }
}
