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

package com.gregtech.gregtech.worldgen.center;

import static com.gregtech.gregtech.worldgen.center.OriginSupport.*;

/** Original GT6 geometry, adapted by tools/import_origin_worldgen.py; platform effects use OriginWorld.
 * @author Gregorius Techneticies (original geometry)
 */
public final class SourceStreets {
    public final int mHeight;
    private final boolean GENERATE_BEACON, GENERATE_BIOMES, GENERATE_NEXUS, GENERATE_TESTING;
    public SourceStreets(int height, boolean beacon, boolean biomes, boolean nexus, boolean testing) {
        mHeight=height; GENERATE_BEACON=beacon; GENERATE_BIOMES=biomes;
        GENERATE_NEXUS=nexus; GENERATE_TESTING=testing;
    }
    public boolean generate(OriginWorld aWorld, int aMinX, int aMinZ,
                            int aMaxX, int aMaxZ, java.util.Set<String> aBiomeNames) {
		if (aMinX == -16 || aMinX == 0) {
			if (aMinZ == -16 || aMinZ == 0) {
				for (int i = -32; i < 32; i++) for (int j = -32; j < 32; j++) {
					for (int k = 2; k < 64; k++) aWorld.setBlock(i, mHeight+k, j, NB, 0, 0);
					for (int k = aWorld.minY()+1; k < mHeight; k++) aWorld.setBlock(i, k, j, BlocksGT.Concrete, DYE_INDEX_Gray, 0);

					aWorld.setBlock(i, mHeight-1, j, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
					if (i <= -29 || j <= -29 || i >= 28 || j >= 28) {
						if (inside(-12, 11, i) || inside(-12, 11, j)) {
							aWorld.setBlock(i, mHeight  , j, BlocksGT.Asphalt, i == -31 || j == -31 || i == 30 || j == 30 ? DYE_INDEX_White : DYE_INDEX_Gray, 0);
							aWorld.setBlock(i, mHeight+1, j, NB, 0, 0);
						} else {
							aWorld.setBlock(i, mHeight  , j, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
							if (!GENERATE_BIOMES && (i == -32 || j == -32 || i == 31 || j == 31) && (!GENERATE_NEXUS || i < 0 || i == 31 || j != -32) && (!GENERATE_TESTING || j > 0 || i != 31 || j == -32)) {
								aWorld.setBlock(i, mHeight+1, j, even(i, 1, j)?BlocksGT.CFoam:BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
								aWorld.setBlock(i, mHeight+2, j, even(i, 2, j)?BlocksGT.CFoam:BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
								aWorld.setBlock(i, mHeight+3, j, even(i, 3, j)?BlocksGT.CFoam:BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
								aWorld.setBlock(i, mHeight+4, j, even(i, 4, j)?BlocksGT.CFoam:BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
								aWorld.setBlock(i, mHeight+5, j, even(i, 5, j)?BlocksGT.CFoam:BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
							} else {
								aWorld.setBlock(i, mHeight+1, j, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
							}
						}
					} else {
						if (inside(-12, 11, i) && inside(-12, 11, j)) {
							aWorld.setBlock(i, mHeight  , j, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
							aWorld.setBlock(i, mHeight+1, j, inside(-11, 10, i) && inside(-11, 10, j) ? BlocksGT.Concrete : BlocksGT.CFoam, DYE_INDEX_LightGray, 0);
						} else {
							aWorld.setBlock(i, mHeight  , j, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
							aWorld.setBlock(i, mHeight+1, j, NB, 0, 0);
						}
					}
				}


				aWorld.setBlock(-32, mHeight+1, - 1, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
				aWorld.setBlock(-31, mHeight+1, - 1, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
				aWorld.setBlock(-32, mHeight+1,   0, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
				aWorld.setBlock(-31, mHeight+1,   0, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
				aWorld.setBlock(-32, mHeight+2, - 1, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
				aWorld.setBlock(-31, mHeight+2, - 1, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
				aWorld.setBlock(-32, mHeight+2,   0, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
				aWorld.setBlock(-31, mHeight+2,   0, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
				aWorld.setBlock(-32, mHeight+3, - 1, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
				aWorld.setBlock(-31, mHeight+3, - 1, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
				aWorld.setBlock(-32, mHeight+3,   0, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
				aWorld.setBlock(-31, mHeight+3,   0, BlocksGT.Concrete, DYE_INDEX_Blue, 0);
				aWorld.setBlock(-32, mHeight+4, - 1, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
				aWorld.setBlock(-31, mHeight+4, - 1, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
				aWorld.setBlock(-32, mHeight+4,   0, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
				aWorld.setBlock(-31, mHeight+4,   0, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
				aWorld.setBlock(-32, mHeight+1, - 2, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_White, 0);
				aWorld.setBlock(-31, mHeight+1, - 2, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_Red, 0);
				aWorld.setBlock(-30, mHeight+1, - 1, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_White, 0);
				aWorld.setBlock(-30, mHeight+1,   0, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_Red, 0);
				aWorld.setBlock(-31, mHeight+1,   1, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_White, 0);
				aWorld.setBlock(-32, mHeight+1,   1, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_Red, 0);
				try {
				aWorld.sign( -30, mHeight+3, 0, SIDE_X_POS, 0
				, text(aWorld.getBiomeGenForCoords(-4096, +95))
				, text(aWorld.getBiomeGenForCoords(-3584, +95))
				, text(aWorld.getBiomeGenForCoords(-3072, +95))
				, text(aWorld.getBiomeGenForCoords(-2560, +95))
				);
				aWorld.sign( -30, mHeight+2, 0, SIDE_X_POS, 0
				, text(aWorld.getBiomeGenForCoords(-2048, +95))
				, text(aWorld.getBiomeGenForCoords(-1536, +95))
				, text(aWorld.getBiomeGenForCoords(-1024, +95))
				, text(aWorld.getBiomeGenForCoords(- 512, +95))
				);

				aWorld.sign( -30, mHeight+3, -1, SIDE_X_POS, 0
				, text(aWorld.getBiomeGenForCoords(-4096, -96))
				, text(aWorld.getBiomeGenForCoords(-3584, -96))
				, text(aWorld.getBiomeGenForCoords(-3072, -96))
				, text(aWorld.getBiomeGenForCoords(-2560, -96))
				);
				aWorld.sign( -30, mHeight+2, -1, SIDE_X_POS, 0
				, text(aWorld.getBiomeGenForCoords(-2048, -96))
				, text(aWorld.getBiomeGenForCoords(-1536, -96))
				, text(aWorld.getBiomeGenForCoords(-1024, -96))
				, text(aWorld.getBiomeGenForCoords(- 512, -96))
				);
				} catch(Throwable e) {e.printStackTrace(System.err);}


				aWorld.setBlock( 31, mHeight+1, - 1, BlocksGT.Concrete, DYE_INDEX_Red, 0);
				aWorld.setBlock( 30, mHeight+1, - 1, BlocksGT.Concrete, DYE_INDEX_Red, 0);
				aWorld.setBlock( 31, mHeight+1,   0, BlocksGT.Concrete, DYE_INDEX_Red, 0);
				aWorld.setBlock( 30, mHeight+1,   0, BlocksGT.Concrete, DYE_INDEX_Red, 0);
				aWorld.setBlock( 31, mHeight+2, - 1, BlocksGT.Concrete, DYE_INDEX_Red, 0);
				aWorld.setBlock( 30, mHeight+2, - 1, BlocksGT.Concrete, DYE_INDEX_Red, 0);
				aWorld.setBlock( 31, mHeight+2,   0, BlocksGT.Concrete, DYE_INDEX_Red, 0);
				aWorld.setBlock( 30, mHeight+2,   0, BlocksGT.Concrete, DYE_INDEX_Red, 0);
				aWorld.setBlock( 31, mHeight+3, - 1, BlocksGT.Concrete, DYE_INDEX_Red, 0);
				aWorld.setBlock( 30, mHeight+3, - 1, BlocksGT.Concrete, DYE_INDEX_Red, 0);
				aWorld.setBlock( 31, mHeight+3,   0, BlocksGT.Concrete, DYE_INDEX_Red, 0);
				aWorld.setBlock( 30, mHeight+3,   0, BlocksGT.Concrete, DYE_INDEX_Red, 0);
				aWorld.setBlock( 31, mHeight+4, - 1, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
				aWorld.setBlock( 30, mHeight+4, - 1, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
				aWorld.setBlock( 31, mHeight+4,   0, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
				aWorld.setBlock( 30, mHeight+4,   0, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
				aWorld.setBlock( 31, mHeight+1, - 2, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_Red, 0);
				aWorld.setBlock( 30, mHeight+1, - 2, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_White, 0);
				aWorld.setBlock( 29, mHeight+1, - 1, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_Red, 0);
				aWorld.setBlock( 29, mHeight+1,   0, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_White, 0);
				aWorld.setBlock( 30, mHeight+1,   1, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_Red, 0);
				aWorld.setBlock( 31, mHeight+1,   1, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_White, 0);
				try {
				aWorld.sign(  29, mHeight+3,   0, SIDE_X_NEG, 0
				, text(aWorld.getBiomeGenForCoords(+4095, +95))
				, text(aWorld.getBiomeGenForCoords(+3583, +95))
				, text(aWorld.getBiomeGenForCoords(+3071, +95))
				, text(aWorld.getBiomeGenForCoords(+2559, +95))
				);
				aWorld.sign(  29, mHeight+2,   0, SIDE_X_NEG, 0
				, text(aWorld.getBiomeGenForCoords(+2047, +95))
				, text(aWorld.getBiomeGenForCoords(+1535, +95))
				, text(aWorld.getBiomeGenForCoords(+1023, +95))
				, text(aWorld.getBiomeGenForCoords(+ 511, +95))
				);

				aWorld.sign(  29, mHeight+3, - 1, SIDE_X_NEG, 0
				, text(aWorld.getBiomeGenForCoords(+4095, -96))
				, text(aWorld.getBiomeGenForCoords(+3583, -96))
				, text(aWorld.getBiomeGenForCoords(+3071, -96))
				, text(aWorld.getBiomeGenForCoords(+2559, -96))
				);
				aWorld.sign(  29, mHeight+2, - 1, SIDE_X_NEG, 0
				, text(aWorld.getBiomeGenForCoords(+2047, -96))
				, text(aWorld.getBiomeGenForCoords(+1535, -96))
				, text(aWorld.getBiomeGenForCoords(+1023, -96))
				, text(aWorld.getBiomeGenForCoords(+ 511, -96))
				);
				} catch(Throwable e) {e.printStackTrace(System.err);}


				aWorld.setBlock(- 1, mHeight+1, -32, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
				aWorld.setBlock(- 1, mHeight+1, -31, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
				aWorld.setBlock(  0, mHeight+1, -32, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
				aWorld.setBlock(  0, mHeight+1, -31, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
				aWorld.setBlock(- 1, mHeight+2, -32, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
				aWorld.setBlock(- 1, mHeight+2, -31, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
				aWorld.setBlock(  0, mHeight+2, -32, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
				aWorld.setBlock(  0, mHeight+2, -31, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
				aWorld.setBlock(- 1, mHeight+3, -32, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
				aWorld.setBlock(- 1, mHeight+3, -31, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
				aWorld.setBlock(  0, mHeight+3, -32, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
				aWorld.setBlock(  0, mHeight+3, -31, BlocksGT.Concrete, DYE_INDEX_Yellow, 0);
				aWorld.setBlock(- 1, mHeight+4, -32, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
				aWorld.setBlock(- 1, mHeight+4, -31, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
				aWorld.setBlock(  0, mHeight+4, -32, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
				aWorld.setBlock(  0, mHeight+4, -31, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
				aWorld.setBlock(- 2, mHeight+1, -32, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_White, 0);
				aWorld.setBlock(- 2, mHeight+1, -31, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_Red, 0);
				aWorld.setBlock(- 1, mHeight+1, -30, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_White, 0);
				aWorld.setBlock(  0, mHeight+1, -30, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_Red, 0);
				aWorld.setBlock(  1, mHeight+1, -31, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_White, 0);
				aWorld.setBlock(  1, mHeight+1, -32, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_Red, 0);
				try {
				aWorld.sign(   0, mHeight+3, -30, SIDE_Z_POS, 0
				, text(aWorld.getBiomeGenForCoords(+95, -4096))
				, text(aWorld.getBiomeGenForCoords(+95, -3584))
				, text(aWorld.getBiomeGenForCoords(+95, -3072))
				, text(aWorld.getBiomeGenForCoords(+95, -2560))
				);
				aWorld.sign(   0, mHeight+2, -30, SIDE_Z_POS, 0
				, text(aWorld.getBiomeGenForCoords(+95, -2048))
				, text(aWorld.getBiomeGenForCoords(+95, -1536))
				, text(aWorld.getBiomeGenForCoords(+95, -1024))
				, text(aWorld.getBiomeGenForCoords(+95, - 512))
				);

				aWorld.sign( - 1, mHeight+3, -30, SIDE_Z_POS, 0
				, text(aWorld.getBiomeGenForCoords(-96, -4096))
				, text(aWorld.getBiomeGenForCoords(-96, -3584))
				, text(aWorld.getBiomeGenForCoords(-96, -3072))
				, text(aWorld.getBiomeGenForCoords(-96, -2560))
				);
				aWorld.sign( - 1, mHeight+2, -30, SIDE_Z_POS, 0
				, text(aWorld.getBiomeGenForCoords(-96, -2048))
				, text(aWorld.getBiomeGenForCoords(-96, -1536))
				, text(aWorld.getBiomeGenForCoords(-96, -1024))
				, text(aWorld.getBiomeGenForCoords(-96, - 512))
				);
				} catch(Throwable e) {e.printStackTrace(System.err);}


				aWorld.setBlock(- 1, mHeight+1,  31, BlocksGT.Concrete, DYE_INDEX_Green, 0);
				aWorld.setBlock(- 1, mHeight+1,  30, BlocksGT.Concrete, DYE_INDEX_Green, 0);
				aWorld.setBlock(  0, mHeight+1,  31, BlocksGT.Concrete, DYE_INDEX_Green, 0);
				aWorld.setBlock(  0, mHeight+1,  30, BlocksGT.Concrete, DYE_INDEX_Green, 0);
				aWorld.setBlock(- 1, mHeight+2,  31, BlocksGT.Concrete, DYE_INDEX_Green, 0);
				aWorld.setBlock(- 1, mHeight+2,  30, BlocksGT.Concrete, DYE_INDEX_Green, 0);
				aWorld.setBlock(  0, mHeight+2,  31, BlocksGT.Concrete, DYE_INDEX_Green, 0);
				aWorld.setBlock(  0, mHeight+2,  30, BlocksGT.Concrete, DYE_INDEX_Green, 0);
				aWorld.setBlock(- 1, mHeight+3,  31, BlocksGT.Concrete, DYE_INDEX_Green, 0);
				aWorld.setBlock(- 1, mHeight+3,  30, BlocksGT.Concrete, DYE_INDEX_Green, 0);
				aWorld.setBlock(  0, mHeight+3,  31, BlocksGT.Concrete, DYE_INDEX_Green, 0);
				aWorld.setBlock(  0, mHeight+3,  30, BlocksGT.Concrete, DYE_INDEX_Green, 0);
				aWorld.setBlock(- 1, mHeight+4,  31, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
				aWorld.setBlock(- 1, mHeight+4,  30, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
				aWorld.setBlock(  0, mHeight+4,  31, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
				aWorld.setBlock(  0, mHeight+4,  30, BlocksGT.FOAM_SLABS[SIDE_Y_NEG], DYE_INDEX_LightGray, 0);
				aWorld.setBlock(- 2, mHeight+1,  31, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_Red, 0);
				aWorld.setBlock(- 2, mHeight+1,  30, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_White, 0);
				aWorld.setBlock(- 1, mHeight+1,  29, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_Red, 0);
				aWorld.setBlock(  0, mHeight+1,  29, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_White, 0);
				aWorld.setBlock(  1, mHeight+1,  30, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_Red, 0);
				aWorld.setBlock(  1, mHeight+1,  31, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_White, 0);
				try {
				aWorld.sign(   0, mHeight+3,  29, SIDE_Z_NEG, 0
				, text(aWorld.getBiomeGenForCoords(+95, +4095))
				, text(aWorld.getBiomeGenForCoords(+95, +3583))
				, text(aWorld.getBiomeGenForCoords(+95, +3071))
				, text(aWorld.getBiomeGenForCoords(+95, +2559))
				);
				aWorld.sign(   0, mHeight+2,  29, SIDE_Z_NEG, 0
				, text(aWorld.getBiomeGenForCoords(+95, +2047))
				, text(aWorld.getBiomeGenForCoords(+95, +1535))
				, text(aWorld.getBiomeGenForCoords(+95, +1023))
				, text(aWorld.getBiomeGenForCoords(+95, + 511))
				);

				aWorld.sign( - 1, mHeight+3,  29, SIDE_Z_NEG, 0
				, text(aWorld.getBiomeGenForCoords(-96, +4095))
				, text(aWorld.getBiomeGenForCoords(-96, +3583))
				, text(aWorld.getBiomeGenForCoords(-96, +3071))
				, text(aWorld.getBiomeGenForCoords(-96, +2559))
				);
				aWorld.sign( - 1, mHeight+2,  29, SIDE_Z_NEG, 0
				, text(aWorld.getBiomeGenForCoords(-96, +2047))
				, text(aWorld.getBiomeGenForCoords(-96, +1535))
				, text(aWorld.getBiomeGenForCoords(-96, +1023))
				, text(aWorld.getBiomeGenForCoords(-96, + 511))
				);
				} catch(Throwable e) {e.printStackTrace(System.err);}

				if (GENERATE_BEACON) {
					for (int i = -5; i < 5; i++) for (int j = -5; j < 5; j++) set(aWorld, i, mHeight-3, j, Blocks.iron_block, 0, 0);
					for (int i = -4; i < 4; i++) for (int j = -4; j < 4; j++) set(aWorld, i, mHeight-2, j, Blocks.iron_block, 0, 0);
					for (int i = -3; i < 3; i++) for (int j = -3; j < 3; j++) set(aWorld, i, mHeight-1, j, Blocks.iron_block, 0, 0);
					for (int i = -2; i < 2; i++) for (int j = -2; j < 2; j++) set(aWorld, i, mHeight  , j, Blocks.iron_block, 0, 0);


					aWorld.beacon(-1, mHeight+1, -1, "speed", "speed");

					aWorld.beacon(-1, mHeight+1, 0, "haste", "haste");

					aWorld.beacon(0, mHeight+1, -1, "strength", "strength");

					aWorld.beacon(0, mHeight+1, 0, "resistance", "regeneration");
				}

				aWorld.setSpawnLocation(0, mHeight+5, 0);
				return T;
			}
			if (aMinZ < -96 || aMinZ > 80) {





				for (int tOpaqueCount = 0, i = -16; i < 16; i++) for (int j = 0; j < 16; j++) {
					OriginWorld.Block tBlock = aWorld.getBlock(i, mHeight+9, aMinZ+j);
					if (tBlock.liquid() || anywater(tBlock) || (opq(tBlock) && !tBlock.wood() && !tBlock.wood() && !tBlock.leaves())) {
						if (tOpaqueCount++ > 128) {
							return generateRoadX(aWorld, aMinZ, F, F, T, F, F);
						}
					}
				}
				aBiomeNames = new java.util.HashSet<>(aBiomeNames);
				for (int i = aMinZ; i <= aMaxZ; i++) for (int j = (aMinZ < 0 ? 0 : -16), k = (aMinZ < 0 ? 16 : 0); j < k; j++) {
					OriginWorld.Biome tBiome = aWorld.getBiomeGenForCoords(j, i);
					if (tBiome != null) aBiomeNames.add(tBiome.biomeName());
				}
				for (String tName : aBiomeNames) if (aWorld.isInfiniteWaterBiome(tName)) {
					return generateRoadX(aWorld, aMinZ, F, T, F, T, T);
				}
				return generateRoadX(aWorld, aMinZ, T, T, F, F, T);
			}
			return aMinZ != -32 && aMinZ != 16 && generateRoadX(aWorld, aMinZ, F, F, F, T, !GENERATE_BIOMES);
		}
		if (aMinZ == -16 || aMinZ == 0) {
			if (aMinX < -96 || aMinX > 80) {





				for (int tOpaqueCount = 0, i = -16; i < 16; i++) for (int j = 0; j < 16; j++) {
					OriginWorld.Block tBlock = aWorld.getBlock(aMinX+j, mHeight+9, i);
					if (tBlock.liquid() || anywater(tBlock) || (opq(tBlock) && !tBlock.wood() && !tBlock.wood() && !tBlock.leaves())) {
						if (tOpaqueCount++ > 128) {
							return generateRoadZ(aWorld, aMinX, F, F, T, F, F);
						}
					}
				}
				aBiomeNames = new java.util.HashSet<>(aBiomeNames);
				for (int i = aMinX; i <= aMaxX; i++) for (int j = (aMinZ < 0 ? 0 : -16), k = (aMinZ < 0 ? 16 : 0); j < k; j++) {
					OriginWorld.Biome tBiome = aWorld.getBiomeGenForCoords(i, j);
					if (tBiome != null) aBiomeNames.add(tBiome.biomeName());
				}
				for (String tName : aBiomeNames) if (aWorld.isInfiniteWaterBiome(tName)) {
					return generateRoadZ(aWorld, aMinX, F, T, F, T, T);
				}
				return generateRoadZ(aWorld, aMinX, T, T, F, F, T);
			}
			return aMinX != -32 && aMinX != 16 && generateRoadZ(aWorld, aMinX, F, F, F, T, !GENERATE_BIOMES);
		}
		return F;
	}

	@SuppressWarnings("unchecked")
	public final boolean generateRoadX(OriginWorld aWorld, int aMinZ, boolean aLand, boolean aKillSky, boolean aTunnel, boolean aBridge, boolean aSideWalls) {
		for (int i = 0; i < 16; i++) {
			if (aLand) {
				for (int j = mHeight+1; j > aWorld.minY(); j--) if (!opq(aWorld, -13, j, aMinZ+i, T, T)) set(aWorld, -13, j, aMinZ+i, Blocks.gravel, 1, 0, T); else break;
				for (int j = mHeight+1; j > aWorld.minY(); j--) if (!opq(aWorld,  12, j, aMinZ+i, T, T)) set(aWorld,  12, j, aMinZ+i, Blocks.gravel, 1, 0, T); else break;
				for (int j = mHeight  ; j > aWorld.minY(); j--) if (!opq(aWorld, -14, j, aMinZ+i, T, T)) set(aWorld, -14, j, aMinZ+i, Blocks.gravel, 1, 0, T); else break;
				for (int j = mHeight  ; j > aWorld.minY(); j--) if (!opq(aWorld,  13, j, aMinZ+i, T, T)) set(aWorld,  13, j, aMinZ+i, Blocks.gravel, 1, 0, T); else break;
				for (int j = mHeight-1; j > aWorld.minY(); j--) if (!opq(aWorld, -15, j, aMinZ+i, T, T)) set(aWorld, -15, j, aMinZ+i, Blocks.gravel, 1, 0, T); else break;
				for (int j = mHeight-1; j > aWorld.minY(); j--) if (!opq(aWorld,  14, j, aMinZ+i, T, T)) set(aWorld,  14, j, aMinZ+i, Blocks.gravel, 1, 0, T); else break;
				for (int j = mHeight-2; j > aWorld.minY(); j--) if (!opq(aWorld, -16, j, aMinZ+i, T, T)) set(aWorld, -16, j, aMinZ+i, Blocks.gravel, 1, 0, T); else break;
				for (int j = mHeight-2; j > aWorld.minY(); j--) if (!opq(aWorld,  15, j, aMinZ+i, T, T)) set(aWorld,  15, j, aMinZ+i, Blocks.gravel, 1, 0, T); else break;
			}
			if (aTunnel) {
				for (int j = -12; j < 12; j++)
				aWorld.setBlock(  j, mHeight+7, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_White, 0);
				for (int j = 0; j < 7; j++) {
				aWorld.setBlock(-13, mHeight+j, aMinZ+i, BlocksGT.Concrete, j == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
				aWorld.setBlock( 12, mHeight+j, aMinZ+i, BlocksGT.Concrete, j == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
				}
			}
			if (aBridge) {
				set(aWorld, -13, mHeight  , aMinZ+i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
				set(aWorld,  12, mHeight  , aMinZ+i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
				aWorld.setBlock(-13, mHeight+1, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
				aWorld.setBlock( 12, mHeight+1, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
			}
			if (aKillSky) {
				for (int j = -13; j < 13; j++) for (int k = 2; k < 64; k++) aWorld.setBlock(j, mHeight+k, aMinZ+i, NB, 0, 0);
			} else {
				for (int j = -12; j < 12; j++) for (int k = 2; k <  7; k++) aWorld.setBlock(j, mHeight+k, aMinZ+i, NB, 0, 0);
			}
			for (int j = -12; j < 2; j++) {
				aWorld.setBlock(j, mHeight+1, aMinZ+i, NB, 0, 0);
				if (aLand) {
				set(aWorld, j, mHeight-2, aMinZ+i, Blocks.cobblestone, 0, 0, T);
				aWorld.setBlock(j, mHeight-1, aMinZ+i, Blocks.gravel, 1, 0);
				for (int k = mHeight-3; k > aWorld.minY(); k--) if (!opq(aWorld, j, k, aMinZ+i, T, T)) set(aWorld, j, k, aMinZ+i, Blocks.cobblestone, 0, 0, T); else break;
				} else {
				set(aWorld, j, mHeight-1, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
				}
			}
			for (int j = 1; j < 12; j++) {
				aWorld.setBlock(j, mHeight+1, aMinZ+i, NB, 0, 0);
				if (aLand) {
				set(aWorld, j, mHeight-2, aMinZ+i, Blocks.cobblestone, 0, 0, T);
				aWorld.setBlock(j, mHeight-1, aMinZ+i, Blocks.gravel, 1, 0);
				for (int k = mHeight-3; k > aWorld.minY(); k--) if (!opq(aWorld, j, k, aMinZ+i, T, T)) set(aWorld, j, k, aMinZ+i, Blocks.cobblestone, 0, 0, T); else break;
				} else {
				set(aWorld, j, mHeight-1, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
				}
			}

			aWorld.setBlock(-12, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock(-12, mHeight+1, aMinZ+i, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_LightGray, 0);
			aWorld.setBlock(-11, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock(-11, mHeight+1, aMinZ+i, BlocksGT.RailRoad, 0, 0);
			aWorld.setBlock(-10, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(- 9, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(- 8, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(- 7, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock(- 7, mHeight+1, aMinZ+i, BlocksGT.RailRoad, ((i+2) / 4) % 2 == 0 ? 8 : 0, 0);
			aWorld.setBlock(- 6, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(- 5, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(- 4, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(- 3, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock(- 3, mHeight+1, aMinZ+i, BlocksGT.RailRoad, 0, 0);
			aWorld.setBlock(- 2, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock(- 2, mHeight+1, aMinZ+i, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_LightGray, 0);

			if (aLand) {
			set(aWorld, - 1, mHeight-1, aMinZ+i, Blocks.cobblestone, 0, 0, T);
			set(aWorld, - 1, mHeight  , aMinZ+i, Blocks.gravel, 1, 0, F);
			set(aWorld, - 1, mHeight+1, aMinZ+i, Blocks.gravel, 1, 0, F);
			set(aWorld,   0, mHeight-1, aMinZ+i, Blocks.cobblestone, 0, 0, T);
			set(aWorld,   0, mHeight  , aMinZ+i, Blocks.gravel, 1, 0, F);
			set(aWorld,   0, mHeight+1, aMinZ+i, Blocks.gravel, 1, 0, F);
			for (int j = mHeight-2; j > aWorld.minY(); j--) if (!opq(aWorld, - 1, j, aMinZ+i, T, T)) set(aWorld, - 1, j, aMinZ+i, Blocks.cobblestone, 0, 0, T); else break;
			for (int j = mHeight-2; j > aWorld.minY(); j--) if (!opq(aWorld,   0, j, aMinZ+i, T, T)) set(aWorld,   0, j, aMinZ+i, Blocks.cobblestone, 0, 0, T); else break;
			} else {
			set(aWorld, - 1, mHeight-1, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
			set(aWorld, - 1, mHeight  , aMinZ+i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, F);
			set(aWorld, - 1, mHeight+1, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, F);
			set(aWorld,   0, mHeight-1, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
			set(aWorld,   0, mHeight  , aMinZ+i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, F);
			set(aWorld,   0, mHeight+1, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_Gray, 0, F);
			}

			aWorld.setBlock(  1, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock(  1, mHeight+1, aMinZ+i, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_LightGray, 0);
			aWorld.setBlock(  2, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock(  2, mHeight+1, aMinZ+i, BlocksGT.RailRoad, 0, 0);
			aWorld.setBlock(  3, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(  4, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(  5, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(  6, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock(  6, mHeight+1, aMinZ+i, BlocksGT.RailRoad, ((i+2) / 4) % 2 == 0 ? 8 : 0, 0);
			aWorld.setBlock(  7, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(  8, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(  9, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock( 10, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock( 10, mHeight+1, aMinZ+i, BlocksGT.RailRoad, 0, 0);
			aWorld.setBlock( 11, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock( 11, mHeight+1, aMinZ+i, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_LightGray, 0);
		}

		if (aTunnel) {
			aWorld.setBlock(-13, mHeight+3, aMinZ+ 1, Blocks.glowstone, 0, 0);
			aWorld.setBlock( 12, mHeight+3, aMinZ+ 1, Blocks.glowstone, 0, 0);
			aWorld.setBlock(-13, mHeight+3, aMinZ+ 6, Blocks.glowstone, 0, 0);
			aWorld.setBlock( 12, mHeight+3, aMinZ+ 6, Blocks.glowstone, 0, 0);
			aWorld.setBlock(-13, mHeight+3, aMinZ+ 9, Blocks.glowstone, 0, 0);
			aWorld.setBlock( 12, mHeight+3, aMinZ+ 9, Blocks.glowstone, 0, 0);
			aWorld.setBlock(-13, mHeight+3, aMinZ+14, Blocks.glowstone, 0, 0);
			aWorld.setBlock( 12, mHeight+3, aMinZ+14, Blocks.glowstone, 0, 0);
		}
		if (aSideWalls) {
			for (int i =  0; i <  8; i++) {OriginWorld.Block tBlock = block(aWorld,  13, mHeight+4, aMinZ+i, T); if (tBlock.liquid() || opq(tBlock)) {for (int j = 0; j <  8; j++) for (int k = 2; k < 6; k++) set(aWorld,  12, mHeight+k, aMinZ+j, even(0, k, j)?BlocksGT.CFoam:BlocksGT.Concrete, DYE_INDEX_LightGray, 0, T); break;}}
			for (int i =  0; i <  8; i++) {OriginWorld.Block tBlock = block(aWorld, -14, mHeight+4, aMinZ+i, T); if (tBlock.liquid() || opq(tBlock)) {for (int j = 0; j <  8; j++) for (int k = 2; k < 6; k++) set(aWorld, -13, mHeight+k, aMinZ+j, even(1, k, j)?BlocksGT.CFoam:BlocksGT.Concrete, DYE_INDEX_LightGray, 0, T); break;}}
			for (int i =  8; i < 16; i++) {OriginWorld.Block tBlock = block(aWorld,  13, mHeight+4, aMinZ+i, T); if (tBlock.liquid() || opq(tBlock)) {for (int j = 8; j < 16; j++) for (int k = 2; k < 6; k++) set(aWorld,  12, mHeight+k, aMinZ+j, even(0, k, j)?BlocksGT.CFoam:BlocksGT.Concrete, DYE_INDEX_LightGray, 0, T); break;}}
			for (int i =  8; i < 16; i++) {OriginWorld.Block tBlock = block(aWorld, -14, mHeight+4, aMinZ+i, T); if (tBlock.liquid() || opq(tBlock)) {for (int j = 8; j < 16; j++) for (int k = 2; k < 6; k++) set(aWorld, -13, mHeight+k, aMinZ+j, even(1, k, j)?BlocksGT.CFoam:BlocksGT.Concrete, DYE_INDEX_LightGray, 0, T); break;}}
		}

		if (aMinZ >> 9 != (aMinZ-16) >> 9) {
			for (int i = 5; i < 11; i++) for (int j = 1; j < 6; j++) {
			set(aWorld, -13, mHeight+j, aMinZ+i, BlocksGT.Concrete, i == 5 || i == 10 || j == 1 || j == 5 ? aTunnel ? DYE_INDEX_Black : DYE_INDEX_White : aMinZ < 0 ? DYE_INDEX_Yellow : DYE_INDEX_Green, 0, j == 1);
			set(aWorld,  12, mHeight+j, aMinZ+i, BlocksGT.Concrete, i == 5 || i == 10 || j == 1 || j == 5 ? aTunnel ? DYE_INDEX_Black : DYE_INDEX_White : aMinZ < 0 ? DYE_INDEX_Yellow : DYE_INDEX_Green, 0, j == 1);
			}
			aWorld.sign( -12, mHeight+3, aMinZ+7, SIDE_X_POS, 0, "", "X: -1", "Z: " + ((aMinZ-16) >> 9), "");
			aWorld.sign( -12, mHeight+3, aMinZ+8, SIDE_X_POS, 0, "", "X: -1", "Z: " + ( aMinZ     >> 9), "");
			aWorld.sign(  11, mHeight+3, aMinZ+7, SIDE_X_NEG, 0, "", "X: 0" , "Z: " + ((aMinZ-16) >> 9), "");
			aWorld.sign(  11, mHeight+3, aMinZ+8, SIDE_X_NEG, 0, "", "X: 0" , "Z: " + ( aMinZ     >> 9), "");

			aWorld.setBlock(-13, mHeight+1, aMinZ+ 5, Blocks.glowstone, 0, 0);
			aWorld.setBlock(-13, mHeight+1, aMinZ+10, Blocks.glowstone, 0, 0);
			aWorld.setBlock(-13, mHeight+5, aMinZ+ 5, Blocks.glowstone, 0, 0);
			aWorld.setBlock(-13, mHeight+5, aMinZ+10, Blocks.glowstone, 0, 0);
			aWorld.setBlock( 12, mHeight+1, aMinZ+ 5, Blocks.glowstone, 0, 0);
			aWorld.setBlock( 12, mHeight+1, aMinZ+10, Blocks.glowstone, 0, 0);
			aWorld.setBlock( 12, mHeight+5, aMinZ+ 5, Blocks.glowstone, 0, 0);
			aWorld.setBlock( 12, mHeight+5, aMinZ+10, Blocks.glowstone, 0, 0);
		}
		if (aMinZ >> 9 != (aMinZ+16) >> 9) {
			aWorld.setBlock(- 2, mHeight+1, aMinZ+0, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_White, 0);
			aWorld.setBlock(- 2, mHeight+1, aMinZ+1, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_Red, 0);
			aWorld.setBlock(- 2, mHeight+1, aMinZ+2, NB, 0, 0);
			aWorld.setBlock(- 1, mHeight+1, aMinZ+2, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_White, 0);
			aWorld.setBlock(  0, mHeight+1, aMinZ+2, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_Red, 0);
			aWorld.setBlock(  1, mHeight+1, aMinZ+2, NB, 0, 0);
			aWorld.setBlock(  1, mHeight+1, aMinZ+1, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_White, 0);
			aWorld.setBlock(  1, mHeight+1, aMinZ+0, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_Red, 0);

			for (int i = 2; i < 14; i++) {
			aWorld.setBlock(- 1, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(  0, mHeight  , aMinZ+i, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			}
			for (int i = 3; i < 13; i++) {
			aWorld.setBlock(- 2, mHeight+1, aMinZ+i, NB, 0, 0);
			aWorld.setBlock(- 1, mHeight+1, aMinZ+i, NB, 0, 0);
			aWorld.setBlock(  0, mHeight+1, aMinZ+i, NB, 0, 0);
			aWorld.setBlock(  1, mHeight+1, aMinZ+i, NB, 0, 0);
			}

			aWorld.setBlock(- 2, mHeight+1, aMinZ+15, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_Red, 0);
			aWorld.setBlock(- 2, mHeight+1, aMinZ+14, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_White, 0);
			aWorld.setBlock(- 2, mHeight+1, aMinZ+13, NB, 0, 0);
			aWorld.setBlock(- 1, mHeight+1, aMinZ+13, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_Red, 0);
			aWorld.setBlock(  0, mHeight+1, aMinZ+13, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_White, 0);
			aWorld.setBlock(  1, mHeight+1, aMinZ+13, NB, 0, 0);
			aWorld.setBlock(  1, mHeight+1, aMinZ+14, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_Red, 0);
			aWorld.setBlock(  1, mHeight+1, aMinZ+15, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_White, 0);

			if (aTunnel) {
				for (int i = 0; i < 7; i++) {
					aWorld.setBlock(-1, mHeight+i, aMinZ+ 0, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock(-1, mHeight+i, aMinZ+ 1, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock( 0, mHeight+i, aMinZ+ 0, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock( 0, mHeight+i, aMinZ+ 1, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock(-1, mHeight+i, aMinZ+14, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock(-1, mHeight+i, aMinZ+15, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock( 0, mHeight+i, aMinZ+14, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock( 0, mHeight+i, aMinZ+15, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
				}
			}
		} else {
			if (aTunnel) {
				for (int i = 0; i < 7; i++) {
					aWorld.setBlock(-1, mHeight+i, aMinZ+ 7, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock(-1, mHeight+i, aMinZ+ 8, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock( 0, mHeight+i, aMinZ+ 7, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock( 0, mHeight+i, aMinZ+ 8, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
				}
			}
		}

		if (aBridge) {
			for (int i = 6; i <= 9; i++) {
				for (int j = -9; j <= -6; j++) {
					aWorld.setBlock(j, mHeight-2, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					aWorld.setBlock(j, mHeight-3, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
				}
				for (int j = 5; j <= 8; j++) {
					aWorld.setBlock(j, mHeight-2, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					aWorld.setBlock(j, mHeight-3, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
				}
			}
			for (int k = mHeight-4; k > aWorld.minY(); k--) if (!(opq(aWorld, -10, k, aMinZ+10, T, T) && opq(aWorld, -5, k, aMinZ+10, T, T) && opq(aWorld, -10, k, aMinZ+5, T, T) && opq(aWorld, -5, k, aMinZ+5, T, T))) {
				aWorld.setBlock( -7, k, aMinZ+7, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
				aWorld.setBlock( -8, k, aMinZ+8, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
				aWorld.setBlock( -7, k, aMinZ+8, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
				aWorld.setBlock( -8, k, aMinZ+7, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
			} else {
				for (int i = 6; i <= 9; i++) for (int j = -9; j <= -6; j++) {
					if (k>aWorld.minY()+(-3)) aWorld.setBlock(j, k+3, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(-2)) aWorld.setBlock(j, k+2, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(-1)) aWorld.setBlock(j, k+1, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(0)) aWorld.setBlock(j, k  , aMinZ+i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(+1)) aWorld.setBlock(j, k-1, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(+2)) aWorld.setBlock(j, k-2, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(+3)) aWorld.setBlock(j, k-3, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
				}
				break;
			}
			for (int k = mHeight-4; k > aWorld.minY(); k--) if (!(opq(aWorld, 9, k, aMinZ+10, T, T) && opq(aWorld, 4, k, aMinZ+10, T, T) && opq(aWorld, 9, k, aMinZ+5, T, T) && opq(aWorld, 4, k, aMinZ+5, T, T))) {
				aWorld.setBlock(  6, k, aMinZ+7, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
				aWorld.setBlock(  7, k, aMinZ+8, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
				aWorld.setBlock(  6, k, aMinZ+8, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
				aWorld.setBlock(  7, k, aMinZ+7, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
			} else {
				for (int i = 6; i <= 9; i++) for (int j = 5; j <= 8; j++) {
					if (k>aWorld.minY()+(-3)) aWorld.setBlock(j, k+3, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(-2)) aWorld.setBlock(j, k+2, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(-1)) aWorld.setBlock(j, k+1, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(0)) aWorld.setBlock(j, k  , aMinZ+i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(+1)) aWorld.setBlock(j, k-1, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(+2)) aWorld.setBlock(j, k-2, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(+3)) aWorld.setBlock(j, k-3, aMinZ+i, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
				}
				break;
			}
		}

		// Kill every living thing close by except Players.
		aWorld.clearNonPlayerEntities(-16, mHeight, aMinZ, +16, mHeight+8, aMinZ+16);
		return T;
	}

	@SuppressWarnings("unchecked")
	public final boolean generateRoadZ(OriginWorld aWorld, int aMinX, boolean aLand, boolean aKillSky, boolean aTunnel, boolean aBridge, boolean aSideWalls) {
		for (int i = 0; i < 16; i++) {
			if (aLand) {
				for (int j = mHeight+1; j > aWorld.minY(); j--) if (!opq(aWorld, aMinX+i, j, -13, T, T)) set(aWorld, aMinX+i, j, -13, Blocks.gravel, 1, 0, T); else break;
				for (int j = mHeight+1; j > aWorld.minY(); j--) if (!opq(aWorld, aMinX+i, j,  12, T, T)) set(aWorld, aMinX+i, j,  12, Blocks.gravel, 1, 0, T); else break;
				for (int j = mHeight  ; j > aWorld.minY(); j--) if (!opq(aWorld, aMinX+i, j, -14, T, T)) set(aWorld, aMinX+i, j, -14, Blocks.gravel, 1, 0, T); else break;
				for (int j = mHeight  ; j > aWorld.minY(); j--) if (!opq(aWorld, aMinX+i, j,  13, T, T)) set(aWorld, aMinX+i, j,  13, Blocks.gravel, 1, 0, T); else break;
				for (int j = mHeight-1; j > aWorld.minY(); j--) if (!opq(aWorld, aMinX+i, j, -15, T, T)) set(aWorld, aMinX+i, j, -15, Blocks.gravel, 1, 0, T); else break;
				for (int j = mHeight-1; j > aWorld.minY(); j--) if (!opq(aWorld, aMinX+i, j,  14, T, T)) set(aWorld, aMinX+i, j,  14, Blocks.gravel, 1, 0, T); else break;
				for (int j = mHeight-2; j > aWorld.minY(); j--) if (!opq(aWorld, aMinX+i, j, -16, T, T)) set(aWorld, aMinX+i, j, -16, Blocks.gravel, 1, 0, T); else break;
				for (int j = mHeight-2; j > aWorld.minY(); j--) if (!opq(aWorld, aMinX+i, j,  15, T, T)) set(aWorld, aMinX+i, j,  15, Blocks.gravel, 1, 0, T); else break;
			}
			if (aTunnel) {
				for (int j = -12; j < 12; j++)
				aWorld.setBlock(aMinX+i, mHeight+7,   j, BlocksGT.Concrete, DYE_INDEX_White, 0);
				for (int j = 0; j < 7; j++) {
				aWorld.setBlock(aMinX+i, mHeight+j, -13, BlocksGT.Concrete, j == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
				aWorld.setBlock(aMinX+i, mHeight+j,  12, BlocksGT.Concrete, j == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
				}
			}
			if (aBridge) {
				set(aWorld, aMinX+i, mHeight  , -13, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
				set(aWorld, aMinX+i, mHeight  ,  12, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
				aWorld.setBlock(aMinX+i, mHeight+1, -13, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
				aWorld.setBlock(aMinX+i, mHeight+1,  12, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
			}
			if (aKillSky) {
				for (int j = -13; j < 13; j++) for (int k = 2; k < 32; k++) aWorld.setBlock(aMinX+i, mHeight+k, j, NB, 0, 0);
			} else {
				for (int j = -12; j < 12; j++) for (int k = 2; k <  7; k++) aWorld.setBlock(aMinX+i, mHeight+k, j, NB, 0, 0);
			}
			for (int j = -12; j < 2; j++) {
				aWorld.setBlock(aMinX+i, mHeight+1, j, NB, 0, 0);
				if (aLand) {
				set(aWorld, aMinX+i, mHeight-2, j, Blocks.cobblestone, 0, 0, T);
				aWorld.setBlock(aMinX+i, mHeight-1, j, Blocks.gravel, 1, 0);
				for (int k = mHeight-3; k > aWorld.minY(); k--) if (!opq(aWorld, aMinX+i, k, j, T, T)) set(aWorld, aMinX+i, k, j, Blocks.cobblestone, 0, 0, T); else break;
				} else {
				set(aWorld, aMinX+i, mHeight-1, j, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
				}
			}
			for (int j = 1; j < 12; j++) {
				aWorld.setBlock(aMinX+i, mHeight+1, j, NB, 0, 0);
				if (aLand) {
				set(aWorld, aMinX+i, mHeight-2, j, Blocks.cobblestone, 0, 0, T);
				aWorld.setBlock(aMinX+i, mHeight-1, j, Blocks.gravel, 1, 0);
				for (int k = mHeight-3; k > aWorld.minY(); k--) if (!opq(aWorld, aMinX+i, k, j, T, T)) set(aWorld, aMinX+i, k, j, Blocks.cobblestone, 0, 0, T); else break;
				} else {
				set(aWorld, aMinX+i, mHeight-1, j, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
				}
			}

			aWorld.setBlock(aMinX+i, mHeight  , -12, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock(aMinX+i, mHeight+1, -12, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_LightGray, 0);
			aWorld.setBlock(aMinX+i, mHeight  , -11, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock(aMinX+i, mHeight+1, -11, BlocksGT.RailRoad, 1, 0);
			aWorld.setBlock(aMinX+i, mHeight  , -10, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(aMinX+i, mHeight  , - 9, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(aMinX+i, mHeight  , - 8, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(aMinX+i, mHeight  , - 7, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock(aMinX+i, mHeight+1, - 7, BlocksGT.RailRoad, ((i+2) / 4) % 2 == 0 ? 9 : 1, 0);
			aWorld.setBlock(aMinX+i, mHeight  , - 6, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(aMinX+i, mHeight  , - 5, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(aMinX+i, mHeight  , - 4, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(aMinX+i, mHeight  , - 3, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock(aMinX+i, mHeight+1, - 3, BlocksGT.RailRoad, 1, 0);
			aWorld.setBlock(aMinX+i, mHeight  , - 2, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock(aMinX+i, mHeight+1, - 2, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_LightGray, 0);

			if (aLand) {
			set(aWorld, aMinX+i, mHeight-1, - 1, Blocks.cobblestone, 0, 0, T);
			set(aWorld, aMinX+i, mHeight  , - 1, Blocks.gravel, 1, 0, F);
			set(aWorld, aMinX+i, mHeight+1, - 1, Blocks.gravel, 1, 0, F);
			set(aWorld, aMinX+i, mHeight-1,   0, Blocks.cobblestone, 0, 0, T);
			set(aWorld, aMinX+i, mHeight  ,   0, Blocks.gravel, 1, 0, F);
			set(aWorld, aMinX+i, mHeight+1,   0, Blocks.gravel, 1, 0, F);
			for (int j = mHeight-2; j > aWorld.minY(); j--) if (!opq(aWorld, aMinX+i, j, - 1, T, T)) set(aWorld, aMinX+i, j, - 1, Blocks.cobblestone, 0, 0, T); else break;
			for (int j = mHeight-2; j > aWorld.minY(); j--) if (!opq(aWorld, aMinX+i, j,   0, T, T)) set(aWorld, aMinX+i, j,   0, Blocks.cobblestone, 0, 0, T); else break;
			} else {
			set(aWorld, aMinX+i, mHeight-1, - 1, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
			set(aWorld, aMinX+i, mHeight  , - 1, BlocksGT.Concrete, DYE_INDEX_Gray, 0, F);
			set(aWorld, aMinX+i, mHeight+1, - 1, BlocksGT.Concrete, DYE_INDEX_Gray, 0, F);
			set(aWorld, aMinX+i, mHeight-1,   0, BlocksGT.Concrete, DYE_INDEX_Gray, 0, T);
			set(aWorld, aMinX+i, mHeight  ,   0, BlocksGT.Concrete, DYE_INDEX_Gray, 0, F);
			set(aWorld, aMinX+i, mHeight+1,   0, BlocksGT.Concrete, DYE_INDEX_Gray, 0, F);
			}

			aWorld.setBlock(aMinX+i, mHeight  ,   1, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock(aMinX+i, mHeight+1,   1, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_LightGray, 0);
			aWorld.setBlock(aMinX+i, mHeight  ,   2, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock(aMinX+i, mHeight+1,   2, BlocksGT.RailRoad, 1, 0);
			aWorld.setBlock(aMinX+i, mHeight  ,   3, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(aMinX+i, mHeight  ,   4, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(aMinX+i, mHeight  ,   5, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(aMinX+i, mHeight  ,   6, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock(aMinX+i, mHeight+1,   6, BlocksGT.RailRoad, ((i+2) / 4) % 2 == 0 ? 9 : 1, 0);
			aWorld.setBlock(aMinX+i, mHeight  ,   7, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(aMinX+i, mHeight  ,   8, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(aMinX+i, mHeight  ,   9, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(aMinX+i, mHeight  ,  10, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock(aMinX+i, mHeight+1,  10, BlocksGT.RailRoad, 1, 0);
			aWorld.setBlock(aMinX+i, mHeight  ,  11, BlocksGT.Asphalt, DYE_INDEX_Gray, 0); aWorld.setBlock(aMinX+i, mHeight+1,  11, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_LightGray, 0);
		}

		if (aTunnel) {
			aWorld.setBlock(aMinX+ 1, mHeight+3, -13, Blocks.glowstone, 0, 0);
			aWorld.setBlock(aMinX+ 1, mHeight+3,  12, Blocks.glowstone, 0, 0);
			aWorld.setBlock(aMinX+ 6, mHeight+3, -13, Blocks.glowstone, 0, 0);
			aWorld.setBlock(aMinX+ 6, mHeight+3,  12, Blocks.glowstone, 0, 0);
			aWorld.setBlock(aMinX+ 9, mHeight+3, -13, Blocks.glowstone, 0, 0);
			aWorld.setBlock(aMinX+ 9, mHeight+3,  12, Blocks.glowstone, 0, 0);
			aWorld.setBlock(aMinX+14, mHeight+3, -13, Blocks.glowstone, 0, 0);
			aWorld.setBlock(aMinX+14, mHeight+3,  12, Blocks.glowstone, 0, 0);
		}
		if (aSideWalls) {
			for (int i =  0; i <  8; i++) {OriginWorld.Block tBlock = block(aWorld, aMinX+i, mHeight+4,  13, T); if (tBlock.liquid() || opq(tBlock)) {for (int j = 0; j <  8; j++) for (int k = 2; k < 6; k++) set(aWorld, aMinX+j, mHeight+k,  12, even(0, k, j)?BlocksGT.CFoam:BlocksGT.Concrete, DYE_INDEX_LightGray, 0, T); break;}}
			for (int i =  0; i <  8; i++) {OriginWorld.Block tBlock = block(aWorld, aMinX+i, mHeight+4, -14, T); if (tBlock.liquid() || opq(tBlock)) {for (int j = 0; j <  8; j++) for (int k = 2; k < 6; k++) set(aWorld, aMinX+j, mHeight+k, -13, even(1, k, j)?BlocksGT.CFoam:BlocksGT.Concrete, DYE_INDEX_LightGray, 0, T); break;}}
			for (int i =  8; i < 16; i++) {OriginWorld.Block tBlock = block(aWorld, aMinX+i, mHeight+4,  13, T); if (tBlock.liquid() || opq(tBlock)) {for (int j = 8; j < 16; j++) for (int k = 2; k < 6; k++) set(aWorld, aMinX+j, mHeight+k,  12, even(0, k, j)?BlocksGT.CFoam:BlocksGT.Concrete, DYE_INDEX_LightGray, 0, T); break;}}
			for (int i =  8; i < 16; i++) {OriginWorld.Block tBlock = block(aWorld, aMinX+i, mHeight+4, -14, T); if (tBlock.liquid() || opq(tBlock)) {for (int j = 8; j < 16; j++) for (int k = 2; k < 6; k++) set(aWorld, aMinX+j, mHeight+k, -13, even(1, k, j)?BlocksGT.CFoam:BlocksGT.Concrete, DYE_INDEX_LightGray, 0, T); break;}}
		}

		if (aMinX >> 9 != (aMinX-16) >> 9) {
			for (int i = 5; i < 11; i++) for (int j = 1; j < 6; j++) {
			set(aWorld, aMinX+i, mHeight+j, -13, BlocksGT.Concrete, i == 5 || i == 10 || j == 1 || j == 5 ? aTunnel ? DYE_INDEX_Black : DYE_INDEX_White : aMinX < 0 ? DYE_INDEX_Blue : DYE_INDEX_Red, 0, j == 1);
			set(aWorld, aMinX+i, mHeight+j,  12, BlocksGT.Concrete, i == 5 || i == 10 || j == 1 || j == 5 ? aTunnel ? DYE_INDEX_Black : DYE_INDEX_White : aMinX < 0 ? DYE_INDEX_Blue : DYE_INDEX_Red, 0, j == 1);
			}
			aWorld.sign( aMinX+7, mHeight+3, -12, SIDE_Z_POS, 0, "", "X: " + ((aMinX-16) >> 9), "Z: -1", "");
			aWorld.sign( aMinX+8, mHeight+3, -12, SIDE_Z_POS, 0, "", "X: " + ( aMinX     >> 9), "Z: -1", "");
			aWorld.sign( aMinX+7, mHeight+3,  11, SIDE_Z_NEG, 0, "", "X: " + ((aMinX-16) >> 9), "Z: 0" , "");
			aWorld.sign( aMinX+8, mHeight+3,  11, SIDE_Z_NEG, 0, "", "X: " + ( aMinX     >> 9), "Z: 0" , "");

			aWorld.setBlock(aMinX+ 5, mHeight+1, -13, Blocks.glowstone, 0, 0);
			aWorld.setBlock(aMinX+10, mHeight+1, -13, Blocks.glowstone, 0, 0);
			aWorld.setBlock(aMinX+ 5, mHeight+5, -13, Blocks.glowstone, 0, 0);
			aWorld.setBlock(aMinX+10, mHeight+5, -13, Blocks.glowstone, 0, 0);
			aWorld.setBlock(aMinX+ 5, mHeight+1,  12, Blocks.glowstone, 0, 0);
			aWorld.setBlock(aMinX+10, mHeight+1,  12, Blocks.glowstone, 0, 0);
			aWorld.setBlock(aMinX+ 5, mHeight+5,  12, Blocks.glowstone, 0, 0);
			aWorld.setBlock(aMinX+10, mHeight+5,  12, Blocks.glowstone, 0, 0);
		}
		if (aMinX >> 9 != (aMinX+16) >> 9) {
			aWorld.setBlock(aMinX+0, mHeight+1, - 2, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_White, 0);
			aWorld.setBlock(aMinX+1, mHeight+1, - 2, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_Red, 0);
			aWorld.setBlock(aMinX+2, mHeight+1, - 2, NB, 0, 0);
			aWorld.setBlock(aMinX+2, mHeight+1, - 1, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_White, 0);
			aWorld.setBlock(aMinX+2, mHeight+1,   0, BlocksGT.FOAM_SLABS[SIDE_X_NEG], DYE_INDEX_Red, 0);
			aWorld.setBlock(aMinX+2, mHeight+1,   1, NB, 0, 0);
			aWorld.setBlock(aMinX+1, mHeight+1,   1, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_White, 0);
			aWorld.setBlock(aMinX+0, mHeight+1,   1, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_Red, 0);

			for (int i = 2; i < 14; i++) {
			aWorld.setBlock(aMinX+i, mHeight  , - 1, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			aWorld.setBlock(aMinX+i, mHeight  ,   0, BlocksGT.Asphalt, DYE_INDEX_Gray, 0);
			}
			for (int i = 3; i < 13; i++) {
			aWorld.setBlock(aMinX+i, mHeight+1, - 2, NB, 0, 0);
			aWorld.setBlock(aMinX+i, mHeight+1, - 1, NB, 0, 0);
			aWorld.setBlock(aMinX+i, mHeight+1,   0, NB, 0, 0);
			aWorld.setBlock(aMinX+i, mHeight+1,   1, NB, 0, 0);
			}

			aWorld.setBlock(aMinX+15, mHeight+1, - 2, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_Red, 0);
			aWorld.setBlock(aMinX+14, mHeight+1, - 2, BlocksGT.FOAM_SLABS[SIDE_Z_POS], DYE_INDEX_White, 0);
			aWorld.setBlock(aMinX+13, mHeight+1, - 2, NB, 0, 0);
			aWorld.setBlock(aMinX+13, mHeight+1, - 1, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_Red, 0);
			aWorld.setBlock(aMinX+13, mHeight+1,   0, BlocksGT.FOAM_SLABS[SIDE_X_POS], DYE_INDEX_White, 0);
			aWorld.setBlock(aMinX+13, mHeight+1,   1, NB, 0, 0);
			aWorld.setBlock(aMinX+14, mHeight+1,   1, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_Red, 0);
			aWorld.setBlock(aMinX+15, mHeight+1,   1, BlocksGT.FOAM_SLABS[SIDE_Z_NEG], DYE_INDEX_White, 0);

			if (aTunnel) {
				for (int i = 0; i < 7; i++) {
					aWorld.setBlock(aMinX+ 0, mHeight+i, -1, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock(aMinX+ 0, mHeight+i,  0, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock(aMinX+ 1, mHeight+i, -1, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock(aMinX+ 1, mHeight+i,  0, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock(aMinX+14, mHeight+i, -1, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock(aMinX+14, mHeight+i,  0, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock(aMinX+15, mHeight+i, -1, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock(aMinX+15, mHeight+i,  0, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
				}
			}
		} else {
			if (aTunnel) {
				for (int i = 0; i < 7; i++) {
					aWorld.setBlock(aMinX+ 7, mHeight+i, -1, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock(aMinX+ 7, mHeight+i,  0, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock(aMinX+ 8, mHeight+i, -1, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
					aWorld.setBlock(aMinX+ 8, mHeight+i,  0, BlocksGT.Concrete, i == 3 ? DYE_INDEX_LightGray : DYE_INDEX_White, 0);
				}
			}
		}

		if (aBridge) {
			for (int i = 6; i <= 9; i++) {
				for (int j = -9; j <= -6; j++) {
					aWorld.setBlock(aMinX+i, mHeight-2, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					aWorld.setBlock(aMinX+i, mHeight-3, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
				}
				for (int j = 5; j <= 8; j++) {
					aWorld.setBlock(aMinX+i, mHeight-2, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					aWorld.setBlock(aMinX+i, mHeight-3, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
				}
			}
			for (int k = mHeight-4; k > aWorld.minY(); k--) if (!(opq(aWorld, aMinX+10, k, -10, T, T) && opq(aWorld, aMinX+10, k, -5, T, T) && opq(aWorld, aMinX+5, k, -10, T, T) && opq(aWorld, aMinX+5, k, -5, T, T))) {
				aWorld.setBlock(aMinX+7, k,  -7, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
				aWorld.setBlock(aMinX+8, k,  -8, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
				aWorld.setBlock(aMinX+7, k,  -8, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
				aWorld.setBlock(aMinX+8, k,  -7, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
			} else {
				for (int i = 6; i <= 9; i++) for (int j = -9; j <= -6; j++) {
					if (k>aWorld.minY()+(-3)) aWorld.setBlock(aMinX+i, k+3, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(-2)) aWorld.setBlock(aMinX+i, k+2, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(-1)) aWorld.setBlock(aMinX+i, k+1, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(0)) aWorld.setBlock(aMinX+i, k  , j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(+1)) aWorld.setBlock(aMinX+i, k-1, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(+2)) aWorld.setBlock(aMinX+i, k-2, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(+3)) aWorld.setBlock(aMinX+i, k-3, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
				}
				break;
			}
			for (int k = mHeight-4; k > aWorld.minY(); k--) if (!(opq(aWorld, aMinX+10, k, 9, T, T) && opq(aWorld, aMinX+10, k, 4, T, T) && opq(aWorld, aMinX+5, k, 9, T, T) && opq(aWorld, aMinX+5, k, 4, T, T))) {
				aWorld.setBlock(aMinX+7, k,   6, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
				aWorld.setBlock(aMinX+8, k,   7, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
				aWorld.setBlock(aMinX+7, k,   7, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
				aWorld.setBlock(aMinX+8, k,   6, BlocksGT.Concrete, DYE_INDEX_Gray, 0);
			} else {
				for (int i = 6; i <= 9; i++) for (int j = 5; j <= 8; j++) {
					if (k>aWorld.minY()+(-3)) aWorld.setBlock(aMinX+i, k+3, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(-2)) aWorld.setBlock(aMinX+i, k+2, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(-1)) aWorld.setBlock(aMinX+i, k+1, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(0)) aWorld.setBlock(aMinX+i, k  , j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(+1)) aWorld.setBlock(aMinX+i, k-1, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(+2)) aWorld.setBlock(aMinX+i, k-2, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
					if (k>aWorld.minY()+(+3)) aWorld.setBlock(aMinX+i, k-3, j, BlocksGT.Concrete, DYE_INDEX_LightGray, 0);
				}
				break;
			}
		}

		// Kill every living thing close by except Players.
		aWorld.clearNonPlayerEntities(aMinX, mHeight, -16, aMinX+16, mHeight+8, +16);
		return T;
	}
}
