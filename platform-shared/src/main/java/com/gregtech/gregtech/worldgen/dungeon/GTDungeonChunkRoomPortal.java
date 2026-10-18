package com.gregtech.gregtech.worldgen.dungeon;

/**
 * Port of GT6's {@code DungeonChunkRoomPortal}
 * ({@code gregapi/worldgen/dungeon/DungeonChunkRoomPortal.java:27-162}): the shared interior of GT6's
 * portal rooms. It is the empty room of a dead end plus the door of GT6's vault, and then one carpet of
 * the dungeon's colour with a kerb of rock tiles around it on the side that faces away from the room's
 * only connection.
 *
 * <p>GT6 builds it per connection direction: a six by six patch at local {@code y = 1} whose border is
 * concrete in the dungeon's colour - the four corners are lamps with their redstone brick one block
 * below - and whose middle four by four are small tiles, plus an L shaped kerb of rock tile
 * <em>slabs</em> around three sides of that patch. GT6's four blocks are the four directions; a dead
 * end has exactly one connection, so exactly one of them fires.</p>
 *
 * <h2>Port differences</h2>
 * <ul>
 *   <li><b>Vault inlined.</b> GT6's {@code DungeonChunkRoomPortal} extends {@code DungeonChunkRoomVault}
 *       ({@code :27}), the three line class holding the one connection check and the door piston that
 *       {@link GTDungeonChunkRoomStorage} also builds on. The port inlines it like its storage room
 *       does, so that check is the first line of {@link #generate(GTDungeonData)}.</li>
 *   <li><b>The portal itself.</b> GT6 places no portal block here: the Nether room's obsidian frame is
 *       left open for the player to light a vanilla nether portal, and the End room places the vanilla
 *       end portal. The port's subclasses put their own key activated portal block into that frame
 *       ({@link GTDungeonChunkRoomPortalNether}, {@link GTDungeonChunkRoomPortalEnd}); see
 *       {@code com.gregtech.gregtech.block.misc.DungeonPortalBlock}.</li>
 *   <li><b>Neighbour test.</b> GT6 reads {@code mRoomLayout[mRoomX + 1][mRoomZ]} directly; the port asks
 *       {@link GTDungeonData#connected}, which answers {@code 0} outside the layout - the same answer,
 *       because a room never sits in the layout's empty border ring.</li>
 * </ul>
 */
public class GTDungeonChunkRoomPortal extends GTDungeonChunkRoomEmpty {

    @Override
    public boolean generate(GTDungeonData data) {
        // GT6's DungeonChunkRoomVault:30-32, inlined: a dead end has exactly one connection.
        if (data.connectionCount != 1 || !super.generate(data)) return false;
        try {
            // GT6's DungeonChunkRoomVault:31 - the vault door across that single connection.
            new GTDungeonChunkDoorPiston().generate(data);
        } catch (Throwable ignored) {
            // GT6: the vault door is not important enough to fail the entire room.
        }

        // GT6's four blocks, in GT6's order: +X (:32-63), -X (:64-95), +Z (:96-127), -Z (:128-159).
        if (data.connected(1, 0)) {
            patch(data, 1, 5);
            kerbX(data, 7, false);
        }
        if (data.connected(-1, 0)) {
            patch(data, 9, 5);
            kerbX(data, 8, true);
        }
        if (data.connected(0, 1)) {
            patch(data, 5, 1);
            kerbZ(data, 7, false);
        }
        if (data.connected(0, -1)) {
            patch(data, 5, 9);
            kerbZ(data, 8, true);
        }
        return true;
    }

    /**
     * GT6's six by six carpet at local {@code y = 1}: concrete in the dungeon's colour as its border,
     * a lamp with a redstone brick below it (GT6's {@code lamp(..., -1)}) in its four corners, and small
     * tiles in its middle four by four.
     */
    private static void patch(GTDungeonData data, int originX, int originZ) {
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                if (i == 0 || j == 0 || i == 5 || j == 5) {
                    if ((i == 0 || i == 5) && (j == 0 || j == 5)) {
                        data.lamp(originX + i, 1, originZ + j, -1);
                    } else {
                        data.colored(originX + i, 1, originZ + j);
                    }
                } else {
                    data.smalltiles(originX + i, 1, originZ + j);
                }
            }
        }
    }

    /**
     * GT6's L shaped kerb of the X neighbours ({@code :43-62} for +X, {@code :75-94} for -X): a row at
     * {@code z = 4} reaching the far X, a column at that far X from {@code z = 5} down to
     * {@code z = 11}, and a row at {@code z = 11} back towards the room's own side.
     *
     * <p>Every piece is one of GT6's tile <em>slabs</em> in its {@code mSlabs[0]} orientation - GT6's
     * bottom half ({@code SIDE_Y_NEG}) - so the kerb is a low rim on the room's floor
     * ({@link GTDungeonData#slabKerbs}).</p>
     */
    private static void kerbX(GTDungeonData data, int far, boolean mirrored) {
        if (mirrored) {
            // GT6 :75-81: x = 14 down to the far X at z = 4.
            data.slabKerbs(14, 1, 4, 14 - far + 1, -1, 0);
        } else {
            // GT6 :43-49: x = 1 up to the far X at z = 4.
            data.slabKerbs(1, 1, 4, far, 1, 0);
        }
        // GT6 :50-56 / :82-88: the column of the far X, z = 5..11.
        data.slabKerbs(far, 1, 5, 7, 0, 1);
        if (mirrored) {
            // GT6 :89-94: z = 11 from the far X + 1 to 14.
            data.slabKerbs(far + 1, 1, 11, 14 - far, 1, 0);
        } else {
            // GT6 :57-62: z = 11 from the far X - 1 down to 1.
            data.slabKerbs(far - 1, 1, 11, far - 1, -1, 0);
        }
    }

    /**
     * GT6's L shaped kerb of the Z neighbours ({@code :107-126} for +Z, {@code :139-158} for -Z): a
     * column at {@code x = 4} reaching the far Z, a row at that far Z from {@code x = 5} to 11, and a
     * column at {@code x = 11} back towards the room's own side. Tile slabs of GT6's bottom half, like
     * {@link #kerbX}.
     */
    private static void kerbZ(GTDungeonData data, int far, boolean mirrored) {
        if (mirrored) {
            // GT6 :139-145: z = 14 down to the far Z at x = 4.
            data.slabKerbs(4, 1, 14, 14 - far + 1, 0, -1);
        } else {
            // GT6 :107-113: z = 1 up to the far Z at x = 4.
            data.slabKerbs(4, 1, 1, far, 0, 1);
        }
        // GT6 :114-120 / :146-152: the row of the far Z, x = 5..11.
        data.slabKerbs(5, 1, far, 7, 1, 0);
        if (mirrored) {
            // GT6 :153-158: x = 11 from the far Z + 1 to 14.
            data.slabKerbs(11, 1, far + 1, 14 - far, 0, 1);
        } else {
            // GT6 :121-126: x = 11 from the far Z - 1 down to 1.
            data.slabKerbs(11, 1, far - 1, far - 1, 0, -1);
        }
    }
}
