package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.stone.GTStoneSlabBlock;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkBarracks;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import java.util.Arrays;
import java.util.HashSet;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Actual barracks generation; no independent reproduction of the placement algorithm. */
@GameTestHolder("gt6ce_barracks_door")
@PrefixGameTestTemplate(false)
public final class BarracksDoorAlignmentTests {
    @GameTest(template = "barracks", timeoutTicks = 100)
    public static void generatedDoorsJoinSlabWallsAndSurviveUpdates(GameTestHelper h) {
        BlockPos origin = h.absolutePos(new BlockPos(2, 3, 2));
        var owned = new HashSet<BlockPos>();
        h.forEveryBlockInStructure(p -> owned.add(h.absolutePos(p).immutable()));
        // The 20x14x20 template encloses the whole 16x9x16 room and update halo.
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-1, -1, -1), origin.offset(16, 10, 16))) {
            h.assertTrue(owned.contains(p),
                    "room and neighbor halo inside test bounds");
            h.assertTrue(h.getLevel().hasChunkAt(p), "fixture chunks already loaded before generation");
        }
        // Stop inherited Pillar before it writes any blocks below this owned fixture.
        h.getLevel().setBlock(origin.offset(6, -1, 6), Blocks.BEDROCK.defaultBlockState(),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        for (int seed = 1; seed <= 2; seed++) {
            ItemStack[] keys = new ItemStack[GTDungeonFeature.KEY_COUNT];
            Arrays.fill(keys, ItemStack.EMPTY);
            var data = new GTDungeonData(h.getLevel(), origin.getX(), origin.getY(), origin.getZ(),
                    StoneType.LIMESTONE, StoneType.SLATE, 3, new byte[5][5], 2, 2, 0,
                    new long[keys.length], keys, new boolean[keys.length],
                    new HashSet<BlockPos>(), new HashSet<String>(), RandomSource.create(seed));
            h.assertTrue(new GTDungeonChunkBarracks().generate(data), "actual barracks generated");
            check(h, origin, false);
        }
        for (int x : new int[]{3, 12}) for (int z : new int[]{5, 10}) for (int y : new int[]{1, 2}) {
            BlockPos p = origin.offset(x, y, z);
            h.getLevel().updateNeighborsAt(p, Blocks.IRON_DOOR);
        }
        h.runAfterDelay(2, () -> {
            check(h, origin, false);
            toggle(h, origin, true);
            check(h, origin, true);
            toggle(h, origin, false);
            check(h, origin, false);
            h.succeed();
        });
    }

    private static void toggle(GameTestHelper h, BlockPos origin, boolean open) {
        for (int x : new int[]{3, 12}) for (int z : new int[]{5, 10}) {
            BlockPos p = origin.offset(x, 1, z);
            ((DoorBlock) Blocks.IRON_DOOR).setOpen(null, h.getLevel(), h.getLevel().getBlockState(p), p, open);
        }
    }

    private static void check(GameTestHelper h, BlockPos origin, boolean open) {
        for (int x : new int[]{3, 12}) for (int z : new int[]{5, 10}) for (int y : new int[]{1, 2}) {
            BlockPos p = origin.offset(x, y, z);
            var state = h.getLevel().getBlockState(p);
            String context = "door " + x + "," + y + "," + z + " open=" + open;
            h.assertTrue(state.is(Blocks.IRON_DOOR), context + " exists");
            boolean left = (x == 3) == (z == 5);
            h.assertTrue(state.getValue(DoorBlock.HINGE) == (left ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT), context + " legacy hinge");
            h.assertTrue(state.getValue(DoorBlock.HALF) == (y == 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER), context + " half");
            h.assertTrue(state.getValue(DoorBlock.OPEN) == open && !state.getValue(DoorBlock.POWERED), context + " open/powered");
            var door = state.getCollisionShape(h.getLevel(), p).bounds();
            BlockPos wallPos = p.offset(x == 3 ? -1 : 1, 0, 0);
            var wallState = h.getLevel().getBlockState(wallPos);
            h.assertTrue(wallState.getBlock() instanceof GTStoneSlabBlock, context + " adjacent half-slab wall");
            var wall = wallState.getCollisionShape(h.getLevel(), wallPos).bounds();
            if (!open) {
                h.assertTrue(door.minX == 0 && door.maxX == 1 && door.minY == 0 && door.maxY == 1,
                        context + " full closed width and height");
                h.assertTrue(door.minZ >= wall.minZ && door.maxZ <= wall.maxZ,
                        context + " collision plane overlaps adjacent slab, no 5/16 gap");
                h.assertTrue(door.minZ == (z == 5 ? 0 : 13.0 / 16) && door.maxZ == (z == 5 ? 3.0 / 16 : 1),
                        context + " correct closed plane");
                h.assertTrue(state.getValue(DoorBlock.FACING) == (z == 5 ? Direction.SOUTH : Direction.NORTH), context + " facing");
            } else {
                h.assertTrue(door.minZ == 0 && door.maxZ == 1 && door.maxX - door.minX == 3.0 / 16,
                        context + " open plane perpendicular");
                h.assertTrue(door.minX == (x == 3 ? 13.0 / 16 : 0), context + " mirrored hinge swings to correct side");
            }
        }
    }
}
