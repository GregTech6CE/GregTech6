package com.gregtech.gregtech.worldgen.dungeon;

import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.registry.GTFluids;

/**
 * Port of GT6's {@code DungeonChunkRoomStorage}
 * ({@code gregapi/worldgen/dungeon/DungeonChunkRoomStorage.java:38-215}): the dungeon's store room. Its
 * lowest four layers are packed with crates of metals, dusts, gems, wood and raw ore, a stack of three
 * crates can carry a loose pile of ingots or plates on top - and behind the vault door, GT6's four walls
 * of fluid tanks.
 *
 * <p>The room is a dead end: GT6's {@code DungeonChunkRoomVault} ({@code DungeonChunkRoomVault.java:27-34}),
 * which GT6's store room extends, refuses every cell that does not have exactly one connection, and GT6's
 * dispatcher only offers the dead end list to those cells ({@code WorldgenDungeonGT:260-267}). The port's
 * dispatcher copies that ({@code GTDungeonFeature:129-134}), and the check is kept here as well.</p>
 *
 * <h2>Port differences</h2>
 * <ul>
 *   <li><b>Vault inlined.</b> GT6's {@code DungeonChunkRoomVault} is a three line class between
 *       {@code DungeonChunkRoomEmpty} and this room, so the port inlines it instead of adding a file for
 *       it: the one connection check is the first line of {@link #generate(GTDungeonData)}.</li>
 *   <li><b>Vault door.</b> GT6 then builds its vault door across the room's single connection
 *       ({@code WorldgenDungeonGT.DOOR_PISTON}, {@code DungeonChunkRoomVault:31}), and does not let a
 *       failure of that door fail the room. The port calls its own
 *       {@link GTDungeonChunkDoorPiston} the same way.</li>
 *   <li><b>Fluid walls.</b> GT6 fills every side without a neighbour with columns of one to four fluid
 *       tank multi-tiles (32714 wooden and 32734 ironwood barrel, 32102 bronze drum - three fluid
 *       families, each with seven fluids) or a barometer gas cylinder (32055 propane, 32056 oxygen and
 *       helium), {@code :65-132}. The port owns every one of those containers
 *       ({@link GTDungeonData#container}) and builds all four walls, {@code :58-63} fluid tables
 *       included ({@link #fluidWall}). GT6's fluid pick is {@code UT.Code.select(NF, tFluids[tType])},
 *       which draws from its <em>global</em> RNG rather than the cell's stream, so the port draws it
 *       from a stream of its own ({@link GTDungeonData#select}) and the cell's roll sequence stays
 *       GT6's. Only two details differ: the port's gas cylinder has no paint channel (GT6 paints
 *       propane red, oxygen light blue, helium yellow) and GT6's {@code FL.lube} is the port's
 *       {@code Lubricant} fluid.</li>
     *   <li><b>Crates.</b> GT6 stacks one of its crate multi-tiles per cell - {@code BlocksGT.crateGtDust},
     *       {@code crateGtIngot}, {@code crateGtPlate}, {@code crateGtGem} and {@code crateGtRaw} plus their
     *       64x variants, twelve of them in the metal quadrant alone ({@code :135}). The port owns both
     *       sixteen- and sixty-four-piece blocks and this room places
 *       them through {@link GTDungeonData#crate} with GT6's own material tables
 *       ({@link GTDungeonData#CRATE_DUSTS}, {@code CRATE_WOODS}, {@code CRATE_GEMS}, {@code CRATE_METALS},
 *       {@link GTDungeonData#CRATE_ORES}) - GT6's block draw and material draw happen in that order, see
 *       {@link GTDungeonData#crate(int, int, int, GTDungeonData.CrateForm[], com.gregtech.gregtech.api.material.GTMaterial[])}.
     *       The size rolls ({@code next1in2}/{@code next1in3}/{@code next1in4} inside GT6's
     *       {@code set} calls) choose the corresponding block.</li>
 *   <li><b>Raw ore blocks.</b> GT6's ore quadrant tops its crate stacks with raw ore blocks
 *       ({@code BlocksGT.blockRaw}, {@code :179-182}); the port owns that block too
 *       ({@code MaterialPrefixes.blockRaw}) and places it through {@link GTDungeonData#rawBlock}, so the
 *       quadrant's four decorations and the roll of each are GT6's again.</li>
 *   <li><b>Mod quadrants.</b> GT6 rolls {@code next(7 + Hexxit + Thaumcraft)} for the category of each of
 *       the four quadrants and leaves the quadrant empty for the mods it does not have ({@code :138-209}).
 *       The port has neither mod, so it rolls {@code next(7)} and leaves cases 5 and 6 empty, which is
 *       exactly what GT6 does without them.</li>
     *   <li><b>Piles.</b> GT6's {@code ingots_or_plates} ({@code DungeonData:259-264}) rolls the ingot's
     *       material and stack size, the plate's material and stack size and then picks plate or ingot with
     *       {@code next1in2()}; the port passes the room's original metal list to
     *       {@link GTDungeonData#ingotsOrPlates(int, int, int, int, String...)}.</li>
 * </ul>
 *
 * <p>Kept from GT6: the coordinates (the four quadrants of local 1..3 and 12..14, in GT6's
 * {@code for (a)}/{@code for (b)} order), the heights (the crates at local 1..3 and the top pile at
 * local 4, well below the hall's ceiling at 7), the write order, the {@code next(7)} category roll per
 * quadrant and every roll that decides how many blocks a stack has and whether a pile joins it
 * ({@code next3in4}, {@code next2in3}, {@code next1in2} and the {@code next1in3} pile rolls).</p>
 */
public class GTDungeonChunkRoomStorage extends GTDungeonChunkRoomEmpty {

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

        // GT6's :65-132: the four fluid tank walls on the sides that have no neighbour cell. GT6 tests
        // mRoomLayout[mRoomX±1][mRoomZ] / [mRoomX][mRoomZ±1] == 0, which is the port's !connected(...).
        if (!data.connected(1, 0)) fluidWall(data, 12, 6, 3, 4);    // GT6 :65-81,  the +X wall
        if (!data.connected(-1, 0)) fluidWall(data, 1, 6, 3, 4);    // GT6 :82-98,  the -X wall
        if (!data.connected(0, 1)) fluidWall(data, 6, 12, 4, 3);    // GT6 :99-115, the +Z wall
        if (!data.connected(0, -1)) fluidWall(data, 6, 1, 4, 3);    // GT6 :116-132, the -Z wall

        // GT6's :135-136: the crate variants and the four quadrants (start/end are GT6's tStart/tEnd).
        int[] start = {1, 12}, end = {3, 14};
        for (int a = 0; a < 2; a++) {
            for (int b = 0; b < 2; b++) {
                // GT6's :138: one category per quadrant, rolled after the two mods the port does not have.
                switch (data.next(7)) {
                    case 0 -> {
                        // GT6's :139-149: crates of one metal, with a pile on the floor or on the stack.
                        for (int i = start[a]; i <= end[a]; i++) {
                            for (int j = start[b]; j <= end[b]; j++) {
                                if (data.next3in4()) {
                                    crate(data, i, 1, j);
                                    if (data.next2in3()) {
                                        crate(data, i, 2, j);
                                        if (data.next1in2()) {
                                            crate(data, i, 3, j);
                                            if (data.next1in3()) pile(data, i, 4, j);
                                        } else if (data.next1in3()) {
                                            pile(data, i, 3, j);
                                        }
                                    } else if (data.next1in3()) {
                                        pile(data, i, 2, j);
                                    }
                                } else if (data.next1in3()) {
                                    pile(data, i, 1, j);
                                }
                            }
                        }
                    }
                    case 1 -> {
                        // GT6's :150-157: crates of one dust.
                        for (int i = start[a]; i <= end[a]; i++) {
                            for (int j = start[b]; j <= end[b]; j++) {
                                if (data.next3in4()) {
                                    dustCrate(data, i, 1, j, data.next1in2());
                                    if (data.next2in3()) {
                                        dustCrate(data, i, 2, j, data.next1in3());
                                        if (data.next1in2()) dustCrate(data, i, 3, j, data.next1in4());
                                    }
                                }
                            }
                        }
                    }
                    case 2 -> {
                        // GT6's :158-165: crates of one gem.
                        for (int i = start[a]; i <= end[a]; i++) {
                            for (int j = start[b]; j <= end[b]; j++) {
                                if (data.next3in4()) {
                                    gemCrate(data, i, 1, j, data.next1in4());
                                    if (data.next2in3()) {
                                        gemCrate(data, i, 2, j, data.next1in6());
                                        if (data.next1in2()) gemCrate(data, i, 3, j, data.next1in8());
                                    }
                                }
                            }
                        }
                    }
                    case 3 -> {
                        // GT6's :166-173: crates of one wood (64x plate crates only, so no size roll).
                        for (int i = start[a]; i <= end[a]; i++) {
                            for (int j = start[b]; j <= end[b]; j++) {
                                if (data.next3in4()) {
                                    data.crate(i, 1, j, GTDungeonData.CrateForm.PLATE64, GTDungeonData.CRATE_WOODS);
                                    if (data.next2in3()) {
                                        data.crate(i, 2, j, GTDungeonData.CrateForm.PLATE64, GTDungeonData.CRATE_WOODS);
                                        if (data.next1in2()) {
                                            data.crate(i, 3, j, GTDungeonData.CrateForm.PLATE64, GTDungeonData.CRATE_WOODS);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    case 4 -> {
                        // GT6's :174-184: crates of one raw ore, topped by raw ore blocks.
                        for (int i = start[a]; i <= end[a]; i++) {
                            for (int j = start[b]; j <= end[b]; j++) {
                                if (data.next3in4()) {
                                    rawCrate(data, i, 1, j, data.next1in2());
                                    if (data.next2in3()) {
                                        rawCrate(data, i, 2, j, data.next1in3());
                                        if (data.next1in2()) {
                                            rawCrate(data, i, 3, j, data.next1in4());
                                            if (data.next1in4()) data.rawBlock(i, 4, j, GTDungeonData.CRATE_ORES);
                                        } else if (data.next1in4()) {
                                            data.rawBlock(i, 3, j, GTDungeonData.CRATE_ORES);
                                        }
                                    } else if (data.next1in4()) {
                                        data.rawBlock(i, 2, j, GTDungeonData.CRATE_ORES);
                                    }
                                } else if (data.next1in4()) {
                                    data.rawBlock(i, 1, j, GTDungeonData.CRATE_ORES);
                                }
                            }
                        }
                    }
                    // GT6's :185-195 (Hexorium crates of Hexxit) and :196-206 (infused crystals of
                    // Thaumcraft) are other-mod content, and GT6's default (:207) does nothing either:
                    // those quadrants stay empty, exactly like GT6 without the two mods installed.
                    default -> {
                        // Nothing.
                    }
                }
            }
        }

        return true;
    }

    /**
     * GT6's four fluid tank walls ({@code :65-132}). Each of the four blocks of GT6's code is the same
     * three statements, only the origin and the extent change, so the port keeps one copy:
     *
     * <pre>
     * if (next1in2()) {                       // GT6 :66 - half the walls stay bare rock
     *     int type = next(3);                 // GT6 :67 - one of the three containers of tIDs (:63)
     *     for i in 0..countI, j in 0..countJ:
     *         if (next1in2()) for k in 0..3: setTank(x, 1+k, z, type); if (next2in3()) break;
     *         else if (next1in2()) switch (next(3)) { propane | oxygen | helium gas cylinder }
     * }
     * </pre>
     *
     * <p>GT6's loop indices walk X and Z respectively in all four walls ({@code set(12+i, …, 6+j)} for
     * the +X wall, {@code set(6+i, …, 12+j)} for the +Z one), so the caller only passes the two origins
     * and the two extents - and the roll order (i outer, j inner) is GT6's.</p>
     */
    private static void fluidWall(GTDungeonData data, int baseX, int baseZ, int countI, int countJ) {
        if (!data.next1in2()) return;                                 // GT6 :66, :83, :100, :117
        int type = data.next(CONTAINERS.length);                       // GT6 :67 tIDs[tType]
        for (int i = 0; i < countI; i++) {
            for (int j = 0; j < countJ; j++) {
                int x = baseX + i, z = baseZ + j;
                if (data.next1in2()) {                                 // GT6 :69 - a column of tanks
                    for (int k = 0; k < 4; k++) {
                        data.tank(x, 1 + k, z, GTDungeonData.container(CONTAINERS[type]),
                                GTFluids.stack(data.select(FLUIDS[type], x, 1 + k, z), AMOUNTS[type]));
                        if (data.next2in3()) break;                    // GT6 :71
                    }
                } else if (data.next1in2()) {                          // GT6 :72 - one gas cylinder
                    switch (data.next(3)) {                            // GT6 :73
                        case 0 -> gasCylinder(data, x, z, false, PROPANE);   // GT6 :74, 32055
                        case 1 -> gasCylinder(data, x, z, true, OXYGEN);     // GT6 :75, 32056
                        default -> gasCylinder(data, x, z, true, HELIUM);    // GT6 :76, 32056
                    }
                }
            }
        }
    }

    /**
     * GT6's gas cylinder writes ({@code :74-76}): {@code 32055} Steel Barometer Gas Cylinder with
     * 8000 mB of propane, {@code 32056} Stainless Barometer Gas Cylinder with 8000 mB of oxygen or
     * helium. GT6 also paints them ({@code NBT_COLOR} red / light blue / yellow); the port's cylinder is
     * one block per material with no paint channel, like the workshop's own cylinders (documented there).
     */
    private static void gasCylinder(GTDungeonData data, int x, int z, boolean stainless, String fluid) {
        data.tank(x, 1, z, GTDungeonData.container(stainless
                        ? "fluid_barometer_gas_cylinder_stainless_steel"
                        : "fluid_barometer_gas_cylinder"),
                GTFluids.stack(fluid, 8000));
    }

    /** GT6's three tank families ({@code :58-63}): the container, its fluid list and its amount. */
    private static final String[] CONTAINERS = {"wood_barrel_treated", "wood_barrel_ironwood", "drum_bronze"};
    private static final int[] AMOUNTS = {16000, 32000, 64000};
    private static final String[][] FLUIDS = {
            // GT6 :59 - Wooden Barrel 32714, 16000 mB (GT6's FL.lube is the port's Lubricant).
            {"Oil_Creosote", "Oil_Seed", "Lubricant", "Glue", "Latex", "Water", "Purple_Drink"},
            // GT6 :60 - Ironwood Barrel 32734, 32000 mB; the water is holy water here.
            {"Oil_Creosote", "Oil_Seed", "Lubricant", "Glue", "Latex", "Holywater", "Purple_Drink"},
            // GT6 :61 - Bronze Drum 32102, 64000 mB; normal oil twice, then the five special oils.
            {"Oil_Normal", "Oil_Normal", "Oil_Soulsand", "Oil_Light", "Oil_Medium", "Oil_Heavy",
                    "Oil_ExtraHeavy"}};

    private static final String PROPANE = "Propane";
    private static final String OXYGEN = "Oxygen";
    private static final String HELIUM = "Helium";

    /**
     * GT6's metal quadrant crate: {@code aData.set(tMetalCrates, i, y, j, sMetals)} ({@code :141-143}),
     * one of GT6's twelve crate entries of {@code :135} for one metal of its {@code sMetals}.
     */
    private static boolean crate(GTDungeonData data, int x, int y, int z) {
        return data.crate(x, y, z, METAL_CRATES, GTDungeonData.CRATE_METALS);
    }

    /**
     * GT6's {@code aData.next1inX() ? BlocksGT.crateGt64<Form> : BlocksGT.crateGt<Form>} pairs
     * ({@code :152-154}, {@code :160-162}, {@code :176-178}).
     *
     * <p>The caller draws the size roll before choosing either the partial or full prefix, preserving
     * GT6's random stream order.</p>
     */
    private static boolean dustCrate(GTDungeonData data, int x, int y, int z, boolean large) {
        return data.crate(x, y, z, large ? GTDungeonData.CrateForm.DUST64 : GTDungeonData.CrateForm.DUST,
                GTDungeonData.CRATE_DUSTS);
    }

    /** GT6's gem crates ({@code :160-162}); see {@link #dustCrate} for the size roll. */
    private static boolean gemCrate(GTDungeonData data, int x, int y, int z, boolean large) {
        return data.crate(x, y, z, large ? GTDungeonData.CrateForm.GEM64 : GTDungeonData.CrateForm.GEM,
                GTDungeonData.CRATE_GEMS);
    }

    /** GT6's raw ore crates ({@code :176-178}); see {@link #dustCrate} for the size roll. */
    private static boolean rawCrate(GTDungeonData data, int x, int y, int z, boolean large) {
        return data.crate(x, y, z, large ? GTDungeonData.CrateForm.RAW64 : GTDungeonData.CrateForm.RAW,
                GTDungeonData.CRATE_ORES);
    }

    /**
     * GT6's {@code tMetalCrates} ({@code :135}): dust twice, ingot four times, plate twice, then the 64
     * sized dust, plate and ingot twice. GT6's {@code set(IPrefixBlock[], ...)} draws one entry at
     * random, so the multiplicities are the odds - and because the port has one block per form, the
     * twelve entries keep their distinct sizes and GT6's odds (dust 3/12, ingot 6/12, plate 3/12).
     */
    private static final GTDungeonData.CrateForm[] METAL_CRATES = {
            GTDungeonData.CrateForm.DUST, GTDungeonData.CrateForm.DUST,
            GTDungeonData.CrateForm.INGOT, GTDungeonData.CrateForm.INGOT,
            GTDungeonData.CrateForm.INGOT, GTDungeonData.CrateForm.INGOT,
            GTDungeonData.CrateForm.PLATE, GTDungeonData.CrateForm.PLATE,
            GTDungeonData.CrateForm.DUST64, GTDungeonData.CrateForm.PLATE64,
            GTDungeonData.CrateForm.INGOT64, GTDungeonData.CrateForm.INGOT64};

    /**
     * GT6's {@code ingots_or_plates} ({@code DungeonData:259-264}): a loose pile of ingots or plates.
     * GT6 rolls the ingot's material and stack size, the plate's material and stack size and then picks
     * plate or ingot with {@code next1in2()}; the port's piles take GT6's own metal list
     * ({@link GTDungeonData#CRATE_METALS}, the same {@code sMetals} GT6 passes here) and roll the size
     * inside {@link GTDungeonData#pile}.
     */
    private static boolean pile(GTDungeonData data, int x, int y, int z) {
        return data.ingotsOrPlates(x, y, z, 0, materialNames(GTDungeonData.CRATE_METALS));
    }

    /** The port material names of a GT6 material list, for the helpers that take strings. */
    private static String[] materialNames(com.gregtech.gregtech.api.material.GTMaterial[] materials) {
        String[] names = new String[materials.length];
        for (int i = 0; i < materials.length; i++) names[i] = materials[i].getName();
        return names;
    }
}
