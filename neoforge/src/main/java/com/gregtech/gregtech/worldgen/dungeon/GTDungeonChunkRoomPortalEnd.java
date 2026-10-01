package com.gregtech.gregtech.worldgen.dungeon;

import com.gregtech.gregtech.registry.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of GT6's {@code DungeonChunkRoomPortalEnd}
 * ({@code gregapi/worldgen/dungeon/DungeonChunkRoomPortalEnd.java:32-102}): the dead end that leads to
 * the End. It is GT6's vault - the empty room with its door - whose floor becomes end stone with
 * glowstone at the sixteen lamp positions, and then, in the branch GT6 gates on the Et Futurum mod, a
 * purpur room: purpur pillars on the four corners and on the lamp grid, purpur blocks as the floor and
 * the ceiling, tiles above them. In the middle of that room stand the eight end portal frames with
 * their eyes and the end portal itself, with obsidian under it.
 *
 * <h2>Port differences</h2>
 * <ul>
 *   <li><b>Vault inlined.</b> GT6's End room extends {@code DungeonChunkRoomVault} ({@code :32}) and not
 *       the portal room of the Nether, so it has no carpet patch. The port inlines that three line class
 *       like its storage room does: the one connection check and the door piston, before everything
 *       else.</li>
 *   <li><b>Purpur.</b> GT6 only builds the purpur shell when Et Futurum - a 1.7.10 backport of the
 *       1.9 blocks - is installed ({@code ST.valid(tPurpurBlock) && ST.valid(tPurpurPillar)},
 *       {@code :46-48}); without it the room keeps the bare end stone floor of its first loop. 1.20.1
 *       has purpur in vanilla, so the port builds the shell unconditionally, including GT6's own effect
 *       that this overwrites the end stone floor of the first loop.</li>
 *   <li><b>The portal itself.</b> GT6 places the vanilla {@code Blocks.end_portal} ({@code :92-95}),
 *       which is live at once. The port preserves its four coordinates, frame ring, and eye state;
 *       dungeon keys only open the safes.</li>
 *   <li><b>Hexorium corners.</b> GT6 puts a rainbow block of the Hexxit mod on the four corners of the
 *       end portal ({@code :71-83}) and falls back to glowstone when the mod is absent, which is what the
 *       port does - GT6's own {@code else} branch.</li>
 *   <li><b>End portal frame metadata.</b> GT6 writes the 1.7.10 metadata 4..7, i.e. one of the four
 *       horizontal facings with the eye bit set. 1.20.1 splits that into
 *       {@link EndPortalFrameBlock#FACING} and {@link EndPortalFrameBlock#EYE}; the low two bits of
 *       GT6's metadata are the 1.7.10 horizontal index, which 1.20.1 counts the same way (0 south,
 *       1 west, 2 north, 3 east - {@code Direction.from2DDataValue}), so the port's ring faces the
 *       portal exactly like GT6's, with every frame carrying an eye.</li>
 *   <li><b>The room tag.</b> GT6 refuses a cell when the dungeon already carries {@code TAG_PORTAL_END}
 *       and adds it afterwards ({@code :35-36}), which keeps GT6's dungeon to one End portal. The port
 *       keeps that check, but its tag set is per cell (GT6 shares one set across the whole dungeon), so
 *       within one cell the rule holds and across cells it cannot - see {@link GTDungeonData}.</li>
 *   <li><b>Chunk walls.</b> GT6's purpur shell is written for local {@code 1..14} only, exactly the inner
 *       fourteen blocks of the cell; the port keeps that range, so the cell's own wall ring stays the
 *       dungeon rock.</li>
 * </ul>
 */
public class GTDungeonChunkRoomPortalEnd extends GTDungeonChunkRoomEmpty {

    /** GT6's room tag ({@code WorldgenDungeonGT:70}); a cell that already carries it is refused. */
    public static final String TAG_PORTAL_END = "gt.dungeon.portal.end";

    @Override
    public boolean generate(GTDungeonData data) {
        // GT6 :35 + DungeonChunkRoomVault:30-32, inlined: the tag first, then a dead end with one
        // connection, then the room shell and the vault door.
        if (data.hasTag(TAG_PORTAL_END) || data.connectionCount != 1 || !super.generate(data)) return false;
        data.tags.add(TAG_PORTAL_END);
        try {
            new GTDungeonChunkDoorPiston().generate(data);
        } catch (Throwable ignored) {
            // GT6: the vault door is not important enough to fail the entire room.
        }

        // GT6 :38-44: the end stone floor with glowstone at its four by four lamp positions. GT6's purpur
        // branch overwrites this floor again below, exactly like it does with the mod installed.
        for (int tX = 1; tX < 15; tX++) {
            for (int tZ = 1; tZ < 15; tZ++) {
                data.set(tX, 0, tZ, pillar(tX, tZ) ? Blocks.GLOWSTONE.defaultBlockState()
                        : Blocks.END_STONE.defaultBlockState());
            }
        }

        // GT6 :46-69: the purpur room. The four two by two corners become pillars over the full height,
        // the lamp grid keeps its pillar at the floor and a glowstone at the ceiling, everything else is
        // purpur, and local 8 is the tile skin GT6 gives every room.
        for (int tX = 1; tX < 15; tX++) {
            for (int tZ = 1; tZ < 15; tZ++) {
                if (corner(tX, tZ)) {
                    for (int tY = 0; tY <= 7; tY++) data.set(tX, tY, tZ, Blocks.PURPUR_PILLAR.defaultBlockState());
                } else if (pillar(tX, tZ)) {
                    data.set(tX, 0, tZ, Blocks.PURPUR_PILLAR.defaultBlockState());
                    data.set(tX, 7, tZ, Blocks.GLOWSTONE.defaultBlockState());
                    data.tiles(tX, 8, tZ);
                } else {
                    data.set(tX, 0, tZ, Blocks.PURPUR_BLOCK.defaultBlockState());
                    data.set(tX, 7, tZ, Blocks.PURPUR_BLOCK.defaultBlockState());
                    data.tiles(tX, 8, tZ);
                }
            }
        }

        // GT6 :71-83: the four corners of the end portal. GT6's rainbow hexorium block of the Hexxit mod
        // is missing in the port, so this is GT6's own else branch, a glowstone.
        for (int tX = 5; tX <= 10; tX++) {
            for (int tZ = 5; tZ <= 10; tZ++) {
                if ((tX == 5 || tX == 10) && (tZ == 5 || tZ == 10)) {
                    data.obsidian(tX, 0, tZ);
                    data.obsidian(tX, 1, tZ);
                    data.set(tX, 2, tZ, Blocks.GLOWSTONE.defaultBlockState());
                }
            }
        }

        // GT6 :84-91: the eight end portal frames, all with their eye, facing the portal in the middle.
        // GT6's metadata 4..7 is the horizontal index 0..3 with the eye bit, i.e. south, west, north,
        // east (Direction.from2DDataValue), which points every frame at the 2x2 portal it surrounds.
        data.set(7, 0, 6, frame(Direction.SOUTH));
        data.set(8, 0, 6, frame(Direction.SOUTH));
        data.set(9, 0, 7, frame(Direction.WEST));
        data.set(9, 0, 8, frame(Direction.WEST));
        data.set(7, 0, 9, frame(Direction.NORTH));
        data.set(8, 0, 9, frame(Direction.NORTH));
        data.set(6, 0, 7, frame(Direction.EAST));
        data.set(6, 0, 8, frame(Direction.EAST));

        // GT6 :92-95: vanilla End portal, already active without a dungeon key.
        data.set(7, 0, 7, Blocks.END_PORTAL.defaultBlockState());
        data.set(7, 0, 8, Blocks.END_PORTAL.defaultBlockState());
        data.set(8, 0, 7, Blocks.END_PORTAL.defaultBlockState());
        data.set(8, 0, 8, Blocks.END_PORTAL.defaultBlockState());

        // GT6 :96-99: the obsidian under the portal, one block below the cell's floor.
        data.obsidian(7, -1, 7);
        data.obsidian(7, -1, 8);
        data.obsidian(8, -1, 7);
        data.obsidian(8, -1, 8);
        return true;
    }

    /** GT6's sixteen glowstone positions ({@code (3, 6, 9, 12)} squared). */
    private static boolean pillar(int x, int z) {
        return (x == 3 || x == 6 || x == 9 || x == 12) && (z == 3 || z == 6 || z == 9 || z == 12);
    }

    /** GT6's four two by two corner posts of the purpur room ({@code 1|2|13|14} squared). */
    private static boolean corner(int x, int z) {
        return (x == 1 || x == 2 || x == 13 || x == 14) && (z == 1 || z == 2 || z == 13 || z == 14);
    }

    /** One of GT6's end portal frames: the 1.7.10 metadata 4..7 as a 1.20.1 frame with its eye. */
    private static BlockState frame(Direction facing) {
        return Blocks.END_PORTAL_FRAME.defaultBlockState()
                .setValue(EndPortalFrameBlock.FACING, facing)
                .setValue(EndPortalFrameBlock.HAS_EYE, true);
    }
}
