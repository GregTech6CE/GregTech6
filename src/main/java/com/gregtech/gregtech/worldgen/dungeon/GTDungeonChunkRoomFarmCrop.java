package com.gregtech.gregtech.worldgen.dungeon;

import com.gregtech.gregtech.block.wood.WoodSpecies;
import com.gregtech.gregtech.registry.GTWoods;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Port of GT6's {@code DungeonChunkRoomFarmCrop}
 * ({@code gregapi/worldgen/dungeon/DungeonChunkRoomFarmCrop.java:35-229}): GT6's crop farm.
 *
 * <p>The empty room is built first (GT6's {@code :38} calls its super class and refuses the cell when that
 * fails), then the four corners of the hall become plots: a rim of smooth rock slabs at local 1, 5 and 6
 * (one GT6 slab orientation per wall) encloses each 4x4 quadrant, a lamp lights every plot cell from local
 * 5, the plots carry farmland with a random crop and, where they touch the middle of the hall, a melon and
 * pumpkin patch; the inner corner cell of each quadrant is water with one of GT6's glowing Glowtus plants.
 * Four planters with sugar cane and cactus stand at the plot corners, and every side of the cell that has
 * no neighbour gets a garden: a wall of jungle wood carrying cocoa beans, planters with saplings and
 * flowers, and one tall double plant in the middle.</p>
 *
 * <p>Every write lands inside the hall of {@link GTDungeonChunkRoomEmpty} (local {@code 0..8}: floor 0,
 * interior 1..6, ceiling 7), so GT6's Y coordinates are kept unchanged - the rims and lamps at 5 and 6,
 * the planters at 4 and the double plants at 5 and 6 are the room's top interior layers, exactly as in
 * GT6's nine block tall room.</p>
 *
 * <h2>Port differences</h2>
 * <ul>
 *   <li><b>Crops.</b> GT6's list ({@code :59-124}) is vanilla carrots, potatoes and wheat, Et Futurum's
 *       beetroot and 55 HarvestCraft crops. The port keeps the four crops GT6's list starts with -
 *       carrots, potatoes, wheat and beetroots from 1.20.1, in GT6's order - and drops the other-mod
 *       entries. GT6 rolls growth stage {@code next(8)} for every crop ({@code :141}); beetroots have four
 *       stages in 1.20.1 ({@code AGE_3}, 0..3), so that roll is halved onto them ({@code stage / 2}) and the
 *       other three keep the full {@code AGE_7} range.</li>
 *   <li><b>Glowtus.</b> The four water cells ({@code :131}) use the port's sixteen Glowtus colour blocks,
 *       selected in GT6's metadata order.</li>
 *   <li><b>Plot rims.</b> GT6 places <em>slabs</em> of its two rocks here with one orientation per wall
 *       ({@code aData.mPrimary.mSlabs[SIDE_...]}, {@code :42-55}); the port's slab helper keeps GT6's
 *       rock, its smooth form and its orientation ({@link GTDungeonData#slab}), so the rims are GT6's
 *       again.</li>
 *   <li><b>Planters.</b> GT6's multi-tile 32065 is its "Universal Plant Pot"
 *       ({@code Loader_MultiTileEntities:2228}). The port's {@code plant_pot} now supports plants on its
 *       top face, including the cactus and sugar cane at local 2..4 and the garden saplings/flowers.</li>
 *   <li><b>Sugar cane and cactus.</b> GT6's {@code Blocks.reeds} and {@code Blocks.cactus} are vanilla and
 *       are kept ({@code Blocks.SUGAR_CANE} at (5,1,5) and (10,1,10), {@code Blocks.CACTUS} at (5,1,10)
 *       and (10,1,5), each three blocks tall as in GT6's {@code :146-149}).</li>
 *   <li><b>The gardens.</b> GT6 builds them on every side whose neighbour cell is empty
 *       ({@code mRoomLayout[..] == 0}, {@code :152/171/190/209}), ported as
 *       {@code data.neighbour(dx, dz) == GTDungeonLayout.EMPTY}. GT6's jungle logs are the 1.7.10
 *       {@code Blocks.log} metas 7 and 11 (both resolve to jungle wood, the only wood cocoa attaches to);
 *       the port uses {@code Blocks.JUNGLE_LOG}, and cocoa's metaless 1.7.10 form becomes a 1.20.1 state
 *       ({@code CocoaBlock.FACING} is the direction from the cocoa towards its log, {@code CocoaBlock.AGE}
 *       is GT6's {@code next(3)}).</li>
 *   <li><b>Saplings.</b> GT6 offers its eight {@code BlocksGT.Saplings_AB} variants, the single
 *       {@code Saplings_CD} Blue Spruce variant, and six vanilla saplings ({@code :160}). The port has
 *       one block per GT species rather than metadata. It resolves that block during generation, after
 *       registration, and keeps GT6's four rolls in order: AB meta, CD meta, vanilla meta, family pick.
 *       The CD roll has bound one, just as {@code BlockTreeSaplingCD.maxMeta()} does in GT6.</li>
 *   <li><b>Tall plants.</b> GT6 places GT6's vanilla {@code Blocks.double_plant} with the 1.7.10 metas 1,
 *       0, 4 and 5 for the lower half and always meta 9 for the upper one ({@code :166/186/204/224}), which
 *       is the upper half of a different variety for the sunflower, rose bush and peony. 1.20.1 needs both
 *       halves to be states of the same block, so the upper half repeats the lower half's plant
 *       (lilac, sunflower, rose bush, peony).</li>
 *   <li><b>Rolls.</b> GT6's crop roll ({@code next(tCrops.size())}) and growth roll, both melon/pumpkin
 *       rolls of the patch ({@code :136}, which rolls both growth stages before it picks the block) and the
 *       cocoa {@code next(3)} and Glowtus colour rolls are kept in GT6's order; the port's {@code pot},
 *       {@code flower} and {@code lamp} helpers roll or not roll on their own.</li>
 * </ul>
 */
public class GTDungeonChunkRoomFarmCrop extends GTDungeonChunkRoomEmpty {

    /** GT6's tag ({@code WorldgenDungeonGT.TAG_FARM_CROP}, {@code WorldgenDungeonGT:81}). */
    public static final String TAG_FARM_CROP = "gt.dungeon.farm.crop";

    /** GT6's {@code tCrops} list ({@code :59-124}) as 1.20.1 crops, in GT6's order (see the javadoc). */
    private static final Block[] CROPS = {
            Blocks.CARROTS, Blocks.POTATOES, Blocks.WHEAT, Blocks.BEETROOTS};

    /** The six vanilla saplings of GT6's {@code Blocks.sapling} meta 0..5. */
    private static final Block[] SAPLINGS = {
            Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.BIRCH_SAPLING,
            Blocks.JUNGLE_SAPLING, Blocks.ACACIA_SAPLING, Blocks.DARK_OAK_SAPLING};

    /** GT6 BlockTreeSaplingAB metadata 0..7, in its constructor's order. */
    private static final WoodSpecies[] SAPLINGS_AB = {
            WoodSpecies.RUBBER, WoodSpecies.MAPLE, WoodSpecies.WILLOW, WoodSpecies.BLUE_MAHOE,
            WoodSpecies.HAZEL, WoodSpecies.CINNAMON, WoodSpecies.COCONUT, WoodSpecies.RAINBOWOOD};

    /** GT6 BlockTreeSaplingCD has maxMeta() == 1: Blue Spruce only. */
    private static final WoodSpecies[] SAPLINGS_CD = {WoodSpecies.BLUE_SPRUCE};

    /**
     * GT6's planter multi-tile {@code 32065} ({@code :146-149}, {@code :160-168}) as the port's own
     * {@code gregtech:plant_pot}.
     *
     * <p>The registered {@code plant_pot} has GT6's top-face plant support; a plain vanilla flower pot
     * cannot support the plants written one block above it.</p>
     */
    private static void planter(GTDungeonData data, int x, int y, int z) {
        Block block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech", "plant_pot"));
        data.set(x, y, z, (block == null ? Blocks.FLOWER_POT : block).defaultBlockState());
    }

    /**
     * GT6's sapling write ({@code :160-163}): {@code aData.set(x, y, z, BlocksGT.Saplings_AB,
     * next(Saplings_AB.maxMeta()), BlocksGT.Saplings_CD, next(Saplings_CD.maxMeta()), Blocks.sapling,
     * next(6))}. GT6's three-block overload evaluates the three <em>metas</em> first (left to right) and
     * then draws one more number to pick among the three blocks ({@code next(3)},
     * {@code DungeonData:378-386}), so one sapling costs four rolls; the port keeps all four in that
     * order and rolls over its own family sizes instead of GT6's {@code maxMeta}.
     */
    private static void sapling(GTDungeonData data, int x, int y, int z) {
        int ab = data.next(SAPLINGS_AB.length);
        int cd = data.next(SAPLINGS_CD.length);
        int vanilla = data.next(SAPLINGS.length);
        switch (data.next(3)) {
            case 0 -> data.set(x, y, z, GTWoods.sapling(SAPLINGS_AB[ab]).defaultBlockState());
            case 1 -> data.set(x, y, z, GTWoods.sapling(SAPLINGS_CD[cd]).defaultBlockState());
            default -> data.set(x, y, z, SAPLINGS[vanilla].defaultBlockState());
        }
    }

    /** GT6's moist farmland ({@code Blocks.farmland} meta 15, {@code :133}). */
    private static final BlockState FARMLAND =
            Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7);

    @Override
    public boolean generate(GTDungeonData data) {
        // GT6's :38.
        if (data.hasTag(TAG_FARM_CROP) || !super.generate(data)) return false;
        data.tags.add(TAG_FARM_CROP);

        // GT6's :41-56 - the rims of the four plots, in GT6's write order: local 1, then 5, then 6. Every
        // rim piece is a smooth rock slab in GT6's per-wall orientation: the wall at z = 5 carries
        // mSlabs[SIDE_Z_NEG] (its half towards the plot), z = 10 mSlabs[SIDE_Z_POS], x = 5
        // mSlabs[SIDE_X_NEG] and x = 10 mSlabs[SIDE_X_POS].
        for (int tCoord = 1; tCoord <= 14; tCoord++) {
            if (tCoord <= 4 || tCoord >= 11) {
                data.smoothSlab(tCoord, 1, 5, Direction.NORTH);
                data.smoothSlab(tCoord, 1, 10, Direction.SOUTH);
                data.smoothSlab(5, 1, tCoord, Direction.WEST);
                data.smoothSlab(10, 1, tCoord, Direction.EAST);

                data.smoothSlab(tCoord, 5, 5, Direction.NORTH);
                data.smoothSlab(tCoord, 5, 10, Direction.SOUTH);
                data.smoothSlab(5, 5, tCoord, Direction.WEST);
                data.smoothSlab(10, 5, tCoord, Direction.EAST);

                data.smoothSlab(tCoord, 6, 5, Direction.NORTH);
                data.smoothSlab(tCoord, 6, 10, Direction.SOUTH);
                data.smoothSlab(5, 6, tCoord, Direction.WEST);
                data.smoothSlab(10, 6, tCoord, Direction.EAST);
            }
        }

        // GT6's :126-144 - the plots themselves: farmland with a crop, the melon and pumpkin patch, and the
        // four water cells of a quadrant. The lamp lights every plot cell from local 5, +1 is GT6's
        // redstone brick one block above it.
        for (int tX = 1; tX <= 14; tX++) {
            for (int tZ = 1; tZ <= 14; tZ++) {
                if ((tX <= 4 || tX >= 11) && (tZ <= 4 || tZ >= 11)) {
                    data.lamp(tX, 5, tZ, +1);

                    if (tX >= 4 && tX <= 11 && tZ >= 4 && tZ <= 11) {
                        // GT6 :130-131 - one water source under a colour-rolled Glowtus.
                        data.set(tX, 1, tZ, Blocks.WATER.defaultBlockState());
                        data.glowtus(tX, 2, tZ);
                    } else {
                        data.set(tX, 1, tZ, FARMLAND);

                        if (tX >= 8 && tZ >= 8) {
                            // GT6's :135-139 - the melon and pumpkin patch: a stem on the even cells, a
                            // whole fruit on the odd ones. GT6 rolls both stems' growth stage first and only
                            // then which of the two blocks it places ({@code :136}), and applies the chosen
                            // block's own stage, so the order of the rolls is kept.
                            if (even(tX, tZ)) {
                                int melonAge = data.next(8);
                                int pumpkinAge = data.next(8);
                                boolean melon = data.next(2) == 0;
                                data.set(tX, 2, tZ, (melon ? Blocks.MELON_STEM : Blocks.PUMPKIN_STEM)
                                        .defaultBlockState()
                                        .setValue(BlockStateProperties.AGE_7, melon ? melonAge : pumpkinAge));
                            } else {
                                // GT6's :138 - a whole melon or pumpkin, chosen by a roll of two.
                                data.set(tX, 2, tZ, (data.next(2) == 0 ? Blocks.MELON : Blocks.PUMPKIN)
                                        .defaultBlockState());
                            }
                        } else {
                            // GT6's :141 - a random crop with a random growth stage.
                            Block crop = CROPS[data.next(CROPS.length)];
                            data.set(tX, 2, tZ, cropState(crop, data.next(8)));
                        }
                    }
                }
            }
        }

        // GT6's :146-149 - the four planters of the plot corners: sugar cane at (5,1,5) and (10,1,10),
        // cactus at (5,1,10) and (10,1,5), each three blocks tall.
        planter(data, 5, 1, 5);
        for (int tY = 2; tY <= 4; tY++) data.set(5, tY, 5, Blocks.SUGAR_CANE.defaultBlockState());
        planter(data, 5, 1, 10);
        for (int tY = 2; tY <= 4; tY++) data.set(5, tY, 10, Blocks.CACTUS.defaultBlockState());
        planter(data, 10, 1, 5);
        for (int tY = 2; tY <= 4; tY++) data.set(10, tY, 5, Blocks.CACTUS.defaultBlockState());
        planter(data, 10, 1, 10);
        for (int tY = 2; tY <= 4; tY++) data.set(10, tY, 10, Blocks.SUGAR_CANE.defaultBlockState());

        // GT6's :152-169 - the garden on the side towards the neighbour at X + 1. Its planters carry a
        // sapling at local 2 (GT6's :160), a flower at local 5 and one tall plant at local 5 and 6.
        if (data.neighbour(1, 0) == GTDungeonLayout.EMPTY) {
            data.smoothSlab(14, 3, 5, Direction.SOUTH);
            // GT6's cocoa metas come from COMPASS_FROM_SIDE (CS.java:516-521): the west wall (SIDE_X_NEG)
            // is meta 3, which is a cocoa attached to the log on its east side.
            cocoaPair(data, 14, 6, 13, 6, Direction.EAST);
            cocoaPair(data, 14, 7, 13, 7, Direction.EAST);
            cocoaPair(data, 14, 8, 13, 8, Direction.EAST);
            cocoaPair(data, 14, 9, 13, 9, Direction.EAST);
            data.smoothSlab(14, 3, 10, Direction.NORTH);

            for (int tZ = 6; tZ <= 9; tZ++) {
                planter(data, 14, 1, tZ);
                sapling(data, 14, 2, tZ);
            }

            planter(data, 14, 4, 6);
            data.flower(14, 5, 6);
            planter(data, 14, 4, 7);
            tallPlant(data, 14, 5, 7, Blocks.LILAC);
            planter(data, 14, 4, 8);
            data.flower(14, 5, 8);
            planter(data, 14, 4, 9);
            data.flower(14, 5, 9);
        }

        // GT6's :171-188 - the garden towards the neighbour at X - 1. The east wall (SIDE_X_POS) is cocoa
        // meta 1, a cocoa attached to the log on its west side.
        if (data.neighbour(-1, 0) == GTDungeonLayout.EMPTY) {
            data.smoothSlab(1, 3, 5, Direction.SOUTH);
            cocoaPair(data, 1, 6, 2, 6, Direction.WEST);
            cocoaPair(data, 1, 7, 2, 7, Direction.WEST);
            cocoaPair(data, 1, 8, 2, 8, Direction.WEST);
            cocoaPair(data, 1, 9, 2, 9, Direction.WEST);
            data.smoothSlab(1, 3, 10, Direction.NORTH);

            for (int tZ = 6; tZ <= 9; tZ++) {
                planter(data, 1, 1, tZ);
                sapling(data, 1, 2, tZ);
            }

            planter(data, 1, 4, 6);
            data.flower(1, 5, 6);
            planter(data, 1, 4, 7);
            data.flower(1, 5, 7);
            // GT6's :186 pairs the lower meta 0 (sunflower) with the upper meta 9 (lilac).
            planter(data, 1, 4, 8);
            tallPlant(data, 1, 5, 8, Blocks.SUNFLOWER);
            planter(data, 1, 4, 9);
            data.flower(1, 5, 9);
        }

        // GT6's :190-207 - the garden towards the neighbour at Z + 1. The north wall (SIDE_Z_NEG) is cocoa
        // meta 0, a cocoa attached to the log on its south side.
        if (data.neighbour(0, 1) == GTDungeonLayout.EMPTY) {
            data.smoothSlab(5, 3, 14, Direction.EAST);
            cocoaPair(data, 6, 14, 6, 13, Direction.SOUTH);
            cocoaPair(data, 7, 14, 7, 13, Direction.SOUTH);
            cocoaPair(data, 8, 14, 8, 13, Direction.SOUTH);
            cocoaPair(data, 9, 14, 9, 13, Direction.SOUTH);
            data.smoothSlab(10, 3, 14, Direction.WEST);

            for (int tX = 6; tX <= 9; tX++) {
                planter(data, tX, 1, 14);
                sapling(data, tX, 2, 14);
            }

            planter(data, 6, 4, 14);
            data.flower(6, 5, 14);
            // GT6's :204 pairs the lower meta 4 (rose bush) with the upper meta 9 (lilac).
            planter(data, 7, 4, 14);
            tallPlant(data, 7, 5, 14, Blocks.ROSE_BUSH);
            planter(data, 8, 4, 14);
            data.flower(8, 5, 14);
            planter(data, 9, 4, 14);
            data.flower(9, 5, 14);
        }

        // GT6's :209-226 - the garden towards the neighbour at Z - 1. The south wall (SIDE_Z_POS) is cocoa
        // meta 2, a cocoa attached to the log on its north side.
        if (data.neighbour(0, -1) == GTDungeonLayout.EMPTY) {
            data.smoothSlab(5, 3, 1, Direction.EAST);
            cocoaPair(data, 6, 1, 6, 2, Direction.NORTH);
            cocoaPair(data, 7, 1, 7, 2, Direction.NORTH);
            cocoaPair(data, 8, 1, 8, 2, Direction.NORTH);
            cocoaPair(data, 9, 1, 9, 2, Direction.NORTH);
            data.smoothSlab(10, 3, 1, Direction.WEST);

            for (int tX = 6; tX <= 9; tX++) {
                planter(data, tX, 1, 1);
                sapling(data, tX, 2, 1);
            }

            planter(data, 6, 4, 1);
            data.flower(6, 5, 1);
            planter(data, 7, 4, 1);
            data.flower(7, 5, 1);
            // GT6's :224 pairs the lower meta 5 (peony) with the upper meta 9 (lilac).
            planter(data, 8, 4, 1);
            tallPlant(data, 8, 5, 1, Blocks.PEONY);
            planter(data, 9, 4, 1);
            data.flower(9, 5, 1);
        }

        return true;
    }

    /**
     * GT6's crop with a growth stage: the three {@code AGE_7} crops keep GT6's {@code next(8)} stage,
     * beetroots have four stages ({@code AGE_3}, 0..3) in 1.20.1, so the same eight stage roll is halved
     * onto them.
     */
    private static BlockState cropState(Block crop, int stage) {
        return crop == Blocks.BEETROOTS
                ? Blocks.BEETROOTS.defaultBlockState().setValue(BlockStateProperties.AGE_3, stage / 2)
                : crop.defaultBlockState().setValue(BlockStateProperties.AGE_7, stage);
    }

    /**
     * GT6's garden wall ({@code :154-157}): one jungle log at local 3 with a cocoa in front of it. GT6
     * writes the log first and rolls the cocoa's growth stage while building the cocoa's 1.7.10 metadata,
     * which is why the roll sits between the two writes here as well.
     */
    private static void cocoaPair(GTDungeonData data, int logX, int logZ, int cocoaX, int cocoaZ,
                                  Direction cocoaFacing) {
        data.set(logX, 3, logZ, Blocks.JUNGLE_LOG.defaultBlockState());
        data.set(cocoaX, 3, cocoaZ, Blocks.COCOA.defaultBlockState()
                .setValue(CocoaBlock.FACING, cocoaFacing)
                .setValue(CocoaBlock.AGE, data.next(3)));
    }

    /** GT6's tall plant ({@code :166}): the lower half at {@code y}, the upper one at {@code y + 1}. */
    private static void tallPlant(GTDungeonData data, int x, int y, int z, Block plant) {
        data.set(x, y, z, plant.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        data.set(x, y + 1, z, plant.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /**
     * GT6's {@code WD.even(tX, 2, tZ)} ({@code WD.java}): the number of even arguments has to be even,
     * which with the constant 2 is a checkerboard over the plot.
     */
    private static boolean even(int x, int z) {
        int count = 1;
        if (x % 2 == 0) count++;
        if (z % 2 == 0) count++;
        return count % 2 == 0;
    }
}
