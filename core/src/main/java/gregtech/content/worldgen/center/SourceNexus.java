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

/** Original GT6 geometry, adapted by tools/import_origin_worldgen.py; platform effects use OriginWorld.
 * @author Gregorius Techneticies (original geometry)
 */
public final class SourceNexus {
    public final int height;
    private final boolean GENERATE_STREETS;

    public SourceNexus(int height, boolean streets) {
        this.height = height;
        GENERATE_STREETS = streets;
    }

    public boolean generate(OriginWorld world, OriginWorld.Chunk chunk, int minX, int minZ, java.util.Random random) {
        if (minX != 16 || minZ != -48) return F;
        final boolean[] fWindow = {F, T, T, T, T, F, T, T, T, T, F, T, T, T, T, F};
        if (GENERATE_STREETS) {
            for (int i = 0; i < 16; i++)
                for (int j = 0; j < 16; j++) {
                    for (int k = 2; k < 64; k++) set(chunk, i, height + k, j, NB, 0);
                    for (int k = world.minY() + 1; k < height; k++)
                        set(chunk, i, k, j, BlocksGT.Concrete, DYE_INDEX_Gray);
                    set(chunk, i, height + 1, j, BlocksGT.CFoam, DYE_INDEX_Gray);
                    if (i == 0 || i == 15 || j == 0 || j == 15) {
                        set(chunk, i, height + 0, j, BlocksGT.Concrete, DYE_INDEX_Gray);
                        set(chunk, i, height + 2, j, BlocksGT.Concrete, DYE_INDEX_LightGray);
                        set(chunk, i, height + 3, j, BlocksGT.Concrete, DYE_INDEX_LightGray);
                        set(chunk, i, height + 4, j, BlocksGT.Concrete, DYE_INDEX_LightGray);
                        set(chunk, i, height + 5, j, BlocksGT.Concrete, DYE_INDEX_LightGray);
                        set(chunk, i, height + 6, j, BlocksGT.Concrete, DYE_INDEX_LightGray);
                        set(chunk, i, height + 7, j, BlocksGT.CFoam, DYE_INDEX_Gray);
                        set(chunk, i, height + 8, j, BlocksGT.Concrete, DYE_INDEX_LightGray);
                        if (fWindow[i] || fWindow[j]) {
                            set(chunk, i, height + 9, j, BlocksGT.Glass, DYE_INDEX_LightBlue);
                            set(chunk, i, height + 10, j, BlocksGT.Glass, DYE_INDEX_LightBlue);
                            set(chunk, i, height + 11, j, BlocksGT.Glass, DYE_INDEX_LightBlue);
                        } else {
                            set(chunk, i, height + 9, j, BlocksGT.Concrete, DYE_INDEX_LightGray);
                            set(chunk, i, height + 10, j, BlocksGT.Concrete, DYE_INDEX_LightGray);
                            set(chunk, i, height + 11, j, BlocksGT.Concrete, DYE_INDEX_LightGray);
                        }
                        set(chunk, i, height + 12, j, BlocksGT.Concrete, DYE_INDEX_LightGray);
                    } else {
                        set(chunk, i, height + 0, j, Blocks.obsidian, 0);
                    }
                    set(chunk, i, height + 13, j, BlocksGT.CFoam, DYE_INDEX_Gray);
                }
        } else {
            int tRockType = random.nextInt(BlocksGT.stones.length);

            for (int i = 0; i < 16; i++)
                for (int j = 0; j < 16; j++) {
                    for (int k = 2; k < 64; k++) set(chunk, i, height + k, j, NB, 0);
                    for (int k = world.minY() + 1; k < height; k++) set(chunk, i, k, j, BlocksGT.stones[tRockType], 0);
                    set(chunk, i, height + 1, j, BlocksGT.stones[tRockType], BlockStones.TILES);
                    if (i == 0 || i == 15 || j == 0 || j == 15) {
                        set(chunk, i, height + 0, j, BlocksGT.stones[tRockType], 3 + random.nextInt(3));
                        set(chunk, i, height + 2, j, BlocksGT.stones[tRockType], 3 + random.nextInt(3));
                        set(chunk, i, height + 3, j, BlocksGT.stones[tRockType], 3 + random.nextInt(3));
                        set(chunk, i, height + 4, j, BlocksGT.stones[tRockType], 3 + random.nextInt(3));
                        set(chunk, i, height + 5, j, BlocksGT.stones[tRockType], 3 + random.nextInt(3));
                        set(chunk, i, height + 6, j, BlocksGT.stones[tRockType], 3 + random.nextInt(3));
                        set(chunk, i, height + 7, j, BlocksGT.stones[tRockType], BlockStones.CHISL);
                        set(chunk, i, height + 8, j, BlocksGT.stones[tRockType], 3 + random.nextInt(3));
                        if (fWindow[i] || fWindow[j]) {
                            set(chunk, i, height + 9, j, Blocks.glass_pane, 0);
                            set(chunk, i, height + 10, j, Blocks.glass_pane, 0);
                            set(chunk, i, height + 11, j, Blocks.glass_pane, 0);
                        } else {
                            set(chunk, i, height + 9, j, BlocksGT.stones[tRockType], 3 + random.nextInt(3));
                            set(chunk, i, height + 10, j, BlocksGT.stones[tRockType], 3 + random.nextInt(3));
                            set(chunk, i, height + 11, j, BlocksGT.stones[tRockType], 3 + random.nextInt(3));
                        }
                        set(chunk, i, height + 12, j, BlocksGT.stones[tRockType], 3 + random.nextInt(3));
                    } else {
                        set(chunk, i, height + 0, j, Blocks.obsidian, 0);
                    }
                    set(chunk, i, height + 13, j, BlocksGT.stones[tRockType], BlockStones.STILE);
                }
        }

        //      set(chunk, 5, height+1,15, GENERATE_STREETS?BlocksGT.CFoam:Blocks.stained_hardened_clay,
        // DYE_INDEX_Gray);
        //      set(chunk, 6, height+1,15, GENERATE_STREETS?BlocksGT.CFoam:Blocks.stained_hardened_clay,
        // DYE_INDEX_Gray);
        //      set(chunk, 7, height+1,15, GENERATE_STREETS?BlocksGT.CFoam:Blocks.stained_hardened_clay,
        // DYE_INDEX_Gray);
        //      set(chunk, 8, height+1,15, GENERATE_STREETS?BlocksGT.CFoam:Blocks.stained_hardened_clay,
        // DYE_INDEX_Gray);
        //      set(chunk, 9, height+1,15, GENERATE_STREETS?BlocksGT.CFoam:Blocks.stained_hardened_clay,
        // DYE_INDEX_Gray);
        //      set(chunk,10, height+1,15, GENERATE_STREETS?BlocksGT.CFoam:Blocks.stained_hardened_clay,
        // DYE_INDEX_Gray);
        set(chunk, 5, height + 2, 15, GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay, DYE_INDEX_Gray);
        set(chunk, 6, height + 2, 15, NB, 0);
        set(chunk, 7, height + 2, 15, NB, 0);
        set(chunk, 8, height + 2, 15, NB, 0);
        set(chunk, 9, height + 2, 15, NB, 0);
        set(
                chunk,
                10,
                height + 2,
                15,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Gray);
        set(chunk, 5, height + 3, 15, GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay, DYE_INDEX_Gray);
        set(chunk, 6, height + 3, 15, NB, 0);
        set(chunk, 7, height + 3, 15, NB, 0);
        set(chunk, 8, height + 3, 15, NB, 0);
        set(chunk, 9, height + 3, 15, NB, 0);
        set(
                chunk,
                10,
                height + 3,
                15,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Gray);
        set(chunk, 5, height + 4, 15, GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay, DYE_INDEX_Gray);
        set(chunk, 6, height + 4, 15, NB, 0);
        set(chunk, 7, height + 4, 15, NB, 0);
        set(chunk, 8, height + 4, 15, NB, 0);
        set(chunk, 9, height + 4, 15, NB, 0);
        set(
                chunk,
                10,
                height + 4,
                15,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Gray);
        set(chunk, 5, height + 5, 15, GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay, DYE_INDEX_Gray);
        set(chunk, 6, height + 5, 15, GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay, DYE_INDEX_Gray);
        set(chunk, 7, height + 5, 15, GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay, DYE_INDEX_Gray);
        set(chunk, 8, height + 5, 15, GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay, DYE_INDEX_Gray);
        set(chunk, 9, height + 5, 15, GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay, DYE_INDEX_Gray);
        set(
                chunk,
                10,
                height + 5,
                15,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Gray);

        //      set(chunk, 6, height+1,14, GENERATE_STREETS?BlocksGT.CFoam:Blocks.stained_hardened_clay,
        // DYE_INDEX_Gray);
        //      set(chunk, 7, height+1,14, GENERATE_STREETS?BlocksGT.CFoam:Blocks.stained_hardened_clay,
        // DYE_INDEX_Gray);
        //      set(chunk, 8, height+1,14, GENERATE_STREETS?BlocksGT.CFoam:Blocks.stained_hardened_clay,
        // DYE_INDEX_Gray);
        //      set(chunk, 9, height+1,14, GENERATE_STREETS?BlocksGT.CFoam:Blocks.stained_hardened_clay,
        // DYE_INDEX_Gray);
        set(chunk, 6, height + 2, 14, GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay, DYE_INDEX_Gray);
        set(chunk, 9, height + 2, 14, GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay, DYE_INDEX_Gray);
        set(chunk, 6, height + 3, 14, GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay, DYE_INDEX_Gray);
        set(chunk, 9, height + 3, 14, GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay, DYE_INDEX_Gray);
        set(chunk, 6, height + 4, 14, GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay, DYE_INDEX_Gray);
        set(chunk, 7, height + 4, 14, GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay, DYE_INDEX_Gray);
        set(chunk, 8, height + 4, 14, GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay, DYE_INDEX_Gray);
        set(chunk, 9, height + 4, 14, GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay, DYE_INDEX_Gray);

        set(chunk, 6, height + 1, 1, Blocks.obsidian, 0);
        set(chunk, 7, height + 1, 1, Blocks.obsidian, 0);
        set(chunk, 8, height + 1, 1, Blocks.obsidian, 0);
        set(chunk, 9, height + 1, 1, Blocks.obsidian, 0);
        set(chunk, 6, height + 2, 1, Blocks.obsidian, 0);
        set(chunk, 9, height + 2, 1, Blocks.obsidian, 0);
        set(chunk, 6, height + 3, 1, Blocks.obsidian, 0);
        set(chunk, 9, height + 3, 1, Blocks.obsidian, 0);
        set(chunk, 6, height + 4, 1, Blocks.obsidian, 0);
        set(chunk, 9, height + 4, 1, Blocks.obsidian, 0);
        set(chunk, 6, height + 5, 1, Blocks.glowstone, 0);
        set(chunk, 7, height + 5, 1, Blocks.obsidian, 0);
        set(chunk, 8, height + 5, 1, Blocks.obsidian, 0);
        set(chunk, 9, height + 5, 1, Blocks.glowstone, 0);

        // -----

        set(chunk, 1, height + 1, 1, Blocks.glowstone, 0);
        set(chunk, 1, height + 1, 2, Blocks.end_portal_frame, 3);
        set(chunk, 1, height + 1, 3, Blocks.end_portal_frame, 3);
        set(chunk, 1, height + 1, 4, Blocks.end_portal_frame, 3);
        set(chunk, 1, height + 1, 5, Blocks.glowstone, 0);
        set(chunk, 2, height + 1, 1, Blocks.end_portal_frame, 0);
        set(chunk, 2, height + 1, 2, NB, 0);
        set(chunk, 2, height + 1, 3, NB, 0);
        set(chunk, 2, height + 1, 4, NB, 0);
        set(chunk, 2, height + 1, 5, Blocks.end_portal_frame, 2);
        set(chunk, 3, height + 1, 1, Blocks.end_portal_frame, 0);
        set(chunk, 3, height + 1, 2, NB, 0);
        set(chunk, 3, height + 1, 3, NB, 0);
        set(chunk, 3, height + 1, 4, NB, 0);
        set(chunk, 3, height + 1, 5, Blocks.end_portal_frame, 2);
        set(chunk, 4, height + 1, 1, Blocks.end_portal_frame, 0);
        set(chunk, 4, height + 1, 2, NB, 0);
        set(chunk, 4, height + 1, 3, NB, 0);
        set(chunk, 4, height + 1, 4, NB, 0);
        set(chunk, 4, height + 1, 5, Blocks.end_portal_frame, 2);
        set(chunk, 5, height + 1, 1, Blocks.glowstone, 0);
        set(chunk, 5, height + 1, 2, Blocks.end_portal_frame, 1);
        set(chunk, 5, height + 1, 3, Blocks.end_portal_frame, 1);
        set(chunk, 5, height + 1, 4, Blocks.end_portal_frame, 1);
        set(chunk, 5, height + 1, 5, Blocks.glowstone, 0);

        // -----

        set(chunk, 10, height + 1, 1, Blocks.glowstone, 0);
        set(
                chunk,
                10,
                height + 1,
                2,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Black);
        set(
                chunk,
                10,
                height + 1,
                3,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Black);
        set(
                chunk,
                10,
                height + 1,
                4,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Black);
        set(chunk, 10, height + 1, 5, Blocks.glowstone, 0);
        set(chunk, 11, height + 1, 1, Blocks.grass, 0);
        set(chunk, 11, height + 1, 2, Blocks.grass, 0);
        set(chunk, 11, height + 1, 3, Blocks.grass, 0);
        set(chunk, 11, height + 1, 4, Blocks.grass, 0);
        set(
                chunk,
                11,
                height + 1,
                5,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Black);
        set(chunk, 12, height + 1, 1, Blocks.grass, 0);
        set(chunk, 12, height + 1, 2, Blocks.water, 0);
        set(chunk, 12, height + 1, 3, Blocks.water, 0);
        set(chunk, 12, height + 1, 4, Blocks.grass, 0);
        set(
                chunk,
                12,
                height + 1,
                5,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Black);
        set(chunk, 13, height + 1, 1, Blocks.grass, 0);
        set(chunk, 13, height + 1, 2, Blocks.water, 0);
        set(chunk, 13, height + 1, 3, Blocks.water, 0);
        set(chunk, 13, height + 1, 4, Blocks.grass, 0);
        set(
                chunk,
                13,
                height + 1,
                5,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Black);
        set(chunk, 14, height + 1, 1, Blocks.grass, 0);
        set(chunk, 14, height + 1, 2, Blocks.grass, 0);
        set(chunk, 14, height + 1, 3, Blocks.grass, 0);
        set(chunk, 14, height + 1, 4, Blocks.grass, 0);
        set(chunk, 14, height + 1, 5, Blocks.glowstone, 0);
        set(chunk, 11, height + 2, 1, Blocks.yellow_flower, 0);
        set(chunk, 11, height + 2, 2, Blocks.yellow_flower, 0);
        set(chunk, 11, height + 2, 3, Blocks.yellow_flower, 0);
        set(chunk, 11, height + 2, 4, Blocks.yellow_flower, 0);
        set(chunk, 12, height + 2, 1, Blocks.yellow_flower, 0);
        set(chunk, 12, height + 2, 4, Blocks.yellow_flower, 0);
        set(chunk, 13, height + 2, 1, Blocks.yellow_flower, 0);
        set(chunk, 13, height + 2, 4, Blocks.yellow_flower, 0);
        set(chunk, 14, height + 2, 1, Blocks.yellow_flower, 0);
        set(chunk, 14, height + 2, 2, Blocks.yellow_flower, 0);
        set(chunk, 14, height + 2, 3, Blocks.yellow_flower, 0);
        set(chunk, 14, height + 2, 4, Blocks.yellow_flower, 0);

        // -----

        set(chunk, 1, height + 0, 10, Blocks.sandstone, 2);
        set(chunk, 1, height + 0, 11, Blocks.sandstone, 2);
        set(chunk, 1, height + 0, 12, Blocks.sandstone, 2);
        set(chunk, 1, height + 0, 13, Blocks.sandstone, 2);
        set(chunk, 1, height + 0, 14, Blocks.sandstone, 2);
        set(chunk, 2, height + 0, 10, Blocks.sandstone, 2);
        set(chunk, 2, height + 0, 11, Blocks.sandstone, 2);
        set(chunk, 2, height + 0, 12, Blocks.sandstone, 2);
        set(chunk, 2, height + 0, 13, Blocks.sandstone, 2);
        set(chunk, 2, height + 0, 14, Blocks.sandstone, 2);
        set(chunk, 3, height + 0, 10, Blocks.sandstone, 2);
        set(chunk, 3, height + 0, 11, Blocks.sandstone, 2);
        set(chunk, 3, height + 0, 12, Blocks.sandstone, 2);
        set(chunk, 3, height + 0, 13, Blocks.sandstone, 2);
        set(chunk, 3, height + 0, 14, Blocks.sandstone, 2);
        set(chunk, 4, height + 0, 10, Blocks.sandstone, 2);
        set(chunk, 4, height + 0, 11, Blocks.sandstone, 2);
        set(chunk, 4, height + 0, 12, Blocks.sandstone, 2);
        set(chunk, 4, height + 0, 13, Blocks.sandstone, 2);
        set(chunk, 4, height + 0, 14, Blocks.sandstone, 2);
        set(chunk, 5, height + 0, 10, Blocks.sandstone, 2);
        set(chunk, 5, height + 0, 11, Blocks.sandstone, 2);
        set(chunk, 5, height + 0, 12, Blocks.sandstone, 2);
        set(chunk, 5, height + 0, 13, Blocks.sandstone, 2);
        set(chunk, 5, height + 0, 14, Blocks.sandstone, 2);

        set(chunk, 1, height + 1, 10, Blocks.sandstone, 2);
        set(chunk, 1, height + 1, 11, Blocks.sandstone, 2);
        set(chunk, 1, height + 1, 12, Blocks.sandstone, 2);
        set(chunk, 1, height + 1, 13, Blocks.sandstone, 2);
        set(chunk, 1, height + 1, 14, Blocks.sandstone, 2);
        set(chunk, 2, height + 1, 10, Blocks.sandstone, 2);
        set(chunk, 2, height + 1, 11, NB, 0);
        set(chunk, 2, height + 1, 12, NB, 0);
        set(chunk, 2, height + 1, 13, NB, 0);
        set(chunk, 2, height + 1, 14, Blocks.sandstone, 2);
        set(chunk, 3, height + 1, 10, Blocks.sandstone, 2);
        set(chunk, 3, height + 1, 11, NB, 0);
        set(chunk, 3, height + 1, 12, NB, 0);
        set(chunk, 3, height + 1, 13, NB, 0);
        set(chunk, 3, height + 1, 14, Blocks.sandstone, 2);
        set(chunk, 4, height + 1, 10, Blocks.sandstone, 2);
        set(chunk, 4, height + 1, 11, NB, 0);
        set(chunk, 4, height + 1, 12, NB, 0);
        set(chunk, 4, height + 1, 13, NB, 0);
        set(chunk, 4, height + 1, 14, Blocks.sandstone, 2);
        set(chunk, 5, height + 1, 10, Blocks.sandstone, 2);
        set(chunk, 5, height + 1, 11, Blocks.sandstone, 2);
        set(chunk, 5, height + 1, 12, Blocks.sandstone, 2);
        set(chunk, 5, height + 1, 13, Blocks.sandstone, 2);
        set(chunk, 5, height + 1, 14, Blocks.sandstone, 2);

        set(chunk, 1, height + 2, 10, Blocks.sandstone, 2);
        set(chunk, 1, height + 2, 14, Blocks.sandstone, 2);
        set(chunk, 5, height + 2, 10, Blocks.sandstone, 2);
        set(chunk, 5, height + 2, 14, Blocks.sandstone, 2);

        set(chunk, 1, height + 3, 10, Blocks.sandstone, 1);
        set(chunk, 1, height + 3, 14, Blocks.sandstone, 1);
        set(chunk, 5, height + 3, 10, Blocks.sandstone, 1);
        set(chunk, 5, height + 3, 14, Blocks.sandstone, 1);

        set(chunk, 1, height + 4, 10, Blocks.glowstone, 0);
        set(chunk, 1, height + 4, 14, Blocks.glowstone, 0);
        set(chunk, 5, height + 4, 10, Blocks.glowstone, 0);
        set(chunk, 5, height + 4, 14, Blocks.glowstone, 0);

        // -----

        set(chunk, 10, height + 1, 10, Blocks.glowstone, 0);
        set(
                chunk,
                10,
                height + 1,
                11,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Black);
        set(
                chunk,
                10,
                height + 1,
                12,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Black);
        set(
                chunk,
                10,
                height + 1,
                13,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Black);
        set(chunk, 10, height + 1, 14, Blocks.glowstone, 0);
        set(
                chunk,
                11,
                height + 1,
                14,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Black);
        set(
                chunk,
                12,
                height + 1,
                14,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Black);
        set(
                chunk,
                13,
                height + 1,
                14,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Black);
        set(chunk, 14, height + 1, 14, Blocks.glowstone, 0);
        set(
                chunk,
                14,
                height + 1,
                13,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Black);
        set(
                chunk,
                14,
                height + 1,
                12,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Black);
        set(
                chunk,
                14,
                height + 1,
                11,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Black);
        set(chunk, 14, height + 1, 10, Blocks.glowstone, 0);
        set(
                chunk,
                13,
                height + 1,
                10,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Black);
        set(
                chunk,
                12,
                height + 1,
                10,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Black);
        set(
                chunk,
                11,
                height + 1,
                10,
                GENERATE_STREETS ? BlocksGT.CFoam : Blocks.stained_hardened_clay,
                DYE_INDEX_Black);

        world.setSpawnLocation(0, height + 5, 0);
        return T;
    }
}
