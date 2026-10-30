/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.machine;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Item and fluid faces for a recipe machine. Leaving {@link gregtech.api.addon.recipe.RecipeMachine.Builder#io(MachineIo)}
 * unset keeps six-side item and fluid access and no automatic face. Once this object is set,
 * a channel that was not named is an empty face list, not the six-side default.
 * Energy faces are not here; they stay on {@link gregtech.api.energy.EnergyInput}.
 * An automatic face is independent of the matching face list: the machine pushes on the automatic
 * face, while the list only limits what pipes may take or insert. An automatic face does not have
 * to appear in that list. Equality compares each face list in its written order.
 *
 * @param itemInputs item-insert faces
 * @param itemOutputs item-extract faces
 * @param fluidInputs fluid-fill faces
 * @param fluidOutputs fluid-drain faces
 * @param itemAutoInput the item automatic input face, or null
 * @param itemAutoOutput the item automatic output face, or null
 * @param fluidAutoInput the fluid automatic input face, or null
 * @param fluidAutoOutput the fluid automatic output face, or null
 * @param itemOutputTarget the item output target, or null
 * @param fluidOutputTarget the fluid output target, or null
 * @param itemInputSource the item input source, or null
 * @param fluidInputSource the fluid input source, or null
 */
public record MachineIo(List<MachineFace> itemInputs, List<MachineFace> itemOutputs,
                        List<MachineFace> fluidInputs, List<MachineFace> fluidOutputs,
                        @Nullable MachineFace itemAutoInput, @Nullable MachineFace itemAutoOutput,
                        @Nullable MachineFace fluidAutoInput, @Nullable MachineFace fluidAutoOutput,
                        @Nullable IoTarget itemOutputTarget, @Nullable IoTarget fluidOutputTarget,
                        @Nullable IoTarget itemInputSource, @Nullable IoTarget fluidInputSource) {
    public MachineIo {
        rejectAutomatic("item output", itemOutputTarget, itemAutoOutput);
        rejectAutomatic("fluid output", fluidOutputTarget, fluidAutoOutput);
        rejectAutomatic("item input", itemInputSource, itemAutoInput);
        rejectAutomatic("fluid input", fluidInputSource, fluidAutoInput);
        itemInputs = MachineFace.distinct(itemInputs, "item input face");
        itemOutputs = MachineFace.distinct(itemOutputs, "item output face");
        fluidInputs = MachineFace.distinct(fluidInputs, "fluid input face");
        fluidOutputs = MachineFace.distinct(fluidOutputs, "fluid output face");
    }

    public boolean hasTarget() {
        return itemOutputTarget != null || fluidOutputTarget != null
                || itemInputSource != null || fluidInputSource != null;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private List<MachineFace> itemInputs = List.of();
        private List<MachineFace> itemOutputs = List.of();
        private List<MachineFace> fluidInputs = List.of();
        private List<MachineFace> fluidOutputs = List.of();
        private @Nullable MachineFace itemAutoInput;
        private @Nullable MachineFace itemAutoOutput;
        private @Nullable MachineFace fluidAutoInput;
        private @Nullable MachineFace fluidAutoOutput;
        private @Nullable IoTarget itemOutputTarget;
        private @Nullable IoTarget fluidOutputTarget;
        private @Nullable IoTarget itemInputSource;
        private @Nullable IoTarget fluidInputSource;

        private Builder() {}

        public Builder itemInputs(MachineFace... faces) { itemInputs = MachineFace.listed(faces); return this; }
        public Builder itemOutputs(MachineFace... faces) { itemOutputs = MachineFace.listed(faces); return this; }
        public Builder fluidInputs(MachineFace... faces) { fluidInputs = MachineFace.listed(faces); return this; }
        public Builder fluidOutputs(MachineFace... faces) { fluidOutputs = MachineFace.listed(faces); return this; }
        public Builder itemAutoInput(MachineFace face) { itemAutoInput = requireFace(face); return this; }
        public Builder itemAutoOutput(MachineFace face) { itemAutoOutput = requireFace(face); return this; }
        public Builder fluidAutoInput(MachineFace face) { fluidAutoInput = requireFace(face); return this; }
        public Builder fluidAutoOutput(MachineFace face) { fluidAutoOutput = requireFace(face); return this; }
        public Builder itemOutputTarget(@Nullable IoTarget target) { itemOutputTarget = target; return this; }
        public Builder fluidOutputTarget(@Nullable IoTarget target) { fluidOutputTarget = target; return this; }
        public Builder itemInputSource(@Nullable IoTarget target) { itemInputSource = target; return this; }
        public Builder fluidInputSource(@Nullable IoTarget target) { fluidInputSource = target; return this; }
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
            return new MachineIo(itemInputs, itemOutputs, fluidInputs, fluidOutputs,
                    itemAutoInput, itemAutoOutput, fluidAutoInput, fluidAutoOutput,
                    itemOutputTarget, fluidOutputTarget, itemInputSource, fluidInputSource);
        }

        private static MachineFace requireFace(MachineFace face) {
            if (face == null) throw new IllegalArgumentException("Missing automatic face");
            return face;
        }
    }

    private static void rejectAutomatic(String channel, @Nullable IoTarget target,
                                        @Nullable MachineFace automatic) {
        if (target != null && automatic != null)
            throw new IllegalArgumentException(channel + " target conflicts with an automatic face");
    }
}
