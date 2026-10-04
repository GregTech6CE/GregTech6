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

package com.gregtech.gregtech.worldgen.center;
import static com.gregtech.gregtech.worldgen.center.OriginSupport.*;
/** Original Gregorius Techneticies geometry; platform operations use OriginWorld. */
public final class SourceCenterBiomes {
    private final int mHeight;
    private static final boolean GENERATE_STREETS=true, GENERATE_NEXUS=true, GENERATE_TESTING=true;
    public SourceCenterBiomes(int height) { mHeight=height; }
    public boolean generate(OriginWorld aWorld, OriginWorld.Chunk aChunk, int aMinX, int aMinZ, java.util.Random aRandom) {
		if (aMinX >= -96 && aMinX <= 80 && aMinZ >= -96 && aMinZ <= 80) {
			if (GENERATE_STREETS && aMinX >= -32 && aMinX <= 16 && aMinZ >= -32 && aMinZ <= 16) {
				aWorld.biome(aChunk, "river");
				return T;
			}
			if (GENERATE_NEXUS && aMinX == 16 && aMinZ == -48) {
				aWorld.biome(aChunk, "plains");
				return T;
			}
			if (GENERATE_TESTING && (aMinX == 32 || aMinX == 48) && (aMinZ == -32 || aMinZ == -48)) {
				aWorld.biome(aChunk, "plains");
				return T;
			}
			if (aMinX == -16 || aMinX == 0 || aMinZ == -16 || aMinZ == 0) {
				aWorld.biome(aChunk, "river");
				for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
					for (int k = -3; k < aWorld.maxY()-mHeight; k++) set(aChunk, i, mHeight+k, j, NB, 0);
					set(aChunk, i, mHeight-4, j, BlocksGT.River, 0);
					set(aChunk, i, mHeight-5, j, BlocksGT.River, 0);
					set(aChunk, i, mHeight-6, j, BlocksGT.River, 0);
					set(aChunk, i, mHeight-7, j, BlocksGT.Sands, aMinX < 0 ? aMinZ < 0 ? 0 : 1 : aMinZ < 0 ? 2 : 0);
					set(aChunk, i, mHeight-8, j, Blocks.gravel, 1);
					set(aChunk, i, mHeight-9, j, Blocks.clay, 0);
					set(aChunk, i, mHeight-10, j, Blocks.clay, 0);
					for (int k = aWorld.minY()+1; k < mHeight-10; k++) set(aChunk, i, k, j, Blocks.stone, 1);
				}
				aWorld.setSpawnLocation(0, mHeight+5, 0);
				return T;
			}
			if (aMinX < 0) {
				if (aMinZ < 0) {
					if ((aMinX == -80 || aMinX == -64) && (aMinZ == -80 || aMinZ == -64)) {
						aWorld.biome(aChunk, "snowy_plains");
						for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
							for (int k = 1; k < aWorld.maxY()-mHeight; k++) set(aChunk, i, mHeight+k, j, NB, 0);
							set(aChunk, i, mHeight  , j, Blocks.ice, 0);
							set(aChunk, i, mHeight-1, j, Blocks.packed_ice, 0);
							set(aChunk, i, mHeight-2, j, Blocks.packed_ice, 0);
							set(aChunk, i, mHeight-3, j, Blocks.packed_ice, 0);
							set(aChunk, i, mHeight-4, j, Blocks.packed_ice, 0);
							set(aChunk, i, mHeight-5, j, Blocks.packed_ice, 0);
							for (int k = aWorld.minY()+1; k < mHeight-5; k++) set(aChunk, i, k, j, k < 32 ? BlocksGT.SchistGreen : BlocksGT.SchistBlue, aRandom.nextBoolean()?2:0);
						}
					} else {
						aWorld.biome(aChunk, "snowy_taiga");
						for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
							for (int k = 2; k < aWorld.maxY()-mHeight; k++) set(aChunk, i, mHeight+k, j, NB, 0);
							set(aChunk, i, mHeight+1, j, Blocks.snow_layer, aRandom.nextInt(2));
							set(aChunk, i, mHeight  , j, Blocks.dirt, 2);
							set(aChunk, i, mHeight-1, j, Blocks.dirt, 2);
							set(aChunk, i, mHeight-2, j, Blocks.dirt, 2);
							set(aChunk, i, mHeight-3, j, Blocks.dirt, 2);
							set(aChunk, i, mHeight-4, j, Blocks.dirt, 2);
							set(aChunk, i, mHeight-5, j, Blocks.dirt, 2);
							for (int k = aWorld.minY()+1; k < mHeight-5; k++) set(aChunk, i, k, j, Blocks.mossy_cobblestone, 0);
						}

						set(aChunk,  4, mHeight+1,  4, NB, aRandom.nextInt(2));
						set(aChunk, 12, mHeight+1,  4, NB, aRandom.nextInt(2));
						set(aChunk,  4, mHeight+1, 12, NB, aRandom.nextInt(2));
						set(aChunk, 12, mHeight+1, 12, NB, aRandom.nextInt(2));

						aWorld.tree(aMinX+ 4, mHeight+1, aMinZ+ 4, 1, 4+aRandom.nextInt(3), aRandom);
						aWorld.tree(aMinX+12, mHeight+1, aMinZ+ 4, 1, 4+aRandom.nextInt(3), aRandom);
						aWorld.tree(aMinX+ 4, mHeight+1, aMinZ+12, 1, 4+aRandom.nextInt(3), aRandom);
						aWorld.tree(aMinX+12, mHeight+1, aMinZ+12, 1, 4+aRandom.nextInt(3), aRandom);
					}
				} else {
					if ((aMinX == -80 || aMinX == -64) && (aMinZ == 48 || aMinZ == 64)) {
						aWorld.biome(aChunk, "forest");
						for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
							for (int k = 1; k < aWorld.maxY()-mHeight; k++) set(aChunk, i, mHeight+k, j, NB, 0);
							set(aChunk, i, mHeight  , j, Blocks.grass, 0);
							set(aChunk, i, mHeight-1, j, Blocks.dirt, 0);
							set(aChunk, i, mHeight-2, j, Blocks.dirt, 0);
							set(aChunk, i, mHeight-3, j, Blocks.dirt, 0);
							set(aChunk, i, mHeight-4, j, Blocks.dirt, 0);
							set(aChunk, i, mHeight-5, j, Blocks.dirt, 0);
							for (int k = aWorld.minY()+1; k < mHeight-5; k++) set(aChunk, i, k, j, k < 32 ? BlocksGT.Kimberlite : BlocksGT.Quartzite, aRandom.nextBoolean()?2:0);
						}
						set(aChunk,  6, mHeight+1,  6, Blocks.pumpkin, 0);
						set(aChunk, 10, mHeight+1,  6, Blocks.pumpkin, 0);
						set(aChunk,  6, mHeight+1, 10, Blocks.pumpkin, 0);
						set(aChunk, 10, mHeight+1, 10, Blocks.pumpkin, 0);

						aWorld.tree(aMinX+ 4, mHeight+1, aMinZ+ 4, 0, 4+aRandom.nextInt(3), aRandom);
						aWorld.tree(aMinX+12, mHeight+1, aMinZ+ 4, 2, 4+aRandom.nextInt(3), aRandom);
						aWorld.tree(aMinX+ 4, mHeight+1, aMinZ+12, 2, 4+aRandom.nextInt(3), aRandom);
						aWorld.tree(aMinX+12, mHeight+1, aMinZ+12, 0, 4+aRandom.nextInt(3), aRandom);
					} else {

						aWorld.biome(aChunk, "plains");
						for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
							for (int k = 1; k < aWorld.maxY()-mHeight; k++) set(aChunk, i, mHeight+k, j, NB, 0);
							set(aChunk, i, mHeight  , j, Blocks.grass, 0);
							set(aChunk, i, mHeight-1, j, Blocks.dirt, 0);
							set(aChunk, i, mHeight-2, j, BlocksGT.Diggables, 1);
							set(aChunk, i, mHeight-3, j, BlocksGT.Diggables, 3);
							set(aChunk, i, mHeight-4, j, BlocksGT.Diggables, 4);
							set(aChunk, i, mHeight-5, j, BlocksGT.Diggables, 5);
							set(aChunk, i, mHeight-6, j, BlocksGT.Diggables, 6);
							for (int k = aWorld.minY()+1; k < mHeight-6; k++) set(aChunk, i, k, j, k < 32 ? BlocksGT.Limestone : BlocksGT.Marble, aRandom.nextBoolean()?2:0);
							switch(aRandom.nextInt(60)) {
							case  0: case  1: case  2: aWorld.litter(aMinX+i, mHeight+1, aMinZ+j, 32757, true); break;
							case  3: case  4: case  5: aWorld.litter(aMinX+i, mHeight+1, aMinZ+j, 32757, false); break;
							case  6: case  7: case  8: aWorld.litter(aMinX+i, mHeight+1, aMinZ+j, 32756, false); break;
							case  9: case 10: case 11: case 12: case 13: case 14: set(aChunk, i, mHeight+1, j, Blocks.tallgrass, 1); break;
							case 15: set(aChunk, i, mHeight+1, j, Blocks.yellow_flower, 0); break;
							case 16: set(aChunk, i, mHeight+1, j, Blocks.red_flower, 0); break;
							case 17: set(aChunk, i, mHeight+1, j, Blocks.red_flower, 1); break;
							case 18: set(aChunk, i, mHeight+1, j, Blocks.red_flower, 2); break;
							case 19: set(aChunk, i, mHeight+1, j, Blocks.red_flower, 3); break;
							case 20: set(aChunk, i, mHeight+1, j, Blocks.red_flower, 4); break;
							case 21: set(aChunk, i, mHeight+1, j, Blocks.red_flower, 5); break;
							case 22: set(aChunk, i, mHeight+1, j, Blocks.red_flower, 6); break;
							case 23: set(aChunk, i, mHeight+1, j, Blocks.red_flower, 7); break;
							case 24: set(aChunk, i, mHeight+1, j, Blocks.red_flower, 8); break;
							}
						}
					}
				}
			} else {
				if (aMinZ < 0) {
					if ((aMinX == 48 || aMinX == 64) && (aMinZ == -80 || aMinZ == -64)) {
						aWorld.biome(aChunk, "badlands");
						for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
							for (int k = 1; k < aWorld.maxY()-mHeight; k++) set(aChunk, i, mHeight+k, j, NB, 0);
							set(aChunk, i, mHeight  , j, Blocks.sand, 1);
							set(aChunk, i, mHeight-1, j, Blocks.sand, 1);
							set(aChunk, i, mHeight-2, j, Blocks.sand, 1);
							set(aChunk, i, mHeight-3, j, Blocks.sand, 1);
							set(aChunk, i, mHeight-4, j, Blocks.sand, 1);
							set(aChunk, i, mHeight-5, j, Blocks.sand, 1);
							for (int k = aWorld.minY()+1; k < mHeight-5; k++) set(aChunk, i, k, j, Blocks.hardened_clay, 0);
						}
						for (int i = 1; i <= 3; i++) {
							set(aChunk,  4, mHeight+i,  4, Blocks.cactus, 0);
							set(aChunk, 12, mHeight+i,  4, Blocks.cactus, 0);
							set(aChunk,  4, mHeight+i, 12, Blocks.cactus, 0);
							set(aChunk, 12, mHeight+i, 12, Blocks.cactus, 0);
						}
					} else {
						aWorld.biome(aChunk, "desert");
						for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
							for (int k = 1; k < aWorld.maxY()-mHeight; k++) set(aChunk, i, mHeight+k, j, NB, 0);
							set(aChunk, i, mHeight  , j, Blocks.sand, 0);
							set(aChunk, i, mHeight-1, j, Blocks.sand, 0);
							set(aChunk, i, mHeight-2, j, Blocks.sand, 0);
							set(aChunk, i, mHeight-3, j, Blocks.sand, 0);
							set(aChunk, i, mHeight-4, j, Blocks.sand, 0);
							set(aChunk, i, mHeight-5, j, Blocks.sand, 0);
							for (int k = aWorld.minY()+1; k < mHeight-5; k++) set(aChunk, i, k, j, Blocks.sandstone, 0);
						}
					}
				} else {
					if ((aMinX == 48 || aMinX == 64) && (aMinZ == 48 || aMinZ == 64)) {
						aWorld.biome(aChunk, "swamp");
						for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
							for (int k = 1; k < aWorld.maxY()-mHeight; k++) set(aChunk, i, mHeight+k, j, NB, 0);
							set(aChunk, i, mHeight  , j, Blocks.water, 0);
							set(aChunk, i, mHeight-1, j, BlocksGT.Diggables, 0);
							set(aChunk, i, mHeight-2, j, BlocksGT.Diggables, 0);
							set(aChunk, i, mHeight-3, j, BlocksGT.Diggables, 2);
							set(aChunk, i, mHeight-4, j, BlocksGT.Diggables, 2);
							set(aChunk, i, mHeight-5, j, BlocksGT.Diggables, 2);
							for (int k = aWorld.minY()+1; k < mHeight-5; k++) set(aChunk, i, k, j, k < 32 ? BlocksGT.GraniteRed : BlocksGT.GraniteBlack, aRandom.nextBoolean()?2:0);
							if (aRandom.nextInt(8) == 0) set(aChunk, i, mHeight+1, j, BlocksGT.Glowtus, aRandom.nextInt(16));
						}
						set(aChunk,  4, mHeight+1,  4, Blocks.waterlily, 0);
						set(aChunk, 12, mHeight+1,  4, Blocks.waterlily, 0);
						set(aChunk,  4, mHeight+1, 12, Blocks.waterlily, 0);
						set(aChunk, 12, mHeight+1, 12, Blocks.waterlily, 0);
					} else {
						aWorld.biome(aChunk, "jungle");
						if (T) {
							OriginWorld.Block tBlock = Blocks.coarse_dirt;
							for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
								for (int k = 1; k < aWorld.maxY()-mHeight; k++) set(aChunk, i, mHeight+k, j, NB, 0);
								set(aChunk, i, mHeight  , j, Blocks.grass, 0);
								set(aChunk, i, mHeight-1, j, tBlock, 0);
								set(aChunk, i, mHeight-2, j, tBlock, 0);
								set(aChunk, i, mHeight-3, j, tBlock, 0);
								set(aChunk, i, mHeight-4, j, tBlock, 0);
								set(aChunk, i, mHeight-5, j, tBlock, 0);
								for (int k = aWorld.minY()+1; k < mHeight-5; k++) set(aChunk, i, k, j, k < 32 ? BlocksGT.Komatiite : BlocksGT.Basalt, aRandom.nextBoolean()?2:0);
							}
						} else {
							for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
								for (int k = 1; k < aWorld.maxY()-mHeight; k++) set(aChunk, i, mHeight+k, j, NB, 0);
								set(aChunk, i, mHeight  , j, Blocks.grass, 0);
								set(aChunk, i, mHeight-1, j, Blocks.dirt, 1);
								set(aChunk, i, mHeight-2, j, Blocks.dirt, 1);
								set(aChunk, i, mHeight-3, j, Blocks.dirt, 1);
								set(aChunk, i, mHeight-4, j, Blocks.dirt, 1);
								set(aChunk, i, mHeight-5, j, Blocks.dirt, 1);
								for (int k = aWorld.minY()+1; k < mHeight-5; k++) set(aChunk, i, k, j, k < 32 ? BlocksGT.Komatiite : BlocksGT.Basalt, aRandom.nextBoolean()?2:0);
							}
						}
						set(aChunk, 6+aRandom.nextInt(4), mHeight+1, 6+aRandom.nextInt(4), Blocks.melon_block, 0);

						aWorld.tree(aMinX+ 4, mHeight+1, aMinZ+ 4, 3, 9+aRandom.nextInt(3), aRandom);
						aWorld.tree(aMinX+12, mHeight+1, aMinZ+ 4, 3, 9+aRandom.nextInt(3), aRandom);
						aWorld.tree(aMinX+ 4, mHeight+1, aMinZ+12, 3, 9+aRandom.nextInt(3), aRandom);
						aWorld.tree(aMinX+12, mHeight+1, aMinZ+12, 3, 9+aRandom.nextInt(3), aRandom);
					}
				}
			}
			return T;
		}
		return F;
	}
}
