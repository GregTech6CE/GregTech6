package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.BookShelfBlock;
import com.gregtech.gregtech.blockentity.inventory.BookShelfBlockEntity;
import com.gregtech.gregtech.content.book.BookShelfGeometry;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class BookShelfGeometryTests {
    @GameTest(template = "test_empty")
    public static void everyVisibleBookIsClickableInEveryFacing(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var absolute = helper.absolutePos(pos);
        var block = GTDecorBlocks.BOOKSHELF.get();
        var player = helper.makeMockPlayer();
        for (var facing : Direction.Plane.HORIZONTAL) {
            var state = block.defaultBlockState().setValue(BookShelfBlock.FACING, facing);
            helper.setBlock(pos, state);
            var shelf = (BookShelfBlockEntity) helper.getLevel().getBlockEntity(absolute);
            for (int slot = 0; slot < 28; slot++) {
                var center = BookShelfGeometry.bookBounds(slot).getCenter();
                double x = center.x, z = center.z;
                Vec3 local = switch (facing) {
                    case EAST -> new Vec3(1 - z, center.y, x);
                    case SOUTH -> new Vec3(1 - x, center.y, 1 - z);
                    case WEST -> new Vec3(z, center.y, 1 - x);
                    default -> center;
                };
                var side = slot < 14 ? facing : facing.getOpposite();
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOOK));
                block.use(state, helper.getLevel(), absolute, player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(local.add(Vec3.atLowerCornerOf(absolute)), side, absolute, false));
                helper.assertTrue(shelf.inventory().getStackInSlot(slot).is(Items.BOOK), facing + " slot " + slot);
            }
            helper.assertTrue(BookShelfGeometry.slotAt(facing, Direction.UP, .5, 1, .5) == -1, "top rejects interaction");
            helper.assertTrue(BookShelfGeometry.slotAt(facing, facing, .5, .99, .5) == -1, "frame rejects interaction");
            for (int slot = 0; slot < 28; slot++) shelf.inventory().setStackInSlot(slot, ItemStack.EMPTY);
            var selection = state.getShape(helper.getLevel(), absolute);
            helper.assertTrue(selection.min(facing.getAxis()) == .125 && selection.max(facing.getAxis()) == .875,
                    "GT6 inset selection along facing");
            var collision = state.getCollisionShape(helper.getLevel(), absolute).bounds();
            helper.assertTrue(collision.minX == 0 && collision.minY == 0 && collision.minZ == 0
                            && collision.maxX == 1 && collision.maxY == 1 && collision.maxZ == 1,
                    "GT6 collision is a full cube on every axis, distinct from the inset selection");
            helper.assertTrue(state.getLightBlock(helper.getLevel(), absolute) == 0,
                    "GT6's non-opaque wooden shelf passes block light despite its full collision");
            for (var side : Direction.values()) {
                boolean sturdy = side != facing && side != facing.getOpposite();
                helper.assertTrue(state.isFaceSturdy(helper.getLevel(), absolute, side) == sturdy,
                        facing + " shelf surface " + side + " matches GT6 side solidity");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void booksSynchronizeAndCapabilitiesRevive(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, GTDecorBlocks.BOOKSHELF.get());
        var shelf = (BookShelfBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        shelf.inventory().setStackInSlot(27, new ItemStack(Items.ENCHANTED_BOOK));
        var client = new BookShelfBlockEntity(shelf.getBlockPos(), shelf.getBlockState());
        client.load(shelf.getUpdatePacket().getTag());
        helper.assertTrue(client.inventory().getStackInSlot(27).is(Items.ENCHANTED_BOOK), "back face book synchronized");
        var old = shelf.getCapability(ForgeCapabilities.ITEM_HANDLER);
        shelf.invalidateCaps();
        helper.assertTrue(!old.isPresent(), "old capability invalidated");
        shelf.reviveCaps();
        helper.assertTrue(shelf.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent(), "new capability available");
        var remainder = shelf.inventory().insertItem(0, new ItemStack(Items.BOOK, 64), false);
        helper.assertTrue(shelf.inventory().getStackInSlot(0).getCount() == 1 && remainder.getCount() == 63,
                "automation respects one book per displayed slot");
        helper.succeed();
    }
}
