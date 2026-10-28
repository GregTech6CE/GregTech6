package gregtech.content.worldgen.dungeon;

//? if >=1.21.1 {
/*import gregtech.content.registry.*;
*///?} else {
//?}
import gregtech.content.registry.GTToolBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;

/**
 * Port of GT6's {@code DungeonChunkDoorPiston}
 * ({@code gregapi/worldgen/dungeon/DungeonChunkDoorPiston.java:30-347}): the piston door GT6 puts into
 * the wall of a dead-end room. {@code DungeonChunkRoomVault:31} (the base of the storage room and of the
 * portal rooms) runs it for a cell with a single connection, and it is the room that owns it: this cell
 * only builds the door itself.
 *
 * <p>The door is a two-block-tall frame of smooth stone with a sticky-piston door in the middle of it,
 * wired up with redstone wire, two redstone torches and two hand cranks, lit by two ceiling lamps whose
 * redstone brick sits above the frame. GT6 builds it once per connected neighbour - the four blocks
 * each write their own frame into the cell edge
 * they face - and the port keeps that shape, that order and the fact that no random number is drawn at
 * all.</p>
 *
 * <p>Port differences:</p>
 * <ul>
 *   <li>GT6's Hand Crank multi-tile (32111, {@code Loader_MultiTileEntities:2105}) becomes the port's own
 *       crank block. GT6's {@code NBT_FACING} is the wall the crank is mounted on, because the crank
 *       drives the block on that side ({@code MultiTileEntityCrank:78}) and its handle - the lit front -
 *       points the other way; {@link gregtech.content.block.tool.CrankBlock}
 *       stores the opposite, the direction it points away from its wall
 *       ({@code CrankBlock}), so {@link #crank} flips it. The colour and the
 *       "painted" flag of GT6's multi-tile are lost, like everywhere else in this port.</li>
 *   <li>GT6's sticky pistons are
 *       vanilla blocks in both versions: the 1.7.10 metadata 2..5 (north, south, west, east) is the same
 *       order as 1.20's direction indices, so {@link PistonBaseBlock#FACING} carries GT6's value.</li>
 *   <li>GT6's redstone torches
 *       add up to a wall torch facing the room: the 1.7.10 metadata 1..4 means "the torch points east,
 *       west, south or north", which is exactly {@link RedstoneWallTorchBlock#FACING}, so the port places
 *       {@code Blocks.REDSTONE_WALL_TORCH} with the same direction.</li>
 *   <li>GT6's redstone wire metadata 0 becomes a plain {@code Blocks.REDSTONE_WIRE} state; the
 *       connections of a wire are recomputed by the game's block updates, exactly as in GT6.</li>
 *   <li>GT6 writes the torches with block flags 3 and everything else with flags 2; the port's
 *       {@link GTDungeonData#set} always uses flags 2, like the rest of this port.</li>
 *   <li>The door frame reaches local {@code y = 6} and the lamps put their redstone brick at
 *       {@code y = 5}, i.e. a couple of layers above the 0..4 shell of a
 *       plain corridor. GT6's rooms are nine blocks tall (floor 0, interior 1..6, ceiling 7, tile skin
 *       8: {@code DungeonChunkBarracks:46}, {@code DungeonChunkEntrance:94}), which is where the door
 *       stands, so GT6's coordinates are kept as they are and stay inside the room height.</li>
 * </ul>
 */
public class GTDungeonChunkDoorPiston implements GTDungeonChunk {

    @Override
    public boolean generate(GTDungeonData data) {
        // GT6's four door blocks, in GT6's order: +X, -X, +Z, -Z.
        if (data.connected(1, 0)) {
            // two smooth pieces at the cell edge.
            data.smooth(15, 3, 6);
            data.smooth(15, 3, 9);

            // the floor of the door frame.
            for (int x = 11; x <= 14; x++) {
                data.smooth(x, 0, 6);
                data.smooth(x, 0, 7);
                data.smooth(x, 0, 8);
                data.smooth(x, 0, 9);
            }
            // the frame around the passage, which stays open at y = 1 and y = 2.
            for (int y = 1; y <= 6; y++) {
                data.smooth(14, y, 4);
                data.smooth(13, y, 4);
                data.smooth(12, y, 4);
                data.smooth(11, y, 5);
                if (y >= 3) {
                    data.smooth(11, y, 6);
                    if (y >= 4) {
                        data.smooth(11, y, 7);
                        data.smooth(11, y, 8);
                    }
                    data.smooth(11, y, 9);
                }
                data.smooth(11, y, 10);
                data.smooth(12, y, 11);
                data.smooth(13, y, 11);
                data.smooth(14, y, 11);
            }
            // the door jambs above the pistons.
            for (int y = 1; y <= 2; y++) {
                data.smooth(14, y, 5);
                data.smooth(14, y, 6);
                data.smooth(14, y, 9);
                data.smooth(14, y, 10);
                data.smooth(12, y, 5);
                data.smooth(12, y, 6);
                data.smooth(12, y, 9);
                data.smooth(12, y, 10);
            }
            // the wiring channels and the top of the passage.
            data.smooth(12, 3, 7);
            data.smooth(12, 3, 8);
            data.smooth(14, 3, 7);
            data.smooth(14, 3, 8);
            data.smooth(13, 4, 7);
            data.smooth(13, 4, 8);
            data.smooth(13, 3, 5);
            data.smooth(13, 3, 6);
            data.smooth(13, 3, 7);
            data.smooth(13, 3, 8);
            data.smooth(13, 3, 9);
            data.smooth(13, 3, 10);

            // the two lamps and the painted plates beside the door.
            data.lamp(15, 4, 7, 1);
            data.lamp(15, 4, 8, 1);
            data.colored(13, 1, 7);
            data.colored(13, 1, 8);
            data.colored(13, 2, 7);
            data.colored(13, 2, 8);

            // the two hand cranks, mounted on the walls at x = 12 and x = 14.
            crank(data, 11, 2, 9, Direction.EAST);
            crank(data, 15, 2, 6, Direction.WEST);

            // the sticky piston door, two blocks tall (metas 3 and 2, so +Z and -Z).
            piston(data, 13, 1, 5, Direction.SOUTH);
            piston(data, 13, 1, 10, Direction.NORTH);
            piston(data, 13, 2, 5, Direction.SOUTH);
            piston(data, 13, 2, 10, Direction.NORTH);

            // the redstone wire over the door.
            wire(data, 12, 3, 6);
            wire(data, 12, 3, 9);
            wire(data, 14, 3, 6);
            wire(data, 14, 3, 9);
            wire(data, 12, 4, 7);
            wire(data, 12, 4, 8);
            wire(data, 13, 4, 5);
            wire(data, 13, 4, 10);
            wire(data, 14, 4, 7);
            wire(data, 14, 4, 8);
            wire(data, 13, 5, 7);
            wire(data, 13, 5, 8);

            // the two torches, mounted on the blocks at z = 7 and z = 8.
            torch(data, 13, 4, 6, Direction.NORTH);
            torch(data, 13, 4, 9, Direction.SOUTH);
        }

        if (data.connected(-1, 0)) {
            data.smooth(0, 3, 6);
            data.smooth(0, 3, 9);

            for (int x = 1; x <= 4; x++) {
                data.smooth(x, 0, 6);
                data.smooth(x, 0, 7);
                data.smooth(x, 0, 8);
                data.smooth(x, 0, 9);
            }
            for (int y = 1; y <= 6; y++) {
                data.smooth(1, y, 4);
                data.smooth(2, y, 4);
                data.smooth(3, y, 4);
                data.smooth(4, y, 5);
                if (y >= 3) {
                    data.smooth(4, y, 6);
                    if (y >= 4) {
                        data.smooth(4, y, 7);
                        data.smooth(4, y, 8);
                    }
                    data.smooth(4, y, 9);
                }
                data.smooth(4, y, 10);
                data.smooth(1, y, 11);
                data.smooth(2, y, 11);
                data.smooth(3, y, 11);
            }
            for (int y = 1; y <= 2; y++) {
                data.smooth(1, y, 5);
                data.smooth(1, y, 6);
                data.smooth(1, y, 9);
                data.smooth(1, y, 10);
                data.smooth(3, y, 5);
                data.smooth(3, y, 6);
                data.smooth(3, y, 9);
                data.smooth(3, y, 10);
            }
            data.smooth(3, 3, 7);
            data.smooth(3, 3, 8);
            data.smooth(1, 3, 7);
            data.smooth(1, 3, 8);
            data.smooth(2, 4, 7);
            data.smooth(2, 4, 8);
            data.smooth(2, 3, 5);
            data.smooth(2, 3, 6);
            data.smooth(2, 3, 7);
            data.smooth(2, 3, 8);
            data.smooth(2, 3, 9);
            data.smooth(2, 3, 10);

            data.lamp(0, 4, 7, 1);
            data.lamp(0, 4, 8, 1);
            data.colored(2, 1, 7);
            data.colored(2, 1, 8);
            data.colored(2, 2, 7);
            data.colored(2, 2, 8);

            // the cranks, mounted on the walls at x = 1 and x = 3.
            crank(data, 0, 2, 9, Direction.EAST);
            crank(data, 4, 2, 6, Direction.WEST);

            piston(data, 2, 1, 5, Direction.SOUTH);
            piston(data, 2, 1, 10, Direction.NORTH);
            piston(data, 2, 2, 5, Direction.SOUTH);
            piston(data, 2, 2, 10, Direction.NORTH);

            wire(data, 3, 3, 6);
            wire(data, 3, 3, 9);
            wire(data, 1, 3, 6);
            wire(data, 1, 3, 9);
            wire(data, 3, 4, 7);
            wire(data, 3, 4, 8);
            wire(data, 2, 4, 5);
            wire(data, 2, 4, 10);
            wire(data, 1, 4, 7);
            wire(data, 1, 4, 8);
            wire(data, 2, 5, 7);
            wire(data, 2, 5, 8);

            torch(data, 2, 4, 6, Direction.NORTH);
            torch(data, 2, 4, 9, Direction.SOUTH);
        }

        if (data.connected(0, 1)) {
            data.smooth(6, 3, 15);
            data.smooth(9, 3, 15);

            for (int z = 11; z <= 14; z++) {
                data.smooth(6, 0, z);
                data.smooth(7, 0, z);
                data.smooth(8, 0, z);
                data.smooth(9, 0, z);
            }
            for (int y = 1; y <= 6; y++) {
                data.smooth(4, y, 14);
                data.smooth(4, y, 13);
                data.smooth(4, y, 12);
                data.smooth(5, y, 11);
                if (y >= 3) {
                    data.smooth(6, y, 11);
                    if (y >= 4) {
                        data.smooth(7, y, 11);
                        data.smooth(8, y, 11);
                    }
                    data.smooth(9, y, 11);
                }
                data.smooth(10, y, 11);
                data.smooth(11, y, 12);
                data.smooth(11, y, 13);
                data.smooth(11, y, 14);
            }
            for (int y = 1; y <= 2; y++) {
                data.smooth(5, y, 14);
                data.smooth(6, y, 14);
                data.smooth(9, y, 14);
                data.smooth(10, y, 14);
                data.smooth(5, y, 12);
                data.smooth(6, y, 12);
                data.smooth(9, y, 12);
                data.smooth(10, y, 12);
            }
            data.smooth(7, 3, 12);
            data.smooth(8, 3, 12);
            data.smooth(7, 3, 14);
            data.smooth(8, 3, 14);
            data.smooth(7, 4, 13);
            data.smooth(8, 4, 13);
            data.smooth(5, 3, 13);
            data.smooth(6, 3, 13);
            data.smooth(7, 3, 13);
            data.smooth(8, 3, 13);
            data.smooth(9, 3, 13);
            data.smooth(10, 3, 13);

            data.lamp(7, 4, 15, 1);
            data.lamp(8, 4, 15, 1);
            data.colored(7, 1, 13);
            data.colored(8, 1, 13);
            data.colored(7, 2, 13);
            data.colored(8, 2, 13);

            // the cranks, mounted on the walls at z = 12 and z = 14.
            crank(data, 6, 2, 11, Direction.SOUTH);
            crank(data, 9, 2, 15, Direction.NORTH);

            piston(data, 5, 1, 13, Direction.EAST);
            piston(data, 10, 1, 13, Direction.WEST);
            piston(data, 5, 2, 13, Direction.EAST);
            piston(data, 10, 2, 13, Direction.WEST);

            wire(data, 6, 3, 12);
            wire(data, 9, 3, 12);
            wire(data, 6, 3, 14);
            wire(data, 9, 3, 14);
            wire(data, 7, 4, 12);
            wire(data, 8, 4, 12);
            wire(data, 5, 4, 13);
            wire(data, 10, 4, 13);
            wire(data, 7, 4, 14);
            wire(data, 8, 4, 14);
            wire(data, 7, 5, 13);
            wire(data, 8, 5, 13);

            torch(data, 6, 4, 13, Direction.WEST);
            torch(data, 9, 4, 13, Direction.EAST);
        }

        if (data.connected(0, -1)) {
            data.smooth(6, 3, 0);
            data.smooth(9, 3, 0);

            for (int z = 1; z <= 4; z++) {
                data.smooth(6, 0, z);
                data.smooth(7, 0, z);
                data.smooth(8, 0, z);
                data.smooth(9, 0, z);
            }
            for (int y = 1; y <= 6; y++) {
                data.smooth(4, y, 1);
                data.smooth(4, y, 2);
                data.smooth(4, y, 3);
                data.smooth(5, y, 4);
                if (y >= 3) {
                    data.smooth(6, y, 4);
                    if (y >= 4) {
                        data.smooth(7, y, 4);
                        data.smooth(8, y, 4);
                    }
                    data.smooth(9, y, 4);
                }
                data.smooth(10, y, 4);
                data.smooth(11, y, 1);
                data.smooth(11, y, 2);
                data.smooth(11, y, 3);
            }
            for (int y = 1; y <= 2; y++) {
                data.smooth(5, y, 1);
                data.smooth(6, y, 1);
                data.smooth(9, y, 1);
                data.smooth(10, y, 1);
                data.smooth(5, y, 3);
                data.smooth(6, y, 3);
                data.smooth(9, y, 3);
                data.smooth(10, y, 3);
            }
            data.smooth(7, 3, 3);
            data.smooth(8, 3, 3);
            data.smooth(7, 3, 1);
            data.smooth(8, 3, 1);
            data.smooth(7, 4, 2);
            data.smooth(8, 4, 2);
            data.smooth(5, 3, 2);
            data.smooth(6, 3, 2);
            data.smooth(7, 3, 2);
            data.smooth(8, 3, 2);
            data.smooth(9, 3, 2);
            data.smooth(10, 3, 2);

            data.lamp(7, 4, 0, 1);
            data.lamp(8, 4, 0, 1);
            data.colored(7, 1, 2);
            data.colored(8, 1, 2);
            data.colored(7, 2, 2);
            data.colored(8, 2, 2);

            // the cranks, mounted on the walls at z = 1 and z = 3.
            crank(data, 6, 2, 0, Direction.SOUTH);
            crank(data, 9, 2, 4, Direction.NORTH);

            piston(data, 5, 1, 2, Direction.EAST);
            piston(data, 10, 1, 2, Direction.WEST);
            piston(data, 5, 2, 2, Direction.EAST);
            piston(data, 10, 2, 2, Direction.WEST);

            wire(data, 6, 3, 3);
            wire(data, 9, 3, 3);
            wire(data, 6, 3, 1);
            wire(data, 9, 3, 1);
            wire(data, 7, 4, 3);
            wire(data, 8, 4, 3);
            wire(data, 5, 4, 2);
            wire(data, 10, 4, 2);
            wire(data, 7, 4, 1);
            wire(data, 8, 4, 1);
            wire(data, 7, 5, 2);
            wire(data, 8, 5, 2);

            torch(data, 6, 4, 2, Direction.WEST);
            torch(data, 9, 4, 2, Direction.EAST);
        }

        return true;
    }

    /**
     * GT6's sticky piston (and its three mirrors). {@code extend} is the side the piston
     * pushes towards, which is GT6's 1.7.10 metadata and 1.20's {@link PistonBaseBlock#FACING}.
     */
    private static void piston(GTDungeonData data, int x, int y, int z, Direction extend) {
        data.set(x, y, z, Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, extend));
    }

    /** GT6's redstone wire (and its three mirrors), placed as metadata 0. */
    private static void wire(GTDungeonData data, int x, int y, int z) {
        data.set(x, y, z, Blocks.REDSTONE_WIRE.defaultBlockState());
    }

    /**
     * GT6's redstone torch (and its three mirrors). {@code points} is the side the torch
     * leans towards, away from the block it is mounted on - GT6's metadata 1 (east) to 4 (north).
     */
    private static void torch(GTDungeonData data, int x, int y, int z, Direction points) {
        data.set(x, y, z, Blocks.REDSTONE_WALL_TORCH.defaultBlockState()
                .setValue(RedstoneWallTorchBlock.FACING, points));
    }

    /**
     * GT6's Hand Crank multi-tile 32111 (and its three mirrors). {@code mountedOn} is
     * GT6's {@code NBT_FACING}, the wall the crank is attached to and drives; the port's crank block
     * stores the direction it points away from that wall, so the two are opposites.
     */
    private static void crank(GTDungeonData data, int x, int y, int z, Direction mountedOn) {
//? if >=1.21.1 {
        /*data.set(x, y, z, GTManualStations.CRANK.get().defaultBlockState()
*///?} else {
        data.set(x, y, z, GTToolBlocks.CRANK.get().defaultBlockState()
//?}
                .setValue(DirectionalBlock.FACING, mountedOn.getOpposite()));
    }
}
