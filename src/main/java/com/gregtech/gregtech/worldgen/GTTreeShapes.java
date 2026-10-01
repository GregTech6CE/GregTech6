package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.block.wood.WoodSpecies;
import com.gregtech.gregtech.block.wood.WoodSaplingBlock;
import com.gregtech.gregtech.registry.GTWoods;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 tree shapes, ported 1:1 from {@code gregtech/blocks/tree/BlockTreeSaplingAB.grow}
 * (metas 0-7) and {@code BlockTreeSaplingCD.grow} (meta 0).
 *
 * <p>GT6's sapling blocks hold the whole tree generator: {@code grow()} switches on the sapling
 * metadata and places logs/leaves by hand (no {@code WorldGenerator} objects). This class keeps
 * that structure: one {@link Shape} per GT6 sapling meta, placeable in any {@link LevelAccessor}
 * so both sapling growth and worldgen can use it.
 *
 * <p>The shapes are literal ports: the trunk-height rolls ({@code aY + min + rand(max-min)}), the
 * {@code canPlaceTree} pre-checks that abort the whole tree, the leaf layer predicates
 * ({@code |i*j| < n}) and the blue spruce's podzol patch are all GT6's own numbers.
 *
 * <p>Documented deviations: GT6's rubber tree punches {@code MultiTileEntityResinHoleRubber}
 * resin holes into the trunk (the port has no resin-hole block yet, so the trunk is solid logs),
 * and GT6's saplings roll "max height" against the 1.7.10 world height.
 */
public final class GTTreeShapes {
    private GTTreeShapes() {}

    /** One GT6 sapling metadata value ({@code BlockTreeSaplingAB} cases 0-7, {@code BlockTreeSaplingCD} case 0). */
    public enum Shape {
        /** AB/0 — Rubber: 7-9 tall, 3x3 leaf cap, resin holes in GT6. */
        RUBBER(7),
        /** AB/1 — Maple: 9-11 tall, wide 7x7 canopy. */
        MAPLE(9),
        /** AB/2 — Willow: 5-7 tall, very wide flat 9x9 canopy. */
        WILLOW(5),
        /** AB/3 — Blue Mahoe: 4-5 tall, compact 5x5 canopy. */
        BLUE_MAHOE(4),
        /** AB/4 — Hazel: 3 tall trunk, dense round canopy (GT6's shortest tree). */
        HAZEL(3),
        /** AB/5 — Cinnamon: 6-8 tall, layered canopy. */
        CINNAMON(6),
        /** AB/6 — Coconut: 8-12 tall palm, fronds along the diagonals and the axes. */
        COCONUT(8),
        /** AB/7 — Rainbowood: 7-9 tall, same canopy family as cinnamon. */
        RAINBOWOOD(7),
        /** CD/0 — Blue Spruce: 14-16 tall conifer that turns the dirt below it into podzol. */
        BLUE_SPRUCE(16);

        /**
         * Free height above the sapling GT6 demands before growing — its
         * {@code getMaxHeight(level, pos, n) < n} gate. {@code getMaxHeight} only inspects offsets
         * {@code 1 .. n-1} and returns {@code n} when they are all replaceable, so the gate really
         * means "at least {@code n - 1} free blocks" for every shape.
         */
        private final int minHeight;

        Shape(int minHeight) { this.minHeight = minHeight; }

        public int minHeight() { return minHeight; }

        /**
         * Smallest trunk the shape can produce (logs above and including the sapling position).
         * GT6 rolls the trunk as {@code y + min + rand(extra)} for the {@code AB} shapes and as
         * {@code y + freeHeight - rand(3)} for the {@code CD} blue spruce, so only the spruce can
         * end up two blocks shorter than its gate.
         */
        public int minTrunk() { return this == BLUE_SPRUCE ? minHeight - 2 : minHeight; }
    }

    /**
     * Species to GT6 shape. The port also carries three wood-dictionary species (other mods' woods
     * in GT6, which GT6 never grows) — those borrow the closest GT6 shape so their saplings still
     * produce a real tree of their own species instead of a vanilla oak.
     */
    public static Shape shapeOf(WoodSpecies species) {
        return switch (species) {
            case RUBBER -> Shape.RUBBER;
            case MAPLE -> Shape.MAPLE;
            case WILLOW -> Shape.WILLOW;
            case BLUE_MAHOE -> Shape.BLUE_MAHOE;
            case HAZEL -> Shape.HAZEL;
            case CINNAMON -> Shape.CINNAMON;
            case COCONUT -> Shape.COCONUT;
            case RAINBOWOOD -> Shape.RAINBOWOOD;
            case BLUE_SPRUCE -> Shape.BLUE_SPRUCE;
            // Not GT6 tree species (GT6 gets these from Forestry/Binnie/HaC/Tropicraft via the wood
            // dictionary): PINE = conifer -> blue spruce, EBONY = broadleaf -> maple,
            // WHITE_MAHOE = tropical broadleaf -> blue mahoe.
            case PINE -> Shape.BLUE_SPRUCE;
            case EBONY -> Shape.MAPLE;
            case WHITE_MAHOE -> Shape.BLUE_MAHOE;
        };
    }

    /** Grows the species' GT6 tree at {@code pos} (the sapling position becomes the lowest log). */
    public static boolean grow(LevelAccessor level, BlockPos pos, WoodSpecies species, RandomSource random) {
        return grow(level, pos, species, shapeOf(species), random);
    }

    /** Grows one specific GT6 shape using the given species' log/leaf blocks. */
    public static boolean grow(LevelAccessor level, BlockPos pos, WoodSpecies species, Shape shape, RandomSource random) {
        if (level.isOutsideBuildHeight(pos)) return false;
        return switch (shape) {
            case RUBBER -> rubber(level, pos, species, random);
            case MAPLE -> maple(level, pos, species, random);
            case WILLOW -> willow(level, pos, species, random);
            case BLUE_MAHOE -> blueMahoe(level, pos, species, random);
            case HAZEL -> hazel(level, pos, species, random);
            case CINNAMON -> cinnamon(level, pos, species, random);
            case COCONUT -> coconut(level, pos, species, random);
            case RAINBOWOOD -> rainbowood(level, pos, species, random);
            case BLUE_SPRUCE -> blueSpruce(level, pos, species, random);
        };
    }

    // ── AB case 0: Rubber (LogA/0, LeavesAB/8) ────────────────────────────────────────────────

    private static boolean rubber(LevelAccessor level, BlockPos pos, WoodSpecies sp, RandomSource random) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int max = maxHeight(level, pos, 9);
        if (max < 7) return false;
        int top = y + 7 + random.nextInt(max - 6);
        for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++)
            if ((i != 0 || j != 0) && !canPlace(level, new BlockPos(x + i, top - 5, z + j))) return false;
        log(level, pos, sp, true);
        // GT6 BlockTreeSaplingAB case 0: one resin hole per tree, at the first trunk block with at
        // least six blocks of trunk above it (GT6 additionally skips the roll when another hole is
        // within 256 blocks — the port always places one, documented in the porting notes).
        boolean canPlaceHole = true;
        for (int ty = y + 1; ty < top; ty++) {
            if (canPlaceHole && top - ty > 5) {
                canPlaceHole = false;
                if (resinHole(level, new BlockPos(x, ty, z), random)) continue;
            }
            log(level, new BlockPos(x, ty, z), sp, false);
        }
        leaves(level, new BlockPos(x, top, z), sp);
        leaves(level, new BlockPos(x, top + 1, z), sp);
        for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++)
            if (i != 0 || j != 0) leaves(level, new BlockPos(x + i, top - 1, z + j), sp);
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
            if (i == 0 && j == 0) continue;
            if (Math.abs(i * j) < 2) leaves(level, new BlockPos(x + i, top - 2, z + j), sp);
            if (Math.abs(i * j) < 4) {
                leaves(level, new BlockPos(x + i, top - 3, z + j), sp);
                leaves(level, new BlockPos(x + i, top - 4, z + j), sp);
            }
            leaves(level, new BlockPos(x + i, top - 5, z + j), sp);
        }
        return true;
    }

    // ── AB case 1: Maple (LogA/1, LeavesAB/9) ─────────────────────────────────────────────────

    private static boolean maple(LevelAccessor level, BlockPos pos, WoodSpecies sp, RandomSource random) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int max = maxHeight(level, pos, 11);
        if (max < 9) return false;
        int top = y + 9 + random.nextInt(max - 8);
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++)
            if ((i != 0 || j != 0) && !canPlace(level, new BlockPos(x + i, top - 4, z + j))) return false;
        log(level, pos, sp, true);
        for (int ty = y + 1; ty < top; ty++) log(level, new BlockPos(x, ty, z), sp, false);
        for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) leaves(level, new BlockPos(x + i, top + 1, z + j), sp);
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
            leaves(level, new BlockPos(x + i, top, z + j), sp);
            if (i != 0 || j != 0) {
                if (Math.abs(i * j) < 4) leaves(level, new BlockPos(x + i, top - 7, z + j), sp);
                leaves(level, new BlockPos(x + i, top - 1, z + j), sp);
            }
        }
        for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++) {
            if (i == 0 && j == 0) continue;
            if (Math.abs(i * j) < 9) {
                leaves(level, new BlockPos(x + i, top - 2, z + j), sp);
                leaves(level, new BlockPos(x + i, top - 3, z + j), sp);
                leaves(level, new BlockPos(x + i, top - 6, z + j), sp);
            }
            leaves(level, new BlockPos(x + i, top - 4, z + j), sp);
            leaves(level, new BlockPos(x + i, top - 5, z + j), sp);
        }
        return true;
    }

    // ── AB case 2: Willow (LogA/2, LeavesAB/10) ───────────────────────────────────────────────

    private static boolean willow(LevelAccessor level, BlockPos pos, WoodSpecies sp, RandomSource random) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int max = maxHeight(level, pos, 7);
        if (max < 5) return false;
        int top = y + 5 + random.nextInt(max - 4);
        for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++)
            if ((i != 0 || j != 0) && !canPlace(level, new BlockPos(x + i, top - 2, z + j))) return false;
        log(level, pos, sp, true);
        for (int ty = y + 1; ty < top; ty++) log(level, new BlockPos(x, ty, z), sp, false);
        for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++) {
            if (Math.abs(i * j) < 9) {
                leaves(level, new BlockPos(x + i, top + 1, z + j), sp);
                if (i != 0 || j != 0) leaves(level, new BlockPos(x + i, top - 2, z + j), sp);
            }
            leaves(level, new BlockPos(x + i, top, z + j), sp);
        }
        for (int i = -4; i <= 4; i++) for (int j = -4; j <= 4; j++) {
            if (i == 0 && j == 0) continue;
            if (Math.abs(i * j) >= 10) continue;
            leaves(level, new BlockPos(x + i, top - 1, z + j), sp);
            if (top - 2 <= y) continue;
            if (Math.abs(i * j) > 6) {
                leaves(level, new BlockPos(x + i, top - 2, z + j), sp);
                if (top - 3 <= y) continue;
                leaves(level, new BlockPos(x + i, top - 3, z + j), sp);
                if (top - 4 <= y) continue;
                leaves(level, new BlockPos(x + i, top - 4, z + j), sp);
                if (top - 5 <= y) continue;
                leaves(level, new BlockPos(x + i, top - 5, z + j), sp);
            }
        }
        return true;
    }

    // ── AB case 3: Blue Mahoe (LogA/3, LeavesAB/11) ───────────────────────────────────────────

    private static boolean blueMahoe(LevelAccessor level, BlockPos pos, WoodSpecies sp, RandomSource random) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int max = maxHeight(level, pos, 5);
        if (max < 4) return false;
        int top = y + 4 + random.nextInt(max - 3);
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++)
            if ((i != 0 || j != 0) && !canPlace(level, new BlockPos(x + i, top - 2, z + j))) return false;
        log(level, pos, sp, true);
        for (int ty = y + 1; ty < top; ty++) log(level, new BlockPos(x, ty, z), sp, false);
        for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) leaves(level, new BlockPos(x + i, top + 3, z + j), sp);
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
            if (Math.abs(i * j) < 4) {
                leaves(level, new BlockPos(x + i, top + 2, z + j), sp);
                leaves(level, new BlockPos(x + i, top + 1, z + j), sp);
                if (i != 0 || j != 0) {
                    leaves(level, new BlockPos(x + i, top, z + j), sp);
                    leaves(level, new BlockPos(x + i, top - 1, z + j), sp);
                }
            }
        }
        return true;
    }

    // ── AB case 4: Hazel (LogB/0, LeavesAB/12) ────────────────────────────────────────────────

    private static boolean hazel(LevelAccessor level, BlockPos pos, WoodSpecies sp, RandomSource random) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (maxHeight(level, pos, 4) < 4) return false;
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++)
            if ((i != 0 || j != 0) && !canPlace(level, new BlockPos(x + i, y + 2, z + j))) return false;
        log(level, pos, sp, true);
        log(level, new BlockPos(x, y + 1, z), sp, false);
        log(level, new BlockPos(x, y + 2, z), sp, false);
        for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) leaves(level, new BlockPos(x + i, y + 4, z + j), sp);
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
            if (i != 0 || j != 0) leaves(level, new BlockPos(x + i, y + 2, z + j), sp);
            if (Math.abs(i * j) < 4) leaves(level, new BlockPos(x + i, y + 3, z + j), sp);
        }
        for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++)
            if (Math.abs(i * j) < 9 && (i != 0 || j != 0)) leaves(level, new BlockPos(x + i, y + 1, z + j), sp);
        return true;
    }

    // ── AB case 5: Cinnamon (LogB/1, LeavesAB/13) ─────────────────────────────────────────────

    private static boolean cinnamon(LevelAccessor level, BlockPos pos, WoodSpecies sp, RandomSource random) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int max = maxHeight(level, pos, 8);
        if (max < 6) return false;
        int top = y + 6 + random.nextInt(max - 5);
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++)
            if ((i != 0 || j != 0) && !canPlace(level, new BlockPos(x + i, top - 4, z + j))) return false;
        log(level, pos, sp, true);
        for (int ty = y + 1; ty < top; ty++) log(level, new BlockPos(x, ty, z), sp, false);
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
            leaves(level, new BlockPos(x + i, top + 2, z + j), sp);
            if (i != 0 || j != 0) leaves(level, new BlockPos(x + i, top - 4, z + j), sp);
        }
        for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++) {
            if (Math.abs(i * j) >= 9) continue;
            if (i != 0 || j != 0) {
                leaves(level, new BlockPos(x + i, top - 1, z + j), sp);
                leaves(level, new BlockPos(x + i, top - 2, z + j), sp);
                leaves(level, new BlockPos(x + i, top - 3, z + j), sp);
            }
            leaves(level, new BlockPos(x + i, top, z + j), sp);
            leaves(level, new BlockPos(x + i, top + 1, z + j), sp);
        }
        return true;
    }

    // ── AB case 6: Coconut (LogB/2, LeavesAB/14) ──────────────────────────────────────────────

    private static boolean coconut(LevelAccessor level, BlockPos pos, WoodSpecies sp, RandomSource random) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int max = maxHeight(level, pos, 12);
        if (max < 8) return false;
        int top = y + 8 + random.nextInt(max - 7);
        for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++)
            if ((i != 0 || j != 0) && !canPlace(level, new BlockPos(x + i, top, z + j))) return false;
        log(level, pos, sp, true);
        for (int ty = y + 1; ty < top; ty++) log(level, new BlockPos(x, ty, z), sp, false);
        // Diagonal fronds (GT6 includes (0,0) here: the frond tips sit on the trunk top).
        for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++) {
            if (i != j && i != -j) continue;
            if (Math.abs(i) == 3 || Math.abs(j) == 3) {
                leaves(level, new BlockPos(x + i, top - 1, z + j), sp);
                leaves(level, new BlockPos(x + i, top - 2, z + j), sp);
            } else if (Math.abs(i) == 2 || Math.abs(j) == 2) {
                leaves(level, new BlockPos(x + i, top, z + j), sp);
                leaves(level, new BlockPos(x + i, top - 1, z + j), sp);
            } else {
                leaves(level, new BlockPos(x + i, top, z + j), sp);
            }
        }
        // Straight fronds along the two axes.
        for (int i = -4; i <= 4; i++) for (int j = -4; j <= 4; j++) {
            if (i != 0 && j != 0) continue;
            if (Math.abs(i) == 4 || Math.abs(j) == 4) {
                leaves(level, new BlockPos(x + i, top - 1, z + j), sp);
                leaves(level, new BlockPos(x + i, top - 2, z + j), sp);
            } else if (Math.abs(i) == 3 || Math.abs(j) == 3) {
                leaves(level, new BlockPos(x + i, top, z + j), sp);
                leaves(level, new BlockPos(x + i, top - 1, z + j), sp);
            } else {
                leaves(level, new BlockPos(x + i, top, z + j), sp);
            }
        }
        return true;
    }

    // ── AB case 7: Rainbowood (LogB/3, LeavesAB/15) ───────────────────────────────────────────

    private static boolean rainbowood(LevelAccessor level, BlockPos pos, WoodSpecies sp, RandomSource random) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int max = maxHeight(level, pos, 9);
        if (max < 7) return false;
        int top = y + 7 + random.nextInt(max - 6);
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++)
            if ((i != 0 || j != 0) && !canPlace(level, new BlockPos(x + i, top - 4, z + j))) return false;
        log(level, pos, sp, true);
        for (int ty = y + 1; ty < top; ty++) log(level, new BlockPos(x, ty, z), sp, false);
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
            leaves(level, new BlockPos(x + i, top + 2, z + j), sp);
            if (i != 0 || j != 0) leaves(level, new BlockPos(x + i, top - 4, z + j), sp);
        }
        for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++) {
            if (Math.abs(i * j) >= 9) continue;
            if (i != 0 || j != 0) {
                leaves(level, new BlockPos(x + i, top - 1, z + j), sp);
                leaves(level, new BlockPos(x + i, top - 2, z + j), sp);
                leaves(level, new BlockPos(x + i, top - 3, z + j), sp);
            }
            leaves(level, new BlockPos(x + i, top, z + j), sp);
            leaves(level, new BlockPos(x + i, top + 1, z + j), sp);
        }
        return true;
    }

    // ── CD case 0: Blue Spruce (LogC/0, LeavesCD/8) ───────────────────────────────────────────

    private static boolean blueSpruce(LevelAccessor level, BlockPos pos, WoodSpecies sp, RandomSource random) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int max = maxHeight(level, pos, 16);
        if (max < 16) return false;
        int top = y + max - random.nextInt(3);
        for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++)
            if ((i != 0 || j != 0) && !canPlace(level, new BlockPos(x + i, top - 5, z + j))) return false;
        log(level, pos, sp, true);
        for (int ty = y + 1; ty < top; ty++) log(level, new BlockPos(x, ty, z), sp, false);
        leaves(level, new BlockPos(x, top, z), sp);
        leaves(level, new BlockPos(x, top + 1, z), sp);
        leaves(level, new BlockPos(x + 1, top - 1, z), sp);
        leaves(level, new BlockPos(x - 1, top - 1, z), sp);
        leaves(level, new BlockPos(x, top - 1, z + 1), sp);
        leaves(level, new BlockPos(x, top - 1, z - 1), sp);
        for (int i = -6; i <= 6; i++) for (int j = -6; j <= 6; j++) {
            if (i == 0 && j == 0) continue;
            // Conical body: the radius grows with the distance below the tip.
            for (int k = 1; k <= 14; k++) if (i * i + j * j < k * k * 0.2) leaves(level, new BlockPos(x + i, top + 1 - k, z + j), sp);
            // GT6 converts the dirt/grass underneath into podzol.
            if (i * i + j * j <= 30) {
                for (int k = 0; k <= 3; k++) {
                    BlockPos p = new BlockPos(x + i, y - k, z + j);
                    BlockState state = level.getBlockState(p);
                    if (canPlace(level, p)) continue;
                    if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)) {
                        level.setBlock(p, Blocks.PODZOL.defaultBlockState(), 3);
                    }
                    break;
                }
            }
        }
        return true;
    }

    // ── GT6 helpers (BlockBaseSapling.getMaxHeight / canPlaceTree / placeTree) ────────────────

    /**
     * GT6 {@code getMaxHeight}: how many blocks above the sapling can be replaced, capped at
     * {@code limit}. Note GT6's semantics — {@code while (r++ < limit - 1)} runs for
     * {@code r = 1 .. limit - 1} and exits with {@code r == limit}, so the result is at most
     * {@code limit} and a gate of {@code < limit} is satisfiable by exactly {@code limit} free blocks.
     */
    public static int maxHeight(LevelAccessor level, BlockPos pos, int limit) {
        int max = limit - 1;
        int r = 0;
        while (r++ < max) {
            if (pos.getY() + r >= level.getMaxBuildHeight() || !canPlace(level, pos.offset(0, r, 0))) return r - 1;
        }
        return r;
    }

    /** GT6 {@code canPlaceTree}: saplings, tall grass, snow, leaves and air are replaceable. */
    public static boolean canPlace(LevelAccessor level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return true;
        if (state.is(BlockTags.LEAVES)) return true;
        if (state.getBlock() instanceof WoodSaplingBlock) return true;
        return state.canBeReplaced() && !state.liquid();
    }

    private static void log(LevelAccessor level, BlockPos pos, WoodSpecies sp, boolean force) {
        place(level, pos, GTWoods.log(sp).defaultBlockState(), force);
    }

    /** GT6's rubber tree punches a resin hole into the trunk ({@code MultiTileEntityResinHoleRubber}). */
    private static boolean resinHole(LevelAccessor level, BlockPos pos, RandomSource random) {
        com.gregtech.gregtech.block.wood.TreeHoleBlock hole =
                com.gregtech.gregtech.registry.GTTreeHoles.hole(WoodSpecies.RUBBER);
        if (hole == null) return false;
        net.minecraft.core.Direction[] sides = net.minecraft.core.Direction.Plane.HORIZONTAL.stream()
                .toArray(net.minecraft.core.Direction[]::new);
        return level.setBlock(pos, hole.defaultBlockState()
                .setValue(com.gregtech.gregtech.block.wood.TreeHoleBlock.FACING, sides[random.nextInt(sides.length)]), 3);
    }

    private static void leaves(LevelAccessor level, BlockPos pos, WoodSpecies sp) {
        place(level, pos, GTWoods.leaves(sp).defaultBlockState(), false);
    }

    /** GT6 {@code placeTree} (respects {@code canPlaceTree}) unless {@code force} (its {@code WD.set} trunk base). */
    private static void place(LevelAccessor level, BlockPos pos, BlockState state, boolean force) {
        if (level.isOutsideBuildHeight(pos)) return;
        if (!force && !canPlace(level, pos)) return;
        level.setBlock(pos, state, 3);
    }

    /** True when the block is one of the port's GT6 leaves blocks (used by tests/tools). */
    public static boolean isGtLeaves(Block block) {
        return GTWoods.leavesOf(block) != null;
    }
}
