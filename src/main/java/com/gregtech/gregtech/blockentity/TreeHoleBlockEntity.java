package com.gregtech.gregtech.blockentity;

import com.gregtech.gregtech.block.wood.TreeHoleBlock;
import com.gregtech.gregtech.block.wood.WoodSpecies;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTWoods;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6's tree-hole refill logic ({@code MultiTileEntityResinHoleRubber.onTick2},
 * {@code MultiTileEntitySapHoleMaple.onTick2}, {@code MultiTileEntitySapHoleRainbowood.onTick2}).
 *
 * <p>Every 600 ticks (30 s) a still-empty hole walks up its own trunk, counts the leaves of the tree
 * at the exact positions GT6 lists for that species, and rolls
 * {@code rng(divisor) < leaves - threshold} — so only a healthy tree refills the hole. GT6's ideal
 * canopies give 86 leaves for the rubber tree, 306 for the maple and 271 for the rainbowood, i.e. a
 * 10% / 10% / 5% chance per check for a perfect tree.
 */
public class TreeHoleBlockEntity extends BlockEntity {
    /** GT6 checks every 600 ticks ({@code aTimer % 600 == 0}). */
    public static final int CHECK_INTERVAL = 600;

    private long timer;

    public TreeHoleBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.TREE_HOLE.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof TreeHoleBlock hole)) return;
        if (state.getValue(TreeHoleBlock.RESIN)) return;
        if (++timer % CHECK_INTERVAL != 0) return;
        refresh(hole.species());
    }

    /**
     * One GT6 refill check. Public so the GameTest can drive it without ticking 600 times, and so a
     * worldgen/automation hook could call it. Sets the block state's {@code resin} property when the
     * roll succeeds.
     */
    public boolean refresh(WoodSpecies species) {
        if (level == null) return false;
        int leaves = countTreeLeaves(level, worldPosition, species);
        int threshold = threshold(species);
        if (leaves <= threshold) return false;
        if (level.random.nextInt(divisor(species)) >= leaves - threshold) return false;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof TreeHoleBlock)) return false;
        level.setBlock(worldPosition, state.setValue(TreeHoleBlock.RESIN, true), 3);
        return true;
    }

    /** GT6 {@code tLeavesCount > …} gates: 60 rubber, 250 maple/rainbowood. */
    public static int threshold(WoodSpecies species) {
        return species == WoodSpecies.RUBBER ? 60 : 250;
    }

    /** GT6's roll divisors: {@code rng(260)} / {@code rng(560)} / {@code rng(420)}. */
    public static int divisor(WoodSpecies species) {
        return switch (species) {
            case RUBBER -> 260;
            case MAPLE -> 560;
            case RAINBOWOOD -> 420;
            default -> 1; // no GT6 hole exists for the other species
        };
    }

    /**
     * GT6's per-species leaf counting: the hole walks up its trunk to the tree top and then checks
     * the leaves block of the canopy positions the sapling shape placed. The three GT6 loops are
     * ported one by one because they differ in shape (the rubber tree has a 3-block cap, the maple a
     * wide layered canopy and the rainbowood a cinnamon-style canopy).
     *
     * @return the number of leaves of that tree, as GT6 counts them
     */
    public static int countTreeLeaves(Level level, BlockPos pos, WoodSpecies species) {
        // A periodic tree scan must not synchronously load adjacent chunks at the canopy edge.
        int radius = species == WoodSpecies.RUBBER ? 2 : 3;
        if (!level.hasChunksAt(pos.offset(-radius, 0, -radius), pos.offset(radius, 0, radius))) return 0;
        Block log = GTWoods.log(species);
        Block leaves = GTWoods.leaves(species);
        // GT6: `tTreeHeight = yCoord + 1` plus one per log found above (10 for rubber, 12 for the saps).
        int top = pos.getY() + 1;
        int limit = species == WoodSpecies.RUBBER ? 10 : 12;
        for (int i = 1; i < limit; i++) {
            if (!level.getBlockState(pos.above(i)).is(log)) break;
            top++;
        }
        int count = 0;
        if (species == WoodSpecies.RUBBER) {
            if (isLeaves(level, pos, 0, top, 0, leaves)) count++;
            if (isLeaves(level, pos, 0, top + 1, 0, leaves)) count++;
            for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                if (isLeaves(level, pos, i, top - 1, j, leaves)) count++;
            }
            for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
                if (i == 0 && j == 0) continue;
                if (Math.abs(i * j) < 2 && isLeaves(level, pos, i, top - 2, j, leaves)) count++;
                if (Math.abs(i * j) < 4) {
                    if (isLeaves(level, pos, i, top - 3, j, leaves)) count++;
                    if (isLeaves(level, pos, i, top - 4, j, leaves)) count++;
                }
                if (isLeaves(level, pos, i, top - 5, j, leaves)) count++;
            }
        } else if (species == WoodSpecies.MAPLE) {
            for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) {
                if (isLeaves(level, pos, i, top + 1, j, leaves)) count++;
            }
            for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
                if (isLeaves(level, pos, i, top, j, leaves)) count++;
                if (i == 0 && j == 0) continue;
                if (Math.abs(i * j) < 4 && isLeaves(level, pos, i, top - 7, j, leaves)) count++;
                if (isLeaves(level, pos, i, top - 1, j, leaves)) count++;
            }
            for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++) {
                if (i == 0 && j == 0) continue;
                if (Math.abs(i * j) < 9) {
                    if (isLeaves(level, pos, i, top - 2, j, leaves)) count++;
                    if (isLeaves(level, pos, i, top - 3, j, leaves)) count++;
                    if (isLeaves(level, pos, i, top - 6, j, leaves)) count++;
                }
                if (isLeaves(level, pos, i, top - 4, j, leaves)) count++;
                if (isLeaves(level, pos, i, top - 5, j, leaves)) count++;
            }
        } else if (species == WoodSpecies.RAINBOWOOD) {
            for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
                if (isLeaves(level, pos, i, top + 2, j, leaves)) count++;
                if ((i != 0 || j != 0) && isLeaves(level, pos, i, top - 4, j, leaves)) count++;
            }
            for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++) {
                if (Math.abs(i * j) >= 9) continue;
                if (i != 0 || j != 0) {
                    if (isLeaves(level, pos, i, top - 1, j, leaves)) count++;
                    if (isLeaves(level, pos, i, top - 2, j, leaves)) count++;
                    if (isLeaves(level, pos, i, top - 3, j, leaves)) count++;
                }
                if (isLeaves(level, pos, i, top, j, leaves)) count++;
                if (isLeaves(level, pos, i, top + 1, j, leaves)) count++;
            }
        }
        return count;
    }

    private static boolean isLeaves(Level level, BlockPos hole, int dx, int y, int dz, Block leaves) {
        return level.getBlockState(new BlockPos(hole.getX() + dx, y, hole.getZ() + dz)).is(leaves);
    }

    /** Test/tool helper: how many leaves the tree at {@code pos} has, mapped into GT6's gates. */
    public static boolean canRefill(Level level, BlockPos pos, WoodSpecies species, RandomSource random) {
        int leaves = countTreeLeaves(level, pos, species);
        return leaves > threshold(species) && random.nextInt(divisor(species)) < leaves - threshold(species);
    }
}
