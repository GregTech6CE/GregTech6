package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.block.OreBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;
import java.util.Set;

/**
 * Port of GT6's {@code WorldgenBlackSand} ({@code Loader_Worldgen:581}, registered as
 * {@code "river.magnetite"}): placer deposits of black magnetite sand in rivers.
 *
 * <p>GT6 rolls one deposit per chunk with {@code nextInt(64) > 0} (about 1 in 64 chunks), refuses
 * any chunk that touches an ocean, beach or swamp biome and requires a river biome in the chunk,
 * then carves the same 48x48 {@link GTPitShape} mask as the clay pits — anchored 16 blocks before
 * the chunk — between {@code waterLevel + 1} and {@code waterLevel - 12}, replacing at most two
 * blocks per column with {@code BlocksGT.Sands}. Which of the three sands (meta 0 magnetite,
 * 1 basaltic, 2 granitic) a deposit uses is a single noise sample per chunk area:
 * {@code noise(minX / 4, 360, minZ / 4, 3)}.
 */
public class GTBlackSandFeature extends Feature<NoneFeatureConfiguration> {
    /** GT6 {@code aRandom.nextInt(64) > 0}. */
    public static final int DIVIDER = 64;
    /** GT6's vertical window: {@code waterLevel + 1} down to {@code waterLevel - 12}. */
    public static final int ABOVE_SEA = 1;
    public static final int BELOW_SEA = 12;
    /** GT6 stops a column after two sand blocks ({@code tGenerated < 2}). */
    public static final int MAX_DEPTH = 2;
    /** GT6's noise height and option count for the sand variant. */
    public static final float NOISE_Y = 360.0F;
    public static final int SAND_VARIANTS = 3;

    /** GT6 {@code BlocksGT.Sands} metas 0/1/2 (Magnetite / BasalticMineralSand / GraniticMineralSand). */
    public static final List<String> BLACK_SANDS = com.gregtech.gregtech.block.BlackSandDefinitions.IDS;

    public GTBlackSandFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        if (level.getLevel().dimension() != Level.OVERWORLD) return false;
        // GT6 rolls the rarity first and only then looks at the biomes.
        if (random.nextInt(DIVIDER) > 0) return false;
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        Set<ResourceLocation> biomes = GTWorldgenBiomes.chunkBiomes(level, minX, minZ);
        if (GTWorldgenBiomes.anyOf(biomes, GTWorldgenBiomes.OCEAN_BEACH)) return false;
        if (GTWorldgenBiomes.anyOf(biomes, GTWorldgenBiomes.SWAMP)) return false;
        if (!GTWorldgenBiomes.anyOf(biomes, GTWorldgenBiomes.RIVER)) return false;

        BlockState fill = blockState(sandId(level, minX, minZ));
        if (fill == null) return false;

        int anchorX = minX - 16;
        int anchorZ = minZ - 16;
        int upper = level.getSeaLevel() + ABOVE_SEA;
        int lower = level.getSeaLevel() - BELOW_SEA;
        boolean placed = false;
        for (int i = 0; i < GTPitShape.SIZE; i++) {
            for (int j = 0; j < GTPitShape.SIZE; j++) {
                if (!GTPitShape.at(i, j)) continue;
                placed |= carveColumn(level, anchorX + i, anchorZ + j, upper, lower, fill);
            }
        }
        return placed;
    }

    /** GT6's per-chunk-area sand variant: {@code noise(aMinX / 4, 360, aMinZ / 4, 3)}. */
    public static String sandId(WorldGenLevel level, int minX, int minZ) {
        int variant = new GTCellNoise(level.getSeed())
                .get(minX / 4.0F, NOISE_Y, minZ / 4.0F, SAND_VARIANTS);
        return BLACK_SANDS.get(variant);
    }

    /**
     * GT6's per-column loop of {@code WorldgenBlackSand.generate}, public so a GameTest can drive a
     * prepared column instead of hunting for a generated deposit (the feature itself is 1 in 64).
     *
     * @return true when at least one block was replaced
     */
    public static boolean carveColumn(WorldGenLevel level, int x, int z, int upper, int lower, BlockState fill) {
        BlockState previous = level.getBlockState(new BlockPos(x, upper + 1, z));
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = upper, generated = 0; y >= lower && generated < MAX_DEPTH; y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            BlockState last = previous;
            previous = state;
            if (state.is(fill.getBlock())) {
                generated++;
                continue;
            }
            if (!state.isSolidRender(level, cursor)) {
                if (generated > 0) break;
                continue;
            }
            if (isSediment(state)) {
                // GT6 leaves the dirt under wood or gourds alone.
                if (generated <= 0 && isWoodOrGourd(last)) continue;
            } else if (generated > 0) {
                if (!GTPitFeature.isRock(state)) break;
            } else {
                continue;
            }
            level.setBlock(cursor, fill, 2);
            placed = true;
            generated++;
        }
        return placed;
    }

    /**
     * GT6's host list: dirt, gravel, sand, clay and the ore-sand/ore-gravel blocks. The port keeps
     * black sands and ores as their own blocks, so {@code sand_*} and any ore whose host rock is a
     * loose sediment count too — read from the ore's {@link OreBlock#STONE} state property (§103.B;
     * it used to be the ore block entity).
     */
    private static boolean isSediment(BlockState state) {
        if (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null || !id.getNamespace().equals("gregtech")) return false;
        if (id.getPath().startsWith("sand_")) return true;
        return state.getBlock() instanceof OreBlock && OreBlock.stoneOf(state).isSediment();
    }

    /** 1.7.10 {@code Material.wood} / {@code Material.gourd}. */
    private static boolean isWoodOrGourd(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS)
                || state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN);
    }

    private static BlockState blockState(String id) {
        Block block = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("gregtech", id));
        return block == null || block == Blocks.AIR ? null : block.defaultBlockState();
    }

    /** Registry key of the configured feature (data file {@code worldgen/configured_feature/gt_black_sand.json}). */
    public static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath("gregtech", "gt_black_sand"));
}
