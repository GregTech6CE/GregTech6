package gregtech.content.worldgen;

import gregtech.GregTechConfig;
import gregtech.content.material.generated.OreMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** GT6 WorldgenColtan places a bedrock core at the centre of one field per world seed. */
public final class GTColtanFeature extends Feature<NoneFeatureConfiguration> {
    public GTColtanFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        if (!GregTechConfig.Worldgen.GEOLOGY.enabled() || level.getLevel().dimension() != Level.OVERWORLD) return false;
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        var center = MineralWorldgenRules.coltanCentre(level.getSeed());
        long seed = MineralWorldgenRules.coltanFieldSeed(level.getSeed(), minX, minZ);
        boolean placed = (center.x() & ~15) == minX
                && (center.z() & ~15) == minZ
                && GTBedrockOreFeature.placeVein(
                        level,
                        RandomSource.create(MineralWorldgenRules.coltanCoreSeed(seed)),
                        minX,
                        minZ,
                        OreMaterials.Coltan);
        var cursor = new BlockPos.MutableBlockPos();
        var terrain = OreGenerationSummary.terrainHeights(level, context.chunkGenerator());
        return SpecialOreDeposits.scatterColtan(
                        seed,
                        center,
                        minX,
                        minZ,
                        terrain,
                        (x, y, z, material, small) ->
                                GTOreBlockResolver.placeOre(level, cursor.set(x, y, z), material, small))
                || placed;
    }
}
