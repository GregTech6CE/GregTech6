package com.gregtech.gregtech.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;

import java.util.HashSet;
import java.util.Set;

/**
 * GT6's biome name sets and the "which biomes does this chunk cover" lookup the GT6 worldgen
 * objects receive as {@code Set&lt;String&gt; aBiomeNames}.
 *
 * <p>GT6 got that set handed to it for free (1.7.10 chunks carried a 16x16 biome array). In 1.20.1
 * biomes live in 4x4x4 cells per chunk section, so the 16 biome cells of a chunk are exactly the
 * samples at {@code (minX + 4k + 2, minZ + 4l + 2)} — the port reads those.
 */
public final class GTWorldgenBiomes {
    /** GT6 {@code BIOMES_RIVER} (plus the 1.20.1 oceans that count as rivers here). */
    public static final Set<ResourceLocation> RIVER = Set.of(
            id("river"), id("frozen_river"));

    /** GT6 {@code BIOMES_OCEAN_BEACH}. */
    public static final Set<ResourceLocation> OCEAN_BEACH = Set.of(
            id("ocean"), id("deep_ocean"), id("warm_ocean"), id("lukewarm_ocean"),
            id("deep_lukewarm_ocean"), id("cold_ocean"), id("deep_cold_ocean"),
            id("frozen_ocean"), id("deep_frozen_ocean"),
            id("beach"), id("snowy_beach"), id("stony_shore"), id("mushroom_field_shore"));

    /** GT6 {@code BIOMES_OCEAN} — the same without the beaches and shores. */
    public static final Set<ResourceLocation> OCEAN = Set.of(
            id("ocean"), id("deep_ocean"), id("warm_ocean"), id("lukewarm_ocean"),
            id("deep_lukewarm_ocean"), id("cold_ocean"), id("deep_cold_ocean"),
            id("frozen_ocean"), id("deep_frozen_ocean"));

    /** GT6 {@code BIOMES_SWAMP}. */
    public static final Set<ResourceLocation> SWAMP = Set.of(
            id("swamp"), id("mangrove_swamp"));

    /** GT6 {@code BIOMES_DESERT}. */
    public static final Set<ResourceLocation> DESERT = Set.of(id("desert"));

    /** GT6 {@code BIOMES_MESA}. */
    public static final Set<ResourceLocation> MESA = Set.of(
            id("badlands"), id("eroded_badlands"), id("wooded_badlands"));

    /** GT6 {@code BIOMES_TAIGA}. */
    public static final Set<ResourceLocation> TAIGA = Set.of(
            id("taiga"), id("snowy_taiga"), id("old_growth_pine_taiga"), id("old_growth_spruce_taiga"));

    /** GT6 {@code BIOMES_WOODS}. */
    public static final Set<ResourceLocation> WOODS = Set.of(
            id("forest"), id("flower_forest"), id("birch_forest"), id("old_growth_birch_forest"),
            id("dark_forest"), id("windswept_forest"), id("grove"));

    /** GT6 {@code BIOMES_MOUNTAINS}. */
    public static final Set<ResourceLocation> MOUNTAINS = Set.of(
            id("windswept_hills"), id("windswept_gravelly_hills"), id("stony_shore"),
            id("jagged_peaks"), id("frozen_peaks"), id("stony_peaks"), id("meadow"), id("snowy_slopes"));

    /** GT6 {@code BIOMES_PLAINS}. */
    public static final Set<ResourceLocation> PLAINS = Set.of(
            id("plains"), id("sunflower_plains"), id("snowy_plains"), id("meadow"), id("cherry_grove"));

    /** GT6 {@code BIOMES_SAVANNA}. */
    public static final Set<ResourceLocation> SAVANNA = Set.of(
            id("savanna"), id("savanna_plateau"), id("windswept_savanna"));

    /** GT6 {@code BIOMES_WASTELANDS} — no 1.20.1 vanilla equivalent (other-mod biomes). */
    public static final Set<ResourceLocation> WASTELANDS = Set.of();

    /**
     * GT6 {@code WorldgenRocks}' biome gate ({@code WorldgenRocks.canGenerate}): desert, mesa,
     * taiga, swamp, savanna, plains, woods, mountains and wastelands.
     */
    public static final Set<ResourceLocation> ROCK_BIOMES = union(
            DESERT, MESA, TAIGA, SWAMP, SAVANNA, PLAINS, WOODS, MOUNTAINS, WASTELANDS);

    /** Set union, for the combined GT6 biome gates. */
    @SafeVarargs
    public static Set<ResourceLocation> union(Set<ResourceLocation>... sets) {
        Set<ResourceLocation> out = new HashSet<>();
        for (Set<ResourceLocation> set : sets) out.addAll(set);
        return out;
    }

    private GTWorldgenBiomes() {}

    /** The biome names of a chunk, GT6-style ({@code aBiomeNames}). */
    public static Set<ResourceLocation> chunkBiomes(WorldGenLevel level, int minX, int minZ) {
        Set<ResourceLocation> ids = new HashSet<>();
        for (int dx = 0; dx < 16; dx += 4) {
            for (int dz = 0; dz < 16; dz += 4) {
                ResourceLocation biome = biomeId(level, minX + dx + 2, minZ + dz + 2);
                if (biome != null) ids.add(biome);
            }
        }
        return ids;
    }

    /** True when any of {@code biomes} is in {@code set}. */
    public static boolean anyOf(Set<ResourceLocation> biomes, Set<ResourceLocation> set) {
        for (ResourceLocation biome : biomes) {
            if (set.contains(biome)) return true;
        }
        return false;
    }

    /** The biome at a column, at the level's sea height (where surface features are decided). */
    public static ResourceLocation biomeId(WorldGenLevel level, int x, int z) {
        Holder<Biome> biome = level.getBiome(new BlockPos(x, level.getSeaLevel(), z));
        return biome.unwrapKey().map(ResourceKey::location).orElse(null);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
