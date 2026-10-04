/**
 * Copyright (c) 2022 GregTech-6 Team
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
public final class SourceTesting {
private final int mHeight; public SourceTesting(int height) {mHeight=height;}
    public boolean generate(OriginWorld aWorld, OriginWorld.Chunk aChunk, int aMinX, int aMinZ) {
		if ((aMinX != 32 && aMinX != 48) || (aMinZ != -32 && aMinZ != -48)) return F;

		for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
			for (int k = aWorld.minY()+1; k <= mHeight; k++) set(aChunk, i, k, j, BlocksGT.Concrete, DYE_INDEX_Gray);
			for (int k = mHeight+2; k < aWorld.maxY(); k++) set(aChunk, i, k, j, NB, 0);

			set(aChunk, i, mHeight+ 1, j, BlocksGT.CFoam, DYE_INDEX_Gray);
			if ((i == 0 && aMinX == 32) || (i == 15 && aMinX == 48) || (j == 0 && aMinZ == -48) || (j == 15 && aMinZ == -32)) {
			set(aChunk, i, mHeight+ 2, j, BlocksGT.CFoam, DYE_INDEX_LightBlue);
			set(aChunk, i, mHeight+ 3, j, BlocksGT.CFoam, DYE_INDEX_Yellow);
			set(aChunk, i, mHeight+ 4, j, BlocksGT.CFoam, DYE_INDEX_LightBlue);
			set(aChunk, i, mHeight+ 5, j, BlocksGT.CFoam, DYE_INDEX_LightBlue);
			set(aChunk, i, mHeight+ 6, j, BlocksGT.CFoam, DYE_INDEX_LightBlue);
			set(aChunk, i, mHeight+ 7, j, BlocksGT.CFoam, DYE_INDEX_LightBlue);
			set(aChunk, i, mHeight+ 8, j, BlocksGT.CFoam, DYE_INDEX_LightBlue);
			set(aChunk, i, mHeight+ 9, j, BlocksGT.CFoam, DYE_INDEX_LightBlue);
			set(aChunk, i, mHeight+10, j, BlocksGT.CFoam, DYE_INDEX_LightBlue);
			set(aChunk, i, mHeight+11, j, BlocksGT.CFoam, DYE_INDEX_LightBlue);
			set(aChunk, i, mHeight+12, j, BlocksGT.CFoam, DYE_INDEX_LightBlue);
			set(aChunk, i, mHeight+13, j, BlocksGT.CFoam, DYE_INDEX_Yellow);
			set(aChunk, i, mHeight+14, j, BlocksGT.CFoam, DYE_INDEX_LightBlue);
			set(aChunk, i, mHeight+15, j, BlocksGT.CFoam, DYE_INDEX_Gray);
			} else if ((i != 1 && i != 5 && i != 10 && i != 14) && (j != 1 && j != 5 && j != 10 && j != 14)) {
			set(aChunk, i, mHeight+15, j, BlocksGT.GLOW_GLASS_SLABS[1], DYE_INDEX_LightBlue);
			} else {
			set(aChunk, i, mHeight+15, j, BlocksGT.FOAM_SLABS[1], DYE_INDEX_LightGray);
			}
		}

		if (aMinX == 32 && aMinZ == -32) {
			set(aChunk, 0, mHeight+ 2, 5, BlocksGT.CFoam, DYE_INDEX_Gray);
			set(aChunk, 0, mHeight+ 2, 6, NB, 0);
			set(aChunk, 0, mHeight+ 2, 7, NB, 0);
			set(aChunk, 0, mHeight+ 2, 8, NB, 0);
			set(aChunk, 0, mHeight+ 2, 9, NB, 0);
			set(aChunk, 0, mHeight+ 2,10, BlocksGT.CFoam, DYE_INDEX_Gray);
			set(aChunk, 1, mHeight+ 2, 6, BlocksGT.CFoam, DYE_INDEX_Gray);
			set(aChunk, 1, mHeight+ 2, 9, BlocksGT.CFoam, DYE_INDEX_Gray);

			set(aChunk, 0, mHeight+ 3, 5, BlocksGT.CFoam, DYE_INDEX_Yellow);
			set(aChunk, 0, mHeight+ 3, 6, NB, 0);
			set(aChunk, 0, mHeight+ 3, 7, NB, 0);
			set(aChunk, 0, mHeight+ 3, 8, NB, 0);
			set(aChunk, 0, mHeight+ 3, 9, NB, 0);
			set(aChunk, 0, mHeight+ 3,10, BlocksGT.CFoam, DYE_INDEX_Yellow);
			set(aChunk, 1, mHeight+ 3, 6, BlocksGT.CFoam, DYE_INDEX_Yellow);
			set(aChunk, 1, mHeight+ 3, 9, BlocksGT.CFoam, DYE_INDEX_Yellow);

			set(aChunk, 0, mHeight+ 4, 5, BlocksGT.CFoam, DYE_INDEX_Gray);
			set(aChunk, 0, mHeight+ 4, 6, NB, 0);
			set(aChunk, 0, mHeight+ 4, 7, NB, 0);
			set(aChunk, 0, mHeight+ 4, 8, NB, 0);
			set(aChunk, 0, mHeight+ 4, 9, NB, 0);
			set(aChunk, 0, mHeight+ 4,10, BlocksGT.CFoam, DYE_INDEX_Gray);
			set(aChunk, 1, mHeight+ 4, 6, BlocksGT.CFoam, DYE_INDEX_Gray);
			set(aChunk, 1, mHeight+ 4, 7, BlocksGT.CFoam, DYE_INDEX_Gray);
			set(aChunk, 1, mHeight+ 4, 8, BlocksGT.CFoam, DYE_INDEX_Gray);
			set(aChunk, 1, mHeight+ 4, 9, BlocksGT.CFoam, DYE_INDEX_Gray);

			set(aChunk, 0, mHeight+ 5, 5, BlocksGT.CFoam, DYE_INDEX_Gray);
			set(aChunk, 0, mHeight+ 5, 6, BlocksGT.CFoam, DYE_INDEX_Gray);
			set(aChunk, 0, mHeight+ 5, 7, BlocksGT.CFoam, DYE_INDEX_Gray);
			set(aChunk, 0, mHeight+ 5, 8, BlocksGT.CFoam, DYE_INDEX_Gray);
			set(aChunk, 0, mHeight+ 5, 9, BlocksGT.CFoam, DYE_INDEX_Gray);
			set(aChunk, 0, mHeight+ 5,10, BlocksGT.CFoam, DYE_INDEX_Gray);



			aWorld.tile(33, mHeight+2, -20, 7133, "UT.NBT.make(NBT_FACING, SIDE_X_POS)");
			aWorld.tile(33, mHeight+2, -21, 7133, "UT.NBT.make(NBT_FACING, SIDE_X_POS)");
			aWorld.tile(33, mHeight+2, -22, 7133, "UT.NBT.make(NBT_FACING, SIDE_X_POS)");
			aWorld.tile(33, mHeight+3, -20, 4033, "UT.NBT.make(NBT_FACING, SIDE_X_POS)");
			aWorld.tile(33, mHeight+3, -21, 4033, "UT.NBT.make(NBT_FACING, SIDE_X_POS)");
			aWorld.tile(33, mHeight+3, -22, 4033, "UT.NBT.make(NBT_FACING, SIDE_X_POS)");
			aWorld.tile(33, mHeight+4, -20, 6033, "UT.NBT.make(NBT_FACING, SIDE_X_POS)");
			aWorld.tile(33, mHeight+4, -21, 6033, "UT.NBT.make(NBT_FACING, SIDE_X_POS)");
			aWorld.tile(33, mHeight+4, -22, 6033, "UT.NBT.make(NBT_FACING, SIDE_X_POS)");
			aWorld.tile(33, mHeight+5, -20, 6033, "UT.NBT.make(NBT_FACING, SIDE_X_POS)");
			aWorld.tile(33, mHeight+5, -21, 6033, "UT.NBT.make(NBT_FACING, SIDE_X_POS)");
			aWorld.tile(33, mHeight+5, -22, 6033, "UT.NBT.make(NBT_FACING, SIDE_X_POS)");

			aWorld.tile(33, mHeight+5, -23, 14999, "UT.NBT.make(NBT_ACTIVE_ENERGY, T)");
			aWorld.tile(33, mHeight+5, -24, 14999, "UT.NBT.make(NBT_ACTIVE_ENERGY, F)");

			aWorld.tile(33, mHeight+2, -19, 32057, "null");

			set(aWorld, 33, mHeight+2, -18, Blocks.crafting_table, 0, 3);
			aWorld.tile(33, mHeight+3, -18, 32737, "null");
			aWorld.tile(33, mHeight+4, -18, 32727, "UT.NBT.make(NBT_FACING, SIDE_Z_POS)");

			aWorld.tile(34, mHeight+2, -18, 5033, "UT.NBT.make(NBT_FACING, SIDE_Z_NEG)");
			aWorld.tile(34, mHeight+3, -18, 32739, "null");
			aWorld.tile(34, mHeight+4, -18, 32062, "UT.NBT.make(NBT_FACING, SIDE_Z_POS)");

			aWorld.tile(36, mHeight+3, -18, 32719, "null");
			aWorld.tile(36, mHeight+2, -18, 26304, "UT.NBT.make(NBT_CONNECTION, (byte)(SBIT_U | SBIT_D))");
			aWorld.tile(36, mHeight+1, -18, 26304, "UT.NBT.make(NBT_CONNECTION, (byte)(SBIT_U | SBIT_D))");
			aWorld.tile(36, mHeight  , -18, 26304, "UT.NBT.make(NBT_CONNECTION, (byte)(SBIT_U | SBIT_D))");
			aWorld.tile(36, mHeight-1, -18, 26304, "UT.NBT.make(NBT_CONNECTION, (byte)(SBIT_U | SBIT_D))");
			aWorld.tile(36, mHeight-2, -18, 26304, "UT.NBT.make(NBT_CONNECTION, (byte)(SBIT_U | SBIT_D))");
			aWorld.tile(36, mHeight-3, -18, 26304, "UT.NBT.make(NBT_CONNECTION, (byte)(SBIT_U | SBIT_D))");
			aWorld.tile(36, mHeight-4, -18, 26304, "UT.NBT.make(NBT_CONNECTION, (byte)(SBIT_U | SBIT_S))");
			aWorld.tile(36, mHeight-4, -17, 26304, "UT.NBT.make(NBT_CONNECTION, SBIT_N)");
            aWorld.drain(36, mHeight-4, -17, SIDE_Z_POS);

			aWorld.tile(35, mHeight+2, -18, 32705, "UT.NBT.make(NBT_FACING, SIDE_X_POS)");
			aWorld.tile(35, mHeight+3, -18, 32732, "UT.NBT.make(NBT_FACING, SIDE_X_POS)");
			aWorld.tile(35, mHeight+4, -18, 32750, "UT.NBT.make(NBT_FACING, SIDE_Z_POS)");

			set(aWorld, 36, mHeight+2, -19, Blocks.cauldron, 0, 3);
			aWorld.tile(36, mHeight+3, -19, 32732, "UT.NBT.make(NBT_FACING, SIDE_Z_POS)");

			aWorld.tile(37, mHeight+2, -18, 32707, "UT.NBT.make(NBT_FACING, SIDE_X_NEG)");
			aWorld.tile(37, mHeight+3, -18, 32732, "UT.NBT.make(NBT_FACING, SIDE_X_NEG)");

			set(aWorld, 38, mHeight+2, -18, Blocks.crafting_table, 0, 3);
			aWorld.tile(38, mHeight+3, -18, 32744, "null");

			// Lots of Items I want to have ready whenever I generate a new Test World.


			aWorld.tile(39, mHeight+2, -18, 4033, "UT.NBT.make(NBT_FACING, SIDE_Z_NEG, NBT_INV_LIST, UT.NBT.makeInv(tInventory))");
			aWorld.tile(39, mHeight+3, -18, 32722, "null");

			aWorld.tile(40, mHeight+2, -18, 4033, "UT.NBT.make(NBT_FACING, SIDE_Z_NEG)");
			aWorld.tile(40, mHeight+3, -18, 32735, "null");

			aWorld.tile(41, mHeight+2, -18, 5033, "UT.NBT.make(NBT_FACING, SIDE_Z_NEG)");


			aWorld.tile(42, mHeight+2, -18, 32703, "UT.NBT.make(NBT_FACING, SIDE_Z_NEG, NBT_STATE, 4)");

			aWorld.tile(43, mHeight+2, -18, 32048, "null");

			aWorld.tile(44, mHeight+1, -20, 32709, "null");
			aWorld.tile(44, mHeight+1, -19, 8033, "UT.NBT.make(NBT_FACING, SIDE_Z_NEG)");
			aWorld.tile(44, mHeight+2, -20, 32711, "UT.NBT.make(NBT_FACING, SIDE_Y_POS, NBT_MODE, T)");
			aWorld.tile(44, mHeight+2, -19, 32702, "null");
			aWorld.tile(44, mHeight+2, -18, 8033, "UT.NBT.make(NBT_FACING, SIDE_Z_NEG)");

			set(aWorld, 45, mHeight+2, -18, Blocks.ender_chest, 2, 3);

			set(aWorld, 46, mHeight+2, -18, Blocks.crafting_table, 0, 3);
			set(aWorld, 46, mHeight+3, -18, Blocks.brewing_stand, 0, 3);

			set(aWorld, 47, mHeight+2, -18, Blocks.anvil, 0, 3);
		}

		aWorld.setSpawnLocation(0, mHeight+5, 0);
		return T;
	}
}
