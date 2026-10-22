package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.block.misc.PileBlock;
import com.gregtech.gregtech.block.misc.PileItemExpireHandler;
import com.gregtech.gregtech.block.misc.PilePlacementHandler;
import com.gregtech.gregtech.blockentity.misc.PileBlockEntity;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class PileShapeAndSyncTests {
    /** GT6 GT_Proxy:307-326 places the whole sneaked material stack, including unified iron. */
    @GameTest(template = "test_empty")
    public static void sneakClickPlacesStockedMaterialPiles(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer();
        player.getAbilities().instabuild = false;
        int index = 0;
        for (PileBlock.Kind kind : PileBlock.Kind.values()) {
            BlockPos clicked = helper.absolutePos(new BlockPos(2 + index * 4, 2, 2));
            BlockPos target = clicked.above();
            level.setBlock(clicked, Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
            ItemStack material = kind == PileBlock.Kind.INGOT ? new ItemStack(Items.IRON_INGOT) : ItemStack.EMPTY;
            if (material.isEmpty()) for (var candidate : GTMaterialRegistry.sortedMaterials()) {
                material = GTItems.getStack(kind.prefix(), candidate);
                if (!material.isEmpty()) break;
            }
            helper.assertTrue(!material.isEmpty(), "a placeable material exists for " + kind);
            int amount = kind == PileBlock.Kind.INGOT ? 9 : kind == PileBlock.Kind.PLATE ? 5 : 3;
            ItemStack held = material.copyWithCount(amount);
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            BlockHitResult hit = new BlockHitResult(new Vec3(clicked.getX() + .5, clicked.getY() + 1,
                    clicked.getZ() + .5), Direction.UP, clicked, false);

            helper.assertTrue(PilePlacementHandler.tryPlace(level, player, InteractionHand.MAIN_HAND, hit)
                            == InteractionResult.PASS && level.getBlockState(target).isAir(),
                    kind + " needs GT6's sneak key before it is placed");
            player.setShiftKeyDown(true);
            helper.assertTrue(PilePlacementHandler.tryPlace(level, player, InteractionHand.MAIN_HAND, hit)
                            .consumesAction(), kind + " sneak-click places its pile");
            helper.assertTrue(level.getBlockState(target).is(kind.block()), kind + " placed its own form");
            var pile = (PileBlockEntity) level.getBlockEntity(target);
            helper.assertTrue(pile != null && pile.count() == amount
                            && ItemStack.isSameItemSameTags(pile.stored(), material),
                    kind + " holds the exact source material and whole count");
            helper.assertTrue(held.isEmpty(), kind + " consumed the held stack in survival");
            helper.assertTrue(level.getBlockState(target).getValue(PileBlock.STACK) == amount,
                    kind + " selected its model for " + amount + " pieces");
            var collision = level.getBlockState(target)
                    .getCollisionShape(level, target, CollisionContext.empty());
            double collisionHeight = collision.isEmpty() ? 0 : collision.max(Direction.Axis.Y);
            double expected = kind == PileBlock.Kind.INGOT ? .125
                    : kind == PileBlock.Kind.PLATE ? .0625 : 0;
            helper.assertTrue(collisionHeight == expected,
                    kind + " placed collision height " + collisionHeight + " differs from GT6's " + expected);

            // A solid target leaves both the held stack and the existing block alone.
            BlockPos blocked = clicked.east();
            level.setBlock(blocked, Blocks.STONE.defaultBlockState(), 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, material.copyWithCount(2));
            BlockHitResult blockedHit = new BlockHitResult(new Vec3(clicked.getX() + 1, clicked.getY() + .5,
                    clicked.getZ() + .5), Direction.EAST, clicked, false);
            helper.assertTrue(PilePlacementHandler.tryPlace(level, player, InteractionHand.MAIN_HAND, blockedHit)
                            == InteractionResult.PASS && level.getBlockState(blocked).is(Blocks.STONE)
                            && player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 2,
                    kind + " cannot replace solid stone");
            player.setShiftKeyDown(false);
            index++;
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void allPileHeightsMatchGt6(GameTestHelper helper) {
        for (var kind : PileBlock.Kind.values()) {
            var block = kind.block();
            int perLayer = kind == PileBlock.Kind.INGOT ? 8 : 4;
            double layerHeight = kind == PileBlock.Kind.INGOT ? 0.125 : 0.0625;
            for (int count = 0; count <= 64; count++) {
                var state = block.defaultBlockState().setValue(PileBlock.STACK, count);
                var selected = state.getShape(helper.getLevel(), BlockPos.ZERO);
                double expected = Math.max(0.0625, ((count + perLayer - 1) / perLayer) * layerHeight);
                helper.assertTrue(selected.max(Direction.Axis.Y) == expected, kind + " selection height " + count);
                var collision = state.getCollisionShape(helper.getLevel(), BlockPos.ZERO, CollisionContext.empty());
                double solidHeight = (count / perLayer) * layerHeight;
                helper.assertTrue(solidHeight == 0 ? collision.isEmpty() : collision.max(Direction.Axis.Y) == solidHeight,
                        kind + " collision height " + count);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void pileMaterialReachesClientPacket(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, PileBlock.Kind.INGOT.block());
        var server = (PileBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        server.add(new ItemStack(Items.GOLD_INGOT, 9));
        var packet = server.getUpdatePacket();
        helper.assertTrue(packet != null && packet.getTag() != null, "material packet exists");
        var client = new PileBlockEntity(server.getBlockPos(), server.getBlockState());
        client.load(packet.getTag());
        helper.assertTrue(client.stored().is(Items.GOLD_INGOT) && client.count() == 9, "client gets material and amount for tint");
        helper.succeed();
    }

    /** GT6 GT_API_Proxy:1468-1470 places each form from the expired material item itself. */
    @GameTest(template = "test_empty")
    public static void expiredMaterialStacksBecomeStockedPiles(GameTestHelper helper) {
        int x = 1;
        for (PileBlock.Kind kind : PileBlock.Kind.values()) {
            ItemStack material = ItemStack.EMPTY;
            for (var candidate : GTMaterialRegistry.sortedMaterials()) {
                material = GTItems.getStack(kind.prefix(), candidate);
                if (!material.isEmpty()) break;
            }
            helper.assertTrue(!material.isEmpty(), "a material exists for " + kind);
            BlockPos target = helper.absolutePos(new BlockPos(x, 2, 1));
            helper.getLevel().setBlock(target, Blocks.AIR.defaultBlockState(), 3);
            ItemStack dropped = material.copyWithCount(5);
            ItemEntity entity = new ItemEntity(helper.getLevel(), target.getX() + 0.5,
                    target.getY() + 0.25, target.getZ() + 0.5, dropped);
            helper.assertTrue(helper.getLevel().addFreshEntity(entity), "the item entity enters the level");
            ItemExpireEvent event = new ItemExpireEvent(entity, 0);
            PileItemExpireHandler.onItemExpire(event);
            helper.assertTrue(event.isCanceled() && entity.isRemoved(), kind + " was consumed by the pile");
            helper.assertTrue(helper.getLevel().getBlockState(target).is(kind.block()),
                    kind + " replaced the expired item at its position");
            var pile = (PileBlockEntity) helper.getLevel().getBlockEntity(target);
            helper.assertTrue(pile != null && pile.count() == 5
                            && ItemStack.isSameItemSameTags(pile.stored(), dropped),
                    kind + " retains the exact five items for material tint and drops");
            helper.assertTrue(helper.getLevel().getBlockState(target).getValue(PileBlock.STACK) == 5,
                    kind + " updates its model and collision state to five items");
            x += 4;
        }
        helper.succeed();
    }

    /** GT6 checks its own cell before the one below; it never overwrites solid stone. */
    @GameTest(template = "test_empty")
    public static void expiredIngotSearchesAdjacentEmptyCell(GameTestHelper helper) {
        BlockPos blocked = helper.absolutePos(new BlockPos(2, 3, 2));
        BlockPos below = blocked.below();
        helper.getLevel().setBlock(blocked, Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(below, Blocks.AIR.defaultBlockState(), 3);
        ItemEntity entity = new ItemEntity(helper.getLevel(), blocked.getX() + 0.5,
                blocked.getY() + 0.5, blocked.getZ() + 0.5, new ItemStack(Items.GOLD_INGOT, 3));
        ItemExpireEvent event = new ItemExpireEvent(entity, 0);
        PileItemExpireHandler.onItemExpire(event);
        helper.assertTrue(event.isCanceled() && helper.getLevel().getBlockState(blocked).is(Blocks.STONE),
                "the expired ingots leave the solid block untouched");
        helper.assertTrue(helper.getLevel().getBlockState(below).is(PileBlock.Kind.INGOT.block()),
                "GT6's second CUBE_3 position receives the pile");
        var pile = (PileBlockEntity) helper.getLevel().getBlockEntity(below);
        helper.assertTrue(pile != null && pile.count() == 3, "no ingot was lost");
        helper.succeed();
    }
}
