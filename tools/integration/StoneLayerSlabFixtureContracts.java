package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.worldgen.GTCellNoise;
import com.gregtech.gregtech.worldgen.GTStoneLayersGen;

/** No Minecraft stubs or copied noise: compile with the real core classes and test-only fixture. */
public final class StoneLayerSlabFixtureContracts {
    private static int assertions;

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static GTStoneLayersGen.LayerDef definition(int x, int y, int z, long seed) {
        return GTStoneLayersGen.LAYERS.get(new GTCellNoise(seed).get(x, y, z, GTStoneLayersGen.LAYERS.size()));
    }

    private static boolean noTarget(int x, int y, int z, long seed) {
        var def = definition(x, y, z, seed);
        return !def.noDeep() && def.stoneType() == null;
    }

    public static void main(String[] args) {
        check(StoneLayerSlabFixture.X == 1 && StoneLayerSlabFixture.Y == -59 && StoneLayerSlabFixture.Z == 1,
                "logical sample coordinates remain pinned inside the generation range");
        check(new GTCellNoise(StoneLayerSlabFixture.TARGET_SEED).get(StoneLayerSlabFixture.X,
                StoneLayerSlabFixture.Y, StoneLayerSlabFixture.Z, GTStoneLayersGen.LAYERS.size()) == 72,
                "positive sample selects pinned layer index");
        check(new GTCellNoise(StoneLayerSlabFixture.NO_TARGET_SEED).get(StoneLayerSlabFixture.X,
                StoneLayerSlabFixture.Y, StoneLayerSlabFixture.Z, GTStoneLayersGen.LAYERS.size()) == 106,
                "negative sample selects pinned layer index");
        var target = StoneLayerSlabFixture.definition(StoneLayerSlabFixture.TARGET_SEED);
        check(!target.noDeep() && "LIMESTONE".equals(target.stoneType()), "positive fixture has real cobble targets");
        var negative = StoneLayerSlabFixture.definition(StoneLayerSlabFixture.NO_TARGET_SEED);
        check(!negative.noDeep() && negative.stoneType() == null && "Stone".equals(negative.material()),
                "negative fixture is no-target, not noDeep suppression");

        // CI XML reports a failure position, not an identified sampled native center.
        // Cover it AND the +1 center-offset candidate without assuming they are the same.
        int[][] failed = {{-12996951, -60, -2694830}, {-12996950, -59, -2694829}};
        long[] knownNegativeSeeds = {1307465, 144263};
        for (int i = 0; i < failed.length; i++) {
            int[] p = failed[i];
            for (int seed = 0; seed < 100000; seed++) {
                check(!noTarget(p[0], p[1], p[2], seed), "old bounded scan unexpectedly found a no-target seed");
            }
            check(noTarget(p[0], p[1], p[2], knownNegativeSeeds[i]), "independent late-seed witness at CI coordinate");
            check(definition(p[0], p[1], p[2], 0).stoneType() != null, "old CI coordinate has a positive branch");
        }

        // Hash index uses bits 0..7 after h ^= h >> 13, hence only seed bits 0..20.
        // One complete 2^21 period covers all possible jitter choices for every long seed.
        // At a cell center no seed can move the closest point to another cell; all 256
        // table offsets have radius ~0.45, smaller than half the distance between centers.
        for (int seed = 0; seed < (1 << 21); seed++) {
            check(new GTCellNoise(seed).get(0, 0, 0, GTStoneLayersGen.LAYERS.size()) == 65,
                    "origin must expose why enlarging the search cannot guarantee both branches");
        }
        int[][] points = {{0, 0, 0}, {1, -59, 1}, failed[0], failed[1],
                {29999983, -63, -29999983}, {-29999983, 111, 29999983}, {18000000, -59, 18000000}};
        long[] seeds = {0, 126, 1307465, 144263, -1, Integer.MAX_VALUE, Integer.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE};
        for (int[] p : points) for (long seed : seeds) {
            float expected = new GTCellNoise(seed).get(p[0], p[1], p[2]);
            for (long delta : new long[]{1L << 21, -(1L << 21), 1L << 32}) {
                check(Float.floatToIntBits(expected) == Float.floatToIntBits(new GTCellNoise(seed + delta).get(p[0], p[1], p[2])),
                        "exact noise seed alias holds at signed/large coordinates");
            }
        }
        System.out.println("StoneLayerSlabFixtureContracts: " + assertions + " checks passed");
    }
}
