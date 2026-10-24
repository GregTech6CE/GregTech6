package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.worldgen.GTWaterBodyFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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

@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class WaterBodyBiomeBoundaryTests {
    @GameTest(template = "water_biome_boundary_empty", timeoutTicks = 100)
    public static void swampOverridesGtWaterOnlyInsideItsOwnColumns(GameTestHelper h) {
        var level = h.getLevel();
        var base = h.absolutePos(BlockPos.ZERO);
        // 32x3x1 owned template always contains a complete aligned X chunk span.
        int chunkX = (base.getX() + 15) & ~15;
        BlockPos left = new BlockPos(chunkX + 7, base.getY() + 1, base.getZ());
        BlockPos right = left.east();
        h.assertTrue(level.hasChunkAt(left) && level.hasChunkAt(right), "both template cells already loaded");
        h.assertTrue(level.getBlockEntity(left) == null && level.getBlockEntity(right) == null, "empty fixture has no block entities");
        BlockState oldLeft = level.getBlockState(left), oldRight = level.getBlockState(right);
        BlockState sea = state("seawater"), river = state("riverwater"), swamp = state("swampwater");
        h.assertTrue(sea.getBlock() != Blocks.AIR && river.getBlock() != Blocks.AIR && swamp.getBlock() != Blocks.AIR,
                "all original registered water IDs exist");
        try {
            // Real full feature: ocean first, then swamp; swamp must not repaint ocean half.
            run(h, left, right, Biomes.OCEAN, Blocks.WATER.defaultBlockState(), sea, swamp);
            // Same counterexample for river first, then swamp.
            run(h, left, right, Biomes.RIVER, Blocks.WATER.defaultBlockState(), river, swamp);
            // Source intentionally admits vanilla water across the chunk; no global column gate.
            run(h, left, right, Biomes.PLAINS, Blocks.WATER.defaultBlockState(), swamp, swamp);
            // Existing GT water in a non-swamp column is protected even without a prior pass.
            run(h, left, right, Biomes.PLAINS, river, river, swamp);
        } finally {
            level.setBlock(left, oldLeft, 18);
            level.setBlock(right, oldRight, 18);
        }
        h.succeed();
    }

    private static BlockState state(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", path)).defaultBlockState();
    }
    private static void run(GameTestHelper h, BlockPos left, BlockPos right,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> leftKey,
            BlockState input, BlockState expectedLeft, BlockState expectedRight) {
        var level = h.getLevel();
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        var leftBiome = biomes.getHolderOrThrow(leftKey);
        var swampBiome = biomes.getHolderOrThrow(Biomes.SWAMP);
        level.setBlock(left, input, 18); level.setBlock(right, input, 18);
        WorldGenLevel fixture = (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getSeaLevel")) return left.getY();
                    if (method.getName().equals("getMinBuildHeight")) return left.getY() - 2;
                    if (method.getName().equals("getBiome") && args[0] instanceof BlockPos p)
                        return (p.getX() & 15) < 8 ? leftBiome : swampBiome;
                    if (method.getName().equals("getBlockState") && args[0] instanceof BlockPos p)
                        return p.equals(left) || p.equals(right) ? level.getBlockState(p) : Blocks.AIR.defaultBlockState();
                    if (method.getName().equals("setBlock") && args[0] instanceof BlockPos p) {
                        h.assertTrue(p.equals(left) || p.equals(right), "generation writes only owned native cells");
                        return level.setBlock(p, (BlockState) args[1], ((Integer) args[2]) | 16);
                    }
                    try { return method.invoke(level, args); }
                    catch (InvocationTargetException e) { throw e.getCause(); }
                });
        boolean placed = new GTWaterBodyFeature().place(new FeaturePlaceContext<>(Optional.empty(), fixture,
                level.getChunkSource().getGenerator(), RandomSource.create(42), left, NoneFeatureConfiguration.INSTANCE));
        h.assertTrue(placed, "real generation passes admit mixed-biome chunk");
        h.assertTrue(level.getBlockState(left).equals(expectedLeft), "non-swamp half keeps source-correct water: " + leftKey);
        h.assertTrue(level.getBlockState(right).equals(expectedRight), "swamp half receives swamp water: " + leftKey);
    }
}
