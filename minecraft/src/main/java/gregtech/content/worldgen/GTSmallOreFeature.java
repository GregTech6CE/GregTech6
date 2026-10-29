package gregtech.content.worldgen;

import gregtech.content.worldgen.GTOreVeins.SmallOre;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Port of GT6 {@code WorldgenOresSmall}: every small ore type scatters individual {@code oreSmall}
 * blocks in every chunk. Per type and chunk, GT6 placed
 * {@code max(1, amount/2 + rand(amount+1)/2)} blocks at random positions inside the chunk within
 * the type's Y range. Placed once per chunk with no placement modifiers.
 */
public class GTSmallOreFeature extends Feature<NoneFeatureConfiguration> {

    public GTSmallOreFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!gregtech.GregTechConfig.Worldgen.GEOLOGY.enabled()) return false;
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;

        // Built-in tables for the three original dimensions, plus addon small ores for this dimension.
        String dimension = level.getLevel().dimension().location().toString();
        java.util.List<SmallOre> ores = GTOreVeins.activeSmallOres(dimension);

        boolean placedAny = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (SmallOre ore : ores) {
            // Only the Overworld uses the ore-height curve.
            int minY = GTWorldgenScale.remapY(level, ore.minY());
            int maxY = Mth.clamp(GTWorldgenScale.remapY(level, ore.maxY()), minY + 1, level.getMaxBuildHeight() - 1);
            int attempts = Math.max(1, ore.amount() / 2 + random.nextInt(1 + ore.amount()) / 2);
            for (int i = 0; i < attempts; i++) {
                cursor.set(
                        minX + random.nextInt(16),
                        minY + random.nextInt(Math.max(1, maxY - minY)),
                        minZ + random.nextInt(16));
                if (level.isOutsideBuildHeight(cursor.getY())) continue;
                placedAny |= GTOreBlockResolver.placeOre(level, cursor, ore.material(), true);
            }
        }
        return placedAny;
    }
}
