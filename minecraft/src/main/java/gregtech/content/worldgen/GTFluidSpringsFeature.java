package gregtech.content.worldgen;

import static gregtech.content.worldgen.FluidSpringRules.SPRINGS;

import gregtech.GregTechConfig;
import gregtech.content.block.GTGrassBlock;
import gregtech.content.block.OreBlock;
import gregtech.content.registry.GTFluidSprings;
import gregtech.content.worldgen.FluidSpringRules.Spring;
import gregtech.platform.Mc;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.FluidState;

/**
 * Modern overworld feature using the shared seven-kind spring catalog and crater writer.
 * One kind is selected before its probability roll. Surface indicators require a generated field.
 */
public class GTFluidSpringsFeature extends Feature<NoneFeatureConfiguration> {

    /** GT6 plants six indicator patches. */
    public static final int INDICATOR_PATCHES = 6;

    public GTFluidSpringsFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!GregTechConfig.Worldgen.FLUID_SPRINGS.enabled()) return false;
        WorldGenLevel level = context.level();
        long seed = WorldgenHash.at(
                level.getSeed() ^ 0x535052494E47L,
                context.origin().getX() >> 4,
                context.origin().getZ() >> 4);
        RandomSource random = RandomSource.create(seed);
        if (level.getLevel().dimension() != Level.OVERWORLD) return false;
        Spring spring = SPRINGS.get(random.nextInt(SPRINGS.size()));
        // GT6: `aRandom.nextInt(mProbability) != 0` → return.
        if (random.nextInt(Math.max(1, GregTechConfig.Number.SPRING_PROBABILITY.value() * (spring.probability() / 100)))
                != 0) return false;
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        if (GTBedrockOreFeature.reservesSpringSite(level, context.chunkGenerator(), new ChunkPos(context.origin())))
            return false;
        if (!carve(level, minX, minZ, spring, random)) return false;
        indicators(level, minX, minZ, spring, RandomSource.create(seed ^ 0x4752415353L));
        return true;
    }

    /**
     * GT6's crater loop ({@code for (int i = 0; i &lt;= 6; i++)}): the filler layer at {@code i + 1}
     * where the space is free, the spring's block at layer {@code i} for {@code i &gt; 0}, and a
     * spring tile entity every 1/16 columns that sit on bedrock.
     */
    private static boolean carve(WorldGenLevel level, int minX, int minZ, Spring spring, RandomSource random) {
        int base = level.getMinBuildHeight();
        // WorldgenFluidSpring shares the chunk with bedrock ores only when no deposit has claimed it.
        if (!GTBedrockOreFeature.canPlaceSpringField(level, minX, minZ)) return false;
        BlockState fill = blockState(spring.blockId());
        if (fill == null) return false;
        Block filler = level.getLevel().dimension() == Level.NETHER ? Blocks.NETHERRACK : Blocks.DEEPSLATE;
        return FluidSpringRules.carve(minX, minZ, random::nextInt, new FluidSpringRules.CraterSink() {

            public void filler(int x, int y, int z) {
                BlockPos p = new BlockPos(x, base + y, z);
                BlockState host = level.getBlockState(p);
                if (!host.hasBlockEntity() && !OreBlock.isBedrockFloor(host) && !host.isSolidRender(level, p))
                    level.setBlock(p, filler.defaultBlockState(), 2);
            }

            public boolean fluid(int x, int y, int z) {
                BlockPos p = new BlockPos(x, base + y, z);
                BlockState host = level.getBlockState(p);
                if (OreBlock.isBedrockOre(host) || host.hasBlockEntity()) return false;
                // The vanilla floor can reach four blocks above the basal layer. At the rim
                // the fluid column is shorter, leaving additional bedrock above the crater roof.
                if (y == 1) {
                    for (int dy = 2; dy <= 4 && base + dy < level.getMaxBuildHeight(); dy++) {
                        var cap = new BlockPos(x, base + dy, z);
                        if (level.getBlockState(cap).is(Blocks.BEDROCK))
                            level.setBlock(cap, filler.defaultBlockState(), 2);
                    }
                }
                if (!level.setBlock(p, fill, 2)) return false;
                // WorldGenRegion does not run LiquidBlock.onPlace; schedule the fluid so gas
                // crater fill starts rising once the chunk is live (see GTDungeonData#set).
                FluidState fluid = fill.getFluidState();
                if (!fluid.isEmpty())
                    level.scheduleTick(p, fluid.getType(), fluid.getType().getTickDelay(level));
                return true;
            }

            public boolean bedrock(int x, int z) {
                return isBedrock(level, x, z);
            }

            public boolean spring(int x, int z) {
                return placeSpring(level, new BlockPos(x, base, z), spring);
            }
        });
    }

    private static boolean isBedrock(WorldGenLevel level, int x, int z) {
        return level.getBlockState(new BlockPos(x, level.getMinBuildHeight(), z))
                .is(Blocks.BEDROCK);
    }

    /** Places GT6's spring tile entity on a bedrock column. */
    private static boolean placeSpring(WorldGenLevel level, BlockPos pos, Spring spring) {
        var variant = GTFluidSprings.byFluid(spring.fluidId());
        return variant != null
                && pos.getY() == level.getMinBuildHeight()
                && level.getBlockState(pos).is(Blocks.BEDROCK)
                && level.setBlock(pos, variant.defaultBlockState(), 2);
    }

    /** GT6's surface indicator: up to six patches of GT6 grass above the field. */
    private static boolean indicators(WorldGenLevel level, int minX, int minZ, Spring spring, RandomSource random) {
        Block indicator = indicatorBlock(spring.indicatorType());
        if (indicator == null) return false;
        boolean placed = false;
        for (int patch = 0; patch < INDICATOR_PATCHES; patch++) {
            int x = minX + 4 + random.nextInt(8);
            int z = minZ + 4 + random.nextInt(8);
            BlockPos surface = GTSurfaceFloraFeature.surface(level, x, z);
            if (surface == null || !isIndicatorGround(level.getBlockState(surface))) continue;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (!random.nextBoolean()) continue;
                    BlockPos pos = surface.offset(dx, 0, dz);
                    if (isIndicatorGround(level.getBlockState(pos))) {
                        placed |= level.setBlock(pos, indicator.defaultBlockState(), 2);
                    }
                }
            }
        }
        return placed;
    }

    private static boolean isIndicatorGround(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.getBlock() instanceof GTGrassBlock;
    }

    /**
     * GT6's {@code mIndicatorType}: 1 and 2 use yellow and brown grass, 3 the plain green one
     * ({@code mIndicatorType == 3 ? 0 : 3 + mIndicatorType} are GT6's grass metas 4/5/0). The port's
     * icon set carries those textures as {@code grassblock_yellow}, {@code grassblock_brown} and
     * {@code grassblock_normal}.
     */
    private static Block indicatorBlock(int indicatorType) {
        String id = switch (indicatorType) {
            case 1 -> "grassblock_yellow";
            case 2 -> "grassblock_brown";
            case 3 -> "grassblock_normal";
            default -> null;
        };
        if (id == null) return null;
        Block block = Mc.block(ResourceLocation.fromNamespaceAndPath("gt6ce", id));
        return block == Blocks.AIR ? null : block;
    }

    private static BlockState blockState(String id) {
        Block block = Mc.block(ResourceLocation.parse(id));
        return block == null || block == Blocks.AIR ? null : block.defaultBlockState();
    }
}
