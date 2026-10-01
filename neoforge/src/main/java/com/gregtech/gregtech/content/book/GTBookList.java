package com.gregtech.gregtech.content.book;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * GT6's book registry ({@code LoaderBookList}, {@code BooksGT.BOOK_REGISTER} /
 * {@code BOOKS_NORMAL} / {@code BOOKS_ENCHANTED}) — what a {@link
 * com.gregtech.gregtech.block.BookShelfBlock GT book shelf} accepts and how much enchanting power
 * each stored item is worth.
 *
 * <p>GT6 keeps three sets: the {@code BOOK_REGISTER} map (everything that can be <em>displayed</em>
 * on a shelf — books, paper, maps, frames, buttons, levers, torches and stone decoys),
 * {@code BOOKS_NORMAL} (plain books, worth one enchantment point) and {@code BOOKS_ENCHANTED}
 * (enchanted books, worth two). The port's own manuals and material dictionaries are vanilla written
 * books (§21/§31), so they fall into {@code BOOKS_NORMAL} automatically. GT6's two dusty loot
 * books are separate items; both are also normal books.
 */
public final class GTBookList {
    private GTBookList() {}

    /** GT6 MultiItemBooks:67-68 registers both dusty loot books as normal shelf books. */
    private static final String DUSTY_GUIDE = "gregtech:dusty_guide_book";
    private static final String DUSTY_DICTIONARY = "gregtech:dusty_material_dictionary";

    private static boolean isDustyBook(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && (id.toString().equals(DUSTY_GUIDE) || id.toString().equals(DUSTY_DICTIONARY));
    }

    /** GT6 {@code BooksGT.BOOK_REGISTER}: everything that can sit on a shelf. */
    public static boolean canPlace(ItemStack stack) {
        return !stack.isEmpty() && BookShelfRules.canPlace(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    /** GT6 {@code BooksGT.BOOKS_NORMAL} — worth one enchantment point. */
    public static boolean isNormalBook(ItemStack stack) {
        return !stack.isEmpty() && BookShelfRules.enchantPower(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())==1;
    }

    /** GT6 {@code BooksGT.BOOKS_ENCHANTED} — worth two enchantment points. */
    public static boolean isEnchantedBook(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK);
    }

    /** Any book, plain or enchanted. */
    public static boolean isBook(ItemStack stack) {
        return isNormalBook(stack) || isEnchantedBook(stack);
    }

    /** GT6 excludes its button, lever, torch and cobblestone decoys from automated extraction. */
    public static boolean canAutoExtract(ItemStack stack) {
        return !stack.isEmpty() && BookShelfRules.canAutoExtract(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    /** GT6 {@code MultiTileEntityBookShelf.getEnchantPowerBonus}: +1 per book, +2 per enchanted book. */
    public static int enchantPower(ItemStack stack) {
        if (isEnchantedBook(stack)) return 2;
        if (isNormalBook(stack)) return 1;
        return 0;
    }

    /** The shelf's total enchanting power across its slots. */
    public static int enchantPower(ItemStackHandler inventory) {
        int points = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            points += enchantPower(inventory.getStackInSlot(slot));
        }
        return points;
    }

    /**
     * GT6's shelf holds 28 books: each face shows two rows of seven (front face 0..13, back face
     * 14..27 — {@code MultiTileEntityBookShelf}'s click formula and its {@code rng(14)} dungeon loot
     * both use that layout).
     */
    public static final int SLOTS = BookShelfRules.SLOTS;

    /** Slots per row on one face. */
    public static final int COLUMNS = BookShelfRules.COLUMNS;

    /**
     * GT6's click-to-slot mapping:
     * {@code (hitY < 0.5 ? 6 : 13) - clamp(0, 6, 8 * (hitX - 1/16))} for the front face, with the
     * back face using 20/27 instead of 6/13.
     *
     * @param front true when the clicked face is the shelf's facing side
     * @param hitX  hit x within the block (0..1, GT6's {@code tCoords[0]})
     * @param hitY  hit y within the block (0..1, GT6's {@code tCoords[1]})
     */
    public static int slotFor(boolean front, double hitX, double hitY) {
        return BookShelfRules.slotFor(front,hitX,hitY);
    }

    /** Unused, but keeps the vanilla bookshelf block referenced for the javadoc above. */
    static boolean isVanillaShelf(net.minecraft.world.level.block.Block block) {
        return block == Blocks.BOOKSHELF;
    }
}
