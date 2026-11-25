package gregtech.content.worldgen;

import gregtech.GregTechConfig;
import gregtech.api.material.GTMaterial;
import gregtech.content.block.FluidSpringBlock;
import gregtech.content.block.OreBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Independent attempts per mineral and chunk, with a shared floor for natural and special deposits. */
public final class GTBedrockOreFeature extends Feature<NoneFeatureConfiguration> {
    public GTBedrockOreFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        var dimension = level.getLevel().dimension();
        var ordinary = dimension == Level.OVERWORLD
                ? OreGenerationSummary.ordinarySources(level, context.chunkGenerator())
                : null;
        String dimensionId = dimension.location().toString();
        var generation = GTBedrockOreDimensions.generation(dimensionId, ordinary);
        var ores = generation.ores();
        if (ores.isEmpty()) return false;
        var chunk = new ChunkPos(context.origin());
        long seed = MineralWorldgenRules.bedrockSeed(level.getSeed(), dimensionId);
        boolean underRoof = dimension == Level.NETHER;
        int terrainTop = level.getMaxBuildHeight() - 1;
        if (underRoof && context.chunkGenerator() instanceof NoiseBasedChunkGenerator generator)
            terrainTop = Math.min(terrainTop, generator.getMinY() + generator.getGenDepth() - 1);
        boolean placed = false;
        for (var ore : ores) {
            if (!MineralWorldgenRules.bedrockHit(
                    seed, chunk.x, chunk.z, ore.name(), ore.chance(), generation.density())) continue;
            if (!placeVein(level, context.random(), chunk.getMinBlockX(), chunk.getMinBlockZ(), ore.material()))
                continue;
            BedrockIndicatorPlacement.place(level, chunk, ore, context.random(), terrainTop, underRoof);
            placed = true;
        }
        return placed;
    }

    /** Common placement for naturally selected, dungeon and coltan veins. */
    public static boolean placeVein(WorldGenLevel level, RandomSource random, int minX, int minZ, GTMaterial material) {
        if (!GregTechConfig.Worldgen.BEDROCK_ORES.enabled() || material == null) return false;
        int floor = level.getMinBuildHeight();
        var cursor = new BlockPos.MutableBlockPos();
        BedrockOccupancy occupied = floorContents(level, cursor, minX, minZ, floor);
        if (occupied == BedrockOccupancy.SPRING) return false;
        boolean prepare = occupied == BedrockOccupancy.NONE;
        var defaultStone = level.getLevel().dimension() == Level.NETHER
                ? Blocks.NETHERRACK.defaultBlockState()
                : Blocks.DEEPSLATE.defaultBlockState();
        return MineralWorldgenRules.bedrockVein(
                minX,
                minZ,
                floor,
                Math.min(level.getSeaLevel(), level.getMaxBuildHeight()),
                prepare,
                random::nextInt,
                new MineralWorldgenRules.VeinSink() {
                    @Override
                    public boolean isBedrockFloor(int x, int y, int z) {
                        return OreBlock.isBedrockFloor(level.getBlockState(cursor.set(x, y, z)));
                    }

                    @Override
                    public void prepareDrillPad(int x, int floor, int z) {
                        // The drill needs a 3x3 row directly on the basal floor.
                        for (int dx = -1; dx <= 1; dx++)
                            for (int dz = -1; dz <= 1; dz++)
                                for (int y = floor + 1; y <= floor + 4 && !level.isOutsideBuildHeight(y); y++) {
                                    var pos = cursor.set(x + dx, y, z + dz);
                                    if (level.getBlockState(pos).is(Blocks.BEDROCK))
                                        level.setBlock(pos, defaultStone, Block.UPDATE_CLIENTS);
                                }
                    }

                    @Override
                    public void prepareStone(int x, int y, int z) {
                        if (level.isOutsideBuildHeight(y)) return;
                        var pos = cursor.set(x, y, z);
                        var state = level.getBlockState(pos);
                        if (!state.hasBlockEntity()
                                && (state.isAir()
                                        || state.is(Blocks.BEDROCK)
                                        || GTOreBlockResolver.stoneOf(state) != null))
                            level.setBlock(pos, defaultStone, Block.UPDATE_CLIENTS);
                    }

                    @Override
                    public boolean bedrock(int x, int y, int z, boolean small) {
                        var pos = cursor.set(x, y, z);
                        var state = level.getBlockState(pos);
                        if (!small && state.getBlock() instanceof OreBlock ore && OreBlock.isBedrockOre(state))
                            return !ore.isSmall() && ore.material().resolve() == material.resolve();
                        return GTOreBlockResolver.placeBedrockOre(level, pos, material, small);
                    }

                    @Override
                    public boolean ore(int x, int y, int z, boolean small) {
                        return !level.isOutsideBuildHeight(y)
                                && GTOreBlockResolver.placeOre(level, cursor.set(x, y, z), material, small);
                    }
                });
    }

    static boolean canPlaceSpringField(WorldGenLevel level, int minX, int minZ) {
        int floor = level.getMinBuildHeight();
        var cursor = new BlockPos.MutableBlockPos();
        return level.getBlockState(cursor.set(minX + 8, floor, minZ + 8)).is(Blocks.BEDROCK)
                && floorContents(level, cursor, minX, minZ, floor) == BedrockOccupancy.NONE;
    }

    private static BedrockOccupancy floorContents(
            WorldGenLevel level, BlockPos.MutableBlockPos cursor, int minX, int minZ, int floor) {
        BedrockOccupancy result = BedrockOccupancy.NONE;
        // The spring crater can place outlets from x/z 3 through 12. Ore cores sit inside that footprint.
        for (int x = 3; x <= 12; x++) {
            for (int z = 3; z <= 12; z++) {
                var state = level.getBlockState(cursor.set(minX + x, floor, minZ + z));
                if (state.getBlock() instanceof FluidSpringBlock) return BedrockOccupancy.SPRING;
                if (OreBlock.isBedrockOre(state)) result = BedrockOccupancy.ORE;
            }
        }
        return result;
    }

    private enum BedrockOccupancy {
        NONE,
        ORE,
        SPRING
    }
}
