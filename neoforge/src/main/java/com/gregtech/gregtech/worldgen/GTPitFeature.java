package com.gregtech.gregtech.worldgen;

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
 * Port of GT6's {@code WorldgenPit} ({@code Loader_Worldgen:592-597}): the large sand and clay pits
 * of GT6's plains and savannas.
 *
 * <p>GT6 rolls one pit per chunk with {@code nextInt(320) > 0} (i.e. about 1 in 320 chunks), only in
 * plains or savanna, and then carves the 48x48 {@link GTPitShape} mask — anchored 16 blocks before
 * the chunk, so a complete pit spans 3x3 chunks — between {@code seaLevel - 8} and
 * {@code seaLevel + 16}. Per column it replaces the sediment (sand, clay, dirt) it finds, may dig up
 * to seven blocks deep, and only continues through rock and gravel once it is inside the pit; dirt
 * under wood or leaves is left alone.
 *
 * <p>GT6's own diggable blocks are the pit fill: vanilla clay plus its Brown/Yellow/Blue/White Clay
 * (metas 1/4/5/6 of {@code BlocksGT.Diggables}); the Red Clay pit is disabled by default in GT6 and
 * the PFAA pits need another mod, so both are skipped here.
 */
public class GTPitFeature extends Feature<NoneFeatureConfiguration> {
    /** GT6 {@code int tChance = 320} with {@code chance = 1}: {@code nextInt(divider) > chance - 1}. */
    public static final int CHANCE = TerrainWorldgenRules.PIT_CHANCE;
    public static final int DIVIDER = TerrainWorldgenRules.PIT_DIVIDER;
    /** GT6's vertical window: {@code waterLevel + 16} down to {@code waterLevel - 8}. */
    public static final int ABOVE_SEA = TerrainWorldgenRules.PIT_ABOVE_SEA;
    public static final int BELOW_SEA = TerrainWorldgenRules.PIT_BELOW_SEA;
    /** GT6 stops a column after seven pit blocks ({@code tGenerated < 7}). */
    public static final int MAX_DEPTH = TerrainWorldgenRules.PIT_MAX_DEPTH;

    /** One GT6 pit registration: the name from {@code Loader_Worldgen} and the block it fills with. */
    public record Pit(String name, String blockId) {}

    /** {@code Loader_Worldgen:592-597} minus the default-disabled red clay and the PFAA entries. */
    public static final List<Pit> PITS = TerrainWorldgenRules.PITS.stream().map(row->new Pit(row.name(),row.blockId())).toList();

    /** GT6 {@code BIOMES_PLAINS} / {@code BIOMES_SAVANNA}. */
    private static final Set<ResourceLocation> PLAINS = Set.of(
            id("plains"), id("sunflower_plains"), id("meadow"), id("cherry_grove"));
    private static final Set<ResourceLocation> SAVANNA = Set.of(
            id("savanna"), id("savanna_plateau"), id("windswept_savanna"));

    public GTPitFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        if (level.getLevel().dimension() != Level.OVERWORLD) return false;
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        // GT6 checks the biome of the chunk centre only (aBiomes[7][7]).
        if (!pitBiome(biomeId(level, minX + 8, minZ + 8))) return false;
        if (random.nextInt(DIVIDER) > CHANCE - 1) return false;
        Pit pit = PITS.get(random.nextInt(PITS.size()));
        BlockState fill = blockState(pit.blockId());
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

    /** GT6's biome gate: plains or savanna. */
    public static boolean pitBiome(ResourceLocation biome) {
        return biome != null && (PLAINS.contains(biome) || SAVANNA.contains(biome));
    }

    /**
     * GT6's per-column loop of {@code WorldgenPit.generate}. Public so the GameTest can carve a
     * prepared column instead of hunting for a generated pit.
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
            if (state.is(BlockTags.DIRT)) {
                // GT6 leaves dirt alone when it is the very first block and sits under wood/leaves.
                if (generated <= 0 && (last.is(BlockTags.LOGS) || last.is(BlockTags.LEAVES))) continue;
            } else if (!hostAllowed(state, generated)) {
                if (generated > 0) break;
                continue;
            }
            level.setBlock(cursor, fill, 2);
            placed = true;
            generated++;
        }
        return placed;
    }

    /**
     * GT6's host test: sediment (sand, clay, sand ores) is always a host, while rock and gravel only
     * continue a pit that already started.
     */
    private static boolean hostAllowed(BlockState state, int generated) {
        if (state.is(BlockTags.SAND) || state.is(Blocks.CLAY) || isSandLike(state)) return true;
        if (generated <= 0) return false;
        return isRock(state) || state.is(Blocks.GRAVEL) || state.is(BlockTags.DIRT);
    }

    /** GT6's {@code BlocksGT.oreSand} family — the port keeps its sands as icon set blocks. */
    public static boolean isSandLike(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && id.getNamespace().equals("gregtech") && id.getPath().startsWith("sand_");
    }

    /**
     * 1.7.10 {@code Material.rock}: the GT6 worldgen objects that share the pit's column loop
     * ({@link GTBlackSandFeature}, {@link GTTurfFeature}) reuse this test.
     */
    public static boolean isRock(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.STONE_BRICKS)
                || state.is(Blocks.OBSIDIAN) || state.is(Blocks.DEEPSLATE);
    }

    private static BlockState blockState(String id) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        return block == null || block == Blocks.AIR ? null : block.defaultBlockState();
    }

    private static ResourceLocation biomeId(WorldGenLevel level, int x, int z) {
        Holder<Biome> biome = level.getBiome(new BlockPos(x, level.getSeaLevel(), z));
        return biome.unwrapKey().map(ResourceKey::location).orElse(null);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    /** Registry key of the configured feature (data file {@code worldgen/configured_feature/gt_pits.json}). */
    public static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("gregtech", "gt_pits"));
}
