package com.gregtech.gregtech.worldgen.dungeon;

import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.blockentity.RockBlockEntity;
import com.gregtech.gregtech.worldgen.GTRockPlacement;
import com.gregtech.gregtech.data.generated.GT6Materials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Port of GT6's {@code DungeonChunkBarracks}
 * ({@code gregapi/worldgen/dungeon/DungeonChunkBarracks.java}).
 *
 * <p>GT6's barracks, one of the two "important" cells of every dungeon (layout marker {@code -1}): the
 * empty room is built first and then furnished as a dormitory for four. Carpet in the inverse of the
 * dungeon's colour theme covers the four corners, an inner room (walls of rock <em>slabs</em> at local 5
 * and 10) is walled off from the outer one with four iron doors, stone buttons and stone pressure plates,
 * four beds, four crafting tables and four loot containers - a shelf and a chest facing the outer wall -
 * line the corners. One of GT6's five dungeon keys hides inside each shelf, and the chest next to a shelf
 * that got a back slot gets a cobble "there is something behind the shelf" hint carved into the wall
 * behind it.</p>
 *
 * <p>Port differences:</p>
 * <ul>
 *   <li>The 1.7.10 block metadata of the doors, buttons, pressure plates and beds becomes a 1.20.1 block
 *       state: GT6's door meta 1 (the Z=5 wall) faces north and meta 3 (the Z=10 wall) faces south, the
 *       upper block's low bit (8/9) picks the hinge so the two doors of a wall mirror each other, GT6's
 *       button meta 3/4 (the wall at Z-1 / Z+1) becomes a wall button facing south / north, and GT6's bed
 *       bit 8 and bits 0..1 select the head and the direction the head points.</li>
 *   <li>Keys: the key that GT6 hides in the shelf's inventory is the port's real key stack
 *       ({@link GTDungeonData#keyStacks}, the ten items of {@code GTDungeonKeys}) in GT6's slot
 *       ({@code next(28)}), so the cobble hint behind the shelf now really points at it. The dungeon key
 *       id GT6 stores in the chest's NBT is still skipped, because the port's chest has no lock (GT6's
 *       multi-tile 3010 is a key locked safe).</li>
 *   <li>The three cups now use GT vessels holding GT6's drink pool. The 1-in-2 Hexorium block of an
 *       optional mod is unavailable. GT6's Sky Stone rock at (14,2,11) uses the port's surface-rock
 *       block with its full stored stack.</li>
 *   <li>The loot categories become 1.20.1 loot tables, GT6's {@code BONUS_CHEST} has none and is dropped,
 *       and the category is drawn from the cell's own random stream instead of GT6's global one.</li>
 *   <li>The two container helpers place their block itself: the port's chest always faces north and its
 *       shelf stores books rather than GT6's inventory list, so GT6's {@code NBT_FACING} and the shelf's
 *       key slot are not passed through.</li>
 * </ul>
 */
public class GTDungeonChunkBarracks extends GTDungeonChunkRoomEmpty {

    /** Vanilla's sixteen carpets in GT6's dye order (white..black), for GT6's {@code Blocks.carpet} meta. */
    private static final Block[] CARPET = {
            Blocks.WHITE_CARPET, Blocks.ORANGE_CARPET, Blocks.MAGENTA_CARPET, Blocks.LIGHT_BLUE_CARPET,
            Blocks.YELLOW_CARPET, Blocks.LIME_CARPET, Blocks.PINK_CARPET, Blocks.GRAY_CARPET,
            Blocks.LIGHT_GRAY_CARPET, Blocks.CYAN_CARPET, Blocks.PURPLE_CARPET, Blocks.BLUE_CARPET,
            Blocks.BROWN_CARPET, Blocks.GREEN_CARPET, Blocks.RED_CARPET, Blocks.BLACK_CARPET};

    /**
     * GT6's {@code tLoots} (:108) as 1.20.1 loot tables, in GT6's own order: stronghold library, stronghold
     * corridor, stronghold crossing, desert pyramid, jungle pyramid, village blacksmith, mineshaft corridor,
     * dungeon chest and GT6's ninth entry {@code BONUS_CHEST}, which 1.20.1 has as
     * {@code chests/spawn_bonus_chest} (the port already injects GT content into it).
     */
    private static final String[] LOOT = {
            "chests/stronghold_library", "chests/stronghold_corridor", "chests/stronghold_crossing",
            "chests/desert_pyramid", "chests/jungle_temple", "chests/village_weaponsmith",
            "chests/abandoned_mineshaft", "chests/simple_dungeon", "chests/spawn_bonus_chest"};

    @Override
    public boolean generate(GTDungeonData data) {
        if (!super.generate(data)) return false;

        // GT6 :43-45 - the inverse of the dungeon's colour as carpet in the four outer corners.
        for (int tX = 1; tX <= 14; tX++) {
            for (int tZ = 1; tZ <= 14; tZ++) {
                if ((tX <= 4 || tX >= 11) && (tZ <= 4 || tZ >= 11)) {
                    data.set(tX, 1, tZ, CARPET[data.colorInversed].defaultBlockState());
                }
            }
        }
        // GT6 :46-66 - the inner room's walls at local 5 and 10, and their smooth corners. Every wall
        // piece is a brick <em>slab</em> of the cell's rock in GT6's own orientation for that wall
        // ({@code mSlabs[SIDE_Z_NEG]} for z = 5, {@code SIDE_Z_POS} for z = 10, {@code SIDE_X_NEG} for
        // x = 5 and {@code SIDE_X_POS} for x = 10), so the walls are half blocks with their flat side
        // towards the inner room. GT6 rolls the brick variant ({@code 3 + next(3)}, i.e. bricks, cracked
        // bricks or mossy bricks); the port's brick helper uses plain bricks, as it does for every other
        // brick wall of the dungeon.
        for (int tY = 1; tY <= 6; tY++) {
            for (int tCoord = 1; tCoord <= 14; tCoord++) {
                if (tCoord <= 3 || tCoord >= 12) {
                    data.bricksSlab(tCoord, tY, 5, Direction.NORTH);
                    data.bricksSlab(tCoord, tY, 10, Direction.SOUTH);
                    data.bricksSlab(5, tY, tCoord, Direction.WEST);
                    data.bricksSlab(10, tY, tCoord, Direction.EAST);
                }
            }

            data.smooth(4, tY, 5);
            data.smooth(5, tY, 4);
            data.smooth(5, tY, 5);
            data.smooth(4, tY, 10);
            data.smooth(5, tY, 10);
            data.smooth(5, tY, 11);
            data.smooth(10, tY, 4);
            data.smooth(10, tY, 5);
            data.smooth(11, tY, 5);
            data.smooth(10, tY, 10);
            data.smooth(10, tY, 11);
            data.smooth(11, tY, 10);
        }

        // GT6 :68-75 - the four iron doors of the inner room. The 1.7.10 metadata becomes a 1.20.1 door
        // state; the hinge comes from the upper block's low bit, so each wall's pair mirrors itself.
        data.set(3, 1, 5, door(Direction.NORTH, DoorHingeSide.LEFT, DoubleBlockHalf.LOWER));
        data.set(3, 2, 5, door(Direction.NORTH, DoorHingeSide.LEFT, DoubleBlockHalf.UPPER));
        data.set(12, 1, 5, door(Direction.NORTH, DoorHingeSide.RIGHT, DoubleBlockHalf.LOWER));
        data.set(12, 2, 5, door(Direction.NORTH, DoorHingeSide.RIGHT, DoubleBlockHalf.UPPER));
        data.set(3, 1, 10, door(Direction.SOUTH, DoorHingeSide.RIGHT, DoubleBlockHalf.LOWER));
        data.set(3, 2, 10, door(Direction.SOUTH, DoorHingeSide.RIGHT, DoubleBlockHalf.UPPER));
        data.set(12, 1, 10, door(Direction.SOUTH, DoorHingeSide.LEFT, DoubleBlockHalf.LOWER));
        data.set(12, 2, 10, door(Direction.SOUTH, DoorHingeSide.LEFT, DoubleBlockHalf.UPPER));
        // GT6 :76-79 - the stone buttons beside the doors: meta 3 is the wall at Z-1, meta 4 the wall at Z+1.
        data.set(4, 2, 6, button(Direction.SOUTH));
        data.set(11, 2, 6, button(Direction.SOUTH));
        data.set(4, 2, 9, button(Direction.NORTH));
        data.set(11, 2, 9, button(Direction.NORTH));
        // GT6 :80-83 - the stone pressure plates in front of the doors.
        data.set(3, 1, 4, Blocks.STONE_PRESSURE_PLATE.defaultBlockState());
        data.set(12, 1, 4, Blocks.STONE_PRESSURE_PLATE.defaultBlockState());
        data.set(3, 1, 11, Blocks.STONE_PRESSURE_PLATE.defaultBlockState());
        data.set(12, 1, 11, Blocks.STONE_PRESSURE_PLATE.defaultBlockState());
        // GT6 :84-91 - four beds, their heads against the outer walls of the corners.
        data.set(1, 1, 1, bed(Direction.NORTH, BedPart.HEAD));
        data.set(1, 1, 2, bed(Direction.NORTH, BedPart.FOOT));
        data.set(1, 1, 13, bed(Direction.SOUTH, BedPart.FOOT));
        data.set(1, 1, 14, bed(Direction.SOUTH, BedPart.HEAD));
        data.set(14, 1, 1, bed(Direction.NORTH, BedPart.HEAD));
        data.set(14, 1, 2, bed(Direction.NORTH, BedPart.FOOT));
        data.set(14, 1, 13, bed(Direction.SOUTH, BedPart.FOOT));
        data.set(14, 1, 14, bed(Direction.SOUTH, BedPart.HEAD));
        // GT6 :92-95 - a crafting table in each corner.
        data.set(1, 1, 4, Blocks.CRAFTING_TABLE.defaultBlockState());
        data.set(1, 1, 11, Blocks.CRAFTING_TABLE.defaultBlockState());
        data.set(14, 1, 4, Blocks.CRAFTING_TABLE.defaultBlockState());
        data.set(14, 1, 11, Blocks.CRAFTING_TABLE.defaultBlockState());

        // GT6 :97-105 - real 250 mB cups, with its optional Hexorium choice consuming the same room roll.
        GTDungeonCupDrinks.barracks(data, 1, 2, 4);
        GTDungeonCupDrinks.barracks(data, 1, 2, 11);
        GTDungeonCupDrinks.barracks(data, 14, 2, 4);

        // GT6 :106 - multi-tile 32074 stores one stack of 16..64 Sky Stone rocks as a small placed rock.
        // Preserve the original draw even if world placement fails, so later room rolls stay aligned.
        int skyStoneCount = 16 + data.next(49);
        BlockPos skyStonePos = new BlockPos(data.x + 14, data.y + 2, data.z + 11);
        if (GTRockPlacement.place(data.level, skyStonePos, GT6Materials.Stones.SkyStone.getName(), null, false)
                && data.level.getBlockEntity(skyStonePos) instanceof RockBlockEntity rock) {
            rock.setCount(skyStoneCount);
        }

        // GT6 :111-121 - the shelf at (3,1,1) and the chest at (4,1,1) beside it, both facing Z+.
        ItemStack key = ItemStack.EMPTY;
        int keySlot = 0;
        if (data.keyAvailable(0)) {
            data.generatedKeys[0] = true;
            // GT6 :115 - the key sits in one of the shelf's 28 slots; a back slot (>= 14) makes GT6
            // carve a cobble hint into the wall behind the shelf. The port's shelf has the same 28
            // slots, so GT6's index is used unchanged.
            keySlot = data.next(28);
            key = data.keyStacks[0];
            if (keySlot >= 14) {
                data.cobble(3, 1, 0);
                data.cobble(3, 2, 0);
                data.cobble(3, 3, 0);
            }
        }
        // The chest is GT6's multi-tile 3010, the key locked safe, which carries the loot table and the
        // dungeon key id that opens it; the key-safe block now retains both.
        data.safe(4, 1, 1, LOOT[data.next(LOOT.length)], data.keyIds[0], true);
        data.shelf(3, 1, 1, LOOT[data.next(LOOT.length)], key, keySlot);

        // GT6 :123-132 - the second pair, facing Z-.
        key = ItemStack.EMPTY;
        keySlot = 0;
        if (data.keyAvailable(1)) {
            data.generatedKeys[1] = true;
            keySlot = data.next(28);
            key = data.keyStacks[1];
            if (keySlot >= 14) {
                data.cobble(3, 1, 15);
                data.cobble(3, 2, 15);
                data.cobble(3, 3, 15);
            }
        }
        data.safe(4, 1, 14, LOOT[data.next(LOOT.length)], data.keyIds[1], true);
        data.shelf(3, 1, 14, LOOT[data.next(LOOT.length)], key, keySlot);

        // GT6 :134-143 - the third pair. GT6 skips key 2 here, which belongs to the corridor room's chests.
        key = ItemStack.EMPTY;
        keySlot = 0;
        if (data.keyAvailable(3)) {
            data.generatedKeys[3] = true;
            keySlot = data.next(28);
            key = data.keyStacks[3];
            if (keySlot >= 14) {
                data.cobble(12, 1, 0);
                data.cobble(12, 2, 0);
                data.cobble(12, 3, 0);
            }
        }
        data.safe(11, 1, 1, LOOT[data.next(LOOT.length)], data.keyIds[3], true);
        data.shelf(12, 1, 1, LOOT[data.next(LOOT.length)], key, keySlot);

        // GT6 :145-154 - the fourth pair.
        key = ItemStack.EMPTY;
        keySlot = 0;
        if (data.keyAvailable(4)) {
            data.generatedKeys[4] = true;
            keySlot = data.next(28);
            key = data.keyStacks[4];
            if (keySlot >= 14) {
                data.cobble(12, 1, 15);
                data.cobble(12, 2, 15);
                data.cobble(12, 3, 15);
            }
        }
        data.safe(11, 1, 14, LOOT[data.next(LOOT.length)], data.keyIds[4], true);
        data.shelf(12, 1, 14, LOOT[data.next(LOOT.length)], key, keySlot);

        // GT6 :156-159 - a 1-in-2 chance for loose coins next to every chest.
        if (data.next1in2()) data.coins(4, 2, 1);
        if (data.next1in2()) data.coins(4, 2, 14);
        if (data.next1in2()) data.coins(11, 2, 1);
        if (data.next1in2()) data.coins(11, 2, 14);

        return true;
    }

    /** GT6 :68-75 - one half of an iron door, from GT6's 1.7.10 metadata. */
    private static BlockState door(Direction facing, DoorHingeSide hinge, DoubleBlockHalf half) {
        return Blocks.IRON_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HINGE, hinge)
                .setValue(DoorBlock.HALF, half);
    }

    /** GT6 :76-79 - a stone button on the wall it points away from. */
    private static BlockState button(Direction facing) {
        return Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.WALL)
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACING, facing);
    }

    /** GT6 :84-91 - one half of a bed; the facing runs from the foot towards the head. */
    private static BlockState bed(Direction facing, BedPart part) {
        return Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, facing)
                .setValue(BedBlock.PART, part);
    }
}
