package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.content.bumble.BumbleBeeGenes;
import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.worldgen.GTBumbleHivesFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.gametest.*;
import java.lang.reflect.Proxy;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class BumbleWorldgenRepairTests {
    @GameTest(template="test_empty")
    public static void genesUseGenerationRegionInsteadOfRequestingFullServerChunk(GameTestHelper h) {
        // The generation view deliberately differs from the completed server world. Any chunk
        // request through this view fails immediately instead of deadlocking a generation worker.
        var pos=h.absolutePos(new BlockPos(1,1,1));
        // Exercise both branches regardless of world seed. At least one generation biome differs
        // from the server biome; plains must expect day-active genes, desert night-active genes.
        for (var generationBiomeKey : java.util.List.of(Biomes.DESERT, Biomes.PLAINS)) {
        var generationBiome=h.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(generationBiomeKey);
        WorldGenLevel region=(WorldGenLevel)Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class},(proxy,method,args)->switch(method.getName()) {
                    case "getLevel" -> h.getLevel();
                    case "getBiome" -> generationBiome;
                    case "getHeight" -> pos.getY();
                    default -> throw new AssertionError("Unexpected world-generation access: "+method.getName());
                });
        boolean nocturnal = generationBiomeKey.equals(Biomes.DESERT);
        for(int seed=0;seed<32;seed++) {
            long temperature=GregTechConstants.C+Math.round(generationBiome.value().getBaseTemperature()*20.0F);
            var expected=BumbleBeeGenes.fromEnvironment(temperature,generationBiome.value().getModifiedClimateSettings().downfall(),true,!nocturnal,nocturnal,RandomSource.create(seed));
            var actual=GTBumbleHivesFeature.genesFor(region,pos,RandomSource.create(seed));
            h.assertTrue(actual.equals(expected),"genome must use the generation-region biome for temperature and rainfall; seed "+seed);
        }
        }
        h.succeed();
    }
}
