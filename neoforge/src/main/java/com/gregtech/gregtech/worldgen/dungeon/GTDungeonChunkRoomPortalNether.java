package com.gregtech.gregtech.worldgen.dungeon;


import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.content.book.GTBooks;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Port of GT6's {@code DungeonChunkRoomPortalNether}
 * ({@code gregapi/worldgen/dungeon/DungeonChunkRoomPortalNether.java:35-191}): the dead end that leads
 * to the Nether. It is GT6's vault (see {@link GTDungeonChunkRoomPortal}) whose floor becomes netherrack
 * with glowstone at its sixteen lamp pillars and whose ceiling becomes Netherlicious shroomlight and
 * wart blocks, plus - per connection direction - a supply chest of obsidian, netherrack, glowstone,
 * ghast tears, blaze rods and a hunting guide, two rows of soul sand with nether wart, and the obsidian
 * portal frame of four by five blocks.
 *
 * <h2>Port differences</h2>
 * <ul>
 *   <li><b>Portal frame.</b> As in GT6, the obsidian frame starts empty and the player lights the vanilla
 *       Nether portal. Dungeon keys belong to safes, not to this frame.</li>
 *   <li><b>Supply chest.</b> GT6 fills the chest multi-tile 502 with obsidian, netherrack and glowstone
 *       (16 each), the guide {@code Manual_Hunting_Blaze}, four ghast tears, four blaze rods and a full
 *       matchbox ({@code :41-48}). The port keeps GT6's seven inventory slots, including the registered
 *       full matchbox; the guide is the port's own written book ({@code GTBooks.bookStack}).</li>
 *   <li><b>Ceiling.</b> GT6's shroomlight and wart blocks come from the Netherlicious mod
 *       ({@code IL.NeLi_ShroomLight}, {@code IL.NeLi_Wart_Block_Crimson}) and are skipped entirely
 *       without it. 1.20.1 has both blocks in vanilla, so the port builds the ceiling unconditionally
 *       with {@code Blocks.SHROOMLIGHT} and {@code Blocks.NETHER_WART_BLOCK}. GT6's {@code next(3)} for
 *       the wart block's metadata ({@code :59}) is drawn before its own validity check and is kept, but
 *       its result is dropped: the Netherlicious block has three metadatas and 1.20.1 has two blocks
 *       instead.</li>
 *   <li><b>The room tag.</b> GT6 refuses a cell when the dungeon already carries
 *       {@code TAG_PORTAL_NETHER} and adds it afterwards ({@code :38-39}), which keeps GT6's dungeon to
 *       one Nether portal. The port keeps that check, but its tag set is per cell (GT6 shares one set
 *       across the whole dungeon), so within one cell the rule holds and across cells it cannot - see
 *       {@link GTDungeonData}.</li>
 * </ul>
 */
public class GTDungeonChunkRoomPortalNether extends GTDungeonChunkRoomPortal {

    /** GT6's room tag ({@code WorldgenDungeonGT:69}); a cell that already carries it is refused. */
    public static final String TAG_PORTAL_NETHER = "gt.dungeon.portal.nether";

    @Override
    public boolean generate(GTDungeonData data) {
        if (data.hasTag(TAG_PORTAL_NETHER) || !super.generate(data)) return false;
        data.tags.add(TAG_PORTAL_NETHER);

        ItemStack[] supplies = supplies();

        // GT6 :50-56: the netherrack floor with glowstone at its four by four lamp positions.
        for (int tX = 1; tX < 15; tX++) {
            for (int tZ = 1; tZ < 15; tZ++) {
                data.set(tX, 0, tZ, pillar(tX, tZ) ? Blocks.GLOWSTONE.defaultBlockState()
                        : Blocks.NETHERRACK.defaultBlockState());
            }
        }

        // GT6 :58-71: the ceiling - shroomlight at the lamp positions, wart blocks elsewhere, tiles above.
        // GT6's next(3) for the wart block metadata is drawn before its validity check and is kept.
        int wartMeta = data.next(3);
        for (int tX = 1; tX < 15; tX++) {
            for (int tZ = 1; tZ < 15; tZ++) {
                data.set(tX, 7, tZ, pillar(tX, tZ) ? Blocks.SHROOMLIGHT.defaultBlockState()
                        : Blocks.NETHER_WART_BLOCK.defaultBlockState());
                data.tiles(tX, 8, tZ);
            }
        }

        // GT6's four blocks, in GT6's order (:73-101, :102-130, :131-159, :160-188). Only the one that
        // faces the room's single connection can fire.
        if (data.connected(1, 0)) {
            data.chestContents(1, 2, 5, supplies);
            farmAlongZ(data);
            frame(data, true, 2, 6, 9);
        }
        if (data.connected(-1, 0)) {
            data.chestContents(14, 2, 10, supplies);
            farmAlongZ(data);
            frame(data, true, 13, 6, 9);
        }
        if (data.connected(0, 1)) {
            data.chestContents(5, 2, 1, supplies);
            farmAlongX(data);
            frame(data, false, 2, 6, 9);
        }
        if (data.connected(0, -1)) {
            data.chestContents(10, 2, 14, supplies);
            farmAlongX(data);
            frame(data, false, 13, 6, 9);
        }
        return true;
    }

    /** GT6's sixteen glowstone pillars of the floor and the ceiling ({@code (3, 6, 9, 12)} squared). */
    private static boolean pillar(int x, int z) {
        return (x == 3 || x == 6 || x == 9 || x == 12) && (z == 3 || z == 6 || z == 9 || z == 12);
    }

    /**
     * The supply chest's contents in GT6's exact 54-slot positions ({@code :41-48}): obsidian,
     * netherrack and glowstone (16 each), the hunting guide, four ghast tears, four blaze rods and
     * {@code IL.Tool_MatchBox_Full}. Empty entries are intentional: {@link GTDungeonData#chestContents}
     * writes only populated slots and preserves this sparse layout.
     * Built per room instead of cached, because a stack created while the registries are still filling
     * would resolve to air.
     */
    private static ItemStack[] supplies() {
        ItemStack[] slots = new ItemStack[41];
        slots[4] = new ItemStack(Blocks.OBSIDIAN, 16);
        slots[11] = new ItemStack(Blocks.NETHERRACK, 16);
        slots[15] = new ItemStack(Blocks.GLOWSTONE, 16);
        slots[22] = GTBooks.bookStack("Manual_Hunting_Blaze");
        slots[29] = new ItemStack(Items.GHAST_TEAR, 4);
        slots[33] = new ItemStack(Items.BLAZE_ROD, 4);
        var matchBox = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","match_box_full"));
        if (matchBox == null || matchBox == Items.AIR) {
            throw new IllegalStateException("Missing GT6 dungeon supply item: gregtech:match_box_full");
        }
        slots[40] = new ItemStack(matchBox);
        return slots;
    }

    /**
     * GT6's soul sand rows of the X branches ({@code :76-85} for +X, {@code :105-114} for -X): the walls
     * at {@code z = 1} and {@code z = 14} carry soul sand with nether wart and a smooth rock slab row
     * above them, the walls at {@code z = 2} and {@code z = 13} only their slab row.
     *
     * <p>GT6's slabs are its two rocks as half blocks in GT6's own orientations: the rows above the wart
     * are {@code mSlabs[SIDE_Y_POS]} (upside down, so they hang over the wart), and the rows at
     * {@code z = 2} / {@code z = 13} are {@code mSlabs[SIDE_Z_NEG]} / {@code mSlabs[SIDE_Z_POS]}
     * (the half towards the soul sand). The port keeps all of them ({@link GTDungeonData#smoothSlab}).</p>
     */
    private static void farmAlongZ(GTDungeonData data) {
        for (int i = 1; i < 15; i++) {
            data.set(i, 1, 1, Blocks.SOUL_SAND.defaultBlockState());
            data.set(i, 2, 1, wart(data.next(4)));
            data.smoothSlab(i, 3, 1, Direction.UP);
            data.set(i, 1, 14, Blocks.SOUL_SAND.defaultBlockState());
            data.set(i, 2, 14, wart(data.next(4)));
            data.smoothSlab(i, 3, 14, Direction.UP);
            data.smoothSlab(i, 1, 2, Direction.NORTH);
            data.smoothSlab(i, 1, 13, Direction.SOUTH);
        }
    }

    /**
     * GT6's soul sand rows of the Z branches ({@code :134-143} for +Z, {@code :163-172} for -Z): the
     * walls at {@code x = 1} and {@code x = 14} carry soul sand with nether wart and a smooth rock slab
     * row above them, the walls at {@code x = 2} and {@code x = 13} only their slab row - the same four
     * GT6 slab orientations as {@link #farmAlongZ}, turned by a quarter.
     */
    private static void farmAlongX(GTDungeonData data) {
        for (int i = 1; i < 15; i++) {
            data.set(1, 1, i, Blocks.SOUL_SAND.defaultBlockState());
            data.set(1, 2, i, wart(data.next(4)));
            data.smoothSlab(1, 3, i, Direction.UP);
            data.set(14, 1, i, Blocks.SOUL_SAND.defaultBlockState());
            data.set(14, 2, i, wart(data.next(4)));
            data.smoothSlab(14, 3, i, Direction.UP);
            data.smoothSlab(2, 1, i, Direction.WEST);
            data.smoothSlab(13, 1, i, Direction.EAST);
        }
    }

    /** GT6's nether wart with {@code next(4)} as its age. */
    private static BlockState wart(int age) {
        return Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, age);
    }

    /**
     * GT6's obsidian portal frame ({@code :87-100} for +X and so on): four blocks wide and five tall in
     * one plane, with the interior of two by three blocks left open for the portal.
     *
     * @param alongZ whether the frame's plane runs along Z (the X neighbours) or along X (the Z ones)
     * @param fixed  the coordinate of the plane
     * @param low    the first coordinate of the frame's four block width
     * @param high   its last one
     */
    private static void frame(GTDungeonData data, boolean alongZ, int fixed, int low, int high) {
        for (int y = 1; y <= 5; y++) {
            for (int i = low; i <= high; i++) {
                if (y != 1 && y != 5 && i != low && i != high) continue;
                if (alongZ) data.obsidian(fixed, y, i); else data.obsidian(i, y, fixed);
            }
        }
    }

}
