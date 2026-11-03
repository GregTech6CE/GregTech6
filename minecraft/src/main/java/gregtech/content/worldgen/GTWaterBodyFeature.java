package gregtech.content.worldgen;

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
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}

import java.util.List;
import java.util.Set;

/**
 * Port of GT6's ocean and river water-body passes ({@code Loader_Worldgen:576-578}):
 * {@code WorldgenOcean("ocean.seawater")} and {@code WorldgenRiver("river.riverwater")}.
 * {@code WorldgenSwamp("swamp.dirtywater")} is not ported because the port no longer registers
 * dirty water; swamp columns keep vanilla water.
 *
 * <p>Each pass walks every column of a chunk from {@code waterLevel} down to bedrock: an opaque
 * block ends the column, every vanilla water block on the way down is replaced with GT6's own water
 * fluid block (so a whole ocean column becomes sea water, not just its surface). The pass only runs
 * when the chunk touches the matching biome: oceans, and rivers (but not a chunk that also touches
 * an ocean).
 *
 * <p>GT6 registers ocean before river ("IT IS IMPORTANT THAT OCEAN COMES BEFORE RIVER AND SWAMP").
 * The port keeps that by running both passes from one feature, in the same order.
 */
public class GTWaterBodyFeature extends Feature<NoneFeatureConfiguration> {
    /** GT6's {@code mHeight = WD.waterLevel()} — the default overworld water level. */
    public static final int DEFAULT_WATER_LEVEL = 62;

    /** One GT6 water-body pass: the registration name, the fluid block and the biome gate. */
    public enum Kind {
        /** {@code WorldgenOcean("ocean.seawater")} — {@code BIOMES_OCEAN}. */
        OCEAN("ocean.seawater", "gt6ce:seawater"),
        /** {@code WorldgenRiver("river.riverwater")} — {@code BIOMES_RIVER} without {@code BIOMES_OCEAN}. */
        RIVER("river.riverwater", "gt6ce:riverwater");

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
    public static final List<Kind> PASSES = List.of(Kind.OCEAN, Kind.RIVER);

    public GTWaterBodyFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!gregtech.GregTechConfig.Worldgen.WATER_BODIES.enabled()) return false;
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
            placed |= convertChunk(level, minX, minZ, switch (kind) { case OCEAN -> gregtech.GregTechConfig.Number.OCEAN_HEIGHT.value(); case RIVER -> gregtech.GregTechConfig.Number.RIVER_HEIGHT.value(); }, fill);
        }
        return placed;
    }

    /** GT6's biome gate for one pass. */
    public static boolean matches(Kind kind, Set<ResourceLocation> biomes) {
        return switch (kind) {
            case OCEAN -> GTWorldgenBiomes.anyOf(biomes, GTWorldgenBiomes.OCEAN);
            case RIVER -> GTWorldgenBiomes.anyOf(biomes, GTWorldgenBiomes.RIVER)
                    && !GTWorldgenBiomes.anyOf(biomes, GTWorldgenBiomes.OCEAN);
        };
    }

    /** Last admitted chunk pass. */
    public static Kind resolve(Set<ResourceLocation> biomes) {
        Kind result = null;
        for (Kind kind : PASSES) {
            if (matches(kind, biomes)) result = kind;
        }
        return result;
    }

    private static boolean convertChunk(WorldGenLevel level, int minX, int minZ, int top, BlockState fill) {
        boolean placed = false;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                placed |= convertColumn(level, minX + i, minZ + j, top, fill);
            }
        }
        return placed;
    }

    /**
     * GT6's per-column loop, split out so a prepared column can be driven directly.
     *
     * @return true when at least one block was replaced
     */
    public static boolean convertColumn(WorldGenLevel level, int x, int z, int top, BlockState fill) {
        boolean placed = false;
        boolean firstWater = true;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = top; y > level.getMinBuildHeight(); y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            // GT6: an opaque block ends the column, anything else that is not water is skipped.
            if (state.isSolidRender(level, cursor)) break;
            if (!state.is(Blocks.WATER)) continue;
            if (state.is(fill.getBlock())) continue;
            // GT6 writes metadata 0 for the first water block in a column, then swaps only the
            // block ID for deeper positions (ExtendedBlockStorage.func_150818_a), keeping each
            // deeper fluid level. Replacing every position with fill's default state made
            // flowing/falling water below the surface turn into full source blocks.
            BlockState replacement = firstWater ? fill
                    : fill.setValue(LiquidBlock.LEVEL, state.getValue(LiquidBlock.LEVEL));
            placed |= level.setBlock(cursor, replacement, 2);
            // Preserve the first encountered eligible water position even if its write fails.
            firstWater = false;
        }
        return placed;
    }

    private static BlockState blockState(String id) {
//? if >=1.21.1 {
        /*Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
*///?} else {
        Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(id));
//?}
        if (block == null || block == Blocks.AIR) return null;
        if (!(block instanceof LiquidBlock)) return null;
        return block.defaultBlockState();
    }

    /** Registry key of the configured feature (data file {@code worldgen/configured_feature/gt_water_bodies.json}). */
    public static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath("gt6ce", "gt_water_bodies"));
}
