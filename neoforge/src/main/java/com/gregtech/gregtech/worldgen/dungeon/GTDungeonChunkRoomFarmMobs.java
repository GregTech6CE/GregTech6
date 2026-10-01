package com.gregtech.gregtech.worldgen.dungeon;


import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.block.machine.ItemPipeBlock;
import com.gregtech.gregtech.block.misc.SpikeBlock;
import com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity;
import com.gregtech.gregtech.data.MaterialPrefix;

import com.gregtech.gregtech.registry.GTItems;


import com.gregtech.gregtech.data.generated.GT6Materials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Port of GT6's {@code DungeonChunkRoomFarmMobs}
 * ({@code gregapi/worldgen/dungeon/DungeonChunkRoomFarmMobs.java:36-205}): GT6's mob farm.
 *
 * <p>The empty room is built first (GT6's {@code :37} calls its super class and refuses the cell when that
 * fails), then a 4x4 stone pillar grows out of the middle of the hall. Two rings of containers hang around
 * it - a mass storage or the compartment drawer on every edge, the farm's mob drops inside them - and
 * twelve lamps light the floor. Above the pillar, GT6's farm itself rises: a 16x16 brick shaft from local 6
 * up to local 43, two blocks of tiles as its roof, mossy cobblestone spawn platforms every three blocks,
 * four steel omni-spikes on the floor with steel hoppers below them and one water source in each corner
 * that washes the drops onto the spikes.</p>
 *
 * <h2>Port differences</h2>
 * <ul>
 *   <li><b>The large set-up.</b> GT6's {@code makePlatForms} builds up to eight further platforms in
 *       neighbouring empty/corridor cells. {@link #footprint(byte[][], int, int)} reproduces the four
 *       three-cell space tests. {@code GTDungeonFeature.generateCell} lets each target cell build its own
 *       platform and {@link #addPipeFragments} clips the connecting pipe runs to that target cell, so
 *       generation order cannot make a neighbouring chunk overwrite the farm.</li>
 *   <li><b>Height.</b> GT6's shaft reaches local 43, i.e. through the ceiling of
 *       {@link GTDungeonChunkRoomEmpty}'s hall (local 7 and 8), which is GT6's own design. It stays inside
 *       the cell's own chunk column, so it never writes into a neighbouring cell; the pillar at local 1..8,
 *       the two container rings at 2 and 3, the lamps at 6 and the hoppers at 8 are inside the hall.</li>
 *   <li><b>No entities.</b> The port must not spawn entities from world generation. GT6's class does not
 *       either - it builds the dark shaft whose spawn platforms the normal mob spawning rules fill, as its
 *       comments "Platforms for Mobs to spawn on" ({@code :62} and {@code :148}) say - and it places no
 *       monster spawner, so no vanilla {@code Blocks.SPAWNER} is added here; that would be content GT6
 *       does not have. There is also no cage or pen of fences, glass or hay in GT6's room to build.</li>
 *   <li><b>Steel spikes.</b> GT6 places four omni-spikes with one shared blue/red steel roll
 *       ({@code next1in2() ? 6 : 14}, {@code :155-159}). The port uses its two-material spike state
 *       and its contact damage.</li>
 *   <li><b>Steel hoppers.</b> GT6's hopper multi-tile {@code 8010} (its comment calls them "Steel
 *       Hoppers!", {@code :161-165}) becomes the port's own steel hopper
 *       ({@code DungeonBindings.supplier("hopper_steel")}) with GT6's output face per hopper ({@code SIDE_X_POS} is east
 *       and {@code SIDE_Z_POS} south, {@code :162-165}).</li>
 *   <li><b>Item pipes.</b> GT6's multi-tile {@code 25377} is the medium Arsenic Bronze pipe. The port
 *       places its real item pipes with GT6's connection masks at the vertical shaft and both storage
 *       rings. Dungeon paint is still missing.</li>
 *   <li><b>Mass storages and the drawer.</b> Eight mass storages of multi-tile {@code 6009} - GT6's
 *       material variants, one stack of a mob drop each: glass bottles, slime balls, string, redstone,
 *       spider eyes, glowstone dust, bones, sticks, feathers, gunpowder, rotten flesh, sugar, arrows and
 *       one of GT6's wooden arrows, each {@code 1 + next(8)} ({@code :92-121}) - plus one compartment
 *       drawer of multi-tile {@code 4009} ({@code :112}) and one smooth block ({@code :109}) form the two
 *       rings. The port uses its own mass storage ({@code GTStorage.MASS_STORAGE}) and quad drawer
 *       with GT6's facing and fills all fourteen bulk stores with their 1..8 starting items. The
 *       original material-less wood arrow is represented by a GT wood arrow.</li>
 *   <li><b>Rolls.</b> The spike-colour roll and all fourteen initial stack-size rolls now follow GT6's
 *       call order. GT6's rock helpers roll a
 *       random brick variant per block, which the port's {@link GTDungeonData} already replaces with a
 *       deterministic variant (see its javadoc), and every other difference above is a skipped or
 *       substituted block.</li>
 * </ul>
 */
public class GTDungeonChunkRoomFarmMobs extends GTDungeonChunkRoomEmpty {

    /** GT6's tag ({@code WorldgenDungeonGT.TAG_FARM_MOBS}, {@code WorldgenDungeonGT:80}). */
    public static final String TAG_FARM_MOBS = "gt.dungeon.farm.mobs";

    /** GT6's {@code tPlatforms} ({@code :149}): the lanes that carry a spawn platform, of the 16 lanes. */
    private static final boolean[] PLATFORMS = {
            false, true, true, false, false, false, true, true,
            true, true, false, false, false, true, true, false};

    /** The GT6 3×3 mob-farm footprint, indexed by dungeon-grid cell rather than absolute blocks. */
    public static final class Footprint {
        private final int farmI, farmJ;
        private final boolean[][] cells;

        private Footprint(int farmI, int farmJ, boolean[][] cells) {
            this.farmI = farmI;
            this.farmJ = farmJ;
            this.cells = cells;
        }

        public boolean includes(int i, int j) {
            int dx = i - farmI, dz = j - farmJ;
            return dx >= -1 && dx <= 1 && dz >= -1 && dz <= 1 && cells[dx + 1][dz + 1];
        }

        public boolean isOuter(int i, int j) {
            return includes(i, j) && (i != farmI || j != farmJ);
        }

        public int farmI() { return farmI; }
        public int farmJ() { return farmJ; }
    }

    /** Find the uniquely reserved mob farm without reading or forcing any other chunk. */
    public static Footprint footprint(byte[][] layout, GTDungeonSharedState plan) {
        for (int i = 1; i < layout.length - 1; i++) {
            for (int j = 1; j < layout[i].length - 1; j++) {
                if (layout[i][j] > 0 && plan.cell(i, j).chosenRoom() instanceof GTDungeonChunkRoomFarmMobs)
                    return footprint(layout, i, j);
            }
        }
        return null;
    }

    /** GT6 {@code tXNZN/tXPZN/tXNZP/tXPZP}: all three cells around a quadrant must be 0 or -128. */
    public static Footprint footprint(byte[][] layout, int i, int j) {
        boolean nw = free(layout, i - 1, j - 1) && free(layout, i - 1, j) && free(layout, i, j - 1);
        boolean ne = free(layout, i + 1, j - 1) && free(layout, i + 1, j) && free(layout, i, j - 1);
        boolean sw = free(layout, i - 1, j + 1) && free(layout, i - 1, j) && free(layout, i, j + 1);
        boolean se = free(layout, i + 1, j + 1) && free(layout, i + 1, j) && free(layout, i, j + 1);
        boolean[][] cells = new boolean[3][3];
        cells[1][1] = true;
        cells[0][0] = nw;
        cells[2][0] = ne;
        cells[0][2] = sw;
        cells[2][2] = se;
        cells[0][1] = nw || sw;
        cells[2][1] = ne || se;
        cells[1][0] = nw || ne;
        cells[1][2] = sw || se;
        return new Footprint(i, j, cells);
    }

    private static boolean free(byte[][] layout, int i, int j) {
        return i >= 0 && i < layout.length && j >= 0 && j < layout[i].length
                && (layout[i][j] == GTDungeonLayout.EMPTY || layout[i][j] == GTDungeonLayout.CORRIDOR);
    }

    /** Builds an outer platform in its own target chunk, with local coordinates 0..15 only. */
    public static void generateOuterPlatform(GTDungeonData data) {
        makePlatform(data, 0, 0);
    }

    /**
     * The original pipe runs extend 7 blocks toward another platform. Each target chunk applies only
     * the fragments it owns, after its platform/floor is built, so reverse chunk order gives the same
     * final network. GT6's Arsenic Bronze pipe connection masks are retained.
     */
    public static void addPipeFragments(GTDungeonData data, Footprint footprint, int targetI, int targetJ) {
        if (footprint == null || !footprint.includes(targetI, targetJ)) return;
        int targetX = (targetI - footprint.farmI) * 16;
        int targetZ = (targetJ - footprint.farmJ) * 16;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx == 0 && dz == 0) || !footprint.cells[dx + 1][dz + 1]) continue;
                int px = dx * 16, pz = dz * 16;
                if (dx > 0) horizontalFragment(data, px - 7, px + 7, pz + 8, targetX, targetZ);
                if (dx < 0) horizontalFragment(data, px + 9, px + 23, pz + 8, targetX, targetZ);
                if (dz > 0) verticalFragment(data, pz - 7, pz + 7, px + 8, targetX, targetZ);
                if (dz < 0) verticalFragment(data, pz + 9, pz + 23, px + 8, targetX, targetZ);
            }
        }
    }

    private static void horizontalFragment(GTDungeonData data, int startX, int endX, int z,
                                           int targetX, int targetZ) {
        if (z < targetZ || z >= targetZ + 16) return;
        for (int x = Math.max(startX, targetX); x <= Math.min(endX, targetX + 15); x++)
            pipe(data, x - targetX, 7, z - targetZ, 48); // SBIT_W | SBIT_E
    }

    private static void verticalFragment(GTDungeonData data, int startZ, int endZ, int x,
                                         int targetX, int targetZ) {
        if (x < targetX || x >= targetX + 16) return;
        for (int z = Math.max(startZ, targetZ); z <= Math.min(endZ, targetZ + 15); z++)
            pipe(data, x - targetX, 7, z - targetZ, 12); // SBIT_N | SBIT_S
    }

    @Override
    public boolean generate(GTDungeonData data) {
        // GT6's :37.
        if (data.hasTag(TAG_FARM_MOBS) || !super.generate(data)) return false;
        data.tags.add(TAG_FARM_MOBS);

        // GT6's :59-60 - the solid pillar in the middle of the hall, carved away by the platforms later.
        for (int tY = 1; tY <= 6; tY++) {
            for (int tX = 6; tX <= 9; tX++) {
                for (int tZ = 6; tZ <= 9; tZ++) data.bricks(tX, tY, tZ);
            }
        }
        for (int tY = 7; tY <= 8; tY++) {
            for (int tX = 5; tX <= 10; tX++) {
                for (int tZ = 5; tZ <= 10; tZ++) data.smalltiles(tX, tY, tZ);
            }
        }

        // GT6's :63-71 - this cell's centre platform. Target cells build the outer platforms from
        // the same footprint and never write beyond their own chunk.
        makePlatform(data, 0, 0);

        // GT6's :74-85 - the twelve lamps of the floor, GT6's 3/6/9/12 grid with a redstone brick above.
        data.lamp(3, 6, 3, +1);
        data.lamp(3, 6, 6, +1);
        data.lamp(3, 6, 9, +1);
        data.lamp(3, 6, 12, +1);
        data.lamp(6, 6, 3, +1);
        data.lamp(9, 6, 3, +1);
        data.lamp(6, 6, 12, +1);
        data.lamp(9, 6, 12, +1);
        data.lamp(12, 6, 3, +1);
        data.lamp(12, 6, 6, +1);
        data.lamp(12, 6, 9, +1);
        data.lamp(12, 6, 12, +1);

        // GT6 :87-90 - its ID 25377 is the medium Arsenic Bronze item pipe (25375 + 2).
        pipe(data, 8, 6, 8, 3);
        pipe(data, 8, 5, 8, 3);
        pipe(data, 8, 4, 8, 3);

        // GT6's :91-106 - the first ring of containers at local 3: eight mass storages (glass bottles,
        // slime balls, string, redstone, spider eyes, glowstone dust, bones and sticks in GT6) around the
        // pillar, the four chiselled corners and the four inner item pipes.
        data.chiseled(6, 3, 6);
        stored(data, 6, 3, 7, Direction.WEST, Items.GLASS_BOTTLE);
        stored(data, 6, 3, 8, Direction.WEST, Items.SLIME_BALL);
        data.chiseled(6, 3, 9);
        stored(data, 7, 3, 6, Direction.NORTH, Items.STRING);
        pipe(data, 7, 3, 7, 60);
        pipe(data, 7, 3, 8, 60);
        stored(data, 7, 3, 9, Direction.SOUTH, Items.REDSTONE);
        stored(data, 8, 3, 6, Direction.NORTH, Items.SPIDER_EYE);
        pipe(data, 8, 3, 7, 60);
        pipe(data, 8, 3, 8, 63);
        stored(data, 8, 3, 9, Direction.SOUTH, Items.GLOWSTONE_DUST);
        data.chiseled(9, 3, 6);
        stored(data, 9, 3, 7, Direction.EAST, Items.BONE);
        stored(data, 9, 3, 8, Direction.EAST, Items.STICK);
        data.chiseled(9, 3, 9);

        // GT6's :108-123 - the second ring at local 2: the compartment drawer at (7, 2, 6), six further
        // mass storages (feathers, gunpowder, rotten flesh, sugar, arrows and a wooden arrow in GT6), the
        // smooth block GT6 leaves in the pillar's corner and the four inner item pipes.
        data.chiseled(6, 2, 6);
        data.smooth(6, 2, 7);
        stored(data, 6, 2, 8, Direction.WEST, Items.FEATHER);
        data.chiseled(6, 2, 9);
        data.set(7, 2, 6, drawer(Direction.NORTH));
        pipe(data, 7, 2, 7, 60);
        pipe(data, 7, 2, 8, 60);
        stored(data, 7, 2, 9, Direction.SOUTH, Items.GUNPOWDER);
        stored(data, 8, 2, 6, Direction.NORTH, Items.ROTTEN_FLESH);
        pipe(data, 8, 2, 7, 60);
        pipe(data, 8, 2, 8, 62);
        stored(data, 8, 2, 9, Direction.SOUTH, Items.SUGAR);
        data.chiseled(9, 2, 6);
        stored(data, 9, 2, 7, Direction.EAST, Items.ARROW);
        int woodenArrowCount = 1 + data.next(8);
        stored(data, 9, 2, 8, Direction.EAST,
                GTItems.getStack(MaterialPrefix.arrowGtWood, GT6Materials.Woods.Wood, woodenArrowCount));
        data.chiseled(9, 2, 9);

        return true;
    }

    /**
     * GT6's {@code makePlatForms} ({@code :128-204}): one 16x16 platform of the farm - the two block thick
     * roof at local 42 and 43, the floor at 6 to 8, the brick walls of the shaft from 9 to 41, the spawn
     * platforms every three blocks ({@code :150}), the omni-spikes ({@code :154-159}), the steel hoppers
     * ({@code :161-165}) and the four water sources of the corners ({@code :181-201}). Called once for
     * the centre and once by each qualifying neighbouring cell, always with local offsets 0,0.
     */
    private static void makePlatform(GTDungeonData data, int aX, int aZ) {
        for (int eX = aX + 15, tX = aX; tX <= eX; tX++) {
            for (int eZ = aZ + 15, tZ = aZ; tZ <= eZ; tZ++) {
                // GT6's :131-137 - the roof, the floor and the outer wall of the shaft.
                data.tiles(tX, 43, tZ);
                data.smalltiles(tX, 42, tZ);
                data.smalltiles(tX, 8, tZ);
                data.tiles(tX, 7, tZ);
                data.tiles(tX, 6, tZ);
                for (int tY = 9; tY < 42; tY++) {
                    if (tX == aX || tX == eX || tZ == aZ || tZ == eZ) {
                        data.bricks(tX, tY, tZ);
                    } else {
                        data.air(tX, tY, tZ);
                    }
                }
            }
        }

        // GT6's :148-152 - the spawn platforms on GT6's tPlatforms lanes, every three blocks.
        for (int tY = 12; tY < 42; tY++) {
            if (tY % 3 == 0) {
                for (int i = 1; i <= 14; i++) {
                    for (int j = 1; j <= 14; j++) {
                        if (PLATFORMS[i] || PLATFORMS[j]) data.mossycobble(aX + i, tY, aZ + j);
                    }
                }
            }
        }

        // GT6 :154-159 - all four steel omni-spikes share the blue/red steel roll (meta 6/14).
        boolean redSteel = data.next1in2();
        for (int tX = 7; tX <= 8; tX++) {
            for (int tZ = 7; tZ <= 8; tZ++) {
                data.set(aX + tX, 9, aZ + tZ, DungeonBindings.block("spike_steel").defaultBlockState()
                        .setValue(SpikeBlock.SECONDARY, redSteel));
            }
        }

        // GT6's :161-165 - the four steel hoppers below the spikes, with GT6's output face each.
        data.set(aX + 7, 8, aZ + 7, hopper(Direction.EAST));
        data.set(aX + 7, 8, aZ + 8, hopper(Direction.EAST));
        data.set(aX + 8, 8, aZ + 7, hopper(Direction.SOUTH));
        data.set(aX + 8, 8, aZ + 8, hopper(Direction.DOWN));

        // GT6 :167-171 - three chiselled corners and the centre pipe. Cross-cell runs are clipped and
        // written by addPipeFragments after the target platform has been constructed.
        data.chiseled(aX + 7, 7, aZ + 7);
        data.chiseled(aX + 7, 7, aZ + 8);
        data.chiseled(aX + 8, 7, aZ + 7);
        pipe(data, aX + 8, 7, aZ + 8, 63);

        // GT6's :181-197 - the mossy cobblestone rims in the four corners of the platform, at local 9.
        data.mossycobble(aX + 1, 9, aZ + 1);
        data.mossycobble(aX + 2, 9, aZ + 1);
        data.mossycobble(aX + 3, 9, aZ + 1);
        data.mossycobble(aX + 4, 9, aZ + 1);
        data.mossycobble(aX + 1, 9, aZ + 2);
        data.mossycobble(aX + 2, 9, aZ + 2);
        data.mossycobble(aX + 3, 9, aZ + 2);
        data.mossycobble(aX + 1, 9, aZ + 3);
        data.mossycobble(aX + 2, 9, aZ + 3);
        data.mossycobble(aX + 1, 9, aZ + 4);

        data.mossycobble(aX + 14, 9, aZ + 1);
        data.mossycobble(aX + 13, 9, aZ + 1);
        data.mossycobble(aX + 12, 9, aZ + 1);
        data.mossycobble(aX + 11, 9, aZ + 1);
        data.mossycobble(aX + 14, 9, aZ + 2);
        data.mossycobble(aX + 13, 9, aZ + 2);
        data.mossycobble(aX + 12, 9, aZ + 2);
        data.mossycobble(aX + 14, 9, aZ + 3);
        data.mossycobble(aX + 13, 9, aZ + 3);
        data.mossycobble(aX + 14, 9, aZ + 4);

        data.mossycobble(aX + 1, 9, aZ + 14);
        data.mossycobble(aX + 2, 9, aZ + 14);
        data.mossycobble(aX + 3, 9, aZ + 14);
        data.mossycobble(aX + 4, 9, aZ + 14);
        data.mossycobble(aX + 1, 9, aZ + 13);
        data.mossycobble(aX + 2, 9, aZ + 13);
        data.mossycobble(aX + 3, 9, aZ + 13);
        data.mossycobble(aX + 1, 9, aZ + 12);
        data.mossycobble(aX + 2, 9, aZ + 12);
        data.mossycobble(aX + 1, 9, aZ + 11);

        data.mossycobble(aX + 14, 9, aZ + 14);
        data.mossycobble(aX + 13, 9, aZ + 14);
        data.mossycobble(aX + 12, 9, aZ + 14);
        data.mossycobble(aX + 11, 9, aZ + 14);
        data.mossycobble(aX + 14, 9, aZ + 13);
        data.mossycobble(aX + 13, 9, aZ + 13);
        data.mossycobble(aX + 12, 9, aZ + 13);
        data.mossycobble(aX + 14, 9, aZ + 12);
        data.mossycobble(aX + 13, 9, aZ + 12);
        data.mossycobble(aX + 14, 9, aZ + 11);

        // GT6's :198-201 - one water source per corner; GT6 passes flag 3, the port's set() uses flag 2.
        data.set(aX + 1, 10, aZ + 1, Blocks.WATER.defaultBlockState());
        data.set(aX + 1, 10, aZ + 14, Blocks.WATER.defaultBlockState());
        data.set(aX + 14, 10, aZ + 1, Blocks.WATER.defaultBlockState());
        data.set(aX + 14, 10, aZ + 14, Blocks.WATER.defaultBlockState());
    }

    /** GT6's mass storage (multi-tile {@code 6009}) as the port's mass storage, with GT6's facing. */
    private static BlockState storage(Direction facing) {
        return DungeonBindings.block("mass_storage").defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    /** GT6 multi-tile 25377: Arsenic Bronze medium pipe; mask uses Direction's DOWN..EAST bit order. */
    private static void pipe(GTDungeonData data, int x, int y, int z, int mask) {
        Block block = BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","item_pipe_medium_arsenic_bronze"));
        if (!(block instanceof ItemPipeBlock)) throw new IllegalStateException("Missing GT6 item pipe 25377");
        BlockState state = block.defaultBlockState();
        for (Direction side : Direction.values())
            state = state.setValue(ItemPipeBlock.propFor(side), (mask & (1 << side.ordinal())) != 0);
        data.set(x, y, z, state);
    }

    private static void stored(GTDungeonData data, int x, int y, int z, Direction facing, Item item) {
        stored(data, x, y, z, facing, new ItemStack(item, 1 + data.next(8)));
    }

    private static void stored(GTDungeonData data, int x, int y, int z, Direction facing, ItemStack stack) {
        if (!data.set(x, y, z, storage(facing))) return;
        if (data.level.getBlockEntity(new BlockPos(data.x + x, data.y + y, data.z + z))
                instanceof MassStorageBlockEntity storage) storage.insert(stack);
    }

    /** GT6's compartment drawer (multi-tile {@code 4009}) as the port's quad drawer, with GT6's facing. */
    private static BlockState drawer(Direction facing) {
        return DungeonBindings.drawer(com.gregtech.gregtech.content.material.Materials.Bronze).defaultBlockState()
                .setValue(com.gregtech.gregtech.block.inventory.DrawerQuadBlock.FACING, facing);
    }

    /** GT6's steel hopper (multi-tile {@code 8010}) as the port's steel hopper, with GT6's output face. */
    private static BlockState hopper(Direction facing) {
        return DungeonBindings.supplier("hopper_steel").get().defaultBlockState().setValue(DirectionalBlock.FACING, facing);
    }
}
