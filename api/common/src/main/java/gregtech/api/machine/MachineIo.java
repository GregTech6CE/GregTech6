/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.machine;

import java.util.List;
import java.util.Objects;

/**
 * Item and fluid faces for a recipe machine. Leaving {@link gregtech.api.addon.recipe.RecipeMachine.Builder#io(MachineIo)}
 * unset keeps six-side item and fluid access and no automatic face. Once this object is set,
 * a channel that was not named is an empty face list, not the six-side default.
 * Energy faces are not here; they stay on {@link gregtech.api.energy.EnergyInput}.
 * An automatic face is independent of the matching face list: the machine pushes on the automatic
 * face, while the list only limits what pipes may take or insert. An automatic face does not have
 * to appear in that list. Equality compares each face list in its written order.
 */
public final class MachineIo {
    private final List<MachineFace> itemInputs;
    private final List<MachineFace> itemOutputs;
    private final List<MachineFace> fluidInputs;
    private final List<MachineFace> fluidOutputs;
    private final MachineFace itemAutoInput;
    private final MachineFace itemAutoOutput;
    private final MachineFace fluidAutoInput;
    private final MachineFace fluidAutoOutput;
    private final IoTarget itemOutputTarget;
    private final IoTarget fluidOutputTarget;
    private final IoTarget itemInputSource;
    private final IoTarget fluidInputSource;

    private MachineIo(List<MachineFace> itemInputs, List<MachineFace> itemOutputs,
                      List<MachineFace> fluidInputs, List<MachineFace> fluidOutputs,
                      MachineFace itemAutoInput, MachineFace itemAutoOutput,
                      MachineFace fluidAutoInput, MachineFace fluidAutoOutput,
                      IoTarget itemOutputTarget, IoTarget fluidOutputTarget,
                      IoTarget itemInputSource, IoTarget fluidInputSource) {
        this.itemInputs = itemInputs;
        this.itemOutputs = itemOutputs;
        this.fluidInputs = fluidInputs;
        this.fluidOutputs = fluidOutputs;
        this.itemAutoInput = itemAutoInput;
        this.itemAutoOutput = itemAutoOutput;
        this.fluidAutoInput = fluidAutoInput;
        this.fluidAutoOutput = fluidAutoOutput;
        this.itemOutputTarget = itemOutputTarget;
        this.fluidOutputTarget = fluidOutputTarget;
        this.itemInputSource = itemInputSource;
        this.fluidInputSource = fluidInputSource;
    }

    public List<MachineFace> itemInputs() { return itemInputs; }
    public List<MachineFace> itemOutputs() { return itemOutputs; }
    public List<MachineFace> fluidInputs() { return fluidInputs; }
    public List<MachineFace> fluidOutputs() { return fluidOutputs; }
    public MachineFace itemAutoInput() { return itemAutoInput; }
    public MachineFace itemAutoOutput() { return itemAutoOutput; }
    public MachineFace fluidAutoInput() { return fluidAutoInput; }
    public MachineFace fluidAutoOutput() { return fluidAutoOutput; }
    public IoTarget itemOutputTarget() { return itemOutputTarget; }
    public IoTarget fluidOutputTarget() { return fluidOutputTarget; }
    public IoTarget itemInputSource() { return itemInputSource; }
    public IoTarget fluidInputSource() { return fluidInputSource; }

    public boolean hasTarget() {
        return itemOutputTarget != null || fluidOutputTarget != null
                || itemInputSource != null || fluidInputSource != null;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MachineIo that)) return false;
        return itemInputs.equals(that.itemInputs) && itemOutputs.equals(that.itemOutputs)
                && fluidInputs.equals(that.fluidInputs) && fluidOutputs.equals(that.fluidOutputs)
                && itemAutoInput == that.itemAutoInput && itemAutoOutput == that.itemAutoOutput
                && fluidAutoInput == that.fluidAutoInput && fluidAutoOutput == that.fluidAutoOutput
                && Objects.equals(itemOutputTarget, that.itemOutputTarget)
                && Objects.equals(fluidOutputTarget, that.fluidOutputTarget)
                && Objects.equals(itemInputSource, that.itemInputSource)
                && Objects.equals(fluidInputSource, that.fluidInputSource);
    }

    @Override public String toString() {
        return "MachineIo[itemIn=" + itemInputs + ", itemOut=" + itemOutputs + ", fluidIn=" + fluidInputs
                + ", fluidOut=" + fluidOutputs + ", itemAutoIn=" + itemAutoInput + ", itemAutoOut=" + itemAutoOutput
                + ", fluidAutoIn=" + fluidAutoInput + ", fluidAutoOut=" + fluidAutoOutput
                + ", itemOutTarget=" + itemOutputTarget + ", fluidOutTarget=" + fluidOutputTarget
                + ", itemInSource=" + itemInputSource + ", fluidInSource=" + fluidInputSource + "]";
    }

    @Override public int hashCode() {
        return Objects.hash(itemInputs, itemOutputs, fluidInputs, fluidOutputs,
                itemAutoInput, itemAutoOutput, fluidAutoInput, fluidAutoOutput,
                itemOutputTarget, fluidOutputTarget, itemInputSource, fluidInputSource);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private List<MachineFace> itemInputs = List.of();
        private List<MachineFace> itemOutputs = List.of();
        private List<MachineFace> fluidInputs = List.of();
        private List<MachineFace> fluidOutputs = List.of();
        private MachineFace itemAutoInput;
        private MachineFace itemAutoOutput;
        private MachineFace fluidAutoInput;
        private MachineFace fluidAutoOutput;
        private IoTarget itemOutputTarget;
        private IoTarget fluidOutputTarget;
        private IoTarget itemInputSource;
        private IoTarget fluidInputSource;

        private Builder() {}

        public Builder itemInputs(MachineFace... faces) { itemInputs = MachineFace.listed(faces); return this; }
        public Builder itemOutputs(MachineFace... faces) { itemOutputs = MachineFace.listed(faces); return this; }
        public Builder fluidInputs(MachineFace... faces) { fluidInputs = MachineFace.listed(faces); return this; }
        public Builder fluidOutputs(MachineFace... faces) { fluidOutputs = MachineFace.listed(faces); return this; }
        public Builder itemAutoInput(MachineFace face) { itemAutoInput = requireFace(face); return this; }
        public Builder itemAutoOutput(MachineFace face) { itemAutoOutput = requireFace(face); return this; }
        public Builder fluidAutoInput(MachineFace face) { fluidAutoInput = requireFace(face); return this; }
        public Builder fluidAutoOutput(MachineFace face) { fluidAutoOutput = requireFace(face); return this; }
        public Builder itemOutputTarget(IoTarget target) { itemOutputTarget = target; return this; }
        public Builder fluidOutputTarget(IoTarget target) { fluidOutputTarget = target; return this; }
        public Builder itemInputSource(IoTarget target) { itemInputSource = target; return this; }
        public Builder fluidInputSource(IoTarget target) { fluidInputSource = target; return this; }
        public Builder itemOutputTarget(int right, int up, int back, MachineFace receiver) {
            return itemOutputTarget(IoTarget.at(right, up, back, receiver));
        }
        public Builder fluidOutputTarget(int right, int up, int back, MachineFace receiver) {
            return fluidOutputTarget(IoTarget.at(right, up, back, receiver));
        }
        public Builder itemInputSource(int right, int up, int back, MachineFace receiver) {
            return itemInputSource(IoTarget.at(right, up, back, receiver));
        }
        public Builder fluidInputSource(int right, int up, int back, MachineFace receiver) {
            return fluidInputSource(IoTarget.at(right, up, back, receiver));
        }

        public MachineIo build() {
            rejectAutomatic("item output", itemOutputTarget, itemAutoOutput);
            rejectAutomatic("fluid output", fluidOutputTarget, fluidAutoOutput);
            rejectAutomatic("item input", itemInputSource, itemAutoInput);
            rejectAutomatic("fluid input", fluidInputSource, fluidAutoInput);
            return new MachineIo(
                    MachineFace.distinct(itemInputs, "item input face"),
                    MachineFace.distinct(itemOutputs, "item output face"),
                    MachineFace.distinct(fluidInputs, "fluid input face"),
                    MachineFace.distinct(fluidOutputs, "fluid output face"),
                    itemAutoInput, itemAutoOutput, fluidAutoInput, fluidAutoOutput,
                    itemOutputTarget, fluidOutputTarget, itemInputSource, fluidInputSource);
        }

        private static MachineFace requireFace(MachineFace face) {
            if (face == null) throw new IllegalArgumentException("Missing automatic face");
            return face;
        }

        private static void rejectAutomatic(String channel, IoTarget target, MachineFace automatic) {
            if (target != null && automatic != null)
                throw new IllegalArgumentException(channel + " target conflicts with an automatic face");
        }
    }
}
