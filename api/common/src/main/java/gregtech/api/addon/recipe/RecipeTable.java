/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import gregtech.api.addon.AddonNames;

/**
 * A machine recipe table or a fuel table. The fields match. The two factories do not share
 * a name space: a machine table and a fuel table with the same fields are not equal.
 *
 * @param fuel true for a fuel table
 * @param name the registry name
 * @param title the display title
 * @param gui the gui texture path
 * @param inputItems item input slots
 * @param outputItems item output slots
 * @param minimalInputItems item inputs a recipe must use
 * @param inputFluids fluid input tanks
 * @param outputFluids fluid output tanks
 * @param minimalInputFluids fluid inputs a recipe must use
 * @param minimalInputs total inputs a recipe must use
 * @param needsOutputs true when a recipe needs room for its outputs
 * @param combinePower true when parallel recipes share one energy charge
 * @param bucketIn true when a bucket fills an input tank
 * @param bucketOut true when a bucket drains an output tank
 */
public record RecipeTable(boolean fuel, String name, String title, String gui, int inputItems, int outputItems,
                          int minimalInputItems, int inputFluids, int outputFluids, int minimalInputFluids,
                          int minimalInputs, boolean needsOutputs, boolean combinePower, boolean bucketIn,
                          boolean bucketOut) {
    public RecipeTable {
        if (!AddonNames.matches(AddonNames.RECIPE_MAP, name))
            throw new IllegalArgumentException((fuel ? "Invalid fuel map name: " : "Invalid recipe map name: ") + name);
        if (title == null) throw new IllegalArgumentException("Missing recipe table title");
        if (!AddonNames.matches(AddonNames.TEXTURE, gui))
            throw new IllegalArgumentException("Invalid recipe table gui texture: " + gui);
        if (inputItems < 0 || outputItems < 0 || minimalInputItems < 0
                || inputFluids < 0 || outputFluids < 0 || minimalInputFluids < 0 || minimalInputs < 0)
            throw new IllegalArgumentException("Negative slot count on " + name);
        if (minimalInputItems > inputItems)
            throw new IllegalArgumentException("Minimal item inputs exceed the input slots on " + name);
        if (minimalInputFluids > inputFluids)
            throw new IllegalArgumentException("Minimal fluid inputs exceed the input tanks on " + name);
    }

    public boolean machine() { return !fuel; }

    public static Builder machine(String name) { return new Builder(false, name); }

    public static Builder fuel(String name) { return new Builder(true, name); }

    public static final class Builder {
        private final boolean fuel;
        private final String name;
        private String title;
        private String gui;
        private int inputItems;
        private int outputItems;
        private int minimalInputItems;
        private int inputFluids;
        private int outputFluids;
        private int minimalInputFluids;
        private int minimalInputs;
        private boolean needsOutputs;
        private boolean combinePower;
        private boolean bucketIn;
        private boolean bucketOut;

        private Builder(boolean fuel, String name) {
            this.fuel = fuel;
            this.name = name;
        }

        public Builder title(String value) { title = value; return this; }
        public Builder gui(String value) { gui = value; return this; }
        public Builder inputItems(int value) { inputItems = value; return this; }
        public Builder outputItems(int value) { outputItems = value; return this; }
        public Builder minimalInputItems(int value) { minimalInputItems = value; return this; }
        public Builder inputFluids(int value) { inputFluids = value; return this; }
        public Builder outputFluids(int value) { outputFluids = value; return this; }
        public Builder minimalInputFluids(int value) { minimalInputFluids = value; return this; }
        public Builder minimalInputs(int value) { minimalInputs = value; return this; }
        public Builder needsOutputs(boolean value) { needsOutputs = value; return this; }
        public Builder combinePower(boolean value) { combinePower = value; return this; }
        public Builder bucketIn(boolean value) { bucketIn = value; return this; }
        public Builder bucketOut(boolean value) { bucketOut = value; return this; }

        public RecipeTable build() {
            return new RecipeTable(fuel, name, title, gui, inputItems, outputItems, minimalInputItems,
                    inputFluids, outputFluids, minimalInputFluids, minimalInputs,
                    needsOutputs, combinePower, bucketIn, bucketOut);
        }
    }
}
