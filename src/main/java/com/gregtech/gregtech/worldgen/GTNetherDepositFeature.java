package com.gregtech.gregtech.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nether deposits, ported from GT6 {@code WorldgenNetherClay} + {@code WorldgenNetherCrystals}:
 *
 * <ul>
 *   <li>Red clay bands replacing netherrack just above the lava sea (noise-gated columns).</li>
 *   <li>Crystal ore clusters growing glowstone-style down from cave ceilings; the crystal
 *       kind is chosen regionally by the same cellular noise.</li>
 * </ul>
 */
public class GTNetherDepositFeature extends Feature<NoneFeatureConfiguration> {

    /** The 12 GT6 crystal ore kinds (iconset blocks). */
    private static final String[] CRYSTAL_ORES = NetherDepositRules.CRYSTALS.toArray(String[]::new);

    /** GT6's nether water level ({@code WD.waterLevel} in a no-sky dimension) - the lava sea. */
    public static final int NETHER_WATER_LEVEL = NetherDepositRules.WATER_LEVEL;

    /** GT6's {@code WorldgenNetherQuartz}: two noise layers at {@code 40 + noise(x, 0|64, z, 200)}. */
    public static final int QUARTZ_BASE_Y = NetherDepositRules.QUARTZ_BASE_Y;
    public static final int QUARTZ_OPTIONS = NetherDepositRules.QUARTZ_OPTIONS;
    public static final float[] QUARTZ_NOISE_Y = NetherDepositRules.QUARTZ_NOISE_Y;

    private static final Map<Long, GTCellNoise> NOISE_BY_SEED = new ConcurrentHashMap<>();

    public GTNetherDepositFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        GTCellNoise noise = NOISE_BY_SEED.computeIfAbsent(level.getSeed(), GTCellNoise::new);

        boolean placedAny = placeClay(level, noise, origin);
        placedAny |= placeSeams(level, noise, origin);
        placedAny |= placeCrystals(level, noise, random, origin);
        return placedAny;
    }

    /**
     * Nether quartz in netherrack (GT6 {@code WorldgenNetherQuartz}, {@code Loader_Worldgen:600}).
     *
     * <p>GT6 walks every column of the chunk and uses its value noise at two fixed heights —
     * {@code 40 + noise(x, 0, z, 200)} and {@code 40 + noise(x, 64, z, 200)} — replacing the
     * netherrack it finds there with its nether quartz rock ore (meta 8). Because the noise is
     * sampled at fixed Y values the quartz forms two thin, region-wide layers.
     */
    public boolean placeSeams(WorldGenLevel level, GTCellNoise noise, BlockPos origin) {
        BlockState quartz = blockState("block_ore_netherquartz");
        if (quartz == null) return false;
        int minX = origin.getX() & ~15, minZ = origin.getZ() & ~15;
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                int x = minX + i, z = minZ + j;
                for (float noiseY : QUARTZ_NOISE_Y) {
                    int y = QUARTZ_BASE_Y + noise.get(x, noiseY, z, QUARTZ_OPTIONS);
                    if (level.isOutsideBuildHeight(y)) continue;
                    cursor.set(x, y, z);
                    if (level.getBlockState(cursor).is(Blocks.NETHERRACK)) {
                        level.setBlock(cursor, quartz, 2);
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }

    /** GT6 WorldgenNetherClay: 1/8 noise columns get red clay at lava level +2..+3. */
    private boolean placeClay(WorldGenLevel level, GTCellNoise noise, BlockPos origin) {
        BlockState clay = blockState("clay_red");
        if (clay == null) return false;
        int minX = origin.getX() & ~15, minZ = origin.getZ() & ~15;
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                int x = minX + i, z = minZ + j;
                if (noise.get(x, 42, z, 8) != 0) continue;
                for (int y = NETHER_WATER_LEVEL + 3; y >= NETHER_WATER_LEVEL + 2; y--) {
                    cursor.set(x, y, z);
                    if (level.getBlockState(cursor).is(Blocks.NETHERRACK)) {
                        level.setBlock(cursor, clay, 2);
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }

    /**
     * GT6 {@code WorldgenNetherCrystals}: glowstone-like clusters under cave ceilings, regional kind.
     *
     * <p>GT6 first rolls {@code aRandom.nextBoolean()} (half the chunks do nothing), walks up from the
     * lava sea to the ceiling, and then:
     * <ul>
     *   <li>rejects a ceiling that is a nether brick block or not {@code Material.rock};</li>
     *   <li>requires at least 11 blocks of air above the lava sea ({@code --aY - 10 < waterLevel});</li>
     *   <li>replaces the <b>ceiling block itself</b> with the crystal ({@code setBlock(aX, aY, aZ)}) —
     *       not the air below it — which then grows into air blocks that already touch a crystal.</li>
     * </ul>
     */
    private boolean placeCrystals(WorldGenLevel level, GTCellNoise noise, RandomSource random, BlockPos origin) {
        if (random.nextBoolean()) return false;
        int x = (origin.getX() & ~15) + random.nextInt(16);
        int z = (origin.getZ() & ~15) + random.nextInt(16);
        int kind = noise.get(x / 2.0F, 360, z / 2.0F, CRYSTAL_ORES.length);
        BlockState crystal = blockState(CRYSTAL_ORES[kind]);
        if (crystal == null) return false;
        return placeCrystalAt(level, x, z, crystal, random);
    }

    /**
     * GT6's crystal pass for one column, public so a GameTest can drive a prepared ceiling.
     *
     * @return true when a crystal cluster was seeded
     */
    public static boolean placeCrystalAt(WorldGenLevel level, int x, int z, BlockState crystal,
                                        RandomSource random) {
        // Ascend through air from the lava sea to the first ceiling (GT6: while (air(++y)));
        int y = NETHER_WATER_LEVEL;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, y, z);
        while (y < level.getMaxBuildHeight() - 2 && level.getBlockState(cursor.set(x, y + 1, z)).isAir()) y++;
        BlockState ceiling = level.getBlockState(cursor.set(x, y + 1, z));
        // GT6: nether bricks are rejected, and the ceiling must be rock.
        if (ceiling.is(Blocks.NETHER_BRICKS) || !isRockMaterial(ceiling)) return false;
        // GT6: at least 11 blocks of air above the lava sea (the Nether's water level is 31).
        if (y - 10 < NETHER_WATER_LEVEL) return false;

        // GT6 replaces the ceiling block itself, not the air block below it.
        level.setBlock(cursor.set(x, y + 1, z), crystal, 2);
        int seedY = y + 1;
        NetherDepositRules.growCrystal(x,seedY,z,random::nextInt,new NetherDepositRules.CrystalSink(){
            public boolean air(int px,int py,int pz){return level.getBlockState(new BlockPos(px,py,pz)).isAir();}
            public int crystalNeighbors(int px,int py,int pz){int count=0;BlockPos pos=new BlockPos(px,py,pz);for(Direction dir:Direction.values())if(level.getBlockState(pos.relative(dir)).is(crystal.getBlock()))count++;return count;}
            public void place(int px,int py,int pz){level.setBlock(new BlockPos(px,py,pz),crystal,2);}
        });
        return true;
    }

    /**
     * 1.7.10 {@code Material.rock} for the ceiling test: netherrack and friends, blackstone, basalt,
     * the GT stones, obsidian, magma and the nether ores — but not nether bricks (which GT6 rejects
     * explicitly) and not glass-like blocks such as glowstone or the crystals themselves.
     */
    public static boolean isRockMaterial(BlockState state) {
        if (state.is(Blocks.NETHER_BRICKS) || state.is(Blocks.GLOWSTONE)) return false;
        return state.is(net.minecraft.tags.BlockTags.BASE_STONE_NETHER)
                || state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)
                || state.is(net.minecraft.tags.BlockTags.STONE_BRICKS)
                || state.is(Blocks.DEEPSLATE) || state.is(Blocks.OBSIDIAN) || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.ANCIENT_DEBRIS) || state.is(Blocks.NETHER_QUARTZ_ORE)
                || state.getBlock() instanceof com.gregtech.gregtech.block.stone.GTStoneBlock;
    }

    @Nullable
    private static BlockState blockState(String id) {
        Block block = ForgeRegistries.BLOCKS.getValue(
                new ResourceLocation(com.gregtech.gregtech.GregTech.NAMESPACE, id));
        return block == null || block == Blocks.AIR ? null : block.defaultBlockState();
    }
}
