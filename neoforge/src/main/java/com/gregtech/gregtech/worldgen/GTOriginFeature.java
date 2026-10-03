package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.worldgen.center.NativeOriginWorld;
import com.gregtech.gregtech.worldgen.center.OriginWorld;
import com.gregtech.gregtech.worldgen.center.SourceNexus;
import com.gregtech.gregtech.worldgen.center.SourceStreets;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import java.util.HashSet;
import java.util.Random;

/** Original GT6 Nexus and axis roads; enabled by user request, configuration is deferred to finalisation. */
public final class GTOriginFeature extends Feature<NoneFeatureConfiguration> {
    public GTOriginFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        if (level.getLevel().dimension() != Level.OVERWORLD) return false;
        ChunkPos chunk = new ChunkPos(context.origin());
        int minX = chunk.getMinBlockX(), minZ = chunk.getMinBlockZ();
        boolean plaza = minX >= -32 && minX <= 16 && minZ >= -32 && minZ <= 16;
        boolean nexus = minX == 16 && minZ == -48;
        boolean road = minX == -16 || minX == 0 || minZ == -16 || minZ == 0;
        if (!plaza && !nexus && !road) return false;
        // WD.waterLevel() is 62 in GT6, while the modern sea-level parameter is 63.
        int height = level.getSeaLevel()+3;
        NativeOriginWorld world = new NativeOriginWorld(level,chunk,height);
        var biomes = new HashSet<String>();
        if (road && !plaza) {
            // The old generator rewrites both halves of a segment together. Read the complete
            // segment's biomes so modern independent half chunks choose the same bridge mode.
            boolean northSouth = minX == -16 || minX == 0;
            for (int along=0;along<16;along++) for (int across=-16;across<16;across++)
                biomes.add(world.getBiomeGenForCoords(northSouth ? across : minX+along,
                        northSouth ? minZ+along : across).biomeName());
        }
        SourceStreets streets = new SourceStreets(height,false,false,true,false);
        // The old generator writes a 64x64 plaza from its four centre chunks. Replaying that plan
        // per intersecting chunk with clipped writes preserves all 16 chunks in any generation order.
        boolean placed = plaza ? streets.generate(world,-16,-16,-1,-1,biomes)
                : road && streets.generate(world,minX,minZ,minX+15,minZ+15,biomes);
        if (nexus) placed |= new SourceNexus(height,true).generate(world,new OriginWorld.Chunk(world,minX,minZ),
                minX,minZ,new Random(level.getSeed() ^ chunk.toLong()));
        return placed;
    }
}
