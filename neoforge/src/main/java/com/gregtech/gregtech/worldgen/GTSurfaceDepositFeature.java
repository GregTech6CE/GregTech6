package com.gregtech.gregtech.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.Nullable;

/**
 * The port's surface clay seams: flat blobs of colored clay at the top of the ground next to water
 * (radius 4-7, two to four blocks deep).
 *
 * <p>PROVENANCE: GT6 has no clay seam pass at all - its only overworld clay generator is
 * {@code WorldgenPit} ({@code Loader_Worldgen:592-597}, ported as {@link GTPitFeature}) and the
 * river placer black sands are {@link GTBlackSandFeature} - so this feature is the port's own
 * substitute for vanilla's round clay discs. Its clay blocks are GT6's {@code BlocksGT.Diggables}
 * metas 1/4/5/6 (Brown/Yellow/Blue/White Clay), i.e. the port's {@code gregtech:clay_brown},
 * {@code gregtech:clay_yellow}, {@code gregtech:clay_blue} and {@code gregtech:clay_white}
 * icon-set blocks; GT6's meta 3 (Red Clay) stays Nether-only ({@link GTNetherDepositFeature}).
 *
 * <p>PORT DIFFERENCE, fixed here: the seam used to start one block <em>below</em> the top ground
 * block whenever that block was not under water ({@code startOffset = underWater ? 0 : 1}), so on
 * dry land every seam kept the grass block it found (or the dirt/sand block of a bare shore) and
 * its clay stayed invisible from the surface. The port's pits never had that behaviour:
 * {@link GTPitFeature#carveColumn} treats the whole {@code #minecraft:dirt} tag as a host, and that
 * tag contains {@code minecraft:grass_block} (vanilla {@code data/minecraft/tags/blocks/dirt.json}),
 * so a pit replaces the surface block. The seam now starts at the top ground block too, which makes
 * both of the port's clay deposits show their clay.
 *
 * <p>NOTE ON GT6: GT6's own pit deliberately keeps a one block cap. {@code WorldgenPit.java:66-74}
 * skips every opaque block that is neither {@code Blocks.dirt} nor sand/clay/ore-sand, and 1.7.10
 * {@code Blocks.grass} is an opaque cube, so the grass block survives and GT6's clay starts at the
 * dirt below it; GT6's changelog 6.05.30 names the result "Clay 'Veins' below Grass". The port keeps
 * that cap nowhere - the port's pit already replaces the surface block, and this seam now does too.
 *
 * <p>Batch 48 removed this feature's black-sand branch: GT6's black sand is river-only
 * ({@code WorldgenBlackSand}, 1 in 64 chunks), so keeping a second beach/desert generator here
 * would have produced magnetite sand where GT6 never does.
 */
public class GTSurfaceDepositFeature extends Feature<NoneFeatureConfiguration> {

    /** GT6 {@code BlocksGT.Diggables} metas 5/1/6/4 - meta 3 (Red Clay) is Nether-only. */
    public static final String[] CLAYS = TerrainWorldgenRules.SEAM_CLAYS.toArray(String[]::new);

    /** The rolled disc: {@code radius = MIN_RADIUS + nextInt(MAX_RADIUS - MIN_RADIUS + 1)}. */
    public static final int MIN_RADIUS = TerrainWorldgenRules.SEAM_MIN_RADIUS;
    public static final int MAX_RADIUS = TerrainWorldgenRules.SEAM_MAX_RADIUS;

    /** The rolled thickness: {@code depth = MIN_DEPTH + nextInt(MAX_DEPTH - MIN_DEPTH + 1)}. */
    public static final int MIN_DEPTH = TerrainWorldgenRules.SEAM_MIN_DEPTH;
    public static final int MAX_DEPTH = TerrainWorldgenRules.SEAM_MAX_DEPTH;

    public GTSurfaceDepositFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        // GT6-ish rarity: roughly one clay seam per ~6 chunks. (GT6's twigs live in
        // GTSurfaceFloraFeature - WorldgenSticks has exactly one registration.)
        if (random.nextInt(6) != 0) return false;

        int x = (origin.getX() & ~15) + 2 + random.nextInt(12);
        int z = (origin.getZ() & ~15) + 2 + random.nextInt(12);
        // The top ground block of the column: Heightmap.Types.OCEAN_FLOOR_WG ignores water, and
        // WorldGenRegion#getHeight is the first block above the highest matching one.
        int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
        if (level.isOutsideBuildHeight(y)) return false;

        BlockPos surface = new BlockPos(x, y, z);
        BlockState surfaceState = level.getBlockState(surface);
        int waterNeighbours = countWater(level, surface);

        if (surfaceState.is(Blocks.CLAY)
                || ((surfaceState.is(Blocks.DIRT) || surfaceState.is(Blocks.GRAVEL) || surfaceState.is(Blocks.GRASS_BLOCK))
                        && waterNeighbours > 0)) {
            BlockState deposit = blockState(CLAYS[random.nextInt(CLAYS.length)]);
            int radius = MIN_RADIUS + random.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
            int depth = MIN_DEPTH + random.nextInt(MAX_DEPTH - MIN_DEPTH + 1);
            if (deposit == null) return false;
            return placeClaySeam(level, x, z, deposit, radius, depth);
        }
        return false;
    }

    /**
     * Clay seam: replaces the sediment from the top ground block downwards, so the seam's own top
     * layer is clay. The port's {@link GTPitFeature#carveColumn} does the same to a pit column,
     * while GT6's {@code WorldgenPit} keeps the grass cap (see the class comment).
     *
     * <p>Public so a GameTest can lay a flat surface and drive the seam directly, since the feature
     * itself only rolls one seam in six chunks.
     *
     * @return true when at least one block was replaced
     */
    public static boolean placeClaySeam(WorldGenLevel level, int x, int z,
                                        BlockState deposit, int radius, int depth) {
        boolean placedAny = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x + dx, z + dz) - 1;
                for (int dy = 0; dy < depth; dy++) {
                    cursor.set(x + dx, top - dy, z + dz);
                    BlockState host = level.getBlockState(cursor);
                    if (isHost(host)) {
                        level.setBlock(cursor, deposit, 2);
                        placedAny = true;
                    }
                }
            }
        }
        return placedAny;
    }

    /**
     * GT6's sediment hosts of {@code WorldgenPit.java:67-69} (dirt, sand, clay) plus gravel, with
     * the {@code #minecraft:dirt} tag the port's own pit column uses - it also covers the grass
     * block of a grass-topped shore, which is what makes the seam reach the surface.
     */
    private static boolean isHost(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.SAND)
                || state.is(Blocks.CLAY) || state.is(Blocks.GRAVEL);
    }

    /** Number of water blocks in the 5x5x2 neighbourhood - 0 = dry, high = river/lake. */
    private static int countWater(WorldGenLevel level, BlockPos pos) {
        int count = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (level.getBlockState(pos.offset(dx, 1, dz)).is(Blocks.WATER)
                        || level.getBlockState(pos.offset(dx, 0, dz)).is(Blocks.WATER)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Nullable
    private static BlockState blockState(String id) {
        Block block = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("gregtech", id));
        return block == null || block == Blocks.AIR ? null : block.defaultBlockState();
    }
}
