package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.block.FluidSpringBlock;
import com.gregtech.gregtech.block.OreBlock;
import com.gregtech.gregtech.blockentity.FluidSpringBlockEntity;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Port of GT6's {@code WorldgenFluidSpring} ({@code Loader_Worldgen:782-797}): the oil, gas,
 * geothermal water and lava springs at bedrock.
 *
 * <p>GT6 rolls one spring kind per chunk ({@code 1/probability}: oil 1/400, gas 1/200, geothermal
 * water 1/100, lava 1/200), refuses to run in chunks that carry bedrock ore, and then carves its
 * crater: the bottom seven layers become a shrinking square of the spring's block (oil, gas, water or
 * lava) with a stone/deepslate/deepslate-ish filler above it, and one spring tile entity is placed per
 * bedrock column with a 1/16 chance. Finally it plants up to six patches of indicator grass on the
 * surface so a player can prospect for the field.
 *
 * <p>The port keeps the whole algorithm; the port's diggable "grass" blocks stand in for GT6's grass
 * variants (yellow for gas and lava, brown for oil, plain green for geothermal water — GT6's
 * {@code mIndicatorType} 1/2/3).
 */
public class GTFluidSpringsFeature extends Feature<NoneFeatureConfiguration> {
    /** One GT6 registration. */
    public record Spring(String name, String fluidId, String blockId, int probability, int indicatorType,
                         int amount) {}

    /** {@code Loader_Worldgen:782-788} — the overworld springs, in GT6's order. */
    public static final List<Spring> SPRINGS=FluidSpringRules.SPRINGS.stream().map(v->new Spring(v.name(),v.fluidId(),v.blockId(),v.probability(),v.indicatorType(),v.amount())).toList();

    /** GT6's crater: seven layers below the filler ring, one spring per 16 bedrock columns. */
    public static final int CRATER_LAYERS = 7;
    public static final int SPRING_CHANCE = 16;
    /** GT6's {@code if (i > 2 && aRandom.nextInt(16) == 0)} gate inside the crater loop. */
    public static final int CRATER_SPRING_CHANCE = 16;
    /** GT6 plants six indicator patches. */
    public static final int INDICATOR_PATCHES = 6;

    public GTFluidSpringsFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        if (level.getLevel().dimension() != Level.OVERWORLD) return false;
        Spring spring = SPRINGS.get(random.nextInt(SPRINGS.size()));
        // GT6: `aRandom.nextInt(mProbability) != 0` → return.
        if (random.nextInt(spring.probability()) != 0) return false;
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        if (!hasBedrock(level, minX + 8, minZ + 8)) return false;
        return carve(level, minX, minZ, spring, random);
    }

    /** GT6 checks the chunk centre column for bedrock (and skips chunks with bedrock ore). */
    public static boolean hasBedrock(WorldGenLevel level, int x, int z) {
        BlockState bottom = level.getBlockState(new BlockPos(x, level.getMinBuildHeight(), z));
        return bottom.is(Blocks.BEDROCK) || isBedrockOre(bottom);
    }

    /**
     * GT6's floor test ({@code WorldgenFluidSpring:67}): vanilla bedrock, or a GT bedrock deposit
     * whose ore replaced it — both are a floor. §103.B: this reads the ore's {@code stone} block-state
     * property; the previous registry-id prefix test ({@code "bedrock_ore"}) never matched any
     * registered block, so the check was dead.
     */
    private static boolean isBedrockOre(BlockState state) {
        return OreBlock.isBedrockOre(state);
    }

    /**
     * GT6's crater loop ({@code for (int i = 0; i &lt;= 6; i++)}): the filler layer at {@code i + 1}
     * where the space is free, the spring's block at layer {@code i} for {@code i &gt; 0}, and a
     * spring tile entity every 1/16 columns that sit on bedrock.
     */
    public static boolean carve(WorldGenLevel level, int minX, int minZ, Spring spring, RandomSource random) {
        BlockState fill = blockState(spring.blockId());
        if (fill == null) return false;
        Block filler = level.getLevel().dimension() == Level.NETHER ? Blocks.NETHERRACK : Blocks.DEEPSLATE;
        int base = level.getMinBuildHeight();
        return FluidSpringRules.carve(minX,minZ,random::nextInt,new FluidSpringRules.CraterSink(){
            public void filler(int x,int y,int z){BlockPos p=new BlockPos(x,base+y,z);if(!level.getBlockState(p).isSolidRender(level,p))level.setBlock(p,filler.defaultBlockState(),2);}
            public void fluid(int x,int y,int z){level.setBlock(new BlockPos(x,base+y,z),fill,2);}
            public boolean bedrock(int x,int z){return isBedrock(level,x,z);}
            public boolean spring(int x,int z){return placeSpring(level,new BlockPos(x,base,z),spring);}
        });
    }

    private static boolean isBedrock(WorldGenLevel level, int x, int z) {
        return level.getBlockState(new BlockPos(x, level.getMinBuildHeight(), z)).is(Blocks.BEDROCK);
    }

    /** Places GT6's spring tile entity on a bedrock column. */
    public static boolean placeSpring(WorldGenLevel level, BlockPos pos, Spring spring) {
        var variant=com.gregtech.gregtech.registry.GTFluidSprings.byFluid(spring.fluidId());
        return variant!=null && level.setBlock(pos,variant.defaultBlockState(),2);

    }

    /** GT6's surface indicator: up to six patches of GT6 grass above the field. */
    public static boolean indicators(WorldGenLevel level, int minX, int minZ, Spring spring, RandomSource random) {
        Block indicator = indicatorBlock(spring.indicatorType());
        if (indicator == null) return false;
        boolean placed = false;
        for (int patch = 0; patch < INDICATOR_PATCHES; patch++) {
            int x = minX + 4 + random.nextInt(8);
            int z = minZ + 4 + random.nextInt(8);
            BlockPos surface = GTSurfaceFloraFeature.surface(level, x, z);
            if (surface == null) continue;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (!random.nextBoolean()) continue;
                    BlockPos pos = surface.offset(dx, 0, dz);
                    if (level.getBlockState(pos).is(BlockTags.DIRT)) {
                        placed |= level.setBlock(pos, indicator.defaultBlockState(), 2);
                    }
                }
            }
        }
        return placed;
    }

    /**
     * GT6's {@code mIndicatorType}: 1 and 2 use yellow and brown grass, 3 the plain green one
     * ({@code mIndicatorType == 3 ? 0 : 3 + mIndicatorType} are GT6's grass metas 4/5/0). The port's
     * icon set carries those textures as {@code grassblock_yellow}, {@code grassblock_brown} and
     * {@code grass}.
     */
    public static Block indicatorBlock(int indicatorType) {
        String id = switch (indicatorType) {
            case 1 -> "grassblock_yellow";
            case 2 -> "grassblock_brown";
            default -> "grass";
        };
        Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", id));
        return block == Blocks.AIR ? null : block;
    }

    private static BlockState blockState(String id) {
        Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(id));
        return block == null || block == Blocks.AIR ? null : block.defaultBlockState();
    }

    /** The spring block a placed spring uses (kept for the tests and tools). */
    public static Block springBlock() {
        return GTDecorBlocks.FLUID_SPRING.get();
    }

    /** Registry key of the configured feature (data file {@code worldgen/configured_feature/gt_fluid_springs.json}). */
    public static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath("gregtech", "gt_fluid_springs"));
}
