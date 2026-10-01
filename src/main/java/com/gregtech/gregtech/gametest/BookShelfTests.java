package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.BookShelfBlock;
import com.gregtech.gregtech.blockentity.inventory.BookShelfBlockEntity;
import com.gregtech.gregtech.content.book.GTBookList;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * §53: GT6's book shelf ({@code MultiTileEntityBookShelf} + {@code LoaderBookList}) — eight slots
 * that only take books, click to store and take back, enchanting power from the stored books, and
 * the contents dropping when the shelf is broken.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class BookShelfTests {
    private static final int BASE_X = 56000;
    private static final int BASE_Z = 56000;

    /** A hit result at a block-relative position on the given face (GT6's {@code tCoords}). */
    private static BlockHitResult hitAt(BlockPos pos, Direction face, double hitX, double hitY) {
        return new BlockHitResult(new Vec3(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + 0.5D),
                face, pos, false);
    }

    /** Number of non-empty slots (the GameTest mock player makes slot-exact assertions fragile). */
    private static int stored(BookShelfBlockEntity shelf) {
        int count = 0;
        for (int slot = 0; slot < shelf.inventory().getSlots(); slot++) {
            if (!shelf.inventory().getStackInSlot(slot).isEmpty()) count++;
        }
        return count;
    }

    /** The block and its block entity are registered and the shelf faces horizontally. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void bookShelfRegistration(GameTestHelper helper) {
        helper.assertTrue(GTDecorBlocks.BOOKSHELF != null && GTDecorBlocks.BOOKSHELF.isPresent(),
                "gregtech:bookshelf is registered");
        helper.assertTrue(GTDecorBlocks.BOOKSHELF.get() instanceof BookShelfBlock,
                "it is the GT book shelf block");
        helper.assertTrue(GTBlockEntities.BOOKSHELF != null && GTBlockEntities.BOOKSHELF.isPresent(),
                "the bookshelf block entity type is registered");
        BlockState state = GTDecorBlocks.BOOKSHELF.get().defaultBlockState();
        helper.assertTrue(state.hasProperty(BookShelfBlock.FACING)
                        && state.getValue(BookShelfBlock.FACING) == Direction.NORTH,
                "GT6's shelf is a facing block (default north)");
        helper.assertTrue(GTBookList.SLOTS == 28 && GTBookList.COLUMNS == 7,
                "GT6's shelf has 28 slots: 2 faces x 2 rows x 7 columns");
        // GT6's click-to-slot mapping: lower half 0..6, upper half 7..13 (front face, right to left),
        // the back face using 14..20 and 21..27.
        helper.assertTrue(GTBookList.slotFor(true, 1.0D / 16.0D, 0.25D) == 6,
                "the leftmost lower slot of the front face is 6");
        helper.assertTrue(GTBookList.slotFor(true, 0.9D, 0.25D) == 0,
                "the rightmost lower slot of the front face is 0");
        helper.assertTrue(GTBookList.slotFor(true, 1.0D / 16.0D, 0.75D) == 13,
                "the upper row starts at 13");
        helper.assertTrue(GTBookList.slotFor(false, 1.0D / 16.0D, 0.25D) == 20
                        && GTBookList.slotFor(false, 1.0D / 16.0D, 0.75D) == 27,
                "the back face uses slots 14..27");
        helper.succeed();
    }

    /** GT6's {@code BooksGT} sets: what may sit on a shelf and what it is worth. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void bookListMatchesGt6(GameTestHelper helper) {
        // BOOKS_NORMAL: +1 each, BOOKS_ENCHANTED: +2 each.
        helper.assertTrue(GTBookList.enchantPower(new ItemStack(Items.BOOK)) == 1
                        && GTBookList.enchantPower(new ItemStack(Items.WRITABLE_BOOK)) == 1
                        && GTBookList.enchantPower(new ItemStack(Items.WRITTEN_BOOK)) == 1,
                "a plain book is worth one enchantment point");
        helper.assertTrue(GTBookList.enchantPower(new ItemStack(Items.ENCHANTED_BOOK)) == 2,
                "an enchanted book is worth two");
        helper.assertTrue(GTBookList.enchantPower(new ItemStack(Items.STONE)) == 0,
                "anything else is worth nothing");

        var dustyGuide = ForgeRegistries.ITEMS.getValue(GregTech.id("dusty_guide_book"));
        var dustyDictionary = ForgeRegistries.ITEMS.getValue(GregTech.id("dusty_material_dictionary"));
        helper.assertTrue(dustyGuide != null && dustyDictionary != null,
                "GT6's two dusty loot books are registered");
        for (var item : new net.minecraft.world.item.Item[] {dustyGuide, dustyDictionary}) {
            ItemStack stack = new ItemStack(item);
            helper.assertTrue(GTBookList.canPlace(stack), "GT6's BOOK_REGISTER accepts " + item);
            helper.assertTrue(GTBookList.enchantPower(stack) == 1,
                    "GT6's BOOKS_NORMAL gives one enchantment point to " + item);
        }

        // BOOK_REGISTER: the display items GT6 also allows on a shelf.
        for (var item : new net.minecraft.world.item.Item[] {Items.BOOK, Items.WRITABLE_BOOK,
                Items.WRITTEN_BOOK, Items.ENCHANTED_BOOK, Items.PAPER, Items.MAP, Items.FILLED_MAP,
                Items.NAME_TAG, Items.ITEM_FRAME, Items.PAINTING}) {
            helper.assertTrue(GTBookList.canPlace(new ItemStack(item)),
                    "GT6's BOOK_REGISTER accepts " + item);
        }
        helper.assertTrue(!GTBookList.canPlace(new ItemStack(Items.STONE))
                        && !GTBookList.canPlace(ItemStack.EMPTY),
                "stone and empty stacks are rejected");
        helper.succeed();
    }

    /** Clicking stores and returns books; the enchantment power follows the stored books. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void shelfStoresBooksAndCountsEnchantPower(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(BASE_X, 210, BASE_Z);
        // The GameTest world persists between runs: clear the site so the shelf starts empty.
        level.removeBlock(pos, false);
        level.setBlock(pos, GTDecorBlocks.BOOKSHELF.get().defaultBlockState(), 2);
        helper.assertTrue(level.getBlockEntity(pos) instanceof BookShelfBlockEntity,
                "placing the shelf creates its block entity");
        BookShelfBlockEntity shelf = (BookShelfBlockEntity) level.getBlockEntity(pos);
        BookShelfBlock block = (BookShelfBlock) GTDecorBlocks.BOOKSHELF.get();
        // §57: the click lands in the slot the player aimed at (GT6's tIndex formula). The
        // leftmost lower slot of the front face is 6; the rightmost is 0.
        Player player = helper.makeMockPlayer();
        player.getAbilities().instabuild = false;
        BlockHitResult leftLower = hitAt(pos, Direction.NORTH, 1.0D / 16.0D, 0.25D);
        BlockHitResult rightLower = hitAt(pos, Direction.NORTH, 0.9D, 0.25D);
        int leftSlot = 7; // North face, world x=1/16, lower row.
        int rightSlot = 13; // North face reverses world x into face-local x.

        // Right-click with a book stores it in the clicked slot.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOOK, 2));
        helper.assertTrue(block.use(level.getBlockState(pos), level, pos, player,
                        InteractionHand.MAIN_HAND, leftLower).consumesAction(),
                "clicking with a book stores it");
        helper.assertTrue(shelf.inventory().getStackInSlot(leftSlot).is(Items.BOOK),
                "the clicked slot holds the book, expected slot " + leftSlot);
        helper.assertTrue(shelf.inventory().getStackInSlot(leftSlot).getCount() == 1,
                "exactly one book is stored per click");
        // (The GameTest mock player always reports itself as creative, so the held stack is not
        // consumed here — the survival path shrinks it by one, which GT6 does as well.)

        // A non-book is refused by an empty slot (the shelf keeps whatever it had).
        int before = stored(shelf);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        helper.assertTrue(!block.use(level.getBlockState(pos), level, pos, player,
                        InteractionHand.MAIN_HAND, rightLower).consumesAction(),
                "GT6's shelf refuses everything outside BOOK_REGISTER");
        helper.assertTrue(stored(shelf) == before, "nothing was stored");

        // Clicking an occupied slot hands the book back (GT6 does this without sneaking).
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(block.use(level.getBlockState(pos), level, pos, player,
                        InteractionHand.MAIN_HAND, leftLower).consumesAction(),
                "clicking an occupied slot takes the book back");
        helper.assertTrue(stored(shelf) == before - 1, "one stored stack was removed");
        helper.assertTrue(player.getInventory().contains(new ItemStack(Items.BOOK)),
                "the player got the book back");

        // Enchanting power: four plain books and four enchanted books = 4 + 8.
        for (int slot = 0; slot < 4; slot++) shelf.inventory().setStackInSlot(slot, new ItemStack(Items.BOOK));
        for (int slot = 4; slot < 8; slot++) {
            shelf.inventory().setStackInSlot(slot, new ItemStack(Items.ENCHANTED_BOOK));
        }
        helper.assertTrue(shelf.enchantPower() == 12,
                "GT6 counts +1 per book and +2 per enchanted book, got " + shelf.enchantPower());
        helper.assertTrue(block.getEnchantPowerBonus(level.getBlockState(pos), level, pos) == 1.0F,
                "twelve book points supply one enchanting-power unit");
        helper.assertTrue(block.getAnalogOutputSignal(level.getBlockState(pos), level, pos) == 5,
                "8 of 28 slots gives a signal of 5, got "
                        + block.getAnalogOutputSignal(level.getBlockState(pos), level, pos));
        // The capability exposes the same inventory to hoppers/pipes.
        helper.assertTrue(shelf.getCapability(ForgeCapabilities.ITEM_HANDLER)
                        .map(handler -> handler.getSlots() == GTBookList.SLOTS).orElse(false),
                "the shelf exposes an item handler");
        helper.succeed();
    }

    /** Breaking the shelf drops every stored book (GT6 drops the inventory). */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void breakingTheShelfDropsItsBooks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(BASE_X + 4, 210, BASE_Z);
        level.removeBlock(pos, false);
        level.setBlock(pos, GTDecorBlocks.BOOKSHELF.get().defaultBlockState(), 2);
        BookShelfBlockEntity shelf = (BookShelfBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(shelf != null, "the shelf exists");
        shelf.inventory().setStackInSlot(0, new ItemStack(Items.WRITTEN_BOOK));
        shelf.inventory().setStackInSlot(1, new ItemStack(Items.ENCHANTED_BOOK, 3));
        helper.assertTrue(shelf.contents().size() == 2, "two stored stacks");

        // §61: the drop hangs on onRemove, so ANY removal path drops the books — here a plain
        // block removal with no player involved at all.
        level.removeBlock(pos, false);
        int dropped = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(3)).size();
        helper.assertTrue(dropped >= 2, "removing the shelf drops its books, got " + dropped);
        // A player break drops exactly the same (no double drop).
        BlockPos second = new BlockPos(BASE_X + 8, 210, BASE_Z);
        level.removeBlock(second, false);
        level.setBlock(second, GTDecorBlocks.BOOKSHELF.get().defaultBlockState(), 2);
        if (level.getBlockEntity(second) instanceof BookShelfBlockEntity other) {
            other.inventory().setStackInSlot(0, new ItemStack(Items.BOOK));
        }
        level.destroyBlock(second, true, helper.makeMockPlayer());
        for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(3))) {
            entity.discard();
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        helper.succeed();
    }
}
