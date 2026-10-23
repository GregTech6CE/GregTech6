package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.worldgen.GTBlackSandFeature;
import com.gregtech.gregtech.worldgen.GTPitFeature;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Full native feature entrypoints over bounded real ServerLevel columns; biome/sea/rarity are
 * deterministic fixture inputs. Unprepared terrain is air, never queried or loaded. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class SedimentSurfaceAuditTests {
    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void pitKeepsGrassAndGeneratesBelowIt(GameTestHelper h) { verify(h, false); }
    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void blackSandKeepsGrassAndGeneratesBelowIt(GameTestHelper h) { verify(h, true); }

    private static void verify(GameTestHelper h, boolean black) {
        var top = h.absolutePos(new BlockPos(2, 12, 2));
        var level = h.getLevel();
        // Four native columns: intact grass, exposed sand, coarse dirt, podzol (old dirt meta 2).
        var caps = new net.minecraft.world.level.block.Block[]{Blocks.GRASS_BLOCK, Blocks.SAND,
                Blocks.COARSE_DIRT, Blocks.PODZOL};
        for (int x = 0; x < caps.length; x++) {
            var p = top.offset(x, 0, 0);
            level.setBlock(p.above(), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(p, caps[x].defaultBlockState(), 2);
            for (int y = 1; y <= 8; y++) level.setBlock(p.below(y), Blocks.DIRT.defaultBlockState(), 2);
        }
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(black ? Biomes.RIVER : Biomes.PLAINS);
        var written = new java.util.HashSet<BlockPos>();
        WorldGenLevel fixture = (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getSeaLevel")) return top.getY();
                    if (method.getName().equals("getBiome")) return biome;
                    if (method.getReturnType() == BlockState.class && args != null && args.length == 1
                            && args[0] instanceof BlockPos p) {
                        if (!inside(p, top)) return Blocks.AIR.defaultBlockState();
                        return level.getBlockState(p);
                    }
                    if (method.getReturnType() == boolean.class && args != null && args.length >= 3
                            && args[0] instanceof BlockPos p && args[1] instanceof BlockState state) {
                        h.assertTrue(inside(p, top), "full feature cannot write outside bounded native fixture");
                        written.add(p.immutable());
                        return level.setBlock(p, state, (Integer) args[2]);
                    }
                    try { return method.invoke(level, args); }
                    catch (InvocationTargetException e) { throw e.getCause(); }
                });
        int divider = black ? GTBlackSandFeature.DIVIDER : GTPitFeature.DIVIDER;
        long seed = 0;
        while (RandomSource.create(seed).nextInt(divider) != 0) seed++;
        var context = new FeaturePlaceContext<NoneFeatureConfiguration>(Optional.empty(), fixture,
                level.getChunkSource().getGenerator(), RandomSource.create(seed), top,
                NoneFeatureConfiguration.INSTANCE);
        boolean placed = black ? new GTBlackSandFeature().place(context) : new GTPitFeature().place(context);
        h.assertTrue(placed, "full native feature passes biome and rarity gates");
        h.assertTrue(level.getBlockState(top).is(Blocks.GRASS_BLOCK), "grass cap must survive source host filter");
        h.assertTrue(written.contains(top.below()), "eligible buried dirt must still generate");
        h.assertTrue(written.contains(top.offset(1, 0, 0)), "bare sand remains legitimate exposed deposit without water");
        h.assertTrue(written.contains(top.offset(2, 0, 0)), "coarse dirt is source dirt meta 1");
        h.assertTrue(written.contains(top.offset(3, 0, 0)) != black,
                "podzol is source dirt meta 2: clay permits it, black sand excludes it");
        h.succeed();
    }
    private static boolean inside(BlockPos p, BlockPos top) {
        return p.getX() >= top.getX() && p.getX() < top.getX() + 4 && p.getZ() == top.getZ()
                && p.getY() >= top.getY() - 8 && p.getY() <= top.getY() + 1;
    }
}
