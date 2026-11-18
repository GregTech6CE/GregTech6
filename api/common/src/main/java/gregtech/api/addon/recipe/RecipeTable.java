/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import gregtech.api.addon.AddonNames;

/**
 * An immutable recipe table declaration. {@link #machine(String)} and {@link #fuel(String)}
 * create builders for tables in separate namespaces.
 *
 * @param fuel true for a fuel table
 * @param name registry name within the machine or fuel namespace
 * @param presentation display title and GUI texture path
 * @param itemSlots item input and output slots and the minimum item inputs
 * @param fluidTanks fluid input and output tanks and the minimum fluid inputs
 * @param rules total minimum inputs and processing options
 */
public record RecipeTable(
        boolean fuel,
        String name,
        Presentation presentation,
        SlotCounts itemSlots,
        SlotCounts fluidTanks,
        Rules rules
) {
    public RecipeTable {
        if (!AddonNames.matches(AddonNames.RECIPE_MAP, name))
            throw new IllegalArgumentException((fuel ? "Invalid fuel map name: " : "Invalid recipe map name: ") + name);
        if (presentation == null || presentation.title() == null)
            throw new IllegalArgumentException("Missing recipe table title");
        if (!AddonNames.matches(AddonNames.TEXTURE, presentation.gui()))
            throw new IllegalArgumentException("Invalid recipe table gui texture: " + presentation.gui());
        if (itemSlots == null || fluidTanks == null || rules == null)
            throw new IllegalArgumentException("Missing recipe table slot counts or rules on " + name);
        if (itemSlots.hasNegativeCount() || fluidTanks.hasNegativeCount() || rules.minimalInputs() < 0)
            throw new IllegalArgumentException("Negative slot count on " + name);
        if (itemSlots.minimumInputs() > itemSlots.inputs())
            throw new IllegalArgumentException("Minimal item inputs exceed the input slots on " + name);
        if (fluidTanks.minimumInputs() > fluidTanks.inputs())
            throw new IllegalArgumentException("Minimal fluid inputs exceed the input tanks on " + name);
    }

    public boolean machine() { return !fuel; }
    public String title() { return presentation.title(); }
    public String gui() { return presentation.gui(); }
    public int inputItems() { return itemSlots.inputs(); }
    public int outputItems() { return itemSlots.outputs(); }
    public int minimalInputItems() { return itemSlots.minimumInputs(); }
    public int inputFluids() { return fluidTanks.inputs(); }
    public int outputFluids() { return fluidTanks.outputs(); }
    public int minimalInputFluids() { return fluidTanks.minimumInputs(); }
    public int minimalInputs() { return rules.minimalInputs(); }
    public boolean needsOutputs() { return rules.needsOutputs(); }
    public boolean combinePower() { return rules.combinePower(); }
    public boolean bucketIn() { return rules.bucketIn(); }
    public boolean bucketOut() { return rules.bucketOut(); }

    public static Builder machine(String name) { return new Builder(false, name); }

    public static Builder fuel(String name) { return new Builder(true, name); }

    public record Presentation(String title, String gui) {}

    /** Input and output capacity and the minimum number of inputs a recipe must use. */
    public record SlotCounts(int inputs, int outputs, int minimumInputs) {
        private boolean hasNegativeCount() {
            return inputs < 0 || outputs < 0 || minimumInputs < 0;
        }
    }

    /**
     * @param minimalInputs total inputs a recipe must use
     * @param needsOutputs true when a recipe needs room for its outputs
     * @param combinePower true when parallel recipes share one energy charge
     * @param bucketIn true when a bucket fills an input tank
     * @param bucketOut true when a bucket drains an output tank
     */
    public record Rules(int minimalInputs, boolean needsOutputs, boolean combinePower, boolean bucketIn,
                        boolean bucketOut) {}

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
            return new RecipeTable(fuel, name, new Presentation(title, gui),
                    new SlotCounts(inputItems, outputItems, minimalInputItems),
                    new SlotCounts(inputFluids, outputFluids, minimalInputFluids),
                    new Rules(minimalInputs, needsOutputs, combinePower, bucketIn, bucketOut));
        }
    }
}
