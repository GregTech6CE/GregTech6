package com.gregtech.gregtech.worldgen.dungeon;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Port of GT6's {@code DungeonChunkRoomLibrary}
 * ({@code gregapi/worldgen/dungeon/DungeonChunkRoomLibrary.java:36-197}): the interior shared by GT6's
 * library rooms - a wooden reading hall inside the stone cell, with a carpet, four walls of bookshelves
 * between the corner posts, the plank row that tops those walls, a wooden coffered ceiling and a set of
 * furniture in the middle of the hall.
 *
 * <p>GT6 has three library rooms and all of them call this class ({@code :36}):
 * {@code DungeonChunkRoomLibraryNormal} plus a Mystcraft and a Thaumcraft variant. The port only keeps
 * the normal one (the other two are other-mod content), but keeps GT6's split, because this shared
 * interior is the bulk of the room - {@link GTDungeonChunkRoomLibraryNormal} is the thin subclass.</p>
 *
 * <p>Exactly like GT6's class, this one extends {@link GTDungeonChunkRoomEmpty}, so the shell (floor,
 * walls, ceiling, the four doorways) is built first and the interior is written over it; GT6's
 * {@code DungeonChunkRoomLibrary:39} returns false when that shell refuses the cell.</p>
 *
 * <h2>Port differences</h2>
 * <ul>
 *   <li><b>Wood.</b> GT6 rolls one of the six vanilla woods ({@code aData.next(6)}, {@code :42}) and uses
 *       the matching {@code Blocks.planks} meta, the same wood as an upside-down slab ({@code tSlab =
 *       tPlank + 8}) and the matching bookshelf multi-tile ({@code tShelf = 7000 + tPlank}). The port
 *       rolls the same and uses the same six vanilla plank blocks in GT6's order plus their TOP slab;
 *       its bookshelf has no wood variant, so the shelf multi-tile id is dropped.</li>
 *   <li><b>Carpet.</b> GT6 paints {@code Blocks.carpet} with {@code mColorInversed} ({@code :47}). The
 *       port maps that to vanilla's sixteen carpet blocks in GT6's dye order - the same convention
 *       {@link GTDungeonData} uses for its concrete and glass families.</li>
 *   <li><b>Shelves.</b> GT6 passes each shelf a facing side and one of nine vanilla chest loot tables
 *       ({@code :44}, defaulting to {@code ChestGenHooks.STRONGHOLD_LIBRARY}). The port's shelf helper
 *       takes a single table and has no facing, so every shelf of this room asks for
 *       {@code stronghold_library}.</li>
 *   <li><b>Keys.</b> GT6 rolls which of the 24 shelves of a row holds one of the five dungeon keys
 *       ({@code next(24)}), which key that is ({@code next(3) + next(3)}), stores the key stack in that
 *       shelf's inventory ({@code :122-128}) and marks the key as generated. The port does all of that
 *       with its real key items; GT6's {@code next(14)} is the shelf slot of the front half, which the
 *       port's 28 slot shelf has as well.</li>
 *   <li><b>Cups and other-mod blocks.</b> GT6's cups ({@code :170-191}) hold a night vision potion and may
 *       be replaced by a Hexxit hexorium block. Without Hexxit they are GT cups containing that potion;
 *       the optional block choice still consumes GT6's room rolls. The enchanting table, crafting table,
 *       jukebox and ender chest of that switch are vanilla and are kept.</li>
 *   <li><b>Rolls.</b> GT6's coordinates, its height (the hall is GT6's nine block tall
 *       {@link GTDungeonChunkRoomEmpty}, ceiling lamps included, which the port writes with GT6's
 *       {@code +1} redstone brick) and the order of every write are kept. Only the rolls that pick a
 *       variant the port cannot express are dropped, and each one is named above: the bookshelf id, the
 *       shelf facing and loot roll of GT6's shelf helper, and the Hexxit block of a pot or cup.</li>
 * </ul>
 */
public class GTDungeonChunkRoomLibrary extends GTDungeonChunkRoomEmpty {

    /** GT6's tag for this room ({@code WorldgenDungeonGT.TAG_LIBRARY}, {@code WorldgenDungeonGT:76}). */
    public static final String TAG_LIBRARY = "gt.dungeon.library";

    /** GT6's default shelf table ({@code ChestGenHooks.STRONGHOLD_LIBRARY}, {@code :44}). */
    protected static final String SHELF_LOOT = "stronghold_library";

    /** GT6's {@code Blocks.planks} metas 0..5, in GT6's order (the six vanilla woods). */
    private static final Block[] PLANKS = {
            Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS,
            Blocks.JUNGLE_PLANKS, Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS};

    /** The same six woods as slabs; GT6 uses them upside down ({@code tSlab = tPlank + 8}). */
    private static final Block[] SLABS = {
            Blocks.OAK_SLAB, Blocks.SPRUCE_SLAB, Blocks.BIRCH_SLAB,
            Blocks.JUNGLE_SLAB, Blocks.ACACIA_SLAB, Blocks.DARK_OAK_SLAB};

    /** Vanilla's sixteen carpets in GT6's dye order (GT6 uses {@code Blocks.carpet} meta mColorInversed). */
    private static final Block[] CARPETS = {
            Blocks.WHITE_CARPET, Blocks.ORANGE_CARPET, Blocks.MAGENTA_CARPET, Blocks.LIGHT_BLUE_CARPET,
            Blocks.YELLOW_CARPET, Blocks.LIME_CARPET, Blocks.PINK_CARPET, Blocks.GRAY_CARPET,
            Blocks.LIGHT_GRAY_CARPET, Blocks.CYAN_CARPET, Blocks.PURPLE_CARPET, Blocks.BLUE_CARPET,
            Blocks.BROWN_CARPET, Blocks.GREEN_CARPET, Blocks.RED_CARPET, Blocks.BLACK_CARPET};

    /**
     * The 24 shelves of a shelf row in GT6's write order ({@code :130-156}), as {x, z} pairs. GT6 walks
     * its key list down this order ({@code tIndex--==0?tList:null}), so the shelf at
     * {@code next(24)} is the one that gets the dungeon key.
     */
    private static final int[][] SHELF_ORDER = {
            {2, 1}, {3, 1}, {4, 1}, {11, 1}, {12, 1}, {13, 1},
            {14, 2}, {14, 3}, {14, 4}, {14, 11}, {14, 12}, {14, 13},
            {2, 14}, {3, 14}, {4, 14}, {11, 14}, {12, 14}, {13, 14},
            {1, 2}, {1, 3}, {1, 4}, {1, 11}, {1, 12}, {1, 13}};

    @Override
    public boolean generate(GTDungeonData data) {
        if (!super.generate(data)) return false;
        data.tags.add(TAG_LIBRARY);

        // GT6's :42: the wood of the whole interior - its planks and its upside down slabs. GT6's bookshelf
        // multi-tile id (tShelf = 7000 + tPlank) has no equivalent in the port, which has one bookshelf.
        int plank = data.next(6);
        BlockState planks = PLANKS[plank].defaultBlockState();
        BlockState topSlab = SLABS[plank].defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);

        // GT6's :46-48: the carpet of the reading hall, painted with the inversed dungeon colour.
        BlockState carpet = CARPETS[data.colorInversed].defaultBlockState();
        for (int tX = 2; tX <= 13; tX++) {
            for (int tZ = 2; tZ <= 13; tZ++) data.set(tX, 1, tZ, carpet);
        }

        // GT6's :50-61: the wooden ceiling. Planks at local 7 replace the shell's ceiling over the whole
        // hall, a bottom slab at local 8 covers them, and the visible coffered layer at local 6 is a ring
        // of planks with a cornice of top slabs at local 5, a lamp on GT6's 4x4 lamp grid, and top slabs
        // between them. GT6's 7 and 8 are the port's {@link GTDungeonData#ROOM_CEILING_Y} and
        // {@link GTDungeonData#ROOM_SKIN_Y}.
        for (int tX = 1; tX <= 14; tX++) {
            for (int tZ = 1; tZ <= 14; tZ++) {
                data.set(tX, GTDungeonData.ROOM_CEILING_Y, tZ, planks);
                data.set(tX, GTDungeonData.ROOM_SKIN_Y, tZ, SLABS[plank].defaultBlockState());
                if (tX == 1 || tX == 14 || tZ == 1 || tZ == 14) {
                    data.set(tX, 6, tZ, planks);
                    data.set(tX, 5, tZ, topSlab);
                } else if ((tX == 3 || tX == 6 || tX == 9 || tX == 12)
                        && (tZ == 3 || tZ == 6 || tZ == 9 || tZ == 12)) {
                    // GT6's :57: the +1 redstone brick above the lamp stays inside the hall's own ceiling.
                    data.lamp(tX, 6, tZ, +1);
                } else {
                    data.set(tX, 6, tZ, topSlab);
                }
            }
        }

        // GT6's :63-85: the shelf tops around the four corners of the hall, one L shaped set per corner.
        data.set(2, 3, 4, topSlab);
        data.set(2, 3, 3, topSlab);
        data.set(2, 3, 2, planks);
        data.set(3, 3, 2, topSlab);
        data.set(4, 3, 2, topSlab);

        data.set(13, 3, 4, topSlab);
        data.set(13, 3, 3, topSlab);
        data.set(13, 3, 2, planks);
        data.set(12, 3, 2, topSlab);
        data.set(11, 3, 2, topSlab);

        data.set(2, 3, 11, topSlab);
        data.set(2, 3, 12, topSlab);
        data.set(2, 3, 13, planks);
        data.set(3, 3, 13, topSlab);
        data.set(4, 3, 13, topSlab);

        data.set(13, 3, 11, topSlab);
        data.set(13, 3, 12, topSlab);
        data.set(13, 3, 13, planks);
        data.set(12, 3, 13, topSlab);
        data.set(11, 3, 13, topSlab);

        // GT6's :87-158: the corner posts and, per row, either the plank row that tops the walls and spans
        // the doorways (local 3) or the 24 shelves of a shelf row (local 1, 2, 4 and 5).
        for (int tY = 1; tY <= 5; tY++) {
            data.set(1, tY, 1, planks);
            data.set(14, tY, 1, planks);
            data.set(1, tY, 14, planks);
            data.set(14, tY, 14, planks);

            if (tY == 3) {
                // GT6's :94-120.
                data.set(2, tY, 1, planks);
                data.set(3, tY, 1, planks);
                data.set(4, tY, 1, planks);
                data.set(11, tY, 1, planks);
                data.set(12, tY, 1, planks);
                data.set(13, tY, 1, planks);

                data.set(14, tY, 2, planks);
                data.set(14, tY, 3, planks);
                data.set(14, tY, 4, planks);
                data.set(14, tY, 11, planks);
                data.set(14, tY, 12, planks);
                data.set(14, tY, 13, planks);

                data.set(2, tY, 14, planks);
                data.set(3, tY, 14, planks);
                data.set(4, tY, 14, planks);
                data.set(11, tY, 14, planks);
                data.set(12, tY, 14, planks);
                data.set(13, tY, 14, planks);

                data.set(1, tY, 2, planks);
                data.set(1, tY, 3, planks);
                data.set(1, tY, 4, planks);
                data.set(1, tY, 11, planks);
                data.set(1, tY, 12, planks);
                data.set(1, tY, 13, planks);
            } else {
                // GT6's :122-128: this row hands out one of the five dungeon keys. GT6 rolls which of the
                // 24 shelves of the row carries it (next(24)), which key that is (next(3) + next(3)) and
                // which of the shelf's front 14 slots holds it (next(14)); its roll order is kept.
                int shelfIndex = data.next(24), keyIndex = data.libraryKeyIndex(tY);
                ItemStack key = ItemStack.EMPTY;
                int keySlot = 0;
                if (keyIndex < data.generatedKeys.length) {
                    data.generatedKeys[keyIndex] = true;
                    keySlot = data.next(14);
                    key = data.keyStacks[keyIndex];
                }

                // GT6's :130-156, in its write order: 24 shelves per row. GT6 passes its key list to the
                // shelf whose running index reached zero, i.e. to the shelfIndex-th one of this order.
                for (int index = 0; index < SHELF_ORDER.length; index++) {
                    int[] at = SHELF_ORDER[index];
                    data.shelf(at[0], tY, at[1], SHELF_LOOT,
                            index == shelfIndex ? key : ItemStack.EMPTY, keySlot);
                }
            }
        }

        // GT6's :164-193: the furniture of the hall, one of four layouts. GT6's Hexxit hexorium blocks (its
        // :160-162) are not ported, which leaves GT6's potion-filled cup in every case.
        switch (data.next(4)) {
            case 0 -> {
                data.set(3, 1, 3, Blocks.ENCHANTING_TABLE.defaultBlockState());
                data.set(3, 1, 12, Blocks.CRAFTING_TABLE.defaultBlockState());
                data.set(12, 1, 3, Blocks.JUKEBOX.defaultBlockState());
                data.set(12, 1, 12, Blocks.ENDER_CHEST.defaultBlockState());
                GTDungeonCupDrinks.library(data, 3, 2, 12);
            }
            case 1 -> {
                data.set(3, 1, 3, Blocks.ENDER_CHEST.defaultBlockState());
                data.set(3, 1, 12, Blocks.ENCHANTING_TABLE.defaultBlockState());
                data.set(12, 1, 3, Blocks.CRAFTING_TABLE.defaultBlockState());
                data.set(12, 1, 12, Blocks.JUKEBOX.defaultBlockState());
                GTDungeonCupDrinks.library(data, 12, 2, 3);
            }
            case 2 -> {
                data.set(3, 1, 3, Blocks.JUKEBOX.defaultBlockState());
                data.set(3, 1, 12, Blocks.ENDER_CHEST.defaultBlockState());
                data.set(12, 1, 3, Blocks.ENCHANTING_TABLE.defaultBlockState());
                data.set(12, 1, 12, Blocks.CRAFTING_TABLE.defaultBlockState());
                GTDungeonCupDrinks.library(data, 12, 2, 12);
            }
            case 3 -> {
                data.set(3, 1, 3, Blocks.CRAFTING_TABLE.defaultBlockState());
                data.set(3, 1, 12, Blocks.JUKEBOX.defaultBlockState());
                data.set(12, 1, 3, Blocks.ENDER_CHEST.defaultBlockState());
                data.set(12, 1, 12, Blocks.ENCHANTING_TABLE.defaultBlockState());
                GTDungeonCupDrinks.library(data, 3, 2, 3);
            }
        }

        return true;
    }
}
