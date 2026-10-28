package gregtech.content.worldgen;

import static gregtech.GregTechConfig.Number.*;
import static gregtech.GregTechConfig.Rule.*;

import gregtech.content.worldgen.center.NativeOriginWorld;
import gregtech.content.worldgen.center.OriginWorld;
import gregtech.content.worldgen.center.SourceCenterBiomes;
import gregtech.content.worldgen.center.SourceNexus;
import gregtech.content.worldgen.center.SourceStreets;
import java.util.HashSet;
import java.util.Random;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Original GT6 Nexus and axis roads; independently configurable origin facilities. */
public final class GTOriginFeature extends Feature<NoneFeatureConfiguration> {

    public GTOriginFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        boolean nexusEnabled = gregtech.GregTechConfig.Worldgen.NEXUS.enabled();
        if (!nexusEnabled && !STREETS.enabled() && !CENTER_BIOMES.enabled() && !BEACON.enabled() && !TESTING.enabled())
            return false;
        var level = context.level();
        if (level.getLevel().dimension() != Level.OVERWORLD) return false;
        ChunkPos chunk = new ChunkPos(context.origin());
        int minX = chunk.getMinBlockX(), minZ = chunk.getMinBlockZ();
        boolean plaza = minX >= -32 && minX <= 16 && minZ >= -32 && minZ <= 16;
        boolean nexus = minX == 16 && minZ == -48;
        boolean road = minX == -16 || minX == 0 || minZ == -16 || minZ == 0;
        boolean center = minX >= -96 && minX <= 80 && minZ >= -96 && minZ <= 80;
        // Legacy development worlds reserved this quadrant for test platforms and item caches.
        boolean testing = (minX == 32 || minX == 48) && (minZ == -32 || minZ == -48);
        if (!center && !plaza && !nexus && !road) return false;
        // WD.waterLevel() is 62 in GT6, while the modern sea-level parameter is 63.
        int height = CENTER_HEIGHT.value();
        NativeOriginWorld world = new NativeOriginWorld(level, chunk, height);
        var biomes = new HashSet<String>();
        if (road && !plaza) {
            // The old generator rewrites both halves of a segment together. Read the complete
            // segment's biomes so modern independent half chunks choose the same bridge mode.
            boolean northSouth = minX == -16 || minX == 0;
            for (int along = 0; along < 16; along++)
                for (int across = -16; across < 16; across++)
                    biomes.add(world.getBiomeGenForCoords(
                                    northSouth ? across : minX + along, northSouth ? minZ + along : across)
                            .biomeName());
        }
        var planChunk = new OriginWorld.Chunk(world, minX, minZ);
        boolean placed = CENTER_BIOMES.enabled()
                && center
                && new SourceCenterBiomes(height)
                        .generate(world, planChunk, minX, minZ, new Random(level.getSeed() ^ chunk.toLong()));
        SourceStreets streets = new SourceStreets(height, BEACON.enabled(), CENTER_BIOMES.enabled(), nexusEnabled);
        // The old generator writes a 64x64 plaza from its four centre chunks. Replaying that plan
        // per intersecting chunk with clipped writes preserves all 16 chunks in any generation order.
        placed |= STREETS.enabled()
                && (plaza
                        ? streets.generate(world, -16, -16, -1, -1, biomes)
                        : road && streets.generate(world, minX, minZ, minX + 15, minZ + 15, biomes));
        if (nexus && nexusEnabled)
            placed |= new SourceNexus(height, STREETS.enabled())
                    .generate(
                            world,
                            new OriginWorld.Chunk(world, minX, minZ),
                            minX,
                            minZ,
                            new Random(level.getSeed() ^ chunk.toLong()));
        if (BEACON.enabled() && !STREETS.enabled() && plaza) {
            for (int layer = 1; layer <= 4; layer++)
                for (int x = layer - 6; x < 6 - layer; x++)
                    for (int z = layer - 6; z < 6 - layer; z++)
                        world.setBlock(x, height + layer, z, new OriginWorld.Block("minecraft:iron_block"), 0, 2);
            world.beacon(-1, height + 5, -1, "speed", "speed");
            world.beacon(-1, height + 5, 0, "haste", "haste");
            world.beacon(0, height + 5, -1, "strength", "strength");
            world.beacon(0, height + 5, 0, "resistance", "regeneration");
            placed = true;
        }
        if (testing && TESTING.enabled())
            placed |= new gregtech.content.worldgen.center.SourceTesting(height).generate(world, planChunk, minX, minZ);
        return placed;
    }
}
