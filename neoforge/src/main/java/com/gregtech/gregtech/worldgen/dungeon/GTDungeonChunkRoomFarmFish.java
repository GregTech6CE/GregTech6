package com.gregtech.gregtech.worldgen.dungeon;

import com.gregtech.gregtech.registry.*;
import net.minecraft.world.level.block.Blocks;

/**
 * Port of GT6's {@code DungeonChunkRoomFarmFish}
 * ({@code gregapi/worldgen/dungeon/DungeonChunkRoomFarmFish.java:38-82}): GT6's fish farm.
 *
 * <p>The empty room is built first (GT6's {@code :41} calls its super class and refuses the cell when that
 * fails), then a concrete basin is poured into the middle of the floor: the ring at local 3 and 12 keeps
 * the room's floor and is the rim of the pond, and everything inside it is dug out - bricks at local -3,
 * coloured concrete at -2 and two layers of water at -1 and 0. Every second water block carries one of
 * GT6's glowing Glowtus plants at local 1. Four chests with GT6's {@code BONUS_CHEST} category stand in
 * the corners of the hall, each with a 1-in-2 chance.</p>
 *
 * <h2>Port differences</h2>
 * <ul>
 *   <li><b>Concrete and water.</b> GT6's rim and basin walls are its coloured concrete
 *       ({@code BlocksGT.Concrete} meta {@code mColor}); the port's {@link GTDungeonData#colored} keeps
 *       that, and the water becomes plain 1.20.1 {@code Blocks.WATER} (GT6's {@code Blocks.water} meta 0
 *       was water as well, written with flag 2 - the port's {@code set} always uses flag 2).</li>
 *   <li><b>Glowtus.</b> GT6 puts its glowing Glowtus plant (a coloured light source, {@code :54}) on every
 *       second water block. The port uses its sixteen waterlily blocks in GT6's metadata order.</li>
 *   <li><b>Fish traps.</b> GT6's four HarvestCraft fish traps and the bait stacks it writes into their
 *       inventories ({@code :58-75}) are other-mod content the port does not have, so the whole block is
 *       skipped (the four air pockets above them are only needed by the traps).</li>
 *   <li><b>Chests.</b> GT6 places four chests (its multi-tile {@code 508 + next(3)}, one of its wooden
 *       chests) with the vanilla {@code ChestGenHooks.BONUS_CHEST} category in the corners ({@code :77-80}),
 *       each after a 1-in-2 roll. The port uses the corresponding reinforced wooden chest,
 *       preserves the material roll and corner facing, and maps the loot category to
 *       {@code chests/spawn_bonus_chest}.</li>
 *   <li><b>Refusal.</b> GT6's fish farm declines a cell only when the dungeon already has one
 *       ({@code WorldgenDungeonGT.TAG_FARM_FISH}) or when the empty room underneath failed
 *       ({@code :41}) - it does <em>not</em> look for water anywhere, it brings its own (the basin at
 *       local {@code -1..0}). The port keeps exactly those two conditions, so the dispatcher's candidate
 *       order and the room's probability stay GT6's; a water precondition would change which rooms a
 *       dungeon builds. {@link GTDungeonData#water} is therefore unused here.</li>
 *   <li><b>Rolls.</b> GT6's Glowtus presence and colour rolls and the four {@code next1in2} chest rolls
 *       are kept in order.</li>
 * </ul>
 */
public class GTDungeonChunkRoomFarmFish extends GTDungeonChunkRoomEmpty {

    /** GT6's tag ({@code WorldgenDungeonGT.TAG_FARM_FISH}, {@code WorldgenDungeonGT:82}). */
    public static final String TAG_FARM_FISH = "gt.dungeon.farm.fish";

    /** GT6's chest category ({@code ChestGenHooks.BONUS_CHEST}, {@code :77-80}) as a 1.20.1 table. */
    private static final String CHEST_LOOT = "chests/spawn_bonus_chest";

    @Override
    public boolean generate(GTDungeonData data) {
        // GT6's :41.
        if (data.hasTag(TAG_FARM_FISH) || !super.generate(data)) return false;
        data.tags.add(TAG_FARM_FISH);

        // GT6's :44-56 - the pond: its rim at local 3 and 12, and the dug out middle.
        for (int tX = 3; tX <= 12; tX++) {
            for (int tZ = 3; tZ <= 12; tZ++) {
                if (tX == 3 || tX == 12 || tZ == 3 || tZ == 12) {
                    // GT6's :46-48 - the rim: the room's floor and one block below it stay concrete.
                    data.colored(tX, 0, tZ);
                    data.colored(tX, -1, tZ);
                    data.bricks(tX, -2, tZ);
                } else {
                    // GT6's :50-54 - the basin: bricks at the bottom, concrete above them, water on top.
                    data.bricks(tX, -3, tZ);
                    data.colored(tX, -2, tZ);
                    data.set(tX, 0, tZ, Blocks.WATER.defaultBlockState());
                    data.set(tX, -1, tZ, Blocks.WATER.defaultBlockState());
                    // GT6 :54 - every other water column receives a colour-rolled Glowtus.
                    if (data.next1in2()) data.glowtus(tX, 1, tZ);
                }
            }
        }

        // GT6's :58-75 - the four HarvestCraft fish traps with their bait are other-mod content and are
        // skipped, together with the four air pockets above them.

        // GT6's :77-80 - the four chests in the corners of the hall, each with a 1-in-2 chance.
        if (data.next1in2()) data.fishFarmChest(1, 1, 1, CHEST_LOOT, net.minecraft.core.Direction.SOUTH);
        if (data.next1in2()) data.fishFarmChest(14, 1, 1, CHEST_LOOT, net.minecraft.core.Direction.WEST);
        if (data.next1in2()) data.fishFarmChest(1, 1, 14, CHEST_LOOT, net.minecraft.core.Direction.EAST);
        if (data.next1in2()) data.fishFarmChest(14, 1, 14, CHEST_LOOT, net.minecraft.core.Direction.NORTH);

        return true;
    }
}
