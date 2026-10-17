package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.block.wood.WoodSpecies;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.List;
import java.util.Set;

/**
 * Port of GT6's {@code WorldgenTree*} surface generators ({@code Loader_Worldgen:608-616}).
 *
 * <p>GT6 registers one {@code WorldgenOnSurface} per tree species: {@code amount = 1} target column
 * per chunk, a {@code 1/probability} roll on that column, a biome-name gate
 * ({@code BIOMES_RUBBER}, {@code BIOMES_MAPLE}, ...), a ray cast down from the sky to the first
 * solid block and then {@code tryPlaceStuff}, which plants a sapling and immediately grows it.
 * This feature keeps that shape of the algorithm: {@link #ENTRIES} carries the species, its GT6
 * probability and the 1.20.1 biome names matching GT6's biome sets, and the tree itself comes from
 * {@link GTTreeShapes} (the same code saplings use).
 *
 * <p>Documented deviations: GT6 matched raw biome <em>names</em> of the whole chunk (including
 * other mods' biomes) — the port checks the biome of the exact column and only knows vanilla
 * biomes, so GT6's modded-biome-only entries cannot generate (Rainbowood's set is
 * "Enchanted Forest" only, so it is growable from saplings but never generated, exactly like GT6
 * without BiomesO'Plenty installed).
 */
public class GTTreesFeature extends Feature<NoneFeatureConfiguration> {

    /** One GT6 {@code WorldgenTree*} registration: species, its 1/N probability and its biome set. */
    public record Entry(WoodSpecies species, int probability, String gt6Biomes, Set<ResourceLocation> biomes) {}

    private static final Set<ResourceLocation> TAIGA = Set.of(
            id("taiga"), id("snowy_taiga"), id("old_growth_pine_taiga"), id("old_growth_spruce_taiga"));
    private static final Set<ResourceLocation> FOREST = Set.of(
            id("forest"), id("flower_forest"), id("birch_forest"), id("old_growth_birch_forest"), id("dark_forest"));
    private static final Set<ResourceLocation> SWAMP = Set.of(id("swamp"), id("mangrove_swamp"));
    private static final Set<ResourceLocation> JUNGLE = Set.of(id("jungle"), id("sparse_jungle"), id("bamboo_jungle"));
    private static final Set<ResourceLocation> PLAINS = Set.of(id("plains"), id("sunflower_plains"), id("meadow"));
    private static final Set<ResourceLocation> BEACH = Set.of(id("beach"), id("snowy_beach"));
    private static final Set<ResourceLocation> MOUNTAINS = Set.of(
            id("windswept_hills"), id("windswept_gravelly_hills"), id("windswept_forest"),
            id("jagged_peaks"), id("frozen_peaks"), id("stony_peaks"), id("snowy_slopes"), id("grove"));

    /**
     * GT6 {@code Loader_Worldgen:608-616} in registration order — {@code (species, probability,
     * GT6 biome set, 1.20.1 biomes)}. Rainbowood is listed by GT6 too, but its biome set contains
     * only the modded "Enchanted Forest", so it has no vanilla biome here (documented above).
     */
    public static final List<Entry> ENTRIES = List.of(
            new Entry(WoodSpecies.RUBBER,      5, "BIOMES_RUBBER (taiga family)", TAIGA),
            new Entry(WoodSpecies.MAPLE,       5, "BIOMES_MAPLE (forest family)", FOREST),
            new Entry(WoodSpecies.WILLOW,      4, "BIOMES_WILLOW (swampland)", SWAMP),
            new Entry(WoodSpecies.BLUE_MAHOE,  3, "BIOMES_BLUEMAHOE (jungle)", JUNGLE),
            new Entry(WoodSpecies.HAZEL,      32, "BIOMES_HAZEL (plains)", PLAINS),
            new Entry(WoodSpecies.CINNAMON,    3, "BIOMES_CINNAMON (jungle)", JUNGLE),
            new Entry(WoodSpecies.COCONUT,     1, "BIOMES_COCONUT (beach)", BEACH),
            new Entry(WoodSpecies.BLUE_SPRUCE,32, "BIOMES_BLUESPRUCE (mountains)", MOUNTAINS));

    public GTTreesFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        if (level.getLevel().dimension() != Level.OVERWORLD) return false;

        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        boolean placed = false;
        for (Entry entry : ENTRIES) {
            // GT6: one target column per chunk, then a 1/probability roll.
            if (random.nextInt(entry.probability()) != 0) continue;
            int x = minX + random.nextInt(16);
            int z = minZ + random.nextInt(16);
            if (!entry.biomes().contains(biomeId(level, x, z))) continue;
            placed |= plant(level, x, z, entry.species(), random);
        }
        return placed;
    }

    /** GT6's sky ray cast: down to the first solid block, then plant the sapling above it and grow. */
    static boolean plant(WorldGenLevel level, int x, int z, WoodSpecies species, RandomSource random) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        for (int y = top; y > level.getMinBuildHeight() + 1; y--) {
            BlockPos ground = new BlockPos(x, y - 1, z);
            BlockState state = level.getBlockState(ground);
            // GT6 ignored liquids, non-opaque blocks, wood and leaves on the way down.
            if (state.liquid()) return false;
            if (!state.isFaceSturdy(level, ground, net.minecraft.core.Direction.UP)
                    && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)) continue;
            if (!state.is(BlockTags.DIRT) && !state.is(BlockTags.SAND)) return false;
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).isAir()) return false;
            return GTTreeShapes.grow(level, pos, species, random);
        }
        return false;
    }

    private static ResourceLocation biomeId(WorldGenLevel level, int x, int z) {
        Holder<Biome> biome = level.getBiome(new BlockPos(x, level.getSeaLevel(), z));
        return biome.unwrapKey().map(ResourceKey::location).orElse(null);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    /** Registry key of the configured feature (data file {@code worldgen/configured_feature/gt_trees.json}). */
    public static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("gregtech", "gt_trees"));
}
