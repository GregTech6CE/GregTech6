package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.registry.GTBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;

/**
 * GT6's forest-floor worldgen data, ported from {@code Loader_Worldgen:603-606} (fallen logs),
 * {@code :630} (twigs) and {@code :632} (glowtus), with the biome sets and probabilities from
 * {@code gregtech/worldgen/tree/WorldgenLog*}, {@code WorldgenSticks} and {@code WorldgenGlowtus}.
 *
 * <p>GT6's four {@code WorldgenLog*} generators all place the very same block
 * ({@code BlocksGT.Log1}, the "Dead/Rotten/Mossy/Frozen Log" block) and differ only in their biome
 * set and roll; the port has those four blocks as separate registry entries
 * ({@code log_dry}, {@code log_rotten}, {@code log_mossy}, {@code log_frozen}) with GT6's own
 * textures, so each entry names its block.
 */
public final class GTSurfaceFlora {
    private GTSurfaceFlora() {}

    // ── GT6 WorldgenSticks ("sticks", T, 2, 2, ...) ───────────────────────────────────────────
    /** {@code mAmount}: target columns per chunk before the biome multiplier. */
    public static final int TWIG_AMOUNT = 2;
    /** {@code mProbability}: {@code 1/2} roll per target column. */
    public static final int TWIG_PROBABILITY = 2;

    // ── GT6 WorldgenGlowtus ("plant.glowtus", T, 16, 2, ...) ─────────────────────────────────
    /** {@code mAmount}: 16 target columns per chunk. */
    public static final int GLOWTUS_AMOUNT = 16;
    /** {@code mProbability}: {@code 1/2} roll per target column. */
    public static final int GLOWTUS_PROBABILITY = 2;
    /** GT6 {@code CS.DYE_NAMES} metadata order, not alphabetical icon/registry order. */
    public static final List<String> GLOWTUS_COLOURS = List.of(
            "black", "red", "green", "brown", "blue", "purple", "cyan", "light_gray",
            "gray", "pink", "lime", "yellow", "light_blue", "magenta", "orange", "white");

    /** One GT6 {@code WorldgenLog*} registration. */
    public record Log(String name, String blockId, int probability, String gt6Biomes,
                      Set<ResourceLocation> biomes, boolean mushrooms, boolean sandAllowed) {}

    private static final Set<ResourceLocation> PLAINS = Set.of(
            id("plains"), id("sunflower_plains"), id("meadow"), id("cherry_grove"));
    private static final Set<ResourceLocation> WOODS = Set.of(
            id("forest"), id("flower_forest"), id("birch_forest"), id("old_growth_birch_forest"),
            id("dark_forest"), id("windswept_forest"));
    private static final Set<ResourceLocation> SAVANNA = Set.of(
            id("savanna"), id("savanna_plateau"), id("windswept_savanna"));
    private static final Set<ResourceLocation> DESERT = Set.of(id("desert"));
    private static final Set<ResourceLocation> MESA = Set.of(
            id("badlands"), id("eroded_badlands"), id("wooded_badlands"));
    private static final Set<ResourceLocation> SWAMP = Set.of(id("swamp"), id("mangrove_swamp"));
    private static final Set<ResourceLocation> JUNGLE = Set.of(
            id("jungle"), id("sparse_jungle"), id("bamboo_jungle"));
    private static final Set<ResourceLocation> FROZEN = Set.of(
            id("snowy_plains"), id("snowy_taiga"), id("snowy_beach"), id("ice_spikes"),
            id("snowy_slopes"), id("frozen_peaks"), id("frozen_river"));
    private static final Set<ResourceLocation> RIVER = Set.of(id("river"), id("frozen_river"));
    private static final Set<ResourceLocation> TAIGA = Set.of(
            id("taiga"), id("snowy_taiga"), id("old_growth_pine_taiga"), id("old_growth_spruce_taiga"));

    /**
     * {@code Loader_Worldgen:603-606} in registration order. GT6's wastelands biome set has no
     * vanilla equivalent, so the "dry" log loses that one entry (documented).
     */
    public static final List<Log> LOGS = List.of(
            new Log("log.dry", "log_dry", 8, "BIOMES_PLAINS/WOODS/SAVANNA/DESERT/MESA/WASTELANDS",
                    union(PLAINS, WOODS, SAVANNA, DESERT, MESA), false, true),
            new Log("log.rotten", "log_rotten", 3, "BIOMES_SWAMP/JUNGLE",
                    union(SWAMP, JUNGLE), false, false),
            new Log("log.mossy", "log_mossy", 8, "BIOMES_PLAINS/WOODS/SWAMP",
                    union(PLAINS, WOODS, SWAMP), true, true),
            new Log("log.frozen", "log_frozen", 8, "BIOMES_FROZEN (contact may also be snow)",
                    FROZEN, false, false));

    /** GT6 {@code WorldgenSticks.canGenerate}: {@code BIOMES_WOODS/SWAMP} give ×3, plains/river/... ×2, taiga/mesa ×1. */
    public static int twigMultiplier(ResourceLocation biome) {
        if (biome == null) return 0;
        if (WOODS.contains(biome) || SWAMP.contains(biome)) return 3;
        if (RIVER.contains(biome) || PLAINS.contains(biome) || SAVANNA.contains(biome)) return 2;
        if (TAIGA.contains(biome) || MESA.contains(biome)) return 1;
        return 0;
    }

    /** GT6 {@code WorldgenGlowtus.canGenerate}: jungle ("Fire Swamp" is a modded biome) — swamp added per GT6's BIOMES_JUNGLE use. */
    public static boolean glowtusBiome(ResourceLocation biome) {
        return biome != null && (JUNGLE.contains(biome) || SWAMP.contains(biome));
    }

    /** Resolves one of the four fallen-log blocks (GT6 {@code BlocksGT.Log1} metas 0-3). */
    public static Block logBlock(Log log) {
        return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", log.blockId()));
    }

    /** The port's ground twigs (GT6 {@code WorldgenSticks} places the stick multi-tile here). */
    public static Block twigs() {
        return GTBlocks.TWIGS.get();
    }

    /** One of the 16 glowtus colours (GT6's {@code BlocksGT.Glowtus} meta 0-15). */
    public static Block glowtus(String colour) {
        return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", "glowtus_" + colour));
    }

    @SafeVarargs
    private static Set<ResourceLocation> union(Set<ResourceLocation>... sets) {
        Set<ResourceLocation> out = new java.util.HashSet<>();
        for (Set<ResourceLocation> set : sets) out.addAll(set);
        return Set.copyOf(out);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
