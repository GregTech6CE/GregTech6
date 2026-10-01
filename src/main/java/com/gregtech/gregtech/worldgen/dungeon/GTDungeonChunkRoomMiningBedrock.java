package com.gregtech.gregtech.worldgen.dungeon;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.misc.BarsBlock;
import com.gregtech.gregtech.block.tool.DynamiteBlock;
import com.gregtech.gregtech.block.tool.ShapedToolBlock;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.OreMaterials;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import com.gregtech.gregtech.registry.GTToolBlocks;
import com.gregtech.gregtech.worldgen.GTBedrockOreFeature;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of GT6's {@code DungeonChunkRoomMiningBedrock}
 * ({@code gregapi/worldgen/dungeon/DungeonChunkRoomMiningBedrock.java:34-139}).
 *
 * <p>The bedrock mine: GT6 first generates a bedrock ore vein of one of thirteen materials under
 * the cell and refuses the cell when it cannot (the vein writer requires bedrock at the cell
 * centre); it then digs a 16x16 shaft from the layer under the room floor down to the world Y just
 * above the bedrock band, replaces the room's floor with a lattice of metal scaffolds, bars and
 * four lamps, carves a chamber into the top of the ore vein and grows loose raw ore pillars from
 * the vein up through the bottom of the shaft.</p>
 *
 * <h2>Local Y</h2>
 * <p>GT6 mixes two kinds of Y here: the room's own layers (floor 0, bars 1) and absolute world
 * levels near bedrock (world Y 5, 4 and 3, written as {@code 5-aData.mY} and so on). The port keeps
 * both, with GT6's world levels mapped onto the port's bedrock band: the port's band lies at
 * {@link net.minecraft.world.level.WorldGenLevel#getMinBuildHeight()}, so GT6's world Y 5 becomes
 * {@code getMinBuildHeight() + 5} (the first layer above the band, exactly like GT6), its world Y 4
 * and 3 become {@code +4} and {@code +3}, and the whole shaft is therefore as far below the room
 * floor as the port's stretched depth band demands (see {@code GTWorldgenScale}).</p>
 *
 * <h2>Port differences</h2>
 * <ul>
 *   <li><b>The vein.</b> GT6 calls {@code WorldgenOresBedrock.generateVein} with one of thirteen
 *       materials chosen by {@code UT.Code.select} (:39-40), i.e. from its global {@code RNGSUS} and
 *       <em>not</em> from the cell's stream. The port calls its own port of that vein writer,
 *       {@link GTBedrockOreFeature#placeVein}, with the same thirteen materials - the port has every one
 *       of them ({@code Materials.Redstone}, {@code Materials.Sulfur}, {@code Materials.Hematite} for
 *       {@code MT.Fe2O3}, {@code Materials.Pyrolusite} for {@code MT.MnO2}, {@code Materials.Apatite}
 *       and the seven {@code MT.OREMATS} ores) - and picks them with
 *       {@link GTDungeonData#select} like GT6 does, so the cell's own roll sequence stays GT6's.</li>
 *   <li><b>The refusal.</b> GT6 returns false when {@code generateVein} fails, which it does when
 *       the cell centre has no bedrock ({@code WorldgenOresBedrock:183-185}). The port tests that
 *       with {@link GTDungeonData#aboveBedrock}, whose result is the inverse of GT6's test. The
 *       port's vein writer repeats the same test internally, so its result is not used twice: a
 *       material whose port ore block is missing must not refuse a cell GT6 would have built.</li>
 *   <li><b>The raw ore pillars.</b> GT6 places {@code BlocksGT.blockRaw} with the vein material as
 *       its meta (:130-132). The port owns that block ({@code MaterialPrefixes.blockRaw}, a block per
 *       material) and places it through {@link GTDungeonData#rawBlock(int, int, int, GTMaterial)}, so
 *       the pillars are GT6's own blocks now; earlier batches of the port used a vanilla diamond-pickaxe
 *       ore of the matching material here.</li>
 *   <li><b>The scaffold and the explosives.</b> GT6's brass/steel roll selects both the bars
 *       and the matching material scaffold throughout the room. The corner explosives use the
 *       port's three dynamite variants, including their original embedded NBT_MODE state.</li>
 *   <li><b>The chamber.</b> GT6 carves its chamber at world Y 4 and 3 (:112-120), inside its 0..4
 *       bedrock band, because its vein had already turned that bedrock into deepslate and ore. The
 *       port's band is still bedrock, so the port carves a chamber cell only where the vein broke
 *       that column's bedrock floor ({@link GTDungeonData#aboveBedrock}) - GT6's rule of stopping
 *       where the rock becomes bedrock. The chamber therefore comes out ragged where the vein left
 *       bedrock, the explosive cells stay solid and the world floor keeps its bedrock.</li>
 *   <li><b>The bars.</b> GT6's Brass and Steel Bars retain their exact four-bit connection
 *       masks (1 = north, 2 = south, 4 = west, 8 = east), including the two-sided
 *       walkway rails.</li>
 *   <li><b>Tag scope.</b> GT6 shares one tag set across the whole dungeon, so only one bedrock mine
 *       is ever built per dungeon. {@code GTDungeonFeature.generateCell} hands every cell its own
 *       empty tag set, so the port's rule is one bedrock mine per cell.</li>
 * </ul>
 */
public class GTDungeonChunkRoomMiningBedrock extends GTDungeonChunkRoomEmpty {

    /** GT6's room tag ({@code WorldgenDungeonGT:75}); a cell that already carries it is refused. */
    public static final String TAG_MINING_BEDROCK = "gt.dungeon.mining.bedrock";

    /**
     * GT6's thirteen bedrock ore materials ({@code :39}: redstone twice, then sulfur, hematite,
     * pyrolusite, apatite, molybdenite, bauxite, sphalerite, tetrahedrite, cassiterite, garnierite and
     * galena) in GT6's own order and multiplicity. The vector drives both the vein ({@code :40}) and the
     * raw ore blocks of the pillars ({@code :130-132}), so the port keeps it as GT6 wrote it -
     * {@code UT.Code.select(MT.Redstone, MT.Redstone, MT.S, MT.Fe2O3, MT.MnO2, MT.Apatite, ...)}.
     */
    private static final GTMaterial[] VEIN_MATERIALS = {
            Materials.Redstone, Materials.Redstone, Materials.Sulfur, Materials.Hematite,
            Materials.Pyrolusite, Materials.Apatite, OreMaterials.Molybdenite, OreMaterials.Bauxite,
            OreMaterials.Sphalerite, OreMaterials.Tetrahedrite, OreMaterials.Cassiterite,
            OreMaterials.Garnierite, OreMaterials.Galena};

    @Override
    public boolean generate(GTDungeonData data) {
        if (data.hasTag(TAG_MINING_BEDROCK) || !super.generate(data)) return false;
        data.tags.add(TAG_MINING_BEDROCK);

        // GT6: one of thirteen bedrock ore materials (:39) and its ore vein (:40). GT6 picks the
        // material with UT.Code.select, i.e. from its global RNG rather than the cell's stream, so the
        // port draws it from a stream of its own (GTDungeonData.select) - the cell's roll sequence has
        // to stay GT6's.
        GTMaterial vein = data.select(VEIN_MATERIALS, 0, 0, 0);
        // GT6's vein writer requires existing bedrock under the cell centre and makes the room
        // refuse the cell without it (WorldgenOresBedrock:183-185); aboveBedrock is that test
        // inverted (GTDungeonData:328-331).
        if (data.aboveBedrock(8, 8)) return false;
        GTBedrockOreFeature.placeVein(data.level, data.random, data.x, data.z, vein);

        // GT6: brass or steel for the whole room (:42).
        boolean brass = data.next1in2();

        int floor = data.level.getMinBuildHeight();
        int lowest = floor + 5 - data.y;        // GT6's world Y 5: one layer above the bedrock band
        int chamberTop = floor + 4 - data.y;    // GT6's world Y 4
        int chamberBottom = floor + 3 - data.y; // GT6's world Y 3

        // GT6's shaft (:44-68): the whole cell from the layer under the room floor down to world Y 5
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
                        scaffold(data, x, y, z, Direction.EAST, brass);        // GT6 :55 SIDE_X_POS
                    } else if (z == 7 && x == 13 && data.connected(1, 0)) {
                        scaffold(data, x, y, z, Direction.WEST, brass);        // GT6 :57 SIDE_X_NEG
                    } else if (x == 7 && z == 2 && data.connected(0, -1)) {
                        scaffold(data, x, y, z, Direction.SOUTH, brass);       // GT6 :59 SIDE_Z_POS
                    } else if (x == 7 && z == 13 && data.connected(0, 1)) {
                        scaffold(data, x, y, z, Direction.NORTH, brass);       // GT6 :61 SIDE_Z_NEG
                    } else if (y == -1 && (x == 2 || x == 13 || z == 2 || z == 13)) {
                        data.bricks(x, y, z);
                    } else {
                        data.air(x, y, z);
                    }
                }
            }
        }

        // GT6's room floor (:70-94): on local y 0 the four lines x == 2, x == 13, z == 2 and z == 13
        // carry a scaffold each (facing out of the room) and the four corners a lit lamp whose redstone
        // brick GT6 puts one layer below the room floor; every other floor cell is opened into the
        // shaft. Local y 1 carries the bars on the same lines.
        for (int x = 2; x <= 13; x++) {
            for (int z = 2; z <= 13; z++) {
                if (x == 2) {
                    if (z != 2 && z != 13) {
                        scaffold(data, x, 0, z, Direction.EAST, brass);        // GT6 :74
                        bars(data, x, 1, z, brass, 8);
                    } else {
                        data.lamp(x, 0, z, -1);
                    }
                } else if (x == 13) {
                    if (z != 2 && z != 13) {
                        scaffold(data, x, 0, z, Direction.WEST, brass);        // GT6 :81
                        bars(data, x, 1, z, brass, 4);
                    } else {
                        data.lamp(x, 0, z, -1);
                    }
                } else if (z == 2) {
                    scaffold(data, x, 0, z, Direction.SOUTH, brass);           // GT6 :87
                    bars(data, x, 1, z, brass, 2);
                } else if (z == 13) {
                    scaffold(data, x, 0, z, Direction.NORTH, brass);           // GT6 :90
                    bars(data, x, 1, z, brass, 1);
                } else {
                    data.air(x, 0, z);
                }
            }
        }

        // GT6's crossing (:96-110): a room that connects on both X sides (or on both Z sides) gets a
        // walkway of scaffold across the shaft with a line of bars on it, and the two door cells lose
        // the railing. The walkway is the port's scaffold (GT6 SIDE_X_NEG resp. SIDE_Z_NEG), the bars
        // are the port's ported ones; a room with no crossing keeps the open pit.
        if (data.connected(1, 0) && data.connected(-1, 0)) {
            data.air(2, 1, 8);
            data.air(13, 1, 8);
            for (int x = 3; x <= 12; x++) {
                scaffold(data, x, 0, 8, Direction.WEST, brass);                // GT6 :100
                bars(data, x, 1, 8, brass, 3);
            }
        } else if (data.connected(0, 1) && data.connected(0, -1)) {
            data.air(8, 1, 2);
            data.air(8, 1, 13);
            for (int z = 3; z <= 12; z++) {
                scaffold(data, 8, 0, z, Direction.NORTH, brass);               // GT6 :107
                bars(data, 8, 1, z, brass, 12);
            }
        }

        // GT6's chamber at world Y 4 (:112-116): the 6x6 centre of the cell minus its four corners,
        // carved only where the vein broke the column's bedrock floor.
        for (int x = 5; x <= 10; x++) {
            for (int z = 5; z <= 10; z++) {
                if ((x != 5 && x != 10) || (z != 5 && z != 10)) {
                    carve(data, x, chamberTop, z);
                }
            }
        }

        // GT6's chamber at world Y 3 (:118-120): the inner 6x6, again only where the vein took the
        // bedrock.
        for (int x = 6; x <= 9; x++) {
            for (int z = 6; z <= 9; z++) {
                carve(data, x, chamberBottom, z);
            }
        }

        // GT6's four explosives at the chamber corners (:122-125), facing up (SIDE_Y_POS): the boomstick
        // at (6,6), dynamite at (6,9) and (9,6) and the strong dynamite at (9,9) - the port registers all
        // three (GTToolBlocks BOOMSTICK / DYNAMITE / DYNAMITE_STRONG, GT6 ids 32104, 32713, 32712).
        // Written after both chamber carves, in GT6's own order: the carve above opens exactly these four
        // cells, so writing them first would erase them again.
        explosive(data, 6, chamberBottom, 6, GTToolBlocks.BOOMSTICK);       // GT6 :122
        explosive(data, 6, chamberBottom, 9, GTToolBlocks.DYNAMITE);        // GT6 :123
        explosive(data, 9, chamberBottom, 6, GTToolBlocks.DYNAMITE);        // GT6 :124
        explosive(data, 9, chamberBottom, 9, GTToolBlocks.DYNAMITE_STRONG); // GT6 :125

        // GT6's loose raw ore (:127-135): in each of the four 4x4 quadrants of the cell, three times
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

    /**
     * GT6's chamber carve: a block of air where the bedrock ore vein already replaced the rock of
     * that column at the world floor. That is the port's form of GT6's "stop where the rock becomes
     * bedrock" rule - a column whose world floor is still bedrock keeps its block, so the carvings
     * never open a hole in the world floor.
     */
    private static void carve(GTDungeonData data, int x, int y, int z) {
        if (data.aboveBedrock(x, z)) data.air(x, y, z);
    }

    /**
     * The original GT6 metadata is the bar connection mask, so no orientation is lost.
     */
    private static void bars(GTDungeonData data, int x, int y, int z, boolean brass, int mask) {
        BarsBlock block = brass ? GTDecorBlocks.BARS_BRASS.get() : GTDecorBlocks.BARS_STEEL.get();
        data.set(x, y, z, block.defaultBlockState().setValue(BarsBlock.MASK, mask));
    }

    /**
     * GT6's scaffold multi-tile ({@code 8408} brass / {@code 8410} steel, {@code MultiTileEntityScaffold}):
     * the port's material-specific scaffold ({@link ShapedToolBlock}, GT6's four horizontal
     * facings). GT6 draws its brass or steel form with the room's own {@code tBrass} roll ({@code :42});
     * the same room roll now selects the corresponding material for both scaffold and bars.
     */
    private static void scaffold(GTDungeonData data, int x, int y, int z, Direction facing, boolean brass) {
        BlockState state = com.gregtech.gregtech.registry.GTToolBlocks.scaffold(brass
                ? com.gregtech.gregtech.content.material.Materials.Brass
                : com.gregtech.gregtech.content.material.Materials.Steel).defaultBlockState();
        if (state != null) data.set(x, y, z, state.setValue(ShapedToolBlock.FACING, facing));
    }

    /**
     * GT6's explosives at the four chamber corners ({@code :122-125}), each facing up
     * ({@code NBT_FACING, SIDE_Y_POS}) - the port's three dynamite blocks. GT6's extra
     * {@code NBT_MODE} is retained as SUNK; the first server tick checks whether the backing stone
     * still supports the embedded charge.
     */
    private static void explosive(GTDungeonData data, int x, int y, int z,
                                  net.minecraftforge.registries.RegistryObject<? extends DynamiteBlock> block) {
        data.set(x, y, z, block.get().defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP).setValue(DynamiteBlock.SUNK, true));
    }

    /** One of the port's blocks by registry name, or {@code null} when it is not registered. */
    private static BlockState port(String id) {
        Block block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech", id));
        return block == null ? null : block.defaultBlockState();
    }
}
