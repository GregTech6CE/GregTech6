package gregtech.content.worldgen;

import gregtech.GregTechConfig;
import gregtech.content.worldgen.GTOreVeins.SmallOre;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Port of GT6 {@code WorldgenOresSmall}: every small ore type scatters individual {@code oreSmall}
 * blocks in every chunk. Per type and chunk, GT6 placed
 * {@code max(1, amount/2 + rand(amount+1)/2)} attempts at random positions inside the chunk within
 * the type's Y range. Placed once per chunk with no placement modifiers.
 */
public class GTSmallOreFeature extends Feature<NoneFeatureConfiguration> {

    public GTSmallOreFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!GregTechConfig.Worldgen.GEOLOGY.enabled()) return false;
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;

        // Built-in tables for the three original dimensions, plus addon small ores for this dimension.
        String dimension = level.getLevel().dimension().location().toString();
        var ores = OreSourceCatalog.current().smallOres(dimension);
        var terrain = OreGenerationSummary.terrainHeights(level, context.chunkGenerator());

        boolean placedAny = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        MineralWorldgenRules.OrePlacer placer =
                (x, y, z, material, small) -> GTOreBlockResolver.placeOre(level, cursor.set(x, y, z), material, small);
        for (SmallOre ore : ores) placedAny |= ore.scatter(level.getSeed(), dimension, minX, minZ, terrain, placer);
        return placedAny;
    }
}
