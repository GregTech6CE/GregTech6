package gregtech.content.worldgen.dungeon;

import gregtech.api.material.GTMaterial;
import gregtech.content.block.OreBlock;
import gregtech.content.block.misc.BarsBlock;
import gregtech.content.block.tool.DynamiteBlock;
import gregtech.content.block.tool.ShapedToolBlock;
import gregtech.content.registry.GTToolBlocks;
import gregtech.content.worldgen.GTBedrockOreFeature;
import gregtech.content.worldgen.SpecialOreDeposits;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A dungeon shaft and raw ore pillars built above a usable bedrock deposit. */
public class GTDungeonChunkRoomMiningBedrock extends GTDungeonChunkRoomEmpty {

    /** GT6's room tag ({@code WorldgenDungeonGT}); a cell that already carries it is refused. */
    public static final String TAG_MINING_BEDROCK = "gt.dungeon.mining.bedrock";

    /**
     * GT6's thirteen bedrock ore materials (: redstone twice, then sulfur, hematite,
     * pyrolusite, apatite, molybdenite, bauxite, sphalerite, tetrahedrite, cassiterite, garnierite and
     * galena) in GT6's own order and multiplicity. The vector drives both the vein and the
     * raw ore blocks of the pillars, so the port keeps it as GT6 wrote it -
     * {@code UT.Code.select(MT.Redstone, MT.Redstone, MT.S, MT.Fe2O3, MT.MnO2, MT.Apatite,...)}.
     */
    private static final GTMaterial[] VEIN_MATERIALS = SpecialOreDeposits.DUNGEON_ROLLS.toArray(GTMaterial[]::new);

    @Override
    public boolean generate(GTDungeonData data) {
        int floor = data.level.getMinBuildHeight();
        int rawOreBottom = floor + SpecialOreDeposits.DUNGEON_RAW_ORE_OFFSET;
        if (data.hasTag(TAG_MINING_BEDROCK) || data.y < rawOreBottom) return false;
        GTMaterial vein = data.select(VEIN_MATERIALS, 0, 0, 0);
        if (!GTBedrockOreFeature.placeVein(data.level, data.random, data.x, data.z, vein)) return false;
        if (!super.generate(data)) return false;
        data.tags.add(TAG_MINING_BEDROCK);

        // GT6: brass or steel for the whole room.
        boolean brass = data.next1in2();

        int lowest = rawOreBottom - data.y; // GT6's world Y 5: one layer above the bedrock band
        int chamberTop = floor + 4 - data.y; // GT6's world Y 4
        int chamberBottom = floor + 3 - data.y; // GT6's world Y 3

        // GT6's shaft: the whole cell from the layer under the room floor down to world Y 5
        // is walled with bricks at x/z 0 and 15 and dug out inside; on the lowest layer below the
        // floor (local -1) the lines x == 2, x == 13, z == 2 and z == 13 stay brick as the frame of
        // the room floor above. GT6's four conditional scaffold pillars stand at (2,7), (13,7), (7,2)
        // and (7,13) - one per side that leads into another cell - and are the port's scaffold block
        // with GT6's facing (the port owns it: {@code GTToolBlocks} "scaffold").
        for (int y = lowest; y <= -1; y++) {
            for (int x = 0; x <= 15; x++) {
                for (int z = 0; z <= 15; z++) {
                    if (x == 0 || x == 15 || z == 0 || z == 15) {
                        data.bricks(x, y, z);
                    } else if (z == 7 && x == 2 && data.connected(-1, 0)) {
                        scaffold(data, x, y, z, Direction.EAST, brass); // SIDE_X_POS
                    } else if (z == 7 && x == 13 && data.connected(1, 0)) {
                        scaffold(data, x, y, z, Direction.WEST, brass); // SIDE_X_NEG
                    } else if (x == 7 && z == 2 && data.connected(0, -1)) {
                        scaffold(data, x, y, z, Direction.SOUTH, brass); // SIDE_Z_POS
                    } else if (x == 7 && z == 13 && data.connected(0, 1)) {
                        scaffold(data, x, y, z, Direction.NORTH, brass); // SIDE_Z_NEG
                    } else if (y == -1 && (x == 2 || x == 13 || z == 2 || z == 13)) {
                        data.bricks(x, y, z);
                    } else {
                        data.air(x, y, z);
                    }
                }
            }
        }

        // GT6's room floor: on local y 0 the four lines x == 2, x == 13, z == 2 and z == 13
        // carry a scaffold each (facing out of the room) and the four corners a lit lamp whose redstone
        // brick GT6 puts one layer below the room floor; every other floor cell is opened into the
        // shaft. Local y 1 carries the bars on the same lines.
        for (int x = 2; x <= 13; x++) {
            for (int z = 2; z <= 13; z++) {
                if (x == 2) {
                    if (z != 2 && z != 13) {
                        scaffold(data, x, 0, z, Direction.EAST, brass);
                        bars(data, x, 1, z, brass, 8);
                    } else {
                        data.lamp(x, 0, z, -1);
                    }
                } else if (x == 13) {
                    if (z != 2 && z != 13) {
                        scaffold(data, x, 0, z, Direction.WEST, brass);
                        bars(data, x, 1, z, brass, 4);
                    } else {
                        data.lamp(x, 0, z, -1);
                    }
                } else if (z == 2) {
                    scaffold(data, x, 0, z, Direction.SOUTH, brass);
                    bars(data, x, 1, z, brass, 2);
                } else if (z == 13) {
                    scaffold(data, x, 0, z, Direction.NORTH, brass);
                    bars(data, x, 1, z, brass, 1);
                } else {
                    data.air(x, 0, z);
                }
            }
        }

        // GT6's crossing: a room that connects on both X sides (or on both Z sides) gets a
        // walkway of scaffold across the shaft with a line of bars on it, and the two door cells lose
        // the railing. The walkway is the port's scaffold (GT6 SIDE_X_NEG resp. SIDE_Z_NEG), the bars
        // are the port's ported ones; a room with no crossing keeps the open pit.
        if (data.connected(1, 0) && data.connected(-1, 0)) {
            data.air(2, 1, 8);
            data.air(13, 1, 8);
            for (int x = 3; x <= 12; x++) {
                scaffold(data, x, 0, 8, Direction.WEST, brass);
                bars(data, x, 1, 8, brass, 3);
            }
        } else if (data.connected(0, 1) && data.connected(0, -1)) {
            data.air(8, 1, 2);
            data.air(8, 1, 13);
            for (int z = 3; z <= 12; z++) {
                scaffold(data, 8, 0, z, Direction.NORTH, brass);
                bars(data, 8, 1, z, brass, 12);
            }
        }

        // The 6x6 chamber at the top of the bedrock band leaves intact bedrock in place.
        for (int x = 5; x <= 10; x++) {
            for (int z = 5; z <= 10; z++) {
                if ((x != 5 && x != 10) || (z != 5 && z != 10)) {
                    carve(data, x, chamberTop, z);
                }
            }
        }

        // The inner chamber is one block lower.
        for (int x = 6; x <= 9; x++) {
            for (int z = 6; z <= 9; z++) {
                carve(data, x, chamberBottom, z);
            }
        }

        // GT6's four explosives at the chamber corners, facing up (SIDE_Y_POS): the boomstick
        // at (6,6), dynamite at (6,9) and (9,6) and the strong dynamite at (9,9) - the port registers all
        // three (GTToolBlocks BOOMSTICK / DYNAMITE / DYNAMITE_STRONG, GT6 ids 32104, 32713, 32712).
        // Written after both chamber carves, in GT6's own order: the carve above opens exactly these four
        // cells, so writing them first would erase them again.
        explosive(data, 6, chamberBottom, 6, GTToolBlocks.BOOMSTICK);
        explosive(data, 6, chamberBottom, 9, GTToolBlocks.DYNAMITE);
        explosive(data, 9, chamberBottom, 6, GTToolBlocks.DYNAMITE);
        explosive(data, 9, chamberBottom, 9, GTToolBlocks.DYNAMITE_STRONG);

        // GT6's loose raw ore: in each of the four 4x4 quadrants of the cell, three times
        // out of four a raw ore block sits on the world Y 5 layer, then two out of three one layer
        // above and then one in two a third one on top, growing pillars of the vein's own material
        // out of the vein (BlocksGT.blockRaw with the vein material as its meta).
        int[] start = {1, 11};
        int[] end = {4, 14};
        for (int a = 0; a < 2; a++) {
            for (int b = 0; b < 2; b++) {
                for (int x = start[a]; x <= end[a]; x++) {
                    for (int z = start[b]; z <= end[b]; z++) {
                        if (data.next3in4()) {
                            data.rawBlock(x, lowest, z, vein);
                            if (data.next2in3()) {
                                data.rawBlock(x, lowest + 1, z, vein);
                                if (data.next1in2()) {
                                    data.rawBlock(x, lowest + 2, z, vein);
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private static void carve(GTDungeonData data, int x, int y, int z) {
        BlockState state = data.get(x, y, z);
        if (!OreBlock.isBedrockFloor(state) && !state.hasBlockEntity()) data.air(x, y, z);
    }

    /**
     * The original GT6 metadata is the bar connection mask, so no orientation is lost.
     */
    private static void bars(GTDungeonData data, int x, int y, int z, boolean brass, int mask) {
        BarsBlock block = brass
                ? (BarsBlock) DungeonBindings.block("bars_brass")
                : (BarsBlock) DungeonBindings.block("bars_steel");
        data.set(x, y, z, block.defaultBlockState().setValue(BarsBlock.MASK, mask));
    }

    /**
     * GT6's scaffold multi-tile ({@code 8408} brass / {@code 8410} steel, {@code MultiTileEntityScaffold}):
     * the port's material-specific scaffold ({@link ShapedToolBlock}, GT6's four horizontal
     * facings). GT6 draws its brass or steel form with the room's own {@code tBrass} roll;
     * the same room roll now selects the corresponding material for both scaffold and bars.
     */
    private static void scaffold(GTDungeonData data, int x, int y, int z, Direction facing, boolean brass) {
        BlockState state = gregtech.content.registry.GTManualStations.scaffold(
                        brass ? gregtech.content.material.Materials.Brass : gregtech.content.material.Materials.Steel)
                .defaultBlockState();
        data.set(x, y, z, state.setValue(ShapedToolBlock.FACING, facing));
    }

    /**
     * GT6's explosives at the four chamber corners, each facing up
     * ({@code NBT_FACING, SIDE_Y_POS}) - the port's three dynamite blocks. GT6's extra
     * {@code NBT_MODE} is retained as SUNK; the first server tick checks whether the backing stone
     * still supports the embedded charge.
     */
    private static void explosive(
            GTDungeonData data,
            int x,
            int y,
            int z,
            gregtech.content.registry.RegisteredBlock<? extends DynamiteBlock> block) {
        BlockState state = data.get(x, y, z);
        if (OreBlock.isBedrockFloor(state) || state.hasBlockEntity()) return;
        data.set(
                x,
                y,
                z,
                block.get()
                        .defaultBlockState()
                        .setValue(DirectionalBlock.FACING, Direction.UP)
                        .setValue(DynamiteBlock.SUNK, true));
    }
}
