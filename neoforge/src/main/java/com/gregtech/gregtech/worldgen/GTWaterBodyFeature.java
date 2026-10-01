package com.gregtech.gregtech.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;
import java.util.Set;

/**
 * Port of GT6's three water-body passes ({@code Loader_Worldgen:576-578}):
 * {@code WorldgenOcean("ocean.seawater")}, {@code WorldgenRiver("river.riverwater")} and
 * {@code WorldgenSwamp("swamp.dirtywater")}.
 *
 * <p>Each pass walks every column of a chunk from {@code waterLevel} down to bedrock: an opaque
 * block ends the column, every water block on the way down is replaced with GT6's own water fluid
 * block (so a whole ocean column becomes sea water, not just its surface). The pass only runs when
 * the chunk touches the matching biome: oceans, rivers (but not a chunk that also touches an ocean)
 * and swamps.
 *
 * <p>GT6 registers the three in a fixed order — ocean, then river, then swamp — and says so in the
 * source ("IT IS IMPORTANT THAT OCEAN COMES BEFORE RIVER AND SWAMP"). The port keeps that by
 * running all three passes from one feature, in the same order, so a chunk that touches both a
 * river and a swamp ends up with swamp water, exactly like GT6. The swamp pass also converts GT6's
 * own waterlike blocks ({@code tBlock instanceof BlockWaterlike}), i.e. water an earlier pass
 * already turned into sea or river water.
 */
public class GTWaterBodyFeature extends Feature<NoneFeatureConfiguration> {
    /** GT6's {@code mHeight = WD.waterLevel()} — the default overworld water level. */
    public static final int DEFAULT_WATER_LEVEL = 62;

    /** One GT6 water-body pass: the registration name, the fluid block and the biome gate. */
    public enum Kind {
        /** {@code WorldgenOcean("ocean.seawater")} — {@code BIOMES_OCEAN}. */
        OCEAN("ocean.seawater", "gregtech:seawater"),
        /** {@code WorldgenRiver("river.riverwater")} — {@code BIOMES_RIVER} without {@code BIOMES_OCEAN}. */
        RIVER("river.riverwater", "gregtech:riverwater"),
        /** {@code WorldgenSwamp("swamp.dirtywater")} — {@code BIOMES_SWAMP}. */
        SWAMP("swamp.dirtywater", "gregtech:swampwater");

        private final String name;
        private final String blockId;

        Kind(String name, String blockId) {
            this.name = name;
            this.blockId = blockId;
        }

        public String registrationName() {
            return name;
        }

        public String blockId() {
            return blockId;
        }
    }

    /** GT6's registration order, which is also the order the passes must run in. */
    public static final List<Kind> PASSES = List.of(Kind.OCEAN, Kind.RIVER, Kind.SWAMP);

    public GTWaterBodyFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (level.getLevel().dimension() != Level.OVERWORLD) return false;
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        Set<ResourceLocation> biomes = GTWorldgenBiomes.chunkBiomes(level, minX, minZ);

        boolean placed = false;
        for (Kind kind : PASSES) {
            if (!matches(kind, biomes)) continue;
            BlockState fill = blockState(kind.blockId());
            if (fill == null) continue;
            placed |= convertChunk(level, minX, minZ, level.getSeaLevel(), fill, kind == Kind.SWAMP);
        }
        return placed;
    }

    /** GT6's biome gate for one pass. */
    public static boolean matches(Kind kind, Set<ResourceLocation> biomes) {
        return switch (kind) {
            case OCEAN -> GTWorldgenBiomes.anyOf(biomes, GTWorldgenBiomes.OCEAN);
            case RIVER -> GTWorldgenBiomes.anyOf(biomes, GTWorldgenBiomes.RIVER)
                    && !GTWorldgenBiomes.anyOf(biomes, GTWorldgenBiomes.OCEAN);
            case SWAMP -> GTWorldgenBiomes.anyOf(biomes, GTWorldgenBiomes.SWAMP);
        };
    }

    /** The kind a chunk ends up with after running every pass in order (swamp wins, as in GT6). */
    public static Kind resolve(Set<ResourceLocation> biomes) {
        Kind result = null;
        for (Kind kind : PASSES) {
            if (matches(kind, biomes)) result = kind;
        }
        return result;
    }

    private static boolean convertChunk(WorldGenLevel level, int minX, int minZ, int top,
                                        BlockState fill, boolean waterlike) {
        boolean placed = false;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                placed |= convertColumn(level, minX + i, minZ + j, top, fill, waterlike);
            }
        }
        return placed;
    }

    /**
     * GT6's per-column loop, public so a GameTest can drive a prepared column.
     *
     * @param waterlike true for the swamp pass, which also converts GT6's own water blocks
     * @return true when at least one block was replaced
     */
    public static boolean convertColumn(WorldGenLevel level, int x, int z, int top,
                                        BlockState fill, boolean waterlike) {
        boolean placed = false;
        boolean firstWater = true;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = top; y > level.getMinBuildHeight(); y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            // GT6: an opaque block ends the column, anything else that is not water is skipped.
            if (state.isSolidRender(level, cursor)) break;
            if (!isWater(state, waterlike)) continue;
            if (state.is(fill.getBlock())) continue;
            // GT6 writes metadata 0 for the first water block in a column, then swaps only the
            // block ID for deeper positions (ExtendedBlockStorage.func_150818_a), keeping each
            // deeper fluid level. Replacing every position with fill's default state made
            // flowing/falling water below the surface turn into full source blocks.
            BlockState replacement = firstWater ? fill
                    : fill.setValue(LiquidBlock.LEVEL, state.getValue(LiquidBlock.LEVEL));
            level.setBlock(cursor, replacement, 2);
            placed = true;
            firstWater = false;
        }
        return placed;
    }

    /** GT6's water test: vanilla water, plus GT6's own waterlike blocks in the swamp pass. */
    private static boolean isWater(BlockState state, boolean waterlike) {
        if (state.is(Blocks.WATER)) return true;
        if (!waterlike) return false;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null || !id.getNamespace().equals("gregtech")) return false;
        return id.getPath().equals("seawater") || id.getPath().equals("riverwater")
                || id.getPath().equals("swampwater");
    }

    private static BlockState blockState(String id) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        if (block == null || block == Blocks.AIR) return null;
        if (!(block instanceof LiquidBlock)) return null;
        return block.defaultBlockState();
    }

    /** Registry key of the configured feature (data file {@code worldgen/configured_feature/gt_water_bodies.json}). */
    public static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath("gregtech", "gt_water_bodies"));
}
