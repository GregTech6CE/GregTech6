/**
 * Copyright (c) 2021 GregTech-6 Team
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

/** Shared centre-biome layout adapted from Gregorius Techneticies; native tree and block-state effects are delegated. */
public final class SourceCenterBiomes {
    private final int height;
    private static final boolean GENERATE_STREETS = true, GENERATE_NEXUS = true;

    public SourceCenterBiomes(int height) {
        this.height = height;
    }

    public boolean generate(OriginWorld world, OriginWorld.Chunk chunk, int minX, int minZ, java.util.Random random) {
        if (minX >= -96 && minX <= 80 && minZ >= -96 && minZ <= 80) {
            if (GENERATE_STREETS && minX >= -32 && minX <= 16 && minZ >= -32 && minZ <= 16) {
                world.biome(chunk, "river");
                return T;
            }
            if (GENERATE_NEXUS && minX == 16 && minZ == -48) {
                world.biome(chunk, "plains");
                return T;
            }
            if (minX == -16 || minX == 0 || minZ == -16 || minZ == 0) {
                world.biome(chunk, "river");
                for (int i = 0; i < 16; i++)
                    for (int j = 0; j < 16; j++) {
                        for (int k = -3; k < world.maxY() - height; k++) set(chunk, i, height + k, j, NB, 0);
                        set(chunk, i, height - 4, j, BlocksGT.River, 0);
                        set(chunk, i, height - 5, j, BlocksGT.River, 0);
                        set(chunk, i, height - 6, j, BlocksGT.River, 0);
                        set(chunk, i, height - 7, j, BlocksGT.Sands, minX < 0 ? minZ < 0 ? 0 : 1 : minZ < 0 ? 2 : 0);
                        set(chunk, i, height - 8, j, Blocks.gravel, 1);
                        set(chunk, i, height - 9, j, Blocks.clay, 0);
                        set(chunk, i, height - 10, j, Blocks.clay, 0);
                        for (int k = world.minY() + 1; k < height - 10; k++) set(chunk, i, k, j, Blocks.stone, 1);
                    }
                world.setSpawnLocation(0, height + 5, 0);
                return T;
            }
            if (minX < 0) {
                if (minZ < 0) {
                    if ((minX == -80 || minX == -64) && (minZ == -80 || minZ == -64)) {
                        world.biome(chunk, "snowy_plains");
                        for (int i = 0; i < 16; i++)
                            for (int j = 0; j < 16; j++) {
                                for (int k = 1; k < world.maxY() - height; k++) set(chunk, i, height + k, j, NB, 0);
                                set(chunk, i, height, j, Blocks.ice, 0);
                                set(chunk, i, height - 1, j, Blocks.packed_ice, 0);
                                set(chunk, i, height - 2, j, Blocks.packed_ice, 0);
                                set(chunk, i, height - 3, j, Blocks.packed_ice, 0);
                                set(chunk, i, height - 4, j, Blocks.packed_ice, 0);
                                set(chunk, i, height - 5, j, Blocks.packed_ice, 0);
                                for (int k = world.minY() + 1; k < height - 5; k++)
                                    set(
                                            chunk,
                                            i,
                                            k,
                                            j,
                                            k < 32 ? BlocksGT.SchistGreen : BlocksGT.SchistBlue,
                                            random.nextBoolean() ? 2 : 0);
                            }
                    } else {
                        world.biome(chunk, "snowy_taiga");
                        for (int i = 0; i < 16; i++)
                            for (int j = 0; j < 16; j++) {
                                for (int k = 2; k < world.maxY() - height; k++) set(chunk, i, height + k, j, NB, 0);
                                set(chunk, i, height + 1, j, Blocks.snow_layer, random.nextInt(2));
                                set(chunk, i, height, j, Blocks.dirt, 2);
                                set(chunk, i, height - 1, j, Blocks.dirt, 2);
                                set(chunk, i, height - 2, j, Blocks.dirt, 2);
                                set(chunk, i, height - 3, j, Blocks.dirt, 2);
                                set(chunk, i, height - 4, j, Blocks.dirt, 2);
                                set(chunk, i, height - 5, j, Blocks.dirt, 2);
                                for (int k = world.minY() + 1; k < height - 5; k++)
                                    set(chunk, i, k, j, Blocks.mossy_cobblestone, 0);
                            }

                        set(chunk, 4, height + 1, 4, NB, random.nextInt(2));
                        set(chunk, 12, height + 1, 4, NB, random.nextInt(2));
                        set(chunk, 4, height + 1, 12, NB, random.nextInt(2));
                        set(chunk, 12, height + 1, 12, NB, random.nextInt(2));

                        world.tree(minX + 4, height + 1, minZ + 4, 1, 4 + random.nextInt(3), random);
                        world.tree(minX + 12, height + 1, minZ + 4, 1, 4 + random.nextInt(3), random);
                        world.tree(minX + 4, height + 1, minZ + 12, 1, 4 + random.nextInt(3), random);
                        world.tree(minX + 12, height + 1, minZ + 12, 1, 4 + random.nextInt(3), random);
                    }
                } else {
                    if ((minX == -80 || minX == -64) && (minZ == 48 || minZ == 64)) {
                        world.biome(chunk, "forest");
                        for (int i = 0; i < 16; i++)
                            for (int j = 0; j < 16; j++) {
                                for (int k = 1; k < world.maxY() - height; k++) set(chunk, i, height + k, j, NB, 0);
                                set(chunk, i, height, j, Blocks.grass, 0);
                                set(chunk, i, height - 1, j, Blocks.dirt, 0);
                                set(chunk, i, height - 2, j, Blocks.dirt, 0);
                                set(chunk, i, height - 3, j, Blocks.dirt, 0);
                                set(chunk, i, height - 4, j, Blocks.dirt, 0);
                                set(chunk, i, height - 5, j, Blocks.dirt, 0);
                                for (int k = world.minY() + 1; k < height - 5; k++)
                                    set(
                                            chunk,
                                            i,
                                            k,
                                            j,
                                            k < 32 ? BlocksGT.Kimberlite : BlocksGT.Quartzite,
                                            random.nextBoolean() ? 2 : 0);
                            }
                        set(chunk, 6, height + 1, 6, Blocks.pumpkin, 0);
                        set(chunk, 10, height + 1, 6, Blocks.pumpkin, 0);
                        set(chunk, 6, height + 1, 10, Blocks.pumpkin, 0);
                        set(chunk, 10, height + 1, 10, Blocks.pumpkin, 0);

                        world.tree(minX + 4, height + 1, minZ + 4, 0, 4 + random.nextInt(3), random);
                        world.tree(minX + 12, height + 1, minZ + 4, 2, 4 + random.nextInt(3), random);
                        world.tree(minX + 4, height + 1, minZ + 12, 2, 4 + random.nextInt(3), random);
                        world.tree(minX + 12, height + 1, minZ + 12, 0, 4 + random.nextInt(3), random);
                    } else {

                        world.biome(chunk, "plains");
                        for (int i = 0; i < 16; i++)
                            for (int j = 0; j < 16; j++) {
                                for (int k = 1; k < world.maxY() - height; k++) set(chunk, i, height + k, j, NB, 0);
                                set(chunk, i, height, j, Blocks.grass, 0);
                                set(chunk, i, height - 1, j, Blocks.dirt, 0);
                                set(chunk, i, height - 2, j, BlocksGT.Diggables, 1);
                                set(chunk, i, height - 3, j, BlocksGT.Diggables, 3);
                                set(chunk, i, height - 4, j, BlocksGT.Diggables, 4);
                                set(chunk, i, height - 5, j, BlocksGT.Diggables, 5);
                                set(chunk, i, height - 6, j, BlocksGT.Diggables, 6);
                                for (int k = world.minY() + 1; k < height - 6; k++)
                                    set(
                                            chunk,
                                            i,
                                            k,
                                            j,
                                            k < 32 ? BlocksGT.Limestone : BlocksGT.Marble,
                                            random.nextBoolean() ? 2 : 0);
                                switch (random.nextInt(60)) {
                                    case 0:
                                    case 1:
                                    case 2:
                                        world.litter(minX + i, height + 1, minZ + j, 32757, true);
                                        break;
                                    case 3:
                                    case 4:
                                    case 5:
                                        world.litter(minX + i, height + 1, minZ + j, 32757, false);
                                        break;
                                    case 6:
                                    case 7:
                                    case 8:
                                        world.litter(minX + i, height + 1, minZ + j, 32756, false);
                                        break;
                                    case 9:
                                    case 10:
                                    case 11:
                                    case 12:
                                    case 13:
                                    case 14:
                                        set(chunk, i, height + 1, j, Blocks.tallgrass, 1);
                                        break;
                                    case 15:
                                        set(chunk, i, height + 1, j, Blocks.yellow_flower, 0);
                                        break;
                                    case 16:
                                        set(chunk, i, height + 1, j, Blocks.red_flower, 0);
                                        break;
                                    case 17:
                                        set(chunk, i, height + 1, j, Blocks.red_flower, 1);
                                        break;
                                    case 18:
                                        set(chunk, i, height + 1, j, Blocks.red_flower, 2);
                                        break;
                                    case 19:
                                        set(chunk, i, height + 1, j, Blocks.red_flower, 3);
                                        break;
                                    case 20:
                                        set(chunk, i, height + 1, j, Blocks.red_flower, 4);
                                        break;
                                    case 21:
                                        set(chunk, i, height + 1, j, Blocks.red_flower, 5);
                                        break;
                                    case 22:
                                        set(chunk, i, height + 1, j, Blocks.red_flower, 6);
                                        break;
                                    case 23:
                                        set(chunk, i, height + 1, j, Blocks.red_flower, 7);
                                        break;
                                    case 24:
                                        set(chunk, i, height + 1, j, Blocks.red_flower, 8);
                                        break;
                                }
                            }
                    }
                }
            } else {
                if (minZ < 0) {
                    if ((minX == 48 || minX == 64) && (minZ == -80 || minZ == -64)) {
                        world.biome(chunk, "badlands");
                        for (int i = 0; i < 16; i++)
                            for (int j = 0; j < 16; j++) {
                                for (int k = 1; k < world.maxY() - height; k++) set(chunk, i, height + k, j, NB, 0);
                                set(chunk, i, height, j, Blocks.sand, 1);
                                set(chunk, i, height - 1, j, Blocks.sand, 1);
                                set(chunk, i, height - 2, j, Blocks.sand, 1);
                                set(chunk, i, height - 3, j, Blocks.sand, 1);
                                set(chunk, i, height - 4, j, Blocks.sand, 1);
                                set(chunk, i, height - 5, j, Blocks.sand, 1);
                                for (int k = world.minY() + 1; k < height - 5; k++)
                                    set(chunk, i, k, j, Blocks.hardened_clay, 0);
                            }
                        for (int i = 1; i <= 3; i++) {
                            set(chunk, 4, height + i, 4, Blocks.cactus, 0);
                            set(chunk, 12, height + i, 4, Blocks.cactus, 0);
                            set(chunk, 4, height + i, 12, Blocks.cactus, 0);
                            set(chunk, 12, height + i, 12, Blocks.cactus, 0);
                        }
                    } else {
                        world.biome(chunk, "desert");
                        for (int i = 0; i < 16; i++)
                            for (int j = 0; j < 16; j++) {
                                for (int k = 1; k < world.maxY() - height; k++) set(chunk, i, height + k, j, NB, 0);
                                set(chunk, i, height, j, Blocks.sand, 0);
                                set(chunk, i, height - 1, j, Blocks.sand, 0);
                                set(chunk, i, height - 2, j, Blocks.sand, 0);
                                set(chunk, i, height - 3, j, Blocks.sand, 0);
                                set(chunk, i, height - 4, j, Blocks.sand, 0);
                                set(chunk, i, height - 5, j, Blocks.sand, 0);
                                for (int k = world.minY() + 1; k < height - 5; k++)
                                    set(chunk, i, k, j, Blocks.sandstone, 0);
                            }
                    }
                } else {
                    if ((minX == 48 || minX == 64) && (minZ == 48 || minZ == 64)) {
                        world.biome(chunk, "swamp");
                        for (int i = 0; i < 16; i++)
                            for (int j = 0; j < 16; j++) {
                                for (int k = 1; k < world.maxY() - height; k++) set(chunk, i, height + k, j, NB, 0);
                                set(chunk, i, height, j, Blocks.water, 0);
                                set(chunk, i, height - 1, j, BlocksGT.Diggables, 0);
                                set(chunk, i, height - 2, j, BlocksGT.Diggables, 0);
                                set(chunk, i, height - 3, j, BlocksGT.Diggables, 2);
                                set(chunk, i, height - 4, j, BlocksGT.Diggables, 2);
                                set(chunk, i, height - 5, j, BlocksGT.Diggables, 2);
                                for (int k = world.minY() + 1; k < height - 5; k++)
                                    set(
                                            chunk,
                                            i,
                                            k,
                                            j,
                                            k < 32 ? BlocksGT.GraniteRed : BlocksGT.GraniteBlack,
                                            random.nextBoolean() ? 2 : 0);
                                if (random.nextInt(8) == 0)
                                    set(chunk, i, height + 1, j, BlocksGT.Glowtus, random.nextInt(16));
                            }
                        set(chunk, 4, height + 1, 4, Blocks.waterlily, 0);
                        set(chunk, 12, height + 1, 4, Blocks.waterlily, 0);
                        set(chunk, 4, height + 1, 12, Blocks.waterlily, 0);
                        set(chunk, 12, height + 1, 12, Blocks.waterlily, 0);
                    } else {
                        world.biome(chunk, "jungle");

                        OriginWorld.Block tBlock = Blocks.coarse_dirt;
                        for (int i = 0; i < 16; i++)
                            for (int j = 0; j < 16; j++) {
                                for (int k = 1; k < world.maxY() - height; k++) set(chunk, i, height + k, j, NB, 0);
                                set(chunk, i, height, j, Blocks.grass, 0);
                                set(chunk, i, height - 1, j, tBlock, 0);
                                set(chunk, i, height - 2, j, tBlock, 0);
                                set(chunk, i, height - 3, j, tBlock, 0);
                                set(chunk, i, height - 4, j, tBlock, 0);
                                set(chunk, i, height - 5, j, tBlock, 0);
                                for (int k = world.minY() + 1; k < height - 5; k++)
                                    set(
                                            chunk,
                                            i,
                                            k,
                                            j,
                                            k < 32 ? BlocksGT.Komatiite : BlocksGT.Basalt,
                                            random.nextBoolean() ? 2 : 0);
                            }

                        set(chunk, 6 + random.nextInt(4), height + 1, 6 + random.nextInt(4), Blocks.melon_block, 0);

                        world.tree(minX + 4, height + 1, minZ + 4, 3, 9 + random.nextInt(3), random);
                        world.tree(minX + 12, height + 1, minZ + 4, 3, 9 + random.nextInt(3), random);
                        world.tree(minX + 4, height + 1, minZ + 12, 3, 9 + random.nextInt(3), random);
                        world.tree(minX + 12, height + 1, minZ + 12, 3, 9 + random.nextInt(3), random);
                    }
                }
            }
            return T;
        }
        return F;
    }
}
