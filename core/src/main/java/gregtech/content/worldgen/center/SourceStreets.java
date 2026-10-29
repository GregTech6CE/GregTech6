/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */
package gregtech.content.worldgen.center;

import static gregtech.content.worldgen.center.OriginSupport.*;

import java.util.HashSet;
import java.util.Set;

/** Shared road layout adapted from GT6; native reads, state conversion and chunk clipping are separate contracts.
 * @author Gregorius Techneticies (original geometry)
 */
public final class SourceStreets {
    public final int height;
    private final boolean generateBeacon, generateBiomes, generateNexus;

    public SourceStreets(int height, boolean beacon, boolean biomes, boolean nexus) {
        this.height = height;
        generateBeacon = beacon;
        generateBiomes = biomes;
        generateNexus = nexus;
    }

    public boolean generate(OriginWorld world, int minX, int minZ, int maxX, int maxZ, Set<String> biomeNames) {
        if (minX == -16 || minX == 0) {
            if (minZ == -16 || minZ == 0) {
                for (int i = -32; i < 32; i++)
                    for (int j = -32; j < 32; j++) {
                        for (int k = 2; k < 64; k++) world.setBlock(i, height + k, j, NB, 0, 0);
                        for (int k = world.minY() + 1; k < height; k++)
                            world.setBlock(i, k, j, BlocksGT.Concrete, DYE_INDEX_Gray, 0);

                        world.setBlock(i, height - 1, j, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                        if (i <= -29 || j <= -29 || i >= 28 || j >= 28) {
                            if (inside(-12, 11, i) || inside(-12, 11, j)) {
                                world.setBlock(
                                        i,
                                        height,
                                        j,
                                        BlocksGT.Asphalt,
                                        i == -31 || j == -31 || i == 30 || j == 30 ? DYE_INDEX_White : DYE_INDEX_Gray,
                                        0);
                                world.setBlock(i, height + 1, j, NB, 0, 0);
                            } else {
                                world.setBlock(i, height, j, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                                if (!generateBiomes
                                        && (i == -32 || j == -32 || i == 31 || j == 31)
                                        && (!generateNexus || i < 0 || i == 31 || j != -32)) {
                                    world.setBlock(
                                            i,
                                            height + 1,
                                            j,
                                            even(i, 1, j) ? BlocksGT.CFoam : BlocksGT.Concrete,
                                            DYE_INDEX_LightGray,
                                            0);
                                    world.setBlock(
                                            i,
                                            height + 2,
                                            j,
                                            even(i, 2, j) ? BlocksGT.CFoam : BlocksGT.Concrete,
                                            DYE_INDEX_LightGray,
                                            0);
                                    world.setBlock(
                                            i,
                                            height + 3,
                                            j,
                                            even(i, 3, j) ? BlocksGT.CFoam : BlocksGT.Concrete,
                                            DYE_INDEX_LightGray,
                                            0);
                                    world.setBlock(
                                            i,
                                            height + 4,
                                            j,
                                            even(i, 4, j) ? BlocksGT.CFoam : BlocksGT.Concrete,
                                            DYE_INDEX_LightGray,
                                            0);
                                    world.setBlock(
                                            i,
                                            height + 5,
                                            j,
                                            even(i, 5, j) ? BlocksGT.CFoam : BlocksGT.Concrete,
                                            DYE_INDEX_LightGray,
                                            0);
                                } else {
                                    world.setBlock(
                                            i, height + 1, j, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
                                }
                            }
                        } else {
                            if (inside(-12, 11, i) && inside(-12, 11, j)) {
                                world.setBlock(i, height, j, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                                world.setBlock(
                                        i,
                                        height + 1,
                                        j,
                                        inside(-11, 10, i) && inside(-11, 10, j) ? BlocksGT.Concrete : BlocksGT.CFoam,
                                        DYE_INDEX_LightGray,
                                        0);
                            } else {
                                world.setBlock(i, height, j, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
                                world.setBlock(i, height + 1, j, NB, 0, 0);
                            }
                        }
                    }

                world.setBlock(-32, height + 1, -1, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
                world.setBlock(-31, height + 1, -1, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
                world.setBlock(-32, height + 1, 0, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
                world.setBlock(-31, height + 1, 0, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
                world.setBlock(-32, height + 2, -1, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
                world.setBlock(-31, height + 2, -1, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
                world.setBlock(-32, height + 2, 0, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
                world.setBlock(-31, height + 2, 0, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
                world.setBlock(-32, height + 3, -1, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
                world.setBlock(-31, height + 3, -1, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
                world.setBlock(-32, height + 3, 0, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
                world.setBlock(-31, height + 3, 0, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
                world.setBlock(-32, height + 4, -1, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
                world.setBlock(-31, height + 4, -1, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
                world.setBlock(-32, height + 4, 0, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
                world.setBlock(-31, height + 4, 0, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
                world.setBlock(-32, height + 1, -2, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_White, 0);
                world.setBlock(-31, height + 1, -2, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_Red, 0);
                world.setBlock(-30, height + 1, -1, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_White, 0);
                world.setBlock(-30, height + 1, 0, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_Red, 0);
                world.setBlock(-31, height + 1, 1, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_White, 0);
                world.setBlock(-32, height + 1, 1, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_Red, 0);
                world.sign(
                        -30,
                        height + 3,
                        0,
                        SIDE_X_POS,
                        0,
                        text(world.getBiomeGenForCoords(-4096, +95)),
                        text(world.getBiomeGenForCoords(-3584, +95)),
                        text(world.getBiomeGenForCoords(-3072, +95)),
                        text(world.getBiomeGenForCoords(-2560, +95)));
                world.sign(
                        -30,
                        height + 2,
                        0,
                        SIDE_X_POS,
                        0,
                        text(world.getBiomeGenForCoords(-2048, +95)),
                        text(world.getBiomeGenForCoords(-1536, +95)),
                        text(world.getBiomeGenForCoords(-1024, +95)),
                        text(world.getBiomeGenForCoords(-512, +95)));

                world.sign(
                        -30,
                        height + 3,
                        -1,
                        SIDE_X_POS,
                        0,
                        text(world.getBiomeGenForCoords(-4096, -96)),
                        text(world.getBiomeGenForCoords(-3584, -96)),
                        text(world.getBiomeGenForCoords(-3072, -96)),
                        text(world.getBiomeGenForCoords(-2560, -96)));
                world.sign(
                        -30,
                        height + 2,
                        -1,
                        SIDE_X_POS,
                        0,
                        text(world.getBiomeGenForCoords(-2048, -96)),
                        text(world.getBiomeGenForCoords(-1536, -96)),
                        text(world.getBiomeGenForCoords(-1024, -96)),
                        text(world.getBiomeGenForCoords(-512, -96)));

                world.setBlock(31, height + 1, -1, BlocksGT.Concrete, DYE_INDEX_Red, 0);
                world.setBlock(30, height + 1, -1, BlocksGT.Concrete, DYE_INDEX_Red, 0);
                world.setBlock(31, height + 1, 0, BlocksGT.Concrete, DYE_INDEX_Red, 0);
                world.setBlock(30, height + 1, 0, BlocksGT.Concrete, DYE_INDEX_Red, 0);
                world.setBlock(31, height + 2, -1, BlocksGT.Concrete, DYE_INDEX_Red, 0);
                world.setBlock(30, height + 2, -1, BlocksGT.Concrete, DYE_INDEX_Red, 0);
                world.setBlock(31, height + 2, 0, BlocksGT.Concrete, DYE_INDEX_Red, 0);
                world.setBlock(30, height + 2, 0, BlocksGT.Concrete, DYE_INDEX_Red, 0);
                world.setBlock(31, height + 3, -1, BlocksGT.Concrete, DYE_INDEX_Red, 0);
                world.setBlock(30, height + 3, -1, BlocksGT.Concrete, DYE_INDEX_Red, 0);
                world.setBlock(31, height + 3, 0, BlocksGT.Concrete, DYE_INDEX_Red, 0);
                world.setBlock(30, height + 3, 0, BlocksGT.Concrete, DYE_INDEX_Red, 0);
                world.setBlock(31, height + 4, -1, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
                world.setBlock(30, height + 4, -1, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
                world.setBlock(31, height + 4, 0, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
                world.setBlock(30, height + 4, 0, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
                world.setBlock(31, height + 1, -2, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_Red, 0);
                world.setBlock(30, height + 1, -2, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_White, 0);
                world.setBlock(29, height + 1, -1, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_Red, 0);
                world.setBlock(29, height + 1, 0, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_White, 0);
                world.setBlock(30, height + 1, 1, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_Red, 0);
                world.setBlock(31, height + 1, 1, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_White, 0);
                world.sign(
                        29,
                        height + 3,
                        0,
                        SIDE_X_NEG,
                        0,
                        text(world.getBiomeGenForCoords(+4095, +95)),
                        text(world.getBiomeGenForCoords(+3583, +95)),
                        text(world.getBiomeGenForCoords(+3071, +95)),
                        text(world.getBiomeGenForCoords(+2559, +95)));
                world.sign(
                        29,
                        height + 2,
                        0,
                        SIDE_X_NEG,
                        0,
                        text(world.getBiomeGenForCoords(+2047, +95)),
                        text(world.getBiomeGenForCoords(+1535, +95)),
                        text(world.getBiomeGenForCoords(+1023, +95)),
                        text(world.getBiomeGenForCoords(+511, +95)));

                world.sign(
                        29,
                        height + 3,
                        -1,
                        SIDE_X_NEG,
                        0,
                        text(world.getBiomeGenForCoords(+4095, -96)),
                        text(world.getBiomeGenForCoords(+3583, -96)),
                        text(world.getBiomeGenForCoords(+3071, -96)),
                        text(world.getBiomeGenForCoords(+2559, -96)));
                world.sign(
                        29,
                        height + 2,
                        -1,
                        SIDE_X_NEG,
                        0,
                        text(world.getBiomeGenForCoords(+2047, -96)),
                        text(world.getBiomeGenForCoords(+1535, -96)),
                        text(world.getBiomeGenForCoords(+1023, -96)),
                        text(world.getBiomeGenForCoords(+511, -96)));

                world.setBlock(-1, height + 1, -32, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
                world.setBlock(-1, height + 1, -31, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
                world.setBlock(0, height + 1, -32, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
                world.setBlock(0, height + 1, -31, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
                world.setBlock(-1, height + 2, -32, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
                world.setBlock(-1, height + 2, -31, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
                world.setBlock(0, height + 2, -32, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
                world.setBlock(0, height + 2, -31, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
                world.setBlock(-1, height + 3, -32, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
                world.setBlock(-1, height + 3, -31, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
                world.setBlock(0, height + 3, -32, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
                world.setBlock(0, height + 3, -31, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
                world.setBlock(-1, height + 4, -32, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
                world.setBlock(-1, height + 4, -31, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
                world.setBlock(0, height + 4, -32, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
                world.setBlock(0, height + 4, -31, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
                world.setBlock(-2, height + 1, -32, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_White, 0);
                world.setBlock(-2, height + 1, -31, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_Red, 0);
                world.setBlock(-1, height + 1, -30, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_White, 0);
                world.setBlock(0, height + 1, -30, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_Red, 0);
                world.setBlock(1, height + 1, -31, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_White, 0);
                world.setBlock(1, height + 1, -32, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_Red, 0);
                world.sign(
                        0,
                        height + 3,
                        -30,
                        SIDE_Z_POS,
                        0,
                        text(world.getBiomeGenForCoords(+95, -4096)),
                        text(world.getBiomeGenForCoords(+95, -3584)),
                        text(world.getBiomeGenForCoords(+95, -3072)),
                        text(world.getBiomeGenForCoords(+95, -2560)));
                world.sign(
                        0,
                        height + 2,
                        -30,
                        SIDE_Z_POS,
                        0,
                        text(world.getBiomeGenForCoords(+95, -2048)),
                        text(world.getBiomeGenForCoords(+95, -1536)),
                        text(world.getBiomeGenForCoords(+95, -1024)),
                        text(world.getBiomeGenForCoords(+95, -512)));

                world.sign(
                        -1,
                        height + 3,
                        -30,
                        SIDE_Z_POS,
                        0,
                        text(world.getBiomeGenForCoords(-96, -4096)),
                        text(world.getBiomeGenForCoords(-96, -3584)),
                        text(world.getBiomeGenForCoords(-96, -3072)),
                        text(world.getBiomeGenForCoords(-96, -2560)));
                world.sign(
                        -1,
                        height + 2,
                        -30,
                        SIDE_Z_POS,
                        0,
                        text(world.getBiomeGenForCoords(-96, -2048)),
                        text(world.getBiomeGenForCoords(-96, -1536)),
                        text(world.getBiomeGenForCoords(-96, -1024)),
                        text(world.getBiomeGenForCoords(-96, -512)));

                world.setBlock(-1, height + 1, 31, BlocksGT.Concrete, DYE_INDEX_Green, 0);
                world.setBlock(-1, height + 1, 30, BlocksGT.Concrete, DYE_INDEX_Green, 0);
                world.setBlock(0, height + 1, 31, BlocksGT.Concrete, DYE_INDEX_Green, 0);
                world.setBlock(0, height + 1, 30, BlocksGT.Concrete, DYE_INDEX_Green, 0);
                world.setBlock(-1, height + 2, 31, BlocksGT.Concrete, DYE_INDEX_Green, 0);
                world.setBlock(-1, height + 2, 30, BlocksGT.Concrete, DYE_INDEX_Green, 0);
                world.setBlock(0, height + 2, 31, BlocksGT.Concrete, DYE_INDEX_Green, 0);
                world.setBlock(0, height + 2, 30, BlocksGT.Concrete, DYE_INDEX_Green, 0);
                world.setBlock(-1, height + 3, 31, BlocksGT.Concrete, DYE_INDEX_Green, 0);
                world.setBlock(-1, height + 3, 30, BlocksGT.Concrete, DYE_INDEX_Green, 0);
                world.setBlock(0, height + 3, 31, BlocksGT.Concrete, DYE_INDEX_Green, 0);
                world.setBlock(0, height + 3, 30, BlocksGT.Concrete, DYE_INDEX_Green, 0);
                world.setBlock(-1, height + 4, 31, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
                world.setBlock(-1, height + 4, 30, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
                world.setBlock(0, height + 4, 31, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
                world.setBlock(0, height + 4, 30, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
                world.setBlock(-2, height + 1, 31, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_Red, 0);
                world.setBlock(-2, height + 1, 30, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_White, 0);
                world.setBlock(-1, height + 1, 29, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_Red, 0);
                world.setBlock(0, height + 1, 29, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_White, 0);
                world.setBlock(1, height + 1, 30, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_Red, 0);
                world.setBlock(1, height + 1, 31, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_White, 0);
                world.sign(
                        0,
                        height + 3,
                        29,
                        SIDE_Z_NEG,
                        0,
                        text(world.getBiomeGenForCoords(+95, +4095)),
                        text(world.getBiomeGenForCoords(+95, +3583)),
                        text(world.getBiomeGenForCoords(+95, +3071)),
                        text(world.getBiomeGenForCoords(+95, +2559)));
                world.sign(
                        0,
                        height + 2,
                        29,
                        SIDE_Z_NEG,
                        0,
                        text(world.getBiomeGenForCoords(+95, +2047)),
                        text(world.getBiomeGenForCoords(+95, +1535)),
                        text(world.getBiomeGenForCoords(+95, +1023)),
                        text(world.getBiomeGenForCoords(+95, +511)));

                world.sign(
                        -1,
                        height + 3,
                        29,
                        SIDE_Z_NEG,
                        0,
                        text(world.getBiomeGenForCoords(-96, +4095)),
                        text(world.getBiomeGenForCoords(-96, +3583)),
                        text(world.getBiomeGenForCoords(-96, +3071)),
                        text(world.getBiomeGenForCoords(-96, +2559)));
                world.sign(
                        -1,
                        height + 2,
                        29,
                        SIDE_Z_NEG,
                        0,
                        text(world.getBiomeGenForCoords(-96, +2047)),
                        text(world.getBiomeGenForCoords(-96, +1535)),
                        text(world.getBiomeGenForCoords(-96, +1023)),
                        text(world.getBiomeGenForCoords(-96, +511)));

                if (generateBeacon) {
                    for (int i = -5; i < 5; i++)
                        for (int j = -5; j < 5; j++) set(world, i, height - 3, j, Blocks.iron_block, 0, 0);
                    for (int i = -4; i < 4; i++)
                        for (int j = -4; j < 4; j++) set(world, i, height - 2, j, Blocks.iron_block, 0, 0);
                    for (int i = -3; i < 3; i++)
                        for (int j = -3; j < 3; j++) set(world, i, height - 1, j, Blocks.iron_block, 0, 0);
                    for (int i = -2; i < 2; i++)
                        for (int j = -2; j < 2; j++) set(world, i, height, j, Blocks.iron_block, 0, 0);

                    world.beacon(-1, height + 1, -1, "speed", "speed");

                    world.beacon(-1, height + 1, 0, "haste", "haste");

                    world.beacon(0, height + 1, -1, "strength", "strength");

                    world.beacon(0, height + 1, 0, "resistance", "regeneration");
                }

                world.setSpawnLocation(0, height + 5, 0);
                return T;
            }
            if (minZ < -96 || minZ > 80) {

                for (int opaqueCount = 0, i = -16; i < 16; i++)
                    for (int j = 0; j < 16; j++) {
                        OriginWorld.Block sampled = world.getBlock(i, height + 9, minZ + j);
                        if (sampled.liquid()
                                || anywater(sampled)
                                || (opq(sampled) && !sampled.wood() && !sampled.wood() && !sampled.leaves())) {
                            if (opaqueCount++ > 128) {
                                return generateRoadX(world, minZ, F, F, T, F, F);
                            }
                        }
                    }
                biomeNames = new HashSet<>(biomeNames);
                for (int i = minZ; i <= maxZ; i++)
                    for (int j = (minZ < 0 ? 0 : -16), k = (minZ < 0 ? 16 : 0); j < k; j++) {
                        OriginWorld.Biome biome = world.getBiomeGenForCoords(j, i);
                        if (biome != null) biomeNames.add(biome.biomeName());
                    }
                for (String biomeName : biomeNames)
                    if (world.isInfiniteWaterBiome(biomeName)) {
                        return generateRoadX(world, minZ, F, T, F, T, T);
                    }
                return generateRoadX(world, minZ, T, T, F, F, T);
            }
            return minZ != -32 && minZ != 16 && generateRoadX(world, minZ, F, F, F, T, !generateBiomes);
        }
        if (minZ == -16 || minZ == 0) {
            if (minX < -96 || minX > 80) {

                for (int opaqueCount = 0, i = -16; i < 16; i++)
                    for (int j = 0; j < 16; j++) {
                        OriginWorld.Block sampled = world.getBlock(minX + j, height + 9, i);
                        if (sampled.liquid()
                                || anywater(sampled)
                                || (opq(sampled) && !sampled.wood() && !sampled.wood() && !sampled.leaves())) {
                            if (opaqueCount++ > 128) {
                                return generateRoadZ(world, minX, F, F, T, F, F);
                            }
                        }
                    }
                biomeNames = new HashSet<>(biomeNames);
                for (int i = minX; i <= maxX; i++)
                    for (int j = (minZ < 0 ? 0 : -16), k = (minZ < 0 ? 16 : 0); j < k; j++) {
                        OriginWorld.Biome biome = world.getBiomeGenForCoords(i, j);
                        if (biome != null) biomeNames.add(biome.biomeName());
                    }
                for (String biomeName : biomeNames)
                    if (world.isInfiniteWaterBiome(biomeName)) {
                        return generateRoadZ(world, minX, F, T, F, T, T);
                    }
                return generateRoadZ(world, minX, T, T, F, F, T);
            }
            return minX != -32 && minX != 16 && generateRoadZ(world, minX, F, F, F, T, !generateBiomes);
        }
        return F;
    }

    public final boolean generateRoadX(
            OriginWorld world,
            int minZ,
            boolean land,
            boolean killSky,
            boolean tunnel,
            boolean bridge,
            boolean sideWalls) {
        for (int i = 0; i < 16; i++) {
            if (land) {
                for (int j = height + 1; j > world.minY(); j--)
                    if (!opq(world, -13, j, minZ + i, T, T)) set(world, -13, j, minZ + i, Blocks.gravel, 1, 0, T);
                    else break;
                for (int j = height + 1; j > world.minY(); j--)
                    if (!opq(world, 12, j, minZ + i, T, T)) set(world, 12, j, minZ + i, Blocks.gravel, 1, 0, T);
                    else break;
                for (int j = height; j > world.minY(); j--)
                    if (!opq(world, -14, j, minZ + i, T, T)) set(world, -14, j, minZ + i, Blocks.gravel, 1, 0, T);
                    else break;
                for (int j = height; j > world.minY(); j--)
                    if (!opq(world, 13, j, minZ + i, T, T)) set(world, 13, j, minZ + i, Blocks.gravel, 1, 0, T);
                    else break;
                for (int j = height - 1; j > world.minY(); j--)
                    if (!opq(world, -15, j, minZ + i, T, T)) set(world, -15, j, minZ + i, Blocks.gravel, 1, 0, T);
                    else break;
                for (int j = height - 1; j > world.minY(); j--)
                    if (!opq(world, 14, j, minZ + i, T, T)) set(world, 14, j, minZ + i, Blocks.gravel, 1, 0, T);
                    else break;
                for (int j = height - 2; j > world.minY(); j--)
                    if (!opq(world, -16, j, minZ + i, T, T)) set(world, -16, j, minZ + i, Blocks.gravel, 1, 0, T);
                    else break;
                for (int j = height - 2; j > world.minY(); j--)
                    if (!opq(world, 15, j, minZ + i, T, T)) set(world, 15, j, minZ + i, Blocks.gravel, 1, 0, T);
                    else break;
            }
            if (tunnel) {
                for (int j = -12; j < 12; j++)
                    world.setBlock(j, height + 7, minZ + i, BlocksGT.Concrete, DYE_INDEX_White, 0);
                for (int j = 0; j < 7; j++) {
                    world.setBlock(
                            -13,
                            height + j,
                            minZ + i,
                            BlocksGT.Concrete,
                            j == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            12,
                            height + j,
                            minZ + i,
                            BlocksGT.Concrete,
                            j == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                }
            }
            if (bridge) {
                set(world, -13, height, minZ + i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
                set(world, 12, height, minZ + i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
                world.setBlock(-13, height + 1, minZ + i, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                world.setBlock(12, height + 1, minZ + i, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
            }
            if (killSky) {
                for (int j = -13; j < 13; j++)
                    for (int k = 2; k < 64; k++) world.setBlock(j, height + k, minZ + i, NB, 0, 0);
            } else {
                for (int j = -12; j < 12; j++)
                    for (int k = 2; k < 7; k++) world.setBlock(j, height + k, minZ + i, NB, 0, 0);
            }
            for (int j = -12; j < 2; j++) {
                world.setBlock(j, height + 1, minZ + i, NB, 0, 0);
                if (land) {
                    set(world, j, height - 2, minZ + i, Blocks.cobblestone, 0, 0, T);
                    world.setBlock(j, height - 1, minZ + i, Blocks.gravel, 1, 0);
                    for (int k = height - 3; k > world.minY(); k--)
                        if (!opq(world, j, k, minZ + i, T, T)) set(world, j, k, minZ + i, Blocks.cobblestone, 0, 0, T);
                        else break;
                } else {
                    set(world, j, height - 1, minZ + i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
                }
            }
            for (int j = 1; j < 12; j++) {
                world.setBlock(j, height + 1, minZ + i, NB, 0, 0);
                if (land) {
                    set(world, j, height - 2, minZ + i, Blocks.cobblestone, 0, 0, T);
                    world.setBlock(j, height - 1, minZ + i, Blocks.gravel, 1, 0);
                    for (int k = height - 3; k > world.minY(); k--)
                        if (!opq(world, j, k, minZ + i, T, T)) set(world, j, k, minZ + i, Blocks.cobblestone, 0, 0, T);
                        else break;
                } else {
                    set(world, j, height - 1, minZ + i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
                }
            }

            world.setBlock(-12, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(-12, height + 1, minZ + i, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_LightGray, 0);
            world.setBlock(-11, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(-11, height + 1, minZ + i, BlocksGT.RailRoad, 0, 0);
            world.setBlock(-10, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(-9, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(-8, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(-7, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(-7, height + 1, minZ + i, BlocksGT.RailRoad, ((i + 2) / 4) % 2 == 0 ? 8 : 0, 0);
            world.setBlock(-6, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(-5, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(-4, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(-3, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(-3, height + 1, minZ + i, BlocksGT.RailRoad, 0, 0);
            world.setBlock(-2, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(-2, height + 1, minZ + i, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_LightGray, 0);

            if (land) {
                set(world, -1, height - 1, minZ + i, Blocks.cobblestone, 0, 0, T);
                set(world, -1, height, minZ + i, Blocks.gravel, 1, 0, F);
                set(world, -1, height + 1, minZ + i, Blocks.gravel, 1, 0, F);
                set(world, 0, height - 1, minZ + i, Blocks.cobblestone, 0, 0, T);
                set(world, 0, height, minZ + i, Blocks.gravel, 1, 0, F);
                set(world, 0, height + 1, minZ + i, Blocks.gravel, 1, 0, F);
                for (int j = height - 2; j > world.minY(); j--)
                    if (!opq(world, -1, j, minZ + i, T, T)) set(world, -1, j, minZ + i, Blocks.cobblestone, 0, 0, T);
                    else break;
                for (int j = height - 2; j > world.minY(); j--)
                    if (!opq(world, 0, j, minZ + i, T, T)) set(world, 0, j, minZ + i, Blocks.cobblestone, 0, 0, T);
                    else break;
            } else {
                set(world, -1, height - 1, minZ + i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
                set(world, -1, height, minZ + i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, F);
                set(world, -1, height + 1, minZ + i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, F);
                set(world, 0, height - 1, minZ + i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
                set(world, 0, height, minZ + i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, F);
                set(world, 0, height + 1, minZ + i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, F);
            }

            world.setBlock(1, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(1, height + 1, minZ + i, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_LightGray, 0);
            world.setBlock(2, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(2, height + 1, minZ + i, BlocksGT.RailRoad, 0, 0);
            world.setBlock(3, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(4, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(5, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(6, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(6, height + 1, minZ + i, BlocksGT.RailRoad, ((i + 2) / 4) % 2 == 0 ? 8 : 0, 0);
            world.setBlock(7, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(8, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(9, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(10, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(10, height + 1, minZ + i, BlocksGT.RailRoad, 0, 0);
            world.setBlock(11, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(11, height + 1, minZ + i, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_LightGray, 0);
        }

        if (tunnel) {
            world.setBlock(-13, height + 3, minZ + 1, Blocks.glowstone, 0, 0);
            world.setBlock(12, height + 3, minZ + 1, Blocks.glowstone, 0, 0);
            world.setBlock(-13, height + 3, minZ + 6, Blocks.glowstone, 0, 0);
            world.setBlock(12, height + 3, minZ + 6, Blocks.glowstone, 0, 0);
            world.setBlock(-13, height + 3, minZ + 9, Blocks.glowstone, 0, 0);
            world.setBlock(12, height + 3, minZ + 9, Blocks.glowstone, 0, 0);
            world.setBlock(-13, height + 3, minZ + 14, Blocks.glowstone, 0, 0);
            world.setBlock(12, height + 3, minZ + 14, Blocks.glowstone, 0, 0);
        }
        if (sideWalls) {
            for (int i = 0; i < 8; i++) {
                OriginWorld.Block sampled = block(world, 13, height + 4, minZ + i, T);
                if (sampled.liquid() || opq(sampled)) {
                    for (int j = 0; j < 8; j++)
                        for (int k = 2; k < 6; k++)
                            set(
                                    world,
                                    12,
                                    height + k,
                                    minZ + j,
                                    even(0, k, j) ? BlocksGT.CFoam : BlocksGT.Concrete,
                                    DYE_INDEX_LightGray,
                                    0,
                                    T);
                    break;
                }
            }
            for (int i = 0; i < 8; i++) {
                OriginWorld.Block sampled = block(world, -14, height + 4, minZ + i, T);
                if (sampled.liquid() || opq(sampled)) {
                    for (int j = 0; j < 8; j++)
                        for (int k = 2; k < 6; k++)
                            set(
                                    world,
                                    -13,
                                    height + k,
                                    minZ + j,
                                    even(1, k, j) ? BlocksGT.CFoam : BlocksGT.Concrete,
                                    DYE_INDEX_LightGray,
                                    0,
                                    T);
                    break;
                }
            }
            for (int i = 8; i < 16; i++) {
                OriginWorld.Block sampled = block(world, 13, height + 4, minZ + i, T);
                if (sampled.liquid() || opq(sampled)) {
                    for (int j = 8; j < 16; j++)
                        for (int k = 2; k < 6; k++)
                            set(
                                    world,
                                    12,
                                    height + k,
                                    minZ + j,
                                    even(0, k, j) ? BlocksGT.CFoam : BlocksGT.Concrete,
                                    DYE_INDEX_LightGray,
                                    0,
                                    T);
                    break;
                }
            }
            for (int i = 8; i < 16; i++) {
                OriginWorld.Block sampled = block(world, -14, height + 4, minZ + i, T);
                if (sampled.liquid() || opq(sampled)) {
                    for (int j = 8; j < 16; j++)
                        for (int k = 2; k < 6; k++)
                            set(
                                    world,
                                    -13,
                                    height + k,
                                    minZ + j,
                                    even(1, k, j) ? BlocksGT.CFoam : BlocksGT.Concrete,
                                    DYE_INDEX_LightGray,
                                    0,
                                    T);
                    break;
                }
            }
        }

        if (minZ >> 9 != (minZ - 16) >> 9) {
            for (int i = 5; i < 11; i++)
                for (int j = 1; j < 6; j++) {
                    set(
                            world,
                            -13,
                            height + j,
                            minZ + i,
                            BlocksGT.Concrete,
                            i == 5 || i == 10 || j == 1 || j == 5
                                    ? tunnel ? DYE_INDEX_Black : DYE_INDEX_White
                                    : minZ < 0 ? DYE_INDEX_Yellow : DYE_INDEX_Green,
                            0,
                            j == 1);
                    set(
                            world,
                            12,
                            height + j,
                            minZ + i,
                            BlocksGT.Concrete,
                            i == 5 || i == 10 || j == 1 || j == 5
                                    ? tunnel ? DYE_INDEX_Black : DYE_INDEX_White
                                    : minZ < 0 ? DYE_INDEX_Yellow : DYE_INDEX_Green,
                            0,
                            j == 1);
                }
            world.sign(-12, height + 3, minZ + 7, SIDE_X_POS, 0, "", "X: -1", "Z: " + ((minZ - 16) >> 9), "");
            world.sign(-12, height + 3, minZ + 8, SIDE_X_POS, 0, "", "X: -1", "Z: " + (minZ >> 9), "");
            world.sign(11, height + 3, minZ + 7, SIDE_X_NEG, 0, "", "X: 0", "Z: " + ((minZ - 16) >> 9), "");
            world.sign(11, height + 3, minZ + 8, SIDE_X_NEG, 0, "", "X: 0", "Z: " + (minZ >> 9), "");

            world.setBlock(-13, height + 1, minZ + 5, Blocks.glowstone, 0, 0);
            world.setBlock(-13, height + 1, minZ + 10, Blocks.glowstone, 0, 0);
            world.setBlock(-13, height + 5, minZ + 5, Blocks.glowstone, 0, 0);
            world.setBlock(-13, height + 5, minZ + 10, Blocks.glowstone, 0, 0);
            world.setBlock(12, height + 1, minZ + 5, Blocks.glowstone, 0, 0);
            world.setBlock(12, height + 1, minZ + 10, Blocks.glowstone, 0, 0);
            world.setBlock(12, height + 5, minZ + 5, Blocks.glowstone, 0, 0);
            world.setBlock(12, height + 5, minZ + 10, Blocks.glowstone, 0, 0);
        }
        if (minZ >> 9 != (minZ + 16) >> 9) {
            world.setBlock(-2, height + 1, minZ + 0, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_White, 0);
            world.setBlock(-2, height + 1, minZ + 1, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_Red, 0);
            world.setBlock(-2, height + 1, minZ + 2, NB, 0, 0);
            world.setBlock(-1, height + 1, minZ + 2, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_White, 0);
            world.setBlock(0, height + 1, minZ + 2, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_Red, 0);
            world.setBlock(1, height + 1, minZ + 2, NB, 0, 0);
            world.setBlock(1, height + 1, minZ + 1, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_White, 0);
            world.setBlock(1, height + 1, minZ + 0, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_Red, 0);

            for (int i = 2; i < 14; i++) {
                world.setBlock(-1, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
                world.setBlock(0, height, minZ + i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            }
            for (int i = 3; i < 13; i++) {
                world.setBlock(-2, height + 1, minZ + i, NB, 0, 0);
                world.setBlock(-1, height + 1, minZ + i, NB, 0, 0);
                world.setBlock(0, height + 1, minZ + i, NB, 0, 0);
                world.setBlock(1, height + 1, minZ + i, NB, 0, 0);
            }

            world.setBlock(-2, height + 1, minZ + 15, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_Red, 0);
            world.setBlock(-2, height + 1, minZ + 14, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_White, 0);
            world.setBlock(-2, height + 1, minZ + 13, NB, 0, 0);
            world.setBlock(-1, height + 1, minZ + 13, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_Red, 0);
            world.setBlock(0, height + 1, minZ + 13, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_White, 0);
            world.setBlock(1, height + 1, minZ + 13, NB, 0, 0);
            world.setBlock(1, height + 1, minZ + 14, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_Red, 0);
            world.setBlock(1, height + 1, minZ + 15, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_White, 0);

            if (tunnel) {
                for (int i = 0; i < 7; i++) {
                    world.setBlock(
                            -1,
                            height + i,
                            minZ + 0,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            -1,
                            height + i,
                            minZ + 1,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            0,
                            height + i,
                            minZ + 0,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            0,
                            height + i,
                            minZ + 1,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            -1,
                            height + i,
                            minZ + 14,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            -1,
                            height + i,
                            minZ + 15,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            0,
                            height + i,
                            minZ + 14,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            0,
                            height + i,
                            minZ + 15,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                }
            }
        } else {
            if (tunnel) {
                for (int i = 0; i < 7; i++) {
                    world.setBlock(
                            -1,
                            height + i,
                            minZ + 7,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            -1,
                            height + i,
                            minZ + 8,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            0,
                            height + i,
                            minZ + 7,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            0,
                            height + i,
                            minZ + 8,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                }
            }
        }

        if (bridge) {
            for (int i = 6; i <= 9; i++) {
                for (int j = -9; j <= -6; j++) {
                    world.setBlock(j, height - 2, minZ + i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                    world.setBlock(j, height - 3, minZ + i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                }
                for (int j = 5; j <= 8; j++) {
                    world.setBlock(j, height - 2, minZ + i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                    world.setBlock(j, height - 3, minZ + i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                }
            }
            for (int k = height - 4; k > world.minY(); k--)
                if (!(opq(world, -10, k, minZ + 10, T, T)
                        && opq(world, -5, k, minZ + 10, T, T)
                        && opq(world, -10, k, minZ + 5, T, T)
                        && opq(world, -5, k, minZ + 5, T, T))) {
                    world.setBlock(-7, k, minZ + 7, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                    world.setBlock(-8, k, minZ + 8, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                    world.setBlock(-7, k, minZ + 8, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                    world.setBlock(-8, k, minZ + 7, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                } else {
                    for (int i = 6; i <= 9; i++)
                        for (int j = -9; j <= -6; j++) {
                            if (k > world.minY() + (-3))
                                world.setBlock(j, k + 3, minZ + i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (-2))
                                world.setBlock(j, k + 2, minZ + i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (-1))
                                world.setBlock(j, k + 1, minZ + i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (0))
                                world.setBlock(j, k, minZ + i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (+1))
                                world.setBlock(j, k - 1, minZ + i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (+2))
                                world.setBlock(j, k - 2, minZ + i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (+3))
                                world.setBlock(j, k - 3, minZ + i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                        }
                    break;
                }
            for (int k = height - 4; k > world.minY(); k--)
                if (!(opq(world, 9, k, minZ + 10, T, T)
                        && opq(world, 4, k, minZ + 10, T, T)
                        && opq(world, 9, k, minZ + 5, T, T)
                        && opq(world, 4, k, minZ + 5, T, T))) {
                    world.setBlock(6, k, minZ + 7, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                    world.setBlock(7, k, minZ + 8, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                    world.setBlock(6, k, minZ + 8, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                    world.setBlock(7, k, minZ + 7, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                } else {
                    for (int i = 6; i <= 9; i++)
                        for (int j = 5; j <= 8; j++) {
                            if (k > world.minY() + (-3))
                                world.setBlock(j, k + 3, minZ + i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (-2))
                                world.setBlock(j, k + 2, minZ + i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (-1))
                                world.setBlock(j, k + 1, minZ + i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (0))
                                world.setBlock(j, k, minZ + i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (+1))
                                world.setBlock(j, k - 1, minZ + i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (+2))
                                world.setBlock(j, k - 2, minZ + i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (+3))
                                world.setBlock(j, k - 3, minZ + i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                        }
                    break;
                }
        }

        // Kill every living thing close by except Players.
        world.clearNonPlayerEntities(-16, height, minZ, +16, height + 8, minZ + 16);
        return T;
    }

    public final boolean generateRoadZ(
            OriginWorld world,
            int minX,
            boolean land,
            boolean killSky,
            boolean tunnel,
            boolean bridge,
            boolean sideWalls) {
        for (int i = 0; i < 16; i++) {
            if (land) {
                for (int j = height + 1; j > world.minY(); j--)
                    if (!opq(world, minX + i, j, -13, T, T)) set(world, minX + i, j, -13, Blocks.gravel, 1, 0, T);
                    else break;
                for (int j = height + 1; j > world.minY(); j--)
                    if (!opq(world, minX + i, j, 12, T, T)) set(world, minX + i, j, 12, Blocks.gravel, 1, 0, T);
                    else break;
                for (int j = height; j > world.minY(); j--)
                    if (!opq(world, minX + i, j, -14, T, T)) set(world, minX + i, j, -14, Blocks.gravel, 1, 0, T);
                    else break;
                for (int j = height; j > world.minY(); j--)
                    if (!opq(world, minX + i, j, 13, T, T)) set(world, minX + i, j, 13, Blocks.gravel, 1, 0, T);
                    else break;
                for (int j = height - 1; j > world.minY(); j--)
                    if (!opq(world, minX + i, j, -15, T, T)) set(world, minX + i, j, -15, Blocks.gravel, 1, 0, T);
                    else break;
                for (int j = height - 1; j > world.minY(); j--)
                    if (!opq(world, minX + i, j, 14, T, T)) set(world, minX + i, j, 14, Blocks.gravel, 1, 0, T);
                    else break;
                for (int j = height - 2; j > world.minY(); j--)
                    if (!opq(world, minX + i, j, -16, T, T)) set(world, minX + i, j, -16, Blocks.gravel, 1, 0, T);
                    else break;
                for (int j = height - 2; j > world.minY(); j--)
                    if (!opq(world, minX + i, j, 15, T, T)) set(world, minX + i, j, 15, Blocks.gravel, 1, 0, T);
                    else break;
            }
            if (tunnel) {
                for (int j = -12; j < 12; j++)
                    world.setBlock(minX + i, height + 7, j, BlocksGT.Concrete, DYE_INDEX_White, 0);
                for (int j = 0; j < 7; j++) {
                    world.setBlock(
                            minX + i,
                            height + j,
                            -13,
                            BlocksGT.Concrete,
                            j == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            minX + i,
                            height + j,
                            12,
                            BlocksGT.Concrete,
                            j == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                }
            }
            if (bridge) {
                set(world, minX + i, height, -13, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
                set(world, minX + i, height, 12, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
                world.setBlock(minX + i, height + 1, -13, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                world.setBlock(minX + i, height + 1, 12, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
            }
            if (killSky) {
                for (int j = -13; j < 13; j++)
                    for (int k = 2; k < 32; k++) world.setBlock(minX + i, height + k, j, NB, 0, 0);
            } else {
                for (int j = -12; j < 12; j++)
                    for (int k = 2; k < 7; k++) world.setBlock(minX + i, height + k, j, NB, 0, 0);
            }
            for (int j = -12; j < 2; j++) {
                world.setBlock(minX + i, height + 1, j, NB, 0, 0);
                if (land) {
                    set(world, minX + i, height - 2, j, Blocks.cobblestone, 0, 0, T);
                    world.setBlock(minX + i, height - 1, j, Blocks.gravel, 1, 0);
                    for (int k = height - 3; k > world.minY(); k--)
                        if (!opq(world, minX + i, k, j, T, T)) set(world, minX + i, k, j, Blocks.cobblestone, 0, 0, T);
                        else break;
                } else {
                    set(world, minX + i, height - 1, j, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
                }
            }
            for (int j = 1; j < 12; j++) {
                world.setBlock(minX + i, height + 1, j, NB, 0, 0);
                if (land) {
                    set(world, minX + i, height - 2, j, Blocks.cobblestone, 0, 0, T);
                    world.setBlock(minX + i, height - 1, j, Blocks.gravel, 1, 0);
                    for (int k = height - 3; k > world.minY(); k--)
                        if (!opq(world, minX + i, k, j, T, T)) set(world, minX + i, k, j, Blocks.cobblestone, 0, 0, T);
                        else break;
                } else {
                    set(world, minX + i, height - 1, j, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
                }
            }

            world.setBlock(minX + i, height, -12, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height + 1, -12, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_LightGray, 0);
            world.setBlock(minX + i, height, -11, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height + 1, -11, BlocksGT.RailRoad, 1, 0);
            world.setBlock(minX + i, height, -10, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height, -9, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height, -8, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height, -7, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height + 1, -7, BlocksGT.RailRoad, ((i + 2) / 4) % 2 == 0 ? 9 : 1, 0);
            world.setBlock(minX + i, height, -6, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height, -5, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height, -4, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height, -3, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height + 1, -3, BlocksGT.RailRoad, 1, 0);
            world.setBlock(minX + i, height, -2, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height + 1, -2, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_LightGray, 0);

            if (land) {
                set(world, minX + i, height - 1, -1, Blocks.cobblestone, 0, 0, T);
                set(world, minX + i, height, -1, Blocks.gravel, 1, 0, F);
                set(world, minX + i, height + 1, -1, Blocks.gravel, 1, 0, F);
                set(world, minX + i, height - 1, 0, Blocks.cobblestone, 0, 0, T);
                set(world, minX + i, height, 0, Blocks.gravel, 1, 0, F);
                set(world, minX + i, height + 1, 0, Blocks.gravel, 1, 0, F);
                for (int j = height - 2; j > world.minY(); j--)
                    if (!opq(world, minX + i, j, -1, T, T)) set(world, minX + i, j, -1, Blocks.cobblestone, 0, 0, T);
                    else break;
                for (int j = height - 2; j > world.minY(); j--)
                    if (!opq(world, minX + i, j, 0, T, T)) set(world, minX + i, j, 0, Blocks.cobblestone, 0, 0, T);
                    else break;
            } else {
                set(world, minX + i, height - 1, -1, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
                set(world, minX + i, height, -1, BlocksGT.Concrete, DYE_INDEX_Gray, 0, F);
                set(world, minX + i, height + 1, -1, BlocksGT.Concrete, DYE_INDEX_Gray, 0, F);
                set(world, minX + i, height - 1, 0, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
                set(world, minX + i, height, 0, BlocksGT.Concrete, DYE_INDEX_Gray, 0, F);
                set(world, minX + i, height + 1, 0, BlocksGT.Concrete, DYE_INDEX_Gray, 0, F);
            }

            world.setBlock(minX + i, height, 1, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height + 1, 1, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_LightGray, 0);
            world.setBlock(minX + i, height, 2, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height + 1, 2, BlocksGT.RailRoad, 1, 0);
            world.setBlock(minX + i, height, 3, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height, 4, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height, 5, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height, 6, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height + 1, 6, BlocksGT.RailRoad, ((i + 2) / 4) % 2 == 0 ? 9 : 1, 0);
            world.setBlock(minX + i, height, 7, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height, 8, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height, 9, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height, 10, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height + 1, 10, BlocksGT.RailRoad, 1, 0);
            world.setBlock(minX + i, height, 11, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            world.setBlock(minX + i, height + 1, 11, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_LightGray, 0);
        }

        if (tunnel) {
            world.setBlock(minX + 1, height + 3, -13, Blocks.glowstone, 0, 0);
            world.setBlock(minX + 1, height + 3, 12, Blocks.glowstone, 0, 0);
            world.setBlock(minX + 6, height + 3, -13, Blocks.glowstone, 0, 0);
            world.setBlock(minX + 6, height + 3, 12, Blocks.glowstone, 0, 0);
            world.setBlock(minX + 9, height + 3, -13, Blocks.glowstone, 0, 0);
            world.setBlock(minX + 9, height + 3, 12, Blocks.glowstone, 0, 0);
            world.setBlock(minX + 14, height + 3, -13, Blocks.glowstone, 0, 0);
            world.setBlock(minX + 14, height + 3, 12, Blocks.glowstone, 0, 0);
        }
        if (sideWalls) {
            for (int i = 0; i < 8; i++) {
                OriginWorld.Block sampled = block(world, minX + i, height + 4, 13, T);
                if (sampled.liquid() || opq(sampled)) {
                    for (int j = 0; j < 8; j++)
                        for (int k = 2; k < 6; k++)
                            set(
                                    world,
                                    minX + j,
                                    height + k,
                                    12,
                                    even(0, k, j) ? BlocksGT.CFoam : BlocksGT.Concrete,
                                    DYE_INDEX_LightGray,
                                    0,
                                    T);
                    break;
                }
            }
            for (int i = 0; i < 8; i++) {
                OriginWorld.Block sampled = block(world, minX + i, height + 4, -14, T);
                if (sampled.liquid() || opq(sampled)) {
                    for (int j = 0; j < 8; j++)
                        for (int k = 2; k < 6; k++)
                            set(
                                    world,
                                    minX + j,
                                    height + k,
                                    -13,
                                    even(1, k, j) ? BlocksGT.CFoam : BlocksGT.Concrete,
                                    DYE_INDEX_LightGray,
                                    0,
                                    T);
                    break;
                }
            }
            for (int i = 8; i < 16; i++) {
                OriginWorld.Block sampled = block(world, minX + i, height + 4, 13, T);
                if (sampled.liquid() || opq(sampled)) {
                    for (int j = 8; j < 16; j++)
                        for (int k = 2; k < 6; k++)
                            set(
                                    world,
                                    minX + j,
                                    height + k,
                                    12,
                                    even(0, k, j) ? BlocksGT.CFoam : BlocksGT.Concrete,
                                    DYE_INDEX_LightGray,
                                    0,
                                    T);
                    break;
                }
            }
            for (int i = 8; i < 16; i++) {
                OriginWorld.Block sampled = block(world, minX + i, height + 4, -14, T);
                if (sampled.liquid() || opq(sampled)) {
                    for (int j = 8; j < 16; j++)
                        for (int k = 2; k < 6; k++)
                            set(
                                    world,
                                    minX + j,
                                    height + k,
                                    -13,
                                    even(1, k, j) ? BlocksGT.CFoam : BlocksGT.Concrete,
                                    DYE_INDEX_LightGray,
                                    0,
                                    T);
                    break;
                }
            }
        }

        if (minX >> 9 != (minX - 16) >> 9) {
            for (int i = 5; i < 11; i++)
                for (int j = 1; j < 6; j++) {
                    set(
                            world,
                            minX + i,
                            height + j,
                            -13,
                            BlocksGT.Concrete,
                            i == 5 || i == 10 || j == 1 || j == 5
                                    ? tunnel ? DYE_INDEX_Black : DYE_INDEX_White
                                    : minX < 0 ? DYE_INDEX_Blue : DYE_INDEX_Red,
                            0,
                            j == 1);
                    set(
                            world,
                            minX + i,
                            height + j,
                            12,
                            BlocksGT.Concrete,
                            i == 5 || i == 10 || j == 1 || j == 5
                                    ? tunnel ? DYE_INDEX_Black : DYE_INDEX_White
                                    : minX < 0 ? DYE_INDEX_Blue : DYE_INDEX_Red,
                            0,
                            j == 1);
                }
            world.sign(minX + 7, height + 3, -12, SIDE_Z_POS, 0, "", "X: " + ((minX - 16) >> 9), "Z: -1", "");
            world.sign(minX + 8, height + 3, -12, SIDE_Z_POS, 0, "", "X: " + (minX >> 9), "Z: -1", "");
            world.sign(minX + 7, height + 3, 11, SIDE_Z_NEG, 0, "", "X: " + ((minX - 16) >> 9), "Z: 0", "");
            world.sign(minX + 8, height + 3, 11, SIDE_Z_NEG, 0, "", "X: " + (minX >> 9), "Z: 0", "");

            world.setBlock(minX + 5, height + 1, -13, Blocks.glowstone, 0, 0);
            world.setBlock(minX + 10, height + 1, -13, Blocks.glowstone, 0, 0);
            world.setBlock(minX + 5, height + 5, -13, Blocks.glowstone, 0, 0);
            world.setBlock(minX + 10, height + 5, -13, Blocks.glowstone, 0, 0);
            world.setBlock(minX + 5, height + 1, 12, Blocks.glowstone, 0, 0);
            world.setBlock(minX + 10, height + 1, 12, Blocks.glowstone, 0, 0);
            world.setBlock(minX + 5, height + 5, 12, Blocks.glowstone, 0, 0);
            world.setBlock(minX + 10, height + 5, 12, Blocks.glowstone, 0, 0);
        }
        if (minX >> 9 != (minX + 16) >> 9) {
            world.setBlock(minX + 0, height + 1, -2, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_White, 0);
            world.setBlock(minX + 1, height + 1, -2, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_Red, 0);
            world.setBlock(minX + 2, height + 1, -2, NB, 0, 0);
            world.setBlock(minX + 2, height + 1, -1, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_White, 0);
            world.setBlock(minX + 2, height + 1, 0, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_Red, 0);
            world.setBlock(minX + 2, height + 1, 1, NB, 0, 0);
            world.setBlock(minX + 1, height + 1, 1, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_White, 0);
            world.setBlock(minX + 0, height + 1, 1, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_Red, 0);

            for (int i = 2; i < 14; i++) {
                world.setBlock(minX + i, height, -1, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
                world.setBlock(minX + i, height, 0, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
            }
            for (int i = 3; i < 13; i++) {
                world.setBlock(minX + i, height + 1, -2, NB, 0, 0);
                world.setBlock(minX + i, height + 1, -1, NB, 0, 0);
                world.setBlock(minX + i, height + 1, 0, NB, 0, 0);
                world.setBlock(minX + i, height + 1, 1, NB, 0, 0);
            }

            world.setBlock(minX + 15, height + 1, -2, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_Red, 0);
            world.setBlock(minX + 14, height + 1, -2, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_White, 0);
            world.setBlock(minX + 13, height + 1, -2, NB, 0, 0);
            world.setBlock(minX + 13, height + 1, -1, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_Red, 0);
            world.setBlock(minX + 13, height + 1, 0, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_White, 0);
            world.setBlock(minX + 13, height + 1, 1, NB, 0, 0);
            world.setBlock(minX + 14, height + 1, 1, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_Red, 0);
            world.setBlock(minX + 15, height + 1, 1, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_White, 0);

            if (tunnel) {
                for (int i = 0; i < 7; i++) {
                    world.setBlock(
                            minX + 0,
                            height + i,
                            -1,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            minX + 0,
                            height + i,
                            0,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            minX + 1,
                            height + i,
                            -1,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            minX + 1,
                            height + i,
                            0,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            minX + 14,
                            height + i,
                            -1,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            minX + 14,
                            height + i,
                            0,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            minX + 15,
                            height + i,
                            -1,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            minX + 15,
                            height + i,
                            0,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                }
            }
        } else {
            if (tunnel) {
                for (int i = 0; i < 7; i++) {
                    world.setBlock(
                            minX + 7,
                            height + i,
                            -1,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            minX + 7,
                            height + i,
                            0,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            minX + 8,
                            height + i,
                            -1,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                    world.setBlock(
                            minX + 8,
                            height + i,
                            0,
                            BlocksGT.Concrete,
                            i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White,
                            0);
                }
            }
        }

        if (bridge) {
            for (int i = 6; i <= 9; i++) {
                for (int j = -9; j <= -6; j++) {
                    world.setBlock(minX + i, height - 2, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                    world.setBlock(minX + i, height - 3, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                }
                for (int j = 5; j <= 8; j++) {
                    world.setBlock(minX + i, height - 2, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                    world.setBlock(minX + i, height - 3, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                }
            }
            for (int k = height - 4; k > world.minY(); k--)
                if (!(opq(world, minX + 10, k, -10, T, T)
                        && opq(world, minX + 10, k, -5, T, T)
                        && opq(world, minX + 5, k, -10, T, T)
                        && opq(world, minX + 5, k, -5, T, T))) {
                    world.setBlock(minX + 7, k, -7, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                    world.setBlock(minX + 8, k, -8, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                    world.setBlock(minX + 7, k, -8, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                    world.setBlock(minX + 8, k, -7, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                } else {
                    for (int i = 6; i <= 9; i++)
                        for (int j = -9; j <= -6; j++) {
                            if (k > world.minY() + (-3))
                                world.setBlock(minX + i, k + 3, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (-2))
                                world.setBlock(minX + i, k + 2, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (-1))
                                world.setBlock(minX + i, k + 1, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (0))
                                world.setBlock(minX + i, k, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (+1))
                                world.setBlock(minX + i, k - 1, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (+2))
                                world.setBlock(minX + i, k - 2, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (+3))
                                world.setBlock(minX + i, k - 3, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                        }
                    break;
                }
            for (int k = height - 4; k > world.minY(); k--)
                if (!(opq(world, minX + 10, k, 9, T, T)
                        && opq(world, minX + 10, k, 4, T, T)
                        && opq(world, minX + 5, k, 9, T, T)
                        && opq(world, minX + 5, k, 4, T, T))) {
                    world.setBlock(minX + 7, k, 6, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                    world.setBlock(minX + 8, k, 7, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                    world.setBlock(minX + 7, k, 7, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                    world.setBlock(minX + 8, k, 6, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
                } else {
                    for (int i = 6; i <= 9; i++)
                        for (int j = 5; j <= 8; j++) {
                            if (k > world.minY() + (-3))
                                world.setBlock(minX + i, k + 3, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (-2))
                                world.setBlock(minX + i, k + 2, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (-1))
                                world.setBlock(minX + i, k + 1, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (0))
                                world.setBlock(minX + i, k, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (+1))
                                world.setBlock(minX + i, k - 1, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (+2))
                                world.setBlock(minX + i, k - 2, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                            if (k > world.minY() + (+3))
                                world.setBlock(minX + i, k - 3, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
                        }
                    break;
                }
        }

        // Kill every living thing close by except Players.
        world.clearNonPlayerEntities(minX, height, -16, minX + 16, height + 8, +16);
        return T;
    }
}
