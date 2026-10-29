/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;




/** Item-output chances in parts per 10_000. An empty list means every output is certain. */
public final class RecipeOutputChances {
    public static final int CERTAIN = 10_000;

    private RecipeOutputChances() {}

    public static int[] normalize(int[] chances, int outputs) {
        if (outputs < 0) throw new IllegalArgumentException("Invalid output count: " + outputs);
        if (chances == null || chances.length == 0) {
            int[] certain = new int[outputs];
            for (int i = 0; i < outputs; i++) certain[i] = CERTAIN;
            return certain;
        }
        if (chances.length != outputs)
            throw new IllegalArgumentException("Output chance count " + chances.length + " does not match " + outputs + " item outputs");
        int[] copy = new int[outputs];
        for (int i = 0; i < outputs; i++) {
            int chance = chances[i];
            if (chance < 1 || chance > CERTAIN)
                throw new IllegalArgumentException("Output chance must be 1.." + CERTAIN + ": " + chance);
            copy[i] = chance;
        }
        return copy;
    }
}
