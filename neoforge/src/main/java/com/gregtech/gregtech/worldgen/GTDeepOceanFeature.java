package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.OreHostStone;
import com.gregtech.gregtech.block.stone.GTStoneBlock;
import com.gregtech.gregtech.block.stone.StoneType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

/**
 * Port of GT6's {@code WorldgenDeepOcean} ({@code Loader_Worldgen:580}, registered as
 * {@code "ocean.prismacorals"}).
 *
 * <p>Only in the deep ocean. GT6 picks one random column of the chunk at {@code 3 + rand(9)},
 * {@code 30 + rand(9)} and requires water there; a 16-way noise sample at
 * {@code (minX + 8, 32, minZ + 8)} then decides what grows: values 12 and 13 build a
 * <b>dark prismarine</b> pylon, 14 and 15 a <b>light prismarine</b> one, everything else leaves the
 * ocean alone.
 *
 * <p>The pylon is a double-ended cone around the water column: four tiers of square rings mirrored
 * above and below {@code j}, one block wide at the top (radius 0 for {@code l = 8..10}), then
 * radius 1 ({@code l = 5..7}), radius 2 ({@code l = 2..4}) and radius 3 ({@code l = 0..1}). Every
 * prismarine block has a 1-in-8 chance of being replaced by an ore of the pylon's metal —
 * garnierite for the dark prismarine, pyrolusite for the light one.
 */
public class GTDeepOceanFeature extends Feature<NoneFeatureConfiguration> {
    /** GT6's noise bound: {@code new NoiseGenerator(world).get(minX + 8, 32, minZ + 8, 16)}. */
    public static final int NOISE_OPTIONS = 16;
    public static final int NOISE_XZ = 8;
    public static final float NOISE_Y = 32.0F;
    /** GT6's two pylon kinds: dark prismarine for noise 12-13, light for 14-15. */
    public static final int DARK_NOISE_MIN = 12;
    public static final int DARK_NOISE_MAX = 13;
    public static final int LIGHT_NOISE_MIN = 14;
    public static final int LIGHT_NOISE_MAX = 15;
    /** Every prismarine block rolls {@code nextInt(8) == 0} for an ore. */
    public static final int ORE_CHANCE = 8;

    /** One GT6 pylon tier: {@code l} range plus the ring radius it fills ({@code m}/{@code n}). */
    public record Tier(int fromL, int toL, int radius) {}

    /** GT6's four tiers, in source order. */
    public static final Tier[] TIERS = MineralWorldgenRules.PYLON_TIERS.stream().map(row->new Tier(row.fromL(),row.toL(),row.radius())).toArray(Tier[]::new);

    public GTDeepOceanFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        if (level.getLevel().dimension() != Level.OVERWORLD) return false;
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        // GT6 requires the deep ocean specifically (BiomeGenBase.deepOcean).
        if (!GTWorldgenBiomes.anyOf(GTWorldgenBiomes.chunkBiomes(level, minX, minZ),
                java.util.Set.of(ResourceLocation.withDefaultNamespace("deep_ocean")))) {
            return false;
        }

        int i = 3 + random.nextInt(9);
        int j = 30 + random.nextInt(9);
        int k = 3 + random.nextInt(9);
        BlockPos column = new BlockPos(minX + i, j, minZ + k);
        if (!isWater(level.getBlockState(column))) return false;

        GTCellNoise noise = new GTCellNoise(level.getSeed());
        int pick = noise.get(minX + NOISE_XZ, NOISE_Y, minZ + NOISE_XZ, NOISE_OPTIONS);
        if (pick >= DARK_NOISE_MIN && pick <= DARK_NOISE_MAX) {
            return buildPylon(level, column, StoneType.PRISMARINE_DARK, "Garnierite", random);
        }
        if (pick >= LIGHT_NOISE_MIN && pick <= LIGHT_NOISE_MAX) {
            return buildPylon(level, column, StoneType.PRISMARINE_LIGHT, "Pyrolusite", random);
        }
        return false;
    }

    /**
     * GT6's pylon loop, public so a GameTest can build one on a prepared water column.
     *
     * @param oreMaterial GT6's ore for this prismarine kind ({@code ores_normal[14]} garnierite for
     *                    dark, {@code ores_normal[13]} pyrolusite for light)
     */
    public static boolean buildPylon(WorldGenLevel level, BlockPos column, StoneType prismarine,
                                     String oreMaterial, RandomSource random) {
        BlockState stone = blockState(prismarine);
        if (stone == null) return false;
        GTMaterial ore = com.gregtech.gregtech.api.material.GTMaterialRegistry.get(oreMaterial);
        boolean placed = false;
        for (Tier tier : TIERS) {
            for (int l = tier.fromL(); l < tier.toL() + 1; l++) {
                for (int m = -tier.radius(); m <= tier.radius(); m++) {
                    for (int n = -tier.radius(); n <= tier.radius(); n++) {
                        placed |= setPylonBlock(level, column.offset(m, l, n), stone, ore, random);
                        placed |= setPylonBlock(level, column.offset(m, -l, n), stone, ore, random);
                    }
                }
            }
        }
        return placed;
    }

    /**
     * One GT6 pylon block: prismarine, or the ore of the pylon with a 1-in-8 chance.
     *
     * <p>GT6 sprinkles {@code BlocksGT.ores_normal[13|14]} — the ore variant whose background is
     * <b>normal stone</b> — into the pylon, so the port places the ore with an explicit
     * {@link OreHostStone#STONE} background instead of the prismarine it replaces (§58, §103.B).
     */
    private static boolean setPylonBlock(WorldGenLevel level, BlockPos pos, BlockState stone,
                                         GTMaterial ore, RandomSource random) {
        if (level.isOutsideBuildHeight(pos)) return false;
        level.setBlock(pos, stone, 2);
        if (ore != null && random.nextInt(ORE_CHANCE) == 0) {
            return GTOreBlockResolver.placeOre(level, pos, OreHostStone.STONE, ore, false);
        }
        return true;
    }

    /** GT6's {@code WD.anywater}: vanilla water or GT's own sea water (§49). */
    public static boolean isWater(BlockState state) {
        if (state.is(Blocks.WATER)) return true;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && id.getNamespace().equals("gregtech")
                && (id.getPath().equals("seawater") || id.getPath().equals("riverwater")
                    || id.getPath().equals("swampwater"));
    }

    /** The GT stone block of a stone type: stone blocks are registered as {@code <type>_<variant>}. */
    private static BlockState blockState(StoneType type) {
        return com.gregtech.gregtech.registry.GTBlocks.getStoneState(
                type, com.gregtech.gregtech.block.stone.StoneVariant.STONE);
    }

    /** Registry key of the configured feature (data file {@code worldgen/configured_feature/gt_deep_ocean.json}). */
    public static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath("gregtech", "gt_deep_ocean"));
}
