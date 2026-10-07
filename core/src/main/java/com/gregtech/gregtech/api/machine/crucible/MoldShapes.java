package com.gregtech.gregtech.api.machine.crucible;

import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Original GT6 5x5 mold table, including rotations/mirrors and overwrite order.
 * Output Items/Blocks stay on each platform. Source: Gregorius Techneticies / GregTech 6 team, MultiTileEntityMold (LGPL).
 * Initialize after the shared material/prefix domain bootstrap.
 */
public final class MoldShapes {
    public static final int SHAPE_MASK = 0b11111_11111_11111_11111_11111;
    public record Recipe(MaterialPrefix itemPrefix, boolean blockSolid) {}
    private static final Recipe BLOCK_SOLID = new Recipe(null, true);
    private static final Map<Integer, Recipe> MOLD_RECIPES = new HashMap<>();
    private static final int[] B = new int[25];
    static {
        for (int i = 0; i < 25; i++) B[i] = 1 << i;
        initMoldRecipes();
    }
    private MoldShapes() {}
    private static Recipe item(MaterialPrefix prefix) { return new Recipe(prefix, false); }

    public static Map<Integer, Recipe> recipes() {
        return Collections.unmodifiableMap(MOLD_RECIPES);
    }

    /** Original masked lookup; unknown nonzero cavity patterns cast nuggets. */
    public static Recipe recipe(int rawShape) {
        int key = rawShape & SHAPE_MASK;
        Recipe recipe = MOLD_RECIPES.get(key);
        if (recipe == null && rawShape != 0) return item(MaterialPrefix.nugget);
        return recipe;
    }

    public static boolean isNuggetFallback(int rawShape) {
        int key = rawShape & SHAPE_MASK;
        return rawShape != 0 && !MOLD_RECIPES.containsKey(key);
    }

    public static long requiredMaterialUnits(int rawShape) {
        Recipe recipe = recipe(rawShape);
        if (recipe == null) return 0;
        // The original loop counts only the25 cavity bits, even when legacy NBT has high bits.
        if (recipe.itemPrefix() == MaterialPrefix.nugget)
            return (long) Integer.bitCount(rawShape & SHAPE_MASK) * GTValues.U9;
        // MaterialPrefixes.java:780-785 defines the original storage block as9U.
        return recipe.blockSolid() ? 9L * GTValues.U : recipe.itemPrefix().getMaterialWeight();
    }

    /** OP category identity for the original prefix name localization. */
    public static String sourcePrefixName(Recipe recipe) {
        if (recipe.blockSolid()) return "blockSolid";
        return recipe.itemPrefix() == MaterialPrefix.itemCasing ? "casingSmall" : recipe.itemPrefix().getName();
    }

    /** MultiTileEntityMold:628-921. Preserve the original HashMap stages and overwrite order. */
    private static void initMoldRecipes() {
		Map<Integer, Recipe> TEMP_MOLD_RECIPES = new HashMap<>();

		TEMP_MOLD_RECIPES.put(0b0_00100_11111_01110_01010_00000, item(MaterialPrefix.toolHeadBuilderwand));
		TEMP_MOLD_RECIPES.put(0b0_00000_00100_11111_01110_01010, item(MaterialPrefix.toolHeadBuilderwand));

		TEMP_MOLD_RECIPES.put(0b0_00000_00110_01111_01111_00110, item(MaterialPrefix.billet));
		TEMP_MOLD_RECIPES.put(0b0_00000_01100_11110_11110_01100, item(MaterialPrefix.billet));
		TEMP_MOLD_RECIPES.put(0b0_00110_01111_01111_00110_00000, item(MaterialPrefix.billet));
		TEMP_MOLD_RECIPES.put(0b0_01100_11110_11110_01100_00000, item(MaterialPrefix.billet));

		TEMP_MOLD_RECIPES.put(
		B[ 0]|B[ 1]|B[ 2]|B[ 3]|
		B[ 5]|B[ 6]|B[ 7]|B[ 8]|
		B[10]|B[11]|B[12]|B[13]|B[14]|
		B[15]|B[16]|B[17]|B[18]|
		B[20]|B[21]|B[22]|B[23]
		, item(MaterialPrefix.toolHeadRawPlow));

		TEMP_MOLD_RECIPES.put(
								B[ 4]|
						  B[ 8]|
					B[12]|
			  B[16]|
		B[20]
		, item(MaterialPrefix.stickLong));

		TEMP_MOLD_RECIPES.put(
		B[ 0]|B[ 1]|B[ 2]|B[ 3]|B[ 4]|
		B[ 5]|B[ 6]|B[ 7]|B[ 8]|B[ 9]|
		B[10]|B[11]|B[12]|B[13]|B[14]|
		B[15]|B[16]|B[17]|B[18]|B[19]|
		B[20]|B[21]|B[22]|B[23]|B[24]
		, item(MaterialPrefix.plate));

		TEMP_MOLD_RECIPES.put(
		B[ 0]|B[ 1]|B[ 2]|    B[ 4]|
		B[ 5]|B[ 6]|B[ 7]|    B[ 9]|
		B[10]|B[11]|B[12]|    B[14]|
							  B[19]|
		B[20]|B[21]|B[22]
		, item(MaterialPrefix.itemCasing));

		TEMP_MOLD_RECIPES.put(
		B[ 0]|      B[ 2]|      B[ 4]|
			  B[ 6]|B[ 7]|B[ 8]|
		B[10]|B[11]|      B[13]|B[14]|
			  B[16]|B[17]|B[18]|
		B[20]|      B[22]|      B[24]
		, item(MaterialPrefix.gearGt));

		TEMP_MOLD_RECIPES.put(
			  B[ 1]|      B[ 3]|
		B[ 5]|B[ 6]|B[ 7]|B[ 8]|B[ 9]|
			  B[11]|      B[13]|
		B[15]|B[16]|B[17]|B[18]|B[19]|
			  B[21]|      B[23]
		, item(MaterialPrefix.gearGtSmall));

		for (int i = 0; i < 3; i++) {
			TEMP_MOLD_RECIPES.put(
			B[i  + 0]|B[i  + 1]|B[i  + 2]|
			B[i  + 5]|B[i  + 6]|B[i  + 7]|
			B[i  +10]|B[i  +11]|B[i  +12]|
			B[i  +15]|B[i  +16]|B[i  +17]|
			B[i  +20]|B[i  +21]|B[i  +22]
			, item(MaterialPrefix.ingot));

			TEMP_MOLD_RECIPES.put(
			B[i  + 0]|B[i  + 1]|B[i  + 2]|
			B[i  + 5]|B[i  + 6]|
			B[i  +10]|B[i  +11]|
			B[i  +15]|B[i  +16]|
			B[i  +20]|B[i  +21]|B[i  +22]
			, item(MaterialPrefix.toolHeadRawAxeDouble));

			TEMP_MOLD_RECIPES.put(
			B[i  + 0]|B[i  + 1]|B[i  + 2]|
			B[i  + 5]|B[i  + 6]|B[i  + 7]|
			B[i  +10]|        B[i  +12]|
			B[i  +15]|B[i  +16]|B[i  +17]|
			B[i  +20]|B[i  +21]|B[i  +22]
			, item(MaterialPrefix.toolHeadHammer));

			TEMP_MOLD_RECIPES.put(
			B[i  + 0]|
			B[i  + 5]|
			B[i  +10]|
			B[i  +15]|
			B[i  +20]
			, item(MaterialPrefix.stick));

			TEMP_MOLD_RECIPES.put(
			B[i  + 0]|B[i  + 1]|B[i  + 2]|
					  B[i  + 6]|
					  B[i  +11]|
					  B[i  +16]|
					  B[i  +21]
			, item(MaterialPrefix.toolHeadRawChisel));

			TEMP_MOLD_RECIPES.put(
			B[i  + 0]|B[i  + 1]|B[i  + 2]|
			B[i  + 5]|B[i  + 6]|B[i  + 7]|
			B[i  +10]|B[i  +11]|B[i  +12]|
					  B[i  +16]|
					  B[i  +21]
			, item(MaterialPrefix.toolHeadFile));

			TEMP_MOLD_RECIPES.put(
					  B[i  + 1]|
			B[i  + 5]|B[i  + 6]|B[i  + 7]|
			B[i  +10]|B[i  +11]|B[i  +12]|
			B[i  +15]|B[i  +16]|B[i  +17]|
			B[i  +20]|B[i  +21]|B[i  +22]
			, item(MaterialPrefix.toolHeadRawSword));

			for (int j = 0; j < 4; j++) {
				TEMP_MOLD_RECIPES.put(
							  B[i  +j*5+ 1]|B[i  +j*5+ 2]|
				B[i  +j*5+ 5]|B[i  +j*5+ 6]|B[i  +j*5+ 7]
				, item(MaterialPrefix.toolHeadRawHoe));
			}

			for (int j = 0; j < 3; j++) {
				TEMP_MOLD_RECIPES.put(
							  B[i  +j*5+ 1]|
							  B[i  +j*5+ 6]|
				B[i  +j*5+10]|B[i  +j*5+11]|B[i  +j*5+12]
				, item(MaterialPrefix.toolHeadRawArrow));

				TEMP_MOLD_RECIPES.put(
				B[i  +j*5+ 0]|B[i  +j*5+ 1]|B[i  +j*5+ 2]|
				B[i  +j*5+ 5]|B[i  +j*5+ 6]|B[i  +j*5+ 7]|
				B[i  +j*5+10]
				, item(MaterialPrefix.toolHeadRawAxe));

				TEMP_MOLD_RECIPES.put(
				B[i  +j*5+ 0]|B[i  +j*5+ 1]|
				B[i  +j*5+ 5]|B[i  +j*5+ 6]
				, item(MaterialPrefix.chunkGt));

				TEMP_MOLD_RECIPES.put(
				B[i  +j*5+ 0]|B[i  +j*5+ 1]|B[i  +j*5+ 2]|
				B[i  +j*5+ 5]|            B[i  +j*5+ 7]|
				B[i  +j*5+10]|B[i  +j*5+11]|B[i  +j*5+12]
				, item(MaterialPrefix.ring));

				TEMP_MOLD_RECIPES.put(
				B[i  +j*5+ 0]|B[i  +j*5+ 1]|B[i  +j*5+ 2]|
				B[i  +j*5+ 5]|B[i  +j*5+ 6]|B[i  +j*5+ 7]|
				B[i  +j*5+10]|B[i  +j*5+11]|B[i  +j*5+12]
				, item(MaterialPrefix.plateTiny));

				TEMP_MOLD_RECIPES.put(
				B[i  +j*5+ 0]|
				B[i  +j*5+ 5]
				, item(MaterialPrefix.bolt));
			}

			for (int j = 0; j < 2; j++) {
				TEMP_MOLD_RECIPES.put(
							  B[i  +j*5+ 1]|
				B[i  +j*5+ 5]|B[i  +j*5+ 6]|B[i  +j*5+ 7]|
				B[i  +j*5+10]|B[i  +j*5+11]|B[i  +j*5+12]|
				B[i  +j*5+15]|B[i  +j*5+16]|B[i  +j*5+17]
				, item(MaterialPrefix.toolHeadRawShovel));

				TEMP_MOLD_RECIPES.put(
				B[i  +j*5+ 0]|B[i  +j*5+ 1]|B[i  +j*5+ 2]|
				B[i  +j*5+ 5]|B[i  +j*5+ 6]|B[i  +j*5+ 7]|
				B[i  +j*5+10]|B[i  +j*5+11]|B[i  +j*5+12]|
				B[i  +j*5+15]|            B[i  +j*5+17]
				, item(MaterialPrefix.toolHeadRawSpade));

				TEMP_MOLD_RECIPES.put(
							  B[i  +j*5+ 1]|
				B[i  +j*5+ 5]|B[i  +j*5+ 6]|B[i  +j*5+ 7]|
				B[i  +j*5+10]|B[i  +j*5+11]|
				B[i  +j*5+15]|B[i  +j*5+16]|B[i  +j*5+17]
				, item(MaterialPrefix.toolHeadRawUniversalSpade));

				TEMP_MOLD_RECIPES.put(
				B[i  +j*5+ 0]|
				B[i  +j*5+ 5]|
				B[i  +j*5+10]|
				B[i  +j*5+15]
				, item(MaterialPrefix.toolHeadScrewdriver));
			}
		}

		for (int i = 0; i < 4; i++) {
			TEMP_MOLD_RECIPES.put(
					  B[i  + 1]|
			B[i  + 5]|
			B[i  +10]|
			B[i  +15]|
					  B[i  +21]
			, item(MaterialPrefix.toolHeadRawPickaxe));

			TEMP_MOLD_RECIPES.put(
			B[i  + 0]|B[i  + 1]|
			B[i  + 5]|B[i  + 6]|
			B[i  +10]|B[i  +11]|
			B[i  +15]|B[i  +16]|
			B[i  +20]|B[i  +21]
			, item(MaterialPrefix.toolHeadRawSaw));

			TEMP_MOLD_RECIPES.put(
			B[i  + 0]|B[i  + 1]|
			B[i  + 5]|B[i  + 6]|
			B[i  +10]|B[i  +11]|
			B[i  +15]|B[i  +16]|
					  B[i  +21]
			, item(MaterialPrefix.toolHeadRawSense));

		}

		for (java.util.Map.Entry<Integer, Recipe> tEntry : TEMP_MOLD_RECIPES.entrySet()) {
			int tKey = tEntry.getKey(), tResult1 = 0, tResult2 = 0, tResult3 = 0;
			MOLD_RECIPES.put(tKey, tEntry.getValue());

			if ((tKey & B[ 0]) != 0) {tResult1 |= B[ 4]; tResult2 |= B[24]; tResult3 |= B[20];}
			if ((tKey & B[ 1]) != 0) {tResult1 |= B[ 9]; tResult2 |= B[23]; tResult3 |= B[15];}
			if ((tKey & B[ 2]) != 0) {tResult1 |= B[14]; tResult2 |= B[22]; tResult3 |= B[10];}
			if ((tKey & B[ 3]) != 0) {tResult1 |= B[19]; tResult2 |= B[21]; tResult3 |= B[ 5];}
			if ((tKey & B[ 4]) != 0) {tResult1 |= B[24]; tResult2 |= B[20]; tResult3 |= B[ 0];}

			if ((tKey & B[ 5]) != 0) {tResult1 |= B[ 3]; tResult2 |= B[19]; tResult3 |= B[21];}
			if ((tKey & B[ 6]) != 0) {tResult1 |= B[ 8]; tResult2 |= B[18]; tResult3 |= B[16];}
			if ((tKey & B[ 7]) != 0) {tResult1 |= B[13]; tResult2 |= B[17]; tResult3 |= B[11];}
			if ((tKey & B[ 8]) != 0) {tResult1 |= B[18]; tResult2 |= B[16]; tResult3 |= B[ 6];}
			if ((tKey & B[ 9]) != 0) {tResult1 |= B[23]; tResult2 |= B[15]; tResult3 |= B[ 1];}

			if ((tKey & B[10]) != 0) {tResult1 |= B[ 2]; tResult2 |= B[14]; tResult3 |= B[22];}
			if ((tKey & B[11]) != 0) {tResult1 |= B[ 7]; tResult2 |= B[13]; tResult3 |= B[17];}
			if ((tKey & B[12]) != 0) {tResult1 |= B[12]; tResult2 |= B[12]; tResult3 |= B[12];}
			if ((tKey & B[13]) != 0) {tResult1 |= B[17]; tResult2 |= B[11]; tResult3 |= B[ 7];}
			if ((tKey & B[14]) != 0) {tResult1 |= B[22]; tResult2 |= B[10]; tResult3 |= B[ 2];}

			if ((tKey & B[15]) != 0) {tResult1 |= B[ 1]; tResult2 |= B[ 9]; tResult3 |= B[23];}
			if ((tKey & B[16]) != 0) {tResult1 |= B[ 6]; tResult2 |= B[ 8]; tResult3 |= B[18];}
			if ((tKey & B[17]) != 0) {tResult1 |= B[11]; tResult2 |= B[ 7]; tResult3 |= B[13];}
			if ((tKey & B[18]) != 0) {tResult1 |= B[16]; tResult2 |= B[ 6]; tResult3 |= B[ 8];}
			if ((tKey & B[19]) != 0) {tResult1 |= B[21]; tResult2 |= B[ 5]; tResult3 |= B[ 3];}

			if ((tKey & B[20]) != 0) {tResult1 |= B[ 0]; tResult2 |= B[ 4]; tResult3 |= B[24];}
			if ((tKey & B[21]) != 0) {tResult1 |= B[ 5]; tResult2 |= B[ 3]; tResult3 |= B[19];}
			if ((tKey & B[22]) != 0) {tResult1 |= B[10]; tResult2 |= B[ 2]; tResult3 |= B[14];}
			if ((tKey & B[23]) != 0) {tResult1 |= B[15]; tResult2 |= B[ 1]; tResult3 |= B[ 9];}
			if ((tKey & B[24]) != 0) {tResult1 |= B[20]; tResult2 |= B[ 0]; tResult3 |= B[ 4];}

			MOLD_RECIPES.put(tResult1, tEntry.getValue());
			MOLD_RECIPES.put(tResult2, tEntry.getValue());
			MOLD_RECIPES.put(tResult3, tEntry.getValue());
		}

		TEMP_MOLD_RECIPES.putAll(MOLD_RECIPES);

		for (java.util.Map.Entry<Integer, Recipe> tEntry : TEMP_MOLD_RECIPES.entrySet()) {
			int tKey = tEntry.getKey(), tResult1 = 0, tResult2 = 0;

			if ((tKey & B[ 0]) != 0) {tResult1 |= B[ 4]; tResult2 |= B[20];}
			if ((tKey & B[ 1]) != 0) {tResult1 |= B[ 3]; tResult2 |= B[21];}
			if ((tKey & B[ 2]) != 0) {tResult1 |= B[ 2]; tResult2 |= B[22];}
			if ((tKey & B[ 3]) != 0) {tResult1 |= B[ 1]; tResult2 |= B[23];}
			if ((tKey & B[ 4]) != 0) {tResult1 |= B[ 0]; tResult2 |= B[24];}

			if ((tKey & B[ 5]) != 0) {tResult1 |= B[ 9]; tResult2 |= B[15];}
			if ((tKey & B[ 6]) != 0) {tResult1 |= B[ 8]; tResult2 |= B[16];}
			if ((tKey & B[ 7]) != 0) {tResult1 |= B[ 7]; tResult2 |= B[17];}
			if ((tKey & B[ 8]) != 0) {tResult1 |= B[ 6]; tResult2 |= B[18];}
			if ((tKey & B[ 9]) != 0) {tResult1 |= B[ 5]; tResult2 |= B[19];}

			if ((tKey & B[10]) != 0) {tResult1 |= B[14]; tResult2 |= B[10];}
			if ((tKey & B[11]) != 0) {tResult1 |= B[13]; tResult2 |= B[11];}
			if ((tKey & B[12]) != 0) {tResult1 |= B[12]; tResult2 |= B[12];}
			if ((tKey & B[13]) != 0) {tResult1 |= B[11]; tResult2 |= B[13];}
			if ((tKey & B[14]) != 0) {tResult1 |= B[10]; tResult2 |= B[14];}

			if ((tKey & B[15]) != 0) {tResult1 |= B[19]; tResult2 |= B[ 5];}
			if ((tKey & B[16]) != 0) {tResult1 |= B[18]; tResult2 |= B[ 6];}
			if ((tKey & B[17]) != 0) {tResult1 |= B[17]; tResult2 |= B[ 7];}
			if ((tKey & B[18]) != 0) {tResult1 |= B[16]; tResult2 |= B[ 8];}
			if ((tKey & B[19]) != 0) {tResult1 |= B[15]; tResult2 |= B[ 9];}

			if ((tKey & B[20]) != 0) {tResult1 |= B[24]; tResult2 |= B[ 0];}
			if ((tKey & B[21]) != 0) {tResult1 |= B[23]; tResult2 |= B[ 1];}
			if ((tKey & B[22]) != 0) {tResult1 |= B[22]; tResult2 |= B[ 2];}
			if ((tKey & B[23]) != 0) {tResult1 |= B[21]; tResult2 |= B[ 3];}
			if ((tKey & B[24]) != 0) {tResult1 |= B[20]; tResult2 |= B[ 4];}

			MOLD_RECIPES.put(tResult1, tEntry.getValue());
			MOLD_RECIPES.put(tResult2, tEntry.getValue());
		}
    }
}
