package com.gregtech.gregtech.api.machine.crucible;

import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Full original5x5 mold table, including rotations/mirrors and original overwrite order.
 * Output Items/Blocks stay on each platform. Source/permission evidence: mold-shapes-extraction.json.
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
        if (recipe == null && key != 0) return item(MaterialPrefix.nugget);
        return recipe;
    }

    public static boolean isNuggetFallback(int rawShape) {
        int key = rawShape & SHAPE_MASK;
        return key != 0 && !MOLD_RECIPES.containsKey(key);
    }

    public static long requiredMaterialUnits(int rawShape) {
        Recipe recipe = recipe(rawShape);
        if (recipe == null) return 0;
        // Preserve original raw-shape bitCount for legacy NBT, rather than silently masking it.
        if (isNuggetFallback(rawShape)) return (long) Integer.bitCount(rawShape) * GTValues.U9;
        // MaterialPrefixes.java:780-785 defines the original storage block as9U.
        return recipe.blockSolid() ? 9L * GTValues.U : recipe.itemPrefix().getMaterialWeight();
    }

    /** Verbatim original bit operations/order; only output values are wrapped as pure tokens. */
    private static void initMoldRecipes() {
        Map<Integer, Recipe> temp = new HashMap<>();

        // --- Builder wand head (2 patterns) ---
        temp.put(B[ 2]|B[ 7]|B[ 8]|B[ 9]|B[11]|B[12]|B[13]|B[14]|B[15]|B[17]|B[18]|B[19], item(MaterialPrefix.toolHeadBuilderwand));
        temp.put(B[ 2]|B[ 7]|B[ 8]|B[ 9]|B[11]|B[12]|B[13]|B[14]|B[17]|B[18]|B[19], item(MaterialPrefix.toolHeadBuilderwand));

        // --- Billet (4 patterns) ---
        temp.put(B[ 6]|B[ 7]|B[11]|B[12]|B[13]|B[14]|B[16]|B[17]|B[18]|B[19], item(MaterialPrefix.billet));
        temp.put(B[ 7]|B[ 8]|B[12]|B[13]|B[14]|B[17]|B[18]|B[19]|B[22]|B[23], item(MaterialPrefix.billet));
        temp.put(B[ 1]|B[ 2]|B[ 6]|B[ 7]|B[11]|B[12]|B[13]|B[14]|B[16]|B[17], item(MaterialPrefix.billet));
        temp.put(B[ 2]|B[ 3]|B[ 7]|B[ 8]|B[12]|B[13]|B[14]|B[17]|B[18]|B[22]|B[23], item(MaterialPrefix.billet));

        // --- Plow head (1 pattern) ---
        temp.put(B[ 0]|B[ 1]|B[ 2]|B[ 3]|B[ 5]|B[ 6]|B[ 7]|B[ 8]|B[10]|B[11]|B[12]|B[13]|B[14]|B[15]|B[16]|B[17]|B[18]|B[20]|B[21]|B[22]|B[23], item(MaterialPrefix.toolHeadRawPlow));

        // --- Long rod (1 pattern: diagonal) ---
        temp.put(B[ 4]|B[ 8]|B[12]|B[16]|B[20], item(MaterialPrefix.stickLong));

        // --- Plate (full 5×5) ---
        temp.put(B[ 0]|B[ 1]|B[ 2]|B[ 3]|B[ 4]|B[ 5]|B[ 6]|B[ 7]|B[ 8]|B[ 9]|B[10]|B[11]|B[12]|B[13]|B[14]|B[15]|B[16]|B[17]|B[18]|B[19]|B[20]|B[21]|B[22]|B[23]|B[24], item(MaterialPrefix.plate));

        // --- Small casing (outer frame, 3 missing corners) ---
        temp.put(B[ 0]|B[ 1]|B[ 2]|B[ 4]|B[ 5]|B[ 6]|B[ 7]|B[ 9]|B[10]|B[11]|B[12]|B[14]|B[19]|B[20]|B[21]|B[22], item(MaterialPrefix.itemCasing));

        // --- Gear (cross shape) ---
        temp.put(B[ 0]|B[ 2]|B[ 4]|B[ 6]|B[ 7]|B[ 8]|B[10]|B[11]|B[13]|B[14]|B[16]|B[17]|B[18]|B[20]|B[22]|B[24], item(MaterialPrefix.gearGt));

        // --- Small gear ---
        temp.put(B[ 1]|B[ 3]|B[ 5]|B[ 6]|B[ 7]|B[ 8]|B[ 9]|B[11]|B[13]|B[15]|B[16]|B[17]|B[18]|B[19]|B[21]|B[23], item(MaterialPrefix.gearGtSmall));

        // --- Sliding-window recipes (3 horizontal offsets) ---
        for (int i = 0; i < 3; i++) {
            // Ingot (3-wide × 5-tall strip)
            temp.put(B[i+ 0]|B[i+ 1]|B[i+ 2]|B[i+ 5]|B[i+ 6]|B[i+ 7]|B[i+10]|B[i+11]|B[i+12]|B[i+15]|B[i+16]|B[i+17]|B[i+20]|B[i+21]|B[i+22], item(MaterialPrefix.ingot));

            // Double axe head
            temp.put(B[i+ 0]|B[i+ 1]|B[i+ 2]|B[i+ 5]|B[i+ 6]|B[i+10]|B[i+11]|B[i+15]|B[i+16]|B[i+20]|B[i+21]|B[i+22], item(MaterialPrefix.toolHeadRawAxeDouble));

            // Hammer head
            temp.put(B[i+ 0]|B[i+ 1]|B[i+ 2]|B[i+ 5]|B[i+ 6]|B[i+ 7]|B[i+10]|B[i+12]|B[i+15]|B[i+16]|B[i+17]|B[i+20]|B[i+21]|B[i+22], item(MaterialPrefix.toolHeadHammer));

            // Stick (vertical line, 5 cells)
            temp.put(B[i+ 0]|B[i+ 5]|B[i+10]|B[i+15]|B[i+20], item(MaterialPrefix.stick));

            // Chisel head
            temp.put(B[i+ 0]|B[i+ 1]|B[i+ 2]|B[i+ 6]|B[i+11]|B[i+16]|B[i+21], item(MaterialPrefix.toolHeadRawChisel));

            // File head
            temp.put(B[i+ 0]|B[i+ 1]|B[i+ 2]|B[i+ 5]|B[i+ 6]|B[i+ 7]|B[i+10]|B[i+11]|B[i+12]|B[i+16]|B[i+21], item(MaterialPrefix.toolHeadFile));

            // Sword blade
            temp.put(B[i+ 1]|B[i+ 5]|B[i+ 6]|B[i+ 7]|B[i+10]|B[i+11]|B[i+12]|B[i+15]|B[i+16]|B[i+17]|B[i+20]|B[i+21]|B[i+22], item(MaterialPrefix.toolHeadRawSword));

            for (int j = 0; j < 4; j++) {
                // Hoe head (2×3 block sliding down)
                temp.put(B[i+j*5+1]|B[i+j*5+2]|B[i+j*5+5]|B[i+j*5+6]|B[i+j*5+7], item(MaterialPrefix.toolHeadRawHoe));
            }

            for (int j = 0; j < 3; j++) {
                // Arrow head
                temp.put(B[i+j*5+1]|B[i+j*5+6]|B[i+j*5+10]|B[i+j*5+11]|B[i+j*5+12], item(MaterialPrefix.toolHeadRawArrow));

                // Axe head
                temp.put(B[i+j*5+0]|B[i+j*5+1]|B[i+j*5+2]|B[i+j*5+5]|B[i+j*5+6]|B[i+j*5+7]|B[i+j*5+10], item(MaterialPrefix.toolHeadRawAxe));

                // Chunk (2×2 square)
                temp.put(B[i+j*5+0]|B[i+j*5+1]|B[i+j*5+5]|B[i+j*5+6], item(MaterialPrefix.chunkGt));

                // Ring (3×3 with corners removed)
                temp.put(B[i+j*5+0]|B[i+j*5+1]|B[i+j*5+2]|B[i+j*5+5]|B[i+j*5+7]|B[i+j*5+10]|B[i+j*5+11]|B[i+j*5+12], item(MaterialPrefix.ring));

                // Tiny plate (3×3 filled)
                temp.put(B[i+j*5+0]|B[i+j*5+1]|B[i+j*5+2]|B[i+j*5+5]|B[i+j*5+6]|B[i+j*5+7]|B[i+j*5+10]|B[i+j*5+11]|B[i+j*5+12], item(MaterialPrefix.plateTiny));

                // Bolt (2 cells vertical)
                temp.put(B[i+j*5+0]|B[i+j*5+5], item(MaterialPrefix.bolt));
            }

            for (int j = 0; j < 2; j++) {
                // Shovel head
                temp.put(B[i+j*5+1]|B[i+j*5+5]|B[i+j*5+6]|B[i+j*5+7]|B[i+j*5+10]|B[i+j*5+11]|B[i+j*5+12]|B[i+j*5+15]|B[i+j*5+16]|B[i+j*5+17], item(MaterialPrefix.toolHeadRawShovel));

                // Spade head
                temp.put(B[i+j*5+0]|B[i+j*5+1]|B[i+j*5+2]|B[i+j*5+5]|B[i+j*5+6]|B[i+j*5+7]|B[i+j*5+10]|B[i+j*5+11]|B[i+j*5+12]|B[i+j*5+15]|B[i+j*5+17], item(MaterialPrefix.toolHeadRawSpade));

                // Universal spade head
                temp.put(B[i+j*5+1]|B[i+j*5+5]|B[i+j*5+6]|B[i+j*5+7]|B[i+j*5+10]|B[i+j*5+11]|B[i+j*5+15]|B[i+j*5+16]|B[i+j*5+17], item(MaterialPrefix.toolHeadRawUniversalSpade));

                // Screwdriver head (vertical line, 4 cells)
                temp.put(B[i+j*5+0]|B[i+j*5+5]|B[i+j*5+10]|B[i+j*5+15], item(MaterialPrefix.toolHeadScrewdriver));
            }
        }

        // --- Pickaxe, Saw, Sense patterns (4 offsets) ---
        for (int i = 0; i < 4; i++) {
            temp.put(B[i+1]|B[i+5]|B[i+10]|B[i+15]|B[i+21], item(MaterialPrefix.toolHeadRawPickaxe));

            temp.put(B[i+0]|B[i+1]|B[i+5]|B[i+6]|B[i+10]|B[i+11]|B[i+15]|B[i+16]|B[i+20]|B[i+21], item(MaterialPrefix.toolHeadRawSaw));

            temp.put(B[i+0]|B[i+1]|B[i+5]|B[i+6]|B[i+10]|B[i+11]|B[i+15]|B[i+16]|B[i+21], item(MaterialPrefix.toolHeadRawSense));
        }

        // --- Phase 1: 90° rotations (3 per recipe) ---
        Map<Integer, Recipe> tempCopy = new HashMap<>(temp);
        temp.clear();
        for (var entry : tempCopy.entrySet()) {
            int key = entry.getKey();
            temp.put(key, entry.getValue());
            int r1 = 0, r2 = 0, r3 = 0;

            if ((key & B[ 0]) != 0) { r1 |= B[ 4]; r2 |= B[24]; r3 |= B[20]; }
            if ((key & B[ 1]) != 0) { r1 |= B[ 9]; r2 |= B[23]; r3 |= B[15]; }
            if ((key & B[ 2]) != 0) { r1 |= B[14]; r2 |= B[22]; r3 |= B[10]; }
            if ((key & B[ 3]) != 0) { r1 |= B[19]; r2 |= B[21]; r3 |= B[ 5]; }
            if ((key & B[ 4]) != 0) { r1 |= B[24]; r2 |= B[20]; r3 |= B[ 0]; }

            if ((key & B[ 5]) != 0) { r1 |= B[ 3]; r2 |= B[19]; r3 |= B[21]; }
            if ((key & B[ 6]) != 0) { r1 |= B[ 8]; r2 |= B[18]; r3 |= B[16]; }
            if ((key & B[ 7]) != 0) { r1 |= B[13]; r2 |= B[17]; r3 |= B[11]; }
            if ((key & B[ 8]) != 0) { r1 |= B[18]; r2 |= B[16]; r3 |= B[ 6]; }
            if ((key & B[ 9]) != 0) { r1 |= B[23]; r2 |= B[15]; r3 |= B[ 1]; }

            if ((key & B[10]) != 0) { r1 |= B[ 2]; r2 |= B[14]; r3 |= B[22]; }
            if ((key & B[11]) != 0) { r1 |= B[ 7]; r2 |= B[13]; r3 |= B[17]; }
            if ((key & B[12]) != 0) { r1 |= B[12]; r2 |= B[12]; r3 |= B[12]; }
            if ((key & B[13]) != 0) { r1 |= B[17]; r2 |= B[11]; r3 |= B[ 7]; }
            if ((key & B[14]) != 0) { r1 |= B[22]; r2 |= B[10]; r3 |= B[ 2]; }

            if ((key & B[15]) != 0) { r1 |= B[ 1]; r2 |= B[ 9]; r3 |= B[23]; }
            if ((key & B[16]) != 0) { r1 |= B[ 6]; r2 |= B[ 8]; r3 |= B[18]; }
            if ((key & B[17]) != 0) { r1 |= B[11]; r2 |= B[ 7]; r3 |= B[13]; }
            if ((key & B[18]) != 0) { r1 |= B[16]; r2 |= B[ 6]; r3 |= B[ 8]; }
            if ((key & B[19]) != 0) { r1 |= B[21]; r2 |= B[ 5]; r3 |= B[ 3]; }

            if ((key & B[20]) != 0) { r1 |= B[ 0]; r2 |= B[ 4]; r3 |= B[24]; }
            if ((key & B[21]) != 0) { r1 |= B[ 5]; r2 |= B[ 3]; r3 |= B[19]; }
            if ((key & B[22]) != 0) { r1 |= B[10]; r2 |= B[ 2]; r3 |= B[14]; }
            if ((key & B[23]) != 0) { r1 |= B[15]; r2 |= B[ 1]; r3 |= B[ 9]; }
            if ((key & B[24]) != 0) { r1 |= B[20]; r2 |= B[ 0]; r3 |= B[ 4]; }

            MOLD_RECIPES.put(r1, entry.getValue());
            MOLD_RECIPES.put(r2, entry.getValue());
            MOLD_RECIPES.put(r3, entry.getValue());
        }

        // --- Phase 2: merge temp into MOLD_RECIPES ---
        for (var entry : temp.entrySet()) {
            MOLD_RECIPES.put(entry.getKey(), entry.getValue());
        }

        // --- Phase 3: mirror / flip (X and Y flips) ---
        Map<Integer, Recipe> allRecipes = new HashMap<>(MOLD_RECIPES);
        for (var entry : allRecipes.entrySet()) {
            int key = entry.getKey(), f1 = 0, f2 = 0;

            // Flip X (column reverse within each row)
            if ((key & B[ 0]) != 0) { f1 |= B[ 4]; f2 |= B[20]; }
            if ((key & B[ 1]) != 0) { f1 |= B[ 3]; f2 |= B[21]; }
            if ((key & B[ 2]) != 0) { f1 |= B[ 2]; f2 |= B[22]; }
            if ((key & B[ 3]) != 0) { f1 |= B[ 1]; f2 |= B[23]; }
            if ((key & B[ 4]) != 0) { f1 |= B[ 0]; f2 |= B[24]; }

            if ((key & B[ 5]) != 0) { f1 |= B[ 9]; f2 |= B[15]; }
            if ((key & B[ 6]) != 0) { f1 |= B[ 8]; f2 |= B[16]; }
            if ((key & B[ 7]) != 0) { f1 |= B[ 7]; f2 |= B[17]; }
            if ((key & B[ 8]) != 0) { f1 |= B[ 6]; f2 |= B[18]; }
            if ((key & B[ 9]) != 0) { f1 |= B[ 5]; f2 |= B[19]; }

            if ((key & B[10]) != 0) { f1 |= B[14]; f2 |= B[10]; }
            if ((key & B[11]) != 0) { f1 |= B[13]; f2 |= B[11]; }
            if ((key & B[12]) != 0) { f1 |= B[12]; f2 |= B[12]; }
            if ((key & B[13]) != 0) { f1 |= B[11]; f2 |= B[13]; }
            if ((key & B[14]) != 0) { f1 |= B[10]; f2 |= B[14]; }

            if ((key & B[15]) != 0) { f1 |= B[19]; f2 |= B[ 5]; }
            if ((key & B[16]) != 0) { f1 |= B[18]; f2 |= B[ 6]; }
            if ((key & B[17]) != 0) { f1 |= B[17]; f2 |= B[ 7]; }
            if ((key & B[18]) != 0) { f1 |= B[16]; f2 |= B[ 8]; }
            if ((key & B[19]) != 0) { f1 |= B[15]; f2 |= B[ 9]; }

            if ((key & B[20]) != 0) { f1 |= B[24]; f2 |= B[ 0]; }
            if ((key & B[21]) != 0) { f1 |= B[23]; f2 |= B[ 1]; }
            if ((key & B[22]) != 0) { f1 |= B[22]; f2 |= B[ 2]; }
            if ((key & B[23]) != 0) { f1 |= B[21]; f2 |= B[ 3]; }
            if ((key & B[24]) != 0) { f1 |= B[20]; f2 |= B[ 4]; }

            MOLD_RECIPES.put(f1, entry.getValue());
            MOLD_RECIPES.put(f2, entry.getValue());
        }

        // --- Add block-solid recipe (4×5, bottom-aligned) ---
        MOLD_RECIPES.put(B[ 5]|B[ 6]|B[ 7]|B[ 8]|B[ 9]|B[10]|B[11]|B[12]|B[13]|B[14]|B[15]|B[16]|B[17]|B[18]|B[19]|B[20]|B[21]|B[22]|B[23]|B[24], BLOCK_SOLID);
        // Nugget (small cross: 3×3 center)
        MOLD_RECIPES.put(B[ 7]|B[11]|B[12]|B[13]|B[17], item(MaterialPrefix.nugget));
    }

}
