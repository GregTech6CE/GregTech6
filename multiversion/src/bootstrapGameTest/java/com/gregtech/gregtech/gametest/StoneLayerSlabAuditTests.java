package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.stone.*;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.worldgen.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Optional;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** INACTIVE proposal: expected to fail the conversion assertion on current main. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class StoneLayerSlabAuditTests {
    @GameTest(template = "stone_layer_slab_empty", timeoutTicks = 200)
    public static void structureCobbleSlabsFollowOnlyTheirLocalLayer(GameTestHelper h) {
        var level = h.getLevel();
        // A single owned native cell, within the feature's generation range. Proxy masks all
        // other reads with air and rejects all other writes; full feature cannot touch neighbors.
        // The anchor may contain the GameTest structure control block entity.
        // Center (1,1,1) is strictly inside our dedicated 3x3x3 empty fixture.
        BlockPos p = h.absolutePos(new BlockPos(1, 1, 1));
        h.assertTrue(net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(h.absolutePos(BlockPos.ZERO), h.absolutePos(new BlockPos(2, 2, 2))).isInside(p), "native cell is inside dedicated structure bounds");
        h.assertTrue(p.getY() > -63 && p.getY() < 112, "fixture must be in layer generation range");
        h.assertTrue(level.hasChunkAt(p), "owned template cell already loaded");
        h.assertTrue(level.getBlockEntity(p) == null, "fixture has no block entity to preserve");
        BlockState pristine = level.getBlockState(p);
        long seed = seedFor(p, true);
        var def = definition(p, seed);
        StoneType type = StoneType.valueOf(def.stoneType());
        try {
            for (boolean mossy : new boolean[]{false, true}) {
                StoneVariant variant = mossy ? StoneVariant.COBBLE_MOSSY : StoneVariant.COBBLE;
                Block vanilla = mossy ? Blocks.MOSSY_COBBLESTONE_SLAB : Blocks.COBBLESTONE_SLAB;
                Block full = GTBlocks.getStone(type, variant);
                Block slab = GTBlocks.getStoneSlab(type, variant);
                h.assertTrue(full != null && slab instanceof GTStoneSlabBlock, "real registered target pair");
                // Calibrate actual full-block behavior at precisely the SAME position and seed.
                BlockState control = (mossy ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE).defaultBlockState();
                h.assertTrue(generate(h, p, seed, control).is(full), "native full-block control chooses expected local material");
                for (SlabType half : SlabType.values()) for (boolean wet : new boolean[]{false, true}) {
                    // Vanilla double slabs cannot contain water through legitimate placement.

                    BlockState input = vanilla.defaultBlockState().setValue(SlabBlock.TYPE, half)
                            .setValue(BlockStateProperties.WATERLOGGED, wet);
                    BlockState actual = generate(h, p, seed, input);
                    if (half == SlabType.DOUBLE && wet) {
                        h.assertTrue(actual.equals(input), "noncanonical waterlogged double stays intact instead of losing water");
                    } else if (half == SlabType.DOUBLE) {
                        h.assertTrue(actual.equals(full.defaultBlockState()), "double maps to matching full block, never half geometry");
                    } else {
                        Direction facing = half == SlabType.TOP ? Direction.UP : Direction.DOWN;
                        BlockState expected = slab.defaultBlockState().setValue(GTStoneSlabBlock.FACING, facing)
                                .setValue(BlockStateProperties.WATERLOGGED, wet);
                        h.assertTrue(actual.equals(expected), "exact local material, variant, half and waterlogging: " + input);
                    }
                }
                // Existing GT half blocks are explicitly NOT eligible. Preserve every axis/half.
                StoneType other = java.util.Arrays.stream(StoneType.values()).filter(t -> t != type
                        && GTBlocks.getStoneSlab(t, variant) != null).findFirst().orElseThrow();
                Block existing = GTBlocks.getStoneSlab(other, variant);
                for (Direction facing : Direction.values()) for (boolean wet : new boolean[]{false, true}) {
                    BlockState input = existing.defaultBlockState().setValue(GTStoneSlabBlock.FACING, facing)
                            .setValue(BlockStateProperties.WATERLOGGED, wet);
                    h.assertTrue(generate(h, p, seed, input).equals(input), "existing GT slab orientation/material survives");
                }
            }
            for (Block block : new Block[]{Blocks.STONE_SLAB, Blocks.STONE_BRICK_SLAB,
                    Blocks.OAK_SLAB, Blocks.COBBLESTONE_STAIRS, Blocks.MOSSY_COBBLESTONE_STAIRS}) {
                BlockState input = block.defaultBlockState();
                h.assertTrue(generate(h, p, seed, input).equals(input), "unrelated structures/stairs excluded: " + block);
            }
            long noTarget = seedFor(p, false);
            for (Block block : new Block[]{Blocks.COBBLESTONE_SLAB, Blocks.MOSSY_COBBLESTONE_SLAB}) {
                BlockState input = block.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP)
                        .setValue(BlockStateProperties.WATERLOGGED, true);
                h.assertTrue(generate(h, p, noTarget, input).equals(input), "layer without cobble target preserves host");
            }
        } finally { level.setBlock(p, pristine, 18); }
        h.succeed();
    }

    private static GTStoneLayersGen.LayerDef definition(BlockPos p, long seed) {
        return GTStoneLayersGen.LAYERS.get(new GTCellNoise(seed).get(p.getX(), p.getY(), p.getZ(), GTStoneLayersGen.LAYERS.size()));
    }
    private static long seedFor(BlockPos p, boolean target) {
        for (long seed = 0; seed < 100000; seed++) {
            var def = definition(p, seed);
            if (!def.noDeep() && (def.stoneType() != null) == target) return seed;
        }
        throw new AssertionError("cannot find bounded deterministic fixture seed");
    }
    private static BlockState generate(GameTestHelper h, BlockPos p, long seed, BlockState input) {
        var level = h.getLevel();
        level.setBlock(p, input, 18);
        WorldGenLevel fixture = (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getSeed")) return seed;
                    // Surface-rock decoration is outside this subterranean one-cell fixture.
                    if (method.getName().equals("getHeight") && args != null && args.length == 3)
                        return level.getMinBuildHeight();
                    if (method.getName().equals("getBlockState") && args[0] instanceof BlockPos read)
                        return read.equals(p) ? level.getBlockState(p) : Blocks.AIR.defaultBlockState();
                    if (method.getName().equals("setBlock") && args[0] instanceof BlockPos write) {
                        h.assertTrue(write.equals(p), "full layer feature must stay within owned cell");
                        return level.setBlock(p, (BlockState) args[1], ((Integer) args[2]) | 16);
                    }
                    try { return method.invoke(level, args); }
                    catch (InvocationTargetException e) { throw e.getCause(); }
                });
        new GTStoneLayerFeature().place(new FeaturePlaceContext<>(Optional.empty(), fixture,
                level.getChunkSource().getGenerator(), RandomSource.create(42), p, NoneFeatureConfiguration.INSTANCE));
        return level.getBlockState(p);
    }
}
