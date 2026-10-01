package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.worldgen.GTOreVeins.SmallOre;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
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
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;

        // Per-dimension tables (GT6 GEN_OVERWORLD / GEN_NETHER / GEN_END).
        java.util.List<SmallOre> ores;
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim = level.getLevel().dimension();
        if (dim == net.minecraft.world.level.Level.NETHER) {
            ores = GTOreVeins.NETHER_SMALL_ORES;
        } else if (dim == net.minecraft.world.level.Level.END) {
            ores = GTOreVeins.END_SMALL_ORES;
        } else {
            ores = GTOreVeins.OVERWORLD_SMALL_ORES;
        }

        boolean placedAny = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (SmallOre ore : ores) {
            // Y range stretched into the extended [-64,112] band in the Overworld only.
            int minY = GTWorldgenScale.remapY(level, ore.minY());
            int maxY = Mth.clamp(GTWorldgenScale.remapY(level, ore.maxY()), minY + 1, level.getMaxBuildHeight() - 1);
            int attempts = Math.max(1, ore.amount() / 2 + random.nextInt(1 + ore.amount()) / 2);
            for (int i = 0; i < attempts; i++) {
                cursor.set(minX + random.nextInt(16),
                        minY + random.nextInt(Math.max(1, maxY - minY)),
                        minZ + random.nextInt(16));
                if (level.isOutsideBuildHeight(cursor.getY())) continue;
                placedAny |= GTOreBlockResolver.placeOre(level, cursor, ore.material(), true);
            }
        }
        return placedAny;
    }
}
