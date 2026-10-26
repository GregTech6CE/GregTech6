/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;




import gregtech.api.addon.*;

import java.util.Objects;

/**
 * A machine recipe table or a fuel table. The fields match. The two factories do not share
 * a name space: a machine table and a fuel table with the same fields are not equal.
 */
public final class RecipeTable {
    private final boolean fuel;
    private final String name;
    private final String title;
    private final String gui;
    private final int inputItems;
    private final int outputItems;
    private final int minimalInputItems;
    private final int inputFluids;
    private final int outputFluids;
    private final int minimalInputFluids;
    private final int minimalInputs;
    private final boolean needsOutputs;
    private final boolean combinePower;
    private final boolean bucketIn;
    private final boolean bucketOut;

    private RecipeTable(boolean fuel, String name, String title, String gui,
                        int inputItems, int outputItems, int minimalInputItems,
                        int inputFluids, int outputFluids, int minimalInputFluids, int minimalInputs,
                        boolean needsOutputs, boolean combinePower, boolean bucketIn, boolean bucketOut) {
        this.fuel = fuel;
        this.name = name;
        this.title = title;
        this.gui = gui;
        this.inputItems = inputItems;
        this.outputItems = outputItems;
        this.minimalInputItems = minimalInputItems;
        this.inputFluids = inputFluids;
        this.outputFluids = outputFluids;
        this.minimalInputFluids = minimalInputFluids;
        this.minimalInputs = minimalInputs;
        this.needsOutputs = needsOutputs;
        this.combinePower = combinePower;
        this.bucketIn = bucketIn;
        this.bucketOut = bucketOut;
    }

    public boolean machine() { return !fuel; }
    public boolean fuel() { return fuel; }
    public String name() { return name; }
    public String title() { return title; }
    public String gui() { return gui; }
    public int inputItems() { return inputItems; }
    public int outputItems() { return outputItems; }
    public int minimalInputItems() { return minimalInputItems; }
    public int inputFluids() { return inputFluids; }
    public int outputFluids() { return outputFluids; }
    public int minimalInputFluids() { return minimalInputFluids; }
    public int minimalInputs() { return minimalInputs; }
    public boolean needsOutputs() { return needsOutputs; }
    public boolean combinePower() { return combinePower; }
    public boolean bucketIn() { return bucketIn; }
    public boolean bucketOut() { return bucketOut; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RecipeTable that)) return false;
        return fuel == that.fuel && inputItems == that.inputItems && outputItems == that.outputItems
                && minimalInputItems == that.minimalInputItems && inputFluids == that.inputFluids
                && outputFluids == that.outputFluids && minimalInputFluids == that.minimalInputFluids
                && minimalInputs == that.minimalInputs && needsOutputs == that.needsOutputs
                && combinePower == that.combinePower && bucketIn == that.bucketIn && bucketOut == that.bucketOut
                && name.equals(that.name) && title.equals(that.title) && gui.equals(that.gui);
    }

    @Override public String toString() {
        return "RecipeTable[name=" + name + ", fuel=" + fuel + ", items=" + inputItems + "/" + outputItems
                + ", minimalInputItems=" + minimalInputItems + ", fluids=" + inputFluids + "/" + outputFluids
                + ", minimalInputFluids=" + minimalInputFluids + "]";
    }

    @Override public int hashCode() {
        return Objects.hash(fuel, name, title, gui, inputItems, outputItems, minimalInputItems,
                inputFluids, outputFluids, minimalInputFluids, minimalInputs,
                needsOutputs, combinePower, bucketIn, bucketOut);
    }

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
            if (!AddonNames.matches(AddonNames.RECIPE_MAP, name))
                throw new IllegalArgumentException((fuel ? "Invalid fuel map name: " : "Invalid recipe map name: ") + name);
            if (title == null) throw new IllegalArgumentException("Missing recipe table title");
            if (gui == null) throw new IllegalArgumentException("Missing recipe table gui");
            if (inputItems < 0 || outputItems < 0 || minimalInputItems < 0
                    || inputFluids < 0 || outputFluids < 0 || minimalInputFluids < 0 || minimalInputs < 0)
                throw new IllegalArgumentException("Negative slot count on " + name);
            if (minimalInputItems > inputItems)
                throw new IllegalArgumentException("Minimal item inputs exceed the input slots on " + name);
            if (minimalInputFluids > inputFluids)
                throw new IllegalArgumentException("Minimal fluid inputs exceed the input tanks on " + name);
            return new RecipeTable(fuel, name, title, gui, inputItems, outputItems, minimalInputItems,
                    inputFluids, outputFluids, minimalInputFluids, minimalInputs,
                    needsOutputs, combinePower, bucketIn, bucketOut);
        }
    }
}
